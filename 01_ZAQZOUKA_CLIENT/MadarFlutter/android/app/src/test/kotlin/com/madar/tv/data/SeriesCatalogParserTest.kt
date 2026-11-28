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
    fun findsMySeedCanonicalAndSeasonParentsFromSelaryBreadcrumbs() {
        val episodeUrl =
            "https://m.myseed.tv/%d9%85%d8%b3%d9%84%d8%b3%d9%84-doctor-x-mafia-in-white-s1-eps1/"
        val canonicalUrl =
            "https://m.myseed.tv/selary/%d9%85%d8%b3%d9%84%d8%b3%d9%84-doctor-x-mafia-in-white-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/"
        val seasonUrl =
            "https://m.myseed.tv/selary/%d9%85%d8%b3%d9%84%d8%b3%d9%84-doctor-x-mafia-in-white-%d8%a7%d9%84%d9%85%d9%88%d8%b3%d9%85-%d8%a7%d9%84%d8%a7%d9%88%d9%84-%d9%85%d8%aa%d8%b1%d8%ac%d9%85/"
        val document = Jsoup.parse(
            """
            <div class="episode-breadcrumb">
              <a href="$canonicalUrl">مسلسل Doctor X: Mafia in White مترجم</a>
              <a href="$seasonUrl">مسلسل Doctor X: Mafia in White الموسم الاول مترجم</a>
            </div>
            <div class="related"><a href="/selary/alls-fair-season-2">All's Fair الموسم الثاني</a></div>
            """.trimIndent(),
            episodeUrl,
        )

        val parents = SeriesCatalogParser.linkedSeriesParentPages(
            document = document,
            currentPageUrl = episodeUrl,
            isAllowedContentUrl = { it.startsWith("https://m.myseed.tv/") },
        )

        assertEquals(listOf(canonicalUrl, seasonUrl), parents.map { it.first })
        assertTrue(parents.none { it.first.contains("alls-fair") })
        assertTrue(
            parents.all { (url, title) ->
                SeriesCatalogParser.pageBelongsToSeries(
                    targetTitles = listOf(
                        "مسلسل Doctor X: Mafia in White الموسم الاول الحلقة 1 الاولي مترجمة",
                    ),
                    targetPageUrls = listOf(episodeUrl),
                    candidateTitles = listOf(title),
                    candidatePageUrls = listOf(url),
                )
            },
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
    fun matchesOnlyTheFullSeriesIdentityFromProviderSearchResults() {
        val identity = "Doctor X: Mafia in White"
        val episodeTitle = "مسلسل Doctor X: Mafia in White الموسم الاول الحلقة 1 الاولي مترجمة"

        assertTrue(
            SeriesCatalogParser.matchesExactSeriesIdentity(
                targetIdentities = listOf(identity),
                candidateTitles = listOf(episodeTitle),
            ),
        )
        assertFalse(
            SeriesCatalogParser.matchesExactSeriesIdentity(
                targetIdentities = listOf(identity),
                candidateTitles = listOf("Doctor on the Edge الموسم الأول الحلقة 6"),
            ),
        )
        assertFalse(
            SeriesCatalogParser.matchesExactSeriesIdentity(
                targetIdentities = listOf(identity),
                candidateTitles = listOf("Doctor X الموسم الأول الحلقة 6"),
            ),
        )
    }

    @Test
    fun rejectsEpisodeLinksFromUnrelatedRecommendationsOnASeasonPage() {
        val targetTitle = "مسلسل انمي One Piece الحلقة 1178 مترجمة"
        val targetUrl = "https://m.myseed.tv/مسلسل-انمي-one-piece-s1-eps1178/"
        assertEquals("one piece", SeriesCatalogParser.seriesIdentityFromUrl(targetUrl))
        assertEquals(
            "one piece",
            SeriesCatalogParser.seriesIdentityFromUrl(
                "https://m.myseed.tv/انمي-one-piece-الحلقة-843-مترجمة/",
            ),
        )

        assertTrue(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf(targetTitle),
                targetPageUrls = listOf(targetUrl),
                episodeTitle = "الحلقة 843",
                episodeUrl = "https://m.myseed.tv/انمي-one-piece-الحلقة-843-مترجمة/",
            ),
        )
        assertFalse(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf(targetTitle),
                targetPageUrls = listOf(targetUrl),
                episodeTitle = "مسلسل Alls Fair الموسم الثاني الحلقة 3 مترجمة",
                episodeUrl = "https://m.myseed.tv/مسلسل-alls-fair-s2-eps3/",
            ),
        )
        assertFalse(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf("One Piece"),
                targetPageUrls = emptyList(),
                episodeTitle = "Episode 3",
                episodeUrl = "https://wecima.cc/video/wa4th782mvck/",
            ),
        )
        assertTrue(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf("One Piece"),
                targetPageUrls = emptyList(),
                episodeTitle = "Episode 3",
                episodeUrl = "https://wecima.cc/video/wa4th782mvck/",
                allowEpisodeListContext = true,
            ),
        )
    }

    @Test
    fun matchesGenericEpisodeTitlesWhenTheReadableEpisodeSlugContainsTheSeriesName() {
        val cases = listOf(
            Triple(
                "One Piece",
                "https://m.myseed.tv/anime/one-piece",
                "https://m.myseed.tv/anime/one-piece-episode-843",
            ),
            Triple(
                "One Piece",
                "https://m.myseed.tv/anime/one-piece",
                "https://m.myseed.tv/anime/انمي-one-piece-الحلقة-843-مترجمة",
            ),
            Triple(
                "Crash",
                "https://akw.cam/akdrama-crash",
                "https://akw.cam/akdrama-crash-e2-s4",
            ),
            Triple(
                "Crash",
                "https://c4u.cam/crash",
                "https://c4u.cam/crash-m2-n4",
            ),
        )

        cases.forEach { (title, seriesUrl, episodeUrl) ->
            assertTrue(
                "$episodeUrl should resolve to $title",
                SeriesCatalogParser.episodeBelongsToSeries(
                    targetTitles = listOf(title),
                    targetPageUrls = listOf(seriesUrl),
                    episodeTitle = "الحلقة 4",
                    episodeUrl = episodeUrl,
                ),
            )
        }
    }

    @Test
    fun providerSearchUsesTheCardHeadingInsteadOfGenericPosterAltText() {
        val provider = MultiProviderRepository.PROVIDERS.first { it.id == "wecima" }
        val page = """
            <ul class="latest-data">
              <li class="video-grid">
                <div class="thumb">
                  <a href="https://wecima.cc/video/wa4th782mvck/">
                    <img alt="One Piece" src="poster.webp">
                  </a>
                </div>
                <div class="data">
                  <h2 class="title">
                    <a href="https://wecima.cc/video/wa4th782mvck/">
                      أنمي ون بيس الحلقة 1153 مترجمة ماي سيما – One Piece
                    </a>
                  </h2>
                </div>
              </li>
            </ul>
        """.trimIndent()

        val results = MultiProviderRepository.parseItems(page, provider, ArabseedType.SERIES)

        assertEquals(1, results.size)
        assertTrue(results.single().title.contains("الحلقة 1153"))
        assertTrue(SeriesCatalogParser.isEpisode(results.single().title, results.single().pageUrl))
    }

    @Test
    fun everyEnabledProviderIsIncludedInSeasonEnrichment() {
        val expectedIds = setOf(
            "arabseed", "wecima", "akwam", "faselhd", "iegybest", "topcinema",
            "shahid4u", "shahid4u_si", "cima4u", "fushaar", "arablionz", "lodynet",
            "aradrama", "esheeq", "prestige", "cimaclub", "egydead", "cimanow", "cimawbas",
        )

        assertEquals(expectedIds, MultiProviderRepository.seasonEnrichmentProviders().map { it.id }.toSet())
        assertEquals(18, MultiProviderRepository.seasonEnrichmentProviders("arabseed").size)
        assertFalse(MultiProviderRepository.seasonEnrichmentProviders().any { !it.enabled })
    }

    @Test
    fun directEpisodeMergePreservesSameEpisodeMirrorsWithoutCrossingSeasonOrNumber() {
        val merged = MultiProviderRepository.mergeDiscoveredEpisodeMirrors(
            listOf(
                ArabseedEpisode(
                    number = 1,
                    title = "الحلقة 1",
                    pageUrl = "https://m.myseed.tv/one-piece-s1-e1/",
                    seasonNumber = 1,
                    providerName = "عرب سيد",
                ),
                ArabseedEpisode(
                    number = 1,
                    title = "الحلقة 1",
                    pageUrl = "https://fushaar.cam/one-piece-season-1-episode-1/",
                    seasonNumber = 1,
                    providerName = "فشار",
                ),
                ArabseedEpisode(
                    number = 2,
                    title = "الحلقة 2",
                    pageUrl = "https://fushaar.cam/one-piece-season-1-episode-2/",
                    seasonNumber = 1,
                    providerName = "فشار",
                ),
                ArabseedEpisode(
                    number = 1,
                    title = "الحلقة 1",
                    pageUrl = "https://fushaar.cam/one-piece-season-2-episode-1/",
                    seasonNumber = 2,
                    providerName = "فشار",
                ),
            ),
        )

        assertEquals(listOf(1 to 1, 1 to 2, 2 to 1), merged.map { it.seasonNumber to it.number })
        val firstSeasonFirstEpisode = merged.first()
        assertEquals("https://m.myseed.tv/one-piece-s1-e1/", firstSeasonFirstEpisode.pageUrl)
        assertEquals(
            listOf("https://fushaar.cam/one-piece-season-1-episode-1/"),
            firstSeasonFirstEpisode.alternatePageUrls,
        )
        assertTrue(merged.drop(1).all { it.alternatePageUrls.isEmpty() })
    }

    @Test
    fun assignsSeasonOnlyFromAnExplicitMarkerOrParentSeasonContext() {
        assertEquals(
            7,
            SeriesCatalogParser.directEpisodeSeasonNumber(
                title = "Doctor X الموسم السابع الحلقة 1",
                url = "https://example.test/doctor-x-s7-e1",
                parentSeasonNumber = 1,
            ),
        )
        assertNull(
            SeriesCatalogParser.directEpisodeSeasonNumber(
                title = "Episode 9",
                url = "https://example.test/episode-9",
                parentSeasonNumber = null,
            ),
        )
        assertNull(
            SeriesCatalogParser.directEpisodeSeasonNumber(
                title = "Episode 9",
                url = "https://example.test/episode-9",
                parentSeasonNumber = null,
            ),
        )
    }

    @Test
    fun rejectsConflictingSeasonAndEpisodeNumbersBetweenTitleAndUrl() {
        assertNull(
            SeriesCatalogParser.episodeNumber(
                title = "Doctor X الموسم الثاني الحلقة 8",
                url = "https://example.test/doctor-x-s2-episode-9",
            ),
        )
        assertNull(
            SeriesCatalogParser.seasonNumber(
                title = "Doctor X الموسم الثاني الحلقة 8",
                url = "https://example.test/doctor-x-s3-episode-8",
            ),
        )
        assertNull(
            SeriesCatalogParser.directEpisodeSeasonNumber(
                title = "Doctor X الموسم الثاني الحلقة 8",
                url = "https://example.test/doctor-x-s3-episode-8",
                parentSeasonNumber = 2,
            ),
        )
    }

    @Test
    fun rejectsDatabaseSeasonIdsInsteadOfTreatingThemAsSeasonNumbers() {
        val currentUrl = "https://provider.test/series/doctor-x"
        val document = Jsoup.parse(
            """
                <nav class="seasons">
                  <li data-season-number="503201" data-url="/series/doctor-x?tab=503201">الموسم</li>
                  <a data-season-number="2" href="/series/doctor-x-season-2">الموسم الثاني</a>
                </nav>
            """.trimIndent(),
            currentUrl,
        )

        val linkedSeasons = SeriesCatalogParser.linkedSeasonPages(document, currentUrl) { true }
        assertEquals(listOf(2), linkedSeasons.map { it.first })
        assertNull(SeriesCatalogParser.seasonNumber("", "https://provider.test/season-503201"))

        val normalized = SeriesCatalogParser.normalizeSeasonBundle(
            ArabseedSeasonBundle(
                listOf(
                    ArabseedSeason(503201, "الموسم", emptyList(), "https://provider.test/season-db-id"),
                ),
            ),
        )
        assertTrue(normalized.seasons.isEmpty())
    }

    @Test
    fun rejectsSeasonlessMirrorRowsAndUncorroboratedExtraSeasons() {
        val target = seriesItem("One Piece", "https://primary.test/anime/one-piece")
        val primary = ArabseedSeasonBundle(
            listOf(
                season(1, "https://primary.test/anime/one-piece/season-1", episode(1, 1, "https://primary.test/anime/one-piece-s1-e1")),
                season(2, "https://primary.test/anime/one-piece/season-2", episode(1, 2, "https://primary.test/anime/one-piece-s2-e1")),
            ),
        )
        val mirror = seriesItem("One Piece", "https://mirror.test/anime/one-piece")
        val unscopedBundle = ArabseedSeasonBundle(
            listOf(
                season(1, "https://mirror.test/anime/one-piece", episode(1153, 1, "https://mirror.test/anime/one-piece-episode-1153")),
                season(28, "https://mirror.test/anime/one-piece-season-28", episode(1, 28, "https://mirror.test/anime/one-piece-s28-e1")),
            ),
        )

        val result = MultiProviderRepository.reconcileMirrorSeasonBundle(target, primary, mirror, unscopedBundle)

        assertTrue(result.seasons.isEmpty())
    }

    @Test
    fun mergesOnlyEpisodesFromAnExplicitSeasonThatExistsOnThePrimary() {
        val target = seriesItem("Doctor X: Mafia in White", "https://primary.test/series/doctor-x-mafia-in-white")
        val primary = ArabseedSeasonBundle(
            listOf(
                season(1, "https://primary.test/series/doctor-x-mafia-in-white-season-1", episode(1, 1, "https://primary.test/series/doctor-x-mafia-in-white-s1-e1")),
                season(2, "https://primary.test/series/doctor-x-mafia-in-white-season-2", episode(1, 2, "https://primary.test/series/doctor-x-mafia-in-white-s2-e1")),
            ),
        )
        val mirror = seriesItem("Doctor X: Mafia in White", "https://mirror.test/series/doctor-x-mafia-in-white-season-1")
        val source = ArabseedSeasonBundle(
            listOf(
                season(1, "https://mirror.test/series/doctor-x-mafia-in-white-season-1", episode(2, 1, "https://mirror.test/series/doctor-x-mafia-in-white-s1-e2")),
                season(2, "https://mirror.test/series/doctor-x-mafia-in-white-season-2", episode(99, 2, "https://mirror.test/series/doctor-x-mafia-in-white-s2-e99")),
            ),
        )

        val result = MultiProviderRepository.reconcileMirrorSeasonBundle(target, primary, mirror, source)

        assertEquals(listOf(1), result.seasons.map { it.number })
        assertEquals(listOf(2), result.seasons.single().episodes.map { it.number })
    }

    @Test
    fun rejectsMirrorWhenReadablePageSlugNamesAnotherSeries() {
        val target = seriesItem("Doctor X: Mafia in White", "https://primary.test/series/doctor-x-mafia-in-white")
        val primary = ArabseedSeasonBundle(
            listOf(season(1, "https://primary.test/series/doctor-x-mafia-in-white-season-1", episode(1, 1, "https://primary.test/doctor-x-mafia-in-white-s1-e1"))),
        )
        val wrongMirror = seriesItem("Doctor X: Mafia in White", "https://mirror.test/series/alls-fair-season-2")
        val source = ArabseedSeasonBundle(
            listOf(season(1, "https://mirror.test/series/alls-fair-season-2", episode(3, 1, "https://mirror.test/series/alls-fair-s2-e3"))),
        )

        val result = MultiProviderRepository.reconcileMirrorSeasonBundle(target, primary, wrongMirror, source)

        assertTrue(result.seasons.isEmpty())
    }

    @Test
    fun startsNumberedPaginationAtPageTwoWhenTheRootHasNoPageNumber() {
        val rootUrl = "https://m.myseed.tv/series/example-season-1"
        val document = Jsoup.parse(
            """
            <div class="pagination">
              <a href="?page=2">2</a>
              <a href="?page=3">3</a>
            </div>
            """.trimIndent(),
            rootUrl,
        )

        assertEquals(
            "https://m.myseed.tv/series/example-season-1?page=2",
            SeriesCatalogParser.nextEpisodePage(document, rootUrl),
        )
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
    fun flagsTheOnePieceGapVisibleInTheLatestDeviceScreenshot() {
        val numbers = (881..891).toList() + (1166..1177).toList()

        assertEquals(listOf(892..1165), SeriesCatalogParser.episodeSequenceGaps(numbers))
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(numbers))
    }

    @Test
    fun reportsTheLargeOnePieceEpisodeGapShownInTheDeviceScreenshot() {
        val numbers = (881..891).toList() + (1166..1177).toList()

        assertEquals(listOf(892..1165), SeriesCatalogParser.episodeSequenceGaps(numbers))
        assertEquals(
            listOf(1..880, 892..1165),
            SeriesCatalogParser.episodeSequenceCoverageGaps(numbers),
        )
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(numbers))
    }

    @Test
    fun treatsAContiguousListStartingAtALateEpisodeAsIncomplete() {
        val numbers = 31..34

        assertEquals(listOf(1..30), SeriesCatalogParser.episodeSequenceCoverageGaps(numbers.toList()))
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(numbers.toList()))
    }

    @Test
    fun keepsAContiguousButStaleListIncompleteWhenCurrentEpisodeProvesItsTailIsMissing() {
        val numbers = 1..8

        assertEquals(
            listOf(9..1177),
            SeriesCatalogParser.episodeSequenceCoverageGaps(
                numbers.toList(),
                expectedLastEpisode = 1177,
            ),
        )
    }

    @Test
    fun includesTheKnownCurrentEpisodeWhenDetectingTheOnePieceGap() {
        val numbers = (881..891).toList() + (1166..1177).toList()

        assertEquals(
            listOf(1..880, 892..1165),
            SeriesCatalogParser.episodeSequenceCoverageGaps(
                numbers,
                expectedLastEpisode = 1177,
            ),
        )
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
    fun retainsLargeAnimeEpisodePagersInsteadOfStoppingAtSixtyPages() {
        val pageUrl = "https://m.myseed.tv/series/one-piece"
        val document = Jsoup.parse(
            buildString {
                append("<div class='episodes-list'><a href='/episode-1'>Episode 1</a></div>")
                append("<div class='pagination'>")
                for (page in 2..140) append("<a href='?page=$page'>$page</a>")
                append("</div>")
            },
            pageUrl,
        )

        val pages = SeriesCatalogParser.linkedEpisodePages(document, pageUrl)
        assertEquals(120, pages.size)
        assertTrue(pages.any { it.endsWith("page=121") })
    }

    @Test
    fun parsesTrailingEpisodeLabelsCompactAndManySeasonsAcrossProviders() {
        // Prestige trailing format + inline multi-season panel
        assertEquals(1, SeriesCatalogParser.episodeNumber("1 حلقة", "https://hd1.brstej.com/video.php?vid=6531582dd"))
        assertEquals(219, SeriesCatalogParser.episodeNumber("219 حلقة", "https://hd1.brstej.com/video.php?vid=faff13d4d"))
        // Akwam trailing episode label + -s8 / -s11 / -e2-s4 / -e7-s12 guards
        assertEquals(8, SeriesCatalogParser.episodeNumber("8 حلقة", "https://akw.cam/akdrama-show-s8/"))
        assertNull(SeriesCatalogParser.seasonNumber("8 حلقة", "https://akw.cam/akdrama-show-s8/"))
        assertEquals(11, SeriesCatalogParser.episodeNumber("حب متنكر Love In Disguise", "https://akw.cam/akdrama-hb-mtnkr-love-in-disguise-s11/"))
        assertNull(SeriesCatalogParser.seasonNumber("حب متنكر Love In Disguise", "https://akw.cam/akdrama-hb-mtnkr-love-in-disguise-s11/"))
        assertEquals(4, SeriesCatalogParser.episodeNumber("تصادم Crash", "https://akw.cam/akdrama-tsadm-crash-e2-s4/"))
        assertEquals(2, SeriesCatalogParser.seasonNumber("تصادم Crash", "https://akw.cam/akdrama-tsadm-crash-e2-s4/"))
        assertEquals(12, SeriesCatalogParser.episodeNumber("جبل جونول", "https://akw.cam/akdrama-jbl-jwnwl-hkayh-anatwlyh-al-e7-s12/"))
        assertEquals(7, SeriesCatalogParser.seasonNumber("جبل جونول", "https://akw.cam/akdrama-jbl-jwnwl-hkayh-anatwlyh-al-e7-s12/"))
        assertTrue(SeriesCatalogParser.needsEpisodeSequenceRepair(listOf(9, 10, 11)))
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

    @Test
    fun extractsAraDramaArchiveAndCimaArabLionzAkwamLodynetFormats() {
        // Cima4U & ArabLionz -mS-nE and -nE patterns + Lodynet "حلقة رقم N"
        assertEquals(4, SeriesCatalogParser.episodeNumber("الحلقة", "https://c4u.cam/tsadm-crash-m2-n4/"))
        assertEquals(2, SeriesCatalogParser.seasonNumber("الحلقة", "https://c4u.cam/tsadm-crash-m2-n4/"))
        assertEquals(11, SeriesCatalogParser.episodeNumber("حب متنكر", "https://arablionz.cam/hb-mtnkr-love-in-disguise-n11/"))
        assertNull(SeriesCatalogParser.seasonNumber("حب متنكر", "https://arablionz.cam/hb-mtnkr-love-in-disguise-n11/"))
        assertEquals(10, SeriesCatalogParser.episodeNumber("حلقة رقم 10 حصريا مسلسل تصادم", "https://lodynet.watch/crash-ep-10"))

        // AraDrama series report page CTA vs generic navigation menu links
        val reportUrl = "https://aradramatv.cc/2026/11/crash-2-fury-road/"
        val reportDoc = Jsoup.parse(
            """
            <header>
              <nav><a href="https://aradramatv.cc/category/episodes/new/">الحلقات الجديدة</a></nav>
            </header>
            <article class="r_post">
              <a class="vc_btn3" href="https://aradramatv.cc/category/episodes/new/korean-ep/%d8%aa%d8%b5%d8%a7%d8%af%d9%85-2-%d8%b7%d8%b1%d9%8a%d9%82-%d8%a7%d9%84%d8%ba%d8%b6%d8%a8/">مشاهدة حلقات المسلسل</a>
            </article>
            """.trimIndent(),
            reportUrl,
        )
        assertFalse(SeriesCatalogParser.isSpecificSeriesArchiveUrl("https://aradramatv.cc/category/episodes/new/"))
        assertFalse(SeriesCatalogParser.isSpecificSeriesArchiveUrl("https://aradramatv.cc/category/episodes/new/korean-ep/"))
        assertFalse(SeriesCatalogParser.isSpecificSeriesArchiveUrl("https://akw.cam/category/مسلسلات-تركي"))
        assertTrue(SeriesCatalogParser.isSpecificSeriesArchiveUrl("https://lodynet.watch/category/مسلسل-تصادم-مترجم/"))
        val archiveUrls = SeriesCatalogParser.findSeriesEpisodeArchiveUrls(reportDoc, reportUrl)
        assertEquals(1, archiveUrls.size)
        assertTrue(archiveUrls.single().first.contains("korean-ep"))

        // Parse episodes from AraDrama archive page + Akwam/Cima4U/ArabLionz/Shahid4U grids
        val archiveUrl = archiveUrls.single().first
        val archiveDoc = Jsoup.parse(
            """
            <main>
              <article class="hentry post_format-post-format-video"><a class="first_A" href="https://aradramatv.cc/2026/11/crash-2-ep-4/"></a><a href="https://aradramatv.cc/2026/11/crash-2-ep-4/">تصادم 2 الحلقة 04</a></article>
              <article class="hentry post_format-post-format-video"><a href="https://aradramatv.cc/2026/11/crash-2-ep-1/">تصادم 2 الحلقة 01</a></article>
            </main>
            """.trimIndent(),
            archiveUrl,
        )
        val archiveEps = SeriesCatalogParser.parseEpisodeEntries(
            document = archiveDoc,
            currentPageUrl = archiveUrl,
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("aradramatv.cc") },
        )
        assertEquals(listOf(4, 1), archiveEps.mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) })

        val akwamDoc = Jsoup.parse(
            """
            <section id="episodes"><div class="br-episode-grid">
              <a href="https://akw.cam/akdrama-tsadm-crash-e2-s1/">1حلقة</a>
              <a href="https://akw.cam/akdrama-tsadm-crash-e2-s2/">2حلقة</a>
            </div></section>
            """.trimIndent(),
            "https://akw.cam/akdrama-tsadm-crash-e2-s2/",
        )
        val akwamEps = SeriesCatalogParser.parseEpisodeEntries(
            document = akwamDoc,
            currentPageUrl = "https://akw.cam/akdrama-tsadm-crash-e2-s2/",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("akw.cam") },
        )
        assertEquals(listOf(1, 2), akwamEps.mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) })
        assertEquals(listOf(2, 2), akwamEps.map { it.third })

        // Bilingual subtitle matching: AraDrama full title vs shorter mirror titles
        assertTrue(
            MultiProviderRepository.titlesStrictlyMatch(
                "تصادم 2: طريق الغضب Crash 2: Fury Road",
                "مسلسل تصادم Crash الموسم الثاني",
            ),
        )
        assertFalse(
            MultiProviderRepository.titlesStrictlyMatch(
                "تصادم 2: طريق الغضب Crash 2: Fury Road",
                "Crash Course in Romance",
            ),
        )
    }

    @Test
    fun extractsPrestigeFaselTopCinemaWecimaEsheeqCimaNowPhpMelodyLiveFormats() {
        // 1. Prestige (hd1.brstej.com): .pds-episodes + #pds-season-1/2 + tab link
        val prestigeHtml = """
            <section class="pds-panel">
              <nav class="pds-seasons">
                <a href="#pds-season-1">الموسم 1</a>
                <a href="#pds-season-2">الموسم 2</a>
              </nav>
              <section id="pds-season-1" class="pds-season"><div class="pds-episodes">
                <article class="pds-episode"><a class="pds-thumb" href="https://hd1.brstej.com/video.php?vid=aaa1">▶</a><h4><a href="https://hd1.brstej.com/video.php?vid=aaa1">الحلقة 1</a></h4></article>
                <article class="pds-episode"><h4><a href="https://hd1.brstej.com/video.php?vid=aaa2">الحلقة 2</a></h4></article>
              </div></section>
              <section id="pds-season-2" class="pds-season"><div class="pds-episodes">
                <article class="pds-episode"><h4><a href="https://hd1.brstej.com/video.php?vid=bbb1">الحلقة 1</a></h4></article>
              </div></section>
            </section>
        """.trimIndent()
        val prestigeDoc = Jsoup.parse(prestigeHtml, "https://hd1.brstej.com/series1.php?id=test")
        val prestigeEps = SeriesCatalogParser.parseEpisodeEntries(
            document = prestigeDoc,
            currentPageUrl = "https://hd1.brstej.com/series1.php?id=test",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("brstej.com") },
        )
        assertEquals(3, prestigeEps.size)
        assertEquals(listOf(1, 1, 2), prestigeEps.map { it.third })
        assertEquals(listOf(1, 2, 1), prestigeEps.mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) })

        // 2. PHPMelody (#Season0 -> "الموسم 1", #Season1 -> "الموسم 2") + tag.php?t= archive CTA
        val phpHtml = """
            <div class="Tab">
              <button class="tablinks" onclick="openTab(event, 'Season0')">الموسم 1</button>
              <button class="tablinks" onclick="openTab(event, 'Season1')">الموسم 2</button>
              <div id="Season0" class="tabcontent"><ul>
                <li><a href="https://cimawbas.rest/watch.php?vid=ep1">1 حلقة</a></li>
              </ul></div>
              <div id="Season1" class="tabcontent"><ul>
                <li><a href="https://cimawbas.rest/watch.php?vid=ep20">20 حلقة</a></li>
              </ul></div>
            </div>
            <a href="https://cimawbas.rest/tag.php?t=test-all">مسلسل جميع الحلقات</a>
        """.trimIndent()
        val phpDoc = Jsoup.parse(phpHtml, "https://cimawbas.rest/watch.php?vid=ep1")
        val phpEps = SeriesCatalogParser.parseEpisodeEntries(
            document = phpDoc,
            currentPageUrl = "https://cimawbas.rest/watch.php?vid=ep1",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("cimawbas.rest") },
        )
        assertEquals(listOf(1, 2), phpEps.map { it.third })
        val phpArchives = SeriesCatalogParser.findSeriesEpisodeArchiveUrls(phpDoc, "https://cimawbas.rest/watch.php?vid=ep1")
        assertTrue(phpArchives.any { it.first.contains("tag.php?t=test-all") })

        // 3. Wecima .epslist a.episode-card must include series eps and ignore widget-video-s2 sidebar
        val wecimaHtml = """
            <div class="tab-content active"><div class="epslist">
              <a class="episode-card current-episode" href="https://wecima.cc/video/ep1/">الحلقة 1</a>
              <a class="episode-card" href="https://wecima.cc/video/ep2/">الحلقة 2</a>
            </div></div>
            <ul><li class="widget-video-s2"><h2 class="title"><a href="https://wecima.cc/video/conan/">المحقق كونان الحلقة 1184</a></h2></li></ul>
        """.trimIndent()
        val wecimaEps = SeriesCatalogParser.parseEpisodeEntries(
            document = Jsoup.parse(wecimaHtml, "https://wecima.cc/video/ep1/"),
            currentPageUrl = "https://wecima.cc/video/ep1/",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("wecima.cc") },
        )
        assertEquals(listOf(1, 2), wecimaEps.mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) })

        // 4. TopCinema section.allseasonss links + section.allepcont + excluding section.otherser
        val topHtml = """
            <section class="allseasonss"><ul class="Blocks--List">
              <div class="Small--Box Season"><a href="https://topcinemaa.tv/series/show-s1/">الموسم 1 مسلسل Show</a></div>
              <div class="Small--Box Season"><a href="https://topcinemaa.tv/series/show-s2/">الموسم 2 مسلسل Show</a></div>
            </ul></section>
            <section class="otherser"><ul class="Blocks--List">
              <div class="Small--Box"><a class="recent--block" href="https://topcinemaa.tv/series/other-ep3/">مسلسل آخر الحلقة 3</a></div>
            </ul></section>
        """.trimIndent()
        val topDoc = Jsoup.parse(topHtml, "https://topcinemaa.tv/series/show/")
        val topSeasons = SeriesCatalogParser.linkedSeasonPages(topDoc, "https://topcinemaa.tv/series/show/") {
            it.contains("topcinemaa.tv")
        }
        assertEquals(listOf(1, 2), topSeasons.map { it.first })
        assertTrue(SeriesCatalogParser.parseEpisodeEntries(topDoc, "https://topcinemaa.tv/series/show/", 1) { true }.isEmpty())

        // 5. Esheeq article.postEp shot90?url=base64 unwrap + Cima4U DB-ID guard (#br-season-744 != season 744)
        val shotTarget = java.util.Base64.getUrlEncoder().withoutPadding()
            .encodeToString("https://qesset.net/clarus/test-show-ep-39/".toByteArray(Charsets.UTF_8))
        val esheeqHtml = """
            <article class="postEp"><div class="block-post">
              <a href="https://shot90.com/article/123?url=$shotTarget">حلقة 39 هذا البحر سوف يفيض</a>
            </div></article>
        """.trimIndent()
        val esheeqEps = SeriesCatalogParser.parseEpisodeEntries(
            document = Jsoup.parse(esheeqHtml, "https://qesset.net/yeni-show/test/"),
            currentPageUrl = "https://qesset.net/yeni-show/test/",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("qesset.net") },
        )
        assertEquals(listOf("https://qesset.net/clarus/test-show-ep-39/"), esheeqEps.map { it.second })

        val c4uDoc = Jsoup.parse(
            """
            <section id="br-season-744" class="br-series-season"><div class="br-series-detail-grid">
              <article class="br-series-detail-card"><a class="br-series-detail-card-link" href="https://c4u.cam/show-n1/">Show Ep 1</a></article>
            </div></section>
            """.trimIndent(),
            "https://c4u.cam/series/show/",
        )
        val c4uEps = SeriesCatalogParser.parseEpisodeEntries(
            document = c4uDoc,
            currentPageUrl = "https://c4u.cam/series/show/",
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("c4u.cam") },
        )
        assertEquals(listOf(1), c4uEps.map { it.third })

        // 6. AraDrama vc_btn3 series archive URL + episode cards + search series vs movie classification
        val araDramaDetailDoc = Jsoup.parse(
            """
            <article class="r_post introfx post-157704 type-k-drama stat-soon">
              <div class="vc_btn3-container vc_btn3-inline">
                <a class="vc_general vc_btn3" href="https://aradramatv.cc/category/episodes/new/korean-ep/%d8%aa%d8%b5%d8%a7%d8%af%d9%85-2-%d8%b7%d8%b1%d9%8a%d9%82-%d8%a7%d9%84%d8%ba%d8%b6%d8%a8/">مشاهدة حلقات المسلسل</a>
              </div>
            </article>
            """.trimIndent(),
            "https://aradramatv.cc/2026/11/crash-2-fury-road/",
        )
        val araArchiveUrls = SeriesCatalogParser.findSeriesEpisodeArchiveUrls(
            araDramaDetailDoc,
            "https://aradramatv.cc/2026/11/crash-2-fury-road/",
        )
        assertEquals(1, araArchiveUrls.size)
        assertTrue(SeriesCatalogParser.isSpecificSeriesArchiveUrl(araArchiveUrls.first().first))

        val araArchiveUrl = araArchiveUrls.first().first
        val araArchiveDoc = Jsoup.parse(
            """
            <div class="eps">
              <article class="hentry post-157940 post_format-post-format-video">
                <h3 class="post-title entry-title">
                  <a title="تصادم 2 الحلقة 04" href="https://aradramatv.cc/2026/11/ep-04/">تصادم 2 الحلقة 04</a>
                </h3>
              </article>
              <article class="hentry post-157880 post_format-post-format-video">
                <h3 class="post-title entry-title">
                  <a title="تصادم 2 الحلقة 01" href="https://aradramatv.cc/2026/11/ep-01/">تصادم 2 الحلقة 01</a>
                </h3>
              </article>
            </div>
            """.trimIndent(),
            araArchiveUrl,
        )
        val araEps = SeriesCatalogParser.parseEpisodeEntries(
            document = araArchiveDoc,
            currentPageUrl = araArchiveUrl,
            defaultSeasonNumber = 1,
            isAllowedContentUrl = { it.contains("aradramatv.cc") },
        )
        assertEquals(listOf(4, 1), araEps.mapNotNull { SeriesCatalogParser.episodeNumber(it.first, it.second) })

        val searchResultsDoc = Jsoup.parse(
            """
            <article class="post-157704 category-ongoing category-korea stat-soon type-k-drama">
              <div class="item wgr">
                <span class="s_category"><a href="#">دراما كورية</a></span>
                <a class="card-link" href="https://aradramatv.cc/2026/11/crash-2-fury-road/"><h3>تصادم 2: طريق الغضب Crash 2: Fury Road</h3></a>
              </div>
            </article>
            <article class="post-157556 category-3827 type-k-movies">
              <div class="item wgr">
                <span class="s_category"><a href="#">فيلم كوري</a></span>
                <a class="card-link" href="https://aradramatv.cc/2026/11/hope/"><h3>أمل HOPE</h3></a>
              </div>
            </article>
            """.trimIndent(),
            "https://aradramatv.cc/?s=Fury+Road",
        )
        val seriesSearchLink = searchResultsDoc.selectFirst("article.type-k-drama a.card-link")!!
        val movieSearchLink = searchResultsDoc.selectFirst("article.type-k-movies a.card-link")!!
        assertTrue(
            MultiProviderRepository.isSeriesCard(
                seriesSearchLink,
                "تصادم 2: طريق الغضب Crash 2: Fury Road",
                seriesSearchLink.absUrl("href"),
                null,
            ),
        )
        assertFalse(
            MultiProviderRepository.isSeriesCard(
                movieSearchLink,
                "أمل HOPE",
                movieSearchLink.absUrl("href"),
                null,
            ),
        )
    }

    @Test
    fun handlesTruncatedMySeedSlugsDropsEmptySeasonsAndDetectsProviderEmptySearch() {
        val parentUrl = "https://m.myseed.tv/selary/مسلسل-محمد-الفاتح-سلطان-الفتوحات-مترج/"
        val expectedId = "محمد الفاتح سلطان الفتوحات"
        assertEquals(expectedId, SeriesCatalogParser.seriesIdentityFromUrl(parentUrl))
        for (epUrl in listOf(
            "https://m.myseed.tv/مسلسل-محمد-الفاتح-سلطان-الفتوحات-الحل-b5/",
            "https://m.myseed.tv/مسلسل-محمد-الفاتح-سلطان-الفتوحات-الحل-10/",
            "https://m.myseed.tv/مسلسل-محمد-الفاتح-سلطان-الفتوحات-المو-p2/",
            "https://m.myseed.tv/مسلسل-محمد-الفاتح-سلطان-الفتوحات-المو-35/",
            "https://m.myseed.tv/selary/مسلسل-محمد-الفاتح-سلطان-الفتوحات-المو/",
        )) {
            assertEquals(expectedId, SeriesCatalogParser.seriesIdentityFromUrl(epUrl))
            assertTrue(
                "Expected truncated URL $epUrl to belong to series",
                SeriesCatalogParser.episodeBelongsToSeries(
                    targetTitles = listOf("محمد الفاتح سلطان الفتوحات"),
                    targetPageUrls = listOf(parentUrl),
                    episodeTitle = "الحلقة 5",
                    episodeUrl = epUrl,
                ),
            )
        }
        assertEquals(
            "الحل الاخير",
            SeriesCatalogParser.seriesIdentityFromUrl("https://m.myseed.tv/فيلم-الحل-الاخير/"),
        )

        val cleanedBundle = SeriesCatalogParser.normalizeSeasonBundle(
            ArabseedSeasonBundle(
                seasons = listOf(
                    ArabseedSeason(1, "الموسم الأول", emptyList(), "https://m.myseed.tv/selary/show-s1/"),
                    ArabseedSeason(
                        2,
                        "الموسم الثاني",
                        listOf(
                            ArabseedEpisode(
                                number = 29,
                                title = "29",
                                pageUrl = "https://m.myseed.tv/مسلسل-محمد-الفاتح-سلطان-الفتوحات-المو-29/",
                                seasonNumber = 2,
                                providerName = "fixture",
                            ),
                        ),
                        "https://m.myseed.tv/selary/show-s2/",
                    ),
                ),
            ),
        )
        assertEquals(listOf(2), cleanedBundle.seasons.map { it.number })
        assertEquals(listOf(29), cleanedBundle.seasons.single().episodes.map { it.number })

        val emptyCases = listOf(
            "https://wecima.cc/?s=test" to "<body><p>عذراً لا توجد نتائج لـ : test</p></body>",
            "https://qesset.net/?s=test" to "<body><p>لم يتم العثور على أي نتيجة.</p></body>",
            "https://topcinemaa.tv/search/?query=test" to "<body><p>لا توجد نتائج مطابقة للبحث</p></body>",
        )
        for ((url, html) in emptyCases) {
            val doc = Jsoup.parse(html, url)
            assertTrue(MultiProviderRepository.isSearchEmptyResultPage(html, url, url, doc))
        }
        assertEquals(
            "https://egweg1.sbs",
            MultiProviderRepository.PROVIDERS.first { it.id == "egydead" }.baseUrl,
        )
    }

    @Test
    fun matchesBilingualAndTransliteratedSeriesAcrossProvidersWithoutFalsePositives() {
        // 1. Arabic prefix translations alongside exact Latin series titles match across mirrors
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Doctor X: Mafia in White"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/"),
                candidateTitles = listOf("الطبيبة إكس: مافيا باللون الأبيض Doctor X: Mafia in White"),
                candidatePageUrls = listOf("https://aradramatv.cc/2026/10/الطبيبة-إكس-مافيا-باللون-الأبيض-doctor-x-mafia-in-white/"),
            ),
        )
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Doctor X: Mafia in White"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/"),
                candidateTitles = listOf("الاخيرة حلقة 12 اثارة 1080p WEB-DL مسلسل الدكتورة اكس Doctor X: Mafia in White"),
                candidatePageUrls = listOf("https://topcinemaa.tv/مسلسل-الدكتورة-اكس-doctor-x-mafia-in-white-الحلقة-12-والاخيرة-مترجمة/"),
            ),
        )
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Doctor X: Mafia in White"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/"),
                candidateTitles = listOf("الدكتورة إكس (Doctor X: Mafia in White)"),
                candidatePageUrls = listOf("https://www.shahid4u.si/series/doctor-x-mafia-in-white-293610/"),
            ),
        )
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Rivals"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-rivals-s2-eps11/"),
                candidateTitles = listOf("مسلسل Rivals الموسم الثاني الحلقة 11"),
                candidatePageUrls = listOf("https://akw.cam/akdrama-rivals-al-e2-s11/"),
            ),
        )
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Rivals"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-rivals-s2-eps11/"),
                candidateTitles = listOf("مسلسل Rivals مترجم"),
                candidatePageUrls = listOf("https://faselhd.online/مسلسل-rivals-1132/"),
            ),
        )
        // Must still reject related-but-different Latin works
        assertFalse(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("Doctor X: Mafia in White"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/"),
                candidateTitles = listOf("دكتور اكس الموسم الخامس Doctor-X: Surgeon Michiko Daimon"),
                candidatePageUrls = listOf("https://www.shahid4u.si/series/doctor-x-surgeon-michiko-daimon-46052/"),
            ),
        )

        // 2. Transliterated/Turkish URL slugs match when the Arabic work title matches
        assertTrue(
            SeriesCatalogParser.pageBelongsToSeries(
                targetTitles = listOf("مسلسل هذا البحر سوف يفيض"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-هذا-البحر-سوف-يفيض-s2-eps9/"),
                candidateTitles = listOf("مسلسل هذا البحر سوف يفيض الحلقة 40 مترجمة"),
                candidatePageUrls = listOf("https://egweg1.sbs/episode/tasacak-bu-deniz-e40/"),
            ),
        )
        assertTrue(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf("مسلسل هذا البحر سوف يفيض"),
                targetPageUrls = listOf("https://m.myseed.tv/مسلسل-هذا-البحر-سوف-يفيض-s2-eps9/"),
                episodeTitle = "حلقة 40 هذا البحر سوف يفيض الحلقة 40",
                episodeUrl = "https://qesset.net/clarus/tasak-bu-deniz-episode-40/",
            ),
        )
        assertTrue(
            SeriesCatalogParser.episodeBelongsToSeries(
                targetTitles = listOf("الطبيبة إكس: مافيا باللون الأبيض Doctor X: Mafia in White"),
                targetPageUrls = listOf("https://aradramatv.cc/2026/10/الطبيبة-إكس-مافيا-باللون-الأبيض-doctor-x-mafia-in-white/"),
                episodeTitle = "الطبيبة إكس: مافيا باللون الأبيض الحلقة 12 الأخيرة",
                episodeUrl = "https://aradramatv.cc/2026/11/الطبيبة-إكس-الحلقة-12/",
            ),
        )

        // 3. Reconciles unambiguous single-season mirrors without mixing multi-season shows
        val target = seriesItem("Doctor X: Mafia in White", "https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/")
        val sparsePrimary = ArabseedSeasonBundle(
            listOf(
                season(1, "https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/",
                    episode(1, 1, "https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps1/"),
                    episode(2, 1, "https://m.myseed.tv/مسلسل-doctor-x-mafia-in-white-s1-eps2/"),
                ),
            ),
        )
        val araDramaMirror = seriesItem(
            "الطبيبة إكس: مافيا باللون الأبيض Doctor X: Mafia in White",
            "https://aradramatv.cc/2026/10/الطبيبة-إكس-مافيا-باللون-الأبيض-doctor-x-mafia-in-white/",
        )
        val araDramaBundle = ArabseedSeasonBundle(
            listOf(
                season(1, "",
                    episode(1, 1, "https://aradramatv.cc/2026/10/الطبيبة-إكس-الحلقة-01/"),
                    episode(2, 1, "https://aradramatv.cc/2026/10/الطبيبة-إكس-الحلقة-02/"),
                    episode(3, 1, "https://aradramatv.cc/2026/10/الطبيبة-إكس-الحلقة-03/"),
                    episode(12, 1, "https://aradramatv.cc/2026/11/الطبيبة-إكس-الحلقة-12/"),
                ),
            ),
        )
        val reconciled = MultiProviderRepository.reconcileMirrorSeasonBundle(target, sparsePrimary, araDramaMirror, araDramaBundle)
        assertEquals(listOf(1), reconciled.seasons.map { it.number })
        assertEquals(listOf(1, 2, 3, 12), reconciled.seasons.single().episodes.map { it.number })
    }

    private fun episode(number: Int, season: Int, url: String) = ArabseedEpisode(
        number = number,
        title = "الحلقة $number",
        pageUrl = url,
        seasonNumber = season,
        providerName = "fixture",
    )

    private fun season(number: Int, url: String, vararg episodes: ArabseedEpisode) = ArabseedSeason(
        number = number,
        title = SeriesCatalogParser.seasonTitle(number),
        episodes = episodes.toList(),
        pageUrl = url,
    )

    private fun seriesItem(title: String, url: String) = ArabseedItem(
        id = "test_${title.hashCode()}",
        title = title,
        arabicTitle = "",
        posterUrl = null,
        year = "",
        quality = "HD",
        rating = "",
        type = ArabseedType.SERIES,
        pageUrl = url,
        category = "series",
        providerName = "test",
    )
}
