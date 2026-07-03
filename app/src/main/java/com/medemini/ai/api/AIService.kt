package com.medemini.ai.api

import com.medemini.ai.model.*
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

interface AIService {
    @POST
    fun chatCompletion(
        @Url url: String,
        @Header("Authorization") auth: String,
        @Body request: ChatCompletionRequest
    ): Call<ChatCompletionResponse>
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageRequest>,
    val tools: List<ToolDefinitionRequest>? = null,
    @SerializedName("tool_choice") val toolChoice: String = "auto",
    val temperature: Float = 0.7f,
    @SerializedName("max_tokens") val maxTokens: Int = 4096,
    val stream: Boolean = false
)

data class MessageRequest(
    val role: String,
    val content: String,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallRequest>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null
)

data class ToolCallRequest(
    val id: String,
    val type: String = "function",
    val function: FunctionRequest
)

data class FunctionRequest(
    val name: String,
    val arguments: String
)

data class ChatCompletionResponse(
    val id: String,
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

data class Choice(
    val index: Int,
    val message: MessageResponse,
    @SerializedName("finish_reason") val finishReason: String
)

data class MessageResponse(
    val role: String,
    val content: String?,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallResponse>?
)

data class ToolCallResponse(
    val id: String,
    val type: String,
    val function: FunctionResponse
)

data class FunctionResponse(
    val name: String,
    val arguments: String
)

data class Usage(
    @SerializedName("prompt_tokens") val promptTokens: Int,
    @SerializedName("completion_tokens") val completionTokens: Int,
    @SerializedName("total_tokens") val totalTokens: Int
)

data class ToolDefinitionRequest(
    val type: String,
    val function: FunctionRequestDefinition
)

data class FunctionRequestDefinition(
    val name: String,
    val description: String,
    val parameters: FunctionParameters
)

data class FunctionParameters(
    val type: String,
    val properties: Map<String, ParameterDefinition>,
    val required: List<String>
)

data class ParameterDefinition(
    val type: String,
    val description: String
)