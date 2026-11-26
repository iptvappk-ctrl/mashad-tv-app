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
    private val episodePaginationPool = Executors.newFixedThreadPool(8)
    private val episodeNumberPattern = Regex(
        """(?i)(?:الحلقة|حلقة|episode|(?<![a-z])eps?|(?<![a-z])ep)(?:[\s._#=/:-]+|[\s._:-]*رقم[\s._:-]*)*([0-9]{1,4})(?![0-9])""",
    )
    private val trailingEpisodeNumberPattern = Regex(
        """(?i)(?<![0-9])([0-9]{1,4})[\s._:-]*(?:حلقة|حلقات|episode|ep)(?![a-z0-9])""",
    )
    private val compactArabicEpisodePattern = Regex(
        """(?:^|[\s|/\\-])ح[\s._:-]*([0-9]{1,4})(?![0-9])""",
    )
    private val seasonNumberPattern = Regex(
        """(?i)(?:الموسم|موسم|(?<![a-z])season)[\s._#=/:-]*([0-9]{1,2})(?![0-9])""",
    )
    private val compactSeasonEpisodePattern = Regex(
        """(?i)(?:^|[^a-z])s[\s._#=/:-]*([0-9]{1,2})[\s._#=/:-]*e(?:p(?:isode)?|ps)?[\s._#=/:-]*[0-9]{1,4}(?![0-9])""",
    )
    private val akwamEpisodeUrlPattern = Regex(
        """(?i)/akdrama-[^/]*?(?:-e[0-9]{1,2})?-s([0-9]{1,4})(?:/|[?#].*)?$""",
    )
    private val akwamSeasonUrlPattern = Regex(
        """(?i)/akdrama-[^/]*?-e([0-9]{1,2})-s([0-9]{1,4})(?:/|[?#].*)?$""",
    )
    private val cimaEpisodeUrlPattern = Regex(
        """(?i)^https?://(?:[^/]*\.)?(?:c4u|cima4u|arablionz)\.[^/]+/[^/?#]*?(?:-m[0-9]{1,2})?-n([0-9]{1,4})(?:/|[?#].*)?$""",
    )
    private val cimaSeasonUrlPattern = Regex(
        """(?i)^https?://(?:[^/]*\.)?(?:c4u|cima4u|arablionz)\.[^/]+/[^/?#]*?-m([0-9]{1,2})-n([0-9]{1,4})(?:/|[?#].*)?$""",
    )
    private val standaloneSeasonTokenPattern = Regex(
        """(?i)(?:^|[/\-_])s([0-9]{1,2})(?:[/\-_]|(?:[?#].*)?)$""",
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
    private val unrelatedContentContainerPattern = Regex(
        """(?i)(^|[\s_-])(related|recommend(?:ed|ation)?|suggest(?:ed|ions?)?|similar|sidebar|widget|popular|trending|also-watch|more-like|otherser|navslide|breadcrumbnav|drawer|tt-more|ads?|advert|banner|social|post-grid|movie-grid|series-grid|related-posts|you-may-like|search[\s_-]*(?:type[\s_-]*drop|res(?:ults?)?(?:[\s_-]*container)?|suggest(?:ions?)?|autocomplete|results?|live)|(?:top|most)[\s_-]*search|autocomplete|typeahead|instant-search|ajax-search|قد[\s_-]*يعجبك|مقترح(?:ات)?|مشابه(?:ة)?|ذات[\s_-]*صلة|اعلانات)([\s_-]|$)""",
    )

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

    /** Reject share, tracking, and other third-party links accidentally nested in episode lists. */
    fun isProviderContentPageUrl(
        candidateUrl: String,
        referencePageUrl: String,
        knownHostRoots: Collection<String>,
    ): Boolean {
        fun host(value: String): String = runCatching { java.net.URI(value.trim()).host.orEmpty() }
            .getOrDefault("")
            .lowercase()
            .removePrefix("www.")

        val candidateHost = host(candidateUrl)
        if (candidateHost.isBlank()) return false
        val referenceHost = host(referencePageUrl)
        if (referenceHost.isNotBlank() &&
            (candidateHost == referenceHost || candidateHost.endsWith(".$referenceHost"))
        ) return true
        return knownHostRoots.any { root ->
            val normalizedRoot = root.lowercase().removePrefix("www.").trim('.')
            normalizedRoot.isNotBlank() &&
                (candidateHost == normalizedRoot || candidateHost.endsWith(".$normalizedRoot"))
        }
    }

    fun normalizeArabicSearchQuery(value: String): String = value
        .replace(Regex("""[\u064B-\u065F\u0670\u0640]"""), "")
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ٱ', 'ا')
        .replace('ى', 'ي')
        .replace(Regex("""\s+"""), " ")
        .trim()

    fun episodeNumber(title: String, url: String, allowNumericTitle: Boolean = false): Int? {
        val titleText = normalizeText(title)
        val urlText = normalizeText(url)
        val labeled = sequenceOf(titleText, urlText)
            .mapNotNull { episodeNumberPattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .firstOrNull { it > 0 }
        if (labeled != null) return labeled

        val trailing = sequenceOf(titleText, urlText)
            .mapNotNull { trailingEpisodeNumberPattern.find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }
            .firstOrNull { it > 0 }
        if (trailing != null) return trailing

        val compact = compactArabicEpisodePattern.find(titleText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (compact != null && compact > 0) return compact

        val akwamEp = akwamEpisodeUrlPattern.find(urlText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (akwamEp != null && akwamEp > 0) return akwamEp

        val cimaEp = cimaEpisodeUrlPattern.find(urlText)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (cimaEp != null && cimaEp > 0) return cimaEp

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
        // Akwam /akdrama-... URLs encode Season as -e<S> and Episode as -s<E>
        // (e.g. crash-e2-s4 = Season 2 Ep 4, love-in-disguise-s11 = Season 1 Ep 11).
        if (normalizedUrl.contains("/akdrama-", ignoreCase = true)) {
            akwamSeasonUrlPattern.find(normalizedUrl)?.groupValues?.getOrNull(1)
                ?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
            if (akwamEpisodeUrlPattern.containsMatchIn(normalizedUrl)) {
                return null
            }
        }
        // Cima4U & ArabLionz encode Season as -m<S>-n<E> (e.g. tsadm-crash-m2-n4).
        cimaSeasonUrlPattern.find(normalizedUrl)?.groupValues?.getOrNull(1)
            ?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
        // Accept a stand-alone S2 path/title token, but never treat an arbitrary "s5"
        // fragment inside a longer slug as a season label.
        val detectedEp = episodeNumber(title, url)
        val hasExplicitSeasonInTitle = seasonNumberPattern.containsMatchIn(title) || seasonOrdinalPattern.containsMatchIn(title)
        sequenceOf(normalizedTitle, normalizedUrl).forEach { value ->
            val candidateSeason = standaloneSeasonTokenPattern.find(value)?.groupValues?.getOrNull(1)
                ?.toIntOrNull()?.takeIf { it > 0 }
            if (candidateSeason != null) {
                // If the standalone -sN token in the URL matches the episode number and the title has no
                // season marker, it is an episode slug (e.g. Akwam /akdrama-...-s8/ for "الحلقة 8"), not a season.
                if (detectedEp != null && candidateSeason == detectedEp && !hasExplicitSeasonInTitle) {
                    // Skip: this is an episode slug token, not a season number
                } else {
                    return candidateSeason
                }
            }
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
        val seasonContainerMarker = Regex("""(?i)(season|tab-pane|tabcontent|pds-season|الموسم|موسم)""")
        val episodeContainerMarker = Regex("""(?i)(episode|episodes|eplist|eps-list|episodios|epslist|allepcont|pds-episodes)""")
        val doc = element.ownerDocument()
        var node: Element? = element
        var depth = 0
        while (node != null && depth < 6) {
            val nodeId = node.id().trim()
            val marker = "$nodeId ${node.className()}"
            val isSeasonContainer = seasonContainerMarker.containsMatchIn(marker) &&
                !Regex("""(?i)(seasons|مواسم|season[-_ ]?(?:tabs|list)|season-tabs|pds-seasons|allseasonss)""")
                    .containsMatchIn(marker)
            val isEpisodeList = node.tagName() !in setOf("a", "li", "button") &&
                episodeContainerMarker.containsMatchIn(marker)
            for (attribute in seasonAttributes) {
                val value = node.attr(attribute).trim()
                if (value.isNotBlank()) {
                    value.toIntOrNull()?.takeIf { it in 1..60 }?.let { return it }
                    seasonNumber(value, "")?.let { return it }
                }
            }

            // Tab-pane / PHPMelody / Prestige containers (<div id="Season0">, #season-2, #pds-season-2):
            // resolve via the matching tab button or link label first (fixes 0-based IDs like #Season0 -> "الموسم 1"
            // and DB-ID containers like #br-season-744 whose heading contains the real season number).
            if (nodeId.isNotBlank() && doc != null && (isSeasonContainer || isEpisodeList)) {
                val escapedId = nodeId.replace("\"", "")
                val tabControl = doc.select(
                    "a[href='#$escapedId'], [data-bs-target='#$escapedId'], [data-target='#$escapedId'], " +
                        "[aria-controls='$escapedId'], button[onclick*=\"'$escapedId'\"], button[onclick*='\"$escapedId\"']",
                ).firstOrNull()
                if (tabControl != null) {
                    seasonNumber(tabControl.text().take(100), "")?.let { return it }
                }
            }

            val explicitAriaLabel = listOf(node.attr("title"), node.attr("aria-label"))
                .filter(String::isNotBlank)
                .joinToString(" ")
            if (isSeasonContainer && explicitAriaLabel.isNotBlank()) {
                seasonNumber(explicitAriaLabel, "")?.let { return it }
            }

            if (isSeasonContainer) {
                node.children()
                    .filter { it.tagName() in setOf("h1", "h2", "h3", "h4", "h5") || it.hasClass("br-series-season-title") || it.hasClass("tt-season-title") }
                    .take(4)
                    .firstNotNullOfOrNull { seasonNumber(it.text().take(100), "") }
                    ?.let { return it }
            }

            if (isSeasonContainer) seasonNumber(marker, "")?.takeIf { it in 1..60 }?.let { return it }

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
    fun linkedSeasonPages(
        document: Document,
        currentPageUrl: String,
        isAllowedContentPage: (String) -> Boolean = { candidate ->
            // Keep callers that only know the current page safe by default:
            // accept same-origin season pages, and require an explicit provider
            // allowlist before following any cross-origin content URL.
            isProviderContentPageUrl(candidate, currentPageUrl, emptyList())
        },
    ): List<Pair<Int, String>> {
        val seasonContainerMarker = Regex("""(?i)(season|الموسم|مواسم)""")
        return document.select(
            // MySeed renders season tabs as <li data-url="..."> rather than anchors.
            // Include those entries so older seasons can be fetched instead of relying
            // on the active season's partial episode list.
            "a[href], li[data-url], li[data-href], [role=tab][data-url], [role=tab][data-href], " +
                "[data-season][data-url], [data-season-number][data-url], " +
                "[data-season][data-href], [data-season-number][data-href]",
        ).mapNotNull { link ->
            if (isInsideUnrelatedContentContainer(link)) return@mapNotNull null
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
                !isAllowedContentPage(href) ||
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
                listOf("data-season-number", "data-season", "data-season_num")
                    .any { link.attr(it).isNotBlank() } ||
                Regex("""(?i)(seasons?|season[-_ ]?(?:tabs?|list|link|item|menu))""")
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
                    Regex("""(?i)(seasons?|season[-_ ]?(?:tabs?|list|menu))""").containsMatchIn(marker)
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
            // Do not turn a season-looking recommendation URL into a season tab.
            // A number is accepted only when it belongs to a season navigation
            // context or is explicitly carried in a data-season attribute.
            val explicitSeasonNumber = explicitLabelNumber ?: explicitUrlNumber
            val number = dataSeason ?: inheritedSeasonNumber
                ?: explicitSeasonNumber?.takeIf { seasonTabContext }
                ?: numericTab
                ?: return@mapNotNull null
            number to href
        }
            .distinctBy { normalizedUrl(it.second) }
            .sortedBy { it.first }
            .take(50)
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
        val pageParams = setOf(
            "page", "paged", "pageno", "page_no", "ep_page", "episode_page",
            "episodes_page", "episodepage", "page_number", "p",
        )

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
            if (isInsideUnrelatedContentContainer(link)) return@mapNotNull null
            val raw = link.attr("href").trim()
            // java.net.URI.resolve("?page=2") drops the current path on some
            // Android/JDK implementations. Preserve the episode-list path for
            // query-only pagination links so page 2+ can actually be requested.
            val reference = if (raw.startsWith("?")) current.rawPath.orEmpty() + raw else raw
            val candidate = runCatching { current.resolve(reference) }.getOrNull() ?: return@mapNotNull null
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
        // Some providers render a complete numbered pager and omit a "next"
        // link. Keep the whole bounded pager; a 12-link cap silently dropped
        // older episodes on long seasons.
        }.distinctBy(::normalizedUrl).take(60)
    }

    val episodeLinkSelectors = listOf(
        ".episodes__list a", "ul.episodes__list a", "section.episodes__section a[href]",
        ".SeasonsBoxUL a[href]", ".seasons-on-serie a[href]",
        ".pwr-season-panel a[href]", ".epslist a[href]", "a.episode-card",
        "div.season-list .tab-pane a[href]", "div.episodes.tab-content a[href]",
        ".pds-episodes a[href]", "article.pds-episode a[href]", "section.pds-season a[href]",
        "div.Tab .tabcontent a[href]", "div[id^='Season'].tabcontent a[href]",
        "section.allepcont a[href]", ".allepcont a[href]",
        "a.epss[href]", "div.items a.epss[href]",
        "article.postEp a[href]", ".postEp .block-post a[href]",
        "a.sh-episode-tile", ".Small--Box a", ".episode a", ".episodes a",
        ".episodes-list a", ".EpisodesList a", ".EpisodesList li a", ".eplister li a",
        ".eps-list li a", ".eps a", "a.ep", "a.episode__item", ".episode__item a", "a.ep-item",
        ".Episodes--List a", ".EpisodeBlock a", ".episodios li a", ".eplist a",
        ".br-episode-grid a[href]", ".br-series-detail-grid a[href]", ".br-series-season a[href]",
        "a[href*='/akdrama-']",
        ".sh-single-episode-grid a[href]", "#episodes a[href]",
        ".fhd-original-episodes-grid a[href]", ".tt-episode-grid a[href]", ".tt-season a[href]",
        ".ListNumberEpisodesMobile a[href]", "a.ItemEpisodeMobile[href]", "#AreaNewly .ItemNewly a[href]",
        "ul.block_Ep a[href]", "#Epis a[href]",
        "div.block-post a[href*='/clarus/']", "a[href*='/clarus/']",
        "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-']", "a[href*='%D8%A7%D9%84%D8%AD%D9%84%D9%82%D8%A9']",
        "a[href*='%d8%ad%d9%84%d9%82%d8%a9']", "a[href*='%D8%AD%D9%84%D9%82%D8%A9']",
        "a[href*='الحلقة']", "a[href*='حلقة']",
        "a[href*='episode']", "a[href*='ep-']", "a[href*='-eps']", ".postDiv a[href*='serie']",
        ".episodes-list [data-url], .episodes-list [data-href]",
        ".EpisodesList [data-url], .EpisodesList [data-href]",
        ".eplister [data-url], .eplister [data-href]",
        "[data-episode][data-url], [data-episode][data-href], [data-episode-link]",
    )

    private val episodeListMarkers = listOf(
        "episode", "episodes", "eplist", "eps-list", "episodeslist", "epslist", "eps",
        "episodes__list", "episodes__section", "allepcont", "pds-episodes", "pds-episode",
        "pds-season", "season-list", "tab-content", "tab-pane", "tabcontent", "postep", "block-post",
        "episodios", "pwr-season-panel", "br-episode-grid", "br-series-detail-grid",
        "br-series-season", "sh-single-episode-grid", "fhd-original-episodes-grid",
        "tt-episode-grid", "tt-season", "block_ep", "epis",
        "listnumberepisodesmobile", "areanewly", "itemnewly",
    )

    fun isEpisodeListElement(element: Element): Boolean =
        element.parents().any { ancestor ->
            val marker = "${ancestor.className()} ${ancestor.id()}".lowercase()
            episodeListMarkers.any(marker::contains)
        } || element.classNames().any { className ->
            className.contains("episode", ignoreCase = true) ||
                className.contains("ep-item", ignoreCase = true) ||
                className == "epss"
        }

    /** Unwrap shot90.com?url=<BASE64> wrappers used by Esheeq / Turkish sources. */
    fun unwrapRedirectUrl(rawUrl: String): String {
        val clean = rawUrl.trim()
        if (!clean.contains("shot90.com", ignoreCase = true)) return clean
        val parsed = runCatching { java.net.URI(clean) }.getOrNull() ?: return clean
        val encoded = parsed.rawQuery.orEmpty()
            .split('&')
            .firstOrNull { it.startsWith("url=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return clean
        if (encoded.startsWith("http://", true) || encoded.startsWith("https://", true)) return encoded
        return runCatching {
            val padded = encoded + "=".repeat((4 - encoded.length % 4) % 4)
            String(java.util.Base64.getDecoder().decode(padded), Charsets.UTF_8).trim()
        }.getOrNull()?.takeIf { it.startsWith("http://", true) || it.startsWith("https://", true) } ?: clean
    }

    /** True only when a /category/... URL identifies one specific series' episode archive. */
    fun isSpecificSeriesArchiveUrl(url: String): Boolean {
        val decoded = decodeUrl(url).trim()
        val uri = runCatching { java.net.URI(decoded) }.getOrNull() ?: return false
        val path = uri.path.orEmpty().lowercase().trimEnd('/')
        if (path.isBlank()) return false
        if (path.contains("/category/episodes")) {
            val segments = path.substringAfter("/category/episodes")
                .trim('/')
                .split('/')
                .filter(String::isNotBlank)
            val genericSegments = setOf(
                "new", "completed", "korean-ep", "chinese-ep", "japanese-ep",
                "thai-ep", "taiwan-ep", "filipino-ep", "asian-ep", "anime-ep", "other",
            )
            return segments.isNotEmpty() && segments.last() !in genericSegments
        }
        return path.contains("/category/مسلسل-") || path.contains("/category/انمي-")
    }

    /** Locate explicit "مشاهدة حلقات المسلسل" / series-archive buttons (e.g. AraDrama, Lodynet, PHPMelody). */
    fun findSeriesEpisodeArchiveUrls(document: Document, currentPageUrl: String): List<Pair<String, String>> {
        val ctaLabelPattern = Regex("""(?:مشاهدة\s+حلقات|حلقات\s+المسلسل|جميع\s+الحلقات|كل\s+الحلقات|مشاهدة\s+الحلقات)""")
        val found = linkedMapOf<String, Pair<String, String>>()
        for (el in document.select("a[href]")) {
            if (isInsideUnrelatedContentContainer(el)) continue
            if (el.parents().any { it.tagName() in setOf("header", "nav", "footer", "aside") }) continue
            val href = el.attr("abs:href").ifBlank { el.attr("href") }.trim()
            if (!href.startsWith("http", ignoreCase = true) ||
                normalizedUrl(href) == normalizedUrl(currentPageUrl)
            ) continue
            val txt = el.text().trim().replace(Regex("\\s+"), " ")
            val isSpecificArchive = isSpecificSeriesArchiveUrl(href)
            val isExplicitAllEpsTag = ctaLabelPattern.containsMatchIn(txt) &&
                href.contains("tag.php?t=", ignoreCase = true)
            if (!isSpecificArchive && !isExplicitAllEpsTag) continue
            if (isExplicitAllEpsTag ||
                ctaLabelPattern.containsMatchIn(txt) ||
                href.contains("/category/episodes/", ignoreCase = true) ||
                decodeUrl(href).lowercase().contains("/category/مسلسل-")
            ) {
                found.putIfAbsent(normalizedUrl(href), href to txt)
            }
        }
        return found.values.toList()
    }

    /** Parse one provider episode-list page for on-demand, incremental loading. */
    fun parseEpisodeEntries(
        document: Document,
        currentPageUrl: String,
        defaultSeasonNumber: Int,
        isAllowedContentUrl: (String) -> Boolean,
    ): List<Triple<String, String, Int?>> {
        val isTagArchive = currentPageUrl.contains("tag.php?t=", ignoreCase = true)
        val archiveExtra = if (isSpecificSeriesArchiveUrl(currentPageUrl) ||
            isSpecificSeriesArchiveUrl(document.location()) ||
            isTagArchive
        ) {
            listOf("article a[href]", ".pm-video-thumb a[href]", "a.ellipsis[href]")
        } else {
            emptyList()
        }
        val selectors = (episodeLinkSelectors + archiveExtra).joinToString(", ")
        val collected = linkedMapOf<String, Triple<String, String, Int?>>()
        for (element in document.select(selectors)) {
            if (isInsideUnrelatedContentContainer(element)) continue
            val rawUrl = listOf("href", "data-url", "data-href", "data-link", "data-episode-link")
                .firstNotNullOfOrNull { attribute ->
                    element.absUrl(attribute).ifBlank { element.attr(attribute).trim() }
                        .takeIf(String::isNotBlank)
                } ?: continue
            val resolvedUrl = runCatching {
                val current = java.net.URI(currentPageUrl)
                current.resolve(rawUrl).toString()
            }.getOrDefault("")
            val url = unwrapRedirectUrl(resolvedUrl)
            if (url.isBlank() || !isAllowedContentUrl(url)) continue

            val title = element.text().trim().ifBlank { element.attr("title").trim() }
            val number = episodeNumber(title, url, allowNumericTitle = isEpisodeListElement(element)) ?: continue
            val season = seasonNumber(title, url)
                ?: seasonNumberFromElement(element, title, url)
                ?: defaultSeasonNumber.takeIf { it > 0 }
            val key = normalizedUrl(url)
            val previous = collected[key]
            if (previous == null || (previous.third == null && season != null)) {
                collected[key] = Triple(title.ifBlank { "الحلقة $number" }, url, season)
            }
        }
        return collected.values.toList()
    }

    /**
     * Resolve only the next numbered page (or explicit rel=next link). Numeric
     * pagers commonly contain links to both earlier and later pages, so choosing
     * the first link causes loops and silently prevents long seasons from loading.
     */
    fun nextEpisodePage(document: Document, currentPageUrl: String): String? {
        val current = runCatching { java.net.URI(currentPageUrl) }.getOrNull() ?: return null
        val candidates = linkedEpisodePages(document, currentPageUrl)
        if (candidates.isEmpty()) return null

        val explicitNext = document.select(
            "a[rel~='next'][href], a.next[href], a[aria-label*=Next][href], a[aria-label*=التالي][href]",
        ).firstNotNullOfOrNull { link ->
            val raw = link.attr("href").trim()
            val reference = if (raw.startsWith("?")) current.rawPath.orEmpty() + raw else raw
            val url = runCatching { current.resolve(reference).toString() }.getOrNull()
            url?.takeIf { candidate -> candidates.any { normalizedUrl(it) == normalizedUrl(candidate) } }
        }
        if (explicitNext != null) return explicitNext

        fun pageNumber(url: String): Int? {
            val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return null
            val queryNumber = uri.rawQuery.orEmpty().split('&').firstNotNullOfOrNull { part ->
                val key = runCatching { URLDecoder.decode(part.substringBefore('='), "UTF-8") }
                    .getOrDefault(part.substringBefore('=')).lowercase()
                if (key !in setOf("page", "paged", "pageno", "page_no", "ep_page", "episode_page", "episodes_page", "p")) {
                    null
                } else {
                    runCatching { URLDecoder.decode(part.substringAfter('=', ""), "UTF-8").toIntOrNull() }.getOrNull()
                }
            }
            return queryNumber ?: Regex("(?i)/(?:page|paged|p)/(\\d+)(?:/|$)")
                .find(uri.path.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
        }

        val currentNumber = pageNumber(currentPageUrl)
        if (currentNumber != null) {
            return candidates.mapNotNull { candidate ->
                pageNumber(candidate)?.takeIf { it > currentNumber }?.let { it to candidate }
            }.minByOrNull { it.first }?.second
        }
        return candidates.singleOrNull()
    }

    /** Fetches numbered/next episode pages concurrently, with a strict time and page cap. */
    fun collectPaginatedEpisodes(
        firstDocument: Document,
        firstPageUrl: String,
        fetchDocument: (String) -> Document?,
        parseEpisodes: (Document) -> List<Triple<String, String, Int?>>,
        maxPages: Int = 25,
        timeoutSeconds: Long = 14,
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
                .take(minOf(6, maxPages - visitedPages))
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

    /** Excludes recommendation/sidebar cards that happen to contain season or episode links. */
    fun isInsideUnrelatedContentContainer(element: Element): Boolean =
        (listOf(element) + element.parents()).take(7).any { node ->
            if (node.tagName() in setOf("aside", "footer", "header")) return@any true
            val markers = listOf(node.id(), node.className(), node.attr("role"), node.attr("aria-label"))
                .joinToString(" ")
            unrelatedContentContainerPattern.containsMatchIn(markers)
        }

    /** A title marked as a finale gives a trustworthy upper bound for that season. */
    fun finalEpisodeNumber(title: String, url: String): Int? {
        val combined = normalizeText("$title $url")
        val finaleMarker = Regex(
            """(?i)(?:والاخيرة|الاخيرة|والاخيره|الاخيره|والاخير|الاخير|النهائية|النهائيه|نهائي|finale|series[-\s]+finale|last[-\s]+episode)""",
        )
        if (!finaleMarker.containsMatchIn(combined)) return null
        return episodeNumber(title, url)
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
        if (numbers.size < 3 || numbers.first() > 1) return true
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
                val episodes = episodesBySeason[seasonNumber].orEmpty()
                    .map { it.second }
                    .groupBy { it.number }
                    .toSortedMap()
                    .map { (_, variants) ->
                        val preferred = variants.first()
                        val allPages = variants.flatMap { episode ->
                            listOf(episode.pageUrl) + episode.alternatePageUrls
                        }.filter { it.startsWith("http", ignoreCase = true) }
                            .distinctBy(::normalizedUrl)
                        preferred.copy(
                            alternatePageUrls = (preferred.alternatePageUrls + allPages)
                                .filter { normalizedUrl(it) != normalizedUrl(preferred.pageUrl) }
                                .distinctBy(::normalizedUrl),
                        )
                    }
                val finaleNumber = episodes.firstNotNullOfOrNull { ep ->
                    finalEpisodeNumber(ep.title, ep.pageUrl)
                }
                val validEpisodes = mutableListOf<ArabseedEpisode>()
                for (ep in episodes) {
                    if (finaleNumber != null && ep.number > finaleNumber) continue
                    if (validEpisodes.isNotEmpty()) {
                        val prevNumber = validEpisodes.last().number
                        val gap = ep.number - prevNumber
                        val hasClusterNeighbor = episodes.any { other ->
                            other.number != ep.number && kotlin.math.abs(other.number - ep.number) <= 3
                        }
                        if (gap >= 8 && ep.number > 15 && !hasClusterNeighbor) continue
                    }
                    validEpisodes.add(ep)
                }
                val seasonPageUrl = bundle.seasons
                    .firstOrNull { it.number == seasonNumber && it.pageUrl.isNotBlank() }
                    ?.pageUrl
                    .orEmpty()
                ArabseedSeason(seasonNumber, seasonTitle(seasonNumber), validEpisodes, seasonPageUrl)
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
        val withoutPrefix = normalizeDigits(title)
            .replace(Regex("""(?i)^\s*(?:(?:مشاهدة|مشاهده|تحميل|سلسلة|مسلسل|فيلم|انمي|أنمي|برنامج)\s+)*"""), "")
        val withoutSeriesSuffix = withoutPrefix
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
            numberPattern.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it > 0 }?.let { return it }
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
