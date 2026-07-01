package com.medecode.editor

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color
import com.medecode.model.Language

/**
 * Syntax highlighter for various programming languages
 */
object SyntaxHighlight {

    // Color scheme for syntax highlighting
    val Keywords = SpanStyle(
        color = Color(0xFF6344CF),
        fontWeight = FontWeight.Bold
    )
    
    val Strings = SpanStyle(
        color = Color(0xFF1A997F)
    )
    
    val Comments = SpanStyle(
        color = Color(0xFF8C8C8C),
        fontStyle = FontStyle.Italic
    )
    
    val Numbers = SpanStyle(
        color = Color(0xFFAC6636)
    )
    
    val Functions = SpanStyle(
        color = Color(0xFF2C72D5),
        fontWeight = FontWeight.Normal
    )
    
    val Types = SpanStyle(
        color = Color(0xFFC084FC),
        fontWeight = FontWeight.Bold
    )
    
    val Annotations = SpanStyle(
        color = Color(0xFFF59E0B),
        fontWeight = FontWeight.Bold
    )
    
    val Operators = SpanStyle(
        color = Color(0xFF636363),
        fontWeight = FontWeight.Bold
    )

    /**
     * Apply syntax highlighting to code text based on the language
     */
    fun applyHighlighting(code: String, language: Language): CharSequence {
        return buildAnnotatedString {
            val lines = code.split('\n')
            
            lines.forEachIndexed { lineIndex, line ->
                val tokens = tokenize(line, language)
                
                tokens.forEachIndexed { idx, token ->
                    when (token.type) {
                        TokenType.KEYWORD -> pushStyle(Keywords)
                        TokenType.STRING -> pushStyle(Strings)
                        TokenType.COMMENT -> pushStyle(Comments)
                        TokenType.NUMBER -> pushStyle(Numbers)
                        TokenType.FUNCTION -> pushStyle(Functions)
                        TokenType.TYPE -> pushStyle(Types)
                        TokenType.ANNOTATION -> pushStyle(Annotations)
                        TokenType.OPERATOR -> pushStyle(Operators)
                        else -> {}
                    }
                    
                    if (idx > 0 || lineIndex > 0) {
                        append(token.value)
                    } else {
                        append(token.value)
                    }
                    
                    when (token.type) {
                        TokenType.KEYWORD, TokenType.STRING, TokenType.COMMENT,
                        TokenType.NUMBER, TokenType.FUNCTION, TokenType.TYPE,
                        TokenType.ANNOTATION, TokenType.OPERATOR -> pop()
                        else -> {}
                    }
                }
                
                if (lineIndex < lines.lastIndex) {
                    append('\n')
                }
            }
        }
    }

    /**
     * A simpler version that directly highlights the code line by line
     */
    fun highlightCode(code: String, language: Language): CharSequence {
        return buildAnnotatedString {
            val lines = code.split('\n')
            var inBlockComment = false
            
            lines.forEachIndexed { index, line ->
                val highlightedLine = highlightLine(line, language, inBlockComment)
                
                if (highlightedLine is Pair<*, *>) {
                    val (text, _) = highlightedLine as Pair<String, Boolean>
                    append(text)
                } else {
                    append(highlightedLine.toString())
                }
                
                if (index < lines.lastIndex) {
                    append('\n')
                }
            }
        }
    }

    /**
     * Token types for syntax highlighting
     */
    sealed class TokenType {
        object KEYWORD : TokenType()
        object STRING : TokenType()
        object COMMENT : TokenType()
        object NUMBER : TokenType()
        object FUNCTION : TokenType()
        object TYPE : TokenType()
        object ANNOTATION : TokenType()
        object OPERATOR : TokenType()
        object TEXT : TokenType()
    }
    
    data class Token(val value: String, val type: TokenType)

