package com.quran.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import androidx.media.session.MediaButtonReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class AudioPlaybackState(
    val surahNumber: Int = 1,
    val surahName: String = "سورة الفاتحة",
    val reciterId: String = "afs",
    val reciterName: String = "مشاري راشد العفاسي",
    val isPlaying: Boolean = false,
    val isPrepared: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val autoPlayNext: Boolean = true
)

class QuranAudioService : Service(), AudioManager.OnAudioFocusChangeListener {

    companion object {
        const val CHANNEL_ID = "quran_audio_channel_v3"
        const val NOTIFICATION_ID = 3001

        const val ACTION_PLAY_SURAH = "com.quran.app.ACTION_PLAY_SURAH"
        const val ACTION_TOGGLE_PLAY = "com.quran.app.ACTION_TOGGLE_PLAY"
        const val ACTION_NEXT = "com.quran.app.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.quran.app.ACTION_PREVIOUS"
        const val ACTION_SEEK_TO = "com.quran.app.ACTION_SEEK_TO"
        const val ACTION_SEEK_BY = "com.quran.app.ACTION_SEEK_BY"
        const val ACTION_STOP_SERVICE = "com.quran.app.ACTION_STOP_SERVICE"

        const val EXTRA_SURAH_NUMBER = "extra_surah_number"
        const val EXTRA_SURAH_NAME = "extra_surah_name"
        const val EXTRA_RECITER_ID = "extra_reciter_id"
        const val EXTRA_RECITER_NAME = "extra_reciter_name"
        const val EXTRA_FILE_PATH = "extra_file_path"
        const val EXTRA_SEEK_MS = "extra_seek_ms"

        private val _playbackState = MutableStateFlow(AudioPlaybackState())
        val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

        fun startSurah(
            context: Context,
            surahNumber: Int,
            surahName: String,
            reciterId: String,
            reciterName: String,
            filePath: String
        ) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = ACTION_PLAY_SURAH
                putExtra(EXTRA_SURAH_NUMBER, surahNumber)
                putExtra(EXTRA_SURAH_NAME, surahName)
                putExtra(EXTRA_RECITER_ID, reciterId)
                putExtra(EXTRA_RECITER_NAME, reciterName)
                putExtra(EXTRA_FILE_PATH, filePath)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun sendCommand(context: Context, action: String, seekMs: Long = 0L) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                this.action = action
                if (seekMs != 0L) putExtra(EXTRA_SEEK_MS, seekMs)
            }
            context.startService(intent)
        }
    }

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioManager: AudioManager
    private var focusRequest: AudioFocusRequest? = null
    private var currentFilePath: String? = null

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            mediaPlayer?.let { player ->
                if (_playbackState.value.isPrepared && player.isPlaying) {
                    val pos = player.currentPosition.toLong()
                    val dur = player.duration.toLong().coerceAtLeast(1L)
                    _playbackState.value = _playbackState.value.copy(
                        currentPositionMs = pos,
                        durationMs = dur,
                        isPlaying = true
                    )
                    updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, pos)
                }
            }
            progressHandler.postDelayed(this, 500L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initMediaSession()
        progressHandler.post(progressRunnable)
    }

    private fun initMediaSession() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSessionCompat(this, "QuranAudioSession").apply {
            setSessionActivity(sessionActivityPendingIntent)
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    resumePlayback()
                }

                override fun onPause() {
                    pausePlayback()
                }

                override fun onSkipToNext() {
                    playAdjacentSurah(1)
                }

                override fun onSkipToPrevious() {
                    playAdjacentSurah(-1)
                }

                override fun onSeekTo(pos: Long) {
                    seekToPosition(pos)
                }

                override fun onStop() {
                    stopAndRelease()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            MediaButtonReceiver.handleIntent(mediaSession, intent)
            when (intent.action) {
                ACTION_PLAY_SURAH -> {
                    val number = intent.getIntExtra(EXTRA_SURAH_NUMBER, 1)
                    val name = intent.getStringExtra(EXTRA_SURAH_NAME) ?: "سورة الفاتحة"
                    val reciterId = intent.getStringExtra(EXTRA_RECITER_ID) ?: "afs"
                    val reciterName = intent.getStringExtra(EXTRA_RECITER_NAME) ?: "مشاري راشد العفاسي"
                    val path = intent.getStringExtra(EXTRA_FILE_PATH) ?: ""
                    playLocalFile(number, name, reciterId, reciterName, path)
                }
                ACTION_TOGGLE_PLAY -> {
                    if (_playbackState.value.isPlaying) pausePlayback() else resumePlayback()
                }
                ACTION_NEXT -> playAdjacentSurah(1)
                ACTION_PREVIOUS -> playAdjacentSurah(-1)
                ACTION_SEEK_TO -> {
                    val target = intent.getLongExtra(EXTRA_SEEK_MS, 0L)
                    seekToPosition(target)
                }
                ACTION_SEEK_BY -> {
                    val delta = intent.getLongExtra(EXTRA_SEEK_MS, 0L)
                    val newPos = (_playbackState.value.currentPositionMs + delta)
                        .coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
                    seekToPosition(newPos)
                }
                ACTION_STOP_SERVICE -> stopAndRelease()
            }
        }
        return START_NOT_STICKY
    }

    private fun playLocalFile(
        surahNumber: Int,
        surahName: String,
        reciterId: String,
        reciterName: String,
        filePath: String
    ) {
        val audioFile = File(filePath)
        if (!audioFile.exists() || audioFile.length() == 0L) return

        currentFilePath = filePath
        requestAudioFocus()

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(audioFile.absolutePath)
                setOnPreparedListener { mp ->
                    val duration = mp.duration.toLong().coerceAtLeast(1L)
                    _playbackState.value = _playbackState.value.copy(
                        surahNumber = surahNumber,
                        surahName = surahName,
                        reciterId = reciterId,
                        reciterName = reciterName,
                        isPlaying = true,
                        isPrepared = true,
                        currentPositionMs = 0L,
                        durationMs = duration
                    )
                    updateMediaMetadata(surahName, reciterName, duration)
                    mp.start()
                    updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, 0L)
                    startForegroundWithNotification()
                }
                setOnCompletionListener {
                    _playbackState.value = _playbackState.value.copy(
                        isPlaying = false,
                        currentPositionMs = _playbackState.value.durationMs
                    )
                    updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED, _playbackState.value.durationMs)
                    if (_playbackState.value.autoPlayNext) {
                        playAdjacentSurah(1)
                    } else {
                        updateNotification()
                    }
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pausePlayback() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                val pos = it.currentPosition.toLong()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = false,
                    currentPositionMs = pos
                )
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PAUSED, pos)
                updateNotification()
            }
        }
    }

    private fun resumePlayback() {
        mediaPlayer?.let {
            if (_playbackState.value.isPrepared && !it.isPlaying) {
                requestAudioFocus()
                it.start()
                val pos = it.currentPosition.toLong()
                _playbackState.value = _playbackState.value.copy(
                    isPlaying = true,
                    currentPositionMs = pos
                )
                updateMediaSessionPlaybackState(PlaybackStateCompat.STATE_PLAYING, pos)
                startForegroundWithNotification()
            }
        }
    }

    private fun seekToPosition(positionMs: Long) {
        mediaPlayer?.let {
            if (_playbackState.value.isPrepared) {
                val safePos = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
                it.seekTo(safePos.toInt())
                _playbackState.value = _playbackState.value.copy(currentPositionMs = safePos)
                val state = if (it.isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                updateMediaSessionPlaybackState(state, safePos)
            }
        }
    }

    private fun playAdjacentSurah(step: Int) {
        val current = _playbackState.value
        val nextNumber = current.surahNumber + step
        if (nextNumber !in 1..114) {
            pausePlayback()
            return
        }

        val audioDir = File(filesDir, "quran_audio/${current.reciterId}")
        val padded = nextNumber.toString().padStart(3, '0')
        val nextFile = File(audioDir, "$padded.mp3")

        if (nextFile.exists() && nextFile.length() > 1024L) {
            val nextSurahName = QuranRepository.getSurahNameByNumber(nextNumber)
            playLocalFile(
                surahNumber = nextNumber,
                surahName = nextSurahName,
                reciterId = current.reciterId,
                reciterName = current.reciterName,
                filePath = nextFile.absolutePath
            )
        } else {
            pausePlayback()
        }
    }

    private fun updateMediaMetadata(title: String, artist: String, durationMs: Long) {
        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_quran)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "الشيخ $artist")
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, "المصحف المرتل")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, largeIcon)
            .build()
        mediaSession.setMetadata(metadata)
    }

    private fun updateMediaSessionPlaybackState(state: Int, positionMs: Long) {
        val actions = PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP

        val playbackState = PlaybackStateCompat.Builder()
            .setActions(actions)
            .setState(state, positionMs, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    private fun buildNotification(): Notification {
        val state = _playbackState.value
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 10, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = PendingIntent.getService(
            this, 11,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val togglePendingIntent = PendingIntent.getService(
            this, 12,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val nextPendingIntent = PendingIntent.getService(
            this, 13,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPendingIntent = PendingIntent.getService(
            this, 14,
            Intent(this, QuranAudioService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIcon = if (state.isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseLabel = if (state.isPlaying) "إيقاف مؤقت" else "تشغيل"

        val largeIcon = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_quran)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_quran)
            .setLargeIcon(largeIcon)
            .setContentTitle(state.surahName)
            .setContentText("القارئ: ${state.reciterName}")
            .setSubText("القرآن الكريم")
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(state.isPlaying)
            .setColor(0xFF0B4F3B.toInt())
            // الأزرار الثلاثة المتراصة في الإشعار: السابق | تشغيل/إيقاف في الوسط | التالي | إغلاق
            .addAction(android.R.drawable.ic_media_previous, "السورة السابقة", prevPendingIntent)
            .addAction(playPauseIcon, playPauseLabel, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "السورة التالية", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق المشغل", stopPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(stopPendingIntent)
            )
            .build()
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "مشغل القرآن الكريم الصوتي",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "التحكم بتلاوة القرآن الكريم في الخلفية وشريط الإشعارات"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener(this)
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pausePlayback()
            AudioManager.AUDIOFOCUS_GAIN -> resumePlayback()
        }
    }

    private fun stopAndRelease() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playbackState.value = _playbackState.value.copy(
            isPlaying = false,
            isPrepared = false,
            currentPositionMs = 0L
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        progressHandler.removeCallbacks(progressRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession.release()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
