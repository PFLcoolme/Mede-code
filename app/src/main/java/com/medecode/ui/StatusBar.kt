package com.medecode.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Status bar information
 */
data class StatusBarInfo(
    val language: String = "",
    val encoding: String = "UTF-8",
    val line: Int = 1,
    val column: Int = 1,
    val selectionStart: Int? = null,
    val selectionEnd: Int? = null,
    val totalLines: Int = 1,
    val totalChars: Int = 0
)

/**
 * Icon for language
 */
fun getLanguageIcon(language: String): ImageVector {
    return when (language.lowercase()) {
        "python", "py" -> Icons.Default.Code
        "javascript", "js", "typescript", "ts" -> Icons.Default.Javascript
        "java", "kotlin" -> Icons.Default.Android
        "c", "cpp", "c++" -> Icons.Default.Code
        "go" -> Icons.Default.Speed
        "rust", "rs" -> Icons.Default.Build
        "ruby" -> Icons.Default.Dangerous
        "php" -> Icons.Default.Web
        "swift" -> Icons.Default.Devices
        "dart" -> Icons.Default.FiberNew
        "html", "css" -> Icons.Default.Language
        "sql" -> Icons.Default.Database
        "shell", "bash" -> Icons.Default.Terminal
        "markdown", "md" -> Icons.Default.Article
        "json" -> Icons.Default.Database
        "xml" -> Icons.Default.Code
        "yaml", "yml" -> Icons.Default.Settings
        else -> Icons.Default.Code
    }
}

/**
 * Status bar composable
 */
@Composable
fun StatusBar(
    info: StatusBarInfo,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Language
            if (info.language.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        getLanguageIcon(info.language),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = info.language,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            
            // Encoding
            Text(
                text = info.encoding,
                style = MaterialTheme.typography.labelMedium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Cursor position
            Text(
                text = "行 ${info.line}, 列 ${info.column}",
                style = MaterialTheme.typography.labelMedium
            )
            
            // Selection info
            if (info.selectionStart != null && info.selectionEnd != null) {
                val selectedChars = kotlin.math.abs(info.selectionEnd - info.selectionStart)
                Text(
                    text = "已选择 $selectedChars 字符",
                    style = MaterialTheme.typography.labelMedium
                )
            }
            
            // Total lines
            Text(
                text = "${info.totalLines} 行",
                style = MaterialTheme.typography.labelMedium
            )
            
            // Total characters
            if (info.totalChars > 0) {
                Text(
                    text = "${info.totalChars} 字符",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}