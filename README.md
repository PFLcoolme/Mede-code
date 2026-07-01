# Medecode - Android 代码编辑器

一个支持多语言语法高亮的 Android 代码编辑器应用。

## 功能特性

- 📝 **代码编辑** - 支持代码编辑的基本功能
- 🎨 **语法高亮** - 支持多种编程语言的语法高亮
- 🌙 **深色/浅色模式** - 跟随系统主题自动切换
- 📂 **文件浏览器** - 浏览设备文件系统，选择文件打开
- 📁 **侧边栏文件树** - VSCode 风格的项目文件树面板
- 🔍 **搜索和替换** - 查找和替换文本，支持区分大小写
- 📊 **底部状态栏** - 显示语言、编码、光标位置、行数
- ⌨️ **智能输入** - 自动缩进、括号自动补全
- 📑 **多标签页** - 同时编辑多个文件
- 🎹 **命令面板** - VSCode 风格的快速命令面板
- 🕐 **最近打开文件** - 快速访问最近编辑的文件
- 📂 **代码折叠** - 基于花括号的代码折叠

## 快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl+P` | 打开命令面板 |
| `Ctrl+F` | 搜索和替换 |
| `Ctrl+S` | 保存文件 |
| `Ctrl+B` | 切换侧边栏 |

## 支持的语言

- Python
- JavaScript / TypeScript
- Java / Kotlin
- C / C++
- Go
- Rust
- Ruby
- PHP
- Swift
- Dart
- Shell / Bash
- HTML / CSS
- SQL
- 更多...

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose
- **设计**: Material Design 3

## 项目结构

```
Medecode/
├── app/
│   ├── src/main/
│   │   ├── java/com/medecode/
│   │   │   ├── MainActivity.kt          # 主活动
│   │   │   ├── editor/
│   │   │   │   ├── CodeEditor.kt        # 代码编辑器组件
│   │   │   │   └── SyntaxHighlight.kt   # 语法高亮逻辑
│   │   │   ├── model/
│   │   │   │   └── EditorFile.kt        # 数据模型
│   │   │   └── ui/
│   │   │       ├── FileBrowser.kt       # 文件浏览器
│   │   │       ├── Sidebar.kt           # 侧边栏文件树
│   │   │       ├── SearchReplace.kt     # 搜索和替换
│   │   │       ├── StatusBar.kt         # 底部状态栏
│   │   │       ├── CommandPalette.kt    # 命令面板
│   │   │       ├── CodeFolding.kt       # 代码折叠
│   │   │       ├── RecentFiles.kt       # 最近文件
│   │   │       └── theme/
│   │   │           ├── Theme.kt         # 应用主题
│   │   │           └── Type.kt          # 字体排版
│   │   ├── res/                         # 资源文件
│   │   └── AndroidManifest.xml          # 应用清单
│   └── build.gradle.kts                 # 应用级构建配置
├── build.gradle.kts                     # 项目级构建配置
├── settings.gradle.kts                  # 项目设置
└── gradle.properties                    # Gradle 配置
```

## 构建和运行

### 前提条件

- Android Studio Hedgehog 或更高版本
- Android SDK 34
- JDK 17 或更高版本

### 构建步骤

1. 打开 Android Studio
2. 选择 `File -> Open`，选择本项目目录
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击运行按钮（Shift + F10）

### 命令行构建

```bash
# 构建调试版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 构建发布版本
./gradlew assembleRelease
```

## 开发计划

- [x] 基本代码编辑器
- [x] 语法高亮
- [x] 行号显示
- [x] 深色/浅色主题
- [x] 文件打开/保存
- [x] 多标签页支持
- [x] 文件浏览器
- [x] 侧边栏文件树
- [x] 搜索和替换
- [x] 底部状态栏
- [x] 命令面板
- [x] 代码折叠
- [x] 最近打开文件
- [ ] 代码执行功能
- [ ] 终端/控制台
- [ ] 版本控制集成
- [ ] 代码自动补全

## 与 Mede-IDE 的区别

| 功能 | Medecode (VSCode) | Mede-IDE (IDEA) |
|------|-------------------|-----------------|
| 定位 | 轻量级代码编辑器 | 完整 IDE |
| AI 集成 | ❌ | ✅ 本地+云端 |
| 终端 | ❌ | ✅ 内置 Termux |
| Git | ❌ | ✅ 完整集成 |
| 文件树 | ✅ 侧边栏 | ✅ 三栏布局 |
| 搜索替换 | ✅ | ✅ |
| 状态栏 | ✅ | ✅ |
| 命令面板 | ✅ | ❌ |
| 最近文件 | ✅ | ❌ |
| 语法高亮 | ✅ | ✅ |
| 多标签 | ✅ | ✅ |
| 文件大小 | 轻量 | 较重 |

## 相关项目

### [Mede-IDE](https://github.com/Evilgodxu/Mede-IDE)

[Mede-IDE](https://github.com/Evilgodxu/Mede-IDE) 是一款功能完整的 Android 平台 AI 辅助代码编辑器，采用 Jetpack Compose + Material 3 构建，集成本地大模型推理与云端 LLM API，具备完整的 IDE 基础功能。

- **GitHub**: https://github.com/Evilgodxu/Mede-IDE
- **定位**: 完整 IDE（类似 IDEA）
- **特性**: AI 协作、内置终端、Git 集成、MCP 服务器、多媒体预览

> **关系说明**: Medecode 由同一开发者维护，定位为轻量级代码编辑器（类似 VSCode），专注于提供简洁高效的编码体验。Mede-IDE 则定位为功能完整的 IDE（类似 IDEA），提供 AI 辅助、终端、Git 集成等高级功能。两个项目互补，满足不同场景需求。

## 开发者

- **开发者**: [PFLcoolme](https://github.com/PFLcoolme)
- **协助者**: [Evilgodxu](https://github.com/Evilgodxu)

## 贡献

欢迎提交 Pull Request 和 Issue！