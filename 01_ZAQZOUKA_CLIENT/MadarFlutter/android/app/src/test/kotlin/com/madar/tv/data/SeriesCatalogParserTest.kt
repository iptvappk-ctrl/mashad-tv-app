package com.madar.tv.data

import org.jsoup.Jsoup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesCatalogParserTest {
    @Test
    fun readsMySeedSeasonTabsFromLiDataUrl() {
        val document = Jsoup.parse(
            """
            <div class="list__sub__cats" id="seasons__list">
              <ul>
                <li data-url="/selary/show-season-1"><span>الموسم الاول مترجم</span></li>
                <li data-url="/selary/show-season-2"><span>الموسم الثاني مترجم</span></li>
                <li data-url="/selary/show-season-3"><span>الموسم الثالث مترجم</span></li>
                <li data-url="/selary/show-season-4"><span>الموسم الرابع مترجم</span></li>
              </ul>
            </div>
            """.trimIndent(),
            "https://m.myseed.tv/selary/show-episode-7",
        )

        val seasons = SeriesCatalogParser.linkedSeasonPages(
            document,
            "https://m.myseed.tv/selary/show-episode-7",
        )

        assertEquals(listOf(1, 2, 3, 4), seasons.map { it.first })
        assertEquals(
            listOf(
                "https://m.myseed.tv/selary/show-season-1",
                "https://m.myseed.tv/selary/show-season-2",
                "https://m.myseed.tv/selary/show-season-3",
                "https://m.myseed.tv/selary/show-season-4",
            ),
            seasons.map { it.second },
        )
    }

    @Test
    fun ignoresSeasonLookingLinksOutsideSeasonNavigation() {
        val document = Jsoup.parse(
            """
            <main>
              <nav id="seasons__list">
                <a href="/show-season-1">الموسم الأول</a>
                <a href="/show-season-2">الموسم الثاني</a>
                <a href="/show-season-3">الموسم الثالث</a>
                <a href="/show-season-4">الموسم الرابع</a>
              </nav>
              <aside>
                <a href="/other-show-season-7">الموسم السابع</a>
              </aside>
              <div class="content-grid">
                <a href="/recommended-season-9">Season 9</a>
              </div>
            </main>
            """.trimIndent(),
            "https://m.myseed.tv/show-season-4",
        )

        val seasons = SeriesCatalogParser.linkedSeasonPages(
            document,
            "https://m.myseed.tv/show-season-4",
        )

        assertEquals(listOf(1, 2, 3), seasons.map { it.first })
    }

    @Test
    fun doesNotInferEpisodeSeasonFromSingleSeasonHeadingNearby() {
        val document = Jsoup.parse(
            """
            <section class="episode-list">
              <h3>الموسم السابع</h3>
              <a class="episode" href="/episode-1">الحلقة 1</a>
            </section>
            """.trimIndent(),
            "https://m.myseed.tv/show",
        )
        val episode = document.selectFirst("a.episode")!!

        assertNull(SeriesCatalogParser.seasonNumberFromElement(episode, episode.text(), episode.absUrl("href")))
    }

    @Test
    fun ignoresEpisodeLinksInsideMySeedSearchSuggestionDropdown() {
        val document = Jsoup.parse(
            """
            <header>
              <div class="search__res__container">
                <div class="most__search">
                  <ul class="top__search">
                    <li><a href="/series/hob-aswad-s1-eps28">مسلسل حب اسود الحلقة 28</a></li>
                  </ul>
                </div>
              </div>
              <div class="search__type__drop">
                <a href="/series/another-show-s1-eps49">مسلسل آخر الحلقة 49</a>
              </div>
            </header>
            <main>
              <div class="episodes-list">
                <a href="/series/dexter-resurrection-s1-eps10">Dexter Resurrection الموسم الأول الحلقة 10</a>
              </div>
            </main>
            """.trimIndent(),
            "https://m.myseed.tv/series/dexter-resurrection-s1-eps10",
        )

        val unrelatedSuggestions = document.select(".search__res__container a, .search__type__drop a")
        val actualEpisode = document.selectFirst(".episodes-list a")!!

        assertEquals(2, unrelatedSuggestions.size)
        assertTrue(unrelatedSuggestions.all(SeriesCatalogParser::isInsideUnrelatedContentContainer))
        assertFalse(SeriesCatalogParser.isInsideUnrelatedContentContainer(actualEpisode))
    }

    @Test
    fun keepsSameEpisodeNumbersInDifferentSeasonsSeparateAndSortsEpisodes() {
        val bundle = SeriesCatalogParser.normalizeSeasonBundle(
            ArabseedSeasonBundle(
                seasons = listOf(
                    ArabseedSeason(
                        number = 2,
                        title = "Season 2",
                        episodes = listOf(
                            episode(2, 2, "https://m.myseed.tv/s2-e2"),
                            episode(1, 2, "https://m.myseed.tv/s2-e1"),
                        ),
                    ),
                    ArabseedSeason(
                        number = 1,
                        title = "Season 1",
                        episodes = listOf(episode(1, 1, "https://m.myseed.tv/s1-e1")),
                    ),
                ),
            ),
        )

        assertEquals(listOf(1, 2), bundle.seasons.map { it.number })
        assertEquals(listOf(1), bundle.seasons.first().episodes.map { it.number })
        assertEquals(listOf(1, 2), bundle.seasons.last().episodes.map { it.number })
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(1, 2, 4, 6)))
        assertFalse(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(1, 2, 3, 4)))
    }

    @Test
    fun followsEpisodePaginationPagesAndCollectsEveryNumberedEpisode() {
        val firstPageUrl = "https://m.myseed.tv/series/example-season-1"
        val first = Jsoup.parse(
            """
            <div class="episodes-list"><a href="/series/example-s1-e1">Episode 1</a></div>
            <div class="pagination">
              <a href="?page=2">2</a>
              <a href="?page=3">3</a>
            </div>
            """.trimIndent(),
            firstPageUrl,
        )

        val pages = mapOf(
            "2" to """
                <div class="episodes-list">
                  <a href="/series/example-s1-e2">Episode 2</a>
                  <a href="/series/example-s1-e3">Episode 3</a>
                </div>
                <div class="pagination"><a href="?page=3">3</a></div>
            """.trimIndent(),
            "3" to """
                <div class="episodes-list">
                  <a href="/series/example-s1-e4">Episode 4</a>
                  <a href="/series/example-s1-e5">Episode 5</a>
                </div>
            """.trimIndent(),
        )
        assertEquals(2, SeriesCatalogParser.linkedEpisodePages(first, firstPageUrl).size)
        val episodes = SeriesCatalogParser.collectPaginatedEpisodes(
            firstDocument = first,
            firstPageUrl = firstPageUrl,
            fetchDocument = { url ->
                val page = java.net.URI(url).rawQuery.orEmpty()
                    .split('&')
                    .firstOrNull { it.startsWith("page=") }
                    ?.substringAfter('=')
                page?.let(pages::get)?.let { Jsoup.parse(it, url) }
            },
            parseEpisodes = { document ->
                document.select(".episodes-list a").mapNotNull { link ->
                    val number = SeriesCatalogParser.episodeNumber(link.text(), link.absUrl("href"))
                        ?: return@mapNotNull null
                    Triple(link.text(), link.absUrl("href"), number)
                }
            },
            maxPages = 10,
            timeoutSeconds = 3,
        )

        assertEquals(listOf(1, 2, 3, 4, 5), episodes.mapNotNull {
            SeriesCatalogParser.episodeNumber(it.first, it.second)
        }.distinct().sorted())
    }

    @Test
    fun retainsNumberedEpisodePagesPastTheFormerTwelvePageLimit() {
        val firstPageUrl = "https://m.myseed.tv/series/long-season"
        val firstDocument = Jsoup.parse(
            buildString {
                append("<div class='episodes-list'><a href='/episode-1'>Episode 1</a></div>")
                append("<div class='pagination'>")
                for (page in 2..16) append("<a href='?page=$page'>$page</a>")
                append("</div>")
            },
            firstPageUrl,
        )
        val episodePages = (2..16).associate { page ->
            page.toString() to """
                <div class="episodes-list">
                  <a href="/episode-$page">Episode $page</a>
                </div>
            """.trimIndent()
        }
        assertEquals(15, SeriesCatalogParser.linkedEpisodePages(firstDocument, firstPageUrl).size)

        val episodes = SeriesCatalogParser.collectPaginatedEpisodes(
            firstDocument = firstDocument,
            firstPageUrl = firstPageUrl,
            fetchDocument = { url ->
                val page = java.net.URI(url).rawQuery.orEmpty()
                    .substringAfter("page=", "")
                episodePages[page]?.let { Jsoup.parse(it, url) }
            },
            parseEpisodes = { document ->
                document.select(".episodes-list a").mapNotNull { link ->
                    val number = SeriesCatalogParser.episodeNumber(link.text(), link.absUrl("href"))
                        ?: return@mapNotNull null
                    Triple(link.text(), link.absUrl("href"), number)
                }
            },
            maxPages = 20,
            timeoutSeconds = 5,
        )

        assertEquals((1..16).toList(), episodes.mapNotNull {
            SeriesCatalogParser.episodeNumber(it.first, it.second)
        }.distinct().sorted())
    }

    @Test
    fun parsesTrailingEpisodeLabelsCompactAndManySeasonsAcrossProviders() {
        // Prestige trailing format + inline multi-season panel
        assertEquals(1, SeriesCatalogParser.episodeNumber("1 حلقة", "https://hd1.brstej.com/video.php?vid=6531582dd"))
        assertEquals(219, SeriesCatalogParser.episodeNumber("219 حلقة", "https://hd1.brstej.com/video.php?vid=faff13d4d"))
        // Akwam trailing episode label + -s8 URL guard (must not misclassify ep 8 as season 8)
        assertEquals(8, SeriesCatalogParser.episodeNumber("8 حلقة", "https://akw.cam/akdrama-show-s8/"))
        assertNull(SeriesCatalogParser.seasonNumber("8 حلقة", "https://akw.cam/akdrama-show-s8/"))
        assertEquals(1179, SeriesCatalogParser.episodeNumber("1179 حلقة", "https://akw.cam/one-piece-s1179/"))
        // Esheeq compact label
        assertEquals(72, SeriesCatalogParser.episodeNumber("وادي الذئاب ح 72", "https://qesset.net/clarus/kurtlar"))
        // Hamza normalization for classic Arabic movie/series queries
        assertEquals("عسل اسود", SeriesCatalogParser.normalizeArabicSearchQuery("عَسَل إسْوِد"))

        // 23-season page like Grey's Anatomy / One Piece retains all season chips without 30-item truncation
        val manySeasonsDoc = Jsoup.parse(
            buildString {
                append("<div class='seasons'>")
                for (s in 1..25) append("<a class='chip' href='/series/one-piece-37854/season-$s/'>الموسم $s</a>")
                append("</div>")
            },
            "https://www.shahid4u.si/series/one-piece-37854/",
        )
        val seasons = SeriesCatalogParser.linkedSeasonPages(
            manySeasonsDoc,
            "https://www.shahid4u.si/series/one-piece-37854/",
        )
        assertEquals((1..25).toList(), seasons.map { it.first })

        // Prestige inline .pwr-season-panel + WeCima .epslist without sidebar leakage
        val mixedDoc = Jsoup.parse(
            """
            <div class="pwr-season-panel" id="pwr-season-0">
              <h3 class="pwr-season-fallback">الموسم 10</h3>
              <a href="https://hd1.brstej.com/video.php?vid=a1">1 حلقة</a>
              <a href="https://hd1.brstej.com/video.php?vid=a2">34 حلقة</a>
            </div>
            <div class="related">
              <a href="https://hd1.brstej.com/video.php?vid=other">999 حلقة</a>
            </div>
            """.trimIndent(),
            "https://hd1.brstej.com/video.php?vid=a1",
        )
        val parsed = SeriesCatalogParser.parseEpisodeEntries(
            document = mixedDoc,
            currentPageUrl = "https://hd1.brstej.com/video.php?vid=a1",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("hd1.brstej.com") },
        )
        assertEquals(2, parsed.size)
        assertEquals(listOf(10, 10), parsed.map { it.third })

        // Clean title strips consecutive category prefixes & prevents mega-episode arc truncation
        assertEquals("One Piece", SeriesCatalogParser.cleanSeriesTitle("مسلسل انمي One Piece الحلقة 1179 مترجمة"))
        assertFalse(MultiProviderRepository.titlesStrictlyMatch("One Piece", "One Piece Log: Fish-Man Island Saga"))
        assertFalse(MultiProviderRepository.titlesStrictlyMatch("One Piece", "Lego One Piece"))
        assertTrue(MultiProviderRepository.titlesStrictlyMatch("One Piece", "ون بيس One Piece"))
        assertTrue(MultiProviderRepository.titlesStrictlyMatch("Grey’s Anatomy", "تشريح غراي Grey's Anatomy"))
        assertEquals(
            "shahid4u_si",
            MultiProviderRepository.providerIdForUrl("https://www.shahid4u.si/series/grey-s-anatomy-1416/"),
        )

        val megaBundle = SeriesCatalogParser.normalizeSeasonBundle(
            ArabseedSeasonBundle(
                seasons = listOf(
                    ArabseedSeason(
                        number = 1,
                        title = "الموسم الأول",
                        episodes = listOf(
                            episode(1, 1, "https://example.com/op-1"),
                            episode(2, 1, "https://example.com/op-2"),
                            episode(843, 1, "https://example.com/op-843"),
                            episode(844, 1, "https://example.com/op-844"),
                            episode(1179, 1, "https://example.com/op-1179"),
                            episode(1180, 1, "https://example.com/op-1180"),
                            episode(9999, 1, "https://example.com/stray-9999"),
                        ),
                    ),
                ),
            ),
        )
        assertEquals(listOf(1, 2, 843, 844, 1179, 1180), megaBundle.seasons.single().episodes.map { it.number })
    }

    private fun episode(number: Int, season: Int, url: String) = ArabseedEpisode(
        number = number,
        title = "الحلقة $number",
        pageUrl = url,
        seasonNumber = season,
        providerName = "fixture",
    )
}
