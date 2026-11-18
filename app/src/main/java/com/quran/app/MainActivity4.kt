@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ============================================================
// 1. خدمة الصوت والتخزين المحلي المطورة (PlayerService & AudioCache)
// ============================================================
data class PlayerState(
    val reciterId: String = "afs",
    val surah: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: Float = 1.0f,
    val repeatMode: Int = 0, // 0: إيقاف، 1: تكرار 3 مرات (تحفيظ)، 2: تكرار مستمر ∞
    val repeatCount: Int = 0,
    val autoNext: Boolean = true,
    val sleepLeft: Int = 0,
    val downloads: Map<String, Float> = emptyMap(),
    val error: String? = null,
    val cacheVersion: Int = 0
)

object AudioCache {
    fun dir(ctx: Context): File = File(ctx.filesDir, "audio").apply { mkdirs() }
    fun file(ctx: Context, reciterId: String, num: Int) = File(dir(ctx), "${reciterId}_$num.mp3")
    fun has(ctx: Context, reciterId: String, num: Int): Boolean = file(ctx, reciterId, num).let { it.exists() && it.length() > 1000 }
    fun count(ctx: Context): Int = dir(ctx).listFiles()?.count { it.name.endsWith(".mp3") } ?: 0
    fun sizeMb(ctx: Context): Long = (dir(ctx).listFiles()?.sumOf { it.length() } ?: 0L) / (1024 * 1024)
    fun clear(ctx: Context) { dir(ctx).listFiles()?.forEach { it.delete() } }
}

class PlayerService : Service() {
    companion object {
        private const val NOTIF_ID = 77
        private const val A_PLAY = "com.quran.app.PLAY"
        private const val A_TOGGLE = "com.quran.app.TOGGLE"
        private const val A_STOP = "com.quran.app.STOP"
        private const val A_NEXT = "com.quran.app.NEXT"
        private const val A_PREV = "com.quran.app.PREV"
        private const val A_SEEK = "com.quran.app.SEEK"
        private const val A_SEEK_BY = "com.quran.app.SEEK_BY"
        private const val A_SPEED = "com.quran.app.SPEED"
        private const val A_REPEAT = "com.quran.app.REPEAT"
        private const val A_AUTONEXT = "com.quran.app.AUTONEXT"
        private const val A_SLEEP = "com.quran.app.SLEEP"
        private const val A_DOWNLOAD = "com.quran.app.DOWNLOAD"

        private val _state = MutableStateFlow(PlayerState())
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        private fun intent(ctx: Context, action: String) = Intent(ctx, PlayerService::class.java).setAction(action)

        private fun sendAction(ctx: Context, i: Intent) {
            try {
                ContextCompat.startForegroundService(ctx, i)
            } catch (_: Exception) {
                runCatching { ctx.startService(i) }
            }
        }

        fun play(ctx: Context, reciterId: String, surah: Int) =
            sendAction(ctx, intent(ctx, A_PLAY).putExtra("r", reciterId).putExtra("s", surah))

        fun download(ctx: Context, reciterId: String, surah: Int) =
            sendAction(ctx, intent(ctx, A_DOWNLOAD).putExtra("r", reciterId).putExtra("s", surah))

        fun delete(ctx: Context, reciterId: String, surah: Int) {
            AudioCache.file(ctx, reciterId, surah).delete()
            bump()
        }

        fun clearAll(ctx: Context) {
            AudioCache.clear(ctx)
            bump()
        }

        fun toggle(ctx: Context) = sendAction(ctx, intent(ctx, A_TOGGLE))
        fun next(ctx: Context) = sendAction(ctx, intent(ctx, A_NEXT))
        fun prev(ctx: Context) = sendAction(ctx, intent(ctx, A_PREV))
        fun stop(ctx: Context) = runCatching { ctx.startService(intent(ctx, A_STOP)) }
        fun seek(ctx: Context, ms: Int) = runCatching { ctx.startService(intent(ctx, A_SEEK).putExtra("ms", ms)) }
        fun seekBy(ctx: Context, deltaMs: Int) = runCatching { ctx.startService(intent(ctx, A_SEEK_BY).putExtra("d", deltaMs)) }
        fun cycleSpeed(ctx: Context) = runCatching { ctx.startService(intent(ctx, A_SPEED)) }
        fun cycleRepeat(ctx: Context) = runCatching { ctx.startService(intent(ctx, A_REPEAT)) }
        fun toggleAutoNext(ctx: Context) = runCatching { ctx.startService(intent(ctx, A_AUTONEXT)) }
        fun setSleep(ctx: Context, minutes: Int) = runCatching { ctx.startService(intent(ctx, A_SLEEP).putExtra("m", minutes)) }
        fun clearError() { _state.update { it.copy(error = null) } }
        private fun bump() { _state.update { it.copy(cacheVersion = it.cacheVersion + 1) } }
    }

