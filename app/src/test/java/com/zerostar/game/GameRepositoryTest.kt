package com.zerostar.game

import com.zerostar.game.data.*
import com.zerostar.game.game.*
import java.io.File
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GameRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private suspend fun withRepo(initial: GameState = GameState(lastSettledEpochMs = 1000), block: suspend (GameRepository) -> Unit) {
        val file = File(folder.newFolder(), "game.json")
        file.writeText(GameSerializer.encode(initial))
        val repo = GameRepository(file)
        try { block(repo) } finally { repo.close() }
    }
    @Test fun cappedReturnAndDuplicateEvents() = runTest {
        withRepo { repo ->
            val first = repo.resume(86401000).getOrThrow()
            assertEquals(28800000L, first.state.balanceMilli)
            assertEquals(28800L, first.seconds)
            assertEquals(0L, repo.resume(86401000).getOrThrow().rewardMilli)
            repo.pause(0, 86401000).getOrThrow()
            assertEquals(0L, repo.resume(86401000).getOrThrow().rewardMilli)
        }
    }
    @Test fun hourAndBackwardsClock() = runTest {
        withRepo { repo ->
            assertEquals(3600000L, repo.resume(3601000).getOrThrow().rewardMilli)
            repo.pause(0, 3601000).getOrThrow()
            val backwards = repo.resume(2000).getOrThrow()
            assertEquals(0L, backwards.rewardMilli)
            assertEquals(2000L, backwards.state.lastSettledEpochMs)
        }
    }
    @Test fun concurrentPurchasesAndRegistration() = runTest {
        withRepo(GameState(balanceMilli = 20000, earnedMilli = 20000, lastSettledEpochMs = 1000)) { repo ->
            repo.resume(1000).getOrThrow()
            coroutineScope { repeat(2) { launch { repo.dispatch(GameAction.BuyUpgrade, 0, 1000).getOrThrow() } } }
            assertEquals(1, repo.state.first().upgradeLevel)
            assertEquals(0L, repo.state.first().balanceMilli)
        }
        withRepo(GameState(earnedMilli = 500000000, lastSettledEpochMs = 1000)) { repo ->
            repo.resume(1000).getOrThrow()
            coroutineScope { repeat(2) { launch { repo.dispatch(GameAction.RegisterStar, 0, 1000).getOrThrow() } } }
            assertEquals(1, repo.state.first().collectedCount)
        }
    }
    @Test fun returnThenPurchaseUsesOldRateAndPausedCommandsCannotSpend() = runTest {
        withRepo { repo ->
            assertTrue(repo.dispatch(GameAction.BuyUpgrade, 0, 21000).isFailure)
            repo.resume(21000).getOrThrow()
            repo.dispatch(GameAction.BuyUpgrade, 0, 21000).getOrThrow()
            assertEquals(0L, repo.state.first().balanceMilli)
            assertEquals(1, repo.state.first().upgradeLevel)
            assertEquals(0L, repo.resume(21000).getOrThrow().rewardMilli)
        }
    }
    @Test fun checkpointAndBackgroundAreNotDuplicated() = runTest {
        withRepo { repo ->
            repo.resume(1000).getOrThrow()
            repo.dispatch(GameAction.Checkpoint, 30, 31000).getOrThrow()
            repo.pause(5, 36000).getOrThrow()
            repo.pause(5, 36000).getOrThrow()
            assertEquals(35000L, repo.state.first().balanceMilli)
            assertEquals(10000L, repo.resume(46000).getOrThrow().rewardMilli)
        }
    }
    @Test fun missingOnlyCreatesNewGameAndResetDoesNotSettle() = runTest {
        val repo = GameRepository(File(folder.newFolder(), "new.json"))
        try {
            assertEquals(0L, repo.resume(999999).getOrThrow().rewardMilli)
            repo.dispatch(GameAction.Checkpoint, 50, 1049999).getOrThrow()
            assertEquals(0L, repo.dispatch(GameAction.Reset, 900, 1050000).getOrThrow().balanceMilli)
        } finally { repo.close() }
    }
    @Test fun corruptFutureAndInvalidSavesPreserveBytesUntilExplicitReset() = runTest {
        for (bad in listOf("not json", "{}", GameSerializer.encode(GameState()).replace("\"schemaVersion\":1", "\"schemaVersion\":2"), GameSerializer.encode(GameState()).replace("\"upgradeLevel\":0", "\"upgradeLevel\":31"))) {
            val file = File(folder.newFolder(), "game.json")
            file.writeText(bad)
            val repo = GameRepository(file)
            try {
                assertTrue(repo.resume(1000).isFailure)
                assertEquals(bad, file.readText())
                assertTrue(repo.dispatch(GameAction.BuyUpgrade, 0, 1000).isFailure)
                assertTrue(repo.dispatch(GameAction.Reset, 0, 1000).isSuccess)
                assertEquals(0L, repo.state.first().balanceMilli)
            } finally { repo.close() }
        }
    }
    @Test fun unwritableDestinationDoesNotReportSuccessAndRetryRecovers() = runTest {
        val blocker = folder.newFile("blocked")
        val repo = GameRepository(File(blocker, "game.json"))
        try {
            assertTrue(repo.resume(1000).isFailure)
            assertTrue(repo.dispatch(GameAction.BuyUpgrade, 0, 1000).isFailure)
            assertTrue(blocker.delete())
            assertTrue(blocker.mkdir())
            assertTrue(repo.resume(1000).isSuccess)
        } finally { repo.close() }
    }
}

