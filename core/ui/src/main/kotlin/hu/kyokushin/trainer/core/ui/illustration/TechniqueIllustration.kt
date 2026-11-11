package hu.kyokushin.trainer.core.ui.illustration

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.ui.R
import hu.kyokushin.trainer.core.ui.theme.KyokushinTrainerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A helyőrző és a még be nem töltött kép képaránya (álló, mint a tudástár képei). */
private const val PLACEHOLDER_ASPECT_RATIO: Float = 4f / 5f

/** A komponens sarokkerekítése – a `Surface` és a kép ugyanazt használja. */
private val CORNER_RADIUS: Dp = 12.dp

/**
 * Egy technika illusztrációja a hivatkozás **mindhárom** esetére (ADR-003, CR-05,
 * FUNCTIONAL_SPEC 2.3).
 *
 * | [illustration] | Megjelenítés |
 * |---|---|
 * | [IllustrationRef.Asset] | a csomagolt kép, képarány megtartva |
 * | [IllustrationRef.Placeholder] | semleges helyőrző + „Ábra készül" |
 * | `null` | **semmi** – a composable nem bocsát ki UI-t |
 *
 * A `null` eset szándékosan nem helyőrző: a rekord nem *hivatkozik* illusztrációra, ami
 * más, mint a hiányzó kép. Hogy a részletező képernyőn a US-07 2. elfogadási kritériuma
 * („semleges helyőrző + ...") érvényesüljön, ott a hívó dönt úgy, hogy a `null`-t
 * [IllustrationRef.Placeholder]-ként adja át – a listán viszont a hely marad üresen.
 * Ezt a döntést a képernyő hozza meg (T-208, T-209), nem ez a komponens.
 *
 * **Akadálymentesség (NFR-04).** A leírást nem string paraméter, hanem az
 * [IllustrationLabel] szándéktípus adja: a kvízopció ágán a technika nevére nincs
 * paraméter, így a név nem szivároghat a `contentDescription`-be. Lásd
 * [IllustrationContentDescription].
 *
 * A komponens **nem interaktív** – nincs benne kattintható elem, ezért a 48 dp-es
 * érintési célpont nem rá vonatkozik; a képet körbefogó kártya kattinthatóságát a
 * listatétel (T-208) adja, a saját minimális méretével.
 *
 * @param illustration a megjelenítendő hivatkozás, vagy `null`.
 * @param label mit mondhat ki az akadálymentes leírás.
 * @param modifier a konténer modifiere. A komponens a rendelkezésre álló szélességet
 *   tölti ki, és [maxWidth]-nél nem lesz szélesebb – a képarányt mindig megtartja.
 * @param awaitingReview `true`, ha a kép szakmai ellenőrzésre vár: ekkor a kép alatt
 *   szöveges jelzés jelenik meg (US-08 4. kritérium, CR-01). A komponens ezt **nem**
 *   származtatja: a hitelesítési státusz a tartalom dolga, kód soha nem emeli (CR-02).
 * @param maxWidth a kép legnagyobb szélessége; a részletezőn a teljes szélesség a cél,
 *   listatételen a hívó ad kisebb korlátot.
 */
@Composable
fun TechniqueIllustration(
    illustration: IllustrationRef?,
    label: IllustrationLabel,
    modifier: Modifier = Modifier,
    awaitingReview: Boolean = false,
    maxWidth: Dp = Dp.Unspecified,
) {
    when (illustration) {
        null -> Unit

        is IllustrationRef.Placeholder -> IllustrationFrame(
            modifier = modifier,
            awaitingReview = awaitingReview,
            maxWidth = maxWidth,
        ) {
            IllustrationPlaceholder(label = label)
        }

        is IllustrationRef.Asset -> IllustrationFrame(
            modifier = modifier,
            awaitingReview = awaitingReview,
            maxWidth = maxWidth,
        ) {
            IllustrationAsset(asset = illustration, label = label)
        }
    }
}

/**
 * A közös keret: a kép/helyőrző, és alatta – ha kell – az ellenőrzésre váró jelzés.
 *
 * A jelzés a képen **kívül**, külön szövegként van, nem a `contentDescription` része:
 * így nagyításnál és TalkBack nélkül is látható, és a bejárási sorrendben önálló
 * egységként, a kép után hangzik el (FUNCTIONAL_SPEC 9.).
 */
@Composable
private fun IllustrationFrame(
    modifier: Modifier,
    awaitingReview: Boolean,
    maxWidth: Dp,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.then(
            if (maxWidth == Dp.Unspecified) Modifier else Modifier.widthIn(max = maxWidth),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
        if (awaitingReview) {
            AwaitingReviewNotice()
        }
    }
}

/** A csomagolt kép. Betöltés közben, és ha a fájl nem olvasható, a helyőrző látszik. */
@Composable
private fun IllustrationAsset(
    asset: IllustrationRef.Asset,
    label: IllustrationLabel,
) {
    val assets = LocalContext.current.assets
    val bitmap: Bitmap? by rememberIllustrationBitmap(asset, assets)
    val loaded = bitmap

    if (loaded == null) {
        // Betöltés alatt vagy hiányzó/olvashatatlan fájl: ugyanaz a semleges felület,
        // mint a helyőrzőnél – nem ugrik a layout, és nincs összeomlás (FUNCTIONAL_SPEC 10.).
        IllustrationPlaceholder(label = label)
        return
    }

    Surface(
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = IllustrationContentDescription.forAsset(
                resources = LocalContext.current.resources,
                label = label,
            ),
            // A tényleges képarány a bitmapből jön, nem beégetett érték: a kép sem
            // nyúlik, sem vágódik (T-204: „képarány megtartása").
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(loaded.width.toFloat() / loaded.height.toFloat()),
        )
    }
}

