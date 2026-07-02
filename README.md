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
- 🕒 最近文件 - 快速访问最近打开的文件
- ⌨️ 命令面板 - 快速访问功能
- 🌗 黑白极简设计 - 舒适的阅读体验
- 🪟 毛玻璃效果 - 半透明浮动侧边栏

### AI 编程助手 🤖
- 💬 **AI 对话窗口** - 右侧滑出式 AI 聊天面板
- 🔧 **50+ AI 工具** - 文件操作、代码编辑、分析、构建测试等
- ⚙️ **AI 设置** - 配置 API Key、MCP 服务、模型参数
- 🎯 **代码审查系统** - AI 修改代码后需要用户审查确认
- 🟢 **差异高亮** - 新增代码半透明绿色标记，删除代码半透明红色标记
- 📝 **智能代码修改** - AI 可直接读取和编辑当前文件内容

### AI 工具列表
| 类别 | 工具数量 | 工具列表 |
|------|----------|----------|
| 文件操作 | 10 | 读取、写入、创建、删除、列出、搜索、复制、移动、获取信息、创建目录 |
| 代码编辑 | 15 | 插入行、删除行、替换行、替换范围、查找引用、查找定义、重命名、添加导入/方法/字段、提取方法、格式化等 |
| 代码分析 | 10 | 语法分析、Bug 查找、类型检查、复杂度分析、重复代码、依赖分析、代码度量、弃用查找、TODO 查找、代码注释 |
| 构建测试 | 8 | 构建项目、运行测试、清理、生成文档、调试、运行命令、检查依赖、Gradle 任务 |
| Git 操作 | 4 | 查看状态、查看变更、查看历史、提交代码 |
| UI/UX | 3 | 改进建议、重构、生成测试 |
| 通用 | 3 | 网络搜索、计算、代码转换 |

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **架构**: MVVM + 状态管理 (StateFlow)
- **网络**: Retrofit + OkHttp
- **异步**: Kotlin Coroutines
- **最低 API**: 26 (Android 8.0)

## 项目结构

```
app/src/main/java/com/medemini/
├── MainActivity.kt              # 主 Activity
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
    └── StatusBar.kt             # 状态栏
```

## 构建说明

1. 使用 Android Studio 打开项目
2. 同步 Gradle 项目
3. 运行或构建应用

### Gradle 依赖

```kotlin
// AI 功能依赖
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
```

## AI 配置

### 配置 OpenAI API
1. 点击底部栏的 AI 按钮打开 AI 面板
2. 点击设置图标进入 AI 设置
3. 输入你的 OpenAI API Key
4. 选择模型（GPT-4, GPT-3.5 Turbo 等）
5. 配置温度和最大 Token 数

### 配置 MCP 服务 (可选)
MCP (Model Context Protocol) 允许 AI 访问本地工具和资源。

## 快捷键/手势

| 手势 | 功能 |
|------|------|
| 双击 | 快速菜单 |
| 长按 | 命令面板 |

## 代码审查流程

1. 用户向 AI 提出代码修改请求
2. AI 分析代码并生成修改建议
3. 系统显示待审查区域，用颜色标记差异：
   - 🟢 绿色 = 新增/修改的代码
   - 🔴 红色 = 删除的代码
4. 用户可以选择：
   - ✅ **接受** - 应用 AI 的修改
   - ❌ **拒绝** - 撤销 AI 的修改

## 许可证

[MIT License](LICENSE)

## 贡献

欢迎提交 Issue 和 Pull Request！