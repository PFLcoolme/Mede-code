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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medecode.editor.CodeEditor
import com.medecode.model.EditorFile
import com.medecode.ui.FileBrowser
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
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (activeFile != null) "${activeFile.name} - Medecode" else "Medecode",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { showFileBrowser = true }) {
                        Icon(Icons.Default.FolderOpen, "文件浏览器")
                    }
                    IconButton(onClick = { fileLauncher.launch(arrayOf("text/*")) }) {
                        Icon(Icons.Default.OpenInNew, "打开文件")
                    }
                    IconButton(onClick = { activeFile?.let { saveFileLauncher.launch(it.name) }) {
                        Icon(Icons.Default.Save, "保存")
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, "设置")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (openFiles.isEmpty()) {
            // Welcome screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                        Icon(Icons.Default.FolderOpen, "打开文件", modifier = Modifier.padding(end = 8.dp))
                        Text("文件浏览器")
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
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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
                    CodeEditor(
                        editorFile = file,
                        onContentChange = { newContent ->
                            openFiles = openFiles.map { 
                                if (it == file) it.copy(content = newContent) else it 
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    
    // File browser dialog
    if (showFileBrowser) {
        FileBrowser(
            onFileSelected = { path, name ->
                // Read file content and open it
                try {
                    val content = android.util.Log.d("Medecode", "Reading: $path")
                    java.io.File(path).takeIf { it.exists() }?.readText()?.let { fileContent ->
                        openFile(name, path, fileContent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Medecode", "Error reading file", e)
                }
            },
            onDismiss = { showFileBrowser = false }
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