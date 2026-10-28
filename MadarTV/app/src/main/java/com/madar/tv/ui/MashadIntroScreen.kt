package com.madar.tv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madar.tv.R
import com.madar.tv.ui.theme.MashadColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MashadIntroScreen(onFinish: () -> Unit) {
    val logoScale = remember { Animatable(0.76f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val sloganAlpha = remember { Animatable(0f) }

    val ambient = rememberInfiniteTransition(label = "intro-ambient")
    val orbitRotation by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6200, easing = LinearEasing)),
        label = "logo-orbit",
    )
    val pulse by ambient.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(
            tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo-pulse",
    )
    val dotsAlpha by ambient.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(620, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "intro-dots",
    )

    LaunchedEffect(Unit) {
        launch {
            logoAlpha.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        }
        launch {
            logoScale.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        }
        delay(220)
        launch { titleAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        delay(140)
        launch { sloganAlpha.animateTo(1f, tween(360, easing = FastOutSlowInEasing)) }
        delay(780)
        onFinish()
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                colors = listOf(
                    MashadColors.Background,
                    MashadColors.SurfaceHighlight.copy(alpha = 0.58f),
                    MashadColors.Background,
                ),
            ),
        ),
    ) {
        Box(
            Modifier.align(Alignment.TopStart).offset(x = (-150).dp, y = (-125).dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MashadColors.Gold.copy(alpha = 0.15f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )
        Box(
            Modifier.align(Alignment.BottomEnd).offset(x = 160.dp, y = 105.dp)
                .size(330.dp)
                .background(
                    Brush.radialGradient(
                        listOf(MashadColors.GoldLight.copy(alpha = 0.14f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        Box(
            Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MashadColors.Surface.copy(alpha = 0.86f))
                .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(20.dp))
                .clickable(onClick = onFinish)
                .padding(horizontal = 15.dp, vertical = 8.dp),
        ) {
            Text("تخطي", color = MashadColors.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier.align(Alignment.Center).padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.size(178.dp).scale(logoScale.value * pulse).alpha(logoAlpha.value),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(166.dp).rotate(orbitRotation).clip(CircleShape)
                        .border(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    MashadColors.Gold,
                                    MashadColors.GoldLight,
                                    MashadColors.LiveGreen,
                                    MashadColors.Gold,
                                ),
                            ),
                            CircleShape,
                        ),
                )
                Box(
                    Modifier.size(148.dp).clip(CircleShape)
                        .background(MashadColors.Surface)
                        .border(1.dp, MashadColors.GoldBorder, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.mashad_logo),
                        contentDescription = "شعار مشهد",
                        modifier = Modifier.size(132.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                }
                Box(
                    Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape)
                        .background(MashadColors.Gold)
                        .border(2.dp, MashadColors.Background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("✦", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(25.dp))
            Text(
                text = "مَــشْــهَــد",
                color = MashadColors.GoldLight,
                fontSize = 35.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha.value),
            )
            Spacer(Modifier.height(9.dp))
            Text(
                text = "كل الحكايات، في مكان واحد",
                color = MashadColors.TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(sloganAlpha.value),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "أفلام ومسلسلات وبث مباشر",
                color = MashadColors.TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(sloganAlpha.value),
            )
            Spacer(Modifier.height(29.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(sloganAlpha.value),
            ) {
                repeat(3) { index ->
                    Box(
                        Modifier.size(if (index == 1) 7.dp else 5.dp).clip(CircleShape)
                            .background(MashadColors.Gold.copy(alpha = dotsAlpha * (1f - index * 0.18f))),
                    )
                }
            }
        }
    }
}
