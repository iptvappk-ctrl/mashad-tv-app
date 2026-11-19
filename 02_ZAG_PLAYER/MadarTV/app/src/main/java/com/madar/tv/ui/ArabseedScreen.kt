package com.madar.tv.ui



import androidx.activity.compose.BackHandler

import com.madar.tv.util.PlayerLauncher
import com.madar.tv.util.PlayerSource

import android.annotation.SuppressLint



import android.app.DownloadManager

import android.os.Environment

import android.widget.Toast

import android.content.Context

import android.content.Intent

import android.net.Uri



import android.view.ViewGroup



import android.webkit.WebChromeClient



import android.webkit.WebSettings



import android.webkit.WebView



import android.webkit.WebViewClient



import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState

import androidx.compose.animation.animateColorAsState

import androidx.compose.animation.core.animateDpAsState



import androidx.compose.animation.fadeIn



import androidx.compose.animation.fadeOut



import androidx.compose.foundation.Image



import androidx.compose.foundation.background



import androidx.compose.foundation.border



import androidx.compose.foundation.clickable



import androidx.compose.foundation.horizontalScroll



import androidx.compose.foundation.layout.*



import androidx.compose.foundation.lazy.LazyColumn



import androidx.compose.foundation.lazy.LazyRow



import androidx.compose.foundation.lazy.grid.GridCells



import androidx.compose.foundation.lazy.grid.LazyVerticalGrid



import androidx.compose.foundation.lazy.grid.items



import androidx.compose.foundation.lazy.items



import androidx.compose.foundation.rememberScrollState



import androidx.compose.foundation.shape.CircleShape



import androidx.compose.foundation.shape.RoundedCornerShape



import androidx.compose.material3.CircularProgressIndicator



import androidx.compose.material3.Text



import androidx.compose.runtime.*
import androidx.compose.runtime.key



import androidx.compose.runtime.saveable.rememberSaveable



import androidx.compose.ui.Alignment



import androidx.compose.ui.Modifier



import androidx.compose.ui.draw.clip



import androidx.compose.ui.graphics.Brush



import androidx.compose.ui.graphics.Color



import androidx.compose.ui.layout.ContentScale



import androidx.compose.ui.platform.LocalContext



import androidx.compose.ui.res.painterResource



import androidx.compose.ui.text.font.FontWeight



import androidx.compose.ui.text.style.TextAlign



import androidx.compose.ui.text.style.TextOverflow



import androidx.compose.ui.unit.dp



import androidx.compose.ui.unit.sp



import androidx.compose.ui.viewinterop.AndroidView



import androidx.lifecycle.Lifecycle



import androidx.lifecycle.LifecycleEventObserver



import androidx.lifecycle.LifecycleOwner



import androidx.media3.common.MediaItem



import androidx.media3.common.Player



import androidx.media3.common.PlaybackException



import androidx.media3.common.util.UnstableApi



import androidx.media3.datasource.DefaultHttpDataSource



import androidx.media3.exoplayer.DefaultLoadControl



import androidx.media3.exoplayer.ExoPlayer

import androidx.media3.exoplayer.trackselection.DefaultTrackSelector



import androidx.media3.exoplayer.hls.HlsMediaSource



import androidx.media3.exoplayer.source.DefaultMediaSourceFactory



import androidx.media3.ui.PlayerView



import coil3.compose.AsyncImage



import com.madar.tv.R



import com.madar.tv.data.*



import com.madar.tv.ui.theme.ZakzoukaColors



import kotlinx.coroutines.Dispatchers



import kotlinx.coroutines.async



import kotlinx.coroutines.delay



import kotlinx.coroutines.launch



import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.channels.Channel







private fun adjacentEpisodeIndex(

    episodes: List<ArabseedEpisode>,

    currentIndex: Int,

    direction: Int,

): Int? {

    if (direction != -1 && direction != 1) return null

    val current = episodes.getOrNull(currentIndex) ?: return null

    // Navigate by exact number, never by the provider's link order.
    val expectedNumber = current.number + direction
    if (expectedNumber < 1) return null
    return episodes.indexOfFirst { candidate ->
        candidate.seasonNumber == current.seasonNumber && candidate.number == expectedNumber
    }.takeIf { it >= 0 }

}





@Composable



