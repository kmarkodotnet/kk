package hu.kyokushin.trainer.core.ui.category

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.CategoryId
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T-705 / FR-39: minden valós tudástár-kategória kap egy, a kategóriától elkülönülő
 * ikont, és egy ismeretlen kategória-azonosító a semleges helyőrzőt kapja – nem egy
 * véletlenül egyező, kitalált "mintha hozzá tartozna" jelölést.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CategoryIconTest {
    private val realCategoryIds = listOf(
        "etiquette", "commands", "stances", "blocks", "punches",
        "kicks", "body_parts", "numbers", "kata",
    ).map(CategoryId::of)

    @Test
    fun categoryIcon_everyRealCategory_getsADistinctIcon_FR39() {
        val icons = realCategoryIds.map { categoryIcon(it) }

        assertThat(icons.toSet()).hasSize(realCategoryIds.size)
    }

    @Test
    fun categoryIcon_unknownCategory_fallsBackToTheNeutralPlaceholder_FR39() {
        val icon = categoryIcon(CategoryId.of("not-a-real-category"))

        assertThat(icon).isEqualTo(Icons.Filled.Category)
    }

    @Test
    fun categoryIcon_noRealCategoryUsesTheFallbackIcon_FR39() {
        val icons = realCategoryIds.map { categoryIcon(it) }

        assertThat(icons).doesNotContain(Icons.Filled.Category)
    }
}
