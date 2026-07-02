package com.medemini

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.ai.ui.AIAssistantPanel
import com.medemini.ai.ui.AISettingsDialog
import com.medemini.ai.viewmodel.AIViewModel
import com.medemini.editor.CodeEditor
import com.medemini.editor.SyntaxHighlight
import com.medemini.model.EditorFile
import com.medemini.ui.*
import com.medemini.ui.theme.MedeMiniTheme
// import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 请求所有文件访问权限（Android 11+）
        requestStoragePermission()
        setContent {
            MedeMiniTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    MedeMiniApp()
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (_: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        }
    }
}

// 全局设置状态
object AppSettings {
    var glassEnabled by mutableStateOf(true)
    var glassColor by mutableStateOf(Color(0xFFE8E8E8))
    var glassAlpha by mutableStateOf(0.7f)
    var editorBgColor by mutableStateOf(Color(0xFFFFFBF3))
    var keywordColor by mutableStateOf(Color(0xFF6344CF))
    var stringColor by mutableStateOf(Color(0xFF1A997F))
    var commentColor by mutableStateOf(Color(0xFF8C8C8C))
    var numberColor by mutableStateOf(Color(0xFFAC6636))
    var functionColor by mutableStateOf(Color(0xFF2C72D5))
    var typeColor by mutableStateOf(Color(0xFFC084FC))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedeMiniApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val aiViewModel = remember { AIViewModel(context.applicationContext as android.app.Application) }
    
    var openFiles by remember { mutableStateOf<List<EditorFile>>(emptyList()) }
    var activeFileIndex by remember { mutableStateOf(-1) }
    var showSidebar by remember { mutableStateOf(false) }
    var showAIChat by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showAISettings by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showCommandPalette by remember { mutableStateOf(false) }
    var projectPath by remember { mutableStateOf("") }

    // 同步高亮配色
    LaunchedEffect(AppSettings.keywordColor) { SyntaxHighlight.keywordColor = AppSettings.keywordColor }
    LaunchedEffect(AppSettings.stringColor) { SyntaxHighlight.stringColor = AppSettings.stringColor }
    LaunchedEffect(AppSettings.commentColor) { SyntaxHighlight.commentColor = AppSettings.commentColor }
    LaunchedEffect(AppSettings.numberColor) { SyntaxHighlight.numberColor = AppSettings.numberColor }
    LaunchedEffect(AppSettings.functionColor) { SyntaxHighlight.functionColor = AppSettings.functionColor }
    LaunchedEffect(AppSettings.typeColor) { SyntaxHighlight.typeColor = AppSettings.typeColor }