fun CinemaSection(



    onSearch: () -> Unit,



    onDeepScreen: (Boolean) -> Unit = {},



    pendingItem: ArabseedItem? = null,



    onPendingItemConsumed: () -> Unit = {},



) {



    var selectedMainTab by rememberSaveable { mutableStateOf("home") }



    var selectedCategoryPath by rememberSaveable { mutableStateOf("main/") }



    var selectedCategoryTitle by rememberSaveable { mutableStateOf("الرئيسية") }







    // Navigation stack: allows Back to return to previous episode/part detail



    var itemStack by rememberSaveable { mutableStateOf(emptyList<String>()) }



    // ArabseedDetail contains nested non-saveable model objects; keep navigation
    // state in memory instead of asking rememberSaveable to serialize the graph.
    var openedDetail by remember { mutableStateOf<ArabseedDetail?>(null) }



    var searchMode by rememberSaveable { mutableStateOf(false) }



    var searchQuery by rememberSaveable { mutableStateOf("") }







    var allItems by remember { mutableStateOf(emptyList<ArabseedItem>()) }



    var activeEpisodes by remember { mutableStateOf<List<ArabseedEpisode>>(emptyList()) }



    var currentEpisodeIndex by remember { mutableStateOf(-1) }



    val context = LocalContext.current

    val scope = rememberCoroutineScope()







    val currentItem = remember(itemStack, allItems) {



        allItems.firstOrNull { it.pageUrl == itemStack.lastOrNull() }



    }



    val detail = openedDetail



    // Robust Back Navigation: handles detail, stack, search, and player back presses

    BackHandler(enabled = openedDetail != null || itemStack.isNotEmpty() || searchMode || searchQuery.isNotBlank()) {

        when {

            openedDetail != null -> {

                openedDetail = null

            }

            itemStack.isNotEmpty() -> {

                itemStack = itemStack.dropLast(1)

            }

            searchMode || searchQuery.isNotBlank() -> {

                searchMode = false

                searchQuery = ""

            }

        }

    }



    val isDeep = detail != null || itemStack.isNotEmpty()



    LaunchedEffect(isDeep) { onDeepScreen(isDeep) }







    LaunchedEffect(pendingItem) {



        if (pendingItem != null) {



            allItems = (allItems + pendingItem).distinctBy { it.pageUrl }



            itemStack = listOf(pendingItem.pageUrl)



            openedDetail = null



            onPendingItemConsumed()



        }



    }







    // ── Single-Active Screen Navigation via AnimatedContent ──
    val navKey = when {
        detail != null -> "player"
        currentItem != null -> "detail:${currentItem.pageUrl}"
        searchMode -> "search"
        else -> "home"
    }

    key(navKey) {
        when {
            navKey == "player" && detail != null -> {
                ZakzoukaPlayerScreen(
                    detail = detail,
                    onBack = { openedDetail = null },
                    hasPrev = adjacentEpisodeIndex(activeEpisodes, currentEpisodeIndex, -1) != null,
                    hasNext = adjacentEpisodeIndex(activeEpisodes, currentEpisodeIndex, 1) != null,
                    onPrev = {
                        val prevIdx = adjacentEpisodeIndex(activeEpisodes, currentEpisodeIndex, -1)
                        if (prevIdx != null) {
                            val prevEp = activeEpisodes[prevIdx]
                            currentEpisodeIndex = prevIdx
                            scope.launch {
                                val (srvs, preRes) = withContext(Dispatchers.IO) {
                                    val s = if (detail.item.providerName == "عرب سيد") {
                                        ArabseedRepository.fetchServersFast(prevEp.pageUrl)
                                    } else {
                                        MultiProviderRepository.fetchServers(prevEp.pageUrl)
                                    }
                                    val pr = s.firstOrNull()?.directStreamUrl
                                        ?: s.firstOrNull()?.let { ArabseedRepository.resolveDirectStream(it.embedUrl) }
                                    s to pr
                                }
                                val newItem = detail.item.copy(
                                    id = prevEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                    title = prevEp.title,
                                    arabicTitle = prevEp.title,
                                    pageUrl = prevEp.pageUrl,
                                    latestEpisodeLabel = "الموسم ${prevEp.seasonNumber} · الحلقة ${prevEp.number}",
                                )
                                openedDetail = ArabseedDetail(newItem, srvs, preRes)
                            }
                        }
                    },
                    onNext = {
                        val nextIdx = adjacentEpisodeIndex(activeEpisodes, currentEpisodeIndex, 1)
                        if (nextIdx != null) {
                            val nextEp = activeEpisodes[nextIdx]
                            currentEpisodeIndex = nextIdx
                            scope.launch {
                                val (srvs, preRes) = withContext(Dispatchers.IO) {
                                    val s = if (detail.item.providerName == "عرب سيد") {
                                        ArabseedRepository.fetchServersFast(nextEp.pageUrl)
                                    } else {
                                        MultiProviderRepository.fetchServers(nextEp.pageUrl)
                                    }
                                    val pr = s.firstOrNull()?.directStreamUrl
                                        ?: s.firstOrNull()?.let { ArabseedRepository.resolveDirectStream(it.embedUrl) }
                                    s to pr
                                }
                                val newItem = detail.item.copy(
                                    id = nextEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                    title = nextEp.title,
                                    arabicTitle = nextEp.title,
                                    pageUrl = nextEp.pageUrl,
                                    latestEpisodeLabel = "الموسم ${nextEp.seasonNumber} · الحلقة ${nextEp.number}",
                                )
                                openedDetail = ArabseedDetail(newItem, srvs, preRes)
                            }
                        }
                    }
                )
            }
            navKey.startsWith("detail:") && currentItem != null -> {
                ZakzoukaDetailScreen(
                    item = currentItem,
                    onBack = {
                        itemStack = itemStack.dropLast(1)
                    },
                    onPlay = { d, eps, epIdx ->
                        val playTitle = d.item.arabicTitle.ifBlank { d.item.title }
                        scope.launch {
                            val sourceReferer = runCatching {
                                val uri = java.net.URI.create(d.item.pageUrl)
                                "${uri.scheme}://${uri.host}/"
                            }.getOrDefault("")
                            val isArabseed = d.item.providerName == "عرب سيد"
                                || d.item.pageUrl.contains("myseed.tv", ignoreCase = true)
                            // The old helper returns govid.live as a fixed Referer. Sending
                            // that for every MySeed/Akwam/etc. stream makes hotlink checks fail.
                            // Use the catalog page as the page Referer and specialize only when
                            // the actual embed belongs to the configured GOVID host.
                            val pageReferer = sourceReferer
                            val srvs = d.servers.ifEmpty {
                                withContext(Dispatchers.IO) {
                                    if (isArabseed) ArabseedRepository.fetchServersFast(d.item.pageUrl)
                                    else MultiProviderRepository.fetchServers(d.item.pageUrl)
                                }
                            }
                            val server = srvs.firstOrNull()
                            if (server == null && d.preResolvedStream.isNullOrBlank()) {
                                Toast.makeText(context, "لا يوجد سيرفر متاح حالياً", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // Open the companion player immediately. It resolves embed pages there,
                            // so a slow source never holds the app's detail screen open.
                            val streamUrl = d.preResolvedStream
                                ?: server?.directStreamUrl
                                ?: server?.embedUrl
                            if (!streamUrl.isNullOrBlank()) {
                                val backupSources = srvs.drop(if (server == null) 0 else 1)
                                    .mapNotNull { backup ->
                                        (backup.directStreamUrl ?: backup.embedUrl.takeIf(String::isNotBlank))?.let { url ->
                                            val embedReferer = playerRefererForEmbed(backup.embedUrl, pageReferer)
                                            val sourceReferer = if (backup.directStreamUrl != null) backup.embedUrl else embedReferer
                                            PlayerSource(url, ArabseedRepository.getPlayerUserAgent(), sourceReferer)
                                        }
                                    }
                                val launched = PlayerLauncher.launchPlayer(
                                    context = context,
                                    streamUrl = streamUrl,
                                    title = playTitle,
                                    userAgent = ArabseedRepository.getPlayerUserAgent(),
                                    referer = if (server?.directStreamUrl != null || d.preResolvedStream != null) {
                                        server?.embedUrl.orEmpty().ifBlank { pageReferer }
                                    } else server?.let { playerRefererForEmbed(it.embedUrl, pageReferer) } ?: pageReferer,
                                    backupSources = backupSources,
                                    fallbackUrl = server?.embedUrl?.takeIf { it != streamUrl }.orEmpty(),
                                    fallbackReferer = pageReferer,
                                )
                                if (!launched) {
                                    openedDetail = d
                                    activeEpisodes = eps
                                    currentEpisodeIndex = epIdx
                                }
                            } else {
                                Toast.makeText(context, "تعذر استخراج رابط البث من السيرفر", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onOpenItem = { newItem ->
                        val normalizedNewUrl = SeriesCatalogParser.normalizedUrl(newItem.pageUrl)
                        allItems = allItems.filterNot {
                            SeriesCatalogParser.normalizedUrl(it.pageUrl) == normalizedNewUrl
                        } + newItem
                        itemStack = itemStack + newItem.pageUrl
                    },
                )
            }
            navKey == "search" -> {
                ZakzoukaSearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onBack = { searchMode = false; searchQuery = "" },
                    onOpen = { item ->
                        com.madar.tv.core.ZakzoukaTelemetry.trackContentView(
                            title = item.title,
                            type = item.type.name,
                            category = item.category,
                            url = item.pageUrl
                        )
                        allItems = (allItems + item).distinctBy { it.pageUrl }
                        itemStack = listOf(item.pageUrl)
                        searchMode = false
                    },
                )
            }
            else -> {
                ZakzoukaMainScreen(
                    selectedTab = selectedMainTab,
                    currentCategoryPath = selectedCategoryPath,
                    currentCategoryTitle = selectedCategoryTitle,
                    onTabSelect = { tab ->
                        selectedMainTab = tab.id
                        val subList = ArabseedCategories.getSubCategories(tab.id)
                        val first = subList.firstOrNull()
                        selectedCategoryPath = first?.path ?: tab.path
                        selectedCategoryTitle = first?.title ?: tab.title
                    },
                    onCategorySelect = { cat ->
                        selectedCategoryPath = cat.path
                        selectedCategoryTitle = cat.title
                    },
                    onSearch = { searchMode = true },
                    onOpen = { item ->
                        com.madar.tv.core.ZakzoukaTelemetry.trackContentView(
                            title = item.title,
                            type = item.type.name,
                            category = item.category,
                            url = item.pageUrl
                        )
                        allItems = (allItems + item).distinctBy { it.pageUrl }
                        itemStack = listOf(item.pageUrl)
                    },
                )
            }
        }
    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Main Zakzouka Screen (Movies, Series, Football Matches, Auto-Updates)



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaMainScreen(



    selectedTab: String,



    currentCategoryPath: String,



    currentCategoryTitle: String,



    onTabSelect: (ArabCategory) -> Unit,



    onCategorySelect: (ArabCategory) -> Unit,



    onSearch: () -> Unit,



    onOpen: (ArabseedItem) -> Unit,



) {



    var page by remember(currentCategoryPath) { mutableStateOf(1) }



    var items by remember(currentCategoryPath, selectedTab) { mutableStateOf(emptyList<ArabseedItem>()) }



    var loading by remember(currentCategoryPath, selectedTab) { mutableStateOf(true) }

    var loadingMore by remember(currentCategoryPath) { mutableStateOf(false) }



    var error by remember(currentCategoryPath) { mutableStateOf<String?>(null) }



    var reloadKey by remember { mutableStateOf(0) }
    var handledReloadKey by remember { mutableStateOf(0) }







    // Refresh the visible catalog occasionally; disk cache remains the first paint.
    // Auto-updating ticker: re-fetch in the background every 20 minutes
    LaunchedEffect(currentCategoryPath, selectedTab) {
        while (true) {
            delay(20 * 60 * 1000L)
            try {
                val liveLatest = async(Dispatchers.IO) {
                    ArabseedRepository.fetchCategory(currentCategoryPath, 1, forceRefresh = reloadKey > 0)
                }
                val providerLatest = async(Dispatchers.IO) {
                    when (selectedTab) {
                        "movies" -> MultiProviderRepository.fetchMoviesAll(1, forceRefresh = true)
                        "series" -> MultiProviderRepository.fetchSeriesAll(1, forceRefresh = true)
                        "home" -> MultiProviderRepository.fetchCatalogAll(1, forceRefresh = true)
                        else -> emptyList()
                    }
                }
                val latestSourceItems = liveLatest.await()
                val latestProviderItems = providerLatest.await()

                if (latestSourceItems.isNotEmpty() || latestProviderItems.isNotEmpty()) {
                    items = withContext(Dispatchers.Default) {
                        MultiProviderRepository.mergeCatalogItems(
                            latestProviderItems + latestSourceItems + items,
                            expectedType = when (selectedTab) {
                                "movies" -> ArabseedType.MOVIE
                                "series" -> ArabseedType.SERIES
                                else -> null
                            },
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Paint stale disk data first, then refresh the providers concurrently.
    // The screen leaves its full-page loading state after 750 ms even on a cold cache;
    // source requests keep running and update the list as each provider finishes.
    LaunchedEffect(currentCategoryPath, selectedTab, reloadKey) {
        val expectedType = when (selectedTab) {
            "movies" -> ArabseedType.MOVIE
            "series" -> ArabseedType.SERIES
            else -> null
        }
        try {
            error = null
            loadingMore = false
            val forceRefresh = reloadKey != handledReloadKey
            handledReloadKey = reloadKey

            val diskSnapshot = withContext(Dispatchers.IO) {
                val primary = ArabseedRepository.loadCachedCategory(currentCategoryPath, 1).orEmpty()
                val providers = MultiProviderRepository.loadCachedCatalog(expectedType, 1).orEmpty()
                primary + providers
            }
            if (diskSnapshot.isNotEmpty()) {
                items = withContext(Dispatchers.Default) {
                    MultiProviderRepository.mergeCatalogItems(diskSnapshot, expectedType)
                }
                loading = false
            }

            val incoming = Channel<List<ArabseedItem>>(capacity = 2)
            val timeoutHint = launch {
                delay(750L)
                if (items.isEmpty()) loading = false
            }
            launch(Dispatchers.IO) {
                val result = withTimeoutOrNull(7_000L) {
                    runCatching {
                        ArabseedRepository.fetchCategory(
                            if (currentCategoryPath.isBlank() || currentCategoryPath.trimEnd('/') == "home") "main/" else currentCategoryPath,
                            1,
                            forceRefresh = forceRefresh,
                        )
                    }.getOrElse { emptyList() }
                }.orEmpty()
                incoming.send(result)
            }
            launch(Dispatchers.IO) {
                val result = withTimeoutOrNull(12_000L) {
                    runCatching {
                        when (selectedTab) {
                            "movies" -> MultiProviderRepository.fetchMoviesAll(1, forceRefresh = forceRefresh)
                            "series" -> MultiProviderRepository.fetchSeriesAll(1, forceRefresh = forceRefresh)
                            "home" -> MultiProviderRepository.fetchCatalogAll(1, forceRefresh = forceRefresh)
                            else -> emptyList()
                        }
                    }.getOrElse { emptyList() }
                }.orEmpty()
                incoming.send(result)
            }

            repeat(2) {
                val fresh = incoming.receive()
                if (fresh.isNotEmpty()) {
                    val merged = withContext(Dispatchers.Default) {
                        MultiProviderRepository.mergeCatalogItems(fresh + items, expectedType)
                    }
                    if (merged.isNotEmpty()) {
                        items = merged
                        error = null
                        loading = false
                    }
                }
            }
            timeoutHint.cancel()
            if (items.isEmpty()) error = "تعذر تحميل المحتوى. اسحب للأسفل لإعادة المحاولة."
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (items.isEmpty()) error = "حدث خطأ في الاتصال. يرجى إعادة المحاولة."
        } finally {
            loading = false
        }
    }
    // Pagination: load next page smoothly in parallel without locking the thread
    LaunchedEffect(currentCategoryPath, selectedTab, page) {
        if (page <= 1) return@LaunchedEffect
        if (loadingMore) return@LaunchedEffect
        loadingMore = true
        try {
            val expectedType = when (selectedTab) {
                "movies" -> ArabseedType.MOVIE
                "series" -> ArabseedType.SERIES
                else -> null
            }
            val cached = withContext(Dispatchers.IO) {
                ArabseedRepository.loadCachedCategory(currentCategoryPath, page).orEmpty() +
                    MultiProviderRepository.loadCachedCatalog(expectedType, page).orEmpty()
            }
            if (cached.isNotEmpty()) {
                val previousItems = items
                items = withContext(Dispatchers.Default) {
                    MultiProviderRepository.mergeCatalogItems(previousItems + cached, expectedType)
                }
            }

            val (sourceItems, providerItems) = withContext(Dispatchers.IO) {
                val sourceDeferred = async {
                    runCatching {
                        ArabseedRepository.fetchCategory(currentCategoryPath, page, forceRefresh = false)
                    }.getOrDefault(emptyList())
                }
                val providerDeferred = async {
                    runCatching {
                        when (selectedTab) {
                            "movies" -> MultiProviderRepository.fetchMoviesAll(page, forceRefresh = false)
                            "series" -> MultiProviderRepository.fetchSeriesAll(page, forceRefresh = false)
                            "home" -> MultiProviderRepository.fetchCatalogAll(page, forceRefresh = false)
                            else -> emptyList()
                        }
                    }.getOrDefault(emptyList())
                }
                sourceDeferred.await() to providerDeferred.await()
            }

            val available = sourceItems + providerItems
            if (available.isNotEmpty()) {
                val previousItems = items
                items = withContext(Dispatchers.Default) {
                    MultiProviderRepository.mergeCatalogItems(previousItems + available, expectedType)
                }
            }
        } finally {
            loadingMore = false
        }
    }

    val featuredItem = remember(items) { items.firstOrNull() }







    // Prefetch top items in background for instant 0.0s opening



    LaunchedEffect(items) {



        if (items.isNotEmpty()) {



            withContext(Dispatchers.IO) {



            items.take(2).forEach { item ->



                    try { ArabseedRepository.prefetch(item) } catch (_: Exception) {}



                }



            }



        }



    }







    Column(Modifier.fillMaxSize()) {



        // 1. Zakzouka Top Header with Logo & Auto-update badge & Search & Refresh



        ZakzoukaHeader(



            onSearch = onSearch,



            onRefresh = {



                ArabseedRepository.clearListingCache()

                MultiProviderRepository.clearCatalogCache()



                page = 1



                reloadKey++



            },



        )







        // 2. Main Tabs (الرئيسية, المسلسلات, الافلام, انمي, رمضان, منوعات)



        ZakzoukaMainTabs(



            selectedTabId = selectedTab,



            onSelect = onTabSelect,



        )







        // 3. Sub-categories chips



        ZakzoukaSubCategoryBar(



            selectedTabId = selectedTab,



            currentPath = currentCategoryPath,



            onSelect = onCategorySelect,



        )







        // 4. Content Area



        when {



            loading && items.isEmpty() -> {

                ZakzoukaCatalogSkeleton(Modifier.fillMaxSize())



            }







            !loading && items.isEmpty() && error == null -> {

                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "بنحمّل أحدث المحتوى في الخلفية…",
                        color = ZakzoukaColors.TextMuted,
                        fontSize = 13.sp,
                    )
                }

            }

            error != null && items.isEmpty() -> {



                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {



                        Text(error!!, color = ZakzoukaColors.TextMuted, fontSize = 13.sp)



                        Text(



                            "إعادة المحاولة",



                            Modifier.clip(RoundedCornerShape(8.dp))



                                .background(ZakzoukaColors.Gold)



                                .clickable { page = 1; reloadKey++ }



                                .padding(horizontal = 20.dp, vertical = 10.dp),



                            color = Color.White,



                            fontWeight = FontWeight.Bold,



                        )



                    }



                }



            }







            else -> {



                val chunkedItems = remember(items) { items.chunked(3) }

                LazyColumn(



                    Modifier.fillMaxSize(),



                    contentPadding = PaddingValues(bottom = 32.dp),



                    verticalArrangement = Arrangement.spacedBy(18.dp),



                ) {



                    // Hero Featured Item (Luxury style)



                    if (featuredItem != null && page == 1 && (selectedTab == "home" || currentCategoryPath == "home/")) {



                        item {



                            ZakzoukaHeroBanner(



                                item = featuredItem,



                                onPlay = { onOpen(featuredItem) },



                                onInfo = { onOpen(featuredItem) },



                            )



                        }



                    }







                    // Category Title & Live auto-update badge



                    item {



                        PaddingBox {



                            Row(



                                Modifier.fillMaxWidth(),



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.SpaceBetween,



                            ) {



                                Row(verticalAlignment = Alignment.CenterVertically) {



                                    Box(



                                        Modifier.size(4.dp, 16.dp)



                                            .clip(RoundedCornerShape(2.dp))



                                            .background(ZakzoukaColors.Gold)



                                    )



                                    Spacer(Modifier.width(8.dp))



                                    Text(



                                        text = currentCategoryTitle,



                                        color = ZakzoukaColors.TextWhite,



                                        fontSize = 17.sp,



                                        fontWeight = FontWeight.Bold,



                                    )



                                }



                                Box(



                                    Modifier.clip(RoundedCornerShape(12.dp))



                                        .background(ZakzoukaColors.GoldBorder)



                                        .padding(horizontal = 8.dp, vertical = 3.dp)



                                ) {



                                    Text(



                                        text = "مُحدث تلقائياً ⚡",



                                        color = ZakzoukaColors.GoldLight,



                                        fontSize = 10.sp,



                                        fontWeight = FontWeight.Bold,



                                    )



                                }



                            }



                        }



                    }







                    // Grid of Items



                    items(chunkedItems, key = { rowItems -> rowItems.mapNotNull { it.pageUrl }.joinToString(",").ifBlank { rowItems.hashCode().toString() } }) { rowItems ->



                        PaddingBox {



                            Row(



                                Modifier.fillMaxWidth(),



                                horizontalArrangement = Arrangement.spacedBy(10.dp),



                            ) {



                                for (item in rowItems) {



                                    Box(Modifier.weight(1f)) {



                                        ZakzoukaCard(item = item, onClick = { onOpen(item) })



                                    }



                                }



                                repeat(3 - rowItems.size) {



                                    Spacer(Modifier.weight(1f))



                                }



                            }



                        }



                    }







                    // Pagination / Load More button



                    item {



                        PaddingBox {



                            Box(



                                Modifier.fillMaxWidth()



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(ZakzoukaColors.SurfaceCard)



                                    .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp))



                                    .clickable { if (!loadingMore) page++ }



                                    .padding(vertical = 14.dp),



                                contentAlignment = Alignment.Center,



                            ) {



                                if (loadingMore) {



                                    CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)



                                } else {



                                    Text(



                                        "تحميل المزيد من الأعمال ↓",



                                        color = ZakzoukaColors.GoldLight,



                                        fontSize = 13.sp,



                                        fontWeight = FontWeight.Bold,



                                    )



                                }



                            }



                        }



                    }



                }



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Zakzouka Header



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaHeader(



    onSearch: () -> Unit,



    onRefresh: () -> Unit,



) {



    Row(



        Modifier



            .fillMaxWidth()



            .statusBarsPadding()



            .padding(horizontal = 14.dp, vertical = 10.dp),



        verticalAlignment = Alignment.CenterVertically,



        horizontalArrangement = Arrangement.SpaceBetween,



    ) {



        // App Logo & Brand Title



        Row(



            verticalAlignment = Alignment.CenterVertically,



            horizontalArrangement = Arrangement.spacedBy(10.dp)



        ) {



            Image(



                painter = painterResource(id = R.drawable.zakzouka_logo),



                contentDescription = "زقزوقة",



                modifier = Modifier



                    .size(36.dp)



                    .clip(CircleShape)



                    .border(1.5.dp, ZakzoukaColors.Gold, CircleShape),



                contentScale = ContentScale.Crop



            )







            Text(



                text = "زَقْـزُوقَـة",



                color = ZakzoukaColors.GoldLight,



                fontSize = 21.sp,



                fontWeight = FontWeight.ExtraBold,



            )



        }







        // Actions: Refresh + Search



        Row(



            verticalAlignment = Alignment.CenterVertically,



            horizontalArrangement = Arrangement.spacedBy(8.dp)



        ) {



            // Live Refresh button



            Box(



                Modifier



                    .size(36.dp)



                    .clip(CircleShape)



                    .background(ZakzoukaColors.SurfaceCard)



                    .border(1.dp, ZakzoukaColors.GoldBorder, CircleShape)



                    .clickable(onClick = onRefresh),



                contentAlignment = Alignment.Center,



            ) {



                Text("🔄", fontSize = 14.sp)



            }







            // Search button



            Box(



                Modifier



                    .size(36.dp)



                    .clip(CircleShape)



                    .background(ZakzoukaColors.SurfaceCard)



                    .border(1.dp, ZakzoukaColors.GoldBorder, CircleShape)



                    .clickable(onClick = onSearch),



                contentAlignment = Alignment.Center,



            ) {



                Text("🔍", fontSize = 14.sp)



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Main Tabs



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaMainTabs(



    selectedTabId: String,



    onSelect: (ArabCategory) -> Unit,



) {



    LazyRow(



        Modifier.fillMaxWidth(),



        contentPadding = PaddingValues(horizontal = 14.dp),



        horizontalArrangement = Arrangement.spacedBy(14.dp),



    ) {



        items(ArabseedCategories.MAIN_TABS) { tab ->



            val active = tab.id == selectedTabId

            val tabColor by animateColorAsState(

                targetValue = if (active) ZakzoukaColors.GoldLight else ZakzoukaColors.TextMuted,

                label = "cinema-tab-color-${tab.id}",

            )

            val underlineWidth by animateDpAsState(

                targetValue = if (active) 28.dp else 0.dp,

                label = "cinema-tab-underline-${tab.id}",

            )



            Column(



                Modifier.clickable { onSelect(tab) },



                horizontalAlignment = Alignment.CenterHorizontally,



            ) {



                Text(



                    text = tab.title,



                    color = tabColor,



                    fontSize = 14.sp,



                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,



                    modifier = Modifier.padding(vertical = 6.dp),



                )



                Box(

                    Modifier.width(28.dp).height(2.5.dp),

                    contentAlignment = Alignment.Center,

                ) {

                    Box(

                        Modifier.width(underlineWidth).height(2.5.dp)

                            .clip(RoundedCornerShape(2.dp))

                            .background(ZakzoukaColors.Gold),

                    )

                }



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Sub-Category Chips Bar



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaSubCategoryBar(



    selectedTabId: String,



    currentPath: String,



    onSelect: (ArabCategory) -> Unit,



) {



    val subCategories = remember(selectedTabId) {



        ArabseedCategories.getSubCategories(selectedTabId)



    }



    LazyRow(



        Modifier.fillMaxWidth().padding(top = 4.dp),



        contentPadding = PaddingValues(horizontal = 14.dp),



        horizontalArrangement = Arrangement.spacedBy(8.dp),



    ) {



        items(subCategories) { cat ->



            val active = cat.path == currentPath || (cat.path == "home/" && currentPath.isBlank())

            val chipBackground by animateColorAsState(

                targetValue = if (active) ZakzoukaColors.Gold else ZakzoukaColors.SurfaceCard,

                label = "subcategory-background-${cat.id}",

            )

            val chipText by animateColorAsState(

                targetValue = if (active) Color.White else ZakzoukaColors.TextMuted,

                label = "subcategory-text-${cat.id}",

            )



            Box(



                Modifier.clip(RoundedCornerShape(20.dp))



                    .background(chipBackground)



                    .border(1.dp, if (active) ZakzoukaColors.Gold else ZakzoukaColors.Border, RoundedCornerShape(20.dp))



                .clickable { onSelect(cat) }



                .padding(horizontal = 14.dp, vertical = 7.dp),



            ) {



                Text(



                    text = cat.title,



                    color = chipText,



                    fontSize = 12.sp,



                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,



                )



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Zakzouka Hero Featured Banner



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaHeroBanner(



    item: ArabseedItem,



    onPlay: () -> Unit,



    onInfo: () -> Unit,



) {



    Box(



        Modifier.fillMaxWidth().height(330.dp)



            .padding(horizontal = 14.dp)



            .clip(RoundedCornerShape(16.dp))



            .background(ZakzoukaColors.Surface),



    ) {



        if (item.posterUrl != null) {



            AsyncImage(



                model = item.posterUrl,



                contentDescription = item.title,



                modifier = Modifier.fillMaxSize(),



                contentScale = ContentScale.Crop,



            )



        }







        // Warm light fade keeps hero text readable while matching the light palette.



        Box(



            Modifier.fillMaxSize().background(



                Brush.verticalGradient(



                    listOf(



                        Color.Transparent,



                        ZakzoukaColors.Background.copy(alpha = 0.42f),



                        ZakzoukaColors.Background.copy(alpha = 0.94f),



                        ZakzoukaColors.Background,



                    )



                )



            )



        )







        // Text & Buttons overlay



        Column(



            Modifier.align(Alignment.BottomStart).padding(16.dp),



            verticalArrangement = Arrangement.spacedBy(8.dp),



        ) {



            // Quality & Category badge



            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {



                if (item.quality.isNotBlank()) {



                    Text(



                        item.quality,



                        Modifier.clip(RoundedCornerShape(4.dp)).background(ZakzoukaColors.Gold).padding(horizontal = 6.dp, vertical = 2.dp),



                        color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,



                    )



                }



                Text(



                    item.category,



                    Modifier.clip(RoundedCornerShape(4.dp)).background(ZakzoukaColors.SurfaceCard.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp),



                    color = ZakzoukaColors.TextWhite, fontSize = 9.sp,



                )



                if (item.year.isNotBlank()) {



                    Text(



                        item.year,



                    Modifier.clip(RoundedCornerShape(4.dp)).background(ZakzoukaColors.SurfaceCard.copy(alpha = 0.9f)).padding(horizontal = 6.dp, vertical = 2.dp),



                    color = ZakzoukaColors.TextMuted, fontSize = 9.sp,



                    )



                }



            }







            Text(



                item.title,



                color = ZakzoukaColors.TextWhite,



                fontSize = 19.sp,



                fontWeight = FontWeight.Bold,



                maxLines = 2,



                overflow = TextOverflow.Ellipsis,



            )







            // Play / Info Buttons



            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



                // Play button in Royal Gold



                Row(



                    Modifier.clip(RoundedCornerShape(6.dp))



                        .background(ZakzoukaColors.Gold)



                        .clickable(onClick = onPlay)



                        .padding(horizontal = 22.dp, vertical = 9.dp),



                    verticalAlignment = Alignment.CenterVertically,



                ) {



                    Text("▶", color = Color.White, fontSize = 13.sp)



                    Spacer(Modifier.width(6.dp))



                    Text("مشاهدة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)



                }







                // Info button



                Row(



                    Modifier.clip(RoundedCornerShape(6.dp))



                        .background(ZakzoukaColors.SurfaceHighlight)



                        .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(6.dp))



                        .clickable(onClick = onInfo)



                        .padding(horizontal = 16.dp, vertical = 9.dp),



                    verticalAlignment = Alignment.CenterVertically,



                ) {



                    Text("ℹ️", fontSize = 12.sp)



                    Spacer(Modifier.width(6.dp))



                    Text("تفاصيل", color = ZakzoukaColors.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)



                }



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Zakzouka Card Item



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaCard(item: ArabseedItem, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(



            Modifier.fillMaxWidth().aspectRatio(2f / 3f)



                .clip(RoundedCornerShape(8.dp))



                .background(ZakzoukaColors.Surface)



                .border(1.dp, ZakzoukaColors.Border, RoundedCornerShape(8.dp)),



        ) {



            if (item.posterUrl != null) {



                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )



            }







            // Quality tag top-right in Gold



            if (item.quality.isNotBlank()) {



                Box(



                    Modifier.align(Alignment.TopEnd).padding(6.dp)



                        .clip(RoundedCornerShape(4.dp))



                        .background(ZakzoukaColors.Gold)



                        .padding(horizontal = 5.dp, vertical = 2.dp),



                ) {



                    Text(



                        item.quality,



                        color = Color.White,



                        fontSize = 9.sp,



                        fontWeight = FontWeight.Bold,



                    )



                }



            }







            // Type tag bottom-left



            Box(



                Modifier.align(Alignment.BottomStart).padding(6.dp)



                    .clip(RoundedCornerShape(4.dp))



                    .background(Color(0xCC000000))



                    .padding(horizontal = 5.dp, vertical = 2.dp),



            ) {



                Text(



                    if (item.type == ArabseedType.MOVIE) "فيلم" else "مسلسل",



                    color = Color.White,



                    fontSize = 9.sp,



                )



            }



        }







        Text(



            text = item.title,



            color = ZakzoukaColors.TextWhite,



            fontSize = 12.sp,



            fontWeight = FontWeight.SemiBold,



            maxLines = 2,



            overflow = TextOverflow.Ellipsis,



            lineHeight = 16.sp,



        )







        val cardSubtitle = item.latestEpisodeLabel.ifBlank { item.category }

        if (item.year.isNotBlank() || cardSubtitle.isNotBlank()) {



            Text(



                text = "${item.year} · $cardSubtitle".trim(' ', '·'),



                color = ZakzoukaColors.GoldLight,



                fontSize = 10.sp,



                maxLines = 1,



            )



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Fast Detail Screen (Full Story, Instant Play)



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaDetailScreen(



    item: ArabseedItem,



    onBack: () -> Unit,



    onPlay: (ArabseedDetail, List<ArabseedEpisode>, Int) -> Unit,



    onOpenItem: ((ArabseedItem) -> Unit)? = null,



) {



    val context = LocalContext.current



    val scope = rememberCoroutineScope()



    var isFollowed by remember(item.pageUrl) { mutableStateOf(false) }







    val cachedDetail = remember(item.pageUrl) {
        if (item.providerName == "عرب سيد") ArabseedRepository.getCachedDetail(item.pageUrl)
        else MultiProviderRepository.getCachedDetail(item.pageUrl)
    }



    var detail by remember(item.pageUrl) { mutableStateOf(cachedDetail) }



    var fullStory by remember(item.pageUrl) { mutableStateOf(cachedDetail?.item?.overview ?: "") }



    var loadingDetail by remember(item.pageUrl) { mutableStateOf(cachedDetail == null) }



    var preResolvedStream by remember(item.pageUrl) { mutableStateOf(cachedDetail?.preResolvedStream) }







    // Seasons, Episodes & Related Parts state — ALWAYS fetched for all items!



    var seasonBundle by remember(item.pageUrl) { mutableStateOf<ArabseedSeasonBundle?>(null) }



    var loadingSeasons by remember(item.pageUrl) { mutableStateOf(true) }



    var selectedSeason by remember(item.pageUrl) { mutableStateOf(0) }

    var manuallySelectedSeasonNumber by remember(item.pageUrl) { mutableStateOf<Int?>(null) }







    // Recommendations state



    var recommendations by remember(item.pageUrl) { mutableStateOf<List<ArabseedItem>>(emptyList()) }



    var loadingRecommendations by remember(item.pageUrl) { mutableStateOf(true) }





    fun episodeItem(episode: ArabseedEpisode): ArabseedItem = item.copy(

        id = episode.pageUrl.trimEnd('/').substringAfterLast('/'),

        title = episode.title,

        arabicTitle = episode.title,

        pageUrl = episode.pageUrl,

        latestEpisodeLabel = "الموسم ${episode.seasonNumber} · الحلقة ${episode.number}",

        parentSeriesTitle = item.parentSeriesTitle?.takeIf(String::isNotBlank) ?: item.title,

        parentSeriesPageUrl = item.parentSeriesPageUrl?.takeIf(String::isNotBlank) ?: item.pageUrl,

    )





    LaunchedEffect(item.pageUrl) {



        if (item.type == ArabseedType.SERIES) {



            scope.launch {



                isFollowed = FollowRepository.isFollowed(context, item.pageUrl)



            }



        }







        // Paint the last saved detail first; refresh it while the screen is already usable.
        val diskDetail = withContext(Dispatchers.IO) {
            if (item.providerName == "عرب سيد") ArabseedRepository.loadCachedDetail(item.pageUrl)
            else MultiProviderRepository.loadCachedDetail(item.pageUrl)
        }
        val savedDetail = cachedDetail ?: diskDetail
        if (savedDetail != null && savedDetail.servers.isNotEmpty()) {
            detail = savedDetail
            fullStory = savedDetail.item.overview
            preResolvedStream = savedDetail.preResolvedStream
            loadingDetail = false
        } else {
            loadingDetail = true
        }

        val detailWatchdog = launch {
            delay(12_000L)
            if (detail?.servers.isNullOrEmpty()) {
                detail = ArabseedDetail(
                    item = item,
                    servers = listOf(ArabseedServer("صفحة المصدر · فتح بالمشغل", item.pageUrl)),
                )
                loadingDetail = false
            }
        }
        try {
            val freshDetail = withContext(Dispatchers.IO) {
                if (item.providerName == "عرب سيد" || item.pageUrl.contains("myseed.tv", ignoreCase = true)) {
                    ArabseedRepository.fetchDetail(item)
                } else {
                    MultiProviderRepository.fetchDetail(item)
                }
            }
            val previousDetail = detail
            val freshServers = freshDetail.servers.ifEmpty { previousDetail?.servers.orEmpty() }
            val usableDetail = freshDetail.copy(
                item = freshDetail.item.copy(
                    overview = freshDetail.item.overview.ifBlank { previousDetail?.item?.overview.orEmpty() }
                        .ifBlank { item.overview },
                ),
                servers = freshServers,
                preResolvedStream = freshDetail.preResolvedStream ?: previousDetail?.preResolvedStream,
                cast = freshDetail.cast.ifEmpty { previousDetail?.cast.orEmpty() },
            )
            detail = if (usableDetail.servers.isNotEmpty()) usableDetail else usableDetail.copy(
                servers = listOf(ArabseedServer("صفحة المصدر · فتح بالمشغل", item.pageUrl)),
            )
            fullStory = usableDetail.item.overview
            preResolvedStream = usableDetail.preResolvedStream
        } catch (_: Exception) {
            if (detail?.servers.isNullOrEmpty()) {
                detail = ArabseedDetail(
                    item = item,
                    servers = listOf(ArabseedServer("صفحة المصدر · فتح بالمشغل", item.pageUrl)),
                )
            }
        } finally {
            detailWatchdog.cancel()
            loadingDetail = false
        }



        // Render cached seasons immediately, then refresh the bundle in the background in parallel.
        val catalogItem = if (!item.parentSeriesPageUrl.isNullOrBlank()) {
            item.copy(
                title = item.parentSeriesTitle?.takeIf(String::isNotBlank) ?: item.title,
                pageUrl = item.parentSeriesPageUrl,
            )
        } else item

        fun publishSeasonBundle(bundle: ArabseedSeasonBundle) {
            val normalizedBundle = SeriesCatalogParser.normalizeSeasonBundle(bundle)
            val manualSeasonIndex = manuallySelectedSeasonNumber?.let { number ->
                normalizedBundle.seasons.indexOfFirst { it.number == number }
            } ?: -1
            seasonBundle = normalizedBundle
            val currentPage = SeriesCatalogParser.normalizedUrl(item.pageUrl)
            val pageSeasonIndex = normalizedBundle.seasons.indexOfFirst { season ->
                season.episodes.any { SeriesCatalogParser.normalizedUrl(it.pageUrl) == currentPage }
            }
            val seasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)
                ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")
            val titleSeasonIndex = seasonNumber?.let { number ->
                normalizedBundle.seasons.indexOfFirst { it.number == number }
            } ?: -1
            selectedSeason = when {
                manualSeasonIndex >= 0 -> manualSeasonIndex
                pageSeasonIndex >= 0 -> pageSeasonIndex
                titleSeasonIndex >= 0 -> titleSeasonIndex
                else -> 0
            }
        }

        val cachedBundle = withContext(Dispatchers.IO) {
            if (item.providerName == "عرب سيد" || item.pageUrl.contains("myseed.tv", ignoreCase = true)) ArabseedRepository.loadCachedSeasonBundle(catalogItem.pageUrl)
            else MultiProviderRepository.loadCachedSeasonBundle(catalogItem.pageUrl)
        }
        if (cachedBundle != null && cachedBundle.seasons.isNotEmpty()) {
            publishSeasonBundle(cachedBundle)
            loadingSeasons = false
        } else {
            loadingSeasons = true
        }

        try {
            val freshBundle = withContext(Dispatchers.IO) {
                withTimeoutOrNull(8_000L) {
                    if (item.providerName == "عرب سيد" || item.pageUrl.contains("myseed.tv", ignoreCase = true)) ArabseedRepository.fetchSeasonsAndEpisodes(catalogItem)
                    else MultiProviderRepository.fetchSeasonsAndEpisodes(catalogItem)
                }
            }
            if (freshBundle != null && freshBundle.seasons.isNotEmpty()) {
                publishSeasonBundle(freshBundle)
            }
        } catch (_: Exception) {
        } finally {
            loadingSeasons = false
        }
    }

    // Instant recommendations from cache (0ms delay) + deferred background refresh
    LaunchedEffect(item.pageUrl) {
        if (recommendations.isNotEmpty()) return@LaunchedEffect
        val expectedType = item.type
        try {
            // STEP 1: Instant cache load
            val diskRecs = withContext(Dispatchers.IO) {
                val primary = ArabseedRepository.loadCachedCategory(if (expectedType == ArabseedType.SERIES) "series/" else "movies/", 1).orEmpty()
                val multi = MultiProviderRepository.loadCachedCatalog(expectedType, 1).orEmpty()
                MultiProviderRepository.mergeCatalogItems(primary + multi, expectedType)
            }
            if (diskRecs.isNotEmpty()) {
                val valid = diskRecs.filter { it.pageUrl != item.pageUrl && !it.posterUrl.isNullOrBlank() }
                if (valid.isNotEmpty()) {
                    recommendations = valid.shuffled().take(16)
                    loadingRecommendations = false
                }
            }
            // STEP 2: Background refresh if cache was empty
            if (recommendations.isEmpty()) {
                val list = withContext(Dispatchers.IO) {
                    val arabseedItems = runCatching {
                        if (expectedType == ArabseedType.SERIES) ArabseedRepository.fetchSeries(1)
                        else ArabseedRepository.fetchMovies(1)
                    }.getOrElse { emptyList() }
                    arabseedItems
                }
                recommendations = list.filter { it.pageUrl != item.pageUrl && !it.posterUrl.isNullOrBlank() }.take(16)
            }
        } catch (_: Exception) {
        } finally {
            loadingRecommendations = false
        }
    }

    val displayOverview = fullStory.ifBlank { item.overview }



    val servers = detail?.servers ?: emptyList()



    val bundle = seasonBundle



    val seasons = bundle?.seasons ?: emptyList()



    val currentSeason = seasons.getOrNull(selectedSeason)



    val currentEpisodes = currentSeason?.episodes ?: emptyList()



    val relatedParts = bundle?.relatedParts ?: emptyList()







    // Determine current episode index if item matches an episode in the list



    val currentEpIdx = remember(item.pageUrl, item.title, currentEpisodes) {

        val normalised = SeriesCatalogParser.normalizedUrl(item.pageUrl)

        val urlIdx = currentEpisodes.indexOfFirst {

            SeriesCatalogParser.normalizedUrl(it.pageUrl) == normalised

        }

        if (urlIdx >= 0) {

            urlIdx

        } else {

            val numFromTitle = SeriesCatalogParser.episodeNumber(item.title, item.pageUrl)

                ?: SeriesCatalogParser.episodeNumber(item.latestEpisodeLabel, "")

            val itemSeasonNumber = SeriesCatalogParser.seasonNumber(item.title, item.pageUrl)

                ?: SeriesCatalogParser.seasonNumber(item.latestEpisodeLabel, "")

            val selectedSeasonMatchesItem = itemSeasonNumber == currentSeason?.number

                || (itemSeasonNumber == null && seasons.size == 1)

            if (numFromTitle != null && selectedSeasonMatchesItem) {

                currentEpisodes.indexOfFirst { it.number == numFromTitle }

            } else {

                -1

            }

        }

    }



    val previousEpisodeIndex = adjacentEpisodeIndex(currentEpisodes, currentEpIdx, -1)



    val nextEpisodeIndex = adjacentEpisodeIndex(currentEpisodes, currentEpIdx, 1)



    val hasPrevEp = previousEpisodeIndex != null



    val hasNextEp = nextEpisodeIndex != null







    Column(Modifier.fillMaxSize().background(ZakzoukaColors.Background)) {



        // Top Back Row



        Row(



            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),



            verticalAlignment = Alignment.CenterVertically,



        ) {



            Box(



                Modifier.size(38.dp).clip(CircleShape).background(ZakzoukaColors.SurfaceCard)



                    .border(1.dp, ZakzoukaColors.GoldBorder, CircleShape).clickable(onClick = onBack),



                contentAlignment = Alignment.Center,



            ) { Text("←", color = ZakzoukaColors.TextWhite, fontSize = 18.sp) }



            Spacer(Modifier.width(12.dp))



            Text(



                item.parentSeriesTitle?.takeIf(String::isNotBlank) ?: item.title,

                color = ZakzoukaColors.TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold,



                maxLines = 1, overflow = TextOverflow.Ellipsis,



            )



        }







        LazyColumn(



            Modifier.weight(1f),



            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),



            verticalArrangement = Arrangement.spacedBy(16.dp),



        ) {



            // ── 1. Poster + Meta + Compact Action Bar ───────────────────────────



            item {



                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {



                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {



                        Box(



                            Modifier.size(width = 110.dp, height = 160.dp)



                                .clip(RoundedCornerShape(8.dp)).background(ZakzoukaColors.Surface)



                                .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp)),



                        ) {



                            if (item.posterUrl != null) {



                                AsyncImage(



                                    model = item.posterUrl, contentDescription = item.title,



                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop



                                )



                            }



                        }



                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {



                            Text(

                                item.parentSeriesTitle?.takeIf(String::isNotBlank) ?: item.title,

                                color = ZakzoukaColors.TextWhite, fontSize = 17.sp, fontWeight = FontWeight.Bold,

                                maxLines = 3, overflow = TextOverflow.Ellipsis,

                            )

                            if (!item.parentSeriesTitle.isNullOrBlank() && item.title != item.parentSeriesTitle) {

                                Text(

                                    item.title,

                                    color = ZakzoukaColors.TextMuted, fontSize = 12.sp,

                                    maxLines = 1, overflow = TextOverflow.Ellipsis,

                                )

                            }



                            Text(



                                buildString {



                                    if (item.year.isNotBlank()) append(item.year)



                                    if (item.quality.isNotBlank()) append(" · ${item.quality}")



                                    append(" · ${if (item.type == ArabseedType.MOVIE) "فيلم" else "مسلسل"}")



                                    if (seasons.isNotEmpty()) append(" · ${seasons.size} مواسم")



                                    if (currentEpisodes.isNotEmpty()) append(" · ${currentEpisodes.size} حلقة")



                                },



                                color = ZakzoukaColors.TextMuted, fontSize = 11.sp,



                            )



                            Text(

                                item.latestEpisodeLabel.ifBlank { item.category },

                                color = ZakzoukaColors.GoldLight,

                                fontSize = 11.sp,

                                fontWeight = FontWeight.Bold,

                            )



                        }



                    }







                    // Compact Action Row (Single slim 42dp line: Play + Follow + Favorite)



                    Row(



                        Modifier.fillMaxWidth().height(42.dp),



                        horizontalArrangement = Arrangement.spacedBy(8.dp),



                        verticalAlignment = Alignment.CenterVertically,



                    ) {



                        // Play button



                        val playLabel = when {



                            currentEpIdx >= 0 -> "▶  تشغيل هذه الحلقة"



                            currentEpisodes.isNotEmpty() -> "▶  تشغيل الحلقة الأولى"



                            else -> "▶  تشغيل الآن"



                        }







                        Box(



                            Modifier.weight(1f).fillMaxHeight()



                                .clip(RoundedCornerShape(8.dp))



                                .background(if (loadingDetail && servers.isEmpty() && currentEpisodes.isEmpty()) ZakzoukaColors.SurfaceHighlight else ZakzoukaColors.Gold)



                                .clickable {
                                    val epIdx = if (currentEpIdx >= 0) currentEpIdx else 0
                                    if (servers.isNotEmpty()) {
                                        onPlay(
                                            (detail ?: ArabseedDetail(item, servers)).copy(preResolvedStream = preResolvedStream),
                                            currentEpisodes,
                                            epIdx
                                        )
                                    } else {
                                        Toast.makeText(context, "جاري تحضير سيرفر البث وتشغيل الفيديو...", Toast.LENGTH_SHORT).show()
                                        scope.launch {
                                            val freshServers = withContext(Dispatchers.IO) {
                                                if (item.providerName == "عرب سيد") ArabseedRepository.fetchServersFast(item.pageUrl)
                                                else MultiProviderRepository.fetchServers(item.pageUrl)
                                            }
                                            if (freshServers.isNotEmpty()) {
                                                val pr = withContext(Dispatchers.IO) {
                                                    if (item.providerName == "عرب سيد") ArabseedRepository.resolveDirectStream(freshServers.first().embedUrl)
                                                    else null
                                                }
                                                val targetDetail = ArabseedDetail(item, freshServers, pr)
                                                detail = targetDetail
                                                onPlay(targetDetail, currentEpisodes, epIdx)
                                            } else {
                                                Toast.makeText(context, "تعذر تجهيز السيرفر حالياً. حاول مع فيلم أو حلقة أخرى.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,



                        ) {



                            if (loadingDetail && servers.isEmpty()) {



                                Row(verticalAlignment = Alignment.CenterVertically) {



                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)



                                    Spacer(Modifier.width(8.dp))



                                    Text("جاري تحضير البث...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                                }



                            } else {



                                Text(playLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)



                            }



                        }







                        // Follow button (Series only)



                        if (item.type == ArabseedType.SERIES) {



                            Box(



                                Modifier.fillMaxHeight()



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(if (isFollowed) ZakzoukaColors.Gold.copy(alpha = 0.18f) else ZakzoukaColors.SurfaceCard)



                                    .border(1.dp, if (isFollowed) ZakzoukaColors.Gold else ZakzoukaColors.Border, RoundedCornerShape(8.dp))



                                    .clickable {



                                        scope.launch {



                                            isFollowed = FollowRepository.toggleFollow(context, item.pageUrl)



                                        }



                                    }



                                    .padding(horizontal = 14.dp),



                                contentAlignment = Alignment.Center,



                            ) {



                                Text(



                                    if (isFollowed) "✓ مُتابع" else "🔔 متابعة",



                                    color = if (isFollowed) ZakzoukaColors.Gold else ZakzoukaColors.TextWhite,



                                    fontSize = 12.sp,



                                    fontWeight = FontWeight.Bold,



                                )



                            }



                        }







                        // Favorite heart button



                        FavoriteHeartButton(item = item, size = 42)



                    }



                }



            }







            // ── 2. Full Story ──────────────────────────────────────────────────



            if (displayOverview.isNotBlank() || loadingDetail) {



                item {



                    Column(



                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))



                            .background(ZakzoukaColors.Surface)



                            .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp))



                            .padding(14.dp),



                        verticalArrangement = Arrangement.spacedBy(8.dp),



                    ) {



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Text("القصة كاملة", color = ZakzoukaColors.GoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)



                            Spacer(Modifier.weight(1f))



                            if (loadingDetail && fullStory.isBlank()) {



                                CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(13.dp), strokeWidth = 1.5.dp)



                                Spacer(Modifier.width(6.dp))



                                Text("جاري جلب القصة...", color = ZakzoukaColors.TextMuted, fontSize = 10.sp)



                            }



                        }



                        if (displayOverview.isNotBlank()) {



                            Text(text = displayOverview, color = Color(0xFF334155), fontSize = 13.sp, lineHeight = 22.sp)



                        }



                    }



                }



            }







            // ── 3. Streaming Servers (ALWAYS SHOWN) ────────────────────────────



            item {



                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {



                    DirectDownloadSection(

                        item = item,

                        servers = servers,

                        preResolvedStream = preResolvedStream,

                        currentEpIdx = currentEpIdx,

                        currentEpisodes = currentEpisodes,

                    )



                    Spacer(Modifier.height(10.dp))



                    Text("اختر سيرفر المشاهدة", color = ZakzoukaColors.TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)



                    if (loadingDetail && servers.isEmpty()) {



                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),



                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {



                            CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(20.dp))



                            Spacer(Modifier.width(10.dp))



                            Text("جاري استخراج السيرفرات السريعة...", color = ZakzoukaColors.TextMuted, fontSize = 12.sp)



                        }



                    } else if (servers.isEmpty()) {

                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("تعذر جلب السيرفرات تلقائياً من المصدر", color = ZakzoukaColors.TextMuted, fontSize = 12.sp)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ZakzoukaColors.Gold)
                                    .clickable {
                                        scope.launch {
                                            loadingDetail = true
                                            val srvs = withContext(Dispatchers.IO) {
                                                val s = if (item.providerName == "عرب سيد") ArabseedRepository.fetchServersFast(item.pageUrl)
                                                else MultiProviderRepository.fetchServers(item.pageUrl)
                                                if (s.isEmpty()) {
                                                    if (item.providerName == "عرب سيد") MultiProviderRepository.fetchServers(item.pageUrl)
                                                    else ArabseedRepository.fetchServersFast(item.pageUrl)
                                                } else s
                                            }
                                            if (srvs.isNotEmpty()) {
                                                val pr = withContext(Dispatchers.IO) {
                                                    if (item.providerName == "عرب سيد") ArabseedRepository.resolveDirectStream(srvs.first().embedUrl)
                                                    else null
                                                }
                                                detail = ArabseedDetail(item, srvs, pr)
                                            }
                                            loadingDetail = false
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("إعادة المحاولة 🔄", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                    } else {



                        servers.forEachIndexed { index, server ->



                            val isFirst = index == 0



                            Row(



                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))



                                    .background(ZakzoukaColors.Surface)



                                    .border(1.dp, if (isFirst) ZakzoukaColors.Gold else ZakzoukaColors.Border, RoundedCornerShape(8.dp))



                                    .clickable {



                                        val targetDetail = (detail ?: ArabseedDetail(item, servers))



                                            .copy(



                                                servers = listOf(server) + (servers - server),



                                                preResolvedStream = if (isFirst) preResolvedStream else null,



                                            )



                                        val epIdx = if (currentEpIdx >= 0) currentEpIdx else 0



                                        onPlay(targetDetail, currentEpisodes, epIdx)



                                    }



                                    .padding(horizontal = 14.dp, vertical = 12.dp),



                                verticalAlignment = Alignment.CenterVertically,



                            ) {



                                Text("▶", color = ZakzoukaColors.Gold, fontSize = 12.sp)



                                Spacer(Modifier.width(10.dp))



                                Text(server.name, color = ZakzoukaColors.TextWhite, fontSize = 13.sp,



                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))



                                Text(if (isFirst) "فائق السرعة ⚡" else "سريع",



                                    color = ZakzoukaColors.LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)



                            }



                        }



                    }



                }



            }







            // ── 4. DIRECTLY UNDER SERVERS: Related Parts / Other Seasons ───────



            if (loadingSeasons || relatedParts.isNotEmpty()) {



                item {



                    Column(



                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))



                            .background(ZakzoukaColors.Surface)



                            .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(10.dp))



                            .padding(14.dp),



                        verticalArrangement = Arrangement.spacedBy(10.dp),



                    ) {



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Text("🎬 الأجزاء الأخرى", color = ZakzoukaColors.GoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)



                            Spacer(Modifier.weight(1f))



                            if (loadingSeasons && relatedParts.isEmpty()) {



                                CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)



                                Spacer(Modifier.width(6.dp))



                                Text("جاري فحص الأجزاء...", color = ZakzoukaColors.TextMuted, fontSize = 10.sp)



                            } else if (relatedParts.isNotEmpty()) {



                                Text("${relatedParts.size} جزء", color = ZakzoukaColors.TextMuted, fontSize = 11.sp)



                            }



                        }







                        if (relatedParts.isNotEmpty()) {



                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



                                items(relatedParts) { part ->



                                    Column(



                                        Modifier.width(115.dp).clickable {



                                            onOpenItem?.invoke(part)



                                        },



                                        verticalArrangement = Arrangement.spacedBy(6.dp),



                                    ) {



                                        Box(



                                            Modifier.fillMaxWidth().aspectRatio(2f / 3f)



                                                .clip(RoundedCornerShape(8.dp))



                                                .background(ZakzoukaColors.SurfaceCard)



                                                .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp)),



                                        ) {



                                            if (part.posterUrl != null) {



                                                AsyncImage(model = part.posterUrl, contentDescription = part.title,



                                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)



                                            }



                                            Box(



                                                Modifier.align(Alignment.BottomCenter).fillMaxWidth()



                                                    .background(Color(0xE6000000)).padding(vertical = 3.dp),



                                                contentAlignment = Alignment.Center,



                                            ) {



                                                Text("اضغط للفتح", color = ZakzoukaColors.GoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)



                                            }



                                        }



                                        Text(



                                            part.title, color = ZakzoukaColors.TextWhite, fontSize = 10.sp,



                                            fontWeight = FontWeight.SemiBold,



                                            maxLines = 2, overflow = TextOverflow.Ellipsis,



                                        )



                                    }



                                }



                            }



                        }



                    }



                }



            }







            // ── 5. SEASONS & EPISODES (Under Other Parts) ───────────────────────



            if (loadingSeasons || seasons.isNotEmpty()) {



                item {



                    Column(



                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))



                            .background(ZakzoukaColors.Surface)



                            .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(10.dp))



                            .padding(14.dp),



                        verticalArrangement = Arrangement.spacedBy(12.dp),



                    ) {



                        // Header



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Text("📺 حلقات هذا الجزء / الموسم", color = ZakzoukaColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)



                            Spacer(Modifier.weight(1f))



                            if (loadingSeasons && seasons.isEmpty()) {



                                CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)



                            } else if (currentEpisodes.isNotEmpty()) {



                                Text(



                                    "${currentEpisodes.size} حلقة",



                                    color = ZakzoukaColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold



                                )



                            }



                        }







                        if (loadingSeasons && seasons.isEmpty()) {



                            Text("جاري استخراج الحلقات...", color = ZakzoukaColors.TextMuted, fontSize = 12.sp)



                        } else if (seasons.isEmpty()) {



                            Text("لا توجد حلقات إضافية في هذا العمل", color = ZakzoukaColors.TextMuted, fontSize = 12.sp)



                        } else {



                            // Season selector tabs (if more than 1 season)



                            if (seasons.size > 1) {



                                LazyRow(



                                    horizontalArrangement = Arrangement.spacedBy(8.dp),



                                    contentPadding = PaddingValues(vertical = 2.dp),



                                ) {



                                    items(seasons.size) { idx ->



                                        val active = idx == selectedSeason



                                        Box(



                                            Modifier.clip(RoundedCornerShape(20.dp))



                                                .background(if (active) ZakzoukaColors.Gold else ZakzoukaColors.SurfaceCard)



                                                .border(1.dp, if (active) ZakzoukaColors.Gold else ZakzoukaColors.Border, RoundedCornerShape(20.dp))



                                                .clickable {
                                                    selectedSeason = idx
                                                    manuallySelectedSeasonNumber = seasons[idx].number
                                                }



                                                .padding(horizontal = 16.dp, vertical = 7.dp),



                                        ) {



                                            Text(



                                                seasons[idx].title,



                                                color = if (active) Color.White else ZakzoukaColors.TextMuted,



                                                fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,



                                            )



                                        }



                                    }



                                }



                            }







                            // Previous / Next Episode buttons (Quick jump)



                            if (hasPrevEp || hasNextEp) {



                                Row(



                                    Modifier.fillMaxWidth(),



                                    horizontalArrangement = Arrangement.spacedBy(10.dp),



                                ) {



                                    if (hasPrevEp) {



                                        val prevEp = currentEpisodes[previousEpisodeIndex!!]



                                        Row(



                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))



                                                .background(ZakzoukaColors.SurfaceCard)



                                                .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp))



                                                .clickable {



                                                    onOpenItem?.invoke(episodeItem(prevEp))



                                                }



                                                .padding(horizontal = 12.dp, vertical = 10.dp),



                                            verticalAlignment = Alignment.CenterVertically,



                                            horizontalArrangement = Arrangement.Center,



                                        ) {



                                            Text("→ السابق: ${prevEp.title}", color = ZakzoukaColors.TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                                        }



                                    } else {



                                        Spacer(Modifier.weight(1f))



                                    }







                                    if (hasNextEp) {



                                        val nextEp = currentEpisodes[nextEpisodeIndex!!]



                                        Row(



                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))



                                                .background(ZakzoukaColors.Gold)



                                                .clickable {



                                                    onOpenItem?.invoke(episodeItem(nextEp))



                                                }



                                                .padding(horizontal = 12.dp, vertical = 10.dp),



                                            verticalAlignment = Alignment.CenterVertically,



                                            horizontalArrangement = Arrangement.Center,



                                        ) {



                                            Text("التالي: ${nextEp.title} ←", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)



                                        }



                                    } else {



                                        Spacer(Modifier.weight(1f))



                                    }



                                }



                            }







                            // Episodes Grid (Clean 4-column compact grid)



                            val episodes = currentEpisodes



                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {



                                episodes.chunked(4).forEach { rowEps ->



                                    Row(



                                        Modifier.fillMaxWidth(),



                                        horizontalArrangement = Arrangement.spacedBy(8.dp),



                                    ) {



                                        rowEps.forEach { ep ->



                                            val isCurrent = SeriesCatalogParser.normalizedUrl(ep.pageUrl) ==

                                                SeriesCatalogParser.normalizedUrl(item.pageUrl)



                                            Box(



                                                Modifier.weight(1f).height(42.dp)



                                                    .clip(RoundedCornerShape(8.dp))



                                                    .background(if (isCurrent) ZakzoukaColors.Gold else ZakzoukaColors.SurfaceHighlight)



                                                    .border(1.dp, if (isCurrent) ZakzoukaColors.Gold else ZakzoukaColors.Border, RoundedCornerShape(8.dp))



                                                    .clickable {



                                                        onOpenItem?.invoke(episodeItem(ep))



                                                    },



                                                contentAlignment = Alignment.Center,



                                            ) {



                                                Text(



                                                    text = "الحلقة ${ep.number}",



                                                    color = if (isCurrent) Color.White else ZakzoukaColors.TextWhite,



                                                    fontSize = 11.sp,



                                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,



                                                )



                                            }



                                        }



                                        repeat(4 - rowEps.size) {



                                            Spacer(Modifier.weight(1f))



                                        }



                                    }



                                }



                            }



                        }



                    }



                }



            }







            // ── 6. ربما يعجبك لاحقاً (Recommendations with Posters) ─────────────



            if (loadingRecommendations || recommendations.isNotEmpty()) {



                item {



                    Column(



                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))



                            .background(ZakzoukaColors.Surface)



                            .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(10.dp))



                            .padding(14.dp),



                        verticalArrangement = Arrangement.spacedBy(10.dp),



                    ) {



                        Row(verticalAlignment = Alignment.CenterVertically) {



                            Text(



                                "✨ ربما يعجبك لاحقاً",



                                color = ZakzoukaColors.GoldLight,



                                fontSize = 14.sp,



                                fontWeight = FontWeight.Bold,



                            )



                            Spacer(Modifier.weight(1f))



                            if (loadingRecommendations && recommendations.isEmpty()) {



                                CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)



                            }



                        }







                        if (recommendations.isNotEmpty()) {



                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {



                                items(recommendations) { recItem ->



                                    Column(



                                        Modifier.width(115.dp).clickable {



                                            onOpenItem?.invoke(recItem)



                                        },



                                        verticalArrangement = Arrangement.spacedBy(6.dp),



                                    ) {



                                        Box(



                                            Modifier.fillMaxWidth().aspectRatio(2f / 3f)



                                                .clip(RoundedCornerShape(8.dp))



                                                .background(ZakzoukaColors.SurfaceCard)



                                                .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp)),



                                        ) {



                                            if (!recItem.posterUrl.isNullOrBlank()) {



                                                AsyncImage(



                                                    model = recItem.posterUrl,



                                                    contentDescription = recItem.title,



                                                    modifier = Modifier.fillMaxSize(),



                                                    contentScale = ContentScale.Crop,



                                                )



                                            }



                                            if (recItem.quality.isNotBlank()) {



                                                Box(



                                                    Modifier.align(Alignment.TopEnd).padding(4.dp)



                                                        .clip(RoundedCornerShape(4.dp))



                                                        .background(Color(0xCC000000))



                                                        .padding(horizontal = 4.dp, vertical = 2.dp),



                                                ) {



                                                    Text(recItem.quality, color = ZakzoukaColors.GoldLight, fontSize = 8.sp, fontWeight = FontWeight.Bold)



                                                }



                                            }



                                        }



                                        Text(



                                            recItem.arabicTitle.ifBlank { recItem.title },



                                            color = ZakzoukaColors.TextWhite,



                                            fontSize = 11.sp,



                                            fontWeight = FontWeight.SemiBold,



                                            maxLines = 2,



                                            overflow = TextOverflow.Ellipsis,



                                            lineHeight = 15.sp,



                                        )



                                    }



                                }



                            }



                        }



                    }



                }



            }



        }



    }



}







