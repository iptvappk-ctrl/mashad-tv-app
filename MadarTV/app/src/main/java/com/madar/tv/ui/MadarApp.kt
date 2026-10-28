package com.madar.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madar.tv.data.ArabseedItem
import com.madar.tv.ui.theme.MashadColors

private data class NavItem(val label: String, val icon: String)

private val navItems = listOf(
    NavItem("أفلام ومسلسلات", "🎬"),
    NavItem("بثات الكورة",    "⚽"),
    NavItem("المفضلة",        "❤️"),
)

@Composable
fun MadarApp() {
    val context = LocalContext.current
    val introPreferences = remember(context) {
        context.getSharedPreferences("mashad_ui", android.content.Context.MODE_PRIVATE)
    }
    var showIntro by rememberSaveable {
        mutableStateOf(!introPreferences.getBoolean("intro_seen", false))
    }
    var selectedTab  by rememberSaveable { mutableIntStateOf(0) }
    var isDeepScreen by rememberSaveable { mutableStateOf(false) }
    val finishIntro = remember(introPreferences) {
        {
            introPreferences.edit().putBoolean("intro_seen", true).apply()
            showIntro = false
        }
    }

    // Shared item opened from Favorites → Cinema section handles it
    var favoritePendingItem by remember { mutableStateOf<ArabseedItem?>(null) }

    BackHandler(enabled = !isDeepScreen && selectedTab != 0) {
        selectedTab = 0
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MashadColors.Background
    ) {
        if (showIntro) {
            MashadIntroScreen(onFinish = finishIntro)
        } else {
            Scaffold(
                containerColor = MashadColors.Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    if (!isDeepScreen) {
                        NavigationBar(
                            containerColor = MashadColors.Surface,
                            tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                        ) {
                            navItems.forEachIndexed { index, item ->
                                val selected = selectedTab == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTab = index },
                                    icon = {
                                        Text(
                                            text = item.icon,
                                            fontSize = if (index == 2) 18.sp else 20.sp
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (selected) MashadColors.Gold else MashadColors.TextMuted
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor   = MashadColors.Gold,
                                        unselectedIconColor = MashadColors.TextMuted,
                                        indicatorColor      = MashadColors.Gold.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(
                        bottom = if (isDeepScreen) 0.dp else paddingValues.calculateBottomPadding(),
                    ),
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val direction = if (targetState >= initialState) 1 else -1
                            (fadeIn(tween(190, easing = FastOutSlowInEasing)) +
                                slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { direction * it / 20 }) togetherWith
                                (fadeOut(tween(120)) +
                                    slideOutHorizontally(tween(180, easing = FastOutSlowInEasing)) { -direction * it / 24 })
                        },
                        label = "primary-tab-transition",
                    ) { tab ->
                        when (tab) {
                            0 -> CinemaSection(
                                onSearch = {},
                                onDeepScreen = { isDeepScreen = it },
                                pendingItem = favoritePendingItem,
                                onPendingItemConsumed = { favoritePendingItem = null },
                            )
                            1 -> LiveTvScreen(
                                onPlayerStateChange = { isPlayerActive -> isDeepScreen = isPlayerActive },
                            )
                            2 -> FavoritesScreen(
                                onOpen = { item ->
                                    favoritePendingItem = item
                                    selectedTab = 0
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
