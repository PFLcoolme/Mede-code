package com.medemini.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class FileTreeNode(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val children: List<FileTreeNode> = emptyList()
)

fun buildFileTree(directory: File, depth: Int = 0): List<FileTreeNode> {
    if (!directory.exists() || !directory.isDirectory) {
        Log.w("Sidebar", "Directory not accessible: ${directory.absolutePath}, exists=${directory.exists()}, isDir=${directory.isDirectory}")
        return emptyList()
    }
    val files = directory.listFiles()
    if (files == null) {
        Log.w("Sidebar", "listFiles() returned null for: ${directory.absolutePath} - likely no permission")
        return emptyList()
    }
    return files.filter {
        it.name != "." && it.name != ".." && !it.name.startsWith(".")
    }.map { file ->
        FileTreeNode(
            file = file,
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            children = if (file.isDirectory && depth < 10) buildFileTree(file, depth + 1) else emptyList()
        )
    }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
}

// 文件类型图标颜色 - 每种文件类型有独特的颜色
data class FileIconStyle(
    val icon: ImageVector,
    val color: Color
)

fun getFileIconStyle(name: String): FileIconStyle {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        // Kotlin - 紫色
        "kt", "kts" -> FileIconStyle(Icons.Default.Code, Color(0xFF7F52FF))
        // Java - 橙红色
        "java" -> FileIconStyle(Icons.Default.Coffee, Color(0xFFE76F00))
        // Python - 蓝黄双色用蓝色
        "py", "pyw" -> FileIconStyle(Icons.Default.SmartToy, Color(0xFF3776AB))
        // JavaScript - 黄色
        "js", "mjs" -> FileIconStyle(Icons.Default.Javascript, Color(0xFFF7DF1E))
        // TypeScript - 蓝色
        "ts", "tsx" -> FileIconStyle(Icons.Default.Code, Color(0xFF3178C6))
        // JSON - 绿色
        "json" -> FileIconStyle(Icons.Default.Storage, Color(0xFF5B9A4A))
        // XML - 橙色
        "xml" -> FileIconStyle(Icons.Default.Code, Color(0xFFE44D26))
        // YAML - 红色
        "yaml", "yml" -> FileIconStyle(Icons.Default.Settings, Color(0xFFCB171E))
        // Gradle - 绿色
        "gradle", "gradle.kts" -> FileIconStyle(Icons.Default.Build, Color(0xFF02303A))
        // HTML - 橙红
        "html", "htm" -> FileIconStyle(Icons.Default.Language, Color(0xFFE44D26))
        // CSS - 蓝色
        "css", "scss", "sass", "less" -> FileIconStyle(Icons.Default.ColorLens, Color(0xFF264DE4))
        // Markdown - 灰蓝
        "md", "mdx" -> FileIconStyle(Icons.Default.Article, Color(0xFF519ABA))
        // Shell - 绿色
        "sh", "bash", "zsh" -> FileIconStyle(Icons.Default.Terminal, Color(0xFF4EAA25))
        // C/C++ - 蓝色
        "c", "h" -> FileIconStyle(Icons.Default.Code, Color(0xFF555555))
        "cpp", "cc", "cxx", "hpp" -> FileIconStyle(Icons.Default.Code, Color(0xFF00599C))
        // Go - 青色
        "go" -> FileIconStyle(Icons.Default.Code, Color(0xFF00ADD8))
        // Rust - 橙色
        "rs" -> FileIconStyle(Icons.Default.Code, Color(0xFFDEA584))
        // Swift - 橙色
        "swift" -> FileIconStyle(Icons.Default.Code, Color(0xFFFA7343))
        // Dart - 蓝色
        "dart" -> FileIconStyle(Icons.Default.Code, Color(0xFF0175C2))
        // 图片 - 粉紫
        "png", "jpg", "jpeg", "gif", "svg", "webp", "ico", "bmp" -> FileIconStyle(Icons.Default.Image, Color(0xFFA076C4))
        // 音频 - 粉色
        "mp3", "wav", "ogg", "flac", "aac" -> FileIconStyle(Icons.Default.MusicNote, Color(0xFFE91E63))
        // 视频 - 红色
        "mp4", "avi", "mkv", "mov", "wmv" -> FileIconStyle(Icons.Default.VideoLibrary, Color(0xFFE53935))
        // 配置文件
        "properties", "conf", "cfg", "ini", "toml" -> FileIconStyle(Icons.Default.Settings, Color(0xFF6D8086))
        // Git
        "gitignore", "gitattributes", "gitmodules" -> FileIconStyle(Icons.Default.Code, Color(0xFFF05032))
        // Docker
        "dockerfile" -> FileIconStyle(Icons.Default.DirectionsBoat, Color(0xFF2496ED))
        // 文本
        "txt", "log" -> FileIconStyle(Icons.Default.Description, Color(0xFF8B9DAF))
        // 默认
        else -> FileIconStyle(Icons.Default.InsertDriveFile, Color(0xFF90A4AE))
    }
}

