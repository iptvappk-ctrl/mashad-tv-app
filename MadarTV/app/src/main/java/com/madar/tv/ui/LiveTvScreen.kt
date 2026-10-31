package com.madar.tv.ui

import androidx.activity.compose.BackHandler
import com.madar.tv.util.PlayerLauncher
import com.madar.tv.util.PlayerSource
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.madar.tv.data.FootballMatch
import com.madar.tv.data.FootballRepository
import com.madar.tv.data.YacineCategory
import com.madar.tv.data.YacineChannel
import com.madar.tv.data.YacineEvent
import com.madar.tv.data.YacineRepository
import com.madar.tv.data.YacineStream
import com.madar.tv.ui.theme.ZakzoukaColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

// ─── Main Screen ─────────────────────────────────────────────────────────────────

@Composable
fun LiveTvScreen(
    onPlayerStateChange: (Boolean) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("⚽ المباريات", "📺 القنوات")

    // Active stream for inline player
    var activeStream by remember { mutableStateOf<YacineStream?>(null) }
    var streamTitle by remember { mutableStateOf("") }
    var isFullscreen by remember { mutableStateOf(false) }
    var playerInstallPrompt by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Robust Back Navigation: prevents app exit when watching or on channels tab
    BackHandler(enabled = isFullscreen || activeStream != null || selectedTab != 0) {
        when {
            isFullscreen -> {
                isFullscreen = false
                resetScreenOrientation(context)
            }
            activeStream != null -> {
                activeStream = null
                isFullscreen = false
            }
            selectedTab != 0 -> selectedTab = 0
        }
    }

    // Notify parent when player is active (fullscreen)
    LaunchedEffect(isFullscreen) {
        onPlayerStateChange(isFullscreen)
    }

    if (playerInstallPrompt) {
        AlertDialog(
            onDismissRequest = { playerInstallPrompt = false },
            title = { Text("مشغل زقزوقة مطلوب") },
            text = { Text("ثبّت مشغل زقزوقة لفتح البث المباشر عليه.") },
            confirmButton = {
                TextButton(onClick = {
                    playerInstallPrompt = false
                    val market = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("market://details?id=com.madar.player"),
                    )
                    runCatching { context.startActivity(market) }.onFailure {
                        runCatching {
                            context.startActivity(android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.madar.player"),
                            ))
                        }
                    }
                }) { Text("تحميل المشغل") }
            },
            dismissButton = {
                TextButton(onClick = { playerInstallPrompt = false }) { Text("إغلاق") }
            },
        )
    }

    if (isFullscreen && activeStream != null) {
        // Full screen player — covers the whole window
        Box(
            Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            YacineStreamPlayer(
                stream = activeStream!!,
                modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center).aspectRatio(16f / 9f),
                onToggleFullscreen = {
                    isFullscreen = false
                    resetScreenOrientation(context)
                }
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ZakzoukaColors.Background)
    ) {
        // ── Top Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(ZakzoukaColors.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(ZakzoukaColors.LiveGreen)
                )
                Text(
                    text = "زقزوقة TV • بثات ومباريات الكورة",
                    color = ZakzoukaColors.GoldLight,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // ── Inline Player ────────────────────────────────────────────────────────
        if (activeStream != null) {
            YacinePlayerSection(
                stream = activeStream!!,
                title = streamTitle,
                onClose = {
                    activeStream = null
                    isFullscreen = false
                },
                onToggleFullscreen = {
                    isFullscreen = true
                    setScreenOrientation(context, true)
                }
            )
        }

        // ── Tab Bar ──────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ZakzoukaColors.Surface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val bgColor by animateColorAsState(
                    targetValue = if (selected) ZakzoukaColors.Gold else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else ZakzoukaColors.TextMuted,
                    label = "tab_text"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .clickable { selectedTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        val playInternal = { stream: YacineStream, title: String ->
            activeStream = stream
            streamTitle = title
            isFullscreen = true
            setScreenOrientation(context, true)
        }

        // ── Tab Content ──────────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> MatchesTab(onStreamSelected = { stream, title, backups ->
                PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    headers = stream.headers,
                    backupSources = backups.map { PlayerSource(it.url, it.userAgent, it.referer, it.headers) },
                    onNotInstalled = { playerInstallPrompt = true },
                )
            })
            1 -> ChannelsTab(onStreamSelected = { stream, title, backups ->
                PlayerLauncher.launchPlayer(
                    context = context,
                    streamUrl = stream.url,
                    title = title,
                    userAgent = stream.userAgent,
                    referer = stream.referer,
                    headers = stream.headers,
                    backupSources = backups.map { PlayerSource(it.url, it.userAgent, it.referer, it.headers) },
                    onNotInstalled = { playerInstallPrompt = true },
                )
            })
        }
    }
}

