# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep model classes
-keep class com.medecode.model.** { *; }

# Keep editor classes
-keep class com.medecode.editor.** { *; }

# Keep UI theme classes
-keep class com.medecode.ui.theme.** { *; }

# Keep MainActivity
-keep class com.medecode.MainActivity { *; }