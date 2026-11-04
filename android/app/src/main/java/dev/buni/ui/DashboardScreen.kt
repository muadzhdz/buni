package dev.buni.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.Language
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.buniColors

enum class EngineMode {
    SHIELD,
    TUNNEL
}

@Composable
fun DashboardScreen(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    isDarkMode: Boolean,
    currentLanguage: Language,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onTogglePower: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(EngineMode.SHIELD) }
    val scrollState = rememberScrollState()
    val colors = buniColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        HeaderSection(
            isActive = isActive,
            latencyMs = stats.third,
            mode = selectedMode,
            isDarkMode = isDarkMode,
            currentLanguage = currentLanguage,
            onToggleTheme = onToggleTheme,
            onToggleLanguage = onToggleLanguage
        )

        Spacer(modifier = Modifier.height(24.dp))

        SlidingModeSelector(
            currentMode = selectedMode,
            onModeSelect = { selectedMode = it }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            MasterPowerButton(
                isActive = isActive,
                mode = selectedMode,
                onToggle = onTogglePower
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        TelemetryStrip(
            queries = stats.first,
            blocked = stats.second,
            latency = stats.third
        )

        Spacer(modifier = Modifier.height(20.dp))

        InspectionConsole(logs = logs)

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun HeaderSection(
    isActive: Boolean,
    latencyMs: Long,
    mode: EngineMode,
    isDarkMode: Boolean,
    currentLanguage: Language,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val colors = buniColors

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "buni",
                color = colors.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = if (mode == EngineMode.SHIELD) {
                    buniString("app_tagline")
                } else {
                    buniString("tunnel_tagline")
                },
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                    .clickable(onClick = onToggleLanguage)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (currentLanguage == Language.EN) "EN" else "ID",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isDarkMode) "☾" else "☼",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Emerald500 else colors.textMuted)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isActive) "${latencyMs}ms" else buniString("standby"),
                    color = if (isActive) colors.textPrimary else colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SlidingModeSelector(
    currentMode: EngineMode,
    onModeSelect: (EngineMode) -> Unit
) {
    val colors = buniColors

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .padding(4.dp)
    ) {
        val tabWidth = maxWidth / 2
        val targetOffset = if (currentMode == EngineMode.SHIELD) 0.dp else tabWidth

        val indicatorOffset by animateDpAsState(
            targetValue = targetOffset,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pill_slide"
        )

        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.surfaceVariant)
                .border(1.dp, colors.borderLight, RoundedCornerShape(8.dp))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            ModeTab(
                title = buniString("shield_tab"),
                subtitle = buniString("shield_sub"),
                selected = currentMode == EngineMode.SHIELD,
                onClick = { onModeSelect(EngineMode.SHIELD) },
                modifier = Modifier.weight(1f)
            )
            ModeTab(
                title = buniString("tunnel_tab"),
                subtitle = buniString("tunnel_sub"),
                selected = currentMode == EngineMode.TUNNEL,
                onClick = { onModeSelect(EngineMode.TUNNEL) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModeTab(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = buniColors

    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = if (selected) colors.textPrimary else colors.textMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            color = if (selected) colors.textSecondary else colors.textMuted,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun MasterPowerButton(
    isActive: Boolean,
    mode: EngineMode,
    onToggle: () -> Unit
) {
    val colors = buniColors
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Emerald500 else colors.borderLight,
        animationSpec = tween(200),
        label = "btn_border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(width = 96.dp, height = 96.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(2.dp, borderColor, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) Emerald500 else colors.textMuted)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isActive) {
                if (mode == EngineMode.SHIELD) buniString("shield_engaged") else buniString("tunnel_engaged")
            } else {
                if (mode == EngineMode.SHIELD) buniString("tap_to_shield") else buniString("tap_to_tunnel")
            },
            color = if (isActive) colors.textPrimary else colors.textMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (mode == EngineMode.SHIELD) {
                buniString("shield_desc")
            } else {
                buniString("tunnel_desc")
            },
            color = colors.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TelemetryStrip(
    queries: Long,
    blocked: Long,
    latency: Long
) {
    val colors = buniColors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = buniString("queries"), value = queries.toString())
        StatItem(label = buniString("blocked"), value = blocked.toString())
        StatItem(label = buniString("latency"), value = "${latency}ms")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    val colors = buniColors

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = colors.textMuted,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InspectionConsole(logs: List<String>) {
    val colors = buniColors
    var filterBlockedOnly by remember { mutableStateOf(false) }

    val displayedLogs = remember(logs, filterBlockedOnly) {
        if (filterBlockedOnly) {
            logs.filter {
                it.contains("SINK", ignoreCase = true) ||
                it.contains("BLOCK", ignoreCase = true) ||
                it.contains("DROP", ignoreCase = true)
            }
        } else {
            logs
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = buniString("live_inspection"),
                color = colors.textMuted,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (!filterBlockedOnly) colors.surfaceVariant else Color.Transparent)
                        .clickable { filterBlockedOnly = false }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = buniString("all"),
                        color = if (!filterBlockedOnly) colors.textPrimary else colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (filterBlockedOnly) colors.surfaceVariant else Color.Transparent)
                        .clickable { filterBlockedOnly = true }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = buniString("blocked"),
                        color = if (filterBlockedOnly) Emerald500 else colors.textMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = "${displayedLogs.size} ${buniString("events")}",
                    color = colors.textMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (displayedLogs.isEmpty()) {
            Text(
                text = if (filterBlockedOnly) "> no blocked queries recorded yet" else buniString("awaiting_packets"),
                color = colors.textMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.height(180.dp)) {
                items(displayedLogs) { logLine ->
                    val isBlockedLine = logLine.contains("SINK", ignoreCase = true) ||
                                        logLine.contains("BLOCK", ignoreCase = true) ||
                                        logLine.contains("DROP", ignoreCase = true)
                    Text(
                        text = "> $logLine",
                        color = if (isBlockedLine) Emerald500 else colors.textSecondary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
