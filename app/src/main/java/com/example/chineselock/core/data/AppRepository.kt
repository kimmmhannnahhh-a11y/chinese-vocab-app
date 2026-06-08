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

    suspend fun getOrCreateUnit(book: Int, lesson: Int, now: Long): Long = withContext(Dispatchers.IO) {
        unitDao.find(book, lesson)?.id
            ?: unitDao.insert(StudyUnit(book = book, lesson = lesson, title = "$book-$lesson", createdAt = now))
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

    // --- 회화 ---
    fun observeDialogue(unitId: Long): Flow<List<Dialogue>> = dialogueDao.observeByUnit(unitId)
    suspend fun deleteDialogue(id: Long) = withContext(Dispatchers.IO) { dialogueDao.deleteById(id) }
    suspend fun addDialogue(d: Dialogue): Long = withContext(Dispatchers.IO) { dialogueDao.insert(d) }

    // --- 노트 ---
    fun observeNotes(): Flow<List<CustomNote>> = noteDao.observeAll()
    suspend fun upsertNote(note: CustomNote) = withContext(Dispatchers.IO) { noteDao.upsert(note) }
    suspend fun deleteNote(id: Long) = withContext(Dispatchers.IO) { noteDao.delete(id) }
}
