package hu.kyokushin.trainer.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import hu.kyokushin.trainer.core.data.di.DefaultDispatcher
import hu.kyokushin.trainer.core.data.di.IoDispatcher
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * A háttérszálkészletek kötése (ARCHITECTURE 7.: `DispatcherModule`, NFR-07, T-203).
 *
 * A qualifier annotációk (`@IoDispatcher`, `@DefaultDispatcher`) a `core/data`-ban élnek,
 * mert azoknak a moduloknak kell látniuk őket, amelyek az injektálást kérik; a `di` a
 * `core/data`-ra függ, nem fordítva (ADR-001). A kötés – vagyis hogy melyik qualifier
 * melyik konkrét dispatcherre mutat – viszont itt, egy helyen dől el.
 *
 * Miért nem hívja az üzleti kód közvetlenül a `Dispatchers.IO`-t? Mert akkor a `runTest`
 * virtuális órája nem vezérelné a futást: a teszt valódi szálkészleten várna, az állítások
 * pedig időzítésérzékenyek lennének. Injektált dispatcher mellett a teszt
 * `TestDispatcher`-t ad át, és a futás lépései determinisztikusak (NFR-07).
 *
 * Ez a fájl az egyetlen hely, ahol a `Dispatchers` objektum megjelenik.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DispatcherModule {

    /**
     * Blokkoló I/O: Room lekérdezések, assetolvasás, tartalomimport.
     *
     * Nem `@Singleton`-nal „létrehozott", hanem megosztott objektum – a scope itt csak azt
     * jelenti, hogy a gráf ugyanazt a példányt adja vissza.
     */
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    /** CPU-kötött munka: keresésnormalizálás, rendezés, kvízterv összeállítása. */
    @Provides
    @Singleton
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
