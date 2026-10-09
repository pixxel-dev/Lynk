-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class * extends java.lang.Enum { *; }
-keep class com.example.lynk.core.domain.** { *; }
-keep class ru.doGood.Lynk.core.domain.** { *; }
-keep class ru.doGood.Lynk.feature.dashboard.DashboardViewModel { *; }
