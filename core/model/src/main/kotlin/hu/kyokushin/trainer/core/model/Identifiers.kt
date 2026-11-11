package hu.kyokushin.trainer.core.model

/**
 * A tudástár egy technikájának stabil azonosítója.
 *
 * Miért külön típus, és nem `String`?
 * A DR-03 követelmény szerint a helyes válasz összehasonlítása **technikaazonosítóval**
 * történik, soha nem szöveges névegyezéssel. Ha az azonosító nyers `String` lenne, egy
 * `TechniqueId` és egy `CategoryId` felcserélése csendben lefordulna és futásidejű hibát
 * adna. Így viszont fordítási hiba (DR-01, DR-03).
 *
 * A konstruktor privát: példány kizárólag a [of] gyáron keresztül jöhet létre, így az
 * üres vagy csak whitespace azonosító nem kerülhet be a domainbe. A [value] olvasható,
 * mert a perzisztencia- és importrétegnek szükséges, de a dobozolás nem kerülhető meg.
 */
@JvmInline
value class TechniqueId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        /**
         * Normalizált (trimmelt) azonosítót készít a nyers értékből.
         *
         * @throws IllegalArgumentException ha [raw] üres vagy csak whitespace.
         */
        fun of(raw: String): TechniqueId = TechniqueId(raw.requireIdentifier("TechniqueId"))
    }
}

/**
 * Egy technikakategória stabil azonosítója (pl. ütések, rúgások, állások).
 *
 * Külön típus a [TechniqueId]-tól: a kettő összekeverése a kvízmotor jelöltszűrésében
 * néma logikai hibát okozna (DR-03). A kategóriaazonosító a kanonikus tartalmi sémából
 * származik (ADR-011), nem a UI-ból.
 */
@JvmInline
value class CategoryId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        /**
         * Normalizált (trimmelt) azonosítót készít a nyers értékből.
         *
         * @throws IllegalArgumentException ha [raw] üres vagy csak whitespace.
         */
        fun of(raw: String): CategoryId = CategoryId(raw.requireIdentifier("CategoryId"))
    }
}

/**
 * Egy kvízmenet (session) azonosítója.
 *
 * A DR-04 szerint a `QuizSession` és a hozzá tartozó `QuizAttempt` rekordok Room-ban
 * tárolódnak, és a kísérletek ezzel az azonosítóval kapcsolódnak a menethez. A seedelt
 * determinizmust (ADR-007) is a menet azonosítója mellett megőrzött seed biztosítja.
 */
@JvmInline
value class SessionId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        /**
         * Normalizált (trimmelt) azonosítót készít a nyers értékből.
         *
         * @throws IllegalArgumentException ha [raw] üres vagy csak whitespace.
         */
        fun of(raw: String): SessionId = SessionId(raw.requireIdentifier("SessionId"))
    }
}

/**
 * Egy kvízmeneten belüli kérdés azonosítója.
 *
 * A kérdések csak azonosítókat tartanak, nem előformázott szöveget vagy képet
 * (ADR-006), ezért a kérdést a saját azonosítója, nem a megjelenített tartalma
 * azonosítja. Erre épül a „egy kérdésre pontosan egy válasz" kikényszerítése is.
 */
@JvmInline
value class QuestionId private constructor(val value: String) {

    override fun toString(): String = value

    companion object {
        /**
         * Normalizált (trimmelt) azonosítót készít a nyers értékből.
         *
         * @throws IllegalArgumentException ha [raw] üres vagy csak whitespace.
         */
        fun of(raw: String): QuestionId = QuestionId(raw.requireIdentifier("QuestionId"))
    }
}

/**
 * Az azonosítótípusok közös bemenet-ellenőrzése: trimmel, és elutasítja az üres értéket.
 *
 * Belső segédfüggvény – szándékosan nem publikus, hogy ne váljon általános
 * sztringnormalizáló eszközzé a domainben.
 */
private fun String.requireIdentifier(typeName: String): String {
    val normalized = trim()
    require(normalized.isNotEmpty()) { "$typeName nem lehet üres vagy csak whitespace." }
    return normalized
}
