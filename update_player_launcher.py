with open("MadarTV/app/src/main/java/com/madar/tv/util/PlayerLauncher.kt", "r", encoding="utf-8") as f:
    text = f.read()

old_fun = """    fun launchPlayer(
        context: Context,
        streamUrl: String,
        title: String,
        userAgent: String = ArabseedRepository.getPlayerUserAgent(),
        referer: String = "",
        onNotInstalled: (() -> Unit)? = null
    ): Boolean {"""

new_fun = """    fun launchPlayer(
        context: Context,
        streamUrl: String,
        title: String,
        userAgent: String = ArabseedRepository.getPlayerUserAgent(),
        referer: String = "",
        backupUrls: List<String> = emptyList(),
        onNotInstalled: (() -> Unit)? = null
    ): Boolean {"""

if "\r\n" in text:
    old_fun = old_fun.replace("\n", "\r\n")
    new_fun = new_fun.replace("\n", "\r\n")

text = text.replace(old_fun, new_fun)

# Add putExtra for backup urls
extra_old = """putExtra("com.madar.player.extra.REFERER", referer)"""
extra_new = """putExtra("com.madar.player.extra.REFERER", referer)
                putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", ArrayList(backupUrls))
                putStringArrayListExtra("backup_stream_urls", ArrayList(backupUrls))"""

if "\r\n" in text:
    extra_old = extra_old.replace("\n", "\r\n")
    extra_new = extra_new.replace("\n", "\r\n")

text = text.replace(extra_old, extra_new)

with open("MadarTV/app/src/main/java/com/madar/tv/util/PlayerLauncher.kt", "w", encoding="utf-8") as f:
    f.write(text)

print("Updated PlayerLauncher.kt")
