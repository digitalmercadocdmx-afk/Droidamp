package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun GlowingVacuumTube(
    warmthLevel: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "tube_flicker")
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Canvas(modifier = modifier.size(width = 46.dp, height = 72.dp)) {
        val width = size.width
        val height = size.height

        // Outer glass envelope outline
        val glassColor = Color(0xFF6B8B82).copy(alpha = 0.45f)
        drawRoundRect(
            color = glassColor,
            topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
            size = Size(width - 8.dp.toPx(), height - 12.dp.toPx()),
            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Tube base metal socket
        drawRoundRect(
            color = Color(0xFF25302B),
            topLeft = Offset(8.dp.toPx(), height - 12.dp.toPx()),
            size = Size(width - 16.dp.toPx(), 10.dp.toPx()),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // Cathode / Anode internal grid plate
        val plateColor = Color(0xFF1E2824)
        drawRoundRect(
            color = plateColor,
            topLeft = Offset(12.dp.toPx(), 16.dp.toPx()),
            size = Size(width - 24.dp.toPx(), height - 36.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Filament core glow
        val intensity = (warmthLevel.coerceIn(0.1f, 1.0f) * flicker).coerceIn(0f, 1.5f)
        val filamentColor = Color(0xFFFF7A00).copy(alpha = (0.2f + 0.75f * warmthLevel).coerceIn(0f, 1f))
        val hotCoreColor = Color(0xFFFFD54F).copy(alpha = (0.3f + 0.7f * warmthLevel).coerceIn(0f, 1f))

        // Ambient thermionic halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    filamentColor.copy(alpha = 0.45f * intensity),
                    hotCoreColor.copy(alpha = 0.20f * intensity),
                    Color.Transparent
                ),
                center = Offset(width / 2f, height / 2f - 4.dp.toPx()),
                radius = 24.dp.toPx()
            ),
            radius = 24.dp.toPx(),
            center = Offset(width / 2f, height / 2f - 4.dp.toPx())
        )

        // Center twin coiled filament wires
        val midX = width / 2f
        val wireTop = 22.dp.toPx()
        val wireBottom = height - 26.dp.toPx()

        drawLine(
            color = hotCoreColor,
            start = Offset(midX - 3.dp.toPx(), wireTop),
            end = Offset(midX - 3.dp.toPx(), wireBottom),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = hotCoreColor,
            start = Offset(midX + 3.dp.toPx(), wireTop),
            end = Offset(midX + 3.dp.toPx(), wireBottom),
            strokeWidth = 2.dp.toPx()
        )

        // Bridge filament arc
        drawLine(
            color = Color(0xFFFFEB3B),
            start = Offset(midX - 3.dp.toPx(), wireTop),
            end = Offset(midX + 3.dp.toPx(), wireTop),
            strokeWidth = 2.5.dp.toPx()
        )
    }
}
