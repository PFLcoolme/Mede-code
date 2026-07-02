package com.medemini.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Code folding information
 */
data class FoldRegion(
    val startLine: Int,
    val endLine: Int,
    val collapsed: Boolean = false
)

/**
 * Parse code and extract foldable regions based on indentation
 */
fun parseFoldRegions(content: String): List<FoldRegion> {
    val lines = content.lines()
    val regions = mutableListOf<FoldRegion>()
    
    // Simple fold detection based on braces and indentation
    val braceStack = mutableListOf<Int>() // stack of line numbers with opening braces
    
    for ((index, line) in lines.withIndex()) {
        val trimmed = line.trim()
        
        // Detect opening braces
        if (trimmed.startsWith("{") || trimmed.endsWith("{") || trimmed == "{") {
            braceStack.add(index)
        }
        
        // Detect closing braces
        if ((trimmed.startsWith("}") || trimmed.endsWith("}")) && trimmed != "}") {
            // Line has both opening and closing, skip
        } else if (trimmed == "}" || trimmed.startsWith("}")) {
            if (braceStack.isNotEmpty()) {
                val startLine = braceStack.removeLast()
                if (index > startLine) {
                    regions.add(FoldRegion(startLine, index))
                }
            }
        }
    }
    
    return regions
}

/**
 * Get collapsed content for a fold region
 */
fun getFoldedContent(content: String, region: FoldRegion): String {
    val lines = content.lines()
    if (region.startLine < lines.size) {
        val firstLine = lines[region.startLine].trim()
        // Remove trailing brace for cleaner display
        val displayLine = firstLine.replace(Regex("\\{[\\s]*$"), "").trim()
        val endLine = lines[region.endLine].trim()
        return "$displayLine { ... } ${endLine.replace(Regex("^\\s*}"), "").trim()}"
    }
    return "..."
}

/**
 * Code folding toggle button
 */
@Composable
fun FoldToggleButton(
    isCollapsed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        modifier = modifier.size(20.dp)
    ) {
        Icon(
            if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = if (isCollapsed) "展开" else "折叠",
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Get line number folding state
 */
@Composable
fun rememberFoldState(initialRegions: List<FoldRegion> = emptyList()) = remember {
    MutableFoldState(initialRegions)
}

class MutableFoldState(initialRegions: List<FoldRegion>) {
    private val _folds = mutableStateMapOf<Int, Boolean>()
    
    init {
        initialRegions.forEach { region ->
            _folds[region.startLine] = region.collapsed
        }
    }
    
    fun isCollapsed(line: Int): Boolean {
        return _folds[line] == true
    }
    
    fun toggleFold(line: Int) {
        _folds[line] = !isCollapsed(line)
    }
    
    fun updateRegions(regions: List<FoldRegion>) {
        _folds.clear()
        regions.forEach { region ->
            _folds[region.startLine] = region.collapsed
        }
    }
}