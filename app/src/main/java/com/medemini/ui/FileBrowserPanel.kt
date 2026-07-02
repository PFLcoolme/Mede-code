package com.medemini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/**
 * 文件浏览器侧边栏面板
 */
@Composable
fun FileBrowserPanel(
    projectPath: String,
    onFileSelected: (String, String, String) -> Unit, // name, path, content
    modifier: Modifier = Modifier.width(200.dp)
) {
    var currentPath by remember { mutableStateOf(if (projectPath.isNotEmpty() && File(projectPath).exists()) projectPath else "/sdcard/Download") }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var expandedFolder by remember { mutableStateOf<String?>(null) }

    // 加载目录内容
    LaunchedEffect(currentPath) {
        val dir = File(currentPath)
        files = if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { it.exists() }?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } else {
            emptyList()
        }
        // 重置展开状态
        expandedFolder = null
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(Color(0xFFF5F5F5))
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, "文件浏览器", modifier = Modifier.size(16.dp), tint = Color(0xFF666666))
            Spacer(modifier = Modifier.width(6.dp))
            Text("文件", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black))
        }

        // 路径栏
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ArrowBack, "返回上级", 
                    modifier = Modifier.size(16.dp).clickable {
                        currentPath = File(currentPath).parentFile?.absolutePath ?: "/"
                    }, 
                    tint = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    currentPath.split("/").lastOrNull { it.isNotEmpty() } ?: "/",
                    style = TextStyle(fontSize = 10.sp, color = Color(0xFF888888)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 文件列表
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(files, key = { it.path }) { file ->
                FileItemRow(
                    file = file,
                    isExpanded = expandedFolder == file.path,
                    onClick = {
                        if (file.isDirectory) {
                            expandedFolder = if (expandedFolder == file.path) null else file.path
                            currentPath = file.path
                        } else {
                            // 读取文件内容
                            try {
                                val content = File(file.path).readText()
                                onFileSelected(file.name, file.path, content)
                            } catch (_: Exception) {}
                        }
                    },
                    onExpandToggle = {
                        expandedFolder = if (expandedFolder == file.path) null else file.path
                    }
                )
            }
        }
    }
}

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L
)

@Composable
private fun FileItemRow(
    file: FileItem,
    isExpanded: Boolean,
    onClick: () -> Unit,
    onExpandToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (file.isDirectory) Color(0xFFEEEEEE) else Color.White
        ),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (file.isDirectory) Color(0xFF1976D2) else Color(0xFF666666)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    file.name,
                    style = TextStyle(fontSize = 11.sp, color = Color.Black),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (file.isDirectory) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        if (isExpanded) "收起" else "展开",
                        modifier = Modifier.size(14.dp).clickable(enabled = true, onClick = onExpandToggle),
                        tint = Color(0xFF999999)
                    )
                }
            }
            // 展开子文件夹
            if (isExpanded && file.isDirectory) {
                SubFiles(
                    folderPath = file.path,
                    onFileSelected = onClick,
                    onSubExpand = onExpandToggle
                )
            }
        }
    }
}

@Composable
private fun SubFiles(
    folderPath: String,
    onFileSelected: () -> Unit,
    onSubExpand: () -> Unit
) {
    var subFiles by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    
    LaunchedEffect(folderPath) {
        val dir = File(folderPath)
        subFiles = if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.filter { it.exists() }?.map { file ->
                FileItem(
                    name = file.name,
                    path = file.absolutePath,
                    isDirectory = file.isDirectory,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
        } else {
            emptyList()
        }
    }

    subFiles.forEach { file ->
        Row(
            modifier = Modifier.fillMaxWidth()
                .padding(start = 24.dp, top = 2.dp, bottom = 2.dp, end = 8.dp)
                .clickable(enabled = true, onClick = onFileSelected),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (file.isDirectory) Color(0xFF1976D2) else Color(0xFF666666)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                file.name,
                style = TextStyle(fontSize = 10.sp, color = Color(0xFF555555)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}