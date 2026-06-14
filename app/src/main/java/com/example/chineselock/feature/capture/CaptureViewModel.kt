package com.example.chineselock.feature.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chineselock.core.data.AppRepository
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.network.DialogueItem
import com.example.chineselock.core.network.OcrStructurer
import com.example.chineselock.core.network.VocabItem
import android.graphics.Bitmap
import com.example.chineselock.core.ocr.OcrTextRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

/** 촬영 대상: 단어 페이지 / 회화 페이지 / 회화 번역(해석) 페이지. */
enum class CaptureMode { VOCAB, DIALOGUE, TRANSLATION }

/**
 * 교재 촬영 → ML Kit OCR → Gemini 구조화 → 검토/수정 → 저장.
 * VOCAB: 단어장 / DIALOGUE: 회화 / TRANSLATION: 회화 해석(순서로 매칭하여 채움).
 * 여러 장을 '이어서 추가 촬영'하면 결과가 아래로 누적된다.
 * 흐름: CAMERA → PROCESSING → REVIEW(편집·이어찍기) → 저장 / ERROR.
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
        val title: String = "",
        val items: List<VocabItem> = emptyList(),       // VOCAB
        val lines: List<DialogueItem> = emptyList(),    // DIALOGUE
        val translations: List<String> = emptyList(),   // TRANSLATION
        val sectionTitle: String? = null,
        val audioTrack: String? = null,
        val error: String? = null,
    ) {
        val count: Int get() = when (mode) {
            CaptureMode.VOCAB -> items.size
            CaptureMode.DIALOGUE -> lines.size
            CaptureMode.TRANSLATION -> translations.size
        }
    }

    /** TRANSLATION 모드에서 번역을 채울 대상 단원. */
    private var targetUnitId: Long? = null

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    fun configure(mode: CaptureMode, unitId: Long? = null) {
        targetUnitId = unitId
        _ui.update { if (it.mode == mode) it else it.copy(mode = mode) }
    }

    fun setTitle(s: String) = _ui.update { it.copy(title = s) }

    /** 촬영 결과 처리. 인식 → 모드별 구조화 → 기존 결과에 누적. 실패 시 안내. */
    fun onImageCaptured(bitmap: Bitmap) {
        val twoColumn = _ui.value.mode == CaptureMode.VOCAB
        _ui.update { it.copy(phase = Phase.PROCESSING, error = null) }
        viewModelScope.launch {
            try {
                val raw = recognizer.recognize(bitmap, twoColumn = twoColumn)
                if (raw.isBlank()) {
                    fail("글자를 인식하지 못했어요. 교재에 더 가까이, 밝은 곳에서 다시 찍어보세요.")
                    return@launch
                }
                when (_ui.value.mode) {
                    CaptureMode.VOCAB -> {
                        val ex = structurer.structureVocab(raw)
                        if (ex.items.isEmpty()) { fail("단어를 정리하지 못했어요. 단어 페이지인지 확인하고 다시 시도해주세요."); return@launch }
                        _ui.update { it.copy(phase = Phase.REVIEW, items = it.items + ex.items, error = null) }
                    }
                    CaptureMode.DIALOGUE -> {
                        val ex = structurer.structureDialogue(raw)
                        if (ex.items.isEmpty()) { fail("회화 문장을 정리하지 못했어요. 회화 페이지인지 확인하고 다시 시도해주세요."); return@launch }
                        _ui.update {
                            it.copy(
                                phase = Phase.REVIEW,
                                lines = it.lines + ex.items,
                                sectionTitle = it.sectionTitle ?: ex.sectionTitle,
                                audioTrack = it.audioTrack ?: ex.audioTrack,
                                error = null,
                            )
                        }
                    }
                    CaptureMode.TRANSLATION -> {
                        val ex = structurer.structureTranslation(raw)
                        if (ex.lines.isEmpty()) { fail("번역 문장을 찾지 못했어요. 해석 페이지인지 확인하고 다시 시도해주세요."); return@launch }
                        _ui.update { it.copy(phase = Phase.REVIEW, translations = it.translations + ex.lines, error = null) }
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

    /** 실패 시: 이미 모은 결과가 있으면 검토 화면을 유지(데이터 보존), 없으면 오류 화면. */
    private fun fail(msg: String) = _ui.update {
        if (it.count > 0) it.copy(phase = Phase.REVIEW, error = msg)
        else it.copy(phase = Phase.ERROR, error = msg)
    }

    /** 이어서 추가 촬영: 기존 결과는 두고 카메라로 복귀(다음 촬영분이 아래로 누적됨). */
    fun captureMore() = _ui.update { it.copy(phase = Phase.CAMERA, error = null) }

    // --- 수동 추가/수정 (검토 화면에서 직접 입력·교정) ---
    fun addVocabItem(hanzi: String, pinyin: String, pos: List<String>, meaning: String) = _ui.update {
        it.copy(items = it.items + VocabItem(hanzi = hanzi, pinyin = pinyin, pos = pos, meaning = meaning, category = null))
    }
    fun updateVocabItem(index: Int, hanzi: String, pinyin: String, pos: List<String>, meaning: String) = _ui.update { st ->
        st.copy(items = st.items.mapIndexed { i, v ->
            if (i == index) v.copy(hanzi = hanzi, pinyin = pinyin, pos = pos, meaning = meaning) else v
        })
    }
    fun addDialogueLine(speaker: String?, chinese: String, pinyin: String?) = _ui.update {
        it.copy(lines = it.lines + DialogueItem(speaker = speaker, chinese = chinese, pinyin = pinyin))
    }
    fun updateDialogueLine(index: Int, speaker: String?, chinese: String, pinyin: String?) = _ui.update { st ->
        st.copy(lines = st.lines.mapIndexed { i, d ->
            if (i == index) d.copy(speaker = speaker, chinese = chinese, pinyin = pinyin) else d
        })
    }
    fun addTranslationLine(text: String) = _ui.update { it.copy(translations = it.translations + text) }
    fun updateTranslationLine(index: Int, text: String) = _ui.update { st ->
        st.copy(translations = st.translations.mapIndexed { i, t -> if (i == index) text else t })
    }

    fun removeItem(index: Int) = _ui.update { st ->
        when (st.mode) {
            CaptureMode.VOCAB -> st.copy(items = st.items.filterIndexed { i, _ -> i != index })
            CaptureMode.DIALOGUE -> st.copy(lines = st.lines.filterIndexed { i, _ -> i != index })
            CaptureMode.TRANSLATION -> st.copy(translations = st.translations.filterIndexed { i, _ -> i != index })
        }
    }

    /** 다시 찍기: 결과를 모두 비우고 처음부터. 제목·모드는 유지. */
    fun retake() = _ui.update { UiState(title = it.title, mode = it.mode) }

    /** 등록. VOCAB/DIALOGUE는 새로 저장, TRANSLATION은 기존 회화에 순서대로 매칭. onDone(개수). */
    fun save(now: Long, onDone: (Int) -> Unit) {
        val st = _ui.value
        if (st.count == 0) { onDone(0); return }

        if (st.mode == CaptureMode.TRANSLATION) {
            val unitId = targetUnitId
            if (unitId == null) { onDone(0); return }
            viewModelScope.launch {
                val n = repo.applyTranslations(unitId, st.translations)
                onDone(n)
            }
            return
        }

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
