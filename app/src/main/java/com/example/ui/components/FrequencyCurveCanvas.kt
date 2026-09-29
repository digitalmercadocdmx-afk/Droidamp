package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.dsp.BiquadFilter
import com.example.dsp.EQ_FREQUENCIES
import kotlin.math.ln

@Composable
fun FrequencyCurveCanvas(
    eqGainsDb: List<Float>,
    modifier: Modifier = Modifier,
    curveColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
) {
    val gainsArray = eqGainsDb.toFloatArray()

    Canvas(modifier = modifier.fillMaxWidth().height(130.dp)) {
        val width = size.width
        val height = size.height

        val minFreq = 20.0
        val maxFreq = 20000.0
        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)

        // Helper to convert log freq to X
        fun freqToX(f: Double): Float {
            val logF = ln(f.coerceIn(minFreq, maxFreq))
            return ((logF - logMin) / (logMax - logMin) * width).toFloat()
        }

        // Helper to convert dB to Y (-14dB to +14dB)
        fun dbToY(db: Double): Float {
            val normalized = (db.coerceIn(-14.0, 14.0) + 14.0) / 28.0
            return (height - (normalized * height)).toFloat()
        }

        // Draw horizontal grid lines: +12, +6, 0, -6, -12 dB
        val gridDbs = listOf(12.0, 6.0, 0.0, -6.0, -12.0)
        gridDbs.forEach { db ->
            val y = dbToY(db)
            val isZero = db == 0.0
            drawLine(
                color = if (isZero) gridColor.copy(alpha = 0.45f) else gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = if (isZero) 1.5f else 0.8f
            )
        }

        // Draw vertical frequency lines for each of the 10 bands
        EQ_FREQUENCIES.forEach { f ->
            val x = freqToX(f.toDouble())
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 0.8f
            )
        }

        // Sample points along the log frequency spectrum
        val path = Path()
        val fillPath = Path()
        val steps = 120

        var firstPoint = true
        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val freq = kotlin.math.exp(logMin + t * (logMax - logMin))
            val magDb = BiquadFilter.calculateMagnitudeResponse(freq, gainsArray, EQ_FREQUENCIES)
            val x = (t * width).toFloat()
            val y = dbToY(magDb)

            if (firstPoint) {
                path.moveTo(x, y)
                fillPath.moveTo(x, dbToY(0.0))
                fillPath.lineTo(x, y)
                firstPoint = false
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, dbToY(0.0))
        fillPath.close()

        // Fill glow gradient under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    curveColor.copy(alpha = 0.28f),
                    curveColor.copy(alpha = 0.02f)
                )
            )
        )

        // Draw main frequency response curve line
        drawPath(
            path = path,
            color = curveColor,
            style = Stroke(width = 2.8f, cap = StrokeCap.Round)
        )

        // Draw points at center frequencies
        for (i in EQ_FREQUENCIES.indices) {
            val f = EQ_FREQUENCIES[i].toDouble()
            val gain = if (i < gainsArray.size) gainsArray[i].toDouble() else 0.0
            val x = freqToX(f)
            val y = dbToY(gain)

            drawCircle(
                color = curveColor,
                radius = 3.5f,
                center = Offset(x, y)
            )
        }
    }
}