    private var mp: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private val dl = mutableMapOf<String, Job>()
    private lateinit var audioManager: AudioManager

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause()
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        Notifs.ensureChannels(this)
        goForeground()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        goForeground()
        when (intent?.action) {
            A_PLAY -> playSurah(intent.getStringExtra("r") ?: _state.value.reciterId, intent.getIntExtra("s", 1), useBackup = false, resetRepeat = true)
            A_DOWNLOAD -> startDownload(intent.getStringExtra("r") ?: _state.value.reciterId, intent.getIntExtra("s", 1))
            A_TOGGLE -> toggle()
            A_STOP -> stopAll()
            A_NEXT -> { val s = _state.value; if (s.surah in 1..113) playSurah(s.reciterId, s.surah + 1, useBackup = false, resetRepeat = true) }
            A_PREV -> { val s = _state.value; if (s.surah > 1) playSurah(s.reciterId, s.surah - 1, useBackup = false, resetRepeat = true) }
            A_SEEK -> seekTo(intent.getIntExtra("ms", 0))
            A_SEEK_BY -> mp?.let { seekTo(runCatching { it.currentPosition }.getOrDefault(0) + intent.getIntExtra("d", 0)) }
            A_SPEED -> cycleSpeedInternal()
            A_REPEAT -> _state.update { it.copy(repeatMode = (it.repeatMode + 1) % 3, repeatCount = 0) }
            A_AUTONEXT -> _state.update { it.copy(autoNext = !it.autoNext) }
            A_SLEEP -> setSleep(intent.getIntExtra("m", 0))
        }
        if (mp == null && dl.isEmpty() && !_state.value.isLoading) {
            maybeStop()
        }
        return START_NOT_STICKY
    }

    private fun primaryUrl(r: Reciter, num: Int): String =
        "${r.url.trimEnd('/')}/${num.toString().padStart(3, '0')}.mp3"

    private fun backupUrl(num: Int): String =
        "https://server8.mp3quran.net/afs/${num.toString().padStart(3, '0')}.mp3"

    private fun playSurah(reciterId: String, num: Int, useBackup: Boolean, resetRepeat: Boolean) {
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: QuranData.reciters.first()
        releasePlayer()
        _state.update {
            it.copy(
                reciterId = reciter.id,
                surah = num,
                isLoading = true,
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
                repeatCount = if (resetRepeat) 0 else it.repeatCount,
                error = null
            )
        }
        goForeground()

        runCatching {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }

        val file = AudioCache.file(this, reciter.id, num)
        val p = MediaPlayer()
        mp = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            p.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)

            if (file.exists() && file.length() > 1000) {
                p.setDataSource(file.absolutePath)
            } else {
                val targetUrl = if (useBackup) backupUrl(num) else primaryUrl(reciter, num)
                p.setDataSource(targetUrl)
            }

            p.setOnPreparedListener { m ->
                applySpeed(m, _state.value.speed)
                m.start()
                _state.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = true,
                        durationMs = runCatching { m.duration }.getOrDefault(0).coerceAtLeast(0)
                    )
                }
                startTicker()
                goForeground()
            }
            p.setOnCompletionListener { onComplete() }
            p.setOnErrorListener { _, _, _ ->
                if (!useBackup && !(file.exists() && file.length() > 1000)) {
                    playSurah(reciter.id, num, useBackup = true, resetRepeat = false)
                } else {
                    fail("تعذر الاتصال بالسيرفر الصوتي، تأكد من وجود اتصال بالإنترنت")
                }
                true
            }
            p.prepareAsync()
        } catch (e: Exception) {
            if (!useBackup) {
                playSurah(reciter.id, num, useBackup = true, resetRepeat = false)
            } else {
                fail("حدث خطأ أثناء تهيئة المشغل الصوتي")
            }
        }
    }

    private fun applySpeed(player: MediaPlayer, speed: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                val wasPlaying = player.isPlaying
                player.playbackParams = PlaybackParams().setSpeed(speed)
                if (!wasPlaying) player.pause()
            }
        }
    }

    private fun cycleSpeedInternal() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 0.75f)
        val cur = _state.value.speed
        val nextSpeed = speeds[(speeds.indexOf(cur) + 1) % speeds.size]
        _state.update { it.copy(speed = nextSpeed) }
        mp?.let { applySpeed(it, nextSpeed) }
    }

    private fun fail(msg: String) {
        releasePlayer()
        _state.update { it.copy(isLoading = false, isPlaying = false, error = msg) }
        maybeStop()
    }

    private fun onComplete() {
        val s = _state.value
        when {
            s.repeatMode == 2 -> {
                mp?.seekTo(0)
                mp?.start()
            }
            s.repeatMode == 1 && s.repeatCount + 1 < 3 -> {
                _state.update { it.copy(repeatCount = s.repeatCount + 1) }
                mp?.seekTo(0)
                mp?.start()
            }
            s.autoNext && s.surah < 114 -> playSurah(s.reciterId, s.surah + 1, useBackup = false, resetRepeat = true)
            else -> {
                _state.update { it.copy(isPlaying = false, positionMs = 0, repeatCount = 0) }
                goForeground()
            }
        }
    }

    private fun toggle() {
        val s = _state.value
        val p = mp
        when {
            s.isLoading -> Unit
            p != null && s.isPlaying -> pause()
            p != null -> {
                runCatching {
                    @Suppress("DEPRECATION")
                    audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
                }
                runCatching {
                    applySpeed(p, s.speed)
                    p.start()
                }
                _state.update { it.copy(isPlaying = true) }
                startTicker()
                goForeground()
            }
            s.surah > 0 -> playSurah(s.reciterId, s.surah, useBackup = false, resetRepeat = false)
            else -> playSurah(s.reciterId, 1, useBackup = false, resetRepeat = true)
        }
    }

    private fun pause() {
        runCatching { mp?.pause() }
        _state.update { it.copy(isPlaying = false) }
        goForeground()
    }

    private fun seekTo(ms: Int) {
        val p = mp ?: return
        val dur = _state.value.durationMs.coerceAtLeast(1)
        val t = ms.coerceIn(0, dur)
        runCatching { p.seekTo(t) }
        _state.update { it.copy(positionMs = t) }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val p = mp
                if (p != null && _state.value.isPlaying) {
                    runCatching {
                        _state.update {
                            it.copy(
                                positionMs = p.currentPosition.coerceAtLeast(0),
                                durationMs = p.duration.coerceAtLeast(0)
                            )
                        }
                    }
                }
                delay(350)
            }
        }
    }

    private fun setSleep(minutes: Int) {
        sleepJob?.cancel()
        _state.update { it.copy(sleepLeft = minutes) }
        if (minutes <= 0) return
        sleepJob = scope.launch {
            var left = minutes
            while (left > 0) {
                delay(60_000L)
                left--
                _state.update { it.copy(sleepLeft = left) }
            }
            pause()
        }
    }

    private fun releasePlayer() {
        ticker?.cancel()
        runCatching {
            mp?.reset()
            mp?.release()
        }
        mp = null
    }

    private fun stopAll() {
        releasePlayer()
        sleepJob?.cancel()
        runCatching {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusListener)
        }
        _state.update {
            it.copy(surah = 0, isPlaying = false, isLoading = false, positionMs = 0, durationMs = 0, sleepLeft = 0, repeatCount = 0)
        }
        maybeStop()
    }

    private fun maybeStop() {
        if (mp == null && dl.isEmpty() && !_state.value.isLoading) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startDownload(reciterId: String, num: Int) {
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: return
        val key = "${reciterId}_$num"
        if (dl.containsKey(key) || AudioCache.has(this, reciterId, num)) return
        goForeground()
        _state.update { it.copy(downloads = it.downloads + (key to 0f), error = null) }
        dl[key] = scope.launch {
            val ok = withContext(Dispatchers.IO) { downloadFile(reciter, num, key) }
            dl.remove(key)
            _state.update {
                it.copy(
                    downloads = it.downloads - key,
                    cacheVersion = it.cacheVersion + 1,
                    error = if (ok) null else "تعذر تنزيل السورة، تحقق من الإنترنت"
                )
            }
            if (mp == null && dl.isEmpty()) maybeStop()
        }
    }

    private fun downloadFile(reciter: Reciter, num: Int, key: String): Boolean {
        val out = AudioCache.file(this, reciter.id, num)
        val part = File(out.path + ".part")
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(primaryUrl(reciter, num)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 25_000
                instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) false
            else {
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    FileOutputStream(part).use { output ->
                        val buf = ByteArray(32 * 1024)
                        var done = 0L
                        var last = -1
                        while (true) {
                            val read = input.read(buf)
                            if (read == -1) break
                            output.write(buf, 0, read)
                            done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != last) {
                                    last = pct
                                    _state.update { it.copy(downloads = it.downloads + (key to pct / 100f)) }
                                }
                            }
                        }
                    }
                }
                out.delete()
                part.renameTo(out)
            }
        } catch (e: Exception) {
            part.delete()
            false
        } finally {
            conn?.disconnect()
        }
    }

    private fun actionPi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(
            this,
            code,
            Intent(this, PlayerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(): Notification {
        val s = _state.value
        val surah = QuranData.surah(s.surah)
        val reciter = QuranData.reciters.firstOrNull { it.id == s.reciterId }?.name ?: "القرآن الكريم"
        val b = NotificationCompat.Builder(this, Notifs.CH_PLAYER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (surah != null) "سورة ${surah.name}" else "المكتبة الصوتية")
            .setContentText(if (surah != null) reciter else "جاهز للتشغيل")
            .setContentIntent(Notifs.contentIntent(this))
            .setOngoing(s.isPlaying || s.downloads.isNotEmpty())
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (surah != null) {
            b.addAction(
                if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (s.isPlaying) "إيقاف مؤقت" else "تشغيل",
                actionPi(A_TOGGLE, 1)
            )
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", actionPi(A_STOP, 2))
        }
        return b.build()
    }

    private fun goForeground() {
        val n = buildNotification()
        runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIF_ID, n)
            }
        }
    }

    override fun onDestroy() {
        releasePlayer()
        scope.cancel()
        super.onDestroy()
    }
}

