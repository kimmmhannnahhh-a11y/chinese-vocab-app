package com.example.chineselock.core.network

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiService {
    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}
