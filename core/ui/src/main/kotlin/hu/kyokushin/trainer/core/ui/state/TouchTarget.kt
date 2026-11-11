package hu.kyokushin.trainer.core.ui.state

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A minimális érintési célpont (FUNCTIONAL_SPEC 9., NFR-04).
 *
 * A Material 3 gombok alapértelmezett magassága 40 dp, a követelmény viszont 48 dp, ezért
 * az állapotkomponensek gombjai explicit `heightIn(min = …)`-t kapnak. Egy helyen van, hogy
 * az üres és a hibaállapot gombja ne csúszhasson el egymástól.
 */
internal val MIN_TOUCH_TARGET: Dp = 48.dp
