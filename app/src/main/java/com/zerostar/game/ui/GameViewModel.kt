package com.zerostar.game.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerostar.game.StarApplication
import com.zerostar.game.data.*
import com.zerostar.game.game.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// Transient state only; every game mutation is committed through GameRepository.
data class GameUiState(
    val game: GameState? = null,
    val error: Boolean = false,
    val reward: ResumeResult? = null,
)
enum class GameSound { Upgrade, Growth, Collection }

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = (application as StarApplication).repository
    private val clock = ForegroundClock()
    private var saved: GameState? = null
    private var active = false
    private val _ui = MutableStateFlow(GameUiState())
    val ui = _ui.asStateFlow()
    private val soundChannel = Channel<GameSound>(Channel.BUFFERED)
    val sounds = soundChannel.receiveAsFlow()
    private data class Event(val kind: Int, val action: GameAction? = null, val mono: Long = SystemClock.elapsedRealtime(), val epoch: Long = System.currentTimeMillis())
    private val events = Channel<Event>(Channel.UNLIMITED)

    init {
        viewModelScope.launch { for (event in events) handle(event) }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (active) events.send(Event(2))
            }
        }
    }
    fun resume() { events.trySend(Event(0)) }
    fun pause() { events.trySend(Event(1)) }
    fun dispatch(action: GameAction) { events.trySend(Event(3, action)) }
    fun dismissReward() { _ui.value = _ui.value.copy(reward = null) }

    private suspend fun handle(event: Event) {
        when (event.kind) {
            0 -> {
                if (active && !_ui.value.error) return
                repository.resume(event.epoch).fold(onSuccess = { result ->
                    saved = result.state
                    clock.stop(event.mono)
                    clock.start(event.mono)
                    active = true
                    _ui.value = GameUiState(result.state, reward = result.takeIf { it.seconds > 0 && it.rewardMilli > 0 })
                }, onFailure = { fail() })
            }
            1 -> {
                if (!active) return
                active = false
                val seconds = clock.stop(event.mono)
                repository.pause(seconds, event.epoch).fold(onSuccess = {
                    saved = it
                    _ui.value = _ui.value.copy(game = it)
                }, onFailure = { fail() })
            }
            2 -> {
                if (!active || _ui.value.error) return
                val base = saved ?: return
                val projected = advance(base, clock.seconds(event.mono))
                if (stageOf(projected.earnedMilli) > stageOf(_ui.value.game?.earnedMilli ?: base.earnedMilli)) {
                    // Growth feedback follows persistence, just like purchases.
                    save(GameAction.Checkpoint, event)
                } else if (clock.seconds(event.mono) >= 30) save(GameAction.Checkpoint, event)
                else _ui.value = _ui.value.copy(game = projected)
            }
            3 -> {
                if (event.action == GameAction.Reset || (active && !_ui.value.error)) save(event.action!!, event)
            }
        }
    }
    private suspend fun save(action: GameAction, event: Event) {
        val before = saved
        repository.dispatch(action, clock.seconds(event.mono), event.epoch).fold(onSuccess = { next ->
            saved = next
            if (action == GameAction.Reset || (before != null && next.collectedCount > before.collectedCount)) {
                clock.stop(event.mono)
                clock.start(event.mono)
            } else clock.commit(event.mono)
            active = true
            _ui.value = _ui.value.copy(game = next, error = false, reward = if (action == GameAction.Reset) null else _ui.value.reward)
            if (next.soundEnabled && before != null) {
                val sound = when {
                    next.collectedCount > before.collectedCount -> GameSound.Collection
                    next.upgradeLevel > before.upgradeLevel -> GameSound.Upgrade
                    stageOf(next.earnedMilli) > stageOf(before.earnedMilli) -> GameSound.Growth
                    else -> null
                }
                if (sound != null) soundChannel.trySend(sound)
            }
        }, onFailure = { fail() })
    }
    private fun fail() {
        active = false
        clock.stop(SystemClock.elapsedRealtime())
        _ui.value = _ui.value.copy(error = true, game = saved)
    }
}
