package hu.kyokushin.trainer.feature.library.list

import android.content.res.Resources
import hu.kyokushin.trainer.core.ui.status.VerificationBadgeText
import hu.kyokushin.trainer.feature.library.R

/**
 * A listatétel akadálymentes leírásának összeállítása (FR-05, NFR-04,
 * FUNCTIONAL_SPEC 9.: „a listatétel egy bejárási egység: „`{romaji}`, `{magyar}`,
 * `{kategória}`, `{státusz}`"").
 *
 * ## Miért `@Composable`-mentes, `Resources`-t fogadó objektum?
 *
 * Ugyanaz a minta, mint a [VerificationBadgeText] és az `IllustrationContentDescription`
 * esetén: a szöveg összeállítása a **tényleges** `strings.xml` fölött, emulátor nélkül,
 * Robolectric unit teszttel ellenőrizhető, a Compose fa kirenderelése nélkül.
 *
 * ## A státusz rész elmaradása VERIFIED esetén szándékos
 *
 * [VerificationBadgeText.label] `VERIFIED` rekordra `null`-t ad – a hitelesített rekord
 * nem kap sem látható chipet, sem kimondott státuszt. Ez nem hiányosság, hanem ugyanaz a
 * döntés, mint a [hu.kyokushin.trainer.core.ui.status.VerificationBadge] látható chipjénél:
 * a hitelesség a jelölő HIÁNYA (FUNCTIONAL_SPEC 2.2).
 *
 * Az alias-sor (US-06 3. kritérium) a leírás **külön mondataként** zárja a szöveget,
 * ugyanazzal a ". " elválasztóval, mint a [VerificationBadge] a címke és a részletezés
 * között.
 */
internal object LibraryListItemAccessibility {

    fun describe(resources: Resources, item: LibraryTechniqueUiModel): String {
        val statusLabel = VerificationBadgeText.label(resources, item.status)
        val core = listOfNotNull(item.romaji, item.hungarian, item.categoryNameHu, statusLabel)
            .joinToString(", ")
        val aliasSentence = item.matchedAlias?.let { alias ->
            resources.getString(R.string.library_matched_alias, alias)
        }
        return listOfNotNull(core, aliasSentence).joinToString(". ")
    }
}
