package hu.kyokushin.trainer.core.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.ui.graphics.vector.ImageVector
import hu.kyokushin.trainer.core.model.CategoryId

/**
 * Semleges, geometrikus ikon a tudástár kategóriáihoz (T-705, FR-39).
 *
 * A `docs/DESIGN_NOTES.md` 6–7. szakasza (T-702) és a CR-08 ("nincs harcművészeti
 * klisékkel túlterhelt megjelenés") szerint **nem** egy adott mozdulatot ábrázoló
 * piktogram jár egy kategóriának (pl. egy rajzolt rúgó alak), hanem a kategória
 * *típusára* utaló, semleges Material-ikon – pontosan úgy, ahogy a `docs/DESIGN_NOTES.md`
 * 6. szakasza javasolja ("irány nyilak", "szám", nem egy rajzolt emberalak).
 *
 * A leképezés a ténylegesen betöltött tudástár (`app/src/main/assets/knowledge/manifest.json`)
 * 9 kategória-azonosítójára épül. **Egy ismeretlen/jövőbeli kategória-azonosító nem kap
 * kitalált, "mintha hozzá tartozna" ikont** – a [Icons.Filled.Category] semleges
 * helyőrzőt kapja, amíg egy tartalomfrissítés nem ad hozzá explicit leképezést.
 */
fun categoryIcon(categoryId: CategoryId): ImageVector = when (categoryId.value) {
    // Etikett és köszönések – kéznyújtásos köszönés, nem meghajlás-sziluett.
    "etiquette" -> Icons.Filled.Handshake
    // Vezényszavak és alapfogalmak – kimondott/hirdetett szó.
    "commands" -> Icons.Filled.Campaign
    // Állások – mérés/helyzet, nem egy konkrét állás rajza.
    "stances" -> Icons.Filled.Straighten
    // Védések – általános védelem-jelkép, nem karate-specifikus.
    "blocks" -> Icons.Filled.Shield
    // Kéz- és könyöktechnikák – a kéz mint testrész, nem egy ütés mozdulatrajza.
    "punches" -> Icons.Filled.PanTool
    // Rúgások és térdtechnikák – irány, nem egy lábmozdulat rajza.
    "kicks" -> Icons.AutoMirrored.Filled.TrendingFlat
    // Ütőfelületek és testrészek – egy pont/felület kiemelése, nem egy testrajz.
    "body_parts" -> Icons.Filled.CenterFocusStrong
    // Japán számok.
    "numbers" -> Icons.Filled.Numbers
    // Katák – egy bejárt útvonal/sorozat jelképe, nem egy pózsorozat rajza.
    "kata" -> Icons.Filled.Route
    else -> Icons.Filled.Category
}
