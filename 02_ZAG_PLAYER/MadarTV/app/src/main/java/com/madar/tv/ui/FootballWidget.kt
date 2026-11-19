package com.madar.tv.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madar.tv.data.FootballMatch
import com.madar.tv.ui.theme.ZakzoukaColors
import java.text.SimpleDateFormat
import java.util.*

private val LEAGUE_FILTERS = listOf(
    "الكل",
    "دوري أبطال أوروبا",
    "الدوري الإنجليزي",
    "الدوري الإسباني",
    "الدوري الإيطالي",
    "الدوري الألماني",
    "الدوري الفرنسي",
)

// ─── Upcoming & Live Matches Widget ──────────────────────────────────────────────

@Composable
fun FootballMatchesWidget(
    matches: List<FootballMatch>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: ((FootballMatch) -> Unit)? = null,
) {
    var selectedFilter by remember { mutableStateOf("الكل") }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Auto-refresh time every 30 seconds to update live match status
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            currentTime = System.currentTimeMillis()
        }
    }

    // Today midnight boundaries (local time)
    val todayStart = remember(currentTime) {
        java.util.Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = remember(todayStart) { todayStart + 24 * 60 * 60 * 1000L }

    // All today matches only
    val todayMatches = remember(matches, todayStart, todayEnd) {
        matches.filter { m ->
            m.state == "in" ||
            (m.kickoffEpochMs != null && m.kickoffEpochMs >= todayStart && m.kickoffEpochMs < todayEnd)
        }
    }

    // 1. Live + upcoming (strictly not finished)
    val liveAndUpcoming = remember(todayMatches, currentTime) {
        todayMatches.filter { m ->
            m.state == "in" || (m.state != "post" && (m.kickoffEpochMs == null || m.kickoffEpochMs > currentTime - (130 * 60 * 1000L)))
        }.sortedWith(compareBy(
            { if (it.state == "in") 0 else 1 },
            { it.kickoffEpochMs ?: Long.MAX_VALUE }
        ))
    }

    // 2. Finished today — strictly at the bottom
    val finishedToday = remember(todayMatches, currentTime) {
        todayMatches.filter { m ->
            m.state == "post" ||
            (m.state != "in" && m.kickoffEpochMs != null && m.kickoffEpochMs <= currentTime - (130 * 60 * 1000L))
        }.sortedByDescending { it.kickoffEpochMs ?: 0L }
    }

    val filtered = remember(liveAndUpcoming, selectedFilter) {
        if (selectedFilter == "الكل") liveAndUpcoming
        else liveAndUpcoming.filter { it.leagueName.contains(selectedFilter, ignoreCase = true) }
    }

    val filteredFinished = remember(finishedToday, selectedFilter) {
        if (selectedFilter == "الكل") finishedToday
        else finishedToday.filter { it.leagueName.contains(selectedFilter, ignoreCase = true) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(ZakzoukaColors.Surface, ZakzoukaColors.SurfaceCard)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(ZakzoukaColors.LiveGreen))
                Text(
                    "المباريات القادمة والمباشرة",
                    color = ZakzoukaColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(ZakzoukaColors.Gold.copy(alpha = 0.12f))
                    .border(1.dp, ZakzoukaColors.GoldBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("تحديث", color = ZakzoukaColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("🔄", fontSize = 11.sp)
            }
        }

        // ── League Filter Chips ──────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(ZakzoukaColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LEAGUE_FILTERS.forEach { league ->
                val sel = selectedFilter == league
                val chipBg by animateColorAsState(
                    if (sel) ZakzoukaColors.Gold else ZakzoukaColors.SurfaceCard,
                    label = "chip",
                )
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(1.dp, if (sel) ZakzoukaColors.Gold else ZakzoukaColors.GoldBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = league }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        league,
                        color = if (sel) Color.White else ZakzoukaColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // Divider
        Box(Modifier.fillMaxWidth().height(1.dp).background(ZakzoukaColors.GoldBorder.copy(alpha = 0.3f)))

        // ── Match List ───────────────────────────────────────────────────────
        when {
            loading -> ZakzoukaMatchSkeleton(Modifier.fillMaxWidth().height(180.dp))
            filtered.isEmpty() -> Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚽", fontSize = 32.sp)
                    Text("لا توجد مباريات قادمة حالياً", color = ZakzoukaColors.TextMuted, fontSize = 13.sp)
                    Text(
                        "تحديث 🔄",
                        color = ZakzoukaColors.Gold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onRefresh),
                    )
                }
            }
            else -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                filtered.forEach { match ->
                    FootballMatchRow(match = match, currentTime = currentTime, onClick = onMatchClick?.let { cb -> { cb(match) } })
                }
                // Finished matches at the bottom
                if (filteredFinished.isNotEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().background(ZakzoukaColors.SurfaceCard)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "✅ مباريات انتهت اليوم",
                            color = ZakzoukaColors.TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    filteredFinished.forEach { match ->
                        FootballMatchRow(match = match, currentTime = currentTime, onClick = null)
                    }
                }
            }
        }
    }
}

// ─── Single Match Row (WeCima-inspired style) ─────────────────────────────────────

