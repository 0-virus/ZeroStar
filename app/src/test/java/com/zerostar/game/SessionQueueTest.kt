package com.zerostar.game

import com.zerostar.game.game.SessionQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionQueueTest {
    @Test fun cancelledUiOwnerCannotDropPauseOrReorderNewOwnerResume() = runTest {
        val diskReady = CompletableDeferred<Unit>()
        val processed = mutableListOf<String>()
        val queue = SessionQueue<suspend () -> Unit>(StandardTestDispatcher(testScheduler)) { it() }
        val owner = launch { queue.send { diskReady.await(); processed += "pause" } }
        runCurrent()
        owner.cancelAndJoin()
        queue.send { processed += "resume" }
        runCurrent()
        assertTrue(processed.isEmpty())
        diskReady.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("pause", "resume"), processed)
        queue.close()
        advanceUntilIdle()
    }
    @Test fun finishingOwnerDrainsAlreadyQueuedPauseBeforeReopen() = runTest {
        val started = CompletableDeferred<Unit>()
        val diskReady = CompletableDeferred<Unit>()
        val processed = mutableListOf<Int>()
        val queue = SessionQueue<Int>(StandardTestDispatcher(testScheduler)) { command ->
            if (command == 1) { started.complete(Unit); diskReady.await() }
            processed += command
        }
        queue.send(1)
        queue.send(2)
        runCurrent()
        started.await()
        queue.close()
        diskReady.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(1, 2), processed)
        assertFalse(queue.send(3))
    }
}
