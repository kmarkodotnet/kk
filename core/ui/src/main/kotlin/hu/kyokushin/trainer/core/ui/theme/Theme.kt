package hu.kyokushin.trainer.core.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Az alkalmazás Material 3 témája – szín, forma ([KyokushinShapes], T-712) és típusskála
 * ([KyokushinTypography], T-712).
 *
 * **Miért a `core/ui`-ban él, és nem az `app`-ban?** A témát a megosztott komponensek
 * `@Preview`-jai és a funkciómodulok (`feature`) képernyői is használják, a
 * függőségi irány viszont a funkciómoduloktól a `core/ui` felé mutat, és `:app`-ra
 * senki nem függhet visszafelé (ADR-001).
 * A `core/ui`-ban hagyva a paletta egyetlen helyen van, és a megosztott komponensek
 * ugyanazzal a sémával rendelhetők elő, mint amivel futnak (T-204 döntése).
 *
 * Visszafogott, funkcionális paletta (CR-08: nincs harcművészeti klisé).
 * A komponensek SOHA nem használnak hardcode-olt színt, csak téma-tokent
 * (lásd .claude/skills/kyo-android-module 3. pont).
 *
 * **`internal`, nem `private` (T-604, NFR-04).** A WCAG AA kontraszt-követelmény
 * (4.5:1 testszövegre) ellenőrzéséhez a [ThemeContrastTest][hu.kyokushin.trainer.core.ui.theme.ThemeContrastTest]
 * közvetlenül ezen a két palettán fut, nem egy a tesztbe duplikált másolaton – így egy
 * jövőbeli színváltás a VALÓS, futó témán buktatja el a tesztet, nem egy elfeledett
 * tesztmásolaton.
 */
internal val LightScheme = lightColorScheme(
    // A splash-logó (T-701, `core/ui/.../brand/KyoKanjiMark.kt`) kék színe – a márka
    // egyetlen, következetes kék tónusa, a logó az igazságforrás (T-712,
    // `docs/DESIGN_NOTES.md` 3. szakasza), nem fordítva.
    primary = Color(0xFF17317A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD9E2F7),
    onPrimaryContainer = Color(0xFF0B1F30),
    secondary = Color(0xFF4F616E),
    onSecondary = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    surface = Color(0xFFFCFCFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF42474E),
    outline = Color(0xFF72777F),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFA2C9EA),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF004B70),
    onPrimaryContainer = Color(0xFFD3E4F5),
    secondary = Color(0xFFB7C9D7),
    onSecondary = Color(0xFF21333E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF42474E),
    onSurfaceVariant = Color(0xFFC2C7CF),
    outline = Color(0xFF8C9199),
)

/**
 * Egyetlen, megnevezett forma-készlet (T-712, `docs/DESIGN_NOTES.md` 5. szakasza) –
 * nyolc fájl korábban egymástól függetlenül írta ki ugyanazt a `RoundedCornerShape(12.dp)`
 * literált (lásd `docs/CONVENTIONS.md` 4. szakasza). A `medium` értéke szándékosan
 * **nem** változott (a meglévő `12.dp` csak egyetlen helyre költözött); a `large` a
 * tanulókártya korábbi `CARD_CORNER_RADIUS`-ának felel meg, a legnagyobb, leghangsúlyosabb
 * felületnek valamivel lágyabb sarkot adva. A Material 3 `extraSmall`/`extraLarge`
 * alapértelmezése változatlan marad, mert egyik hívó sem igényelt rá eltérést.
 */
private val KyokushinShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

/**
 * Egyetlen, megnevezett típusskála (T-712, `docs/DESIGN_NOTES.md` 4. szakasza): a
 * romaji/japán fogalom címszerepei (`titleLarge`, `headlineSmall`) **félkövérek**, a
 * Material 3 alapértelmezett `Medium` súlya helyett – ez adja a tipográfiai
 * megkülönböztetést a romaji (főszereplő) és a magyar jelentés (alárendelt, változatlan
 * súlyú `bodyLarge`/`bodyMedium`) között. **Nincs** új betűtípus-erőforrás (a terv
 * kifejezetten elvetette, lásd `docs/DESIGN_NOTES.md` 4. szakasza "elvetett ötlet" sora)
 * – a Roboto (Material 3 alapértelmezett) marad, csak a súlykiosztás explicit.
 */
private val baseTypography = Typography()
private val KyokushinTypography = baseTypography.copy(
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun KyokushinTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Alapból az automatizált WCAG-tesztekkel ellenőrzött palettát használjuk.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else -> LightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KyokushinTypography,
        shapes = KyokushinShapes,
        content = content,
    )
}
