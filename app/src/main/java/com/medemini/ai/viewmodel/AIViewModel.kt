package com.medemini.ai.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import com.medemini.ai.api.*
import com.medemini.model.EditorFile
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.UUID

class AIViewModel : ViewModel() {

    val messages = mutableStateListOf<AIMessage>()
    val isLoading = mutableStateOf(false)
    val config = mutableStateOf(AIConfig())
    val pendingChanges = mutableStateListOf<CodeChange>()
    val pendingFileChanges = mutableStateListOf<FileChange>()
    val showReviewPanel = mutableStateOf(false)
    val reviewMode = mutableStateOf(false)
    val currentReviewFilePath = mutableStateOf("")

    private val DEFAULT_SYSTEM_PROMPT = """
你是一个运行在 MedeMini 移动代码编辑器中的编程智能体。

【你的环境】
- 你运行在 Android 平台上的 MedeMini 代码编辑器中
- 用户正在编辑一个项目，项目路径: {PROJECT_PATH}
- 所有文件操作都相对于项目路径进行

【你的能力】
你可以调用以下工具来操作文件和代码：

=== 文件操作 ===
- list_files: 列出目录中的文件和子目录
- search_files: 在项目中搜索文件
- read_file: 读取文件内容
- write_file: 写入文件内容（会覆盖原有内容）
- create_file: 创建新文件
- delete_file: 删除文件
- copy_file: 复制文件
- move_file: 移动文件
- get_file_info: 获取文件详细信息
- create_directory: 创建目录

=== 代码编辑 ===
- insert_line: 在指定行号插入代码行
- delete_line: 删除指定行号的代码
- replace_line: 替换指定行号的代码
- replace_range: 替换指定行号范围的代码

=== 代码分析 ===
- find_references: 查找符号引用（需要索引支持）
- analyze_code: 分析代码结构
- get_project_structure: 获取项目结构
- count_lines: 统计代码行数
- check_syntax: 检查语法错误
- analyze_complexity: 分析代码复杂度

=== 构建工具 ===
- run_command: 运行系统命令
- build_project: 构建项目
- run_tests: 运行测试
- check_dependencies: 检查依赖

=== Git 操作 ===
- git_status: 查看 Git 状态
- git_diff: 查看 Git 差异
- git_commit: 提交代码
- git_push: 推送代码
- git_pull: 拉取代码
- git_log: 查看提交日志

=== UI 操作 ===
- show_toast: 显示提示消息
- set_title: 设置标题

=== 通用工具 ===
- calculate: 数学计算
- format_json: 格式化 JSON
- format_xml: 格式化 XML
- convert_case: 转换大小写
- trim_whitespace: 去除空白
- generate_uuid: 生成 UUID
- encode_base64: Base64 编码
- decode_base64: Base64 解码
- url_encode: URL 编码
- url_decode: URL 解码
- hash_md5: MD5 哈希
- hash_sha256: SHA256 哈希
- validate_email: 验证邮箱
- validate_url: 验证 URL
- parse_date: 解析日期
- format_date: 格式化日期
- translate_code: 代码翻译

【工作流程】
1. 用户提出需求或问题
2. 你分析需求，决定是否需要调用工具
3. 如果需要操作文件，先使用 list_files 或 search_files 找到目标文件
4. 使用 read_file 读取文件内容
5. 使用代码编辑工具修改代码（insert_line/delete_line/replace_line/replace_range）
6. 用户会审查你的修改，决定接受或拒绝
7. 最后给出完整的总结

【注意事项】
- 所有路径参数都使用相对路径（相对于项目路径）
- 行号从 1 开始计数
- 修改代码时要谨慎，确保语法正确
- 如果文件不存在，先使用 create_file 创建
- 代码修改会进入待审查状态，用户确认后才会真正生效
- 请按照以下格式回答：
[思考]你的思考过程...[/思考]
[回答]你的最终回答...[/回答]
"""

    private lateinit var aiService: AIService
    private var currentEditorFile: EditorFile? = null
    private var projectPath: String = ""

    fun setCurrentFile(file: EditorFile?) {
        currentEditorFile = file
    }

    fun setProjectPath(path: String) {
        projectPath = path
    }

    fun initService() {
        val baseUrl = config.value.apiBaseUrl.trim()
        val formattedBaseUrl = if (!baseUrl.endsWith("/")) {
            baseUrl + "/"
        } else {
            baseUrl
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        aiService = retrofit.create(AIService::class.java)
    }

    fun sendMessage(userMessage: String) {
        if (config.value.apiKey.isEmpty()) {
            addSystemMessage("请先在设置中配置API Key")
            return
        }

        if (!::aiService.isInitialized) {
            initService()
        }

        if (messages.isEmpty()) {
            val prompt = DEFAULT_SYSTEM_PROMPT.replace("{PROJECT_PATH}", if (projectPath.isNotEmpty()) projectPath else "未设置")
            messages.add(AIMessage(
                id = UUID.randomUUID().toString(),
                role = "system",
                content = prompt
            ))
        }

        val userMsg = AIMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = userMessage
        )
        messages.add(userMsg)
        isLoading.value = true

        executeChatCompletion()
    }

