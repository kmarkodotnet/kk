package hu.kyokushin.trainer.feature.progress.overview

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import hu.kyokushin.trainer.core.model.IllustrationRef
import hu.kyokushin.trainer.core.model.TechniqueId
import hu.kyokushin.trainer.core.quiz.scoring.Score
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

private const val LAST_PRACTICED_TEXT = "Utolsó gyakorlás: 2026.11.01."

/**
 * A „Gyakorlást igénylő" listatétel egyetlen bejárási egységként felolvasott leírása
 * (FR-26, NFR-04, FUNCTIONAL_SPEC 9., T-504).
 *
 * A teszt a **tényleges** `strings.xml` fölött fut (Robolectric,
 * `isIncludeAndroidResources = true`) – ugyanaz a minta, mint a `feature/library`
 * `LibraryListAccessibilityTest`-je.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProgressNeedsPracticeAccessibilityTest {
    private lateinit var resources: Resources

    @Before
    fun setUp() {
        resources = ApplicationProvider.getApplicationContext<Context>().resources
    }

    private fun item(
        romaji: String = "mae geri",
        hungarian: String = "egyenes előre rúgás",
    ): ProgressNeedsPracticeItem = ProgressNeedsPracticeItem(
        techniqueId = TechniqueId.of("KY-0002"),
        romaji = romaji,
        hungarian = hungarian,
        illustration = IllustrationRef.Placeholder,
        score = Score(answered = 4, correct = 1, ratio = 0.25),
        lastPracticedAt = Instant.parse("2026-11-01T08:00:00Z"),
    )

    @Test
    fun describe_includesRomajiHungarianPercentAndLastPracticedText_FR26() {
        val description = ProgressNeedsPracticeAccessibility.describe(
            resources = resources,
            item = item(),
            percent = 25,
            lastPracticedText = LAST_PRACTICED_TEXT,
        )

        assertThat(description).isEqualTo(
            "mae geri, egyenes előre rúgás, sikerarány 25 százalék, Utolsó gyakorlás: 2026.11.01.",
        )
    }

    @Test
    fun describe_reflectsTheGivenTechnique_FR26() {
        val description = ProgressNeedsPracticeAccessibility.describe(
            resources = resources,
            item = item(romaji = "mawashi geri", hungarian = "köríves rúgás"),
            percent = 50,
            lastPracticedText = LAST_PRACTICED_TEXT,
        )

        assertThat(description).contains("mawashi geri")
        assertThat(description).contains("köríves rúgás")
        assertThat(description).contains("50 százalék")
    }
}
