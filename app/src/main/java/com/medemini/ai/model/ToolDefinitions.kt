package com.medemini.ai.model

object ToolDefinitions {

    fun getAllTools(): List<ToolDefinition> {
        return buildList {
            addAll(fileTools())
            addAll(codeEditTools())
            addAll(codeAnalysisTools())
            addAll(buildTools())
            addAll(gitTools())
            addAll(uiTools())
            addAll(generalTools())
        }
    }

    private fun fileTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "read_file",
                description = "读取文件内容",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "write_file",
                description = "写入文件内容（覆盖整个文件）",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("content", "string", "文件内容", required = true)
                )
            ),
            ToolDefinition(
                name = "create_file",
                description = "创建新文件",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("content", "string", "文件内容", required = false)
                )
            ),
            ToolDefinition(
                name = "delete_file",
                description = "删除文件",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "list_files",
                description = "列出目录下的文件",
                parameters = listOf(
                    Parameter("directory", "string", "目录路径", required = false)
                )
            ),
            ToolDefinition(
                name = "search_files",
                description = "在文件中搜索文本",
                parameters = listOf(
                    Parameter("pattern", "string", "搜索模式", required = true),
                    Parameter("directory", "string", "搜索目录", required = false)
                )
            ),
            ToolDefinition(
                name = "copy_file",
                description = "复制文件",
                parameters = listOf(
                    Parameter("source", "string", "源文件路径", required = true),
                    Parameter("destination", "string", "目标文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "move_file",
                description = "移动文件",
                parameters = listOf(
                    Parameter("source", "string", "源文件路径", required = true),
                    Parameter("destination", "string", "目标文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "get_file_info",
                description = "获取文件信息",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "create_directory",
                description = "创建目录",
                parameters = listOf(
                    Parameter("directory_path", "string", "目录路径", required = true)
                )
            )
        )
    }

    private fun codeEditTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "insert_line",
                description = "在指定行插入代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("line_number", "integer", "行号", required = true),
                    Parameter("content", "string", "插入内容", required = true)
                )
            ),
            ToolDefinition(
                name = "delete_line",
                description = "删除指定行",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("line_number", "integer", "行号", required = true),
                    Parameter("count", "integer", "删除行数", required = false)
                )
            ),
            ToolDefinition(
                name = "replace_line",
                description = "替换指定行",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("line_number", "integer", "行号", required = true),
                    Parameter("content", "string", "新内容", required = true)
                )
            ),
            ToolDefinition(
                name = "replace_range",
                description = "替换指定范围的代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("start_line", "integer", "起始行", required = true),
                    Parameter("end_line", "integer", "结束行", required = true),
                    Parameter("content", "string", "新内容", required = true)
                )
            ),
            ToolDefinition(
                name = "find_references",
                description = "查找引用",
                parameters = listOf(
                    Parameter("symbol", "string", "符号名称", required = true),
                    Parameter("file_path", "string", "文件路径", required = false)
                )
            ),
            ToolDefinition(
                name = "find_definition",
                description = "查找定义",
                parameters = listOf(
                    Parameter("symbol", "string", "符号名称", required = true),
                    Parameter("file_path", "string", "文件路径", required = false)
                )
            ),
            ToolDefinition(
                name = "rename_symbol",
                description = "重命名符号",
                parameters = listOf(
                    Parameter("old_name", "string", "旧名称", required = true),
                    Parameter("new_name", "string", "新名称", required = true),
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "add_import",
                description = "添加导入语句",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("import_statement", "string", "导入语句", required = true)
                )
            ),
            ToolDefinition(
                name = "remove_import",
                description = "移除导入语句",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("import_statement", "string", "导入语句", required = true)
                )
            ),
            ToolDefinition(
                name = "add_method",
                description = "添加方法",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("class_name", "string", "类名", required = true),
                    Parameter("method_code", "string", "方法代码", required = true)
                )
            ),
            ToolDefinition(
                name = "add_field",
                description = "添加字段",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("class_name", "string", "类名", required = true),
                    Parameter("field_code", "string", "字段代码", required = true)
                )
            ),
            ToolDefinition(
                name = "extract_method",
                description = "提取方法",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("start_line", "integer", "起始行", required = true),
                    Parameter("end_line", "integer", "结束行", required = true),
                    Parameter("method_name", "string", "方法名", required = true)
                )
            ),
            ToolDefinition(
                name = "format_code",
                description = "格式化代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "indent_code",
                description = "缩进代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("start_line", "integer", "起始行", required = true),
                    Parameter("end_line", "integer", "结束行", required = true),
                    Parameter("level", "integer", "缩进级别", required = false)
                )
            )
        )
    }

    private fun codeAnalysisTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "analyze_syntax",
                description = "语法分析",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "find_bugs",
                description = "查找Bug",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "type_check",
                description = "类型检查",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "complexity_analysis",
                description = "复杂度分析",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "find_duplicate_code",
                description = "查找重复代码",
                parameters = listOf(
                    Parameter("directory", "string", "目录路径", required = false)
                )
            ),
            ToolDefinition(
                name = "dependency_analysis",
                description = "依赖分析",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "code_metrics",
                description = "代码度量",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "find_deprecated",
                description = "查找弃用代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "find_todos",
                description = "查找TODO注释",
                parameters = listOf(
                    Parameter("directory", "string", "目录路径", required = false)
                )
            ),
            ToolDefinition(
                name = "generate_comments",
                description = "生成代码注释",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("start_line", "integer", "起始行", required = false),
                    Parameter("end_line", "integer", "结束行", required = false)
                )
            )
        )
    }

    private fun buildTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "build_project",
                description = "构建项目",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            ),
            ToolDefinition(
                name = "run_tests",
                description = "运行测试",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false),
                    Parameter("test_class", "string", "测试类名", required = false)
                )
            ),
            ToolDefinition(
                name = "clean_project",
                description = "清理项目",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            ),
            ToolDefinition(
                name = "generate_documentation",
                description = "生成文档",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true)
                )
            ),
            ToolDefinition(
                name = "debug_code",
                description = "调试代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("line_number", "integer", "行号", required = false)
                )
            ),
            ToolDefinition(
                name = "run_command",
                description = "运行命令",
                parameters = listOf(
                    Parameter("command", "string", "命令", required = true),
                    Parameter("working_directory", "string", "工作目录", required = false)
                )
            ),
            ToolDefinition(
                name = "check_dependencies",
                description = "检查依赖",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            ),
            ToolDefinition(
                name = "gradle_tasks",
                description = "列出Gradle任务",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            )
        )
    }

    private fun gitTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "git_status",
                description = "查看Git状态",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            ),
            ToolDefinition(
                name = "git_diff",
                description = "查看Git变更",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = false)
                )
            ),
            ToolDefinition(
                name = "git_log",
                description = "查看Git历史",
                parameters = listOf(
                    Parameter("project_path", "string", "项目路径", required = false),
                    Parameter("limit", "integer", "限制数量", required = false)
                )
            ),
            ToolDefinition(
                name = "git_commit",
                description = "提交代码",
                parameters = listOf(
                    Parameter("message", "string", "提交信息", required = true),
                    Parameter("project_path", "string", "项目路径", required = false)
                )
            )
        )
    }

    private fun uiTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "improve_ui",
                description = "改进UI/UX",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("component_name", "string", "组件名称", required = false)
                )
            ),
            ToolDefinition(
                name = "refactor_component",
                description = "重构组件",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("component_name", "string", "组件名称", required = false)
                )
            ),
            ToolDefinition(
                name = "generate_tests",
                description = "生成测试代码",
                parameters = listOf(
                    Parameter("file_path", "string", "文件路径", required = true),
                    Parameter("test_type", "string", "测试类型", required = false)
                )
            )
        )
    }

    private fun generalTools(): List<ToolDefinition> {
        return listOf(
            ToolDefinition(
                name = "web_search",
                description = "网络搜索",
                parameters = listOf(
                    Parameter("query", "string", "搜索查询", required = true)
                )
            ),
            ToolDefinition(
                name = "calculate",
                description = "计算",
                parameters = listOf(
                    Parameter("expression", "string", "数学表达式", required = true)
                )
            ),
            ToolDefinition(
                name = "convert_code",
                description = "代码转换",
                parameters = listOf(
                    Parameter("code", "string", "原始代码", required = true),
                    Parameter("from_language", "string", "源语言", required = true),
                    Parameter("to_language", "string", "目标语言", required = true)
                )
            )
        )
    }
}

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: List<Parameter> = emptyList()
)

data class Parameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = false
)