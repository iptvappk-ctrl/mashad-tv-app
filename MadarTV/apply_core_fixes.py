import sys, os, re
sys.stdout.reconfigure(encoding='utf-8')

print("Applying deep fixes to ArabseedRepository.kt, MultiProviderRepository.kt, and ArabseedScreen.kt...")

# ==============================================================================
# 1. ArabseedRepository.kt
# ==============================================================================
repo_path = r'c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\data\ArabseedRepository.kt'
with open(repo_path, 'r', encoding='utf-8') as f:
    text = f.read()

start_marker = 'val rawEpisodes = mutableListOf<Pair<String, String>>()'
end_marker = 'val bundle = ArabseedSeasonBundle('
s_idx = text.find(start_marker)
e_idx = text.find(end_marker)

assert s_idx != -1, "start_marker not found in ArabseedRepository.kt"
assert e_idx != -1, "end_marker not found in ArabseedRepository.kt"

new_repo_core = """val rawEpisodes = mutableListOf<Pair<String, String>>()
                val episodeSelectors = listOf(
                    "a.episode__item", ".episode__item a",
                    "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9']",
                    "a[href*='الحلقة']", "a[href*='-episode-']", "a[href*='/episode/']",
                    ".Episodes--List a", ".EpisodeBlock a",
                    ".episodes-list a", ".EpisodesList li a", ".eplister li a",
                    ".eps-list li a", ".eps a", "a.ep-item", ".episodios li a", ".eplist a"
                )

                fun collectEpisodes(d: org.jsoup.nodes.Document): List<Pair<String, String>> {
                    var best = mutableListOf<Pair<String, String>>()
                    for (sel in episodeSelectors) {
                        val els = d.select(sel)
                        if (els.isNotEmpty()) {
                            val current = mutableListOf<Pair<String, String>>()
                            for (el in els) {
                                val href = el.attr("abs:href").ifBlank { el.attr("href") }
                                val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                                if (href.isBlank() || !href.startsWith(P_HTTP) || href.endsWith("/home/") || href == BASE_URL) continue
                                if (href.contains("/category/") || href.contains("/tag/") || href.contains("/genre/") || href.contains("/actor/")) continue

                                val isEp = href.contains("episode", ignoreCase = true)
                                    || href.contains("%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9")
                                    || href.contains("الحلقة")
                                    || txt.contains("الحلقة")
                                    || txt.contains("حلقة")
                                    || txt.matches(Regex(\"\"\"^\\d+$\"\"\"))

                                if (isEp && current.none { it.second == href }) {
                                    current += txt to href
                                }
                            }
                            if (current.size > best.size) {
                                best = current
                            }
                        }
                    }
                    return best
                }

                val docEps = collectEpisodes(doc)
                for (ep in docEps) {
                    if (rawEpisodes.none { it.second == ep.second }) rawEpisodes += ep
                }

                // If few episodes found, search for breadcrumbs / parent series page
                if (rawEpisodes.size <= 4) {
                    val seriesLinkSelectors = listOf(
                        "a[href*='/series/']", "a[href*='/season/']", "a[href*='الموسم']",
                        ".breadcrumb a", "ol.breadcrumb a", ".SeriesTitle a", ".series-title a",
                        ".entry-breadcrumb a", "a.SeriesLink"
                    )
                    for (sel in seriesLinkSelectors) {
                        val links = doc.select(sel)
                        val parent = links.lastOrNull { el ->
                            val href = el.attr("abs:href").ifBlank { el.attr("href") }
                            val txt = el.text().trim()
                            href.isNotBlank() && href.startsWith(P_HTTP) && href != item.pageUrl
                                && !txt.contains("الرئيسية") && !txt.contains("home", ignoreCase = true)
                                && !txt.contains("الحلقة") && !txt.contains("حلقة")
                        }
                        if (parent != null) {
                            val parentHref = parent.attr("abs:href").ifBlank { parent.attr("href") }
                            try {
                                val parentDoc = Jsoup.parse(fetchHtml(parentHref), BASE_URL)
                                parentDoc.select("footer, script, style").remove()
                                val parentEps = collectEpisodes(parentDoc)
                                if (parentEps.size > rawEpisodes.size) {
                                    rawEpisodes.clear()
                                    rawEpisodes.addAll(parentEps)
                                    break
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }

                // Group into seasons with accurate episode numbering
                val seasons = mutableListOf<ArabseedSeason>()
                if (rawEpisodes.isNotEmpty()) {
                    val seasonMap = LinkedHashMap<Int, MutableList<ArabseedEpisode>>()
                    rawEpisodes.forEachIndexed { index, (title, url) ->
                        val seasonNum = Regex(\"\"\"(?:الموسم|الجزء)\\s*([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                            .find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: Regex(\"\"\"(?:season|part)[-_ ]?([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: 1

                        val epNum = Regex(\"\"\"(?:الحلقة|حلقة)\\s*([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                            .find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: Regex(\"\"\"^([0-9]+)$\"\"\").find(title.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: Regex(\"\"\"(?:%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9|الحلقة|episode|ep)[-_ ]?([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                                .find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
                            ?: Regex(\"\"\"([0-9]+)\"\"\").findAll(title).lastOrNull()?.value?.toIntOrNull()
                            ?: (index + 1)

                        val cleanEpTitle = "الحلقة $epNum"
                        seasonMap.getOrPut(seasonNum) { mutableListOf() }
                            .add(ArabseedEpisode(epNum, cleanEpTitle, url, seasonNum))
                    }

                    seasonMap.entries.sortedBy { it.key }.forEach { (sNum, eps) ->
                        val sortedEps = eps.distinctBy { it.pageUrl }.sortedBy { it.number }
                        seasons += ArabseedSeason(
                            number = sNum,
                            title = when (sNum) {
                                1 -> "الموسم الأول"; 2 -> "الموسم الثاني"; 3 -> "الموسم الثالث"
                                4 -> "الموسم الرابع"; 5 -> "الموسم الخامس"; 6 -> "الموسم السادس"
                                else -> "الموسم $sNum"
                            },
                            episodes = sortedEps,
                        )
                    }
                }

                // Find related parts/seasons linked from the page (EXCLUDING any episodes)
                val relatedParts = mutableListOf<ArabseedItem>()
                val partSelectors = listOf(
                    ".Seasons--List a", ".seasons-list a", ".parts-list a",
                    ".SeasonBlock a", ".related-series a",
                    "a[href*='%d8%a7%d9%84%d8%ac%d8%b2%d8%a1']", "a[href*='الجزء']",
                    "a[href*='%d8%a7%d9%84%d9%85%d9%88%d8%b3%d9%85']", "a[href*='الموسم']",
                    "a[href*='part']", "a[href*='season']"
                )
                for (sel in partSelectors) {
                    for (el in doc.select(sel)) {
                        val href = el.attr("abs:href").ifBlank { el.attr("href") }
                        val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                        if (href.isNotBlank() && href.startsWith(P_HTTP)
                            && href != item.pageUrl && txt.isNotBlank()
                        ) {
                            val isEpisodeLike = txt.contains("الحلقة") || txt.contains("حلقة")
                                || href.contains("%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9")
                                || href.contains("الحلقة")
                                || href.contains("episode", ignoreCase = true)
                            val isPart = !isEpisodeLike && (
                                txt.contains("جزء") || txt.contains("موسم") || txt.contains("الجزء") || txt.contains("الموسم")
                                || href.contains("season") || href.contains("part")
                            )
                            if (isPart && relatedParts.none { it.pageUrl == href }) {
                                val img = el.selectFirst("img")?.attr("abs:src")
                                    ?.ifBlank { el.selectFirst("img")?.attr("src") }
                                    ?: item.posterUrl
                                relatedParts += ArabseedItem(
                                    id = href.trimEnd('/').substringAfterLast('/').take(80),
                                    title = txt, arabicTitle = txt,
                                    posterUrl = img, year = item.year,
                                    quality = item.quality, rating = item.rating,
                                    type = ArabseedType.SERIES, pageUrl = href,
                                    category = item.category,
                                )
                            }
                        }
                    }
                    if (relatedParts.isNotEmpty()) break
                }

                // Additional smart discovery: search for other seasons/parts (EXCLUDING any episodes)
                val cleanBaseTitle = item.title
                    .replace(Regex(\"\"\"(?:الموسم|الجزء|موسم|جزء)[ -]?[0-9]+.*\"\"\", RegexOption.IGNORE_CASE), "")
                    .replace(Regex(\"\"\"(?:الحلقة|حلقة)[ -]?[0-9]+.*\"\"\", RegexOption.IGNORE_CASE), "")
                    .replace(Regex(\"(?:مترجم|مدبلج|فيلم|مسلسل|WEB-DL|720p|1080p|HD).*\", RegexOption.IGNORE_CASE), "")
                    .trim()

                if (cleanBaseTitle.length >= 3) {
                    try {
                        val searchResults = search(cleanBaseTitle)
                        for (sr in searchResults) {
                            val srTitle = sr.title
                            val srUrl = sr.pageUrl
                            val isEp = srTitle.contains("الحلقة") || srTitle.contains("حلقة")
                                || srUrl.contains("%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9")
                                || srUrl.contains("episode", ignoreCase = true)

                            val isPartOrSeason = !isEp && (
                                srTitle.contains("موسم") || srTitle.contains("الموسم")
                                || srTitle.contains("جزء") || srTitle.contains("الجزء")
                                || srTitle.contains("season", ignoreCase = true)
                                || srTitle.contains("part", ignoreCase = true)
                            )

                            if (isPartOrSeason && srUrl != item.pageUrl && srTitle.contains(cleanBaseTitle, ignoreCase = true)) {
                                if (relatedParts.none { it.pageUrl == srUrl }) {
                                    relatedParts += sr
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // Final safety filter: NO EPISODES in relatedParts
                val finalRelatedParts = relatedParts.filter { part ->
                    val t = part.title
                    val u = part.pageUrl
                    !t.contains("الحلقة") && !t.contains("حلقة")
                        && !u.contains("%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9")
                        && !u.contains("episode", ignoreCase = true)
                }.distinctBy { it.pageUrl }.take(12)

                """

