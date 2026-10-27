package com.madar.tv.ui

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madar.tv.ui.theme.MashadColors

private data class NavItem(val label: String, val icon: String)

private val navItems = listOf(
    NavItem("أفلام ومسلسلات", "🎬"),
    NavItem("بثات الكورة", "⚽"),
)

@Composable
fun MadarApp() {
    var showIntro by rememberSaveable { mutableStateOf(true) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var isDeepScreen by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MashadColors.Background
    ) {
        if (showIntro) {
            MashadIntroScreen(onFinish = { showIntro = false })
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
                                            fontSize = 20.sp
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
                                        selectedIconColor = MashadColors.Gold,
                                        unselectedIconColor = MashadColors.TextMuted,
                                        indicatorColor = MashadColors.Gold.copy(alpha = 0.12f)
                                    )
                                )
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier.padding(
                        bottom = if (isDeepScreen) 0.dp else paddingValues.calculateBottomPadding()
                    )
                ) {
                    when (selectedTab) {
                        0 -> CinemaSection(
                            onSearch = {},
                            onDeepScreen = { isDeepScreen = it }
                        )
                        1 -> LiveTvScreen(
                            onPlayerStateChange = { isPlayerActive -> isDeepScreen = isPlayerActive }
                        )
                    }
                }
            }
        }
    }
}
