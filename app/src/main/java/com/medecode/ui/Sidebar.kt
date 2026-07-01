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

/**
 * Sidebar state for managing expanded folders
 */
data class SidebarState(
    val expandedFolders: Set<String> = setOf(),
    val selectedPath: String? = null
)

/**
 * File tree node for recursive file structure
 */
data class FileTreeNode(
    val file: File,
    val children: List<FileTreeNode> = emptyList(),
    val isDirectory: Boolean = file.isDirectory
) {
    val name: String
        get() = file.name
    val path: String
        get() = file.absolutePath
}

/**
 * Build file tree from directory
 */
fun buildFileTree(directory: File): List<FileTreeNode> {
    if (!directory.exists() || !directory.isDirectory) return emptyList()
    
    return directory.listFiles()?.filter { 
        it.name != "." && it.name != ".." 
    }?.map { file ->
        FileTreeNode(
            file = file,
            children = if (file.isDirectory) buildFileTree(file) else emptyList(),
            isDirectory = file.isDirectory
        )
    }?.sortedWith(compareBy(
        { !it.isDirectory },
        { it.name.lowercase() }
    )) ?: emptyList()
}

/**
 * Recursively expand folders in tree
 */
fun expandTree(trees: List<FileTreeNode>, depth: Int = 0): List<FileTreeNode> {
    return trees.map { node ->
        if (node.isDirectory) {
            node.copy(
                children = expandTree(node.children, depth + 1)
            )
        } else {
            node
        }
    }
}

/**
 * Get all file paths in tree
 */
fun getAllFilePaths(trees: List<FileTreeNode>): Set<String> {
    return trees.flatMap { node ->
        if (node.isDirectory) {
            listOf(node.path) + getAllFilePaths(node.children)
        } else {
            listOf(node.path)
        }
    }.toSet()
}

/**
 * Find node by path
 */
fun findNodeByPath(trees: List<FileTreeNode>, path: String): FileTreeNode? {
    for (node in trees) {
        if (node.path == path) return node
        node.children.forEach { child ->
            findNodeByPath(listOf(child), path)?.let { return it }
        }
    }
    return null
}

/**
 * Icon for file type
 */
fun getFileIcon(name: String): ImageVector {
    val ext = name.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.Android
        "py" -> Icons.Default.Description
        "js", "ts" -> Icons.Default.Javascript
        "json" -> Icons.Default.Database
        "xml" -> Icons.Default.Code
        "yaml", "yml" -> Icons.Default.Settings
        "gradle" -> Icons.Default.Build
        "html" -> Icons.Default.Language
        "css" -> Icons.Default.ColorLens
        "md" -> Icons.Default.Article
        "gitignore", "gitkeep" -> Icons.Default.Git
        "sh" -> Icons.Default.Terminal
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Icons.Default.Image
        "mp3", "wav", "ogg" -> Icons.Default.MusicNote
        "mp4", "avi", "mkv" -> Icons.Default.VideoLibrary
        else -> Icons.Default.InsertDriveFile
    }
}

/**
 * Sidebar composable with file tree
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sidebar(
    projectPath: String = "",
    onFileSelected: (String, String) -> Unit, // path, name
    onProjectPathChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    sidebarWidth: Dp = 240.dp
) {
    var expandedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedPath by remember { mutableStateOf<String?>(null) }
    var fileTree by remember { mutableStateOf<List<FileTreeNode>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    
    LaunchedEffect(projectPath) {
        if (projectPath.isNotEmpty()) {
            isLoading = true
            val dir = File(projectPath)
            fileTree = buildFileTree(dir)
            isLoading = false
            
            // Auto-expand first level
            expandedFolders = fileTree.filter { it.isDirectory }.map { it.path }.toSet()
        }
    }
    
    Column(modifier = modifier.width(sidebarWidth)) {
        // Project path selector
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "资源管理器",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (projectPath.isNotEmpty()) projectPath else "未选择项目",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    IconButton(
                        onClick = { 
                            // Reset project
                            onProjectPathChange("")
                            expandedFolders = emptySet()
                            fileTree = emptyList()
                            selectedPath = null
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        // File tree
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            if (projectPath.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "请选择项目目录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else if (fileTree.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "空目录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    items(fileTree, key = { it.path }) { node ->
                        FileTreeNodeItem(
                            node = node,
                            expandedFolders = expandedFolders,
                            onToggleExpanded = { path ->
                                expandedFolders = if (expandedFolders.contains(path)) {
                                    expandedFolders - path
                                } else {
                                    expandedFolders + path
                                }
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
}

@Composable
private fun FileTreeNodeItem(
    node: FileTreeNode,
    expandedFolders: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onFileSelected: (String, String) -> Unit,
    currentSelected: String?
) {
    val isExpanded = expandedFolders.contains(node.path)
    val isSelected = currentSelected == node.path
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isDirectory) {
                    onToggleExpanded(node.path)
                } else {
                    onFileSelected(node.path, node.name)
                }
            }
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (isSelected) {
                    Modifier.background(
                        MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    )
                } else {
                    Modifier
                }
            )
    ) {
        // Expand/Collapse icon for directories
        if (node.isDirectory) {
            val icon = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ExpandMore
            IconButton(
                onClick = { onToggleExpanded(node.path) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
        
        // File/Folder icon
        Icon(
            imageVector = if (node.isDirectory) Icons.Default.Folder else getFileIcon(node.name),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (node.isDirectory) 
                MaterialTheme.colorScheme.primary 
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // File/Folder name
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp),
            maxLines = 1
        )
    }
    
    // Render children if directory is expanded
    if (node.isDirectory && isExpanded && node.children.isNotEmpty()) {
        node.children.forEach { child ->
            FileTreeNodeItem(
                node = child,
                expandedFolders = expandedFolders,
                onToggleExpanded = onToggleExpanded,
                onFileSelected = onFileSelected,
                currentSelected = currentSelected
            )
        }
    }
}