package com.madar.tv.data

import org.jsoup.nodes.Element
import java.net.URLDecoder

/** Shared, conservative parsing for episode/season/part labels found in provider pages. */
internal object SeriesCatalogParser {
    private val episodeNumberPattern = Regex(
        """(?i)(?:الحلقة|حلقة|episode|(?<![a-z])ep)[\s._#=/:-]*([0-9]{1,4})(?![0-9])""",
    )
    private val seasonNumberPattern = Regex(
        """(?i)(?:الموسم|موسم|(?<![a-z])season)[\s._#=/:-]*([0-9]{1,2})(?![0-9])""",
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
    private val episodeMarkerPattern = Regex("""(?i)(الحلقة|حلقة|episode|(?<![a-z])ep(?:[\s._#=/:-]|[0-9]|$))""")
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

    fun seasonNumber(title: String, url: String): Int? = extractLabeledNumber(
        title = title,
        url = url,
        numberPattern = seasonNumberPattern,
        ordinalPattern = seasonOrdinalPattern,
    )

    /**
     * Reads a season label from the episode link or its nearby DOM container.
     * Episode URLs often omit the season, while the provider groups links under
     * containers such as `season-2` or `data-season="2"`.
     */
    fun seasonNumberFromElement(element: Element, title: String, url: String): Int? {
        seasonNumber(title, url)?.let { return it }

        val seasonAttributes = listOf(
            "data-season-number", "data-season", "data-season_num",
        )
        var node: Element? = element
        var depth = 0
        while (node != null && depth < 8) {
            val contextValues = listOf(
                node.id(), node.className(), node.attr("title"), node.attr("aria-label"),
                node.attr("data-title"), node.attr("data-name"),
            ).filter(String::isNotBlank)
            contextValues.firstNotNullOfOrNull { seasonNumber(it, "") }?.let { return it }

            for (attribute in seasonAttributes) {
                val value = node.attr(attribute).trim()
                if (value.isNotBlank()) {
                    value.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
                    seasonNumber(value, "")?.let { return it }
                }
            }

            val childSeasonNumbers = node.children()
                .filter { child ->
                    child.tagName() in setOf("h1", "h2", "h3", "h4", "h5", "button")
                        || child.attr("role").equals("tab", ignoreCase = true)
                }
                .take(4)
                .mapNotNull { seasonNumber(it.text().take(100), "") }
                .distinct()
            if (childSeasonNumbers.size == 1) return childSeasonNumbers.single()

            var sibling = node.previousElementSibling()
            var siblingCount = 0
            while (sibling != null && siblingCount < 3) {
                val isSeasonLabel = sibling.tagName() in setOf("h1", "h2", "h3", "h4", "h5", "button")
                    || sibling.className().contains("season", ignoreCase = true)
                    || sibling.id().contains("season", ignoreCase = true)
                if (isSeasonLabel) {
                    seasonNumber(sibling.text().take(100), "")?.let { return it }
                }
                sibling = sibling.previousElementSibling()
                siblingCount++
            }

            node = node.parent()
            depth++
        }

        return null
    }

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
