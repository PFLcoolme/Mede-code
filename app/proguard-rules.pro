# Add project specific ProGuard rules here.

# Strong obfuscation for API key protection
-repackageclasses ''
-allowaccessmodification
-optimizations !code/allocation/variable
-optimizations !field/removal/writeonly
-optimizations !method/removal/parameter

# Keep CryptoBridge native methods
-keep class com.medemini.ai.native.CryptoBridge {
    native <methods>;
}

# Keep AI API service
-keep interface com.medemini.ai.api.AIService { *; }

# Keep ViewModel classes
-keep class com.medemini.ai.viewmodel.AIViewModel { *; }

# Keep EditorFile data class
-keep class com.medemini.model.EditorFile { *; }

# Keep Compose UI components
-keep class com.medemini.ui.** { *; }

# Keep theme classes
-keep class com.medemini.ui.theme.** { *; }

# Keep MainActivity
-keep class com.medemini.MainActivity { *; }

# Keep AppStateManager
-keep class com.medemini.AppStateManager { *; }

# Hide string constants
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Optimize aggressively
-optimizationpasses 5

# Flatten packages
-dontusemixedcaseclassnames
