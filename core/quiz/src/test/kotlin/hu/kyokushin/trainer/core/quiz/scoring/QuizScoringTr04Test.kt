package hu.kyokushin.trainer.core.quiz.scoring

import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.QuizAttempt
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import java.time.Instant
import org.junit.Test

/**
 * TR-04 – az eredményszámítás határesetei (FR-18, FR-20, FR-27, DR-09).
 *
 * A [QuizScoringTest] a `QuizScoring` szerződését fedi (T-113); ez a készlet a TR-04
 * mátrixát járja végig: 0 válasz → „nincs adat", 100%, 0%, félbehagyott kvíz, dupla
 * válasz (a lista szintjén is ellenőrizve), és vegyes kérdéstípusok egy munkamenetben.
 *
 * Két szabály húzódik végig a teszteken:
 * - a **meg nem válaszolt kérdés nem hibás válasz** – egyszerűen nem szerepel a
 *   nevezőben (FR-20, US-03/2);
 * - a `ratio` **arány** `0.0`–`1.0` között, nem százalék – a formázás a UI dolga.
 *
 * Az időbélyegek fix, injektált [Instant]-ból származnak: a teszt sem hívja az órát
 * (NFR-07).
 */
class QuizScoringTr04Test {

    private val session = SessionId.of("session-tr04")

    private val base: Instant = Instant.parse("2026-02-01T18:30:00Z")

    private fun attempt(
        index: Int,
        correct: Boolean,
        sessionId: SessionId = session,
        questionId: QuestionId = QuestionId.of("q-$index"),
        questionType: QuestionType = QuestionType.JP_TO_HU,
        answeredAt: Instant = base.plusSeconds(index.toLong()),
    ): QuizAttempt {
        val techniqueId = TechniqueId.of("tech-$index")
        return QuizAttempt(
            id = 0L,
            sessionId = sessionId,
            questionId = questionId,
            techniqueId = techniqueId,
            questionType = questionType,
            selectedTechniqueId = if (correct) techniqueId else TechniqueId.of("tech-wrong-$index"),
            correct = correct,
            answeredAt = answeredAt,
        )
    }

    /**
     * A dupla válasz ellenőrzésének **lista szintű** fele: a megválaszolt darabszám
     * sosem lehet több, mint a `(sessionId, questionId)` párok száma – vagyis a
     * nevezőben egy kérdés pontosan egyszer szerepelhet (FR-18, ADR-005).
     */
    private fun assertAnsweredMatchesDistinctQuestions(attempts: List<QuizAttempt>, score: Score) {
        val distinctQuestions = attempts.map { it.sessionId to it.questionId }.toSet()
        assertThat(score.answered).isEqualTo(distinctQuestions.size)
        assertThat(score.answered).isAtMost(attempts.size)
        assertThat(score.correct).isAtMost(score.answered)
    }

    // --- 0 megválaszolt kérdés → „nincs adat", nem 0% ------------------------------

    @Test
    fun score_zeroAnsweredQuestions_isMissingDataNotZeroPercent_TR04() {
        val noAnswers = QuizScoring.score(emptyList())

        assertThat(noAnswers.answered).isEqualTo(0)
        assertThat(noAnswers.correct).isEqualTo(0)
        // A „nincs adat" (null) és a „0%" (0.0) két külön állapot (FR-27).
        assertThat(noAnswers.ratio).isNull()

        val allWrong = QuizScoring.score(listOf(attempt(1, correct = false)))
        assertThat(allWrong.ratio).isEqualTo(0.0)
        // Ha a kettő egybeesne, a UI nem tudná szétválasztani őket.
        assertThat(noAnswers.ratio).isNotEqualTo(allWrong.ratio)
    }

