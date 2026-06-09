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
data class GeminiPart(val text: String)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("responseMimeType") val responseMimeType: String = "application/json",
    val temperature: Double = 0.2,
)

@Serializable
data class GeminiResponse(val candidates: List<GeminiCandidate> = emptyList()) {
    @Serializable
    data class GeminiCandidate(val content: GeminiContent? = null)
}
