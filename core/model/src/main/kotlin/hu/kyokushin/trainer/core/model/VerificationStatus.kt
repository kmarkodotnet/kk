package hu.kyokushin.trainer.core.model

/**
 * Egy technikarekord hitelesítési állapota a domainben (ADR-002).
 *
 * A csomagolt JSON sémában `verified: Boolean` szerepel; a harmadik állapotot
 * ([INCOMPLETE]) a validátor **származtatja** hiányzó kötelező mezőből vagy érvénytelen
 * képhivatkozásból. A háromállapotú enum azért kell a két boolean helyett, mert a UI-nak
 * három különböző üzenetet kell adnia (FUNCTIONAL_SPEC 2.2), és mert az enum kizárja az
 * értelmetlen kombinációkat.
 *
 * A státuszt kód, import vagy migráció **soha nem emelheti** (CR-02): az
 * [INCOMPLETE] → [VERIFIED] átmenet programból nem létezik, [VERIFIED] kizárólag
 * emberi döntés nyomán, a tartalomban keletkezhet.
 *
 * Perzisztálásra az enum **neve** megy (nem az `ordinal`), hogy az értékek átrendezése
 * ne rontsa el a meglévő adatot.
 */
enum class VerificationStatus {

    /** Szakmailag ellenőrzött rekord. A UI nem tesz rá jelölőt. */
    VERIFIED,

    /** Ép, de még nem lektorált rekord. UI-jelölő: „Ellenőrzésre vár". */
    AWAITING_REVIEW,

    /**
     * Hiányos vagy hibás hivatkozású rekord. UI-jelölő: „Hiányos" + a hiányzó mezők.
     * Hiányos rekord **soha** nem kvízképes, bármilyen is a tartalom bizalmi szintje
     * (ADR-010).
     */
    INCOMPLETE,
}
