package com.example.chineselock.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.network.DialogueItem
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
import retrofit2.HttpException
import javax.inject.Inject

/** 촬영 대상: 단어 페이지 / 회화 페이지. */
enum class CaptureMode { VOCAB, DIALOGUE }

/**
 * 교재 촬영 → ML Kit OCR → Gemini 구조화 → 검토/수정 → 저장.
 * VOCAB: 단어장에 저장 / DIALOGUE: 회화에 저장.
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
        val mode: CaptureMode = CaptureMode.VOCAB,
        val title: String = "3-1",
        val items: List<VocabItem> = emptyList(),       // VOCAB
        val lines: List<DialogueItem> = emptyList(),    // DIALOGUE
        val sectionTitle: String? = null,
        val audioTrack: String? = null,
        val error: String? = null,
    ) {
        val count: Int get() = if (mode == CaptureMode.VOCAB) items.size else lines.size
    }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun setMode(mode: CaptureMode) = _ui.update { if (it.mode == mode) it else it.copy(mode = mode) }
    fun setTitle(s: String) = _ui.update { it.copy(title = s) }

    /** 촬영 결과 처리. 인식 → 모드별 구조화. 실패 시 ERROR 단계로. */
    fun onImageCaptured(image: InputImage) {
        _ui.update { it.copy(phase = Phase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val raw = recognizer.recognize(image)
                if (raw.isBlank()) {
                    fail("글자를 인식하지 못했어요. 교재에 더 가까이, 밝은 곳에서 다시 찍어보세요.")
                    return@launch
                }
                if (_ui.value.mode == CaptureMode.VOCAB) {
                    val ex = structurer.structureVocab(raw)
                    if (ex.items.isEmpty()) { fail("단어를 정리하지 못했어요. 단어 페이지인지 확인하고 다시 시도해주세요."); return@launch }
                    _ui.update { it.copy(phase = Phase.REVIEW, items = ex.items) }
                } else {
                    val ex = structurer.structureDialogue(raw)
                    if (ex.items.isEmpty()) { fail("회화 문장을 정리하지 못했어요. 회화 페이지인지 확인하고 다시 시도해주세요."); return@launch }
                    _ui.update {
                        it.copy(
                            phase = Phase.REVIEW,
                            lines = ex.items,
                            sectionTitle = ex.sectionTitle,
                            audioTrack = ex.audioTrack,
                        )
                    }
                }
            } catch (e: HttpException) {
                fail(
                    when (e.code()) {
                        429 -> "요청이 잠시 몰렸어요(무료 한도). 1~2분 뒤 다시 시도해주세요."
                        in 500..599 -> "Gemini 서버가 잠시 혼잡해요. 잠시 후 다시 시도해주세요."
                        else -> "서버 오류(${e.code()}). 잠시 후 다시 시도해주세요."
                    }
                )
            } catch (e: Exception) {
                fail(e.message ?: "처리 중 오류가 발생했어요.")
            }
        }
    }

    private fun fail(msg: String) = _ui.update { it.copy(phase = Phase.ERROR, error = msg) }

    fun removeItem(index: Int) = _ui.update { st ->
        if (st.mode == CaptureMode.VOCAB) st.copy(items = st.items.filterIndexed { i, _ -> i != index })
        else st.copy(lines = st.lines.filterIndexed { i, _ -> i != index })
    }

    /** 다시 찍기: 제목·모드는 유지하고 카메라로 복귀. */
    fun retake() = _ui.update { UiState(title = it.title, mode = it.mode) }

    /** 등록: 제목(3-1)을 권/과로 파싱 → 단원 생성/조회 → 일괄 저장. onDone(개수). */
    fun save(now: Long, onDone: (Int) -> Unit) {
        val st = _ui.value
        if (st.count == 0) { onDone(0); return }
        val parts = st.title.split("-", " ", ".").mapNotNull { it.trim().toIntOrNull() }
        val book = parts.getOrElse(0) { 0 }
        val lesson = parts.getOrElse(1) { 0 }
        viewModelScope.launch {
            val unitId = repo.getOrCreateUnit(book, lesson, now)
            if (st.mode == CaptureMode.VOCAB) {
                repo.addVocabBatch(unitId, st.items, startOrder = 0)
            } else {
                val rows = st.lines.mapIndexed { i, d ->
                    Dialogue(
                        unitId = unitId,
                        sectionTitle = st.sectionTitle,
                        audioTrack = st.audioTrack,
                        speaker = d.speaker,
                        chinese = d.chinese,
                        pinyin = d.pinyin,
                        korean = null,
                        orderInUnit = i,
                    )
                }
                repo.addDialogueBatch(rows)
            }
            onDone(st.count)
        }
    }
}
