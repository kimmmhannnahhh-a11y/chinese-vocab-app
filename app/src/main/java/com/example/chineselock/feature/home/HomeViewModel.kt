package com.example.chineselock.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.Vocab
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AppRepository,
) : ViewModel() {

    private val _todayWord = MutableStateFlow<Vocab?>(null)
    val todayWord: StateFlow<Vocab?> = _todayWord

    init {
        viewModelScope.launch {
            _todayWord.value = repo.todayWord(LocalDate.now().toEpochDay())
        }
    }
}
