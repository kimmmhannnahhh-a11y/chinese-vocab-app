package com.example.chineselock.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Google Gemini generateContent request/response (JSON 모드) ---
// 무료 티어: Google AI Studio에서 발급한 API 키 사용. OpenAI GPT-4o 대체.

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig = GeminiGenerationConfig(),
)

@Serializable
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null,
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null,
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String, // Base64
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String = "application/json",
    val temperature: Double = 0.2,
    // 2.5-flash의 '사고' 기능을 끈다(thinkingBudget=0). 응답 속도가 약 2배 빨라지고
    // 교재 OCR 정확도는 동일(검증 완료) → 타임아웃 위험 감소.
    @SerialName("thinkingConfig") val thinkingConfig: GeminiThinkingConfig = GeminiThinkingConfig(),
)

@Serializable
data class GeminiThinkingConfig(
    @SerialName("thinkingBudget") val thinkingBudget: Int = 0,
)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList()) {
    @Serializable
    data class GeminiCandidate(val content: GeminiContent? = null)
}
