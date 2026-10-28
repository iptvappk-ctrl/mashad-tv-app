package com.madar.tv.data



import okhttp3.FormBody

import okhttp3.OkHttpClient

import okhttp3.Request

import org.jsoup.Jsoup

import java.util.concurrent.ConcurrentHashMap

import java.util.concurrent.Executors

import java.util.concurrent.Future

import java.util.concurrent.TimeUnit



/**

 * MultiProviderRepository — مجمّع المحتوى من 7 مواقع عربية شهيرة لتطبيق مَشْهَد TV

 * ───────────────────────────────────────────────────────────────────────────────

 * يعمل بالتوازي (Thread Pool) مع cache ذكي لضمان سرعة فائقة.

 * المواقع المدعومة:

 * 1. أكوام (Akwam)

 * 2. وي سيما (WeCima)

 * 3. فاصل HD (FaselHD)

 * 4. إيجي بست (EgyBest)

 * 5. لودي نت (Lodynet)

 * 6. بريستيج (Brstej / Prestige)

 * 7. قصة عشق (Esheeq / 3sk)

 */

object MultiProviderRepository {



    // ── Constants ──────────────────────────────────────────────────────

    private const val UA =

        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.6367.82 Mobile Safari/537.36"



    private const val LIST_TTL   = 10 * 60 * 1000L  // 10 دقائق

    private const val SEARCH_TTL = 8 * 60 * 1000L   // 8 دقائق

    private const val DETAIL_TTL = 30 * 60 * 1000L  // 30 دقيقة



    // ── Thread Pool ────────────────────────────────────────────────────

    private val pool = Executors.newCachedThreadPool()



    // ── HTTP Client ────────────────────────────────────────────────────

    private val http = OkHttpClient.Builder()

        .connectTimeout(3, TimeUnit.SECONDS)

        .readTimeout(4, TimeUnit.SECONDS)

        .followRedirects(true)

        .followSslRedirects(true)

        .build()



    // ── Caches ─────────────────────────────────────────────────────────

    private val listCache   = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val searchCache = ConcurrentHashMap<String, Pair<Long, List<ArabseedItem>>>()

    private val detailCache = ConcurrentHashMap<String, ArabseedDetail>()

    private val seasonCache = ConcurrentHashMap<String, ArabseedSeasonBundle>()



    fun clearCache() {

        listCache.clear()

        searchCache.clear()

        detailCache.clear()

        seasonCache.clear()

    }



    // ══════════════════════════════════════════════════════════════════

    // Provider Definitions

    // ══════════════════════════════════════════════════════════════════



    data class ProviderConfig(

        val id: String,

        val name: String,

        val baseUrl: String,

        val moviesPath: String,

        val seriesPath: String,

        val searchPath: String,    // use {q} as placeholder

        val cardSelectors: List<String>,

        val titleSelectors: List<String>,

        val posterAttr: List<String> = listOf("data-src", "src", "data-lazy-src", "data-original", "data-lazy", "abs:src"),

        val qualitySelectors: List<String> = listOf(".quality", ".ribbon", ".__quality", ".qlty"),

        val yearSelectors: List<String> = listOf(".year", ".date"),

        val enabled: Boolean = true,

    )



    val PROVIDERS = listOf(

        // ── أكوام ──────────────────────────────────────────────────────

        ProviderConfig(

            id = "akwam",

            name = "أكوام",

            baseUrl = "https://akw.cam",

            moviesPath = "/movies?page={page}",

            seriesPath = "/series?page={page}",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "a.br-card-link[href*='/akfilm-']",

                "a.br-card-link[href*='/akseries-']",

                "a[href*='/akfilm-']",

                "a[href*='/akseries-']",

            ),

            titleSelectors = listOf("h3.br-card-title", "h3", "h2", ".title"),

        ),



        // ── وي سيما ────────────────────────────────────────────────────

        ProviderConfig(

            id = "wecima",

            name = "وي سيما",

            baseUrl = "https://myweecima.beauty",

            moviesPath = "/movies.php?page={page}",

            seriesPath = "/series.php?page={page}",

            searchPath = "/search.php?keywords={q}",

            cardSelectors = listOf(

                "a[href*='watch.php?vid=']",

                ".block-post a",

                "li.block-post a",

            ),

            titleSelectors = listOf("[title]", "h2", "h3", ".title"),

        ),



        // ── فاصل HD ────────────────────────────────────────────────────

