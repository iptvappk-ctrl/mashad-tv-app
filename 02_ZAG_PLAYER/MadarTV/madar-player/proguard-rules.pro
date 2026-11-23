-keep class com.madar.player.CastOptionsProvider { *; }

# Start.io SDK
-keepattributes Exceptions, InnerClasses, Signature, Deprecated, SourceFile, LineNumberTable, *Annotation*, EnclosingMethod
-dontwarn com.startapp.**
-keep class com.startapp.** { *; }
-keep interface com.startapp.** { *; }
