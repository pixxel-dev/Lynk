# Keep domain models and business logic
-keep class * extends java.lang.Enum { *; }
-keep class com.example.lynk.** { *; }
-keep class ru.doGood.Lynk.** { *; }

# Keep Jetpack Compose, Navigation 3 and Gson/JSON if used
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class androidx.compose.** { *; }
-keep class com.google.gson.** { *; }
