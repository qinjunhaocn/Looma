package com.voxyn.looma

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Utility to run shell commands via root (su).
 */
object RootShell {

    private const val TAG = "RootShell"

    /** Check whether the device has root access. */
    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val uid = proc.inputStream.bufferedReader().readText().trim()
            proc.waitFor()
            uid == "0"
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
            proc.waitFor()
            Regex("(\\d+x\\d+)").find(out)?.value ?: "1920x1080"
        } catch (e: Exception) {
            "1920x1080"
        }
    }

    /**
     * Start screenrecord via root.
     * Writes the PID to a known file so we can SIGINT it later.
     * Returns the PID file path.
     */
    suspend fun startRecord(
        outputPath: String,
        resolution: String,
        fps: Int,
        bitRate: Int,
    ): String = withContext(Dispatchers.IO) {
        val pidFile = "/sdcard/SR/.looma_pid"
        val logFile = "/sdcard/SR/.looma_log"

        // Build args
        val args = buildString {
            append("screenrecord")
            append(" --size \"$resolution\"")
            append(" --bit-rate $bitRate")
            append(" --time-limit 1800")
            if (fps > 0) append(" --set-fps $fps")
            append(" \"$outputPath\"")
        }

        // Write a shell script that runs screenrecord and saves its PID,
        // then run that script via su
        val script = buildString {
            appendLine("#!/system/bin/sh")
            appendLine("$args &")
            appendLine("echo \\$! > $pidFile")
            appendLine("wait")
        }

        val scriptFile = "/sdcard/SR/.looma_cmd.sh"
        File(scriptFile).writeText(script)

        // Make it executable and run it via su
        val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "sh $scriptFile > $logFile 2>&1 &"))

        // Read PID from file with retries
        var pid = ""
        for (i in 1..10) {
            kotlinx.coroutines.delay(200)
            val pf = File(pidFile)
            if (pf.exists()) {
                pid = pf.readText().trim()
                if (pid.isNotEmpty()) break
            }
        }

        Log.d(TAG, "screenrecord PID: $pid")
        pid
    }

    /** Stop recording by sending SIGINT. */
    suspend fun stopRecord(pid: String?) {
        withContext(Dispatchers.IO) {
            // Try by PID first
            if (!pid.isNullOrEmpty()) {
                Log.d(TAG, "Killing PID $pid")
                Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -INT $pid")).waitFor()
            }
            // Also try pkill / killall as fallback
            Runtime.getRuntime().exec(arrayOf(
                "su", "-c",
                "killall -INT screenrecord 2>/dev/null || pkill -INT screenrecord 2>/dev/null || kill -INT \$(pgrep -x screenrecord) 2>/dev/null || true"
            )).waitFor()

            // Cleanup temp files
            try {
                File("/sdcard/SR/.looma_pid").delete()
                File("/sdcard/SR/.looma_cmd.sh").delete()
            } catch (_: Exception) {}
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
    suspend fun listRecordings(dir: String): List<File> = withContext(Dispatchers.IO) {
        val d = File(dir)
        if (!d.exists()) return@withContext emptyList()
        d.listFiles { f -> f.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    /** Ensure directory exists */
    suspend fun ensureDir(dir: String) = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p \"$dir\"")).waitFor()
        Runtime.getRuntime().exec(arrayOf("su", "-c", "chmod 777 \"$dir\"")).waitFor()
    }
}
