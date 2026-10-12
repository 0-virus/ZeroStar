package com.zerostar.game

import android.app.Application
import com.zerostar.game.data.GameRepository
import java.io.File
import com.zerostar.game.game.SessionQueue
import kotlinx.coroutines.Dispatchers

class StarApplication : Application() {
    val commands = SessionQueue<suspend () -> Unit>(Dispatchers.Main.immediate) { it() }
    val repository by lazy { GameRepository(File(filesDir, "datastore/game.json")) }
}
