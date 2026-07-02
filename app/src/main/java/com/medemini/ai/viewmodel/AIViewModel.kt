package com.medemini.ai.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medemini.ai.model.*
import com.medemini.ai.api.AIClient
import com.medemini.ai.api.AIService
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.prefs.Preferences

/**
 * AI 对话状态
 */
sealed class ChatState {
    object Idle : ChatState()
    object Loading : ChatState()
    data class Success(val messages: List<AIMessage>) : ChatState()
    data class Error(val message: String) : ChatState()
}

/**
 * 代码修改操作
 */
data class CodeEditOperation(
    val originalCode: String,
    val modifiedCode: String,
    val startLine: Int,
    val endLine: Int,
    val description: String,
    val toolName: String
)

/**
 * 待审查状态
 */
data class PendingReview(
    val operations: List<CodeEditOperation>,
    val originalFileContent: String,
    val newFileContent: String,
    val messages: List<AIMessage>
)

/**
 * AI 工具定义
 */
data class AITool(
    val name: String,
    val description: String,
    val parameters: Map<String, String>
)

/**
 * AI ViewModel - 管理 AI 对话、工具和代码审查
 */
class AIViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _chatState = MutableStateFlow<ChatState>(ChatState.Idle)
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()
    
    private val _messages = mutableStateListOf<AIMessage>()
    val messages: MutableList<AIMessage> get() = _messages
    
    private val _pendingReview = MutableStateFlow<PendingReview?>(null)
    val pendingReview: StateFlow<PendingReview?> = _pendingReview
    
    private val _showAIChat = MutableStateFlow(false)
    val showAIChat: StateFlow<Boolean> = _showAIChat
    
    private var apiKey: String = ""
    private var apiEndpoint: String = "https://api.openai.com/v1"
    private var mcpEndpoint: String = ""
    private var modelName: String = "gpt-4"
    private var temperature: Double = 0.7
    private var maxTokens: Int = 4096
    private var aiService: AIService? = null
    
    // 内置 AI 工具列表 (50+ 工具)
    val availableTools = listOf(
        // 文件操作工具 (10个)
        AITool("read_file", "读取指定路径的文件内容", mapOf("path" to "文件路径")),
        AITool("write_file", "写入内容到文件，会创建不存在的文件", mapOf("path" to "文件路径", "content" to "文件内容")),
        AITool("create_file", "创建新文件，如果文件已存在则失败", mapOf("path" to "文件路径", "content" to "初始内容")),
        AITool("delete_file", "删除指定文件", mapOf("path" to "文件路径")),
        AITool("list_directory", "列出目录中的所有文件和子目录", mapOf("path" to "目录路径")),
        AITool("search_files", "搜索文件名匹配模式的文件", mapOf("path" to "搜索目录", "pattern" to "匹配模式")),
        AITool("copy_file", "复制文件到目标位置", mapOf("source" to "源路径", "destination" to "目标路径")),
        AITool("move_file", "移动文件到目标位置", mapOf("source" to "源路径", "destination" to "目标路径")),
        AITool("get_file_info", "获取文件的详细信息", mapOf("path" to "文件路径")),
        AITool("create_directory", "创建新目录", mapOf("path" to "目录路径")),
        
        // 代码编辑工具 (15个)
        AITool("insert_line", "在指定行插入新代码", mapOf("path" to "文件路径", "line" to "行号", "content" to "插入内容")),
        AITool("delete_lines", "删除指定范围的行", mapOf("path" to "文件路径", "start" to "起始行", "end" to "结束行")),
        AITool("replace_line", "替换指定行的内容", mapOf("path" to "文件路径", "line" to "行号", "content" to "新内容")),
        AITool("replace_range", "替换指定行范围的内容", mapOf("path" to "文件路径", "start" to "起始行", "end" to "结束行", "content" to "新内容")),
        AITool("find_references", "查找函数/变量的所有引用", mapOf("name" to "名称", "path" to "搜索目录")),
        AITool("find_definition", "查找函数/变量的定义", mapOf("name" to "名称")),
        AITool("rename_symbol", "重命名符号（全局替换）", mapOf("oldName" to "旧名称", "newName" to "新名称")),
        AITool("add_import", "添加导入语句", mapOf("path" to "文件路径", "import" to "导入语句")),
        AITool("remove_import", "移除导入语句", mapOf("path" to "文件路径", "import" to "导入语句")),
        AITool("add_method", "在类中添加方法", mapOf("path" to "文件路径", "class" to "类名", "method" to "方法定义")),
        AITool("add_field", "在类中添加字段", mapOf("path" to "文件路径", "class" to "类名", "field" to "字段定义")),
        AITool("add_annotation", "添加注解/注释", mapOf("path" to "文件路径", "target" to "目标符号", "annotation" to "注解内容")),
        AITool("remove_method", "移除类中的方法", mapOf("path" to "文件路径", "class" to "类名", "method" to "方法名")),
        AITool("extract_method", "提取代码块为新方法", mapOf("path" to "文件路径", "start" to "起始行", "end" to "结束行", "name" to "新方法名")),
        AITool("format_code", "格式化代码", mapOf("path" to "文件路径")),
        
        // 代码分析工具 (10个)
        AITool("analyze_syntax", "分析代码语法错误", mapOf("path" to "文件路径")),
        AITool("find_bugs", "查找代码中的潜在 Bug", mapOf("path" to "文件路径")),
        AITool("check_types", "检查类型错误", mapOf("path" to "文件路径")),
        AITool("complexity_analysis", "分析代码复杂度", mapOf("path" to "文件路径")),
        AITool("find_duplicates", "查找重复代码", mapOf("path" to "搜索目录")),
        AITool("dependency_analysis", "分析依赖关系", mapOf("path" to "文件路径")),
        AITool("code_metrics", "计算代码度量指标", mapOf("path" to "文件路径")),
        AITool("find_deprecated", "查找已弃用的 API 使用", mapOf("path" to "搜索目录")),
        AITool("todo_finder", "查找 TODO/FIXME 注释", mapOf("path" to "搜索目录")),
        AITool("comment_code", "为代码添加注释", mapOf("path" to "文件路径", "start" to "起始行", "end" to "结束行")),
        
        // 构建和测试工具 (8个)
        AITool("build_project", "构建项目", mapOf()),
        AITool("run_tests", "运行测试", mapOf("filter" to "测试过滤器(可选)")),
        AITool("clean_build", "清理构建缓存", mapOf()),
        AITool("generate_docs", "生成文档", mapOf("path" to "生成文档的路径")),
        AITool("debug", "调试代码", mapOf("path" to "文件路径", "line" to "断点行号")),
        AITool("run_command", "运行终端命令", mapOf("command" to "命令")),
        AITool("check_dependencies", "检查依赖更新", mapOf()),
        AITool("gradle_task", "执行 Gradle 任务", mapOf("task" to "任务名")),
        
        // Git 工具 (4个)
        AITool("git_status", "查看 Git 状态", mapOf()),
        AITool("git_diff", "查看代码变更", mapOf()),
        AITool("git_log", "查看提交历史", mapOf("limit" to "限制数量")),
        AITool("git_commit", "提交代码变更", mapOf("message" to "提交信息")),
        
        // UI/UX 工具 (3个)
        AITool("suggest_improvements", "提出代码改进建议", mapOf("path" to "文件路径")),
        AITool("refactor", "重构代码", mapOf("path" to "文件路径", "strategy" to "重构策略")),
        AITool("generate_test", "为代码生成单元测试", mapOf("path" to "文件路径")),
        
        // 通用工具 (3个)
        AITool("web_search", "搜索网络信息", mapOf("query" to "搜索关键词")),
        AITool("calculate", "执行数学计算", mapOf("expression" to "表达式")),
        AITool("convert_code", "转换代码格式/语言", mapOf("code" to "代码", "from" to "源语言", "to" to "目标语言"))
    )
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        try {
            val prefs = Preferences.userNodeForPackage(AIViewModel::class.java)
            apiKey = prefs.get("api_key", "")
            apiEndpoint = prefs.get("api_endpoint", "https://api.openai.com/v1")
            mcpEndpoint = prefs.get("mcp_endpoint", "")
            modelName = prefs.get("model_name", "gpt-4")
            temperature = prefs.get("temperature", "0.7").toDouble()
            maxTokens = prefs.get("max_tokens", "4096").toInt()
            if (apiKey.isNotBlank()) {
                aiService = AIClient.create(apiKey, apiEndpoint)
            }
        } catch (_: Exception) {
            // Use defaults
        }
    }
    
    private fun saveSettings() {
        try {
            val prefs = Preferences.userNodeForPackage(AIViewModel::class.java)
            prefs.put("api_key", apiKey)
            prefs.put("api_endpoint", apiEndpoint)
            prefs.put("mcp_endpoint", mcpEndpoint)
            prefs.put("model_name", modelName)
            prefs.put("temperature", temperature.toString())
            prefs.put("max_tokens", maxTokens.toString())
            if (apiKey.isNotBlank()) {
                aiService = AIClient.create(apiKey, apiEndpoint)
            }
        } catch (_: Exception) {}
    }
    
    fun toggleAIChat() {
        _showAIChat.value = !_showAIChat.value
    }
    
    fun closeAIChat() {
        _showAIChat.value = false
    }
    
    fun openAIChat() {
        _showAIChat.value = true
    }
    
    /**
     * 发送用户消息并获取 AI 回复
     */
    fun sendMessage(content: String, currentFileContent: String?, currentFilePath: String?) {
        if (aiService == null || apiKey.isBlank()) {
            _chatState.value = ChatState.Error("请先在设置中配置 API Key")
            return
        }
        
        _chatState.value = ChatState.Loading
        
        // 添加用户消息
        _messages.add(AIMessage(MessageType.USER, content))
        
        viewModelScope.launch {
            try {
                val systemPrompt = buildSystemPrompt(currentFileContent, currentFilePath)
                val requestMessages = mutableListOf(
                    AIMessage(MessageType.SYSTEM, systemPrompt)
                )
                requestMessages.addAll(_messages)
                
                val request = ChatRequest(
                    messages = requestMessages,
                    model = modelName,
                    temperature = temperature
                )
                
                val response = aiService!!.chat(
                    endpoint = "chat/completions",
                    authorization = "Bearer $apiKey",
                    request = request
                )
                
                if (response.isSuccessful) {
                    val body = response.body()
                    body?.let {
                        _messages.add(it.message)
                        _chatState.value = ChatState.Success(_messages.toList())
                        
                        // 检查是否有代码修改请求
                        checkForCodeModifications(it.message.content, currentFileContent, currentFilePath)
                    }
                } else {
                    _chatState.value = ChatState.Error("请求失败: ${response.code()}")
                }
            } catch (e: Exception) {
                _chatState.value = ChatState.Error("出错: ${e.message}")
            }
        }
    }
    
    /**
     * 检查是否有代码修改请求
     */
    private fun checkForCodeModifications(aiContent: String, originalContent: String?, filePath: String?) {
        // 解析 AI 响应中的代码修改指令
        // 这里简化处理，实际应该解析更复杂的格式
        val codeBlockRegex = Regex("\\{\\{EDIT\\}\\}(.*?)\\{\\{\\/EDIT\\}\\}", RegexOption.DOT_MATCHES_ALL)
        val matches = codeBlockRegex.findAll(aiContent)
        
        matches.forEach { match ->
            val editCommand = match.groupValues[1]
            // 解析 editCommand 并生成待审查的修改
            val newContent = parseEditCommand(editCommand, originalContent ?: "")
            
            if (newContent != originalContent) {
                _pendingReview.value = PendingReview(
                    operations = listOf(CodeEditOperation(
                        originalCode = originalContent ?: "",
                        modifiedCode = newContent,
                        startLine = 0,
                        endLine = (originalContent ?: "").lineCount(),
                        description = "AI 代码修改",
                        toolName = "code_edit"
                    )),
                    originalFileContent = originalContent ?: "",
                    newFileContent = newContent,
                    messages = _messages.toList()
                )
            }
        }
    }
    
    private fun String.lineCount() = this.count { it == '\n' } + 1
    
    /**
     * 解析编辑命令
     */
    private fun parseEditCommand(editCommand: String, originalContent: String): String {
        // 简化实现：提取新代码块
        val codeBlockRegex = Regex("```[\\w]*\\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
        val match = codeBlockRegex.find(editCommand)
        return match?.groupValues?.get(1)?.trim() ?: originalContent
    }
    
    /**
     * 构建系统提示词
     */
    private fun buildSystemPrompt(fileContent: String?, filePath: String?): String {
        var prompt = """你是一个专业的编程助手，名叫 MedeMini AI。你的任务是帮助开发者编写、理解和调试代码。

你拥有以下能力：
1. 可以直接读取和修改代码文件
2. 可以执行终端命令
3. 可以运行测试和构建项目
4. 可以提供代码改进建议

当你需要修改代码时，请使用以下格式：
{{EDIT}}
<file_path>/path/to/file.kt</file_path>
<new_code>
// 新的代码内容
</new_code>
{{/EDIT}}

如果用户请求的操作需要执行工具，请说明需要使用的工具名称和参数。"""
        
        fileContent?.let {
            val truncated = if (it.length > 5000) it.take(5000) + "\n// ... (内容被截断)" else it
            prompt += "\n\n当前用户正在编辑的文件: ${filePath ?: "未知"}\n文件内容：\n```\n$truncated\n```"
        }
        
        return prompt
    }
    
    /**
     * 应用 AI 修改（用户审查后同意）
     */
    fun applyReview() {
        _pendingReview.value = null
        // 这里应该实际写入文件
    }
    
    /**
     * 拒绝 AI 修改（用户审查后不同意）
     */
    fun rejectReview() {
        _pendingReview.value = null
    }
    
    /**
     * 清除对话历史
     */
    fun clearChat() {
        _messages.clear()
        _chatState.value = ChatState.Idle
    }
    
    /**
     * 更新 API 设置
     */
    fun updateSettings(apiKey: String, apiEndpoint: String, mcpEndpoint: String, modelName: String, temperature: Double, maxTokens: Int) {
        this.apiKey = apiKey
        this.apiEndpoint = apiEndpoint
        this.mcpEndpoint = mcpEndpoint
        this.modelName = modelName
        this.temperature = temperature
        this.maxTokens = maxTokens
        saveSettings()
        if (apiKey.isNotBlank()) {
            aiService = AIClient.create(apiKey, apiEndpoint)
        }
    }
}