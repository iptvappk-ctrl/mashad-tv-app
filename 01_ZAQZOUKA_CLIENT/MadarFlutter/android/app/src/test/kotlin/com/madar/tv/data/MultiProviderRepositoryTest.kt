package com.madar.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicReference

class MultiProviderRepositoryTest {
    @Test
    fun reportsReachableProviderRouteAfterParsingItsSearchPage() {
        val server = ServerSocket(0)
        val responseThread = Thread {
            server.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val body = """
                    <html><body>
                      <a class="card" href="/series/test"><h2 class="title">Series Test</h2></a>
                    </body></html>
                """.trimIndent()
                val bytes = body.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\n".toByteArray())
                    write("Content-Type: text/html; charset=utf-8\r\n".toByteArray())
                    write("Content-Length: ${bytes.size}\r\n".toByteArray())
                    write("Connection: close\r\n\r\n".toByteArray())
                    write(bytes)
                    flush()
                }
            }
            server.close()
        }.apply { start() }
        val provider = providerAt("reachable-test", server.localPort)
        val reachable = AtomicReference<Boolean?>()

        val rows = MultiProviderRepository.fetchProviderListing(
            provider = provider,
            path = "/search?q=series",
            type = ArabseedType.SERIES,
            onRouteComplete = reachable::set,
        )
        responseThread.join(2_000)

        assertEquals(true, reachable.get())
        assertEquals(1, rows.size)
        assertEquals("Series Test", rows.single().title)
    }

    @Test
    fun reportsProviderRouteFailureWhenConnectionCannotBeEstablished() {
        val closedSocket = ServerSocket(0)
        val port = closedSocket.localPort
        closedSocket.close()
        val provider = providerAt("unreachable-test-${System.nanoTime()}", port)
        val reachable = AtomicReference<Boolean?>()

        val rows = MultiProviderRepository.fetchProviderListing(
            provider = provider,
            path = "/search?q=series",
            type = ArabseedType.SERIES,
            timeoutSeconds = 2,
            onRouteComplete = reachable::set,
        )

        assertFalse(reachable.get() ?: true)
        assertTrue(rows.isEmpty())
    }

    @Test
    fun treatsHttp404EmptySearchHtmlAsReachableZeroResults() {
        val server = ServerSocket(0)
        val responseThread = Thread {
            server.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val body = """
                    <html dir="rtl" lang="ar"><body>
                      <main id="prs-results"><section class="prs-empty"><h2>لا توجد نتائج مطابقة</h2></section></main>
                    </body></html>
                """.trimIndent()
                val bytes = body.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().apply {
                    write("HTTP/1.1 404 Not Found\r\n".toByteArray())
                    write("Content-Type: text/html; charset=utf-8\r\n".toByteArray())
                    write("Content-Length: ${bytes.size}\r\n".toByteArray())
                    write("Connection: close\r\n\r\n".toByteArray())
                    write(bytes)
                    flush()
                }
            }
            server.close()
        }.apply { start() }
        val provider = providerAt("prestige-404-empty", server.localPort)
        val reachable = AtomicReference<Boolean?>()

        val rows = MultiProviderRepository.fetchProviderListing(
            provider = provider,
            path = "/search.php?keywords=nomatch",
            type = ArabseedType.SERIES,
            onRouteComplete = reachable::set,
        )
        responseThread.join(2_000)

        assertEquals(true, reachable.get())
        assertTrue(rows.isEmpty())
    }

    @Test
    fun parsesAllTwentyFiveEpisodesFromShahid4uSiSeriesHtml() {
        val server = ServerSocket(0)
        // Exact compact DOM structure from shahid4u.si: no space between <span class="no">N</span>
        // and <b>الحلقة N</b>, synopsis mentioning "الأخيرة" on ep 13, and finale badge on ep 25.
        val linksHtml = (1..25).joinToString("") { ep ->
            val finaleBadge = if (ep == 25) """<span class="badge b-new">الحلقة الأخيرة</span>""" else ""
            val synopsis = if (ep == 13) {
                """التحق رفاق مجموعة إف 4 بالجامعة، بينما تبدأ جان دي سنتها الأخيرة في المدرسة."""
            } else {
                """وصف الحلقة رقم $ep في مدرسة شين وا."""
            }
            """<a class="ep" href="/series/boys-over-flowers-16420/season-1/episode-$ep/#play"><span class="th"><img src="https://example.com/$ep.jpg" alt="" /><span class="no">$ep</span></span><span class="b"><b>الحلقة $ep $finaleBadge</b><small>5 يناير 2009 · 1 س 10 د</small><span class="ov">$synopsis</span></span></a>"""
        }
        val body = """
            <html dir="rtl" lang="ar">
            <head><title>مسلسل أيام الزهور (Boys Over Flowers) مترجم كامل 25 حلقة | شاهد فور يو</title></head>
            <body>
              <section class="sec" id="episodes">
                <div class="ep-panel" data-tv="16420" data-season="1" data-mode="list">
                  <div class="ep-panel-body">
                    <div class="eplist">$linksHtml</div>
                  </div>
                </div>
              </section>
            </body>
            </html>
        """.trimIndent()
        val responseThread = Thread {
            server.accept().use { socket ->
                val input = socket.getInputStream().bufferedReader()
                while (true) {
                    val line = input.readLine() ?: break
                    if (line.isEmpty()) break
                }
                val bytes = body.toByteArray(Charsets.UTF_8)
                socket.getOutputStream().apply {
                    write("HTTP/1.1 200 OK\r\n".toByteArray())
                    write("Content-Type: text/html; charset=utf-8\r\n".toByteArray())
                    write("Content-Length: ${bytes.size}\r\n".toByteArray())
                    write("Connection: close\r\n\r\n".toByteArray())
                    write(bytes)
                    flush()
                }
            }
            server.close()
        }.apply { start() }

        val port = server.localPort
        val item = ArabseedItem(
            id = "shahid4u_si_boys-over-flowers-16420",
            title = "أيام الزهور (Boys Over Flowers)",
            arabicTitle = "أيام الزهور (Boys Over Flowers)",
            posterUrl = null,
            year = "2009",
            quality = "HD",
            rating = "",
            type = ArabseedType.SERIES,
            pageUrl = "http://127.0.0.1:$port/series/boys-over-flowers-16420/",
            category = "مسلسل",
            overview = "شاهد فور يو VIP",
            providerName = "شاهد فور يو VIP",
            parentSeriesTitle = "أيام الزهور (Boys Over Flowers)",
        )

        val bundle = MultiProviderRepository.fetchSeasonsAndEpisodes(item, forceRefresh = true)
        responseThread.join(2_000)

        assertEquals(1, bundle.seasons.size)
        assertEquals(1, bundle.seasons.first().number)
        assertEquals((1..25).toList(), bundle.seasons.first().episodes.map { it.number })
    }

    @Test
    fun percentEncodesArabicAndBracketedUrlsForOkHttpHeadersAndUri() {
        val rawArabicUrl = "https://myseed.name/episode/مسلسل-حب-محتمل-الموسم-الاول-الحلقة-1-مترجمة"
        val safeArabic = SeriesCatalogParser.asciiSafeUrl(rawArabicUrl)
        val safeReferer = SeriesCatalogParser.asciiSafeHeaderValue(rawArabicUrl)

        assertTrue(safeArabic.all { it.code in 0x20..0x7E })
        assertEquals(safeArabic, safeReferer)
        assertTrue(safeArabic.contains("%D9%85%D8%B3%D9%84%D8%B3%D9%84"))
        okhttp3.Request.Builder().url(safeArabic).header("Referer", safeReferer).build()
        java.net.URI(safeArabic)

        val bracketedEmbed = "https://vidmoly.org/embed-[arabseed]abc123.html"
        val safeBracketed = SeriesCatalogParser.asciiSafeUrl(bracketedEmbed)
        assertEquals("https://vidmoly.org/embed-%5Barabseed%5Dabc123.html", safeBracketed)
        java.net.URI(safeBracketed)

        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(1, 2)))
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(2, 3, 4)))
        assertFalse(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(1, 2, 3)))
    }

    private fun providerAt(id: String, port: Int) =
        MultiProviderRepository.ProviderConfig(
            id = id,
            name = "Test provider",
            baseUrl = "http://127.0.0.1:$port",
            moviesPath = "/movies",
            seriesPath = "/series",
            searchPath = "/search?q={q}",
            cardSelectors = listOf("a.card"),
            titleSelectors = listOf(".title"),
        )
}
