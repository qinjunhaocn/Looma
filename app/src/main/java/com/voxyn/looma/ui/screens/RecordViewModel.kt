package com.voxyn.looma.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voxyn.looma.RecordPrefs
import com.voxyn.looma.RecordState
import com.voxyn.looma.RootShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordViewModel : ViewModel() {

    private val _recordState = MutableStateFlow(RecordState())
    val recordState: StateFlow<RecordState> = _recordState.asStateFlow()

    private val _prefs = MutableStateFlow(RecordPrefs())
    val prefs: StateFlow<RecordPrefs> = _prefs.asStateFlow()

    private val _hasRoot = MutableStateFlow<Boolean?>(null)
    val hasRoot: StateFlow<Boolean?> = _hasRoot.asStateFlow()

    private val _detectedResolution = MutableStateFlow("")
    val detectedResolution: StateFlow<String> = _detectedResolution.asStateFlow()

    private var currentPid: String? = null

    init {
        checkRootAndDetect()
        refreshRecordings()
    }

    fun updateResolution(res: String) {
        _prefs.update { it.copy(resolution = res) }
    }

    fun updateFps(fps: Int) {
        _prefs.update { it.copy(fps = fps) }
    }

    fun updateBitRate(mbps: Int) {
        _prefs.update { it.copy(bitRateMbps = mbps) }
    }

    fun toggleRecording() {
        if (_recordState.value.isRecording) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun checkRootAndDetect() {
        viewModelScope.launch {
            val ok = RootShell.hasRoot()
            _hasRoot.value = ok
            if (ok) {
                val res = RootShell.detectResolution()
                _detectedResolution.value = res
            }
        }
    }

    private fun startRecording() {
        viewModelScope.launch {
            val prefs = _prefs.value
            RootShell.ensureDir(prefs.outputDir)

            val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "${ts}.mp4"
            val filePath = "${prefs.outputDir}/$fileName"

            val resolution = prefs.resolution.ifEmpty { _detectedResolution.value }
                .ifEmpty { "1920x1080" }

            _recordState.update {
                it.copy(isRecording = true, currentFile = File(filePath))
            }

            currentPid = RootShell.startRecord(
                outputPath = filePath,
                resolution = resolution,
                fps = prefs.fps,
                bitRate = prefs.bitRate,
            )
        }
    }

    private fun stopRecording() {
        viewModelScope.launch {
            RootShell.stopRecord(currentPid)
            currentPid = null

            // Wait for screenrecord to finalize MP4
            delay(2000)

            val state = _recordState.value
            val file = state.currentFile
            val size = if (file != null && file.exists()) {
                RootShell.fileSize(file.absolutePath)
            } else {
                // Try to find the most recent file in output dir
                val files = RootShell.listRecordings(_prefs.value.outputDir)
                if (files.isNotEmpty()) {
                    val latest = files.first()
                    _recordState.update { it.copy(currentFile = latest) }
                    RootShell.fileSize(latest.absolutePath)
                } else ""
            }

            _recordState.update { it.copy(isRecording = false, currentSize = size) }

            refreshRecordings()
        }
    }

    fun refreshRecordings() {
        viewModelScope.launch {
            val files = RootShell.listRecordings(_prefs.value.outputDir)
            _recordState.update { it.copy(lastFiles = files) }
        }
    }
}
