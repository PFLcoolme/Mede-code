package com.medecode.editor

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medecode.model.EditorFile

/**
 * A composable for editing code with line numbers and syntax support
 */
@Composable
fun CodeEditor(
    editorFile: EditorFile,
    onContentChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    
    // Editor state
    var textFieldValue by remember { 
        mutableStateOf(TextFieldValue(editorFile.content, TextRange(editorFile.content.length))) 
    }
    
    // Scroll states
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    // Font settings
    val fontSize = remember { mutableIntStateOf(14) }
    val fontFamily = FontFamily.Monospace
    
    // Tab settings
    val tabSize = 4
    
    // Colors based on theme
    val editorColors = if (darkTheme) {
        EditorColors(
            background = Color(0xFF1E1E1E),
            lineNumbersBg = Color(0xFF252526),
            lineNumbersText = Color(0xFF858585),
            currentLine = Color(0xFF2A2D2E),
            text = Color(0xFFD4D4D4),
            lineNumber = Color(0xFF858585),
            selection = Color(0x80415567),
            indentGuide = Color(0xFF3E3E40),
            bracketMatch = Color(0xFF3C3C3C)
        )
    } else {
        EditorColors(
            background = Color(0xFFFFFBF3),
            lineNumbersBg = Color(0xFFF0F0F0),
            lineNumbersText = Color(0xFF9E9E9E),
            currentLine = Color(0xFFF5F5DC),
            text = Color(0xFF333333),
            lineNumber = Color(0xFF9E9E9E),
            selection = Color(0x40ADD8),
            indentGuide = Color(0xFFE0E0E0),
            bracketMatch = Color(0xFFE8E8E8)
        )
    }
    
    // Calculate line count
    val lineCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }
    
    // Keyboard handling
    val onKeyDown: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
        when {
            // Tab key handling
            keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown -> {
                val newText = textFieldValue.text.buildString {
                    append(textFieldValue.text.substring(0, textFieldValue.selection.start))
                    repeat(tabSize) { append(" ") }
                    append(textFieldValue.text.substring(textFieldValue.selection.end))
                }
                val newCursorPos = textFieldValue.selection.start + tabSize
                textFieldValue = TextFieldValue(
                    newText,
                    TextRange(newCursorPos)
                )
                onContentChange(newText)
                true
            }
            
            // Enter key handling - auto indent
            keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                val cursorPos = textFieldValue.selection.start
                val text = textFieldValue.text
                
                // Find the current line
                val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
                val lineEnd = text.indexOf('\n', cursorPos)
                val actualLineEnd = if (lineEnd == -1) text.length else lineEnd
                val currentLine = text.substring(lineStart, actualLineEnd)
                
                // Calculate indentation
                val indent = currentLine.takeWhile { it == ' ' || it == '\t' }.toString()
                
                // Check if we need extra indent (after { or :)
                val trimmedLine = currentLine.trimEnd()
                val extraIndent = if (trimmedLine.endsWith('{') || trimmedLine.endsWith(':')) {
                    "    "
                } else {
                    ""
                }
                
                // Check if we need to reduce indent (before })
                val nextLineText = text.substring(actualLineEnd).trimStart()
                val hasReduceIndent = nextLineText.startsWith('}')
                
                val newIndent = when {
                    hasReduceIndent && indent.length >= 4 -> indent.substring(4)
                    extraIndent.isNotEmpty() -> indent + extraIndent
                    else -> indent
                }
                
                val newText = text.buildString {
                    append(text.substring(0, cursorPos))
                    append("\n$newIndent")
                    append(text.substring(cursorPos))
                }
                
                val newCursorPos = cursorPos + 1 + newIndent.length
                textFieldValue = TextFieldValue(
                    newText,
                    TextRange(newCursorPos)
                )
                onContentChange(newText)
                true
            }
            
            // Auto-close brackets and quotes
            keyEvent.type == KeyEventType.KeyDown -> {
                val pairing = when (keyEvent.key) {
                    Key.ParenLeft -> "(" to ")"
                    Key.BracketLeft -> "[" to "]"
                    Key.BracketRight -> null
                    Key.Companion.getBraceLeft() -> "{" to "}"
                    Key.Companion.getQuote() -> "\"" to "\""
                    Key.Apostrophe -> "'" to "'"
                    else -> null
                }
                
                pairing?.let { (open, close) ->
                    val newText = textFieldValue.text.buildString {
                        append(textFieldValue.text.substring(0, textFieldValue.selection.start))
                        append(open)
                        append(close)
                        append(textFieldValue.text.substring(textFieldValue.selection.end))
                    }
                    val newCursorPos = textFieldValue.selection.start + 1
                    textFieldValue = TextFieldValue(
                        newText,
                        TextRange(newCursorPos)
                    )
                    onContentChange(newText)
                    true
                } ?: false
            }
            
            else -> false
        }
    }
    
    Column(modifier = modifier) {
        // Editor area
        Row(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Line numbers gutter
            LineNumbersColumn(
                lineCount = lineCount,
                scrollState = verticalScrollState,
                colors = editorColors,
                fontSize = fontSize.intValue,
                modifier = Modifier
                    .width(48.dp)
                    .background(editorColors.lineNumbersBg)
            )
            
            // Code editing area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(editorColors.background)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            horizontalScrollState.scrollBy(-dragAmount)
                        }
                    }
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { newValue ->
                        textFieldValue = newValue
                        onContentChange(newValue.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(editorColors.background)
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState)
                        .onPreviewKeyEvent(onKeyDown),
                    textStyle = TextStyle(
                        color = editorColors.text,
                        fontSize = fontSize.value.sp,
                        fontFamily = fontFamily,
                        lineHeight = (fontSize.intValue * 1.5).sp,
                        textAlign = TextAlign.Left
                    ),
                    cursorBrush = SolidColor(editorColors.text),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        capitalization = KeyboardCapitalization.None
                    ),
                    maxLines = Int.MAX_VALUE
                )
            }
        }
    }
}

@Composable
private fun LineNumbersColumn(
    lineCount: Int,
    scrollState: androidx.compose.foundation.scroll.ScrollState,
    colors: EditorColors,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        state = scrollState,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(lineCount) { lineNum ->
            Text(
                text = (lineNum + 1).toString(),
                color = colors.lineNumbersText,
                fontSize = fontSize.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .height(20.dp)
            )
        }
    }
}

/**
 * Editor colors for light and dark themes
 */
data class EditorColors(
    val background: Color,
    val lineNumbersBg: Color,
    val lineNumbersText: Color,
    val currentLine: Color,
    val text: Color,
    val lineNumber: Color,
    val selection: Color,
    val indentGuide: Color,
    val bracketMatch: Color
)