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
 * 교재가 2단(좌/우) 구성이면 ML Kit 기본 읽기 순서가 좌우를 섞어버린다.
 * 줄의 좌표(boundingBox)로 단을 나눠 '왼쪽 세로 전체 → 오른쪽 세로 전체' 순서로 재정렬한다.
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

    private data class OcrLine(val box: Rect, val text: String)

    /** 줄 좌표로 단(컬럼)을 감지해 좌→우, 각 단은 위→아래 순서로 텍스트를 재구성. */
    private fun orderByColumns(text: Text): String {
        val lines = ArrayList<OcrLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                if (line.text.isNotBlank()) lines.add(OcrLine(box, line.text))
            }
        }
        if (lines.size < 2) return text.text

        val pageLeft = lines.minOf { it.box.left }
        val pageRight = lines.maxOf { it.box.right }
        val pageWidth = (pageRight - pageLeft).coerceAtLeast(1)

        // 줄 중심 x들을 정렬해 '중앙 영역(30~70%)에서 가장 큰 빈 간격'을 단 경계로 본다.
        val centers = lines.map { it.box.exactCenterX() }.sorted()
        var splitX = Float.NaN
        var bestGap = 0f
        for (i in 0 until centers.size - 1) {
            val gap = centers[i + 1] - centers[i]
            val mid = (centers[i] + centers[i + 1]) / 2f
            val rel = (mid - pageLeft) / pageWidth
            if (rel in 0.30f..0.70f && gap > bestGap) {
                bestGap = gap
                splitX = mid
            }
        }
        val twoColumns = !splitX.isNaN() && bestGap > 0.12f * pageWidth

        val ordered = if (twoColumns) {
            val (left, right) = lines.partition { it.box.exactCenterX() < splitX }
            left.sortedBy { it.box.top } + right.sortedBy { it.box.top }
        } else {
            lines.sortedBy { it.box.top }
        }
        return ordered.joinToString("\n") { it.text }
    }
}
