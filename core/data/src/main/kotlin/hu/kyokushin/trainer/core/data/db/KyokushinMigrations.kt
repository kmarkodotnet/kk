package hu.kyokushin.trainer.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Az adatbázis **explicit** migrációi (DR-08, ADR-012).
 *
 * Minden `version` emeléshez tartozik egy `Migration` objektum ebben a fájlban, és az
 * adatbázist építő Hilt kötés (T-203) ezt a listát adja át az `addMigrations(...)`-nak.
 * `fallbackToDestructiveMigration()` **tilos**: az pontosan azt a `quiz_session` /
 * `quiz_attempt` / `study_review` tartalmat törölné csendben, amit a DR-07 és az ADR-004
 * védeni hivatott (FR-33). Hiányzó migráció esetén a helyes viselkedés a hangos hiba.
 */
internal object KyokushinMigrations {

    /**
     * 1 → 2: a keresés SQL-oldalra költözése (FR-03, T-202, ADR-012).
     *
     * A `technique` tábla két új, előre normalizált oszlopot kap, hogy a Tudástár keresése
     * `LIKE '%…%'` feltétellel az adatbázisban fusson, ne a beolvasott soron.
     *
     * ## Miért `ALTER TABLE … ADD COLUMN`, és nem tábla-újraépítés?
     *
     * Mert a `technique` sorai megmaradnak, és a rájuk hivatkozó felhasználói rekordok
     * hivatkozása sem szakadhat el (ADR-004). Egy „új tábla + másolás + átnevezés" minta
     * itt semmit nem adna, viszont a `technique_alias` és a `synonym_group` cascade
     * idegen kulcsain keresztül valódi kockázat lenne.
     *
     * ## A visszatöltés két lépése
     *
     * 1. **SQL-oldali áthidalás.** A `search_romaji` pontosan előállítható: a `romajiAscii`
     *    már diakritikus jel nélküli alak, tehát `lower()` ugyanazt adja, mint a
     *    `NameNormalizer`. A `search_hungarian`-nál az SQLite `lower()` a nem ASCII
     *    betűket **nem** kisbetűsíti és az ékezeteket nem bontja le – itt tehát csak
     *    közelítés. Ezt szándékosan nem pótoljuk SQL-ben megírt karaktertáblával: az a
     *    normalizálás második, szétcsúszásra hajlamos definíciója lenne (a `NameNormalizer`
     *    az egyetlen).
     * 2. **A verziókapu nyitása.** A `content_state.contentVersion` `NULL`-ra áll, ezért a
     *    következő [hu.kyokushin.trainer.core.data.content.ContentImporter.importIfNeeded]
     *    hívás nem tér vissza `UpToDate`-tel, hanem újra felviszi a csomagolt tudástárat –
     *    és az upsert a két oszlopot a `NameNormalizer` kimenetével írja felül. Az
     *    áthidaló értékek így az app első indulásáig élnek.
     *
     * A `content_state` **tartalmi** metaadat, nem felhasználói adat: a `contentVersion`
     * kinullázása egyetlen `quiz_attempt`, `quiz_session` vagy `study_review` sort sem
     * érint – a migráció ezekhez a táblákhoz nem is nyúl.
     */
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `technique` ADD COLUMN `search_romaji` TEXT NOT NULL DEFAULT ''",
            )
            db.execSQL(
                "ALTER TABLE `technique` ADD COLUMN `search_hungarian` TEXT NOT NULL DEFAULT ''",
            )
            db.execSQL(
                "UPDATE `technique` SET " +
                    "`search_romaji` = lower(`romajiAscii`), " +
                    "`search_hungarian` = lower(`hungarian`)",
            )
            // A verziókapu nyitása: a következő import a `NameNormalizer` kimenetét írja
            // a két oszlopba. Felhasználói táblát nem érint.
            db.execSQL("UPDATE `content_state` SET `contentVersion` = NULL")
        }
    }

    /** A kötésnek átadandó migrációk, verzió szerint növekvő sorrendben (T-203). */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}
