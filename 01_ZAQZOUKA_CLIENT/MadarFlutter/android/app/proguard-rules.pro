# WorkManager creates this Room-generated database with reflection. R8 was
# keeping the class but removing its no-argument constructor, so AndroidX
# Startup crashed before MainActivity could be created.
-keep class androidx.work.impl.WorkDatabase_Impl {
    public <init>();
}
