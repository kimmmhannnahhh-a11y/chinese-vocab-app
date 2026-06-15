package com.example.chineselock.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * "광고 보고 오늘 무료로 인식하기" 잠금 상태(하루 단위).
 * 그날 광고를 1회 보면 그날 OCR(사진 인식)이 무제한으로 열린다. 날짜가 바뀌면 다시 잠김.
 */
@Singleton
class OcrGateStore @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("ocr_gate", Context.MODE_PRIVATE)

    fun isUnlocked(epochDay: Long): Boolean = sp.getLong(KEY_DAY, -1L) == epochDay

    fun unlock(epochDay: Long) { sp.edit().putLong(KEY_DAY, epochDay).apply() }

    private companion object { const val KEY_DAY = "day" }
}
