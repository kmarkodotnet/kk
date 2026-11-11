package hu.kyokushin.trainer.core.data.repository

import hu.kyokushin.trainer.core.model.DatasetTrust
import java.time.Instant

/**
 * A betöltött tudástár állapota – a `content_state` tábla **domain olvasata**
 * (ARCHITECTURE 5.2/5.3, FUNCTIONAL_SPEC 6.3, FR-32).
 *
 * Ez a [TechniqueRepository.observeContentState] visszatérési típusa, és három, egymástól
 * független fogyasztója van:
 *
 * 1. a **Tudástár** képernyő: van-e egyáltalán megjeleníthető tartalom ([hasContent]) –
 *    ebből lesz az „Empty – nincs tartalom" állapot (FUNCTIONAL_SPEC 7.1),
 * 2. a **kvíz**: a [datasetTrust] az ADR-010 jogosultsági kapu egyik bemenete, és ez
 *    dönti el, hogy a „Lektorálatlan gyakorlóanyag" jelölés megjelenik-e (AC-08),
 * 3. a **Beállítások**: `schemaVersion`, `contentVersion`, rekordszám (FUNCTIONAL_SPEC 6.3).
 *
 * ## Miért sealed, és miért nem nullázható mezős data class?
 *
 * Mert a „még egyetlen import sem futott" az első indítás **rendes** állapota, nem hiba és
 * nem is „üres tartalom": ilyenkor nincs verzió, nincs időbélyeg és nincs validációs
 * mérleg, amit ki lehetne írni. Egyetlen data classban ez négy-öt `null` lenne, és minden
 * hívónak magának kellene kitalálnia, hogy a `contentVersion == null` most „nem futott
 * import" vagy „futott, de a manifestből hiányzott a verzió" (az [Imported] ágon ez
 * utóbbi is lehetséges). A sealed hierarchia ezt a különbséget fordítási szintre emeli.
 *
 * ## Amit ez a típus NEM tartalmaz
 *
 * A tételes validációs észrevételeket. Azok nem perzisztálódnak (lásd a `content_state`
 * tábla leírását); a fejlesztői diagnosztika a
 * [hu.kyokushin.trainer.core.data.content.ContentImporter.lastValidationReport] jelentést
 * kapja. Itt csak **összegzés** van, mert a Beállítások képernyőnek is csak az kell.
 */
sealed interface ContentState {

    /**
     * A tudástár bizalmi szintje (ADR-010, CR-02).
     *
     * **Soha nem emelkedik kódból**, és ismeretlen állapotban a szigorúbb olvasat
     * érvényes: amíg nincs betöltött, elfogadott tartalom, az érték
     * [DatasetTrust.PRACTICE_ON_UNREVIEWED]. Egy „még nem tudjuk" állapotot nem
     * minősíthetünk lektoráltnak.
     */
    val datasetTrust: DatasetTrust

    /**
     * Az **aktuálisan megjeleníthető** (nem visszavont) technikák száma a tartalomtáblákban.
     *
     * Szándékosan nem a legutóbbi import `acceptedCount` mérlege: egy elutasított
     * frissítés után a korábbi tartalom érvényben marad (FR-31), tehát a kettő eltérhet.
     * A UI-nak az érdekli, mi van *most* a táblában.
     */
    val activeTechniqueCount: Int

    /** Van-e megjeleníthető tartalom. `false` → a Tudástár „nincs tartalom" állapota. */
    val hasContent: Boolean
        get() = activeTechniqueCount > 0

    /**
     * Még egyetlen tartalombetöltés sem futott le ebben a telepítésben – az első indítás
     * állapota (FUNCTIONAL_SPEC 7.1).
     *
     * Nem hibaállapot: a hívó `Loading`-ot mutat, amíg a
     * [hu.kyokushin.trainer.core.data.content.ContentImporter.importIfNeeded] le nem fut.
     */
    data object NotImported : ContentState {
        override val datasetTrust: DatasetTrust = DatasetTrust.PRACTICE_ON_UNREVIEWED
        override val activeTechniqueCount: Int = 0
    }

    /**
     * Futott betöltés, és a `content_state` sor megmondja, mi lett a mérlege.
     *
     * @property schemaVersion a feldolgozott `schemaVersion`, vagy `null`, ha a manifestből
     *   hiányzott.
     * @property contentVersion a betöltött `contentVersion`, vagy `null`, ha a manifestből
     *   hiányzott. A tartalomfrissítés idempotenciája ezen az értéken dől el (FR-30).
     * @property importedAt a betöltés időpontja; az injektált `Clock`-ból származik, nem
     *   rendszeróra-hívásból (NFR-07).
     * @property acceptedCount a legutóbbi betöltés során elfogadott rekordok száma.
     * @property rejectedCount a legutóbbi betöltés során elvetett rekordok száma.
     * @property blockingIssueCount a BLOKKOLÓ súlyosságú észrevételek száma.
     * @property warningIssueCount a FIGYELMEZTETŐ súlyosságú észrevételek száma.
     * @property datasetRejected a legutóbbi csomag **egészében** elutasítva (V001 nem
     *   támogatott séma, V010 értelmezhetetlen JSON). Ha `true`, a tartalomtáblákban a
     *   *korábbi* kiadás sorai vannak – ezért létezhet `datasetRejected = true` és
     *   [hasContent] `= true` egyszerre, és ez nem ellentmondás (FR-31).
     */
    data class Imported(
        val schemaVersion: String?,
        val contentVersion: String?,
        override val datasetTrust: DatasetTrust,
        val importedAt: Instant,
        val acceptedCount: Int,
        val rejectedCount: Int,
        val blockingIssueCount: Int,
        val warningIssueCount: Int,
        val datasetRejected: Boolean,
        override val activeTechniqueCount: Int,
    ) : ContentState {
        init {
            require(acceptedCount >= 0 && rejectedCount >= 0) {
                "A ContentState rekordszámai nem lehetnek negatívak."
            }
            require(activeTechniqueCount >= 0) {
                "A ContentState.activeTechniqueCount nem lehet negatív."
            }
        }
    }
}