# Replace in text
text = text[:s_idx] + new_repo_core + text[e_idx:]
# Also replace relatedParts = relatedParts.distinctBy... with finalRelatedParts
text = text.replace('relatedParts = relatedParts.distinctBy { it.pageUrl }.take(12)', 'relatedParts = finalRelatedParts')

# Check braces
assert text.count('{') == text.count('}'), f"ArabseedRepository brace imbalance! {{={text.count('{')}, }}={text.count('}')}"

with open(repo_path, 'w', encoding='utf-8') as f:
    f.write(text)
print("✓ ArabseedRepository.kt updated successfully!")


# ==============================================================================
# 2. MultiProviderRepository.kt
# ==============================================================================
multi_path = r'c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\data\MultiProviderRepository.kt'
with open(multi_path, 'r', encoding='utf-8') as f:
    mtext = f.read()

# Fix episode number extraction in MultiProvider
old_mep = """            val episodes = rawEpisodes.mapIndexed { idx, (title, href) ->
                val epNum = Regex(\"\"\"(?:الحلقة|حلقة|ep|e)[ -]?([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                    .find(href + " " + title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex(\"\"\"([0-9]+)\"\"\").findAll(title).lastOrNull()?.value?.toIntOrNull()
                    ?: (idx + 1)
                ArabseedEpisode(
                    number = epNum,
                    title = "الحلقة $epNum",
                    pageUrl = href,
                    seasonNumber = 1,
                )
            }.distinctBy { it.pageUrl }.sortedBy { it.number }"""

