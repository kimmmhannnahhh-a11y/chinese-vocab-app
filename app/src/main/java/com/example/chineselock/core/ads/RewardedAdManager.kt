package com.example.chineselock.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 리워드(보상형) 광고 관리. "광고 보고 오늘 무료로 인식하기"에 사용.
 * 현재는 구글 공식 '테스트' 리워드 광고 ID. 출시 전 실제 광고 단위 ID로 교체.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val adUnitId = "ca-app-pub-3940256099942544/5224354917" // 테스트용
    private var ad: RewardedAd? = null
    private var loading = false

    /** 미리 한 개 로드해 둔다(버튼 누르면 바로 뜨도록). */
    fun preload() {
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            context, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) { ad = loaded; loading = false }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null; loading = false }
            },
        )
    }

    /**
     * 광고 표시. 보상 획득 시 onReward, 광고가 없거나 실패하면 onUnavailable.
     * (호출부는 onUnavailable에서 '광고 인프라 문제로 막지 않도록' 그냥 통과시켜도 됨)
     */
    fun show(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val current = ad
        if (current == null) {
            preload()
            onUnavailable()
            return
        }
        ad = null // 1회용
        var earned = false
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                preload()
                if (earned) onReward() else onUnavailable()
            }
            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                preload()
                onUnavailable()
            }
        }
        current.show(activity) { earned = true }
    }
}
