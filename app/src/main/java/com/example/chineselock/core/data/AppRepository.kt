package com.example.chineselock.core.data

import com.example.chineselock.core.db.CustomNote
import com.example.chineselock.core.db.CustomNoteDao
import com.example.chineselock.core.db.Dialogue
import com.example.chineselock.core.db.DialogueDao
import com.example.chineselock.core.db.PartOfSpeechCount
import com.example.chineselock.core.db.StudyUnit
import com.example.chineselock.core.db.StudyUnitDao
import com.example.chineselock.core.db.Vocab
import com.example.chineselock.core.db.VocabDao
import com.example.chineselock.core.db.VocabPos
import com.example.chineselock.core.db.VocabPosDao
import com.example.chineselock.core.network.VocabItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val unitDao: StudyUnitDao,
    private val vocabDao: VocabDao,
    private val vocabPosDao: VocabPosDao,
    private val dialogueDao: DialogueDao,
    private val noteDao: CustomNoteDao,
) {
    // --- 단원 ---
    fun observeUnits(): Flow<List<StudyUnit>> = unitDao.observeAll()
    fun observeUnitsWithVocab(): Flow<List<StudyUnit>> = unitDao.observeWithVocab()
    fun observeUnitsWithDialogue(): Flow<List<StudyUnit>> = unitDao.observeWithDialogue()

    suspend fun getOrCreateUnit(book: Int, lesson: Int, now: Long): Long = withContext(Dispatchers.IO) {
        unitDao.find(book, lesson)?.id
            ?: unitDao.insert(StudyUnit(book = book, lesson = lesson, title = "$book-$lesson", createdAt = now))
    }

    /**
     * 같은 제목(권-과) 단원에 같은 종류 콘텐츠가 이미 있는지 — 중복 저장 방지용.
     * isVocab=true면 단어, false면 회화 존재 여부. (단어/회화는 같은 단원 공유 가능)
     */
    suspend fun titleHasContent(book: Int, lesson: Int, isVocab: Boolean): Boolean = withContext(Dispatchers.IO) {
        val unit = unitDao.find(book, lesson) ?: return@withContext false
        if (isVocab) vocabDao.countByUnit(unit.id) > 0 else dialogueDao.countByUnit(unit.id) > 0
    }

    /** 단원 통째로 삭제(단어·품사·회화 CASCADE). */
    suspend fun deleteUnit(unitId: Long) = withContext(Dispatchers.IO) { unitDao.deleteById(unitId) }

    /** 단원의 '단어만' 전체 삭제(회화는 보존). 단어·회화 모두 비면 단원도 삭제. */
    suspend fun deleteVocabForUnit(unitId: Long) = withContext(Dispatchers.IO) {
        vocabDao.deleteByUnit(unitId)
        removeUnitIfEmpty(unitId)
    }

    /** 단원의 '회화만' 전체 삭제(단어는 보존). 단어·회화 모두 비면 단원도 삭제. */
    suspend fun deleteDialoguesForUnit(unitId: Long) = withContext(Dispatchers.IO) {
        dialogueDao.deleteByUnit(unitId)
        removeUnitIfEmpty(unitId)
    }

    /** 단어·회화가 모두 없으면 빈 단원(제목)을 삭제. */
    private fun removeUnitIfEmpty(unitId: Long) {
        if (vocabDao.countByUnit(unitId) == 0 && dialogueDao.countByUnit(unitId) == 0) {
            unitDao.deleteById(unitId)
        }
    }

    /** 단원 제목(권/과) 수정. "3-1" 같은 입력을 권/과로 파싱해 갱신. */
    suspend fun renameUnit(unitId: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val parts = newTitle.split("-", " ", ".").mapNotNull { it.trim().toIntOrNull() }
        val book = parts.getOrElse(0) { 0 }
        val lesson = parts.getOrElse(1) { 0 }
        val title = if (parts.isNotEmpty()) "$book-$lesson" else newTitle.trim()
        unitDao.rename(unitId, book, lesson, title)
    }

    // --- 단어 ---
    fun observeVocab(unitId: Long): Flow<List<Vocab>> = vocabDao.observeByUnit(unitId)
    fun observeByPartOfSpeech(pos: String): Flow<List<Vocab>> = vocabDao.observeByPartOfSpeech(pos)
    fun observePosCounts(): Flow<List<PartOfSpeechCount>> = vocabDao.observePartOfSpeechCounts()
    fun observeFavorites(): Flow<List<Vocab>> = vocabDao.observeFavorites()
    fun search(q: String): Flow<List<Vocab>> = vocabDao.search(q)

    suspend fun setFavorite(vocabId: Long, fav: Boolean) = withContext(Dispatchers.IO) {
        vocabDao.setFavorite(vocabId, fav)
    }

    suspend fun deleteVocab(id: Long) = withContext(Dispatchers.IO) { vocabDao.deleteById(id) }
    suspend fun updateVocab(v: Vocab) = withContext(Dispatchers.IO) { vocabDao.update(v) }
    suspend fun updateDialogue(d: Dialogue) = withContext(Dispatchers.IO) { dialogueDao.update(d) }

    /** VocabItem(OCR/직접입력 결과)을 단어 + 품사 연결행으로 저장. */
    suspend fun addVocab(unitId: Long, item: VocabItem, order: Int): Long = withContext(Dispatchers.IO) {
        val id = vocabDao.insert(
            Vocab(
                unitId = unitId,
                hanzi = item.hanzi,
                pinyin = item.pinyin,
                partOfSpeech = item.pos.joinToString("·").ifEmpty { null },
                meaning = item.meaning,
                category = item.category,
                orderInUnit = order,
            )
        )
        if (item.pos.isNotEmpty()) {
            vocabPosDao.insertAll(item.pos.mapIndexed { i, p -> VocabPos(vocabId = id, pos = p, ord = i) })
        }
        id
    }

    suspend fun addVocabBatch(unitId: Long, items: List<VocabItem>, startOrder: Int) = withContext(Dispatchers.IO) {
        items.forEachIndexed { i, item -> addVocab(unitId, item, startOrder + i) }
    }

    /** 오늘의 단어: 날짜-결정형(같은 날 같은 단어). */
    suspend fun todayWord(epochDay: Long): Vocab? = withContext(Dispatchers.IO) {
        val n = vocabDao.count()
        if (n == 0) null else vocabDao.getAt((epochDay % n).toInt())
    }

    /** 오늘의 회화: 오늘의 단어가 들어간 회화 문장(없으면 날짜-결정형으로 아무 회화). */
    suspend fun todayDialogue(word: Vocab?, epochDay: Long): Dialogue? = withContext(Dispatchers.IO) {
        if (word != null) dialogueDao.findContaining(word.hanzi)?.let { return@withContext it }
        val n = dialogueDao.count()
        if (n == 0) null else dialogueDao.getAt((epochDay % n).toInt())
    }

    // --- 회화 ---
    fun observeDialogue(unitId: Long): Flow<List<Dialogue>> = dialogueDao.observeByUnit(unitId)
    suspend fun deleteDialogue(id: Long) = withContext(Dispatchers.IO) { dialogueDao.deleteById(id) }
    suspend fun addDialogue(d: Dialogue): Long = withContext(Dispatchers.IO) { dialogueDao.insert(d) }
    suspend fun addDialogueBatch(items: List<Dialogue>) = withContext(Dispatchers.IO) { dialogueDao.insertAll(items) }

    /** 해석 페이지에서 뽑은 한국어를 회화 문장에 순서대로 매칭해 채운다. 채운 개수 반환. */
    suspend fun applyTranslations(unitId: Long, koreanLines: List<String>): Int = withContext(Dispatchers.IO) {
        val dialogues = dialogueDao.getByUnit(unitId)
        val n = minOf(dialogues.size, koreanLines.size)
        for (i in 0 until n) {
            dialogueDao.update(dialogues[i].copy(korean = koreanLines[i]))
        }
        n
    }

    // --- 노트 ---
    fun observeNotes(): Flow<List<CustomNote>> = noteDao.observeAll()
    suspend fun upsertNote(note: CustomNote) = withContext(Dispatchers.IO) { noteDao.upsert(note) }
    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) { noteDao.delete(id) }
}
