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
 * Safe root path for file browsing on Android.
 * Apps cannot read the system root `/` due to SELinux, so we start from public Download.
 */
private const val DEFAULT_ROOT_PATH = "/sdcard/Download"

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
    initialPath: String = DEFAULT_ROOT_PATH,
    onFileSelected: (String, String) -> Unit, // path, name
    onFolderSelected: (String) -> Unit, // path
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var state by remember { mutableStateOf(FileBrowserState(currentPath = initialPath)) }

    LaunchedEffect(state.currentPath) {
        state = state.copy(isLoading = true, error = null)
        val loadResult = loadDirectory(state.currentPath)
        state = state.copy(
            files = loadResult.getOrElse { emptyList() },
            isLoading = false,
            error = loadResult.exceptionOrNull()?.message
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
                Row {
                    TextButton(onClick = {
                        onFolderSelected(state.currentPath)
                        onDismiss()
                    }) {
                        Text("选择目录")
                    }
                    IconButton(onClick = {
                        val parent = File(state.currentPath).parent
                        state = state.copy(currentPath = if (parent.isNullOrBlank() || parent == "/") DEFAULT_ROOT_PATH else parent)
                    }) {
                        Icon(Icons.Default.ArrowBack, "返回上级")
                    }
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
                        // Parent directory entry (if not at safe root)
                        if (state.currentPath != DEFAULT_ROOT_PATH) {
                            item {
                                FileInfoRow(
                                    info = FileInfo(
                                        name = "..",
                                        path = File(state.currentPath).parent ?: DEFAULT_ROOT_PATH,
                                        isDirectory = true
                                    ),
                                    onClick = {
                                        val parent = File(state.currentPath).parent
                                        state = state.copy(currentPath = if (parent.isNullOrBlank() || parent == "/") DEFAULT_ROOT_PATH else parent)
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
                                        readFileSync(file.path)?.let { content ->
                                            onFileSelected(file.path, file.name)
                                            onDismiss()
                                        }
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

private fun loadDirectory(path: String): Result<List<FileInfo>> {
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

        Result.success(entries)
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