// ============================================================
// 2. شاشة الاستماع والمكتبة الصوتية بتصميم احترافي موسع (AudioScreen)
// ============================================================
private fun fmtMs(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

@Composable
fun AudioScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) } // 0: الكل, 1: المحملة, 2: مكية, 3: مدنية
    var showSleep by remember { mutableStateOf(false) }
    var showRecitersDialog by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "disc")
    val discAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(14000, easing = LinearEasing)),
        label = "discSpin"
    )

    LaunchedEffect(ps.error) {
        ps.error?.let {
            Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
            PlayerService.clearError()
        }
    }

    val selectedReciter = QuranData.reciters.firstOrNull { it.id == st.reciterId } ?: QuranData.reciters.first()
    val playingSurah = QuranData.surah(ps.surah)
    val playingReciter = QuranData.reciters.firstOrNull { it.id == ps.reciterId } ?: selectedReciter

    val filteredSurahs = remember(query, filterTab, ps.cacheVersion, st.reciterId) {
        QuranData.search(query).filter { s ->
            when (filterTab) {
                1 -> AudioCache.has(ctx, st.reciterId, s.num)
                2 -> s.type == "مكية"
                3 -> s.type == "مدنية"
                else -> true
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("المكتبة الصوتية (${QuranData.reciters.size} قارئاً)", onBack = { nav.back() })

        // شريط اختيار القراء + زر فتح دليل الـ 30 قارئاً
        Row(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = { fx.click(); showRecitersDialog = true },
                shape = RoundedCornerShape(16.dp),
                color = GoldWarm,
                shadowElevation = 3.dp
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Groups, null, tint = RoyalForest, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("كل القراء", color = RoyalForest, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.width(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(QuranData.reciters, key = { it.id }) { r ->
                    val isSelected = r.id == st.reciterId
                    Surface(
                        onClick = {
                            fx.click()
                            st.reciterId = r.id
                            if (ps.surah > 0 && ps.isPlaying) {
                                PlayerService.play(ctx, r.id, ps.surah)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) EmeraldVibrant else p.card,
                        border = BorderStroke(1.2.dp, if (isSelected) GoldShine else p.border)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                null,
                                tint = if (isSelected) GoldShine else TurquoiseAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                r.name,
                                color = if (isSelected) Color.White else p.ink,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // ============================================================
        // بطاقة المشغل الاحترافي (Royal Studio Player)
        // ============================================================
        Surface(
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.6.dp, GoldWarm),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        ) {
            Box(
                Modifier.background(
                    Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant, TealStart))
                )
            ) {
                IslamicPattern(Modifier.matchParentSize())

                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .rotate(if (ps.isPlaying) discAngle else 0f)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Brush.radialGradient(listOf(GoldShine, GoldWarm, RoyalForest)))
                                .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).background(RoyalForest),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, null, tint = GoldShine, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Text(
                                text = playingSurah?.let { "سورة ${it.name}" } ?: "سورة الفاتحة (اضغط تشغيل)",
                                color = Color.White,
                                fontFamily = FontFamily.Serif,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (playingSurah != null) "${playingReciter.name} • ${playingReciter.style}" else "${selectedReciter.name} • ${selectedReciter.style}",
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (playingSurah != null) {
                                Text(
                                    text = "${playingSurah.type} • ${playingSurah.ayahs} آية • صفحة ${playingSurah.page}",
                                    color = GoldSoftBg.copy(alpha = 0.8f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (playingSurah != null) {
                            IconButton(
                                onClick = { fx.click(); nav.openPage(playingSurah.page) },
                                modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                            ) {
                                Icon(Icons.Default.MenuBook, "اقرأ السورة", tint = GoldShine)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(Modifier.fillMaxWidth()) {
                            val frac = drag ?: if (ps.durationMs > 0) ps.positionMs / ps.durationMs.toFloat() else 0f
                            Slider(
                                value = frac.coerceIn(0f, 1f),
                                onValueChange = { drag = it },
                                onValueChangeFinished = {
                                    drag?.let { PlayerService.seek(ctx, (it * ps.durationMs).toInt()) }
                                    drag = null
                                },
                                enabled = ps.surah > 0 && ps.durationMs > 0 && !ps.isLoading,
                                colors = SliderDefaults.colors(
                                    thumbColor = GoldShine,
                                    activeTrackColor = GoldShine,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                                )
                            )
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    fmtMs(if (drag != null) (drag!! * ps.durationMs).toInt() else ps.positionMs),
                                    color = GoldSoftBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(
                                    if (ps.isLoading) "جارٍ الاتصال بالسيرفر..." else fmtMs(ps.durationMs),
                                    color = GoldShine,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { fx.click(); PlayerService.prev(ctx) },
                                    enabled = ps.surah > 1
                                ) {
                                    Icon(Icons.Default.SkipPrevious, "السابقة", tint = Color.White, modifier = Modifier.size(28.dp))
                                }

                                IconButton(
                                    onClick = { fx.click(); PlayerService.seekBy(ctx, -10_000) },
                                    enabled = ps.surah > 0
                                ) {
                                    Icon(Icons.Default.Replay10, "رجوع 10ث", tint = GoldSoftBg, modifier = Modifier.size(26.dp))
                                }

                                Surface(
                                    onClick = {
                                        fx.click()
                                        if (ps.surah == 0) PlayerService.play(ctx, st.reciterId, 1)
                                        else PlayerService.toggle(ctx)
                                    },
                                    shape = CircleShape,
                                    color = GoldShine,
                                    border = BorderStroke(2.dp, Color.White),
                                    shadowElevation = 8.dp,
                                    modifier = Modifier.size(60.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (ps.isLoading) {
                                            CircularProgressIndicator(Modifier.size(28.dp), color = RoyalForest, strokeWidth = 3.dp)
                                        } else {
                                            Icon(
                                                if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                "تشغيل/إيقاف",
                                                tint = RoyalForest,
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { fx.click(); PlayerService.seekBy(ctx, 10_000) },
                                    enabled = ps.surah > 0
                                ) {
                                    Icon(Icons.Default.Forward10, "تقديم 10ث", tint = GoldSoftBg, modifier = Modifier.size(26.dp))
                                }

                                IconButton(
                                    onClick = { fx.click(); PlayerService.next(ctx) },
                                    enabled = ps.surah in 1..113
                                ) {
                                    Icon(Icons.Default.SkipNext, "التالية", tint = Color.White, modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(Modifier.height(6.dp))

                    // شريط الأدوات الموسعة: السرعة + التكرار الذكي + التتالي + مؤقت النوم
                    val repeatLabel = when (ps.repeatMode) {
                        1 -> "تحفيظ (${ps.repeatCount + 1}/3)"
                        2 -> "تكرار ∞"
                        else -> "تكرار"
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StudioToolChip("${ps.speed}x", Icons.Default.Speed, ps.speed != 1.0f) { PlayerService.cycleSpeed(ctx) }
                        StudioToolChip(repeatLabel, Icons.Default.RepeatOne, ps.repeatMode > 0) { PlayerService.cycleRepeat(ctx) }
                        StudioToolChip("متتالية", Icons.Default.PlaylistPlay, ps.autoNext) { PlayerService.toggleAutoNext(ctx) }
                        StudioToolChip(if (ps.sleepLeft > 0) "${ps.sleepLeft} د" else "نوم", Icons.Default.Bedtime, ps.sleepLeft > 0) { showSleep = true }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") }
                }
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("الكل (114)", "المحملة (بدون نت)", "مكية", "مدنية")
            items(tabs.size) { idx ->
                FilterChip(
                    selected = filterTab == idx,
                    onClick = { fx.click(); filterTab = idx },
                    label = { Text(tabs[idx], fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldWarm,
                        selectedLabelColor = RoyalForest
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (filteredSurahs.isEmpty()) {
                item { EmptyState(Icons.Default.MusicOff, "لا توجد سور مطابقة في هذا القسم") }
            }
            items(filteredSurahs, key = { it.num }) { s ->
                val cached = remember(ps.cacheVersion, st.reciterId, s.num) { AudioCache.has(ctx, st.reciterId, s.num) }
                val progress = ps.downloads["${st.reciterId}_${s.num}"]
                val isCurrent = ps.surah == s.num && ps.reciterId == st.reciterId

                QCard(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isCurrent) EmeraldVibrant.copy(alpha = 0.12f) else null,
                    borderColor = if (isCurrent) GoldWarm else null,
                    onClick = {
                        if (isCurrent) PlayerService.toggle(ctx)
                        else PlayerService.play(ctx, st.reciterId, s.num)
                    }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberBadge(s.num, 34)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "سورة ${s.name}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (isCurrent) TurquoiseAccent else p.ink
                            )
                            Text(
                                when {
                                    progress != null -> "جارٍ التنزيل ${(progress * 100).toInt()}%"
                                    cached -> "محملة • تعمل بدون إنترنت"
                                    else -> "${s.type} • ${s.ayahs} آية"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (cached) TurquoiseAccent else p.muted
                            )
                        }

                        when {
                            progress != null -> CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.size(24.dp),
                                color = EmeraldVibrant,
                                strokeWidth = 3.dp
                            )
                            cached -> IconButton(
                                onClick = { fx.click(); PlayerService.delete(ctx, st.reciterId, s.num) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, "حذف", tint = Color(0xFFB71C1C), modifier = Modifier.size(21.dp))
                            }
                            else -> IconButton(
                                onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DownloadForOffline, "تنزيل", tint = GoldWarm, modifier = Modifier.size(22.dp))
                            }
                        }

                        IconButton(
                            onClick = {
                                fx.click()
                                if (isCurrent) PlayerService.toggle(ctx)
                                else PlayerService.play(ctx, st.reciterId, s.num)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                if (isCurrent && ps.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                "تشغيل",
                                tint = if (isCurrent) GoldWarm else EmeraldVibrant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // نافذة دليل الـ 30 قارئاً مع البحث الفوري
    // ============================================================
    if (showRecitersDialog) {
        var reciterSearch by remember { mutableStateOf("") }
        val matchingReciters = remember(reciterSearch) {
            val q = reciterSearch.normalizeAr()
            if (q.isEmpty()) QuranData.reciters
            else QuranData.reciters.filter { it.name.normalizeAr().contains(q) || it.style.normalizeAr().contains(q) }
        }

        AlertDialog(
            onDismissRequest = { showRecitersDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = p.card,
            title = {
                Column {
                    Text("اختر القارئ (${QuranData.reciters.size} قارئاً)", fontWeight = FontWeight.ExtraBold, color = p.ink)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reciterSearch,
                        onValueChange = { reciterSearch = it },
                        singleLine = true,
                        placeholder = { Text("ابحث باسم الشيخ...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TurquoiseAccent) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(matchingReciters, key = { it.id }) { r ->
                        val active = r.id == st.reciterId
                        Surface(
                            onClick = {
                                fx.click()
                                st.reciterId = r.id
                                showRecitersDialog = false
                                if (ps.surah > 0) PlayerService.play(ctx, r.id, ps.surah)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) EmeraldVibrant else p.bg,
                            border = BorderStroke(1.dp, if (active) GoldShine else p.border)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.RecordVoiceOver,
                                    null,
                                    tint = if (active) GoldShine else TurquoiseAccent
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.name, fontWeight = FontWeight.ExtraBold, color = if (active) Color.White else p.ink, fontSize = 14.sp)
                                    Text(r.style, fontSize = 11.sp, color = if (active) GoldSoftBg else p.muted, fontWeight = FontWeight.Bold)
                                }
                                if (active) Icon(Icons.Default.CheckCircle, null, tint = GoldShine)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRecitersDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent)
                }
            }
        )
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("مؤقت إيقاف التلاوة تلقائياً", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إلغاء المؤقت", 15 to "بعد 15 دقيقة", 30 to "بعد 30 دقيقة", 45 to "بعد 45 دقيقة", 60 to "بعد ساعة").forEach { (m, label) ->
                        TextButton(
                            onClick = { PlayerService.setSleep(ctx, m); showSleep = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun StudioToolChip(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(
        onClick = { fx.click(); onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (active) GoldShine.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, if (active) GoldShine else Color.White.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (active) GoldShine else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (active) GoldShine else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

// ============================================================
// 3. مواقيت الصلاة واتجاه القبلة (PrayerScreen)
// ============================================================
@Composable
fun PrayerScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val geo = st.geo
    val method = st.prayerMethod
    val tz = remember(geo) { PrayerCalc.tzFor(geo) }
    val times = remember(geo, method) { PrayerCalc.millis(geo, method, 0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val next = remember(geo, method, now / 60_000) { PrayerCalc.nextPrayer(geo, method, now) }
    var showCities by remember { mutableStateOf(false) }
    val qibla = remember(geo) { PrayerCalc.qibla(geo.lat, geo.lng) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val useGps = {
        val l = LocationHelper.lastKnown(ctx)
        if (l != null) st.setLocation("موقعي الحالي", l.latitude, l.longitude)
        else Toast.makeText(ctx, "تعذر تحديد موقعك، فعّل الموقع أو اختر مدينتك يدوياً", Toast.LENGTH_LONG).show()
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useGps() else Toast.makeText(ctx, "لم يُمنح إذن الموقع، اختر مدينتك يدوياً", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("مواقيت الصلاة والقبلة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, modifier = Modifier.fillMaxWidth()) {
                    Box {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الصلاة القادمة", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(PrayerCalc.names[next.first], color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text(PrayerCalc.fmt(next.second, tz), color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(if (next.second > 0) "بعد ${PrayerCalc.countdown(next.second - now)}" else "--:--:--", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = GoldWarm)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(geo.name, fontWeight = FontWeight.ExtraBold, color = p.ink)
                                if (geo.isDefault) Text("اختر مدينتك أو حدّد موقعك لدقة أعلى", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    fx.click()
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useGps()
                                    else locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant), modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp), tint = GoldShine)
                                Spacer(Modifier.width(4.dp))
                                Text("موقعي", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { fx.click(); showCities = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("اختيار مدينة", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
            val nextIdx = PrayerCalc.prayerIdx[next.first]
            items(6) { idx ->
                val isNext = idx == nextIdx
                QCard(shape = RoundedCornerShape(18.dp), color = if (isNext) GoldShine.copy(alpha = 0.18f) else null, borderColor = if (isNext) GoldWarm else null) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(labels[idx], fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (idx == 1) p.muted else p.ink)
                        Text(PrayerCalc.fmt(times[idx], tz), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isNext) TurquoiseAccent else EmeraldVibrant)
                    }
                }
            }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, "القبلة", tint = GoldWarm, modifier = Modifier.size(40.dp).rotate(qibla.toFloat()))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("اتجاه القبلة", fontWeight = FontWeight.ExtraBold, color = p.ink)
                            Text("${qibla.toInt()}° من الشمال باتجاه عقارب الساعة", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { SectionTitle("طريقة الحساب") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PrayerCalc.methodNames.size) { i ->
                        FilterChip(selected = method == i, onClick = { fx.click(); st.prayerMethod = i; Reminders.rescheduleAll(ctx) }, label = { Text(PrayerCalc.methodNames[i], fontWeight = FontWeight.Bold, fontSize = 12.sp) }, shape = RoundedCornerShape(14.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg))
                    }
                }
            }
            item {
                SettingToggle("تنبيه عند دخول وقت كل صلاة", st.prayerAlert) { on ->
                    st.prayerAlert = on
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Reminders.rescheduleAll(ctx)
                }
            }
        }
    }

    if (showCities) {
        AlertDialog(
            onDismissRequest = { showCities = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("اختر مدينتك", fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(QuranData.cities, key = { it.name }) { c ->
                        TextButton(onClick = { fx.click(); st.setLocation(c.name, c.lat, c.lng); showCities = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(c.name, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCities = false }) { Text("إغلاق") } }
        )
    }
}
