package com.example.chineselock.core.network

import com.example.chineselock.BuildConfig
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OCR raw text -> Gemini(JSON 모드) -> 구조화 데이터.
 * 호출은 "촬영 시 1회"만. 결과는 호출부에서 DB에 영구 저장하여 이후 비용 0.
 * Google AI Studio 무료 티어 키 사용(GEMINI_API_KEY).
 */
@Singleton
class OcrStructurer @Inject constructor(
    private val gemini: GeminiService,
) {
    private val json = Json { ignoreUnknownKeys = true }

    /** system + user 1턴 호출 → 모델이 돌려준 JSON 문자열 반환. */
    private suspend fun complete(systemPrompt: String, userText: String): String {
        val resp = gemini.generate(
            model = MODEL,
            apiKey = BuildConfig.GEMINI_API_KEY,
            request = GeminiRequest(
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(systemPrompt))),
                contents = listOf(GeminiContent(parts = listOf(GeminiPart(userText)))),
            ),
        )
        return resp.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: error("Gemini 응답이 비어 있어요. 키/네트워크를 확인해주세요.")
    }

    suspend fun structureVocab(rawOcrText: String): VocabExtraction =
        json.decodeFromString(complete(VOCAB_SYSTEM_PROMPT, rawOcrText))

    suspend fun structureDialogue(rawOcrText: String): DialogueExtraction =
        json.decodeFromString(complete(DIALOGUE_SYSTEM_PROMPT, rawOcrText))

    /** 해석 페이지(한국어만)를 줄 순서대로 추출. 회화 문장과 순서로 매칭하는 데 사용. */
    suspend fun structureTranslation(rawOcrText: String): TranslationExtraction =
        json.decodeFromString(complete(TRANSLATION_SYSTEM_PROMPT, rawOcrText))

    private companion object {
        const val MODEL = "gemini-2.0-flash"

        const val VOCAB_SYSTEM_PROMPT = """
너는 중국어 교재 단어 페이지 OCR 텍스트를 표로 구조화하는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.
교재 단어는 [한자 | 병음 | (품사) | 한국어 뜻] 형태이며 '회화 단어/어법 단어/고유명사' 같은 섹션으로 묶여 있다.
품사는 작은 색 박스에 한 글자 약자로 표기된다. 다음 약자를 정규화된 풀네임으로 변환하라:
양→양사, 명→명사, 형→형용사, 동→동사, 부→부사, 대→대명사, 접→접속사, 조→조사,
조동→조동사, 수→수사, 감→감탄사, 개→개사, 수량→수량사.
한 단어에 품사 박스가 여러 개면(예: 瓶 [양][명]) 모두 배열에 담아라.
각 항목 필드:
- hanzi: 간체 한자
- pinyin: 성조 부호 포함 병음 (예: yuánzhūbǐ)
- pos: 품사 풀네임 배열. 박스가 없으면 빈 배열 []. 다중이면 표기 순서대로 모두 (예: ["양사","명사"])
- meaning: 한국어 뜻
- category: 이 단어가 속한 섹션 이름(예: "회화 단어", "어법 단어", "고유명사"). 없으면 null
OCR 오타로 보이는 병음/성조는 한자를 기준으로 자연스럽게 교정하라. 단, 한자 자체는 임의로 바꾸지 마라.
스키마: { "items": [ { "hanzi": "", "pinyin": "", "pos": [], "meaning": "", "category": null } ] }
"""

        const val DIALOGUE_SYSTEM_PROMPT = """
너는 중국어 교재 회화 페이지 OCR 텍스트를 정리하는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.
이 페이지에는 한국어 해석이 없을 수 있다. 한국어를 임의로 만들어 넣지 마라(없으면 생략).

먼저 유형을 판단하라:
- 두 명 이상이 주고받는 '대화'면 type="dialogue".
- 화자 구분 없이 쭉 이어진 '본문/지문'이면 type="passage".

최상위 필드:
- sectionTitle: 섹션 제목(예: "작별 인사를 해요"). 없으면 null
- audioTrack: 오디오 트랙 번호(예: "01-03"). 없으면 null
- type: "dialogue" 또는 "passage"
- items: 순서를 유지한 문장 배열

각 item 필드:
- speaker: type가 "dialogue"면 화자를 "A"/"B"로 매긴다(처음 말한 사람=A, 다음 사람=B, 같은 사람은 같은 글자 유지). 교재의 실제 이름은 무시하고 A/B만 사용. type가 "passage"면 null.
- chinese: 중국어(간체) 문장
- pinyin: 병음(없으면 null)
스키마: { "sectionTitle": null, "audioTrack": null, "type": "dialogue", "items": [ { "speaker": "A", "chinese": "", "pinyin": null } ] }
"""

        const val TRANSLATION_SYSTEM_PROMPT = """
너는 회화 해석 페이지 OCR 텍스트에서 한국어 문장만 순서대로 뽑는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.
번호/화자 라벨은 제거하고 한국어 문장 본문만, 페이지에 나온 순서대로 배열에 담아라.
스키마: { "lines": [ "" ] }
"""
    }
}
