package hu.kyokushin.trainer.feature.quiz.result

import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score

/**
 * A kvízeredmény képernyő állapota (FUNCTIONAL_SPEC 5.3, FR-20, FR-21, FR-27, T-411,
 * T-412, T-415, G-09 csoport).
 *
 * A [Content.score] a **mozgásban lévő** igazság (`QuizScoring`-ból, T-113): a
 * [QuizResultViewModel] nem számol újra helyességet, csak összegzi a már megítélt
 * próbálkozásokat (DR-09) – lásd a `kyo-quiz-engine` skill 7. szakasza.
 */
sealed interface QuizResultUiState {

    /** A menet visszaolvasása még nem tért vissza. */
    data object Loading : QuizResultUiState

    /**
     * Védelmi háló (NFR-06): a `sessionId` hiányzik, sérült, vagy nincs ilyen menet a
     * tárolt adatban (pl. a felhasználó közben törölte a haladást). A `quiz/run` normál
     * esetben nem engedi ide eljutni a felhasználót.
     */
    data object NotFound : QuizResultUiState

    /**
     * @property sessionId a megjelenített menet azonosítója.
     * @property score a sikerarány (FR-20, FR-27) – [Score.answered] `0`-ra a [Score.ratio]
     *   `null`, ezt a képernyő „Nem válaszoltál egyetlen kérdésre sem." üzenetre képezi
     *   (lásd [quizResultSummary]), nem `0%`-ra.
     * @property requestedCount a kért kérdésszám – a megszakított kvíz üzenetéhez
     *   („Megszakított kvíz – `{megválaszolt}` / `{kért}` kérdés.", FUNCTIONAL_SPEC 5.3/1.).
     * @property aborted igaz, ha a menet `finishedAt`-ja `null` (FR-19) – a felhasználó
     *   megszakította, nem fejezte be rendesen.
     * @property wrongItems a hibás kérdések listája, a helyes és a választott technika
     *   feloldott megjelenítési adataival (FUNCTIONAL_SPEC 5.3/2.). Csak azokat a hibás
     *   próbálkozásokat tartalmazza, amelyeknek MINDKÉT technikája feloldható a jelenlegi
     *   tudástárból (NFR-06) – lásd [QuizResultViewModel] KDoc-ja.
     * @property canRetryWrong igaz, ha van legalább egy hibás próbálkozás ebben a
     *   menetben – ettől **függetlenül** jelenik meg a gomb attól, hogy [wrongItems]
     *   fel tudta-e oldani mindegyiket (FUNCTIONAL_SPEC 5.3/3., T-412 „Teendők" 3.).
     * @property datasetTrust a tudástár bizalmi szintje (ADR-010): a lektorálatlanság-
     *   szalag bemenete (T-415).
     */
    data class Content(
        val sessionId: SessionId,
        val score: Score,
        val requestedCount: Int,
        val aborted: Boolean,
        val wrongItems: List<QuizResultWrongItem>,
        val canRetryWrong: Boolean,
        val datasetTrust: DatasetTrust,
    ) : QuizResultUiState
}

/**
 * Egy hibás kérdés tétele az eredménylistában (FUNCTIONAL_SPEC 5.3/2.).
 *
 * A tétel mindkét technikáját (a helyeset és a választottat is) a
 * [QuizResultViewModel] oldja fel a tudástárból, `getTechnique`-tel – ez a metódus a
 * visszavont technikát is visszaadja (DR-07), tehát egy később visszavont technikára
 * hivatkozó, régebbi menet eredménye is megjeleníthető marad.
 *
 * @property questionType a kérdés típusa – a lista soronként jelzi, milyen kérdésben
 *   hibázott a felhasználó.
 * @property correctIllustration csak a helyes technikáé: a kis ábra (FUNCTIONAL_SPEC
 *   5.3/2.: „a helyes technika (romaji + magyar + kis ábra)"). A választott technikának
 *   a specifikáció nem ír elő ábrát.
 */
data class QuizResultWrongItem(
    val questionType: QuestionType,
    val correctTechniqueId: TechniqueId,
    val correctRomaji: String,
    val correctHungarian: String,
    val correctIllustration: IllustrationRef?,
    val selectedTechniqueId: TechniqueId,
    val selectedRomaji: String,
    val selectedHungarian: String,
)
