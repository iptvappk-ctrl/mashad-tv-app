import re

multi_path = r"c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\data\MultiProviderRepository.kt"

with open(multi_path, "r", encoding="utf-8") as f:
    text = f.read()

# Replace fetchServers method with the perfected implementation
old_fetch_servers = """    fun fetchServers(pageUrl: String): List<ArabseedServer> {
        val servers = mutableListOf<ArabseedServer>()
        val provider = getProviderForUrl(pageUrl)
        val baseUrl = provider?.baseUrl ?: ""

        try {
            val html = fetchHtml(pageUrl, baseUrl)
            if (html.isBlank()) return emptyList()
            val doc = Jsoup.parse(html, baseUrl)

            // 1. FaselHD special embed buttons -> AJAX call to /ajax/embed
            if (pageUrl.contains("fasel")) {
                val embedButtons = doc.select("button.btn-service[data-embed], [data-embed]")
                for (btn in embedButtons) {
                    val embedId = btn.attr("data-embed").trim()
                    val btnName = btn.text().trim().ifBlank { "سيرفر فاصل ${servers.size + 1}" }
                    if (embedId.isNotBlank()) {
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
                            if (ifr.isNotBlank() && ifr.startsWith("http")) {
                                if (servers.none { it.embedUrl == ifr }) {
                                    servers += ArabseedServer(btnName, ifr)
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            }

            // 2. Akwam servers page (e.g. /watch/.../servers)
            if (pageUrl.contains("akw")) {
                for (el in doc.select("a[data-url], [data-url]")) {
                    val url = el.attr("data-url").trim()
                    val name = el.text().trim().ifBlank { "سيرفر أكوام ${servers.size + 1}" }
                    if (url.startsWith("http") && servers.none { it.embedUrl == url }) {
                        servers += ArabseedServer(name, url)
                    }
                }
            }

            // 3. WeCima player resolution
            if (pageUrl.contains("wecima") || pageUrl.contains("myweecima")) {
                for (el in doc.select("a[href*='watch.php'], a[href*='play.php']")) {
                    val playHref = el.attr("abs:href").ifBlank { el.attr("href") }
                    if (playHref.startsWith("http")) {
                        val playHtml = fetchHtml(playHref, baseUrl)
                        val ifr = Jsoup.parse(playHtml).selectFirst("iframe")?.attr("src")?.trim() ?: ""
                        if (ifr.startsWith("http") && servers.none { it.embedUrl == ifr }) {
                            servers += ArabseedServer("سيرفر وي سيما ${servers.size + 1}", ifr)
                        }
                    }
                }
            }

            // 4. Standard iframes and data-embed attributes (works across EgyBest, Lodynet, Prestige)
            val iframes = doc.select("iframe[src]")
            for ((idx, ifr) in iframes.withIndex()) {
                val src = ifr.attr("abs:src").ifBlank { ifr.attr("src") }.trim()
                if (src.startsWith("http") && !src.contains("googletag") && !src.contains("facebook")) {
                    if (servers.none { it.embedUrl == src }) {
                        servers += ArabseedServer("سيرفر ${provider?.name ?: "رئيسي"} ${idx + 1}", src)
                    }
                }
            }

            for (el in doc.select("[data-embed], [data-src], [data-link], .servers a, a.server-btn, .watch-buttons a")) {
                val link = el.attr("data-embed").ifBlank {
                    el.attr("data-src").ifBlank { el.attr("data-link").ifBlank { el.attr("href") } }
                }.trim()
                val name = el.text().trim().ifBlank { "سيرفر ${servers.size + 1}" }
                if (link.startsWith("http") && !link.contains("google") && servers.none { it.embedUrl == link }) {
                    servers += ArabseedServer(name, link)
                }
            }
        } catch (_: Exception) {}

        return servers.take(10)
    }"""

new_fetch_servers = """    fun fetchServers(pageUrl: String): List<ArabseedServer> {
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
    }"""

text_lf = text.replace("\r\n", "\n")
old_lf = old_fetch_servers.replace("\r\n", "\n")

if old_lf in text_lf:
    text_lf = text_lf.replace(old_lf, new_fetch_servers.replace("\r\n", "\n"))
    print("MultiProviderRepository fetchServers updated!")
else:
    print("Warning: old_fetch_servers not found")

with open(multi_path, "w", encoding="utf-8", newline="\r\n") as f:
    f.write(text_lf.replace("\n", "\r\n"))
