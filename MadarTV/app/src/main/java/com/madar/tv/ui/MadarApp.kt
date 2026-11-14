package com.madar.tv.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madar.tv.data.ArabseedItem
import com.madar.tv.ui.theme.ZakzoukaColors

private data class NavItem(val label: String, val icon: String)

private val navItems = listOf(
    NavItem("أفلام ومسلسلات", "🎬"),
    NavItem("بثات الكورة",    "⚽"),
    NavItem("المفضلة",        "❤️"),
)

@Composable
fun MadarApp() {
    val context = LocalContext.current
    var showIntro by remember { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isDeepScreen by rememberSaveable { mutableStateOf(false) }
    val finishIntro: () -> Unit = remember {
        {
            showIntro = false
            Unit
        }
    }

    // Shared item opened from Favorites → Cinema section handles it
    var favoritePendingItem by remember { mutableStateOf<ArabseedItem?>(null) }

    // Back navigation on root tabs
    BackHandler(enabled = !isDeepScreen && selectedTab != 0) {
        selectedTab = 0
    }

    // Double-press back to exit app only from the home root screen
    var lastBackPressTime by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    BackHandler(enabled = !isDeepScreen && selectedTab == 0) {
        val now = System.currentTimeMillis()
        if (now - lastBackPressTime < 2000L) {
            (context as? android.app.Activity)?.finish()
        } else {
            lastBackPressTime = now
            android.widget.Toast.makeText(
                context,
                "اضغط مرة أخرى للخروج من زقزوقة",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = ZakzoukaColors.Background
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                    containerColor = ZakzoukaColors.Background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!isDeepScreen) {
                            NavigationBar(
                                containerColor = ZakzoukaColors.Surface,
                                tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                            ) {
                                navItems.forEachIndexed { index, item ->
                                    val selected = selectedTab == index
                                    val iconScale by androidx.compose.animation.core.animateFloatAsState(
                                        targetValue = if (selected) 1.08f else 1f,
                                        animationSpec = tween(170, easing = FastOutSlowInEasing),
                                        label = "nav-icon-scale-$index",
                                    )
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { selectedTab = index },
                                        icon = {
                                            Text(
                                                text = item.icon,
                                                fontSize = if (index == 2) 18.sp else 20.sp,
                                                modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) ZakzoukaColors.Gold else ZakzoukaColors.TextMuted
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor   = ZakzoukaColors.Gold,
                                            unselectedIconColor = ZakzoukaColors.TextMuted,
                                            indicatorColor      = ZakzoukaColors.Gold.copy(alpha = 0.12f)
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
                        when (selectedTab) {
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
            if (showIntro) {
                ZakzoukaIntroScreen(onFinish = finishIntro)
            }
        }
    }
}
