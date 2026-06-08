package com.example.chineselock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 시안(흰색 + 보라) 팔레트
object AppColors {
    val Purple = Color(0xFF5B3F8C)
    val PurpleSoft = Color(0xFFECE4F7)
    val Ink = Color(0xFF3D3450)
    val Sub = Color(0xFF8E86A0)
    val Muted = Color(0xFFBCB4CE)
    val Faint = Color(0xFFD6CFE4)
    val Line = Color(0xFFF0ECF8)
    val Bg = Color(0xFFFFFFFF)
    val Gold = Color(0xFFE8B53D) // 즐겨찾기 ★
    val SpeakerA = Color(0xFF5B3F8C)
    val SpeakerASoft = Color(0xFFECE4F7)
    val SpeakerB = Color(0xFF2E8B6B)
    val SpeakerBSoft = Color(0xFFDCF1EA)

    // 품사별 색 (배경, 글자)
    fun posBg(pos: String) = when (pos) {
        "명사" -> Color(0xFFDEE9FB)
        "동사" -> Color(0xFFFBE2D6)
        "형용사" -> Color(0xFFFBE0EC)
        "부사" -> Color(0xFFDCF1EA)
        "양사" -> Color(0xFFECE4F7)
        "대명사" -> Color(0xFFF7EBC9)
        else -> PurpleSoft
    }

    fun posFg(pos: String) = when (pos) {
        "명사" -> Color(0xFF3667A8)
        "동사" -> Color(0xFFBF5A36)
        "형용사" -> Color(0xFFC2457E)
        "부사" -> Color(0xFF2E8B6B)
        "양사" -> Color(0xFF5B3F8C)
        "대명사" -> Color(0xFF9A7B1F)
        else -> Purple
    }

    /** 품사 풀네임 → 교재식 한 글자 약자 (표시용). */
    fun posAbbr(pos: String) = when (pos) {
        "명사" -> "명"; "동사" -> "동"; "형용사" -> "형"; "부사" -> "부"
        "양사" -> "양"; "대명사" -> "대"; "접속사" -> "접"; "조사" -> "조"
        "조동사" -> "조동"; "수사" -> "수"; else -> pos.take(1)
    }
}

private val LightColors = lightColorScheme(
    primary = AppColors.Purple,
    onPrimary = Color.White,
    primaryContainer = AppColors.PurpleSoft,
    onPrimaryContainer = AppColors.Purple,
    background = AppColors.Bg,
    onBackground = AppColors.Ink,
    surface = AppColors.Bg,
    onSurface = AppColors.Ink,
    surfaceVariant = AppColors.PurpleSoft,
    outline = AppColors.Muted,
)

@Composable
fun ChineseLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
