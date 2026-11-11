package hu.kyokushin.trainer.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A séma 1 → 2 migrációja (DR-08, ADR-012, FR-03, FR-33).
 *
 * Két dolgot bizonyít, és mindkettő a `fallbackToDestructiveMigration()` tilalmának a
 * lényege:
 *
 * 1. **A migráció érvényes sémát hoz létre.** A
 *    [MigrationTestHelper.runMigrationsAndValidate] a `2.json` exporthoz hasonlítja a
 *    migrált adatbázist; egy elfelejtett `ADD COLUMN` itt bukik el, nem a felhasználó
 *    készülékén.
 * 2. **A felhasználói adat megmarad** (DR-07, FR-33). A `quiz_session`, `quiz_attempt` és
 *    `study_review` sorok a migráció után is ott vannak – a `search_*` oszlopok
 *    felvétele nem nyúl hozzájuk.
 *
 * A [MigrationTestHelper] a séma JSON-okat az **assets**-ből olvassa, ezért a
 * `core/data/schemas/` könyvtár a `test` source set assetjeiként van bekötve (lásd
 * `core/data/build.gradle.kts`). Így a teszt Robolectric alatt, emulátor nélkül fut –
 * a leszállított APK-ba viszont nem kerül bele a sémaexport.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KyokushinMigrationTest {

    /** Az átmeneti adatbázisfájl – nem az éles `kyokushin.db`. */
    private val databaseFile = File(System.getProperty("java.io.tmpdir"), DATABASE_FILE_NAME)

    /**
     * A migrációt egy **fájl alapú**, átmeneti adatbázison futtatjuk: a migrációnak van
     * „előtte" és „utána" állapota, amit in-memory adatbázissal nem lehet megfogni.
     *
     * A fájl utat közvetlenül adjuk át a segítőnek (és nem adatbázis-névként): a név
     * alapú, `SupportSQLiteOpenHelper`-es ága Robolectric alatt a név és az
     * `…/databases/<név>` abszolút út összetévesztése miatt elhasal
     * („This driver is configured to open a database named … but … was requested").
     */
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        databaseFile,
        AndroidSQLiteDriver(),
        KyokushinDatabase::class,
    )

    /**
     * Egy előző futtatás fájlja már a 2-es verzión állna, és a `createDatabase(1)` ekkor
     * 2 → 1 visszamigrálást kérne (ami nem létezik). A segítő nem törli a fájlt, ezért
     * minden futtatás tiszta lapról indul – a WAL és a naplófájlokkal együtt.
     */
    @Before
    fun deleteLeftoverDatabaseFiles() = deleteDatabaseFiles()

    @After
    fun cleanUpDatabaseFiles() = deleteDatabaseFiles()

    private fun deleteDatabaseFiles() {
        listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
            File(databaseFile.parentFile, databaseFile.name + suffix).delete()
        }
    }

    @Test
    fun migration1To2_addsSearchColumnsAndKeepsUserProgress_DR08() {
        helper.createDatabase(1).use { db ->
            db.execSQL(
                "INSERT INTO category (id, nameHu, sortOrder) VALUES ('dachi', 'Állások', 0)",
            )
            db.execSQL(
                """
                INSERT INTO technique (
                    id, categoryId, romaji, romajiAscii, hungarian, japaneseScript,
                    descriptionHu, status, quizExcluded, retired, datasetVersion
                ) VALUES (
                    'KY-0001', 'dachi', 'Heisoku Dachi', 'Heisoku Dachi', 'Zárt lábú állás',
                    NULL, NULL, 'AWAITING_REVIEW', 0, 0, '0.1.0'
                )
                """.trimIndent(),
            )
            db.execSQL(
                """
                INSERT INTO content_state (
                    id, schemaVersion, contentVersion, datasetTrust, importedAt,
                    acceptedCount, rejectedCount, blockingIssueCount, warningIssueCount,
                    datasetRejected
                ) VALUES (
                    1, '1.0.0', '0.1.0', 'PRACTICE_ON_UNREVIEWED', 1762000000000, 1, 0, 0, 0, 0
                )
                """.trimIndent(),
            )
            // Felhasználói adat a migráció előtt: ez az, amit egy destruktív tartalék
            // csendben eltüntetne (DR-07, FR-33).
            db.execSQL(
                "INSERT INTO quiz_session (id, startedAt, finishedAt, requestedCount, " +
                    "answeredCount, seed) VALUES ('S-1', 1762000000000, NULL, 10, 1, 42)",
            )
            db.execSQL(
                "INSERT INTO quiz_attempt (sessionId, questionId, techniqueId, questionType, " +
                    "selectedTechniqueId, correct, answeredAt) VALUES " +
                    "('S-1', 'q-1', 'KY-0001', 'JP_TO_HU', 'KY-0001', 1, 1762000000001)",
            )
            db.execSQL(
                "INSERT INTO study_review (techniqueId, selfRating, reviewedAt) VALUES " +
                    "('KY-0001', 'KNOWN', 1762000000002)",
            )
        }

        // A migráció lefut, és a Room a `2.json` exporthoz validálja az eredményt.
        helper.runMigrationsAndValidate(2, listOf(KyokushinMigrations.MIGRATION_1_2)).use { db ->
            // 1. A két új oszlop létezik, és az áthidaló visszatöltés megtörtént: a
            // `romajiAscii` már diakritikus jel nélküli alak, ezért a `lower()` pontosan a
            // `NameNormalizer` kimenetét adja.
            assertThat(db.text("SELECT search_romaji FROM technique WHERE id = 'KY-0001'"))
                .isEqualTo("heisoku dachi")

            // 2. A verziókapu nyitva: a következő import újra felviszi a tartalmat, és a
            // `search_hungarian` oszlopot a normalizáló kimenetével írja felül.
            assertThat(db.text("SELECT contentVersion FROM content_state WHERE id = 1")).isNull()

            // 3. A felhasználói sorok érintetlenek (FR-33).
            assertThat(db.count("SELECT COUNT(*) FROM quiz_session")).isEqualTo(1)
            assertThat(db.count("SELECT COUNT(*) FROM quiz_attempt")).isEqualTo(1)
            assertThat(db.count("SELECT COUNT(*) FROM study_review")).isEqualTo(1)
            assertThat(db.text("SELECT techniqueId FROM study_review")).isEqualTo("KY-0001")
        }
    }

    /** Egyetlen szöveges érték az első sor első oszlopából, `NULL` esetén `null`. */
    private fun SQLiteConnection.text(sql: String): String? = prepare(sql).use { statement ->
        check(statement.step()) { "A lekérdezés egyetlen sort sem adott: $sql" }
        if (statement.isNull(0)) null else statement.getText(0)
    }

    /** Egyetlen szám az első sor első oszlopából. */
    private fun SQLiteConnection.count(sql: String): Int = prepare(sql).use { statement ->
        check(statement.step()) { "A lekérdezés egyetlen sort sem adott: $sql" }
        statement.getLong(0).toInt()
    }

    private companion object {
        /** A fájlnév a konstruktorban és a takarításban is kell, ezért konstans. */
        const val DATABASE_FILE_NAME: String = "kyo-migration-test.db"
    }
}
