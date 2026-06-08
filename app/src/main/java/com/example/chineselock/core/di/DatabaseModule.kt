package com.example.chineselock.core.di

import android.content.Context
import androidx.room.Room
import com.example.chineselock.core.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "chineselock.db")
            .fallbackToDestructiveMigration() // 개발 단계 한정. 출시 전 실제 마이그레이션으로 교체.
            .build()

    @Provides fun provideStudyUnitDao(db: AppDatabase) = db.studyUnitDao()
    @Provides fun provideVocabDao(db: AppDatabase) = db.vocabDao()
    @Provides fun provideDialogueDao(db: AppDatabase) = db.dialogueDao()
    @Provides fun provideCustomNoteDao(db: AppDatabase) = db.customNoteDao()
    @Provides fun provideLearningStateDao(db: AppDatabase) = db.learningStateDao()
    @Provides fun provideVocabPosDao(db: AppDatabase) = db.vocabPosDao()
}
