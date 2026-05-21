package com.example.pdfreader.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.pdfreader.R
import timber.log.Timber

class ReadingTimerService : Service() {

    companion object {
        const val CHANNEL_ID = "reading_timer_channel"
        const val NOTIFICATION_ID = 1001
        const val EXTRA_BOOK_TITLE = "book_title"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bookTitle = intent?.getStringExtra(EXTRA_BOOK_TITLE) ?: "Reading"

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Reading in progress")
            .setContentText(bookTitle)
            .setSmallIcon(R.drawable.ic_menu_book)
            .setOngoing(true)
            .setSilent(true)
            .build()

        try {
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service")
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reading Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your reading time"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