        ProviderConfig(

            id = "faselhd",

            name = "فاصل HD",

            baseUrl = "https://wb49584.fasel4k.site",

            moviesPath = "/movies/?page={page}",

            seriesPath = "/series/?page={page}",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "a[href*='/movie/']",

                "a[href*='/serie/']",

                ".postDiv a",

                ".movie__block a",

            ),

            titleSelectors = listOf("h2", "h3", ".post__title"),

        ),



        // ── إيجي بست ───────────────────────────────────────────────────

        ProviderConfig(

            id = "egybest",

            name = "إيجي بست",

            baseUrl = "https://egybest.mx",

            moviesPath = "/movies/?page={page}",

            seriesPath = "/series/?page={page}",

            searchPath = "/explore/?q={q}",

            cardSelectors = listOf(

                "a.movie",

                ".movies-list-full a.movie",

                ".content-box a",

                "div.movie-card a",

            ),

            titleSelectors = listOf("h2", "h3", ".title", ".movie-title"),

        ),



        // ── لودي نت ────────────────────────────────────────────────────

        ProviderConfig(

            id = "lodynet",

            name = "لودي نت",

            baseUrl = "https://lodynet.art",

            moviesPath = "/page/{page}/",

            seriesPath = "/category/series/page/{page}/",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "article.bt a",

                ".content-box article a",

                ".movies-list article a",

                "h2.entry-title a",

            ),

            titleSelectors = listOf("h2", "h3", ".entry-title", ".title"),

        ),



        // ── بريستيج ────────────────────────────────────────────────────

        ProviderConfig(

            id = "prestige",

            name = "بريستيج",

            baseUrl = "https://brstej.surf",

            moviesPath = "/movies.php?page={page}",

            seriesPath = "/series.php?page={page}",

            searchPath = "/?s={q}",

            cardSelectors = listOf(

                "a[href*='video.php?vid=']",

                ".movie__block a",

                ".MovieBlock a",

            ),

            titleSelectors = listOf("h2", "h3", ".Title", "span.title"),

        ),



        // ── قصة عشق ────────────────────────────────────────────────────

        ProviderConfig(

            id = "esheeq",

            name = "قصة عشق",

            baseUrl = "https://e.q-ask.video",

            moviesPath = "/movies?page={page}",

            seriesPath = "/series?page={page}",

            searchPath = "/search?q={q}",

            cardSelectors = listOf(

                "a[href*='/video-']",

                ".video-card a",

                "article a",

            ),

            titleSelectors = listOf("h2", "h3", ".title"),

        ),

    )



    fun isMultiProvider(pageUrl: String): Boolean {

        return pageUrl.contains("akw.cam") || pageUrl.contains("akwam")

            || pageUrl.contains("wecima") || pageUrl.contains("myweecima")

            || pageUrl.contains("fasel") || pageUrl.contains("fasel4k")

            || pageUrl.contains("egybest") || pageUrl.contains("lodynet")

            || pageUrl.contains("brstej") || pageUrl.contains("q-ask") || pageUrl.contains("3sk")

    }



    private fun getProviderForUrl(url: String): ProviderConfig? {

        return PROVIDERS.firstOrNull { p ->

            url.contains(p.baseUrl.replace("https://", "").replace("http://", ""))

        } ?: when {

            url.contains("akw") -> PROVIDERS.firstOrNull { it.id == "akwam" }

            url.contains("wecima") -> PROVIDERS.firstOrNull { it.id == "wecima" }

            url.contains("fasel") -> PROVIDERS.firstOrNull { it.id == "faselhd" }

            url.contains("egybest") -> PROVIDERS.firstOrNull { it.id == "egybest" }

            url.contains("lodynet") -> PROVIDERS.firstOrNull { it.id == "lodynet" }

            url.contains("brstej") -> PROVIDERS.firstOrNull { it.id == "prestige" }

            url.contains("q-ask") || url.contains("3sk") -> PROVIDERS.firstOrNull { it.id == "esheeq" }

            else -> null

        }

    }



    // ══════════════════════════════════════════════════════════════════

    // Fetch HTML helper

    // ══════════════════════════════════════════════════════════════════

    private fun fetchHtml(url: String, referer: String = ""): String {

        return try {

            val req = Request.Builder()

                .url(url)

                .header("User-Agent", UA)

                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")

                .header("Accept-Language", "ar,en;q=0.9")

                .apply { if (referer.isNotBlank()) header("Referer", referer) }

                .build()

            http.newCall(req).execute().use { r ->

                if (r.isSuccessful) r.body?.string() ?: "" else ""

            }

        } catch (_: Exception) { "" }

    }



    // ══════════════════════════════════════════════════════════════════

    // Parse items from a provider's HTML

    // ══════════════════════════════════════════════════════════════════

    private fun parseItems(

        html: String,

        provider: ProviderConfig,

        defaultType: ArabseedType,

    ): List<ArabseedItem> {

        if (html.isBlank()) return emptyList()

        val doc = try { Jsoup.parse(html, provider.baseUrl) } catch (_: Exception) { return emptyList() }

        val items = mutableListOf<ArabseedItem>()



        for (sel in provider.cardSelectors) {

            val elements = doc.select(sel)

            if (elements.isEmpty()) continue



            for (el in elements) {

                val href = el.attr("abs:href").ifBlank { el.attr("href") }

                if (href.isBlank() || !href.startsWith("http")) continue

                if (href.contains("/category") || href.contains("/tag/") || href.contains("/page/") || href.contains("category.php") || href.contains("topvideos") || href.contains("favicons")) continue



                // Title

                var title = ""

                for (ts in provider.titleSelectors) {

                    title = el.selectFirst(ts)?.text()?.trim() ?: ""

                    if (title.isNotBlank()) break

                }

                if (title.isBlank()) title = el.attr("title").trim()

                if (title.isBlank()) title = el.selectFirst("img")?.attr("alt")?.trim() ?: ""

                if (title.length < 2) continue



                // Poster
                val img = el.selectFirst("img")
                var poster: String? = null

                // 1. Try img attributes (data-src, src, etc.)
                for (attr in provider.posterAttr) {
                    val raw = img?.attr(attr) ?: continue
                    poster = when {
                        raw.startsWith("http") -> raw
                        raw.startsWith("/") -> "${provider.baseUrl}$raw"
                        raw.isNotBlank() -> "${provider.baseUrl}/$raw"
                        else -> null
                    }
                    if (poster != null) break
                }

                // 2. Try abs:src fallback (Jsoup resolves relative URLs)
                if (poster == null) {
                    poster = img?.attr("abs:src")?.takeIf { it.startsWith("http") }
                }

                // 3. Try background-image in style attribute (WeCima, Akwam)
                if (poster == null) {
                    val styleEl = el.selectFirst("[style*='background-image']")
                        ?: el.selectFirst(".br-thumb, .ak-poster, .imgSer, .poster, .pm-video-thumb")
                    val style = styleEl?.attr("style") ?: ""
                    val bgMatch = Regex("""url\(['"]?(https?://[^'")\s]+)['"]?\)""").find(style)
                    if (bgMatch != null) poster = bgMatch.groupValues[1]
                }

                // 4. Try any child element's background-image style
                if (poster == null) {
                    for (child in el.allElements) {
                        val style = child.attr("style")
                        if (style.contains("background-image")) {
                            val bgMatch = Regex("""url\(['"]?(https?://[^'")\s]+)['"]?\)""").find(style)
                            if (bgMatch != null) { poster = bgMatch.groupValues[1]; break }
                        }
                    }
                }



                // Quality

                var quality = ""

                for (qs in provider.qualitySelectors) {

                    quality = el.selectFirst(qs)?.text()?.trim() ?: ""

                    if (quality.isNotBlank()) break

                }

                if (quality.isBlank()) quality = Regex("(?i)(4K|1080p|720p|480p|WEB-DL|BluRay|HD)")

                    .find("$href $title")?.value ?: "HD"



                // Year

                var year = ""

                for (ys in provider.yearSelectors) {

                    year = Regex("""\b(20\d\d|19\d\d)\b""")

                        .find(el.selectFirst(ys)?.text() ?: "")?.value ?: ""

                    if (year.isNotBlank()) break

                }

                if (year.isBlank()) year = Regex("""\b(20\d\d|19\d\d)\b""").find(href)?.value ?: ""



                val isSeries = defaultType == ArabseedType.SERIES

                    || href.contains("/series/") || href.contains("/serie/") || href.contains("/مسلسل")

                    || title.contains("مسلسل") || title.contains("الحلقة")



                val cleanTitle = title

                    .replace(Regex("""(?i)(مترجم|مدبلج|مشاهدة|تحميل|اون لاين|فيلم|مسلسل|HD|4K|1080p|720p)"""), "")

                    .replace(Regex("""\s{2,}"""), " ").trim()



                items += ArabseedItem(

                    id = "${provider.id}_${href.trimEnd('/').substringAfterLast('/').take(60)}",

                    title = cleanTitle.ifBlank { title },

                    arabicTitle = title,

                    posterUrl = poster,

                    year = year,

                    quality = quality,

                    rating = "",

                    type = if (isSeries) ArabseedType.SERIES else ArabseedType.MOVIE,

                    pageUrl = href,

                    category = if (isSeries) "مسلسل" else "فيلم",

                    overview = provider.name,

                    providerName = provider.name,

                )

            }

            if (items.isNotEmpty()) break

        }

        return items.distinctBy { it.pageUrl }

    }



    // ══════════════════════════════════════════════════════════════════

    // PUBLIC LISTING & SEARCH

    // ══════════════════════════════════════════════════════════════════



    fun fetchMoviesAll(page: Int = 1): List<ArabseedItem> {

        val key = "movies:$page"

        val now = System.currentTimeMillis()

        listCache[key]?.let { (t, l) -> if (now - t < LIST_TTL && l.isNotEmpty()) return l }



        val futures = mutableListOf<Future<List<ArabseedItem>>>()



        for (p in PROVIDERS.filter { it.enabled }) {

            futures += pool.submit<List<ArabseedItem>> {

                try {

                    val url = "${p.baseUrl}${p.moviesPath.replace("{page}", page.toString())}"

                    parseItems(fetchHtml(url, "${p.baseUrl}/"), p, ArabseedType.MOVIE)

                } catch (_: Exception) { emptyList() }

            }

        }



        val all = mutableListOf<ArabseedItem>()

        for (f in futures) {

            try { all += f.get(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        }



        val deduped = deduplicateItems(all)
        val enriched = enrichPostersFromSource(deduped)

        if (enriched.isNotEmpty()) listCache[key] = now to enriched

        return enriched

    }



    fun fetchSeriesAll(page: Int = 1): List<ArabseedItem> {

        val key = "series:$page"

        val now = System.currentTimeMillis()

        listCache[key]?.let { (t, l) -> if (now - t < LIST_TTL && l.isNotEmpty()) return l }



        val futures = mutableListOf<Future<List<ArabseedItem>>>()



        for (p in PROVIDERS.filter { it.enabled }) {

            futures += pool.submit<List<ArabseedItem>> {

                try {

                    val url = "${p.baseUrl}${p.seriesPath.replace("{page}", page.toString())}"

                    parseItems(fetchHtml(url, "${p.baseUrl}/"), p, ArabseedType.SERIES)

                } catch (_: Exception) { emptyList() }

            }

        }



        val all = mutableListOf<ArabseedItem>()

        for (f in futures) {

            try { all += f.get(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        }



        val deduped = deduplicateItems(all)
        val enriched = enrichPostersFromSource(deduped)

        if (enriched.isNotEmpty()) listCache[key] = now to enriched

        return enriched

    }



    fun searchAll(query: String): List<ArabseedItem> {

        val q = query.trim()

        if (q.isBlank()) return emptyList()



        val key = "search:${q.lowercase()}"

        val now = System.currentTimeMillis()

        searchCache[key]?.let { (t, l) -> if (now - t < SEARCH_TTL && l.isNotEmpty()) return l }



        val futures = mutableListOf<Future<List<ArabseedItem>>>()



        for (p in PROVIDERS.filter { it.enabled }) {

            futures += pool.submit<List<ArabseedItem>> {

                try {

                    val url = "${p.baseUrl}${p.searchPath.replace("{q}", java.net.URLEncoder.encode(q, "UTF-8"))}"

                    parseItems(fetchHtml(url, "${p.baseUrl}/"), p, ArabseedType.MOVIE)

                } catch (_: Exception) { emptyList() }

            }

        }



        val all = mutableListOf<ArabseedItem>()

        for (f in futures) {

            try { all += f.get(2000, TimeUnit.MILLISECONDS) } catch (_: Exception) {}

        }



        val deduped = deduplicateItems(all)

        if (deduped.isNotEmpty()) searchCache[key] = now to deduped

        return deduped

    }



    // ══════════════════════════════════════════════════════════════════

    // STORY, SERVERS, DETAIL & SEASONS FOR ALL PROVIDERS

    // ══════════════════════════════════════════════════════════════════



    fun fetchStoryAndCast(pageUrl: String): Pair<String, List<String>> {

        try {

            val html = fetchHtml(pageUrl)

            if (html.isBlank()) return "" to emptyList()

            val doc = Jsoup.parse(html)

            val selectors = listOf(

                ".post__story", ".story", ".Story", ".entry__desc", "div.desc",

                ".overview", ".description", ".entry-content p", ".post-story",

                ".story-content", "#story", "p.story", "div.story",

            )

            var overview = ""

            for (s in selectors) {

                val txt = doc.selectFirst(s)?.text()?.trim() ?: ""

                if (txt.length > 20 && !txt.contains("تحميل ومشاهدة")) {

                    overview = txt

                    break

                }

            }

            val cast = linkedSetOf<String>()

            doc.select("[itemprop=actor], [itemprop=actors], .actors a, .Actors a, .actor a, .cast a, .Cast a, .cast-list a, .post__actors a, .persons a")

                .forEach { node ->

                    val name = node.attr("content").ifBlank { node.selectFirst("[itemprop=name]")?.text()?.trim().orEmpty().ifBlank { node.text().trim() } }

                    if (name.isNotBlank() && name.length < 80) cast += name.replace(Regex("\\s+"), " ")

                }

            return overview to cast.toList()

        } catch (_: Exception) {}

        return "" to emptyList()

    }



    fun fetchStory(pageUrl: String): String = fetchStoryAndCast(pageUrl).first



    fun fetchServers(pageUrl: String): List<ArabseedServer> {

        val servers = mutableListOf<ArabseedServer>()

        val provider = getProviderForUrl(pageUrl)

        val baseUrl = provider?.baseUrl ?: ""



        try {

            // 1. Akwam Servers (watch/.../servers)

            if (pageUrl.contains("akw")) {

                val watchUrl = pageUrl.replace("/akfilm-", "/watch/akfilm-")

                    .replace("/akseries-", "/watch/akseries-")

                    .trimEnd('/') + "/servers"

                val watchHtml = fetchHtml(watchUrl, "https://akw.cam/")

                val wDoc = Jsoup.parse(watchHtml, "https://akw.cam/")

                val primaryIframe = wDoc.selectFirst("iframe")?.attr("src")?.trim() ?: ""

                if (primaryIframe.startsWith("http")) {

                    servers += ArabseedServer("سيرفر 1 (فائق السرعة)", primaryIframe)

                }

                for (el in wDoc.select("[data-url], [data-src], a.server, .watchServers a, iframe[src]")) {

                    val sUrl = el.attr("data-url").ifBlank { el.attr("data-src").ifBlank { el.attr("src").ifBlank { el.attr("href") } } }.trim()

                    val sName = el.text().trim().replace(Regex("(?i)(أكوام|akwam)"), "").trim()

                    if (sUrl.startsWith("http") && servers.none { it.embedUrl == sUrl }) {

                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                        servers += ArabseedServer("سيرفر ${servers.size + 1}$label", sUrl)

                    }

                }

            }



            // 2. WeCima Servers (embed.php?vid=... & downloads.php?vid=...)

            if (pageUrl.contains("wecima") || pageUrl.contains("myweecima")) {

                val vid = Regex("vid=([a-zA-Z0-9]+)").find(pageUrl)?.groupValues?.get(1) ?: ""

                if (vid.isNotBlank()) {

                    servers += ArabseedServer("سيرفر 1 (فائق السرعة)", "https://myweecima.beauty/embed.php?vid=$vid")

                    try {

                        val dlHtml = fetchHtml("https://myweecima.beauty/downloads.php?vid=$vid", pageUrl)

                        val dlDoc = Jsoup.parse(dlHtml, "https://myweecima.beauty/")

                        for (a in dlDoc.select("a[href]")) {

                            val h = a.attr("href").trim()

                            val sName = a.text().trim().replace(Regex("(?i)(وي سيما|wecima)"), "").trim()

                            if (h.startsWith("http") && (

                                h.contains("uqload") || h.contains("vikingfile") || h.contains("turboviplay") ||

                                h.contains("usersdrive") || h.contains("gofile") || h.contains("vidspeed") ||

                                h.contains("forafile") || h.contains("down.") || h.contains("vidoba")

                            )) {

                                if (servers.none { it.embedUrl == h }) {

                                    val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                                    servers += ArabseedServer("سيرفر ${servers.size + 1}$label", h)

                                }

                            }

                        }

                    } catch (_: Exception) {}

                }

            }



            // 3. FaselHD special embed buttons -> AJAX call to /ajax/embed

            if (pageUrl.contains("fasel")) {

                val fHtml = fetchHtml(pageUrl, baseUrl)

                val fDoc = Jsoup.parse(fHtml, baseUrl)

                val embedButtons = fDoc.select("button.btn-service[data-embed], [data-embed]")

                val futures = mutableListOf<Future<Pair<String, String>?>>()

                for ((idx, btn) in embedButtons.take(6).withIndex()) {

                    val embedId = btn.attr("data-embed").trim()

                    val sName = btn.text().trim().replace(Regex("(?i)(فاصل|fasel)"), "").trim()

                    if (embedId.isNotBlank()) {

                        futures += pool.submit<Pair<String, String>?> {

                            try {

                                val formBody = FormBody.Builder().add("id", embedId).build()

                                val postReq = Request.Builder()

                                    .url("https://wb49584.fasel4k.site/ajax/embed")

                                    .post(formBody)

                                    .header("User-Agent", UA)

                                    .header("Referer", pageUrl)

                                    .header("X-Requested-With", "XMLHttpRequest")

                                    .build()

                                val respHtml = http.newCall(postReq).execute().use { it.body?.string() ?: "" }

                                val ifr = Jsoup.parse(respHtml).selectFirst("iframe")?.attr("src")?.trim() ?: ""

                                if (ifr.startsWith("http")) sName to ifr else null

                            } catch (_: Exception) { null }

                        }

                    }

                }

                for (f in futures) {

                    try {

                        val pair = f.get(3, TimeUnit.SECONDS)

                        if (pair != null && servers.none { it.embedUrl == pair.second }) {

                            val label = if (pair.first.isNotBlank() && pair.first.length < 25) " (${pair.first})" else ""

                            servers += ArabseedServer("سيرفر ${servers.size + 1}$label", pair.second)

                        }

                    } catch (_: Exception) {}

                }

            }



            // 4. Standard iframes and data-embed attributes (works across EgyBest, Lodynet, Prestige)

            val html = fetchHtml(pageUrl, baseUrl)

            if (html.isNotBlank()) {

                val doc = Jsoup.parse(html, baseUrl)

                for (ifr in doc.select("iframe[src]")) {

                    val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()

                    if (src.startsWith("http") && !src.contains("googletag") && !src.contains("facebook")) {

                        if (servers.none { it.embedUrl == src }) {

                            servers += ArabseedServer("سيرفر ${servers.size + 1}", src)

                        }

                    }

                }

                for (el in doc.select("[data-embed], [data-src], [data-link], .servers a, a.server-btn, .watch-buttons a")) {

                    val link = el.attr("data-embed").ifBlank {

                        el.attr("data-src").ifBlank { el.attr("data-link").ifBlank { el.attr("href") } }

                    }.trim()

                    val sName = el.text().trim().replace(Regex("(?i)(أكوام|فاصل|وي سيما|إيجي بست|لودي نت)"), "").trim()

                    if (link.startsWith("http") && !link.contains("google") && servers.none { it.embedUrl == link }) {

                        val label = if (sName.isNotBlank() && sName.length < 25) " ($sName)" else ""

                        servers += ArabseedServer("سيرفر ${servers.size + 1}$label", link)

                    }

                }

            }

        } catch (_: Exception) {}



        if (servers.isEmpty()) {

            servers += ArabseedServer("سيرفر 1 (مباشر)", pageUrl)

        }



        return servers.take(10)

    }



    fun fetchDetail(item: ArabseedItem): ArabseedDetail {

        detailCache[item.pageUrl]?.let { return it }



        var pageMetadata: Pair<String, List<String>> = "" to emptyList()

        var servers = emptyList<ArabseedServer>()



        val t1 = Thread { pageMetadata = fetchStoryAndCast(item.pageUrl) }

        val t2 = Thread { servers = fetchServers(item.pageUrl) }

        t1.start(); t2.start()

        try { t1.join(4000); t2.join(4500) } catch (_: Exception) {}



        val detail = ArabseedDetail(

            item = item.copy(overview = pageMetadata.first.ifBlank { item.overview }),

            servers = servers,

            preResolvedStream = servers.firstOrNull()?.directStreamUrl,

            cast = pageMetadata.second,

        )

        detailCache[item.pageUrl] = detail

        return detail

    }



    fun fetchSeasonsAndEpisodes(item: ArabseedItem): ArabseedSeasonBundle {
        seasonCache[item.pageUrl]?.let { return it }
        val provider = getProviderForUrl(item.pageUrl)
        val baseUrl = provider?.baseUrl ?: ""

        return try {
            val html = fetchHtml(item.pageUrl, baseUrl)
            val doc = Jsoup.parse(html, baseUrl)
            doc.select("footer, script, style").remove()

            val rawEpisodes = mutableListOf<Pair<String, String>>()
            val selectors = listOf(
                ".episodes-list a", ".EpisodesList li a", ".eplister li a",
                ".eps-list li a", ".eps a", "a.episode__item", ".episode__item a", "a.ep-item",
                "a[href*='%d8%a7%d9%84%d8%ad%d9%84%d9%82%d8%a9-']", "a[href*='الحلقة']",
                "a[href*='episode']", "a[href*='ep-']", ".postDiv a[href*='serie']"
            )

            fun collectEpisodes(d: org.jsoup.nodes.Document): List<Pair<String, String>> {
                var best = mutableListOf<Pair<String, String>>()
                for (sel in selectors) {
                    val els = d.select(sel)
                    if (els.isNotEmpty()) {
                        val current = mutableListOf<Pair<String, String>>()
                        for (el in els) {
                            val href = el.attr("abs:href").ifBlank { el.attr("href") }
                            val txt = el.text().trim().ifBlank { el.attr("title").trim() }
                            val hasEpisodeListParent = el.parents().any { ancestor ->
                                val marker = "${ancestor.className()} ${ancestor.id()}".lowercase()
                                listOf("episode", "episodes", "eplist", "eps-list", "episodeslist", "eps", "episodios")
                                    .any(marker::contains)
                            } || el.classNames().any { className ->
                                className.contains("episode", ignoreCase = true)
                                    || className.contains("ep-item", ignoreCase = true)
                            }
                            val episodeNumber = SeriesCatalogParser.episodeNumber(
                                title = txt,
                                url = href,
                                allowNumericTitle = hasEpisodeListParent,
                            )
                            if (href.startsWith("http") && episodeNumber != null) {
                                if (current.none {
                                        SeriesCatalogParser.normalizedUrl(it.second) == SeriesCatalogParser.normalizedUrl(href)
                                    }
                                ) {
                                    current += txt to href
                                }
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
                val parentSelectors = listOf(
                    "a[href*='/series/']", "a[href*='/season/']", "a[href*='الموسم']",
                    ".breadcrumb a", "ol.breadcrumb a", ".entry-breadcrumb a", "nav a"
                )
                for (sel in parentSelectors) {
                    val links = doc.select(sel)
                    val parent = links.lastOrNull { el ->
                        val href = el.attr("abs:href").ifBlank { el.attr("href") }
                        val txt = el.text().trim()
                        href.startsWith("http") && href != item.pageUrl
                            && !txt.contains("الرئيسية") && !txt.contains("home", ignoreCase = true)
                            && !SeriesCatalogParser.isEpisode(txt, href)
                    }
                    if (parent != null) {
                        val parentHref = parent.attr("abs:href").ifBlank { parent.attr("href") }
                        try {
                            val parentDoc = Jsoup.parse(fetchHtml(parentHref, baseUrl), baseUrl)
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

            val defaultSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl) ?: 1
            val numberedEpisodes = rawEpisodes.mapNotNull { (title, href) ->
                val episodeNumber = SeriesCatalogParser.episodeNumber(title, href) ?: return@mapNotNull null
                val seasonNumber = SeriesCatalogParser.seasonNumber(title, href) ?: defaultSeasonNumber
                Triple(seasonNumber, episodeNumber, href)
            }.distinctBy { (_, _, href) -> SeriesCatalogParser.normalizedUrl(href) }
            val seasons = numberedEpisodes.groupBy { it.first }.toSortedMap().map { (seasonNumber, rows) ->
                val episodes = rows
                    .distinctBy { it.second }
                    .sortedBy { it.second }
                    .map { (_, episodeNumber, href) ->
                        ArabseedEpisode(
                            number = episodeNumber,
                            title = "الحلقة $episodeNumber",
                            pageUrl = href,
                            seasonNumber = seasonNumber,
                        )
                    }
                ArabseedSeason(seasonNumber, SeriesCatalogParser.seasonTitle(seasonNumber), episodes)
            }

            val relatedParts = linkedMapOf<String, ArabseedItem>()
            val partSelectors = listOf(
                "a[href*='part']", "a[href*='الجزء']",
                ".parts a", ".parts-list a",
            )
            for (selector in partSelectors) {
                for (node in doc.select(selector)) {
                    val href = node.attr("abs:href").ifBlank { node.attr("href") }
                    val title = node.text().trim().replace(Regex("\\s+"), " ")
                    val isEp = SeriesCatalogParser.isEpisode(title, href)
                    val labelMatches = SeriesCatalogParser.hasPartMarker(title, href)

                    if (!isEp && href.startsWith("http") && href != item.pageUrl && title.isNotBlank() && (labelMatches || selector.startsWith("."))) {
                        val img = node.selectFirst("img")?.attr("abs:src")
                            ?.ifBlank { node.selectFirst("img")?.attr("src") }
                            ?: item.posterUrl
                        relatedParts.putIfAbsent(href, item.copy(
                            id = "${item.id}_${href.hashCode()}",
                            title = title,
                            arabicTitle = title,
                            pageUrl = href,
                            posterUrl = img,
                            overview = "",
                        ))
                    }
                }
                if (relatedParts.isNotEmpty()) break
            }

            val cleanParts = relatedParts.values
                .filter { part ->
                    SeriesCatalogParser.hasPartMarker(part.title, part.pageUrl)
                        && !SeriesCatalogParser.isEpisode(part.title, part.pageUrl)
                }
                .distinctBy { part ->
                    SeriesCatalogParser.partNumber(part.title, part.pageUrl)?.toString()
                        ?: SeriesCatalogParser.normalizedUrl(part.pageUrl)
                }
                .sortedWith(compareBy<ArabseedItem> {
                    SeriesCatalogParser.partNumber(it.title, it.pageUrl) ?: Int.MAX_VALUE
                }.thenBy { it.title })
            val bundle = ArabseedSeasonBundle(seasons = seasons, relatedParts = cleanParts)
            seasonCache[item.pageUrl] = bundle
            bundle
        } catch (_: Exception) {
            ArabseedSeasonBundle(emptyList())
        }
    }



    // ══════════════════════════════════════════════════════════════════

    // Smart Deduplication

    // ══════════════════════════════════════════════════════════════════

    private fun normalizeTitle(title: String): String =

        title.lowercase()

            .replace(Regex("[أإآ]"), "ا")

            .replace("ة", "ه")

            .replace("ى", "ي")

            .replace(Regex("[\\u064B-\\u0652]"), "")

            .replace(Regex("""(?i)\b(فيلم|مسلسل|مترجم|مدبلج|كامل|hd|fhd|4k|1080p|720p)\b"""), "")

            .replace(Regex("""[^\w\s\u0600-\u06FF]"""), " ")

            .replace(Regex("""\s+"""), " ")

            .trim()



    private fun deduplicateItems(items: List<ArabseedItem>): List<ArabseedItem> {

        val seen = LinkedHashMap<String, ArabseedItem>()

        for (item in items) {

            val norm = normalizeTitle(item.title)

            if (norm.length < 2) continue

            if (!seen.containsKey(norm)) {

                seen[norm] = item

            } else {

                val existing = seen[norm]!!

                if ((existing.posterUrl == null || !existing.posterUrl.startsWith("http"))

                    && item.posterUrl?.startsWith("http") == true) {

                    seen[norm] = item

                }

            }

        }

        return seen.values.toList()

    }

    // Fetch og:image from the source page
    private fun fetchOgImage(pageUrl: String): String? {
        return try {
            val html = fetchHtml(pageUrl)
            if (html.isBlank()) return null
            val doc = Jsoup.parse(html, pageUrl)
            doc.selectFirst("meta[property=og:image], meta[name=og:image]")
                ?.attr("content")
                ?.takeIf { it.startsWith("http") }
        } catch (_: Exception) { null }
    }

    // Enrich missing posters by fetching og:image from source site
    private fun enrichPostersFromSource(items: List<ArabseedItem>): List<ArabseedItem> {
        val needsPoster = items.filter { it.posterUrl.isNullOrBlank() }.take(12)
        if (needsPoster.isEmpty()) return items

        val batchPool = java.util.concurrent.Executors.newFixedThreadPool(6)
        val futureMap = needsPoster.associate { item ->
            item.pageUrl to batchPool.submit<String?> { fetchOgImage(item.pageUrl) }
        }
        batchPool.shutdown()

        val posterMap = mutableMapOf<String, String>()
        for ((url, future) in futureMap) {
            try {
                val p = future.get(3, TimeUnit.SECONDS)
                if (p != null) posterMap[url] = p
            } catch (_: Exception) {}
        }

        return items.map { item ->
            if (item.posterUrl.isNullOrBlank() && posterMap.containsKey(item.pageUrl))
                item.copy(posterUrl = posterMap[item.pageUrl])
            else item
        }
    }

}