new_mep = """            val episodes = rawEpisodes.mapIndexed { idx, (title, href) ->
                val epNum = Regex(\"\"\"(?:الحلقة|حلقة)\\s*([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                    .find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex(\"\"\"^([0-9]+)$\"\"\").find(title.trim())?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: Regex(\"\"\"(?:%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9|الحلقة|episode|ep)[-_ ]?([0-9]+)\"\"\", RegexOption.IGNORE_CASE)
                        .find(href)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: (idx + 1)
                ArabseedEpisode(
                    number = epNum,
                    title = "الحلقة $epNum",
                    pageUrl = href,
                    seasonNumber = 1,
                )
            }.distinctBy { it.pageUrl }.sortedBy { it.number }"""

if old_mep in mtext:
    mtext = mtext.replace(old_mep, new_mep)
    print("✓ MultiProviderRepository.kt episode extraction fixed!")
else:
    print("! MultiProvider episode target not found directly, checking partial...")

assert mtext.count('{') == mtext.count('}'), f"MultiProvider brace imbalance! {{={mtext.count('{')}, }}={mtext.count('}')}"
with open(multi_path, 'w', encoding='utf-8') as f:
    f.write(mtext)


# ==============================================================================
# 3. ArabseedScreen.kt
# ==============================================================================
screen_path = r'c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\ui\ArabseedScreen.kt'
with open(screen_path, 'r', encoding='utf-8') as f:
    stext = f.read()

