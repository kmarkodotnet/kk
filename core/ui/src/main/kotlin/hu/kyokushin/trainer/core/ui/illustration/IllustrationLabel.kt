package hu.kyokushin.trainer.core.ui.illustration

import android.content.res.Resources
import hu.kyokushin.trainer.core.ui.R

/**
 * Mit mondhat ki egy illusztráció akadálymentes leírása (NFR-04, FUNCTIONAL_SPEC 2.3).
 *
 * **Miért sealed típus, és miért nem egy `contentDescription: String` paraméter?**
 *
 * A `contentDescription` a legkönnyebben elrontható akadálymentességi pont: a kvíz (D)
 * típusánál a „Kiba dachi illusztrációja" leírás TalkBackkel **elárulná a helyes
 * választ**. Ha a komponens egy kész stringet kapna, a hívó szabadon átadhatná a
 * technika nevét a kvízopcióhoz is, és ezt sem a fordító, sem egy teszt nem venné észre.
 *
 * Itt viszont a hívó nem leírást ad, hanem **szándékot**: „ez a technika ábrája" vagy
 * „ez a *n*. válaszlehetőség ábrája". A tényleges szöveget ez a típus állítja elő, és a
 * [QuizOption] ágon a technika nevére nincs is paraméter – a név kiszivárgása így nem
 * programozói figyelmetlenség kérdése, hanem fordításilag lehetetlen.
 */
sealed interface IllustrationLabel {
    /**
     * Tudástár (lista és részletező): a képet a technika neve azonosítja.
     *
     * A FUNCTIONAL_SPEC 9. pontja itt **megköveteli** a nevet: „a Tudástár részletben az
     * illusztráció leírása tartalmazza a technika nevét".
     *
     * @property romaji a technika romaji neve.
     * @property hungarian a magyar jelentés.
     */
    data class Technique(
        val romaji: String,
        val hungarian: String,
    ) : IllustrationLabel

    /**
     * Kvíz (D) típus képopciója: a leírás **csak a pozíciót** mondja ki.
     *
     * @property position a válaszlehetőség 1-alapú sorszáma a képrácsban.
     */
    data class QuizOption(
        val position: Int,
    ) : IllustrationLabel {
        init {
            require(position >= 1) { "IllustrationLabel.QuizOption.position 1-alapú sorszám." }
        }
    }

    /**
     * Kvíz (C) típus kérdésábrája (T-408): a kép **a kérdés tárgya** – a felhasználónak
     * ebből kell kitalálnia a technika japán nevét. A leírás ezért, [QuizOption]-hoz
     * hasonlóan, **nem nevezheti meg a technikát** (az maga a helyes válasz lenne), de a
     * [QuizOption]-tól eltérően nincs pozíciója – ez a kérdés egyetlen, nem
     * válaszlehetőségként szereplő képe. A leírás emiatt egy harmadik, semleges szöveg
     * (nem a [Technique] neve, nem a [QuizOption] pozíciója): „a kvízkérdés ábrája".
     */
    data object QuizPrompt : IllustrationLabel

    /**
     * A kép tisztán dekoratív: a TalkBack átugorja (`contentDescription = null`).
     *
     * Kvízben **nem** használható: ott a kép hordozza a kérdés vagy a válasz
     * információtartalmát, tehát sosem dekoratív (NFR-04).
     */
    data object Decorative : IllustrationLabel
}

/**
 * Az [IllustrationLabel] feloldása tényleges, magyar nyelvű leírásokká.
 *
 * Azért külön, `@Composable`-mentes függvényekben van, és azért `Resources`-t kap, hogy
 * a leírások **emulátor nélkül, a tényleges `strings.xml` fölött** tesztelhetők legyenek
 * (Robolectric) – a NFR-04 szabály bizonyítéka így nem UI-teszt, hanem unit teszt.
 */
object IllustrationContentDescription {
    /** A valódi képhez tartozó leírás; [IllustrationLabel.Decorative] esetén `null`. */
    fun forAsset(resources: Resources, label: IllustrationLabel): String? = when (label) {
        is IllustrationLabel.Technique -> resources.getString(
            R.string.illustration_a11y_technique,
            label.romaji,
            label.hungarian,
        )

        is IllustrationLabel.QuizOption -> resources.getString(
            R.string.illustration_a11y_quiz_option,
            label.position,
        )

        IllustrationLabel.QuizPrompt -> resources.getString(R.string.illustration_a11y_quiz_prompt)

        IllustrationLabel.Decorative -> null
    }

    /**
     * A helyőrzőhöz tartozó leírás.
     *
     * A Tudástárban nem a technika nevét mondja ki, hanem a hiány tényét („Ehhez a
     * technikához még nincs ábra" – FUNCTIONAL_SPEC 2.3), mert a nevet a képernyő
     * szövege már felolvasta, és a helyőrző nem a technikáról hordoz információt.
     * A kvízopciónál viszont a pozíció marad az azonosító.
     */
    fun forPlaceholder(resources: Resources, label: IllustrationLabel): String? = when (label) {
        is IllustrationLabel.Technique -> resources.getString(R.string.illustration_a11y_placeholder)

        is IllustrationLabel.QuizOption -> resources.getString(
            R.string.illustration_a11y_quiz_option,
            label.position,
        )

        IllustrationLabel.QuizPrompt -> resources.getString(R.string.illustration_a11y_quiz_prompt)

        IllustrationLabel.Decorative -> null
    }
}
