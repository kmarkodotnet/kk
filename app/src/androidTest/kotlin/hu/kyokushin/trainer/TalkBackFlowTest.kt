package hu.kyokushin.trainer

import android.app.UiAutomation
import android.content.Context
import android.os.SystemClock
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileInputStream

/** TalkBack futása mellett, az Android accessibility csatornáján végzett teljes menet. */
class TalkBackFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation: UiAutomation
        get() = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)

    private fun shell(command: String): String = automation.executeShellCommand(command).use {
        FileInputStream(it.fileDescriptor).bufferedReader().readText().trim()
    }

    private fun nodes(root: AccessibilityNodeInfo? = automation.rootInActiveWindow): List<AccessibilityNodeInfo> {
        if (root == null) return emptyList()
        return listOf(root) + (0 until root.childCount).flatMap { nodes(root.getChild(it)) }
    }

    private fun waitNode(label: String, scroll: Boolean = true): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 15_000
        while (SystemClock.uptimeMillis() < deadline) {
            val all = nodes()
            all.firstOrNull { it.text?.toString() in listOf("Don’t allow", "Don't allow") }?.let {
                it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                SystemClock.sleep(200)
            }
            all.firstOrNull {
                it.isVisibleToUser &&
                    (it.text?.toString() == label || it.contentDescription?.toString() == label)
            }?.let { return it }
            if (scroll) {
                all.filter {
                    it.isScrollable
                }.forEach { it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }
            }
            SystemClock.sleep(150)
        }
        error("Nem érhető el accessibility útján: $label. ${nodes().map { it.text ?: it.contentDescription }}")
    }

    private fun activate(label: String) {
        var node = waitNode(label)
        while (!node.isClickable && node.parent != null) node = node.parent
        node.refresh()
        assertTrue(
            "Nem fókuszálható: $label; $node",
            node.isAccessibilityFocused || node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS),
        )
        assertTrue("Nem aktiválható: $label", node.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        SystemClock.sleep(250)
    }

    @Test
    fun fullQuizViaAccessibilityWithTalkBack_NFR04_TR06() {
        val previousServices = shell("settings get secure enabled_accessibility_services")
        val previousEnabled = shell("settings get secure accessibility_enabled")
        val talkBack = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
        try {
            val services = if (previousServices == "null" ||
                previousServices.isBlank()
            ) {
                talkBack
            } else {
                "$previousServices:$talkBack"
            }
            shell("settings put secure enabled_accessibility_services $services")
            shell("settings put secure accessibility_enabled 1")
            val manager = instrumentation.targetContext
                .getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val deadline = SystemClock.uptimeMillis() + 10_000
            while (manager.getEnabledAccessibilityServiceList(-1).none {
                    it.id.contains("talkback", ignoreCase = true)
                } &&
                SystemClock.uptimeMillis() < deadline
            ) {
                SystemClock.sleep(100)
            }
            assertTrue(
                "A TalkBack szolgáltatásnak ténylegesen futnia kell",
                manager.getEnabledAccessibilityServiceList(-1).any {
                    it.id.contains("talkback", ignoreCase = true)
                },
            )
            ActivityScenario.launch(MainActivity::class.java).use {
                activate("Kvíz")
                activate("Etikett és köszönések")
                activate("5")
                activate("Kvíz indítása")
                repeat(5) { index ->
                    waitNode("${index + 1} / 5", scroll = false)
                    val deadlineOptions = SystemClock.uptimeMillis() + 10_000
                    var options = nodes().filter { it.className?.toString() == "android.widget.RadioButton" }
                    while (options.isEmpty() && SystemClock.uptimeMillis() < deadlineOptions) {
                        nodes().filter {
                            it.isScrollable
                        }.forEach { it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }
                        SystemClock.sleep(100)
                        options = nodes().filter { it.className?.toString() == "android.widget.RadioButton" }
                    }
                    assertTrue("A válaszok accessibility opcióként elérhetők", options.isNotEmpty())
                    options.forEach { option ->
                        assertFalse(
                            "Válasz előtt nem szivároghat ki helyesség",
                            option.stateDescription?.toString()?.contains("helyes", ignoreCase = true) == true,
                        )
                        assertFalse(option.isSelected)
                    }
                    var option = options.first { it.isVisibleToUser }
                    while (!option.isClickable && option.parent != null) option = option.parent
                    option.refresh()
                    assertTrue(
                        option.isAccessibilityFocused ||
                            option.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS),
                    )
                    assertTrue(
                        "Nem kattintható opció: $option",
                        option.performAction(AccessibilityNodeInfo.ACTION_CLICK),
                    )
                    activate(if (index == 4) "Eredmény" else "Következő")
                }
                waitNode("Új kvíz")
            }
        } finally {
            shell(
                if (previousServices ==
                    "null"
                ) {
                    "settings delete secure enabled_accessibility_services"
                } else {
                    "settings put secure enabled_accessibility_services $previousServices"
                },
            )
            shell(
                if (previousEnabled ==
                    "null"
                ) {
                    "settings delete secure accessibility_enabled"
                } else {
                    "settings put secure accessibility_enabled $previousEnabled"
                },
            )
        }
    }
}