    private fun tokenize(line: String, language: Language): List<Token> {
        val tokens = mutableListOf<Token>()
        var remaining = line
        var inBlockComment = false
        
        while (remaining.isNotEmpty()) {
            // Check for comments first
            if (inBlockComment) {
                val endIdx = remaining.indexOf("*/")
                if (endIdx >= 0) {
                    tokens.add(Token(remaining.substring(0, endIdx + 2), TokenType.COMMENT))
                    remaining = remaining.substring(endIdx + 2)
                    inBlockComment = false
                } else {
                    tokens.add(Token(remaining, TokenType.COMMENT))
                    remaining = ""
                }
                continue
            }
            
            // Single-line comment
            val singleCommentIdx = findCommentStart(remaining, language)
            if (singleCommentIdx >= 0) {
                tokens.add(Token(remaining.substring(0, singleCommentIdx), TokenType.TEXT))
                tokens.add(Token(remaining.substring(singleCommentIdx), TokenType.COMMENT))
                remaining = ""
                continue
            }
            
            // Block comment start
            val blockCommentIdx = remaining.indexOf("/*")
            if (blockCommentIdx >= 0) {
                if (blockCommentIdx > 0) {
                    tokens.add(Token(remaining.substring(0, blockCommentIdx), TokenType.TEXT))
                }
                val endIdx = remaining.indexOf("*/", blockCommentIdx + 2)
                if (endIdx >= 0) {
                    tokens.add(Token(remaining.substring(blockCommentIdx, endIdx + 2), TokenType.COMMENT))
                    remaining = remaining.substring(endIdx + 2)
                } else {
                    tokens.add(Token(remaining.substring(blockCommentIdx), TokenType.COMMENT))
                    remaining = ""
                    inBlockComment = true
                }
                continue
            }
            
            // String literals
            val stringStart = findStringStart(remaining, language)
            if (stringStart >= 0) {
                if (stringStart > 0) {
                    tokens.addAll(tokenizePrefix(remaining.substring(0, stringStart), language))
                    remaining = remaining.substring(stringStart)
                    continue
                }
                
                val endQuote = findEndingQuote(remaining, language)
                if (endQuote > 0) {
                    tokens.add(Token(remaining.substring(0, endQuote + 1), TokenType.STRING))
                    remaining = remaining.substring(endQuote + 1)
                    continue
                }
            }
            
            // Keywords, types, functions, numbers
            val wordMatch = WORD_PATTERN.find(remaining)
            if (wordMatch != null) {
                val prefix = remaining.substring(0, wordMatch.range.first)
                if (prefix.isNotEmpty()) {
                    tokens.addAll(tokenizePrefix(prefix, language))
                }
                
                val word = wordMatch.groupValues[1]
                val keyword = KEYWORDS[language]
                
                when {
                    keyword.contains(word.lowercase()) -> {
                        tokens.add(Token(word, TokenType.KEYWORD))
                    }
                    TYPE_KEYWORDS.contains(word.lowercase()) -> {
                        tokens.add(Token(word, TokenType.TYPE))
                    }
                    FUNCTION_KEYWORDS.contains(word.lowercase()) -> {
                        tokens.add(Token(word, TokenType.FUNCTION))
                    }
                    else -> {
                        tokens.add(Token(word, TokenType.TEXT))
                    }
                }
                
                remaining = remaining.substring(wordMatch.range.last + 1)
                continue
            }
            
            // Numbers
            val numberMatch = NUMBER_PATTERN.find(remaining)
            if (numberMatch != null && numberMatch.range.first == 0) {
                tokens.add(Token(numberMatch.groupValues[0], TokenType.NUMBER))
                remaining = remaining.substring(numberMatch.range.last)
                continue
            }
            
            // Operators
            val opMatch = OPERATOR_PATTERN.find(remaining)
            if (opMatch != null && opMatch.range.first == 0) {
                tokens.add(Token(opMatch.groupValues[0], TokenType.OPERATOR))
                remaining = remaining.substring(opMatch.range.last)
                continue
            }
            
            // Default: treat as text
            tokens.add(Token(remaining, TokenType.TEXT))
            remaining = ""
        }
        
        return tokens
    }
    
