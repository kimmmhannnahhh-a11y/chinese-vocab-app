package com.example.chineselock.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// NOTE: 시안은 메뉴/버튼=Cafe24 써라운드, 단어·회화 본문=맑은 고딕을 사용했다.
// 안드로이드에선 해당 폰트 파일(ttf)을 res/font에 번들해야 동일하게 보인다.
// 지금은 시스템 기본(Default)으로 두고, 추후 폰트 추가 시 아래 FontFamily만 교체.
val UiFont = FontFamily.Default       // 메뉴/버튼/헤더 (Cafe24 자리)
val ContentFont = FontFamily.Default  // 단어·회화 본문 (맑은 고딕 자리)

val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = UiFont, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = UiFont, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = UiFont, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = ContentFont, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = ContentFont, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = UiFont, fontSize = 12.sp),
)
