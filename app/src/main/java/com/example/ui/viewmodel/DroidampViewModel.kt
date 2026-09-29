package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.AppDatabase
import com.example.data.CyberdeckSkin
import com.example.data.EqualizerPresetEntity
import com.example.data.GeminiAudioAnalysisResult
import com.example.data.GeminiEqualizerRepository
import com.example.data.PresetRepository
import com.example.data.RadioRepository
import com.example.data.RadioStation
import com.example.data.SongUploadManager
import com.example.data.UploadedSongEntity
import com.example.data.UploadedSongRepository
import com.example.data.UserPreferencesRepository
import com.example.dsp.AbCompareState
import com.example.dsp.AudioResolutionMetrics
import com.example.dsp.DspParameters
import com.example.dsp.ResolutionUpscaleMode
import com.example.dsp.VuMeterState
import com.example.service.AudioPlayerController
import android.net.Uri
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@UnstableApi
class DroidampViewModel(application: Application) : AndroidViewModel(application) {

    val playerController = AudioPlayerController(application)
    private val db = AppDatabase.getDatabase(application)
    private val presetRepository = PresetRepository(db.presetDao())
    private val songUploadManager = SongUploadManager(application)
    val uploadedSongRepository = UploadedSongRepository(db.uploadedSongDao(), songUploadManager)
    val preferencesRepository = UserPreferencesRepository(application)
    val radioRepository = RadioRepository()
    val geminiRepository = GeminiEqualizerRepository()

    // Real-time Audio Resolution & Upscaling Metrics
    val resolutionMetrics: StateFlow<AudioResolutionMetrics> = playerController.audioResolutionMetrics

    // A/B Comparison States (Raw vs Upscaled)
    private val _abCompareMode = MutableStateFlow(AbCompareState.MODE_B_UPSCALED)
    val abCompareMode: StateFlow<AbCompareState> = _abCompareMode.asStateFlow()

    private val _isAutoCycleActive = MutableStateFlow(false)
    val isAutoCycleActive: StateFlow<Boolean> = _isAutoCycleActive.asStateFlow()

    private val _autoCycleCountdown = MutableStateFlow(0)
    val autoCycleCountdown: StateFlow<Int> = _autoCycleCountdown.asStateFlow()

    private var autoCycleJob: Job? = null

    // Auto-Detect & Auto-Apply EQ States
    private val _isAutoEqEnabled = MutableStateFlow(true) // Enabled by default
    val isAutoEqEnabled: StateFlow<Boolean> = _isAutoEqEnabled.asStateFlow()

    private val _autoDetectedGenre = MutableStateFlow<String?>("Synthwave")
    val autoDetectedGenre: StateFlow<String?> = _autoDetectedGenre.asStateFlow()

    private val _autoEqStatusMessage = MutableStateFlow<String?>("Auto-EQ Active: Real-time Profile Tuning")
    val autoEqStatusMessage: StateFlow<String?> = _autoEqStatusMessage.asStateFlow()

    // Uploaded Songs Flow from Room
    val uploadedSongs: StateFlow<List<UploadedSongEntity>> = uploadedSongRepository.allUploadedSongs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadMessage = MutableStateFlow<String?>(null)
    val uploadMessage: StateFlow<String?> = _uploadMessage.asStateFlow()

