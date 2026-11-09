package dev.buni.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.Language
import dev.buni.core.ThemeMode
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.buniColors
import dev.buni.ui.components.BuniLogoIcon
import dev.buni.ui.components.ThumbDock
import kotlinx.coroutines.launch

@Composable
fun AppShell(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    isDarkMode: Boolean,
    currentThemeMode: ThemeMode,
    currentLanguage: Language,
    onThemeModeChange: (ThemeMode) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onTogglePower: () -> Unit
) {
    var showingAbout by remember { mutableStateOf(false) }

    if (showingAbout) {
        AboutScreen(onNavigateBack = { showingAbout = false })
        return
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val colors = buniColors
    val focusManager = LocalFocusManager.current

    LaunchedEffect(pagerState.currentPage) {
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        FluxHeader(
            currentPage = pagerState.currentPage,
            isActive = isActive
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                        currentThemeMode = currentThemeMode,
                        currentLanguage = currentLanguage,
                        onThemeModeChange = onThemeModeChange,
                        onToggleLanguage = onToggleLanguage,
                        onOpenAbout = { showingAbout = true }
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
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun FluxHeader(
    currentPage: Int,
    isActive: Boolean
) {
    val colors = buniColors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 3) {
                val isSelected = i == currentPage
                val dotWidth by animateDpAsState(
                    targetValue = if (isSelected) 22.dp else 5.dp,
                    label = "flux_dot_$i"
                )
                Box(
                    modifier = Modifier
                        .height(5.dp)
                        .width(dotWidth)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(if (isSelected) colors.accent else colors.borderLight)
                )
                if (i < 2) {
                    Spacer(modifier = Modifier.width(6.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BuniLogoIcon(
                    modifier = Modifier.size(24.dp),
                    tint = colors.textPrimary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    val title = when (currentPage) {
                        0 -> buniString("hdr_shield_title")
                        1 -> buniString("hdr_apps_title")
                        else -> buniString("hdr_settings_title")
                    }
                    val subtitle = when (currentPage) {
                        0 -> if (isActive) buniString("hdr_shield_sub_active") else buniString("hdr_shield_sub_idle")
                        1 -> buniString("hdr_apps_sub")
                        else -> buniString("hdr_settings_sub")
                    }

                    Text(
                        text = title,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface)
                    .border(1.dp, if (isActive) Emerald500.copy(alpha = 0.5f) else colors.border, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) Emerald500 else colors.textMuted)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (isActive) buniString("status_shield") else buniString("status_standby"),
                        color = if (isActive) colors.textPrimary else colors.textMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )
    }
}
