package com.example.chineselock

import android.app.Application
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // AdMob 초기화(배너 광고용). 백그라운드에서 1회.
        MobileAds.initialize(this) {}
    }
}
