with open("MadarTV/app/src/main/java/com/madar/tv/data/ArabseedRepository.kt", "r", encoding="utf-8") as f:
    code = f.read()

# Replace parseServersFast and isAdUrl
old_bad_start = "    private val _badPatterns: List<String> by lazy {"
old_bad_end = "    private fun isAdUrl(url: String): Boolean {\n\n        val lower = url.lowercase()\n\n        return _badPatterns.any { lower.contains(it) }\n\n    }"

# In case of CRLF
if "\r\n" in code:
    old_bad_end = old_bad_end.replace("\n", "\r\n")

idx_start = code.find(old_bad_start)
idx_end = code.find("    private fun cleanTitle", idx_start)

if idx_start != -1 and idx_end != -1:
    new_ad_block = """    private val AD_PATTERNS = listOf(
        "doubleclick", "googlead", "googlesyndication", "popads", "adsterra",
        "propellerads", "monetag", "trafficjunky", "histats", "onclickmega",
        "ad-maven", "adcash", "exoclick", "juicyads", "bet365", "1xbet", "melbet",
        "facebook.com", "twitter.com", "instagram.com"
    )

    private fun isAdUrl(url: String): Boolean {
        val lower = url.lowercase()
        return AD_PATTERNS.any { lower.contains(it) }
    }

"""
    if "\r\n" in code:
        new_ad_block = new_ad_block.replace("\n", "\r\n")
    code = code[:idx_start] + new_ad_block + code[idx_end:]
    print("Replaced _badPatterns successfully!")
else:
    print("Could not find _badPatterns indices:", idx_start, idx_end)

# Also enhance parseServersFast selector to include .watch-servers li, etc.
old_sel = 'for (el in doc.select("[data-link], $SEL_DLINK")) {'
new_sel = 'for (el in doc.select("[data-link], $SEL_DLINK, ul.servers li, .watch-servers li, #servers li, .servers li")) {'
if old_sel in code:
    code = code.replace(old_sel, new_sel)
    print("Updated data-link selector!")

# Also improve base64 decode check
old_b64 = 'if (link.contains("id=") && link.contains("http")) {'
new_b64 = 'if (link.contains("id=")) {'
if old_b64 in code:
    code = code.replace(old_b64, new_b64)
    print("Updated base64 decode check!")

with open("MadarTV/app/src/main/java/com/madar/tv/data/ArabseedRepository.kt", "w", encoding="utf-8") as f:
    f.write(code)
print("Finished saving ArabseedRepository.kt")
