package hu.kyokushin.trainer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Az alkalmazás gyökér composable-je.
 *
 * ÁLLAPOT: Fázis 1 váz. A bottom navigation és a NavHost a `:navigation` modulból
 * kerül ide a Fázis 4.2 lépésben (docs/IMPLEMENTATION_PLAN.md); addig ez a képernyő
 * csak azt jelzi, hogy a build és a Hilt gráf működik.
 */
@Composable
fun KyokushinApp() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Kyokushin Trainer",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "A projektváz felállt. A képernyők a következő fázisokban készülnek el.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