// ─────────────────────────────────────────────────────────────────────────────



// Hybrid Player Screen (Native ExoPlayer for m3u8 + Clean Web Embed fallback)



// ─────────────────────────────────────────────────────────────────────────────







// ── DIRECT DOWNLOAD TO MOBILE COMPONENT ──────────────────────────────────────

@Composable

private fun DirectDownloadSection(

    item: ArabseedItem,

    servers: List<ArabseedServer>,

    preResolvedStream: String?,

    currentEpIdx: Int,

    currentEpisodes: List<ArabseedEpisode>,

) {

    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }



    val currentEp = currentEpisodes.getOrNull(currentEpIdx)

    val epLabel = if (currentEp != null) {

        "الحلقة ${currentEp.number}"

    } else if (item.type == ArabseedType.SERIES) {

        val numMatch = Regex("""(?:الحلقة|حلقة)\\s*([0-9]+)""").find(item.title)?.groupValues?.getOrNull(1)

        if (numMatch != null) "الحلقة $numMatch" else "الحلقة الحالية"

    } else {

        "الفيلم كامل"

    }



    val displayTitle = if (item.type == ArabseedType.SERIES) {

        val base = item.title.replace(Regex("""(?:الحلقة|حلقة)[ -]?[0-9]+.*"""), "").trim()

        "$base - $epLabel"

    } else {

        item.title

    }



    Box(

        modifier = Modifier

            .fillMaxWidth()

            .clip(RoundedCornerShape(12.dp))

            .background(

                Brush.horizontalGradient(

                    colors = listOf(

                        Color(0xFF065F46), // Rich dark emerald

                        Color(0xFF059669), // Vivid emerald green

                    )

                )

            )

            .clickable(enabled = !isDownloading) {

                isDownloading = true

                scope.launch {

                    try {

                        val resolvedSource = withContext(Dispatchers.IO) {

                            if (!preResolvedStream.isNullOrBlank()) {

                                preResolvedStream to servers.firstOrNull()?.embedUrl.orEmpty()

                            } else {

                                var direct: Pair<String, String>? = null

                                // Trying every server can leave the download button in a
                                // spinner for minutes when several hosts are offline.
                                for (srv in servers.take(4)) {

                                    val d = srv.directStreamUrl ?: ArabseedRepository.resolveDirectStream(srv.embedUrl)

                                    if (!d.isNullOrBlank()) {

                                        direct = d to srv.embedUrl

                                        break

                                    }

                                    val g = extractGenericVideoFromEmbed(srv.embedUrl)

                                    if (!g.isNullOrBlank()) {

                                        direct = g to srv.embedUrl

                                        break

                                    }

                                }

                                direct

                            }

                        }



                        val resolvedUrl = resolvedSource?.first
                        val isRealVideo = resolvedUrl?.let { u ->

                            u.contains(".m3u8", ignoreCase = true) ||

                            u.contains(".mp4", ignoreCase = true) ||

                            u.contains(".mkv", ignoreCase = true) ||

                            u.contains(".ts", ignoreCase = true) ||

                            (!u.contains("embed", ignoreCase = true) && !u.contains("iframe", ignoreCase = true))

                        } == true



                        if (isRealVideo && !resolvedUrl.isNullOrBlank()) {
                            val pageReferer = runCatching {
                                val uri = java.net.URI.create(item.pageUrl)
                                "${uri.scheme}://${uri.host}/"
                            }.getOrDefault("")
                            val downloadReferer = resolvedSource?.second.orEmpty().ifBlank { pageReferer }

                            performNativeDownload(

                                context = context,

                                title = displayTitle,

                                downloadUrl = resolvedUrl,

                                userAgent = ArabseedRepository.getPlayerUserAgent(),

                                referer = downloadReferer,

                            )

                        } else {

                            Toast.makeText(

                                context,

                                "المصدر لم يوفّر رابط تنزيل مباشر. جرّب سيرفر مشاهدة آخر.",

                                Toast.LENGTH_LONG

                            ).show()

                        }

                    } catch (e: Exception) {

                        Toast.makeText(context, "تعذر التنزيل: ${e.message}", Toast.LENGTH_SHORT).show()

                    } finally {

                        isDownloading = false

                    }

                }

            }

            .padding(horizontal = 14.dp, vertical = 12.dp)

    ) {

        Row(

            modifier = Modifier.fillMaxWidth(),

            verticalAlignment = Alignment.CenterVertically,

            horizontalArrangement = Arrangement.SpaceBetween,

        ) {

            Row(

                modifier = Modifier.weight(1f),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(12.dp),

            ) {

                Box(

                    modifier = Modifier

                        .size(40.dp)

                        .clip(CircleShape)

                        .background(Color.White.copy(alpha = 0.22f)),

                    contentAlignment = Alignment.Center,

                ) {

                    Text("⬇", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                }



                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {

                    Text(

                        text = "تحميل مباشر إلى الهاتف",

                        color = Color.White,

                        fontSize = 14.sp,

                        fontWeight = FontWeight.Bold,

                    )

                    Text(

                        text = "$epLabel • تنزيل فوري إلى الهاتف (Downloads)",

                        color = Color.White.copy(alpha = 0.88f),

                        fontSize = 11.sp,

                    )

                }

            }



            Box(

                modifier = Modifier

                    .clip(RoundedCornerShape(8.dp))

                    .background(Color.White)

                    .padding(horizontal = 12.dp, vertical = 7.dp),

                contentAlignment = Alignment.Center,

            ) {

                if (isDownloading) {

                    Row(

                        verticalAlignment = Alignment.CenterVertically,

                        horizontalArrangement = Arrangement.spacedBy(6.dp),

                    ) {

                        CircularProgressIndicator(

                            color = Color(0xFF065F46),

                            modifier = Modifier.size(14.dp),

                            strokeWidth = 2.dp,

                        )

                        Text(

                            "جاري التحميل...",

                            color = Color(0xFF065F46),

                            fontSize = 11.sp,

                            fontWeight = FontWeight.Bold,

                        )

                    }

                } else {

                    Text(

                        "تحميل الآن ⬇",

                        color = Color(0xFF065F46),

                        fontSize = 12.sp,

                        fontWeight = FontWeight.Bold,

                    )

                }

            }

        }

    }

}



