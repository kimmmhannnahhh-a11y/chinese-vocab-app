package com.example.chineselock.feature.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.db.StudyUnit
import com.example.chineselock.core.tts.TtsManager
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
class ConversationViewModel @Inject constructor(
    private val repo: AppRepository,
    private val tts: TtsManager,
) : ViewModel() {

    val units: StateFlow<List<StudyUnit>> =
        repo.observeUnits().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedUnitId = MutableStateFlow<Long?>(null)
    val selectedUnitId: StateFlow<Long?> = _selectedUnitId

    val turns: StateFlow<List<Dialogue>> = _selectedUnitId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.observeDialogue(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val showTranslation = MutableStateFlow(false)
    val editMode = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            units.collect { list ->
                if (_selectedUnitId.value == null && list.isNotEmpty()) _selectedUnitId.value = list.first().id
            }
        }
    }

    fun selectUnit(id: Long) { _selectedUnitId.value = id }
    fun setTranslation(v: Boolean) { showTranslation.value = v }
    fun toggleEdit() { editMode.value = !editMode.value }

    fun speak(text: String) = tts.speak(text)
    fun playAll() = tts.speakSequence(turns.value.map { it.chinese })

    fun delete(d: Dialogue) = viewModelScope.launch { repo.deleteDialogue(d.id) }

    fun addLine(speaker: String?, chinese: String, pinyin: String?, korean: String?) {
        val unitId = _selectedUnitId.value ?: return
        val first = turns.value.firstOrNull()
        viewModelScope.launch {
            repo.addDialogue(
                Dialogue(
                    unitId = unitId,
                    sectionTitle = first?.sectionTitle,
                    audioTrack = first?.audioTrack,
                    speaker = speaker,
                    chinese = chinese,
                    pinyin = pinyin,
                    korean = korean,
                    orderInUnit = turns.value.size,
                )
            )
        }
    }
}
