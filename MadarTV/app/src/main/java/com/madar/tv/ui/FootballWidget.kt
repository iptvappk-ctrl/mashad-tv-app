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
import com.madar.tv.ui.theme.MashadColors
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

    val liveAndUpcoming = remember(matches, currentTime) {
        matches.filter { m ->
            m.state == "in" || m.state == "pre" ||
            (m.kickoffEpochMs != null && m.kickoffEpochMs > currentTime - 2 * 60 * 60 * 1000)
        }.sortedWith(compareBy(
            // live first
            { if (it.state == "in") 0 else 1 },
            // then by kickoff time
            { it.kickoffEpochMs ?: Long.MAX_VALUE }
        ))
    }

    val filtered = remember(liveAndUpcoming, selectedFilter) {
        if (selectedFilter == "الكل") liveAndUpcoming
        else liveAndUpcoming.filter { it.leagueName.contains(selectedFilter, ignoreCase = true) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {

        // ── Header ──────────────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(MashadColors.Surface, MashadColors.SurfaceCard)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(MashadColors.LiveGreen))
                Text(
                    "المباريات القادمة والمباشرة",
                    color = MashadColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier.clip(RoundedCornerShape(20.dp))
                    .background(MashadColors.Gold.copy(alpha = 0.12f))
                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("تحديث", color = MashadColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("🔄", fontSize = 11.sp)
            }
        }

        // ── League Filter Chips ──────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .background(MashadColors.Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LEAGUE_FILTERS.forEach { league ->
                val sel = selectedFilter == league
                val chipBg by animateColorAsState(
                    if (sel) MashadColors.Gold else MashadColors.SurfaceCard,
                    label = "chip",
                )
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(chipBg)
                        .border(1.dp, if (sel) MashadColors.Gold else MashadColors.GoldBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedFilter = league }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        league,
                        color = if (sel) Color.White else MashadColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // Divider
        Box(Modifier.fillMaxWidth().height(1.dp).background(MashadColors.GoldBorder.copy(alpha = 0.3f)))

        // ── Match List ───────────────────────────────────────────────────────
        when {
            loading -> MashadMatchSkeleton(Modifier.fillMaxWidth().height(180.dp))
            filtered.isEmpty() -> Box(
                Modifier.fillMaxWidth().height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("⚽", fontSize = 32.sp)
                    Text("لا توجد مباريات قادمة حالياً", color = MashadColors.TextMuted, fontSize = 13.sp)
                    Text(
                        "تحديث 🔄",
                        color = MashadColors.Gold,
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
            }
        }
    }
}

// ─── Single Match Row (WeCima-inspired style) ─────────────────────────────────────

@Composable
fun FootballMatchRow(match: FootballMatch, currentTime: Long = System.currentTimeMillis(), onClick: (() -> Unit)? = null) {
    val isLive = match.state == "in"
    val kickoffMs = match.kickoffEpochMs
    val timeStr = if (kickoffMs != null) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(kickoffMs))
    } else match.timeOrStatus

    val dateStr = if (kickoffMs != null) {
        SimpleDateFormat("EEE d MMM", Locale.forLanguageTag("ar")).format(Date(kickoffMs))
    } else ""

    val minutesUntil = if (kickoffMs != null) ((kickoffMs - currentTime) / 60_000).toInt() else Int.MAX_VALUE
    val startingSoon = minutesUntil in 0..30 && !isLive

    // Pulsing animation for live dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val pulsAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 1f, label = "pulse",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
    )

    Column(
        Modifier.fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .background(MashadColors.SurfaceCard)
            .border(
                width = if (isLive) 1.dp else 0.dp,
                color = if (isLive) MashadColors.LiveGreen.copy(alpha = 0.4f) else Color.Transparent,
            ),
    ) {
        // ── League banner ────────────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth()
                .background(if (isLive) MashadColors.LiveGreen.copy(alpha = 0.08f) else MashadColors.Surface.copy(alpha = 0.6f))
                .padding(horizontal = 16.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("🏆", fontSize = 11.sp)
                Text(
                    match.leagueName.ifBlank { "مباراة" },
                    color = MashadColors.GoldLight,
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
                        .background(MashadColors.LiveGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(MashadColors.LiveGreen.copy(alpha = pulsAlpha)))
                    Text("مباشر الآن", color = MashadColors.LiveGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                startingSoon -> Box(
                    Modifier.clip(RoundedCornerShape(4.dp))
                        .background(MashadColors.Gold.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        "يبدأ خلال ${minutesUntil} دقيقة",
                        color = MashadColors.Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    )
                }
                else -> Column(horizontalAlignment = Alignment.End) {
                    Text(timeStr, color = MashadColors.TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (dateStr.isNotBlank()) Text(dateStr, color = MashadColors.TextMuted, fontSize = 9.sp)
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
                    color = MashadColors.TextWhite,
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
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MashadColors.Surface),
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
                            color = MashadColors.LiveGreen,
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
                                    if (startingSoon) MashadColors.Gold.copy(alpha = 0.12f)
                                    else MashadColors.Surface
                                )
                                .border(
                                    1.dp,
                                    if (startingSoon) MashadColors.Gold.copy(alpha = 0.5f) else MashadColors.GoldBorder,
                                    RoundedCornerShape(8.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                timeStr,
                                color = if (startingSoon) MashadColors.Gold else MashadColors.TextWhite,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        if (dateStr.isNotBlank()) {
                            Spacer(Modifier.height(3.dp))
                            Text(dateStr, color = MashadColors.TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
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
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MashadColors.Surface),
                    contentScale = ContentScale.Fit,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    match.awayTeamArabic.ifBlank { match.awayTeam },
                    color = MashadColors.TextWhite,
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
                .background(if (isLive) MashadColors.LiveGreen.copy(alpha = 0.12f) else MashadColors.Surface.copy(alpha = 0.7f))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                if (isLive) "🔴 شاهد البث المباشر الآن" else "📺 مشاهدة القناة الناقلة للمباراة",
                color = if (isLive) MashadColors.LiveGreen else MashadColors.Gold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(6.dp))
            Text("▶", color = if (isLive) MashadColors.LiveGreen else MashadColors.Gold, fontSize = 11.sp)
        }

        // Divider between matches
        Box(Modifier.fillMaxWidth().height(1.dp).background(MashadColors.GoldBorder.copy(alpha = 0.15f)))
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
            Modifier.fillMaxWidth().background(MashadColors.Surface).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("نتائج المباريات المنتهية", color = MashadColors.GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        if (loading) {
            MashadMatchSkeleton(Modifier.fillMaxWidth().height(120.dp))
        } else if (finished.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد نتائج", color = MashadColors.TextMuted, fontSize = 13.sp)
            }
        } else {
            finished.forEach { match ->
                FootballMatchRow(match = match)
            }
        }
    }
}