private fun performNativeDownload(

    context: Context,

    title: String,

    downloadUrl: String,

    userAgent: String,

    referer: String,

) {

    val cleanTitle = title

        .replace(Regex("""[\\/:*?"<>|]"""), "")

        .replace(Regex("""\s+"""), " ")

        .trim()

        .take(70)



    try {

        com.madar.tv.download.ZakzoukaDownloadService.startDownload(

            context = context,

            title = cleanTitle,

            url = downloadUrl,

            userAgent = userAgent,

            referer = referer,

        )

        Toast.makeText(

            context,

            "⬇ بدأ تنزيل $cleanTitle في الخلفية. يمكنك متابعته وإلغاؤه من شريط الإشعارات.",

            Toast.LENGTH_LONG

        ).show()

    } catch (e: Exception) {

        Toast.makeText(context, "تعذر بدء التنزيل: ${e.message}", Toast.LENGTH_SHORT).show()

    }

}



private fun extractGenericVideoFromEmbed(embedUrl: String, depth: Int = 0): String? {

    if (depth > 2) return null

    if (embedUrl.endsWith(".mp4", ignoreCase = true) ||

        embedUrl.endsWith(".m3u8", ignoreCase = true) ||

        embedUrl.contains(".m3u8?", ignoreCase = true) ||

        embedUrl.contains(".mp4?", ignoreCase = true)) {

        return embedUrl

    }



    return try {

        val client = okhttp3.OkHttpClient.Builder()

            .connectTimeout(7, java.util.concurrent.TimeUnit.SECONDS)

            .readTimeout(8, java.util.concurrent.TimeUnit.SECONDS)

            .followRedirects(true)

            .build()

        val req = okhttp3.Request.Builder()

            .url(embedUrl)

            .header("User-Agent", ArabseedRepository.getPlayerUserAgent())

            .header("Referer", embedUrl)

            .build()

        val html = client.newCall(req).execute().body?.string() ?: return null



        // 1. Hex-encoded URLs (e.g. const Mohix = "68747470..." on govid/mycima)

        val hexRegex = Regex("""["'](68747470[0-9a-fA-F]{30,})["']""")

        hexRegex.findAll(html).forEach { match ->

            val hex = match.groupValues[1]

            try {

                val sb = StringBuilder()

                var i = 0

                while (i < hex.length - 1) {

                    sb.append(hex.substring(i, i + 2).toInt(16).toChar())

                    i += 2

                }

                val dec = sb.toString()

                if (dec.startsWith("http") && (dec.contains(".m3u8") || dec.contains(".mp4"))) {

                    return dec

                }

            } catch (_: Exception) {}

        }



        // 2. JWPlayer / VideoJS: sources / file / src

        val jwRegex = Regex("""(?:file|sources|src)\s*:\s*(?:\[\s*\{[^}]*?file\s*:\s*["']([^"']+)["']|\[\s*["']([^"']+)["']|["']([^"']+\.(?:mp4|m3u8)[^"']*)["'])""", RegexOption.IGNORE_CASE)

        val jwMatch = jwRegex.find(html)

        if (jwMatch != null) {

            val cand = jwMatch.groupValues.drop(1).firstOrNull { it.isNotBlank() }

            if (cand != null && cand.startsWith("http")) return cand

        }



        // 3. HTML5 source / video tags

        val tagRegex = Regex("""<(?:video|source)[^>]+src=["'](https?://[^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)

        tagRegex.find(html)?.groupValues?.getOrNull(1)?.let { return it }



        // 4. Direct regex match in script

        val directRegex = Regex("""["'](https?://[^\s"'<>]+\.(?:m3u8|mp4)(?:\?[^\s"'<>]*)?)["']""", RegexOption.IGNORE_CASE)

        directRegex.find(html)?.groupValues?.getOrNull(1)?.let { return it }



        // 5. Follow iframes or data-link

        val iframeRegex = Regex("""<iframe[^>]+src=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)

        for (m in iframeRegex.findAll(html)) {

            val ifr = m.groupValues[1]

            if (!ifr.contains("google") && !ifr.contains("facebook") && !ifr.contains("banner")) {

                val sub = extractGenericVideoFromEmbed(ifr, depth + 1)

                if (sub != null) return sub

            }

        }



        val dataLinkRegex = Regex("""data-(?:link|url|src)=["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE)

        for (m in dataLinkRegex.findAll(html)) {

            val dl = m.groupValues[1]

            if (dl.contains("govid") || dl.contains("play") || dl.contains("embed")) {

                val sub = extractGenericVideoFromEmbed(dl, depth + 1)

                if (sub != null) return sub

            }

        }



        null

    } catch (_: Exception) {

        null

    }

}



@SuppressLint("SetJavaScriptEnabled")

@Composable

private fun ZakzoukaPlayerScreen(



    detail: ArabseedDetail,



    onBack: () -> Unit,



    hasPrev: Boolean = false,



    hasNext: Boolean = false,



    onPrev: (() -> Unit)? = null,



    onNext: (() -> Unit)? = null,



) {



    val context = LocalContext.current



    var activeServer by remember(detail) { mutableStateOf(detail.servers.firstOrNull()) }



    val resolvedCache = remember { mutableMapOf<String, String>() }



    BackHandler {

        resetScreenOrientation(context)

        onBack()

    }







    // Auto rotate to landscape & hide system bars on enter, reset on exit



    DisposableEffect(Unit) {



        setScreenOrientation(context, true)



        onDispose {



            resetScreenOrientation(context)



        }



    }







    if (detail.preResolvedStream != null && detail.servers.isNotEmpty()) {



        resolvedCache[detail.servers.first().embedUrl] = detail.preResolvedStream



    }







    var resolvedStreamUrl by remember(activeServer) {



        mutableStateOf(activeServer?.let { resolvedCache[it.embedUrl] })



    }



    var resolving by remember(activeServer) {



        mutableStateOf(resolvedStreamUrl == null)



    }

    val itemPageReferer = remember(detail.item.pageUrl) {
        runCatching {
            val uri = java.net.URI.create(detail.item.pageUrl)
            "${uri.scheme}://${uri.host}/"
        }.getOrDefault("")
    }
    val activeEmbedReferer = activeServer?.let {
        playerRefererForEmbed(it.embedUrl, itemPageReferer)
    } ?: itemPageReferer
    val activeStreamReferer = if (activeServer != null && resolvedStreamUrl != activeServer?.embedUrl) {
        activeServer?.embedUrl.orEmpty().ifBlank { activeEmbedReferer }
    } else activeEmbedReferer







    var playerPrompt by remember(activeServer) { mutableStateOf<String?>(null) }

    var promptNeedsRetry by remember(activeServer) { mutableStateOf(false) }



    // Auto-hiding controls (overlay)



    var controlsVisible by remember { mutableStateOf(true) }



    LaunchedEffect(controlsVisible) {



        if (controlsVisible) {



            delay(4000)



            controlsVisible = false



        }



    }







    LaunchedEffect(activeServer) {
        val s = activeServer
        if (s == null) {
            resolving = false
            return@LaunchedEffect
        }
        // 1. Direct stream already resolved on server object (instant playback, 0s delay, 0 ads)
        if (!s.directStreamUrl.isNullOrBlank()) {
            resolvedStreamUrl = s.directStreamUrl
            resolvedCache[s.embedUrl] = s.directStreamUrl!!
            resolving = false
            return@LaunchedEffect
        }
        val cached = resolvedCache[s.embedUrl]
        if (cached != null) {
            resolvedStreamUrl = cached
            resolving = false
            return@LaunchedEffect
        }
        resolving = true
        val stream = withContext(Dispatchers.IO) {
            ArabseedRepository.resolveDirectStream(s.embedUrl)
        }
        if (stream != null) {
            resolvedCache[s.embedUrl] = stream
            resolvedStreamUrl = stream
        } else {
            // Embed URL fallback for WebView
            resolvedStreamUrl = s.embedUrl
        }
        resolving = false
    }



    // No auto-launch effect needed – we play directly inside the app via NativeStreamPlayer/WebView







    Box(



        Modifier



            .fillMaxSize()



            .background(Color.Black)



            .clickable { controlsVisible = !controlsVisible }



    ) {



        // ── 1. Video Player Surface (100% Fullscreen, Edge-to-Edge) ─────────



        val server = activeServer



        if (server == null) {



            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                Text("لا يوجد سيرفر تشغيل متاح", color = Color.White, fontSize = 14.sp)



            }



        } else if (resolving) {



            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {



                    CircularProgressIndicator(color = ZakzoukaColors.Gold)



                    Text("جاري تجهيز التشغيل...", color = ZakzoukaColors.GoldLight, fontSize = 12.sp)



                }



            }



        } else if (!resolvedStreamUrl.isNullOrBlank()) {

            // ── Direct stream URL → play with built-in ExoPlayer ────────────

            val isDirectVideo = resolvedStreamUrl!!.let { u ->

                u.contains(".m3u8", ignoreCase = true) ||

                u.contains(".mp4", ignoreCase = true) ||

                u.contains(".mkv", ignoreCase = true) ||

                u.contains(".ts", ignoreCase = true) ||

                (u.startsWith("http", ignoreCase = true) && !u.contains("embed", ignoreCase = true))

            }

            if (isDirectVideo) {

                NativeStreamPlayer(

                    streamUrl = resolvedStreamUrl!!,

                    referer = activeStreamReferer,

                    contentTitle = detail.item.title,

                    contentType = if (detail.item.type == ArabseedType.SERIES) "series" else "movie",

                    onError = {

                        // on ExoPlayer error fall back to WebView

                        playerPrompt = "تعذر تشغيل الفيديو. جرّب سيرفرًا آخر."

                        promptNeedsRetry = false

                    }

                )

            } else {

                // ── Embed/iframe URL → WebView fallback ─────────────────────

                AndroidView(

                    modifier = Modifier.fillMaxSize(),

                    factory = { ctx ->

                        android.webkit.WebView(ctx).apply {

                            setBackgroundColor(android.graphics.Color.BLACK)

                            settings.javaScriptEnabled = true

                            settings.domStorageEnabled = true

                            settings.databaseEnabled = true

                            settings.mediaPlaybackRequiresUserGesture = false

                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)

                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            settings.userAgentString = ArabseedRepository.getPlayerUserAgent()

                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            webChromeClient = object : android.webkit.WebChromeClient() {
                                override fun onCreateWindow(view: android.webkit.WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false
                            }

                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    if (ArabseedRepository.isAd(reqUrl)) return true
                                    if (reqUrl.startsWith("intent:") || reqUrl.startsWith("market:") || reqUrl.startsWith("whatsapp:")) return true
                                    return false
                                }
                                override fun shouldInterceptRequest(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): android.webkit.WebResourceResponse? {
                                    val reqUrl = request?.url?.toString() ?: return null
                                    if (ArabseedRepository.isAd(reqUrl)) {
                                        return android.webkit.WebResourceResponse("text/plain", "UTF-8", java.io.ByteArrayInputStream(ByteArray(0)))
                                    }
                                    return super.shouldInterceptRequest(view, request)
                                }
                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    view?.evaluateJavascript(
                                        """(function(){document.querySelectorAll('.div-over,.overlay,#modal,[id^="ad-"],[class^="ad-"],[id*="-ad-"],[class*="-ad-"],[id*="banner"],[class*="banner"],iframe[src*="ads"]').forEach(function(e){try{e.remove()}catch(x){}});var v=document.querySelector('video');if(v){v.muted=false;v.play();}})();""",
                                        null
                                    )
                                }
                            }

                            val refererHeader = try {

                                val u = java.net.URI.create(resolvedStreamUrl!!)

                                mapOf("Referer" to "${u.scheme}://${u.host}/")

                            } catch (_: Exception) { emptyMap() }

                            if (refererHeader.isNotEmpty()) {

                                loadUrl(resolvedStreamUrl!!, refererHeader)

                            } else {

                                loadUrl(resolvedStreamUrl!!)

                            }

                        }

                    }

                )

            }

        } else {

            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    Text("⚠️", fontSize = 32.sp)

                    Text(

                        "تعذر تجهيز رابط التشغيل.\nاختر سيرفرًا آخر من الأسفل.",

                        color = ZakzoukaColors.GoldLight,

                        fontSize = 14.sp,

                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,

                    )

                }

            }

        }



        // ── 2. Floating Animated Controls Overlay (Auto-hides after 4s) ──────



        AnimatedVisibility(



            visible = controlsVisible,



            enter = fadeIn(),



            exit = fadeOut(),



            modifier = Modifier.fillMaxSize()



        ) {



            Box(Modifier.fillMaxSize()) {



                // Top Floating Bar (Back button + Title)



                Row(



                    Modifier



                        .fillMaxWidth()



                        .align(Alignment.TopCenter)



                        .background(



                            Brush.verticalGradient(



                                listOf(Color(0xE6000000), Color(0x66000000), Color.Transparent)



                            )



                        )



                        .statusBarsPadding()



                        .padding(horizontal = 16.dp, vertical = 12.dp),



                    verticalAlignment = Alignment.CenterVertically,



                ) {



                    Box(



                        Modifier



                            .size(40.dp)



                            .clip(CircleShape)



                            .background(Color(0x66000000))



                            .border(1.dp, ZakzoukaColors.GoldBorder, CircleShape)



                            .clickable {



                                resetScreenOrientation(context)



                                onBack()



                            },



                        contentAlignment = Alignment.Center,



                    ) {



                        Text("←", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)



                    }



                    Spacer(Modifier.width(12.dp))



                    Text(



                        detail.item.title,



                        color = Color.White,



                        fontSize = 15.sp,



                        fontWeight = FontWeight.Bold,



                        maxLines = 1,



                        overflow = TextOverflow.Ellipsis,



                        modifier = Modifier.weight(1f)



                    )



                }







                // Bottom Floating Bar: Episode Navigation (Previous / Next Episode)



                if (hasPrev || hasNext) {



                    Row(



                        Modifier



                            .fillMaxWidth()



                            .align(Alignment.BottomCenter)



                            .background(



                                Brush.verticalGradient(



                                    listOf(Color.Transparent, Color(0x66000000), Color(0xE6000000))



                                )



                            )



                            .navigationBarsPadding()



                            .padding(horizontal = 24.dp, vertical = 14.dp),



                        horizontalArrangement = Arrangement.SpaceBetween,



                        verticalAlignment = Alignment.CenterVertically



                    ) {



                        if (hasPrev && onPrev != null) {



                            Row(



                                Modifier



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(Color(0xCC1A1F2C))



                                    .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp))



                                    .clickable(onClick = onPrev)



                                    .padding(horizontal = 16.dp, vertical = 10.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(8.dp)



                            ) {



                                Text("→", color = ZakzoukaColors.GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)



                                Text("الحلقة السابقة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)



                            }



                        } else {



                            Spacer(Modifier.width(1.dp))



                        }







                        if (hasNext && onNext != null) {



                            Row(



                                Modifier



                                    .clip(RoundedCornerShape(8.dp))



                                    .background(ZakzoukaColors.Gold)



                                    .clickable(onClick = onNext)



                                    .padding(horizontal = 16.dp, vertical = 10.dp),



                                verticalAlignment = Alignment.CenterVertically,



                                horizontalArrangement = Arrangement.spacedBy(8.dp)



                            ) {



                                Text("الحلقة التالية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)



                                Text("←", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)



                            }



                        } else {



                            Spacer(Modifier.width(1.dp))



                        }



                    }



                }



            }



        }



        if (controlsVisible && !resolvedStreamUrl.isNullOrBlank()) {

            androidx.compose.material3.Surface(

                modifier = Modifier.align(Alignment.TopStart).padding(16.dp),

                onClick = {

                    val stream = resolvedStreamUrl!!

                    val canOpen = stream.startsWith("http", ignoreCase = true)

                    if (!canOpen || !openMadarPlayer(context, stream, detail.item.title, activeStreamReferer)) {

                        promptNeedsRetry = true

                        playerPrompt = "مشغل زقزوقة غير مثبت. ثبّت تطبيق مشغل زقزوقة على الهاتف ثم اضغط إعادة المحاولة."

                    }

                },

                color = Color(0xDD252B3D),

                shape = RoundedCornerShape(14.dp),

            ) {

                Text(

                    "▶ فتح في مشغل زقزوقة",

                    color = Color.White,

                    fontSize = 13.sp,

                    fontWeight = FontWeight.SemiBold,

                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),

                )

            }

        }



        playerPrompt?.let { message ->

            androidx.compose.material3.AlertDialog(

                onDismissRequest = { playerPrompt = null },

                title = {

                    Text(if (promptNeedsRetry) "مشغل زقزوقة غير مثبت" else "تعذر تشغيل هذا السيرفر")

                },

                text = { Text(message) },

                confirmButton = {

                    androidx.compose.material3.TextButton(

                        onClick = {

                            if (promptNeedsRetry) {

                                val stream = resolvedStreamUrl

                                val opened = stream?.startsWith("https://", ignoreCase = true) == true

                                    && openMadarPlayer(context, stream, detail.item.title, activeStreamReferer)

                                if (opened) {

                                    playerPrompt = null

                                    promptNeedsRetry = false

                                } else {

                                    playerPrompt = "مشغل زقزوقة ما زال غير مثبت. ثبّته ثم اضغط إعادة المحاولة."

                                }

                            } else {

                                playerPrompt = null

                            }

                        },

                    ) {

                        Text(if (promptNeedsRetry) "إعادة المحاولة" else "حسنًا")

                    }

                },

                dismissButton = {

                    if (promptNeedsRetry) {

                        androidx.compose.material3.TextButton(onClick = { playerPrompt = null }) {

                            Text("إغلاق")

                        }

                    }

                },

            )

        }



    }



}



