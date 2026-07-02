package com.medemini.ai.api

import com.medemini.ai.model.ChatRequest
import com.medemini.ai.model.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * AI Chat API 接口
 * 支持 OpenAI 兼容的 API
 */
interface AIService {
    
    @POST
    suspend fun chat(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequest
    ): Response<ChatResponse>
    
    @POST
    suspend fun chatStream(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatRequest
    ): Response<okhttp3.ResponseBody>
}

/**
 * AI API 客户端工厂
 */
object AIClient {
    private const val BASE_URL = "https://api.openai.com/v1/"
    
    fun create(apiKey: String, baseUrl: String = BASE_URL): AIService {
        val client = retrofit2.Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        
        return client.create(AIService::class.java)
    }
    
    fun defaultClient(): AIService {
        return create(apiKey = "")
    }
}