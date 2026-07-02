package com.medemini.ai.model

/**
 * AI 消息类型
 */
enum class MessageType {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * AI 消息数据类
 */
data class AIMessage(
    val role: MessageType,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * AI 请求数据类
 */
data class ChatRequest(
    val messages: List<AIMessage>,
    val model: String = "default",
    val temperature: Double = 0.7
)

/**
 * AI 响应数据类
 */
data class ChatResponse(
    val message: AIMessage,
    val usage: Usage? = null
)

/**
 * 令牌使用信息
 */
data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * AI 设置数据类
 */
data class AISettings(
    val apiKey: String = "",
    val apiEndpoint: String = "https://api.openai.com/v1",
    val model: String = "gpt-3.5-turbo",
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048
)