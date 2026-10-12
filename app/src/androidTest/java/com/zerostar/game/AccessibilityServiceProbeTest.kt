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
import java.io.File
import java.io.FileInputStream
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Opt-in real screen-reader focus probe. Does not claim to verify audible speech. */
class AccessibilityServiceProbeTest {
    @Test fun talkBackMovesFocusThroughActualGame() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Optional real-service probe requires probeTalkBack", InstrumentationRegistry.getArguments().getString("probeTalkBack") == "true")
        val context = instrumentation.targetContext
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val manager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        assertTrue("TalkBack must be installed and enabled on the disposable emulator",
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).any { it.id.contains("talkback", ignoreCase = true) })
        val file = File(context.filesDir, "datastore/game.json")
        file.parentFile!!.mkdirs()
        file.writeText(GameSerializer.encode(GameState(tutorialSeen = true, soundEnabled = false, reduceMotion = true)))
        val labels = mutableListOf<String>()
        ActivityScenario.launch(MainActivity::class.java).use {
            Thread.sleep(2000)
            repeat(16) {
                // Official TalkBack keyboard navigation. Shell touch injection did not
                // traverse TalkBack on this image; do not count repeated focus as a pass.
                automation.executeShellCommand("input keycombination 57 22").use { descriptor ->
                    FileInputStream(descriptor.fileDescriptor).readBytes()
                }
                Thread.sleep(450)
                val focused = automation.rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                val label = (focused?.text ?: focused?.contentDescription)?.toString().orEmpty()
                if (label.isNotBlank()) labels.add(label)
                Log.i("ZeroStarAccessibilityProbe", "focus=$label enabled=${focused?.isEnabled} class=${focused?.className}")
            }
            assertTrue("Expected multiple named game controls in TalkBack traversal", labels.distinct().size >= 5)
            assertTrue("Game currency/rate must be reachable", labels.any { it.contains("별빛") })
        }
    }
}
