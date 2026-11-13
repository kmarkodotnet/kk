package hu.kyokushin.trainer.feature.progress

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Az utolsó gyakorlás/önértékelés időpontjának megjelenítése, `@Composable`-mentes
 * formában (FUNCTIONAL_SPEC 6.1/4., 6.2, FR-25, FR-26, T-504, T-506).
 *
 * Ugyanaz a minta, mint a `progressScoreDisplay` (CONVENTIONS 4.1 „Compose döntési logika
 * tesztelése emulátor nélkül"): a formázás sima JUnittal bizonyítható, `Resources` vagy
 * Compose nélkül. A visszaadott szöveg maga **nem** `strings.xml`-ben él – csak egy
 * számjegyekből és pontokból álló dátum –, a *körülötte* lévő mondat (pl. „Utolsó
 * gyakorlás: {dátum}") viszont igen (NFR-02); ezt a hívó formátumstring adja.
 *
 * A [zone] paraméter csak tesztelhetőség miatt van: nem egy óra lekérdezése (NFR-07 a
 * *mennyi az idő* kérdésre vonatkozik, nem egy **már meglévő**, rögzített [Instant]
 * időzóna szerinti megjelenítésére) – a hívó ettől eltekintve mindig az eszköz
 * [ZoneId.systemDefault]-ját adja át.
 */
fun formatLastPracticed(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_FORMATTER.withZone(zone).format(instant)

private val DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy.MM.dd.", Locale.forLanguageTag("hu-HU"))
