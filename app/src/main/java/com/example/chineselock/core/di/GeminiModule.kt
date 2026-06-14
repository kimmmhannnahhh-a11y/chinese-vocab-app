package com.example.chineselock.core.di

import com.example.chineselock.BuildConfig
import com.example.chineselock.core.network.GeminiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Gemini는 OpenAI와 base URL이 달라 전용 Retrofit을 내부에서 구성한다.
 * (Hilt에 Retrofit/OkHttp를 별도로 @Provides 하면 NetworkModule과 중복 바인딩되므로 노출하지 않음)
 */
@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    private const val GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/"

    // null 필드를 직렬화에서 빼야 한다(예: 이미지 파트의 text=null). explicitNulls=false.
    private val geminiJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideGeminiService(): GeminiService {
        val json = geminiJson
        val logging = HttpLoggingInterceptor().apply {
            // BODY로 하면 base64 이미지가 통째로 로그에 찍히므로 BASIC만.
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        // Vision 호출은 이미지 업로드+모델 추론으로 10초 이상 걸린다(2.5-flash ~10~25초).
        // OkHttp 기본 readTimeout(10초)이면 "단어 인식 전 타임아웃"이 나므로 넉넉히 늘린다.
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)   // base64 이미지 업로드
            .readTimeout(120, TimeUnit.SECONDS)   // 모델 추론 대기
            .callTimeout(150, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(GEMINI_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeminiService::class.java)
    }
}
