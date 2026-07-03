package com.medemini.ai.model

data class AIMessage(
    val id: String,
    val role: String,
    val content: String,
    val thinking: String? = null,
    val toolCalls: List<ToolCall>? = null,
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class ToolCall(
    val id: String,
    val type: String,
    val function: ToolFunction
)

data class ToolFunction(
    val name: String,
    val arguments: Map<String, String>
)

data class ToolResult(
    val toolCallId: String,
    val name: String,
    val content: String,
    val success: Boolean = true
)

data class CodeChange(
    val type: ChangeType,
    val lineNumber: Int,
    val oldContent: String,
    val newContent: String,
    val startOffset: Int,
    val endOffset: Int
)

data class FileChange(
    val filePath: String,
    val originalContent: String,
    val newContent: String,
    val lineChanges: List<CodeChange>
)

enum class ChangeType {
    INSERT, DELETE, MODIFY
}

data class AIConfig(
    var apiBaseUrl: String = "https://api.openai.com/v1",
    var apiKey: String = "",
    var model: String = "gpt-4o-mini",
    var temperature: Float = 0.7f,
    var maxTokens: Int = 4096,
    var mcpServices: List<MCPService> = emptyList()
)

data class MCPService(
    val id: String,
    val name: String,
    val url: String,
    val enabled: Boolean = true
)