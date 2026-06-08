package com.example.chineselock.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudyUnit::class,
        Vocab::class,
        VocabPos::class,
        Dialogue::class,
        CustomNote::class,
        LearningState::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyUnitDao(): StudyUnitDao
    abstract fun vocabDao(): VocabDao
    abstract fun vocabPosDao(): VocabPosDao
    abstract fun dialogueDao(): DialogueDao
    abstract fun customNoteDao(): CustomNoteDao
    abstract fun learningStateDao(): LearningStateDao
}