    val recentManager = rememberRecentFileManager()
    val activeFile = if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) openFiles[activeFileIndex] else null

    // 同步 AI 聊天显示状态
    LaunchedEffect(aiViewModel.showAIChat.value) {
        aiViewModel.showAIChat.collect { showAIChat = it }
    }

    fun openFile(name: String, path: String, content: String) {
        val editorFile = EditorFile(name = name, path = path, content = content)
        val existingIndex = openFiles.indexOfFirst { it.path == path }
        if (existingIndex >= 0) {
            activeFileIndex = existingIndex
        } else {
            openFiles = openFiles.toMutableList().apply { add(editorFile) }
            activeFileIndex = openFiles.size - 1
        }
        recentManager.addFile(path, name)
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val fileName = getFileName(context, it) ?: "untitled"
            context.contentResolver.openInputStream(it)?.use { inputStream ->
                openFile(fileName, it.toString(), inputStream.bufferedReader().readText())
            }
        }
    }

    val projectLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let { treeUri ->
            try {
                // 获取物理路径用于文件树浏览
                val path = treeUri.path
                // 尝试从URI提取真实路径
                val realPath = try {
                    // /tree/primary:xxx -> /storage/emulated/0/xxx
                    val segments = path?.split(":")
                    if (segments != null && segments.size >= 2) {
                        "/storage/emulated/0/${segments.last()}"
                    } else path ?: ""
                } catch (_: Exception) { path ?: "" }
                projectPath = realPath
                showSidebar = true

                // 同时打开目录中的文本文件
                val treeDocId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
                val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId)
                val cursor = context.contentResolver.query(childrenUri, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val mimeIndex = c.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val docIdIndex = c.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    while (c.moveToNext()) {
                        val name = if (nameIndex >= 0) c.getString(nameIndex) else continue
                        val mime = if (mimeIndex >= 0) c.getString(mimeIndex) else ""
                        val docId = if (docIdIndex >= 0) c.getString(docIdIndex) else continue
                        if (mime.startsWith("text/") || mime.isEmpty() || (name.contains(".") && !mime.startsWith("image/") && !mime.startsWith("video/") && !mime.startsWith("audio/"))) {
                            try {
                                val docUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                                context.contentResolver.openInputStream(docUri)?.use { inputStream ->
                                    openFile(name, docUri.toString(), inputStream.bufferedReader().readText())
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val saveFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/*")) { uri ->
        uri?.let {
            activeFile?.let { file ->
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream -> outputStream.write(file.content.toByteArray()) }
                } catch (_: Exception) {}
            }
        }
    }

    if (openFiles.isEmpty()) {
        WelcomeScreen(
            onOpenProject = { projectLauncher.launch(null) },
            onOpenFile = { fileLauncher.launch(arrayOf("text/*")) },
            recentFiles = recentManager.getRecentFiles(),
            onRecentSelected = { path, name ->
                File(path).readText().let { content -> openFile(name, path, content) }
            }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppSettings.editorBgColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { showMenu = !showMenu },
                        onLongPress = { showCommandPalette = !showCommandPalette }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 主内容区域
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // 文件浏览器侧边栏
                    if (showSidebar) {
                        FileBrowserPanel(
                            projectPath = projectPath,
                            onFileSelected = { name, path, content ->
                                openFile(name, path, content)
                            },
                            modifier = Modifier.width(200.dp).align(Alignment.CenterStart)
                        )
                    }
                
                    // 编辑器主区域
                    Box(modifier = Modifier.fillMaxSize().background(AppSettings.editorBgColor)) {
                        activeFile?.let { file ->
                            CodeEditor(
                                editorFile = file, 
                                onContentChange = { newContent ->
                                    openFiles = openFiles.map { if (it == file) it.copy(content = newContent) else it }
                                }, 
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                
                    // AI 助手面板（覆盖在编辑器上方，实现半透明效果）
                    if (showAIChat) {
                        AIAssistantPanel(
                            viewModel = aiViewModel,
                            currentFileContent = activeFile?.content,
                            currentFilePath = activeFile?.path,
                            onShowSettings = { showAISettings = true }
                        )
                    }
                }
            
                // 底部标签栏和 AI 切换按钮
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    FluidTabBar(
                        openFiles = openFiles, 
                        activeFileIndex = activeFileIndex,
                        onTabClick = { activeFileIndex = it },
                        onTabClose = { index ->
                            openFiles = openFiles.toMutableList().apply { removeAt(index) }
                            if (activeFileIndex >= openFiles.size) activeFileIndex = (openFiles.size - 1).coerceAtLeast(-1)
                        },
                        onSettingsClick = { showSettings = true }
                    )
                
                    Spacer(modifier = Modifier.weight(1f))
                
                    // 侧边栏切换按钮
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (showSidebar) Color(0xFF666666) else Color(0xFF999999).copy(alpha = 0.8f))
                            .clickable { showSidebar = !showSidebar },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Menu, 
                            "侧边栏", 
                            modifier = Modifier.size(14.dp), 
                            tint = Color.White
                        )
                    }
                
                    Spacer(modifier = Modifier.width(6.dp))
                
                    // AI 切换按钮
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(if (showAIChat) Color(0xFF6344CF) else Color(0xFF333333).copy(alpha = 0.8f))
                            .clickable { showAIChat = !showAIChat },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoFixHigh, 
                            "AI助手", 
                            modifier = Modifier.size(14.dp), 
                            tint = Color.White
                        )
                    }
                }
            }

            if (showMenu) {
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color.White).border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(text = { Text("打开文件...", fontSize = 13.sp) }, onClick = { showMenu = false; fileLauncher.launch(arrayOf("text/*")) })
                    DropdownMenuItem(text = { Text("打开项目...", fontSize = 13.sp) }, onClick = { showMenu = false; projectLauncher.launch(null) })
                    Divider()
                    DropdownMenuItem(text = { Text("保存", fontSize = 13.sp) }, onClick = { showMenu = false; activeFile?.let { saveFileLauncher.launch(it.name) } })
                    DropdownMenuItem(text = { Text("另存为...", fontSize = 13.sp) }, onClick = { showMenu = false; activeFile?.let { saveFileLauncher.launch(it.name) } })
                    Divider()
                    DropdownMenuItem(text = { Text(if (showSidebar) "隐藏侧边栏" else "显示侧边栏", fontSize = 13.sp) }, onClick = { showMenu = false; showSidebar = !showSidebar })
                    DropdownMenuItem(text = { Text(if (showAIChat) "隐藏 AI" else "显示 AI", fontSize = 13.sp) }, onClick = { showMenu = false; showAIChat = !showAIChat })
                    DropdownMenuItem(text = { Text("设置", fontSize = 13.sp) }, onClick = { showMenu = false; showSettings = true })
                    Divider()
                    DropdownMenuItem(text = { Text("关闭文件", fontSize = 13.sp) }, onClick = {
                        showMenu = false
                        if (activeFileIndex >= 0) {
                            openFiles = openFiles.toMutableList().apply { removeAt(activeFileIndex) }
                            activeFileIndex = if (openFiles.isEmpty()) -1 else (activeFileIndex - 1).coerceAtLeast(0)
                        }
                    })
                }
            }
        }
    }

    if (showCommandPalette) {
        CommandPaletteDialog(
            onDismiss = { showCommandPalette = false },
            onOpenFile = { showCommandPalette = false; fileLauncher.launch(arrayOf("text/*")) },
            onOpenProject = { showCommandPalette = false; projectLauncher.launch(null) },
            onToggleSidebar = { showCommandPalette = false; showSidebar = !showSidebar },
            onSettings = { showCommandPalette = false; showSettings = true },
            onSave = { showCommandPalette = false; activeFile?.let { saveFileLauncher.launch(it.name) } }
        )
    }

            if (showSettings) {
        SettingsDialog(onDismiss = { showSettings = false })
    }
    
    if (showAISettings) {
        AISettingsDialog(
            viewModel = aiViewModel,
            onDismiss = { showAISettings = false }
        )
    }
}

@Composable
private fun FluidTabBar(
    openFiles: List<EditorFile>, activeFileIndex: Int,
    onTabClick: (Int) -> Unit, onTabClose: (Int) -> Unit, onSettingsClick: () -> Unit
) {
    val fileCount = openFiles.size
    val targetFileWidth = when {
        fileCount == 0 -> 0.dp
        fileCount == 1 -> 120.dp
        else -> (120 + (fileCount - 1) * 100).coerceAtMost(500).dp
    }
    val animatedFileWidth by animateDpAsState(targetValue = targetFileWidth, animationSpec = tween(400), label = "fw")
    val targetHeight = 26.dp
    val animatedHeight by animateDpAsState(targetValue = targetHeight, animationSpec = tween(300), label = "h")
    val targetRadius = animatedHeight / 2
    val animatedRadius by animateDpAsState(targetValue = targetRadius, animationSpec = tween(300), label = "r")

    val glassBg = if (AppSettings.glassEnabled) {
        AppSettings.glassColor.copy(alpha = AppSettings.glassAlpha)
    } else {
        AppSettings.glassColor
    }
    val glassBorder = if (AppSettings.glassEnabled) {
        Color(0xFFD0D0D0).copy(alpha = AppSettings.glassAlpha * 0.7f)
    } else {
        Color(0xFFD0D0D0)
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(Color.Transparent).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (fileCount > 0) {
            Box(
                modifier = Modifier.width(animatedFileWidth).height(animatedHeight)
                    .clip(RoundedCornerShape(animatedRadius))
                    .background(glassBg)
                    .border(1.dp, glassBorder, RoundedCornerShape(animatedRadius))
            ) {
                if (fileCount == 1) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, null, modifier = Modifier.size(12.dp), tint = Color(0xFF666666))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(openFiles[0].name, style = TextStyle(fontSize = 11.sp, color = Color(0xFF333333), fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(12.dp).clickable { onTabClose(0) }, tint = Color(0xFF999999))
                    }
                } else if (fileCount > 1) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        openFiles.forEachIndexed { index, file ->
                            val isActive = index == activeFileIndex
                            Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp))
                                .background(if (isActive) Color.White.copy(alpha = 0.9f) else Color.Transparent)
                                .clickable { onTabClick(index) }.padding(horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Text(file.name, style = TextStyle(fontSize = 10.sp, color = if (isActive) Color.Black else Color(0xFF888888), fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal),
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(10.dp).clickable { onTabClose(index) }, tint = if (isActive) Color(0xFF666666) else Color(0xFFBBBBBB))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
        }

        Box(modifier = Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFF333333).copy(alpha = 0.8f)).clickable { onSettingsClick() },
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Settings, "设置", modifier = Modifier.size(14.dp), tint = Color.White)
        }
    }
}

@Composable
private fun CommandPaletteDialog(
    onDismiss: () -> Unit, onOpenFile: () -> Unit, onOpenProject: () -> Unit,
    onToggleSidebar: () -> Unit, onSettings: () -> Unit, onSave: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.White,
        title = { Text("命令面板", color = Color.Black, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CommandItem(Icons.Default.Description, "打开文件", onOpenFile)
                CommandItem(Icons.Default.FolderOpen, "打开项目", onOpenProject)
                CommandItem(Icons.Default.Save, "保存文件", onSave)
                CommandItem(Icons.Default.Menu, "切换侧边栏", onToggleSidebar)
                CommandItem(Icons.Default.Settings, "设置", onSettings)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭", color = Color(0xFF999999)) } }
    )
}

@Composable
private fun CommandItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick)
        .background(Color(0xFFF5F5F5)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = Color(0xFF333333))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = TextStyle(fontSize = 14.sp, color = Color.Black))
    }
}

@Composable
private fun WelcomeScreen(
    onOpenProject: () -> Unit, onOpenFile: () -> Unit,
    recentFiles: List<RecentFile>, onRecentSelected: (String, String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(80.dp).border(3.dp, Color.Black, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Code, null, modifier = Modifier.size(48.dp), tint = Color.Black)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("MedeMini", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.Black))
        Text("Android Code Editor", style = TextStyle(fontSize = 14.sp, color = Color.Black.copy(0.6f)))
        Spacer(modifier = Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onOpenProject, colors = ButtonDefaults.buttonColors(containerColor = Color.Black), shape = RoundedCornerShape(12.dp), modifier = Modifier.height(52.dp)) {
                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.padding(end = 8.dp), tint = Color.White)
                Text("打开项目", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
            }
            OutlinedButton(onClick = onOpenFile, shape = RoundedCornerShape(12.dp), modifier = Modifier.height(52.dp), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color.Black)) {
                Icon(Icons.Default.Description, null, modifier = Modifier.padding(end = 8.dp))
                Text("打开文件", fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (recentFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(48.dp))
            Text("最近文件", style = TextStyle(fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.SemiBold))
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recentFiles.take(8), key = { it.path }) { file ->
                    Card(modifier = Modifier.fillMaxWidth().clickable { onRecentSelected(file.path, file.name) }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).border(1.5.dp, Color.Black, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Description, null, tint = Color.Black, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(file.name, style = TextStyle(fontSize = 15.sp, color = Color.Black), modifier = Modifier.weight(1f), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsDialog(onDismiss: () -> Unit) {
    var settingsPage by remember { mutableStateOf("main") }

    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (settingsPage != "main") {
                    Icon(Icons.Default.ArrowBack, "返回", modifier = Modifier.size(20.dp).clickable { settingsPage = "main" }, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(when(settingsPage) { "glass" -> "毛玻璃设置"; "highlight" -> "高亮配色"; "editor" -> "编辑器设置"; else -> "设置" },
                    color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            when (settingsPage) {
                "main" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingsNavItem(Icons.Default.Fingerprint, "快捷手势", subtitle = "双击→菜单 | 长按→命令面板")
                    SettingsNavItem(Icons.Default.BlurOn, "毛玻璃设置", onClick = { settingsPage = "glass" })
                    SettingsNavItem(Icons.Default.Palette, "高亮配色", onClick = { settingsPage = "highlight" })
                    SettingsNavItem(Icons.Default.Edit, "编辑器设置", onClick = { settingsPage = "editor" })
                }
                "glass" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("启用毛玻璃", fontSize = 14.sp, color = Color.Black)
                        Switch(checked = AppSettings.glassEnabled, onCheckedChange = { AppSettings.glassEnabled = it }, colors = SwitchDefaults.colors(checkedTrackColor = Color.Black))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("透明度", fontSize = 14.sp, color = Color.Black)
                        Slider(value = AppSettings.glassAlpha, onValueChange = { AppSettings.glassAlpha = it }, valueRange = 0.1f..1f, steps = 9, colors = SliderDefaults.colors(activeTrackColor = Color.Black))
                    }
                    ColorRow("毛玻璃配色", AppSettings.glassColor) { AppSettings.glassColor = it }
                }
                "highlight" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorRow("关键字", AppSettings.keywordColor) { AppSettings.keywordColor = it }
                    ColorRow("字符串", AppSettings.stringColor) { AppSettings.stringColor = it }
                    ColorRow("注释", AppSettings.commentColor) { AppSettings.commentColor = it }
                    ColorRow("数字", AppSettings.numberColor) { AppSettings.numberColor = it }
                    ColorRow("函数", AppSettings.functionColor) { AppSettings.functionColor = it }
                    ColorRow("类型", AppSettings.typeColor) { AppSettings.typeColor = it }
                }
                "editor" -> Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ColorRow("编辑器背景", AppSettings.editorBgColor) { AppSettings.editorBgColor = it }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text("完成", color = Color.White) } }
    )
}

@Composable
private fun SettingsNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, subtitle: String? = null, onClick: (() -> Unit)? = null) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .background(Color(0xFFF5F5F5)).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = Color(0xFF333333))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = Color.Black)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color(0xFF999999))
        }
        if (onClick != null) Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(18.dp), tint = Color(0xFF999999))
    }
}

@Composable
private fun ColorRow(label: String, currentColor: Color, onColorChange: (Color) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp, color = Color.Black)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Color(0xFF6344CF), Color(0xFF1A997F), Color(0xFFAC6636), Color(0xFF2C72D5), Color(0xFFC084FC), Color(0xFFF59E0B), Color.Black, Color(0xFFE53935), Color(0xFF43A047)).forEach { color ->
                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(12.dp)).background(color)
                    .border(if (currentColor == color) 2.dp else 0.dp, Color.White, RoundedCornerShape(12.dp))
                    .clickable { onColorChange(color) }
                )
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try { if (cursor != null && cursor.moveToFirst()) { val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (index != -1) result = cursor.getString(index) } }
        finally { cursor?.close() }
    }
    return result ?: uri.path?.substringAfterLast('/')
}