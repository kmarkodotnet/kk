package hu.kyokushin.trainer.feature.quiz.setup

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.feature.quiz.R

/**
 * A kvízbeállító képernyő apró szövegdöntései, `@Composable`-mentes formában
 * (FUNCTIONAL_SPEC 5.1, FR-12, FR-22, T-404).
 *
 * Ugyanaz a minta, mint a `LibraryListItemAccessibility`/`UnreviewedNotice`/
 * `LearnCardIllustrationLabelPolicy`: egy döntés (melyik `strings.xml` kulcs és milyen
 * argumentumokkal), kiemelve a `@Composable` függvény testéből, hogy sima JUnittel (itt:
 * Robolectric-kel, a tényleges `strings.xml` fölött) bizonyítható legyen emulátor és
 * Compose-renderelés nélkül (CONVENTIONS 4.1).
 *
 * @property textRes a megjelenítendő `strings.xml` kulcs.
 * @property args a formázó argumentumok, a kulcs `%1$d`/`%2$d` sorrendjében – a Composable
 *   oldalon `stringResource(textRes, *args.toTypedArray())` váltja szöveggé.
 */
data class QuizSetupMessage(
    @StringRes val textRes: Int,
    val args: List<Int> = emptyList(),
)

/**
 * Egy kérdéstípus felirata a jelölőnégyzet mellett (FUNCTIONAL_SPEC 5.1/4., FR-12).
 *
 * A négy szöveg a FUNCTIONAL_SPEC 8. fejezetében szó szerint megadott
 * (`quiz.type.jpToHu`/`huToJp`/`imageToJp`/`jpToImage`) – lásd a `strings.xml` megfelelő
 * bejegyzéseit.
 */
@StringRes
fun questionTypeLabelRes(type: QuestionType): Int = when (type) {
    QuestionType.JP_TO_HU -> R.string.quiz_setup_type_jp_to_hu
    QuestionType.HU_TO_JP -> R.string.quiz_setup_type_hu_to_jp
    QuestionType.IMAGE_TO_JP -> R.string.quiz_setup_type_image_to_jp
    QuestionType.JP_TO_IMAGE -> R.string.quiz_setup_type_jp_to_image
}

/**
 * Az élő készenléti összegzés (FUNCTIONAL_SPEC 5.1/5.): „A beállításokkal `{n}` kérdés
 * állítható össze.", vagy – ha a kért kérdésszám nem jön ki – a hiány oka és mértéke
 * (FR-22, `quiz.shortfall`: „A kért `{k}` helyett `{n}` kérdés lesz, mert nincs több
 * megfelelő technika.").
 *
 * A [QuizSetupUiState.Content.shortfall] `null`/nem-`null` állapota dönt a két variáns
 * között; ez **ugyanaz** a feltétel, amit a `QuizReadiness.init` már kikényszerít (lásd
 * `core/quiz/.../generator/QuizReadiness.kt`), tehát a két ág kölcsönösen kizárja egymást
 * – nincs harmadik, "mindkettő" vagy "se" eset.
 */
fun quizReadinessSummary(state: QuizSetupUiState.Content): QuizSetupMessage {
    val shortfall = state.shortfall
    return if (shortfall == null) {
        QuizSetupMessage(
            textRes = R.string.quiz_setup_summary_ready,
            args = listOf(state.expectedQuestionCount),
        )
    } else {
        QuizSetupMessage(
            textRes = R.string.quiz_setup_shortfall,
            args = listOf(shortfall.requestedCount, shortfall.actualCount),
        )
    }
}
