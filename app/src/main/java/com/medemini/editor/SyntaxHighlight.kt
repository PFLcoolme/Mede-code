package com.medemini.editor

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import com.medemini.model.Language

object SyntaxHighlight {

    var keywordColor = Color(0xFF6344CF)
    var stringColor = Color(0xFF1A997F)
    var commentColor = Color(0xFF8C8C8C)
    var numberColor = Color(0xFFAC6636)
    var functionColor = Color(0xFF2C72D5)
    var typeColor = Color(0xFFC084FC)
    var annotationColor = Color(0xFFF59E0B)
    var operatorColor = Color(0xFF636363)

    private val Keywords get() = SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold)
    private val Strings get() = SpanStyle(color = stringColor)
    private val Comments get() = SpanStyle(color = commentColor, fontStyle = FontStyle.Italic)
    private val Numbers get() = SpanStyle(color = numberColor)
    private val Functions get() = SpanStyle(color = functionColor)
    private val Types get() = SpanStyle(color = typeColor, fontWeight = FontWeight.Bold)
    private val Annotations get() = SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold)

    fun highlightCode(code: String, language: Language): androidx.compose.ui.text.AnnotatedString {
        return buildAnnotatedString {
            val lines = code.split('\n')
            var inBlockComment = false

            lines.forEachIndexed { index, line ->
                val (spans, updatedInBlockComment) = highlightLineSpans(line, language, inBlockComment)
                var pos = 0
                for ((start, end, style) in spans) {
                    if (start > pos) append(line.substring(pos, start))
                    withStyle(style) { append(line.substring(start, end)) }
                    pos = end
                }
                if (pos < line.length) append(line.substring(pos))

                if (index < lines.lastIndex) append('\n')
                inBlockComment = updatedInBlockComment
            }
        }
    }

    private data class Span(val start: Int, val end: Int, val style: SpanStyle)

    private fun highlightLineSpans(line: String, language: Language, inBlockComment: Boolean): Pair<List<Span>, Boolean> {
        val spans = mutableListOf<Span>()
        var remaining = line
        var offset = 0
        var localInBlockComment = inBlockComment
        val commentStart = getSingleLineCommentPrefix(language)

        while (remaining.isNotEmpty()) {
            if (localInBlockComment) {
                val endIdx = remaining.indexOf("*/")
                if (endIdx >= 0) {
                    spans.add(Span(offset, offset + endIdx + 2, Comments))
                    offset += endIdx + 2
                    remaining = remaining.substring(endIdx + 2)
                    localInBlockComment = false
                } else {
                    spans.add(Span(offset, offset + remaining.length, Comments))
                    remaining = ""
                }
                continue
            }

            val commentIdx = remaining.indexOf(commentStart)
            val blockCommentIdx = remaining.indexOf("/*")
            val stringIdx = findStringStart(remaining)

            val nextEvent = listOf(
                if (commentIdx >= 0) commentIdx else Int.MAX_VALUE,
                if (blockCommentIdx >= 0) blockCommentIdx else Int.MAX_VALUE,
                if (stringIdx >= 0) stringIdx else Int.MAX_VALUE
            ).minOrNull() ?: Int.MAX_VALUE

            if (nextEvent == Int.MAX_VALUE) {
                highlightWords(remaining, offset, language, spans)
                remaining = ""
                continue
            }

            if (nextEvent > 0) {
                highlightWords(remaining.substring(0, nextEvent), offset, language, spans)
            }

            when {
                nextEvent == commentIdx -> {
                    spans.add(Span(offset + commentIdx, offset + remaining.length, Comments))
                    remaining = ""
                }
                nextEvent == blockCommentIdx -> {
                    val endIdx = remaining.indexOf("*/", blockCommentIdx + 2)
                    if (endIdx >= 0) {
                        spans.add(Span(offset + blockCommentIdx, offset + endIdx + 2, Comments))
                        offset += endIdx + 2
                        remaining = remaining.substring(endIdx + 2)
                    } else {
                        spans.add(Span(offset + blockCommentIdx, offset + remaining.length, Comments))
                        localInBlockComment = true
                        remaining = ""
                    }
                }
                nextEvent == stringIdx -> {
                    val endQuote = findEndingQuote(remaining.substring(stringIdx))
                    if (endQuote > 0) {
                        spans.add(Span(offset + stringIdx, offset + stringIdx + endQuote + 1, Strings))
                        offset += stringIdx + endQuote + 1
                        remaining = remaining.substring(stringIdx + endQuote + 1)
                    } else {
                        spans.add(Span(offset + stringIdx, offset + remaining.length, Strings))
                        remaining = ""
                    }
                }
            }
        }

        return spans to localInBlockComment
    }

    private fun highlightWords(text: String, baseOffset: Int, language: Language, spans: MutableList<Span>) {
        val regex = WORD_REGEX
        var searchStart = 0
        while (searchStart < text.length) {
            val match = regex.find(text, searchStart) ?: break
            if (match.range.first > searchStart) {
                val between = text.substring(searchStart, match.range.first)
                val numMatch = NUMBER_REGEX.find(between)
                if (numMatch != null) {
                    spans.add(Span(baseOffset + searchStart + numMatch.range.first, baseOffset + searchStart + numMatch.range.last + 1, Numbers))
                }
            }
            val word = match.groupValues[1]
            val wordStart = baseOffset + match.range.first
            val wordEnd = baseOffset + match.range.last + 1
            when {
                isKeyword(word, language) -> spans.add(Span(wordStart, wordEnd, Keywords))
                isType(word, language) -> spans.add(Span(wordStart, wordEnd, Types))
                isAnnotation(word, language) -> spans.add(Span(wordStart, wordEnd, Annotations))
                isFunctionCall(text, match.range.first, match.range.last + 1) -> spans.add(Span(wordStart, wordEnd, Functions))
            }
            val numInMatch = NUMBER_REGEX.find(word)
            if (numInMatch != null && !isKeyword(word, language)) {
                spans.add(Span(baseOffset + match.range.first + numInMatch.range.first,
                    baseOffset + match.range.first + numInMatch.range.last + 1, Numbers))
            }
            searchStart = match.range.last + 1
        }
    }

    private fun isFunctionCall(text: String, start: Int, end: Int): Boolean {
        if (end < text.length && text[end] == '(') return true
        return false
    }

    private fun isType(word: String, language: Language): Boolean {
        return TYPES_MAP[language]?.contains(word) ?: false
    }

    private fun isAnnotation(word: String, language: Language): Boolean {
        return word.startsWith("@") || word.startsWith("#[")
    }

    private fun isKeyword(word: String, language: Language): Boolean {
        return KEYWORDS_MAP[language]?.contains(word.lowercase()) ?: false
    }

    private fun getSingleLineCommentPrefix(language: Language): String {
        return when (language) {
            Language.PYTHON, Language.BASH, Language.BASHRC, Language.ZSH, Language.BASH_PROFILE,
            Language.RUBY, Language.YAML, Language.YML, Language.TOML -> "#"
            Language.SQL -> "--"
            Language.HTML, Language.XML, Language.SVG -> "<!--"
            else -> "//"
        }
    }

    private fun findStringStart(line: String): Int {
        var minIdx = -1
        arrayOf('"', '\'', '`').forEach { quote ->
            val idx = line.indexOf(quote)
            if (idx >= 0 && (minIdx < 0 || idx < minIdx)) minIdx = idx
        }
        return minIdx
    }

    private fun findEndingQuote(text: String): Int {
        if (text.isEmpty()) return -1
        val firstChar = text[0]
        return when (firstChar) {
            '\'' -> text.indexOf('\'', 1).let { if (it <= 0) text.length - 1 else it }
            '"' -> text.indexOf('"', 1).let { if (it <= 0) text.length - 1 else it }
            '`' -> {
                var idx = 1
                while (idx < text.length) {
                    if (text[idx] == '`' && text[idx - 1] != '\\') return idx
                    idx++
                }
                text.length - 1
            }
            else -> -1
        }
    }

    private val TYPES_MAP = mapOf(
        Language.JAVA to listOf("String", "Integer", "Boolean", "Long", "Double", "Float", "Byte", "Short", "Character", "List", "Map", "Set", "ArrayList", "HashMap", "Object", "Class"),
        Language.KOTLIN to listOf("String", "Int", "Boolean", "Long", "Double", "Float", "Byte", "Short", "Char", "List", "Map", "Set", "Array", "Any", "Unit", "Nothing"),
        Language.TYPESCRIPT to listOf("string", "number", "boolean", "any", "void", "never", "unknown", "undefined", "null", "object", "Array", "Promise", "Record"),
        Language.CPP to listOf("int", "float", "double", "char", "bool", "void", "long", "short", "unsigned", "signed", "size_t", "string", "vector", "map", "set", "auto"),
        Language.C to listOf("int", "float", "double", "char", "void", "long", "short", "unsigned", "signed", "size_t"),
        Language.SWIFT to listOf("String", "Int", "Double", "Float", "Bool", "Array", "Dictionary", "Set", "Any", "Void"),
        Language.DART to listOf("String", "int", "double", "bool", "List", "Map", "Set", "dynamic", "void", "var"),
        Language.GO to listOf("int", "string", "float64", "float32", "bool", "byte", "rune", "error", "any"),
        Language.RUST to listOf("i32", "i64", "u32", "u64", "f32", "f64", "bool", "str", "String", "Vec", "Option", "Result", "Box"),
        Language.PYTHON to listOf("str", "int", "float", "bool", "list", "dict", "set", "tuple", "bytes", "object", "type", "NoneType")
    )

    private val KEYWORDS_MAP = mapOf(
        Language.PYTHON to listOf("false", "none", "true", "and", "as", "assert", "async", "await",
            "break", "class", "continue", "def", "del", "elif", "else", "except",
            "finally", "for", "from", "global", "if", "import", "in", "is",
            "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try",
            "while", "with", "yield"),
        Language.JAVASCRIPT to listOf("break", "case", "catch", "continue", "debugger", "default", "delete",
            "do", "else", "finally", "for", "function", "if", "in", "of", "instanceof",
            "new", "return", "switch", "this", "throw", "try", "typeof", "var", "void",
            "while", "with", "yield", "const", "let", "async", "await", "class", "extends", "super", "import", "export"),
        Language.TYPESCRIPT to listOf("break", "case", "catch", "continue", "debugger", "default", "delete",
            "do", "else", "finally", "for", "function", "if", "in", "of", "instanceof",
            "new", "return", "switch", "this", "throw", "try", "typeof", "var", "void",
            "while", "with", "yield", "const", "let", "async", "await", "class", "extends", "super",
            "import", "export", "interface", "type", "namespace", "enum", "implements", "private", "protected", "public", "static"),
        Language.JAVA to listOf("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "var", "records", "sealed", "permits"),
        Language.KOTLIN to listOf("package", "import", "class", "object", "interface", "fun", "val", "var",
            "data", "typealias", "constructor", "this", "super", "if", "else", "when", "is", "in",
            "throw", "try", "catch", "finally", "return", "break", "continue", "for", "while",
            "companion", "init", "public", "private", "protected", "internal",
            "abstract", "open", "final", "sealed", "enum", "annotation", "lateinit", "lazy",
            "inline", "noinline", "crossinline", "tailrec", "operator", "infix", "where", "by"),
        Language.C to listOf("auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof",
            "static", "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while"),
        Language.CPP to listOf("alignas", "alignof", "and", "and_eq", "asm", "auto", "bitand", "bitor",
            "bool", "break", "case", "catch", "char", "char8_t", "char16_t", "char32_t",
            "class", "compl", "concept", "const", "consteval", "constexpr", "constinit",
            "const_cast", "continue", "co_await", "co_return", "co_yield", "decltype", "default",
            "delete", "do", "double", "dynamic_cast", "enum", "explicit", "export", "extern",
            "false", "float", "for", "friend", "goto", "if", "inline", "int", "long", "mutable",
            "namespace", "new", "noexcept", "not", "not_eq", "nullptr", "operator", "or", "or_eq",
            "private", "protected", "public", "register", "reinterpret_cast", "requires", "return",
            "short", "signed", "sizeof", "static", "static_assert", "static_cast", "struct", "switch",
            "template", "this", "throw", "true", "try", "typedef", "typeid", "typename", "union",
            "unsigned", "using", "virtual", "void", "volatile", "wchar_t", "while", "xor", "xor_eq"),
        Language.GO to listOf("break", "case", "chan", "const", "continue", "default", "defer",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct", "switch", "type", "var"),
        Language.RUST to listOf("as", "async", "await", "break", "const", "continue", "crate", "dyn",
            "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
            "self", "static", "struct", "super", "trait", "true", "type", "unsafe", "use", "where", "while"),
        Language.RUBY to listOf("alias", "and", "begin", "break", "case", "class", "def", "defined?",
            "do", "else", "elsif", "end", "ensure", "false", "for", "if", "in",
            "module", "next", "nil", "not", "or", "require", "rescue", "retry",
            "return", "self", "super", "then", "true", "undef", "unless", "until", "when", "while", "yield"),
        Language.PHP to listOf("abstract", "and", "array", "as", "break", "callable", "case", "catch",
            "class", "clone", "const", "continue", "declare", "default", "die", "do", "echo",
            "else", "elseif", "empty", "eval", "exit", "extends", "final", "finally", "fn", "for",
            "foreach", "function", "global", "goto", "if", "implements", "include", "instanceof",
            "interface", "isset", "list", "match", "namespace", "new", "or", "print", "private",
            "protected", "public", "require", "return", "static", "switch", "throw", "trait", "try",
            "unset", "use", "var", "while", "yield"),
        Language.SWIFT to listOf("actor", "associatedtype", "async", "await", "as", "break", "case",
            "catch", "class", "continue", "convenience", "default", "defer", "deinit", "didSet", "do",
            "dynamic", "else", "enum", "extension", "fallthrough", "false", "final", "for", "func",
            "guard", "if", "import", "in", "init", "infix", "inout", "internal", "is", "lazy", "let",
            "mutating", "none", "operator", "override", "private", "protocol", "public", "repeat",
            "required", "rethrows", "return", "self", "static", "struct", "subscript", "super",
            "switch", "throw", "throws", "true", "try", "typealias", "var", "weak", "where", "while", "willSet"),
        Language.DART to listOf("abstract", "as", "assert", "async", "await", "break", "case", "catch",
            "class", "const", "continue", "covariant", "default", "deferred", "do", "dynamic", "else",
            "enum", "implements", "export", "extends", "extension", "external", "factory", "false",
            "final", "finally", "for", "function", "get", "hide", "if", "import", "in", "interface",
            "is", "lateinit", "mixin", "new", "null", "operator", "part", "required", "rethrow",
            "return", "set", "static", "super", "switch", "sync", "this", "throw", "try", "typedef",
            "var", "void", "while", "with", "yield", "show"),
        Language.BASH to listOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "in", "echo", "exit", "export", "return", "read", "set", "unset", "shift", "trap"),
        Language.BASHRC to listOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "in", "echo", "exit", "export", "return"),
        Language.ZSH to listOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "in", "echo", "exit", "export", "return"),
        Language.BASH_PROFILE to listOf("if", "then", "else", "elif", "fi", "for", "while", "until", "do", "done",
            "case", "esac", "function", "in", "echo", "exit", "export", "return"),
        Language.SQL to listOf("select", "insert", "update", "delete", "create", "drop", "alter", "table",
            "from", "where", "join", "inner", "left", "right", "outer", "on", "and", "or", "not", "null",
            "is", "as", "order", "by", "group", "having", "index", "view", "distinct", "count", "sum", "avg", "min", "max", "into"),
        Language.HTML to listOf("html", "head", "body", "div", "span", "p", "a", "img", "ul", "ol", "li",
            "h1", "h2", "h3", "h4", "h5", "h6", "form", "input", "button", "table", "tr", "td", "th",
            "select", "option", "textarea", "label", "link", "meta", "script", "style", "src", "href", "class", "id"),
        Language.CSS to listOf("color", "background", "margin", "padding", "border", "display", "position",
            "width", "height", "font", "text", "flex", "grid", "align", "justify", "overflow", "opacity", "transform"),
        Language.JSON to emptyList<String>(),
        Language.XML to emptyList<String>(),
        Language.MARKDOWN to listOf("heading", "bold", "italic", "link", "image", "code", "list"),
        Language.YAML to listOf("true", "false", "null", "yes", "no"),
        Language.YML to listOf("true", "false", "null", "yes", "no"),
        Language.TOML to listOf("true", "false"),
        Language.SVG to emptyList<String>(),
        Language.SASS to listOf("color", "background", "margin", "padding", "border", "display", "position", "width", "height"),
        Language.SCSS to listOf("color", "background", "margin", "padding", "border", "display", "position", "width", "height"),
        Language.PROPERTIES to emptyList<String>(),
        Language.ENV to emptyList<String>(),
        Language.TEXT to emptyList<String>(),
        Language.LOG to emptyList<String>(),
        Language.UNKNOWN to emptyList<String>()
    )

    private val WORD_REGEX = Regex("""([a-zA-Z_@#]\w*)""")
    private val NUMBER_REGEX = Regex("""\d+(\.\d+)?([eE][+-]?\d+)?([uU][lL]?[lL]?|[fF])?""")
}