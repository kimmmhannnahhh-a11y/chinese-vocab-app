package com.example.chineselock.core.ocr

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
 * 사진(InputImage) → 인식된 원문 텍스트. Gemini 구조화 전 단계.
 */
@Singleton
class OcrTextRecognizer @Inject constructor() {

    private val client = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    suspend fun recognize(image: InputImage): String = suspendCancellableCoroutine { cont ->
        client.process(image)
            .addOnSuccessListener { result -> cont.resume(result.text) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
        cont.invokeOnCancellation { /* ML Kit Task는 명시적 취소 API가 없음 */ }
    }
}
