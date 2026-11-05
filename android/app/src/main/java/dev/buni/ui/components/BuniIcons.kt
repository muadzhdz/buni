package dev.buni.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BuniLogoIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val shacklePath = Path().apply {
            moveTo(w * 0.36f, h * 0.42f)
            lineTo(w * 0.36f, h * 0.28f)
            cubicTo(
                w * 0.36f, h * 0.12f,
                w * 0.64f, h * 0.12f,
                w * 0.64f, h * 0.28f
            )
            lineTo(w * 0.64f, h * 0.42f)
        }
        drawPath(
            path = shacklePath,
            color = tint,
            style = Stroke(
                width = w * 0.09f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        val bodyRect = RoundRect(
            rect = Rect(
                offset = Offset(w * 0.26f, h * 0.39f),
                size = Size(w * 0.48f, h * 0.45f)
            ),
            cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
        )
        val bodyPath = Path().apply { addRoundRect(bodyRect) }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Fill
        )
    }
}

@Composable
fun BugIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val bodyRect = RoundRect(
            rect = Rect(
                offset = Offset(w * 0.30f, h * 0.28f),
                size = Size(w * 0.40f, h * 0.50f)
            ),
            cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
        )
        val bodyPath = Path().apply { addRoundRect(bodyRect) }
        drawPath(path = bodyPath, color = tint, style = stroke)

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.28f),
            end = Offset(w * 0.50f, h * 0.78f),
            strokeWidth = w * 0.07f
        )

        val headPath = Path().apply {
            moveTo(w * 0.36f, h * 0.28f)
            cubicTo(w * 0.36f, h * 0.18f, w * 0.64f, h * 0.18f, w * 0.64f, h * 0.28f)
        }
        drawPath(path = headPath, color = tint, style = stroke)

        drawLine(color = tint, start = Offset(w * 0.40f, h * 0.18f), end = Offset(w * 0.30f, h * 0.10f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.60f, h * 0.18f), end = Offset(w * 0.70f, h * 0.10f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)

        drawLine(color = tint, start = Offset(w * 0.16f, h * 0.38f), end = Offset(w * 0.30f, h * 0.40f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.14f, h * 0.53f), end = Offset(w * 0.30f, h * 0.53f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.16f, h * 0.68f), end = Offset(w * 0.30f, h * 0.66f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)

        drawLine(color = tint, start = Offset(w * 0.84f, h * 0.38f), end = Offset(w * 0.70f, h * 0.40f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.86f, h * 0.53f), end = Offset(w * 0.70f, h * 0.53f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.84f, h * 0.68f), end = Offset(w * 0.70f, h * 0.66f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun MedalIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.28f,
            center = Offset(w * 0.50f, h * 0.38f),
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = w * 0.12f,
            center = Offset(w * 0.50f, h * 0.38f),
            style = stroke
        )

        val ribbonLeft = Path().apply {
            moveTo(w * 0.38f, h * 0.62f)
            lineTo(w * 0.34f, h * 0.88f)
            lineTo(w * 0.44f, h * 0.80f)
            lineTo(w * 0.48f, h * 0.66f)
        }
        drawPath(path = ribbonLeft, color = tint, style = stroke)

        val ribbonRight = Path().apply {
            moveTo(w * 0.62f, h * 0.62f)
            lineTo(w * 0.66f, h * 0.88f)
            lineTo(w * 0.56f, h * 0.80f)
            lineTo(w * 0.52f, h * 0.66f)
        }
        drawPath(path = ribbonRight, color = tint, style = stroke)
    }
}

@Composable
fun CodeIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val leftBracket = Path().apply {
            moveTo(w * 0.38f, h * 0.32f)
            lineTo(w * 0.20f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.68f)
        }
        drawPath(path = leftBracket, color = tint, style = stroke)

        val rightBracket = Path().apply {
            moveTo(w * 0.62f, h * 0.32f)
            lineTo(w * 0.80f, h * 0.50f)
            lineTo(w * 0.62f, h * 0.68f)
        }
        drawPath(path = rightBracket, color = tint, style = stroke)
    }
}

