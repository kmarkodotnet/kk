package hu.kyokushin.trainer.feature.learn.setup

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.TechniqueId
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
 * @property drillDown nem `null`, ha éppen egy kategória technikalistáját böngésszük
 *   (T-705, FR-39) – ilyenkor a UI a kategóriarács helyett ezt jeleníti meg. Lásd
 *   [LearnCategoryDrillDownUiState] a tervezési döntésért.
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
        val drillDown: LearnCategoryDrillDownUiState? = null,
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

/**
 * Egy kategória technikalistájának böngészése (T-705, FR-39).
 *
 * ## Tervezési döntés: a kijelölés szintje kategória marad, nem technika
 *
 * A task felveti, hogy a technikalistából "akár egy technikával is, akár többet
 * kijelölve" induljanak a kártyák. Ez a megvalósítás **szándékosan a kategória
 * szintjén tartja a kijelölést** (ugyanaz a [LearnSetupUiState.Content.selectedCategories]
 * – a drill-down innen a teljes kategóriát kapcsolja ki/be, nem egyes technikákat),
 * **nem** vezet be technika-szintű kijelölést. Ennek két oka van:
 *
 * 1. A task fejléce ezt a modulhatárt adja: `feature/learn, core/ui` – a kártyák
 *    tényleges tartalmát a `LearnCardsViewModel` (szintén `feature/learn`, de a
 *    `navigation` modul route-ja is érinti) ÉS a navigáció route-ja adja át; ha a
 *    kijelölés technika-szintűvé válna, ennek a két, a task hatókörén kívüli helynek
 *    is változnia kellene, csak hogy ugyanazt az eredményt (egy kategória teljes
 *    tananyaga) adja, mint ma.
 * 2. A technikalista itt **böngészésre** szolgál (a felhasználó konkrétan lássa, mi
 *    van egy kategóriában, mielőtt kiválasztja) – ez már önmagában teljesíti a
 *    "kattints a kategóriára, és egyesével jelenjenek meg a tananyagok" kérést. A
 *    tényleges tanulás ettől függetlenül a teljes kategórián megy (ugyanaz a
 *    seedelt, kevert sorrend, mint eddig). A drill-down fejlécének kijelölés-gombja a
 *    már meglévő `LearnSetupViewModel.onToggleCategory(categoryId)`-t hívja – nincs
 *    hozzá külön ViewModel-metódus.
 *
 * **Az US-09 "több kategória egyszerre" folytonossága:** mivel a kijelölés
 * [LearnSetupUiState.Content.selectedCategories]-ben él, változatlanul egyszerre több
 * kategória is kijelölhető – a felhasználó bejárhat több kategóriát egymás után a
 * "Kiválasztva a tanuláshoz" gombbal, a kijelölés kategóriánként összeadódik, pontosan
 * úgy, mint a korábbi checkbox-listánál.
 *
 * @property categoryId a böngészett kategória.
 * @property categoryName a kategória magyar neve (a fejlécben jelenik meg).
 * @property techniques a kategória technikái, böngészhető, nem interaktív listában.
 * @property selected ki van-e jelölve ez a kategória a tanuláshoz (ugyanaz az érték,
 *   mint a megfelelő [LearnCategoryUiModel.selected] – csak a drill-down fejlécében
 *   is megjelenítve, hogy a felhasználó innen is lássa/váltsa).
 */
data class LearnCategoryDrillDownUiState(
    val categoryId: CategoryId,
    val categoryName: String,
    val techniques: List<LearnTechniqueUiModel>,
    val selected: Boolean,
)

/**
 * Egy technika egy sora a kategória-drill-down böngészhető listáján (T-705, FR-39).
 *
 * Szándékosan csak romaji + magyar jelentés – nem a teljes
 * [hu.kyokushin.trainer.core.model.Technique] (ami pl. illusztrációt, aliasokat,
 * hitelesítési státuszt is hordozna): a böngészhető lista ezekből semmit nem jelenít
 * meg, egy szűkebb, UI-nak szánt vetület egyszerűbb, mint a teljes domain modell
 * átadása és a Composable-ben való szűrése.
 */
data class LearnTechniqueUiModel(
    val id: TechniqueId,
    val romaji: String,
    val hungarian: String,
)
