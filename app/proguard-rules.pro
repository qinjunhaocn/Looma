# Looma ProGuard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
