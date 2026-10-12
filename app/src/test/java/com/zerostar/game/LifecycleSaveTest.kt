package com.zerostar.game

import com.zerostar.game.game.ForegroundClock
import org.junit.Assert.*
import org.junit.Test

class LifecycleSaveTest {
    @Test fun subsecondRemainderSurvivesCheckpointsAndPurchases() {
        val clock = ForegroundClock()
        clock.start(100)
        assertEquals(0L, clock.seconds(1099))
        assertEquals(1L, clock.seconds(1100))
        clock.commit(1350)
        assertEquals(0L, clock.seconds(2099))
        assertEquals(1L, clock.seconds(2100))
        clock.commit(30100)
        assertEquals(1L, clock.seconds(31100))
    }
    @Test fun repeatedBackgroundDoesNotConsumeTimeAndRestartClearsFraction() {
        val clock = ForegroundClock()
        clock.start(100)
        assertEquals(2L, clock.stop(2600))
        assertEquals(0L, clock.stop(9999))
        clock.start(11000)
        assertEquals(0L, clock.seconds(11999))
        assertEquals(1L, clock.seconds(12000))
    }
}
