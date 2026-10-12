package com.zerostar.game.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.common.RequestConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.AgeRestrictedTreatment
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.*
import com.zerostar.game.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private object AdsInitialization {
    private val mutex = Mutex()
    private var initialized = false
    suspend fun initialize(context: Context) = mutex.withLock {
        if (!initialized) withContext(Dispatchers.IO) {
            val config = RequestConfiguration.Builder()
                .setAgeRestrictedTreatment(AgeRestrictedTreatment.valueOf(BuildConfig.AD_AGE_TREATMENT))
                .setMaxAdContentRating(RequestConfiguration.MaxAdContentRating.MAX_AD_CONTENT_RATING_G)
                .build()
            MobileAds.initialize(context.applicationContext, InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).setRequestConfiguration(config).build()) {}
            initialized = true
        }
    }
}

class AdsConsent(private val activity: Activity, private val owner: LifecycleOwner) {
    private val information = UserMessagingPlatform.getConsentInformation(activity)
    var ready by mutableStateOf(false)
        private set
    var privacyRequired by mutableStateOf(false)
        private set
    private var closed = false
    private val gate = AdsGate(initialize = { done ->
        owner.lifecycleScope.launch {
            try { AdsInitialization.initialize(activity.applicationContext); done() }
            catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { ready = false }
        }
    }, changed = { updateReady() })
    private fun updateReady() { ready = gate.ready }
    private fun update(error: Boolean = false) {
        if (closed) return
        privacyRequired = information.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        // Fail closed when this launch cannot establish current consent.
        gate.update(!error && information.canRequestAds())
    }
    fun start() {
        val params = ConsentRequestParameters.Builder().setTagForUnderAgeOfConsent(BuildConfig.UMP_UNDER_AGE).build()
        information.requestConsentInfoUpdate(activity, params, {
            if (!closed) UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error -> update(error != null) }
        }, { update(error = true) })
    }
    fun showPrivacyOptions() {
        gate.update(false)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error -> update(error != null) }
    }
    fun close() { closed = true; gate.close() }
}

