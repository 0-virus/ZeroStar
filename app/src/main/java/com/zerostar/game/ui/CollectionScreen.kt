package com.zerostar.game.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.dp
import com.zerostar.game.R
import com.zerostar.game.game.GameState

@Composable fun CollectionScreen(state: GameState) {
    val names = stringArrayResource(R.array.star_names)
    val descriptions = stringArrayResource(R.array.star_descriptions)
    var detail by remember { mutableStateOf<Int?>(null) }
    Text(stringResource(R.string.collection_count, state.collectedCount), style = MaterialTheme.typography.titleLarge)
    Text(stringResource(R.string.collection_bonus, state.collectedCount * 25))
    for (row in 0..2) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in row*2..row*2+1) PaperPanel(Modifier.weight(1f)) {
            Text(names[i], style = MaterialTheme.typography.titleMedium)
            if (i < state.collectedCount) {
                StarButton(stringResource(R.string.earned), { detail = i })
            } else Text(stringResource(R.string.locked))
        }
    }
    detail?.let { i ->
        AlertDialog(onDismissRequest = { detail = null }, title = { Text(names[i]) }, text = {
            Column { PixelStar(5, i, true); Text(descriptions[i]) }
        }, confirmButton = { TextButton(onClick = { detail = null }) { Text(stringResource(R.string.close)) } })
    }
}