private fun openMadarPlayer(context: Context, streamUrl: String, title: String, referer: String = ""): Boolean {
    return PlayerLauncher.launchPlayer(
        context = context,
        streamUrl = streamUrl,
        title = title,
        userAgent = ArabseedRepository.getPlayerUserAgent(),
        referer = referer,
    )
}

private fun playerRefererForEmbed(embedUrl: String, pageReferer: String): String {
    val configuredReferer = ArabseedRepository.getPlayerHeaders().values.firstOrNull().orEmpty()
    val configuredHost = runCatching { java.net.URI.create(configuredReferer).host }.getOrNull()
    val embedHost = runCatching { java.net.URI.create(embedUrl).host }.getOrNull()
    return if (!configuredHost.isNullOrBlank() && embedHost.equals(configuredHost, ignoreCase = true)) {
        configuredReferer
    } else {
        pageReferer
    }
}


// ─────────────────────────────────────────────────────────────────────────────



// Native ExoPlayer Component



// ─────────────────────────────────────────────────────────────────────────────







@androidx.annotation.OptIn(UnstableApi::class)

@Composable

private fun NativeStreamPlayer(

    streamUrl: String,

    referer: String,

    contentTitle: String,

    contentType: String,

    onError: () -> Unit = {}

) {

    val context = LocalContext.current

    var isFullscreen by remember { mutableStateOf(false) }

    var showQualityDialog by remember { mutableStateOf(false) }

    var resizeModeIndex by remember { mutableStateOf(0) } // 0: Fit, 1: Zoom, 2: Fill

    var selectedQuality by remember { mutableStateOf(CompressionQuality.DATA_SAVER) }



    val trackSelector = remember {

        DefaultTrackSelector(context)

    }



    // Apply smart bandwidth & quality constraints dynamically

    LaunchedEffect(selectedQuality) {

        val builder = trackSelector.buildUponParameters()

        when (selectedQuality) {

            CompressionQuality.DATA_SAVER -> {

                trackSelector.parameters = builder

                    .setMaxVideoSize(1280, 720)

                    .setMaxVideoBitrate(1_000_000) // 1 Mbps max -> HD sharpness + 60% bandwidth savings

                    .build()

            }

            CompressionQuality.SUPER_SAVER -> {

                trackSelector.parameters = builder

                    .setMaxVideoSize(854, 480)

                    .setMaxVideoBitrate(500_000) // 500 kbps max -> Smooth play on weakest connections

                    .build()

            }

            CompressionQuality.FULL_HD -> {

                trackSelector.parameters = builder

                    .setMaxVideoSize(1920, 1080)

                    .setMaxVideoBitrate(Int.MAX_VALUE)

                    .build()

            }

            CompressionQuality.HIGH_HD -> {

                trackSelector.parameters = builder

                    .setMaxVideoSize(1280, 720)

                    .setMaxVideoBitrate(Int.MAX_VALUE)

                    .build()

            }

            CompressionQuality.MEDIUM_SD -> {

                trackSelector.parameters = builder

                    .setMaxVideoSize(640, 360)

                    .setMaxVideoBitrate(350_000)

                    .build()

            }

            CompressionQuality.AUTO -> {

                trackSelector.parameters = builder

                    .clearVideoSizeConstraints()

                    .setMaxVideoBitrate(Int.MAX_VALUE)

                    .build()

            }

        }

    }



    val exoPlayer = remember(streamUrl) {

        val dataSourceFactory = DefaultHttpDataSource.Factory()

            .setUserAgent(ArabseedRepository.getPlayerUserAgent())

            .setDefaultRequestProperties(mapOf("Referer" to referer))

            .setConnectTimeoutMs(8000)

            .setReadTimeoutMs(10000)

            .setAllowCrossProtocolRedirects(true)



        val mediaItem = MediaItem.fromUri(streamUrl)

        val mediaSource = if (streamUrl.contains(".m3u8")) {

            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

        } else {

            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)

        }



        // Smart data-saving load control (prevents excessive preloading of unwanted data)

        val loadControl = DefaultLoadControl.Builder()

            .setBufferDurationsMs(

                /* minBufferMs = */ 4_000,

                /* maxBufferMs = */ 18_000,

                /* bufferForPlaybackMs = */ 500,

                /* bufferForPlaybackAfterRebufferMs = */ 1_000

            )

            .setPrioritizeTimeOverSizeThresholds(true)

            .build()



        ExoPlayer.Builder(context)

            .setTrackSelector(trackSelector)

            .setLoadControl(loadControl)

            .build().apply {

                var playbackStartedLogged = false

                addListener(object : Player.Listener {

                    override fun onIsPlayingChanged(isPlaying: Boolean) {

                        if (isPlaying && !playbackStartedLogged) {

                            playbackStartedLogged = true

                            com.madar.tv.core.ZakzoukaTelemetry.trackPlaybackStarted(contentType, contentTitle)

                        }

                    }



                    override fun onPlayerError(error: PlaybackException) {

                        com.madar.tv.core.ZakzoukaTelemetry.trackPlaybackError(contentType)

                        onError()

                    }

                })

                setMediaSource(mediaSource)

                prepare()

                playWhenReady = true

            }

    }



    val lifecycleOwner = context.findLifecycleOwner()

    DisposableEffect(exoPlayer, lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_STOP) exoPlayer.pause()

            if (event == Lifecycle.Event.ON_RESUME) if (isFullscreen) exoPlayer.play()

        }

        lifecycleOwner?.lifecycle?.addObserver(observer)

        onDispose {

            lifecycleOwner?.lifecycle?.removeObserver(observer)

            if (isFullscreen) resetScreenOrientation(context)

            exoPlayer.release()

        }

    }



    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(

            modifier = Modifier.fillMaxSize(),

            factory = { ctx ->

                PlayerView(ctx).apply {

                    layoutParams = ViewGroup.LayoutParams(

                        ViewGroup.LayoutParams.MATCH_PARENT,

                        ViewGroup.LayoutParams.MATCH_PARENT,

                    )

                    keepScreenOn = true

                    useController = true

                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT

                    setFullscreenButtonClickListener { goFullscreen ->

                        isFullscreen = goFullscreen

                        if (goFullscreen) {

                            setScreenOrientation(context, true)

                        } else {

                            resetScreenOrientation(context)

                        }

                    }

                    player = exoPlayer

                }

            },

            update = { playerView ->

                playerView.player = exoPlayer

                playerView.resizeMode = when (resizeModeIndex) {

                    1 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM

                    2 -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL

                    else -> androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT

                }

            },

        )



        // Top Controls: Floating Quality & Compression Badge + Aspect Ratio

        Row(

            modifier = Modifier

                .fillMaxWidth()

                .statusBarsPadding()

                .padding(horizontal = 14.dp, vertical = 10.dp),

            horizontalArrangement = Arrangement.SpaceBetween,

            verticalAlignment = Alignment.CenterVertically

        ) {

            // Quality & Data Compression Button

            Row(

                modifier = Modifier

                    .clip(RoundedCornerShape(20.dp))

                    .background(Color.Black.copy(alpha = 0.72f))

                    .border(1.dp, if (selectedQuality.isDataSaver) Color(0xFF10B981) else ZakzoukaColors.Gold, RoundedCornerShape(20.dp))

                    .clickable { showQualityDialog = true }

                    .padding(horizontal = 12.dp, vertical = 6.dp),

                verticalAlignment = Alignment.CenterVertically,

                horizontalArrangement = Arrangement.spacedBy(6.dp)

            ) {

                Text(

                    text = if (selectedQuality.isDataSaver) "⚡" else "⚙️",

                    fontSize = 13.sp

                )

                Text(

                    text = selectedQuality.shortLabel,

                    color = Color.White,

                    fontSize = 12.sp,

                    fontWeight = FontWeight.Bold

                )

                if (selectedQuality.isDataSaver) {

                    Box(

                        modifier = Modifier

                            .clip(RoundedCornerShape(4.dp))

                            .background(Color(0xFF10B981))

                            .padding(horizontal = 5.dp, vertical = 1.dp)

                    ) {

                        Text("ضغط النت", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)

                    }

                }

            }



            // Aspect Ratio Toggle Button

            Box(

                modifier = Modifier

                    .clip(RoundedCornerShape(20.dp))

                    .background(Color.Black.copy(alpha = 0.72f))

                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(20.dp))

                    .clickable {

                        resizeModeIndex = (resizeModeIndex + 1) % 3

                    }

                    .padding(horizontal = 10.dp, vertical = 6.dp)

            ) {

                Text(

                    text = when (resizeModeIndex) {

                        1 -> "🔍 تكبير (Zoom)"

                        2 -> "↔ ملء الشاشة (Stretch)"

                        else -> "📺 أبعاد أصلية (Fit)"

                    },

                    color = Color.White,

                    fontSize = 11.sp,

                    fontWeight = FontWeight.Medium

                )

            }

        }



        // Quality & Compression Dialog

        if (showQualityDialog) {

            QualitySelectionDialog(

                current = selectedQuality,

                onSelect = { q ->

                    selectedQuality = q

                    showQualityDialog = false

                    Toast.makeText(context, "تم تفعيل: ${q.title}", Toast.LENGTH_SHORT).show()

                },

                onDismiss = { showQualityDialog = false }

            )

        }

    }

}



