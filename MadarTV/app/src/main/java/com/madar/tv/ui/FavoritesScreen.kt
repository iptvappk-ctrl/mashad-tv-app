package com.madar.tv.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.madar.tv.data.ArabseedItem
import com.madar.tv.data.ArabseedType
import com.madar.tv.data.FavoritesRepository
import com.madar.tv.ui.theme.MashadColors
import kotlinx.coroutines.launch

// ─── Favorites Screen ─────────────────────────────────────────────────────────

@Composable
fun FavoritesScreen(
    onOpen: (ArabseedItem) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Reactive list — updates instantly when items are added/removed
    val favorites by FavoritesRepository.getFavoritesFlow(context)
        .collectAsState(initial = emptyList())

    Column(
        Modifier
            .fillMaxSize()
            .background(MashadColors.Background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    Brush.horizontalGradient(
                        listOf(MashadColors.Surface, MashadColors.SurfaceCard)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("❤️", fontSize = 22.sp)
                    Text(
                        "المفضلة",
                        color = MashadColors.GoldLight,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (favorites.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${favorites.size} عنصر محفوظ",
                        color = MashadColors.TextMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Gold divider line
        Box(Modifier.fillMaxWidth().height(1.dp).background(MashadColors.GoldBorder.copy(alpha = 0.4f)))

        // ── Content ───────────────────────────────────────────────────────────
        if (favorites.isEmpty()) {
            // Empty state
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("💔", fontSize = 64.sp)
                    Text(
                        "لا يوجد محتوى في المفضلة",
                        color = MashadColors.TextWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        "اضغط على ❤️ على أي فيلم أو مسلسل\nلإضافته هنا",
                        color = MashadColors.TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 14.dp, bottom = 40.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favorites, key = { it.pageUrl }) { item ->
                    FavoriteCard(
                        item = item,
                        onClick = { onOpen(item) },
                        onRemove = {
                            scope.launch {
                                FavoritesRepository.removeFavorite(context, item.pageUrl)
                            }
                        },
                    )
                }
            }
        }
    }
}

// ─── Favorite Card ────────────────────────────────────────────────────────────

@Composable
fun FavoriteCard(
    item: ArabseedItem,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "card_scale",
    )

    Column(
        Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable {
                pressed = true
                onClick()
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MashadColors.Surface)
                .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(10.dp)),
        ) {
            // Poster
            if (item.posterUrl != null) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            // Gradient overlay at bottom
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.45f)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )

            // Quality badge top-left
            if (item.quality.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MashadColors.Gold)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(item.quality, color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Type badge top-right
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xCC000000))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    if (item.type == ArabseedType.MOVIE) "فيلم" else "مسلسل",
                    color = Color.White,
                    fontSize = 8.sp,
                )
            }

            // ❤️ Remove button - bottom left
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xCC1A1A2E))
                    .border(1.dp, MashadColors.Red.copy(alpha = 0.6f), CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) {
                Text("❤️", fontSize = 12.sp)
            }

            // Year bottom right
            if (item.year.isNotBlank()) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    Text(item.year, color = Color.White.copy(alpha = 0.9f), fontSize = 8.sp)
                }
            }
        }

        // Title
        Text(
            text = item.arabicTitle.ifBlank { item.title },
            color = MashadColors.TextWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp,
        )
    }
}

// ─── Heart Button (reusable, used on cards and detail screen) ─────────────────

@Composable
fun FavoriteHeartButton(
    item: ArabseedItem,
    modifier: Modifier = Modifier,
    size: Int = 36,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isFav by remember { mutableStateOf(false) }

    // Load initial state
    LaunchedEffect(item.pageUrl) {
        isFav = FavoritesRepository.isFavorite(context, item.pageUrl)
    }

    // Animated scale on press
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 1.35f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        finishedListener = { pressed = false },
        label = "heart_bounce",
    )

    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                if (isFav) MashadColors.Red.copy(alpha = 0.18f)
                else Color(0xCC000000)
            )
            .border(
                1.dp,
                if (isFav) MashadColors.Red.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                CircleShape,
            )
            .clickable {
                pressed = true
                scope.launch {
                    isFav = FavoritesRepository.toggleFavorite(context, item)
                }
            }
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = isFav,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Text("❤️", fontSize = (size * 0.42f).sp)
        }
        AnimatedVisibility(
            visible = !isFav,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
        ) {
            Text("🤍", fontSize = (size * 0.42f).sp)
        }
    }
}
