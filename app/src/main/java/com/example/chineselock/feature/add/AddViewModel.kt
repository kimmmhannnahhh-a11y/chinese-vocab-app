package com.example.chineselock.feature.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.network.VocabItem
import com.example.chineselock.feature.capture.ManualVocabParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repo: AppRepository,
) : ViewModel() {

    val title = MutableStateFlow("")   // "권-과" 형식. 비워두면 입력 힌트(예: 3-1)만 표시
    val pasteText = MutableStateFlow("")

    /** 붙여넣기 텍스트(뜻\t병음\t한자) 미리보기. */
    val preview: StateFlow<List<VocabItem>> =
        pasteText.map { ManualVocabParser.parse(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTitle(s: String) { title.value = s }
    fun setPaste(s: String) { pasteText.value = s }

    /** 등록: 제목(입력 그대로) → 단원 생성/조회 → 일괄 저장. 제목 필수·중복 차단. onDone(개수, 0이면 미저장). */
    fun register(now: Long, onDone: (Int) -> Unit) {
        val items = preview.value
        val t = title.value.trim()
        if (items.isEmpty() || t.isBlank()) { onDone(0); return }
        viewModelScope.launch {
            if (repo.titleHasContent(t, isVocab = true)) { onDone(0); return@launch } // 같은 제목 단어 중복
            val unitId = repo.getOrCreateUnit(t, now)
            repo.addVocabBatch(unitId, items, startOrder = 0)
            onDone(items.size)
        }
    }
}