fun getFolderIcon(isOpen: Boolean): FileIconStyle {
    return if (isOpen) FileIconStyle(Icons.Default.FolderOpen, Color(0xFFE8A838))
    else FileIconStyle(Icons.Default.Folder, Color(0xFFE8A838))
}

@Composable
fun Sidebar(
    projectPath: String = "",
    onFileSelected: (String, String) -> Unit,
    onProjectPathChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = 180.dp
) {
    var expandedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var fileTree by remember { mutableStateOf<List<FileTreeNode>>(emptyList()) }

    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            val dir = File(projectPath)
            Log.d("Sidebar", "Loading project: $projectPath, exists=${dir.exists()}, isDir=${dir.isDirectory}")
            fileTree = buildFileTree(dir)
            Log.d("Sidebar", "Loaded ${fileTree.size} top-level items")
            // 默认展开第一层目录
            expandedFolders = fileTree.filter { it.isDirectory }.map { it.path }.toSet()
        } else {
            fileTree = emptyList()
        }
    }

    Column(modifier = modifier) {
        // 紧凑标题栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(12.dp), tint = Color(0xFF444444))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (projectPath.isNotEmpty()) projectPath.substringAfterLast('/') else "资源管理器",
                style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF222222)),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
            if (projectPath.isNotEmpty()) {
                Icon(Icons.Default.Close, "关闭",
                    modifier = Modifier.size(12.dp).clickable {
                        onProjectPathChange("")
                        expandedFolders = emptySet()
                        fileTree = emptyList()
                        selectedPath = null
                    }, tint = Color(0xFF666666))
            }
        }
        Divider(color = Color.Black.copy(alpha = 0.15f))

        // 文件树 - 使用 weight 填充剩余空间并确保可滚动
        if (projectPath.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("请选择项目目录", style = TextStyle(fontSize = 10.sp, color = Color(0xFF999999)))
            }
        } else if (fileTree.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOff, null, modifier = Modifier.size(24.dp), tint = Color(0xFFCCCCCC))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("无法读取目录", style = TextStyle(fontSize = 10.sp, color = Color(0xFF999999)))
                    Text("请检查存储权限", style = TextStyle(fontSize = 8.sp, color = Color(0xFFBBBBBB)))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 2.dp, horizontal = 2.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(fileTree, key = { it.path }) { node ->
                    FileTreeNodeItem(
                        node = node, depth = 0,
                        expandedFolders = expandedFolders,
                        onToggleExpanded = { path ->
                            expandedFolders = if (expandedFolders.contains(path)) expandedFolders - path else expandedFolders + path
                        },
                        onFileSelected = { path, name ->
                            selectedPath = path
                            onFileSelected(path, name)
                        },
                        currentSelected = selectedPath
                    )
                }
            }
        }
    }
}

@Composable
private fun FileTreeNodeItem(
    node: FileTreeNode,
    depth: Int = 0,
    expandedFolders: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onFileSelected: (String, String) -> Unit,
    currentSelected: String?
) {
    val isExpanded = expandedFolders.contains(node.path)
    val isSelected = currentSelected == node.path
    val indent = (depth * 12).dp

    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .padding(start = indent, end = 2.dp, top = 1.dp, bottom = 1.dp)
                .then(if (isSelected) Modifier.background(Color(0xFF000000).copy(alpha = 0.12f)) else Modifier)
                .clickable {
                    if (node.isDirectory) onToggleExpanded(node.path)
                    else onFileSelected(node.path, node.name)
                }
                .padding(horizontal = 3.dp, vertical = 2.dp)
        ) {
            // 展开/折叠箭头
            if (node.isDirectory) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    null, modifier = Modifier.size(12.dp), tint = Color(0xFF888888)
                )
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Spacer(modifier = Modifier.width(2.dp))
            // 文件/文件夹图标 - 使用个性化颜色
            if (node.isDirectory) {
                val folderStyle = getFolderIcon(isExpanded)
                Icon(folderStyle.icon, null, modifier = Modifier.size(13.dp), tint = folderStyle.color)
            } else {
                val fileStyle = getFileIconStyle(node.name)
                Icon(fileStyle.icon, null, modifier = Modifier.size(13.dp), tint = fileStyle.color)
            }
            Spacer(modifier = Modifier.width(3.dp))
            // 名称
            Text(
                node.name,
                style = TextStyle(fontSize = 9.sp, color = Color(0xFF111111), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium),
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
            )
        }
        // 子节点 - 展开时显示
        if (node.isDirectory && isExpanded && node.children.isNotEmpty()) {
            node.children.forEach { child ->
                FileTreeNodeItem(child, depth + 1, expandedFolders, onToggleExpanded, onFileSelected, currentSelected)
            }
        }
    }
}