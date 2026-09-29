package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.EqualizerPresetEntity
import com.example.dsp.AbCompareState
import com.example.dsp.EQ_FREQ_LABELS
import com.example.ui.components.FrequencyCurveCanvas
import com.example.ui.components.GlowingVacuumTube
import com.example.ui.viewmodel.DroidampViewModel
import androidx.compose.material.icons.filled.Compare

@Composable
fun EqualizerDeckScreen(
    viewModel: DroidampViewModel,
    modifier: Modifier = Modifier
) {
    val dspParams by viewModel.dspParams.collectAsStateWithLifecycle()
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val selectedPresetName by viewModel.selectedPresetName.collectAsStateWithLifecycle()
    val aiState by viewModel.aiTuningState.collectAsStateWithLifecycle()
    val resolutionMetrics by viewModel.resolutionMetrics.collectAsStateWithLifecycle()
    val abCompareMode by viewModel.abCompareMode.collectAsStateWithLifecycle()
    val isAutoEqEnabled by viewModel.isAutoEqEnabled.collectAsStateWithLifecycle()
    val autoDetectedGenre by viewModel.autoDetectedGenre.collectAsStateWithLifecycle()
    val autoEqStatusMessage by viewModel.autoEqStatusMessage.collectAsStateWithLifecycle()
    val isAutoCycleActive by viewModel.isAutoCycleActive.collectAsStateWithLifecycle()
    val autoCycleCountdown by viewModel.autoCycleCountdown.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var showAiDialog by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }
    var newPresetGenre by remember { mutableStateOf("Custom") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Deck Header with AI button and Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "10-BAND PARAMETRIC GRAPHIC EQ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Active Profile: $selectedPresetName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // AI Neural Tune Button
                Button(
                    onClick = { showAiDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("open_ai_tune_button")
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI Auto-EQ",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "AI AUTO-EQ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Reset Button
                IconButton(
                    onClick = { viewModel.resetEq() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("reset_eq_button")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Reset EQ",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Chips Carousel
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items = presets) { preset: EqualizerPresetEntity ->
                val isSelected = preset.name == selectedPresetName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.applyPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (!preset.isFactoryDefault) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.deleteUserPreset(preset.id) }
                            )
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSaveDialog = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save Preset",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+ Save",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // A/B Comparison Bar inside Equalizer
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (abCompareMode == AbCompareState.MODE_B_UPSCALED) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp)
                )
                .testTag("eq_screen_ab_comparison_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Compare,
                        contentDescription = null,
                        tint = if (abCompareMode == AbCompareState.MODE_B_UPSCALED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "A/B TEST:",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val isA = abCompareMode == AbCompareState.MODE_A_RAW
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isA) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isA) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setAbCompareMode(AbCompareState.MODE_A_RAW) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("eq_ab_raw_button")
                    ) {
                        Text(
                            text = "[A] RAW 1:1",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isA) FontWeight.Bold else FontWeight.Normal,
                            color = if (isA) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val isB = abCompareMode == AbCompareState.MODE_B_UPSCALED
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isB) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isB) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.setAbCompareMode(AbCompareState.MODE_B_UPSCALED) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("eq_ab_upscaled_button")
                    ) {
                        Text(
                            text = "[B] 384 kHz 32-BIT",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isB) FontWeight.Bold else FontWeight.Normal,
                            color = if (isB) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Auto-Detect & Auto-Apply EQ Quick Bar
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isAutoEqEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp)
                )
                .testTag("eq_screen_auto_eq_bar")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = if (isAutoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-APPLY EQ:",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isAutoEqEnabled && autoDetectedGenre != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = autoDetectedGenre!!,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Text(
                            text = autoEqStatusMessage ?: "Adaptive acoustic tuning",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = isAutoEqEnabled,
                    onCheckedChange = { viewModel.toggleAutoEq(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.testTag("eq_auto_eq_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Frequency Curve Canvas
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (resolutionMetrics.isUpscalingActive)
                                "MAGNITUDE RESPONSE // HI-RES (${resolutionMetrics.outputSampleRate / 1000}kHz NYQUIST)"
                            else
                                "REAL-TIME MAGNITUDE RESPONSE (20Hz - 20kHz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (resolutionMetrics.isUpscalingActive) "${resolutionMetrics.formattedMultiplier} UPSCALE" else "±12 dB RANGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (resolutionMetrics.isUpscalingActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                FrequencyCurveCanvas(
                    eqGainsDb = dspParams.eqGainsDb,
                    curveColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 10-Band Sliders Deck
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "BIQUAD PEAKING FILTER GAINS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0 until 10) {
                        val freqLabel = EQ_FREQ_LABELS[i]
                        val gainDb = if (i < dspParams.eqGainsDb.size) dspParams.eqGainsDb[i] else 0f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (gainDb >= 0) "+%.1f".format(gainDb) else "%.1f".format(gainDb),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (gainDb != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1
                            )

                            // Vertical Slider emulation using custom bar column with tap/drag
                            Box(
                                modifier = Modifier
                                    .height(120.dp)
                                    .width(22.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .clickable {
                                        viewModel.updateEqBand(i, 0f)
                                    }
                            ) {
                                // Center 0dB line
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(Color.White.copy(alpha = 0.4f))
                                )

                                // Active fill bar
                                val fraction = (gainDb.coerceIn(-12f, 12f) / 12f)
                                val barHeightFraction = kotlin.math.abs(fraction) * 0.5f

                                if (fraction >= 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxWidth()
                                            .height((barHeightFraction * 120).dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .fillMaxWidth()
                                            .height((barHeightFraction * 120).dp)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f))
                                    )
                                }

                                // Interactive + / - nudge buttons overlay for precise mobile touch
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable { viewModel.updateEqBand(i, (gainDb + 1.0f).coerceAtMost(12f)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("+", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clickable { viewModel.updateEqBand(i, (gainDb - 1.0f).coerceAtLeast(-12f)) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("-", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // DSP Master Modules: Tube Warmth & Super-Resolution
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Analog Tube Warmth Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ANALOG TUBE WARMTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    GlowingVacuumTube(warmthLevel = dspParams.tubeWarmth)

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${(dspParams.tubeWarmth * 100).toInt()}% Saturation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = dspParams.tubeWarmth,
                        onValueChange = { viewModel.setTubeWarmth(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier.testTag("tube_warmth_slider")
                    )
                }
            }

            // Audio Super-Resolution Card
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TENSORRT SUPER-RES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                        Switch(
                            checked = dspParams.superResolutionEnabled,
                            onCheckedChange = { viewModel.toggleSuperResolution(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("super_res_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Icon(
                        Icons.Default.Speed,
                        contentDescription = "Neural Clarity",
                        tint = if (dspParams.superResolutionEnabled) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(36.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${(dspParams.superResClarity * 100).toInt()}% Clarity",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )

                    Slider(
                        value = dspParams.superResClarity,
                        onValueChange = { viewModel.setSuperResClarity(it) },
                        enabled = dspParams.superResolutionEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("super_res_slider")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Binaural Crossfeed Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = "Crossfeed",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Binaural Headphone Crossfeed",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Bauer filter simulation • reduces ear fatigue",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Switch(
                    checked = dspParams.crossfeedEnabled,
                    onCheckedChange = { viewModel.toggleCrossfeed(it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.testTag("crossfeed_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Custom Preset") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPresetName,
                        onValueChange = { newPresetName = it },
                        label = { Text("Preset Name") },
                        modifier = Modifier.fillMaxWidth().testTag("preset_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPresetGenre,
                        onValueChange = { newPresetGenre = it },
                        label = { Text("Genre Tag") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPresetName.isNotBlank()) {
                            viewModel.saveCurrentAsPreset(newPresetName.trim(), newPresetGenre.trim())
                            showSaveDialog = false
                            newPresetName = ""
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_preset")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Tuning Dialog
    if (showAiDialog) {
        GeminiTuneDialog(
            aiState = aiState,
            onAnalyze = { prompt, ctx -> viewModel.runGeminiSpectralTuning(prompt, ctx) },
            onDismiss = {
                showAiDialog = false
                viewModel.dismissAiDialog()
            },
            onSavePreset = { name, genre ->
                viewModel.saveCurrentAsPreset(name, genre)
            }
        )
    }
}
