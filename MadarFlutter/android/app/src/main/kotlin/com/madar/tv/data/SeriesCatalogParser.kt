package com.madar.tv.data

import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import java.net.URLDecoder

/** Shared, conservative parsing for episode/season/part labels found in provider pages. */
internal object SeriesCatalogParser {
    private val episodeNumberPattern = Regex(
        """(?i)(?:الحلقة|حلقة|episode|(?<![a-z])eps?|(?<![a-z])ep)[\s._#=/:-]*([0-9]{1,4})(?![0-9])""",
    )
    private val seasonNumberPattern = Regex(
        """(?i)(?:الموسم|موسم|(?<![a-z])season)[\s._#=/:-]*([0-9]{1,2})(?![0-9])""",
    )
    private val compactSeasonEpisodePattern = Regex(
        """(?i)(?:^|[^a-z])s[\s._#=/:-]*([0-9]{1,2})[\s._#=/:-]*e(?:p(?:isode)?|ps)?[\s._#=/:-]*[0-9]{1,4}(?![0-9])""",
    )
    private val standaloneSeasonTokenPattern = Regex(
        """(?i)(?:^|/)s([0-9]{1,2})(?:/?(?:[?#].*)?)$""",
    )
    private val partNumberPattern = Regex(
        """(?i)(?:الجزء|جزء|(?<![a-z])part)[\s._#=/:-]*([0-9]{1,3})(?![0-9])""",
    )
    private val seasonOrdinalPattern = Regex(
        """(?i)(?:الموسم|موسم|(?<![a-z])season)[\s._#=/:-]*(الاولى|الاول|الثانية|الثاني|الثالثة|الثالث|الرابعة|الرابع|الخامسة|الخامس|السادسة|السادس|السابعة|السابع|الثامنة|الثامن|التاسعة|التاسع|العاشرة|العاشر)""",
    )
    private val partOrdinalPattern = Regex(
        """(?i)(?:الجزء|جزء|(?<![a-z])part)[\s._#=/:-]*(الاولى|الاول|الثانية|الثاني|الثالثة|الثالث|الرابعة|الرابع|الخامسة|الخامس|السادسة|السادس|السابعة|السابع|الثامنة|الثامن|التاسعة|التاسع|العاشرة|العاشر)""",
    )
    private val ordinals = mapOf(
        "الاول" to 1, "الاولى" to 1,
        "الثاني" to 2, "الثانية" to 2,
        "الثالث" to 3, "الثالثة" to 3,
        "الرابع" to 4, "الرابعة" to 4,
        "الخامس" to 5, "الخامسة" to 5,
        "السادس" to 6, "السادسة" to 6,
        "السابع" to 7, "السابعة" to 7,
        "الثامن" to 8, "الثامنة" to 8,
        "التاسع" to 9, "التاسعة" to 9,
        "العاشر" to 10, "العاشرة" to 10,
    )
    private val episodeMarkerPattern = Regex("""(?i)(الحلقة|حلقة|episode|(?<![a-z])eps?(?:[\s._#=/:-]|[0-9]|$))""")
    private val partMarkerPattern = Regex("""(?i)(الجزء|جزء|(?<![a-z])part(?:[\s._#=/:-]|[0-9]|$))""")

    fun normalizeDigits(value: String): String = buildString(value.length) {
        value.forEach { char ->
            append(
                when (char) {
                    in '٠'..'٩' -> '0' + (char - '٠')
                    in '۰'..'۹' -> '0' + (char - '۰')
                    else -> char
                },
            )
        }
    }

    fun normalizedUrl(value: String): String = decodeUrl(value)
        .trim()
        .trimEnd('/')
        .lowercase()

