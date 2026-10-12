package com.zerostar.game.game

import kotlinx.serialization.Serializable

const val MAX_MILLI = 1_000_000_000_000_000L
@Serializable
data class GameState(
    val schemaVersion: Int = 1,
    val balanceMilli: Long = 0,
    val earnedMilli: Long = 0,
    val upgradeLevel: Int = 0,
    val collectedCount: Int = 0,
    val lastSettledEpochMs: Long = 0,
    val soundEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    val tutorialSeen: Boolean = false,
)
