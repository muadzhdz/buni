package dev.buni.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.buni.theme.Black
import dev.buni.ui.components.NavTab
import dev.buni.ui.components.ThumbDock

@Composable
fun AppShell(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    onTogglePower: () -> Unit
) {
    var currentTab by remember { mutableStateOf(NavTab.SHIELD) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
    ) {
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "screen_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { tab ->
            when (tab) {
                NavTab.SHIELD -> DashboardScreen(
                    isActive = isActive,
                    stats = stats,
                    logs = logs,
                    onTogglePower = onTogglePower
                )
                NavTab.APPS -> AppsScreen()
                NavTab.RULES -> RulesScreen()
                NavTab.NODE -> NodeScreen()
            }
        }

        ThumbDock(
            currentTab = currentTab,
            onTabSelected = { currentTab = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )
    }
}
