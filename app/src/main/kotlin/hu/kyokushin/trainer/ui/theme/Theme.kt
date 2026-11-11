package hu.kyokushin.trainer.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Visszafogott, funkcionális paletta (CR-08: nincs harcművészeti klisé).
 * A komponensek SOHA nem használnak hardcode-olt színt, csak téma-tokent
 * (lásd .claude/skills/kyo-android-module 3. pont).
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF1C3D5A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD3E4F5),
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

private val DarkScheme = darkColorScheme(
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

@Composable
fun KyokushinTrainerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // A dinamikus szín (Material You) alapból be van kapcsolva, de a kontrasztot a
    // saját palettánk garantálja; teszteléshez kikapcsolható.
    dynamicColor: Boolean = true,
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
        content = content,
    )
}
