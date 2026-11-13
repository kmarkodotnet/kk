package hu.kyokushin.trainer.core.model

/**
 * A kvíz kérdéstípusai.
 *
 * A megnevezés mindig `<kérdés> _TO_ <válasz>` irányú: az első fele az, amit a
 * felhasználó látni fog, a második fele az, amit választania kell. A típus dönti el,
 * hogy egy technika milyen mezői kötelezőek ahhoz, hogy a technika egyáltalán kvízképes
 * legyen (ADR-010 `hasFieldsFor`), és azt is, hogy a válaszopciók szövegek vagy
 * illusztrációk.
 *
 * A kérdés soha nem előformázott szöveget hordoz, hanem azonosítókat (ADR-006), ezért a
 * típus a megjelenítés egyetlen bemenete a renderelő oldalon.
 */
enum class QuestionType {
    /** Japán (romaji) név látszik, a magyar jelentést kell kiválasztani. */
    JP_TO_HU,

    /** Magyar jelentés látszik, a japán (romaji) nevet kell kiválasztani. */
    HU_TO_JP,

    /**
     * Illusztráció látszik, a japán (romaji) nevet kell kiválasztani.
     * Csak olyan technikán jöhet létre, amelynek érvényes képhivatkozása van (ADR-003).
     */
    IMAGE_TO_JP,

    /**
     * Japán (romaji) név látszik, a helyes illusztrációt kell kiválasztani.
     * Minden opcióhoz érvényes kép kell, és a képek akadálymentes leírása nem fedheti
     * fel a technika nevét – ott pozíció alapú leírás jár.
     */
    JP_TO_IMAGE,
}