# Fix currentEpIdx calculation to have number fallback
old_cidx_block = """    val currentEpIdx = remember(item.pageUrl, allEpisodes) {

        val normalised = item.pageUrl.trimEnd('/')

        allEpisodes.indexOfFirst { it.pageUrl.trimEnd('/') == normalised }

    }"""

new_cidx_block = """    val currentEpIdx = remember(item.pageUrl, allEpisodes) {
        val normalised = item.pageUrl.trimEnd('/')
        val urlIdx = allEpisodes.indexOfFirst { it.pageUrl.trimEnd('/') == normalised }
        if (urlIdx >= 0) {
            urlIdx
        } else {
            val numFromTitle = Regex(\"\"\"(?:الحلقة|حلقة)\\s*([0-9]+)\"\"\").find(item.title)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: Regex(\"\"\"(?:%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9|episode|ep)[-_ ]?([0-9]+)\"\"\").find(item.pageUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            if (numFromTitle != null) {
                allEpisodes.indexOfFirst { it.number == numFromTitle }
            } else {
                -1
            }
        }
    }"""

# Try exact or normalized replacement
if old_cidx_block in stext:
    stext = stext.replace(old_cidx_block, new_cidx_block)
    print("✓ currentEpIdx robust matching updated!")
else:
    # Use regex
    pattern = re.compile(r'val currentEpIdx = remember\(item\.pageUrl,\s*allEpisodes\)\s*\{[^}]+\}')
    if pattern.search(stext):
        stext = pattern.sub(new_cidx_block, stext, count=1)
        print("✓ currentEpIdx updated via regex!")
    else:
        print("! currentEpIdx target not found!")

# Fix previous & next button labels: display actual episode title/number, not index!
stext = re.sub(
    r'Text\(\s*"→\s*الحلقة السابقة\s*\([^)]*\)"',
    'Text("→ السابق: ${prevEp.title}"',
    stext
)
stext = re.sub(
    r'Text\(\s*"الحلقة التالية\s*\([^)]*\)\s*←"',
    'Text("التالي: ${nextEp.title} ←"',
    stext
)
print("✓ Previous and Next button text updated to show actual episode title!")

assert stext.count('{') == stext.count('}'), f"ArabseedScreen brace imbalance! {{={stext.count('{')}, }}={stext.count('}')}"
with open(screen_path, 'w', encoding='utf-8') as f:
    f.write(stext)
print("✓ ArabseedScreen.kt updated successfully!")
