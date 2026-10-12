package com.zerostar.game

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.zerostar.game.data.GameSerializer
import com.zerostar.game.game.GameState
import com.zerostar.game.game.upgradeCostMilli
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in real screen-reader focus probe. Does not claim to verify audible speech. */
class AccessibilityServiceProbeTest {
    @Test fun talkBackMovesFocusThroughActualGame() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Optional real-service probe requires probeTalkBack", args.getString("probeTalkBack") == "true")
        val context = instrumentation.targetContext
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command: String): String = automation.executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).readBytes().toString(Charsets.UTF_8).trim()
        }
        assertEquals("Kernel input is restricted to a disposable emulator", "1", shell("getprop ro.kernel.qemu"))
        assertEquals("The emulator must provide root for kernel input", "0", shell("su 0 id -u"))
        val keyboard = args.getString("probeKeyboardDevice").orEmpty()
        require(keyboard.matches(Regex("/dev/input/event[0-9]+"))) { "Pass the keyboard device reported by getevent -pl" }
        val event = keyboard.substringAfterLast('/')
        assertEquals("AT Translated Set 2 keyboard", shell("su 0 cat /sys/class/input/$event/device/name"))
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val bindDeadline = android.os.SystemClock.uptimeMillis() + 10_000
        while (manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN)
                .none { it.id.contains("talkback", ignoreCase = true) } && android.os.SystemClock.uptimeMillis() < bindDeadline) {
            Thread.sleep(100)
        }
        assertTrue("TalkBack must be installed and enabled on the disposable emulator",
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).any { it.id.contains("talkback", ignoreCase = true) })
        val file = File(context.filesDir, "datastore/game.json")
        file.parentFile!!.mkdirs()
        // A valid paid-upgrade fixture keeps the next purchase disabled for minutes,
        // so emulator boot/SDK startup speed cannot erase the shortage explanation.
        file.writeText(GameSerializer.encode(GameState(upgradeLevel = 29,
            earnedMilli = (0 until 29).sumOf { upgradeCostMilli(it) },
            tutorialSeen = true, soundEnabled = false, reduceMotion = true)))
        val labels = mutableListOf<String>()
        fun chord(key: Int) {
            listOf("1 56 1", "0 0 0", "1 $key 1", "0 0 0",
                "1 $key 0", "0 0 0", "1 56 0", "0 0 0").forEach {
                shell("su 0 sendevent $keyboard $it")
            }
        }
        fun nodeText(node: AccessibilityNodeInfo?, depth: Int = 0): String {
            if (node == null || depth > 3) return ""
            val own = (node.text ?: node.contentDescription)?.toString().orEmpty()
            return own.ifBlank { (0 until node.childCount).map { nodeText(node.getChild(it), depth + 1) }
                .filter { it.isNotBlank() }.distinct().joinToString(" · ") }
        }
        fun next(): String {
            // `input keycombination` injects downstream of the accessibility input
            // filter. Kernel events traverse TalkBack, like this AVD's keyboard.
            chord(106) // Linux KEY_RIGHT, with Alt.
            Thread.sleep(450)
            val focused = automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
            val label = nodeText(focused)
            if (label.isNotBlank()) labels.add(label)
            Log.i("ZeroStarAccessibilityProbe", "focus=$label enabled=${focused?.isEnabled} class=${focused?.className}")
            return label
        }
        fun openGameTab(name: String) {
            repeat(40) {
                if (next() == name) {
                    val focused = automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                    assertEquals("Only activate this game's named tab", context.packageName, focused?.packageName?.toString())
                    chord(28) // Linux KEY_ENTER, with Alt. Never activate advertising.
                    Thread.sleep(500)
                    return
                }
            }
            fail("TalkBack could not reach the game tab $name")
        }
        ActivityScenario.launch(MainActivity::class.java).use {
            val readyDeadline = android.os.SystemClock.uptimeMillis() + 20_000
            while (automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY) == null &&
                android.os.SystemClock.uptimeMillis() < readyDeadline) Thread.sleep(100)
            for (step in 0 until 48) {
                next()
                if (labels.distinct().size >= 5 && labels.any { it.contains("빛 모으기") } &&
                    labels.any { it.contains("별빛이 더 필요해요") }) break
            }
            assertTrue("Expected multiple named game controls in TalkBack traversal", labels.distinct().size >= 5)
            assertTrue("Game currency/rate must be reachable", labels.any { it.contains("별빛") })
            assertTrue("Upgrade name and cost must be exposed", labels.any { it.contains("빛 모으기") && it.contains("별빛") && it.any(Char::isDigit) })
            assertTrue("Disabled upgrade reason must be reachable", labels.any { it.contains("별빛이 더 필요해요") })
            openGameTab("설정")
            val settings = List(12) { next() }
            assertTrue("Sound switch must be reachable", settings.any { it.contains("효과음") })
            assertTrue("Motion switch must be reachable", settings.any { it.contains("움직임 줄이기") })
            openGameTab("도감")
            val collection = List(16) { next() }
            assertTrue("Collection entries must be reachable", collection.any { it.contains("새벽별") })
            assertTrue("Collection lock state must be reachable", collection.any { it.contains("잠김") })
        }
    }
}
