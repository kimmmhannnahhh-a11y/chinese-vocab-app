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

    /** 등록: 제목(3-1)을 권/과로 파싱 → 단원 생성/조회 → 일괄 저장. onDone(개수). */
    fun register(now: Long, onDone: (Int) -> Unit) {
        val items = preview.value
        if (items.isEmpty()) { onDone(0); return }
        val parts = title.value.split("-", " ", ".").mapNotNull { it.trim().toIntOrNull() }
        val book = parts.getOrElse(0) { 0 }
        val lesson = parts.getOrElse(1) { 0 }
        viewModelScope.launch {
            val unitId = repo.getOrCreateUnit(book, lesson, now)
            repo.addVocabBatch(unitId, items, startOrder = 0)
            onDone(items.size)
        }
    }
}
