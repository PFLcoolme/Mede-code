package com.medemini.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medemini.model.EditorFile

@Composable
fun CodeEditor(
    editorFile: EditorFile,
    onContentChange: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    isReviewMode: Boolean = false,
    diffAnnotatedString: AnnotatedString? = null
) {
    val darkTheme = isSystemInDarkTheme()

    var textFieldValue by remember(editorFile.path) {
        mutableStateOf(TextFieldValue(text = editorFile.content, selection = TextRange(editorFile.content.length)))
    }

    // 当文件切换时，更新内容
    LaunchedEffect(editorFile.path) {
        textFieldValue = TextFieldValue(text = editorFile.content, selection = TextRange(editorFile.content.length))
    }

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val fontSize = remember { mutableIntStateOf(14) }
    val fontFamily = FontFamily.Monospace
    val tabSize = 4

    val editorColors = if (darkTheme) {
        EditorColors(
            background = Color(0xFF1E1E1E),
            lineNumbersBg = Color(0x80252526),
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
            lineNumbersBg = Color(0x80F0F0F0),
            lineNumbersText = Color(0xFF9E9E9E),
            currentLine = Color(0xFFF5F5DC),
            text = Color(0xFF333333),
            lineNumber = Color(0xFF9E9E9E),
            selection = Color(0x40ADD8),
            indentGuide = Color(0xFFE0E0E0),
            bracketMatch = Color(0xFFE8E8E8)
        )
    }

    val lineCount = remember(textFieldValue.text) {
        textFieldValue.text.count { it == '\n' } + 1
    }

    // 语法高亮
    val highlightedText = remember(textFieldValue.text, editorFile.language) {
        SyntaxHighlight.highlightCode(textFieldValue.text, editorFile.language)
    }

    val onKeyDown: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { keyEvent ->
        when {
            keyEvent.key == Key.Tab && keyEvent.type == KeyEventType.KeyDown -> {
                val newText = buildString {
                    append(textFieldValue.text.substring(0, textFieldValue.selection.start))
                    repeat(tabSize) { append(" ") }
                    append(textFieldValue.text.substring(textFieldValue.selection.end))
                }
                val newCursorPos = textFieldValue.selection.start + tabSize
                textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
                onContentChange(newText)
                true
            }
            keyEvent.key == Key.Enter && keyEvent.type == KeyEventType.KeyDown -> {
                val cursorPos = textFieldValue.selection.start
                val text = textFieldValue.text
                val lineStart = text.lastIndexOf('\n', cursorPos - 1) + 1
                val lineEnd = text.indexOf('\n', cursorPos)
                val actualLineEnd = if (lineEnd == -1) text.length else lineEnd
                val currentLine = text.substring(lineStart, actualLineEnd)
                val indent = currentLine.takeWhile { it == ' ' || it == '\t' }.toString()
                val trimmedLine = currentLine.trimEnd()
                val extraIndent = if (trimmedLine.endsWith('{') || trimmedLine.endsWith(':')) "    " else ""
                val nextLineText = text.substring(actualLineEnd).trimStart()
                val hasReduceIndent = nextLineText.startsWith('}')
                val newIndent = when {
                    hasReduceIndent && indent.length >= 4 -> indent.substring(4)
                    extraIndent.isNotEmpty() -> indent + extraIndent
                    else -> indent
                }
                val newText = buildString {
                    append(text.substring(0, cursorPos))
                    append("\n$newIndent")
                    append(text.substring(cursorPos))
                }
                val newCursorPos = cursorPos + 1 + newIndent.length
                textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
                onContentChange(newText)
                true
            }
            keyEvent.type == KeyEventType.KeyDown -> {
                val keyChar = keyEvent.nativeKeyEvent.unicodeChar.toChar().toString()
                val pairing = when {
                    keyChar == "(" -> "(" to ")"
                    keyChar == "[" -> "[" to "]"
                    keyChar == "{" -> "{" to "}"
                    keyChar == "\"" -> "\"" to "\""
                    keyChar == "'" || keyChar == "Apostrophe" -> "'" to "'"
                    else -> null
                }
                pairing?.let { (open, close) ->
                    val newText = buildString {
                        append(textFieldValue.text.substring(0, textFieldValue.selection.start))
                        append(open)
                        append(close)
                        append(textFieldValue.text.substring(textFieldValue.selection.end))
                    }
                    val newCursorPos = textFieldValue.selection.start + 1
                    textFieldValue = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
                    onContentChange(newText)
                    true
                } ?: false
            }
            else -> false
        }
    }

    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 行号栏 - 更窄 + 毛玻璃背景
            LineNumbersColumn(
                lineCount = lineCount,
                scrollState = verticalScrollState,
                colors = editorColors,
                fontSize = fontSize.intValue,
                modifier = Modifier
                    .width(32.dp)
                    .background(editorColors.lineNumbersBg)
            )

            // 代码编辑区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(editorColors.background)
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
                    visualTransformation = { text ->
                        val baseAnnotated = if (isReviewMode && diffAnnotatedString != null) {
                            diffAnnotatedString
                        } else {
                            SyntaxHighlight.highlightCode(text.text, editorFile.language)
                        }
                        androidx.compose.ui.text.input.TransformedText(
                            baseAnnotated,
                            androidx.compose.ui.text.input.OffsetMapping.Identity
                        )
                    },
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
    scrollState: androidx.compose.foundation.ScrollState,
    colors: EditorColors,
    fontSize: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(scrollState),
        horizontalAlignment = Alignment.End
    ) {
        for (lineNum in 1..lineCount) {
            androidx.compose.material3.Text(
                text = lineNum.toString(),
                color = colors.lineNumbersText,
                fontSize = (fontSize - 2).sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .height((fontSize * 1.5).dp)
            )
        }
    }
}

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