/**
 * A dekódolt bitmap aszinkron betöltése.
 *
 * A fájl I/O és a dekódolás `Dispatchers.IO`-n fut, nem a kompozícióban: a részletező
 * képernyő megnyitása így nem akad meg a képen. A `key` az asset fájlneve, tehát
 * újrahasznosított listatételnél (T-208) nem a régi kép marad bent.
 */
@Composable
private fun rememberIllustrationBitmap(
    asset: IllustrationRef.Asset,
    assets: android.content.res.AssetManager,
): State<Bitmap?> = produceState<Bitmap?>(initialValue = null, asset.fileName, assets) {
    value = withContext(Dispatchers.IO) {
        IllustrationAssetLoader.load(assets, asset.fileName)
    }
}

/** Semleges helyőrző: glyph + „Ábra készül". Minden szín téma-tokenből. */
@Composable
private fun IllustrationPlaceholder(label: IllustrationLabel) {
    val description = IllustrationContentDescription.forPlaceholder(
        resources = LocalContext.current.resources,
        label = label,
    )
    Surface(
        shape = RoundedCornerShape(CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(PLACEHOLDER_ASPECT_RATIO)
            // A helyőrző EGY bejárási egység: a glyph és a „Ábra készül" felirat
            // helyett a TalkBack a hiányt mondja ki egyszer (NFR-04).
            .then(
                if (description == null) {
                    Modifier.clearAndSetSemantics { }
                } else {
                    Modifier.clearAndSetSemantics {
                        this.contentDescription = description
                    }
                },
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_illustration_placeholder),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = stringResource(R.string.illustration_placeholder_label),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * „A kép szakmai ellenőrzésre vár" jelzés.
 *
 * Nem csak színnel jelez: a tartalom maga a szöveg, tehát színvakság és monokróm
 * nagyításnál is olvasható (NFR-04).
 */
@Composable
private fun AwaitingReviewNotice() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Kis jelzőpont: megerősítés, nem információhordozó – a tartalmat a szöveg adja.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(8.dp),
            content = {},
        )
        Text(
            text = stringResource(R.string.illustration_awaiting_review),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------------------
// Előnézetek – mindhárom eset, világos és sötét témában (globális DoD 6. pont).
// A dinamikus szín kikapcsolva, hogy a saját, kontrasztra ellenőrzött paletta látszódjon.
// ---------------------------------------------------------------------------------------

/**
 * `Asset` eset. Az előnézet a csomagolt képet nem tudja megnyitni (a renderelőnek nincs
 * `AssetManager`-e a tudástárhoz), ezért itt a betöltési/hiányzó-fájl ág látszik –
 * pontosan az az állapot, amit a hibakezelési mátrix előír.
 */
@Preview(name = "Asset – világos", showBackground = true)
@Preview(name = "Asset – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TechniqueIllustrationAssetPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        TechniqueIllustration(
            illustration = IllustrationRef.Asset(
                ref = "KY-0044",
                fileName = "KY-0044.jpg",
                quizUsable = true,
                hideableOnStudyCard = true,
            ),
            label = IllustrationLabel.Technique(romaji = "kiba dachi", hungarian = "lovagló állás"),
            awaitingReview = true,
            maxWidth = 240.dp,
        )
    }
}

/** `Placeholder` eset: semleges helyőrző + „Ábra készül". */
@Preview(name = "Placeholder – világos", showBackground = true)
@Preview(name = "Placeholder – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TechniqueIllustrationPlaceholderPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        TechniqueIllustration(
            illustration = IllustrationRef.Placeholder,
            label = IllustrationLabel.Technique(romaji = "gedan barai", hungarian = "lefelé hárítás"),
            maxWidth = 240.dp,
        )
    }
}

/**
 * `null` eset: a komponens nem bocsát ki UI-t, ezért az előnézetben csak a körülötte
 * lévő szöveg látszik – ez az ellenőrizhető bizonyíték arra, hogy nincs képblokk.
 */
@Preview(name = "Nincs hivatkozás – világos", showBackground = true)
@Preview(name = "Nincs hivatkozás – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TechniqueIllustrationMissingPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Csak az előnézet állványzata: fejlesztői felirat, soha nem kerül a
            // felhasználó elé, ezért nem `strings.xml`-ben van (NFR-02).
            Text(text = "— nincs képblokk —", style = MaterialTheme.typography.bodySmall)
            TechniqueIllustration(
                illustration = null,
                label = IllustrationLabel.Technique(romaji = "seiken", hungarian = "ököl"),
                maxWidth = 240.dp,
            )
        }
    }
}

/**
 * Kvízopció-helyőrző: a leírás **csak pozíció alapú**, a technika neve nem hangzik el
 * (NFR-04). A valódi kvízrács a T-408 feladatban épül fel.
 */
@Preview(name = "Kvízopció – világos", showBackground = true)
@Preview(name = "Kvízopció – sötét", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TechniqueIllustrationQuizOptionPreview() {
    KyokushinTrainerTheme(dynamicColor = false) {
        TechniqueIllustration(
            illustration = IllustrationRef.Placeholder,
            label = IllustrationLabel.QuizOption(position = 1),
            maxWidth = 160.dp,
        )
    }
}
