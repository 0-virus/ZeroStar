package com.zerostar.game

import com.zerostar.game.ads.AdsGate
import com.zerostar.game.game.*
import org.junit.Assert.*
import org.junit.Test

class AdsFlowTest {
    @Test fun cannotRequestAndFailureNeverInitialize() {
        var initializations = 0
        val gate = AdsGate({ initializations++; it() }, {})
        gate.update(false)
        gate.update(false)
        assertEquals(0, initializations)
        assertFalse(gate.ready)
    }
    @Test fun duplicateConsentCallbacksInitializeOnlyOnceAndRevokeHidesAd() {
        var initializations = 0
        var complete: (() -> Unit)? = null
        val gate = AdsGate({ initializations++; complete = it }, {})
        gate.update(true); gate.update(true)
        assertEquals(1, initializations)
        assertFalse(gate.ready)
        complete!!()
        assertTrue(gate.ready)
        gate.update(false)
        assertFalse(gate.ready)
        gate.update(true)
        assertEquals(1, initializations)
        assertTrue(gate.ready)
    }
    @Test fun callbackAfterRevocationOrDestructionCannotDisplayBanner() {
        var complete: (() -> Unit)? = null
        val gate = AdsGate({ complete = it }, {})
        gate.update(true); gate.update(false); complete!!()
        assertFalse(gate.ready)
        gate.close(); gate.update(true)
        assertFalse(gate.ready)
    }
    @Test fun unavailableAdsCannotAlterGameEconomy() {
        val gate = AdsGate({ it() }, {})
        gate.update(false)
        val state = advance(GameState(), 3600)
        assertEquals(3600000L, state.balanceMilli)
        assertEquals(1, buyUpgrade(state).upgradeLevel)
    }
}
