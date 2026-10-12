package com.zerostar.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.*
import com.zerostar.game.R
import com.zerostar.game.game.*
import java.text.NumberFormat
import java.util.Locale

fun whole(milli: Long): String = NumberFormat.getIntegerInstance(Locale.KOREA).format(milli / 1000)
fun rateText(milli: Long): String = java.math.BigDecimal.valueOf(milli, 3).stripTrailingZeros().toPlainString()
@Composable fun HomeScreen(state: GameState, onUpgrade: () -> Unit, onRegister: () -> Unit, onTutorial: () -> Unit) {
    val stage = stageOf(state.earnedMilli)
    PaperPanel(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.balance, whole(state.balanceMilli)), Modifier.testTag("balance"), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.rate, rateText(productionMilliPerSecond(state))))
    }
    PixelStar(stage, minOf(state.collectedCount, 5), state.reduceMotion)
    Text(stringArrayResource(R.array.star_names)[minOf(state.collectedCount, 5)], style = MaterialTheme.typography.titleLarge)
    Text(stringArrayResource(R.array.stages)[stage])
    if (state.collectedCount == 6) {
        Text(stringResource(R.string.ending), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.ending_detail))
    } else if (stage == 5) {
        StarButton(stringResource(R.string.register), onRegister, Modifier.fillMaxWidth().testTag("register"))
        Text(stringResource(R.string.completed_hint))
    } else {
        val goal = STAGE_THRESHOLDS[stage + 1]
        Text(stringResource(R.string.next_goal, whole(state.earnedMilli), whole(goal)))
        LinearProgressIndicator(progress = { (state.earnedMilli.toDouble() / goal).toFloat() }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.upgrade_level, state.upgradeLevel))
        if (state.upgradeLevel < 30) {
            val cost = upgradeCostMilli(state.upgradeLevel)
            StarButton(stringResource(R.string.upgrade, whole(cost)), onUpgrade,
                Modifier.fillMaxWidth().testTag("upgrade"), state.balanceMilli >= cost)
            Text(stringResource(R.string.upgrade_effect, rateText(productionMilliPerSecond(state.copy(upgradeLevel = state.upgradeLevel + 1)))))
            if (state.balanceMilli < cost) Text(stringResource(R.string.shortfall, whole(cost - state.balanceMilli)))
        } else Text(stringResource(R.string.max_upgrade))
    }
    Text(stringResource(R.string.offline_hint))
    if (!state.tutorialSeen) PaperPanel {
        Text(stringResource(R.string.tutorial))
        StarButton(stringResource(R.string.tutorial_ack), onTutorial)
    }
}
