package com.zerostar.game.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.zerostar.game.R
import kotlinx.coroutines.delay
import kotlin.math.floor

private val palettes = listOf(0xFFFFD780,0xFFFF9D91,0xFFD8AEFF,0xFF83D9E0,0xFFFFB26F,0xFFCBC6FF)
@Composable fun PixelStar(stage: Int, kind: Int, reduceMotion: Boolean, modifier: Modifier = Modifier) {
    var flash by remember { mutableStateOf(false) }
    LaunchedEffect(flash) { if (flash) { delay(250); flash = false } }
    val drift = if (reduceMotion) 0f else {
        val transition = rememberInfiniteTransition(label = "star float")
        val offset by transition.animateFloat(-1f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Reverse), label = "drift")
        offset
    }
    val touch = stringResource(R.string.star_touch)
    Canvas(modifier.fillMaxWidth().height(200.dp).background(Brush.verticalGradient(listOf(Color(0xFF241B4D),Color(0xFF4B2A7B),Color(0xFFC86BB1),Color(0xFFFF9D6C))))
        .semantics { contentDescription = touch; role = Role.Button }
        .clickable(onClickLabel = touch) { if (!reduceMotion) flash = true }) {
        val unit = floor(minOf(size.width / 48, size.height / 32)).coerceAtLeast(1f)
        val cx = floor(size.width / 2 / unit) * unit
        val cy = floor(size.height / 2 / unit) * unit + floor(drift) * unit
        fun pixel(x: Int, y: Int, w: Int = 1, h: Int = 1, color: Color = Color(0xFFFFF4D6)) {
            drawRect(color, Offset(cx + x * unit, cy + y * unit), Size(w * unit, h * unit))
        }
        for (i in 0..14) pixel((i * 17 % 43) - 21, (i * 11 % 27) - 13, color = Color(0xFFDDC9E8))
        val color = if (flash) Color.White else Color(palettes[kind.coerceIn(0,5)])
        if (stage == 0) {
            pixel(-2,-2,4,4,color); pixel(-1,0,color=Color(0xFF2B1B3D)); pixel(1,0,color=Color(0xFF2B1B3D))
        } else {
            val rows = listOf("....##....","....##....","...####...","##########",".########.","..######..","..######..",".###..###.",".##....##.")
            val scale = if (stage >= 2) 2 else 1
            if (stage >= 4) {
                pixel(-12,-1,24,2,Color(0xFFCC9ABF)); pixel(-1,-12,2,24,Color(0xFFCC9ABF))
            }
            rows.forEachIndexed { y, row -> row.forEachIndexed { x, c ->
                if (c == '#') pixel((x-5)*scale,(y-4)*scale,scale,scale,color)
            } }
            pixel(-2*scale,0,1,2,Color(0xFF2B1B3D)); pixel(2*scale,0,1,2,Color(0xFF2B1B3D))
            if (stage >= 3) { pixel(-13,-9); pixel(12,8); pixel(10,-10) }
            if (stage == 5) when (kind) {
                0 -> { pixel(-4,-11,8); pixel(-4,-13,1,2); pixel(0,-14,1,3); pixel(3,-13,1,2) }
                1 -> { pixel(-6,3,3,2,Color(0xFFE85D75)); pixel(4,3,3,2,Color(0xFFE85D75)) }
                2 -> { pixel(-13,0,3,3); pixel(-14,1,5); pixel(12,-2,3,3) }
                3 -> { pixel(-13,10,7); pixel(-6,11,7); pixel(1,10,7); pixel(8,11,6) }
                4 -> { for (i in -13..13) if (i % 2 == 0) pixel(i, i/2,2,1,Color(0xFFFFF0CE)) }
                5 -> { pixel(-13,-7,3,3); pixel(11,-10,2,2); pixel(12,8,3,3) }
            }
        }
    }
}
