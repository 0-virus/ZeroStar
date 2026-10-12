package com.zerostar.game

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    private fun show(state: GameState, reward: ResumeResult? = null, banner: @Composable () -> Unit = {},
        privacyRequired: Boolean = false, onPrivacy: () -> Unit = {}) {
        val file = File(temp.newFolder(), "game.json")
        file.writeText(GameSerializer.encode(state))
        repo = GameRepository(file)
        val ui = mutableStateOf(GameUiState(runBlocking { repo.resume(1000).getOrThrow().state }, reward = reward))
        compose.setContent {
            StarScreen(ui.value, onAction = { action ->
                ui.value = ui.value.copy(game = runBlocking { repo.dispatch(action, 0, 1000).getOrThrow() })
            }, onRetry = {}, onDismissReward = { ui.value = ui.value.copy(reward = null) },
                privacyRequired = privacyRequired, onPrivacy = onPrivacy, banner = banner)
        }
    }
    @Test fun homeShowsUpgradeWithoutScrollingWithBanner() {
        show(GameState(lastSettledEpochMs = 1000, tutorialSeen = true), banner = { Box(Modifier.height(60.dp)) })
        compose.onNodeWithTag("upgrade").assertIsDisplayed()
    }
    @Test fun disabledUpgradeHasReadableReasonAndAdequateTouchTarget() {
        show(GameState(lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithTag("upgrade").performScrollTo()
            .assertIsNotEnabled().assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("빛 모으기 · 20 별빛").assertExists()
        compose.onNodeWithText("20 별빛이 더 필요해요").performScrollTo().assertIsDisplayed()
        // Production text is not a live region that would speak every 1Hz tick.
        val semantics = compose.onNodeWithTag("balance").fetchSemanticsNode().config
        org.junit.Assert.assertFalse(semantics.contains(androidx.compose.ui.semantics.SemanticsProperties.LiveRegion))
    }
    @Test fun settingsHaveNamedToggleSemanticsAndPersistChanges() {
        show(GameState(lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.onNodeWithText("효과음").assertIsToggleable().assertIsOn().performClick().assertIsOff()
        compose.onNodeWithText("움직임 줄이기").assertIsToggleable().assertIsOff().performClick().assertIsOn()
        compose.onNodeWithText("광고 개인정보 선택").assertDoesNotExist()
    }
    @Test fun requiredPrivacyOptionsEntryInvokesTheSdkBoundary() {
        var openings = 0
        show(GameState(lastSettledEpochMs = 1000, tutorialSeen = true),
            privacyRequired = true, onPrivacy = { openings++ })
        compose.onNodeWithText("설정", useUnmergedTree = true).performClick()
        compose.onNodeWithText("광고 개인정보 선택").performScrollTo().performClick()
        org.junit.Assert.assertEquals(1, openings)
    }
    @Test fun collectedDescriptionCanScrollIntoView() {
        show(GameState(collectedCount = 1, lastSettledEpochMs = 1000, tutorialSeen = true))
        compose.onNodeWithText("도감", useUnmergedTree = true).performClick()
        compose.onNodeWithText("획득 완료").performScrollTo().performClick()
        val description = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext.resources.getStringArray(R.array.star_descriptions)[0]
        compose.onNodeWithText(description).performScrollTo().assertIsDisplayed()
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
