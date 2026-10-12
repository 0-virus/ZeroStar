package com.zerostar.game.game

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/** Process-owned FIFO: Activity/ViewModel cancellation cannot cancel committed commands. */
class SessionQueue<T>(dispatcher: CoroutineDispatcher, handler: suspend (T) -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val channel = Channel<T>(Channel.UNLIMITED)
    init {
        scope.launch {
            try { for (event in channel) handler(event) }
            finally { scope.cancel() }
        }
    }
    fun send(event: T): Boolean = channel.trySend(event).isSuccess
    /** Stop accepting commands, but finish writes already accepted. */
    fun close() { channel.close() }
}
