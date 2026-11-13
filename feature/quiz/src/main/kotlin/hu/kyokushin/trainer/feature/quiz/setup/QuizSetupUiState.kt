package hu.kyokushin.trainer.feature.quiz.setup

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.quiz.generator.Shortfall

/**
 * A kvízbeállító képernyő teljes állapota (FUNCTIONAL_SPEC 5.1, FR-11, FR-12, FR-13,
 * FR-22, US-12, T-403).
 *
 * ## Üres kategóriakijelölés = MINDEN kategória
 *
 * Ellentétben a tanulás-beállítóval ([hu.kyokushin.trainer.feature.learn.setup.LearnSetupUiState]),
 * ahol a kijelölés kötelező választás, itt az üres [Content.selectedCategories] a
 * `QuizRequest.categoryFilter` dokumentált szemantikáját követi: „a beállító képernyő
 * kijelölésmentes állapota" pontosan a „minden kategória" jelentést hordozza
 * (`core/quiz/.../QuizRequest.kt` KDoc-ja, FR-11). A [Content.canStart] ezért **soha**
 * nem a kategóriakijelölésen bukik el – kizárólag a [Content.expectedQuestionCount]-on.
 *
 * ## A `dryRun()` a motor egyetlen igazságforrása
 *
 * A [Content.expectedQuestionCount] és a [Content.shortfall] közvetlenül a
 * `QuizGenerator.dryRun()` kimenete – ez a típus nem számol újra semmit, nem kerekít, és
 * nem optimistább a motornál (T-403 „Figyelj rá"). A `generate()` ugyanarra a kérésre
 * bizonyítottan ugyanennyi kérdést ad (T-111, T-112).
 *
 * @property categories a kategóriák jelölőnégyzet-listája, a tartalom diktálta sorrendben.
 * @property selectedCategories a kijelölt kategóriák; lásd a típus KDoc-ját az üres
 *   halmaz jelentéséről.
 * @property requestedCount a kért kérdésszám (5 / 10 / 20, FR-11).
 * @property questionTypes a négy kérdéstípus jelölőnégyzete, letiltási okkal (FR-12, FR-13).
 * @property expectedQuestionCount ennyi kérdés áll össze a jelenlegi beállításokkal – a
 *   `dryRun()` közvetlen kimenete.
 * @property shortfall `null`, ha a kért kérdésszám pontosan kijön; egyébként a hiány és az
 *   oka (FR-22). A szöveggé alakítás a UI feladata (`quiz.shortfall`, FUNCTIONAL_SPEC 8.).
 * @property canStart igaz, ha [expectedQuestionCount] pozitív – a „Kvíz indítása" gomb
 *   egyetlen bemenete.
 * @property notEnoughDataMessageRes a letiltott „Kvíz indítása" gomb alatti indoklás,
 *   vagy `null`, ha [canStart] igaz (FUNCTIONAL_SPEC 5.1/5., T-403 „Kész ha" 1. pont). A
 *   szöveg a `quiz.notEnoughData` kulcs alatt szó szerint szerepel a FUNCTIONAL_SPEC 8.
 *   fejezetében – egyetlen mondat, amely mindhárom javaslatot (kategória, kérdéstípus,
 *   kérdésszám) hordozza; **nem** három külön `strings.xml` bejegyzés, mert a 8. fejezet
 *   nem ad hármat, és a CONVENTIONS 4. szerint tilos új szöveget kitalálni, ahol a
 *   mesterforrás már pontos szöveget ad.
 * @property datasetTrust a tudástár bizalmi szintje; a lektorálatlanság-szalag bemenete
 *   (ADR-010).
 */
sealed interface QuizSetupUiState {
    /** Első betöltés fut (a tartalomimportot ez a ViewModel indítja, FUNCTIONAL_SPEC 7.1). */
    data object Loading : QuizSetupUiState

    data class Content(
        val categories: List<QuizCategoryUiModel>,
        val selectedCategories: Set<CategoryId>,
        val requestedCount: Int,
        val questionTypes: List<QuizQuestionTypeUiModel>,
        val expectedQuestionCount: Int,
        val shortfall: Shortfall?,
        val canStart: Boolean,
        @StringRes val notEnoughDataMessageRes: Int?,
        val datasetTrust: DatasetTrust,
    ) : QuizSetupUiState

    /**
     * A tartalom betöltése kivétellel elhasalt (FUNCTIONAL_SPEC 10.).
     *
     * `@StringRes`, nem `String`: a kivétel angol `message`-e nem juthat a felhasználó elé
     * (NFR-02).
     */
    data class Error(@StringRes val messageRes: Int) : QuizSetupUiState
}

/**
 * Egy kategória jelölőnégyzete a kvízbeállító képernyőn (FUNCTIONAL_SPEC 5.1/2.: „8
 * kvízképes / 12 technika").
 *
 * @property id a kategória azonosítója; a sor megnyomása ezt váltja a kijelölésben.
 * @property nameHu a kategória magyar neve – tartalmi adat, nem `strings.xml` erőforrás
 *   (ADR-011).
 * @property quizEligibleCount hány technika kvízképes ebben a kategóriában – legalább egy
 *   kérdéstípusban átjut a jogosultsági kapun ([hu.kyokushin.trainer.core.quiz.validation.QuizEligibility]),
 *   a jelenlegi tudástár bizalmi szintje mellett. Független attól, melyik típust jelölte
 *   ki a felhasználó.
 * @property totalCount hány, nem visszavont technika van ebben a kategóriában összesen
 *   (a hitelesítési státusztól függetlenül) – a „/ 12 technika" fele.
 * @property selected ki van-e jelölve ez a kategória.
 */
data class QuizCategoryUiModel(
    val id: CategoryId,
    val nameHu: String,
    val quizEligibleCount: Int,
    val totalCount: Int,
    val selected: Boolean,
)

/**
 * Egy kérdéstípus jelölőnégyzete a kvízbeállító képernyőn (FUNCTIONAL_SPEC 5.1/4., FR-12,
 * FR-13).
 *
 * @property type a kérdéstípus.
 * @property selected a felhasználó kijelölése – a jelölőnégyzet állása, függetlenül attól,
 *   hogy a típus jelenleg [enabled] is.
 * @property enabled interakcióra kész-e a jelölőnégyzet. Hamis, ha a kiválasztott
 *   kategóriákban **tartalmilag** nem áll össze ebből a típusból kérdés (FR-13) – ez a
 *   döntés a felhasználó saját [selected] állásától **független**: egy ki nem jelölt, de
 *   egyébként tartalmilag elérhető típus [enabled] marad, hogy a felhasználó bármikor
 *   bekapcsolhassa.
 * @property disabledReasonRes a letiltás indoklása, vagy `null`, ha [enabled] igaz.
 * @property disabledReasonArg az indoklás szövegének `%1$d` paramétere – a kiválasztott
 *   kategóriákban jelenleg rendelkezésre álló jelöltek száma (0, ha [enabled] igaz).
 */
data class QuizQuestionTypeUiModel(
    val type: QuestionType,
    val selected: Boolean,
    val enabled: Boolean,
    @StringRes val disabledReasonRes: Int?,
    val disabledReasonArg: Int,
)