    private fun executeChatCompletion() {
        val messagesRequest = messages.map { msg ->
            if (msg.toolCalls != null && msg.toolCalls.isNotEmpty()) {
                MessageRequest(
                    role = msg.role,
                    content = msg.content,
                    toolCalls = msg.toolCalls.map { tc ->
                        ToolCallRequest(
                            id = tc.id,
                            type = tc.type,
                            function = FunctionRequest(
                                name = tc.function.name,
                                arguments = tc.function.arguments.toString()
                            )
                        )
                    }
                )
            } else {
                MessageRequest(role = msg.role, content = msg.content)
            }
        }

        val tools = ToolDefinitions.getAllTools().map {
            ToolDefinitionRequest(
                type = "function",
                function = FunctionRequestDefinition(
                    name = it.name,
                    description = it.description,
                    parameters = FunctionParameters(
                        type = "object",
                        properties = it.parameters.associate { p ->
                            p.name to ParameterDefinition(p.type, p.description)
                        },
                        required = it.parameters.filter { p -> p.required }.map { p -> p.name }
                    )
                )
            )
        }

        val request = ChatCompletionRequest(
            model = config.value.model,
            messages = messagesRequest,
            tools = tools,
            temperature = config.value.temperature,
            maxTokens = config.value.maxTokens
        )

        val baseUrl = config.value.apiBaseUrl.trim()
        val fullUrl = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else if (baseUrl.endsWith("/")) {
            "${baseUrl}chat/completions"
        } else {
            "${baseUrl}/chat/completions"
        }
        
        val call = aiService.chatCompletion(
            fullUrl,
            "Bearer ${config.value.apiKey}",
            request
        )

        call.enqueue(object : retrofit2.Callback<ChatCompletionResponse> {
            override fun onResponse(
                call: Call<ChatCompletionResponse>,
                response: retrofit2.Response<ChatCompletionResponse>
            ) {
                isLoading.value = false
                if (response.isSuccessful) {
                    val result = response.body()
                    result?.choices?.firstOrNull()?.message?.let { message ->
                        handleAIResponse(message)
                    }
                } else {
                    addSystemMessage("API调用失败: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ChatCompletionResponse>, t: Throwable) {
                isLoading.value = false
                addSystemMessage("网络错误: ${t.message}")
            }
        })
    }

    private fun handleAIResponse(message: MessageResponse) {
        if (message.toolCalls != null && message.toolCalls.isNotEmpty()) {
            val toolCalls = message.toolCalls.map { tc ->
                ToolCall(
                    id = tc.id,
                    type = tc.type,
                    function = ToolFunction(
                        name = tc.function.name,
                        arguments = parseArguments(tc.function.arguments)
                    )
                )
            }

            val aiMsg = AIMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = "",
                toolCalls = toolCalls
            )
            messages.add(aiMsg)

            executeToolCalls(toolCalls)
        } else {
            val content = message.content ?: ""
            val (thinking, reply) = parseThinkingAndReply(content)
            val aiMsg = AIMessage(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = reply,
                thinking = thinking.takeIf { it.isNotEmpty() }
            )
            messages.add(aiMsg)
        }
    }

    private fun parseThinkingAndReply(content: String): Pair<String, String> {
        val thinkingStart = content.indexOf("[思考]")
        val thinkingEnd = content.indexOf("[/思考]")
        val replyStart = content.indexOf("[回答]")
        val replyEnd = content.indexOf("[/回答]")

        var thinking = ""
        var reply = content

        if (thinkingStart >= 0 && thinkingEnd > thinkingStart) {
            thinking = content.substring(thinkingStart + 4, thinkingEnd).trim()
        }

        if (replyStart >= 0 && replyEnd > replyStart) {
            reply = content.substring(replyStart + 4, replyEnd).trim()
        } else if (thinking.isNotEmpty()) {
            reply = content.replace("[思考]$thinking[/思考]", "").trim()
        }

        return Pair(thinking, reply)
    }

    private fun parseArguments(argsJson: String): Map<String, String> {
        return try {
            val gson = com.google.gson.Gson()
            val map = gson.fromJson(argsJson, Map::class.java) as Map<String, Any>
            map.mapValues { it.value.toString() }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun executeToolCalls(toolCalls: List<ToolCall>) {
        val results = mutableListOf<ToolResult>()

        toolCalls.forEach { toolCall ->
            val result = executeTool(toolCall.function.name, toolCall.function.arguments)
            val isSuccess = !result.startsWith("文件不存在") && !result.startsWith("目录不存在") && 
                            !result.startsWith("参数错误") && !result.startsWith("不是目录") &&
                            !result.startsWith("读取文件失败") && !result.startsWith("写入文件失败") &&
                            !result.startsWith("删除文件失败") && !result.startsWith("创建文件失败") &&
                            !result.startsWith("列出文件失败") && !result.startsWith("搜索文件失败") &&
                            !result.startsWith("创建目录失败")
            results.add(ToolResult(toolCall.id, toolCall.function.name, result, isSuccess))
        }

        results.forEach { result ->
            val toolMsg = AIMessage(
                id = UUID.randomUUID().toString(),
                role = "tool",
                content = if (result.success) result.content else "工具调用失败: ${result.content}",
                toolCallId = result.toolCallId
            )
            messages.add(toolMsg)
        }

        if (results.any { !it.success }) {
            addSystemMessage("部分工具调用失败")
        }

        executeChatCompletion()
    }

    private fun executeTool(name: String, arguments: Map<String, String>): String {
        return when (name) {
            "read_file" -> readFile(arguments["file_path"])
            "write_file" -> writeFile(arguments["file_path"], arguments["content"])
            "create_file" -> createFile(arguments["file_path"], arguments["content"])
            "delete_file" -> deleteFile(arguments["file_path"])
            "list_files" -> listFiles(arguments["directory"])
            "search_files" -> searchFiles(arguments["pattern"], arguments["directory"])
            "copy_file" -> copyFile(arguments["source"], arguments["destination"])
            "move_file" -> moveFile(arguments["source"], arguments["destination"])
            "get_file_info" -> getFileInfo(arguments["file_path"])
            "create_directory" -> createDirectory(arguments["directory_path"])
            "insert_line" -> insertLine(arguments["file_path"], arguments["line_number"], arguments["content"])
            "delete_line" -> deleteLine(arguments["file_path"], arguments["line_number"], arguments["count"])
            "replace_line" -> replaceLine(arguments["file_path"], arguments["line_number"], arguments["content"])
            "replace_range" -> replaceRange(arguments["file_path"], arguments["start_line"], arguments["end_line"], arguments["content"])
            "find_references" -> findReferences(arguments["symbol"], arguments["file_path"])
            "find_definition" -> findDefinition(arguments["symbol"], arguments["file_path"])
            "rename_symbol" -> renameSymbol(arguments["old_name"], arguments["new_name"], arguments["file_path"])
            "add_import" -> addImport(arguments["file_path"], arguments["import_statement"])
            "remove_import" -> removeImport(arguments["file_path"], arguments["import_statement"])
            "add_method" -> addMethod(arguments["file_path"], arguments["class_name"], arguments["method_code"])
            "add_field" -> addField(arguments["file_path"], arguments["class_name"], arguments["field_code"])
            "extract_method" -> extractMethod(arguments["file_path"], arguments["start_line"], arguments["end_line"], arguments["method_name"])
            "format_code" -> formatCode(arguments["file_path"])
            "indent_code" -> indentCode(arguments["file_path"], arguments["start_line"], arguments["end_line"], arguments["level"])
            "analyze_syntax" -> analyzeSyntax(arguments["file_path"])
            "find_bugs" -> findBugs(arguments["file_path"])
            "type_check" -> typeCheck(arguments["file_path"])
            "complexity_analysis" -> complexityAnalysis(arguments["file_path"])
            "find_duplicate_code" -> findDuplicateCode(arguments["directory"])
            "dependency_analysis" -> dependencyAnalysis(arguments["file_path"])
            "code_metrics" -> codeMetrics(arguments["file_path"])
            "find_deprecated" -> findDeprecated(arguments["file_path"])
            "find_todos" -> findTodos(arguments["directory"])
            "generate_comments" -> generateComments(arguments["file_path"], arguments["start_line"], arguments["end_line"])
            "build_project" -> buildProject(arguments["project_path"])
            "run_tests" -> runTests(arguments["project_path"], arguments["test_class"])
            "clean_project" -> cleanProject(arguments["project_path"])
            "generate_documentation" -> generateDocumentation(arguments["file_path"])
            "debug_code" -> debugCode(arguments["file_path"], arguments["line_number"])
            "run_command" -> runCommand(arguments["command"], arguments["working_directory"])
            "check_dependencies" -> checkDependencies(arguments["project_path"])
            "gradle_tasks" -> gradleTasks(arguments["project_path"])
            "git_status" -> gitStatus(arguments["project_path"])
            "git_diff" -> gitDiff(arguments["file_path"])
            "git_log" -> gitLog(arguments["project_path"], arguments["limit"])
            "git_commit" -> gitCommit(arguments["message"], arguments["project_path"])
            "improve_ui" -> improveUI(arguments["file_path"], arguments["component_name"])
            "refactor_component" -> refactorComponent(arguments["file_path"], arguments["component_name"])
            "generate_tests" -> generateTests(arguments["file_path"], arguments["test_type"])
            "web_search" -> webSearch(arguments["query"])
            "calculate" -> calculate(arguments["expression"])
            "convert_code" -> convertCode(arguments["code"], arguments["from_language"], arguments["to_language"])
            else -> "未知工具: $name"
        }
    }

    private fun readFile(filePath: String?): String {
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            if (!file.exists()) {
                return "文件不存在: $resolvedPath"
            }
            file.readText()
        } catch (e: Exception) {
            "读取文件失败: ${e.message}"
        }
    }

    private fun writeFile(filePath: String?, content: String?): String {
        if (content == null) return "参数错误"
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(resolvedPath).writeText(content)
            currentEditorFile?.let {
                if (it.path == filePath || it.path == resolvedPath) {
                    it.content = content
                }
            }
            "文件写入成功"
        } catch (e: Exception) {
            "写入文件失败: ${e.message}"
        }
    }

    private fun createFile(filePath: String?, content: String?): String {
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(resolvedPath).writeText(content ?: "")
            "文件创建成功"
        } catch (e: Exception) {
            "创建文件失败: ${e.message}"
        }
    }

