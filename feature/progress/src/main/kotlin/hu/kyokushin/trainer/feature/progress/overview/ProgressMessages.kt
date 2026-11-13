package hu.kyokushin.trainer.feature.progress.overview

import hu.kyokushin.trainer.core.quiz.scoring.Score
import kotlin.math.roundToInt

/**
 * A sikerarány megjelenítésének döntési logikája, `@Composable`-mentes formában
 * (FUNCTIONAL_SPEC 6.1/2–3., FR-27, T-503 „Kész ha" 1.).
 *
 * Ugyanaz a minta, mint a `feature/quiz` `QuizResultMessages.kt`/`quizResultSummary`-je
 * (CONVENTIONS 4.1 „Compose döntési logika tesztelése emulátor nélkül"): a „0 megválaszolt
 * kérdés → nincs adat, nem 0%" elágazás itt él, sima JUnittal bizonyíthatóan, és ezt
 * hívja **mind** az összesítő kártya, **mind** egy kategóriasor – nincs két külön
 * megfogalmazás ugyanarra a szabályra (DR-09).
 */
sealed interface ProgressScoreDisplay {
    /** Legalább egy próbálkozás történt – [percent] a `0..100` tartományban. */
    data class Scored(val correct: Int, val answered: Int, val percent: Int) : ProgressScoreDisplay

    /** [Score.answered]`== 0`: „nincs adat", soha nem „0%" (FR-27). */
    data object NoData : ProgressScoreDisplay
}

/** A [score]-hoz tartozó [ProgressScoreDisplay] – lásd a típus KDoc-ja. */
fun progressScoreDisplay(score: Score): ProgressScoreDisplay =
    if (score.answered == 0) {
        ProgressScoreDisplay.NoData
    } else {
        ProgressScoreDisplay.Scored(
            correct = score.correct,
            answered = score.answered,
            // `ratio` nem lehet `null`, mert `answered > 0` (lásd a `Score` konstruktora).
            percent = (requireNotNull(score.ratio) * PERCENT_SCALE).roundToInt(),
        )
    }

private const val PERCENT_SCALE = 100.0
