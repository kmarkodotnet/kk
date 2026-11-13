package hu.kyokushin.trainer.feature.learn.cards

import android.content.res.Resources
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.ui.illustration.IllustrationLabel
import hu.kyokushin.trainer.feature.learn.R

/**
 * Tiszta, `@Composable`-mentes döntési szabályok a tanulókártyához (T-304, T-305,
 * FR-07, FR-08, NFR-04).
 *
 * Azért külön vannak a Compose rétegtől, hogy emulátor/Compose-renderelés nélkül,
 * egyszerű JVM (illetve ahol tényleges `strings.xml` kell, Robolectric) teszttel
 * ellenőrizhetők legyenek – ugyanaz a minta, mint az `IllustrationContentDescription`
 * és a `VerificationBadgeText` a `core/ui`-ban, vagy a `LibraryListItemAccessibility`
 * a `feature/library`-ban.
 */
object LearnCardImagePolicy {
    /**
     * Igaz, ha a kártya ábráját **nem szabad** megjeleníteni felfedés előtt (T-305).
     *
     * Kizárólag a feliratos képekre igaz (`IllustrationRef.Asset.hideableOnStudyCard ==
     * false`): a jelenlegi tartalomban ez a KY-0045, KY-0050 és KY-0052 (lásd
     * `knowledgebase_transfer/manifest.json`). A [IllustrationRef.Placeholder] és a
     * `null` eset nem rejtendő – azoknál nincs feliratozott pixel, amit elrejtsünk.
     */
    fun shouldHideUntilRevealed(illustration: IllustrationRef?, revealed: Boolean): Boolean =
        illustration is IllustrationRef.Asset && !illustration.hideableOnStudyCard && !revealed
}

/**
 * Az ábra akadálymentes leírásának szándéka a tanulókártyán (FR-07, NFR-04).
 */
object LearnCardIllustrationLabelPolicy {
    /**
     * Felfedés **előtt** [IllustrationLabel.Decorative] – nem a technika neve.
     *
     * Ha a [hu.kyokushin.trainer.core.ui.illustration.TechniqueIllustration] a teljes
     * nevet kapná leírásként már felfedés előtt is, a TalkBack kimondaná a rejtett
     * nevet, és ez pontosan úgy elárulná a választ, mint egy véletlen felfedés – amit a
     * FR-07 kifejezetten explicit interakcióhoz köt. Felfedés **után** a teljes
     * [IllustrationLabel.Technique] jár, mert akkor a név már látványban is olvasható.
     */
    fun labelFor(romaji: String, hungarian: String, revealed: Boolean): IllustrationLabel =
        if (revealed) {
            IllustrationLabel.Technique(romaji = romaji, hungarian = hungarian)
        } else {
            IllustrationLabel.Decorative
        }
}

/**
 * A kártya felfedési állapotának `stateDescription`-je (T-304 „Kész ha": „felfedés előtt
 * »elrejtve«, utána »felfedve«").
 */
object LearnCardStateDescription {
    fun forRevealed(resources: Resources, revealed: Boolean): String = resources.getString(
        if (revealed) R.string.learn_cards_a11y_state_revealed else R.string.learn_cards_a11y_state_hidden,
    )
}