    private fun highlightLine(line: String, language: Language, inBlockComment: Boolean): CharSequence {
        return buildAnnotatedString {
            var remaining = line
            var localInBlockComment = inBlockComment
            
            while (remaining.isNotEmpty()) {
                // Handle block comment continuation
                if (localInBlockComment) {
                    val endIdx = remaining.indexOf("*/")
                    if (endIdx >= 0) {
                        pushStyle(Comments)
                        append(remaining.substring(0, endIdx + 2))
                        pop()
                        remaining = remaining.substring(endIdx + 2)
                        localInBlockComment = false
                    } else {
                        pushStyle(Comments)
                        append(remaining)
                        pop()
                        remaining = ""
                    }
                    continue
                }
                
                // Single-line comment
                val commentIdx = findCommentStart(remaining, language)
                if (commentIdx >= 0) {
                    val prefix = remaining.substring(0, commentIdx)
                    if (prefix.isNotEmpty()) {
                        appendHighlightedPrefix(prefix, language)
                    }
                    pushStyle(Comments)
                    append(remaining.substring(commentIdx))
                    pop()
                    remaining = ""
                    continue
                }
                
                // Block comment start
                val blockCommentIdx = remaining.indexOf("/*")
                if (blockCommentIdx >= 0) {
                    if (blockCommentIdx > 0) {
                        appendHighlightedPrefix(remaining.substring(0, blockCommentIdx), language)
                    }
                    val endIdx = remaining.indexOf("*/", blockCommentIdx + 2)
                    if (endIdx >= 0) {
                        pushStyle(Comments)
                        append(remaining.substring(blockCommentIdx, endIdx + 2))
                        pop()
                        remaining = remaining.substring(endIdx + 2)
                    } else {
                        pushStyle(Comments)
                        append(remaining.substring(blockCommentIdx))
                        pop()
                        remaining = ""
                        localInBlockComment = true
                    }
                    continue
                }
                
                // String literals
                val stringStart = findStringStart(remaining, language)
                if (stringStart >= 0) {
                    if (stringStart > 0) {
                        appendHighlightedPrefix(remaining.substring(0, stringStart), language)
                    }
                    val endQuote = findEndingQuote(remaining, language)
                    if (endQuote > 0) {
                        pushStyle(Strings)
                        append(remaining.substring(0, endQuote + 1))
                        pop()
                        remaining = remaining.substring(endQuote + 1)
                        continue
                    }
                }
                
                // Keywords, types, functions
                val wordMatch = WORD_PATTERN.find(remaining)
                if (wordMatch != null && wordMatch.range.first == 0) {
                    val word = wordMatch.groupValues[1]
                    val keyword = KEYWORDS[language]
                    
                    when {
                        keyword.contains(word.lowercase()) -> {
                            pushStyle(Keywords)
                            append(word)
                            pop()
                        }
                        TYPE_KEYWORDS.contains(word.lowercase()) -> {
                            pushStyle(Types)
                            append(word)
                            pop()
                        }
                        else -> {
                            append(word)
                        }
                    }
                    remaining = remaining.substring(wordMatch.range.last + 1)
                    continue
                }
                
                // Numbers
                val numberMatch = NUMBER_PATTERN.find(remaining)
                if (numberMatch != null && numberMatch.range.first == 0) {
                    pushStyle(Numbers)
                    append(numberMatch.groupValues[0])
                    pop()
                    remaining = remaining.substring(numberMatch.range.last)
                    continue
                }
                
                // Default: rest of line as text
                append(remaining)
                remaining = ""
            }
        }
    }
    
    private fun appendHighlightedPrefix(prefix: String, language: Language) {
        // Simple approach: just append the prefix
        // For more complex cases, we'd recursively tokenize
        val wordMatch = WORD_PATTERN.find(prefix)
        if (wordMatch != null && wordMatch.range.first == 0) {
            val word = wordMatch.groupValues[1]
            when {
                KEYWORDS[language].contains(word.lowercase()) -> {
                    buildAnnotatedString {
                        pushStyle(Keywords)
                        append(word)
                        pop()
                        append(prefix.substring(wordMatch.range.last + 1))
                    }.let { append(it) }
                }
                TYPE_KEYWORDS.contains(word.lowercase()) -> {
                    buildAnnotatedString {
                        pushStyle(Types)
                        append(word)
                        pop()
                        append(prefix.substring(wordMatch.range.last + 1))
                    }.let { append(it) }
                }
                else -> {
                    append(prefix)
                }
            }
        } else {
            append(prefix)
        }
    }

    // Language-specific keyword patterns
    private val SINGLE_LINE_COMMENTS = mapOf(
        Language.PYTHON to "//",
        Language.JAVASCRIPT to "//",
        Language.TYPESCRIPT to "//",
        Language.JAVA to "//",
        Language.KOTLIN to "//",
        Language.C to "//",
        Language.CPP to "//",
        Language.GO to "//",
        Language.RUST to "//",
        Language.RUBY to "//",
        Language.PHP to "//",
        Language.SWIFT to "//",
        Language.DART to "//",
        Language.BASH to "#",
        Language.PHP to "//",
        Language.SQL to "--",
    )
    
    private val BLOCK_COMMENT_PAIRS = mapOf(
        Language.JAVASCRIPT to "/*" to "*/",
        Language.TYPESCRIPT to "/*" to "*/",
        Language.JAVA to "/*" to "*/",
        Language.KOTLIN to "/*" to "*/",
        Language.C to "/*" to "*/",
        Language.CPP to "/*" to "*/",
        Language.RUST to "/*" to "*/",
        Language.GO to "/*" to "*/",
        Language.PHP to "/*" to "*/",
        Language.SWIFT to "/*" to "*/",
        Language.DART to "/*" to "*/",
        Language.HTML to "<!--" to "-->",
        Language.XML to "?>" to "?>",
    )
    
