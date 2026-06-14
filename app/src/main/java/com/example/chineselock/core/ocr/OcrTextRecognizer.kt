package com.example.chineselock.core.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * 온디바이스 중국어 OCR(ML Kit). 무료·오프라인.
 *
 * 교재 단어 페이지는 2단(좌/우) 구성이라 통째로 OCR하면 좌우가 한 줄로 섞인다.
 * 그래서 2단(twoColumn=true)일 땐 사진을 '왼쪽 반·오른쪽 반'으로 잘라 각각 OCR한 뒤
 * 왼쪽 결과 → 오른쪽 결과 순으로 이어 붙인다. 각 반쪽은 단일 단이라 위→아래로 정확히 읽힌다.
 */
@Singleton
class OcrTextRecognizer @Inject constructor() {

    private val client = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    /** bitmap은 이미 정방향(회전 보정 완료)으로 들어온다. */
    suspend fun recognize(bitmap: Bitmap, twoColumn: Boolean): String {
        if (!twoColumn || bitmap.width < 200) return recognizeOne(bitmap)

        val w = bitmap.width
        val h = bitmap.height
        val mid = w / 2
        // 가운데 살짝 겹치게 잘라 경계 글자 누락 방지(겹친 부분은 Gemini가 한자 기준 중복 제거).
        val pad = (w * 0.02f).toInt()
        val left = Bitmap.createBitmap(bitmap, 0, 0, (mid + pad).coerceAtMost(w), h)
        val right = Bitmap.createBitmap(bitmap, (mid - pad).coerceAtLeast(0), 0, w - (mid - pad).coerceAtLeast(0), h)
        val l = recognizeOne(left)
        val r = recognizeOne(right)
        return buildString {
            append(l)
            if (l.isNotBlank() && r.isNotBlank()) append("\n")
            append(r)
        }
    }

    private suspend fun recognizeOne(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        client.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { result -> cont.resume(result.text) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }
}
