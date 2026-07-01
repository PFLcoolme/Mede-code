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
    fun highlightCode(code: String, language: Language): CharSequence {
        return buildAnnotatedString {
            val lines = code.split('\n')
            var inBlockComment = false
            
            lines.forEachIndexed { index, line ->
                val (highlightedLine, updatedInBlockComment) = highlightLine(line, language, inBlockComment)
                append(highlightedLine)
                
                if (index < lines.lastIndex) {
                    append('\n')
                }
                inBlockComment = updatedInBlockComment
            }
        }
    }

    /**
     * Highlight a single line of code
     */
    private fun highlightLine(line: String, language: Language, inBlockComment: Boolean): Pair<String, Boolean> {
        var result = ""
        var remaining = line
        var localInBlockComment = inBlockComment
        val commentStart = getSingleLineCommentPrefix(language)
        
        while (remaining.isNotEmpty()) {
            // Handle block comment continuation
            if (localInBlockComment) {
                val endIdx = remaining.indexOf("*/")
                if (endIdx >= 0) {
                    result += "<comment>${remaining.substring(0, endIdx + 2)}</comment>"
                    remaining = remaining.substring(endIdx + 2)
                    localInBlockComment = false
                } else {
                    result += "<comment>$remaining</comment>"
                    remaining = ""
                }
                continue
            }
            
            // Single-line comment
            val commentIdx = remaining.indexOf(commentStart)
            if (commentIdx >= 0) {
                result += highlightPrefix(remaining.substring(0, commentIdx), language)
                result += "<comment>${remaining.substring(commentIdx)}</comment>"
                remaining = ""
                continue
            }
            
            // Block comment start
            val blockCommentIdx = remaining.indexOf("/*")
            if (blockCommentIdx >= 0) {
                if (blockCommentIdx > 0) {
                    result += highlightPrefix(remaining.substring(0, blockCommentIdx), language)
                }
                val endIdx = remaining.indexOf("*/", blockCommentIdx + 2)
                if (endIdx >= 0) {
                    result += "<comment>${remaining.substring(blockCommentIdx, endIdx + 2)}</comment>"
                    remaining = remaining.substring(endIdx + 2)
                } else {
                    result += "<comment>${remaining.substring(blockCommentIdx)}</comment>"
                    remaining = ""
                    localInBlockComment = true
                }
                continue
            }
            
            // String literals
            val stringStart = findStringStart(remaining)
            if (stringStart >= 0) {
                if (stringStart > 0) {
                    result += highlightPrefix(remaining.substring(0, stringStart), language)
                }
                val endQuote = findEndingQuote(remaining)
                if (endQuote > 0) {
                    result += "<string>${remaining.substring(0, endQuote + 1)}</string>"
                    remaining = remaining.substring(endQuote + 1)
                    continue
                }
            }
            
            // Keywords and identifiers
            val wordMatch = WORD_REGEX.find(remaining)
            if (wordMatch != null && wordMatch.range.first == 0) {
                val word = wordMatch.groupValues[1]
                result += if (isKeyword(word, language)) {
                    "<keyword>$word</keyword>"
                } else {
                    word
                }
                remaining = remaining.substring(wordMatch.range.last + 1)
                continue
            }
            
            // Numbers
            val numberMatch = NUMBER_REGEX.find(remaining)
            if (numberMatch != null && numberMatch.range.first == 0) {
                result += "<number>${numberMatch.groupValues[0]}</number>"
                remaining = remaining.substring(numberMatch.range.last)
                continue
            }
            
            // Default: rest of line as text
            result += remaining
            remaining = ""
        }
        
        return result to localInBlockComment
    }
    
    private fun highlightPrefix(prefix: String, language: Language): String {
        val wordMatch = WORD_REGEX.find(prefix)
        if (wordMatch != null && wordMatch.range.first == 0) {
            val word = wordMatch.groupValues[1]
            return if (isKeyword(word, language)) {
                "<keyword>$word</keyword>${prefix.substring(wordMatch.range.last + 1)}"
            } else {
                prefix
            }
        }
        return prefix
    }
    
    private fun isKeyword(word: String, language: Language): Boolean {
        return KEYWORDS_MAP[language]?.contains(word.lowercase()) ?: false
    }

    // Get single line comment prefix for a language
    private fun getSingleLineCommentPrefix(language: Language): String {
        return when (language) {
            Language.PYTHON, Language.BASH -> "#"
            else -> "//"
        }
    }

    // Find the start of a string literal
    private fun findStringStart(line: String): Int {
        var minIdx = -1
        
        arrayOf('"', '\'', '`').forEach { quote ->
            val idx = line.indexOf(quote)
            if (idx >= 0 && (minIdx < 0 || idx < minIdx)) {
                minIdx = idx
            }
        }
        
        return minIdx
    }
    
    // Find the ending quote
    private fun findEndingQuote(line: String): Int {
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

    // Keyword sets for each language
    private val KEYWORDS_MAP = mapOf(
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
            "when", "while", "yield", "__FILE__", "__LINE__"
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
            "finally", "for", "Function", "get", "hide", "if", "import", "in",
            "interface", "is", "lateinit", "mixin", "new",
            "null", "operator", "part", "required", "rethrow", "return", "set",
            "static", "super", "switch", "sync", "this", "throw", "try",
            "typedef", "var", "void", "while", "with", "yield", "show"
        ),
        Language.BASH to listOf(
            "if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "in", "echo", "exit", "export", "return",
            "read", "set", "unset", "shift", "trap", "umask"
        ),
        Language.SQL to listOf(
            "select", "insert", "update", "delete", "create", "drop", "alter", "table",
            "from", "where", "join", "inner", "left", "right", "outer", "on", "and",
            "or", "not", "null", "is", "as", "order", "by", "group", "having",
            "index", "view", "distinct", "count", "sum", "avg", "min", "max", "into"
        ),
        Language.HTML to listOf(
            "html", "head", "body", "div", "span", "p", "a", "img", "ul", "ol", "li",
            "h1", "h2", "h3", "h4", "h5", "h6", "form", "input", "button", "table",
            "tr", "td", "th", "select", "option", "textarea", "label", "link", "meta"
        )
    )
    
    // Regex patterns
    private val WORD_REGEX = Regex("""([a-zA-Z_]\w*)""")
    private val NUMBER_REGEX = Regex("""\d+(\.\d+)?([eE][+-]?\d+)?([uU][lL]?[lL]?|[fF])?""")
}