# MedeMini

一个轻量级的 Android 代码编辑器，内置强大的 AI 编程助手。

## 功能特性

### 核心功能
- 📱 轻量级代码编辑器，专为 Android 设计
- 📂 文件浏览器 - 浏览和管理项目文件
- 📑 多文件标签页 - 同时编辑多个文件
- 🎨 语法高亮 - 支持多种编程语言
- 🔍 搜索和替换 - 强大的文本搜索功能
- 📜 代码折叠 - 更清晰地查看代码结构
- 🕒 最近文件/项目 - 快速访问最近打开的文件和项目
- ⌨️ 命令面板 - 快速访问功能
- 🌗 黑白极简设计 - 舒适的阅读体验
- 🪟 毛玻璃效果 - 半透明浮动侧边栏和 AI 面板
- 💾 状态持久化 - 退出应用后自动恢复之前的编辑状态

### AI 编程助手 🤖
- 💬 **AI 对话窗口** - 右侧滑出式 AI 聊天面板，半透明设计
- 🧠 **编程智能体** - 内置详细的系统提示词，AI 了解自身环境和能力
- 🔧 **50+ AI 工具** - 文件操作、代码编辑、分析、构建测试、Git、通用工具等
- ⚙️ **AI 设置** - 配置 API 链接、密钥、模型名称、温度、最大 Token
- 🎯 **代码审查系统** - AI 修改代码后自动跳转到文件，顶部显示审查按钮
- 🟢 **差异高亮** - 新增代码半透明绿色背景，修改代码半透明橙色背景
- 📝 **智能代码修改** - AI 可直接读取和编辑当前文件内容
- 💬 **上下文记忆** - AI 记住之前的对话历史

### AI 工具列表
| 类别 | 工具数量 | 工具列表 |
|------|----------|----------|
| 文件操作 | 10 | list_files, search_files, read_file, write_file, create_file, delete_file, copy_file, move_file, get_file_info, create_directory |
| 代码编辑 | 4 | insert_line, delete_line, replace_line, replace_range |
| 代码分析 | 6 | find_references, analyze_code, get_project_structure, count_lines, check_syntax, analyze_complexity |
| 构建测试 | 4 | run_command, build_project, run_tests, check_dependencies |
| Git 操作 | 6 | git_status, git_diff, git_commit, git_push, git_pull, git_log |
| UI 操作 | 2 | show_toast, set_title |
| 通用工具 | 13 | calculate, format_json, format_xml, convert_case, trim_whitespace, generate_uuid, encode_base64, decode_base64, url_encode, url_decode, hash_md5, hash_sha256, validate_email, validate_url, parse_date, format_date, translate_code |

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose 1.5
- **架构**: MVVM + ViewModel
- **网络**: Retrofit 2.9 + OkHttp
- **异步**: Kotlin Coroutines
- **最低 API**: 26 (Android 8.0)

## 项目结构

```
app/src/main/java/com/medemini/
├── MainActivity.kt              # 主 Activity
├── AppStateManager.kt           # 状态持久化管理
├── ai/
│   ├── model/
│   │   └── AIMessage.kt         # AI 消息模型
│   ├── api/
│   │   └── AIService.kt         # AI API 接口
│   ├── viewmodel/
│   │   └── AIViewModel.kt       # AI ViewModel
│   └── ui/
│       ├── AIAssistant.kt       # AI 对话面板
│       └── AISettings.kt        # AI 设置窗口
├── editor/
│   ├── CodeEditor.kt            # 代码编辑器
│   └── SyntaxHighlight.kt       # 语法高亮
├── model/
│   └── EditorFile.kt            # 文件模型
└── ui/
    ├── CodeFolding.kt           # 代码折叠
    ├── CommandPalette.kt        # 命令面板
    ├── FileBrowser.kt           # 文件浏览器
    ├── RecentFiles.kt           # 最近文件
    ├── SearchReplace.kt         # 搜索替换
    ├── Sidebar.kt               # 侧边栏
    ├── StatusBar.kt             # 状态栏
    └── WelcomeScreen.kt         # 欢迎界面
```

## 构建说明

### Gradle 构建

```bash
# 构建调试版
./gradlew assembleDebug

# 构建发行版
./gradlew assembleRelease
```

### 使用 Android Studio
1. 使用 Android Studio 打开项目
2. 同步 Gradle 项目
3. 运行或构建应用

## AI 配置

### 配置自定义 API
1. 点击底部栏的 AI 按钮打开 AI 面板
2. 点击设置图标进入 AI 设置
3. 输入 API 地址（支持完整路径或基础路径）
4. 输入 API 密钥
5. 输入模型名称
6. 配置温度和最大 Token 数

### API URL 格式支持
- 完整路径: `https://api.example.com/v1/chat/completions`
- 基础路径: `https://api.example.com/v1`
- 带尾部斜杠: `https://api.example.com/v1/`

## 代码审查流程

1. 用户向 AI 提出代码修改请求
2. AI 分析代码并调用编辑工具
3. 系统自动跳转到被修改的文件
4. 编辑器顶部显示审查栏：
   - 变更数量
   - ❌ 拒绝按钮（红色）
   - ✅ 接受按钮（绿色）
5. AI 修改的代码行显示颜色标记：
   - 🟢 绿色 = 新增的代码行
   - 🟠 橙色 = 修改的代码行
6. 用户点击接受或拒绝

## 许可证

[MIT License](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！
