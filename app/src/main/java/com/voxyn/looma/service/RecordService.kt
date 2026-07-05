package com.voxyn.looma.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service to keep recording alive when app is in background.
 * Currently a scaffold — recording logic is driven from ViewModel + RootShell.
 */
class RecordService : Service() {

    companion object {
        const val CHANNEL_ID = "looma_record"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Screen Recording",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Looma screen recording service"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Looma")
            .setContentText("Recording in progress…")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
