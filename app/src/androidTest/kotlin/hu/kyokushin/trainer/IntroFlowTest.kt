package hu.kyokushin.trainer

import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream

/** Valódi idő és Android accessibility: a WebView nem Compose-idling resource. */
class IntroFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation

    private fun nodes(root: AccessibilityNodeInfo? = automation.rootInActiveWindow): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        return listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
    }

    private fun skip() = nodes().firstOrNull { it.text?.toString() == "Animáció átugrása" }

    private fun waitFor(timeout: Long, condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < deadline) {
            if (condition()) return
            SystemClock.sleep(100)
        }
        assertTrue("Az intro várt állapota nem jelent meg: ${nodes().map { it.text }}", condition())
    }

    private fun launch() {
        // Nincs Espresso-idle várakozás: az animációt még lejátszás közben vizsgáljuk.
        automation.executeShellCommand(
            "am start -n hu.kyokushin.trainer/.MainActivity -f 0x10008000",
        ).use { FileInputStream(it.fileDescriptor).bufferedReader().readText() }
        waitFor(5000) { skip() != null }
    }

    @Test
    fun intro_canBeSkipped_andDoesNotReturnAfterRecreation() {
        launch()
        var button = checkNotNull(skip())
        while (!button.isClickable && button.parent != null) button = button.parent
        assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        waitFor(3000) { skip() == null && nodes().any { it.text?.toString() == "Tudástár" } }
        instrumentation.runOnMainSync {
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                .filterIsInstance<MainActivity>().single().recreate()
        }
        waitFor(5000) { nodes().any { it.text?.toString() == "Tudástár" } }
        assertTrue(skip() == null)
    }

    @Test
    fun intro_finishesWithoutInteraction() {
        launch()
        // A 17 mp-es védőidőzítő előtt kell eltűnnie: a JS-híd fejezi be.
        waitFor(16_000) { skip() == null && nodes().any { it.text?.toString() == "Tudástár" } }
    }
}
