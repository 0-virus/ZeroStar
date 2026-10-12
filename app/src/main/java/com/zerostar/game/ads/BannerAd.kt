package com.zerostar.game.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.google.android.libraries.ads.mobile.sdk.banner.*
import com.google.android.libraries.ads.mobile.sdk.common.*
import com.zerostar.game.BuildConfig

@Composable fun BannerAd(allowed: Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value
    if (!allowed || !lifecycle.isAtLeast(Lifecycle.State.RESUMED)) return
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val context = LocalContext.current
        val width = maxWidth.value.toInt().coerceAtLeast(1)
        val adView = remember(context, width) { AdView(context) }
        var loaded by remember(adView) { mutableStateOf(false) }
        var alive by remember(adView) { mutableStateOf(true) }
        val adSize = remember(context, width) { AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, width) }
        DisposableEffect(adView) {
            onDispose {
                alive = false
                (adView.parent as? ViewGroup)?.removeView(adView)
                adView.destroy()
            }
        }
        LaunchedEffect(adView) {
            adView.loadAd(BannerAdRequest.Builder(BuildConfig.ADMOB_BANNER_ID, adSize).build(), object : AdLoadCallback<com.google.android.libraries.ads.mobile.sdk.banner.BannerAd> {
                override fun onAdLoaded(ad: com.google.android.libraries.ads.mobile.sdk.banner.BannerAd) {
                    if (alive) loaded = true
                }
                override fun onAdFailedToLoad(adError: LoadAdError) { if (alive) loaded = false }
            })
        }
        AndroidView(factory = { adView }, modifier = Modifier.fillMaxWidth().height(if (loaded) adSize.height.dp else 0.dp))
    }
}
