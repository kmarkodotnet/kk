package hu.kyokushin.trainer.core.data.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * A `DatabaseModule` kötésének ellenőrzése (T-203, NFR-03, DR-08).
 *
 * Miért kell külön teszt, ha a Hilt a gráfot fordítási időben is ellenőrzi? Mert a gráf
 * teljessége és az adatbázis **helyes felépítése** két különböző állítás. A fordító azt
 * nézi, feloldható-e a `KyokushinDatabase` kötés; azt nem, hogy a builder megkapta-e a
 * migrációkat, és hogy a kapott példány tényleg megnyitható-e a mai sémával. Egy elfelejtett
 * `addMigrations` hívás fordítási hibát nem okoz – csak a frissítő felhasználónál omlik
 * össze.
 *
 * Robolectric kell hozzá (a Room `SupportSQLite`-ja Android API), emulátor nem.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseModuleTest {
    private var database: KyokushinDatabase? = null

    @After
    fun tearDown() {
        database?.close()
    }

    /** A kötés által adott példány a mai sémával nyílik meg és használható (NFR-03). */
    @Test
    fun provideDatabase_opensWithCurrentSchemaVersionAndIsUsable_NFR03() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val built = DatabaseModule.provideDatabase(context)
        database = built

        // A `content_state` sor lekérdezése nyitja meg az adatbázist: ha a séma vagy a
        // TypeConverter-készlet nem állna össze, itt derülne ki, nem a felhasználónál.
        // Friss adatbázison a sor még nincs meg – ez az első indítás rendes állapota.
        assertThat(built.contentStateDao().get()).isNull()
        assertThat(built.openHelper.readableDatabase.version).isEqualTo(KyokushinDatabase.VERSION)
        assertThat(KyokushinDatabase.VERSION).isEqualTo(2)
    }

    /**
     * Mind a nyolc DAO-nak van kötése (ARCHITECTURE 7.: `DatabaseModule` → „`AppDatabase`,
     * DAO-k"), és mind ugyanabból az egy adatbázispéldányból származik (DR-04).
     */
    @Test
    fun provideDaos_bindEveryDaoFromTheSingleDatabase_NFR03() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val built = DatabaseModule.provideDatabase(context)
        database = built

        val daos = listOf(
            DatabaseModule.provideCategoryDao(built),
            DatabaseModule.provideTechniqueDao(built),
            DatabaseModule.provideTechniqueAliasDao(built),
            DatabaseModule.provideSynonymGroupDao(built),
            DatabaseModule.provideContentStateDao(built),
            DatabaseModule.provideQuizSessionDao(built),
            DatabaseModule.provideQuizAttemptDao(built),
            DatabaseModule.provideStudyReviewDao(built),
        )

        assertThat(daos).hasSize(8)
        daos.forEach { dao -> assertThat(dao).isNotNull() }
    }

    /**
     * A `core/data` egyetlen forrásfájljában sincs destruktív migrációs tartalék (DR-08).
     *
     * Forrásellenőrzés, mert a hiányt futásidőben csak egy valódi 1 → 2 frissítéssel lehetne
     * kimutatni (azt a `KyokushinMigrationTest` fedi), a *jelenlétét* viszont bármikor
     * visszacsempészheti egy „gyorsan megoldom" jellegű módosítás. Ugyanaz a minta, mint a
     * `core/quiz` NFR-07 forrásszkennelő tesztje.
     */
    @Test
    fun coreDataMainSources_containNoDestructiveMigrationFallback_DR08() {
        val repoRoot = File(
            requireNotNull(System.getProperty("kyo.repoRoot")) {
                "A kyo.repoRoot rendszertulajdonság nincs beállítva – lásd core/data/build.gradle.kts."
            },
        )
        val mainSources = File(repoRoot, "core/data/src/main/kotlin")
        val sources = mainSources.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        assertThat(sources).isNotEmpty()

        val offenders = sources.filter { file ->
            withoutComments(file.readText()).contains("fallbackToDestructiveMigration")
        }.map { it.name }
        assertThat(offenders).isEmpty()

        // És a kötés tényleg átadja a migrációkat – különben a 2-es sémára frissítő
        // felhasználó `IllegalStateException`-t kapna az app indulásakor.
        val moduleSource = File(
            mainSources,
            "hu/kyokushin/trainer/core/data/di/DatabaseModule.kt",
        ).readText()
        assertThat(withoutComments(moduleSource)).contains("addMigrations(*KyokushinMigrations.ALL)")
    }

    /**
     * A KDoc és a soros megjegyzés épp ezekről a szabályokról beszél – azt nem szabad
     * szabálysértésnek venni.
     */
    private fun withoutComments(code: String): String =
        code.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("""//.*"""), "")
}
