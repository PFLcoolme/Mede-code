# Medecode - Android 代码编辑器

一个支持多语言语法高亮的 Android 代码编辑器应用。

## 功能特性

- 📝 **代码编辑** - 支持代码编辑的基本功能
- 🎨 **语法高亮** - 支持多种编程语言的语法高亮
- 🌙 **深色/浅色模式** - 跟随系统主题自动切换
- 📂 **文件浏览器** - 浏览设备文件系统，选择文件打开
- 📁 **侧边栏文件树** - VSCode 风格的项目文件树面板
- 📂 **文件操作** - 打开和保存代码文件
- 📑 **多标签页** - 同时编辑多个文件
- ⌨️ **智能输入** - 自动缩进、括号自动补全

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
- [ ] 搜索和替换
- [ ] 底部状态栏
- [ ] 命令面板
- [ ] 代码折叠
- [ ] 最近打开文件

## 与 Mede-IDE 的区别

| 功能 | Medecode (VSCode) | Mede-IDE (IDEA) |
|------|-------------------|-----------------|
| 定位 | 轻量级代码编辑器 | 完整 IDE |
| AI 集成 | ❌ | ✅ 本地+云端 |
| 终端 | ❌ | ✅ 内置 Termux |
| Git | ❌ | ✅ 完整集成 |
| 文件树 | ✅ 侧边栏 | ✅ 三栏布局 |
| 语法高亮 | ✅ | ✅ |
| 多标签 | ✅ | ✅ |
| 文件大小 | 轻量 | 较重 |

## 许可证

MIT License

## 贡献

欢迎提交 Pull Request 和 Issue！