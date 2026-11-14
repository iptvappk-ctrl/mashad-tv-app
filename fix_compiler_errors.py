# 1. Fix PlayerLauncher.kt
with open("MadarTV/app/src/main/java/com/madar/tv/util/PlayerLauncher.kt", "r", encoding="utf-8") as f:
    text = f.read()

text = text.replace(
    'putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", ArrayList(backupUrls))\n                putStringArrayListExtra("backup_stream_urls", ArrayList(backupUrls))',
    'launchIntent.putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", ArrayList(backupUrls))\n                launchIntent.putStringArrayListExtra("backup_stream_urls", ArrayList(backupUrls))'
)
text = text.replace(
    'putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", ArrayList(backupUrls))\r\n                putStringArrayListExtra("backup_stream_urls", ArrayList(backupUrls))',
    'launchIntent.putStringArrayListExtra("com.madar.player.extra.BACKUP_URLS", ArrayList(backupUrls))\r\n                launchIntent.putStringArrayListExtra("backup_stream_urls", ArrayList(backupUrls))'
)

with open("MadarTV/app/src/main/java/com/madar/tv/util/PlayerLauncher.kt", "w", encoding="utf-8") as f:
    f.write(text)
print("Fixed PlayerLauncher.kt!")

# 2. Fix LiveTvScreen.kt
with open("MadarTV/app/src/main/java/com/madar/tv/ui/LiveTvScreen.kt", "r", encoding="utf-8") as f:
    text = f.read()

old_s = """                                        val best = streams.firstOrNull {
                                            it.name.contains("720") || it.name.contains("HD", ignoreCase = true)
                                        } ?: streams.first()
                                        onStreamSelected(best, "${event.team1.name} vs ${event.team2.name}")"""

new_s = """                                        val valid = streams.filter { it.url.isNotBlank() && !it.url.contains("rex.new-redirect.online") }
                                        val best = valid.firstOrNull {
                                            it.name.contains("720") || it.name.contains("HD", ignoreCase = true)
                                        } ?: valid.firstOrNull() ?: streams.first()
                                        val backups = valid.filter { it.url != best.url }.map { it.url }
                                        onStreamSelected(best, "${event.team1.name} vs ${event.team2.name}", backups)"""

if old_s in text:
    text = text.replace(old_s, new_s)
elif old_s.replace("\n", "\r\n") in text:
    text = text.replace(old_s.replace("\n", "\r\n"), new_s.replace("\n", "\r\n"))
else:
    print("Could not find old_s in LiveTvScreen.kt")

with open("MadarTV/app/src/main/java/com/madar/tv/ui/LiveTvScreen.kt", "w", encoding="utf-8") as f:
    f.write(text)
print("Fixed LiveTvScreen.kt!")
