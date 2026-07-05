package com.voxyn.looma

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Root shell screen recording — literally just your shell script ported.
 */
object RootShell {

    private const val TAG = "RootShell"
    private const val PID_FILE = "/sdcard/SR/.looma_pid"
    private const val SCRIPT_FILE = "/sdcard/SR/.looma_cmd.sh"

    suspend fun hasRoot(): Boolean = withContext(Dispatchers.IO) {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "id -u"))
            val uid = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            uid == "0"
        } catch (e: Exception) {
            Log.e(TAG, "Root check failed", e)
            false
        }
    }

    suspend fun detectResolution(): String = withContext(Dispatchers.IO) {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "wm size"))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            Regex("(\\d+x\\d+)").find(out)?.value ?: "1920x1080"
        } catch (_: Exception) {
            "1920x1080"
        }
    }

    /**
     * Write & run the shell script, return PID.
     */
    suspend fun startRecord(
        outputPath: String,
        resolution: String,
        fps: Int,
        bitRate: Int,
    ): String = withContext(Dispatchers.IO) {
        // 1. Write the exact same shell script the user wrote
        //    but with dynamic params and PID saved to a known file.
        val script = buildString {
            appendLine("#!/system/bin/sh")
            appendLine("mkdir -p \"${File(outputPath).parent}\"")
            append("screenrecord --size $resolution --bit-rate $bitRate --time-limit 1800")
            if (fps > 0) append(" --set-fps $fps")
            appendLine(" \"$outputPath\" &")
            appendLine("echo \$! > $PID_FILE")
            appendLine("wait")
        }
        File(SCRIPT_FILE).writeText(script)

        // 2. Run it via su
        Log.d(TAG, "Starting: su -c sh $SCRIPT_FILE")
        Runtime.getRuntime().exec(arrayOf("su", "-c", "sh $SCRIPT_FILE"))
            

        // 3. Wait for PID file
        for (i in 1..20) {
            kotlinx.coroutines.delay(250)
            val f = File(PID_FILE)
            if (f.exists()) {
                val pid = f.readText().trim()
                if (pid.isNotEmpty()) {
                    Log.d(TAG, "Got PID=$pid")
                    return@withContext pid
                }
            }
        }
        Log.w(TAG, "PID file not found after 5s")
        ""
    }

    /**
     * Stop by sending SIGINT to the recorded PID.
     */
    suspend fun stopRecord(pid: String?) {
        withContext(Dispatchers.IO) {
            // 1. Try by PID
            if (!pid.isNullOrEmpty()) {
                Log.d(TAG, "kill -INT $pid")
                Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -INT $pid")).waitFor()
            }
            // 2. Try reading PID file as fallback
            try {
                val pidFromFile = File(PID_FILE).readText().trim()
                if (pidFromFile.isNotEmpty()) {
                    Log.d(TAG, "kill -INT $pidFromFile (from file)")
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "kill -INT $pidFromFile")).waitFor()
                }
            } catch (_: Exception) {}

            // 3. Nuke any remaining screenrecord just in case
            Runtime.getRuntime().exec(arrayOf("su", "-c", "pkill -INT screenrecord 2>/dev/null; killall -INT screenrecord 2>/dev/null; true")).waitFor()

            // 4. Cleanup
            try { File(PID_FILE).delete() } catch (_: Exception) {}
            try { File(SCRIPT_FILE).delete() } catch (_: Exception) {}
        }
    }

    suspend fun fileSize(filePath: String): String = withContext(Dispatchers.IO) {
        try {
            val p = Runtime.getRuntime().exec(arrayOf("su", "-c", "du -h \"$filePath\""))
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            out.split("\t").firstOrNull() ?: ""
        } catch (_: Exception) { "" }
    }

    suspend fun listRecordings(dir: String): List<File> = withContext(Dispatchers.IO) {
        val d = File(dir)
        if (!d.exists()) return@withContext emptyList()
        d.listFiles { f -> f.extension == "mp4" }
            ?.sortedByDescending { it.lastModified() }
            ?.toList() ?: emptyList()
    }

    suspend fun ensureDir(dir: String) = withContext(Dispatchers.IO) {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "mkdir -p \"$dir\"")).waitFor()
    }
}