// ── Compression Quality Profile ──────────────────────────────────────────────

private enum class CompressionQuality(

    val title: String,

    val subtitle: String,

    val shortLabel: String,

    val isDataSaver: Boolean = false,

) {

    DATA_SAVER(

        title = "⚡ وضع ضغط النت الذكي (موصى به)",

        subtitle = "جودة 720p HD عالية الوضوح مع توفير حتى 60% من باقة النت",

        shortLabel = "ضغط ذكي (720p)",

        isDataSaver = true

    ),

    SUPER_SAVER(

        title = "📉 توفير فائق للنت الضعيف (480p)",

        subtitle = "جودة متوسطة تعمل بسلاسة تامة وبدون تقطيع على أضعف شبكة",

        shortLabel = "موفر فائق (480p)",

        isDataSaver = true

    ),

    FULL_HD(

        title = "💎 جودة فائقة كاملة (1080p Full HD)",

        subtitle = "أعلى وضوح شاشة متاح بدون ضغط (يتطلب اتصال سريع)",

        shortLabel = "1080p FHD",

        isDataSaver = false

    ),

    HIGH_HD(

        title = "🌟 جودة عالية (720p HD)",

        subtitle = "وضوح ممتاز بدون تقييد للبيانات",

        shortLabel = "720p HD",

        isDataSaver = false

    ),

    MEDIUM_SD(

        title = "📱 جودة خفيفة (360p)",

        subtitle = "استهلاك نت شبه منعدم للأجهزة الضعيفة",

        shortLabel = "360p SD",

        isDataSaver = true

    ),

    AUTO(

        title = "🔄 تلقائي (يتكيف مع سرعة النت)",

        subtitle = "يغير الجودة تلقائياً بحسب سرعة الاتصال لتجنب التوقف",

        shortLabel = "تلقائي (Auto)",

        isDataSaver = false

    )

}



