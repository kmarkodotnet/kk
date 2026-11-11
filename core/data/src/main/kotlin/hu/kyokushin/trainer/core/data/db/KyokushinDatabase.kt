package hu.kyokushin.trainer.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Az alkalmazás egyetlen Room adatbázisa (DR-04, ARCHITECTURE 5.2).
 *
 * Egy adatbázis, két logikai tábla-csoport:
 *
 * | Csoport | Táblák | Életciklus |
 * |---|---|---|
 * | tartalom | `category`, `technique`, `technique_alias`, `synonym_group`, `content_state` | tartalomfrissítéskor upserttel újraírható |
 * | felhasználói | `quiz_session`, `quiz_attempt`, `study_review` | **soha nem törlődik** tartalomfrissítéskor (DR-07) |
 *
 * Miért egy adatbázis és nem kettő? Mert a tartalomimportnak és az állapotfrissítésnek
 * egyetlen tranzakcióban kell futnia, és a Haladás képernyő aggregált statisztikái
 * (T-501) a felhasználói sorokat a technika kategóriájával összekapcsolva kérdezik –
 * két adatbázisfájl között `JOIN` nem lehetséges. Az életciklus-szétválasztást nem a
 * fájlhatár, hanem az idegen kulcsok szándékos **hiánya** biztosítja (ADR-004).
 *
 * ## Migráció (DR-08)
 *
 * `version = 2`. A séma JSON a `core/data/schemas/` alá exportálódik
 * (`room.schemaLocation`, lásd `core/data/build.gradle.kts`), és **verziókövetett**: az
 * `1.json` az összehasonlítás alapja a `2.json`-hoz.
 *
 * | Verzió | Változás | Migráció | ADR |
 * |---|---|---|---|
 * | 1 | az első séma | — | — |
 * | 2 | `technique.search_romaji`, `technique.search_hungarian` (SQL-oldali keresés, FR-03) | [KyokushinMigrations.MIGRATION_1_2] | ADR-012 |
 *
 * **`fallbackToDestructiveMigration()` használata tilos.** Nem azért, mert kényelmetlen,
 * hanem mert pontosan azt a felhasználói adatot törölné, amit a DR-07 és az ADR-004
 * védeni hivatott: egy elfelejtett migráció csendben, hibajelzés nélkül nullázná a
 * felhasználó előrehaladását. A helyes viselkedés a hangos hiba. Az adatbázist építő
 * Hilt kötés (T-203) ezért `addMigrations(...)`-t kap, destruktív tartalékot nem.
 * Minden `version` emelés explicit `Migration`-t és ADR-t igényel – a migrációk a
 * [KyokushinMigrations] alatt, egy helyen élnek.
 */
@Database(
    version = KyokushinDatabase.VERSION,
    exportSchema = true,
    entities = [
        // tartalom
        CategoryEntity::class,
        TechniqueEntity::class,
        TechniqueAliasEntity::class,
        SynonymGroupMemberEntity::class,
        ContentStateEntity::class,
        // felhasználói
        QuizSessionEntity::class,
        QuizAttemptEntity::class,
        StudyReviewEntity::class,
    ],
)
@TypeConverters(KyokushinTypeConverters::class)
internal abstract class KyokushinDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao

    abstract fun techniqueDao(): TechniqueDao

    abstract fun techniqueAliasDao(): TechniqueAliasDao

    abstract fun synonymGroupDao(): SynonymGroupDao

    abstract fun contentStateDao(): ContentStateDao

    abstract fun quizSessionDao(): QuizSessionDao

    abstract fun quizAttemptDao(): QuizAttemptDao

    abstract fun studyReviewDao(): StudyReviewDao

    internal companion object {
        /**
         * A séma verziója. Emeléskor: explicit `Migration` + ADR (DR-08).
         *
         * 2 – a `technique` két keresőoszlopa (`search_romaji`, `search_hungarian`),
         * [KyokushinMigrations.MIGRATION_1_2], ADR-012.
         */
        const val VERSION: Int = 2

        /** Az adatbázisfájl neve. Egy fájl, egy adatbázis az app teljes élettartamára. */
        const val FILE_NAME: String = "kyokushin.db"
    }
}
