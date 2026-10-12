package com.zerostar.game.data

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.zerostar.game.game.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface GameAction {
    data object Checkpoint : GameAction
    data object BuyUpgrade : GameAction
    data object RegisterStar : GameAction
    data class SetSound(val enabled: Boolean) : GameAction
    data class SetReduceMotion(val enabled: Boolean) : GameAction
    data object MarkTutorialSeen : GameAction
    data object Reset : GameAction
}
data class ResumeResult(val state: GameState, val rewardMilli: Long, val seconds: Long)

/** Exactly one instance per app process; callers never write the file themselves. */
class GameRepository(file: File) {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.IO)
    private val mutex = Mutex()
    @Volatile private var explicitReset = false
    private var foreground = false
    private var failed = false
    private val store = DataStoreFactory.create(
        serializer = GameSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { error ->
            if (explicitReset) GameState() else throw error
        },
        scope = scope,
        produceFile = { file },
    )
    val state: Flow<GameState> = store.data

    suspend fun resume(nowEpochMs: Long): Result<ResumeResult> = mutex.withLock {
        attempt {
            require(nowEpochMs >= 0)
            if (foreground && !failed) return@attempt ResumeResult(state.first(), 0, 0)
            var reward = 0L
            var seconds = 0L
            val saved = store.updateData { old ->
                seconds = if (old.lastSettledEpochMs == 0L || nowEpochMs < old.lastSettledEpochMs) 0
                else minOf(nowEpochMs - old.lastSettledEpochMs, 28_800_000L) / 1000
                val next = advance(old, seconds)
                reward = next.balanceMilli - old.balanceMilli
                next.copy(lastSettledEpochMs = nowEpochMs)
            }
            foreground = true
            ResumeResult(saved, reward, seconds)
        }
    }

    suspend fun dispatch(action: GameAction, elapsedSeconds: Long, nowEpochMs: Long): Result<GameState> = mutex.withLock {
        attempt {
            check(action == GameAction.Reset || (foreground && !failed)) { "Save unavailable or app paused" }
            require(nowEpochMs >= 0 && elapsedSeconds >= 0)
            explicitReset = action == GameAction.Reset
            try {
                store.updateData { old ->
                    val settled = if (action == GameAction.Reset) GameState() else advance(old, elapsedSeconds)
                    val next = when (action) {
                        GameAction.Checkpoint -> settled
                        GameAction.BuyUpgrade -> buyUpgrade(settled)
                        GameAction.RegisterStar -> registerStar(settled)
                        is GameAction.SetSound -> settled.copy(soundEnabled = action.enabled)
                        is GameAction.SetReduceMotion -> settled.copy(reduceMotion = action.enabled)
                        GameAction.MarkTutorialSeen -> settled.copy(tutorialSeen = true)
                        GameAction.Reset -> settled
                    }
                    next.copy(lastSettledEpochMs = nowEpochMs)
                }.also { foreground = true }
            } finally { explicitReset = false }
        }
    }

    suspend fun pause(elapsedSeconds: Long, nowEpochMs: Long): Result<GameState> = mutex.withLock {
        attempt {
            if (!foreground || failed) return@attempt state.first()
            require(elapsedSeconds >= 0 && nowEpochMs >= 0)
            store.updateData { advance(it, elapsedSeconds).copy(lastSettledEpochMs = nowEpochMs) }
                .also { foreground = false }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        val value = block()
        failed = false
        Result.success(value)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (error: Exception) {
        failed = true
        Result.failure(error)
    }
    suspend fun close() { job.cancelAndJoin() }
}
