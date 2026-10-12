package com.zerostar.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.zerostar.game.ui.GameViewModel
import com.zerostar.game.ui.StarApp

class MainActivity : ComponentActivity() {
    private val game: GameViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { StarApp(game) }
    }
    override fun onStart() { super.onStart(); game.resume() }
    override fun onStop() { game.pause(); super.onStop() }
}
