package hu.kyokushin.trainer.feature.library.detail

import androidx.annotation.StringRes
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import hu.kyokushin.trainer.core.ui.status.TechniqueField

/**
 * A Tudástár részletező képernyő teljes állapota (FUNCTIONAL_SPEC 3.2, FR-04, FR-05,
 * US-07, US-08).
 *
 * ## Miért sealed, és miért van mégis egy közös tulajdonsága?
 *
 * Ugyanaz a döntés, mint a [hu.kyokushin.trainer.feature.library.list.LibraryListUiState]-nél:
 * a lektorálatlanság-szalag minden állapotban – betöltés, tartalom, hiba – látszik, tehát a
 * [datasetTrust] a sealed interfész **közös** tulajdonsága (ADR-010, AC-08).
 *
 * ## Amit ez a típus nem tartalmaz
 *
 * - Előformázott magyar szöveget: a hibaüzenet `@StringRes` azonosító (NFR-02), a
 *   kategórianév pedig **tartalmi adat** (`Category.nameHu`, ADR-011).
 * - „Eddig gyakoroltam" összegzést a haladás-részletezőhöz. A FUNCTIONAL_SPEC 3.2 11.
 *   pontja ezt előírja, de nincs hozzá elérhető adatforrás: a `ProgressRepository`
 *   (T-301/T-401) még nincs megírva, és a `TechniqueRepository` szándékosan csak a
 *   tartalmat adja (nem a felhasználói próbálkozásokat). A mezőt csak akkor érdemes
 *   felvenni, amikor van mögötte valódi lekérdezés – addig a hiánya nem „nincs adat"
 *   zaj, hanem a funkció tényleges, dokumentált hiánya (lásd `docs/CONVENTIONS.md` 8.).
 *
 * @property datasetTrust a tudástár bizalmi szintje; ebből dönti el a képernyő, hogy
 *   kirajzolja-e a lektorálatlanság-szalagot. Nem felhasználói beállítás, és kódból soha
 *   nem emelkedik (CR-02).
 */
sealed interface LibraryDetailUiState {

    val datasetTrust: DatasetTrust

    /** Az `getTechnique` lekérdezés fut. */
    data class Loading(
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : LibraryDetailUiState

    /**
     * A technika betöltve.
     *
     * @property japaneseScript az eredeti japán írásmód, vagy `null`. `null` esetén a
     *   szakasz nem jelenik meg (FUNCTIONAL_SPEC 3.2: „Hiányzó opcionális mező... nincs
     *   'nincs adat' zaj").
     * @property aliases további elfogadott elnevezések. Üres lista esetén a „Más néven…"
     *   szakasz nem jelenik meg.
     * @property descriptionHu rövid magyar leírás, vagy `null` – ugyanaz a szabály.
     * @property illustration a nyers képhivatkozás, `null`-lal együtt: a képernyő (nem
     *   ez a típus) dönti el, hogy a `null`-t helyőrzőként jeleníti meg (US-07 2.
     *   kritérium), mert ez a döntés kizárólag itt, a részletezőn érvényes – a listán a
     *   `null` hely egyszerűen üresen marad (T-204, T-208).
     * @property missingFields a hiányzó mezők, kizárólag [VerificationStatus.INCOMPLETE]
     *   esetén nem üres (lásd [LibraryDetailViewModel] dokumentációja: a jelenlegi
     *   validátor ezt kizárólag a lógó képhivatkozás miatt állítja be).
     */
    data class Content(
        val id: TechniqueId,
        val romaji: String,
        val hungarian: String,
        val japaneseScript: String?,
        val aliases: List<String>,
        val descriptionHu: String?,
        val illustration: IllustrationRef?,
        val categoryId: CategoryId,
        val categoryNameHu: String,
        val status: VerificationStatus,
        val missingFields: List<TechniqueField>,
        override val datasetTrust: DatasetTrust,
    ) : LibraryDetailUiState

    /**
     * A betöltés kivétellel elhasalt, vagy a technika nem létezik ebben a tudástárban
     * (FUNCTIONAL_SPEC 10.).
     *
     * `@StringRes`, nem `String`: a kivétel angol `message`-e nem juthat a felhasználó
     * elé (NFR-02).
     */
    data class Error(
        @StringRes val messageRes: Int,
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED,
    ) : LibraryDetailUiState
}
