package com.medemini.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Command palette entry
 */
data class CommandEntry(
    val id: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val shortcut: String = "",
    val action: () -> Unit
)

/**
 * Command palette state
 */
class CommandPaletteState(
    private val commands: List<CommandEntry>,
    onCommandSelected: (CommandEntry) -> Unit
) {
    var isOpen by mutableStateOf(false)
        private set
    
    var query by mutableStateOf("")
        private set
    
    private val filteredCommands: List<CommandEntry>
        get() {
            if (query.isBlank()) return commands
            val q = query.lowercase()
            return commands.filter {
                it.label.lowercase().contains(q) || 
                it.description.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
            }
        }
    
    fun open() {
        isOpen = true
        query = ""
    }
    
    fun close() {
        isOpen = false
        query = ""
    }
    
    fun updateQuery(newQuery: String) {
        query = newQuery
    }
    
    val results: List<CommandEntry>
        get() = filteredCommands
    
    val isEmpty: Boolean
        get() = filteredCommands.isEmpty() && query.isNotEmpty()
}

/**
 * Create default command palette with common commands
 */
@Composable
fun rememberCommandPalette(
    onOpenSearch: () -> Unit,
    onToggleSidebar: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenFile: () -> Unit,
    onSaveFile: () -> Unit,
    onClearAll: () -> Unit
): CommandPaletteState {
    val commands = remember {
        listOf(
            CommandEntry(
                id = "search",
                label = "搜索和替换",
                description = "在文件中搜索和替换文本",
                icon = Icons.Default.Search,
                shortcut = "Ctrl+F"
            ) { onOpenSearch() }
            ,
            CommandEntry(
                id = "toggle_sidebar",
                label = "切换侧边栏",
                description = "显示或隐藏文件树侧边栏",
                icon = Icons.Default.Menu,
                shortcut = "Ctrl+B"
            ) { onToggleSidebar() }
            ,
            CommandEntry(
                id = "toggle_theme",
                label = "切换主题",
                description = "切换深色/浅色模式",
                icon = Icons.Default.DarkMode,
                shortcut = "Ctrl+T"
            ) { onToggleTheme() }
            ,
            CommandEntry(
                id = "open_file",
                label = "打开文件",
                description = "从设备打开代码文件",
                icon = Icons.Default.OpenInNew
            ) { onOpenFile() }
            ,
            CommandEntry(
                id = "save_file",
                label = "保存文件",
                description = "保存当前编辑的文件",
                icon = Icons.Default.Save,
                shortcut = "Ctrl+S"
            ) { onSaveFile() }
            ,
            CommandEntry(
                id = "clear_all",
                label = "清除所有内容",
                description = "清除编辑器所有内容",
                icon = Icons.Default.Delete
            ) { onClearAll() }
        )
    }
    
    return remember { CommandPaletteState(commands, {}) }
}

/**
 * Command palette dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommandPalette(
    state: CommandPaletteState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.width(500.dp),
        title = {
            OutlinedTextField(
                value = state.query,
                onValueChange = state::updateQuery,
                placeholder = { Text("输入命令...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true
            )
        },
        text = {
            if (state.isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "未找到匹配的命令",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(state.results, key = { it.id }) { command ->
                        CommandItem(command = command)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

@Composable
private fun CommandItem(command: CommandEntry) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                command.action()
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                command.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = command.label,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = command.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (command.shortcut.isNotEmpty()) {
                Text(
                    text = command.shortcut,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}