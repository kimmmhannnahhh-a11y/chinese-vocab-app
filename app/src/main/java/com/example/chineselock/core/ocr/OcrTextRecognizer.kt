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

    /**
     * 줄 좌표로 단(컬럼)을 감지해 좌→우, 각 단은 위→아래 순서로 텍스트를 재구성.
     * 줄의 '시작 x(left)'로 단을 가른다(긴 줄이 가운데를 넘어가도 안정적). 손글씨 메모로
     * 간격 감지가 실패하면 페이지 중앙으로 양분하되, 한쪽이 너무 적으면 단일 단으로 본다.
     */
    private fun orderByColumns(text: Text): String {
        val lines = ArrayList<OcrLine>()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox ?: continue
                if (line.text.isNotBlank()) lines.add(OcrLine(box, line.text))
            }
        }
        if (lines.size < 4) return lines.sortedBy { it.box.top }.joinToString("\n") { it.text }

        val pageLeft = lines.minOf { it.box.left }
        val pageRight = lines.maxOf { it.box.right }
        val pageWidth = (pageRight - pageLeft).coerceAtLeast(1)

        // 1) 줄 시작 x들 중 중앙(25~75%)에서 가장 큰 빈 간격을 단 경계 후보로.
        val lefts = lines.map { it.box.left.toFloat() }.sorted()
        var splitX = pageLeft + pageWidth * 0.5f   // 기본: 페이지 중앙
        var bestGap = 0f
        for (i in 0 until lefts.size - 1) {
            val gap = lefts[i + 1] - lefts[i]
            val mid = (lefts[i] + lefts[i + 1]) / 2f
            val rel = (mid - pageLeft) / pageWidth
            if (rel in 0.25f..0.75f && gap > bestGap) {
                bestGap = gap
                splitX = mid
            }
        }
        // 뚜렷한 간격이 없으면(손글씨로 메워짐) 중앙 양분 사용 — splitX는 이미 중앙.

        val (left, right) = lines.partition { it.box.left < splitX }
        // 2) 한쪽이 전체의 15% 미만이면 사실상 단일 단 → 위→아래로만 정렬.
        val minor = minOf(left.size, right.size)
        val ordered = if (minor < lines.size * 0.15f) {
            lines.sortedBy { it.box.top }
        } else {
            left.sortedBy { it.box.top } + right.sortedBy { it.box.top }
        }
        return ordered.joinToString("\n") { it.text }
    }
}
