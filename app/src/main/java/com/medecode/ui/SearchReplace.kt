package com.medecode.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Search and replace dialog for code editor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchReplaceDialog(
    initialText: String,
    onDismiss: () -> Unit,
    onSearch: (String, Int) -> Unit, // text, matchIndex
    onReplace: (String, String) -> Unit, // search, replace
    onReplaceAll: (String, String, String) -> Unit // text, search, replace
) {
    var searchText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var matchIndex by remember { mutableStateOf(0) }
    var totalMatches by remember { mutableStateOf(0) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    val results = remember(searchText, caseSensitive, initialText) {
        findAllMatches(initialText, searchText, caseSensitive)
    }
    
    totalMatches = results.size
    matchIndex = (matchIndex % totalMatches).takeIf { totalMatches > 0 } ?: 0
    
    // Auto-select next match when typing
    LaunchedEffect(searchText) {
        if (searchText.isNotEmpty() && totalMatches > 0) {
            matchIndex = 0
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("搜索和替换") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Search input
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("搜索") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = "" }) {
                                Icon(Icons.Default.Close, "清除")
                            }
                        }
                    }
                )
                
                if (searchText.isNotEmpty()) {
                    Text(
                        text = "找到 $totalMatches 个匹配",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (totalMatches > 0) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    // Show current match position
                    if (totalMatches > 0) {
                        Text(
                            text = "第 ${matchIndex + 1}/$totalMatches 个",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    // Navigate matches
                    if (totalMatches > 0) {
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { 
                                    matchIndex = if (matchIndex == 0) totalMatches - 1 
                                    else matchIndex - 1 
                                },
                                enabled = totalMatches > 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "上一个")
                            }
                            IconButton(
                                onClick = { 
                                    matchIndex = if (matchIndex >= totalMatches - 1) 0 
                                    else matchIndex + 1 
                                },
                                enabled = totalMatches > 1
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "下一个")
                            }
                        }
                    }
                    
                    // Case sensitive option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Switch(
                            checked = caseSensitive,
                            onCheckedChange = { caseSensitive = it }
                        )
                        Text("区分大小写", modifier = Modifier.padding(start = 8.dp))
                    }
                    
                    // Replace input
                    OutlinedTextField(
                        value = replaceText,
                        onValueChange = { replaceText = it },
                        label = { Text("替换为") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    
                    // Replace buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (totalMatches > 0 && results.isNotEmpty()) {
                                    val (start, end) = results[matchIndex]
                                    val replaced = initialText.replaceRange(start, end, replaceText)
                                    onReplace(replaceText, replaced)
                                }
                            },
                            enabled = totalMatches > 0
                        ) {
                            Icon(Icons.Default.SyncAlt, "替换", modifier = Modifier.padding(end = 4.dp))
                            Text("替换")
                        }
                        
                        Button(
                            onClick = {
                                if (totalMatches > 0) {
                                    var result = initialText
                                    results.reversed().forEach { (start, end) ->
                                        result = result.replaceRange(start, end, replaceText)
                                    }
                                    onReplaceAll(result, searchText, replaceText)
                                }
                            },
                            enabled = totalMatches > 0
                        ) {
                            Icon(Icons.Default.SyncAlt, "全部替换", modifier = Modifier.padding(end = 4.dp))
                            Text("全部替换")
                        }
                    }
                }
                
                if (showError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
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

/**
 * Find all matches of search text in content
 */
private fun findAllMatches(
    content: String,
    search: String,
    caseSensitive: Boolean
): List<Pair<Int, Int>> {
    if (search.isEmpty()) return emptyList()
    
    val result = mutableListOf<Pair<Int, Int>>()
    val searchIn = if (caseSensitive) content else content.lowercase()
    val searchPattern = if (caseSensitive) search else search.lowercase()
    
    var index = 0
    while (index <= searchIn.length - searchPattern.length) {
        val foundIndex = searchIn.indexOf(searchPattern, index)
        if (foundIndex == -1) break
        
        result.add(foundIndex to foundIndex + searchPattern.length)
        index = foundIndex + 1
    }
    
    return result
}

/**
 * Get text selection range for a match index
 */
fun getMatchRange(content: String, search: String, index: Int, caseSensitive: Boolean): Pair<Int, Int>? {
    val matches = findAllMatches(content, search, caseSensitive)
    return if (index >= 0 && index < matches.size) {
        matches[index]
    } else {
        null
    }
}

/**
 * Search toolbar for inline search in editor
 */
@Composable
fun SearchToolbar(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "搜索和替换",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onOpenSearch) {
                Text("打开")
            }
        }
    }
}