@Composable
fun DownloadIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.18f),
            end = Offset(w * 0.50f, h * 0.58f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.34f, h * 0.44f)
            lineTo(w * 0.50f, h * 0.60f)
            lineTo(w * 0.66f, h * 0.44f)
        }
        drawPath(path = arrowHead, color = tint, style = stroke)

        val trayPath = Path().apply {
            moveTo(w * 0.22f, h * 0.58f)
            lineTo(w * 0.22f, h * 0.78f)
            lineTo(w * 0.78f, h * 0.78f)
            lineTo(w * 0.78f, h * 0.58f)
        }
        drawPath(path = trayPath, color = tint, style = stroke)
    }
}

@Composable
fun ExternalLinkIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val boxPath = Path().apply {
            moveTo(w * 0.48f, h * 0.24f)
            lineTo(w * 0.24f, h * 0.24f)
            lineTo(w * 0.24f, h * 0.80f)
            lineTo(w * 0.80f, h * 0.80f)
            lineTo(w * 0.80f, h * 0.56f)
        }
        drawPath(path = boxPath, color = tint, style = stroke)

        drawLine(
            color = tint,
            start = Offset(w * 0.44f, h * 0.60f),
            end = Offset(w * 0.80f, h * 0.24f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.58f, h * 0.24f)
            lineTo(w * 0.80f, h * 0.24f)
            lineTo(w * 0.80f, h * 0.46f)
        }
        drawPath(path = arrowHead, color = tint, style = stroke)
    }
}

@Composable
fun ChevronRightIcon(
    modifier: Modifier = Modifier.size(20.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.10f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.38f, h * 0.26f)
            lineTo(w * 0.64f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.74f)
        }
        drawPath(path = path, color = tint, style = stroke)
    }
}

@Composable
fun ArrowBackIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawLine(
            color = tint,
            start = Offset(w * 0.78f, h * 0.50f),
            end = Offset(w * 0.22f, h * 0.50f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )

        val head = Path().apply {
            moveTo(w * 0.46f, h * 0.26f)
            lineTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.74f)
        }
        drawPath(path = head, color = tint, style = stroke)
    }
}

@Composable
fun MoonIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.85f, h * 0.52f)
            cubicTo(w * 0.85f, h * 0.74f, w * 0.67f, h * 0.85f, w * 0.48f, h * 0.85f)
            cubicTo(w * 0.28f, h * 0.85f, w * 0.15f, h * 0.70f, w * 0.15f, h * 0.48f)
            cubicTo(w * 0.15f, h * 0.28f, w * 0.28f, h * 0.15f, w * 0.48f, h * 0.15f)
            cubicTo(w * 0.40f, h * 0.25f, w * 0.38f, h * 0.38f, w * 0.45f, h * 0.52f)
            cubicTo(w * 0.52f, h * 0.65f, w * 0.68f, h * 0.68f, w * 0.85f, h * 0.52f)
            close()
        }
        drawPath(path = path, color = tint, style = stroke)
    }
}

@Composable
fun SunIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )

        val rayLen = w * 0.12f
        val innerR = w * 0.28f
        val outerR = innerR + rayLen

        for (i in 0 until 8) {
            val angle = i * (Math.PI / 4.0)
            val cx = w * 0.50f
            val cy = h * 0.50f
            val x1 = (cx + innerR * cos(angle)).toFloat()
            val y1 = (cy + innerR * sin(angle)).toFloat()
            val x2 = (cx + outerR * cos(angle)).toFloat()
            val y2 = (cy + outerR * sin(angle)).toFloat()

            drawLine(
                color = tint,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = w * 0.085f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun SystemModeIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )

        val toothPath = Path()
        val numTeeth = 6
        val rOuter = w * 0.36f
        val rInner = w * 0.26f

        for (i in 0 until numTeeth) {
            val a0 = i * (2.0 * Math.PI / numTeeth)
            val a1 = a0 + 0.25
            val a2 = a0 + 0.45
            val a3 = a0 + 0.70

            val cx = w * 0.50f
            val cy = h * 0.50f

            val p0x = (cx + rInner * cos(a0)).toFloat()
            val p0y = (cy + rInner * sin(a0)).toFloat()
            val p1x = (cx + rOuter * cos(a1)).toFloat()
            val p1y = (cy + rOuter * sin(a1)).toFloat()
            val p2x = (cx + rOuter * cos(a2)).toFloat()
            val p2y = (cy + rOuter * sin(a2)).toFloat()
            val p3x = (cx + rInner * cos(a3)).toFloat()
            val p3y = (cy + rInner * sin(a3)).toFloat()

            if (i == 0) toothPath.moveTo(p0x, p0y) else toothPath.lineTo(p0x, p0y)
            toothPath.lineTo(p1x, p1y)
            toothPath.lineTo(p2x, p2y)
            toothPath.lineTo(p3x, p3y)
        }
        toothPath.close()
        drawPath(path = toothPath, color = tint, style = stroke)
    }
}

