package com.example.chineselock.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// 현재는 구글 공식 '테스트' 배너 광고 단위 ID. 출시 전 실제 AdMob 광고 단위 ID로 교체할 것.
private const val TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/** 하단 배너 광고. */
@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = TEST_BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
