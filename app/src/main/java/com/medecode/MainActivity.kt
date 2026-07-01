package com.medecode

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medecode.editor.CodeEditor
import com.medecode.model.EditorFile
import com.medecode.ui.*
import com.medecode.ui.theme.MedecodeTheme

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 全屏显示，隐藏状态栏
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedecodeApp() {
    // State for open files
    var openFiles by remember { mutableStateOf<List<EditorFile>>(emptyList()) }
    var activeFileIndex by remember { mutableIntStateOf(-1) }
    var showFileBrowser by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(false) }
    var projectPath by remember { mutableStateOf("") }
    
    // New features state
    var showSearchReplace by remember { mutableStateOf(false) }
    var showRecentFiles by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    
    // 长按搜索状态
    var longPressSearchText by remember { mutableStateOf("") }
    
    // Recent files manager
    val recentManager = rememberRecentFileManager()
    
    // Get active file
    val activeFile = remember(openFiles, activeFileIndex) {
        if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) {
            openFiles[activeFileIndex]
        } else {
            null
        }
    }
    
    val context = LocalContext.current

    fun openFile(name: String, path: String, content: String) {
        val editorFile = EditorFile(name = name, path = path, content = content)
        val existingIndex = openFiles.indexOfFirst { it.path == path }

        if (existingIndex >= 0) {
            activeFileIndex = existingIndex
        } else {
            openFiles = openFiles + editorFile
            activeFileIndex = openFiles.size - 1
        }

        recentManager.addFile(path, name)
    }

    // File launcher for opening files
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "untitled"
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                openFile(fileName, it.toString(), content)
            }
        }
    }

    // Save file launcher
    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri: Uri? ->
        uri?.let {
            activeFile?.let { file ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(file.content.toByteArray())
                    }
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }
    
    // 横屏布局 - 使用 Row 并排显示
    if (openFiles.isEmpty()) {
        WelcomeScreenLandscape(
            onOpenProject = { showFileBrowser = true },
            onOpenFile = { fileLauncher.launch(arrayOf("text/*")) },
            onOpenRecentFiles = { showRecentFiles = true },
            recentFiles = recentManager.getRecentFiles(),
            onRecentFileSelected = { path, name ->
                try {
                    java.io.File(path).takeIf { it.exists() }?.readText()?.let { content ->
                        openFile(name, path, content)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Medecode", "Error reading recent file", e)
                }
            },
            onRecentFileRemoved = { recentManager.removeFile(it) }
        )
    } else {
        // 主编辑界面 - 横屏布局
        Scaffold(
            topBar = {
                EditorTopBarLandscape(
                    fileName = activeFile?.name ?: "",
                    fileCount = openFiles.size,
                    activeIndex = activeFileIndex,
                    onFileSelected = { activeFileIndex = it },
                    onSearch = { showSearchReplace = true },
                    onRecentFiles = { showRecentFiles = true },
                    onOpenProject = { 
                        if (projectPath.isEmpty()) {
                            showFileBrowser = true
                        } else {
                            projectPath = ""
                            showSidebar = !showSidebar
                        }
                    },
                    onSave = { activeFile?.let { saveFileLauncher.launch(it.name) } },
                    onSettings = { showSettings = true },
                    onToggleSidebar = { showSidebar = !showSidebar },
                    showSidebar = showSidebar
                )
            },
            content = { paddingValues ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // 侧边栏 - 可切换显示
                    if (showSidebar && projectPath.isNotEmpty()) {
                        SidebarPanel(
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
                            modifier = Modifier.width(240.dp)
                        )
                    } else if (showSidebar) {
                        // 显示欢迎侧边栏
                        SidebarPanel(
                            onOpenProject = { showFileBrowser = true },
                            onOpenFile = { fileLauncher.launch(arrayOf("text/*")) },
                            recentFiles = recentManager.getRecentFiles(),
                            onRecentFileSelected = { path, name ->
                                try {
                                    java.io.File(path).takeIf { it.exists() }?.readText()?.let { content ->
                                        openFile(name, path, content)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("Medecode", "Error reading recent file", e)
                                }
                            },
                            modifier = Modifier.width(280.dp)
                        )
                    }
                    
                    // 主编辑区域
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // File tabs
                        if (openFiles.size > 1) {
                            FileTabsLandscape(
                                files = openFiles,
                                activeIndex = activeFileIndex,
                                onTabSelected = { activeFileIndex = it },
                                onTabClosed = { index ->
                                    openFiles = openFiles.toMutableList().apply { removeAt(index) }
                                    if (activeFileIndex >= openFiles.size) {
                                        activeFileIndex = if (openFiles.isEmpty()) -1 else openFiles.size - 1
                                    }
                                }
                            )
                        } else {
                            activeFileIndex = 0
                        }
                        
                        // Code editor
                        activeFile?.let { file ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .detectLongPressSwipe(
                                        onLongPress = { x, y ->
                                            longPressSearchText = getTextAtPosition(file.content, x, y)
                                            showSearchReplace = true
                                        }
                                    )
                            ) {
                                CodeEditor(
                                    editorFile = file,
                                    onContentChange = { newContent ->
                                        openFiles = openFiles.map { 
                                            if (it == file) it.copy(content = newContent) else it 
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        
                        // 状态栏
                        activeFile?.let { file ->
                            StatusBarLandscape(
                                language = getLanguageFromFileName(file.name),
                                totalLines = file.content.lines().size,
                                totalChars = file.content.length
                            )
                        }
                    }
                }
            }
        )
    }
    
    // File browser dialog
    if (showFileBrowser) {
        FileBrowser(
            onFileSelected = { path, name ->
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
            onFolderSelected = { path ->
                projectPath = path
                showSidebar = true
            },
            onDismiss = { showFileBrowser = false }
        )
    }
    
    // Search and replace dialog
    if (showSearchReplace && activeFile != null) {
        MobileSearchReplaceDialog(
            initialText = activeFile.content,
            onDismiss = { showSearchReplace = false },
            onReplace = { _, newContent ->
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
        MobileRecentFilesDialog(
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
            onRemove = { path -> recentManager.removeFile(path) },
            onClearAll = { recentManager.clearAll() },
            onDismiss = { showRecentFiles = false }
        )
    }
    
    // Command palette dialog
    if (showCommandPalette) {
        MobileCommandPalette(
            onDismiss = { showCommandPalette = false },
            onSearch = { showSearchReplace = true },
            onToggleSidebar = { showSidebar = !showSidebar },
            onOpenFile = { fileLauncher.launch(arrayOf("text/*")) },
            onSaveFile = { activeFile?.let { saveFileLauncher.launch(it.name) } },
            onRecentFiles = { showRecentFiles = true }
        )
    }
    
    // Settings dialog
    if (showSettings) {
        MobileSettingsDialog(
            onDismiss = { showSettings = false }
        )
    }
}

/**
 * 长按检测
 */
private fun Modifier.detectLongPressSwipe(
    onLongPress: (Float, Float) -> Unit
): Modifier = composed {
    var longPressTriggered by remember { mutableStateOf(false) }
    var startX = 0f
    var startY = 0f
    var pressStartTime = 0L
    
    this
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = {
                    startX = it.x
                    startY = it.y
                    pressStartTime = System.currentTimeMillis()
                },
                onDrag = { _, _ ->
                    val pressDuration = System.currentTimeMillis() - pressStartTime
                    if (pressDuration > 500) {
                        longPressTriggered = true
                    }
                },
                onDragEnd = {
                    val pressDuration = System.currentTimeMillis() - pressStartTime
                    if (!longPressTriggered && pressDuration > 500) {
                        onLongPress(startX, startY)
                    }
                    longPressTriggered = false
                },
                onDragCancel = {
                    longPressTriggered = false
                }
            )
        }
}

/**
 * 获取位置对应的代码行和列
 */
private fun getTextAtPosition(content: String, x: Float, y: Float): String {
    val lines = content.lines()
    val lineHeight = 21f // 约 14sp * 1.5
    val lineIndex = (y / lineHeight).toInt().coerceIn(0, lines.size - 1)
    return lines.getOrElse(lineIndex) { "" }
}

/**
 * 欢迎屏幕 - 适配手机
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeScreen(
    onOpenProject: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenRecentFiles: () -> Unit,
    recentFiles: List<com.medecode.ui.RecentFile>,
    onRecentFileSelected: (String, String) -> Unit,
    onRecentFileRemoved: (String) -> Unit
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Medecode") },
                actions = {
                    IconButton(onClick = onOpenRecentFiles) {
                        Icon(Icons.Default.Schedule, "最近打开")
                    }
                    IconButton(onClick = { /* TODO: 设置 */ }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Medecode",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "Android 代码编辑器",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 主要操作按钮
                OutlinedButton(
                    onClick = onOpenProject,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.padding(end = 8.dp))
                    Text("打开项目", fontSize = 16.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedButton(
                    onClick = onOpenFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.OpenInNew, null, modifier = Modifier.padding(end = 8.dp))
                    Text("打开文件", fontSize = 16.sp)
                }
                
                if (recentFiles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "最近打开",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(recentFiles, key = { it.path }) { file ->
                            MobileRecentFileItem(
                                file = file,
                                onClick = { onRecentFileSelected(file.path, file.name) },
                                onRemove = { onRecentFileRemoved(file.path) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 支持的语言提示
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "支持 Python · JavaScript · Java · Kotlin · C/C++ · Go · Rust 等",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    )
}

/**
 * 编辑器顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBar(
    fileName: String,
    fileCount: Int,
    activeIndex: Int,
    onFileSelected: (Int) -> Unit,
    onSearch: () -> Unit,
    onRecentFiles: () -> Unit,
    onOpenProject: () -> Unit,
    onSave: () -> Unit,
    onSettings: () -> Unit,
    onSidebarToggle: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = if (fileName.isNotEmpty()) fileName else "Medecode",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (fileName.isNotEmpty()) {
                IconButton(onClick = onSidebarToggle) {
                    Icon(Icons.Default.Menu, "菜单")
                }
            }
        },
        actions = {
            // 文件标签指示器
            if (fileCount > 1) {
                Text(
                    text = "${activeIndex + 1}/$fileCount",
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .clickable { onFileSelected((activeIndex + 1) % fileCount) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // 搜索
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, "搜索")
            }
            // 最近文件
            IconButton(onClick = onRecentFiles) {
                Icon(Icons.Default.Schedule, "最近")
            }
            // 打开项目
            IconButton(onClick = onOpenProject) {
                Icon(Icons.Default.FolderOpen, "项目")
            }
            // 保存
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, "保存")
            }
            // 设置
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "设置")
            }
        }
    )
}

/**
 * 侧边栏抽屉内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SidebarDrawerContent(
    projectPath: String,
    onFileSelected: (String, String) -> Unit,
    onClose: () -> Unit
) {
    ModalNavigationDrawer(
        drawerState = rememberDrawerState(DrawerValue.Open),
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(280.dp)
            ) {
                DrawerHeader(
                    projectName = projectPath.substringAfterLast('/'),
                    onClose = onClose
                )
                
                Divider()
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // 这里应该显示项目文件树
                    // 为简化，显示提示
                    item {
                        Text(
                            text = "项目文件",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(16.dp, 8.dp)
                        )
                        Text(
                            text = "请先选择一个项目目录",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    ) {
        // 内容由外部处理
    }
}

@Composable
private fun DrawerHeader(
    projectName: String,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "项目",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = projectName.ifEmpty { "未选择" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "关闭")
            }
        }
    }
}

/**
 * 手机文件标签页
 */
@Composable
private fun MobileFileTabs(
    files: List<EditorFile>,
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(files.indices.toList()) { index ->
                TabChip(
                    text = files[index].name,
                    selected = index == activeIndex,
                    onClose = if (index != activeIndex) { { onTabClosed(index) } } else null,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

@Composable
private fun TabChip(
    text: String,
    selected: Boolean,
    onClose: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .height(32.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer 
                else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            onClose?.let {
                IconButton(
                    onClick = it,
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "关闭",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * 手机状态栏
 */
@Composable
private fun MobileStatusBar(
    language: String,
    totalLines: Int,
    totalChars: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "$totalLines 行 · $totalChars 字符",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * 手机搜索替换对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSearchReplaceDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onReplace: (String, String) -> Unit,
    onReplaceAll: (String, String, String) -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    
    // 计算匹配数
    val matchCount = remember(searchText, caseSensitive, initialText) {
        findAllMatches(initialText, searchText, caseSensitive).size
    }
    
    BottomSheetDialog(
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "搜索和替换",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("搜索") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (searchText.isNotEmpty()) {
                Text(
                    text = "找到 $matchCount 个匹配",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (matchCount > 0) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )
            }
            
            OutlinedTextField(
                value = replaceText,
                onValueChange = { replaceText = it },
                label = { Text("替换为") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (searchText.isNotEmpty()) {
                            val newContent = initialText.replace(searchText, replaceText)
                            onReplace(searchText, newContent)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SyncAlt, null, modifier = Modifier.padding(end = 4.dp))
                    Text("全部替换")
                }
                
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

/**
 * 手机最近文件对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileRecentFilesDialog(
    recentManager: com.medecode.ui.RecentFileManager,
    onFileSelected: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "最近打开",
                    style = MaterialTheme.typography.titleLarge
                )
                if (recentManager.getRecentFiles().isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("清空")
                    }
                }
            }
            
            Divider(modifier = Modifier.padding(top = 8.dp))
            
            val recentFiles = recentManager.getRecentFiles()
            if (recentFiles.isEmpty()) {
                Text(
                    text = "还没有打开过文件",
                    modifier = Modifier.padding(vertical = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(recentFiles, key = { it.path }) { file ->
                        MobileRecentFileItem(
                            file = file,
                            onClick = { 
                                onFileSelected(file.path, file.name) 
                                onDismiss()
                            },
                            onRemove = { onRemove(file.path) }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("关闭")
            }
        }
    }
}

@Composable
private fun MobileRecentFileItem(
    file: com.medecode.ui.RecentFile,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getFileIconForRecent(file.name),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = formatTimestamp(file.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "移除", modifier = Modifier.size(20.dp))
            }
        }
    }
}

/**
 * 手机命令面板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileCommandPalette(
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onToggleSidebar: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onRecentFiles: () -> Unit
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "命令面板",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            CommandButton(
                icon = Icons.Default.Search,
                label = "搜索和替换",
                description = "在文件中搜索和替换文本",
                onClick = onSearch
            )
            
            CommandButton(
                icon = Icons.Default.Menu,
                label = "切换侧边栏",
                description = "显示或隐藏文件树",
                onClick = onToggleSidebar
            )
            
            CommandButton(
                icon = Icons.Default.OpenInNew,
                label = "打开文件",
                description = "从设备打开代码文件",
                onClick = onOpenFile
            )
            
            CommandButton(
                icon = Icons.Default.Save,
                label = "保存文件",
                description = "保存当前编辑的文件",
                onClick = onSaveFile
            )
            
            CommandButton(
                icon = Icons.Default.Schedule,
                label = "最近打开",
                description = "查看最近打开的文件",
                onClick = onRecentFiles
            )
        }
    }
}

@Composable
private fun CommandButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * 手机设置对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MobileSettingsDialog(
    onDismiss: () -> Unit
) {
    var fontSize by remember { mutableIntStateOf(14) }
    
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text("字体大小: $fontSize", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = fontSize.toFloat(),
                onValueChange = { fontSize = it.toInt() },
                valueRange = 10f..24f,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("完成")
            }
        }
    }
}

// ==================== 工具函数 ====================

/**
 * 底部弹窗 (Bottom Sheet 风格)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        content()
    }
}

private fun getUriFileName(uri: Uri, context: android.content.Context): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
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

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
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
        "sql" -> "SQL"
        "sh" -> "Shell"
        "md" -> "Markdown"
        "rs" -> "Rust"
        "go" -> "Go"
        "rb" -> "Ruby"
        "php" -> "PHP"
        else -> ext.ifEmpty { "Text" }.replaceFirstChar { it.uppercase() }
    }
}

private fun findAllMatches(content: String, search: String, caseSensitive: Boolean): List<Int> {
    if (search.isEmpty()) return emptyList()
    val result = mutableListOf<Int>()
    val searchIn = if (caseSensitive) content else content.lowercase()
    val searchPattern = if (caseSensitive) search else search.lowercase()
    var index = 0
    while (index <= searchIn.length - searchPattern.length) {
        val found = searchIn.indexOf(searchPattern, index)
        if (found == -1) break
        result.add(found)
        index = found + 1
    }
    return result
}

fun getFileIconForRecent(name: String): androidx.compose.ui.graphics.vector.ImageVector {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts", "java" -> androidx.compose.material.icons.Icons.Default.Code
        "py" -> androidx.compose.material.icons.Icons.Default.Description
        "js", "ts" -> androidx.compose.material.icons.Icons.Default.Javascript
        "json" -> androidx.compose.material.icons.Icons.Default.Storage
        "xml" -> androidx.compose.material.icons.Icons.Default.Code
        "html" -> androidx.compose.material.icons.Icons.Default.Language
        "md" -> androidx.compose.material.icons.Icons.Default.Article
        else -> androidx.compose.material.icons.Icons.Default.InsertDriveFile
    }
}

fun formatTimestamp(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60000
    val hours = diff / 3600000
    val days = diff / 86400000
    
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        hours < 24 -> "$hours 小时前"
        days < 7 -> "$days 天前"
        else -> "${days / 7} 周前"
    }
}

// ==================== 横屏布局组件 ====================

/**
 * 横屏欢迎屏幕
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WelcomeScreenLandscape(
    onOpenProject: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenRecentFiles: () -> Unit,
    recentFiles: List<com.medecode.ui.RecentFile>,
    onRecentFileSelected: (String, String) -> Unit,
    onRecentFileRemoved: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Medecode",
                        style = TextStyle(fontWeight = FontWeight.ExtraBold)
                    ) 
                },
                actions = {
                    IconButton(onClick = onOpenRecentFiles) {
                        Icon(Icons.Default.Schedule, "最近打开", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = { /* TODO: 设置 */ }) {
                        Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 左侧：欢迎内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo
                Icon(
                    Icons.Default.Code,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Medecode",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = "Android 代码编辑器",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // 主要操作按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenProject,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.padding(end = 8.dp))
                        Text("打开项目", fontSize = 14.sp)
                    }
                    
                    OutlinedButton(
                        onClick = onOpenFile,
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(Icons.Default.OpenInNew, null, modifier = Modifier.padding(end = 8.dp))
                        Text("打开文件", fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 支持的语言提示
                Surface(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Python · JavaScript · Java · Kotlin · C/C++ · Go · Rust",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            
            // 右侧：最近文件列表
            if (recentFiles.isNotEmpty()) {
                Divider(
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.width(1.dp)
                )
                
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "最近打开",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(recentFiles, key = { it.path }) { file ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRecentFileSelected(file.path, file.name) }
                                    .height(48.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        getFileIconForRecent(file.name),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 12.dp),
                                        maxLines = 1
                                    )
                                    Text(
                                        text = formatTimestamp(file.timestamp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    IconButton(
                                        onClick = { onRecentFileRemoved(file.path) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            "移除",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 横屏编辑器顶部栏
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTopBarLandscape(
    fileName: String,
    fileCount: Int,
    activeIndex: Int,
    onFileSelected: (Int) -> Unit,
    onSearch: () -> Unit,
    onRecentFiles: () -> Unit,
    onOpenProject: () -> Unit,
    onSave: () -> Unit,
    onSettings: () -> Unit,
    onToggleSidebar: () -> Unit,
    showSidebar: Boolean
) {
    TopAppBar(
        title = {
            Text(
                text = if (fileName.isNotEmpty()) fileName else "Medecode",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            IconButton(onClick = onToggleSidebar) {
                Icon(
                    if (showSidebar) Icons.Default.Menu else Icons.Default.Menu,
                    "切换侧边栏",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            if (fileCount > 1) {
                Text(
                    text = "${activeIndex + 1}/$fileCount",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSearch) {
                Icon(Icons.Default.Search, "搜索", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onRecentFiles) {
                Icon(Icons.Default.Schedule, "最近", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onOpenProject) {
                Icon(Icons.Default.FolderOpen, "项目", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, "保存", tint = MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, "设置", tint = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * 侧边栏面板
 */
@Composable
private fun SidebarPanel(
    projectPath: String? = null,
    onOpenProject: (() -> Unit)? = null,
    onOpenFile: (() -> Unit)? = null,
    recentFiles: List<com.medecode.ui.RecentFile> = emptyList(),
    onRecentFileSelected: ((String, String) -> Unit)? = null,
    onFileSelected: ((String, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (projectPath != null) "项目" else "文件",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Divider(color = MaterialTheme.colorScheme.outline)
            
            if (projectPath != null) {
                // 项目文件列表
                Text(
                    text = "请先选择一个项目目录",
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                // 最近文件列表
                if (recentFiles.isEmpty()) {
                    Text(
                        text = "还没有打开过文件",
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(recentFiles, key = { it.path }) { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onRecentFileSelected?.invoke(file.path, file.name) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    getFileIconForRecent(file.name),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 横屏文件标签
 */
@Composable
private fun FileTabsLandscape(
    files: List<EditorFile>,
    activeIndex: Int,
    onTabSelected: (Int) -> Unit,
    onTabClosed: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(files.indices.toList()) { index ->
                TabChip(
                    text = files[index].name,
                    selected = index == activeIndex,
                    onClose = if (index != activeIndex) { { onTabClosed(index) } } else null,
                    onClick = { onTabSelected(index) }
                )
            }
        }
    }
}

/**
 * 横屏状态栏
 */
@Composable
private fun StatusBarLandscape(
    language: String,
    totalLines: Int,
    totalChars: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$totalLines 行 · $totalChars 字符",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
