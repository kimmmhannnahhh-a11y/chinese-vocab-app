package com.example.chineselock.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.PartOfSpeechCount
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.tts.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WordSearchViewModel @Inject constructor(
    private val repo: AppRepository,
    private val tts: TtsManager,
) : ViewModel() {

    val query = MutableStateFlow("")
    val selectedPos = MutableStateFlow<String?>(null)

    val posCounts: StateFlow<List<PartOfSpeechCount>> =
        repo.observePosCounts().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val results: StateFlow<List<Vocab>> =
        combine(query, selectedPos) { q, pos -> q to pos }
            .flatMapLatest { (q, pos) ->
                when {
                    q.isNotBlank() -> repo.search(q)
                    pos != null -> repo.observeByPartOfSpeech(pos)
                    else -> flowOf(emptyList())
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(s: String) { query.value = s }
    fun selectPos(p: String) { selectedPos.value = if (selectedPos.value == p) null else p }
    fun speak(text: String) = tts.speak(text)
    fun toggleFavorite(v: Vocab) = viewModelScope.launch { repo.setFavorite(v.id, !v.isFavorite) }
}
