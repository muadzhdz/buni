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
import dev.buni.theme.Black
import dev.buni.theme.Emerald500
import dev.buni.theme.White
import dev.buni.theme.Zinc400
import dev.buni.theme.Zinc500
import dev.buni.theme.Zinc600
import dev.buni.theme.Zinc700
import dev.buni.theme.Zinc800
import dev.buni.theme.Zinc900
import dev.buni.theme.Zinc950

enum class EngineMode {
    SHIELD,
    TUNNEL
}

@Composable
fun DashboardScreen(
    isActive: Boolean,
    stats: Triple<Long, Long, Long>,
    logs: List<String>,
    onTogglePower: () -> Unit
) {
    var selectedMode by remember { mutableStateOf(EngineMode.SHIELD) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        HeaderSection(isActive = isActive, latencyMs = stats.third, mode = selectedMode)

        Spacer(modifier = Modifier.height(24.dp))

        SlidingModeSelector(
            currentMode = selectedMode,
            onModeSelect = { selectedMode = it }
        )

        Spacer(modifier = Modifier.height(32.dp))

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

        Spacer(modifier = Modifier.height(32.dp))

        TelemetryStrip(
            queries = stats.first,
            blocked = stats.second,
            latency = stats.third
        )

        Spacer(modifier = Modifier.height(24.dp))

        InspectionConsole(logs = logs)
    }
}

@Composable
private fun HeaderSection(isActive: Boolean, latencyMs: Long, mode: EngineMode) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "buni",
                color = White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = if (mode == EngineMode.SHIELD) "the hidden network shield" else "wireguard privacy tunnel",
                color = Zinc500,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Zinc950)
                .border(1.dp, Zinc800, RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Emerald500 else Zinc600)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "${latencyMs}ms" else "STANDBY",
                color = if (isActive) White else Zinc500,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SlidingModeSelector(
    currentMode: EngineMode,
    onModeSelect: (EngineMode) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Zinc950)
            .border(1.dp, Zinc800, RoundedCornerShape(10.dp))
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
                .background(Zinc800)
                .border(1.dp, Zinc700, RoundedCornerShape(8.dp))
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            ModeTab(
                title = "SHIELD",
                subtitle = "Local Zero-Latency",
                selected = currentMode == EngineMode.SHIELD,
                onClick = { onModeSelect(EngineMode.SHIELD) },
                modifier = Modifier.weight(1f)
            )
            ModeTab(
                title = "TUNNEL",
                subtitle = "WireGuard Node",
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
    Column(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            color = if (selected) White else Zinc500,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            color = if (selected) Zinc400 else Zinc600,
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
    val borderColor by animateColorAsState(
        targetValue = if (isActive) Emerald500 else Zinc700,
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
                .background(Zinc950)
                .border(2.dp, borderColor, CircleShape)
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 24.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive) Emerald500 else Zinc600)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (isActive) {
                if (mode == EngineMode.SHIELD) "SHIELD ENGAGED" else "TUNNEL ENGAGED"
            } else {
                if (mode == EngineMode.SHIELD) "TAP TO SHIELD" else "TAP TO TUNNEL"
            },
            color = if (isActive) White else Zinc500,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (mode == EngineMode.SHIELD) {
                "Direct WAN · Local DNS Sinkhole · Cloudflare"
            } else {
                "Encrypted Uplink · Remote WireGuard Node"
            },
            color = Zinc600,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Zinc950)
            .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatItem(label = "QUERIES", value = queries.toString())
        StatItem(label = "BLOCKED", value = blocked.toString())
        StatItem(label = "LATENCY", value = "${latency}ms")
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Zinc500,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = White,
            fontSize = 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InspectionConsole(logs: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Zinc950)
            .border(1.dp, Zinc800, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LIVE INSPECTION",
                color = Zinc500,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = "${logs.size} EVENTS",
                color = Zinc600,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (logs.isEmpty()) {
            Text(
                text = "> awaiting packet stream...",
                color = Zinc600,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.height(140.dp)) {
                items(logs) { logLine ->
                    Text(
                        text = "> $logLine",
                        color = Zinc400,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
