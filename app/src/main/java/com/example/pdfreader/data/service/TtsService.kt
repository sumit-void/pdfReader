package com.example.pdfreader.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.pdfreader.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

class TtsService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val CHANNEL_ID = "tts_channel"
        const val NOTIFICATION_ID = 1002
        
        const val ACTION_PLAY_PAUSE = "com.example.pdfreader.ACTION_PLAY_PAUSE"
        const val ACTION_STOP = "com.example.pdfreader.ACTION_STOP"
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    private val binder = TtsBinder()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentWordRange = MutableStateFlow<Pair<Int, Int>?>(null)
    val currentWordRange: StateFlow<Pair<Int, Int>?> = _currentWordRange.asStateFlow()

    private var fullText: String = ""
    private var lastStartIndex: Int = 0
    private var mediaSession: MediaSessionCompat? = null

    inner class TtsBinder : Binder() {
        fun getService(): TtsService = this@TtsService
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        mediaSession = MediaSessionCompat(this, "PaperbackTtsSession").apply {
            isActive = true
        }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Timber.e("Language is not supported or missing data")
            } else {
                isTtsInitialized = true
                setupUtteranceListener()
            }
        } else {
            Timber.e("TTS Initialization failed")
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                updateNotification()
            }

            override fun onDone(utteranceId: String?) {
                _isPlaying.value = false
                _currentWordRange.value = null
                lastStartIndex = 0
                updateNotification()
                stopForeground(STOP_FOREGROUND_REMOVE)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
                updateNotification()
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val absoluteStart = lastStartIndex + start
                val absoluteEnd = lastStartIndex + end
                _currentWordRange.value = Pair(absoluteStart, absoluteEnd)
            }
        })
    }

    fun startSpeaking(text: String) {
        if (!isTtsInitialized) return
        fullText = text
        lastStartIndex = 0
        speakCurrent()
    }

    private fun speakCurrent() {
        if (fullText.isEmpty()) return
        val textToSpeak = fullText.substring(lastStartIndex)
        if (textToSpeak.isBlank()) {
            _isPlaying.value = false
            return
        }

        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "PaperbackTts")
        }
        
        tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "PaperbackTts")
        _isPlaying.value = true
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun pauseSpeaking() {
        tts?.stop()
        _isPlaying.value = false
        updateNotification()
        _currentWordRange.value?.let {
            lastStartIndex = it.first
        }
    }

    fun resumeSpeaking() {
        speakCurrent()
    }

    fun stopSpeaking() {
        tts?.stop()
        _isPlaying.value = false
        _currentWordRange.value = null
        lastStartIndex = 0
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY_PAUSE -> {
                if (_isPlaying.value) {
                    pauseSpeaking()
                } else {
                    resumeSpeaking()
                }
            }
            ACTION_STOP -> {
                stopSpeaking()
            }
        }
        return START_NOT_STICKY
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): android.app.Notification {
        val playPauseIcon = if (_isPlaying.value) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseTitle = if (_isPlaying.value) "Pause" else "Play"

        val playPauseIntent = Intent(this, TtsService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TtsService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession?.sessionToken)
            .setShowActionsInCompactView(0, 1)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Paperback Reader TTS")
            .setContentText(if (_isPlaying.value) "Reading aloud..." else "Playback paused")
            .setSmallIcon(R.drawable.ic_menu_book)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_ff, "Stop", stopPendingIntent)
            .setStyle(mediaStyle)
            .setOngoing(_isPlaying.value)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Text-to-Speech",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls for Text-to-Speech playback"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        mediaSession?.release()
        super.onDestroy()
    }
}
