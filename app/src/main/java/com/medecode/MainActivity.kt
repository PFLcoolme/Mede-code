package com.medecode

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.tab.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medecode.editor.CodeEditor
import com.medecode.model.EditorFile
import com.medecode.ui.*
import com.medecode.ui.theme.MedecodeTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            MedecodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedecodeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedecodeApp() {
    val darkTheme = isSystemInDarkTheme()
    
    // State for open files
    var openFiles by remember { mutableStateOf<List<EditorFile>>(emptyList()) }
    var activeFileIndex by remember { mutableIntStateOf(-1) }
    var showFileBrowser by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var projectPath by remember { mutableStateOf("") }
    var showSidebar by remember { mutableStateOf(false) }
    
    // New features state
    var showSearchReplace by remember { mutableStateOf(false) }
    var showRecentFiles by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    
    // Recent files manager
    val recentManager = rememberRecentFileManager()
    
    // Command palette state
    var commandQuery by remember { mutableStateOf("") }
    var showCommandQuery by remember { mutableStateOf(false) }
    
    // Get active file
    val activeFile = remember(openFiles, activeFileIndex) {
        if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) {
            openFiles[activeFileIndex]
        } else {
            null
        }
    }
    
    // File launcher for opening files
    val fileLauncher = remember {
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                val fileName = getFileName(it) ?: "untitled"
                contentResolver.openInputStream(it)?.use { inputStream ->
                    val content = inputStream.bufferedReader().readText()
                    openFile(fileName, it.toString(), content)
                }
            }
        }
    }
    
    // Save file launcher
    val saveFileLauncher = remember {
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri: Uri? ->
            uri?.let {
                activeFile?.let { file ->
                    try {
                        contentResolver.openOutputStream(it)?.use { outputStream ->
                            outputStream.write(file.content.toByteArray())
                        }
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        }
    }
    
    fun openFile(name: String, path: String, content: String) {
        val editorFile = EditorFile(name = name, path = path, content = content)
        val existingIndex = openFiles.indexOfFirst { it.path == path }
        
        if (existingIndex >= 0) {
            // File already open, switch to it
            activeFileIndex = existingIndex
        } else {
            // Add new file
            openFiles = openFiles + editorFile
            activeFileIndex = openFiles.size - 1
        }
        
        // Add to recent files
        recentManager.addFile(path, name)
    }
    
    fun closeFile(path: String) {
        openFiles = openFiles.filter { it.path != path }
        if (activeFileIndex >= openFiles.size) {
            activeFileIndex = if (openFiles.isEmpty()) -1 else openFiles.size - 1
        }
    }
    
    // Keyboard shortcut handler
    val onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
        when {
            // Ctrl+P or Ctrl+Shift+P - Command palette
            keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                androidx.compose.ui.input.key.isCtrlPressed(keyEvent) &&
                keyEvent.key == androidx.compose.ui.input.key.Key.P -> {
                showCommandPalette = true
                true
            }
            // Ctrl+S - Save
            keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                androidx.compose.ui.input.key.isCtrlPressed(keyEvent) &&
                keyEvent.key == androidx.compose.ui.input.key.Key.S -> {
                activeFile?.let { saveFileLauncher.launch(it.name) }
                true
            }
            // Ctrl+F - Search
            keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                androidx.compose.ui.input.key.isCtrlPressed(keyEvent) &&
                keyEvent.key == androidx.compose.ui.input.key.Key.F -> {
                showSearchReplace = true
                true
            }
            // Ctrl+B - Toggle sidebar
            keyEvent.type == androidx.compose.ui.input.key.KeyEventType.KeyDown &&
                androidx.compose.ui.input.key.isCtrlPressed(keyEvent) &&
                keyEvent.key == androidx.compose.ui.input.key.Key.B -> {
                showSidebar = !showSidebar
                true
            }
            else -> false
        }
    }
    
    LaunchedEffect(showCommandPalette) {
        if (showCommandPalette) {
            commandQuery = ""
            showCommandQuery = true
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent(onKeyEvent)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Sidebar (optional, shown when project is open)
            if (showSidebar && projectPath.isNotEmpty()) {
                Sidebar(
                    projectPath = projectPath,
                    onFileSelected = { path, name ->
                        try {
                            java.io.File(path).takeIf { it.exists() }?.readText()?.let { fileContent ->
                                openFile(name, path, fileContent)
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("Medecode", "Error reading file", e)
                        }
                    },
                    onProjectPathChange = { newPath ->
                        projectPath = newPath
                        if (newPath.isEmpty()) {
                            showSidebar = false
                        }
                    },
                    modifier = Modifier.weight(0f)
                )
            }
            
            // Main content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                // Top app bar
                TopAppBar(
                    title = { 
                        Text(
                            text = if (activeFile != null) "${activeFile.name} - Medecode" else "Medecode",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        // Recent files button
                        IconButton(onClick = { showRecentFiles = true }) {
                            Icon(
                                Icons.Default.Schedule,
                                "最近打开"
                            )
                        }
                        // Toggle sidebar
                        IconButton(onClick = { showSidebar = !showSidebar }) {
                            Icon(
                                Icons.Default.Menu,
                                if (showSidebar) "隐藏侧边栏" else "显示侧边栏"
                            )
                        }
                        // Open project
                        IconButton(onClick = { 
                            if (projectPath.isEmpty()) {
                                showFileBrowser = true
                            } else {
                                projectPath = ""
                                showSidebar = false
                            }
                        }) {
                            Icon(
                                Icons.Default.FolderOpen,
                                if (projectPath.isNotEmpty()) "切换项目" else "打开项目"
                            )
                        }
                        // Open file
                        IconButton(onClick = { fileLauncher.launch(arrayOf("text/*")) }) {
                            Icon(Icons.Default.OpenInNew, "打开文件")
                        }
                        // Save file
                        IconButton(onClick = { activeFile?.let { saveFileLauncher.launch(it.name) }) {
                            Icon(Icons.Default.Save, "保存")
                        }
                        // Settings
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Default.Settings, "设置")
                        }
                    }
                )
                
                // Content
                if (openFiles.isEmpty()) {
                    // Welcome screen
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "Medecode",
                                style = MaterialTheme.typography.displayLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Android 代码编辑器",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(
                                onClick = { showFileBrowser = true },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Icon(Icons.Default.FolderOpen, "打开项目", modifier = Modifier.padding(end = 8.dp))
                                Text("打开项目")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { fileLauncher.launch(arrayOf("text/*")) },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Icon(Icons.Default.OpenInNew, "打开文件", modifier = Modifier.padding(end = 8.dp))
                                Text("打开文件")
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "支持 Python、JavaScript、Java、Kotlin、C/C++、Go、Rust 等多种语言",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "快捷键: Ctrl+P 命令面板 | Ctrl+F 搜索 | Ctrl+S 保存 | Ctrl+B 侧边栏",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Tab row for open files
                    if (openFiles.size > 1) {
                        TabRow(
                            selectedTabIndex = activeFileIndex,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            scrollIndicatorModifier = Modifier.scrollIndicator(
                                rememberScrollIndicatorState()
                            )
                        ) {
                            openFiles.forEachIndexed { index, file ->
                                Tab(
                                    selected = index == activeFileIndex,
                                    onClick = { activeFileIndex = index },
                                    text = {
                                        Text(
                                            text = file.name,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    selectedContentColor = MaterialTheme.colorScheme.primary,
                                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        activeFileIndex = 0
                    }
                    
                    // Editor
                    activeFile?.let { file ->
                        val language = getLanguageFromFileName(file.name)
                        val statusBarInfo = StatusBarInfo(
                            language = language,
                            line = 1,
                            column = 1,
                            totalLines = file.content.lines().size,
                            totalChars = file.content.length
                        )
                        
                        Column(modifier = Modifier.weight(1f)) {
                            CodeEditor(
                                editorFile = file,
                                onContentChange = { newContent ->
                                    openFiles = openFiles.map { 
                                        if (it == file) it.copy(content = newContent) else it 
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // Status bar
                            StatusBar(info = statusBarInfo)
                        }
                    }
                }
            }
        }
    }
    
    // File browser dialog
    if (showFileBrowser) {
        FileBrowser(
            onFileSelected = { path, name ->
                // If it's a directory, set as project path
                val file = java.io.File(path)
                if (file.isDirectory) {
                    projectPath = path
                    showSidebar = true
                } else {
                    try {
                        file.takeIf { it.exists() }?.readText()?.let { fileContent ->
                            openFile(name, path, fileContent)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("Medecode", "Error reading file", e)
                    }
                }
            },
            onDismiss = { showFileBrowser = false }
        )
    }
    
    // Search and replace dialog
    if (showSearchReplace && activeFile != null) {
        SearchReplaceDialog(
            initialText = activeFile.content,
            onDismiss = { showSearchReplace = false },
            onSearch = { _, _ -> },
            onReplace = { search, newContent ->
                openFiles = openFiles.map { 
                    if (it == activeFile) it.copy(content = newContent) else it 
                }
            },
            onReplaceAll = { _, search, replace ->
                val newContent = activeFile.content.replace(search, replace)
                openFiles = openFiles.map { 
                    if (it == activeFile) it.copy(content = newContent) else it 
                }
            }
        )
    }
    
    // Recent files dialog
    if (showRecentFiles) {
        RecentFilesPanel(
            recentManager = recentManager,
            onFileSelected = { path, name ->
                try {
                    java.io.File(path).takeIf { it.exists() }?.readText()?.let { content ->
                        openFile(name, path, content)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Medecode", "Error reading recent file", e)
                }
            },
            onRemove = { path ->
                recentManager.removeFile(path)
            },
            onClearAll = {
                recentManager.clearAll()
            },
            onDismiss = { showRecentFiles = false }
        )
    }
    
    // Command palette dialog
    if (showCommandPalette) {
        CommandPalette(
            state = remember { CommandPaletteState(
                commands = listOf(
                    CommandEntry(
                        id = "search",
                        label = "搜索和替换",
                        description = "在文件中搜索和替换文本",
                        icon = Icons.Default.Search,
                        shortcut = "Ctrl+F"
                    ) { showSearchReplace = true },
                    CommandEntry(
                        id = "toggle_sidebar",
                        label = "切换侧边栏",
                        description = "显示或隐藏文件树侧边栏",
                        icon = Icons.Default.Menu,
                        shortcut = "Ctrl+B"
                    ) { showSidebar = !showSidebar },
                    CommandEntry(
                        id = "open_file",
                        label = "打开文件",
                        description = "从设备打开代码文件",
                        icon = Icons.Default.OpenInNew
                    ) { fileLauncher.launch(arrayOf("text/*")) },
                    CommandEntry(
                        id = "save_file",
                        label = "保存文件",
                        description = "保存当前编辑的文件",
                        icon = Icons.Default.Save,
                        shortcut = "Ctrl+S"
                    ) { activeFile?.let { saveFileLauncher.launch(it.name) } },
                    CommandEntry(
                        id = "recent_files",
                        label = "最近打开",
                        description = "查看最近打开的文件",
                        icon = Icons.Default.Schedule
                    ) { showRecentFiles = true }
                ),
                onCommandSelected = {}
            )},
            onDismiss = { 
                showCommandPalette = false 
                showCommandQuery = false
            }
        )
    }
    
    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableIntStateOf(14) }
    var darkTheme by remember { mutableStateOf(isSystemInDarkTheme()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置") },
        text = {
            Column {
                Text("字体大小: $fontSize")
                Slider(
                    value = fontSize.toFloat(),
                    onValueChange = { fontSize = it.toInt() },
                    valueRange = 10f..24f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("深色模式")
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = darkTheme,
                        onCheckedChange = { darkTheme = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("确定")
            }
        }
    )
}

private fun getUriFileName(uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path?.substringAfterLast('/')
    }
    return result
}

private fun getFileName(uri: Uri): String? {
    return getUriFileName(uri)
}

private fun getLanguageFromFileName(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "py" -> "Python"
        "js" -> "JavaScript"
        "ts" -> "TypeScript"
        "json" -> "JSON"
        "xml" -> "XML"
        "yaml", "yml" -> "YAML"
        "gradle" -> "Gradle"
        "html" -> "HTML"
        "css" -> "CSS"
        "scss" -> "SCSS"
        "sass" -> "Sass"
        "less" -> "Less"
        "sql" -> "SQL"
        "sh" -> "Shell"
        "bash" -> "Bash"
        "md" -> "Markdown"
        "txt" -> "Text"
        "c" -> "C"
        "cpp", "cc", "cxx" -> "C++"
        "h", "hpp" -> "C++ Header"
        "rs" -> "Rust"
        "go" -> "Go"
        "rb" -> "Ruby"
        "php" -> "PHP"
        "swift" -> "Swift"
        "dart" -> "Dart"
        "r" -> "R"
        "lua" -> "Lua"
        "scala" -> "Scala"
        "jsx" -> "JSX"
        "tsx" -> "TSX"
        else -> ext.ifEmpty { "Text" }.replaceFirstChar { it.uppercase() }
    }
}