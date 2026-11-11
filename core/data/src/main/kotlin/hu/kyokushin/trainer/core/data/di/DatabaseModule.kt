package hu.kyokushin.trainer.core.data.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.data.db.CategoryDao
import hu.kyokushin.trainer.core.data.db.ContentStateDao
import hu.kyokushin.trainer.core.data.db.KyokushinDatabase
import hu.kyokushin.trainer.core.data.db.KyokushinMigrations
import hu.kyokushin.trainer.core.data.db.QuizAttemptDao
import hu.kyokushin.trainer.core.data.db.QuizSessionDao
import hu.kyokushin.trainer.core.data.db.StudyReviewDao
import hu.kyokushin.trainer.core.data.db.SynonymGroupDao
import hu.kyokushin.trainer.core.data.db.TechniqueAliasDao
import hu.kyokushin.trainer.core.data.db.TechniqueDao
import javax.inject.Singleton

/**
 * A Room adatbázis és a DAO-k kötése (ARCHITECTURE 7.: `DatabaseModule`, T-203).
 *
 * ## Miért a `core/data`-ban él, és nem a `di` modulban?
 *
 * Az ARCHITECTURE 7. táblázata a `di` modulhoz rendeli ezt a kötést, a kód valósága viszont
 * ennél szigorúbb: a [KyokushinDatabase] és az összes DAO `internal` láthatóságú
 * (ADR-001, L3/L4 – a Room típusok nem szivároghatnak ki a modulból). Egy `internal` típust
 * csak az a modul tud `@Provides`-szel kötni, amelyik látja, ezért a kötésnek itt kell
 * élnie. A `di` modul a maradékot adja: `Clock`, dispatcherek, RNG, `QuizGenerator`,
 * `KnowledgeValidator`.
 *
 * Ez nem rétegsértés: a Hilt gráf a `SingletonComponent`-ben áll össze, a kötés fizikai
 * helye a modulhatárokat nem mozdítja. Az alternatíva – a DAO-k publikussá tétele a DI
 * kedvéért – pontosan azt a határt nyitná meg, amit az ADR-001 zárva akar tartani.
 * Ugyanez a döntés a T-117 (`@IoDispatcher` qualifier) és a T-201 nyomvonalán.
 *
 * ## Miért nincs destruktív tartalék?
 *
 * [Room.databaseBuilder] `addMigrations(*KyokushinMigrations.ALL)`-t kap és
 * `fallbackToDestructiveMigration()`-t **nem** (DR-08). A séma a 2. verzión áll: egy
 * frissítő felhasználónál a destruktív tartalék csendben, hibajelzés nélkül törölné a
 * `quiz_attempt` és `study_review` sorokat – azt az adatot, amit a DR-07 és az ADR-004
 * védeni hivatott. A helyes viselkedés a hangos hiba, nem a néma adatvesztés.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    /**
     * Az alkalmazás egyetlen adatbázis-példánya.
     *
     * `@Singleton`, mert a Room példány maga tartja a kapcsolatkészletet és a megnyitott
     * `Flow`-k invalidációs követését: két példány két egymásról nem tudó cache-t jelentene,
     * és a Tudástár képernyő nem frissülne a tartalomimport után (FR-30).
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KyokushinDatabase =
        Room.databaseBuilder(
            // Pozicionális argumentumok: a `Room.databaseBuilder` Java statikus metódus,
            // névvel hívni fordítási hiba lenne.
            context,
            KyokushinDatabase::class.java,
            KyokushinDatabase.FILE_NAME,
        )
            .addMigrations(*KyokushinMigrations.ALL)
            .build()

    // A DAO-k kötése azért szerepel külön, mert így a későbbi felhasználók (pl. a haladási
    // repository, T-301) egyetlen DAO-t kérhetnek az egész adatbázis helyett – szűkebb
    // függőség, kisebb tesztfelület. A példányt a Room cache-eli, a `@Singleton` csak ezt
    // teszi explicitté.

    @Provides
    @Singleton
    fun provideCategoryDao(database: KyokushinDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideTechniqueDao(database: KyokushinDatabase): TechniqueDao = database.techniqueDao()

    @Provides
    @Singleton
    fun provideTechniqueAliasDao(database: KyokushinDatabase): TechniqueAliasDao =
        database.techniqueAliasDao()

    @Provides
    @Singleton
    fun provideSynonymGroupDao(database: KyokushinDatabase): SynonymGroupDao =
        database.synonymGroupDao()

    @Provides
    @Singleton
    fun provideContentStateDao(database: KyokushinDatabase): ContentStateDao =
        database.contentStateDao()

    @Provides
    @Singleton
    fun provideQuizSessionDao(database: KyokushinDatabase): QuizSessionDao =
        database.quizSessionDao()

    @Provides
    @Singleton
    fun provideQuizAttemptDao(database: KyokushinDatabase): QuizAttemptDao =
        database.quizAttemptDao()

    @Provides
    @Singleton
    fun provideStudyReviewDao(database: KyokushinDatabase): StudyReviewDao =
        database.studyReviewDao()
}
