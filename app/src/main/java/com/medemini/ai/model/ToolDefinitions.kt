package com.medemini.ai.model

data class ToolParameterDefinition(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false
)

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<ToolParameterDefinition>
)

object ToolDefinitions {
    fun getAllTools(): List<ToolDefinition> = listOf(
        // 文件操作
        ToolDefinition(
            name = "list_files",
            description = "列出目录中的文件和子目录",
            parameters = listOf(
                ToolParameterDefinition("directory", "string", "目录路径", true)
            )
        ),
        ToolDefinition(
            name = "search_files",
            description = "在项目中搜索文件内容",
            parameters = listOf(
                ToolParameterDefinition("pattern", "string", "搜索模式", true),
                ToolParameterDefinition("directory", "string", "搜索目录", false)
            )
        ),
        ToolDefinition(
            name = "read_file",
            description = "读取文件内容",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "write_file",
            description = "写入文件内容（会覆盖原有内容）",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("content", "string", "文件内容", true)
            )
        ),
        ToolDefinition(
            name = "create_file",
            description = "创建新文件",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("content", "string", "文件内容", false)
            )
        ),
        ToolDefinition(
            name = "delete_file",
            description = "删除文件",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "copy_file",
            description = "复制文件",
            parameters = listOf(
                ToolParameterDefinition("source", "string", "源文件路径", true),
                ToolParameterDefinition("destination", "string", "目标文件路径", true)
            )
        ),
        ToolDefinition(
            name = "move_file",
            description = "移动文件",
            parameters = listOf(
                ToolParameterDefinition("source", "string", "源文件路径", true),
                ToolParameterDefinition("destination", "string", "目标文件路径", true)
            )
        ),
        ToolDefinition(
            name = "get_file_info",
            description = "获取文件详细信息",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "create_directory",
            description = "创建目录",
            parameters = listOf(
                ToolParameterDefinition("directory_path", "string", "目录路径", true)
            )
        ),
        // 代码编辑
        ToolDefinition(
            name = "insert_line",
            description = "在指定行号插入代码行",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("line_number", "string", "行号", true),
                ToolParameterDefinition("content", "string", "插入内容", true)
            )
        ),
        ToolDefinition(
            name = "delete_line",
            description = "删除指定行号的代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("line_number", "string", "行号", true),
                ToolParameterDefinition("count", "string", "删除行数", false)
            )
        ),
        ToolDefinition(
            name = "replace_line",
            description = "替换指定行号的代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("line_number", "string", "行号", true),
                ToolParameterDefinition("content", "string", "替换内容", true)
            )
        ),
        ToolDefinition(
            name = "replace_range",
            description = "替换指定行号范围的代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("start_line", "string", "起始行号", true),
                ToolParameterDefinition("end_line", "string", "结束行号", true),
                ToolParameterDefinition("content", "string", "替换内容", true)
            )
        ),
        // 代码分析
        ToolDefinition(
            name = "find_references",
            description = "查找符号引用（需要索引支持）",
            parameters = listOf(
                ToolParameterDefinition("symbol", "string", "符号名称", true),
                ToolParameterDefinition("file_path", "string", "文件路径", false)
            )
        ),
        ToolDefinition(
            name = "analyze_code",
            description = "分析代码结构",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "get_project_structure",
            description = "获取项目结构",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "count_lines",
            description = "统计代码行数",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "check_syntax",
            description = "检查语法错误",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "analyze_complexity",
            description = "分析代码复杂度",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        // 构建工具
        ToolDefinition(
            name = "run_command",
            description = "运行系统命令",
            parameters = listOf(
                ToolParameterDefinition("command", "string", "命令", true),
                ToolParameterDefinition("working_directory", "string", "工作目录", false)
            )
        ),
        ToolDefinition(
            name = "build_project",
            description = "构建项目",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "run_tests",
            description = "运行测试",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false),
                ToolParameterDefinition("test_class", "string", "测试类名", false)
            )
        ),
        ToolDefinition(
            name = "check_dependencies",
            description = "检查依赖",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        // Git 操作
        ToolDefinition(
            name = "git_status",
            description = "查看 Git 状态",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "git_diff",
            description = "查看 Git 差异",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", false)
            )
        ),
        ToolDefinition(
            name = "git_commit",
            description = "提交代码",
            parameters = listOf(
                ToolParameterDefinition("message", "string", "提交信息", true),
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "git_push",
            description = "推送代码",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "git_pull",
            description = "拉取代码",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "git_log",
            description = "查看提交日志",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false),
                ToolParameterDefinition("limit", "string", "显示条数", false)
            )
        ),
        // 通用工具
        ToolDefinition(
            name = "calculate",
            description = "数学计算",
            parameters = listOf(
                ToolParameterDefinition("expression", "string", "计算表达式", true)
            )
        ),
        ToolDefinition(
            name = "format_json",
            description = "格式化 JSON",
            parameters = listOf(
                ToolParameterDefinition("json_string", "string", "JSON 字符串", true)
            )
        ),
        ToolDefinition(
            name = "format_xml",
            description = "格式化 XML",
            parameters = listOf(
                ToolParameterDefinition("xml_string", "string", "XML 字符串", true)
            )
        ),
        ToolDefinition(
            name = "convert_case",
            description = "转换大小写",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true),
                ToolParameterDefinition("case", "string", "转换类型（upper/lower/camel/pascal）", true)
            )
        ),
        ToolDefinition(
            name = "trim_whitespace",
            description = "去除空白",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "generate_uuid",
            description = "生成 UUID",
            parameters = listOf()
        ),
        ToolDefinition(
            name = "encode_base64",
            description = "Base64 编码",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "decode_base64",
            description = "Base64 解码",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "url_encode",
            description = "URL 编码",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "url_decode",
            description = "URL 解码",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "hash_md5",
            description = "MD5 哈希",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "hash_sha256",
            description = "SHA256 哈希",
            parameters = listOf(
                ToolParameterDefinition("text", "string", "文本", true)
            )
        ),
        ToolDefinition(
            name = "validate_email",
            description = "验证邮箱",
            parameters = listOf(
                ToolParameterDefinition("email", "string", "邮箱地址", true)
            )
        ),
        ToolDefinition(
            name = "validate_url",
            description = "验证 URL",
            parameters = listOf(
                ToolParameterDefinition("url", "string", "URL 地址", true)
            )
        ),
        ToolDefinition(
            name = "parse_date",
            description = "解析日期",
            parameters = listOf(
                ToolParameterDefinition("date_string", "string", "日期字符串", true),
                ToolParameterDefinition("format", "string", "日期格式", false)
            )
        ),
        ToolDefinition(
            name = "format_date",
            description = "格式化日期",
            parameters = listOf(
                ToolParameterDefinition("timestamp", "string", "时间戳", true),
                ToolParameterDefinition("format", "string", "输出格式", false)
            )
        ),
        ToolDefinition(
            name = "translate_code",
            description = "代码翻译（转换为另一种语言）",
            parameters = listOf(
                ToolParameterDefinition("code", "string", "源代码", true),
                ToolParameterDefinition("from_language", "string", "源语言", true),
                ToolParameterDefinition("to_language", "string", "目标语言", true)
            )
        ),
        // 代码重构
        ToolDefinition(
            name = "find_definition",
            description = "查找符号定义",
            parameters = listOf(
                ToolParameterDefinition("symbol", "string", "符号名称", true),
                ToolParameterDefinition("file_path", "string", "文件路径", false)
            )
        ),
        ToolDefinition(
            name = "rename_symbol",
            description = "重命名符号",
            parameters = listOf(
                ToolParameterDefinition("old_name", "string", "旧名称", true),
                ToolParameterDefinition("new_name", "string", "新名称", true),
                ToolParameterDefinition("file_path", "string", "文件路径", false)
            )
        ),
        ToolDefinition(
            name = "add_import",
            description = "添加导入语句",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("import_statement", "string", "导入语句", true)
            )
        ),
        ToolDefinition(
            name = "remove_import",
            description = "移除导入语句",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("import_statement", "string", "导入语句", true)
            )
        ),
        ToolDefinition(
            name = "add_method",
            description = "添加方法",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("class_name", "string", "类名", true),
                ToolParameterDefinition("method_code", "string", "方法代码", true)
            )
        ),
        ToolDefinition(
            name = "add_field",
            description = "添加字段",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("class_name", "string", "类名", true),
                ToolParameterDefinition("field_code", "string", "字段代码", true)
            )
        ),
        ToolDefinition(
            name = "extract_method",
            description = "提取方法",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("start_line", "string", "起始行", true),
                ToolParameterDefinition("end_line", "string", "结束行", true),
                ToolParameterDefinition("method_name", "string", "方法名", true)
            )
        ),
        ToolDefinition(
            name = "format_code",
            description = "格式化代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "indent_code",
            description = "缩进代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("start_line", "string", "起始行", true),
                ToolParameterDefinition("end_line", "string", "结束行", true),
                ToolParameterDefinition("level", "string", "缩进级别", false)
            )
        ),
        ToolDefinition(
            name = "analyze_syntax",
            description = "分析语法",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "find_bugs",
            description = "查找 Bug",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "type_check",
            description = "类型检查",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "complexity_analysis",
            description = "复杂度分析",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "find_duplicate_code",
            description = "查找重复代码",
            parameters = listOf(
                ToolParameterDefinition("directory", "string", "目录路径", false)
            )
        ),
        ToolDefinition(
            name = "dependency_analysis",
            description = "依赖分析",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "code_metrics",
            description = "代码度量",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "find_deprecated",
            description = "查找弃用代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "find_todos",
            description = "查找 TODO/FIXME",
            parameters = listOf(
                ToolParameterDefinition("directory", "string", "目录路径", false)
            )
        ),
        ToolDefinition(
            name = "generate_comments",
            description = "生成注释",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("start_line", "string", "起始行", false),
                ToolParameterDefinition("end_line", "string", "结束行", false)
            )
        ),
        // 构建和测试
        ToolDefinition(
            name = "clean_project",
            description = "清理项目",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        ToolDefinition(
            name = "generate_documentation",
            description = "生成文档",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true)
            )
        ),
        ToolDefinition(
            name = "debug_code",
            description = "调试代码",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("line_number", "string", "行号", false)
            )
        ),
        ToolDefinition(
            name = "gradle_tasks",
            description = "列出 Gradle 任务",
            parameters = listOf(
                ToolParameterDefinition("project_path", "string", "项目路径", false)
            )
        ),
        // UI 改进
        ToolDefinition(
            name = "improve_ui",
            description = "改进 UI 建议",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("component_name", "string", "组件名称", false)
            )
        ),
        ToolDefinition(
            name = "refactor_component",
            description = "重构组件建议",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("component_name", "string", "组件名称", false)
            )
        ),
        ToolDefinition(
            name = "generate_tests",
            description = "生成测试",
            parameters = listOf(
                ToolParameterDefinition("file_path", "string", "文件路径", true),
                ToolParameterDefinition("test_type", "string", "测试类型", false)
            )
        ),
        // 其他
        ToolDefinition(
            name = "web_search",
            description = "网络搜索",
            parameters = listOf(
                ToolParameterDefinition("query", "string", "搜索关键词", true)
            )
        ),
        ToolDefinition(
            name = "convert_code",
            description = "代码转换",
            parameters = listOf(
                ToolParameterDefinition("code", "string", "源代码", true),
                ToolParameterDefinition("from_language", "string", "源语言", true),
                ToolParameterDefinition("to_language", "string", "目标语言", true)
            )
        )
    )
}