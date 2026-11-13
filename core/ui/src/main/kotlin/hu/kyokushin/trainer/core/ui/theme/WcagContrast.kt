package hu.kyokushin.trainer.core.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/*
 * A WCAG 2.1 kontraszt-számítás (NFR-04, T-604) – tiszta matek, nincs Android-függés a
 * számításban magában (a [Color] csak a bemeneti csatornaértékeket adja).
 */

/**
 * A két szín WCAG kontrasztaránya (1:1 … 21:1).
 *
 * A képlet a W3C hivatalos definíciója: relatív fényesség (`relativeLuminance`) a
 * világosabb és a sötétebb szín közül, `(L1 + 0.05) / (L2 + 0.05)`, `L1 >= L2`. Ez a
 * szabvány forrása a projekt NFR-04 „WCAG AA kontraszt" követelményének – 4.5:1 a
 * normál méretű szövegre.
 */
internal fun wcagContrastRatio(a: Color, b: Color): Double {
    val lA = relativeLuminance(a)
    val lB = relativeLuminance(b)
    val lighter = maxOf(lA, lB)
    val darker = minOf(lA, lB)
    return (lighter + 0.05) / (darker + 0.05)
}

/** A W3C relatív fényesség-képlete (sRGB gamma-dekódolással csatornánként). */
private fun relativeLuminance(color: Color): Double {
    val r = linearize(color.red)
    val g = linearize(color.green)
    val b = linearize(color.blue)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** Egy sRGB csatorna (0f..1f) lineáris fénysűrűséggé alakítása. */
private fun linearize(channel: Float): Double {
    val c = channel.toDouble()
    return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
}
