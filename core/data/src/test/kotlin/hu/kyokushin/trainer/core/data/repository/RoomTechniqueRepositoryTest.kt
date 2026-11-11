package hu.kyokushin.trainer.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.CategoryEntity
import hu.kyokushin.trainer.core.data.db.ContentStateEntity
import hu.kyokushin.trainer.core.data.db.IllustrationKind
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.SynonymGroupMemberEntity
import hu.kyokushin.trainer.core.data.db.TechniqueAliasEntity
import hu.kyokushin.trainer.core.data.db.TechniqueEntity
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.text.NameNormalizer
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A [TechniqueRepository] szerződésének tényleges viselkedése in-memory Room példányon
 * (FR-01…FR-03, FR-31, FR-32, DR-02, DR-07, CR-07, NFR-03).
 *
 * A legfontosabb, amit ez a készlet bizonyít: a **`retired` szűrés a repositoryban van,
 * nem a hívónál** (ADR-004). Ez csendes hiba lenne – visszavont technikák jelennének meg a
 * listában és a kvízben, hibajelzés nélkül –, ezért mindhárom érintett metódusnak külön
 * tesztje van, a [getTechnique] dokumentált kivételével együtt.
 *
 * Robolectric kell hozzá, mert a Room `SupportSQLite` implementációja Android API –
 * emulátor viszont nem. A dispatcher teszt-dispatcher: a determinizmus feltétele, hogy ne
 * a rendszertől jöjjön (NFR-07).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomTechniqueRepositoryTest {

    private lateinit var database: KyokushinDatabase
    private lateinit var repository: TechniqueRepository

    private val tsuki = CategoryId.of("tsuki")
    private val dachi = CategoryId.of("dachi")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KyokushinDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /**
     * A tesztkörnyezet: in-memory adatbázis + a vizsgált repository teszt-dispatcherrel.
     *
     * A teszt-dispatchernek **a `runTest` ütemezőjét** kell megkapnia. Egy paraméter
     * nélküli `UnconfinedTestDispatcher()` saját `TestCoroutineScheduler`-t hoz létre, és
     * a `combine`/`flowOn` láncon a Room megfigyelt lekérdezése két különböző ütemezővel
     * találkozna („Detected use of different schedulers"). Pontszerű, `withContext`-es
     * metódusnál ez nem derül ki – a `Flow`-s ágaknál igen, ezért közös a gyárfüggvény.
     */
    private fun repositoryTest(body: suspend TestScope.() -> Unit) = runTest {
        repository = RoomTechniqueRepository(
            database = database,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )
        body()
    }

    // --- DR-02 / FR-01: a kategóriák a tartalom sorrendjében ---------------------

    @Test
    fun observeCategories_followsContentSortOrderNotAlphabet_DR02() = repositoryTest {
        database.categoryDao().upsertAll(
            listOf(
                CategoryEntity(id = tsuki, nameHu = "Ütések", sortOrder = 1),
                CategoryEntity(id = dachi, nameHu = "Állások", sortOrder = 0),
            ),
        )

        val categories = repository.observeCategories().first()

        // Ábécé szerint az "Állások" is előre kerülne, de itt a `sortOrder` dönt: a
        // kategóriák oktatási logikát követnek (DR-02, ADR-011). A fordított `sortOrder`
        // ezért szándékos a fixtúrában.
        assertThat(categories.map { it.id }).containsExactly(dachi, tsuki).inOrder()
        assertThat(categories.map { it.nameHu }).containsExactly("Állások", "Ütések").inOrder()
    }

    // --- DR-07 / ADR-004: a `retired` szűrés a repository felelőssége -----------

    @Test
    fun observeTechniques_excludesRetiredTechniques_DR07() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki"),
                technique(id = "KY-0002", romaji = "Visszavont Technika", retired = true),
            ),
        )

        val techniques = repository.observeTechniques().first()

        assertThat(techniques.map { it.id.value }).containsExactly("KY-0001")
    }

    @Test
    fun getQuizCandidates_excludesRetiredTechniques_DR07() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki"),
                technique(id = "KY-0002", romaji = "Visszavont Technika", retired = true),
            ),
        )

        val candidates = repository.getQuizCandidates()

        assertThat(candidates.techniques.map { it.id.value }).containsExactly("KY-0001")
    }

    @Test
    fun getTechnique_resolvesRetiredTechniqueSoProgressKeepsReference_DR07() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(technique(id = "KY-0002", romaji = "Visszavont Technika", retired = true)),
        )

        val retired = repository.getTechnique(TechniqueId.of("KY-0002"))
        val unknown = repository.getTechnique(TechniqueId.of("KY-9999"))

        // A Haladás képernyőnek fel kell tudnia oldani a visszavont technikát, hogy
        // kiírhassa: „már nem része a tananyagnak, de a korábbi eredményeid megmaradtak"
        // (FUNCTIONAL_SPEC 6.2). A `null` ezért csak az ismeretlen azonosítót jelenti.
        assertThat(retired).isNotNull()
        assertThat(retired!!.retired).isTrue()
        assertThat(unknown).isNull()
    }

    // --- FR-03: ékezet- és kisbetű-érzéketlen részszó-keresés --------------------

    @Test
    fun observeTechniques_matchesRomajiHungarianAndAliasCaseAndAccentInsensitively_FR03() =
        repositoryTest {
            seedCategories()
            database.techniqueDao().upsertAll(
                listOf(
                    technique(
                        id = "KY-0001",
                        categoryId = dachi,
                        romaji = "Heisoku Dachi",
                        hungarian = "Zárt lábú állás",
                    ),
                    technique(id = "KY-0002", romaji = "Seiken Chudan Tsuki", hungarian = "Ököl ütés"),
                ),
            )
            database.techniqueAliasDao().upsertAll(
                listOf(
                    TechniqueAliasEntity(
                        techniqueId = TechniqueId.of("KY-0002"),
                        alias = "Chūdan Zuki",
                        aliasNormalized = "chudan zuki",
                    ),
                ),
            )

            // romaji, nagybetűvel: a "DACHI" minden `… dachi` technikát megtalál
            assertThat(repository.observeTechniques(query = "DACHI").first().map { it.id.value })
                .containsExactly("KY-0001")
            // magyar név, ékezet nélkül és szó közepén
            assertThat(repository.observeTechniques(query = "zart labu").first().map { it.id.value })
                .containsExactly("KY-0001")
            // alias, a tárolt normalizált alakon
            assertThat(repository.observeTechniques(query = "ZUKI").first().map { it.id.value })
                .containsExactly("KY-0002")
            // üres keresés = nincs szűrés, nem „nincs találat"
            assertThat(repository.observeTechniques(query = "   ").first()).hasSize(2)
        }

    @Test
    fun observeTechniques_emptyCategoryFilterMeansEveryCategory_FR02() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", categoryId = dachi, romaji = "Heisoku Dachi"),
                technique(id = "KY-0002", categoryId = tsuki, romaji = "Seiken Chudan Tsuki"),
            ),
        )

        assertThat(repository.observeTechniques(categories = emptySet()).first()).hasSize(2)
        assertThat(
            repository.observeTechniques(categories = setOf(dachi)).first().map { it.id.value },
        ).containsExactly("KY-0001")
    }

    @Test
    fun observeTechniques_attachesAliasesWithoutPerRecordQuery_FR03() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(technique(id = "KY-0002", romaji = "Seiken Chudan Tsuki")),
        )
        database.techniqueAliasDao().upsertAll(
            listOf(
                TechniqueAliasEntity(TechniqueId.of("KY-0002"), "Chudan Zuki", "chudan zuki"),
                TechniqueAliasEntity(TechniqueId.of("KY-0002"), "Chudan Tsuki", "chudan tsuki"),
            ),
        )

        val technique = repository.observeTechniques().first().single()

        assertThat(technique.aliases).containsExactly("Chudan Tsuki", "Chudan Zuki")
    }

    @Test
    fun observeTechniques_findsEveryDachiTechniqueForAnUppercaseQuery_FR03() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", categoryId = dachi, romaji = "Heisoku Dachi"),
                technique(id = "KY-0002", categoryId = dachi, romaji = "Zenkutsu Dachi"),
                technique(id = "KY-0003", romaji = "Seiken Chudan Tsuki"),
            ),
        )

        // US-06 2. kritérium: a „DACHI" **minden** `… dachi` technikát megtalál, nem csak
        // az elsőt – a `LIKE '%dachi%'` részszó-egyezés, nem prefix.
        assertThat(repository.observeTechniques(query = "DACHI").first().map { it.id.value })
            .containsExactly("KY-0001", "KY-0002")
    }

    @Test
    fun observeTechniques_filtersOnTheStoredSearchColumnsInSql_FR03() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki"),
                // Szándékosan üres keresőoszlopok: a név illeszkedne, a tárolt normalizált
                // alak viszont nem. Ha a szűrés Kotlin oldalon, a beolvasott soron futna, ez
                // a technika mégis találat lenne – a teszt tehát azt bizonyítja, hogy a
                // feltétel az adatbázisban, a `search_*` oszlopokon fut (ADR-012).
                technique(id = "KY-0002", romaji = "Seiken Jodan Tsuki").copy(
                    searchRomaji = "",
                    searchHungarian = "",
                ),
            ),
        )

        val hits = repository.observeTechniques(query = "tsuki").first()

        assertThat(hits.map { it.id.value }).containsExactly("KY-0001")
    }

    @Test
    fun observeTechniqueMatches_tellsWhichAliasMatched_FR03() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki", hungarian = "Ököl ütés"),
            ),
        )
        // Az `aliases` a valódi tartalomban mindenhol üres (R-05), ezért az alias-ág
        // fixtúrája szintetikus – de a tárolt `aliasNormalized` ugyanaz a normalizáló
        // kimenete, mint éles úton.
        database.techniqueAliasDao().upsertAll(
            listOf(
                aliasOf("KY-0001", "Chūdan Zuki"),
                aliasOf("KY-0001", "Ago Uchi"),
            ),
        )

        // Alias alapján talált tétel: a UI kiírja, hogy „Egyezés: Chūdan Zuki"
        // (FUNCTIONAL_SPEC 3.1, US-06 3. kritérium).
        val aliasHit = repository.observeTechniqueMatches(query = "ZUKI").first().single()
        assertThat(aliasHit.technique.id.value).isEqualTo("KY-0001")
        assertThat(aliasHit.matchedAlias).isEqualTo("Chūdan Zuki")

        // A technika saját nevére illeszkedő találat önmagát magyarázza: nincs mit kiírni.
        val nameHit = repository.observeTechniqueMatches(query = "chudan tsuki").first().single()
        assertThat(nameHit.matchedAlias).isNull()

        // Magyar név szerinti és üres keresésre sincs alias-megjelölés.
        assertThat(repository.observeTechniqueMatches(query = "okol").first().single().matchedAlias)
            .isNull()
        assertThat(repository.observeTechniqueMatches().first().single().matchedAlias).isNull()
    }

    @Test
    fun observeTechniques_treatsLikeWildcardsAsLiteralCharacters_FR03() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki")),
        )

        // A `%` és a `_` a keresőszövegben beírt karakter, nem joker: különben egyetlen
        // `%` a teljes tudástárat adná vissza, és a találatlista hazudna.
        assertThat(repository.observeTechniques(query = "%").first()).isEmpty()
        assertThat(repository.observeTechniques(query = "chudan_tsuki").first()).isEmpty()
        assertThat(repository.observeTechniques(query = "chudan tsuki").first()).hasSize(1)
    }

    // --- CR-07 / V006: a szinonimacsoportok a szűrés után is érvényesek ----------

    @Test
    fun getQuizCandidates_dropsSynonymMembersOutsideTheCandidateSet_CR07() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki"),
                technique(id = "KY-0002", romaji = "Chudan Zuki"),
                technique(id = "KY-0003", romaji = "Visszavont Technika", retired = true),
            ),
        )
        database.synonymGroupDao().upsertAll(
            listOf(
                // 0. csoport: két aktív tag → megmarad
                SynonymGroupMemberEntity(groupId = 0, techniqueId = TechniqueId.of("KY-0001")),
                SynonymGroupMemberEntity(groupId = 0, techniqueId = TechniqueId.of("KY-0002")),
                // 1. csoport: az egyik tagja visszavont → egyelemű lesz, ezért kiesik
                SynonymGroupMemberEntity(groupId = 1, techniqueId = TechniqueId.of("KY-0001")),
                SynonymGroupMemberEntity(groupId = 1, techniqueId = TechniqueId.of("KY-0003")),
            ),
        )

        val candidates = repository.getQuizCandidates()

        // Egyetlen csoport marad, és az sem hivatkozik a készleten kívüli azonosítóra –
        // különben a QuizCandidateSet konstruktora joggal dobna.
        assertThat(candidates.synonymGroups).containsExactly(
            setOf(TechniqueId.of("KY-0001"), TechniqueId.of("KY-0002")),
        )
        assertThat(candidates.synonymsOf(TechniqueId.of("KY-0001")))
            .containsExactly(TechniqueId.of("KY-0002"))
    }

    @Test
    fun getQuizCandidates_honoursCategoryFilterAndKeepsIllustrationGates_FR11() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(
                    id = "KY-0001",
                    categoryId = dachi,
                    romaji = "Heisoku Dachi",
                    illustrationKind = IllustrationKind.ASSET,
                ),
                technique(id = "KY-0002", categoryId = tsuki, romaji = "Seiken Chudan Tsuki"),
            ),
        )

        val candidates = repository.getQuizCandidates(categories = setOf(dachi))

        assertThat(candidates.techniques.map { it.id.value }).containsExactly("KY-0001")
        val illustration = candidates.techniques.single().illustration
        // A kvízkapu `false` marad: a mapper nem emel kapuszintet (CR-02, ADR-010).
        assertThat(illustration).isInstanceOf(IllustrationRef.Asset::class.java)
        assertThat((illustration as IllustrationRef.Asset).quizUsable).isFalse()
    }

    // --- FR-31 / FR-32: a tartalomállapot ---------------------------------------

    @Test
    fun observeContentState_reportsNotImportedBeforeTheFirstImport_FR32() = repositoryTest {
        val state = repository.observeContentState().first()

        assertThat(state).isEqualTo(ContentState.NotImported)
        assertThat(state.hasContent).isFalse()
        // Ismeretlen állapotot nem minősítünk lektoráltnak (CR-02).
        assertThat(state.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
    }

    @Test
    fun observeContentState_reportsCurrentTableContentNotTheLastImportBalance_FR31() = repositoryTest {
        seedCategories()
        database.techniqueDao().upsertAll(
            listOf(
                technique(id = "KY-0001", romaji = "Seiken Chudan Tsuki"),
                technique(id = "KY-0002", romaji = "Visszavont Technika", retired = true),
            ),
        )
        // Egy elutasított frissítés: a tartalomtáblák a korábbi kiadásból vannak, a
        // `content_state` mérlege viszont nulla elfogadott rekord (FR-31).
        database.contentStateDao().upsert(
            ContentStateEntity(
                schemaVersion = "1.0.0",
                contentVersion = "2026.11.01",
                datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
                importedAt = Instant.ofEpochMilli(1_762_000_000_000L),
                acceptedCount = 0,
                rejectedCount = 0,
                blockingIssueCount = 1,
                warningIssueCount = 0,
                datasetRejected = true,
            ),
        )

        val state = repository.observeContentState().first()

        assertThat(state).isInstanceOf(ContentState.Imported::class.java)
        val imported = state as ContentState.Imported
        assertThat(imported.datasetRejected).isTrue()
        assertThat(imported.acceptedCount).isEqualTo(0)
        // A visszavont sor nem számít bele, az aktív igen – és így `hasContent` igaz
        // marad egy elutasított frissítés után is.
        assertThat(imported.activeTechniqueCount).isEqualTo(1)
        assertThat(imported.hasContent).isTrue()
        assertThat(imported.contentVersion).isEqualTo("2026.11.01")
    }

    // --- NFR-03 / TR-07 / ADR-001 L4: a publikus API nem lát Room típust --------

    @Test
    fun techniqueRepository_publicApiExposesNoRoomOrDatabaseTypes_NFR03() {
        val leaked = TechniqueRepository::class.java.methods.flatMap { method ->
            val types = buildList {
                add(method.genericReturnType)
                addAll(method.genericParameterTypes)
            }
            types.flatMap { collectTypeNames(it) }
        }.filter { name ->
            name.contains(".core.data.db.") ||
                name.startsWith("androidx.room") ||
                name.startsWith("androidx.sqlite") ||
                name.contains(".content.KnowledgeDto") ||
                name.endsWith("Entity") ||
                name.endsWith("Dao") ||
                name.endsWith("Dto")
        }

        assertThat(leaked).isEmpty()
    }

    /** Egy generikus típus minden összetevőjének neve – `Flow<List<Technique>>` is. */
    private fun collectTypeNames(type: Type): List<String> = when (type) {
        is ParameterizedType ->
            listOf((type.rawType as? Class<*>)?.name.orEmpty()) +
                type.actualTypeArguments.flatMap { collectTypeNames(it) }

        is Class<*> -> listOf(type.name)
        else -> emptyList()
    }

    // --- fixtúra ----------------------------------------------------------------

    private suspend fun seedCategories() {
        database.categoryDao().upsertAll(
            listOf(
                CategoryEntity(id = dachi, nameHu = "Állások", sortOrder = 0),
                CategoryEntity(id = tsuki, nameHu = "Ütések", sortOrder = 1),
            ),
        )
    }

    private fun aliasOf(techniqueId: String, alias: String) = TechniqueAliasEntity(
        techniqueId = TechniqueId.of(techniqueId),
        alias = alias,
        aliasNormalized = NameNormalizer.normalize(alias),
    )

    private fun technique(
        id: String,
        categoryId: CategoryId = tsuki,
        romaji: String,
        hungarian: String = "magyar jelentés",
        retired: Boolean = false,
        illustrationKind: IllustrationKind? = null,
    ): TechniqueEntity = TechniqueEntity(
        id = TechniqueId.of(id),
        categoryId = categoryId,
        romaji = romaji,
        romajiAscii = romaji,
        hungarian = hungarian,
        // A két keresőoszlopot az éles útvonalon a `ContentEntityMappers.toEntity`
        // tölti ki ugyanezzel a normalizálóval (FR-03, ADR-012) – a fixtúra ezt
        // utánozza, nem egy saját, eltérő szabállyal.
        searchRomaji = NameNormalizer.normalize(romaji),
        searchHungarian = NameNormalizer.normalize(hungarian),
        status = VerificationStatus.AWAITING_REVIEW,
        quizExcluded = false,
        retired = retired,
        datasetVersion = "2026.11.01",
        illustrationKind = illustrationKind,
        illustrationRef = illustrationKind?.let { "images/$id.webp" },
        illustrationFileName = illustrationKind?.let { "$id.webp" },
        illustrationQuizUsable = illustrationKind?.let { false },
        illustrationHideableOnStudyCard = illustrationKind?.let { false },
    )
}
