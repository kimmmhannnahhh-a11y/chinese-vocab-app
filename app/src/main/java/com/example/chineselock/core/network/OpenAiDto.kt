package com.example.chineselock.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Chat Completions request/response (JSON mode) ---

@Serializable
data class ChatRequest(
    val model: String = "gpt-4o",
    val messages: List<ChatMessage>,
    @SerialName("response_format") val responseFormat: ResponseFormat = ResponseFormat(),
    val temperature: Double = 0.2,
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ResponseFormat(val type: String = "json_object")

@Serializable
data class ChatResponse(val choices: List<Choice>) {
    @Serializable
    data class Choice(val message: ChatMessage)
}

// --- Structured payloads returned by the model (parsed from message.content) ---

@Serializable
data class VocabExtraction(val items: List<VocabItem>)

@Serializable
data class VocabItem(
    val hanzi: String,
    val pinyin: String,
    val pos: List<String> = emptyList(), // 품사(정규화 풀네임). 다중 가능: 瓶 → ["양사","명사"]
    val meaning: String,
    val category: String? = null,        // 회화 단어 / 어법 단어 / 고유명사 등
)

@Serializable
data class DialogueExtraction(
    val sectionTitle: String? = null,
    val audioTrack: String? = null,
    val type: String = "dialogue", // "dialogue"(대화: A/B) | "passage"(본문: 화자 없음)
    val items: List<DialogueItem>,
)

@Serializable
data class DialogueItem(
    val speaker: String? = null,
    val chinese: String,
    val pinyin: String? = null,
)

/** 해석 페이지(한국어만) 추출 결과. Dialogue와 순서로 매칭. */
@Serializable
data class TranslationExtraction(val lines: List<String>)

/** 오늘의 회화: 오늘의 단어로 만든 AI 예문 1개(중국어+병음+해석). */
@Serializable
data class ExampleSentence(
    val chinese: String = "",
    val pinyin: String = "",
    val korean: String = "",
)
