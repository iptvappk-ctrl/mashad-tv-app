import re

path = r"c:\Users\hassa\Desktop\all project\iptv app\MadarTV\app\src\main\java\com\madar\tv\ui\ArabseedScreen.kt"

with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update MashadDetailScreen invocation at line 155
old_call = """            currentItem != null -> {
                MashadDetailScreen(
                    item = currentItem,
                    onBack = { openedItem = null },
                    onPlay = { d, eps, epIdx ->
                        openedDetail = d
                        activeEpisodes = eps
                        currentEpisodeIndex = epIdx
                    },
                )
            }"""

new_call = """            currentItem != null -> {
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
            }"""

assert old_call in content, "old_call not found in ArabseedScreen.kt!"
content = content.replace(old_call, new_call, 1)
print("Updated MashadDetailScreen invocation successfully!")

# 2. Replace MashadDetailScreen implementation
start_needle = "@Composable\nprivate fun MashadDetailScreen("
end_needle = "// ─────────────────────────────────────────────────────────────────────────────\n// Hybrid Player Screen (Native ExoPlayer for m3u8 + Clean Web Embed fallback)"

start_idx = content.find(start_needle)
end_idx = content.find(end_needle)

assert start_idx != -1 and end_idx != -1 and start_idx < end_idx, "Could not find MashadDetailScreen boundaries!"

new_screen = """@Composable
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

"""

content = content[:start_idx] + new_screen + content[end_idx:]

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("ArabseedScreen.kt updated successfully!")
