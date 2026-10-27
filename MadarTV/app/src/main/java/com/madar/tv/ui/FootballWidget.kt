package com.madar.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun FootballMatchesWidget(
    matches: List<FootballMatch>,
    loading: Boolean,
    onRefresh: () -> Unit,
) {
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filters = listOf(
        "الكل",
        "دوري أبطال أوروبا",
        "الدوري الإنجليزي",
        "الدوري الإسباني",
        "الدوري الإيطالي",
        "الدوري الألماني",
        "الدوري الفرنسي",
    )

    val filteredMatches = remember(matches, selectedFilter) {
        if (selectedFilter == "الكل") matches else matches.filter { it.leagueName == selectedFilter }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🏆", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "مباريات اليوم والدوريات الكبرى",
                    color = MashadColors.TextWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Row(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MashadColors.SurfaceCard)
                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onRefresh)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "تحديث 🔄",
                    color = MashadColors.GoldLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // League Filter Chips
        LazyRow(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filters) { f ->
                val active = f == selectedFilter
                Box(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active) MashadColors.Gold else MashadColors.SurfaceCard)
                        .border(1.dp, if (active) MashadColors.Gold else MashadColors.Border, RoundedCornerShape(16.dp))
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = f,
                        color = if (active) Color.Black else MashadColors.TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // Matches Horizontal Carousel
        if (loading && matches.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MashadColors.Gold, modifier = Modifier.size(24.dp))
            }
        } else if (filteredMatches.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = "لا توجد مباريات مجدولة حالياً لهذا الدوري اليوم",
                    color = MashadColors.TextMuted,
                    fontSize = 12.sp,
                )
            }
        } else {
            LazyRow(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredMatches, key = { it.id }) { match ->
                    FootballMatchCard(match = match)
                }
            }
        }
    }
}

@Composable
fun FootballMatchCard(match: FootballMatch) {
    Box(
        Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MashadColors.Surface)
            .border(
                1.dp,
                if (match.state == "in") MashadColors.LiveGreen else MashadColors.GoldBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // League Name + Status Tag
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = match.leagueName,
                    color = MashadColors.GoldLight,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (match.state == "in") MashadColors.LiveGreen.copy(alpha = 0.2f)
                            else MashadColors.SurfaceCard
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = match.timeOrStatus,
                        color = if (match.state == "in") MashadColors.LiveGreen else MashadColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Teams Row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Home Team
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (match.homeLogo.isNotBlank()) {
                        AsyncImage(
                            model = match.homeLogo,
                            contentDescription = match.homeTeamArabic,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(MashadColors.SurfaceCard))
                    }
                    Text(
                        text = match.homeTeamArabic,
                        color = MashadColors.TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }

                // Center Score or VS
                Column(
                    Modifier.padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.state != "pre" && match.homeScore.isNotBlank()) {
                        Text(
                            text = "${match.homeScore} - ${match.awayScore}",
                            color = if (match.state == "in") MashadColors.LiveGreen else MashadColors.Gold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    } else {
                        Text(
                            text = "VS",
                            color = MashadColors.GoldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Away Team
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (match.awayLogo.isNotBlank()) {
                        AsyncImage(
                            model = match.awayLogo,
                            contentDescription = match.awayTeamArabic,
                            modifier = Modifier.size(36.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(Modifier.size(36.dp).clip(CircleShape).background(MashadColors.SurfaceCard))
                    }
                    Text(
                        text = match.awayTeamArabic,
                        color = MashadColors.TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
