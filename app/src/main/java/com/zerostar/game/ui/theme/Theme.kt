package com.zerostar.game.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.zerostar.game.R

val Paper = Color(0xFFF6EAD8)
val Panel = Color(0xFFFFF8EC)
val Ink = Color(0xFF2B1B3D)
val Coral = Color(0xFFE85D75)
val Shadow = Color(0xFFD8C7B0)
val PixelFont = FontFamily(Font(R.font.press_start))
private val body = TextStyle(fontFamily = FontFamily(Font(R.font.ibm_plex_sans_kr)), fontSize = 16.sp, lineHeight = 24.sp)
@Composable fun StarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = Ink, onPrimary = Panel, background = Paper, onBackground = Ink, surface = Panel, onSurface = Ink, secondary = Coral),
        typography = Typography(bodyLarge = body, bodyMedium = body, bodySmall = body, labelLarge = body, titleLarge = body.copy(fontSize = 24.sp, lineHeight = 32.sp), titleMedium = body.copy(fontSize = 20.sp, lineHeight = 28.sp)),
        content = content,
    )
}
