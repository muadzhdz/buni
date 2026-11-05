package dev.buni.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.buni.core.buniString
import dev.buni.theme.buniColors

enum class NavTab {
    SHIELD,
    APPS,
    SETTINGS
}

@Composable
fun ThumbDock(
    currentPage: Int,
    pageOffsetFraction: Float,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = NavTab.values()
    val colors = buniColors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface)
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.border)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.ordinal == currentPage
                val title = when (tab) {
                    NavTab.SHIELD -> buniString("nav_shield")
                    NavTab.APPS -> buniString("nav_apps")
                    NavTab.SETTINGS -> buniString("nav_settings")
                }

                FluxNavItem(
                    tab = tab,
                    title = title,
                    isSelected = isSelected,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun FluxNavItem(
    tab: NavTab,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = buniColors
    val interactionSource = remember { MutableInteractionSource() }

    val pillColor by animateColorAsState(
        targetValue = if (isSelected) colors.surfaceVariant else Color.Transparent,
        animationSpec = tween(durationMillis = 200)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) colors.textPrimary else colors.textMuted,
        animationSpec = tween(durationMillis = 200)
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 54.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(pillColor),
            contentAlignment = Alignment.Center
        ) {
            NavIconCanvas(tab = tab, color = contentColor)
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = title,
            color = contentColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun NavIconCanvas(tab: NavTab, color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height

        when (tab) {
            NavTab.SHIELD -> {
                val path = Path().apply {
                    moveTo(w * 0.5f, h * 0.08f)
                    lineTo(w * 0.88f, h * 0.26f)
                    lineTo(w * 0.88f, h * 0.56f)
                    cubicTo(w * 0.88f, h * 0.78f, w * 0.68f, h * 0.92f, w * 0.5f, h * 0.98f)
                    cubicTo(w * 0.32f, h * 0.92f, w * 0.12f, h * 0.78f, w * 0.12f, h * 0.56f)
                    lineTo(w * 0.12f, h * 0.26f)
                    close()
                }
                drawPath(path, color = color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(w * 0.5f, h * 0.32f),
                    end = Offset(w * 0.5f, h * 0.68f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            NavTab.APPS -> {
                val pad = 1.dp.toPx()
                val itemSize = (w - pad * 3) / 2
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, pad),
                    size = Size(itemSize, itemSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad * 2 + itemSize, pad),
                    size = Size(itemSize, itemSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad, pad * 2 + itemSize),
                    size = Size(itemSize, itemSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                drawRoundRect(
                    color = color,
                    topLeft = Offset(pad * 2 + itemSize, pad * 2 + itemSize),
                    size = Size(itemSize, itemSize),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()),
                    style = Stroke(width = 1.8.dp.toPx())
                )
            }
            NavTab.SETTINGS -> {
                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, h * 0.28f),
                    end = Offset(w * 0.9f, h * 0.28f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = 2.4.dp.toPx(),
                    center = Offset(w * 0.35f, h * 0.28f)
                )

                drawLine(
                    color = color,
                    start = Offset(w * 0.1f, h * 0.72f),
                    end = Offset(w * 0.9f, h * 0.72f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(
                    color = color,
                    radius = 2.4.dp.toPx(),
                    center = Offset(w * 0.65f, h * 0.72f)
                )
            }
        }
    }
}
