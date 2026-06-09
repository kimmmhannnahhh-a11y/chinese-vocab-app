package com.example.chineselock.core.ocr

import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 온디바이스 중국어 OCR(ML Kit). 무료·오프라인.
 * 사진(InputImage) → 인식된 원문 텍스트. Gemini 구조화 전 단계.
 *
 * 교재가 2단(좌/우) 구성이면 ML Kit 기본 읽기 순서가 좌우를 한 줄로 섞어버린다.
 * 그래서 '글자(element) 단위 좌표'로 좌/우 단을 가른 뒤,
 * 왼쪽 단을 위→아래로 전부 재조립하고 그다음 오른쪽 단을 위→아래로 재조립한다.
 */
@Singleton
class OcrTextRecognizer @Inject constructor() {

    private val client = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    suspend fun recognize(image: InputImage): String = suspendCancellableCoroutine { cont ->
        client.process(image)
            .addOnSuccessListener { result -> cont.resume(orderByColumns(result)) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    private data class El(val box: Rect, val text: String)

    private fun orderByColumns(text: Text): String {
        val els = ArrayList<El>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (e in line.elements) {
                    val b = e.boundingBox ?: continue
                    if (e.text.isNotBlank()) els.add(El(b, e.text))
                }
            }
        }
        if (els.size < 6) return text.text

        val pageLeft = els.minOf { it.box.left }
        val pageRight = els.maxOf { it.box.right }
        val width = (pageRight - pageLeft).coerceAtLeast(1)
        val splitX = pageLeft + width * 0.5f

        val left = els.filter { it.box.exactCenterX() < splitX }
        val right = els.filter { it.box.exactCenterX() >= splitX }

        // 양쪽 단에 충분한 글자가 있으면 2단으로 보고 좌→우, 아니면 단일 단.
        return if (left.size >= 4 && right.size >= 4) {
            rebuild(left) + "\n" + rebuild(right)
        } else {
            rebuild(els)
        }
    }

    /** 글자들을 같은 줄(행)끼리 묶어 위→아래로, 각 행은 좌→우로 이어 붙인다. */
    private fun rebuild(els: List<El>): String {
        if (els.isEmpty()) return ""
        val heights = els.map { it.box.height() }.sorted()
        val medianH = heights[heights.size / 2].coerceAtLeast(1)
        val rowGap = medianH * 0.6f

        val sorted = els.sortedBy { it.box.exactCenterY() }
        val rows = ArrayList<MutableList<El>>()
        var rowY = Float.NEGATIVE_INFINITY
        for (e in sorted) {
            val cy = e.box.exactCenterY()
            if (rows.isEmpty() || cy - rowY > rowGap) {
                rows.add(mutableListOf(e))
            } else {
                rows.last().add(e)
            }
            rowY = rows.last().map { it.box.exactCenterY() }.average().toFloat()
        }
        return rows.joinToString("\n") { row ->
            row.sortedBy { it.box.left }.joinToString(" ") { it.text }
        }
    }
}
