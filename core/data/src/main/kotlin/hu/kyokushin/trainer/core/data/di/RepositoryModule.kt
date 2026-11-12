package hu.kyokushin.trainer.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.data.content.ContentImporter
import hu.kyokushin.trainer.core.data.content.RoomContentImporter
import hu.kyokushin.trainer.core.data.repository.ProgressRepository
import hu.kyokushin.trainer.core.data.repository.RoomProgressRepository
import hu.kyokushin.trainer.core.data.repository.RoomTechniqueRepository
import hu.kyokushin.trainer.core.data.repository.TechniqueRepository
import javax.inject.Singleton

/**
 * Interfész → implementáció kötések (ARCHITECTURE 7.: `RepositoryModule`, T-203).
 *
 * `@Binds` és nem `@Provides`: az implementációk `@Inject constructor`-ral rendelkeznek, így
 * a Hilt maga állítja össze őket; a `@Binds` csak annyit mond meg, melyik interfészt
 * elégítik ki. Ez egy absztrakt metódus a generált kódban – nincs futásidejű gyártófüggvény.
 *
 * A három implementáció `internal` (ADR-001, L3: `RoomTechniqueRepository`,
 * `RoomProgressRepository` és `RoomContentImporter` Room DAO-kra hivatkozik), ezért ez a
 * modul is a `core/data`-ban él, nem a `di`-ben – ugyanaz az eltérés az ARCHITECTURE 7.
 * táblázatától, mint a `DatabaseModule` esetében, és ugyanazzal az indokkal.
 *
 * Mindhárom kötés `@Singleton`:
 *
 * - a [RoomContentImporter] a „egyszerre egy betöltés" invariánst egy példánybeli `Mutex`-szel
 *   tartja be, és az utolsó validációs jelentést is példányállapotban őrzi (FR-32) – két
 *   példány mellett mindkét garancia elvesznék;
 * - a [RoomTechniqueRepository] és a [RoomProgressRepository] állapotmentes, de így a
 *   `Flow`-k ugyanazon a Room invalidációs követésen osztoznak, és a képernyők közti
 *   újrafelhasználás nem épít új objektumgráfot.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTechniqueRepository(
        implementation: RoomTechniqueRepository,
    ): TechniqueRepository

    @Binds
    @Singleton
    abstract fun bindProgressRepository(
        implementation: RoomProgressRepository,
    ): ProgressRepository

    @Binds
    @Singleton
    abstract fun bindContentImporter(implementation: RoomContentImporter): ContentImporter
}
