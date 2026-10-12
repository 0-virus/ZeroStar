package com.zerostar.game.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zerostar.game.BuildConfig
import com.zerostar.game.R
import com.zerostar.game.data.GameAction
import com.zerostar.game.game.GameState

@Composable fun SettingsScreen(state: GameState, onAction: (GameAction) -> Unit, onReset: () -> Unit, privacyRequired: Boolean, onPrivacy: () -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<String?>(null) }
    fun openLink(link: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
        catch (_: ActivityNotFoundException) { Toast.makeText(context, R.string.link_error, Toast.LENGTH_SHORT).show() }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.sound), Modifier.weight(1f))
        Switch(state.soundEnabled, { onAction(GameAction.SetSound(it)) })
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.reduce_motion), Modifier.weight(1f))
        Switch(state.reduceMotion, { onAction(GameAction.SetReduceMotion(it)) })
    }
    Text(stringResource(R.string.local_storage))
    StarButton(stringResource(R.string.privacy), {
        if (BuildConfig.PRIVACY_URL.isNotBlank()) openLink(BuildConfig.PRIVACY_URL)
        else detail = context.getString(R.string.policy_summary) + "\n\n" + context.getString(R.string.not_configured)
    }, Modifier.fillMaxWidth())
    if (privacyRequired) StarButton(stringResource(R.string.ad_privacy), onPrivacy, Modifier.fillMaxWidth())
    StarButton(stringResource(R.string.licenses), {
        detail = context.getString(R.string.license_text) + "\n\n" + listOf("IBM-Plex-Sans-KR.txt", "Press-Start-2P.txt").joinToString("\n\n") { name -> context.assets.open("licenses/$name").bufferedReader().use { it.readText() } }
    }, Modifier.fillMaxWidth())
    StarButton(stringResource(R.string.contact), {
        if (BuildConfig.SUPPORT_EMAIL.isNotBlank()) openLink("mailto:" + BuildConfig.SUPPORT_EMAIL)
        else detail = context.getString(R.string.not_configured)
    }, Modifier.fillMaxWidth())
    Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
    Spacer(Modifier.height(12.dp))
    StarButton(stringResource(R.string.reset), onReset, Modifier.fillMaxWidth())
    detail?.let { text ->
        AlertDialog(onDismissRequest = { detail = null }, text = {
            androidx.compose.foundation.lazy.LazyColumn { item { Text(text) } }
        }, confirmButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.close)) } })
    }
}
