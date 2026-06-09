package com.example.chineselock.core.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiService {
    /**
     * 예: v1beta/models/gemini-2.0-flash:generateContent?key=...
     * 키는 쿼리 파라미터로 전달(구글 표준). 출시 시에는 프록시 경유 권장.
     */
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse
}
