package com.example.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import com.example.dsp.AudioResolutionMetrics
import com.example.dsp.DspParameters
import com.example.dsp.VuMeterState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@UnstableApi
class AudioPlayerController(private val context: Context) {

    private var service: AudioEngineService? = null
    private var isBound = false
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    val isPlaying = MutableStateFlow(false)
    val currentPositionMs = MutableStateFlow(0L)
    val durationMs = MutableStateFlow(0L)
    val trackTitle = MutableStateFlow("TensorRT Audio Engine Ready")
    val vuMeterState = MutableStateFlow(VuMeterState())
    val sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val audioResolutionMetrics = MutableStateFlow(AudioResolutionMetrics())

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as? AudioEngineService.LocalBinder
            service = localBinder?.getService()
            isBound = true
            _isConnected.value = true

            service?.let { s ->
                scope.launch {
                    s.isPlaying.collect { isPlaying.value = it }
                }
                scope.launch {
                    s.currentPositionMs.collect { currentPositionMs.value = it }
                }
                scope.launch {
                    s.durationMs.collect { durationMs.value = it }
                }
                scope.launch {
                    s.currentTrackTitle.collect { trackTitle.value = it }
                }
                scope.launch {
                    s.vuMeterState.collect { vuMeterState.value = it }
                }
                scope.launch {
                    s.sleepTimerRemainingSeconds.collect { sleepTimerSeconds.value = it }
                }
                scope.launch {
                    s.audioResolutionMetrics.collect { audioResolutionMetrics.value = it }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            isBound = false
            _isConnected.value = false
        }
    }

    fun bind() {
        try {
            val intent = Intent(context, AudioEngineService::class.java).apply {
                action = ACTION_LOCAL_BIND
            }
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        } catch (e: Exception) {
            android.util.Log.e("AudioPlayerController", "Failed to bind AudioEngineService", e)
        }
    }

    fun unbind() {
        if (isBound) {
            try {
                context.unbindService(connection)
            } catch (e: Exception) {
                android.util.Log.e("AudioPlayerController", "Failed to unbind AudioEngineService", e)
            }
            isBound = false
            _isConnected.value = false
        }
    }

    companion object {
        const val ACTION_LOCAL_BIND = "com.example.service.ACTION_LOCAL_BIND"
    }

    fun playStream(url: String, title: String) {
        service?.playStream(url, title)
    }

    fun togglePlayPause() {
        service?.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        service?.seekTo(positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        service?.setPlaybackSpeed(speed)
    }

    fun jumpToHotCue(positionMs: Long) {
        service?.jumpToHotCue(positionMs)
    }

    fun setAbLoop(startMs: Long, endMs: Long) {
        service?.setAbLoop(startMs, endMs)
    }

    fun clearAbLoop() {
        service?.clearAbLoop()
    }

    fun startSleepTimer(minutes: Int) {
        service?.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        service?.cancelSleepTimer()
    }

    fun updateDsp(params: DspParameters) {
        service?.updateDspParameters(params)
    }

    fun setAbCompareMode(mode: com.example.dsp.AbCompareState) {
        service?.setAbCompareMode(mode)
    }

    fun setAutoAbCycle(active: Boolean, remainingSec: Int) {
        service?.setAutoAbCycle(active, remainingSec)
    }
}