    fun episodeNumber(title: String, url: String, allowNumericTitle: Boolean = false): Int? {
        val titleText = normalizeText(title)
        val urlText = normalizeText(url)
        val labeled = sequenceOf(titleText, urlText)
            .mapNotNull { episodeNumberPattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .firstOrNull { it > 0 }
        if (labeled != null) return labeled

        if (allowNumericTitle) {
            return normalizeDigits(titleText).trim().toIntOrNull()?.takeIf { it in 1..9999 }
        }
        return null
    }

    fun seasonNumber(title: String, url: String): Int? {
        extractLabeledNumber(
            title = title,
            url = url,
            numberPattern = seasonNumberPattern,
            ordinalPattern = seasonOrdinalPattern,
        )?.let { return it }

        val normalizedTitle = normalizeText(title)
        val normalizedUrl = normalizeText(url)
        sequenceOf(normalizedTitle, normalizedUrl).forEach { value ->
            compactSeasonEpisodePattern.find(value)?.groupValues?.getOrNull(1)
                ?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
        }
        // Accept a stand-alone S2 path/title token, but never treat an arbitrary "s5"
        // fragment inside a longer slug as a season label.
        sequenceOf(normalizedTitle, normalizedUrl).forEach { value ->
            standaloneSeasonTokenPattern.find(value)?.groupValues?.getOrNull(1)
                ?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
        }
        return null
    }

    /**
     * Reads a season label from the episode link or its nearby DOM container.
     * Episode URLs often omit the season, while the provider groups links under
     * containers such as `season-2` or `data-season="2"`.
     */
    fun seasonNumberFromElement(element: Element, title: String, url: String): Int? {
        seasonNumber(title, url)?.let { return it }

        val seasonAttributes = listOf("data-season-number", "data-season", "data-season_num")
        val seasonContainerMarker = Regex("""(?i)(season|الموسم|موسم)""")
        var node: Element? = element
        var depth = 0
        while (node != null && depth < 5) {
            val marker = "${node.id()} ${node.className()}"
            val isSeasonContainer = seasonContainerMarker.containsMatchIn(marker)
            for (attribute in seasonAttributes) {
                val value = node.attr(attribute).trim()
                if (value.isNotBlank()) {
                    value.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
                    seasonNumber(value, "")?.let { return it }
                }
            }

            // Generic wrappers often contain tabs for several seasons. Do not inherit a
            // title/label from those distant wrappers; only inspect labels on an element
            // which is itself explicitly marked as a season container.
            if (isSeasonContainer) {
                listOf(node.id(), node.className(), node.attr("title"), node.attr("aria-label"))
                    .firstNotNullOfOrNull { seasonNumber(it, "") }
                    ?.let { return it }
            }

            val childSeasonNumbers = node.children()
                .filter { child ->
                    child.tagName() in setOf("h1", "h2", "h3", "h4", "h5", "button")
                        || child.attr("role").equals("tab", ignoreCase = true)
                }
                .take(6)
                .mapNotNull { seasonNumber(it.text().take(100), "") }
                .distinct()
            if (childSeasonNumbers.size == 1 && (isSeasonContainer || depth <= 2)) {
                return childSeasonNumbers.single()
            }

            val nodeMarker = "${node.className()} ${node.id()}".lowercase()
            val isEpisodeList = listOf("episode", "episodes", "eplist", "eps-list", "episodios")
                .any(nodeMarker::contains)
            if (isEpisodeList || isSeasonContainer) {
                var sibling = node.previousElementSibling()
                var siblingCount = 0
                while (sibling != null && siblingCount < 2) {
                    val isSeasonLabel = sibling.tagName() in setOf("h1", "h2", "h3", "h4", "h5", "button")
                        || seasonContainerMarker.containsMatchIn("${sibling.className()} ${sibling.id()}")
                    if (isSeasonLabel) {
                        seasonNumber(sibling.text().take(100), "")?.let { return it }
                    }
                    sibling = sibling.previousElementSibling()
                    siblingCount++
                }
            }

            node = node.parent()
            depth++
        }

        return null
    }

    /** Links explicitly labelled as a season, used to fetch each season page on sources
     * which only render the currently selected season's episode list in the page body. */
    fun linkedSeasonPages(document: Document, currentPageUrl: String): List<Pair<Int, String>> =
        document.select("a[href]")
            .mapNotNull { link ->
                val href = link.attr("abs:href").ifBlank { link.attr("href") }.trim()
                val title = link.text().trim().ifBlank {
                    link.attr("title").ifBlank { link.attr("aria-label") }
                }
                if (href.isBlank() || !href.startsWith("http", ignoreCase = true) ||
                    normalizedUrl(href) == normalizedUrl(currentPageUrl) || isEpisode(title, href)
                ) return@mapNotNull null
                val number = seasonNumber(title, href) ?: return@mapNotNull null
                number to href
            }
            .distinctBy { normalizedUrl(it.second) }
            .sortedBy { it.first }
            .take(10)

    fun partNumber(title: String, url: String): Int? = extractLabeledNumber(
        title = title,
        url = url,
        numberPattern = partNumberPattern,
        ordinalPattern = partOrdinalPattern,
    )

    fun isEpisode(title: String, url: String): Boolean {
        val combined = "${normalizeText(title)} ${normalizeText(url)}"
        return episodeMarkerPattern.containsMatchIn(combined) || episodeNumber(title, url) != null
    }

    fun hasPartMarker(title: String, url: String): Boolean {
        val combined = "${normalizeText(title)} ${normalizeText(url)}"
        return partMarkerPattern.containsMatchIn(combined)
    }

    fun seasonTitle(number: Int): String = when (number) {
        1 -> "الموسم الأول"
        2 -> "الموسم الثاني"
        3 -> "الموسم الثالث"
        4 -> "الموسم الرابع"
        5 -> "الموسم الخامس"
        6 -> "الموسم السادس"
        else -> "الموسم $number"
    }

    /** Normalize provider and disk-cache data before the UI uses it. */
    fun normalizeSeasonBundle(bundle: ArabseedSeasonBundle): ArabseedSeasonBundle {
        val byUrl = linkedMapOf<String, Pair<Int, ArabseedEpisode>>()
        bundle.seasons.sortedBy { it.number }.forEach { season ->
            season.episodes.forEach { episode ->
                if (episode.number <= 0 || !episode.pageUrl.startsWith("http", ignoreCase = true)) return@forEach
                val explicitSeason = seasonNumber(episode.title, episode.pageUrl)
                // A season marker in the episode URL/title is more reliable than a stale
                // parent grouping. Otherwise keep the provider's season assignment.
                val targetSeason = explicitSeason ?: episode.seasonNumber.takeIf { it > 0 } ?: season.number
                val key = normalizedUrl(episode.pageUrl)
                val previous = byUrl[key]
                if (previous == null) {
                    byUrl[key] = targetSeason to episode.copy(seasonNumber = targetSeason)
                } else {
                    val previousHasExplicitSeason = seasonNumber(previous.second.title, previous.second.pageUrl) != null
                    if (explicitSeason != null && !previousHasExplicitSeason) {
                        byUrl[key] = targetSeason to episode.copy(seasonNumber = targetSeason)
                    }
                }
            }
        }
        val normalizedSeasons = byUrl.values
            .groupBy { it.first }
            .toSortedMap()
            .map { (seasonNumber, rows) ->
                val episodes = rows.asSequence()
                    .map { it.second }
                    .distinctBy { it.number }
                    .sortedBy { it.number }
                    .toList()
                ArabseedSeason(seasonNumber, seasonTitle(seasonNumber), episodes)
            }
            .filter { it.episodes.isNotEmpty() }

        val relatedParts = bundle.relatedParts
            .filter { it.pageUrl.startsWith("http", ignoreCase = true) }
            .distinctBy { partNumber(it.title, it.pageUrl)?.let { n -> "part:$n" }
                ?: normalizedUrl(it.pageUrl) }
            .sortedWith(compareBy<ArabseedItem> {
                partNumber(it.title, it.pageUrl) ?: Int.MAX_VALUE
            }.thenBy { it.title.lowercase() })

        return ArabseedSeasonBundle(normalizedSeasons, relatedParts)
    }

    fun cleanSeriesTitle(title: String): String {
        val withoutSeriesSuffix = normalizeDigits(title)
            .replace(
                Regex("""(?i)\s+(?:الموسم|موسم|season|الجزء|جزء|part|الحلقة|حلقة|episode|ep)\s*.*$"""),
                "",
            )
            .replace(Regex("""(?i)\s+(?:مترجم|مدبلج|فيلم|مسلسل|WEB-DL|720p|1080p|HD).*"""), "")
            .trim()
        return withoutSeriesSuffix
    }

    private fun extractLabeledNumber(
        title: String,
        url: String,
        numberPattern: Regex,
        ordinalPattern: Regex,
    ): Int? {
        val values = sequenceOf(normalizeText(title), normalizeText(url))
        for (value in values) {
            numberPattern.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { return it }
            ordinalPattern.find(value)?.groupValues?.getOrNull(1)?.let { word ->
                ordinals[word]?.let { return it }
            }
        }
        return null
    }

    private fun normalizeText(value: String): String = normalizeDigits(decodeUrl(value))
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
        .replace(Regex("[\\u0622\\u0623\\u0625]"), "ا")
        .lowercase()

    private fun decodeUrl(value: String): String = runCatching {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    }.getOrDefault(value)
}
