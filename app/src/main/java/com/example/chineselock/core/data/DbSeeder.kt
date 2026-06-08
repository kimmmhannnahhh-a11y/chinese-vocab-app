package com.example.chineselock.core.data

import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.db.StudyUnit
import com.example.chineselock.core.db.StudyUnitDao
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.db.VocabDao
import com.example.chineselock.core.db.VocabPos
import com.example.chineselock.core.db.VocabPosDao
import com.example.chineselock.core.db.DialogueDao
import javax.inject.Inject
import javax.inject.Singleton

/** 첫 실행 시 시안과 동일한 샘플 데이터(3-1 단어/회화)를 넣는다. */
@Singleton
class DbSeeder @Inject constructor(
    private val unitDao: StudyUnitDao,
    private val vocabDao: VocabDao,
    private val vocabPosDao: VocabPosDao,
    private val dialogueDao: DialogueDao,
) {
    fun seedIfEmpty(now: Long) {
        if (vocabDao.count() > 0) return

        val unitId = unitDao.insert(StudyUnit(book = 3, lesson = 1, title = "3-1", createdAt = now))

        data class V(val hanzi: String, val pinyin: String, val pos: List<String>, val meaning: String, val fav: Boolean = false)
        val words = listOf(
            V("圆珠笔", "yuánzhūbǐ", listOf("명사"), "볼펜", fav = true),
            V("啤酒", "píjiǔ", listOf("명사"), "맥주"),
            V("瓶", "píng", listOf("양사", "명사"), "병", fav = true),
            V("一共", "yígòng", listOf("부사"), "모두"),
            V("旅行", "lǚxíng", listOf("명사"), "여행"),
            V("觉得", "juéde", listOf("동사"), "느끼다"),
            V("红", "hóng", listOf("형용사"), "붉다, 빨갛다"),
            V("还是", "háishi", listOf("접속사"), "아니면, 또는"),
            V("给", "gěi", listOf("동사"), "주다"),
            V("名字", "míngzi", listOf("명사"), "이름"),
        )
        words.forEachIndexed { i, w ->
            val id = vocabDao.insert(
                Vocab(
                    unitId = unitId,
                    hanzi = w.hanzi,
                    pinyin = w.pinyin,
                    partOfSpeech = w.pos.joinToString("·"),
                    meaning = w.meaning,
                    category = "회화 단어",
                    isFavorite = w.fav,
                    orderInUnit = i,
                )
            )
            vocabPosDao.insertAll(w.pos.mapIndexed { j, p -> VocabPos(vocabId = id, pos = p, ord = j) })
        }

        data class T(val sp: String, val zh: String, val pin: String, val ko: String)
        val turns = listOf(
            T("A", "你好！你叫什么名字？", "Nǐ hǎo! Nǐ jiào shénme míngzi?", "안녕! 너 이름이 뭐야?"),
            T("B", "我叫李钟文。", "Wǒ jiào Lǐ Zhōngwén.", "내 이름은 이종원이야."),
            T("A", "你好，李钟文！我猜你一定是韩国人。", "Nǐ hǎo, Lǐ Zhōngwén! Wǒ cāi nǐ yídìng shì Hánguó rén.", "안녕 이종원! 너 분명 한국 사람이지."),
            T("B", "你呢？", "Nǐ ne?", "너는?"),
            T("A", "我叫飞龙，法国人，是大学生。", "Wǒ jiào Fēilóng, Fǎguó rén, shì dàxuéshēng.", "난 페이롱, 프랑스 사람이고 대학생이야."),
            T("B", "你为什么来学汉语？", "Nǐ wèishénme lái xué Hànyǔ?", "너는 왜 중국어를 배우러 왔어?"),
            T("A", "去法国的中国人越来越多，我希望以后做汉语翻译。你呢？", "Qù Fǎguó de Zhōngguó rén yuè lái yuè duō, wǒ xīwàng yǐhòu zuò Hànyǔ fānyì. Nǐ ne?", "프랑스에 가는 중국인이 점점 많아져서, 난 나중에 중국어 번역가가 되고 싶어. 너는?"),
            T("B", "是公司派我来学习的。我要先在这儿学习半年，然后在中国工作。", "Shì gōngsī pài wǒ lái xuéxí de. Wǒ yào xiān zài zhèr xuéxí bàn nián, ránhòu zài Zhōngguó gōngzuò.", "회사에서 보내줘서 왔어. 여기서 반년 공부하고 중국에서 일할 거야."),
        )
        turns.forEachIndexed { i, t ->
            dialogueDao.insert(
                Dialogue(
                    unitId = unitId,
                    sectionTitle = "왜 중국어를 배우러 왔나요?",
                    audioTrack = "01-02",
                    speaker = t.sp,
                    chinese = t.zh,
                    pinyin = t.pin,
                    korean = t.ko,
                    orderInUnit = i,
                )
            )
        }
    }
}
