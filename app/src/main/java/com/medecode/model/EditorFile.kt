package com.medecode.model

/**
 * Represents an open file in the editor
 */
data class EditorFile(
    val name: String,
    val path: String,
    var content: String,
    val language: Language = detectLanguage(path)
) {
    val extension: String = path.substringAfterLast('.', "").lowercase()
    
    companion object {
        fun detectLanguage(path: String): Language {
            val ext = path.substringAfterLast('.', "").lowercase()
            return Language.fromExtension(ext)
        }
    }
}

/**
 * Supported programming languages
 */
enum class Language(val extension: String, val displayName: String) {
    // Python
    PYTHON("py", "Python"),
    
    // JavaScript
    JAVASCRIPT("js", "JavaScript"),
    
    // TypeScript
    TYPESCRIPT("ts", "TypeScript"),
    
    // Java
    JAVA("java", "Java"),
    
    // Kotlin
    KOTLIN("kt", "Kotlin"),
    KELINT_SCRIPT("kts", "Kotlin Script"),
    
    // C/C++
    C("c", "C"),
    CPP("cpp", "C++"),
    H("h", "C Header"),
    HPP("hpp", "C++ Header"),
    
    // Go
    GO("go", "Go"),
    
    // Rust
    RUST("rs", "Rust"),
    
    // Ruby
    RUBY("rb", "Ruby"),
    
    // PHP
    PHP("php", "PHP"),
    
    // Swift
    SWIFT("swift", "Swift"),
    
    // Dart
    DART("dart", "Dart"),
    
    // Shell scripts
    BASH("sh", "Shell"),
    BASHRC("bashrc", "Shell"),
    ZSH("zsh", "Shell"),
    BASH_PROFILE("bash_profile", "Shell"),
    
    // Web
    HTML("html", "HTML"),
    CSS("css", "CSS"),
    SASS("sass", "Sass"),
    SCSS("scss", "Scss"),
    JSON("json", "JSON"),
    XML("xml", "XML"),
    SVG("svg", "SVG"),
    
    // Markdown
    MARKDOWN("md", "Markdown"),
    
    // Configuration
    YAML("yaml", "YAML"),
    YML("yml", "YAML"),
    TOML("toml", "TOML"),
    PROPERTIES("properties", "Properties"),
    ENV("env", "Environment"),
    
    // SQL
    SQL("sql", "SQL"),
    
    // Other
    TEXT("txt", "Text"),
    LOG("log", "Log"),
    
    // Unknown
    UNKNOWN("", "Unknown");
    
    companion object {
        fun fromExtension(ext: String): Language {
            return values().find { it.extension == ext } ?: UNKNOWN
        }
    }
}