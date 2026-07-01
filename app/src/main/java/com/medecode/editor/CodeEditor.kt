package com.medecode.editor

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingGraphicsConfig
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medecode.model.EditorFile
import com.medecode.model.Language

/**
 * A composable for editing code with syntax highlighting, line numbers, and more
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun CodeEditor(
    editorFile: EditorFile,
    onContentChange: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    
    // Editor state
    val textState = remember { MutableAnnotatedStringState(AnnotatedString(editorFile.content)) }
    val searchText by remember { mutableStateOf("") }
    val showSearch by remember { mutableStateOf(false) }
    
    // Scroll states
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    
    // Font settings
    val fontSize = remember { mutableIntStateOf(14) }
    val fontFamily = FontFamily.Monospace
    
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
    
    // Tab settings
    val tabSize = 4
    
    // Keyboard handling
    val onKeyDown: (KeyEvent) -> Boolean = { keyEvent ->
        when {
            // Tab key handling
            keyEvent.key == Key.Tab -> {
                val cursorStart = textState.annotatedString.selection.start
                val cursorEnd = textState.annotatedString.selection.end
                
                if (cursorStart == cursorEnd) {
                    // Insert spaces at cursor position
                    val text = textState.annotatedString.text
                    val newText = text.buildString {
                        append(text.substring(0, cursorStart))
                        repeat(tabSize) { append(" ") }
                        append(text.substring(cursorEnd))
                    }
                    textState.annotatedString = AnnotatedString(newText)
                    val newCursorPos = cursorStart + tabSize
                    textState.annotatedString = AnnotatedString(
                        newText,
                        TextRange(newCursorPos)
                    )
                    onContentChange(newText)
                }
                true
            }
            
            // Enter key handling - auto indent
            keyEvent.key == Key.Enter -> {
                val cursorPos = textState.annotatedString.selection.start
                val text = textState.annotatedString.text
                
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
                val nextLine = text.substring(actualLineEnd).trimStart()
                val reduceIndent = if (nextLine.startsWith('}')) {
                    "    "
                } else {
                    ""
                }
                
                val newIndent = indent + extraIndent.takeUnless { reduceIndent.isNullOrEmpty() }
                    ?: if (reduceIndent != null && indent.length >= reduceIndent.length) {
                        indent.substring(reduceIndent.length)
                    } else {
                        indent
                    }
                
                val newText = text.buildString {
                    append(text.substring(0, cursorPos))
                    append("\n$newIndent")
                    append(text.substring(cursorPos))
                }
                
                textState.annotatedString = AnnotatedString(
                    newText,
                    TextRange(cursorPos + 1 + newIndent.length)
                )
                onContentChange(newText)
                true
            }
            
            // Auto-close brackets and quotes
            keyEvent.key == Key.LeftParen || keyEvent.key == Key.RightParen ||
            keyEvent.key == Key.LeftBracket || keyEvent.key == Key.RightBracket ||
            keyEvent.key == Key.LeftBracket || keyEvent.key == Key.RightBracket ||
            keyEvent.key == Key.Quest || keyEvent.key == Key.Apostrophe -> {
                val cursorStart = textState.annotatedString.selection.start
                val cursorEnd = textState.annotatedString.selection.end
                val text = textState.annotatedString.text
                
                val pairing = when (keyEvent.key) {
                    Key.LeftParen -> "(" to ")"
                    Key.RightParen -> null
                    Key.LeftBracket -> "[" to "]"
                    Key.RightBracket -> null
                    Key.LeftBrace -> "{" to "}"
                    Key.RightBrace -> null
                    Key.Quest -> "\"" to "\""
                    Key.Apostrophe -> "'" to "'"
                    else -> null
                }
                
                if (pairing != null) {
                    val (open, close) = pairing
                    if (cursorStart == cursorEnd) {
                        // Select and insert paired characters
                        val newText = text.buildString {
                            append(text.substring(0, cursorStart))
                            append(open)
                            append(close)
                            append(text.substring(cursorEnd))
                        }
                        textState.annotatedString = AnnotatedString(
                            newText,
                            TextRange(cursorStart + 1)
                        )
                        onContentChange(newText)
                    }
                    true
                } else {
                    false
                }
            }
            
            else -> false
        }
    }
    
    // Calculate line count
    val lineCount = remember(textState.annotatedString.text) {
        textState.annotatedString.text.count { it == '\n' } + 1
    }
    
    Column(modifier = modifier) {
        // Editor area
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        horizontalScrollState.scrollBy(-dragAmount)
                    }
                }
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
            
            // Current line highlight
            CurrentLineHighlight(
                lineCount = lineCount,
                scrollState = verticalScrollState,
                colors = editorColors,
                fontFontFamily = fontFontFamily,
                fontSize = fontSize.intValue
            )
            
            // Code editing area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(editorColors.background)
            ) {
                BasicTextField(
                    value = AnnotatedString(textState.annotatedString.text),
                    onValueChange = { newText ->
                        textState.annotatedString = newText
                        onContentChange(newText.text)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(editorColors.background)
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { _, dragAmount ->
                                horizontalScrollState.scrollBy(-dragAmount)
                            }
                        },
                    textStyle = TextStyle(
                        color = editorColors.text,
                        fontSize = fontSize.value.sp,
                        fontFamily = fontFontFamily,
                        lineHeight = lineHeight(fontSize.intValue),
                        caretStyle = CaretStyle(
                            size = Offset(1f, fontSize.intValue.sp.value * 1.2f),
                            color = editorColors.text
                        )
                    ),
                    cursorBrush = SolidColor(editorColors.text),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        capitalization = KeyboardCapitalization.None
                    ),
                    maxLines = Int.MAX_VALUE,
                    onKeyEvent = onKeyDown,
                    onSelectionChanged = { selection ->
                        // Handle selection change
                    }
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

@Composable
private fun CurrentLineHighlight(
    lineCount: Int,
    scrollState: androidx.compose.foundation.scroll.ScrollState,
    colors: EditorColors,
    fontFontFamily: FontFamily,
    fontSize: Int
) {
    // This is a simplified version - a full implementation would track the current cursor line
    Box(
        modifier = Modifier
            .width(1.dp)
            .background(colors.currentLine)
    )
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

/**
 * Line height calculation based on font size
 */
fun lineHeight(fontSize: Int): LineHeight {
    return LineHeight((fontSize * 1.5).sp)
}

/**
 * State holder for the annotated string
 */
class MutableAnnotatedStringState(initialValue: AnnotatedString) {
    var annotatedString: AnnotatedString by mutableStateOf(initialValue)
        private set
}

/**
 * Helper to get syntax highlighted text
 */
@Composable
fun getSyntaxHighlightedText(code: String, language: Language): AnnotatedString {
    val highlighted = SyntaxHighlight.highlightCode(code, language)
    return if (highlighted is AnnotatedString) {
        highlighted
    } else {
        AnnotatedString(highlighted.toString())
    }
}