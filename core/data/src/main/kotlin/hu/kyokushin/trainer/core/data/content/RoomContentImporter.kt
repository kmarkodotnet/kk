package hu.kyokushin.trainer.core.data.content

import androidx.room.withTransaction
import hu.kyokushin.trainer.core.data.db.ContentStateEntity
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.toAliasEntities
import hu.kyokushin.trainer.core.data.db.toCategoryEntities
import hu.kyokushin.trainer.core.data.db.toEntity
import hu.kyokushin.trainer.core.data.db.toSynonymGroupEntities
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeInput
import hu.kyokushin.trainer.core.quiz.validation.KnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.Severity
import hu.kyokushin.trainer.core.quiz.validation.ValidationReport
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * A [ContentImporter] egyetlen megvalósítása: csomagolt JSON → validátor → **upsert**
 * (FR-29, FR-31, FR-33, ADR-004, ARCHITECTURE 5.1).
 *
 * ## A futás menete
 *
 * 1. **Beolvasás** ([KnowledgeContentReader]) – két ág: értelmezhető bemenet, vagy kész
 *    `V010` jelentés. Kivételkezelés itt nincs: a beolvasó jelentést ad, nem dob.
 * 2. **Verziókapu** – ha a csomagolt `dataset_version` megegyezik a tárolttal, és az előző
 *    betöltés nem volt elutasítva, a függvény [ImportResult.UpToDate]-tel visszatér.
 *    Validáció és adatbázisírás nem történik: ez az idempotencia elsődleges mechanizmusa
 *    (FR-30), a másodlagos maga az upsert.
 * 3. **Validáció** ([KnowledgeValidator]) – a döntés egyetlen kapun megy:
 *    [ValidationReport.datasetRejected]. Ha igaz, a tartalomtáblákhoz **nem nyúlunk**.
 * 4. **Upsert egyetlen tranzakcióban** – kategóriák, technikák, aliasok,
 *    szinonimacsoportok, visszavonás, végül a `content_state`. Egy tranzakció, mert
 *    különben előfordulhatna, hogy a technikák már az új kiadásból vannak, a kijelzett
 *    `contentVersion` viszont a régi.
 *
 * ## Amit az import soha nem tesz
 *
 * - **Nem töröl technikát.** Ami kimaradt az aktuális csomagból, az `retired = true`
 *   jelölést kap (ADR-004 2. pont), hogy a rá hivatkozó `quiz_attempt` és `study_review`
 *   sorok feloldhatók maradjanak (FR-33, TR-05).
 * - **Nem nyúl a felhasználói táblákhoz.** A `quiz_session`, `quiz_attempt`,
 *   `study_review` táblákra ebben az osztályban egyetlen hivatkozás sincs – ez a FR-33
 *   garancia legegyszerűbb bizonyítéka.
 * - **Nem hív közvetlen órát és RNG-t** (NFR-07): az `importedAt` az injektált [Clock]-ból
 *   jön.
 * - **Nem emel hitelesítési szintet** (CR-02): mindent a validátor jelentéséből vesz át.
 *
 * @property database az adatbázis – a DAO-kon túl a tranzakcióhatár miatt is kell.
 * @property reader a csomagolt tudástár beolvasója.
 * @property validator a tartalmi szabályok érvényesítője (`core/quiz`).
 * @property clock az `importedAt` időbélyeg forrása.
 * @property ioDispatcher a háttér-I/O dispatcher; a kötést a `di` modul adja (T-203).
 */