    // Preferences
    val userSettings = preferencesRepository.userSettingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.data.UserSettings()
    )

    // Presets from Room
    val presets: StateFlow<List<EqualizerPresetEntity>> = presetRepository.allPresets.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active DSP Parameters
    private val _dspParams = MutableStateFlow(DspParameters())
    val dspParams: StateFlow<DspParameters> = _dspParams.asStateFlow()

    // Current selected preset name
    private val _selectedPresetName = MutableStateFlow("Cyberdeck Club")
    val selectedPresetName: StateFlow<String> = _selectedPresetName.asStateFlow()

    // Radio stations
    private val _radioStations = MutableStateFlow<List<RadioStation>>(emptyList())
    val radioStations: StateFlow<List<RadioStation>> = _radioStations.asStateFlow()

    private val _isRadioLoading = MutableStateFlow(false)
    val isRadioLoading: StateFlow<Boolean> = _isRadioLoading.asStateFlow()

    // AI Tuning State
    private val _aiTuningState = MutableStateFlow<AiTuneUiState>(AiTuneUiState.Idle)
    val aiTuningState: StateFlow<AiTuneUiState> = _aiTuningState.asStateFlow()

    // Hot Cues positions
    var hotCue1Ms = 0L
    var hotCue2Ms = 0L
    var hotCue3Ms = 0L
    var hotCue4Ms = 0L

    // A-B Looping
    var loopStartMs = 0L
    var loopEndMs = 0L
    private val _isLoopActive = MutableStateFlow(false)
    val isLoopActive: StateFlow<Boolean> = _isLoopActive.asStateFlow()

    init {
        playerController.bind()
        viewModelScope.launch {
            presetRepository.ensureDefaultPresets()
            loadCuratedStations()
        }
        viewModelScope.launch {
            userSettings.collect { settings ->
                hotCue1Ms = settings.cue1Ms
                hotCue2Ms = settings.cue2Ms
                hotCue3Ms = settings.cue3Ms
                hotCue4Ms = settings.cue4Ms
            }
        }
        viewModelScope.launch {
            playerController.trackTitle.collect { title ->
                if (_isAutoEqEnabled.value && title.isNotBlank() && title != "TensorRT Audio Engine Ready") {
                    autoDetectAndApplyForTrack(title)
                }
            }
        }
    }

    private fun loadCuratedStations() {
        _radioStations.value = radioRepository.curatedStations
    }

    fun uploadSongFromUri(uri: Uri) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Importing audio file..."
            val result = uploadedSongRepository.uploadFromUri(uri)
            result.onSuccess { song ->
                _uploadMessage.value = "Imported: ${song.title}"
                playUploadedSong(song)
            }.onFailure { e ->
                _uploadMessage.value = "Failed: ${e.message}"
            }
            _isUploading.value = false
        }
    }

    fun playUploadedSong(song: UploadedSongEntity) {
        val label = if (song.artist.isNotBlank() && song.artist != "Local Device") {
            "${song.artist} - ${song.title}"
        } else {
            song.title
        }
        playerController.playStream(song.filePath, label)
    }

    fun deleteUploadedSong(song: UploadedSongEntity) {
        viewModelScope.launch {
            uploadedSongRepository.deleteSong(song)
        }
    }

    fun uploadMultipleSongs(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Importing ${uris.size} tracks..."
            val songs = uploadedSongRepository.uploadMultipleFromUris(uris)
            if (songs.isNotEmpty()) {
                _uploadMessage.value = "Imported ${songs.size} tracks"
                playUploadedSong(songs.first())
            } else {
                _uploadMessage.value = "Import failed"
            }
            _isUploading.value = false
        }
    }

    fun uploadFromUrl(url: String, customTitle: String? = null) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Downloading audio stream..."
            val result = uploadedSongRepository.uploadFromUrl(url.trim(), customTitle)
            result.onSuccess { song ->
                _uploadMessage.value = "Imported: ${song.title}"
                playUploadedSong(song)
            }.onFailure { e ->
                _uploadMessage.value = "Download failed: ${e.message}"
            }
            _isUploading.value = false
        }
    }

    fun generateLoFiDemoTrack() {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Synthesizing 808 Lo-Fi Beat..."
            val result = uploadedSongRepository.createDemoLoFiTrack()
            result.onSuccess { song ->
                _uploadMessage.value = "Generated: ${song.title}"
                playUploadedSong(song)
            }.onFailure { e ->
                _uploadMessage.value = "Failed: ${e.message}"
            }
            _isUploading.value = false
        }
    }

    fun generateDemoTrack() {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadMessage.value = "Synthesizing Cyberdeck Track..."
            val result = uploadedSongRepository.createDemoCyberdeckTrack()
            result.onSuccess { song ->
                _uploadMessage.value = "Generated: ${song.title}"
                playUploadedSong(song)
            }.onFailure { e ->
                _uploadMessage.value = "Failed: ${e.message}"
            }
            _isUploading.value = false
        }
    }

    fun clearUploadMessage() {
        _uploadMessage.value = null
    }

    fun searchRadio(query: String? = null, tag: String? = null) {
        viewModelScope.launch {
            _isRadioLoading.value = true
            _radioStations.value = radioRepository.getStations(tag = tag, query = query)
            _isRadioLoading.value = false
        }
    }

    fun playStation(station: RadioStation) {
        playerController.playStream(station.streamUrl, station.name)
        viewModelScope.launch {
            preferencesRepository.setLastStation(station.name, station.streamUrl)
        }
    }

    fun playDemoSynth() {
        // Built-in Synthwave stream for immediate playback demo
        val demo = radioRepository.curatedStations.first()
        playStation(demo)
    }

    fun togglePlayPause() {
        if (!playerController.isPlaying.value && playerController.trackTitle.value == "TensorRT Audio Engine Ready") {
            playDemoSynth()
        } else {
            playerController.togglePlayPause()
        }
    }

    fun updateEqBand(index: Int, gainDb: Float) {
        val currentGains = _dspParams.value.eqGainsDb.toMutableList()
        if (index in currentGains.indices) {
            currentGains[index] = gainDb.coerceIn(-12f, 12f)
            val updated = _dspParams.value.copy(eqGainsDb = currentGains)
            _dspParams.value = updated
            playerController.updateDsp(updated)
            _selectedPresetName.value = "Custom"
        }
    }

    fun setTubeWarmth(warmth: Float) {
        val updated = _dspParams.value.copy(tubeWarmth = warmth.coerceIn(0f, 1f))
        _dspParams.value = updated
        playerController.updateDsp(updated)
    }

    fun setSuperResClarity(clarity: Float) {
        val updated = _dspParams.value.copy(superResClarity = clarity.coerceIn(0f, 1f))
        _dspParams.value = updated
        playerController.updateDsp(updated)
    }

    fun toggleSuperResolution(enabled: Boolean) {
        val updated = _dspParams.value.copy(superResolutionEnabled = enabled)
        _dspParams.value = updated
        playerController.updateDsp(updated)
    }

    fun toggleCrossfeed(enabled: Boolean) {
        val updated = _dspParams.value.copy(crossfeedEnabled = enabled)
        _dspParams.value = updated
        playerController.updateDsp(updated)
    }

    fun setCrossfeedLevel(level: Float) {
        val updated = _dspParams.value.copy(crossfeedLevel = level.coerceIn(0f, 1f))
        _dspParams.value = updated
        playerController.updateDsp(updated)
    }

    fun toggleUpscaling(enabled: Boolean) {
        val updated = _dspParams.value.copy(isUpscalingEnabled = enabled)
        _dspParams.value = updated
        playerController.updateDsp(updated)
        viewModelScope.launch {
            preferencesRepository.setUpscalingEnabled(enabled)
        }
    }

    fun setUpscaleMode(mode: ResolutionUpscaleMode) {
        val updated = _dspParams.value.copy(upscaleMode = mode)
        _dspParams.value = updated
        playerController.updateDsp(updated)
        viewModelScope.launch {
            preferencesRepository.setUpscaleMode(mode)
        }
    }

    fun setUltrasonicAir(level: Float) {
        val updated = _dspParams.value.copy(ultrasonicAirLevel = level.coerceIn(0f, 1f))
        _dspParams.value = updated
        playerController.updateDsp(updated)
        viewModelScope.launch {
            preferencesRepository.setUltrasonicAir(level)
        }
    }

    fun setTransientSharpening(level: Float) {
        val updated = _dspParams.value.copy(transientSharpening = level.coerceIn(0f, 1f))
        _dspParams.value = updated
        playerController.updateDsp(updated)
        viewModelScope.launch {
            preferencesRepository.setTransientSharpening(level)
        }
    }

    // A/B Instant Comparison Methods
    fun setAbCompareMode(mode: AbCompareState) {
        _abCompareMode.value = mode
        playerController.setAbCompareMode(mode)
    }

    fun toggleAbCompare() {
        val next = if (_abCompareMode.value == AbCompareState.MODE_B_UPSCALED) {
            AbCompareState.MODE_A_RAW
        } else {
            AbCompareState.MODE_B_UPSCALED
        }
        setAbCompareMode(next)
    }

    fun toggleAutoAbCycle(intervalSeconds: Int = 5) {
        if (_isAutoCycleActive.value) {
            autoCycleJob?.cancel()
            autoCycleJob = null
            _isAutoCycleActive.value = false
            _autoCycleCountdown.value = 0
            playerController.setAutoAbCycle(false, 0)
        } else {
            _isAutoCycleActive.value = true
            playerController.setAutoAbCycle(true, intervalSeconds)
            autoCycleJob = viewModelScope.launch {
                while (isActive) {
                    for (sec in intervalSeconds downTo 1) {
                        _autoCycleCountdown.value = sec
                        playerController.setAutoAbCycle(true, sec)
                        delay(1000)
                    }
                    toggleAbCompare()
                }
            }
        }
    }

    // Auto-Detect & Auto-Apply EQ Methods
    fun toggleAutoEq(enabled: Boolean) {
        _isAutoEqEnabled.value = enabled
        if (enabled) {
            _autoEqStatusMessage.value = "Auto-EQ Active: Real-time Profile Tuning"
            val title = playerController.trackTitle.value
            if (title.isNotBlank() && title != "TensorRT Audio Engine Ready") {
                autoDetectAndApplyForTrack(title)
            }
        } else {
            _autoEqStatusMessage.value = "Auto-EQ Paused (Manual Mode)"
        }
    }

    fun autoDetectAndApplyForTrack(title: String) {
        if (title.isBlank() || title == "TensorRT Audio Engine Ready") return
        viewModelScope.launch {
            _autoEqStatusMessage.value = "Auto-Detecting genre & mastering curve..."
            val result = geminiRepository.analyzeAndTune(title)
            result.onSuccess { analysis ->
                _autoDetectedGenre.value = analysis.detectedGenre
                _autoEqStatusMessage.value = "Auto-Applied: [${analysis.detectedGenre}] ${analysis.spectralBalance}"
                _selectedPresetName.value = "Auto: ${analysis.detectedGenre}"

                val updatedDsp = _dspParams.value.copy(
                    eqGainsDb = analysis.eqGainsDb,
                    tubeWarmth = analysis.tubeWarmth,
                    superResolutionEnabled = analysis.superResolutionEnabled,
                    superResClarity = analysis.superResClarity,
                    crossfeedEnabled = analysis.crossfeedEnabled,
                    crossfeedLevel = analysis.crossfeedLevel
                )
                _dspParams.value = updatedDsp
                playerController.updateDsp(updatedDsp)
            }.onFailure {
                // Deterministic acoustic fallback
                val profile = geminiRepository.getDeterministicProfile(title, "Headphones")
                _autoDetectedGenre.value = profile.detectedGenre
                _autoEqStatusMessage.value = "Auto-Applied: [${profile.detectedGenre}] Optimal Curve"
                _selectedPresetName.value = "Auto: ${profile.detectedGenre}"

                val updatedDsp = _dspParams.value.copy(
                    eqGainsDb = profile.eqGainsDb,
                    tubeWarmth = profile.tubeWarmth,
                    superResolutionEnabled = profile.superResolutionEnabled,
                    superResClarity = profile.superResClarity,
                    crossfeedEnabled = profile.crossfeedEnabled,
                    crossfeedLevel = profile.crossfeedLevel
                )
                _dspParams.value = updatedDsp
                playerController.updateDsp(updatedDsp)
            }
        }
    }

    fun resetEq() {
        val updated = _dspParams.value.copy(eqGainsDb = List(10) { 0f })
        _dspParams.value = updated
        playerController.updateDsp(updated)
        _selectedPresetName.value = "Flat Line"
    }

    fun applyPreset(preset: EqualizerPresetEntity) {
        val gains = preset.toGainsList()
        val updated = _dspParams.value.copy(
            eqGainsDb = gains,
            tubeWarmth = preset.tubeWarmth,
            superResClarity = preset.superResClarity,
            crossfeedEnabled = preset.crossfeedEnabled,
            crossfeedLevel = preset.crossfeedLevel
        )
        _dspParams.value = updated
        _selectedPresetName.value = preset.name
        playerController.updateDsp(updated)
    }

    fun saveCurrentAsPreset(name: String, genre: String) {
        viewModelScope.launch {
            val preset = EqualizerPresetEntity.fromGainsList(
                name = name,
                genre = genre,
                gains = _dspParams.value.eqGainsDb,
                tubeWarmth = _dspParams.value.tubeWarmth,
                superResClarity = _dspParams.value.superResClarity,
                crossfeedEnabled = _dspParams.value.crossfeedEnabled,
                crossfeedLevel = _dspParams.value.crossfeedLevel,
                isFactoryDefault = false
            )
            presetRepository.savePreset(preset)
            _selectedPresetName.value = name
        }
    }

    fun deleteUserPreset(id: Long) {
        viewModelScope.launch {
            presetRepository.deleteUserPreset(id)
        }
    }

    // Hot Cues
    fun triggerHotCue(cueIndex: Int) {
        val pos = when (cueIndex) {
            1 -> hotCue1Ms
            2 -> hotCue2Ms
            3 -> hotCue3Ms
            4 -> hotCue4Ms
            else -> 0L
        }
        playerController.jumpToHotCue(pos)
    }

    fun storeHotCue(cueIndex: Int) {
        val currentPos = playerController.currentPositionMs.value
        when (cueIndex) {
            1 -> hotCue1Ms = currentPos
            2 -> hotCue2Ms = currentPos
            3 -> hotCue3Ms = currentPos
            4 -> hotCue4Ms = currentPos
        }
        viewModelScope.launch {
            preferencesRepository.setHotCue(cueIndex, currentPos)
        }
    }

    // A-B Looping
    fun setLoopPinA() {
        loopStartMs = playerController.currentPositionMs.value
        if (loopEndMs > loopStartMs) {
            _isLoopActive.value = true
            playerController.setAbLoop(loopStartMs, loopEndMs)
        }
    }

    fun setLoopPinB() {
        val current = playerController.currentPositionMs.value
        if (current > loopStartMs) {
            loopEndMs = current
            _isLoopActive.value = true
            playerController.setAbLoop(loopStartMs, loopEndMs)
        }
    }

    fun toggleAbLoop() {
        if (_isLoopActive.value) {
            _isLoopActive.value = false
            playerController.clearAbLoop()
        } else if (loopEndMs > loopStartMs) {
            _isLoopActive.value = true
            playerController.setAbLoop(loopStartMs, loopEndMs)
        }
    }

    // Playback Pitch/Speed
    fun setPlaybackSpeed(speed: Float) {
        playerController.setPlaybackSpeed(speed)
        viewModelScope.launch {
            preferencesRepository.setPlaybackPitch(speed)
        }
    }

    // Sleep Timer
    fun setSleepTimer(minutes: Int) {
        playerController.startSleepTimer(minutes)
    }

    fun cancelSleepTimer() {
        playerController.cancelSleepTimer()
    }

    // Skin
    fun changeSkin(skin: CyberdeckSkin) {
        viewModelScope.launch {
            preferencesRepository.setSkin(skin)
        }
    }

    // Gemini Neural Auto-EQ Tuning
    fun runGeminiSpectralTuning(prompt: String, context: String) {
        viewModelScope.launch {
            _aiTuningState.value = AiTuneUiState.Analyzing
            val result = geminiRepository.analyzeAndTune(prompt, context)
            result.onSuccess { data ->
                _aiTuningState.value = AiTuneUiState.Success(data)
                // Automatically apply AI-tuned parameters
                val updated = _dspParams.value.copy(
                    eqGainsDb = data.eqGainsDb,
                    tubeWarmth = data.tubeWarmth,
                    superResolutionEnabled = data.superResolutionEnabled,
                    superResClarity = data.superResClarity,
                    crossfeedEnabled = data.crossfeedEnabled,
                    crossfeedLevel = data.crossfeedLevel
                )
                _dspParams.value = updated
                _selectedPresetName.value = "AI: ${data.detectedGenre}"
                playerController.updateDsp(updated)
            }.onFailure { err ->
                _aiTuningState.value = AiTuneUiState.Error(err.message ?: "Analysis failed")
            }
        }
    }

    fun dismissAiDialog() {
        _aiTuningState.value = AiTuneUiState.Idle
    }

    override fun onCleared() {
        playerController.unbind()
        super.onCleared()
    }
}

sealed interface AiTuneUiState {
    data object Idle : AiTuneUiState
    data object Analyzing : AiTuneUiState
    data class Success(val result: GeminiAudioAnalysisResult) : AiTuneUiState
    data class Error(val message: String) : AiTuneUiState
}
