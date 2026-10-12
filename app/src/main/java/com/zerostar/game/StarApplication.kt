package com.zerostar.game

import android.app.Application
import com.zerostar.game.data.GameRepository
import java.io.File

class StarApplication : Application() {
    val repository by lazy { GameRepository(File(filesDir, "datastore/game.json")) }
}
