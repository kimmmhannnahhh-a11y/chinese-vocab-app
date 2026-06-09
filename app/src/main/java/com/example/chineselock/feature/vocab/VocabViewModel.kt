package com.example.chineselock.feature.vocab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.StudyUnit
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.network.VocabItem
import com.example.chineselock.core.tts.TtsManager
import com.example.chineselock.ui.StudyMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class VocabViewModel @Inject constructor(
    private val repo: AppRepository,
    private val tts: TtsManager,
) : ViewModel() {

    val units: StateFlow<List<StudyUnit>> =
        repo.observeUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedUnitId = MutableStateFlow<Long?>(null)
    val selectedUnitId: StateFlow<Long?> = _selectedUnitId

    val vocab: StateFlow<List<Vocab>> = _selectedUnitId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeVocab(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mode = MutableStateFlow(StudyMode.ALL)
    val editMode = MutableStateFlow(false)
    val revealed = MutableStateFlow<Set<Long>>(emptySet())

    init {
        viewModelScope.launch {
            units.collect { list ->
                val cur = _selectedUnitId.value
                if (list.isEmpty()) {
                    _selectedUnitId.value = null
                } else if (cur == null || list.none { it.id == cur }) {
                    // 선택한 단원이 사라졌거나 미선택이면 첫 단원으로 자동 복구
                    _selectedUnitId.value = list.first().id
                }
            }
        }
    }

    fun selectUnit(id: Long) { _selectedUnitId.value = id; revealed.value = emptySet() }
    fun setMode(m: StudyMode) { mode.value = m; revealed.value = emptySet() }
    fun toggleEdit() { editMode.value = !editMode.value }
    fun reveal(id: Long) { revealed.value = revealed.value + id }

    fun speak(text: String) = tts.speak(text)

    fun toggleFavorite(v: Vocab) = viewModelScope.launch { repo.setFavorite(v.id, !v.isFavorite) }
    fun delete(v: Vocab) = viewModelScope.launch { repo.deleteVocab(v.id) }

    /** 현재 단원의 '단어만' 전체 삭제(회화는 보존). 단원·선택은 유지. */
    fun deleteCurrentUnitVocab() {
        val id = _selectedUnitId.value ?: return
        viewModelScope.launch {
            repo.deleteVocabForUnit(id)
            editMode.value = false
        }
    }

    fun addVocab(hanzi: String, pinyin: String, pos: List<String>, meaning: String) {
        val unitId = _selectedUnitId.value ?: return
        viewModelScope.launch {
            repo.addVocab(
                unitId,
                VocabItem(hanzi = hanzi, pinyin = pinyin, pos = pos, meaning = meaning, category = "직접 추가"),
                order = vocab.value.size,
            )
        }
    }
}
