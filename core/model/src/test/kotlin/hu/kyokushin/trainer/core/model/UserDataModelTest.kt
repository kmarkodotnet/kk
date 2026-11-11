package hu.kyokushin.trainer.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

/**
 * A felhasználói adat domain modelljének ([QuizAttempt], [StudyReview], [QuizSession])
 * szerződése.
 *
 * Lefedett követelmények: DR-04 (a felhasználói rekordok mezői), DR-07 (a tartalomtól
 * külön életciklus – a rekordok azonosítóra hivatkoznak, nem beágyazott technikára),
 * FR-18 / ADR-005 (a `questionId` jelenléte, ami nélkül az `UNIQUE(sessionId, questionId)`
 * index nem építhető meg), FR-19 (a megszakított menet `finishedAt = null`),
 * FR-20 (csak a megválaszolt kérdések számítanak), ADR-007 (a seed a menetben tárolódik),
 * ADR-009 (`Instant` desugaring nélkül).
 */
class UserDataModelTest {

    private val startedAt: Instant = Instant.parse("2026-11-11T09:00:00Z")
    private val answeredAt: Instant = Instant.parse("2026-11-11T09:00:12Z")

    private fun attempt(
        selected: TechniqueId = TechniqueId.of("technique-uke-0001"),
        correct: Boolean = true,
    ) = QuizAttempt(
        id = 0L,
        sessionId = SessionId.of("session-0001"),
        questionId = QuestionId.of("q-03"),
        techniqueId = TechniqueId.of("technique-uke-0001"),
        questionType = QuestionType.JP_TO_HU,
        selectedTechniqueId = selected,
        correct = correct,
        answeredAt = answeredAt,
    )

    private fun session(
        finishedAt: Instant? = null,
        requestedCount: Int = 10,
        answeredCount: Int = 0,
    ) = QuizSession(
        id = SessionId.of("session-0001"),
        startedAt = startedAt,
        finishedAt = finishedAt,
        requestedCount = requestedCount,
        answeredCount = answeredCount,
        seed = 4_815_162_342L,
    )

    @Test
    fun quizAttempt_carriesTheRequiredFields_DR04() {
        val a = attempt()

        assertThat(a.id).isEqualTo(0L)
        assertThat(a.sessionId.value).isEqualTo("session-0001")
        assertThat(a.techniqueId.value).isEqualTo("technique-uke-0001")
        assertThat(a.questionType).isEqualTo(QuestionType.JP_TO_HU)
        assertThat(a.selectedTechniqueId.value).isEqualTo("technique-uke-0001")
        assertThat(a.correct).isTrue()
        // ADR-009: Instant közvetlenül használható, nincs saját időbélyeg-típus.
        assertThat(a.answeredAt).isEqualTo(answeredAt)
    }

    /**
     * ADR-005: a `quiz_attempt` `UNIQUE(sessionId, questionId)` indexe csak akkor
     * építhető meg, ha a kérdésazonosító a rekord része. A `(sessionId, techniqueId)`
     * pár nem helyes kulcs, mert ugyanaz a technika több kérdésben is szerepelhet.
     */
    @Test
    fun quizAttempt_carriesQuestionIdSoTheUniqueIndexCanBeBuilt_FR18() {
        val a = attempt()

        assertThat(a.questionId.value).isEqualTo("q-03")

        // Ugyanaz a technika, ugyanaz a menet, de MÁS kérdés → két jogos, külön rekord:
        // a kulcs a (sessionId, questionId) pár, nem a (sessionId, techniqueId).
        val other = a.copy(questionId = QuestionId.of("q-07"))
        assertThat(other).isNotEqualTo(a)
        assertThat(other.techniqueId).isEqualTo(a.techniqueId)
        assertThat(other.sessionId).isEqualTo(a.sessionId)

        // Ugyanarra a kérdésre érkező második válasz azonos kulccsal jönne létre –
        // ezt a DB index zárja ki, a modell szintjén a két rekord megkülönböztethetetlen.
        val duplicate = a.copy(answeredAt = answeredAt.plusSeconds(1))
        assertThat(duplicate.sessionId to duplicate.questionId)
            .isEqualTo(a.sessionId to a.questionId)
    }

