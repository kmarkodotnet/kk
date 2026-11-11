package hu.kyokushin.trainer.core.data.repository

import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.TechniqueAliasEntity
import hu.kyokushin.trainer.core.data.db.TechniqueSearchRow
import hu.kyokushin.trainer.core.data.db.toCategory
import hu.kyokushin.trainer.core.data.db.toContentState
import hu.kyokushin.trainer.core.data.db.toTechnique
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import hu.kyokushin.trainer.core.model.Category
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.Technique
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.generator.QuizCandidateSet
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * A [TechniqueRepository] egyetlen megvalósítása Room felett (ARCHITECTURE 5.1, NFR-03).
 *
 * `internal`, és a Hilt köti be (`@Binds`, T-203). A kötésnek a `core/data`-ban kell
 * élnie, mert a konstruktor paramétere, a [KyokushinDatabase] is `internal` – a `di`
 * modul nem látja a típust, amit injektálnia kellene.
 *
 * ## Négy szabály, amit ez az osztály betart
 *
 * 1. **A `retired` szűrés és a keresés is itt van, nem a hívónál** (ADR-004, DR-07,
 *    FR-03). A listánál és a kvízjelölteknél **SQL-ben**, nem Kotlin oldali `filter`-rel:
 *    így egy jövőbeli, nyers DAO-hívás sem tudja véletlenül kikerülni. A [getTechnique] a
 *    dokumentált kivétel.
 * 2. **Egyetlen Room típus sem szivárog ki.** A metódusok domain modellt adnak vissza, a
 *    leképezés a `ContentEntityMappers` (T-117) – nincs második mapper-implementáció.
 * 3. **Nincs N+1 lekérdezés** (NFR-10). Az aliasok egyetlen `technique_alias`
 *    beolvasásból, memóriában csoportosítva kerülnek a technikákra, nem technikánkénti
 *    külön kérdéssel.
 * 4. **Nincs közvetlen szálkezelés és nincs blokkolás** (NFR-01, NFR-07). A `suspend`
 *    metódusok `withContext(ioDispatcher)`-rel, a `Flow`-k `flowOn(ioDispatcher)`-rel
 *    futnak; a dispatcher injektált, hogy a teszt virtuális órája vezérelhesse.
 *
 * ## A keresés (FR-03, T-202, ADR-012)
 *
 * A szűrés teljes egészében az adatbázisban fut: az előre normalizált
 * `technique.search_romaji`, `technique.search_hungarian` és
 * `technique_alias.aliasNormalized` oszlopokon `LIKE '%…%'` feltétellel. A normalizálás
 * a `NameNormalizer` – ugyanaz a függvény, amit a validátor és az import használ –,
 * tehát a keresés nem találhat olyan egyezést, amit a szinonimakizárás nem tart
 * azonosnak. Az egyező alias ugyanabból a lekérdezésből jön, mert a UI-nak ki kell
 * írnia („Egyezés: `{alias}`"), és egy Kotlin oldali újbóli mintaillesztés ugyanazt a
 * feltételt írná le másodszor.
 *
 * ## Mi tartozik a következő feladatokhoz
 *
 * - Az [QuizCandidateSet.illustrationDigests] feltöltése a T-402 tárgya: a képek tartalmi
 *   ujjlenyomatához **nincs oszlop** a sémában, tehát a mező itt üres. Üres térkép
 *   mellett a `JP_TO_IMAGE` típus nem tud vizuálisan azonos képpárt kizárni – ezt a
 *   hiányt a T-402 pótolja, nem egy itt kitalált, becsült ujjlenyomat.
 *
 * @property database az adatbázis; a DAO-k rajta keresztül érhetők el.
 * @property ioDispatcher a háttér-I/O dispatcher; a kötést a `di` modul adja (T-203).
 */
internal class RoomTechniqueRepository @Inject constructor(
    private val database: KyokushinDatabase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TechniqueRepository {

    override fun observeCategories(): Flow<List<Category>> =
        database.categoryDao()
            .observeAll()
            .map { rows -> rows.map { it.toCategory() } }
            .flowOn(ioDispatcher)

    override fun observeTechniques(
        query: String,
        categories: Set<CategoryId>,
    ): Flow<List<Technique>> =
        observeTechniqueMatches(query, categories)
            .map { matches -> matches.map(TechniqueMatch::technique) }

    override fun observeTechniqueMatches(
        query: String,
        categories: Set<CategoryId>,
    ): Flow<List<TechniqueMatch>> {
        // A normalizálás a `core/quiz` NameNormalizer-éé: ugyanaz az alak, amit a validátor
        // V006 szinonimavizsgálata, a tárolt `aliasNormalized` és a két keresőoszlop
        // használ (FR-03). Két külön szabály azt jelentené, hogy a keresés olyan egyezést
        // talál, amit a szinonimakizárás nem tart azonosnak.
        val pattern = searchPattern(NameNormalizer.normalize(query))
        return combine(
            matchingTechniques(pattern, categories),
            database.techniqueAliasDao().observeAll(),
        ) { rows, aliasRows ->
            val aliasesById = aliasRows.groupBy { it.techniqueId }
            rows.map { row ->
                TechniqueMatch(
                    technique = row.technique.toTechnique(
                        aliasesById[row.technique.id].orEmpty().map(TechniqueAliasEntity::alias),
                    ),
                    // Ha a technika saját neve is illeszkedett, a találat önmagát
                    // magyarázza – ilyenkor nincs mit kiírni (FUNCTIONAL_SPEC 3.1).
                    matchedAlias = row.matchedAlias.takeUnless { row.nameMatched },
                )
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun getTechnique(id: TechniqueId): Technique? = withContext(ioDispatcher) {
        // Itt szándékosan `getById`, nem az aktívakra szűrő lekérdezés: a Haladás
        // képernyőnek a visszavont technikát is fel kell tudnia oldani (DR-07,
        // FUNCTIONAL_SPEC 6.2). A `null` jelentése „ilyen azonosító nincs", nem
        // „kikerült a tananyagból".
        val row = database.techniqueDao().getById(id) ?: return@withContext null
        val aliases = database.techniqueAliasDao().getForTechnique(id)
        row.toTechnique(aliases.map(TechniqueAliasEntity::alias))
    }

    override suspend fun getQuizCandidates(categories: Set<CategoryId>): QuizCandidateSet =
        withContext(ioDispatcher) {
            val rows = if (categories.isEmpty()) {
                database.techniqueDao().getActive()
            } else {
                database.techniqueDao().getActiveInCategories(categories.toList())
            }
            val aliasesById = database.techniqueAliasDao().getAll().groupBy { it.techniqueId }
            val techniques = rows.map { row ->
                row.toTechnique(aliasesById[row.id].orEmpty().map(TechniqueAliasEntity::alias))
            }
            QuizCandidateSet(
                techniques = techniques,
                synonymGroups = synonymGroups(techniques.mapTo(HashSet()) { it.id }),
                // Nincs ujjlenyomat-oszlop a sémában – a feltöltés a T-402 dolga.
                illustrationDigests = emptyMap(),
            )
        }

    override fun observeContentState(): Flow<ContentState> =
        combine(
            database.contentStateDao().observe(),
            database.techniqueDao().observeActiveCount(),
        ) { state, activeCount -> state.toContentState(activeCount) }
            .flowOn(ioDispatcher)

    /**
     * A keresésnek megfelelő aktív technikák a kategóriaszűrő két ágán.
     *
     * Üres halmaz jelentése **minden kategória**, nem „semmi" – ezért külön lekérdezés,
     * nem egy `IN ()` üres listával (az egyetlen sort sem adna vissza).
     */
    private fun matchingTechniques(
        pattern: String,
        categories: Set<CategoryId>,
    ): Flow<List<TechniqueSearchRow>> =
        if (categories.isEmpty()) {
            database.techniqueDao().observeActiveMatching(pattern)
        } else {
            database.techniqueDao()
                .observeActiveMatchingInCategories(pattern, categories.toList())
        }

    /**
     * A `synonym_group` tagságok visszaolvasása a [QuizCandidateSet] alakjára (V006, CR-07).
     *
     * A szűrés után a csoportok **megcsonkulhatnak**: egy visszavont vagy kategóriaszűrőn
     * kiesett tag nem lehet a készlet része, és a [QuizCandidateSet] konstruktora joggal
     * dobna egy ismeretlen azonosítóra hivatkozó csoportra. Ezért a nem ismert tagok
     * kiesnek, és a kétnél kevesebb tagúra fogyott csoport is – egy egyelemű „csoport"
     * nem zár ki semmit.
     */
    private suspend fun synonymGroups(knownIds: Set<TechniqueId>): List<Set<TechniqueId>> =
        database.synonymGroupDao()
            .getAll()
            .groupBy { it.groupId }
            .toSortedMap()
            .values
            .map { members ->
                members.asSequence()
                    .map { it.techniqueId }
                    .filterTo(LinkedHashSet()) { it in knownIds }
            }
            .filter { it.size >= 2 }

    /**
     * A normalizált keresőszövegből **kész `LIKE` minta** (FR-03, ADR-012).
     *
     * Két dolgot tesz, és mindkettő szándékos:
     *
     * 1. **Rész-szó egyezés**: `%` mindkét oldalon, tehát a `dachi` a `Heisoku Dachi`
     *    végén is talál, nem csak prefixként.
     * 2. **A joker karakterek escape-elése**: a beírt `%` és `_` literális karakter, nem
     *    joker – egy „%" beírása különben a teljes tudástárat adná vissza. Az escape
     *    karakter a `\`, ezért azt kell elsőként duplázni.
     *
     * Üres keresőszövegre a minta `%`: minden sorra illeszkedik, mert a kikapcsolt szűrő
     * jelentése „nincs szűrés", nem „nincs találat".
     */
    private fun searchPattern(normalizedQuery: String): String {
        val escaped = normalizedQuery
            .replace("""\""", """\\""")
            .replace("%", """\%""")
            .replace("_", """\_""")
        return "%$escaped%"
    }
}