internal class RoomContentImporter @Inject constructor(
    private val database: KyokushinDatabase,
    private val reader: KnowledgeContentReader,
    private val validator: KnowledgeValidator,
    private val clock: Clock,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ContentImporter {

    /**
     * Egyszerre egy betöltés futhat.
     *
     * Nem elméleti aggály: az app indulása és a fejlesztői diagnosztika is hívhatja, és két
     * párhuzamos futás a verziókapu két oldalán kötne ki (mindkettő „eltér" olvasatot kap),
     * majd ugyanazokat a sorokat írná. Az upsert ettől sem duplikálna, de a `retired`
     * jelölés és a `content_state` sorrendje versenyezne.
     */
    private val mutex = Mutex()

    /** Az utolsó validáció jelentése ebben a processzben; perzisztálva nincs (FR-32). */
    private var lastReport: ValidationReport? = null

    override suspend fun lastValidationReport(): ValidationReport? = mutex.withLock { lastReport }

    override suspend fun importIfNeeded(): ImportResult = withContext(ioDispatcher) {
        mutex.withLock {
            val storedState = database.contentStateDao().get()
            when (val read = reader.read()) {
                // A `V010` jelentés már kész: a betöltés elutasítva, a tartalom marad.
                is KnowledgeContentReadResult.Unreadable -> reject(read.report, storedState)

                is KnowledgeContentReadResult.Parsed -> {
                    val packagedVersion = read.input.manifest.contentVersion
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    if (packagedVersion != null && storedState.isUpToDateWith(packagedVersion)) {
                        // Se validáció, se írás: a verziókapu zárva (FR-30).
                        ImportResult.UpToDate(packagedVersion)
                    } else {
                        val report = validator.validate(read.input)
                        if (report.datasetRejected) {
                            reject(report, storedState)
                        } else {
                            apply(read.input, report)
                        }
                    }
                }
            }
        }
    }

    /**
     * A tárolt állapot akkor zárja a kaput, ha **ugyanabból** a kiadásból származik, és az
     * a betöltés sikeres volt.
     *
     * Az elutasított állapot szándékosan nem zár: egy javított csomag ugyanazzal a
     * `dataset_version` értékkel is érkezhet (fejlesztés közben ez a tipikus eset), és egy
     * korábbi elutasítás nem zárhatja ki örökre a betöltést.
     */
    private fun ContentStateEntity?.isUpToDateWith(packagedVersion: String): Boolean =
        this != null && !datasetRejected && contentVersion == packagedVersion

    /**
     * Elutasított adathalmaz: a tartalomtáblák érintetlenek (FR-31).
     *
     * A `content_state` sort **csak akkor** írjuk, ha még egy sikeres betöltés sem volt:
     * ilyenkor a sor az egyetlen nyoma annak, hogy az app futott, de nincs tartalma. Ha
     * viszont van korábbi, sikeres állapot, azt nem írjuk át – a tartalomtáblákban *annak*
     * a kiadásnak a sorai vannak, és a kijelzett verziónak ezt kell tükröznie.
     */
    private suspend fun reject(report: ValidationReport, storedState: ContentStateEntity?): ImportResult {
        lastReport = report
        if (storedState == null) {
            database.contentStateDao().upsert(report.toContentState())
        }
        return ImportResult.Rejected(report)
    }

    /**
     * A tartalom felvitele egyetlen tranzakcióban.
     *
     * A műveletek sorrendje kötött:
     *
     * 1. **kategóriák** – a `technique.categoryId` idegen kulcsa megkívánja a kategóriasort
     *    (`NO_ACTION`, nem cascade), ezért a technikák előtt kell lennie.
     * 2. **technikák** upsert – a már létező `id` sorai frissülnek, nem duplázódnak, és egy
     *    korábban visszavont, most visszatérő technika `retired` jelölése is feloldódik,
     *    mert a teljes sort írjuk.
     * 3. **aliasok** – technikánként csere (törlés + upsert): az aliaslista szerkeszthető,
     *    tehát egy eltávolított alias nem élheti túl a frissítést. Tartalom → tartalom
     *    törlés, felhasználói adatot nem érint.
     * 4. **visszavonás** – amit a csomag nem tartalmaz elfogadott rekordként, az
     *    `retired = true` (ADR-004). Az *elvetett* rekord is ide tartozik: érvénytelen
     *    tartalmat nem mutatunk meg, de a sor és a rá hivatkozó előrehaladás megmarad.
     * 5. **szinonimacsoportok** – teljes csere, mert a `groupId` nem stabil azonosító.
     * 6. **`content_state`** – a verzió és a validációs összegzés ugyanabban a
     *    tranzakcióban, mint a tartalom.
     */
    private suspend fun apply(
        input: KnowledgeInput,
        report: ValidationReport,
    ): ImportResult {
        lastReport = report
        val acceptedIds = report.accepted.map { it.id }.toSet()
        var retiredCount = 0

        database.withTransaction {
            database.categoryDao().upsertAll(input.manifest.categories.toCategoryEntities())
            database.techniqueDao().upsertAll(report.accepted.map { it.toEntity() })

            report.accepted.forEach { technique ->
                database.techniqueAliasDao().deleteForTechnique(technique.id)
                val aliases = technique.toAliasEntities()
                if (aliases.isNotEmpty()) {
                    database.techniqueAliasDao().upsertAll(aliases)
                }
            }

            retiredCount = retireMissing(acceptedIds)

            database.synonymGroupDao().clear()
            val synonymMembers = report.synonymGroups.toSynonymGroupEntities()
            if (synonymMembers.isNotEmpty()) {
                database.synonymGroupDao().upsertAll(synonymMembers)
            }

            database.contentStateDao().upsert(report.toContentState())
        }

        return ImportResult.Imported(
            contentVersion = report.datasetVersion,
            acceptedCount = report.accepted.size,
            rejectedCount = report.rejected.size,
            retiredCount = retiredCount,
        )
    }

    /**
     * A csomagban elfogadott rekordként nem szereplő technikák visszavonása.
     *
     * Darabolva, mert az `IN (:ids)` SQLite-kötésváltozói végesek (`SQLITE_MAX_VARIABLE_NUMBER`),
     * a visszavont sorok pedig idővel gyűlnek – egy több száz rekordos tudástárnál a
     * daraboltság nélkül a lekérdezés futásidőben hasalna el.
     */
    private suspend fun retireMissing(acceptedIds: Set<TechniqueId>): Int {
        val missing = database.techniqueDao().getAllIds().filterNot { it in acceptedIds }
        if (missing.isEmpty()) return 0
        return missing.chunked(RETIRE_CHUNK_SIZE).sumOf { chunk ->
            database.techniqueDao().markRetired(chunk)
        }
    }

    /**
     * A jelentés **összegzése** a `content_state` egysoros táblába.
     *
     * A részletes észrevételek nem perzisztálódnak (lásd [ContentStateEntity]); innen a
     * fejlesztői diagnosztika a számokat olvassa, a tételes listát a
     * [ContentImporter.lastValidationReport] adja.
     */
    private fun ValidationReport.toContentState(): ContentStateEntity = ContentStateEntity(
        schemaVersion = schemaVersion,
        contentVersion = datasetVersion,
        // A bizalmi szint a validátor olvasata a manifest szerkesztői állapotáról; kódból
        // nem emeljük, és nem a tartalom `dataset_trust` önbevallásából vesszük (CR-02).
        datasetTrust = datasetTrust,
        importedAt = clock.instant(),
        acceptedCount = accepted.size,
        rejectedCount = rejected.size,
        blockingIssueCount = issues.count { it.severity == Severity.BLOCKING },
        warningIssueCount = issues.count { it.severity == Severity.WARNING },
        datasetRejected = datasetRejected,
    )

    private companion object {
        /** Ennyi azonosító mehet egy `UPDATE … WHERE id IN (…)` hívásba. */
        const val RETIRE_CHUNK_SIZE: Int = 500
    }
}
