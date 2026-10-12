package com.zerostar.game

import com.zerostar.game.game.*
import org.junit.Assert.*
import org.junit.Test

class EconomySimulationTest {
    private fun buyAll(state: GameState): GameState {
        var s = state
        while (true) {
            val next = buyUpgrade(s)
            if (next == s) return s
            s = next
        }
    }
    @Test fun firstStarImmediateUpgrades() {
        var s = GameState()
        var first = 0
        val milestones = mutableListOf<Int>()
        var stage = 0
        var seconds = 0
        while (stageOf(s.earnedMilli) < 5 && seconds < 10000) {
            seconds++
            s = advance(s, 1)
            if (stageOf(s.earnedMilli) > stage) {
                stage = stageOf(s.earnedMilli)
                milestones += seconds
            }
            if (stage < 5) {
                val next = buyAll(s)
                if (first == 0 && next.upgradeLevel > 0) first = seconds
                s = next
            }
        }
        assertEquals(20, first)
        assertEquals(listOf(39,95,346,1943,8501), milestones)
        println("first upgrade=$first; stages=$milestones")
    }
    private fun intermittent(hours: Long): Long {
        var s = GameState()
        repeat(180) { s = buyAll(advance(s, 1)) }
        var elapsed = 180L
        while (s.collectedCount < 6 && elapsed < 1000000) {
            elapsed += hours * 3600
            s = advance(s, minOf(hours * 3600, 28800))
            s = if (stageOf(s.earnedMilli) == 5) registerStar(s) else buyAll(s)
        }
        assertEquals(6, s.collectedCount)
        return elapsed
    }
    @Test fun fourEightAndTwelveHourReturns() {
        assertEquals(158580L, intermittent(4))
        assertEquals(316980L, intermittent(8))
        val twelve = intermittent(12)
        assertEquals(475380L, twelve)
        println("collection 4h=158580s; 8h=316980s; 12h=${twelve}s")
    }
}