@Composable

private fun QualitySelectionDialog(

    current: CompressionQuality,

    onSelect: (CompressionQuality) -> Unit,

    onDismiss: () -> Unit,

) {

    Box(

        modifier = Modifier

            .fillMaxSize()

            .background(Color.Black.copy(alpha = 0.75f))

            .clickable(onClick = onDismiss),

        contentAlignment = Alignment.Center

    ) {

        Column(

            modifier = Modifier

                .fillMaxWidth(0.92f)

                .clip(RoundedCornerShape(16.dp))

                .background(Color(0xFF1E293B))

                .border(1.dp, ZakzoukaColors.Gold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))

                .clickable(enabled = false) {}

                .padding(18.dp),

            verticalArrangement = Arrangement.spacedBy(10.dp)

        ) {

            Row(

                modifier = Modifier.fillMaxWidth(),

                horizontalArrangement = Arrangement.SpaceBetween,

                verticalAlignment = Alignment.CenterVertically

            ) {

                Column {

                    Text(

                        "تحكم الجودة وضغط استهلاك النت ⚡",

                        color = Color.White,

                        fontSize = 15.sp,

                        fontWeight = FontWeight.Bold

                    )

                    Text(

                        "شاهد بأعلى جودة مع تقليل استهلاك باقة النت لأقصى درجة",

                        color = ZakzoukaColors.TextMuted,

                        fontSize = 11.sp

                    )

                }



                Box(

                    modifier = Modifier

                        .size(30.dp)

                        .clip(CircleShape)

                        .background(Color.White.copy(alpha = 0.1f))

                        .clickable(onClick = onDismiss),

                    contentAlignment = Alignment.Center

                ) {

                    Text("✕", color = Color.White, fontSize = 14.sp)

                }

            }



            Spacer(Modifier.height(4.dp))



            CompressionQuality.values().forEach { quality ->

                val isSelected = quality == current

                Row(

                    modifier = Modifier

                        .fillMaxWidth()

                        .clip(RoundedCornerShape(10.dp))

                        .background(

                            if (isSelected) Color(0xFF0F172A)

                            else Color(0xFF334155).copy(alpha = 0.45f)

                        )

                        .border(

                            1.dp,

                            if (isSelected) Color(0xFF10B981) else Color.Transparent,

                            RoundedCornerShape(10.dp)

                        )

                        .clickable { onSelect(quality) }

                        .padding(horizontal = 12.dp, vertical = 10.dp),

                    verticalAlignment = Alignment.CenterVertically,

                    horizontalArrangement = Arrangement.SpaceBetween

                ) {

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                            Text(

                                text = quality.title,

                                color = if (isSelected) Color(0xFF10B981) else Color.White,

                                fontSize = 13.sp,

                                fontWeight = FontWeight.Bold

                            )

                            if (quality.isDataSaver) {

                                Box(

                                    modifier = Modifier

                                        .clip(RoundedCornerShape(4.dp))

                                        .background(Color(0xFF10B981).copy(alpha = 0.2f))

                                        .border(1.dp, Color(0xFF10B981), RoundedCornerShape(4.dp))

                                        .padding(horizontal = 5.dp, vertical = 1.dp)

                                ) {

                                    Text("توفير 60% نت", color = Color(0xFF10B981), fontSize = 9.sp, fontWeight = FontWeight.Bold)

                                }

                            }

                        }

                        Text(

                            text = quality.subtitle,

                            color = Color(0xFF94A3B8),

                            fontSize = 10.sp

                        )

                    }



                    if (isSelected) {

                        Text("✓", color = Color(0xFF10B981), fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    }

                }

            }

        }

    }

}



