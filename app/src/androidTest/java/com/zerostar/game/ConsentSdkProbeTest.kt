package com.zerostar.game

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in diagnostic against Google's test app, never a release entry point. */
class ConsentSdkProbeTest {
    @Test fun requestDebugGeography() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue("Optional SDK network probe requires probeGeography", args.containsKey("probeGeography"))
        val geography = args.getString("probeGeography")!!
        val done = CountDownLatch(1)
        var error: String? = null
        var status = ConsentInformation.ConsentStatus.UNKNOWN
        var available = false
        var allowed = false
        var privacy = ConsentInformation.PrivacyOptionsRequirementStatus.UNKNOWN
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val info = UserMessagingPlatform.getConsentInformation(activity)
                info.reset()
                val debug = ConsentDebugSettings.Builder(activity)
                    .setDebugGeography(if (geography == "EEA") ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA else ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_DISABLED)
                args.getString("probeDeviceHash")?.let { debug.addTestDeviceHashedId(it) }
                val params = ConsentRequestParameters.Builder().setConsentDebugSettings(debug.build()).build()
                info.requestConsentInfoUpdate(activity, params, {
                    status = info.consentStatus
                    available = info.isConsentFormAvailable
                    allowed = info.canRequestAds()
                    privacy = info.privacyOptionsRequirementStatus
                    done.countDown()
                }, { failure ->
                    status = info.consentStatus
                    available = info.isConsentFormAvailable
                    allowed = info.canRequestAds()
                    error = "${failure.errorCode}: ${failure.message}"
                    done.countDown()
                })
            }
            assertTrue("SDK callback timeout", done.await(45, TimeUnit.SECONDS))
            Log.i("ZeroStarConsentProbe", "geography=$geography status=$status form=$available ads=$allowed privacy=$privacy error=$error")
            if (args.getString("probeExpectError") == "true") {
                assertNotNull("Disconnected SDK request should fail", error)
                assertFalse(allowed)
            } else {
                assertNull("SDK consent update error", error)
                if (geography == "EEA") {
                    assertEquals(ConsentInformation.ConsentStatus.REQUIRED, status)
                    assertTrue("A test message must be configured before testing the consent form", available)
                    assertFalse(allowed)
                } else assertTrue(allowed)
            }
            if (args.getString("probeShowForm") == "true" && error == null) {
                val dismissed = CountDownLatch(1)
                scenario.onActivity { activity ->
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { failure ->
                        error = failure?.let { "${it.errorCode}: ${it.message}" }
                        val info = UserMessagingPlatform.getConsentInformation(activity)
                        Log.i("ZeroStarConsentProbe", "afterForm status=${info.consentStatus} ads=${info.canRequestAds()} privacy=${info.privacyOptionsRequirementStatus} error=$error")
                        dismissed.countDown()
                    }
                }
                assertTrue("Test form dismissal timeout", dismissed.await(120, TimeUnit.SECONDS))
                assertNull(error)
            }
            if (args.getString("probeShowPrivacy") == "true" && error == null) {
                val dismissed = CountDownLatch(1)
                scenario.onActivity { activity ->
                    UserMessagingPlatform.showPrivacyOptionsForm(activity) { failure ->
                        error = failure?.let { "${it.errorCode}: ${it.message}" }
                        val info = UserMessagingPlatform.getConsentInformation(activity)
                        Log.i("ZeroStarConsentProbe", "afterPrivacy status=${info.consentStatus} ads=${info.canRequestAds()} privacy=${info.privacyOptionsRequirementStatus} error=$error")
                        dismissed.countDown()
                    }
                }
                assertTrue("Test privacy options dismissal timeout", dismissed.await(120, TimeUnit.SECONDS))
                assertNull(error)
            }
        }
    }
}
