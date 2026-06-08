package com.example.chineselock.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.ColumnInfo
import kotlinx.coroutines.flow.Flow

/** 단어찾기 화면의 품사별 개수 집계 결과. */
data class PartOfSpeechCount(
    @ColumnInfo(name = "pos") val pos: String,
    @ColumnInfo(name = "count") val count: Int,
)

@Dao
interface StudyUnitDao {
    @Insert fun insert(unit: StudyUnit): Long

    @Query("SELECT * FROM study_unit ORDER BY book, lesson")
    fun observeAll(): Flow<List<StudyUnit>>

    @Query("SELECT * FROM study_unit WHERE book = :book AND lesson = :lesson LIMIT 1")
    fun find(book: Int, lesson: Int): StudyUnit?
}

@Dao
interface VocabDao {
    @Insert fun insertAll(items: List<Vocab>): List<Long>

    // 수정(편집): 단어 추가 / 수정 / 삭제
    @Insert fun insert(item: Vocab): Long
    @Update fun update(item: Vocab)
    @Query("DELETE FROM vocab WHERE id = :id") fun deleteById(id: Long)

    @Query("SELECT * FROM vocab WHERE unitId = :unitId ORDER BY orderInUnit")
    fun observeByUnit(unitId: Long): Flow<List<Vocab>>

    /** 단어찾기: 등록한 모든 단어에서 품사로 필터 (단원 무관, 다중 품사 단어도 잡힘). */
    @Query(
        """SELECT v.* FROM vocab v
           JOIN vocab_pos p ON v.id = p.vocabId
           WHERE p.pos = :pos
           ORDER BY v.hanzi"""
    )
    fun observeByPartOfSpeech(pos: String): Flow<List<Vocab>>

    /** 오늘의 단어: 등록된 단어 수와 인덱스로 날짜-결정형 선택(같은 날엔 같은 단어). */
    @Query("SELECT COUNT(*) FROM vocab")
    fun count(): Int

    @Query("SELECT * FROM vocab LIMIT 1 OFFSET :index")
    fun getAt(index: Int): Vocab?

    /** 단어 검색: 한자·병음·한국어 뜻 어느 쪽이든 부분 일치(중국어/한글 모두 검색). */
    @Query(
        """SELECT * FROM vocab
           WHERE hanzi LIKE '%' || :q || '%'
              OR pinyin LIKE '%' || :q || '%'
              OR meaning LIKE '%' || :q || '%'
           ORDER BY hanzi"""
    )
    fun search(q: String): Flow<List<Vocab>>

    /** 즐겨찾기(★) 단어만 모아보기. */
    @Query("SELECT * FROM vocab WHERE isFavorite = 1 ORDER BY hanzi")
    fun observeFavorites(): Flow<List<Vocab>>

    /** 즐겨찾기 토글. */
    @Query("UPDATE vocab SET isFavorite = :fav WHERE id = :vocabId")
    fun setFavorite(vocabId: Long, fav: Boolean)

    /** 단어찾기 화면의 품사 칩 목록 + 개수. 데이터에 실제로 있는 품사만 노출. */
    @Query(
        """SELECT pos, COUNT(DISTINCT vocabId) AS count FROM vocab_pos
           GROUP BY pos ORDER BY count DESC"""
    )
    fun observePartOfSpeechCounts(): Flow<List<PartOfSpeechCount>>

    /** 잠금화면: 오늘 단원에서 미숙 단어 가중 무작위 N개. */
    @Query(
        """SELECT v.* FROM vocab v
           LEFT JOIN learning_state s ON v.id = s.vocabId
           WHERE v.unitId = :unitId
           ORDER BY COALESCE(s.seenCount, 0) ASC, RANDOM()
           LIMIT :n"""
    )
    fun pickForLockscreen(unitId: Long, n: Int): List<Vocab>
}

@Dao
interface VocabPosDao {
    @Insert fun insertAll(rows: List<VocabPos>)
}

@Dao
interface DialogueDao {
    @Insert fun insertAll(items: List<Dialogue>): List<Long>

    @Query("SELECT * FROM dialogue WHERE unitId = :unitId ORDER BY orderInUnit")
    fun observeByUnit(unitId: Long): Flow<List<Dialogue>>

    // 수정(편집): 문장 추가 / 수정 / 삭제
    @Insert fun insert(item: Dialogue): Long
    @Update fun update(item: Dialogue)
    @Query("DELETE FROM dialogue WHERE id = :id") fun deleteById(id: Long)
}

@Dao
interface CustomNoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) fun upsert(note: CustomNote): Long

    @Query("SELECT * FROM custom_note ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<CustomNote>>

    @Query("DELETE FROM custom_note WHERE id = :id")
    fun delete(id: Long)
}

@Dao
interface LearningStateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insertIfAbsent(state: LearningState)

    @Update fun update(state: LearningState)

    @Query("SELECT * FROM learning_state WHERE vocabId = :vocabId")
    fun find(vocabId: Long): LearningState?
}

