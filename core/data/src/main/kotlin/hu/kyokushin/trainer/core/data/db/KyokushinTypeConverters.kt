package hu.kyokushin.trainer.core.data.db

import androidx.room.TypeConverter
import hu.kyokushin.trainer.core.model.CategoryId
import hu.kyokushin.trainer.core.model.DatasetTrust
import hu.kyokushin.trainer.core.model.QuestionId
import hu.kyokushin.trainer.core.model.QuestionType
import hu.kyokushin.trainer.core.model.SelfRating
import hu.kyokushin.trainer.core.model.SessionId
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.model.VerificationStatus
import java.time.Instant

/**
 * A Room oszlop-konverterei (DR-08, ARCHITECTURE 5.2).
 *
 * Két szabály vezeti az egész készletet:
 *
 * 1. **Időbélyeg → `Long` epoch milli.** Egyetlen, zónafüggetlen, rendezhető ábrázolás.
 *    A `TEXT` ISO-8601 olvashatóbb lenne naplóban, de rendezéskor és
 *    intervallumlekérdezésnél karakteres összehasonlítást jelentene, és a formátum
 *    megváltoztatása néma adatromlás lenne. Milliszekundum felbontás: a nanoszekundumos
 *    rész elvész, és ez tudatos – a felhasználói esemény időpontja (válasz, önértékelés)
 *    sosem igényel ennél finomabb felbontást, az `Instant.toEpochMilli()` pedig a teljes
 *    tárolható időtartományt lefedi.
 *
 * 2. **Enum → a `name`, soha nem az `ordinal`.** Az `ordinal` az enum-konstansok
 *    *sorrendjére* köt, így egy új érték beszúrása vagy két érték átrendezése csendben
 *    átértelmezné a már eltárolt sorokat (egy `KNEW` önértékelés `NEEDS_PRACTICE`-szé
 *    válna, migráció nélkül, hibajelzés nélkül). A `name` a konstans átnevezésére
 *    érzékeny – az viszont fordítási hiba itt, nem néma adatromlás, és migrációval
 *    kezelhető (DR-08).
 *
 * A visszaolvasás **szándékosan dob**, ha ismeretlen enum-nevet vagy üres azonosítót
 * talál: az adatbázis tartalma így nem csúszhat át hallgatólagosan egy „alapértelmezett"
 * értékre. A hitelesítési státusz esetében ez CR-02 kérdése is – egy olvashatatlan
 * státuszt nem szabad `VERIFIED`-re vagy bármi engedékenyebbre értelmezni.
 *
 * Nem `object`, hanem példányosítható osztály: a Room így maga tudja létrehozni, a
 * unit teszt pedig ugyanazt a példányt használja, amit a generált kód.
 */
internal class KyokushinTypeConverters {
    // --- Időbélyeg ---------------------------------------------------------------

    /** `Instant` → epoch milli. `null` marad `null` (pl. `quiz_session.finishedAt`, FR-19). */
    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    /** Epoch milli → `Instant`. */
    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    // --- Enumok (a `name` megy ki, nem az `ordinal`) -----------------------------

    @TypeConverter
    fun fromQuestionType(value: QuestionType): String = value.name

    @TypeConverter
    fun toQuestionType(value: String): QuestionType = enumByName(value, "QuestionType")

    @TypeConverter
    fun fromSelfRating(value: SelfRating): String = value.name

    @TypeConverter
    fun toSelfRating(value: String): SelfRating = enumByName(value, "SelfRating")

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus): String = value.name

    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus =
        enumByName(value, "VerificationStatus")

    @TypeConverter
    fun fromDatasetTrust(value: DatasetTrust): String = value.name

    @TypeConverter
    fun toDatasetTrust(value: String): DatasetTrust = enumByName(value, "DatasetTrust")

    @TypeConverter
    fun fromIllustrationKind(value: IllustrationKind?): String? = value?.name

    @TypeConverter
    fun toIllustrationKind(value: String?): IllustrationKind? =
        value?.let { enumByName<IllustrationKind>(it, "IllustrationKind") }

    // --- Azonosító value classok -------------------------------------------------
    //
    // A value class konstruktora privát: példány kizárólag az `of()` gyáron keresztül
    // keletkezhet, így az adatbázisból visszaolvasott üres vagy csak whitespace
    // azonosító nem kerülhet be a domainbe – kivételt kap, nem néma rossz adatot.

    @TypeConverter
    fun fromTechniqueId(value: TechniqueId): String = value.value

    @TypeConverter
    fun toTechniqueId(value: String): TechniqueId = TechniqueId.of(value)

    @TypeConverter
    fun fromCategoryId(value: CategoryId): String = value.value

    @TypeConverter
    fun toCategoryId(value: String): CategoryId = CategoryId.of(value)

    @TypeConverter
    fun fromSessionId(value: SessionId): String = value.value

    @TypeConverter
    fun toSessionId(value: String): SessionId = SessionId.of(value)

    @TypeConverter
    fun fromQuestionId(value: QuestionId): String = value.value

    @TypeConverter
    fun toQuestionId(value: String): QuestionId = QuestionId.of(value)
}

/**
 * Enum visszaolvasás a konstans **neve** alapján, beszédes hibával.
 *
 * A `enumValueOf` önmagában `IllegalArgumentException`-t dob, de a szövegében nem
 * szerepel, melyik oszlop melyik értéke volt olvashatatlan – egy adatbázishibánál éppen
 * ez a használható információ.
 */
private inline fun <reified T : Enum<T>> enumByName(value: String, typeName: String): T =
    enumValues<T>().firstOrNull { it.name == value }
        ?: throw IllegalArgumentException(
            "Ismeretlen $typeName érték az adatbázisban: '$value'. " +
                "Enum-konstans átnevezése explicit Room migrációt igényel (DR-08).",
        )
