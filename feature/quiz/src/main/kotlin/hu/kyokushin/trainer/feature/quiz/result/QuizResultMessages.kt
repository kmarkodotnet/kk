package hu.kyokushin.trainer.feature.quiz.result

import kotlin.math.roundToInt

/**
 * A kvízeredmény „Nagy összegzés" sorának döntési logikája, `@Composable`-mentes formában
 * (FUNCTIONAL_SPEC 5.3/1., FR-20, FR-27, T-411 „Kész ha").
 *
 * Ugyanaz a minta, mint a `QuizSetupMessages.kt`/`UnreviewedNotice`/
 * `LearnCardIllustrationLabelPolicy`: egy elágazás, kiemelve a `@Composable` törzséből,
 * hogy sima JUnittal (Robolectric/Compose nélkül) bizonyítható legyen (CONVENTIONS 4.1).
 *
 * A három ág **kölcsönösen kizárja** egymást, és a FUNCTIONAL_SPEC 5.3/1. felsorolásának
 * sorrendjét követi: a „0 válasz" eset elsőbbséget kap a „megszakított" eset előtt (egy
 * válasz nélkül megszakított menet is elsősorban „nem válaszoltál" üzenetet kap, nem
 * „megszakított kvíz – 0 / `n` kérdés"-t – a specifikáció a kettőt egymást kizáró, nem
 * egymásra épülő ágként sorolja fel).
 */
sealed interface QuizResultSummary {
    /** FUNCTIONAL_SPEC 5.3/1.: „`{helyes}` / `{megválaszolt}` helyes" + százalék. */
    data class Scored(val correct: Int, val answered: Int, val percent: Int) : QuizResultSummary

    /** FUNCTIONAL_SPEC 5.3/1., FR-27: „Nem válaszoltál egyetlen kérdésre sem." */
    data object NoAnswers : QuizResultSummary

    /** FUNCTIONAL_SPEC 5.3/1.: „Megszakított kvíz – `{megválaszolt}` / `{kért}` kérdés." */
    data class Aborted(val answered: Int, val requested: Int) : QuizResultSummary
}

/** Az aktuális állapothoz tartozó [QuizResultSummary] – lásd a típus KDoc-ja a sorrendről. */
fun quizResultSummary(state: QuizResultUiState.Content): QuizResultSummary {
    val score = state.score
    return when {
        score.answered == 0 -> QuizResultSummary.NoAnswers
        state.aborted -> QuizResultSummary.Aborted(answered = score.answered, requested = state.requestedCount)
        else -> QuizResultSummary.Scored(
            correct = score.correct,
            answered = score.answered,
            // `ratio` nem lehet `null`, mert `answered > 0` (lásd a `Score` konstruktora).
            percent = (requireNotNull(score.ratio) * PERCENT_SCALE).roundToInt(),
        )
    }
}

private const val PERCENT_SCALE = 100.0
