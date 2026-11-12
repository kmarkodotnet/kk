package hu.kyokushin.trainer.feature.library.list

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.state.EmptyStateReason

/**
 * A Tudástár lista képernyő teljes állapota (FUNCTIONAL_SPEC 3.1, FR-01…FR-03).
 *
 * ## Miért sealed, és miért van mégis négy közös tulajdonsága?
 *
 * A képernyő négy állapota (`Loading` / `Empty` / `Content` / `Error`, FUNCTIONAL_SPEC 2.4)
 * **egymást kizárja** a lista helyén – ezért sealed. A keresősáv és a kategóriachipek
 * viszont **mind a négy** állapotban látszanak, és látszaniuk is kell: a „nincs találat"
 * helyzetből éppen a keresősáv törlésével lehet kijutni (US-06 4. kritérium). Ha a
 * keresőszöveg csak a [Content] ágon élne, az `Empty` állapotban eltűnne a vezérlő,
 * amivel a felhasználó visszavonhatja a saját szűkítését.
 *
 * Ezért a [query], a [categories], a [selectedCategories] és a [datasetTrust] a sealed
 * interfész **közös** tulajdonsága: nem lehet olyan állapotot előállítani, amelyben a
 * képernyő kerete hiányzik.
 *
 * ## Amit ez a típus nem tartalmaz
 *
 * Előformázott magyar szöveget. A ViewModel nem ismeri a Compose-t és nem oldja fel az
 * erőforrásokat (L6): az üres állapot üzenetét az [EmptyStateReason] hordozza, a
 * hibaüzenet `@StringRes` azonosító, a kategórianév pedig **tartalmi adat**
 * (`Category.nameHu`, ADR-011), nem erőforrás.
 *
 * @property query a keresőszöveg, ahogy a felhasználó beírta – nem a normalizált alak.
 *   Ez megy vissza a keresősávba, és ezt idézi a „nincs találat" üzenet.
 * @property categories a kategóriachipek a tartalom diktálta sorrendben (DR-02), mindegyik
 *   a saját találatszámával.
 * @property selectedCategories a kijelölt kategóriák. **Üres halmaz = minden kategória**
 *   („Összes" chip) – ugyanaz a konvenció, mint a repositoryban és a kvízkérésben.
 * @property datasetTrust a tudástár bizalmi szintje; ebből dönti el a képernyő, hogy
 *   kirajzolja-e a lektorálatlanság-szalagot (ADR-010, AC-08). Nem felhasználói
 *   beállítás, és kódból soha nem emelkedik.
 */
sealed interface LibraryListUiState {

    val query: String
    val categories: List<CategoryFilterUiModel>
    val selectedCategories: Set<CategoryId>
    val datasetTrust: DatasetTrust

