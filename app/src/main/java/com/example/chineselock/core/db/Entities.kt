package com.example.chineselock.core.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** 단원: "3권 1과" 등. OCR 업로드 시 사용자가 입력/인식한 대단원 분류 단위. */
@Entity(tableName = "study_unit")
data class StudyUnit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: Int,
    val lesson: Int,
    val title: String,
    val createdAt: Long,
)

/**
 * 단어장: 교재 구조 그대로 [한자 | 병음 | 품사 | 뜻]. category로 섹션(회화단어/어법단어/고유명사) 구분.
 * 한 단어가 품사를 여러 개 가질 수 있어(예: 瓶 = 양사·명사) 품사는 vocab_pos 테이블로 분리.
 * partOfSpeech는 화면 표시용으로 합친 문자열(예: "양사·명사").
 */
@Entity(
    tableName = "vocab",
    foreignKeys = [ForeignKey(
        entity = StudyUnit::class,
        parentColumns = ["id"],
        childColumns = ["unitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("unitId")],
)
data class Vocab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val hanzi: String,
    val pinyin: String,
    val partOfSpeech: String?, // 표시용 합친 품사 (예: "양사·명사"). 필터는 vocab_pos 사용
    val meaning: String,       // 한국어 뜻
    val category: String?,     // 회화 단어 / 어법 단어 / 고유명사 등
    val isFavorite: Boolean = false, // 즐겨찾기 ★
    val orderInUnit: Int,
)

/**
 * 단어-품사 연결(1:N). 다중 품사 단어를 품사별 필터에서 모두 잡기 위함.
 * 예: 瓶 → (양사),(명사) 두 행. 단어찾기에서 양사·명사 양쪽 모두에 노출됨.
 */
@Entity(
    tableName = "vocab_pos",
    primaryKeys = ["vocabId", "pos"],
    foreignKeys = [ForeignKey(
        entity = Vocab::class,
        parentColumns = ["id"],
        childColumns = ["vocabId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("pos")],
)
data class VocabPos(
    val vocabId: Long,
    val pos: String, // 정규화된 품사명 (명사/동사/형용사/부사/양사/대명사/접속사/조사/조동사/수사 등)
    val ord: Int,    // 교재 표기 순서 (표시용)
)

/**
 * 회화: 교재 회화 페이지엔 [화자 | 중국어 | 병음]만 있고 한국어 해석은 없음.
 * 한국어(korean)는 해석 페이지를 따로 촬영해 순서로 매칭하여 채운다(처음엔 null).
 */
@Entity(
    tableName = "dialogue",
    foreignKeys = [ForeignKey(
        entity = StudyUnit::class,
        parentColumns = ["id"],
        childColumns = ["unitId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("unitId")],
)
data class Dialogue(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long,
    val sectionTitle: String?, // 회화 섹션 제목 (예: "작별 인사를 해요")
    val audioTrack: String?,   // 오디오 트랙 번호 (예: "01-03")
    val speaker: String?,      // 화자 라벨 (예: "A", "B")
    val chinese: String,
    val pinyin: String?,
    val korean: String?,       // 해석 페이지 매칭으로 채움. 매칭 전엔 null
    val orderInUnit: Int,
)

/** 커스텀 노트: 문법/뉘앙스 자유 텍스트. unitId=null이면 전역 노트. */
@Entity(tableName = "custom_note")
data class CustomNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val unitId: Long?,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

/** 학습 상태: 간이 SRS(잠금화면용 가중치 등에 사용). */
@Entity(tableName = "learning_state")
data class LearningState(
    @PrimaryKey val vocabId: Long,
    val seenCount: Int = 0,
    val correctCount: Int = 0,
    val lastSeenAt: Long = 0,
    val isMastered: Boolean = false,
)
