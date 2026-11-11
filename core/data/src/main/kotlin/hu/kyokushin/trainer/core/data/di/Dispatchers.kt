package hu.kyokushin.trainer.core.data.di

import javax.inject.Qualifier

/**
 * A háttér-I/O dispatcher kötésének azonosítója (NFR-07, ARCHITECTURE 9.: `DispatcherModule`).
 *
 * Miért itt él az annotáció, és nem a `di` modulban?
 * A kötést (`@Provides`) a `di` modul adja (T-203), de a *qualifier* típusát annak a
 * modulnak kell látnia, amelyik az injektálást kéri. A `di` a `core/data`-ra függ, nem
 * fordítva (ADR-001), ezért a qualifier a legalacsonyabb rétegben él, ahol használják.
 *
 * Miért qualifier, és miért nem `Dispatchers.IO` közvetlen hívása?
 * Mert akkor a tartalomimport tesztje valódi szálkészleten futna, és a `runTest` virtuális
 * órája nem vezérelné – ugyanaz az ok, amiért a `Clock` és az RNG is injektált (NFR-07).
 * A tesztek `UnconfinedTestDispatcher`-t adnak át ennek a paraméternek.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
