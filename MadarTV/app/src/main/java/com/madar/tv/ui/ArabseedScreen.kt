package com.madar.tv.ui

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
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
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.madar.tv.R
import com.madar.tv.data.*
import com.madar.tv.ui.theme.MashadColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CinemaSection(
    onSearch: () -> Unit,
    onDeepScreen: (Boolean) -> Unit = {},
) {
    var selectedMainTab by rememberSaveable { mutableStateOf("home") }
    var selectedCategoryPath by rememberSaveable { mutableStateOf("home/") }
    var selectedCategoryTitle by rememberSaveable { mutableStateOf("الرئيسية") }

    var openedItem by rememberSaveable { mutableStateOf<String?>(null) }
    var openedDetail by rememberSaveable { mutableStateOf<ArabseedDetail?>(null) }
    var searchMode by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    var allItems by remember { mutableStateOf(emptyList<ArabseedItem>()) }
    var activeEpisodes by remember { mutableStateOf<List<ArabseedEpisode>>(emptyList()) }
    var currentEpisodeIndex by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()

    val currentItem = remember(openedItem, allItems) {
        allItems.firstOrNull { it.pageUrl == openedItem }
    }
    val detail = openedDetail

    val isDeep = detail != null || currentItem != null
    LaunchedEffect(isDeep) { onDeepScreen(isDeep) }

    Box(Modifier.fillMaxSize().background(MashadColors.Background)) {
        when {
            detail != null -> {
                MashadPlayerScreen(
                    detail = detail,
                    onBack = { openedDetail = null },
                    hasPrev = currentEpisodeIndex > 0,
                    hasNext = currentEpisodeIndex in 0 until activeEpisodes.size - 1,
                    onPrev = {
                        if (currentEpisodeIndex > 0) {
                            val prevIdx = currentEpisodeIndex - 1
                            val prevEp = activeEpisodes[prevIdx]
                            currentEpisodeIndex = prevIdx
                            scope.launch {
                                val srvs = withContext(Dispatchers.IO) {
                                    ArabseedRepository.fetchEpisodeServers(prevEp.pageUrl)
                                }
                                if (srvs.isNotEmpty()) {
                                    val preRes = withContext(Dispatchers.IO) {
                                        ArabseedRepository.resolveDirectStream(srvs.first().embedUrl)
                                    }
                                    val newItem = detail.item.copy(
                                        id = prevEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                        title = prevEp.title,
                                        arabicTitle = prevEp.title,
                                        pageUrl = prevEp.pageUrl,
                                    )
                                    openedDetail = ArabseedDetail(newItem, srvs, preRes)
                                }
                            }
                        }
                    },
                    onNext = {
                        if (currentEpisodeIndex in 0 until activeEpisodes.size - 1) {
                            val nextIdx = currentEpisodeIndex + 1
                            val nextEp = activeEpisodes[nextIdx]
                            currentEpisodeIndex = nextIdx
                            scope.launch {
                                val srvs = withContext(Dispatchers.IO) {
                                    ArabseedRepository.fetchEpisodeServers(nextEp.pageUrl)
                                }
                                if (srvs.isNotEmpty()) {
                                    val preRes = withContext(Dispatchers.IO) {
                                        ArabseedRepository.resolveDirectStream(srvs.first().embedUrl)
                                    }
                                    val newItem = detail.item.copy(
                                        id = nextEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                        title = nextEp.title,
                                        arabicTitle = nextEp.title,
                                        pageUrl = nextEp.pageUrl,
                                    )
                                    openedDetail = ArabseedDetail(newItem, srvs, preRes)
                                }
                            }
                        }
                    }
                )
            }

            currentItem != null -> {
                MashadDetailScreen(
                    item = currentItem,
                    onBack = { openedItem = null },
                    onPlay = { d, eps, epIdx ->
                        openedDetail = d
                        activeEpisodes = eps
                        currentEpisodeIndex = epIdx
                    },
                    onOpenItem = { newItem ->
                        allItems = (allItems + newItem).distinctBy { it.pageUrl }
                        openedItem = newItem.pageUrl
                    },
                )
            }

            searchMode -> {
                MashadSearchScreen(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onBack = { searchMode = false; searchQuery = "" },
                    onOpen = { item ->
                        com.madar.tv.core.MashadTelemetry.trackContentView(
                            title = item.title,
                            type = item.type.name,
                            category = item.category,
                            url = item.pageUrl
                        )
                        allItems = (allItems + item).distinctBy { it.pageUrl }
                        openedItem = item.pageUrl
                        searchMode = false
                    },
                )
            }

            else -> {
                MashadMainScreen(
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
                        com.madar.tv.core.MashadTelemetry.trackContentView(
                            title = item.title,
                            type = item.type.name,
                            category = item.category,
                            url = item.pageUrl
                        )
                        allItems = (allItems + item).distinctBy { it.pageUrl }
                        openedItem = item.pageUrl
                    },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Main Mashad Screen (Movies, Series, Football Matches, Auto-Updates)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadMainScreen(
    selectedTab: String,
    currentCategoryPath: String,
    currentCategoryTitle: String,
    onTabSelect: (ArabCategory) -> Unit,
    onCategorySelect: (ArabCategory) -> Unit,
    onSearch: () -> Unit,
    onOpen: (ArabseedItem) -> Unit,
) {
    var page by remember(currentCategoryPath) { mutableStateOf(1) }
    var items by remember(currentCategoryPath) { mutableStateOf(emptyList<ArabseedItem>()) }
    var loading by remember(currentCategoryPath, page) { mutableStateOf(true) }
    var error by remember(currentCategoryPath) { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    // Auto-updating live movies ticker: re-fetch in background every 4 minutes
    LaunchedEffect(currentCategoryPath) {
        while (true) {
            delay(4 * 60 * 1000L)
            try {
                val liveLatest = withContext(Dispatchers.IO) {
                    ArabseedRepository.fetchCategory(currentCategoryPath, 1)
                }
                if (liveLatest.isNotEmpty()) {
                    items = (liveLatest + items).distinctBy { it.pageUrl }
                }
            } catch (_: Exception) {}
        }
    }

    // Live Fetch Movies for Current Category
    LaunchedEffect(currentCategoryPath, page, reloadKey) {
        error = null
        loading = true
        try {
            val fetched = withContext(Dispatchers.IO) {
                ArabseedRepository.fetchCategory(currentCategoryPath, page, forceRefresh = reloadKey > 0)
            }
            items = if (page == 1) fetched else (items + fetched).distinctBy { it.pageUrl }
        } catch (e: Exception) {
            if (items.isEmpty()) error = "تعذر تحميل المحتوى. تأكد من الاتصال وحاول مجدداً."
        } finally {
            loading = false
        }
    }

    val featuredItem = remember(items) { items.firstOrNull() }

    // Prefetch top items in background for instant 0.0s opening
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                items.take(4).forEach { item ->
                    try { ArabseedRepository.prefetch(item) } catch (_: Exception) {}
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // 1. Mashad Top Header with Logo & Auto-update badge & Search & Refresh
        MashadHeader(
            onSearch = onSearch,
            onRefresh = {
                ArabseedRepository.clearListingCache()
                reloadKey++
            },
        )

        // 2. Main Tabs (الرئيسية, المسلسلات, الافلام, انمي, رمضان, منوعات)
        MashadMainTabs(
            selectedTabId = selectedTab,
            onSelect = onTabSelect,
        )

        // 3. Sub-categories chips
        MashadSubCategoryBar(
            selectedTabId = selectedTab,
            currentPath = currentCategoryPath,
            onSelect = onCategorySelect,
        )

        // 4. Content Area
        when {
            loading && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MashadColors.Gold)
                }
            }

            error != null && items.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error!!, color = MashadColors.TextMuted, fontSize = 13.sp)
                        Text(
                            "إعادة المحاولة",
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(MashadColors.Gold)
                                .clickable { reloadKey++ }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    // Hero Featured Item (Luxury style)
                    if (featuredItem != null && page == 1 && (selectedTab == "home" || currentCategoryPath == "home/")) {
                        item {
                            MashadHeroBanner(
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
                                            .background(MashadColors.Gold)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = currentCategoryTitle,
                                        color = MashadColors.TextWhite,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                Box(
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                        .background(MashadColors.GoldBorder)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "مُحدث تلقائياً ⚡",
                                        color = MashadColors.GoldLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }

                    // Grid of Items
                    val chunked = items.chunked(3)
                    items(chunked) { rowItems ->
                        PaddingBox {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                for (item in rowItems) {
                                    Box(Modifier.weight(1f)) {
                                        MashadCard(item = item, onClick = { onOpen(item) })
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
                                    .background(MashadColors.SurfaceCard)
                                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp))
                                    .clickable { if (!loading) page++ }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (loading) {
                                    CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        "تحميل المزيد من الأعمال ↓",
                                        color = MashadColors.GoldLight,
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
// Mashad Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadHeader(
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
                painter = painterResource(id = R.drawable.mashad_logo),
                contentDescription = "مشهد",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.5.dp, MashadColors.Gold, CircleShape),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "مَـشْـهَـد",
                color = MashadColors.GoldLight,
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
                    .background(MashadColors.SurfaceCard)
                    .border(1.dp, MashadColors.GoldBorder, CircleShape)
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
                    .background(MashadColors.SurfaceCard)
                    .border(1.dp, MashadColors.GoldBorder, CircleShape)
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
private fun MashadMainTabs(
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
            Column(
                Modifier.clickable { onSelect(tab) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tab.title,
                    color = if (active) MashadColors.GoldLight else MashadColors.TextMuted,
                    fontSize = 14.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 6.dp),
                )
                if (active) {
                    Box(
                        Modifier.height(2.5.dp).width(28.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MashadColors.Gold)
                    )
                } else {
                    Spacer(Modifier.height(2.5.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sub-Category Chips Bar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadSubCategoryBar(
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
            Box(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(if (active) MashadColors.Gold else MashadColors.SurfaceCard)
                    .border(1.dp, if (active) MashadColors.Gold else MashadColors.Border, RoundedCornerShape(20.dp))
                .clickable { onSelect(cat) }
                .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    text = cat.title,
                    color = if (active) Color.Black else MashadColors.TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mashad Hero Featured Banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadHeroBanner(
    item: ArabseedItem,
    onPlay: () -> Unit,
    onInfo: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().height(330.dp)
            .padding(horizontal = 14.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MashadColors.Surface),
    ) {
        if (item.posterUrl != null) {
            AsyncImage(
                model = item.posterUrl,
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        // Gradient overlay fading to dark at bottom
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0x66000000),
                        Color(0xE60C0D12),
                        MashadColors.Background,
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
                        Modifier.clip(RoundedCornerShape(4.dp)).background(MashadColors.Gold).padding(horizontal = 6.dp, vertical = 2.dp),
                        color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    item.category,
                    Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MashadColors.TextWhite, fontSize = 9.sp,
                )
                if (item.year.isNotBlank()) {
                    Text(
                        item.year,
                        Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0x99000000)).padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MashadColors.TextMuted, fontSize = 9.sp,
                    )
                }
            }

            Text(
                item.title,
                color = MashadColors.TextWhite,
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
                        .background(MashadColors.Gold)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 22.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("▶", color = Color.Black, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("مشاهدة", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Info button
                Row(
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(MashadColors.SurfaceHighlight)
                        .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(6.dp))
                        .clickable(onClick = onInfo)
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("ℹ️", fontSize = 12.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("تفاصيل", color = MashadColors.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mashad Card Item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadCard(item: ArabseedItem, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(8.dp))
                .background(MashadColors.Surface)
                .border(1.dp, MashadColors.Border, RoundedCornerShape(8.dp)),
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
                        .background(MashadColors.Gold)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        item.quality,
                        color = Color.Black,
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
                    color = MashadColors.TextMuted,
                    fontSize = 9.sp,
                )
            }
        }

        Text(
            text = item.title,
            color = MashadColors.TextWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
        )

        if (item.year.isNotBlank() || item.category.isNotBlank()) {
            Text(
                text = "${item.year} · ${item.category}".trim(' ', '·'),
                color = MashadColors.GoldLight,
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
private fun MashadDetailScreen(
    item: ArabseedItem,
    onBack: () -> Unit,
    onPlay: (ArabseedDetail, List<ArabseedEpisode>, Int) -> Unit,
    onOpenItem: ((ArabseedItem) -> Unit)? = null,
) {
    val cachedDetail = remember(item.pageUrl) { ArabseedRepository.getCachedDetail(item.pageUrl) }
    var detail by remember(item.pageUrl) { mutableStateOf(cachedDetail) }
    var fullStory by remember(item.pageUrl) { mutableStateOf(cachedDetail?.item?.overview ?: "") }
    var loadingDetail by remember(item.pageUrl) { mutableStateOf(cachedDetail == null) }
    var preResolvedStream by remember(item.pageUrl) { mutableStateOf(cachedDetail?.preResolvedStream) }

    // Seasons, Episodes & Related Parts state — ALWAYS fetched for all items!
    var seasonBundle by remember(item.pageUrl) { mutableStateOf<ArabseedSeasonBundle?>(null) }
    var loadingSeasons by remember(item.pageUrl) { mutableStateOf(true) }
    var selectedSeason by remember(item.pageUrl) { mutableStateOf(0) }

    LaunchedEffect(item.pageUrl) {
        if (cachedDetail != null) {
            loadingDetail = false
        } else {
            loadingDetail = true
            try {
                val storyJob = async(Dispatchers.IO) { ArabseedRepository.fetchStoryFast(item.pageUrl) }
                val detailJob = async(Dispatchers.IO) { ArabseedRepository.fetchDetail(item) }
                val s = storyJob.await()
                if (s.isNotBlank()) fullStory = s
                val d = detailJob.await()
                detail = d
                if (fullStory.isBlank() && d.item.overview.isNotBlank()) fullStory = d.item.overview
                if (d.preResolvedStream != null) preResolvedStream = d.preResolvedStream
            } catch (_: Exception) {
            } finally {
                loadingDetail = false
            }
        }

        // Always load seasons, episodes, and other parts!
        loadingSeasons = true
        try {
            val bundle = withContext(Dispatchers.IO) {
                ArabseedRepository.fetchSeasonsAndEpisodes(item)
            }
            seasonBundle = bundle
        } catch (_: Exception) {
        } finally {
            loadingSeasons = false
        }
    }

    val displayOverview = fullStory.ifBlank { item.overview }
    val servers = detail?.servers ?: emptyList()
    val bundle = seasonBundle
    val seasons = bundle?.seasons ?: emptyList()
    val currentSeason = seasons.getOrNull(selectedSeason)
    val allEpisodes = seasons.flatMap { it.episodes }
    val relatedParts = bundle?.relatedParts ?: emptyList()

    // Determine current episode index if item matches an episode in the list
    val currentEpIdx = remember(item.pageUrl, allEpisodes) {
        allEpisodes.indexOfFirst {
            it.pageUrl == item.pageUrl || it.title.trim() == item.title.trim()
        }
    }
    val hasPrevEp = currentEpIdx > 0
    val hasNextEp = currentEpIdx in 0 until (allEpisodes.size - 1)

    Column(Modifier.fillMaxSize().background(MashadColors.Background)) {
        // Top Back Row
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(MashadColors.SurfaceCard)
                    .border(1.dp, MashadColors.GoldBorder, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", color = Color.White, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Text(
                item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. Poster + Meta + Play button ──────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Box(
                        Modifier.size(width = 115.dp, height = 165.dp)
                            .clip(RoundedCornerShape(8.dp)).background(MashadColors.Surface)
                            .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp)),
                    ) {
                        if (item.posterUrl != null) {
                            AsyncImage(model = item.posterUrl, contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(item.title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(
                            buildString {
                                if (item.year.isNotBlank()) append(item.year)
                                if (item.quality.isNotBlank()) append(" · ${item.quality}")
                                append(" · ${if (item.type == ArabseedType.MOVIE) "فيلم" else "مسلسل"}")
                                if (seasons.isNotEmpty()) append(" · ${seasons.size} مواسم")
                                if (allEpisodes.isNotEmpty()) append(" · ${allEpisodes.size} حلقة")
                            },
                            color = MashadColors.TextMuted, fontSize = 11.sp,
                        )
                        Text(item.category, color = MashadColors.GoldLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))

                        // Play button
                        val playLabel = when {
                            currentEpIdx >= 0 -> "▶  تشغيل هذه الحلقة"
                            allEpisodes.isNotEmpty() -> "▶  تشغيل الحلقة الأولى"
                            else -> "▶  تشغيل الآن"
                        }

                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                                .background(if (loadingDetail && servers.isEmpty() && allEpisodes.isEmpty()) MashadColors.SurfaceHighlight else MashadColors.Gold)
                                .clickable {
                                    if (servers.isNotEmpty()) {
                                        val epIdx = if (currentEpIdx >= 0) currentEpIdx else 0
                                        onPlay(
                                            (detail ?: ArabseedDetail(item, servers)).copy(preResolvedStream = preResolvedStream),
                                            allEpisodes,
                                            epIdx
                                        )
                                    }
                                }
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (loadingDetail && servers.isEmpty()) {
                                CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("جاري تحضير البث...", color = Color.Black, fontSize = 13.sp)
                            } else {
                                Text(playLabel, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── 2. Full Story ──────────────────────────────────────────────────
            if (displayOverview.isNotBlank() || loadingDetail) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                            .background(MashadColors.Surface)
                            .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("القصة كاملة", color = MashadColors.GoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            if (loadingDetail && fullStory.isBlank()) {
                                CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(13.dp), strokeWidth = 1.5.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("جاري جلب القصة...", color = MashadColors.TextMuted, fontSize = 10.sp)
                            }
                        }
                        if (displayOverview.isNotBlank()) {
                            Text(text = displayOverview, color = Color(0xFFDDDDDD), fontSize = 13.sp, lineHeight = 22.sp)
                        }
                    }
                }
            }

            // ── 3. Streaming Servers (ALWAYS SHOWN at the top!) ────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("اختر سيرفر المشاهدة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (loadingDetail && servers.isEmpty()) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("جاري استخراج السيرفرات السريعة...", color = MashadColors.TextMuted, fontSize = 12.sp)
                        }
                    } else if (servers.isEmpty()) {
                        Text("لا توجد سيرفرات متاحة حالياً", color = MashadColors.TextMuted, fontSize = 12.sp)
                    } else {
                        servers.forEachIndexed { index, server ->
                            val isFirst = index == 0
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                    .background(MashadColors.Surface)
                                    .border(1.dp, if (isFirst) MashadColors.Gold else MashadColors.Border, RoundedCornerShape(8.dp))
                                    .clickable {
                                        val targetDetail = (detail ?: ArabseedDetail(item, servers))
                                            .copy(
                                                servers = listOf(server) + (servers - server),
                                                preResolvedStream = if (isFirst) preResolvedStream else null,
                                            )
                                        val epIdx = if (currentEpIdx >= 0) currentEpIdx else 0
                                        onPlay(targetDetail, allEpisodes, epIdx)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("▶", color = MashadColors.Gold, fontSize = 12.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(server.name, color = Color.White, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Text(if (isFirst) "فائق السرعة ⚡" else "سريع",
                                    color = MashadColors.LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                            .background(MashadColors.Surface)
                            .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎬 الأجزاء والمواسم الأخرى", color = MashadColors.GoldLight, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            if (loadingSeasons && relatedParts.isEmpty()) {
                                CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("جاري فحص الأجزاء...", color = MashadColors.TextMuted, fontSize = 10.sp)
                            } else if (relatedParts.isNotEmpty()) {
                                Text("${relatedParts.size} جزء/موسم", color = MashadColors.TextMuted, fontSize = 11.sp)
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
                                                .background(MashadColors.SurfaceCard)
                                                .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp)),
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
                                                Text("اضغط للفتح", color = MashadColors.GoldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            part.title, color = Color.White, fontSize = 10.sp,
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

            // ── 5. SEASONS & EPISODES of Current Part ───────────────────────────
            if (loadingSeasons || seasons.isNotEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .background(MashadColors.Surface)
                            .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📺 حلقات هذا الجزء / الموسم", color = MashadColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.weight(1f))
                            if (loadingSeasons && seasons.isEmpty()) {
                                CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else if (allEpisodes.isNotEmpty()) {
                                Text(
                                    "${allEpisodes.size} حلقة",
                                    color = MashadColors.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (loadingSeasons && seasons.isEmpty()) {
                            Text("جاري استخراج الحلقات...", color = MashadColors.TextMuted, fontSize = 12.sp)
                        } else if (seasons.isEmpty()) {
                            Text("لا توجد حلقات إضافية في هذا العمل", color = MashadColors.TextMuted, fontSize = 12.sp)
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
                                                .background(if (active) MashadColors.Gold else MashadColors.SurfaceCard)
                                                .border(1.dp, if (active) MashadColors.Gold else MashadColors.Border, RoundedCornerShape(20.dp))
                                                .clickable { selectedSeason = idx }
                                                .padding(horizontal = 16.dp, vertical = 7.dp),
                                        ) {
                                            Text(
                                                seasons[idx].title,
                                                color = if (active) Color.Black else MashadColors.TextMuted,
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
                                        val prevEp = allEpisodes[currentEpIdx - 1]
                                        Row(
                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .background(MashadColors.SurfaceCard)
                                                .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    val epItem = ArabseedItem(
                                                        id = prevEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                                        title = prevEp.title, arabicTitle = prevEp.title,
                                                        posterUrl = item.posterUrl, year = item.year,
                                                        quality = item.quality, rating = item.rating,
                                                        type = ArabseedType.SERIES, pageUrl = prevEp.pageUrl,
                                                        category = item.category,
                                                    )
                                                    onOpenItem?.invoke(epItem)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text("→ الحلقة السابقة (${prevEp.number})", color = MashadColors.TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }

                                    if (hasNextEp) {
                                        val nextEp = allEpisodes[currentEpIdx + 1]
                                        Row(
                                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                .background(MashadColors.Gold)
                                                .clickable {
                                                    val epItem = ArabseedItem(
                                                        id = nextEp.pageUrl.trimEnd('/').substringAfterLast('/'),
                                                        title = nextEp.title, arabicTitle = nextEp.title,
                                                        posterUrl = item.posterUrl, year = item.year,
                                                        quality = item.quality, rating = item.rating,
                                                        type = ArabseedType.SERIES, pageUrl = nextEp.pageUrl,
                                                        category = item.category,
                                                    )
                                                    onOpenItem?.invoke(epItem)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Text("الحلقة التالية (${nextEp.number}) ←", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }

                            // Episodes Grid
                            val episodes = currentSeason?.episodes ?: emptyList()
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                episodes.chunked(2).forEach { rowEps ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        rowEps.forEach { ep ->
                                            val isCurrent = (ep.pageUrl == item.pageUrl || ep.title.trim() == item.title.trim())
                                            Box(
                                                Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                                                    .background(if (isCurrent) Color(0xFF242215) else MashadColors.SurfaceCard)
                                                    .border(1.dp, if (isCurrent) MashadColors.Gold else MashadColors.Border, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        val epItem = ArabseedItem(
                                                            id = ep.pageUrl.trimEnd('/').substringAfterLast('/'),
                                                            title = ep.title,
                                                            arabicTitle = ep.title,
                                                            posterUrl = item.posterUrl,
                                                            year = item.year, quality = item.quality, rating = item.rating,
                                                            type = ArabseedType.SERIES,
                                                            pageUrl = ep.pageUrl,
                                                            category = item.category,
                                                        )
                                                        onOpenItem?.invoke(epItem)
                                                    }
                                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    Box(
                                                        Modifier.size(30.dp).clip(CircleShape)
                                                            .background(if (isCurrent) MashadColors.GoldLight else MashadColors.Gold),
                                                        contentAlignment = Alignment.Center,
                                                    ) {
                                                        Text(
                                                            ep.number.toString(),
                                                            color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Black,
                                                        )
                                                    }
                                                    Column(Modifier.weight(1f)) {
                                                        Text(
                                                            ep.title.take(30),
                                                            color = if (isCurrent) MashadColors.GoldLight else Color.White,
                                                            fontSize = 11.sp,
                                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                        )
                                                        if (isCurrent) {
                                                            Text("الحلقة الحالية 🔴", color = MashadColors.Gold, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        if (rowEps.size == 1) Spacer(Modifier.weight(1f))
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun MashadPlayerScreen(
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
        }
        resolvedStreamUrl = stream
        resolving = false
    }

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
                    CircularProgressIndicator(color = MashadColors.Gold)
                    Text("جاري تشغيل البث بأقصى سرعة...", color = MashadColors.GoldLight, fontSize = 12.sp)
                }
            }
        } else if (resolvedStreamUrl != null) {
            // 🚀 Native ExoPlayer: 100% ad-free, instant, ultra-fast!
            NativeStreamPlayer(
                streamUrl = resolvedStreamUrl!!,
                onError = {
                    resolvedStreamUrl = null
                }
            )
        } else {
            // 🌐 Clean Web Embed Fallback (Forced Fullscreen via CSS/JS injection)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        keepScreenOn = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            @Suppress("DEPRECATION")
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?): Boolean = false
                            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean { result?.confirm(); return true }
                            override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: android.webkit.JsResult?): Boolean { result?.confirm(); return true }
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                if (newProgress > 50) {
                                    view?.evaluateJavascript("""
                                        (function(){
                                            var style = document.getElementById('mashad_fs_css');
                                            if (!style) {
                                                style = document.createElement('style');
                                                style.id = 'mashad_fs_css';
                                                style.innerHTML = `
                                                    * { box-sizing: border-box !important; }
                                                    html, body {
                                                        width: 100vw !important;
                                                        height: 100vh !important;
                                                        margin: 0 !important;
                                                        padding: 0 !important;
                                                        overflow: hidden !important;
                                                        background: #000 !important;
                                                    }
                                                    video {
                                                        width: 100vw !important;
                                                        height: 100vh !important;
                                                        max-width: 100vw !important;
                                                        max-height: 100vh !important;
                                                        position: fixed !important;
                                                        top: 0 !important;
                                                        left: 0 !important;
                                                        z-index: 999999 !important;
                                                        object-fit: contain !important;
                                                        background: #000 !important;
                                                    }
                                                    iframe, #player, .video-js, .jwplayer, div[id*="player"], div[class*="player"] {
                                                        width: 100vw !important;
                                                        height: 100vh !important;
                                                        max-width: 100vw !important;
                                                        max-height: 100vh !important;
                                                        position: fixed !important;
                                                        top: 0 !important;
                                                        left: 0 !important;
                                                        z-index: 99999 !important;
                                                        background: #000 !important;
                                                    }
                                                    header, footer, nav, .ad, .banner, .ads, [id*="ad"], [class*="ad"] {
                                                        display: none !important;
                                                    }
                                                `;
                                                document.head.appendChild(style);
                                            }
                                        })();
                                    """.trimIndent(), null)
                                }
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
                                val target = request.url.toString()
                                if (ArabseedRepository.isAd(target)) return true
                                return false
                            }
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                view.evaluateJavascript("""
                                    (function(){
                                        window.open = function(){ return null; };
                                        window.alert = function(){ return true; };
                                        var style = document.getElementById('mashad_fs_css');
                                        if (!style) {
                                            style = document.createElement('style');
                                            style.id = 'mashad_fs_css';
                                            style.innerHTML = `
                                                * { box-sizing: border-box !important; }
                                                html, body {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    margin: 0 !important;
                                                    padding: 0 !important;
                                                    overflow: hidden !important;
                                                    background: #000 !important;
                                                }
                                                video {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    position: fixed !important;
                                                    top: 0 !important;
                                                    left: 0 !important;
                                                    z-index: 999999 !important;
                                                    object-fit: contain !important;
                                                    background: #000 !important;
                                                }
                                                iframe, #player, .video-js, .jwplayer, div[id*="player"], div[class*="player"] {
                                                    width: 100vw !important;
                                                    height: 100vh !important;
                                                    max-width: 100vw !important;
                                                    max-height: 100vh !important;
                                                    position: fixed !important;
                                                    top: 0 !important;
                                                    left: 0 !important;
                                                    z-index: 99999 !important;
                                                    background: #000 !important;
                                                }
                                                header, footer, nav, .ad, .banner, .ads, [id*="ad"], [class*="ad"] {
                                                    display: none !important;
                                                }
                                            `;
                                            document.head.appendChild(style);
                                        }
                                        var v = document.querySelector('video');
                                        if (v) { v.play().catch(function(){}); }
                                    })();
                                """.trimIndent(), null)
                            }
                        }
                        loadUrl(server.embedUrl, ArabseedRepository.getEmbedHeaders())
                    }
                },
                update = { wv ->
                    if (wv.url != server.embedUrl) {
                        wv.loadUrl(server.embedUrl, ArabseedRepository.getEmbedHeaders())
                    }
                },
            )
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
                            .border(1.dp, MashadColors.GoldBorder, CircleShape)
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
                                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp))
                                    .clickable(onClick = onPrev)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("→", color = MashadColors.GoldLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text("الحلقة السابقة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }

                        if (hasNext && onNext != null) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MashadColors.Gold)
                                    .clickable(onClick = onNext)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("الحلقة التالية", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("←", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Spacer(Modifier.width(1.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Native ExoPlayer Component
// ─────────────────────────────────────────────────────────────────────────────

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun NativeStreamPlayer(
    streamUrl: String,
    onError: () -> Unit = {}
) {
    val context = LocalContext.current
    var isFullscreen by remember { mutableStateOf(false) }

    val exoPlayer = remember(streamUrl) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ArabseedRepository.getPlayerUserAgent())
            .setDefaultRequestProperties(ArabseedRepository.getPlayerHeaders())
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(10000)
            .setAllowCrossProtocolRedirects(true)

        val mediaItem = MediaItem.fromUri(streamUrl)
        val mediaSource = if (streamUrl.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 10_000,
                /* maxBufferMs = */ 40_000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1_000
            )
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
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
        update = { it.player = exoPlayer },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Mashad Search Screen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MashadSearchScreen(
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

        delay(600)
        lastQuery = trimmed
        loading = true
        error = null
        try {
            com.madar.tv.core.MashadTelemetry.trackSearchQuery(trimmed)
            val fetched = withContext(Dispatchers.IO) { ArabseedRepository.search(trimmed) }
            results = fetched
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = "تعذر البحث. تأكد من الاتصال."
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().background(MashadColors.Background)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(38.dp).clip(CircleShape).background(MashadColors.SurfaceCard).border(1.dp, MashadColors.GoldBorder, CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("←", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.width(10.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MashadColors.SurfaceCard)
                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp),
                decorationBox = { inner ->
                    Box(Modifier.fillMaxHeight(), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("ابحث عن أي فيلم أو مسلسل...", color = MashadColors.TextMuted, fontSize = 12.sp)
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
                        Text("اكتب اسم الفيلم أو المسلسل للبحث", color = MashadColors.TextMuted, fontSize = 13.sp)
                    }
                }
            }
            loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MashadColors.Gold)
                }
            }
            error != null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(error!!, color = MashadColors.TextMuted, fontSize = 13.sp)
                }
            }
            results.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد نتائج مطابقة لـ \"$query\"", color = MashadColors.TextMuted, fontSize = 13.sp)
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
                        MashadCard(item = item, onClick = { onOpen(item) })
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
