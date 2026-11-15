package org.pilou.bettersony.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val background = Color(0xFFF2F2F7)
    val surface = Color.White
    val ink = Color(0xFF111111)
    val secondary = Color(0xFF8A8A8F)
    val tertiary = Color(0xFFA0A0A6)
    val body = Color(0xFF555555)
    val divider = Color(0xFFEFEFF4)
    val track = Color(0xFFE5E5EA)
    val segmentTrack = Color(0xFFEEEEF0)
    val skeleton = Color(0xFFE6E6EB)
    val green = Color(0xFF34C759)
    val orange = Color(0xFFFF9500)
    val blue = Color(0xFF007AFF)
    val chevron = Color(0xFFC4C4C8)
    val orangeTint = Color(0xFFFFF1E0)
    val greenTint = Color(0xFFE3F7E8)
    val blueTint = Color(0xFFE1EEFF)
    val red = Color(0xFFFF3B30)
    val batteryOutline = Color(0xFFB9B9BF)
}

object Type {
    val largeTitle = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.9).sp, color = Palette.ink)
    val title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, color = Palette.ink)
    val body = TextStyle(fontSize = 15.sp, color = Palette.ink)
    val bodyStrong = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Palette.ink)
    val callout = TextStyle(fontSize = 14.sp, color = Palette.secondary)
    val label = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Palette.secondary)
    val caption = TextStyle(fontSize = 12.sp, color = Palette.tertiary)
    val button = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
}
