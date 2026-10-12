package com.zerostar.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.zerostar.game.data.*
import com.zerostar.game.game.*
import com.zerostar.game.ui.*
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.TemporaryFolder

class GameFlowTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val temp = TemporaryFolder()
    private lateinit var repo: GameRepository
    @After fun close() { if (::repo.isInitialized) runBlocking { repo.close() } }
    private fun show(state: GameState, reward: ResumeResult? = null) {
        val file = File(temp.newFolder(), "game.json")
        file.writeText(GameSerializer.encode(state))
        repo = GameRepository(file)
        val ui = mutableStateOf(GameUiState(runBlocking { repo.resume(1000).getOrThrow().state }, reward = reward))
        compose.setContent {
            StarScreen(ui.value, onAction = { action ->
                ui.value = ui.value.copy(game = runBlocking { repo.dispatch(action, 0, 1000).getOrThrow() })
            }, onRetry = {}, onDismissReward = { ui.value = ui.value.copy(reward = null) })
        }
    }
    @Test fun upgradeAndNavigation() {
        show(GameState(balanceMilli = 20000, earnedMilli = 20000, lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithTag("upgrade").performScrollTo().performClick()
        compose.onNodeWithText("강화 1 / 30").assertExists()
        compose.onNodeWithText("도감", useUnmergedTree = true).performClick()
        compose.onNodeWithText("0 / 6개의 별").assertExists()
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.onNodeWithText("효과음").assertExists()
    }
    @Test fun registerConfirmsResetAndStartsNextStar() {
        show(GameState(earnedMilli = 500000000, lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithTag("register").performScrollTo().performClick()
        compose.onNodeWithText("다음 별 시작").performClick()
        compose.onNodeWithText("산호별").assertExists()
        compose.onNodeWithText("별씨").assertExists()
    }
    @Test fun lastStarEndsAndCannotRegisterTwice() {
        show(GameState(earnedMilli = 500000000, collectedCount = 5, lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithTag("register").performScrollTo().performClick()
        compose.onNodeWithText("도감 완성").performClick()
        compose.onNodeWithText("여섯 빛깔의 우주를 완성했어요.").assertExists()
        compose.onNodeWithTag("register").assertDoesNotExist()
    }
    @Test fun returnDismissDoesNotGrantAgainAndResetNeedsConfirmation() {
        val saved = GameState(balanceMilli = 3600000, earnedMilli = 3600000, lastSettledEpochMs = 1000, tutorialSeen = true)
        show(saved, ResumeResult(saved, 3600000, 3600))
        compose.onNodeWithText("확인").performClick()
        compose.onNodeWithTag("balance").assertTextContains("3,600", substring = true)
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.onNodeWithText("전체 초기화").performScrollTo().performClick()
        compose.onNodeWithText("취소").performClick()
        compose.onNodeWithText("홈", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("balance").assertTextContains("3,600", substring = true)
    }
}
