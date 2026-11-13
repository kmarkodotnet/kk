package hu.kyokushin.trainer.feature.progress.overview

import android.content.res.Resources
import hu.kyokushin.trainer.feature.progress.R

/**
 * A „Gyakorlást igénylő" listatétel akadálymentes leírásának összeállítása (FR-26,
 * NFR-04, FUNCTIONAL_SPEC 9.: a sikerarány-sávok szöveges értékkel is).
 *
 * Ugyanaz a minta, mint a `feature/library` [hu.kyokushin.trainer.feature.library.list.LibraryListItemAccessibility]
 * esetén: a teljes sor **egy** TalkBack bejárási egység, és a leírás összeállítása a
 * tényleges `strings.xml` fölött, `@Composable`-mentes formában, emulátor nélkül
 * tesztelhető (Robolectric).
 *
 * A [lastPracticedText] már **kész, formázott** szöveg (a hívó állítja elő
 * `hu.kyokushin.trainer.feature.progress.formatLastPracticed`-del, az eszköz
 * időzónájával) – ezt kapja meg [describe], nem a nyers [java.time.Instant]-ot, hogy a
 * dátumformázás időzónafüggése (lásd a függvény KDoc-ja) itt, a leírás-összeállítás
 * tesztjeiben ne okozzon a futtató gép időzónájától függő, nem determinisztikus eredményt.
 */
internal object ProgressNeedsPracticeAccessibility {
    fun describe(
        resources: Resources,
        item: ProgressNeedsPracticeItem,
        percent: Int,
        lastPracticedText: String,
    ): String = resources.getString(
        R.string.progress_overview_needs_practice_item_a11y,
        item.romaji,
        item.hungarian,
        percent,
        lastPracticedText,
    )
}
