package com.voxyn.looma

import java.io.File

/**
 * Represents the current recording state, shared across UI and service.
 */
data class RecordState(
    val isRecording: Boolean = false,
    val currentFile: File? = null,
    val currentSize: String = "",
    val lastFiles: List<File> = emptyList(),
) {
    val recordingLabel: String get() = if (isRecording) "Recording…" else "Ready"
}

/**
 * User-configurable recording preferences.
 */
data class RecordPrefs(
    val resolution: String = "",          // empty = auto-detect
    val fps: Int = 30,
    val bitRateMbps: Int = 8,
    val outputDir: String = "/sdcard/SR",
) {
    val bitRate: Int get() = bitRateMbps * 1_000_000
}
