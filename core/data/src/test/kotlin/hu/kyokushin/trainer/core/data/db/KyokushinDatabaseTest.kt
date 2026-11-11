package hu.kyokushin.trainer.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Az adatbázis tényleges viselkedése in-memory Room példányon (FR-18, DR-07).
 *
 * A [RoomSchemaContractTest] a séma *alakját* méri az exportált JSON-ból; ez a teszt azt
 * méri, mit tesz az adatbázis futás közben. A kettő együtt kell: egy helyes alakú index is
 * elromolhat, ha a beszúrás konfliktusstratégiája rossz (kivételt dobna a `false` helyett),
 * és a cascade hiánya csak tényleges törléssel bizonyítható.
 *
 * Robolectric kell hozzá, mert a Room `SupportSQLite` implementációja Android API – de
 * emulátor nem: a JVM-en futó SQLite ugyanazokat a megszorításokat kényszeríti ki.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KyokushinDatabaseTest {

    private lateinit var database: KyokushinDatabase

    private val sessionId = SessionId.of("session-1")
    private val techniqueId = TechniqueId.of("KY-0001")
    private val categoryId = CategoryId.of("tsuki")
    private val answeredAt = Instant.ofEpochMilli(1_762_000_000_000L)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, KyokushinDatabase::class.java)
            // Nincs `fallbackToDestructiveMigration()` – sem itt, sem éles kódban (DR-08).
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- FR-18 / ADR-005: egy kérdésre pontosan egy válasz ------------------------

    @Test
    fun recordAttempt_secondAnswerToTheSameQuestionIsIgnoredNotDuplicated_FR18() = runTest {
        insertSession()

        val first = database.quizAttemptDao().insertIgnoringDuplicate(attempt(correct = true))
        val second = database.quizAttemptDao().insertIgnoringDuplicate(attempt(correct = false))

        // Az első beszúrás valódi sorazonosítót ad, a második `-1`-et: ebből lesz a
        // `ProgressRepository.recordAttempt` `false` visszatérése (ADR-005).
        assertThat(first).isGreaterThan(0L)
        assertThat(second).isEqualTo(-1L)
        assertThat(database.quizAttemptDao().count()).isEqualTo(1)

        // És az ELSŐ válasz maradt meg: a későbbi nem írja felül a korábbi ítéletet.
        val stored = database.quizAttemptDao().getBySession(sessionId).single()
        assertThat(stored.correct).isTrue()
    }

    @Test
    fun recordAttempt_differentQuestionsInTheSameSessionAreBothRecorded_FR18() = runTest {
        insertSession()

        database.quizAttemptDao().insertIgnoringDuplicate(attempt(questionId = "q-1"))
        database.quizAttemptDao().insertIgnoringDuplicate(attempt(questionId = "q-2"))

        assertThat(database.quizAttemptDao().count()).isEqualTo(2)
    }

    @Test
    fun recordAttempt_sameTechniqueTwiceInOneSessionIsAllowed_ADR005() = runTest {
        insertSession()

        // Ugyanaz a technika két KÜLÖN kérdésben: az index a kérdésre épül, nem a
        // technikára, ezért ez nem ütközés.
        database.quizAttemptDao().insertIgnoringDuplicate(
            attempt(questionId = "q-1", type = QuestionType.JP_TO_HU),
        )
        database.quizAttemptDao().insertIgnoringDuplicate(
            attempt(questionId = "q-2", type = QuestionType.HU_TO_JP),
        )

        assertThat(database.quizAttemptDao().getByTechnique(techniqueId)).hasSize(2)
    }

    @Test
    fun recordAttempt_theSameQuestionInANewSessionIsRecordedAgain_FR21() = runTest {
        insertSession()
        val retrySessionId = SessionId.of("session-2")
        insertSession(retrySessionId)

        database.quizAttemptDao().insertIgnoringDuplicate(attempt())
        val retry = database.quizAttemptDao()
            .insertIgnoringDuplicate(attempt(sessionId = retrySessionId))

        // A „hibák újragyakorlása" új sessionId-t kap (FR-21), az index nem akadályozza.
        assertThat(retry).isGreaterThan(0L)
        assertThat(database.quizAttemptDao().count()).isEqualTo(2)
    }

    // --- DR-07 / ADR-004: a tartalom frissítése nem viszi el az előrehaladást ------

    @Test
    fun userData_survivesRetiringAndDeletingTheTechnique_TR05() = runTest {
        insertSession()
        insertContent()
        database.quizAttemptDao().insertIgnoringDuplicate(attempt())
        database.studyReviewDao().insert(
            StudyReviewEntity(
                techniqueId = techniqueId,
                selfRating = SelfRating.NEEDS_PRACTICE,
                reviewedAt = answeredAt,
            ),
        )

        // 1. Visszavonás jelöléssel (ADR-004 rendes útja).
        assertThat(database.techniqueDao().markRetired(listOf(techniqueId))).isEqualTo(1)
        assertThat(database.quizAttemptDao().count()).isEqualTo(1)
        assertThat(database.studyReviewDao().count()).isEqualTo(1)

        // 2. És még a technikasor FIZIKAI törlése sem viszi el a felhasználói adatot:
        //    nincs idegen kulcs a tartalomra, tehát nincs mit cascade-elni.
        database.openHelper.writableDatabase.execSQL("DELETE FROM technique")
        assertThat(database.techniqueDao().count()).isEqualTo(0)
        assertThat(database.quizAttemptDao().count()).isEqualTo(1)
        assertThat(database.studyReviewDao().count()).isEqualTo(1)

        // A hivatkozás feloldható marad: a TechniqueId örök (DR-06).
        assertThat(database.quizAttemptDao().getBySession(sessionId).single().techniqueId)
            .isEqualTo(techniqueId)
    }

    @Test
    fun contentUpsert_isIdempotentAndKeepsUserRows_FR32() = runTest {
        insertSession()
        insertContent()
        database.quizAttemptDao().insertIgnoringDuplicate(attempt())

        // Ugyanaz a kiadás újra betöltve: upsert, nem törlés + beszúrás.
        insertContent(datasetVersion = "2026.11.2", hungarian = "felső ütés")

        assertThat(database.techniqueDao().count()).isEqualTo(1)
        assertThat(database.techniqueDao().getById(techniqueId)!!.hungarian).isEqualTo("felső ütés")
        assertThat(database.quizAttemptDao().count()).isEqualTo(1)
    }

    @Test
    fun deletingASession_cascadesToItsOwnAttemptsOnly_DR07() = runTest {
        insertSession()
        insertContent()
        database.quizAttemptDao().insertIgnoringDuplicate(attempt())
        database.studyReviewDao().insert(
            StudyReviewEntity(
                techniqueId = techniqueId,
                selfRating = SelfRating.KNEW,
                reviewedAt = answeredAt,
            ),
        )

        database.openHelper.writableDatabase.execSQL("DELETE FROM quiz_session")

        // A menet törlése a saját válaszait viszi (felhasználói → felhasználói cascade),
        // az önértékeléseket és a tartalmat nem.
        assertThat(database.quizAttemptDao().count()).isEqualTo(0)
        assertThat(database.studyReviewDao().count()).isEqualTo(1)
        assertThat(database.techniqueDao().count()).isEqualTo(1)
    }

    // --- Tárolt alak: enum = név, időbélyeg = epoch milli -------------------------

    @Test
    fun storedColumns_holdEnumNamesAndEpochMillis_DR08() = runTest {
        insertSession()
        insertContent()
        database.quizAttemptDao().insertIgnoringDuplicate(attempt(type = QuestionType.IMAGE_TO_JP))

        database.openHelper.readableDatabase.query(
            "SELECT questionType, answeredAt, sessionId FROM quiz_attempt",
        ).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("IMAGE_TO_JP")
            assertThat(cursor.getLong(1)).isEqualTo(answeredAt.toEpochMilli())
            assertThat(cursor.getString(2)).isEqualTo(sessionId.value)
        }

        database.openHelper.readableDatabase.query("SELECT status FROM technique").use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(0)).isEqualTo("AWAITING_REVIEW")
        }
    }

    @Test
    fun unfinishedSession_keepsNullFinishedAtUntilClosed_FR19() = runTest {
        insertSession()

        assertThat(database.quizSessionDao().getById(sessionId)!!.finishedAt).isNull()

        val finishedAt = answeredAt.plusSeconds(60)
        assertThat(database.quizSessionDao().markFinished(sessionId, finishedAt)).isEqualTo(1)
        assertThat(database.quizSessionDao().getById(sessionId)!!.finishedAt).isEqualTo(finishedAt)

        // Egy már lezárt menet lezárási időpontja nem íródik át.
        assertThat(database.quizSessionDao().markFinished(sessionId, finishedAt.plusSeconds(1)))
            .isEqualTo(0)
    }

    @Test
    fun contentState_staysASingleRowAcrossImports_DR04() = runTest {
        database.contentStateDao().upsert(contentState(contentVersion = "2026.11.1"))
        database.contentStateDao().upsert(contentState(contentVersion = "2026.11.2"))

        val state = database.contentStateDao().get()
        assertThat(state!!.contentVersion).isEqualTo("2026.11.2")
        assertThat(state.id).isEqualTo(ContentStateEntity.SINGLETON_ID)
    }

    // --- fixture-ök ---------------------------------------------------------------

    private suspend fun insertSession(id: SessionId = sessionId) {
        database.quizSessionDao().insert(
            QuizSessionEntity(
                id = id,
                startedAt = answeredAt.minusSeconds(10),
                requestedCount = 10,
                seed = 42L,
            ),
        )
    }

    private suspend fun insertContent(
        datasetVersion: String = "2026.11.1",
        hungarian: String = "felütés",
    ) {
        database.categoryDao().upsertAll(
            listOf(CategoryEntity(id = categoryId, nameHu = "Ütések", sortOrder = 1)),
        )
        database.techniqueDao().upsertAll(
            listOf(
                TechniqueEntity(
                    id = techniqueId,
                    categoryId = categoryId,
                    romaji = "age tsuki",
                    romajiAscii = "age tsuki",
                    hungarian = hungarian,
                    status = VerificationStatus.AWAITING_REVIEW,
                    quizExcluded = false,
                    retired = false,
                    datasetVersion = datasetVersion,
                ),
            ),
        )
    }

    private fun attempt(
        sessionId: SessionId = this.sessionId,
        questionId: String = "q-1",
        type: QuestionType = QuestionType.JP_TO_HU,
        correct: Boolean = true,
    ) = QuizAttemptEntity(
        sessionId = sessionId,
        questionId = QuestionId.of(questionId),
        techniqueId = techniqueId,
        questionType = type,
        selectedTechniqueId = techniqueId,
        correct = correct,
        answeredAt = answeredAt,
    )

    private fun contentState(contentVersion: String) = ContentStateEntity(
        schemaVersion = "1.0.0",
        contentVersion = contentVersion,
        datasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
        importedAt = answeredAt,
        acceptedCount = 161,
        rejectedCount = 0,
        blockingIssueCount = 0,
        warningIssueCount = 0,
        datasetRejected = false,
    )
}
