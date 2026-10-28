import sys

path = r'c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\data\ArabseedRepository.kt'

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. LISTING_CACHE_TTL
old_ttl = 'private const val LISTING_CACHE_TTL = 3 * 60 * 1000L // 3 minutes'
new_ttl = 'private const val LISTING_CACHE_TTL = 15 * 60 * 1000L // 15 minutes'
assert old_ttl in content, 'old_ttl not found'
content = content.replace(old_ttl, new_ttl, 1)

# 2. getRefererForUrl
old_headers = '    fun getEmbedHeaders(): Map<String, String> = mapOf(HDR_REF to BASE_SLASH)'
referer_func = """    fun getRefererForUrl(embedUrl: String): String {
        return when {
            embedUrl.contains("govid") || embedUrl.contains("mohimaserver") -> GOVID_SLASH
            embedUrl.contains("akw.cam") || embedUrl.contains("akwam") -> "https://akw.cam/"
            embedUrl.contains("wecima") || embedUrl.contains("myweecima") -> "https://myweecima.beauty/"
            embedUrl.contains("fasel") || embedUrl.contains("fasel4k") -> "https://wb49584.fasel4k.site/"
            embedUrl.contains("egybest") -> "https://egybest.mx/"
            embedUrl.contains("lodynet") -> "https://lodynet.art/"
            embedUrl.contains("brstej") -> "https://brstej.surf/"
            embedUrl.contains("q-ask") || embedUrl.contains("esheeq") -> "https://e.q-ask.video/"
            embedUrl.contains("uqload") -> "https://uqload.com/"
            embedUrl.contains("doodstream") || embedUrl.contains("dood") -> "https://doodstream.com/"
            embedUrl.contains("streamtape") -> "https://streamtape.com/"
            embedUrl.contains("vidobox") || embedUrl.contains("vidoba") -> "https://vidobox.me/"
            embedUrl.contains("govad") || embedUrl.contains("govidem") -> GOVID_SLASH
            else -> BASE_SLASH
        }
    }

    fun getEmbedHeaders(): Map<String, String> = mapOf(HDR_REF to BASE_SLASH)"""
assert old_headers in content, 'old_headers not found'
content = content.replace(old_headers, referer_func, 1)

# 3. parseListing posterUrl
old_poster = """            val posterUrl = img?.attr(A_SRC)?.ifBlank { null }

                ?: img?.attr("src")?.ifBlank { null }

                ?: img?.attr(R_DSRC)?.ifBlank { null }

                ?: img?.attr(R_DLAZY)?.ifBlank { null }"""

new_poster = """            val posterUrl = img?.attr(A_SRC)?.takeIf { it.startsWith("http") }
                ?: img?.attr("src")?.takeIf { it.startsWith("http") }
                ?: img?.attr(R_DSRC)?.takeIf { it.startsWith("http") }
                ?: img?.attr(R_DLAZY)?.takeIf { it.startsWith("http") }
                ?: img?.attr("data-original")?.takeIf { it.startsWith("http") }
                ?: img?.attr("data-lazy-src")?.takeIf { it.startsWith("http") }
                ?: img?.attr("abs:src")?.takeIf { it.startsWith("http") }"""
assert old_poster in content, 'old_poster not found'
content = content.replace(old_poster, new_poster, 1)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print('ArabseedRepository.kt updated successfully!')
