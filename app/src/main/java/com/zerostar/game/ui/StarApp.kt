package com.zerostar.game.ui

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerostar.game.ui.theme.PixelFont
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zerostar.game.R
import com.zerostar.game.data.GameAction
import com.zerostar.game.ui.theme.StarTheme

@Composable fun StarApp(model: GameViewModel, privacyRequired: Boolean = false, onPrivacy: () -> Unit = {}, banner: @Composable () -> Unit = {}) {
    val ui by model.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pool = remember { SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build() }
    val ids = remember(pool) { listOf(R.raw.upgrade, R.raw.growth, R.raw.collection).map { pool.load(context, it, 1) } }
    DisposableEffect(pool) { onDispose { pool.release() } }
    LaunchedEffect(model, pool) { model.sounds.collect { pool.play(ids[it.ordinal], 0.35f, 0.35f, 0, 0, 1f) } }
    StarScreen(ui, model::dispatch, model::resume, model::dismissReward, privacyRequired, onPrivacy, banner)
}

@Composable fun StarScreen(ui: GameUiState, onAction: (GameAction) -> Unit, onRetry: () -> Unit, onDismissReward: () -> Unit,
    privacyRequired: Boolean = false, onPrivacy: () -> Unit = {}, banner: @Composable () -> Unit = {}) {
    StarTheme {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var dialog by rememberSaveable { mutableStateOf<String?>(null) }
        val state = ui.game
        val scroll = rememberScrollState()
        LaunchedEffect(tab) { scroll.scrollTo(0) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(stringResource(R.string.logo), fontFamily = PixelFont, fontSize = 12.sp); Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium) }
                Column(Modifier.weight(1f).widthIn(max = 600.dp).fillMaxWidth().verticalScroll(scroll).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (ui.error) {
                        Text(stringResource(R.string.save_error))
                        StarButton(stringResource(R.string.retry), onRetry)
                        StarButton(stringResource(R.string.reset), { dialog = "reset" })
                    } else if (state == null) Text(stringResource(R.string.loading))
                    else when(tab) {
                        0 -> HomeScreen(state, { onAction(GameAction.BuyUpgrade) }, { dialog = "register" }, { onAction(GameAction.MarkTutorialSeen) })
                        1 -> CollectionScreen(state)
                        2 -> SettingsScreen(state, onAction, { dialog = "reset" }, privacyRequired, onPrivacy)
                    }
                }
                if (tab == 0 && dialog == null && ui.reward == null && !ui.error) Box(Modifier.widthIn(max=600.dp).fillMaxWidth().padding(vertical=8.dp), contentAlignment = Alignment.Center) { banner() }
                Row(Modifier.widthIn(max=600.dp).fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(R.string.home,R.string.collection,R.string.settings).forEachIndexed { index, label ->
                        StarButton(stringResource(label), { tab = index }, Modifier.weight(1f))
                    }
                }
            }
        }
        if (dialog != null) {
            val reset = dialog == "reset"
            val last = state?.collectedCount == 5
            AlertDialog(containerColor = MaterialTheme.colorScheme.surface, shape = androidx.compose.ui.graphics.RectangleShape, onDismissRequest = { dialog = null }, title = { Text(stringResource(if (reset) R.string.reset_title else R.string.confirm_register)) },
                text = { Text(stringResource(if (reset) R.string.reset_detail else if(last) R.string.register_final_detail else R.string.register_detail)) },
                confirmButton = { TextButton(onClick = { onAction(if(reset) GameAction.Reset else GameAction.RegisterStar); dialog = null }) {
                    Text(stringResource(if(reset) R.string.reset_confirm else if(last) R.string.finish_collection else R.string.next_star))
                } }, dismissButton = { TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.cancel)) } })
        } else ui.reward?.let { reward ->
            AlertDialog(containerColor = MaterialTheme.colorScheme.surface, shape = androidx.compose.ui.graphics.RectangleShape, onDismissRequest = onDismissReward, title = { Text(stringResource(R.string.return_title)) },
                text = { Text(stringResource(R.string.return_detail, reward.seconds/60, whole(reward.rewardMilli))) },
                confirmButton = { TextButton(onClick = onDismissReward) { Text(stringResource(R.string.ok)) } })
        }
    }
}