    @Test
    fun score_quizStartedButNoQuestionAnsweredYet_isMissingData_TR04() {
        // 10 kérdéses kvíz, a felhasználó kilép az első válasz előtt: nincs próbálkozás,
        // tehát nincs arány – nem 0%, mert nem hibázott semmit (FR-19, FR-27).
        val attempts = emptyList<QuizAttempt>()

        val score = QuizScoring.score(attempts)

        assertThat(score).isEqualTo(Score(answered = 0, correct = 0, ratio = null))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    // --- 100% és 0% ----------------------------------------------------------------

    @Test
    fun score_allTenAnswersCorrect_isFullRatio_TR04() {
        val attempts = (1..10).map { attempt(it, correct = true) }

        val score = QuizScoring.score(attempts)

        assertThat(score).isEqualTo(Score(answered = 10, correct = 10, ratio = 1.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_allTenAnswersWrong_isZeroRatio_TR04() {
        val attempts = (1..10).map { attempt(it, correct = false) }

        val score = QuizScoring.score(attempts)

        assertThat(score).isEqualTo(Score(answered = 10, correct = 0, ratio = 0.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    // --- Félbehagyott kvíz ---------------------------------------------------------

    @Test
    fun score_abandonedQuizOfTen_countsOnlyTheFourAnswered_TR04() {
        // 10 kérdésből 4 megválaszolt, abból 3 helyes → 3/4 (US-03/2).
        val answered = listOf(
            attempt(1, correct = true),
            attempt(2, correct = true),
            attempt(3, correct = false),
            attempt(4, correct = true),
        )

        val score = QuizScoring.score(answered)

        assertThat(score).isEqualTo(Score(answered = 4, correct = 3, ratio = 0.75))
        assertAnsweredMatchesDistinctQuestions(answered, score)
    }

    @Test
    fun score_unansweredQuestionsAreNotCountedAsWrong_TR04() {
        // Ugyanaz a 4 válasz, és ugyanaz a kvíz végigvive úgy, hogy a maradék 6 hibás.
        // A félbehagyott menet nem eshet egybe a végig elhibázottal – ez a leggyakoribb
        // hiba az eredményszámításban (FR-20).
        val abandoned = listOf(
            attempt(1, correct = true),
            attempt(2, correct = true),
            attempt(3, correct = false),
            attempt(4, correct = true),
        )
        val finishedWithSixWrong = abandoned + (5..10).map { attempt(it, correct = false) }

        val abandonedScore = QuizScoring.score(abandoned)
        val finishedScore = QuizScoring.score(finishedWithSixWrong)

        assertThat(abandonedScore.answered).isEqualTo(4)
        assertThat(abandonedScore.ratio).isEqualTo(0.75)
        assertThat(finishedScore.answered).isEqualTo(10)
        assertThat(finishedScore.ratio).isEqualTo(0.3)
        // A meg nem válaszolt 6 kérdés nem rontja a félbehagyott menet arányát.
        assertThat(abandonedScore.ratio!!).isGreaterThan(finishedScore.ratio!!)
        assertThat(abandonedScore.correct).isEqualTo(finishedScore.correct)
    }

    @Test
    fun score_abandonedQuizWithOnlyCorrectAnswers_isStillFullRatio_TR04() {
        // 10-ből 3 megválaszolt, mind helyes: a félbehagyás nem von le – 100% a 3-ból.
        val attempts = (1..3).map { attempt(it, correct = true) }

        val score = QuizScoring.score(attempts)

        assertThat(score.answered).isEqualTo(3)
        assertThat(score.ratio).isEqualTo(1.0)
    }

    @Test
    fun score_ratioIsFractionNotPercentage_TR04() {
        // 4-ből 1 helyes → 0.25, nem 25.0: a százalékformázás a UI dolga (T-411).
        val score = QuizScoring.score(
            listOf(
                attempt(1, correct = true),
                attempt(2, correct = false),
                attempt(3, correct = false),
                attempt(4, correct = false),
            ),
        )

        assertThat(score.ratio).isEqualTo(0.25)
        assertThat(score.ratio!!).isAtMost(1.0)
    }

    // --- Dupla válasz --------------------------------------------------------------

    @Test
    fun score_doubleTapOnSameQuestion_doesNotDoubleTheResult_TR04() {
        val questionId = QuestionId.of("q-double")
        val first = attempt(1, correct = true, questionId = questionId, answeredAt = base)
        val secondTap = attempt(
            index = 2,
            correct = true,
            questionId = questionId,
            answeredAt = base.plusSeconds(1),
        )
        val attempts = listOf(first, secondTap)

        val score = QuizScoring.score(attempts)

        // Két rekord, egy kérdés: a számláló és a nevező is 1 (FR-18).
        assertThat(attempts).hasSize(2)
        assertThat(score).isEqualTo(Score(answered = 1, correct = 1, ratio = 1.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_laterCorrectionDoesNotOverrideFirstAnswer_TR04() {
        // A felhasználó először hibázik, majd „javít": az első válasz az érvényes,
        // az arány nem javul fel 100%-ra.
        val questionId = QuestionId.of("q-retry")
        val attempts = listOf(
            attempt(1, correct = false, questionId = questionId, answeredAt = base),
            attempt(2, correct = true, questionId = questionId, answeredAt = base.plusSeconds(5)),
        )

        val score = QuizScoring.score(attempts)

        assertThat(score).isEqualTo(Score(answered = 1, correct = 0, ratio = 0.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_duplicatesInArbitraryOrder_keepTheEarliestAnswer_TR04() {
        // A duplikátum a listában a párja előtt is állhat (pl. újrajátszott eseménysor):
        // az `answeredAt` dönt, nem a lista sorrendje.
        val questionId = QuestionId.of("q-replayed")
        val attempts = listOf(
            attempt(2, correct = true, questionId = questionId, answeredAt = base.plusSeconds(9)),
            attempt(1, correct = false, questionId = questionId, answeredAt = base),
            attempt(3, correct = true),
        )

        val score = QuizScoring.score(attempts)

        // A legkorábbi (hibás) válasz nyer, a harmadik kérdés külön számít.
        assertThat(score).isEqualTo(Score(answered = 2, correct = 1, ratio = 0.5))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_duplicatesWithIdenticalTimestamp_areDeterministic_TR04() {
        val questionId = QuestionId.of("q-tie")
        val attempts = listOf(
            attempt(1, correct = true, questionId = questionId, answeredAt = base),
            attempt(2, correct = false, questionId = questionId, answeredAt = base),
        )

        val score = QuizScoring.score(attempts)

        // Azonos időbélyegnél a lista elsőként felsorolt eleme dönt – de egy kérdés
        // akkor is csak egyszer számít.
        assertThat(score).isEqualTo(Score(answered = 1, correct = 1, ratio = 1.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_everyQuestionDuplicatedOnceAcrossWholeSession_TR04() {
        // Mind a 6 kérdésre két rekord érkezett: 12 próbálkozás, de 6 megválaszolt
        // kérdés. A lista szintű ellenőrzés itt a lényeg: a nevező a párok száma.
        val firstPass = (1..6).map { index ->
            attempt(index, correct = index % 2 == 0, answeredAt = base.plusSeconds(index.toLong()))
        }
        val duplicates = firstPass.map { original ->
            original.copy(correct = !original.correct, answeredAt = original.answeredAt.plusSeconds(30))
        }
        val attempts = firstPass + duplicates

        val score = QuizScoring.score(attempts)

        assertThat(attempts).hasSize(12)
        assertThat(score).isEqualTo(Score(answered = 6, correct = 3, ratio = 0.5))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    // --- Vegyes kérdéstípusok egy munkamenetben ------------------------------------

    @Test
    fun score_mixedQuestionTypesInOneSession_everyTypeCountsOnce_TR04() {
        val attempts = listOf(
            attempt(1, correct = true, questionType = QuestionType.JP_TO_HU),
            attempt(2, correct = false, questionType = QuestionType.HU_TO_JP),
            attempt(3, correct = true, questionType = QuestionType.IMAGE_TO_JP),
            attempt(4, correct = false, questionType = QuestionType.JP_TO_IMAGE),
            attempt(5, correct = true, questionType = QuestionType.JP_TO_IMAGE),
        )

        val score = QuizScoring.score(attempts)

        // A kérdéstípus nem befolyásolja a súlyt: minden megválaszolt kérdés egy egység.
        assertThat(score).isEqualTo(Score(answered = 5, correct = 3, ratio = 3.0 / 5.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }

    @Test
    fun score_perQuestionTypeBreakdownSumsBackToTheWholeSession_TR04() {
        // A haladás-képernyő típusonként bontva is mutatja az eredményt (FR-24): a
        // bontás ugyanezzel a függvénnyel készül, és vissza kell adnia az összesítést.
        val attempts = listOf(
            attempt(1, correct = true, questionType = QuestionType.JP_TO_HU),
            attempt(2, correct = true, questionType = QuestionType.JP_TO_HU),
            attempt(3, correct = false, questionType = QuestionType.HU_TO_JP),
            attempt(4, correct = true, questionType = QuestionType.IMAGE_TO_JP),
            attempt(5, correct = false, questionType = QuestionType.JP_TO_IMAGE),
            attempt(6, correct = false, questionType = QuestionType.JP_TO_IMAGE),
        )

        val whole = QuizScoring.score(attempts)
        val perType = QuestionType.entries.associateWith { type ->
            QuizScoring.score(attempts.filter { it.questionType == type })
        }

        assertThat(perType.values.sumOf { it.answered }).isEqualTo(whole.answered)
        assertThat(perType.values.sumOf { it.correct }).isEqualTo(whole.correct)
        assertThat(perType.getValue(QuestionType.JP_TO_HU).ratio).isEqualTo(1.0)
        assertThat(perType.getValue(QuestionType.HU_TO_JP).ratio).isEqualTo(0.0)
        assertThat(perType.getValue(QuestionType.JP_TO_IMAGE).ratio).isEqualTo(0.0)
        assertThat(whole.ratio).isEqualTo(0.5)
    }

    @Test
    fun score_typeWithNoAnsweredQuestion_isMissingDataNotZeroPercent_TR04() {
        // Vegyes menet, amelyben egy típusra nem jutott válasz: annak a bontásnak
        // „nincs adat" az értéke, nem 0% (FR-27).
        val attempts = listOf(
            attempt(1, correct = true, questionType = QuestionType.JP_TO_HU),
            attempt(2, correct = false, questionType = QuestionType.HU_TO_JP),
        )

        val imageToJp = QuizScoring.score(attempts.filter { it.questionType == QuestionType.IMAGE_TO_JP })

        assertThat(imageToJp.answered).isEqualTo(0)
        assertThat(imageToJp.ratio).isNull()
    }

    @Test
    fun score_mixedTypesWithDuplicatesAcrossSessions_aggregatesForProgressScreen_TR04() {
        // A haladás-képernyő több menet próbálkozásait adja át egyszerre: a kérdés-
        // azonosító csak menetenként egyedi, a dedup kulcsa ezért a pár (FR-21).
        val second = SessionId.of("session-tr04-b")
        val sharedQuestion = QuestionId.of("q-1")
        val attempts = listOf(
            attempt(1, correct = true, questionId = sharedQuestion, questionType = QuestionType.JP_TO_HU),
            // dupla rekord ugyanarra a menet+kérdés párra
            attempt(
                index = 2,
                correct = false,
                questionId = sharedQuestion,
                questionType = QuestionType.JP_TO_HU,
                answeredAt = base.plusSeconds(60),
            ),
            attempt(
                index = 3,
                correct = false,
                sessionId = second,
                questionId = sharedQuestion,
                questionType = QuestionType.IMAGE_TO_JP,
            ),
            attempt(4, correct = true, sessionId = second, questionType = QuestionType.JP_TO_IMAGE),
        )

        val score = QuizScoring.score(attempts)

        assertThat(attempts).hasSize(4)
        assertThat(score).isEqualTo(Score(answered = 3, correct = 2, ratio = 2.0 / 3.0))
        assertAnsweredMatchesDistinctQuestions(attempts, score)
    }
}