// Zakzouka Search Screen



// ─────────────────────────────────────────────────────────────────────────────







@Composable



private fun ZakzoukaSearchScreen(



    query: String,



    onQueryChange: (String) -> Unit,



    onBack: () -> Unit,



    onOpen: (ArabseedItem) -> Unit,



) {



    var results by remember { mutableStateOf(emptyList<ArabseedItem>()) }



    var loading by remember { mutableStateOf(false) }



    var error by remember { mutableStateOf<String?>(null) }



    var lastQuery by remember { mutableStateOf("") }







    LaunchedEffect(query) {



        val trimmed = query.trim()



        if (trimmed.length < 2) {



            results = emptyList()



            loading = false



            error = null



            return@LaunchedEffect



        }



        if (trimmed == lastQuery) return@LaunchedEffect
        delay(350)
        lastQuery = trimmed
        error = null

        val cachedResults = withContext(Dispatchers.IO) {
            ArabseedRepository.loadCachedSearch(trimmed).orEmpty() +
                MultiProviderRepository.loadCachedSearch(trimmed).orEmpty()
        }
        if (cachedResults.isNotEmpty()) {
            results = withContext(Dispatchers.Default) {
                MultiProviderRepository.mergeCatalogItems(cachedResults)
            }
            loading = false
        } else {
            results = emptyList()
            loading = true
        }

        try {
            com.madar.tv.core.ZakzoukaTelemetry.trackSearchQuery(trimmed)
            var sourceResults = emptyList<ArabseedItem>()
            var providerResults = emptyList<ArabseedItem>()
            val completed = kotlinx.coroutines.channels.Channel<Pair<Boolean, List<ArabseedItem>>>(2)
            val sourceJob = async(Dispatchers.IO) {
                val found = runCatching { ArabseedRepository.search(trimmed, forceRefresh = false) }
                    .getOrDefault(emptyList())
                completed.send(true to found)
            }
            val providersJob = async(Dispatchers.IO) {
                val found = runCatching { MultiProviderRepository.searchAll(trimmed, forceRefresh = false) }
                    .getOrDefault(emptyList())
                completed.send(false to found)
            }
            repeat(2) {
                val (isSource, found) = completed.receive()
                if (isSource) sourceResults = found else providerResults = found
                val available = sourceResults + providerResults
                if (available.isNotEmpty()) {
                    val previousResults = results
                    results = withContext(Dispatchers.Default) {
                        MultiProviderRepository.mergeCatalogItems(previousResults + available)
                    }
                    loading = false
                }
            }
            sourceJob.await()
            providersJob.await()
            completed.close()
            if (results.isEmpty()) error = "لم نجد نتائج مطابقة."
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            if (results.isEmpty()) error = "تعذر البحث. تأكد من الاتصال."
        } finally {
            loading = false
        }
    }







    Column(Modifier.fillMaxSize().background(ZakzoukaColors.Background)) {



        Row(



            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),



            verticalAlignment = Alignment.CenterVertically,



        ) {



            Box(



                Modifier.size(38.dp).clip(CircleShape).background(ZakzoukaColors.SurfaceCard).border(1.dp, ZakzoukaColors.GoldBorder, CircleShape).clickable(onClick = onBack),



                contentAlignment = Alignment.Center,



            ) {



                Text("←", color = ZakzoukaColors.TextWhite, fontSize = 18.sp)



            }



            Spacer(Modifier.width(10.dp))



            androidx.compose.foundation.text.BasicTextField(



                value = query,



                onValueChange = onQueryChange,



                modifier = Modifier.weight(1f).height(42.dp)



                    .clip(RoundedCornerShape(8.dp))



                    .background(ZakzoukaColors.SurfaceCard)



                    .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(8.dp))



                    .padding(horizontal = 14.dp),



                singleLine = true,



                textStyle = androidx.compose.ui.text.TextStyle(color = ZakzoukaColors.TextWhite, fontSize = 13.sp),



                decorationBox = { inner ->



                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {



                        if (query.isEmpty()) {



                            Text("ابحث عن أي فيلم أو مسلسل...", color = ZakzoukaColors.TextMuted, fontSize = 12.sp)



                        }



                        inner()



                    }



                },



            )



        }







        when {



            query.trim().length < 2 -> {



                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {



                        Text("🔍", fontSize = 36.sp)



                        Text("اكتب اسم الفيلم أو المسلسل للبحث", color = ZakzoukaColors.TextMuted, fontSize = 13.sp)



                    }



                }



            }



            loading -> {



                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                    CircularProgressIndicator(color = ZakzoukaColors.Gold)



                }



            }



            error != null -> {



                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                    Text(error!!, color = ZakzoukaColors.TextMuted, fontSize = 13.sp)



                }



            }



            results.isEmpty() -> {



                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {



                    Text("لا توجد نتائج مطابقة لـ \"$query\"", color = ZakzoukaColors.TextMuted, fontSize = 13.sp)



                }



            }



            else -> {



                LazyVerticalGrid(



                    columns = GridCells.Fixed(3),



                    modifier = Modifier.fillMaxSize(),



                    contentPadding = PaddingValues(14.dp),



                    horizontalArrangement = Arrangement.spacedBy(10.dp),



                    verticalArrangement = Arrangement.spacedBy(12.dp),



                ) {



                    items(results, key = { it.pageUrl }) { item ->



                        ZakzoukaCard(item = item, onClick = { onOpen(item) })



                    }



                }



            }



        }



    }



}







@Composable



private fun PaddingBox(content: @Composable () -> Unit) {



    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {



        content()



    }



}







private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {



    is LifecycleOwner -> this



    is android.content.ContextWrapper -> baseContext.findLifecycleOwner()



    else -> null



}
