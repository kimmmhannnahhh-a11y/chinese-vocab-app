package com.example.chineselock.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.tts.TtsManager
import com.example.chineselock.ui.StudyMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repo: AppRepository,
    private val tts: TtsManager,
) : ViewModel() {

    val favorites: StateFlow<List<Vocab>> =
        repo.observeFavorites().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mode = MutableStateFlow(StudyMode.ALL)
    val revealed = MutableStateFlow<Set<Long>>(emptySet())

    fun setMode(m: StudyMode) { mode.value = m; revealed.value = emptySet() }
    fun reveal(id: Long) { revealed.value = revealed.value + id }
    fun speak(text: String) = tts.speak(text)
    fun toggleFavorite(v: Vocab) = viewModelScope.launch { repo.setFavorite(v.id, !v.isFavorite) }
}
