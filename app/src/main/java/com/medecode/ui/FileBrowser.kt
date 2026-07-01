package com.medecode.ui

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
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * File browser state management
 */
data class FileBrowserState(
    val currentPath: String = "/",
    val files: List<FileInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class FileInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long = 0L,
    val lastModified: Long = 0L
)

@Composable
fun rememberFileBrowser(): FileBrowserState {
    return remember { FileBrowserState() }
}

/**
 * File browser composable for browsing device files
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowser(
    initialPath: String = "/",
    onFileSelected: (String, String) -> Unit, // path, content
    onFolderSelected: (String) -> Unit, // path
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(FileBrowserState(currentPath = initialPath)) }
    var showOptions by remember { mutableStateOf(false) }
    var selectedFile: FileInfo? by remember { mutableStateOf(null) }
    
    LaunchedEffect(state.currentPath) {
        state = state.copy(isLoading = true, error = null)
        val result = loadDirectory(state.currentPath)
        state = state.copy(
            files = result.files,
            isLoading = false,
            error = result.error
        )
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("文件浏览器")
                IconButton(onClick = {
                    if (state.currentPath != "/") {
                        state = state.copy(currentPath = File(state.currentPath).parent ?: "/")
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, "返回上级")
                }
            }
        },
        text = {
            Column(modifier = modifier) {
                // Current path display
                Text(
                    text = "路径: $state.currentPath",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                state.error?.let { error ->
                    Text(
                        text = "错误: $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Parent directory entry (if not root)
                        if (state.currentPath != "/") {
                            item {
                                FileInfoRow(
                                    info = FileInfo(
                                        name = "..",
                                        path = File(state.currentPath).parent ?: "/",
                                        isDirectory = true
                                    ),
                                    onClick = {
                                        state = state.copy(currentPath = File(state.currentPath).parent ?: "/")
                                    }
                                )
                            }
                        }
                        
                        items(state.files, key = { it.path }) { file ->
                            FileInfoRow(
                                info = file,
                                onClick = {
                                    if (file.isDirectory) {
                                        state = state.copy(currentPath = file.path)
                                    } else {
                                        selectedFile = file
                                        showOptions = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
    
    // File options dialog
    if (showOptions && selectedFile != null) {
        AlertDialog(
            onDismissRequest = { 
                showOptions = false
                selectedFile = null
            },
            title = { Text("选择操作") },
            text = {
                Column {
                    Text("文件: ${selectedFile!!.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "路径: ${selectedFile!!.path}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedFile?.let { file ->
                        val content = readFileSync(file.path)
                        if (content != null) {
                            onFileSelected(file.path, file.name)
                        }
                        showOptions = false
                        selectedFile = null
                    }
                }) {
                    Text("在编辑器中打开")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showOptions = false
                    selectedFile = null
                }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun FileInfoRow(
    info: FileInfo,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = if (info.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (info.isDirectory) 
                MaterialTheme.colorScheme.primary 
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = info.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        if (info.isDirectory) {
            Text(
                text = "${listOfFilesInDirectory(info.path).size} 项",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = formatFileSize(info.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun loadDirectory(path: String): Result<FileBrowserState> {
    return try {
        val file = File(path)
        if (!file.exists() || !file.isDirectory) {
            return Result.failure(Exception("目录不存在"))
        }
        
        val entries = file.listFiles()?.filter { 
            it.name != "." && it.name != ".."
        }?.map { entry ->
            FileInfo(
                name = entry.name,
                path = entry.absolutePath,
                isDirectory = entry.isDirectory,
                size = entry.length(),
                lastModified = entry.lastModified()
            )
        }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            ?: emptyList()
        
        Result.success(FileBrowserState(
            currentPath = path,
            files = entries ?: emptyList()
        ))
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private fun listOfFilesInDirectory(path: String): List<String> {
    return try {
        File(path).listFiles()?.map { it.name } ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

private fun readFileSync(path: String): String? {
    return try {
        File(path).readText()
    } catch (e: Exception) {
        null
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
        else -> "${size / (1024 * 1024 * 1024)} GB"
    }
}