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

/**
 * A CPU-kötött háttérmunka dispatcherének kötésazonosítója (NFR-07, ARCHITECTURE 7.:
 * `DispatcherModule`).
 *
 * Miért kell az [IoDispatcher] mellé egy második qualifier?
 * Mert a két dispatcher *más* szálkészlet, és a kettő összekeverése mérhető kárt okoz: a
 * `Dispatchers.IO` készlete blokkoló lemez- és asseteléréshez van méretezve (sok, többnyire
 * várakozó szál), a `Dispatchers.Default` a magok számához (rendezés, szűrés, normalizálás).
 * A keresés normalizálása és rendezése (FR-03) vagy a kvízterv összeállítása (FR-11…FR-15)
 * CPU-munka: ha az I/O készleten futna, a párhuzamos lemezműveletek mellett akadna.
 *
 * Qualifier – és nem `Dispatchers.Default` közvetlen hívás – ugyanazért, amiért az
 * [IoDispatcher] is az: a ViewModel tesztek `TestDispatcher`-t adnak át, különben a
 * `runTest` virtuális órája nem vezérelné a futást, és a tesztek nem lennének
 * determinisztikusak (NFR-07).
 *
 * A kötést a `di` modul `DispatcherModule`-ja adja (T-203).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
