package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.dsp.VuMeterState
import com.example.ui.theme.LedGreen
import com.example.ui.theme.LedRed
import com.example.ui.theme.LedYellow

@Composable
fun VuMeterCanvas(
    vuState: VuMeterState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF080D0A)
) {
    Canvas(modifier = modifier.fillMaxWidth().height(36.dp)) {
        val totalWidth = size.width
        val barHeight = 12.dp.toPx()
        val spacing = 4.dp.toPx()
        val segmentCount = 28
        val segmentGap = 2.dp.toPx()
        val segmentWidth = (totalWidth - (segmentCount - 1) * segmentGap) / segmentCount

        fun drawChannel(yOffset: Float, level: Float, peak: Float) {
            val activeSegments = (level.coerceIn(0f, 1f) * segmentCount).toInt()
            val peakSegment = (peak.coerceIn(0f, 1f) * (segmentCount - 1)).toInt()

            for (i in 0 until segmentCount) {
                val x = i * (segmentWidth + segmentGap)
                val fraction = i.toFloat() / segmentCount

                // Color based on standard VU meter calibration:
                // Green: -inf to -6dB (0.0 to 0.70)
                // Yellow: -6dB to -2dB (0.70 to 0.88)
                // Red: -2dB to 0dB+ (0.88 to 1.0)
                val baseColor = when {
                    fraction >= 0.88f -> LedRed
                    fraction >= 0.70f -> LedYellow
                    else -> LedGreen
                }

                val isActive = i <= activeSegments
                val isPeak = i == peakSegment && peakSegment > 0

                val color = when {
                    isPeak -> LedRed
                    isActive -> baseColor
                    else -> baseColor.copy(alpha = 0.15f) // Dim inactive LED
                }

                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, yOffset),
                    size = Size(segmentWidth, barHeight),
                    cornerRadius = CornerRadius(1.5f, 1.5f)
                )
            }
        }

        // Draw Left channel (top)
        drawChannel(yOffset = 2f, level = vuState.leftRms * 1.8f, peak = vuState.leftPeak)

        // Draw Right channel (bottom)
        drawChannel(yOffset = barHeight + spacing, level = vuState.rightRms * 1.8f, peak = vuState.rightPeak)
    }
}
