package dev.buni.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.EngineMode
import dev.buni.core.Language
import dev.buni.core.buniString
import dev.buni.theme.Emerald500
import dev.buni.theme.buniColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DashboardScreen(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    isDarkMode: Boolean,
    currentLanguage: Language,
    currentMode: EngineMode,
    onModeChange: (EngineMode) -> Unit,
    onToggleTheme: () -> Unit,
    onToggleLanguage: () -> Unit,
    onTogglePower: () -> Unit
) {
    val selectedMode = currentMode
    val scrollState = rememberScrollState()
    val colors = buniColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SlidingModeSelector(
            currentMode = selectedMode,
            onModeSelect = onModeChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            InteractivePowerButton(
                isActive = isActive,
                mode = selectedMode,
                onToggle = onTogglePower
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        UnifiedTelemetryCard(
            queries = stats.first,
            blocked = stats.second,
            latency = stats.third,
            logs = logs
        )

        Spacer(modifier = Modifier.height(100.dp))
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
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleLanguage)
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Text(
                    text = if (currentLanguage == Language.EN) "EN" else "ID",
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .clickable(onClick = onToggleTheme)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                ThemeToggleIcon(
                    isDarkMode = isDarkMode,
                    color = colors.textPrimary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
private fun ThemeToggleIcon(isDarkMode: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        if (isDarkMode) {
            val center = Offset(size.width * 0.44f, size.height * 0.5f)
            val radius = size.minDimension * 0.44f
            val baseCircle = Path().apply {
                addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
            }
            val cutoutCenter = Offset(center.x + radius * 0.52f, center.y - radius * 0.36f)
            val cutoutRadius = radius * 0.88f
            val cutoutCircle = Path().apply {
                addOval(Rect(cutoutCenter.x - cutoutRadius, cutoutCenter.y - cutoutRadius, cutoutCenter.x + cutoutRadius, cutoutCenter.y + cutoutRadius))
            }
            val crescent = Path().apply {
                op(baseCircle, cutoutCircle, PathOperation.Difference)
            }
            drawPath(path = crescent, color = color)
        } else {
            val center = Offset(size.width / 2f, size.height / 2f)
            val centerRadius = size.minDimension * 0.22f
            drawCircle(color = color, radius = centerRadius, center = center)

            val rayCount = 8
            val rayInner = size.minDimension * 0.34f
            val rayOuter = size.minDimension * 0.48f
            for (i in 0 until rayCount) {
                val angle = (i * 45f) * (Math.PI / 180f).toFloat()
                val start = Offset(center.x + rayInner * cos(angle), center.y + rayInner * sin(angle))
                val end = Offset(center.x + rayOuter * cos(angle), center.y + rayOuter * sin(angle))
                drawLine(color = color, start = start, end = end, strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
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
private fun InteractivePowerButton(
    isActive: Boolean,
    mode: EngineMode,
    onToggle: () -> Unit
) {
    val colors = buniColors
    var isPressed by remember { mutableStateOf(false) }

    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btn_scale"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isActive) Emerald500 else colors.borderLight,
        animationSpec = tween(250),
        label = "btn_border"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_loop")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.38f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(100.dp * pulseScale)
                        .clip(CircleShape)
                        .border(1.5.dp, Emerald500.copy(alpha = pulseAlpha), CircleShape)
                )

                Box(
                    modifier = Modifier
                        .size(100.dp * ((pulseScale - 1f) * 0.5f + 1f))
                        .clip(CircleShape)
                        .border(1.dp, Emerald500.copy(alpha = pulseAlpha * 0.7f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(buttonScale)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .border(2.5.dp, borderColor, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                                onToggle()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Emerald500.copy(alpha = 0.12f))
                    )
                }

                Box(
                    modifier = Modifier
                        .size(width = 4.dp, height = 26.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) Emerald500 else colors.textMuted)
                )
            }
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
private fun UnifiedTelemetryCard(
    queries: Long,
    blocked: Long,
    latency: Long,
    logs: List<String>
) {
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
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = buniString("queries"), value = queries.toString())
            StatItem(label = buniString("blocked"), value = blocked.toString())
            StatItem(label = buniString("latency"), value = "${latency}ms")
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )

        Column(modifier = Modifier.padding(14.dp)) {
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

            Spacer(modifier = Modifier.height(10.dp))

            if (displayedLogs.isEmpty()) {
                Text(
                    text = if (filterBlockedOnly) buniString("no_blocked_queries") else buniString("awaiting_packets"),
                    color = colors.textMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                LazyColumn(modifier = Modifier.height(260.dp)) {
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
