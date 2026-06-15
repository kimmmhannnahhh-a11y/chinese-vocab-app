package com.example.chineselock.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.data.DailyTalkStore
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.network.ExampleSentence
import com.example.chineselock.core.network.OcrStructurer
import com.example.chineselock.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AppRepository,
    private val tts: TtsManager,
    private val structurer: OcrStructurer,
    private val talkStore: DailyTalkStore,
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _todayWord = MutableStateFlow<Vocab?>(null)
    val todayWord: StateFlow<Vocab?> = _todayWord

    private val _todayTalk = MutableStateFlow<ExampleSentence?>(null)
    val todayTalk: StateFlow<ExampleSentence?> = _todayTalk

    /** 오늘의 단어는 있는데 예문을 만드는 중일 때 true(로딩 표시용). */
    private val _talkLoading = MutableStateFlow(false)
    val talkLoading: StateFlow<Boolean> = _talkLoading

    fun speak(text: String) = tts.speak(text)

    init {
        viewModelScope.launch {
            val epochDay = LocalDate.now().toEpochDay()
            val word = repo.todayWord(epochDay)
            _todayWord.value = word
            if (word == null) return@launch

            val key = "$epochDay:${word.id}"
            // 1) 오늘 이미 만든 예문이 있으면 그대로 사용(비용 0)
            talkStore.get(key)?.let { cached ->
                _todayTalk.value = runCatching { json.decodeFromString<ExampleSentence>(cached) }.getOrNull()
                if (_todayTalk.value != null) return@launch
            }
            // 2) 없으면 AI로 생성 → 캐시
            _talkLoading.value = true
            val generated = runCatching { structurer.exampleSentence(word.hanzi, word.meaning) }
                .getOrNull()
                ?.takeIf { it.chinese.isNotBlank() }
            if (generated != null) {
                _todayTalk.value = generated
                talkStore.put(key, json.encodeToString(generated))
            } else {
                // 3) 생성 실패 시: 내가 저장한 회화 중 그 단어가 든 문장으로 폴백
                repo.todayDialogue(word, epochDay)?.let {
                    _todayTalk.value = ExampleSentence(it.chinese, it.pinyin ?: "", it.korean ?: "")
                }
            }
            _talkLoading.value = false
        }
    }
}
