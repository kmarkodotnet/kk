package hu.kyokushin.trainer.core.data.content

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.IllustrationKind
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.QuizAttemptEntity
import hu.kyokushin.trainer.core.data.db.QuizSessionEntity
import hu.kyokushin.trainer.core.data.db.StudyReviewEntity
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.quiz.validation.DefaultKnowledgeValidator
import hu.kyokushin.trainer.core.quiz.validation.IssueCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A tartalomimport tényleges viselkedése in-memory Room példányon (FR-29…FR-33, ADR-004).
 *
 * Ezek az **alaptesztek**: hogy a betöltés megtörténik, hogy a verziókapu zár, hogy a
 * frissítés upsert és visszavonás (nem törlés), és hogy az elutasított adathalmaz nem
 * nyúl a tartalomhoz. A teljes TR-05 garancia-tesztkészlet (átnevezés, statisztika-
 * változatlanság, több körös frissítés) a T-118 tárgya.
 *
 * Robolectric kell hozzá, mert a Room `SupportSQLite` implementációja Android API –
 * emulátor viszont nem. Az óra fix, a dispatcher teszt-dispatcher: a determinizmus
 * feltétele, hogy egyik se a rendszertől jöjjön (NFR-07).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ContentImporterTest {

    private lateinit var database: KyokushinDatabase

    private val clock = Clock.fixed(Instant.ofEpochMilli(1_762_000_000_000L), ZoneOffset.UTC)
    private val sessionId = SessionId.of("session-1")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KyokushinDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- FR-29: a csomagolt tartalom betöltése -----------------------------------

    @Test
    fun importIfNeeded_loadsPackagedContentIntoTheContentTables_FR29() = runTest {
        val result = importer(source(version = "1.0.0")).importIfNeeded()

        assertThat(result).isInstanceOf(ImportResult.Imported::class.java)
        val imported = result as ImportResult.Imported
        assertThat(imported.contentVersion).isEqualTo("1.0.0")
        assertThat(imported.acceptedCount).isEqualTo(2)
        assertThat(imported.rejectedCount).isEqualTo(0)
        assertThat(imported.retiredCount).isEqualTo(0)

        assertThat(database.categoryDao().getAll().map { it.id.value }).containsExactly("stances")
        assertThat(database.techniqueDao().getAll().map { it.id.value })
            .containsExactly("KY-0001", "KY-0002")

        val stored = database.techniqueDao().getById(TechniqueId.of("KY-0001"))!!
        assertThat(stored.romaji).isEqualTo("Zenkutsu dachi")
        assertThat(stored.romajiAscii).isEqualTo("Zenkutsu dachi")
        assertThat(stored.hungarian).isEqualTo("Elöl álló állás")
        assertThat(stored.datasetVersion).isEqualTo("1.0.0")
        assertThat(stored.retired).isFalse()
        // A hitelesítési szint a tartalomból jön, kódból nem emelkedik (CR-02).
        assertThat(stored.status).isEqualTo(VerificationStatus.AWAITING_REVIEW)
        // A csomagolt képfájl megvan: valódi `Asset` hivatkozás (ADR-003).
        assertThat(stored.illustrationKind).isEqualTo(IllustrationKind.ASSET)
        assertThat(stored.illustrationFileName).isEqualTo("KY-0044.jpg")

        // Az alias a normalizált kereső alakjával együtt kerül be (FR-03).
        val aliases = database.techniqueAliasDao().getForTechnique(TechniqueId.of("KY-0001"))
        assertThat(aliases.map { it.alias }).containsExactly("Zenkucu dachi")
        assertThat(aliases.single().aliasNormalized).isEqualTo("zenkucu dachi")
    }

    @Test
    fun importIfNeeded_fillsContentStateWithTheValidationSummary_FR32() = runTest {
        importer(source(version = "1.0.0")).importIfNeeded()

        val state = database.contentStateDao().get()!!
        assertThat(state.id).isEqualTo(1)
        assertThat(state.schemaVersion).isEqualTo("1.0.0")
        assertThat(state.contentVersion).isEqualTo("1.0.0")
        // `editorial_status = draft_review_required` → a szigorúbb olvasat (CR-02).
        assertThat(state.datasetTrust).isEqualTo(DatasetTrust.PRACTICE_ON_UNREVIEWED)
        assertThat(state.importedAt).isEqualTo(clock.instant())
        assertThat(state.acceptedCount).isEqualTo(2)
        assertThat(state.rejectedCount).isEqualTo(0)
        assertThat(state.datasetRejected).isFalse()
    }

    @Test
    fun lastValidationReport_exposesTheReportOfTheLastImport_FR32() = runTest {
        val importer = importer(source(version = "1.0.0"))

        assertThat(importer.lastValidationReport()).isNull()
        importer.importIfNeeded()

        val report = importer.lastValidationReport()!!
        assertThat(report.datasetVersion).isEqualTo("1.0.0")
        assertThat(report.accepted.map { it.id.value }).containsExactly("KY-0001", "KY-0002")
    }

    // --- FR-30: idempotencia -----------------------------------------------------

    @Test
    fun importIfNeeded_sameVersionTwice_skipsTheImportAndDoesNotDuplicate_FR30() = runTest {
        val source = source(version = "1.0.0")
        importer(source).importIfNeeded()

        // Új példány: a verziókapu a tárolt állapotból dönt, nem memóriából.
        val second = importer(source)
        val result = second.importIfNeeded()

        assertThat(result).isEqualTo(ImportResult.UpToDate("1.0.0"))
        assertThat(database.techniqueDao().count()).isEqualTo(2)
        assertThat(database.techniqueAliasDao().count()).isEqualTo(1)
        assertThat(database.categoryDao().count()).isEqualTo(1)
        // Validáció sem futott, ezért nincs jelentés ebben a processzben.
        assertThat(second.lastValidationReport()).isNull()
    }

    // --- FR-33 / ADR-004: a frissítés nem veszít felhasználói adatot --------------

    @Test
    fun importIfNeeded_newVersionRetiresMissingTechniqueAndKeepsUserRows_FR33() = runTest {
        importer(source(version = "1.0.0")).importIfNeeded()
        insertUserProgress(TechniqueId.of("KY-0002"))
        val attemptsBefore = database.quizAttemptDao().count()
        val reviewsBefore = database.studyReviewDao().count()

        // A 2.0.0 csomag már csak az első technikát tartalmazza, átnevezve.
        val result = importer(
            source(
                version = "2.0.0",
                entries = listOf(entry(id = "KY-0001", romaji = "Zenkutsu dachi (javított)")),
            ),
        ).importIfNeeded()

        assertThat((result as ImportResult.Imported).retiredCount).isEqualTo(1)

        // 1. A kimaradt technika sora megvan, csak visszavonva – nincs fizikai törlés.
        assertThat(database.techniqueDao().count()).isEqualTo(2)
        assertThat(database.techniqueDao().getById(TechniqueId.of("KY-0002"))!!.retired).isTrue()
        assertThat(database.techniqueDao().observeActive().first().map { it.id.value })
            .containsExactly("KY-0001")

        // 2. A felhasználói sorok száma változatlan, és a hivatkozásuk feloldható (TR-05).
        assertThat(database.quizAttemptDao().count()).isEqualTo(attemptsBefore)
        assertThat(database.studyReviewDao().count()).isEqualTo(reviewsBefore)
        val attempt = database.quizAttemptDao().getByTechnique(TechniqueId.of("KY-0002")).single()
        assertThat(database.techniqueDao().getById(attempt.techniqueId)).isNotNull()

        // 3. Az átnevezett technika `id`-ja ugyanaz, a neve új (DR-06).
        val renamed = database.techniqueDao().getById(TechniqueId.of("KY-0001"))!!
        assertThat(renamed.romaji).isEqualTo("Zenkutsu dachi (javított)")
        assertThat(renamed.datasetVersion).isEqualTo("2.0.0")
        assertThat(database.contentStateDao().get()!!.contentVersion).isEqualTo("2.0.0")
    }

    @Test
    fun importIfNeeded_removedAliasDoesNotSurviveTheUpdate_FR29() = runTest {
        importer(source(version = "1.0.0")).importIfNeeded()
        assertThat(database.techniqueAliasDao().count()).isEqualTo(1)

        importer(
            source(
                version = "2.0.0",
                entries = listOf(entry(id = "KY-0001", aliases = emptyList())),
            ),
        ).importIfNeeded()

        // Az alias tartalom → tartalom adat: a frissítés cseréli, nem gyűjti.
        assertThat(database.techniqueAliasDao().getForTechnique(TechniqueId.of("KY-0001"))).isEmpty()
    }

    @Test
    fun importIfNeeded_retiredTechniqueReturningInANewVersionBecomesActiveAgain_ADR004() = runTest {
        importer(source(version = "1.0.0")).importIfNeeded()
        importer(
            source(version = "2.0.0", entries = listOf(entry(id = "KY-0001"))),
        ).importIfNeeded()
        assertThat(database.techniqueDao().getById(TechniqueId.of("KY-0002"))!!.retired).isTrue()

        importer(source(version = "3.0.0")).importIfNeeded()

        // Az upsert a teljes sort írja, ezért a visszatérő technika jelölése feloldódik.
        assertThat(database.techniqueDao().getById(TechniqueId.of("KY-0002"))!!.retired).isFalse()
    }

    // --- FR-31: az érvénytelen tartalom nem omlasztja össze és nem töröl ----------

    @Test
    fun importIfNeeded_unreadableContentIsRejectedAndKeepsTheLoadedContent_FR31() = runTest {
        importer(source(version = "1.0.0")).importIfNeeded()

        val broken = FakeKnowledgeAssetSource(
            files = mapOf(
                KnowledgeAssetLayout.MANIFEST_FILE to manifest(version = "2.0.0"),
                KnowledgeAssetLayout.ENTRIES_FILE to """{ "entries": [ { "id": "KY-0001" """,
            ),
        )
        val importer = importer(broken)
        val result = importer.importIfNeeded()

        val report = (result as ImportResult.Rejected).report
        assertThat(report.issues.single().code).isEqualTo(IssueCode.V010)
        assertThat(importer.lastValidationReport()).isEqualTo(report)

        // A tartalom és a kijelzett verzió az előző, sikeres kiadásé marad.
        assertThat(database.techniqueDao().count()).isEqualTo(2)
        assertThat(database.contentStateDao().get()!!.contentVersion).isEqualTo("1.0.0")
        assertThat(database.contentStateDao().get()!!.datasetRejected).isFalse()
    }

    @Test
    fun importIfNeeded_unsupportedSchemaVersionIsRejectedWithoutWritingContent_FR31() = runTest {
        val result = importer(source(version = "1.0.0", schemaVersion = "9.0.0")).importIfNeeded()

        val report = (result as ImportResult.Rejected).report
        assertThat(report.issues.single().code).isEqualTo(IssueCode.V001)
        assertThat(database.techniqueDao().count()).isEqualTo(0)
        assertThat(database.categoryDao().count()).isEqualTo(0)

        // Sikeres betöltés még nem volt, ezért az elutasítás nyoma a `content_state`-be kerül:
        // ebből tudja a diagnosztika, hogy az app futott, de nincs használható tartalma.
        val state = database.contentStateDao().get()!!
        assertThat(state.datasetRejected).isTrue()
        assertThat(state.schemaVersion).isEqualTo("9.0.0")
        assertThat(state.acceptedCount).isEqualTo(0)
        assertThat(state.blockingIssueCount).isEqualTo(1)
    }

    @Test
    fun importIfNeeded_rejectedRecordIsNotImportedButDoesNotStopTheRest_FR31() = runTest {
        val result = importer(
            source(
                version = "1.0.0",
                entries = listOf(
                    entry(id = "KY-0001"),
                    // Hiányzó `hungarian_name` → V002, a rekord elvetve.
                    entry(id = "KY-0003", hungarian = null),
                ),
            ),
        ).importIfNeeded()

        val imported = result as ImportResult.Imported
        assertThat(imported.acceptedCount).isEqualTo(1)
        assertThat(imported.rejectedCount).isEqualTo(1)
        assertThat(database.techniqueDao().getAll().map { it.id.value }).containsExactly("KY-0001")
    }

    // --- Segédek ------------------------------------------------------------------

    private fun importer(source: KnowledgeAssetSource): ContentImporter = RoomContentImporter(
        database = database,
        reader = KnowledgeContentReader(source),
        validator = DefaultKnowledgeValidator(),
        clock = clock,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private suspend fun insertUserProgress(techniqueId: TechniqueId) {
        database.quizSessionDao().insert(
            QuizSessionEntity(
                id = sessionId,
                startedAt = clock.instant(),
                requestedCount = 1,
                seed = 42L,
            ),
        )
        database.quizAttemptDao().insertIgnoringDuplicate(
            QuizAttemptEntity(
                sessionId = sessionId,
                questionId = QuestionId.of("q-1"),
                techniqueId = techniqueId,
                questionType = QuestionType.JP_TO_HU,
                selectedTechniqueId = techniqueId,
                correct = true,
                answeredAt = clock.instant(),
            ),
        )
        database.studyReviewDao().insert(
            StudyReviewEntity(
                techniqueId = techniqueId,
                selfRating = SelfRating.KNEW,
                reviewedAt = clock.instant(),
            ),
        )
    }

    private fun source(
        version: String,
        schemaVersion: String = "1.0.0",
        entries: List<String> = listOf(
            entry(id = "KY-0001"),
            entry(
                id = "KY-0002",
                romaji = "Kokutsu dachi",
                hungarian = "Hátul álló állás",
                aliases = emptyList(),
            ),
        ),
    ) = FakeKnowledgeAssetSource(
        files = mapOf(
            KnowledgeAssetLayout.MANIFEST_FILE to manifest(version, schemaVersion),
            KnowledgeAssetLayout.ENTRIES_FILE to """
                { "schema_version": "$schemaVersion", "dataset_version": "$version",
                  "entries": [ ${entries.joinToString(",")} ] }
            """.trimIndent(),
        ),
        illustrationFiles = setOf("KY-0044.jpg"),
    )

    private fun manifest(version: String, schemaVersion: String = "1.0.0") = """
        {
          "schema_version": "$schemaVersion",
          "dataset_version": "$version",
          "editorial_status": "draft_review_required",
          "categories": [ { "id": "stances", "name_hu": "Állások", "sort_order": 3 } ],
          "illustrations": [
            {
              "ref": "KY-0044",
              "file": "illustrations/KY-0044.jpg",
              "license": "project-internal",
              "quiz_usable": true,
              "hideable_on_study_card": true
            }
          ]
        }
    """.trimIndent()

    private fun entry(
        id: String,
        romaji: String? = "Zenkutsu dachi",
        hungarian: String? = "Elöl álló állás",
        aliases: List<String> = listOf("Zenkucu dachi"),
    ) = """
        {
          "id": "$id",
          "category_id": "stances",
          "japanese_romaji": ${romaji.jsonOrNull()},
          "hungarian_name": ${hungarian.jsonOrNull()},
          "aliases": [ ${aliases.joinToString(",") { "\"$it\"" }} ],
          "illustration_ref": "KY-0044",
          "review_status": "editorial_review_required",
          "quiz_excluded": false,
          "retired": false
        }
    """.trimIndent()

    private fun String?.jsonOrNull(): String = if (this == null) "null" else "\"$this\""
}
