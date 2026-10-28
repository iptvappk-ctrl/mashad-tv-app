package com.madar.tv.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.madar.tv.ui.theme.MashadColors

@Composable
fun MashadCatalogSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerOffset()
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                ShimmerBlock(shimmer, Modifier.width(136.dp).height(15.dp))
                Box(
                    Modifier.fillMaxWidth().height(190.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(MashadColors.SurfaceHighlight)
                        .shimmerMask(shimmer),
                ) {
                    Column(
                        Modifier.align(Alignment.BottomStart).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ShimmerBlock(shimmer, Modifier.width(100.dp).height(9.dp))
                        ShimmerBlock(shimmer, Modifier.width(174.dp).height(16.dp))
                        ShimmerBlock(shimmer, Modifier.width(118.dp).height(29.dp), RoundedCornerShape(16.dp))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                ShimmerBlock(shimmer, Modifier.width(112.dp).height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        PosterSkeleton(shimmer, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                ShimmerBlock(shimmer, Modifier.width(148.dp).height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(3) {
                        PosterSkeleton(shimmer, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun MashadChannelSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerOffset()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(6) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MashadColors.Surface).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                ShimmerBlock(shimmer, Modifier.size(58.dp), CircleShape)
                ShimmerBlock(shimmer, Modifier.fillMaxWidth(0.76f).height(11.dp))
                ShimmerBlock(shimmer, Modifier.fillMaxWidth(0.48f).height(8.dp))
            }
        }
    }
}

@Composable
fun MashadMatchSkeleton(modifier: Modifier = Modifier) {
    val shimmer = rememberShimmerOffset()
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(MashadColors.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        ShimmerBlock(shimmer, Modifier.width(112.dp).height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ShimmerBlock(shimmer, Modifier.size(36.dp), CircleShape)
                ShimmerBlock(shimmer, Modifier.width(58.dp).height(8.dp))
            }
            ShimmerBlock(shimmer, Modifier.width(62.dp).height(31.dp), RoundedCornerShape(9.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                ShimmerBlock(shimmer, Modifier.size(36.dp), CircleShape)
                ShimmerBlock(shimmer, Modifier.width(58.dp).height(8.dp))
            }
        }
        ShimmerBlock(shimmer, Modifier.fillMaxWidth(0.6f).height(8.dp))
    }
}

@Composable
private fun PosterSkeleton(shimmer: State<Float>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        ShimmerBlock(
            shimmer,
            Modifier.fillMaxWidth().aspectRatio(0.70f),
            RoundedCornerShape(14.dp),
        )
        ShimmerBlock(shimmer, Modifier.fillMaxWidth(0.88f).height(9.dp))
        ShimmerBlock(shimmer, Modifier.fillMaxWidth(0.55f).height(8.dp))
    }
}

@Composable
private fun ShimmerBlock(
    shimmer: State<Float>,
    modifier: Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
) {
    Spacer(
        modifier.clip(shape)
            .background(MashadColors.SurfaceHighlight)
            .shimmerMask(shimmer),
    )
}

@Composable
private fun rememberShimmerOffset(): State<Float> {
    val transition = rememberInfiniteTransition(label = "mashad-skeleton-shimmer")
    return transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-position",
    )
}

private fun Modifier.shimmerMask(shimmer: State<Float>): Modifier = drawWithCache {
    val startX = (shimmer.value - 1f) * size.width
    val brush = Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            MashadColors.SurfaceCard.copy(alpha = 0.78f),
            Color.Transparent,
        ),
        start = Offset(startX, 0f),
        end = Offset(startX + size.width, size.height),
    )
    onDrawWithContent {
        drawContent()
        drawRect(brush)
    }
}
