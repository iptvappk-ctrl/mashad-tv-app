# WorkManager creates this Room-generated database with reflection. R8 was
# keeping the class but removing its no-argument constructor, so AndroidX
# Startup crashed before MainActivity could be created.
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}

# Start.io SDK
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn com.startapp.**
-keep class com.startapp.** { *; }
-keep interface com.startapp.** { *; }
