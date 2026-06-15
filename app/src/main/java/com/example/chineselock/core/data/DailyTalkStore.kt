package com.example.chineselock.core.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * '오늘의 회화' AI 예문을 하루 1회만 생성하도록 캐시(SharedPreferences).
 * 같은 날 같은 단어면 저장값 재사용 → Gemini 호출/비용 0. 날짜나 단어가 바뀌면 새로 생성.
 */
@Singleton
class DailyTalkStore @Inject constructor(@ApplicationContext context: Context) {
    private val sp = context.getSharedPreferences("daily_talk", Context.MODE_PRIVATE)

    /** key가 저장된 key와 같으면 캐시 JSON 반환, 아니면 null. */
    fun get(key: String): String? = if (sp.getString(KEY_K, null) == key) sp.getString(KEY_V, null) else null

    fun put(key: String, value: String) {
        sp.edit().putString(KEY_K, key).putString(KEY_V, value).apply()
    }

    private companion object {
        const val KEY_K = "k"
        const val KEY_V = "v"
    }
}
