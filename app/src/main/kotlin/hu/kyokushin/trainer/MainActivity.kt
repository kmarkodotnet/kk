package hu.kyokushin.trainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import hu.kyokushin.trainer.ui.theme.KyokushinTrainerTheme

/**
 * Az alkalmazás egyetlen Activityje (NFR-01: egyetlen APK, egyetlen belépési pont).
 * A képernyők közti váltás a Navigation Compose feladata.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KyokushinTrainerTheme {
                KyokushinApp()
            }
        }
    }
}
