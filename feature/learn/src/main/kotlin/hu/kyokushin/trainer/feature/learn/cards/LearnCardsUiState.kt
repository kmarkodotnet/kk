package hu.kyokushin.trainer.feature.learn.cards

import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.feature.learn.HideMode

/**
 * A tanulókártya képernyő állapota (T-303, T-304, T-307, FR-06…FR-09, NFR-07,
 * FUNCTIONAL_SPEC 4.2, US-09).
 *
 * A [Content] és az [Empty] ág a T-303-ban készült; a Compose UI, ami ezeket fogyasztja,
 * a T-304 tárgya. A [Finished] ág (az utolsó kártya utáni záróképernyő) a T-307-ben
 * készült, ugyanerre a [LearnCardsViewModel]-re épülve.
 */
sealed interface LearnCardsUiState {
    /** A technikák betöltése még nem futott le (az első emisszióig). */
    data object Loading : LearnCardsUiState

    /**
     * Van megjeleníthető kártya.
     *
     * @property card az aktuális pozíción álló kártya tartalma.
     * @property position az aktuális kártya 0-alapú pozíciója a (seedelt) sorrendben.
     * @property total az összes kártya száma ebben a munkamenetben.
     * @property hasPrevious van-e „Előző" kártya.
     * @property hasNext van-e „Következő" kártya **a kártyák között** (a legutolsó
     *   kártyán `false` – ettől függetlenül a „Következő" gomb a UI-ban (T-304) innen
     *   is a [Finished] záróképernyőre vezet, nem letiltott; lásd [LearnCardsViewModel]).
     * @property hideMode melyik nevet rejti el a kártya (FR-07) – a `learn/setup`
     *   képernyő (T-302) bemenete.
     */
    data class Content(
        val card: LearnCardUiModel,
        val position: Int,
        val total: Int,
        val hasPrevious: Boolean,
        val hasNext: Boolean,
        val hideMode: HideMode,
    ) : LearnCardsUiState

    /**
     * A kiválasztott kategóriákban nincs megjeleníthető technika.
     *
     * A `learn/setup` képernyő (T-302) a „Kártyák indítása" gombot letiltva tartja,
     * amíg ez a helyzet fennáll, tehát ez az ág a gyakorlatban védelmi háló (NFR-06):
     * például ha a tartalom a kártyák megnyitása **után** frissül úgy, hogy minden
     * kiválasztott technika visszavonásra kerül.
     */
    data object Empty : LearnCardsUiState

    /**
     * Az utolsó kártya után megjelenő záróképernyő (T-307, FR-06, FUNCTIONAL_SPEC 4.2:
     * „Az utolsó kártya után záróképernyő: … + Újrakezdés / Vissza a beállításokhoz /
     * Kvíz indítása ezekkel a kategóriákkal").
     *
     * @property total hány kártyán ment végig a felhasználó ebben a munkamenetben.
     * @property categoryIds a kiválasztott kategóriák – ez adódik tovább a „Kvíz
     *   indítása ezekkel a kategóriákkal" gombbal (lásd [LearnCardsUiState] osztály
     *   KDoc-ja és a `LearnCardsScreen` `onStartQuiz` paramétere).
     */
    data class Finished(
        val total: Int,
        val categoryIds: Set<CategoryId>,
    ) : LearnCardsUiState
}

/**
 * Egy tanulókártya tartalma (FUNCTIONAL_SPEC 4.2).
 *
 * @property techniqueId a kártya technikája – a felfedettség ehhez az azonosítóhoz
 *   kötődik (lásd [LearnCardsViewModel] osztály-dokumentációja).
 * @property categoryName a technika kategóriájának magyar neve (a felső sáv „kategória
 *   neve" eleme, FUNCTIONAL_SPEC 4.2/1.) – kártyánként más lehet, ha a kiválasztás több
 *   kategóriát fed. Üres sztring, ha a kategória valamiért nem található (NFR-06, nem
 *   omlik össze).
 * @property romaji a technika eredeti neve latin átírásban.
 * @property hungarian a magyar jelentés.
 * @property illustration a képhivatkozás, vagy `null`, ha a rekord nem hivatkozik képre.
 * @property status a hitelesítési státusz – a tanuláshoz ez NEM kapu (T-302 „Kész ha"),
 *   csak megjelenítési adat (pl. „Ellenőrzésre vár" jelölő, T-304 tárgya).
 * @property revealed fel van-e fedve ez a kártya (FUNCTIONAL_SPEC 4.2: „Felfedés után a
 *   kártyán mind a három elem látszik"). Kártyánként tárolt, nem globális (FR-08).
 */
data class LearnCardUiModel(
    val techniqueId: TechniqueId,
    val categoryName: String,
    val romaji: String,
    val hungarian: String,
    val illustration: IllustrationRef?,
    val status: VerificationStatus,
    val revealed: Boolean,
)
