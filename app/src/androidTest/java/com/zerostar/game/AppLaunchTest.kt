package com.zerostar.game

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Rule
import org.junit.Test

class AppLaunchTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun launchesHome() {
        compose.onNodeWithText("별 키우기 · ZeroStar").assertIsDisplayed()
        compose.onNodeWithText("홈").assertIsDisplayed()
    }
}