@Composable
fun FootballMatchRow(match: FootballMatch, currentTime: Long = System.currentTimeMillis(), onClick: (() -> Unit)? = null) {
    val isLive = match.state == "in"
    val kickoffMs = match.kickoffEpochMs
    val isFinished = match.state == "post" || (match.state != "in" && kickoffMs != null && kickoffMs <= currentTime - (130 * 60 * 1000L))
    val isUpcoming = !isLive && !isFinished

    val timeStr = if (kickoffMs != null) {
        SimpleDateFormat("h:mm a", Locale.forLanguageTag("ar")).apply {
            timeZone = TimeZone.getDefault()
        }.format(Date(kickoffMs)).replace("AM", "ص").replace("PM", "م")
    } else match.timeOrStatus

    val dateStr = if (kickoffMs != null) {
        "اليوم"
    } else ""

    val minutesUntil = if (kickoffMs != null) ((kickoffMs - currentTime) / 60_000).toInt() else Int.MAX_VALUE
    val startingSoon = minutesUntil in 0..30 && isUpcoming

    // Pulsing animation for live dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    )

    Column(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .background(ZakzoukaColors.SurfaceCard)
            .border(
                width = if (isLive) 1.dp else 0.dp,
                color = if (isLive) ZakzoukaColors.LiveGreen.copy(alpha = 0.4f) else Color.Transparent,
            ),
    ) {
        // ── League banner ────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(if (isLive) ZakzoukaColors.LiveGreen.copy(alpha = 0.08f) else ZakzoukaColors.Surface.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏆", fontSize = 11.sp)
                Text(
                    match.leagueName.ifBlank { "مباراة" },
                    color = ZakzoukaColors.GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // Status badge
            when {
                isLive -> Row(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(ZakzoukaColors.LiveGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(ZakzoukaColors.LiveGreen.copy(alpha = pulsAlpha)))
                    Text("مباشر الآن 🔴", color = ZakzoukaColors.LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                isFinished -> Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(ZakzoukaColors.Surface.copy(alpha = 0.8f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("انتهت ✅", color = ZakzoukaColors.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                startingSoon -> Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(ZakzoukaColors.Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "تبدأ خلال ${minutesUntil} دقيقة",
                        color = ZakzoukaColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    )
                }
                else -> Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(ZakzoukaColors.Gold.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text("قادمة اليوم ⏳", color = ZakzoukaColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ── Teams Row ────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Home Team
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    match.homeTeamArabic.ifBlank { match.homeTeam },
                    color = ZakzoukaColors.TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                AsyncImage(
                    model = match.homeLogo.ifEmpty { null },
                    contentDescription = match.homeTeam,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ZakzoukaColors.Surface),
                    contentScale = ContentScale.Fit,
                )
            }

            // Score / VS
            Box(
                Modifier.width(72.dp).padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isLive && match.homeScore.isNotBlank()) {
                    // Live score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${match.homeScore}  -  ${match.awayScore}",
                            color = ZakzoukaColors.LiveGreen,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                } else {
                    // Time badge
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier.clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (startingSoon) ZakzoukaColors.Gold.copy(alpha = 0.12f)
                                    else ZakzoukaColors.Surface
                                )
                                .border(
                                    1.dp,
                                    if (startingSoon) ZakzoukaColors.Gold.copy(alpha = 0.5f) else ZakzoukaColors.GoldBorder,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                timeStr,
                                color = if (startingSoon) ZakzoukaColors.Gold else ZakzoukaColors.TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (dateStr.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(dateStr, color = ZakzoukaColors.TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // Away Team
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                AsyncImage(
                    model = match.awayLogo.ifEmpty { null },
                    contentDescription = match.awayTeam,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(ZakzoukaColors.Surface),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    match.awayTeamArabic.ifBlank { match.awayTeam },
                    color = ZakzoukaColors.TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // ── Watch Broadcast Button ──
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (isLive) ZakzoukaColors.LiveGreen.copy(alpha = 0.12f) else ZakzoukaColors.Surface.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            val btnText = when {
                isLive -> "🔴 شاهد البث المباشر الآن"
                isFinished -> "✅ انتهت المباراة (${match.homeScore.ifBlank { "0" }} - ${match.awayScore.ifBlank { "0"}})"
                else -> "📺 مشاهدة القناة الناقلة للمباراة (لم تبدأ بعد)"
            }
            val btnColor = when {
                isLive -> ZakzoukaColors.LiveGreen
                isFinished -> ZakzoukaColors.TextMuted
                else -> ZakzoukaColors.Gold
            }
            Text(
                btnText,
                color = btnColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text("▶", color = if (isLive) ZakzoukaColors.LiveGreen else ZakzoukaColors.Gold, fontSize = 11.sp)
        }

        // Divider between matches
        Box(Modifier.fillMaxWidth().height(1.dp).background(ZakzoukaColors.GoldBorder.copy(alpha = 0.15f)))
    }
}

// ─── Finished Matches Widget (kept for backward compat) ──────────────────────────

@Composable
fun FinishedMatchesWidget(
    matches: List<FootballMatch>,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    val finished = remember(matches) { matches.filter { it.state == "post" } }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(
            Modifier.fillMaxWidth().background(ZakzoukaColors.Surface).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("نتائج المباريات المنتهية", color = ZakzoukaColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (loading) {
            ZakzoukaMatchSkeleton(Modifier.fillMaxWidth().height(120.dp))
        } else if (finished.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد نتائج", color = ZakzoukaColors.TextMuted, fontSize = 13.sp)
            }
        } else {
            finished.forEach { match ->
                FootballMatchRow(match = match)
            }
        }
    }
}
