package com.zerostar.game

import com.zerostar.game.game.*
import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {
    @Test fun initialProductionAndPurchase() {
        val initial = GameState()
        assertEquals(1000L, productionMilliPerSecond(initial))
        assertEquals(20000L, upgradeCostMilli(0))
        assertEquals(60000L, advance(initial, 60).balanceMilli)
        val bought = buyUpgrade(initial.copy(balanceMilli = 20000, earnedMilli = 20000))
        assertEquals(0L, bought.balanceMilli)
        assertEquals(1, bought.upgradeLevel)
        assertEquals(20000L, bought.earnedMilli)
        assertEquals(initial, buyUpgrade(initial))
    }
    @Test fun everyStageBoundary() {
        val boundaries = listOf(100000L, 1000000L, 10000000L, 100000000L, 500000000L)
        boundaries.forEachIndexed { i, value ->
            assertEquals(i, stageOf(value - 1))
            assertEquals(i + 1, stageOf(value))
        }
    }
    @Test fun costsAndLimits() {
        val expected = listOf(20,25,32,40,49,62,77,96,120,150,187,233,292,364,455,569,711,889,1111,1388,1735,2169,2711,3389,4236,5294,6618,8272,10340,12925)
        expected.forEachIndexed { i, cost -> assertEquals(cost * 1000L, upgradeCostMilli(i)) }
        assertThrows(IllegalArgumentException::class.java) { upgradeCostMilli(-1) }
        assertThrows(IllegalArgumentException::class.java) { upgradeCostMilli(30) }
        assertThrows(IllegalArgumentException::class.java) { advance(GameState(), -1) }
        val max = GameState(upgradeLevel = 30, balanceMilli = MAX_MILLI)
        assertEquals(max, buyUpgrade(max))
        val saturated = advance(max, Long.MAX_VALUE)
        assertEquals(MAX_MILLI, saturated.balanceMilli)
        assertEquals(MAX_MILLI, saturated.earnedMilli)
    }
    @Test fun fifthAndSixthRegistrations() {
        val complete = GameState(earnedMilli = 500000000, balanceMilli = 9000, upgradeLevel = 12, collectedCount = 4)
        val fifth = registerStar(complete)
        assertEquals(5, fifth.collectedCount)
        assertEquals(0L, fifth.earnedMilli)
        assertEquals(2250L, productionMilliPerSecond(fifth))
        val last = fifth.copy(earnedMilli = 500000000, balanceMilli = 89000, upgradeLevel = 10)
        val ending = registerStar(last)
        assertEquals(6, ending.collectedCount)
        assertEquals(last.earnedMilli, ending.earnedMilli)
        assertEquals(last.balanceMilli, ending.balanceMilli)
        assertEquals(0L, productionMilliPerSecond(ending))
        assertEquals(ending, advance(ending, Long.MAX_VALUE))
        assertEquals(ending, registerStar(ending))
        assertEquals(ending, buyUpgrade(ending))
        assertEquals(GameState(), registerStar(GameState()))
        assertEquals(complete, buyUpgrade(complete))
    }
}