@Composable
fun InfoIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.40f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )

        drawCircle(
            color = tint,
            radius = w * 0.045f,
            center = Offset(w * 0.50f, h * 0.34f),
            style = Fill
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.45f),
            end = Offset(w * 0.50f, h * 0.68f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun LanguageIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.38f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.12f, h * 0.50f),
            end = Offset(w * 0.88f, h * 0.50f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )

        val ellipsePath = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            cubicTo(w * 0.28f, h * 0.24f, w * 0.28f, h * 0.76f, w * 0.50f, h * 0.88f)
            cubicTo(w * 0.72f, h * 0.76f, w * 0.72f, h * 0.24f, w * 0.50f, h * 0.12f)
        }
        drawPath(path = ellipsePath, color = tint, style = stroke)
    }
}

@Composable
fun ShieldRuleIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.15f)
            lineTo(w * 0.80f, h * 0.28f)
            cubicTo(w * 0.80f, h * 0.60f, w * 0.62f, h * 0.78f, w * 0.50f, h * 0.85f)
            cubicTo(w * 0.38f, h * 0.78f, w * 0.20f, h * 0.60f, w * 0.20f, h * 0.28f)
            close()
        }
        drawPath(path = path, color = tint, style = stroke)

        drawLine(
            color = tint,
            start = Offset(w * 0.36f, h * 0.50f),
            end = Offset(w * 0.46f, h * 0.60f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.46f, h * 0.60f),
            end = Offset(w * 0.64f, h * 0.38f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun RadarRuleIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        drawCircle(
            color = tint,
            radius = w * 0.38f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = stroke
        )
        drawCircle(
            color = tint,
            radius = w * 0.05f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Fill
        )

        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.10f), end = Offset(w * 0.50f, h * 0.30f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.50f, h * 0.70f), end = Offset(w * 0.50f, h * 0.90f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.10f, h * 0.50f), end = Offset(w * 0.30f, h * 0.50f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
        drawLine(color = tint, start = Offset(w * 0.70f, h * 0.50f), end = Offset(w * 0.90f, h * 0.50f), strokeWidth = w * 0.07f, cap = StrokeCap.Round)
    }
}

@Composable
fun MalwareRuleIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val path = Path().apply {
            moveTo(w * 0.50f, h * 0.15f)
            lineTo(w * 0.86f, h * 0.80f)
            lineTo(w * 0.14f, h * 0.80f)
            close()
        }
        drawPath(path = path, color = tint, style = stroke)

        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.40f),
            end = Offset(w * 0.50f, h * 0.58f),
            strokeWidth = w * 0.085f,
            cap = StrokeCap.Round
        )

        drawCircle(
            color = tint,
            radius = w * 0.045f,
            center = Offset(w * 0.50f, h * 0.69f),
            style = Fill
        )
    }
}

@Composable
fun DnsServerIcon(
    modifier: Modifier = Modifier.size(24.dp),
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        val server1 = RoundRect(
            rect = Rect(Offset(w * 0.20f, h * 0.20f), Size(w * 0.60f, h * 0.24f)),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawPath(Path().apply { addRoundRect(server1) }, color = tint, style = stroke)
        drawCircle(color = tint, radius = w * 0.035f, center = Offset(w * 0.32f, h * 0.32f), style = Fill)

        val server2 = RoundRect(
            rect = Rect(Offset(w * 0.20f, h * 0.56f), Size(w * 0.60f, h * 0.24f)),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
        )
        drawPath(Path().apply { addRoundRect(server2) }, color = tint, style = stroke)
        drawCircle(color = tint, radius = w * 0.035f, center = Offset(w * 0.32f, h * 0.68f), style = Fill)
    }
}
