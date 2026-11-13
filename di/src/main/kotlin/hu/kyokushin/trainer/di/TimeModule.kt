package hu.kyokushin.trainer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/**
 * Az idő egyetlen forrása az alkalmazásban (ARCHITECTURE 7.: `TimeModule`, NFR-07, T-203).
 *
 * ## Miért kötés, és miért nem `Instant.now()` a hívás helyén?
 *
 * Mert az időbélyeget az üzleti kód *eredménye* hordozza: a tartalomimport `importedAt`
 * értéke (T-117), a kvízmunkamenet kezdete és vége (FR-16), az önértékelés
 * `reviewedAt`-je (FR-09). Ha ezek a rendszerórát hívnák, a tesztek vagy nem tudnák
 * ellenőrizni az eredményt, vagy „körülbelül most" alapú, időzítésérzékeny állításokat
 * írnának. Fix [Clock] mellett az elvárás pontos egyenlőség.
 *
 * Ez a fájl az **egyetlen** hely az üzleti kódban, ahol a rendszeróra megjelenik; mindenhol
 * máshol a [Clock] injektált (NFR-07, a `core/quiz` forrásellenőrző tesztje ezt
 * modulszinten ki is kényszeríti). A teszt a saját gráfjában fix órát kötve írja felül ezt
 * a kötést.
 *
 * ## Miért UTC?
 *
 * Mert a tárolt időbélyegek `Instant`-ok (DR-05): zónamentes, monoton pillanatok. A helyi
 * időzónás megjelenítés a UI dolga, és a tárolt értéket nem befolyásolhatja – különben egy
 * utazás vagy egy nyári időszámítás-váltás a korábbi előrehaladás sorrendjét borítaná fel.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object TimeModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
