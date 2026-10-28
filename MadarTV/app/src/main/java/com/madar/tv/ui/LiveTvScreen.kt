package com.madar.tv.ui

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
import com.madar.tv.ui.theme.MashadColors
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
    val context = androidx.compose.ui.platform.LocalContext.current

    // Notify parent when player is active (fullscreen)
    LaunchedEffect(isFullscreen) {
        onPlayerStateChange(isFullscreen)
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
            .background(MashadColors.Background)
    ) {
        // ── Top Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(MashadColors.Surface)
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
                        .background(MashadColors.LiveGreen)
                )
                Text(
                    text = "مشهد TV • بثات ومباريات الكورة",
                    color = MashadColors.GoldLight,
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
                .background(MashadColors.Surface)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val selected = selectedTab == index
                val bgColor by animateColorAsState(
                    targetValue = if (selected) MashadColors.Gold else Color.Transparent,
                    label = "tab_bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (selected) Color.White else MashadColors.TextMuted,
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

        // ── Tab Content ──────────────────────────────────────────────────────────
        when (selectedTab) {
            0 -> MatchesTab(onStreamSelected = { stream, title ->
                activeStream = stream
                streamTitle = title
                isFullscreen = false
            })
            1 -> ChannelsTab(onStreamSelected = { stream, title ->
                activeStream = stream
                streamTitle = title
                isFullscreen = false
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
            Text(title, color = MashadColors.TextWhite, fontSize = 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("✕", color = MashadColors.TextMuted, fontSize = 18.sp,
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
                .background(MashadColors.SurfaceCard)
                .padding(20.dp)
        ) {
            Text(
                "اختر جودة البث",
                color = MashadColors.Gold,
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
                        .background(MashadColors.Surface)
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
                        color = MashadColors.TextWhite,
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
    val suggestedName = remember(match.leagueName) {
        when {
            match.leagueName.contains("إنجليزي", true) || match.leagueName.contains("Premier", true) -> "beIN SPORTS 1"
            match.leagueName.contains("إسباني", true) || match.leagueName.contains("La Liga", true) -> "beIN SPORTS 1"
            match.leagueName.contains("أبطال", true) || match.leagueName.contains("Champions", true) -> "beIN SPORTS 1"
            match.leagueName.contains("إيطالي", true) || match.leagueName.contains("Serie A", true) -> "beIN SPORTS 4"
            match.leagueName.contains("ألماني", true) || match.leagueName.contains("Bundesliga", true) -> "beIN SPORTS 5"
            match.leagueName.contains("فرنسي", true) || match.leagueName.contains("Ligue 1", true) -> "beIN SPORTS 4"
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
                .background(MashadColors.SurfaceCard)
                .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(16.dp))
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
                            color = MashadColors.GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${match.homeTeamArabic.ifBlank { match.homeTeam }} vs ${match.awayTeamArabic.ifBlank { match.awayTeam }}",
                        color = MashadColors.TextWhite,
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
                        .background(MashadColors.Surface)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", color = MashadColors.TextMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(14.dp))

            // Suggested Channel Quick-Play Button
            if (suggestedChannel != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MashadColors.Gold)
                        .clickable { onChannelSelect(suggestedChannel) }
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "▶ تشغيل القناة المقترحة للمباراة",
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
                        Text("مباشر 🔴", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(Modifier.height(12.dp))
            }

            Text(
                "أو اختر القناة الناقلة من قنوات البث:",
                color = MashadColors.TextMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(8.dp))

            if (loadingChannels && channels.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(32.dp))
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
                                .background(if (isSuggested) MashadColors.SurfaceHighlight else MashadColors.Surface)
                                .border(
                                    1.dp,
                                    if (isSuggested) MashadColors.Gold.copy(alpha = 0.6f) else MashadColors.GoldBorder.copy(alpha = 0.2f),
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
                                        color = MashadColors.TextWhite,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "بث مباشر بجودة عالية",
                                        color = MashadColors.TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MashadColors.Gold.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("مشاهدة", color = MashadColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("▶", color = MashadColors.Gold, fontSize = 9.sp)
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
fun MatchesTab(onStreamSelected: (YacineStream, String) -> Unit) {
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

    LaunchedEffect(Unit) {
        loadingSportsChannels = true
        sportsChannels = runCatching { YacineRepository.getSportsChannels() }.getOrElse { emptyList() }
        loadingSportsChannels = false
    }

    LaunchedEffect(yacineReloadKey) {
        loadingEvents = true
        eventError = false
        events = try { YacineRepository.getEvents() }
                 catch (e: Exception) { eventError = true; emptyList() }
        loadingEvents = false
    }

    LaunchedEffect(footballReloadKey) {
        footballLoading = true
        footballMatches = try {
            FootballRepository.fetchUpcomingMatches(forceRefresh = footballReloadKey > 0)
        } catch (_: Exception) {
            emptyList()
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

    // Quality picker
    if (pendingStreams != null) {
        if (pendingStreams!!.size == 1) {
            onStreamSelected(pendingStreams!!.first(), pendingTitle)
            pendingStreams = null
        } else {
            StreamQualityDialog(
                streams = pendingStreams!!,
                onSelect = { onStreamSelected(it, pendingTitle) },
                onDismiss = { pendingStreams = null }
            )
        }
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
                            loadingStreams = true
                            com.madar.tv.core.MashadTelemetry.trackLiveMatchView(
                                "${match.homeTeamArabic.ifBlank { match.homeTeam }} vs ${match.awayTeamArabic.ifBlank { match.awayTeam }}",
                                match.leagueName
                            )
                            var streams: List<YacineStream> = emptyList()
                            var streamTitle = "${match.homeTeamArabic.ifBlank { match.homeTeam }} vs ${match.awayTeamArabic.ifBlank { match.awayTeam }}"

                            // 1. Try matching Yacine live event first
                            val matched = findMatchingYacineEvent(match, events)
                            if (matched != null) {
                                streams = runCatching {
                                    YacineRepository.getEventStreams(matched.id)
                                }.getOrElse { emptyList() }
                            }

                            // 2. If no event streams, resolve the suggested beIN SPORTS / sports channel immediately
                            if (streams.isEmpty()) {
                                val chs = if (sportsChannels.isNotEmpty()) sportsChannels else runCatching {
                                    YacineRepository.getSportsChannels()
                                }.getOrElse { emptyList() }
                                val suggestedName = when {
                                    match.leagueName.contains("إنجليزي", true) || match.leagueName.contains("Premier", true) -> "beIN SPORTS 1"
                                    match.leagueName.contains("إسباني", true) || match.leagueName.contains("La Liga", true) -> "beIN SPORTS 1"
                                    match.leagueName.contains("أبطال", true) || match.leagueName.contains("Champions", true) -> "beIN SPORTS 1"
                                    match.leagueName.contains("إيطالي", true) || match.leagueName.contains("Serie A", true) -> "beIN SPORTS 4"
                                    match.leagueName.contains("ألماني", true) || match.leagueName.contains("Bundesliga", true) -> "beIN SPORTS 5"
                                    match.leagueName.contains("فرنسي", true) || match.leagueName.contains("Ligue 1", true) -> "beIN SPORTS 4"
                                    else -> "beIN SPORTS 1"
                                }
                                val targetChannel = chs.firstOrNull { it.name.contains(suggestedName, ignoreCase = true) }
                                    ?: chs.firstOrNull { it.name.contains("beIN SPORTS 1", ignoreCase = true) }
                                    ?: chs.firstOrNull()

                                if (targetChannel != null) {
                                    streams = runCatching {
                                        YacineRepository.getChannelStreams(targetChannel.id)
                                    }.getOrElse { emptyList() }
                                    streamTitle += " • ${targetChannel.name}"
                                }
                            }

                            loadingStreams = false

                            if (streams.isNotEmpty()) {
                                val bestStream = streams.firstOrNull {
                                    it.name.contains("720") || it.name.contains("HD", ignoreCase = true)
                                } ?: streams.first()
                                onStreamSelected(bestStream, streamTitle)
                            }
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
                            .background(MashadColors.LiveGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "سيرفرات البث المباشر للقنوات والمباريات",
                        color = MashadColors.GoldLight,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 3. Yacine Events / Status
            when {
                loadingEvents -> {
                    item {
                        MashadMatchSkeleton(Modifier.fillMaxWidth().height(120.dp))
                    }
                }
                eventError -> {
                    item {
                        Column(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("تعذر جلب قنوات البث المباشر حاليًا", color = MashadColors.TextMuted, fontSize = 13.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "إعادة المحاولة 🔄",
                                color = MashadColors.Gold,
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
                                color = MashadColors.TextMuted,
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
                                com.madar.tv.core.MashadTelemetry.trackLiveMatchView("${event.team1.name} vs ${event.team2.name}", event.champions)
                                scope.launch {
                                    loadingStreams = true
                                    val streams = runCatching {
                                        YacineRepository.getEventStreams(event.id)
                                    }.getOrElse { emptyList() }
                                    loadingStreams = false
                                    if (streams.isNotEmpty()) {
                                        val best = streams.firstOrNull {
                                            it.name.contains("720") || it.name.contains("HD", ignoreCase = true)
                                        } ?: streams.first()
                                        onStreamSelected(best, "${event.team1.name} vs ${event.team2.name}")
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
                CircularProgressIndicator(color = MashadColors.Gold)
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
        colors = CardDefaults.cardColors(containerColor = MashadColors.SurfaceCard),
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
                        color = MashadColors.Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = event.channel,
                        color = MashadColors.TextMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                if (isLive) {
                    LiveBadge()
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(timeStr, color = MashadColors.TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(dateStr, color = MashadColors.TextMuted, fontSize = 11.sp)
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
                        color = MashadColors.TextMuted,
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
                    color = MashadColors.TextMuted,
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
                .background(MashadColors.Surface),
            contentScale = ContentScale.Fit
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = team1Name,
            color = MashadColors.TextWhite,
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
            .background(MashadColors.Red.copy(alpha = 0.15f))
            .border(1.dp, MashadColors.Red.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MashadColors.Red)
        )
        Text("مباشر", color = MashadColors.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

// ─── Channels Tab ────────────────────────────────────────────────────────────────

@Composable
fun ChannelsTab(onStreamSelected: (YacineStream, String) -> Unit) {
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<YacineCategory>>(emptyList()) }
    var selectedCat by remember { mutableStateOf<YacineCategory?>(null) }
    var channels by remember { mutableStateOf<List<YacineChannel>>(emptyList()) }
    var loadingCats by remember { mutableStateOf(true) }
    var loadingChannels by remember { mutableStateOf(false) }
    var loadingStreams by remember { mutableStateOf(false) }
    var pendingStreams by remember { mutableStateOf<List<YacineStream>?>(null) }
    var pendingTitle by remember { mutableStateOf("") }

    // Load categories
    LaunchedEffect(Unit) {
        categories = runCatching { YacineRepository.getCategories() }.getOrElse { emptyList() }
        loadingCats = false
        if (categories.isNotEmpty()) {
            selectedCat = categories.first()
        }
    }

    // Load channels when category changes
    LaunchedEffect(selectedCat) {
        val cat = selectedCat ?: return@LaunchedEffect
        loadingChannels = true
        channels = runCatching { YacineRepository.getChannels(cat.id) }.getOrElse { emptyList() }
        loadingChannels = false
    }

    // Quality picker
    if (pendingStreams != null) {
        if (pendingStreams!!.size == 1) {
            onStreamSelected(pendingStreams!!.first(), pendingTitle)
            pendingStreams = null
        } else {
            StreamQualityDialog(
                streams = pendingStreams!!,
                onSelect = { onStreamSelected(it, pendingTitle) },
                onDismiss = { pendingStreams = null }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category chips
        if (loadingCats) {
            Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(24.dp))
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .background(MashadColors.Surface)
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
                loadingChannels -> MashadChannelSkeleton(Modifier.fillMaxSize())
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
                            com.madar.tv.core.MashadTelemetry.trackContentView(channel.name, "LIVE_CHANNEL", selectedCat?.name ?: "قنوات رياضية", "")
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
                    CircularProgressIndicator(color = MashadColors.Gold)
                }
            }
        }
    }
}

@Composable
fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) MashadColors.Gold else MashadColors.SurfaceCard,
        label = "chip_bg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else MashadColors.TextMuted,
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
        colors = CardDefaults.cardColors(containerColor = MashadColors.SurfaceCard),
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
                        .background(MashadColors.Surface),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = channel.name,
                    color = MashadColors.TextWhite,
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
                    .background(MashadColors.Gold.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = MashadColors.Gold, fontSize = 10.sp)
            }
        }
    }
}

// ─── Utilities ───────────────────────────────────────────────────────────────────

@Composable
fun EmptySection(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(message, color = MashadColors.TextMuted, fontSize = 15.sp)
    }
}

@Composable
fun RetrySection(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("حدث خطأ في تحميل البيانات", color = MashadColors.TextMuted, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = MashadColors.Gold)
        ) {
            Text("إعادة المحاولة", color = Color.White)
        }
    }
}