    private fun deleteFile(filePath: String?): String {
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(resolvedPath).delete()
            "文件删除成功"
        } catch (e: Exception) {
            "删除文件失败: ${e.message}"
        }
    }

    private fun listFiles(directory: String?): String {
        val resolvedPath = resolvePath(directory)
        if (resolvedPath.isNullOrEmpty()) {
            return "未设置项目路径，请先打开一个项目"
        }
        val dir = File(resolvedPath)
        return try {
            if (!dir.exists()) {
                return "目录不存在: $resolvedPath"
            }
            if (!dir.isDirectory) {
                return "不是目录: $resolvedPath"
            }
            val files = dir.listFiles()
            if (files == null || files.isEmpty()) {
                "目录为空: $resolvedPath"
            } else {
                files.sortedBy { it.name.lowercase() }.joinToString("\n") {
                    val type = if (it.isDirectory) "[DIR]" else "[FILE]"
                    "$type ${it.name}"
                }
            }
        } catch (e: Exception) {
            "列出文件失败: ${e.message}"
        }
    }

    private fun searchFiles(pattern: String?, directory: String?): String {
        if (pattern == null) return "参数错误"
        val resolvedPath = resolvePath(directory)
        if (resolvedPath.isNullOrEmpty()) {
            return "未设置项目路径，请先打开一个项目"
        }
        val dir = File(resolvedPath)
        return try {
            dir.walk()
                .filter { it.isFile && it.readText().contains(pattern) }
                .joinToString("\n") { it.path }
        } catch (e: Exception) {
            "搜索失败: ${e.message}"
        }
    }

    private fun copyFile(source: String?, destination: String?): String {
        if (source == null || destination == null) return "参数错误"
        val srcPath = resolvePath(source)
        val destPath = resolvePath(destination)
        if (srcPath == null || destPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(srcPath).copyTo(File(destPath), overwrite = true)
            "文件复制成功"
        } catch (e: Exception) {
            "复制文件失败: ${e.message}"
        }
    }

    private fun moveFile(source: String?, destination: String?): String {
        if (source == null || destination == null) return "参数错误"
        val srcPath = resolvePath(source)
        val destPath = resolvePath(destination)
        if (srcPath == null || destPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(srcPath).renameTo(File(destPath))
            "文件移动成功"
        } catch (e: Exception) {
            "移动文件失败: ${e.message}"
        }
    }

    private fun getFileInfo(filePath: String?): String {
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            "名称: ${file.name}\n路径: ${file.path}\n大小: ${file.length()} 字节\n修改时间: ${file.lastModified()}"
        } catch (e: Exception) {
            "获取文件信息失败: ${e.message}"
        }
    }

    private fun createDirectory(directoryPath: String?): String {
        val resolvedPath = resolvePath(directoryPath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            File(resolvedPath).mkdirs()
            "目录创建成功"
        } catch (e: Exception) {
            "创建目录失败: ${e.message}"
        }
    }

    private fun insertLine(filePath: String?, lineNumber: String?, content: String?): String {
        if (lineNumber == null || content == null) return "参数错误"
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            val originalContent = file.readText()
            val lines = file.readLines().toMutableList()
            val line = parseLineNumber(lineNumber, lines.size) ?: return "无效行号"
            val change = CodeChange(ChangeType.INSERT, line, "", content, 0, 0)
            lines.add(line, content)
            val newContent = lines.joinToString("\n")
            pendingFileChanges.add(FileChange(resolvedPath, originalContent, newContent, listOf(change)))
            pendingChanges.add(change)
            reviewMode.value = true
            currentReviewFilePath.value = resolvedPath
            "代码插入成功（待审查）"
        } catch (e: Exception) {
            "插入代码失败: ${e.message}"
        }
    }

    private fun deleteLine(filePath: String?, lineNumber: String?, count: String?): String {
        if (lineNumber == null) return "参数错误"
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            val originalContent = file.readText()
            val lines = file.readLines().toMutableList()
            val line = parseLineNumber(lineNumber, lines.size - 1) ?: return "无效行号"
            val deleteCount = count?.toIntOrNull() ?: 1
            val startIdx = line
            val endIdx = (startIdx + deleteCount).coerceAtMost(lines.size)
            val deletedLines = lines.subList(startIdx, endIdx)
            val lineChanges = mutableListOf<CodeChange>()
            deletedLines.forEachIndexed { i, oldContent ->
                lineChanges.add(CodeChange(ChangeType.DELETE, startIdx + i, oldContent, "", 0, 0))
            }
            lines.subList(startIdx, endIdx).clear()
            val newContent = lines.joinToString("\n")
            pendingFileChanges.add(FileChange(resolvedPath, originalContent, newContent, lineChanges))
            pendingChanges.addAll(lineChanges)
            reviewMode.value = true
            currentReviewFilePath.value = resolvedPath
            "删除成功（待审查）"
        } catch (e: Exception) {
            "删除代码失败: ${e.message}"
        }
    }

    private fun replaceLine(filePath: String?, lineNumber: String?, content: String?): String {
        if (lineNumber == null || content == null) return "参数错误"
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            val originalContent = file.readText()
            val lines = file.readLines().toMutableList()
            val line = parseLineNumber(lineNumber, lines.size - 1) ?: return "无效行号"
            val oldContent = lines[line]
            val change = CodeChange(ChangeType.MODIFY, line, oldContent, content, 0, 0)
            lines[line] = content
            val newContent = lines.joinToString("\n")
            pendingFileChanges.add(FileChange(resolvedPath, originalContent, newContent, listOf(change)))
            pendingChanges.add(change)
            reviewMode.value = true
            currentReviewFilePath.value = resolvedPath
            "替换成功（待审查）"
        } catch (e: Exception) {
            "替换代码失败: ${e.message}"
        }
    }

    private fun replaceRange(filePath: String?, startLine: String?, endLine: String?, content: String?): String {
        if (startLine == null || endLine == null || content == null) return "参数错误"
        val resolvedPath = resolvePath(filePath)
        if (resolvedPath == null) return "未设置项目路径，请先打开一个项目"
        return try {
            val file = File(resolvedPath)
            val originalContent = file.readText()
            val lines = file.readLines().toMutableList()
            val start = parseLineNumber(startLine, lines.size - 1) ?: return "无效起始行号"
            val end = parseLineNumber(endLine, lines.size - 1) ?: return "无效结束行号"
            val effectiveEnd = end.coerceAtLeast(start)
            val oldContent = lines.subList(start, effectiveEnd + 1).joinToString("\n")
            val change = CodeChange(ChangeType.MODIFY, start, oldContent, content, 0, 0)
            lines.subList(start, effectiveEnd + 1).clear()
            content.lines().forEachIndexed { i, line ->
                lines.add(start + i, line)
            }
            val newContent = lines.joinToString("\n")
            pendingFileChanges.add(FileChange(resolvedPath, originalContent, newContent, listOf(change)))
            pendingChanges.add(change)
            reviewMode.value = true
            currentReviewFilePath.value = resolvedPath
            "范围替换成功（待审查）"
        } catch (e: Exception) {
            "范围替换失败: ${e.message}"
        }
    }

    private fun findReferences(symbol: String?, filePath: String?): String {
        if (symbol == null) return "参数错误"
        return "搜索引用功能需要索引支持"
    }

    private fun findDefinition(symbol: String?, filePath: String?): String {
        if (symbol == null) return "参数错误"
        return "查找定义功能需要索引支持"
    }

    private fun renameSymbol(oldName: String?, newName: String?, filePath: String?): String {
        if (oldName == null || newName == null || filePath == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val newContent = content.replace(oldName, newName)
            File(filePath).writeText(newContent)
            "重命名成功"
        } catch (e: Exception) {
            "重命名失败: ${e.message}"
        }
    }

    private fun addImport(filePath: String?, importStatement: String?): String {
        if (filePath == null || importStatement == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val newContent = if (content.startsWith("package ")) {
                val packageEnd = content.indexOf('\n') + 1
                content.substring(0, packageEnd) + "\n$importStatement\n" + content.substring(packageEnd)
            } else {
                "$importStatement\n$content"
            }
            File(filePath).writeText(newContent)
            "导入添加成功"
        } catch (e: Exception) {
            "添加导入失败: ${e.message}"
        }
    }

    private fun removeImport(filePath: String?, importStatement: String?): String {
        if (filePath == null || importStatement == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val newContent = content.replace("$importStatement\n", "")
            File(filePath).writeText(newContent)
            "导入移除成功"
        } catch (e: Exception) {
            "移除导入失败: ${e.message}"
        }
    }

    private fun addMethod(filePath: String?, className: String?, methodCode: String?): String {
        if (filePath == null || className == null || methodCode == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val classPattern = Regex("class\\s+$className\\s*\\{")
            val match = classPattern.find(content)
            if (match != null) {
                val braceIndex = content.indexOf('{', match.range.last) + 1
                val newContent = content.substring(0, braceIndex) + "\n    $methodCode\n" + content.substring(braceIndex)
                File(filePath).writeText(newContent)
                "方法添加成功"
            } else {
                "未找到类: $className"
            }
        } catch (e: Exception) {
            "添加方法失败: ${e.message}"
        }
    }

    private fun addField(filePath: String?, className: String?, fieldCode: String?): String {
        if (filePath == null || className == null || fieldCode == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val classPattern = Regex("class\\s+$className\\s*\\{")
            val match = classPattern.find(content)
            if (match != null) {
                val braceIndex = content.indexOf('{', match.range.last) + 1
                val newContent = content.substring(0, braceIndex) + "\n    $fieldCode\n" + content.substring(braceIndex)
                File(filePath).writeText(newContent)
                "字段添加成功"
            } else {
                "未找到类: $className"
            }
        } catch (e: Exception) {
            "添加字段失败: ${e.message}"
        }
    }

    private fun extractMethod(filePath: String?, startLine: String?, endLine: String?, methodName: String?): String {
        if (filePath == null || startLine == null || endLine == null || methodName == null) return "参数错误"
        return "提取方法功能需要更复杂的代码分析"
    }

    private fun formatCode(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "代码格式化功能需要集成格式化工具"
    }

    private fun indentCode(filePath: String?, startLine: String?, endLine: String?, level: String?): String {
        if (filePath == null || startLine == null || endLine == null) return "参数错误"
        return try {
            val lines = File(filePath).readLines().toMutableList()
            val start = startLine.toIntOrNull() ?: return "无效起始行号"
            val end = endLine.toIntOrNull() ?: return "无效结束行号"
            val indentLevel = level?.toIntOrNull() ?: 1
            for (i in start..end) {
                if (i < lines.size) {
                    lines[i] = "    ".repeat(indentLevel) + lines[i]
                }
            }
            File(filePath).writeText(lines.joinToString("\n"))
            "缩进成功"
        } catch (e: Exception) {
            "缩进失败: ${e.message}"
        }
    }

    private fun analyzeSyntax(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "语法分析功能需要集成语言解析器"
    }

    private fun findBugs(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "Bug查找功能需要集成静态分析工具"
    }

    private fun typeCheck(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "类型检查功能需要集成编译器"
    }

    private fun complexityAnalysis(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "复杂度分析功能需要静态分析支持"
    }

    private fun findDuplicateCode(directory: String?): String {
        val dir = File(directory ?: ".")
        return try {
            "重复代码分析功能需要集成专门工具"
        } catch (e: Exception) {
            "分析失败: ${e.message}"
        }
    }

    private fun dependencyAnalysis(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "依赖分析功能需要项目配置解析"
    }

    private fun codeMetrics(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return try {
            val content = File(filePath).readText()
            val lines = content.lines()
            val codeLines = lines.count { it.trim().isNotEmpty() && !it.trim().startsWith("//") }
            val comments = lines.count { it.trim().startsWith("//") }
            "总行数: ${lines.size}\n代码行数: $codeLines\n注释行数: $comments"
        } catch (e: Exception) {
            "计算度量失败: ${e.message}"
        }
    }

    private fun findDeprecated(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "弃用代码查找需要语言支持"
    }

    private fun findTodos(directory: String?): String {
        val dir = File(directory ?: ".")
        return try {
            dir.walk()
                .filter { it.isFile }
                .flatMap { file ->
                    file.readLines().mapIndexed { idx, line ->
                        if (line.contains("TODO") || line.contains("FIXME")) {
                            "${file.name}:${idx + 1}: $line"
                        } else null
                    }
                }
                .filterNotNull()
                .joinToString("\n")
        } catch (e: Exception) {
            "查找失败: ${e.message}"
        }
    }

    private fun generateComments(filePath: String?, startLine: String?, endLine: String?): String {
        if (filePath == null) return "参数错误"
        return "注释生成功能需要AI支持"
    }

    private fun buildProject(projectPath: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val result = ProcessBuilder("gradle", "build")
                .directory(dir)
                .start()
                .waitFor()
            if (result == 0) "构建成功" else "构建失败"
        } catch (e: Exception) {
            "构建失败: ${e.message}"
        }
    }

    private fun runTests(projectPath: String?, testClass: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val command = if (testClass != null) {
                listOf("gradle", "test", "--tests", testClass)
            } else {
                listOf("gradle", "test")
            }
            val result = ProcessBuilder(command)
                .directory(dir)
                .start()
                .waitFor()
            if (result == 0) "测试通过" else "测试失败"
        } catch (e: Exception) {
            "运行测试失败: ${e.message}"
        }
    }

    private fun cleanProject(projectPath: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val result = ProcessBuilder("gradle", "clean")
                .directory(dir)
                .start()
                .waitFor()
            if (result == 0) "清理成功" else "清理失败"
        } catch (e: Exception) {
            "清理失败: ${e.message}"
        }
    }

    private fun generateDocumentation(filePath: String?): String {
        if (filePath == null) return "参数错误"
        return "文档生成功能需要AI支持"
    }

    private fun debugCode(filePath: String?, lineNumber: String?): String {
        if (filePath == null) return "参数错误"
        return "调试功能需要集成调试器"
    }

    private fun runCommand(command: String?, workingDirectory: String?): String {
        if (command == null) return "参数错误"
        return try {
            val dir = File(workingDirectory ?: ".")
            val parts = command.split(" ")
            val process = ProcessBuilder(parts)
                .directory(dir)
                .start()
            process.waitFor()
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            "$output\n$error"
        } catch (e: Exception) {
            "命令执行失败: ${e.message}"
        }
    }

    private fun checkDependencies(projectPath: String?): String {
        return "依赖检查功能需要项目配置解析"
    }

    private fun gradleTasks(projectPath: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val process = ProcessBuilder("gradle", "tasks")
                .directory(dir)
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "获取任务失败: ${e.message}"
        }
    }

    private fun gitStatus(projectPath: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val process = ProcessBuilder("git", "status")
                .directory(dir)
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Git状态获取失败: ${e.message}"
        }
    }

    private fun gitDiff(filePath: String?): String {
        return try {
            val dir = File(".")
            val process = if (filePath != null) {
                ProcessBuilder("git", "diff", filePath).directory(dir).start()
            } else {
                ProcessBuilder("git", "diff").directory(dir).start()
            }
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Git差异获取失败: ${e.message}"
        }
    }

    private fun gitLog(projectPath: String?, limit: String?): String {
        return try {
            val dir = File(projectPath ?: ".")
            val command = if (limit != null) {
                listOf("git", "log", "--oneline", "-n", limit)
            } else {
                listOf("git", "log", "--oneline", "-n", "10")
            }
            val process = ProcessBuilder(command)
                .directory(dir)
                .start()
            process.waitFor()
            process.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            "Git历史获取失败: ${e.message}"
        }
    }

    private fun gitCommit(message: String?, projectPath: String?): String {
        if (message == null) return "参数错误"
        return try {
            val dir = File(projectPath ?: ".")
            ProcessBuilder("git", "add", ".").directory(dir).start().waitFor()
            val process = ProcessBuilder("git", "commit", "-m", message)
                .directory(dir)
                .start()
            val result = process.waitFor()
            if (result == 0) "提交成功" else "提交失败"
        } catch (e: Exception) {
            "提交失败: ${e.message}"
        }
    }

    private fun improveUI(filePath: String?, componentName: String?): String {
        if (filePath == null) return "参数错误"
        return "UI改进建议需要AI分析"
    }

    private fun refactorComponent(filePath: String?, componentName: String?): String {
        if (filePath == null) return "参数错误"
        return "重构建议需要AI分析"
    }

    private fun generateTests(filePath: String?, testType: String?): String {
        if (filePath == null) return "参数错误"
        return "测试生成需要AI支持"
    }

    private fun webSearch(query: String?): String {
        if (query == null) return "参数错误"
        return "网络搜索功能需要联网支持"
    }

    private fun calculate(expression: String?): String {
        if (expression == null) return "参数错误"
        return try {
            val result = evaluateExpression(expression)
            "结果: $result"
        } catch (e: Exception) {
            "计算失败: ${e.message}"
        }
    }

    private fun evaluateExpression(expr: String): Double {
        val tokens = expr.replace(" ", "").toList()
        var result = 0.0
        var current = 0.0
        var op = '+'
        var i = 0
        while (i < tokens.size) {
            if (tokens[i].isDigit() || tokens[i] == '.') {
                var numStr = ""
                while (i < tokens.size && (tokens[i].isDigit() || tokens[i] == '.')) {
                    numStr += tokens[i]
                    i++
                }
                current = numStr.toDouble()
            } else {
                result = applyOp(result, current, op)
                op = tokens[i]
                i++
            }
        }
        return applyOp(result, current, op)
    }

    private fun applyOp(a: Double, b: Double, op: Char): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> a / b
            else -> b
        }
    }

    private fun convertCode(code: String?, fromLanguage: String?, toLanguage: String?): String {
        if (code == null || fromLanguage == null || toLanguage == null) return "参数错误"
        return "代码转换需要AI支持"
    }

    private fun resolvePath(path: String?): String? {
        if (path.isNullOrEmpty()) {
            return projectPath.takeIf { it.isNotEmpty() }
        }
        return if (path.startsWith("/")) {
            path
        } else {
            if (projectPath.isEmpty()) {
                null
            } else {
                File(projectPath, path).absolutePath
            }
        }
    }

    private fun parseLineNumber(lineStr: String?, maxLine: Int): Int? {
        if (lineStr.isNullOrEmpty()) return null
        val cleaned = lineStr.replace(Regex("[^0-9]"), "").trim()
        if (cleaned.isEmpty()) return null
        val line = cleaned.toIntOrNull() ?: return null
        val adjustedLine = if (line > 0) line - 1 else line
        return adjustedLine.coerceIn(0, maxLine)
    }

    private fun addSystemMessage(content: String) {
        messages.add(AIMessage(
            id = UUID.randomUUID().toString(),
            role = "system",
            content = content
        ))
    }

    fun acceptChanges() {
        pendingFileChanges.forEach { fileChange ->
            try {
                File(fileChange.filePath).writeText(fileChange.newContent)
                currentEditorFile?.let { editorFile ->
                    if (editorFile.path == fileChange.filePath) {
                        editorFile.content = fileChange.newContent
                    }
                }
            } catch (_: Exception) {}
        }
        pendingFileChanges.clear()
        pendingChanges.clear()
        showReviewPanel.value = false
        reviewMode.value = false
        currentReviewFilePath.value = ""
    }

    fun rejectChanges() {
        pendingFileChanges.clear()
        pendingChanges.clear()
        showReviewPanel.value = false
        reviewMode.value = false
        currentReviewFilePath.value = ""
    }

    fun clearMessages() {
        messages.clear()
    }

    fun getReviewContent(filePath: String): String {
        val fileChange = pendingFileChanges.find { it.filePath == filePath }
        return fileChange?.newContent ?: ""
    }

    fun getOriginalContent(filePath: String): String {
        val fileChange = pendingFileChanges.find { it.filePath == filePath }
        return fileChange?.originalContent ?: ""
    }

    fun getDiffAnnotatedString(filePath: String): AnnotatedString {
        val fileChange = pendingFileChanges.find { it.filePath == filePath } ?: return AnnotatedString("")
        
        val newLines = fileChange.newContent.lines()
        val changes = fileChange.lineChanges
        
        return buildAnnotatedString {
            val insertColor = SpanStyle(background = Color(0xFF4CAF50).copy(alpha = 0.25f))
            val deleteColor = SpanStyle(background = Color(0xFFF44336).copy(alpha = 0.25f))
            val modifyColor = SpanStyle(background = Color(0xFFFF9800).copy(alpha = 0.25f))
            
            val changeMap = mutableMapOf<Int, CodeChange>()
            changes.forEach { change ->
                when (change.type) {
                    ChangeType.INSERT -> changeMap[change.lineNumber] = change
                    ChangeType.DELETE -> {}
                    ChangeType.MODIFY -> changeMap[change.lineNumber] = change
                }
            }
            
            newLines.forEachIndexed { index, line ->
                val change = changeMap[index]
                if (change != null) {
                    when (change.type) {
                        ChangeType.INSERT -> withStyle(insertColor) { append(line + "\n") }
                        ChangeType.MODIFY -> withStyle(modifyColor) { append(line + "\n") }
                        else -> append(line + "\n")
                    }
                } else {
                    append(line + "\n")
                }
            }
        }
    }
}