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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Recently opened file entry
 */
data class RecentFile(
    val path: String,
    val name: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Recently files manager
 */
class RecentFileManager {
    companion object {
        private const val MAX_RECENT_FILES = 10
        private const val PREFS_NAME = "recent_files_prefs"
        private const val KEY_RECENT_FILES = "recent_files"
    }
    
    private var recentFiles: MutableList<RecentFile> = mutableListOf()
    
    init {
        // In a real app, this would load from SharedPreferences
        // For now, start empty
    }
    
    fun addFile(path: String, name: String) {
        // Remove if already exists
        recentFiles.removeAll { it.path == path }
        
        // Add to beginning
        recentFiles.add(0, RecentFile(path, name))
        
        // Keep only MAX_RECENT_FILES
        if (recentFiles.size > MAX_RECENT_FILES) {
            recentFiles = recentFiles.subList(0, MAX_RECENT_FILES).toMutableList()
        }
    }
    
    fun getRecentFiles(): List<RecentFile> {
        return recentFiles.toList()
    }
    
    fun clearAll() {
        recentFiles.clear()
    }
    
    fun removeFile(path: String) {
        recentFiles.removeAll { it.path == path }
    }
}

/**
 * Create a remember instance of RecentFileManager
 */
@Composable
fun rememberRecentFileManager(): RecentFileManager {
    return remember { RecentFileManager() }
}

/**
 * Icon for file type based on extension
 */
fun getFileIconForRecent(name: String): ImageVector {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.Android
        "py" -> Icons.Default.Description
        "js", "ts" -> Icons.Default.Javascript
        "json" -> Icons.Default.Storage
        "xml" -> Icons.Default.Code
        "yaml", "yml" -> Icons.Default.Settings
        "gradle" -> Icons.Default.Build
        "html" -> Icons.Default.Language
        "css" -> Icons.Default.ColorLens
        "md" -> Icons.Default.Article
        "sh" -> Icons.Default.Terminal
        "png", "jpg", "jpeg", "gif", "svg" -> Icons.Default.Image
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * Format timestamp to readable string
 */
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
        else -> {
            val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
            val month = cal.get(Calendar.MONTH) + 1
            val day = cal.get(Calendar.DAY_OF_MONTH)
            "${month}月${day}日"
        }
    }
}

/**
 * Recent files panel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentFilesPanel(
    recentManager: RecentFileManager,
    onFileSelected: (String, String) -> Unit,
    onRemove: (String) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val recentFiles = recentManager.getRecentFiles()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("最近打开")
                if (recentFiles.isNotEmpty()) {
                    TextButton(onClick = onClearAll) {
                        Text("清空")
                    }
                }
            }
        },
        text = {
            if (recentFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "还没有打开过文件",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(recentFiles, key = { it.path }) { file ->
                        RecentFileItem(
                            file = file,
                            onClick = { onFileSelected(file.path, file.name) },
                            onRemove = { onRemove(file.path) }
                        )
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
private fun RecentFileItem(
    file: RecentFile,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            getFileIconForRecent(file.name),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
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
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "移除",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}