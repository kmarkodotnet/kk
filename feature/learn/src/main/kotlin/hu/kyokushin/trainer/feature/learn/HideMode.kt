package hu.kyokushin.trainer.feature.learn

/**
 * Melyik nevet rejti el a tanulókártya (FR-07, FUNCTIONAL_SPEC 4.1/3., US-10).
 *
 * Két, egymást kizáró lehetőség van – nincs „semmit se rejt el" harmadik ág, mert a
 * tanulókártya célja éppen a felidézés kikényszerítése, nem a böngészés (az a Tudástár
 * feladata, FUNCTIONAL_SPEC 3.).
 *
 * A `learn/setup` (T-302) és a `learn/cards` (T-303) ViewModel is ezt a típust használja:
 * a beállítás a kártyaállapot bemenete (`docs/backlog/GROUPS.md` G-04).
 */
enum class HideMode {

    /** A japán (romaji) név van elrejtve; a magyar név és az ábra látszik. */
    HIDE_JAPANESE,

    /** A magyar név van elrejtve; a romaji és az ábra látszik. */
    HIDE_HUNGARIAN,
}
