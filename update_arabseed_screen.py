with open("MadarTV/app/src/main/java/com/madar/tv/ui/ArabseedScreen.kt", "r", encoding="utf-8") as f:
    code = f.read()

# 1. Update episodeItem
code = code.replace(
    "parentSeriesPageUrl = item.parentSeriesPageUrl?.takeIf(String::isNotBlank),",
    "parentSeriesPageUrl = item.parentSeriesPageUrl?.takeIf(String::isNotBlank) ?: item.pageUrl,"
)

# 2. Update retry button to check both Arabseed and MultiProvider if empty
old_retry = """                                            val srvs = withContext(Dispatchers.IO) {
                                                if (item.providerName == "عرب سيد") ArabseedRepository.fetchServersFast(item.pageUrl)
                                                else MultiProviderRepository.fetchServers(item.pageUrl)
                                            }"""

new_retry = """                                            val srvs = withContext(Dispatchers.IO) {
                                                val s = if (item.providerName == "عرب سيد") ArabseedRepository.fetchServersFast(item.pageUrl)
                                                else MultiProviderRepository.fetchServers(item.pageUrl)
                                                if (s.isEmpty()) {
                                                    if (item.providerName == "عرب سيد") MultiProviderRepository.fetchServers(item.pageUrl)
                                                    else ArabseedRepository.fetchServersFast(item.pageUrl)
                                                } else s
                                            }"""

if "\r\n" in code:
    old_retry = old_retry.replace("\n", "\r\n")
    new_retry = new_retry.replace("\n", "\r\n")

if old_retry in code:
    code = code.replace(old_retry, new_retry)
    print("Updated retry block!")
else:
    print("Could not find exact old_retry string")

with open("MadarTV/app/src/main/java/com/madar/tv/ui/ArabseedScreen.kt", "w", encoding="utf-8") as f:
    f.write(code)
print("Finished updating ArabseedScreen.kt")
