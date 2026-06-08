package com.example.chineselock.feature.capture

import com.example.chineselock.core.network.VocabItem

/**
 * 직접 입력 파서: 사용자가 붙여넣은 [뜻 \t 병음 \t 한자] 줄들을 VocabItem으로 변환.
 * 예) "다음에\txiàcì\t下次"
 * - 구분자는 탭 우선, 없으면 콤마. 한 줄에 3칸이 안 되면 건너뜀.
 * - 품사(pos)는 직접 입력에선 없으므로 빈 리스트.
 */
object ManualVocabParser {

    fun parse(raw: String, category: String? = "직접 추가"): List<VocabItem> =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val cols = line.split('\t')
                    .let { if (it.size >= 3) it else line.split(',') }
                    .map { it.trim() }
                if (cols.size < 3) return@mapNotNull null
                val (meaning, pinyin, hanzi) = cols
                if (hanzi.isBlank() || meaning.isBlank()) return@mapNotNull null
                VocabItem(
                    hanzi = hanzi,
                    pinyin = pinyin,
                    pos = emptyList(),
                    meaning = meaning,
                    category = category,
                )
            }
            .toList()
}
