package hu.kyokushin.trainer.feature.progress.settings

import hu.kyokushin.trainer.core.model.DatasetTrust

/**
 * A Beállítások képernyő (`settings`) állapota (FUNCTIONAL_SPEC 6.3, FR-32, FR-35, NFR-09,
 * US-19, T-507).
 *
 * Két, egymástól teljesen független adatforrásból épül (lásd [ProgressSettingsViewModel]
 * KDoc-ja): a [Content] tartalominfó-mezői (`schemaVersion`…`categoryCount`) a
 * `TechniqueRepository`-ból jönnek, a [Content.clearStep]/[Content.isClearing] pedig a
 * „Előrehaladás törlése" UI-csak állapota. A kettő soha nem hat egymásra – a törlés
 * megerősítése a tartalominfót **nem** változtatja meg (FR-35 „Kész ha": „A törlés nem
 * érinti a tudástárat").
 */
sealed interface ProgressSettingsUiState {
    /** A `TechniqueRepository` `Flow`-jai még nem adtak értéket. */
    data object Loading : ProgressSettingsUiState

    /**
     * @property schemaVersion a betöltött tudástár séma-verziója (FUNCTIONAL_SPEC 6.3)
     *   – `null`, ha még nem futott import ([hu.kyokushin.trainer.core.data.repository.ContentState.NotImported])
     *   vagy a manifestből hiányzott.
     * @property contentVersion a betöltött tartalom verziója (`dataset_version`) –
     *   ugyanazokkal a `null` esetekkel, mint [schemaVersion].
     * @property datasetTrust a tudástár bizalmi szintje (ADR-010) – ez adja a
     *   „szerkesztői állapot" kijelzést (lektorált / lektorálatlan gyakorlóanyag).
     * @property recordCount az aktuálisan megjeleníthető (nem visszavont) technikák
     *   száma – [hu.kyokushin.trainer.core.data.repository.ContentState.activeTechniqueCount].
     * @property categoryCount a kategóriák száma
     *   ([hu.kyokushin.trainer.core.data.repository.TechniqueRepository.observeCategories]
     *   mérete).
     * @property clearStep az „Előrehaladás törlése" kétlépcsős megerősítésének jelenlegi
     *   lépése (FR-35) – [ClearProgressStep.NONE] esetén nincs nyitott párbeszéd.
     * @property isClearing igaz, amíg a
     *   [hu.kyokushin.trainer.core.data.repository.ProgressRepository.clearAllProgress]
     *   hívás fut – a végső megerősítő gomb ezalatt letiltva, hogy ne lehessen kétszer
     *   elindítani.
     */
    data class Content(
        val schemaVersion: String?,
        val contentVersion: String?,
        val datasetTrust: DatasetTrust,
        val recordCount: Int,
        val categoryCount: Int,
        val clearStep: ClearProgressStep,
        val isClearing: Boolean,
    ) : ProgressSettingsUiState
}

/**
 * Az „Előrehaladás törlése" kétlépcsős megerősítésének lépései (FUNCTIONAL_SPEC 6.3,
 * FR-35: „kétlépcsős megerősítés, pontos felsorolással, hogy mi törlődik").
 *
 * A két lépés **tartalmilag** különbözik, nem csak számban: [FIRST_WARNING] sorolja fel,
 * mi törlődik (kvízpróbálkozások, munkamenetek, önértékelések) és mi NEM (a tudástár),
 * [FINAL_WARNING] a végső, rövidebb „biztosan?" megerősítés – mindkettő kimondja, hogy a
 * művelet nem visszavonható (T-507 „Kész ha" 1.).
 */
enum class ClearProgressStep {
    /** Nincs nyitott megerősítő párbeszéd. */
    NONE,

    /** Az első párbeszéd: mi törlődik, mi nem, és hogy a művelet nem visszavonható. */
    FIRST_WARNING,

    /** A végső, megerősítő párbeszéd – innen indul a tényleges törlés. */
    FINAL_WARNING,
}
