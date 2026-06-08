package com.example.chineselock.core.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** 중국어(zh-CN) 성조 재생용 내장 TTS 래퍼. 앱 전역 싱글톤. */
@Singleton
class TtsManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    @Volatile var isChineseAvailable: Boolean = false
        private set

    private val tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts.setLanguage(Locale.CHINA)
                isChineseAvailable = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                tts.setSpeechRate(0.9f) // 학습용으로 살짝 느리게
            }
        }
    }

    /** 한 줄 듣기: 진행 중이던 발화를 끊고 이 문장을 재생. */
    fun speak(text: String) {
        if (text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    /** 전체 듣기: 회화 문장들을 순서대로 큐에 쌓아 연속 재생. */
    fun speakSequence(lines: List<String>) {
        lines.filter { it.isNotBlank() }.forEachIndexed { i, line ->
            val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(line, mode, null, "seq_$i")
        }
    }

    fun stop() = tts.stop()

    fun shutdown() = tts.shutdown()
}
