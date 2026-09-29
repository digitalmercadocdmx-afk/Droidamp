package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import com.example.dsp.AbCompareState
import com.example.dsp.AudioResolutionMetrics
import com.example.dsp.ResolutionUpscaleMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CyberdeckSkin
import com.example.ui.components.VuMeterCanvas
import com.example.ui.components.WaveformScrubberCanvas
import com.example.ui.viewmodel.DroidampViewModel

@Composable
fun DroidampPlayerDeck(
    viewModel: DroidampViewModel,
    modifier: Modifier = Modifier
) {
    val isPlaying by viewModel.playerController.isPlaying.collectAsStateWithLifecycle()
    val positionMs by viewModel.playerController.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.playerController.durationMs.collectAsStateWithLifecycle()
    val trackTitle by viewModel.playerController.trackTitle.collectAsStateWithLifecycle()
    val vuState by viewModel.playerController.vuMeterState.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.playerController.sleepTimerSeconds.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val isLoopActive by viewModel.isLoopActive.collectAsStateWithLifecycle()
    val uploadedSongs by viewModel.uploadedSongs.collectAsStateWithLifecycle()
    val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
    val uploadMessage by viewModel.uploadMessage.collectAsStateWithLifecycle()
    val resolutionMetrics by viewModel.resolutionMetrics.collectAsStateWithLifecycle()
    val dspParams by viewModel.dspParams.collectAsStateWithLifecycle()
    val abCompareMode by viewModel.abCompareMode.collectAsStateWithLifecycle()
    val isAutoCycleActive by viewModel.isAutoCycleActive.collectAsStateWithLifecycle()
    val autoCycleCountdown by viewModel.autoCycleCountdown.collectAsStateWithLifecycle()
    val isAutoEqEnabled by viewModel.isAutoEqEnabled.collectAsStateWithLifecycle()
    val autoDetectedGenre by viewModel.autoDetectedGenre.collectAsStateWithLifecycle()
    val autoEqStatusMessage by viewModel.autoEqStatusMessage.collectAsStateWithLifecycle()

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadSongFromUri(it) }
    }

    var showSkinDialog by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var pitchSliderValue by remember { mutableFloatStateOf(userSettings.playbackPitch) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Deck Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) MaterialTheme.colorScheme.primary else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DROIDAMP // TENSORRT AUDIO ENGINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "DSP MASTER DECK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Upload Song Button in Header
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .clickable { audioPickerLauncher.launch("audio/*") }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("header_upload_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Upload Audio",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UPLOAD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sleep Timer Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { showSleepDialog = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("sleep_timer_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AvTimer,
                            contentDescription = "Sleep Timer",
                            tint = if (sleepRemaining != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (sleepRemaining != null) "${sleepRemaining!! / 60}m" else "TIMER",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sleepRemaining != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Theme Skin Button
                IconButton(
                    onClick = { showSkinDialog = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("skin_picker_button")
                ) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = "Theme Skin",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Upload Status Banner
        if (isUploading || uploadMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.clearUploadMessage() }
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = uploadMessage ?: "Processing audio...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (!isUploading) {
                        Text(
                            text = "DISMISS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Ultra-HD / Maximum Resolution Upscale Telemetry Badge
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (resolutionMetrics.isUpscalingActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp)
                )
                .testTag("max_resolution_telemetry_badge")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (resolutionMetrics.isUpscalingActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (resolutionMetrics.isUpscalingActive) "HI-RES UPSCALE" else "NATIVE DIRECT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (resolutionMetrics.isUpscalingActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        val outFormatted = if (resolutionMetrics.outputSampleRate >= 1000000) {
                            "%.3f MHz".format(resolutionMetrics.outputSampleRate / 1000000.0)
                        } else {
                            "${resolutionMetrics.outputSampleRate / 1000.0} kHz"
                        }
                        Text(
                            text = "$outFormatted • ${resolutionMetrics.outputBitDepth}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (resolutionMetrics.isUpscalingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "SOURCE: ${resolutionMetrics.inputSampleRate / 1000.0} kHz ➔ $outFormatted (${resolutionMetrics.formattedMultiplier})",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Ultrasonic Harmonic Activity Bar
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ULTRASONIC AIR",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        val activeBars = (resolutionMetrics.ultrasonicEnergy * 6).toInt().coerceIn(1, 6)
                        for (bar in 1..6) {
                            Box(
                                modifier = Modifier
                                    .size(width = 4.dp, height = 10.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (bar <= activeBars) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Audiophile A/B Instant Comparison Switcher
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (abCompareMode == AbCompareState.MODE_B_UPSCALED) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                    RoundedCornerShape(12.dp)
                )
                .testTag("ab_comparison_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Compare,
                            contentDescription = "A/B Compare",
                            tint = if (abCompareMode == AbCompareState.MODE_B_UPSCALED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "REAL-TIME A/B AUDIOPHILE COMPARISON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Auto-Cycle Toggle Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isAutoCycleActive) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.dp,
                                if (isAutoCycleActive) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { viewModel.toggleAutoAbCycle() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("auto_ab_cycle_button")
                    ) {
                        Text(
                            text = if (isAutoCycleActive) "CYCLE [${autoCycleCountdown}s]" else "AUTO CYCLE (5s)",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAutoCycleActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Segmented A/B Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // MODE A: RAW DIRECT
                    val isA = abCompareMode == AbCompareState.MODE_A_RAW
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isA) MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.5.dp,
                                if (isA) MaterialTheme.colorScheme.secondary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setAbCompareMode(AbCompareState.MODE_A_RAW) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("ab_mode_a_raw_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isA) MaterialTheme.colorScheme.secondary else Color.Gray.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MODE [A] RAW DIRECT",
                                    fontSize = 10.sp,
                                    fontWeight = if (isA) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isA) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "16-Bit / Native 1:1 Stream",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // MODE B: 1.536 MHz 32-BIT FLOAT UPSCALED
                    val isB = abCompareMode == AbCompareState.MODE_B_UPSCALED
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isB) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.5.dp,
                                if (isB) MaterialTheme.colorScheme.primary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setAbCompareMode(AbCompareState.MODE_B_UPSCALED) }
                            .padding(vertical = 10.dp, horizontal = 6.dp)
                            .testTag("ab_mode_b_upscaled_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isB) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "MODE [B] 384 kHz MASTER",
                                    fontSize = 10.sp,
                                    fontWeight = if (isB) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isB) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "384 kHz • 32-Bit Float Studio",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isB) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Auto-Detect & Auto-Apply EQ Status Card
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (isAutoEqEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(10.dp)
                )
                .testTag("auto_detect_eq_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Auto-EQ",
                        tint = if (isAutoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AUTO-DETECT & APPLY EQ",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAutoEqEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isAutoEqEnabled && autoDetectedGenre != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = autoDetectedGenre!!,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        Text(
                            text = autoEqStatusMessage ?: "Auto-adapts mastering curve to track content",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isAutoEqEnabled,
                    onCheckedChange = { viewModel.toggleAutoEq(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.testTag("auto_detect_eq_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cyberdeck Cassette Reel Display
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cassette Deck Graphic with spinning spools
                CassetteReelVisualizer(isPlaying = isPlaying, pitch = pitchSliderValue)

                Spacer(modifier = Modifier.height(10.dp))

                // Track Title Display
                Text(
                    text = trackTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "PCM 16-BIT 44.1kHz • TENSORRT DSP ACTIVE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dual Stereo VU Meters
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "STEREO VU METERS (L / R)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "-24dB  -12dB  -6dB  0dB PEAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                VuMeterCanvas(vuState = vuState)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Waveform Scrubber & A-B Looping
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (durationMs > 0) formatTime(durationMs) else "LIVE STREAM",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                WaveformScrubberCanvas(
                    currentPositionMs = positionMs,
                    durationMs = durationMs,
                    loopStartMs = viewModel.loopStartMs,
                    loopEndMs = viewModel.loopEndMs,
                    isLoopActive = isLoopActive,
                    onSeek = { viewModel.playerController.seekTo(it) },
                    activeColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // A-B Loop Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.setLoopPinA() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f).testTag("loop_pin_a_button")
                    ) {
                        Text(
                            text = if (viewModel.loopStartMs > 0) "A: ${formatTime(viewModel.loopStartMs)}" else "SET A",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Button(
                        onClick = { viewModel.setLoopPinB() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.weight(1f).testTag("loop_pin_b_button")
                    ) {
                        Text(
                            text = if (viewModel.loopEndMs > 0) "B: ${formatTime(viewModel.loopEndMs)}" else "SET B",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Button(
                        onClick = { viewModel.toggleAbLoop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLoopActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.weight(1.2f).testTag("toggle_ab_loop_button")
                    ) {
                        Icon(
                            Icons.Default.Loop,
                            contentDescription = "Loop",
                            tint = if (isLoopActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isLoopActive) "LOOP ON" else "A-B LOOP",
                            fontSize = 11.sp,
                            color = if (isLoopActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Transport Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rewind 10s
            IconButton(
                onClick = { viewModel.playerController.seekTo((positionMs - 10000L).coerceAtLeast(0L)) },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("rewind_button")
            ) {
                Icon(Icons.Default.FastRewind, contentDescription = "Rewind 10s", tint = MaterialTheme.colorScheme.onSurface)
            }

            // Play / Pause Master Button
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Forward 10s
            IconButton(
                onClick = { viewModel.playerController.seekTo(positionMs + 10000L) },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("forward_button")
            ) {
                Icon(Icons.Default.FastForward, contentDescription = "Forward 10s", tint = MaterialTheme.colorScheme.onSurface)
            }

            // Stop
            IconButton(
                onClick = {
                    if (isPlaying) viewModel.togglePlayPause()
                    viewModel.playerController.seekTo(0L)
                },
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("stop_button")
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 DJ Hot Cues Deck
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "DJ HOT CUES (TAP: JUMP • HOLD: STORE)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val cues = listOf(
                        Triple(1, viewModel.hotCue1Ms, "CUE 1"),
                        Triple(2, viewModel.hotCue2Ms, "CUE 2"),
                        Triple(3, viewModel.hotCue3Ms, "CUE 3"),
                        Triple(4, viewModel.hotCue4Ms, "CUE 4")
                    )

                    cues.forEach { (index, timeMs, label) ->
                        val isSet = timeMs > 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSet) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSet) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    if (isSet) {
                                        viewModel.triggerHotCue(index)
                                    } else {
                                        viewModel.storeHotCue(index)
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (isSet) formatTime(timeMs) else "--:--",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Playback Pitch / Speed Slider (0.8x to 1.2x)
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TURNTABLE PITCH / SPEED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "%.2fx".format(pitchSliderValue),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            pitchSliderValue = 1.0f
                            viewModel.setPlaybackSpeed(1.0f)
                        }
                    )
                }

                Slider(
                    value = pitchSliderValue,
                    onValueChange = {
                        pitchSliderValue = it
                        viewModel.setPlaybackSpeed(it)
                    },
                    valueRange = 0.8f..1.2f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("pitch_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Maximum Resolution Audio Upscaler Control Module
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    if (dspParams.isUpscalingEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                )
                .testTag("max_res_upscaler_card")
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header with title and Master Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.HighQuality,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MAX RESOLUTION UPSCALER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = "Hermite Spline Resampler & Ultrasonic Synthesis",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Switch(
                        checked = dspParams.isUpscalingEnabled,
                        onCheckedChange = { viewModel.toggleUpscaling(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.testTag("upscale_master_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Target Resolution Mode Chips
                Text(
                    text = "TARGET RESOLUTION CEILING // 32-BIT FLOAT MASTER",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Group 1: Ultra High-Res (1.536 MHz, 768 kHz, 384 kHz)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val ultraModes = listOf(
                        ResolutionUpscaleMode.ULTRA_1536K,
                        ResolutionUpscaleMode.DXD_768K,
                        ResolutionUpscaleMode.STUDIO_384K
                    )

                    ultraModes.forEach { mode ->
                        val isSelected = dspParams.upscaleMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setUpscaleMode(mode) }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mode.badgeLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = mode.bitDepthLabel,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Group 2: High-Res & Direct (192 kHz, 96 kHz, 1:1 Direct)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val standardModes = listOf(
                        ResolutionUpscaleMode.AUTO_MAX,
                        ResolutionUpscaleMode.STUDIO_96K,
                        ResolutionUpscaleMode.DIRECT
                    )

                    standardModes.forEach { mode ->
                        val isSelected = dspParams.upscaleMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.setUpscaleMode(mode) }
                                .padding(vertical = 8.dp, horizontal = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = mode.badgeLabel,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = mode.bitDepthLabel,
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider 1: Ultrasonic Air & Harmonic Synthesis
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ULTRASONIC AIR & HARMONICS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Synthesize lost high overtones (>16kHz)",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "${(dspParams.ultrasonicAirLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = dspParams.ultrasonicAirLevel,
                    onValueChange = { viewModel.setUltrasonicAir(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("ultrasonic_air_slider")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Slider 2: Transient Sharpening & Micro-Dynamics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TRANSIENT ATTACK & MICRO-DYNAMICS",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Restores compressed punch & acoustic transients",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Text(
                        text = "${(dspParams.transientSharpening * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Slider(
                    value = dspParams.transientSharpening,
                    onValueChange = { viewModel.setTransientSharpening(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("transient_sharp_slider")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Uploaded Tracks Library Section
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOCAL AUDIO & UPLOADED TRACKS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${uploadedSongs.size} Tracks Loaded",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { audioPickerLauncher.launch("audio/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("upload_file_button")
                        ) {
                            Icon(
                                Icons.Default.UploadFile,
                                contentDescription = "Choose File",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+ UPLOAD",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { viewModel.generateDemoTrack() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("demo_track_button")
                        ) {
                            Text(
                                text = "DEMO WAV",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (uploadedSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { audioPickerLauncher.launch("audio/*") }
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "No local songs uploaded yet",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap to upload MP3, WAV, FLAC, OGG, or AAC from your device",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uploadedSongs.forEach { song ->
                            val isCurrentSong = trackTitle.contains(song.title, ignoreCase = true)
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrentSong) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        1.dp,
                                        if (isCurrentSong) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.playUploadedSong(song) }
                                    .testTag("uploaded_song_${song.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrentSong && isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                if (isCurrentSong && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = if (isCurrentSong && isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = song.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentSong) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${song.artist} • ${song.fileSizeFormatted}" + if (song.durationMs > 0) " • ${formatTime(song.durationMs)}" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { viewModel.deleteUploadedSong(song) },
                                            modifier = Modifier.size(28.dp).testTag("delete_song_${song.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Skin Picker Dialog
    if (showSkinDialog) {
        AlertDialog(
            onDismissRequest = { showSkinDialog = false },
            title = { Text("Select Cyberdeck Skin") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CyberdeckSkin.entries.forEach { skin ->
                        val isSelected = userSettings.skin == skin
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    viewModel.changeSkin(skin)
                                    showSkinDialog = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = skin.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSkinDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Sleep Timer (Exponential Fade)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val timerOptions = listOf(15, 30, 45, 60)
                    timerOptions.forEach { mins ->
                        Button(
                            onClick = {
                                viewModel.setSleepTimer(mins)
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("$mins Minutes Fade-out", color = MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    if (sleepRemaining != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepDialog = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel Timer", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSleepDialog = false }) {
                    Text("Dismiss")
                }
            }
        )
    }
}

@Composable
fun CassetteReelVisualizer(isPlaying: Boolean, pitch: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "tape_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = (2400 / pitch.coerceIn(0.5f, 2f)).toInt(), easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRot = if (isPlaying) rotation else 0f

    Row(
        modifier = Modifier.fillMaxWidth().height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Spool
        SpoolWheel(rotationAngle = currentRot)

        // Center Cassette Bridge with tape ribbon
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF091410))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TENSORRT 10-EQ",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                // Tape ribbon bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(3.dp)
                        .background(Color(0xFF8B4513))
                )
            }
        }

        // Right Spool
        SpoolWheel(rotationAngle = currentRot)
    }
}

@Composable
fun SpoolWheel(rotationAngle: Float) {
    Canvas(
        modifier = Modifier
            .size(56.dp)
            .rotate(rotationAngle)
    ) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Outer spool ring
        drawCircle(
            color = Color(0xFF1E2F26),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color(0xFF00FF9D),
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )

        // Inner hub
        drawCircle(
            color = Color(0xFF07120D),
            radius = radius * 0.45f,
            center = center
        )

        // 3 Spoke teeth
        for (i in 0..2) {
            val angleRad = Math.toRadians((i * 120.0)).toFloat()
            val spokeX = center.x + kotlin.math.cos(angleRad) * radius * 0.75f
            val spokeY = center.y + kotlin.math.sin(angleRad) * radius * 0.75f
            drawLine(
                color = Color(0xFF00E5FF),
                start = center,
                end = Offset(spokeX, spokeY),
                strokeWidth = 2.5.dp.toPx()
            )
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return "%02d:%02d".format(minutes, seconds)
}
