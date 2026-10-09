-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class * extends java.lang.Enum { *; }
-keep class com.example.lynk.core.domain.** { *; }
-keep class ru.doGood.Lynk.core.domain.** { *; }
-keep class ru.doGood.Lynk.feature.dashboard.DashboardViewModel { *; }

# Hilt & Application
-keep class * extends android.app.Application { *; }
-keep class dagger.hilt.** { *; }

# Serialization (Gson/CloudConnection)
-keepclassmembers class * implements java.io.Serializable { *; }
