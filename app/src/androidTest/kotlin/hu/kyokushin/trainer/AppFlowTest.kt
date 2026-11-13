package hu.kyokushin.trainer

import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/** Valódi app, Hilt és Room: a navigációs határokat is végigjáró eszköztesztek. */
class AppFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitFor(text: String) {
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
    }

    private fun click(text: String) {
        waitFor(text)
        val matcher = hasText(text) and hasClickAction()
        if (compose.onAllNodes(matcher and hasAnyAncestor(hasScrollAction())).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNode(matcher).performScrollTo()
        }
        compose.onNode(matcher).performClick()
    }

    private fun tab(text: String) {
        waitFor(text)
        compose.onNode(hasText(text) and hasClickAction()).performClick()
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        android.os.SystemClock.sleep(700) // A rendszer forgatási animációja nem Compose-idling resource.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File(instrumentation.targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use {
            instrumentation.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        shell("cp ${file.absolutePath} /sdcard/Download/kyo-$name.png")
    }

    private fun shell(command: String): String =
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command).use {
            FileInputStream(it.fileDescriptor).bufferedReader().readText().trim()
        }

    private fun assertNoTextOverflow() {
        val nodes = compose.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
            useUnmergedTree = true,
        )
            .fetchSemanticsNodes()
        compose.runOnIdle {
            nodes.forEach { node ->
                val layouts = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
                layouts.forEach { layout ->
                    // LazyRow-ban a bekezdés mérési szélessége végtelen lehet. A tényleges
                    // sorok szélességét ellenőrizzük, egy pixel kerekítési toleranciával.
                    val overflow = layout.didOverflowHeight ||
                        (0 until layout.lineCount).any {
                            layout.isLineEllipsized(it) ||
                                layout.getLineRight(it) - layout.getLineLeft(it) > layout.size.width + 1f
                        }
                    org.junit.Assert.assertFalse(
                        "Levágott szöveg: ${layout.layoutInput.text}; ${layout.size}",
                        overflow,
                    )
                }
            }
        }
    }

    @Test
    fun largeFontAndLandscape_NFR04_NFR06() {
        val oldScale = shell("settings get system font_scale")
        try {
            shell("settings put system font_scale 2.0")
            compose.waitUntil(10_000) { compose.activity.resources.configuration.fontScale >= 1.99f }
            listOf("Tudástár", "Tanulás", "Kvíz", "Haladás").forEachIndexed { index, title ->
                tab(title)
                compose.waitForIdle()
                screenshot("large-font-$index-portrait")
                assertNoTextOverflow()
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                compose.waitUntil(10_000) {
                    compose.activity.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_LANDSCAPE
                }
                compose.waitForIdle()
                screenshot("large-font-$index-landscape")
                assertNoTextOverflow()
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                compose.waitUntil(10_000) {
                    compose.activity.resources.configuration.orientation ==
                        android.content.res.Configuration.ORIENTATION_PORTRAIT
                }
            }
            studyRevealRatingAndRotation_TR06_FR10_FR19()
            completeQuizWithRotation_TR06_FR18_FR19_FR20()
        } finally {
            shell(
                if (oldScale ==
                    "null"
                ) {
                    "settings delete system font_scale"
                } else {
                    "settings put system font_scale $oldScale"
                },
            )
        }
    }

    @Test
    fun fourDestinationsAndSettings_TR06() {
        waitFor("Keresés romaji vagy magyar név szerint")
        tab("Tanulás")
        waitFor("Kártyák indítása")
        tab("Kvíz")
        waitFor("Kvíz indítása")
        tab("Haladás")
        click("Beállítások")
        click("Tartalomdiagnosztika")
        compose.onNodeWithText("Tartalomdiagnosztika").assertIsDisplayed()
        screenshot("diagnostics")
    }

    @Test
    fun studyRevealRatingAndRotation_TR06_FR10_FR19() {
        tab("Tanulás")
        click("Etikett és köszönések")
        click("Kártyák indítása")
        waitFor("Koppints a felfedéshez")
        compose.onNodeWithText("Tudtam").assertIsNotEnabled()
        click("Koppints a felfedéshez")
        compose.onNodeWithText("Tudtam").assertIsEnabled()
        compose.activityRule.scenario.recreate()
        waitFor("Tudtam")
        compose.onNodeWithText("Tudtam").assertIsEnabled()
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        compose.waitUntil(10_000) {
            compose.activity.resources.configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE
        }
        waitFor("Tudtam")
        compose.onNodeWithText("Tudtam").performScrollTo().assertIsEnabled()
        assertNoTextOverflow()
        screenshot("study-${compose.activity.resources.configuration.fontScale}-landscape")
        click("Tudtam")
        compose.activityRule.scenario.onActivity {
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        waitFor("Tudtam")
        compose.onNodeWithContentDescription("Bezárás").performScrollTo().performClick()
        tab("Haladás")
        waitFor("Önértékelés a tanulókártyákon")
        compose.onNodeWithText("Önértékelés a tanulókártyákon").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun studyCategoriesReachQuizSetup_FR11_TR06() {
        tab("Tanulás")
        click("Etikett és köszönések")
        click("Kártyák indítása")
        waitFor("1 / 9")
        repeat(9) { click("Következő") }
        click("Kvíz indítása ezekkel a kategóriákkal")
        waitFor("Kvíz indítása")
        compose.onNode(hasText("Etikett és köszönések") and hasClickAction()).assertIsOn()
        compose.onNode(hasText("Állások") and hasClickAction()).assertIsOff()
        compose.activityRule.scenario.recreate()
        waitFor("Kvíz indítása")
        compose.onNode(hasText("Etikett és köszönések") and hasClickAction()).assertIsOn()
    }

    private fun wrongOptionIndex(labels: List<String>): Int {
        val json = InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("knowledge/entries.json").bufferedReader().use { JSONObject(it.readText()) }
        val entries = json.getJSONArray("entries")
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val jp = entry.getString("japanese_romaji")
            val hu = entry.getString("hungarian_name")
            if (compose.onAllNodesWithText("$jp?").fetchSemanticsNodes().isNotEmpty()) {
                return labels.indexOfFirst { it != hu }
            }
            if (compose.onAllNodesWithText("$hu?").fetchSemanticsNodes().isNotEmpty()) {
                return labels.indexOfFirst { it != jp }
            }
        }
        error("A kérdés nem a csomagolt tudástárból származik")
    }

    @Test
    fun completeQuizWithRotation_TR06_FR18_FR19_FR20() {
        tab("Kvíz")
        click("Etikett és köszönések")
        click("5")
        click("Kvíz indítása")
        val option = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        repeat(5) { index ->
            waitFor("${index + 1} / 5")
            compose.waitUntil(10_000) { compose.onAllNodes(option).fetchSemanticsNodes().size == 4 }
            val before = compose.onAllNodes(option).fetchSemanticsNodes().map { it.config[SemanticsProperties.Text] }
            if (index == 0) {
                compose.activityRule.scenario.recreate()
                waitFor("1 / 5")
                val after = compose.onAllNodes(option).fetchSemanticsNodes().map { it.config[SemanticsProperties.Text] }
                org.junit.Assert.assertEquals(before, after)
            }
            val wrongIndex = wrongOptionIndex(before.map { it.single().text })
            compose.onAllNodes(option)[wrongIndex].performScrollTo().performClick()
            val next = if (index == 4) "Eredmény" else "Következő"
            waitFor(next)
            if (index == 0) {
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
                waitFor(next)
                screenshot("quiz-${compose.activity.resources.configuration.fontScale}-landscape")
                compose.activityRule.scenario.onActivity {
                    it.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                waitFor(next)
            }
            click(next)
        }
        waitFor("Új kvíz")
        screenshot("quiz-result")
        compose.activityRule.scenario.recreate()
        waitFor("Új kvíz")
        compose.onNodeWithText("Hibák újragyakorlása").assertIsEnabled()
        click("Hibák újragyakorlása")
        compose.waitUntil(10_000) {
            compose.onAllNodes(option).fetchSemanticsNodes(atLeastOneRootRequired = false).size == 4
        }
        compose.onNodeWithContentDescription("Kvíz megszakítása").performScrollTo().performClick()
        click("Folytatás")
        compose.onNodeWithContentDescription("Kvíz megszakítása").performScrollTo().performClick()
        click("Megszakítás")
        waitFor("Kvíz indítása")
    }
}
