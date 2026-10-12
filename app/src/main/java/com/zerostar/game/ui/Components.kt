package com.zerostar.game.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.zerostar.game.ui.theme.*

@Composable fun PaperPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(end = 5.dp, bottom = 5.dp).drawBehind {
        drawRect(Shadow, topLeft = Offset(5.dp.toPx(), 5.dp.toPx()))
    }.background(Panel).border(3.dp, Ink).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}
@Composable fun StarButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 48.dp), enabled = enabled, shape = RectangleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Coral, contentColor = Ink, disabledContainerColor = Shadow, disabledContentColor = Ink),
        border = BorderStroke(2.dp, Ink)) { Text(text) }
}
