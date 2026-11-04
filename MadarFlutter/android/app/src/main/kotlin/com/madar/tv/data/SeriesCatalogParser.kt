package com.madar.tv.data

import org.jsoup.nodes.Element
import org.jsoup.nodes.Document
import java.net.URLDecoder
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Shared, conservative parsing for episode/season/part labels found in provider pages. */
internal object SeriesCatalogParser {
    private val episodePaginationPool = Executors.newFixedThreadPool(4)
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
        val episodeContainerMarker = Regex("""(?i)(episode|episodes|eplist|eps-list|episodios)""")
        var node: Element? = element
        var depth = 0
        while (node != null && depth < 5) {
            val marker = "${node.id()} ${node.className()}"
            val isSeasonContainer = seasonContainerMarker.containsMatchIn(marker) &&
                !Regex("""(?i)(seasons|مواسم|season[-_ ]?(?:tabs|list)|season-tabs)""")
                    .containsMatchIn(marker)
            val isEpisodeList = episodeContainerMarker.containsMatchIn(marker)
            val label = listOf(node.id(), node.className(), node.attr("title"), node.attr("aria-label"))
                .joinToString(" ")
            seasonNumber(label, "")?.let { return it }
            for (attribute in seasonAttributes) {
                val value = node.attr(attribute).trim()
                if (value.isNotBlank()) {
                    value.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
                    seasonNumber(value, "")?.let { return it }
                }
            }

            if (isSeasonContainer) {
                node.children()
                    .filter { it.tagName() in setOf("h1", "h2", "h3", "h4", "h5") }
                    .take(3)
                    .firstNotNullOfOrNull { seasonNumber(it.text().take(100), "") }
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
    fun linkedSeasonPages(document: Document, currentPageUrl: String): List<Pair<Int, String>> {
        val seasonContainerMarker = Regex("""(?i)(season|الموسم|مواسم)""")
        return document.select(
            "a[href], [data-season][data-url], [data-season-number][data-url], " +
                "[data-season][data-href], [data-season-number][data-href]",
        ).mapNotNull { link ->
            val urlAttribute = listOf("href", "data-url", "data-href")
                .firstOrNull { link.attr(it).isNotBlank() } ?: return@mapNotNull null
            val rawUrl = link.attr(urlAttribute).trim()
            val href = link.absUrl(urlAttribute).ifBlank {
                runCatching { java.net.URI(currentPageUrl).resolve(rawUrl).toString() }
                    .getOrDefault(rawUrl)
            }.trim()
            val label = listOf(link.text(), link.attr("title"), link.attr("aria-label"))
                .filter(String::isNotBlank)
                .joinToString(" ")
            if (href.isBlank() || !href.startsWith("http", ignoreCase = true) ||
                normalizedUrl(href) == normalizedUrl(currentPageUrl) || isEpisode(label, href)
            ) return@mapNotNull null

            val explicitLabelNumber = seasonNumber(label, "")
            val explicitUrlNumber = seasonNumber("", href)
            val dataSeason = listOf("data-season-number", "data-season", "data-season_num")
                .firstNotNullOfOrNull { name ->
                    link.attr(name).toIntOrNull()?.takeIf { it > 0 }
                        ?: seasonNumber(link.attr(name), "")
                }
            var inheritedSeasonNumber: Int? = null
            var seasonTabContext = link.tagName() == "button" ||
                link.attr("role").equals("tab", ignoreCase = true) ||
                Regex("""(?i)(season[-_ ]?tab|season-link|season-item)""")
                    .containsMatchIn("${link.className()} ${link.id()}")
            var node: Element? = link
            var depth = 0
            while (node != null && depth < 4) {
                val marker = "${node.id()} ${node.className()} ${node.attr("role")} " +
                    "${node.attr("title")} ${node.attr("aria-label")}"
                val isPluralSeasonWrapper = Regex(
                    """(?i)(seasons|مواسم|season[-_ ]?(?:tabs|list)|season-tabs)""",
                ).containsMatchIn(marker)
                val isSeasonContainer = seasonContainerMarker.containsMatchIn(marker) && !isPluralSeasonWrapper
                val isEpisodeContainer = Regex("""(?i)(episode|episodes|eplist|eps-list|episodios)""")
                    .containsMatchIn(marker)
                if (node.tagName() == "nav" || node.attr("role").equals("tablist", ignoreCase = true) ||
                    Regex("""(?i)(season[-_ ]?(?:tabs|list)|season-tabs)""").containsMatchIn(marker)
                ) seasonTabContext = true
                if (inheritedSeasonNumber == null && isSeasonContainer && !isEpisodeContainer) {
                    inheritedSeasonNumber = seasonNumber(marker, "")
                    inheritedSeasonNumber = listOf("data-season-number", "data-season", "data-season_num")
                        .firstNotNullOfOrNull { name ->
                            node.attr(name).toIntOrNull()?.takeIf { it > 0 }
                                ?: seasonNumber(node.attr(name), "")
                        } ?: inheritedSeasonNumber
                }
                node = node.parent()
                depth++
            }
            val numericTab = normalizeDigits(label).trim().toIntOrNull()
                ?.takeIf { seasonTabContext && it in 1..99 }
            val number = explicitLabelNumber ?: explicitUrlNumber ?: dataSeason ?: inheritedSeasonNumber ?: numericTab
                ?: return@mapNotNull null
            number to href
        }
            .distinctBy { normalizedUrl(it.second) }
            .sortedBy { it.first }
            .take(30)
    }

    /** Finds pagination links that belong to an episode list, without following season tabs. */
    fun linkedEpisodePages(document: Document, currentPageUrl: String): List<String> {
        val current = runCatching { java.net.URI(currentPageUrl) }.getOrNull() ?: return emptyList()
        val paginationSelector = listOf(
            ".pagination a[href]", "[class*=pagination] a[href]", ".page-numbers[href]",
            ".page-numbers a[href]", ".paginate_links a[href]", ".wp-pagenavi a[href]",
            ".episodes-pagination a[href]", ".episode-pagination a[href]", ".ep-pagination a[href]",
            "a[rel~='next'][href]", "a.next[href]", "a[aria-label*=Next][href]",
            "a[aria-label*=التالي][href]",
        ).joinToString(", ")
        val paginationMarker = Regex("""(?i)(pagination|page-numbers|paginate|pager|paging)""")
        val pageParams = setOf("page", "paged", "pageno", "page_no", "ep_page", "episode_page")

        fun hostKey(uri: java.net.URI): String = uri.host.orEmpty().lowercase().removePrefix("www.")
        fun family(uri: java.net.URI): String? {
            var path = uri.path.orEmpty().trimEnd('/')
                .replace(Regex("""(?i)/(?:page|paged|p)/\d+$"""), "")
                .ifBlank { "/" }
            val query = uri.rawQuery.orEmpty().split('&').filter(String::isNotBlank).filterNot { pair ->
                val key = runCatching { java.net.URLDecoder.decode(pair.substringBefore('='), "UTF-8") }
                    .getOrDefault(pair.substringBefore('='))
                    .lowercase()
                key in pageParams
            }.sorted().joinToString("&")
            path = path.trimEnd('/').ifBlank { "/" }
            return "${hostKey(uri)}|$path|$query"
        }

        return document.select(paginationSelector).mapNotNull { link ->
            val raw = link.attr("href").trim()
            val candidate = runCatching { current.resolve(raw) }.getOrNull() ?: return@mapNotNull null
            if (candidate.scheme !in setOf("http", "https") || hostKey(candidate) != hostKey(current)) {
                return@mapNotNull null
            }
            val label = link.text().trim()
            val relNext = link.attr("rel").split(Regex("\\s+")).any { it.equals("next", true) }
            val nextLabel = link.classNames().any { it.contains("next", true) } ||
                link.attr("aria-label").contains("next", true) ||
                link.attr("aria-label").contains("التالي") || label.contains("التالي")
            val inPaginationContainer = link.parents().take(4).any { parent ->
                paginationMarker.containsMatchIn("${parent.className()} ${parent.id()}")
            }
            val numericPage = normalizeDigits(label).trim().toIntOrNull()?.let { it > 0 } == true
            if (!(relNext || nextLabel || inPaginationContainer || numericPage)) return@mapNotNull null
            if (episodeNumber(label, candidate.toString()) != null) return@mapNotNull null
            if (normalizedUrl(candidate.toString()) == normalizedUrl(current.toString()) ||
                family(candidate) != family(current)
            ) return@mapNotNull null
            candidate.toString()
        }.distinctBy(::normalizedUrl).take(12)
    }

    /** Fetches numbered/next episode pages concurrently, with a strict time and page cap. */
    fun collectPaginatedEpisodes(
        firstDocument: Document,
        firstPageUrl: String,
        fetchDocument: (String) -> Document?,
        parseEpisodes: (Document) -> List<Triple<String, String, Int?>>,
        maxPages: Int = 12,
        timeoutSeconds: Long = 6,
    ): List<Triple<String, String, Int?>> {
        val byUrl = linkedMapOf<String, Triple<String, String, Int?>>()
        fun merge(rows: List<Triple<String, String, Int?>>) {
            rows.forEach { row ->
                val key = normalizedUrl(row.second)
                if (key.isNotBlank()) {
                    val old = byUrl[key]
                    if (old == null || (old.third == null && row.third != null)) byUrl[key] = row
                }
            }
        }

        merge(parseEpisodes(firstDocument))
        val pending = linkedSetOf<String>().apply {
            addAll(linkedEpisodePages(firstDocument, firstPageUrl))
        }
        val visited = linkedSetOf(normalizedUrl(firstPageUrl))
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
        var visitedPages = 0

        while (pending.isNotEmpty() && visitedPages < maxPages) {
            val batch = pending.asSequence()
                .filter { visited.add(normalizedUrl(it)) }
                .take(minOf(4, maxPages - visitedPages))
                .toList()
            pending.removeAll(batch.toSet())
            if (batch.isEmpty()) break
            visitedPages += batch.size

            val completion = ExecutorCompletionService<Pair<String, Document>>(episodePaginationPool)
            val futures = batch.map { url ->
                completion.submit(Callable {
                    url to (fetchDocument(url) ?: Document(url))
                })
            }
            repeat(batch.size) {
                val remaining = deadline - System.nanoTime()
                if (remaining <= 0L) return@repeat
                val finished = try {
                    completion.poll(remaining, TimeUnit.NANOSECONDS) ?: return@repeat
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    return@repeat
                }
                val (url, document) = runCatching { finished.get() }.getOrNull() ?: return@repeat
                merge(parseEpisodes(document))
                linkedEpisodePages(document, url)
                    .filterNot { normalizedUrl(it) in visited }
                    .forEach(pending::add)
            }
            futures.filterNot { it.isDone }.forEach { it.cancel(true) }
            if (System.nanoTime() >= deadline) break
        }
        return byUrl.values.toList()
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

    /**
     * Detects seasons that are clearly partial without assuming a fixed episode
     * count. Providers rarely expose a trustworthy total; sparse lists and gaps
     * in numbered episodes are the signals we can verify locally.
     */
    fun needsEpisodeSequenceRepair(episodeNumbers: Collection<Int>): Boolean {
        val numbers = episodeNumbers.asSequence()
            .filter { it > 0 }
            .distinct()
            .sorted()
            .toList()
        if (numbers.size < 3) return true
        return numbers.zipWithNext().any { (previous, next) -> next - previous > 1 }
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
        val episodesBySeason = byUrl.values.groupBy { it.first }
        val seasonNumbers = (bundle.seasons.map { it.number } + episodesBySeason.keys)
            .filter { it > 0 }
            .distinct()
            .sorted()
        val normalizedSeasons = seasonNumbers
            .map { seasonNumber ->
                val episodes = episodesBySeason[seasonNumber].orEmpty().asSequence()
                    .map { it.second }
                    .distinctBy { it.number }
                    .sortedBy { it.number }
                    .toList()
                val seasonPageUrl = bundle.seasons
                    .firstOrNull { it.number == seasonNumber && it.pageUrl.isNotBlank() }
                    ?.pageUrl
                    .orEmpty()
                ArabseedSeason(seasonNumber, seasonTitle(seasonNumber), episodes, seasonPageUrl)
            }
            .filter { it.episodes.isNotEmpty() || it.pageUrl.isNotBlank() }

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

    fun normalizedSeriesTitle(title: String): String = normalizeText(cleanSeriesTitle(title))
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

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
