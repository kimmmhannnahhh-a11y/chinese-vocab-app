package com.example.chineselock.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.network.OcrStructurer
import com.example.chineselock.core.network.VocabItem
import com.example.chineselock.core.ocr.OcrTextRecognizer
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 교재 촬영 → ML Kit OCR → Gemini 구조화 → 검토/수정 → 단어장 저장.
 * 흐름: CAMERA → PROCESSING → REVIEW(편집) → 저장 / ERROR.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val recognizer: OcrTextRecognizer,
    private val structurer: OcrStructurer,
    private val repo: AppRepository,
) : ViewModel() {

    enum class Phase { CAMERA, PROCESSING, REVIEW, ERROR }

    data class UiState(
        val phase: Phase = Phase.CAMERA,
        val title: String = "3-1",
        val items: List<VocabItem> = emptyList(),
        val error: String? = null,
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun setTitle(s: String) = _ui.update { it.copy(title = s) }

    /** 촬영 결과 처리. 인식 → 구조화. 실패 시 ERROR 단계로. */
    fun onImageCaptured(image: InputImage) {
        _ui.update { it.copy(phase = Phase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val raw = recognizer.recognize(image)
                if (raw.isBlank()) {
                    fail("글자를 인식하지 못했어요. 교재에 더 가까이, 밝은 곳에서 다시 찍어보세요.")
                    return@launch
                }
                val extraction = structurer.structureVocab(raw)
                if (extraction.items.isEmpty()) {
                    fail("단어를 정리하지 못했어요. 단어 페이지인지 확인하고 다시 시도해주세요.")
                    return@launch
                }
                _ui.update { it.copy(phase = Phase.REVIEW, items = extraction.items) }
            } catch (e: Exception) {
                fail(e.message ?: "처리 중 오류가 발생했어요.")
            }
        }
    }

    private fun fail(msg: String) = _ui.update { it.copy(phase = Phase.ERROR, error = msg) }

    fun removeItem(index: Int) = _ui.update { st ->
        st.copy(items = st.items.filterIndexed { i, _ -> i != index })
    }

    /** 다시 찍기: 제목은 유지하고 카메라로 복귀. */
    fun retake() = _ui.update { UiState(title = it.title) }

    /** 등록: 제목(3-1)을 권/과로 파싱 → 단원 생성/조회 → 일괄 저장. onDone(개수). */
    fun save(now: Long, onDone: (Int) -> Unit) {
        val st = _ui.value
        if (st.items.isEmpty()) { onDone(0); return }
        val parts = st.title.split("-", " ", ".").mapNotNull { it.trim().toIntOrNull() }
        val book = parts.getOrElse(0) { 0 }
        val lesson = parts.getOrElse(1) { 0 }
        viewModelScope.launch {
            val unitId = repo.getOrCreateUnit(book, lesson, now)
            repo.addVocabBatch(unitId, st.items, startOrder = 0)
            onDone(st.items.size)
        }
    }
}