    /**
     * Első betöltés vagy tartalomfrissítés fut (FUNCTIONAL_SPEC 7.1).
     *
     * A tartalomimportot a Tudástár ViewModelje indítja, nem az `Application`: csak így
     * tud a képernyő valódi `Loading` állapotot mutatni, amíg a betöltés tart.
     */
    data class Loading(
        override val query: String = "",
        override val categories: List<CategoryFilterUiModel> = emptyList(),
        override val selectedCategories: Set<CategoryId> = emptySet(),
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : LibraryListUiState

    /**
     * Nincs megjeleníthető tétel – és az **ok** mondja meg, mit írunk ki.
     *
     * A két ok (`LibraryNoContent` vs. `LibraryNoResult`) a felhasználó számára két
     * teljesen más helyzet: az egyik betöltési hiba, a másik a saját keresésének
     * következménye (T-205, FUNCTIONAL_SPEC 3.1). Puszta „üres lista" nem elég.
     */
    data class Empty(
        val reason: EmptyStateReason,
        override val query: String,
        override val categories: List<CategoryFilterUiModel>,
        override val selectedCategories: Set<CategoryId>,
        override val datasetTrust: DatasetTrust,
    ) : LibraryListUiState

    /**
     * Van találat.
     *
     * @property items a tételek a megjelenítés sorrendjében: kategória `sortOrder`, azon
     *   belül romaji ábécé szerint (FUNCTIONAL_SPEC 3.1, US-05 1. kritérium). A lista
     *   **már rendezett** – a Compose oldal nem rendez újra.
     */
    data class Content(
        val items: List<LibraryTechniqueUiModel>,
        override val query: String,
        override val categories: List<CategoryFilterUiModel>,
        override val selectedCategories: Set<CategoryId>,
        override val datasetTrust: DatasetTrust,
    ) : LibraryListUiState

    /**
     * A tartalom betöltése kivétellel elhasalt (FUNCTIONAL_SPEC 10.).
     *
     * `@StringRes` azonosító, nem `String`: így a kivétel angol `message`-e nem juthat a
     * felhasználó elé (NFR-02, lásd `ErrorState`).
     */
    data class Error(
        @StringRes val messageRes: Int,
        override val query: String = "",
        override val categories: List<CategoryFilterUiModel> = emptyList(),
        override val selectedCategories: Set<CategoryId> = emptySet(),
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : LibraryListUiState
}

/**
 * Egy kategóriachip a szűrősorban (FR-02, FUNCTIONAL_SPEC 3.1).
 *
 * @property id a kategória azonosítója; a chip megnyomása ezt váltja a kijelölésben.
 * @property nameHu a kategória magyar neve – **a tartalomból**, nem `strings.xml`-ből
 *   (ADR-011, US-05 4. kritérium).
 * @property matchCount hány találat esik erre a kategóriára az **aktuális keresőszóval**.
 *   Szándékosan a kategóriakijelölést **figyelmen kívül hagyva** számolt: ha a kijelölést
 *   is beszámítanánk, minden nem kijelölt chip nullát mutatna, és a szám épp arra a
 *   kérdésre nem válaszolna, amire a felhasználó használja („érdemes-e ide kattintani?").
 * @property selected ki van-e jelölve ez a chip.
 */
data class CategoryFilterUiModel(
    val id: CategoryId,
    val nameHu: String,
    val matchCount: Int,
    val selected: Boolean,
)

/**
 * Egy lista tétel (FUNCTIONAL_SPEC 3.1).
 *
 * @property id a technika azonosítója – a `LazyColumn` stabil kulcsa és a részletező
 *   képernyő navigációs argumentuma.
 * @property romaji a technika eredeti neve latin átírásban (a tétel fő sora).
 * @property hungarian a magyar jelentés (a tétel alsó sora).
 * @property categoryId a technika kategóriája; a részletező képernyő kategóriachipje
 *   ezzel szűr vissza a listára.
 * @property categoryNameHu a kategória magyar neve, feloldva – hogy a lista
 *   szakaszfejlécet is tudjon írni anélkül, hogy a Compose oldal újra összepárosítaná a
 *   kategóriákat a technikákkal.
 * @property illustration a képhivatkozás, vagy `null`, ha a rekord nem hivatkozik képre.
 * @property status a hitelesítési státusz; ebből lesz a tétel jobb oldali chipje
 *   (FUNCTIONAL_SPEC 2.2). A ViewModel **nem emeli** a státuszt (CR-02).
 * @property matchedAlias annak az aliasnak az eredeti alakja, amely a keresőszövegre
 *   illeszkedett, vagy `null`, ha a találat a technika saját nevéből jött. Nem `null`
 *   érték mellett jelenik meg a tétel alatt az „Egyezés: `{alias}`" sor
 *   (US-06 3. kritérium). A repository mondja meg (`TechniqueMatch`), nem a UI találja ki.
 */
data class LibraryTechniqueUiModel(
    val id: TechniqueId,
    val romaji: String,
    val hungarian: String,
    val categoryId: CategoryId,
    val categoryNameHu: String,
    val illustration: IllustrationRef?,
    val status: VerificationStatus,
    val matchedAlias: String?,
)
