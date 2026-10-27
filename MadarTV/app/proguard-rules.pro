# ╔══════════════════════════════════════════════════════════════════════════╗
# ║         MASHAD — R8 Full-Mode Obfuscation Configuration                ║
# ║  Every class, method, field is renamed to single-letter identifiers.    ║
# ║  All source information is stripped. Dead code is eliminated.           ║
# ╚══════════════════════════════════════════════════════════════════════════╝

# ── R8 Aggressive Optimization ────────────────────────────────────────────
-optimizationpasses 7
-overloadaggressively
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# ── Strip Kotlin Intrinsics (removes parameter name strings from DEX) ──────
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    static void throwUninitializedPropertyAccessException(java.lang.String);
}

# ── Strip all debug / source information ──────────────────────────────────
-renamesourcefileattribute ''
-keepattributes !SourceFile,!LineNumberTable,!LocalVariableTable,!LocalVariableTypeTable,!EnclosingMethod,!Exceptions

# Only keep Signature + InnerClasses + Annotation (required by Compose/Kotlin reflection)
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# ── Strip all logging calls entirely ──────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}
-assumenosideeffects class java.io.PrintStream {
    public void println(...);
    public void print(...);
}

# ── Strip Kotlin metadata (harder to decompile) ───────────────────────────
-dontwarn kotlin.Metadata
-dontwarn kotlin.reflect.**

# ── Required Android keeps ────────────────────────────────────────────────
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
}

# ── Jetpack Compose ───────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class kotlin.coroutines.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Kotlin Coroutines ─────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── OkHttp / Okio ────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Jsoup ────────────────────────────────────────────────────────────────
-keep class org.jsoup.** { *; }

# ── Coil image loader ─────────────────────────────────────────────────────
-keep class coil.** { *; }
-dontwarn coil.**

# ── JSON ──────────────────────────────────────────────────────────────────
-keep class org.json.** { *; }

# ── Suppress common warnings ──────────────────────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn **$$Lambda$*
-dontwarn javax.annotation.**
-dontwarn sun.misc.**
