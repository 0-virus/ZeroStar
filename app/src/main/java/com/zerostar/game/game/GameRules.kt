package com.zerostar.game.game

import kotlin.math.ceil
import kotlin.math.pow

val STAGE_THRESHOLDS = listOf(0L, 100_000L, 1_000_000L, 10_000_000L, 100_000_000L, 500_000_000L)
fun productionMilliPerSecond(state: GameState): Long =
    if (state.collectedCount == 6) 0 else (1L + 2L * state.upgradeLevel) * (1000L + 250L * state.collectedCount)

fun upgradeCostMilli(level: Int): Long {
    require(level in 0..29)
    return ceil(20.0 * 1.25.pow(level)).toLong() * 1000
}

fun advance(state: GameState, elapsedSeconds: Long): GameState {
    require(elapsedSeconds >= 0)
    val rate = productionMilliPerSecond(state)
    if (rate == 0L || elapsedSeconds == 0L) return state
    val added = if (elapsedSeconds > MAX_MILLI / rate) MAX_MILLI else elapsedSeconds * rate
    return state.copy(
        balanceMilli = state.balanceMilli + minOf(added, MAX_MILLI - state.balanceMilli),
        earnedMilli = state.earnedMilli + minOf(added, MAX_MILLI - state.earnedMilli),
    )
}

fun buyUpgrade(state: GameState): GameState {
    if (state.upgradeLevel >= 30 || stageOf(state.earnedMilli) == 5 || state.collectedCount == 6) return state
    val cost = upgradeCostMilli(state.upgradeLevel)
    if (state.balanceMilli < cost) return state
    return state.copy(balanceMilli = state.balanceMilli - cost, upgradeLevel = state.upgradeLevel + 1)
}

fun registerStar(state: GameState): GameState {
    if (stageOf(state.earnedMilli) != 5 || state.collectedCount == 6) return state
    val count = state.collectedCount + 1
    return if (count == 6) state.copy(collectedCount = count)
    else state.copy(collectedCount = count, balanceMilli = 0, earnedMilli = 0, upgradeLevel = 0)
}

fun stageOf(earnedMilli: Long): Int {
    require(earnedMilli >= 0)
    return STAGE_THRESHOLDS.indexOfLast { earnedMilli >= it }
}
