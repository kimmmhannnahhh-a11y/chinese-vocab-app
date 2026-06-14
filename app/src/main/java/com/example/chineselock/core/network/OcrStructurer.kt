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
        // gemini-2.0-flash는 일부 프로젝트에서 무료 할당량이 0(429)이라 사용 불가.
        // 2.5-flash-lite는 무료 티어에서 안정적으로 동작(JSON 모드 지원).
        const val MODEL = "gemini-2.5-flash-lite"

        const val VOCAB_SYSTEM_PROMPT = """
너는 중국어 교재 단어 페이지 OCR 텍스트를 표로 구조화하는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.

[무엇을 추출하나]
교재에 '인쇄된' 단어 목록 항목만 추출한다. 인쇄 항목은 반드시 [한자 + 병음(로마자) + 한국어 뜻]이 한 세트로 같이 있다.
- 한자 옆에 병음(로마자, 예: cuò, huílái)이 인쇄되어 있는 것만 진짜 단어 항목이다.
- 병음이 없는 것은 학습자 손글씨/메모이므로 버려라.

[손글씨/잡음 무시 — 매우 중요]
학습자가 연필·펜으로 적은 메모, 낙서, 동그라미, 화살표, 별표, 밑줄, 한글 풀이, 페이지번호, 트랙아이콘은 단어가 아니다. 절대 추출하지 마라.
특히 OCR에 한자가 하나 보여도(예: 손으로 쓴 吃, 完了, 就 같은 메모) 그 옆에 '인쇄된 병음과 인쇄된 뜻'이 세트로 없으면 손글씨다 — 버려라.

[문형(grammar pattern) 항목]
'一……就……', 'A……B……' 처럼 점(…/.../··)이 들어간 문형도 하나의 정식 항목이다. 절대 빠뜨리지 마라.
이 경우 hanzi 필드에 점을 빼고 한자만 이어서(예: "一就") 넣지 말고, 문형 그대로 "一…就…" 형태로 넣어라. 병음도 "yī…jiù…" 처럼 넣어라.

[품사]
품사는 작은 색 박스에 한 글자 약자로 표기된다. 약자를 풀네임으로 변환:
양→양사, 명→명사, 형→형용사, 동→동사, 부→부사, 대→대명사, 접→접속사, 조→조사,
조동→조동사, 수→수사, 감→감탄사, 개→개사, 수량→수량사. 박스가 여러 개면(예: 瓶 [양][명]) 모두 배열에.

[필드]
- hanzi: 간체 한자(문형이면 점 포함 그대로)
- pinyin: 성조 부호 포함 병음
- pos: 품사 풀네임 배열. 없으면 []
- meaning: 한국어 뜻(인쇄된 뜻만. 손글씨 뜻은 무시)
- category: 섹션 이름("회화 단어"/"어법 단어"/"고유명사"). 없으면 null
OCR 오타로 보이는 병음/성조는 한자 기준으로 자연스럽게 교정하되, 한자 자체는 임의로 바꾸지 마라.
교재에 인쇄된 항목은 하나도 빠뜨리지 말고 인쇄 순서대로 모두 담아라.
스키마: { "items": [ { "hanzi": "", "pinyin": "", "pos": [], "meaning": "", "category": null } ] }
"""

        const val DIALOGUE_SYSTEM_PROMPT = """
너는 중국어 교재 회화 페이지 OCR 텍스트를 정리하는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.

[화자 라벨이 턴의 경계다 — 핵심 규칙]
교재의 각 발화는 줄 맨 앞에 '화자 이름표'로 시작한다. 이름표는 한글 이름(예: 샤오예, 릴리, 나오미, 다니엘), 알파벳(A, B), 또는 사람 이름이다.
- 줄 맨 앞에 이름표가 나오면 거기서 '새 턴'이 시작된다.
- 이름표 없이 이어지는 줄(중국어가 줄바꿈으로 계속되거나, 병음 줄)은 '바로 앞 턴'에 속한다 → 그 턴의 chinese/pinyin에 이어 붙인다. 절대 새 턴으로 쪼개지 마라.
- 이름표를 A/B/C…로 정규화: 처음 나온 이름표=A, 그다음 '다른' 이름표=B, 같은 이름표가 다시 나오면 같은 글자 유지. (예: 샤오예=A, 릴리=B, 다시 샤오예=A)
- 실제 이름은 버리고 A/B만 출력한다.

[손글씨/잡음 무시]
이름표 없이 떠 있는 한자 조각(예: 손글씨 吃完了, 加油), 별표(* * *), 각주번호(①②), 페이지번호, 트랙아이콘, 한글 메모는 회화 문장이 아니다 → 버린다. (이름표 바로 뒤에 이어지는 정식 중국어 문장만 회화다.)

[예시]
입력:
민수 你好!
Nǐ hǎo!
지영 你好，好久不见。
最近怎么样?
Nǐ hǎo, hǎojiǔ bújiàn. Zuìjìn zěnmeyàng?
민수 我很好。
Wǒ hěn hǎo.
加油 (손글씨)
출력:
{"sectionTitle":null,"audioTrack":null,"type":"dialogue","items":[
{"speaker":"A","chinese":"你好!","pinyin":"Nǐ hǎo!"},
{"speaker":"B","chinese":"你好，好久不见。最近怎么样?","pinyin":"Nǐ hǎo, hǎojiǔ bújiàn. Zuìjìn zěnmeyàng?"},
{"speaker":"A","chinese":"我很好。","pinyin":"Wǒ hěn hǎo."}
]}
(지영의 둘째 줄은 이름표가 없으니 합쳐졌고, 손글씨 '加油'는 버려졌다.)

[유형]
이름표로 주고받으면 type="dialogue". 이름표 없이 쭉 이어진 본문/지문이면 type="passage"이고 speaker는 모두 null.

[최상위 필드]
- sectionTitle: 섹션 제목(예: "전화 잘못 거셨습니다", "가 본 적 있어요?"). 끝의 마침표는 떼라. 없으면 null
- audioTrack: 트랙 번호(예: "03-02"). 없으면 null
- type, items
[item 필드] speaker(A/B…/passage면 null), chinese(턴 전체), pinyin(없으면 null). 한국어 해석은 넣지 마라.
스키마: { "sectionTitle": null, "audioTrack": null, "type": "dialogue", "items": [ { "speaker": "A", "chinese": "", "pinyin": null } ] }
"""

        const val TRANSLATION_SYSTEM_PROMPT = """
너는 회화 해석 페이지 OCR 텍스트에서 한국어 문장만 순서대로 뽑는 도우미다. 출력은 반드시 유효한 JSON 하나뿐.
번호/화자 라벨은 제거하고 한국어 문장 본문만, 페이지에 나온 순서대로 배열에 담아라.
스키마: { "lines": [ "" ] }
"""
    }
}
