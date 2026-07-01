package com.medecode

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.tab.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medecode.editor.CodeEditor
import com.medecode.model.EditorFile
import com.medecode.ui.theme.MedecodeTheme
import java.io.File

class MainActivity : ComponentActivity() {
    private var currentFileUri: Uri? = null
    
    // File content storage
    private val fileContents = mutableMapOf<Uri, String>()
    
    // Activity result launcher for opening files
    private val fileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            currentFileUri = it
            // Read file content
            contentResolver.openInputStream(it)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                fileContents[it] = content
                // Update the open files
                openFiles = openFiles.filter { it.path != it.name } // Remove placeholder
                openFiles = openFiles + createEditorFile(it, content)
            }
        }
    }
    
    // Activity result launcher for saving files
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        uri?.let {
            currentFileUri = it
            activeFile?.let { file ->
                contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(file.content.toByteArray())
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check for intent data (opening a file from another app)
        intent?.data?.let { uri ->
            currentFileUri = uri
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val content = inputStream.bufferedReader().readText()
                fileContents[uri] = content
                openFiles = openFiles + createEditorFile(uri, content)
            }
        }
        
        setContent {
            MedecodeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MedecodeApp(
                        onOpenFile = {
                            fileLauncher.launch(arrayOf("text/*"))
                        },
                        onSaveFile = { file ->
                            saveFileLauncher.launch(file.name)
                        }
                    )
                }
            }
        }
    }
    
    private fun createEditorFile(uri: Uri, content: String): EditorFile {
        val name = getFileName(uri) ?: "untitled"
        return EditorFile(
            name = name,
            path = uri.toString(),
            content = content
        )
    }
    
    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MedecodeApp(
    onOpenFile: () -> Unit,
    onSaveFile: (EditorFile) -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    
    // State for open files
    var openFiles by remember { mutableStateOf<List<EditorFile>>(emptyList()) }
    var activeFileIndex by remember { mutableIntStateOf(-1) }
    var showSettings by remember { mutableStateOf(false) }
    
    // Get active file
    val activeFile = remember(openFiles, activeFileIndex) {
        if (activeFileIndex >= 0 && activeFileIndex < openFiles.size) {
            openFiles[activeFileIndex]
        } else {
            null
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
                    IconButton(onClick = onOpenFile) {
                        Icon(Icons.Default.FolderOpen, "打开文件")
                    }
                    IconButton(onClick = { activeFile?.let { onSaveFile(it) }) {
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
                        text = "👨‍💻 Medecode",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Android 代码编辑器",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onOpenFile,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Icon(Icons.Default.FolderOpen, "打开文件", modifier = Modifier.padding(end = 8.dp))
                        Text("打开代码文件")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        openFiles.forEachIndexed { index, file ->
                            Tab(
                                selected = index == activeFileIndex,
                                onClick = { activeFileIndex = index },
                                icon = {
                                    Icon(
                                        getLanguageIcon(file.language),
                                        text = file.name
                                    )
                                },
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

@Composable
fun getLanguageIcon(language: com.medecode.model.Language): ImageVector {
    return when (language) {
        com.medecode.model.Language.PYTHON -> Icons.Default.Code
        com.medecode.model.Language.JAVASCRIPT, 
        com.medecode.model.Language.TYPESCRIPT -> Icons.Default.Js
        com.medecode.model.Language.JAVA, 
        com.medecode.model.Language.KOTLIN -> Icons.Default.Android
        com.medecode.model.Language.C, 
        com.medecode.model.Language.CPP -> Icons.Default.Build
        com.medecode.model.Language.GO -> Icons.Default.Dns
        com.medecode.model.Language.RUST -> Icons.Default.Shield
        else -> Icons.Default.Code
    }
}

// Extension for Icon to accept ImageVector
private fun Icon(Icons.Default.Js, text: String?) {}
private fun Icon(Icons.Default.Android, text: String?) {}
private fun Icon(Icons.Default.Build, text: String?) {}
private fun Icon(Icons.Default.Dns, text: String?) {}
private fun Icon(Icons.Default.Shield, text: String?) {}