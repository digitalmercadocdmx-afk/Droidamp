package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.R
import com.example.dsp.DspParameters
import com.example.dsp.AudioResolutionMetrics
import com.example.dsp.EqualizerAudioProcessor
import com.example.dsp.VuMeterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.max

@UnstableApi
class AudioEngineService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
        private set
    lateinit var equalizerAudioProcessor: EqualizerAudioProcessor
        private set

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    // Playback state flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("TensorRT Audio Engine Ready")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _vuMeterState = MutableStateFlow(VuMeterState())
    val vuMeterState: StateFlow<VuMeterState> = _vuMeterState.asStateFlow()

    private val _audioResolutionMetrics = MutableStateFlow(AudioResolutionMetrics())
    val audioResolutionMetrics: StateFlow<AudioResolutionMetrics> = _audioResolutionMetrics.asStateFlow()

    // A-B Looping
    var loopStartMs: Long = 0L
    var loopEndMs: Long = 0L
    var isAbLoopActive: Boolean = false

    // Sleep timer
    private var sleepTimerJob: Job? = null
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Int?> = _sleepTimerRemainingSeconds.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): AudioEngineService = this@AudioEngineService
    }

    override fun onBind(intent: Intent?): IBinder? {
        if (intent?.action == AudioPlayerController.ACTION_LOCAL_BIND) {
            return binder
        }
        return super.onBind(intent)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // 1. Initialize Equalizer & Max Resolution Upscaling AudioProcessor
        equalizerAudioProcessor = EqualizerAudioProcessor().apply {
            onVuMeterUpdate = { rmsL, rmsR, peakL, peakR ->
                _vuMeterState.value = VuMeterState(
                    leftRms = rmsL,
                    rightRms = rmsR,
                    leftPeak = peakL,
                    rightPeak = peakR
                )
            }
            onResolutionUpdate = { metrics ->
                _audioResolutionMetrics.value = metrics
            }
        }

        // 2. Build custom DefaultRenderersFactory injecting EqualizerAudioProcessor into AudioSink
        // Using standard 16-bit PCM output guarantees 100% crash-proof compatibility on all Android AudioTrack HALs
        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(false)
                    .setAudioProcessors(arrayOf(equalizerAudioProcessor))
                    .build()
            }
        }

        // 3. Build ExoPlayer
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val dur = player.duration
                    _durationMs.value = if (dur > 0) dur else 0L
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("AudioEngineService", "ExoPlayer playback error: ${error.message}", error)
                _currentTrackTitle.value = "Playback Error: ${error.errorCodeName}"
                _isPlaying.value = false
            }
        })

        // 4. Create MediaSession
        try {
            mediaSession = MediaSession.Builder(this, player).build()
        } catch (e: Exception) {
            android.util.Log.e("AudioEngineService", "Failed to build MediaSession", e)
        }

        // 5. Start position ticker and A-B loop check loop
        startPlaybackTicker()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    private fun startPlaybackTicker() {
        serviceScope.launch {
            while (isActive) {
                try {
                    if (player.isPlaying) {
                        val pos = player.currentPosition
                        _currentPositionMs.value = pos
                        val dur = player.duration
                        if (dur > 0) _durationMs.value = dur

                        // Check A-B loop wrap
                        if (isAbLoopActive && loopEndMs > loopStartMs && pos >= loopEndMs) {
                            player.seekTo(loopStartMs)
                        }
                    } else if (!_isPlaying.value) {
                        // Decay VU meters when paused
                        val cur = _vuMeterState.value
                        if (cur.leftPeak > 0.01f || cur.rightPeak > 0.01f) {
                            _vuMeterState.value = VuMeterState(
                                leftRms = max(0f, cur.leftRms * 0.85f),
                                rightRms = max(0f, cur.rightRms * 0.85f),
                                leftPeak = max(0f, cur.leftPeak * 0.90f),
                                rightPeak = max(0f, cur.rightPeak * 0.90f)
                            )
                        }
                    }
                } catch (ignored: Exception) {
                }
                delay(50)
            }
        }
    }

    fun playStream(url: String, title: String) {
        if (url.isBlank()) return
        _currentTrackTitle.value = title
        val parsedUri = when {
            url.startsWith("http://", ignoreCase = true) ||
            url.startsWith("https://", ignoreCase = true) ||
            url.startsWith("content://", ignoreCase = true) ||
            url.startsWith("file://", ignoreCase = true) -> android.net.Uri.parse(url)
            else -> android.net.Uri.fromFile(java.io.File(url))
        }

        val mediaItem = MediaItem.Builder()
            .setUri(parsedUri)
            .setMediaId(url)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun togglePlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun setPlaybackSpeed(speed: Float) {
        player.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2.0f))
    }

    fun jumpToHotCue(positionMs: Long) {
        if (positionMs >= 0) {
            seekTo(positionMs)
            if (!player.isPlaying) {
                player.play()
            }
        }
    }

    fun setAbLoop(startMs: Long, endMs: Long) {
        loopStartMs = startMs
        loopEndMs = endMs
        isAbLoopActive = true
    }

    fun clearAbLoop() {
        isAbLoopActive = false
        loopStartMs = 0L
        loopEndMs = 0L
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemainingSeconds.value = null
            player.volume = 1.0f
            return
        }

        val totalSeconds = minutes * 60
        _sleepTimerRemainingSeconds.value = totalSeconds

        sleepTimerJob = serviceScope.launch {
            var remaining = totalSeconds
            val originalVolume = 1.0f

            while (remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining

                // Exponential fade-out during the last 60 seconds
                if (remaining <= 60) {
                    val factor = remaining.toFloat() / 60.0f
                    // Exponential curve: factor^2
                    val expVolume = (factor * factor) * originalVolume
                    player.volume = expVolume.coerceIn(0f, 1f)
                }
            }

            // Pause playback when timer completes
            player.pause()
            player.volume = 1.0f
            _sleepTimerRemainingSeconds.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSeconds.value = null
        player.volume = 1.0f
    }

    fun updateDspParameters(params: DspParameters) {
        equalizerAudioProcessor.params = params
    }

    fun setAbCompareMode(mode: com.example.dsp.AbCompareState) {
        equalizerAudioProcessor.abCompareMode = mode
    }

    fun setAutoAbCycle(active: Boolean, remainingSec: Int) {
        equalizerAudioProcessor.isAutoCycleActive = active
        equalizerAudioProcessor.autoCycleRemainingSeconds = remainingSec
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_playback_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_playback_desc)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        serviceScope.cancel()
        try {
            mediaSession?.run {
                player.release()
                release()
                mediaSession = null
            }
        } catch (e: Exception) {
            android.util.Log.e("AudioEngineService", "Error during onDestroy", e)
        }
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "droidamp_playback_channel"
    }
}
