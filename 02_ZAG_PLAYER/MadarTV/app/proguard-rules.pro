# R8 runs in optimized mode for release builds. Keep only metadata that may be
# needed by Kotlin/Compose and Android library consumers; avoid broad package
# keeps that would disable shrinking and renaming across whole dependencies.
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Remove Android log calls from optimized release code.
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
    public static boolean isLoggable(...);
}
