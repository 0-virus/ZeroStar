package com.zerostar.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.zerostar.game.ui.GameViewModel
import com.zerostar.game.ui.StarApp
import com.zerostar.game.ads.AdsConsent
import com.zerostar.game.ads.BannerAd

class MainActivity : ComponentActivity() {
    private lateinit var ads: AdsConsent
    private val game: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ads = AdsConsent(this, this)
        setContent { StarApp(game, ads.privacyRequired, ads::showPrivacyOptions) { BannerAd(ads.ready) } }
        ads.start()
    }
    override fun onStart() { super.onStart(); game.resume() }
    override fun onDestroy() { ads.close(); super.onDestroy() }
    override fun onStop() { game.pause(); super.onStop() }
}