    /**
     * DR-03: a helyesség azonosító-összehasonlításból adódik, nem névegyezésből, és a
     * kvízmotor ítéletét a rekord megőrzi (DR-09) – a statisztika nem számol újra.
     */
    @Test
    fun quizAttempt_storesTheEngineVerdictNextToBothIds_DR04() {
        val wrong = attempt(selected = TechniqueId.of("technique-uke-0042"), correct = false)

        assertThat(wrong.selectedTechniqueId).isNotEqualTo(wrong.techniqueId)
        assertThat(wrong.correct).isFalse()
    }

    @Test
    fun quizAttempt_rejectsNegativeRowId_DR04() {
        assertThrows(IllegalArgumentException::class.java) {
            attempt().copy(id = -1L)
        }
    }

    @Test
    fun studyReview_carriesTheRequiredFields_DR04() {
        val r = StudyReview(
            id = 7L,
            techniqueId = TechniqueId.of("technique-geri-0003"),
            selfRating = SelfRating.NEEDS_PRACTICE,
            reviewedAt = startedAt,
        )

        assertThat(r.id).isEqualTo(7L)
        assertThat(r.techniqueId.value).isEqualTo("technique-geri-0003")
        assertThat(r.selfRating).isEqualTo(SelfRating.NEEDS_PRACTICE)
        assertThat(r.reviewedAt).isEqualTo(startedAt)
    }

    @Test
    fun studyReview_rejectsNegativeRowId_DR04() {
        assertThrows(IllegalArgumentException::class.java) {
            StudyReview(
                id = -5L,
                techniqueId = TechniqueId.of("technique-geri-0003"),
                selfRating = SelfRating.KNEW,
                reviewedAt = startedAt,
            )
        }
    }

    /**
     * DR-07: a felhasználói rekordok csak azonosítóval hivatkoznak a tartalomra, ezért
     * egy átnevezett vagy visszavont technika sem teszi őket érvénytelenné – a modell
     * nem ágyaz be [Technique] példányt, amit egy frissítés elavulttá tenne.
     */
    @Test
    fun userDataReferencesContentByIdOnly_DR07() {
        val attemptFields = QuizAttempt::class.java.declaredFields.map { it.type }
        val reviewFields = StudyReview::class.java.declaredFields.map { it.type }

        assertThat(attemptFields).doesNotContain(Technique::class.java)
        assertThat(attemptFields).doesNotContain(Category::class.java)
        assertThat(reviewFields).doesNotContain(Technique::class.java)
        assertThat(reviewFields).doesNotContain(Category::class.java)
    }

    /**
     * FR-19: a megszakított kvíz munkamenete befejezetlen marad – a `finishedAt`
     * nullable, és a lezáratlanság nem „nulla időbélyeg"-ként jelenik meg.
     */
    @Test
    fun quizSession_abandonedRunStaysUnfinished_FR19() {
        val abandoned = session(finishedAt = null, answeredCount = 4)

        assertThat(abandoned.finishedAt).isNull()
        assertThat(abandoned.isFinished).isFalse()
        // A megválaszolt kérdések beszámítanak, a többi nem minősül hibásnak (FR-20).
        assertThat(abandoned.answeredCount).isEqualTo(4)
        assertThat(abandoned.requestedCount).isEqualTo(10)

        val finished = abandoned.copy(finishedAt = startedAt.plusSeconds(90))
        assertThat(finished.isFinished).isTrue()
    }

    /** ADR-007: a seed a munkamenet része, hogy a kvíz utólag reprodukálható legyen. */
    @Test
    fun quizSession_storesTheSeedForReproducibility_FR19() {
        assertThat(session().seed).isEqualTo(4_815_162_342L)
    }

    @Test
    fun quizSession_rejectsImpossibleCounters_FR20() {
        // Több megválaszolt kérdés, mint amennyit kértünk – nem állítható elő.
        assertThrows(IllegalArgumentException::class.java) {
            session(requestedCount = 10, answeredCount = 11)
        }
        assertThrows(IllegalArgumentException::class.java) {
            session(answeredCount = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            session(requestedCount = 0)
        }
    }

    @Test
    fun quizSession_rejectsFinishBeforeStart_FR19() {
        assertThrows(IllegalArgumentException::class.java) {
            session(finishedAt = startedAt.minusSeconds(1))
        }
        // A nulla hosszú menet határeset, de érvényes.
        assertThat(session(finishedAt = startedAt).isFinished).isTrue()
    }
}