// ─── Inline Player Section ───────────────────────────────────────────────────────

@Composable
fun YacinePlayerSection(
    stream: YacineStream,
    title: String,
    onClose: () -> Unit,
    onToggleFullscreen: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
    ) {
        // Close bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = ZakzoukaColors.TextWhite, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("✕", color = ZakzoukaColors.TextMuted, fontSize = 18.sp,
                modifier = Modifier.clickable { onClose() }.padding(8.dp))
        }
        // Player
        YacineStreamPlayer(
            stream = stream,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            onToggleFullscreen = onToggleFullscreen,
        )
    }
}

// ─── Stream Quality Picker Dialog ────────────────────────────────────────────────

@Composable
fun StreamQualityDialog(
    streams: List<YacineStream>,
    onSelect: (YacineStream) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(16.dp))
                .background(ZakzoukaColors.SurfaceCard)
                .padding(20.dp)
        ) {
            Text(
                "اختر جودة البث",
                color = ZakzoukaColors.Gold,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            streams.forEach { stream ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(ZakzoukaColors.Surface)
                        .clickable {
                            onSelect(stream)
                            onDismiss()
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stream.name,
                        color = ZakzoukaColors.TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


// ─── Match Broadcast Channels Dialog ─────────────────────────────────────────────

@Composable
fun MatchBroadcastDialog(
    match: FootballMatch,
    channels: List<YacineChannel>,
    loadingChannels: Boolean = false,
    onChannelSelect: (YacineChannel) -> Unit,
    onDismiss: () -> Unit
) {
    val isLive = match.state == "in"
    val kickoffMs = match.kickoffEpochMs
    val timeFormatted = if (kickoffMs != null) {
        java.text.SimpleDateFormat("h:mm a", java.util.Locale.forLanguageTag("ar")).apply {
            timeZone = java.util.TimeZone.getDefault()
        }.format(java.util.Date(kickoffMs)).replace("AM", "ص").replace("PM", "م")
    } else match.timeOrStatus

    val suggestedName = remember(match.leagueName) {
        val l = match.leagueName.lowercase()
        when {
            l.contains("فرنسي") || l.contains("ligue 1") -> "beIN SPORTS 4"
            l.contains("ألماني") || l.contains("bundesliga") -> "beIN SPORTS 5"
            l.contains("إسباني") || l.contains("la liga") -> "beIN SPORTS 3"
            l.contains("إنجليزي") || l.contains("premier") -> "beIN SPORTS 1"
            l.contains("أبطال") || l.contains("champions") -> "beIN SPORTS 1"
            l.contains("إيطالي") || l.contains("serie a") -> "beIN SPORTS 4"
            else -> "beIN SPORTS 1"
        }
    }

    val suggestedChannel = channels.firstOrNull { it.name.contains(suggestedName, ignoreCase = true) }
        ?: channels.firstOrNull { it.name.contains("beIN SPORTS 1", ignoreCase = true) }
        ?: channels.firstOrNull()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(16.dp))
                .background(ZakzoukaColors.SurfaceCard)
                .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            // Header: Title + Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🏆", fontSize = 12.sp)
                        Text(
                            match.leagueName.ifBlank { "مباراة اليوم" },
                            color = ZakzoukaColors.GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${match.homeTeamArabic.ifBlank { match.homeTeam }} vs ${match.awayTeamArabic.ifBlank { match.awayTeam }}",
                        color = ZakzoukaColors.TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ZakzoukaColors.Surface)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = ZakzoukaColors.TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(10.dp))

            // Match Time & State Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isLive) ZakzoukaColors.LiveGreen.copy(alpha = 0.12f) else ZakzoukaColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isLive) "🔴 جارية الآن" else "⏰ موعد المباراة: اليوم $timeFormatted",
                    color = if (isLive) ZakzoukaColors.LiveGreen else ZakzoukaColors.Gold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (isLive) "بث مباشر" else "لم تبدأ بعد",
                    color = ZakzoukaColors.TextMuted,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(12.dp))

            // Suggested Channel Quick-Play Button
            if (suggestedChannel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(ZakzoukaColors.Gold)
                        .clickable { onChannelSelect(suggestedChannel) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            if (isLive) "▶ تشغيل البث المباشر الآن" else "▶ تشغيل بث القناة الناقلة الآن",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            suggestedChannel.name,
                            color = Color.White.copy(alpha = 0.86f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.16f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(if (isLive) "مباشر 🔴" else "قادمة ⏰", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            Text(
                "أو اختر القناة الناقلة من قنوات البث:",
                color = ZakzoukaColors.TextMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            if (loadingChannels && channels.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(32.dp))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        val isSuggested = channel.id == suggestedChannel?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSuggested) ZakzoukaColors.SurfaceHighlight else ZakzoukaColors.Surface)
                                .border(
                                    1.dp,
                                    if (isSuggested) ZakzoukaColors.Gold.copy(alpha = 0.6f) else ZakzoukaColors.GoldBorder.copy(alpha = 0.2f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onChannelSelect(channel) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("📺", fontSize = 16.sp)
                                Column {
                                    Text(
                                        channel.name,
                                        color = ZakzoukaColors.TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "بث مباشر بجودة عالية",
                                        color = ZakzoukaColors.TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ZakzoukaColors.Gold.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("مشاهدة", color = ZakzoukaColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("▶", color = ZakzoukaColors.Gold, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun findMatchingYacineEvent(match: FootballMatch, events: List<YacineEvent>): YacineEvent? {
    val hAr = match.homeTeamArabic.trim()
    val aAr = match.awayTeamArabic.trim()
    val hEn = match.homeTeam.trim().lowercase()
    val aEn = match.awayTeam.trim().lowercase()

    return events.firstOrNull { ev ->
        val t1 = ev.team1.name
        val t2 = ev.team2.name
        val hMatch = (hAr.isNotEmpty() && (t1.contains(hAr, true) || t2.contains(hAr, true))) ||
                     (hEn.isNotEmpty() && (t1.lowercase().contains(hEn) || t2.lowercase().contains(hEn)))
        val aMatch = (aAr.isNotEmpty() && (t1.contains(aAr, true) || t2.contains(aAr, true))) ||
                     (aEn.isNotEmpty() && (t1.lowercase().contains(aEn) || t2.lowercase().contains(aEn)))
        hMatch && aMatch
    } ?: events.firstOrNull { ev ->
        val t1 = ev.team1.name
        val t2 = ev.team2.name
        val hSub = if (hAr.length >= 4) hAr.substring(0, 4) else hAr
        val aSub = if (aAr.length >= 4) aAr.substring(0, 4) else aAr
        (hSub.length >= 3 && (t1.contains(hSub, true) || t2.contains(hSub, true))) ||
        (aSub.length >= 3 && (t1.contains(aSub, true) || t2.contains(aSub, true)))
    }
}

// ─── Matches Tab ─────────────────────────────────────────────────────────────────

@Composable
fun MatchesTab(onStreamSelected: (YacineStream, String, List<YacineStream>) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<YacineEvent>>(emptyList()) }
    var loadingEvents by remember { mutableStateOf(true) }
    var eventError by remember { mutableStateOf(false) }
    var yacineReloadKey by remember { mutableIntStateOf(0) }

    var footballMatches by remember { mutableStateOf<List<FootballMatch>>(emptyList()) }
    var footballLoading by remember { mutableStateOf(true) }
    var footballReloadKey by remember { mutableIntStateOf(0) }

    var sportsChannels by remember { mutableStateOf<List<YacineChannel>>(emptyList()) }
    var loadingSportsChannels by remember { mutableStateOf(false) }

    var loadingStreams by remember { mutableStateOf(false) }
    var pendingStreams by remember { mutableStateOf<List<YacineStream>?>(null) }
    var pendingTitle by remember { mutableStateOf("") }

    // Dialog state: user picks the broadcasting channel for a match
    var broadcastDialogMatch by remember { mutableStateOf<FootballMatch?>(null) }
    var broadcastDialogChannels by remember { mutableStateOf<List<YacineChannel>>(emptyList()) }

    LaunchedEffect(Unit) {
        val cachedSportsChannels = withContext(Dispatchers.IO) {
            val categories = YacineRepository.getCachedCategories().orEmpty()
            val category = categories.firstOrNull { it.name.contains("beIN SPORTS", ignoreCase = true) }
                ?: categories.firstOrNull { it.name.contains("SPORTS", ignoreCase = true) }
                ?: categories.firstOrNull()
            category?.let { YacineRepository.getCachedChannels(it.id) }
        }
        if (!cachedSportsChannels.isNullOrEmpty()) {
            sportsChannels = cachedSportsChannels
            loadingSportsChannels = false
        } else loadingSportsChannels = true
        val refreshed = runCatching { YacineRepository.getSportsChannels(forceRefresh = true) }.getOrElse { emptyList() }
        if (refreshed.isNotEmpty()) sportsChannels = refreshed
        loadingSportsChannels = false
    }

    LaunchedEffect(yacineReloadKey) {
        val cachedEvents = withContext(Dispatchers.IO) { YacineRepository.getCachedEvents() }
        if (!cachedEvents.isNullOrEmpty()) {
            events = cachedEvents
            loadingEvents = false
        } else loadingEvents = true
        eventError = false
        events = try { YacineRepository.getEvents(forceRefresh = true).ifEmpty { cachedEvents.orEmpty() } }
                 catch (_: Exception) { eventError = cachedEvents.isNullOrEmpty(); cachedEvents.orEmpty() }
        loadingEvents = false
    }

    LaunchedEffect(footballReloadKey) {
        val cachedMatches = withContext(Dispatchers.IO) { FootballRepository.loadCachedMatches() }
        if (!cachedMatches.isNullOrEmpty()) {
            footballMatches = cachedMatches
            footballLoading = false
        } else footballLoading = true
        footballMatches = try {
            FootballRepository.fetchUpcomingMatches(forceRefresh = true).ifEmpty { cachedMatches.orEmpty() }
        } catch (_: Exception) {
            cachedMatches.orEmpty()
        }
        footballLoading = false
    }

    // Auto-refresh every 60s to update live match status
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            footballReloadKey++
        }
    }

    // Instant auto-play: pick best quality stream immediately (NO quality dialog)
    LaunchedEffect(pendingStreams) {
        val ps = pendingStreams
        if (!ps.isNullOrEmpty()) {
            val best = ps.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                ?: ps.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                ?: ps.first()
            val backups = ps.filter { it.url != best.url }
            onStreamSelected(best, pendingTitle, backups)
            pendingStreams = null
        }
    }

    // Channel picker dialog — opens when no live Yacine event is found for the match
    val dlgMatch = broadcastDialogMatch
    if (dlgMatch != null) {
        MatchBroadcastDialog(
            match = dlgMatch,
            channels = broadcastDialogChannels,
            loadingChannels = loadingStreams,
            onChannelSelect = { channel ->
                broadcastDialogMatch = null
                val title = "${dlgMatch.homeTeamArabic.ifBlank { dlgMatch.homeTeam }} vs ${dlgMatch.awayTeamArabic.ifBlank { dlgMatch.awayTeam }} • ${channel.name}"
                val isLive = dlgMatch.state == "in"
                val kickoffMs = dlgMatch.kickoffEpochMs
                val timeFormatted = if (kickoffMs != null) {
                    java.text.SimpleDateFormat("h:mm a", java.util.Locale.forLanguageTag("ar")).apply {
                        timeZone = java.util.TimeZone.getDefault()
                    }.format(java.util.Date(kickoffMs)).replace("AM", "ص").replace("PM", "م")
                } else dlgMatch.timeOrStatus

                if (!isLive) {
                    android.widget.Toast.makeText(
                        context,
                        "المباراة تبدأ الساعة $timeFormatted — جاري تشغيل بث ${channel.name} 📺",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
                scope.launch {
                    loadingStreams = true
                    val streams = runCatching {
                        YacineRepository.getChannelStreams(channel.id)
                    }.getOrElse { emptyList() }
                    loadingStreams = false
                    if (streams.isNotEmpty()) {
                        pendingTitle = title
                        pendingStreams = streams
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "لم يبدأ البث بعد على هذه القناة 📺",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            onDismiss = { broadcastDialogMatch = null }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. European Leagues & Champions League Widget
            item {
                FootballMatchesWidget(
                    matches = footballMatches,
                    loading = footballLoading,
                    onRefresh = { footballReloadKey++ },
                    onMatchClick = { match ->
                        scope.launch {
                            val isLive = match.state == "in"
                            val isFinished = match.state == "post" || (match.kickoffEpochMs != null && match.kickoffEpochMs <= System.currentTimeMillis() - (130 * 60 * 1000L))

                            if (isFinished) {
                                android.widget.Toast.makeText(
                                    context,
                                    "المباراة انتهت بنتيجة ${match.homeScore.ifBlank { "0" }} - ${match.awayScore.ifBlank { "0"}} ✅",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }

                            val chs = if (sportsChannels.isNotEmpty()) sportsChannels else runCatching {
                                YacineRepository.getSportsChannels()
                            }.getOrElse { emptyList() }

                            // If match has NOT started yet, show details dialog so user clearly knows match kickoff time
                            if (!isLive) {
                                broadcastDialogChannels = chs
                                broadcastDialogMatch = match
                                return@launch
                            }

                            val title = "${match.homeTeamArabic.ifBlank { match.homeTeam }} vs ${match.awayTeamArabic.ifBlank { match.awayTeam }}"
                            com.madar.tv.core.ZakzoukaTelemetry.trackLiveMatchView(title, match.leagueName)

                            loadingStreams = true
                            val matched = findMatchingYacineEvent(match, events)
                            val chHint = matched?.channel.orEmpty()

                            val targetChannel = (if (chHint.isNotBlank()) chs.firstOrNull { it.name.contains(chHint, true) || chHint.contains(it.name, true) } else null)
                                ?: run {
                                    val league = match.leagueName.lowercase()
                                    when {
                                        league.contains("فرنسي") || league.contains("ligue 1") -> chs.firstOrNull { it.name.contains("beIN", true) && it.name.contains("4") }
                                        league.contains("ألماني") || league.contains("bundesliga") -> chs.firstOrNull { it.name.contains("beIN", true) && it.name.contains("5") }
                                        league.contains("إسباني") || league.contains("la liga") -> chs.firstOrNull { it.name.contains("beIN", true) && it.name.contains("3") }
                                        league.contains("إنجليزي") || league.contains("premier") -> chs.firstOrNull { it.name.contains("beIN", true) && (it.name.contains("1") || it.name.contains("2")) }
                                        else -> null
                                    }
                                }
                                ?: chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) && (it.name.contains("1") || it.name.contains("Premium")) }
                                ?: chs.firstOrNull { it.name.contains("beIN", ignoreCase = true) }
                                ?: chs.firstOrNull()

                            val allStreams = mutableListOf<YacineStream>()

                            // Channel streams have guaranteed fresh 200 OK tokens
                            if (targetChannel != null) {
                                val chStreams = runCatching { YacineRepository.getChannelStreams(targetChannel.id) }.getOrElse { emptyList() }
                                allStreams.addAll(chStreams)
                            }
                            if (matched != null) {
                                val evStreams = runCatching { YacineRepository.getEventStreams(matched.id) }.getOrElse { emptyList() }
                                allStreams.addAll(evStreams)
                            }
                            loadingStreams = false

                            val validStreams = allStreams.filter { it.url.isNotBlank() && !it.url.contains("rex.new-redirect.online") }.distinctBy { it.url }
                            if (validStreams.isNotEmpty()) {
                                val best = validStreams.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                                    ?: validStreams.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                                    ?: validStreams.first()
                                val backups = validStreams.filter { it.url != best.url }
                                val label = targetChannel?.name?.let { " · $it" } ?: ""
                                onStreamSelected(best, "$title$label", backups)
                                return@launch
                            }

                            // Fallback dialog if channel auto-select failed
                            broadcastDialogChannels = chs
                            broadcastDialogMatch = match
                        }
                    }
                )
            }

            // 2. Section Header: Yacine Live Broadcasts
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ZakzoukaColors.LiveGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "سيرفرات البث المباشر للقنوات والمباريات",
                        color = ZakzoukaColors.GoldLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Yacine Events / Status
            when {
                loadingEvents -> {
                    item {
                        ZakzoukaMatchSkeleton(Modifier.fillMaxWidth().height(120.dp))
                    }
                }
                eventError -> {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("تعذر جلب قنوات البث المباشر حاليًا", color = ZakzoukaColors.TextMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "إعادة المحاولة 🔄",
                                color = ZakzoukaColors.Gold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { yacineReloadKey++ }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
                events.isEmpty() -> {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "لا توجد مباريات منقولة بالبث المباشر حاليًا ⚽\nتابع نتائج ومواعيد المباريات والدوريات أعلاه",
                                color = ZakzoukaColors.TextMuted,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    items(events) { event ->
                        Box(Modifier.padding(horizontal = 12.dp)) {
                            MatchCard(event = event, onClick = {
                                com.madar.tv.core.ZakzoukaTelemetry.trackLiveMatchView("${event.team1.name} vs ${event.team2.name}", event.champions)
                                scope.launch {
                                    loadingStreams = true
                                    val streams = runCatching {
                                        YacineRepository.getEventStreams(event.id)
                                    }.getOrElse { emptyList() }
                                    loadingStreams = false
                                    if (streams.isNotEmpty()) {
                                        val valid = streams.filter { it.url.isNotBlank() && !it.url.contains("rex.new-redirect.online") }
                                        val best = valid.firstOrNull {
                                            it.name.contains("720") || it.name.contains("HD", ignoreCase = true)
                                        } ?: valid.firstOrNull() ?: streams.first()
                                        val backups = valid.filter { it.url != best.url }
                                        onStreamSelected(best, "${event.team1.name} vs ${event.team2.name}", backups)
                                    }
                                }
                            })
                        }
                    }
                }
            }
        }

        if (loadingStreams) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ZakzoukaColors.Gold)
            }
        }
    }
}

@Composable
fun MatchCard(event: YacineEvent, onClick: () -> Unit) {
    val now = System.currentTimeMillis() / 1000
    val isLive = now in event.startTime..event.endTime
    val startDate = Date(event.startTime * 1000)
    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(startDate)
    val dateStr = SimpleDateFormat("d MMM", Locale.forLanguageTag("ar")).format(startDate)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ZakzoukaColors.SurfaceCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: competition + channel + live badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.champions,
                        color = ZakzoukaColors.Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = event.channel,
                        color = ZakzoukaColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                if (isLive) {
                    LiveBadge()
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(timeStr, color = ZakzoukaColors.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(dateStr, color = ZakzoukaColors.TextMuted, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Teams row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Team 1
                TeamDisplay(team1Name = event.team1.name, teamLogo = event.team1.logo)
                // VS
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "VS",
                        color = ZakzoukaColors.TextMuted,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                // Team 2
                TeamDisplay(team1Name = event.team2.name, teamLogo = event.team2.logo)
            }

            // Commentary
            if (event.commentary.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "🎙 ${event.commentary}",
                    color = ZakzoukaColors.TextMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TeamDisplay(team1Name: String, teamLogo: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(110.dp)
    ) {
        AsyncImage(
            model = teamLogo.ifEmpty { null },
            contentDescription = team1Name,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(ZakzoukaColors.Surface),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = team1Name,
            color = ZakzoukaColors.TextWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiveBadge() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(ZakzoukaColors.Red.copy(alpha = 0.15f))
            .border(1.dp, ZakzoukaColors.Red.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(ZakzoukaColors.Red)
        )
        Text("مباشر", color = ZakzoukaColors.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Channels Tab ────────────────────────────────────────────────────────────────

@Composable
fun ChannelsTab(onStreamSelected: (YacineStream, String, List<YacineStream>) -> Unit) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<YacineCategory>>(emptyList()) }
    var selectedCat by remember { mutableStateOf<YacineCategory?>(null) }
    var channels by remember { mutableStateOf<List<YacineChannel>>(emptyList()) }
    var loadingCats by remember { mutableStateOf(true) }
    var loadingChannels by remember { mutableStateOf(false) }
    var loadingStreams by remember { mutableStateOf(false) }
    var pendingStreams by remember { mutableStateOf<List<YacineStream>?>(null) }
    var pendingTitle by remember { mutableStateOf("") }

    // Restore saved channels/categories first and refresh them without blocking the screen.
    LaunchedEffect(Unit) {
        val cachedCategories = withContext(Dispatchers.IO) { YacineRepository.getCachedCategories() }
        if (!cachedCategories.isNullOrEmpty()) {
            categories = cachedCategories
            selectedCat = selectedCat ?: cachedCategories.firstOrNull()
            loadingCats = false
        } else loadingCats = true
        val freshCategories = runCatching { YacineRepository.getCategories(forceRefresh = true) }.getOrElse { emptyList() }
        if (freshCategories.isNotEmpty()) {
            categories = freshCategories
            selectedCat = selectedCat?.let { selected -> freshCategories.firstOrNull { it.id == selected.id } }
                ?: freshCategories.firstOrNull()
        }
        loadingCats = false
    }

    LaunchedEffect(selectedCat) {
        val cat = selectedCat ?: return@LaunchedEffect
        val cachedChannels = withContext(Dispatchers.IO) { YacineRepository.getCachedChannels(cat.id) }
        if (!cachedChannels.isNullOrEmpty()) {
            channels = cachedChannels
            loadingChannels = false
        } else loadingChannels = true
        val freshChannels = runCatching { YacineRepository.getChannels(cat.id, forceRefresh = true) }.getOrElse { emptyList() }
        if (freshChannels.isNotEmpty() || cachedChannels.isNullOrEmpty()) channels = freshChannels
        loadingChannels = false
    }

    // Instant auto-play: pick best quality stream immediately
    LaunchedEffect(pendingStreams) {
        val ps = pendingStreams
        if (!ps.isNullOrEmpty()) {
            val best = ps.firstOrNull { it.name.contains("1080", ignoreCase = true) || it.name.contains("FHD", ignoreCase = true) }
                ?: ps.firstOrNull { it.name.contains("720", ignoreCase = true) || it.name.contains("HD", ignoreCase = true) }
                ?: ps.first()
            val backups = ps.filter { it.url != best.url }
            onStreamSelected(best, pendingTitle, backups)
            pendingStreams = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category chips
        if (loadingCats) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ZakzoukaColors.Gold, modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(ZakzoukaColors.Surface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val selected = selectedCat?.id == cat.id
                    CategoryChip(
                        label = cat.name,
                        selected = selected,
                        onClick = { selectedCat = cat }
                    )
                }
            }
        }

        // Channels grid
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                loadingChannels -> ZakzoukaChannelSkeleton(Modifier.fillMaxSize())
                channels.isEmpty() && !loadingChannels -> EmptySection("لا توجد قنوات")
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(channels, key = { it.id }) { channel ->
                        ChannelCard(channel = channel, onClick = {
                            com.madar.tv.core.ZakzoukaTelemetry.trackContentView(channel.name, "LIVE_CHANNEL", selectedCat?.name ?: "قنوات رياضية", "")
                            scope.launch {
                                loadingStreams = true
                                val streams = runCatching {
                                    YacineRepository.getChannelStreams(channel.id)
                                }.getOrElse { emptyList() }
                                loadingStreams = false
                                if (streams.isNotEmpty()) {
                                    pendingTitle = channel.name
                                    pendingStreams = streams
                                }
                            }
                        })
                    }
                }
            }
            if (loadingStreams) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ZakzoukaColors.Gold)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) ZakzoukaColors.Gold else ZakzoukaColors.SurfaceCard,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else ZakzoukaColors.TextMuted,
        label = "chip_text"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
fun ChannelCard(channel: YacineChannel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.4f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = ZakzoukaColors.SurfaceCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AsyncImage(
                    model = channel.logo.ifEmpty { null },
                    contentDescription = channel.name,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ZakzoukaColors.Surface),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = channel.name,
                    color = ZakzoukaColors.TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Play icon overlay
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(ZakzoukaColors.Gold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = ZakzoukaColors.Gold, fontSize = 10.sp)
            }
        }
    }
}

// ─── Utilities ───────────────────────────────────────────────────────────────────

@Composable
fun EmptySection(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = ZakzoukaColors.TextMuted, fontSize = 15.sp)
    }
}

@Composable
fun RetrySection(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("حدث خطأ في تحميل البيانات", color = ZakzoukaColors.TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ZakzoukaColors.Gold)
        ) {
            Text("إعادة المحاولة", color = Color.White)
        }
    }
}
