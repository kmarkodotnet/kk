package hu.kyokushin.trainer.core.model

/**
 * A betöltött tudástár egészének bizalmi szintje (ADR-010).
 *
 * Ez a kvíz jogosultsági kapujának egyik bemenete:
 * ```
 * quizEligible = status != INCOMPLETE && hasFieldsFor(questionType) &&
 *     (datasetTrust == REVIEWED || datasetTrust == PRACTICE_ON_UNREVIEWED)
 * ```
 *
 * A bizalmi szint **a tartalomból** származik – a manifest szerkesztői állapotából –,
 * nem felhasználói beállításból: a tartalom dönti el, nem a felhasználó. Ezért nincs és
 * nem is lehet olyan kódút, amely felhasználói bemenetből állítaná be, és a hozzá tartozó
 * figyelmeztető jelölés nem rejthető el és nem kapcsolható ki.
 */
enum class DatasetTrust {
    /**
     * Lektorált tartalom. A „lektorálatlan gyakorlóanyag" jelölések nem jelennek meg –
     * kódmódosítás nélkül, puszta tartalomcserétől (AC-08).
     */
    REVIEWED,

    /**
     * Lektorálatlan tartalom, amelyen a gyakorlás engedett, de az alkalmazás végig,
     * feltűnően jelzi: „Lektorálatlan gyakorlóanyag – a tartalom szakmai ellenőrzésre vár."
     */
    PRACTICE_ON_UNREVIEWED,
}
