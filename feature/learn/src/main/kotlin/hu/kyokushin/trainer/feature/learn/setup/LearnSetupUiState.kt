package hu.kyokushin.trainer.feature.learn.setup

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.feature.learn.HideMode

/**
 * A tanulás-beállító képernyő teljes állapota (FUNCTIONAL_SPEC 4.1, FR-06, FR-07, US-09,
 * US-10).
 *
 * ## Miért nincs `quizEligible` vagy `verified` szűrés itt
 *
 * A tanuláshoz **nem** kell hitelesített vagy kvízképes rekord (T-302 „Kész ha"): a
 * lektorálatlan tartalom is böngészhető, jelöléssel. A [LearnCategoryUiModel.displayableCount]
 * ezért minden nem visszavont technikát számol a kategóriában, függetlenül a
 * [hu.kyokushin.trainer.core.model.VerificationStatus]-tól – ellentétben a kvíz
 * beállításával (FUNCTIONAL_SPEC 5.1), ahol a szám a **kvízképes** technikákra szól. A
 * FUNCTIONAL_SPEC 4.1/2. pontja szó szerint „a kvízképes és összes rekord száma" szöveget
 * ad, de ez ellentmond a rögtön utána következő, explicit mondatnak („A tanuláshoz NEM
 * szükséges `quizEligible` vagy `verified` státusz") és a megadott példának
 * (csak „12 technika", nem „8 kvízképes / 12 technika") – feltehetően a kvíz 5.1
 * szakaszából átvett, nem frissített mondat (lásd `docs/CONVENTIONS.md` 8. – doksi drift).
 * Ez a típus ezért kizárólag az összes megjeleníthető (nem visszavont) technika számát
 * hordozza.
 *
 * @property categories a kategóriák jelölőnégyzet-listája, a tartalom diktálta sorrendben.
 * @property selectedCategories a kijelölt kategóriák. **Itt az üres halmaz NEM „minden
 *   kategória"** (ellentétben a Tudástár szűrőjével és a kvíz kategóriaszűrőjével): a
 *   FUNCTIONAL_SPEC 4.1/4. pontja szerint a „Kártyák indítása" gomb pontosan akkor tiltott,
 *   ha **nincs** kiválasztott kategória – azaz itt a kijelölés kötelező, nem szűrő.
 * @property hideMode melyik nevet rejtse el a kártya (FR-07); az utolsó választás a
 *   `SavedStateHandle`-ben őrződik meg (FUNCTIONAL_SPEC 4.1/5.).
 * @property canStart igaz, ha a „Kártyák indítása" gomb aktív: legalább egy kategória
 *   kijelölve, és a kijelölésben legalább egy megjeleníthető technika van.
 * @property disabledReasonRes a letiltás indoklása, vagy `null`, ha [canStart] igaz
 *   (FUNCTIONAL_SPEC 4.1/4.: „A letiltás mellett magyarázó szöveg").
 * @property datasetTrust a tudástár bizalmi szintje; a lektorálatlanság-szalag bemenete
 *   (ADR-010).
 */
sealed interface LearnSetupUiState {
    /** Első betöltés fut (a tartalomimportot ez a ViewModel indítja, FUNCTIONAL_SPEC 7.1). */
    data object Loading : LearnSetupUiState

    data class Content(
        val categories: List<LearnCategoryUiModel>,
        val selectedCategories: Set<CategoryId>,
        val hideMode: HideMode,
        val canStart: Boolean,
        @StringRes val disabledReasonRes: Int?,
        val datasetTrust: DatasetTrust,
    ) : LearnSetupUiState

    /**
     * A tartalom betöltése kivétellel elhasalt (FUNCTIONAL_SPEC 10.).
     *
     * `@StringRes`, nem `String`: a kivétel angol `message`-e nem juthat a felhasználó elé
     * (NFR-02).
     */
    data class Error(@StringRes val messageRes: Int) : LearnSetupUiState
}

/**
 * Egy kategória jelölőnégyzete a beállító képernyőn (FUNCTIONAL_SPEC 4.1/2.).
 *
 * @property id a kategória azonosítója; a sor megnyomása ezt váltja a kijelölésben.
 * @property nameHu a kategória magyar neve – tartalmi adat, nem `strings.xml` erőforrás
 *   (ADR-011), ugyanaz a konvenció, mint a Tudástár kategóriachipjeinél.
 * @property displayableCount hány, nem visszavont technika van ebben a kategóriában,
 *   hitelesítési státusztól függetlenül (lásd a típus osztály-dokumentációja).
 * @property selected ki van-e jelölve ez a kategória.
 */
data class LearnCategoryUiModel(
    val id: CategoryId,
    val nameHu: String,
    val displayableCount: Int,
    val selected: Boolean,
)
