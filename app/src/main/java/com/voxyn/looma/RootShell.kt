package com.voxyn.looma

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Utility to run shell commands via root (su).
 */
object RootShell {

    private const val TAG = "RootShell"

    /** Check whether the device has root access. */
    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val exit = proc.waitFor()
            val uid = proc.inputStream.bufferedReader().readText().trim()
            exit == 0 && uid == "0"
        } catch (e: Exception) {
            Log.e(TAG, "Root check failed", e)
            false
        }
    }

    /** Auto-detect display resolution via `wm size` */
    suspend fun detectResolution(): String = withContext(Dispatchers.IO) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "wm size"))
            val out = proc.inputStream.bufferedReader().readText().trim()
            val match = Regex("(\\d+x\\d+)").find(out)
            match?.value ?: "1920x1080"
        } catch (e: Exception) {
            "1920x1080"
        }
    }

    /**
     * Start screenrecord as a background process.
     * Returns the PID (as a string) so we can stop it later.
     */
    suspend fun startRecord(
        outputPath: String,
        resolution: String,
        fps: Int,
        bitRate: Int,
    ): String = withContext(Dispatchers.IO) {
        val cmd = buildString {
            append("su -c '")
            append("screenrecord")
            append(" --size \"$resolution\"")
            append(" --bit-rate $bitRate")
            append(" --time-limit 1800")
            if (fps > 0) append(" --set-fps $fps")
            append(" \"$outputPath\"")
            append(" & echo \\$!'")
        }
        Log.d(TAG, "startRecord: $cmd")

        val proc = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
        val pid = proc.inputStream.bufferedReader().readText().trim()
        val err = proc.errorStream.bufferedReader().readText().trim()
        if (err.isNotEmpty()) Log.w(TAG, "startRecord stderr: $err")
        proc.waitFor()
        pid.ifEmpty {
            // Fallback: try to find PID by grepping
            findPid(outputPath)
        }
    }

    /** Stop the recording by sending SIGINT. */
    suspend fun stopRecord(pid: String?) {
        withContext(Dispatchers.IO) {
            // Kill by PID if we have it
            if (!pid.isNullOrEmpty()) {
                Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -INT $pid")).waitFor()
            }
            // Also kill all screenrecord processes
            Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -INT screenrecord")).waitFor()
        }
    }

    /** Get file size in human-readable form */
    suspend fun fileSize(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "du -h \"$filePath\""))
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out.split("\t").firstOrNull() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    /** List recordings sorted by newest first */
    suspend fun listRecordings(dir: String): List<java.io.File> = withContext(Dispatchers.IO) {
        val d = java.io.File(dir)
        if (!d.exists()) return@withContext emptyList()
        d.listFiles { f -> f.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    /** Ensure directory exists (with root, in case it's on a restricted path) */
    suspend fun ensureDir(dir: String) = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p \"$dir\"")).waitFor()
    }

    private suspend fun findPid(outputPath: String): String = withContext(Dispatchers.IO) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "pgrep -x screenrecord"))
            val out = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            out.lines().firstOrNull() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}
