-keep class org.f7.motoactions.MotoActionsApp { *; }
-keep class org.f7.motoactions.service.** { *; }
-keep class org.f7.motoactions.ui.** { *; }
-keep class org.f7.motoactions.detect.** { *; }
-keep class org.f7.motoactions.util.** { *; }

# Compose / Kotlin
-dontwarn androidx.compose.**
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
