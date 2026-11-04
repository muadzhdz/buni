package dev.buni.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.buni.core.Language
import dev.buni.theme.buniColors
import dev.buni.ui.components.NavTab
import dev.buni.ui.components.ThumbDock
import kotlinx.coroutines.launch

@Composable
fun AppShell(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    isDarkMode: Boolean,
    currentLanguage: Language,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onTogglePower: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val colors = buniColors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> DashboardScreen(
                    isActive = isActive,
                    stats = stats,
                    logs = logs,
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    onToggleTheme = onToggleTheme,
                    onToggleLanguage = onToggleLanguage,
                    onTogglePower = onTogglePower
                )
                1 -> AppsScreen()
                2 -> SettingsScreen(
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    onToggleTheme = onToggleTheme,
                    onToggleLanguage = onToggleLanguage
                )
            }
        }

        ThumbDock(
            currentPage = pagerState.currentPage,
            pageOffsetFraction = pagerState.currentPageOffsetFraction,
            onTabSelected = { tab ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(tab.ordinal)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        )
    }
}
