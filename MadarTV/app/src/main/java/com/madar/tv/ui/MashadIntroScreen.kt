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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

@Composable
fun MashadIntroScreen(onFinish: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }
    val sloganAlpha = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        // Animate Logo
        alpha.animateTo(1f, animationSpec = tween(700, easing = LinearEasing))
        scale.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))

        // Animate Title
        textAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))

        // Animate Slogan
        sloganAlpha.animateTo(1f, animationSpec = tween(700, easing = FastOutSlowInEasing))

        // Hold and complete
        delay(1200)
        onFinish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF07080A))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinish
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient Radial Golden Background Glow
        Box(
            Modifier
                .size(340.dp)
                .scale(pulseGlow)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MashadColors.Gold.copy(alpha = 0.22f),
                            MashadColors.GoldDark.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Skip Button in Top Left (or Top Right depending on layout)
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = 44.dp, end = 20.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, MashadColors.GoldBorder, RoundedCornerShape(20.dp))
                    .clickable(onClick = onFinish)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "تخطي ❯",
                    color = MashadColors.TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Creative Logo with Golden Ring & Pulse
            Box(
                Modifier
                    .size(140.dp)
                    .scale(scale.value)
                    .alpha(alpha.value),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing circle
                Box(
                    Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, Brush.sweepGradient(listOf(MashadColors.Gold, MashadColors.GoldLight, MashadColors.GoldDark, MashadColors.Gold)), CircleShape)
                )

                // Logo Image
                Image(
                    painter = painterResource(id = R.drawable.mashad_logo),
                    contentDescription = "مشهد",
                    modifier = Modifier
                        .size(126.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(24.dp))

            // App Title in Luxury Gold
            Text(
                text = "مَــشْــهَــد",
                color = MashadColors.GoldLight,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(textAlpha.value)
            )

            Spacer(Modifier.height(10.dp))

            // Expressive Slogan
            Text(
                text = "عالمك السينمائي بين يديك · متعة المشاهدة بلا حدود وبدون إعلانات",
                color = MashadColors.TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .alpha(sloganAlpha.value)
            )

            Spacer(Modifier.height(32.dp))

            // Elegant Loading Indicator Dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.alpha(sloganAlpha.value)
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(MashadColors.Gold))
                Box(Modifier.size(6.dp).clip(CircleShape).background(MashadColors.Gold.copy(alpha = 0.5f)))
                Box(Modifier.size(6.dp).clip(CircleShape).background(MashadColors.Gold.copy(alpha = 0.25f)))
            }
        }
    }
}
