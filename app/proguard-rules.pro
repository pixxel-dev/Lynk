# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- Pine (ART Hooking в стиле subver55) ---
-keep class top.canyie.pine.** { *; }
-dontwarn top.canyie.pine.**

# --- HiddenApiBypass (доступ к UserHandle, startActivityAsUser, getCurrentUser) ---
-keep class org.lsposed.hiddenapibypass.** { *; }
-dontwarn org.lsposed.hiddenapibypass.**

# --- Shizuku API (рефлексивный вызов Shizuku.newProcess) ---
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# --- Application and Hilt ---
-keep class ru.doGood.Lynk.LynkApplication { *; }
-keep class * extends android.app.Application { *; }
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