    private fun findCommentStart(line: String, language: Language): Int {
        // Check for single-line comment
        val commentStart = SINGLE_LINE_COMMENTS.entries
            .mapNotNull { (lang, prefix) ->
                if (lang == language) {
                    val idx = line.indexOf(prefix)
                    if (idx >= 0) idx else null
                } else null
            }
            .minOrNull()
        
        return commentStart ?: -1
    }

    // Keyword sets for each language
    private val KEYWORDS = mapOf(
        Language.PYTHON to listOf(
            "False", "None", "True", "and", "as", "assert", "async", "await",
            "break", "class", "continue", "def", "del", "elif", "else", "except",
            "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
            "while", "with", "yield"
        ),
        Language.JAVASCRIPT to listOf(
            "break", "case", "catch", "continue", "debugger", "default", "delete",
            "do", "else", "finally", "for", "function", "if", "in", "of", "instanceof",
            "new", "return", "switch", "this", "throw", "try", "typeof", "var", "void",
            "while", "with", "yield",
            "const", "let",
            "async", "await",
            "class", "extends", "super",
            "import", "export"
        ),
        Language.TYPESCRIPT to listOf(
            "break", "case", "catch", "continue", "debugger", "default", "delete",
            "do", "else", "finally", "for", "function", "if", "in", "of", "instanceof",
            "new", "return", "switch", "this", "throw", "try", "typeof", "var", "void",
            "while", "with", "yield",
            "const", "let",
            "async", "await",
            "class", "extends", "super",
            "import", "export",
            "interface", "type", "namespace", "enum", "implements", "private", "protected", "public", "static"
        ),
        Language.JAVA to listOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "var", "records", "sealed", "permits"
        ),
        Language.KOTLIN to listOf(
            "package", "import", "class", "object", "interface", "fun", "val", "var",
            "data", "typealias", "constructor", "this", "super",
            "if", "else", "when", "is", "in",
            "throw", "try", "catch", "finally", "return", "break", "continue",
            "for", "while",
            "companion", "init",
            "public", "private", "protected", "internal",
            "abstract", "open", "final", "sealed", "enum", "annotation",
            "lateinit", "lazy",
            "inline", "noinline", "crossinline",
            "tailrec", "operator", "infix",
            "where", "by"
        ),
        Language.C to listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "typedef", "union", "unsigned", "void",
            "volatile", "while"
        ),
        Language.CPP to listOf(
            "alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor",
            "bool", "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t",
            "class", "compl", "concept", "const", "consteval", "constexpr", "constinit",
            "const_cast", "continue", "co_await", "co_return", "co_yield",
            "decltype", "default", "delete", "do", "double", "dynamic_cast",
            "enum", "explicit", "export", "extern", "false", "float", "for",
            "friend", "goto", "if", "inline", "int", "long", "mutable",
            "namespace", "new", "noexcept", "not", "not_eq", "nullptr",
            "operator", "or", "or_eq", "private", "protected", "public",
            "register", "reinterpret_cast", "requires", "return", "short",
            "signed", "sizeof", "static", "static_assert", "static_cast",
            "struct", "switch", "template", "this", "throw", "true", "try",
            "typedef", "typeid", "typename", "union", "unsigned", "using",
            "virtual", "void", "volatile", "wchar_t", "while", "xor", "xor_eq"
        ),
        Language.GO to listOf(
            "break", "case", "chan", "const", "continue", "default", "defer",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct",
            "switch", "type", "var"
        ),
        Language.RUST to listOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn",
            "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
            "self", "Self", "static", "struct", "super", "trait", "true", "type",
            "unsafe", "use", "where", "while", "impl", "async", "await"
        ),
        Language.RUBY to listOf(
            "alias", "and", "begin", "break", "case", "class", "def", "defined?",
            "do", "else", "elsif", "end", "ensure", "false", "for", "if", "in",
            "module", "next", "nil", "not", "or", "require", "rescue", "retry",
            "return", "self", "super", "then", "true", "undef", "unless", "until",
            "when", "while", "yield", "__FILE__", "__LINE__", "__ENCODING__"
        ),
        Language.PHP to listOf(
            "__halt_compiler", "abstract", "and", "array", "as", "break", "callable",
            "case", "catch", "class", "clone", "const", "continue", "declare",
            "default", "die", "do", "echo", "else", "elseif", "empty", "enddeclare",
            "endfor", "endforeach", "endif", "endswitch", "endwhile", "eval",
            "exit", "extends", "final", "finally", "fn", "for", "foreach", "function",
            "global", "goto", "if", "implements", "include", "include_once", "instanceof",
            "insteadof", "interface", "isset", "list", "match", "namespace", "new",
            "or", "print", "private", "protected", "public", "require", "require_once",
            "return", "static", "switch", "throw", "trait", "try", "unset", "use",
            "var", "while", "yield"
        ),
        Language.SWIFT to listOf(
            "actor", "associatedtype", "async", "await", "as", "break", "case",
            "catch", "class", "continue", "convenience", "default", "defer", "deinit",
            "didSet", "do", "dynamic", "else", "enum", "extension", "fallthrough",
            "false", "final", "for", "func", "guard", "if", "import", "in",
            "init", "infix", "inout", "internal", "is", "lazy", "left", "let",
            "mutating", "none", "nonisolated", "nonmutating", "operator", "optional",
            "override", "postfix", "prefix", "private", "protocol", "public",
            "repeat", "required", "rethrows", "return", "right", "securescope",
            "self", "Self", "static", "struct", "subscript", "super", "switch",
            "throw", "throws", "true", "try", "typealias", "var", "weak", "where",
            "while", "willSet"
        ),
        Language.DART to listOf(
            "abstract", "as", "assert", "async", "await", "break", "case", "catch",
            "class", "coentrant", "const", "continue", "covariant", "default",
            "deferred", "do", "dynamic", "else", "enum", "implements", "export",
            "extends", "extension", "external", "factory", "false", "final",
            "finally", "for", "Function", "get", "hide", "if", "implements",
            "import", "in", "interface", "is", "lateinit", "mixin", "new",
            "null", "operator", "part", "required", "rethrow", "return", "set",
            "static", "super", "switch", "sync", "this", "throw", "try",
            "typedef", "var", "void", "while", "with", "yield", "show"
        )
    )
    
    private val TYPE_KEYWORDS = listOf(
        "int", "float", "double", "char", "boolean", "void", "string",
        "bool", "u8", "u16", "u32", "u64", "i8", "i16", "i32", "i64",
        "usize", "isize", "ptr", "ref", "str",
        "Int", "Float", "Bool", "Char", "Unit", "Any",
        "List", "Map", "Set", "Optional",
        "Integer", "Long", "Short", "Byte", "String"
    )
    
    private val FUNCTION_KEYWORDS = listOf(
        "main", "print", "println", "console", "log", "toString", "equals",
        "hashCode", "getClass", "notify", "notifyAll", "wait", "sleep",
        "run", "map", "filter", "reduce", "forEach", "find", "flatMap"
    )
    
    // Regex patterns
    private val WORD_PATTERN = Regex("""([a-zA-Z_]\w*)""")
    private val NUMBER_PATTERN = Regex("""\d+(\.\d+)?([eE][+-]?\d+)?([uU][lL]?[lL]?|[fF])?""")
    private val OPERATOR_PATTERN = Regex("""[+\-*/%=!<>&|^~?]+""")
    
    private fun findStringStart(line: String, language: Language): Int {
        val quoteChars = when (language) {
            Language.PYTHON -> arrayOf('\'', '"')
            Language.JAVASCRIPT, Language.TYPESCRIPT -> arrayOf('"', '\'', '`')
            Language.JAVA, Language.KOTLIN, Language.C, Language.CPP, Language.GO, 
            Language.RUST, Language.SWIFT, Language.DART -> arrayOf('"', '\'')
            Language.RUBY -> arrayOf('"', '\'')
            Language.PHP -> arrayOf('"', '\'')
            Language.HTML -> emptyArray()
            else -> arrayOf('"', '\'')
        }
        
        var minIdx = -1
        quoteChars.forEach { quote ->
            val idx = line.indexOf(quote)
            if (idx >= 0 && (minIdx < 0 || idx < minIdx)) {
                minIdx = idx
            }
        }
        
        return minIdx
    }
    
    private fun findEndingQuote(line: String, language: Language): Int {
        if (line.isEmpty()) return -1
        
        val firstChar = line[0]
        return when (firstChar) {
            '\'' -> line.indexOf('\'', 1)
            '"' -> line.indexOf('"', 1)
            '`' -> {
                var idx = 1
                while (idx < line.length) {
                    if (line[idx] == '`' && line[idx - 1] != '\\') return idx
                    idx++
                }
                -1
            }
            else -> -1
        }
    }
    
    private fun tokenizePrefix(prefix: String, language: Language): List<Token> {
        return listOf(Token(prefix, TokenType.TEXT))
    }
}