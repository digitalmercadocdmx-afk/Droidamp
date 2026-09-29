package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sin

@Composable
fun WaveformScrubberCanvas(
    currentPositionMs: Long,
    durationMs: Long,
    loopStartMs: Long,
    loopEndMs: Long,
    isLoopActive: Boolean,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
    loopRegionColor: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
) {
    // Generate pseudo-waveform bars based on seed hash
    val barCount = 75
    val heights = remember {
        FloatArray(barCount) { i ->
            val angle = i * 0.35f
            val base = (sin(angle) * 0.4f + sin(angle * 2.3f) * 0.3f + sin(angle * 5.1f) * 0.2f)
            (0.18f + kotlin.math.abs(base) * 0.78f).coerceIn(0.15f, 1.0f)
        }
    }

    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val loopStartFraction = if (durationMs > 0 && loopStartMs > 0) (loopStartMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val loopEndFraction = if (durationMs > 0 && loopEndMs > 0) (loopEndMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (durationMs > 0) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures { change, _ ->
                    if (durationMs > 0) {
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((fraction * durationMs).toLong())
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val barGap = 2.dp.toPx()
        val barWidth = (width - (barCount - 1) * barGap) / barCount

        // 1. Draw A-B Loop shaded region if active
        if (isLoopActive && loopEndFraction > loopStartFraction) {
            val startX = loopStartFraction * width
            val endX = loopEndFraction * width
            drawRect(
                color = loopRegionColor,
                topLeft = Offset(startX, 0f),
                size = Size(endX - startX, height)
            )
        }

        // 2. Draw Waveform bars
        val playheadX = progress * width

        for (i in 0 until barCount) {
            val x = i * (barWidth + barGap)
            val barH = heights[i] * height * 0.85f
            val top = centerY - barH / 2f
            val isPlayed = (x + barWidth) <= playheadX

            drawRoundRect(
                color = if (isPlayed) activeColor else inactiveColor,
                topLeft = Offset(x, top),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(1.5f, 1.5f)
            )
        }

        // 3. Draw Loop Pin A marker
        if (isLoopActive && loopStartMs > 0) {
            val pinAX = loopStartFraction * width
            drawLine(
                color = Color(0xFF00FF9D),
                start = Offset(pinAX, 0f),
                end = Offset(pinAX, height),
                strokeWidth = 2.5f
            )
            // Flag A
            val flagPath = Path().apply {
                moveTo(pinAX, 0f)
                lineTo(pinAX + 14f, 7f)
                lineTo(pinAX, 14f)
                close()
            }
            drawPath(flagPath, color = Color(0xFF00FF9D))
        }

        // 4. Draw Loop Pin B marker
        if (isLoopActive && loopEndMs > 0) {
            val pinBX = loopEndFraction * width
            drawLine(
                color = Color(0xFFFF9100),
                start = Offset(pinBX, 0f),
                end = Offset(pinBX, height),
                strokeWidth = 2.5f
            )
            // Flag B
            val flagPath = Path().apply {
                moveTo(pinBX, 0f)
                lineTo(pinBX - 14f, 7f)
                lineTo(pinBX, 14f)
                close()
            }
            drawPath(flagPath, color = Color(0xFFFF9100))
        }

        // 5. Draw Playhead cursor
        drawLine(
            color = Color.White,
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, height),
            strokeWidth = 2.5f
        )
        drawCircle(
            color = activeColor,
            radius = 5.dp.toPx(),
            center = Offset(playheadX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(playheadX, centerY)
        )
    }
}
