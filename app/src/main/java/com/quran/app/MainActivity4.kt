@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
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
// 1. محرك الصوت والتنزيل المرن لجميع الشبكات (QuranAudioEngine & PlayerService)
// ============================================================
data class PlayerState(
    val reciterId: String = "afs",
    val surah: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val speed: Float = 1.0f,
    val repeatMode: Int = 0,
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
    fun has(ctx: Context, reciterId: String, num: Int): Boolean =
        file(ctx, reciterId, num).let { it.exists() && it.length() > 2048 }
    fun count(ctx: Context): Int = dir(ctx).listFiles()?.count { it.name.endsWith(".mp3") && it.length() > 2048 } ?: 0
    fun sizeMb(ctx: Context): Long = (dir(ctx).listFiles()?.sumOf { it.length() } ?: 0L) / (1024 * 1024)
    fun clear(ctx: Context) { dir(ctx).listFiles()?.forEach { it.delete() } }
}

object QuranAudioEngine {
    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mp: MediaPlayer? = null
    private var ticker: Job? = null
    private var sleepJob: Job? = null
    private val activeDownloads = mutableMapOf<String, Job>()

    private fun primaryUrl(r: Reciter, num: Int): String =
        "${r.url.trimEnd('/')}/${num.toString().padStart(3, '0')}.mp3"

    private fun backupUrl(num: Int): String =
        "https://server8.mp3quran.net/afs/${num.toString().padStart(3, '0')}.mp3"

    fun play(ctx: Context, reciterId: String, num: Int, attemptStep: Int = 0, resetRepeat: Boolean = true) {
        val appCtx = ctx.applicationContext
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: QuranData.reciters.first()

        AdhanEngine.stopAdhan(appCtx)
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
        startKeepAliveService(appCtx)

        val localFile = AudioCache.file(appCtx, reciter.id, num)
        val player = MediaPlayer()
        mp = player

        val streamCandidates = listOf(
            primaryUrl(reciter, num),
            primaryUrl(reciter, num).replace("https://", "http://"),
            backupUrl(num),
            backupUrl(num).replace("https://", "http://")
        ).distinct()

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK)

            if (localFile.exists() && localFile.length() > 2048) {
                player.setDataSource(localFile.absolutePath)
            } else {
                val targetUrl = streamCandidates[attemptStep.coerceIn(0, streamCandidates.lastIndex)]
                player.setDataSource(targetUrl)
            }

            player.setOnPreparedListener { m ->
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
                updateNotification(appCtx)
            }

            player.setOnCompletionListener { onComplete(appCtx) }

            player.setOnErrorListener { _, _, _ ->
                if (attemptStep + 1 < streamCandidates.size && !(localFile.exists() && localFile.length() > 2048)) {
                    play(appCtx, reciter.id, num, attemptStep = attemptStep + 1, resetRepeat = false)
                } else {
                    releasePlayer()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isPlaying = false,
                            error = "تعذر تشغيل التلاوة على هذه الشبكة، تحقق من الاتصال"
                        )
                    }
                    stopKeepAliveService(appCtx)
                }
                true
            }

            player.prepareAsync()
        } catch (_: Exception) {
            if (attemptStep + 1 < streamCandidates.size) {
                play(appCtx, reciter.id, num, attemptStep = attemptStep + 1, resetRepeat = false)
            } else {
                releasePlayer()
                _state.update {
                    it.copy(isLoading = false, isPlaying = false, error = "حدث خطأ في الاتصال بالخادم الصوتي")
                }
                stopKeepAliveService(appCtx)
            }
        }
    }

    private fun onComplete(ctx: Context) {
        val s = _state.value
        when {
            s.repeatMode == 2 -> { mp?.seekTo(0); mp?.start() }
            s.repeatMode == 1 && s.repeatCount + 1 < 3 -> {
                _state.update { it.copy(repeatCount = s.repeatCount + 1) }
                mp?.seekTo(0); mp?.start()
            }
            s.autoNext && s.surah in 1..113 -> play(ctx, s.reciterId, s.surah + 1, attemptStep = 0, resetRepeat = true)
            else -> {
                _state.update { it.copy(isPlaying = false, positionMs = 0, repeatCount = 0) }
                updateNotification(ctx)
            }
        }
    }

    fun toggle(ctx: Context) {
        val appCtx = ctx.applicationContext
        val s = _state.value
        val p = mp
        when {
            s.isLoading -> Unit
            p != null && s.isPlaying -> {
                runCatching { p.pause() }
                _state.update { it.copy(isPlaying = false) }
                updateNotification(appCtx)
            }
            p != null -> {
                runCatching { applySpeed(p, s.speed); p.start() }
                _state.update { it.copy(isPlaying = true) }
                startTicker()
                updateNotification(appCtx)
            }
            s.surah > 0 -> play(appCtx, s.reciterId, s.surah, attemptStep = 0, resetRepeat = false)
            else -> play(appCtx, s.reciterId, 1, attemptStep = 0, resetRepeat = true)
        }
    }

    fun pauseIfPlaying(ctx: Context) {
        if (_state.value.isPlaying) {
            runCatching { mp?.pause() }
            _state.update { it.copy(isPlaying = false) }
            updateNotification(ctx.applicationContext)
        }
    }

    fun next(ctx: Context) {
        val s = _state.value
        if (s.surah in 1..113) play(ctx, s.reciterId, s.surah + 1)
    }

    fun prev(ctx: Context) {
        val s = _state.value
        if (s.surah > 1) play(ctx, s.reciterId, s.surah - 1)
    }

    fun seek(ms: Int) {
        val p = mp ?: return
        val dur = _state.value.durationMs.coerceAtLeast(1)
        val target = ms.coerceIn(0, dur)
        runCatching { p.seekTo(target) }
        _state.update { it.copy(positionMs = target) }
    }

    fun seekBy(deltaMs: Int) {
        val cur = runCatching { mp?.currentPosition }.getOrNull() ?: _state.value.positionMs
        seek(cur + deltaMs)
    }

    fun cycleSpeed() {
        val speeds = listOf(1.0f, 1.25f, 1.5f, 0.75f)
        val next = speeds[(speeds.indexOf(_state.value.speed) + 1) % speeds.size]
        _state.update { it.copy(speed = next) }
        mp?.let { applySpeed(it, next) }
    }

    fun cycleRepeat() {
        _state.update { it.copy(repeatMode = (it.repeatMode + 1) % 3, repeatCount = 0) }
    }

    fun toggleAutoNext() {
        _state.update { it.copy(autoNext = !it.autoNext) }
    }

    fun setSleep(ctx: Context, minutes: Int) {
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
            if (_state.value.isPlaying) toggle(ctx)
        }
    }

    fun stop(ctx: Context) {
        val appCtx = ctx.applicationContext
        releasePlayer()
        sleepJob?.cancel()
        _state.update {
            it.copy(surah = 0, isPlaying = false, isLoading = false, positionMs = 0, durationMs = 0, sleepLeft = 0, repeatCount = 0)
        }
        stopKeepAliveService(appCtx)
    }

    fun download(ctx: Context, reciterId: String, num: Int) {
        val appCtx = ctx.applicationContext
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: return
        val key = "${reciter.id}_$num"
        if (activeDownloads.containsKey(key) || AudioCache.has(appCtx, reciter.id, num)) return

        _state.update { it.copy(downloads = it.downloads + (key to 0.01f), error = null) }
        Toast.makeText(appCtx, "بدأ تنزيل سورة ${QuranData.surah(num)?.name ?: ""}...", Toast.LENGTH_SHORT).show()

        activeDownloads[key] = scope.launch {
            val urlsToTry = listOf(
                primaryUrl(reciter, num),
                primaryUrl(reciter, num).replace("https://", "http://"),
                backupUrl(num),
                backupUrl(num).replace("https://", "http://")
            ).distinct()

            var success = false
            for (candidateUrl in urlsToTry) {
                if (withContext(Dispatchers.IO) { downloadWithResume(appCtx, candidateUrl, AudioCache.file(appCtx, reciter.id, num), key) }) {
                    success = true
                    break
                }
            }

            activeDownloads.remove(key)
            _state.update {
                it.copy(
                    downloads = it.downloads - key,
                    cacheVersion = it.cacheVersion + 1,
                    error = if (success) null else "تعذر التنزيل على هذه الشبكة، حاول مجدداً"
                )
            }
            if (success) {
                Toast.makeText(appCtx, "تم حفظ السورة للاستماع بدون إنترنت ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }

    internal fun downloadWithResume(ctx: Context, initialUrl: String, outFile: File, key: String? = null): Boolean {
        val tempFile = File(outFile.path + ".tmp")

        repeat(3) { attempt ->
            var currentUrl = initialUrl
            var redirects = 0
            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            while (redirects < 6) {
                var conn: HttpURLConnection? = null
                try {
                    conn = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = false
                        connectTimeout = 25_000
                        readTimeout = 40_000
                        setRequestProperty("User-Agent", "Dalvik/2.1.0 (Linux; U; Android)")
                        setRequestProperty("Accept", "*/*")
                        setRequestProperty("Connection", "keep-alive")
                        if (existingBytes > 0) setRequestProperty("Range", "bytes=$existingBytes-")
                    }

                    val code = conn.responseCode
                    if (code in 300..399) {
                        val location = conn.getHeaderField("Location") ?: break
                        currentUrl = URL(URL(currentUrl), location).toString()
                        redirects++
                        conn.disconnect()
                        continue
                    }

                    if (code != 200 && code != 206) {
                        conn.disconnect()
                        break
                    }

                    val appendMode = (code == 206 && existingBytes > 0)
                    if (!appendMode && tempFile.exists()) tempFile.delete()

                    val contentLen = conn.contentLengthLong
                    val totalBytes = if (contentLen > 0) contentLen + (if (appendMode) existingBytes else 0L) else -1L

                    conn.inputStream.use { input ->
                        FileOutputStream(tempFile, appendMode).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var downloaded = if (appendMode) existingBytes else 0L
                            var lastPct = -1
                            while (true) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                downloaded += read
                                if (key != null) {
                                    if (totalBytes > 0) {
                                        val pct = ((downloaded * 100) / totalBytes).toInt().coerceIn(1, 99)
                                        if (pct != lastPct) {
                                            lastPct = pct
                                            _state.update { it.copy(downloads = it.downloads + (key to pct / 100f)) }
                                        }
                                    } else {
                                        _state.update { it.copy(downloads = it.downloads + (key to 0.5f)) }
                                    }
                                }
                            }
                        }
                    }
                    conn.disconnect()

                    if (tempFile.exists() && tempFile.length() > 2048) {
                        outFile.delete()
                        return tempFile.renameTo(outFile)
                    }
                    break
                } catch (_: Exception) {
                    conn?.disconnect()
                    Thread.sleep(1000L * (attempt + 1))
                    break
                }
            }
        }
        return false
    }

    fun delete(ctx: Context, reciterId: String, num: Int) {
        AudioCache.file(ctx, reciterId, num).delete()
        _state.update { it.copy(cacheVersion = it.cacheVersion + 1) }
    }

    fun clearAll(ctx: Context) {
        AudioCache.clear(ctx)
        _state.update { it.copy(cacheVersion = it.cacheVersion + 1) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
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

    private fun releasePlayer() {
        ticker?.cancel()
        runCatching { mp?.reset(); mp?.release() }
        mp = null
    }

    private fun startKeepAliveService(ctx: Context) {
        runCatching { ctx.startService(Intent(ctx, PlayerService::class.java)) }
        updateNotification(ctx)
    }

    private fun stopKeepAliveService(ctx: Context) {
        runCatching { ctx.stopService(Intent(ctx, PlayerService::class.java)) }
        NotificationManagerCompat.from(ctx).cancel(77)
    }

    fun updateNotification(ctx: Context) {
        val s = _state.value
        if (s.surah == 0) return
        Notifs.ensureChannels(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val surah = QuranData.surah(s.surah) ?: return
        val reciter = QuranData.reciters.firstOrNull { it.id == s.reciterId }?.name ?: ""

        val togglePi = PendingIntent.getService(
            ctx, 1, Intent(ctx, PlayerService::class.java).setAction("TOGGLE"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopPi = PendingIntent.getService(
            ctx, 2, Intent(ctx, PlayerService::class.java).setAction("STOP"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, Notifs.CH_PLAYER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("سورة ${surah.name}")
            .setContentText(reciter)
            .setContentIntent(Notifs.contentIntent(ctx))
            .setOngoing(s.isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(
                if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (s.isPlaying) "إيقاف مؤقت" else "تشغيل",
                togglePi
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", stopPi)
            .build()
        try { nm.notify(77, notif) } catch (_: SecurityException) { }
    }
}

class PlayerService : Service() {
    companion object {
        val state: StateFlow<PlayerState> = QuranAudioEngine.state
        fun play(ctx: Context, reciterId: String, surah: Int) = QuranAudioEngine.play(ctx, reciterId, surah)
        fun download(ctx: Context, reciterId: String, surah: Int) = QuranAudioEngine.download(ctx, reciterId, surah)
        fun delete(ctx: Context, reciterId: String, surah: Int) = QuranAudioEngine.delete(ctx, reciterId, surah)
        fun clearAll(ctx: Context) = QuranAudioEngine.clearAll(ctx)
        fun toggle(ctx: Context) = QuranAudioEngine.toggle(ctx)
        fun next(ctx: Context) = QuranAudioEngine.next(ctx)
        fun prev(ctx: Context) = QuranAudioEngine.prev(ctx)
        fun stop(ctx: Context) = QuranAudioEngine.stop(ctx)
        fun seek(ctx: Context, ms: Int) = QuranAudioEngine.seek(ms)
        fun seekBy(ctx: Context, deltaMs: Int) = QuranAudioEngine.seekBy(deltaMs)
        fun cycleSpeed(ctx: Context) = QuranAudioEngine.cycleSpeed()
        fun cycleRepeat(ctx: Context) = QuranAudioEngine.cycleRepeat()
        fun toggleAutoNext(ctx: Context) = QuranAudioEngine.toggleAutoNext()
        fun setSleep(ctx: Context, minutes: Int) = QuranAudioEngine.setSleep(ctx, minutes)
        fun clearError() = QuranAudioEngine.clearError()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "TOGGLE" -> QuranAudioEngine.toggle(this)
            "STOP" -> { QuranAudioEngine.stop(this); stopSelf() }
            "STOP_ADHAN" -> AdhanEngine.stopAdhan(this)
        }
        return START_NOT_STICKY
    }
}

// ============================================================
// 2. شاشة الاستماع: القائمة الكاملة بالأعلى + المشغل السفلي المتحرك
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

    val listState = rememberLazyListState()
    var query by remember { mutableStateOf("") }
    var filterTab by remember { mutableIntStateOf(0) }
    var showSleep by remember { mutableStateOf(false) }
    var showRecitersDialog by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }
    var manualExpanded by remember { mutableStateOf<Boolean?>(null) }

    val isScrollingDown by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 1 || (listState.firstVisibleItemIndex == 1 && listState.firstVisibleItemScrollOffset > 80)
        }
    }

    LaunchedEffect(isScrollingDown) {
        if (!isScrollingDown) manualExpanded = null
    }

    val isPlayerExpanded = manualExpanded ?: !isScrollingDown

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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("المكتبة الصوتية (${QuranData.reciters.size} قارئاً)", onBack = { nav.back() })

            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = { fx.click(); showRecitersDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = GoldWarm,
                    shadowElevation = 3.dp
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Groups, null, tint = RoyalForest, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("كل القراء", color = RoyalForest, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }

                Spacer(Modifier.width(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    items(QuranData.reciters, key = { it.id }) { r ->
                        val isSelected = r.id == st.reciterId
                        Surface(
                            onClick = {
                                fx.click()
                                st.reciterId = r.id
                                if (ps.surah > 0 && ps.isPlaying) PlayerService.play(ctx, r.id, ps.surah)
                            },
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) EmeraldVibrant else p.card,
                            border = BorderStroke(1.2.dp, if (isSelected) GoldShine else p.border)
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, null, tint = if (isSelected) GoldShine else TurquoiseAccent, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(r.name, color = if (isSelected) Color.White else p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") }
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf("الكل (114)", "المحملة (بدون نت)", "مكية", "مدنية")
                items(tabs.size) { idx ->
                    FilterChip(
                        selected = filterTab == idx,
                        onClick = { fx.click(); filterTab = idx },
                        label = { Text(tabs[idx], fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldWarm, selectedLabelColor = RoyalForest)
                    )
                }
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 4.dp,
                    bottom = if (isPlayerExpanded) 250.dp else 90.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f).fillMaxWidth()
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
                        color = if (isCurrent) EmeraldVibrant.copy(alpha = 0.14f) else null,
                        borderColor = if (isCurrent) GoldWarm else null,
                        onClick = {
                            if (isCurrent) PlayerService.toggle(ctx)
                            else PlayerService.play(ctx, st.reciterId, s.num)
                        }
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(s.num, 36)
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
                                        cached -> "محملة • تعمل بدون إنترنت ✓"
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
                                    modifier = Modifier.size(26.dp),
                                    color = EmeraldVibrant,
                                    strokeWidth = 3.dp
                                )
                                cached -> IconButton(
                                    onClick = { fx.click(); PlayerService.delete(ctx, st.reciterId, s.num) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, "حذف", tint = Color(0xFFB71C1C), modifier = Modifier.size(22.dp))
                                }
                                else -> IconButton(
                                    onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(Icons.Default.DownloadForOffline, "تنزيل", tint = GoldWarm, modifier = Modifier.size(24.dp))
                                }
                            }

                            IconButton(
                                onClick = {
                                    fx.click()
                                    if (isCurrent) PlayerService.toggle(ctx)
                                    else PlayerService.play(ctx, st.reciterId, s.num)
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    if (isCurrent && ps.isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                    "تشغيل",
                                    tint = if (isCurrent) GoldWarm else EmeraldVibrant,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            border = BorderStroke(1.6.dp, GoldWarm),
            shadowElevation = 14.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow))
        ) {
            Box(Modifier.background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
                IslamicPattern(Modifier.matchParentSize())

                if (!isPlayerExpanded) {
                    Column(Modifier.fillMaxWidth().clickable { fx.click(); manualExpanded = true }) {
                        LinearProgressIndicator(
                            progress = { if (ps.durationMs > 0) (ps.positionMs / ps.durationMs.toFloat()).coerceIn(0f, 1f) else 0f },
                            color = GoldShine,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.fillMaxWidth().height(3.dp)
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .rotate(if (ps.isPlaying) discAngle else 0f)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldShine, GoldWarm)))
                                    .border(1.5.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GraphicEq, null, tint = RoyalForest, modifier = Modifier.size(20.dp))
                            }

                            Spacer(Modifier.width(10.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    playingSurah?.let { "سورة ${it.name}" } ?: "اختر سورة للتلاوة",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    if (playingSurah != null) playingReciter.name else selectedReciter.name,
                                    color = GoldShine,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }

                            IconButton(onClick = { fx.click(); manualExpanded = true }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Default.KeyboardArrowUp, "توسيع المشغل", tint = GoldSoftBg)
                            }

                            Surface(
                                onClick = {
                                    fx.click()
                                    if (ps.surah == 0) PlayerService.play(ctx, st.reciterId, 1)
                                    else PlayerService.toggle(ctx)
                                },
                                shape = CircleShape,
                                color = GoldShine,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (ps.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = RoyalForest, strokeWidth = 2.5.dp)
                                    else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل", tint = RoyalForest, modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier.fillMaxWidth().clickable { fx.click(); manualExpanded = false }.padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(42.dp).height(4.dp).clip(CircleShape).background(GoldShine.copy(alpha = 0.6f)))
                        }

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .rotate(if (ps.isPlaying) discAngle else 0f)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Brush.radialGradient(listOf(GoldShine, GoldWarm, RoyalForest)))
                                    .border(1.8.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(Modifier.size(18.dp).clip(CircleShape).background(RoyalForest), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.GraphicEq, null, tint = GoldShine, modifier = Modifier.size(12.dp))
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = playingSurah?.let { "سورة ${it.name}" } ?: "سورة الفاتحة (اضغط تشغيل)",
                                    color = Color.White,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (playingSurah != null) "${playingReciter.name} • ${playingReciter.style}" else "${selectedReciter.name} • ${selectedReciter.style}",
                                    color = GoldShine,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (playingSurah != null) {
                                IconButton(
                                    onClick = { fx.click(); nav.openPage(playingSurah.page) },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Icon(Icons.Default.MenuBook, "اقرأ السورة", tint = GoldShine, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

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
                                    ),
                                    modifier = Modifier.height(28.dp)
                                )
                                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(fmtMs(if (drag != null) (drag!! * ps.durationMs).toInt() else ps.positionMs), color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text(if (ps.isLoading) "جارٍ تحميل البث..." else fmtMs(ps.durationMs), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Row(
                                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { fx.click(); PlayerService.prev(ctx) }, enabled = ps.surah > 1, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.SkipPrevious, "السابقة", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                    IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, -10_000) }, enabled = ps.surah > 0, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Replay10, "رجوع 10ث", tint = GoldSoftBg, modifier = Modifier.size(24.dp))
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
                                        shadowElevation = 6.dp,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (ps.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = RoyalForest, strokeWidth = 3.dp)
                                            else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل", tint = RoyalForest, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                    IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, 10_000) }, enabled = ps.surah > 0, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.Forward10, "تقديم 10ث", tint = GoldSoftBg, modifier = Modifier.size(24.dp))
                                    }
                                    IconButton(onClick = { fx.click(); PlayerService.next(ctx) }, enabled = ps.surah in 1..113, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Default.SkipNext, "التالية", tint = Color.White, modifier = Modifier.size(26.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(4.dp))
                        val repeatLabel = when (ps.repeatMode) {
                            1 -> "تحفيظ (${ps.repeatCount + 1}/3)"
                            2 -> "تكرار ∞"
                            else -> "تكرار"
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            StudioToolChip("${ps.speed}x", Icons.Default.Speed, ps.speed != 1.0f) { PlayerService.cycleSpeed(ctx) }
                            StudioToolChip(repeatLabel, Icons.Default.RepeatOne, ps.repeatMode > 0) { PlayerService.cycleRepeat(ctx) }
                            StudioToolChip("متتالية", Icons.Default.PlaylistPlay, ps.autoNext) { PlayerService.toggleAutoNext(ctx) }
                            StudioToolChip(if (ps.sleepLeft > 0) "${ps.sleepLeft} د" else "نوم", Icons.Default.Bedtime, ps.sleepLeft > 0) { showSleep = true }
                        }
                    }
                }
            }
        }
    }

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
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, null, tint = if (active) GoldShine else TurquoiseAccent)
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
                TextButton(onClick = { showRecitersDialog = false }) { Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) }
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
                        TextButton(onClick = { PlayerService.setSleep(ctx, m); showSleep = false }, modifier = Modifier.fillMaxWidth()) {
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
        Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = if (active) GoldShine else Color.White.copy(alpha = 0.8f), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = if (active) GoldShine else Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

// ============================================================
// 3. محرك الأذان (7 مؤذنين - الافتراضي الحرم المكي) + تحديد الموقع + التعديل اليدوي
// ============================================================
data class Muezzin(val id: String, val name: String, val title: String, val url: String, val backupUrl: String)

object AdhanEngine {
    val muezzins = listOf(
        Muezzin("makkah", "أذان الحرم المكي", "المسجد الحرام • مكة المكرمة (الافتراضي)", "https://www.islamcan.com/audio/adhan/azan1.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/01.mp3"),
        Muezzin("madinah", "أذان الحرم المدني", "المسجد النبوي • المدينة المنورة", "https://www.islamcan.com/audio/adhan/azan2.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/02.mp3"),
        Muezzin("afasy", "مشاري راشد العفاسي", "أذان خاشع • دولة الكويت", "https://www.islamcan.com/audio/adhan/azan11.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/05.mp3"),
        Muezzin("abdulbasit", "عبد الباسط عبد الصمد", "أذان مصري أصيل • رحمه الله", "https://www.islamcan.com/audio/adhan/azan6.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/04.mp3"),
        Muezzin("qatami", "ناصر القطامي", "أذان مؤثر • الرياض", "https://www.islamcan.com/audio/adhan/azan15.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/07.mp3"),
        Muezzin("dosari", "ياسر الدوسري", "إمام الحرم المكي • تلاوة وأذان", "https://www.islamcan.com/audio/adhan/azan14.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/08.mp3"),
        Muezzin("aqsa", "أذان المسجد الأقصى", "أولى القبلتين • القدس الشريف", "https://www.islamcan.com/audio/adhan/azan3.mp3", "https://download.tvquran.com/download/TvQuran.com__Athan/03.mp3")
    )

    private const val PREFS = "adhan_engine_prefs"
    private val _playingId = MutableStateFlow<String?>(null)
    val playingId: StateFlow<String?> = _playingId.asStateFlow()

    private val _downloadingId = MutableStateFlow<String?>(null)
    val downloadingId: StateFlow<String?> = _downloadingId.asStateFlow()

    private val _cacheTick = MutableStateFlow(0)
    val cacheTick: StateFlow<Int> = _cacheTick.asStateFlow()

    private var adhanPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun selectedId(ctx: Context): String =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("muezzin_id", "makkah") ?: "makkah"

    fun setSelectedId(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("muezzin_id", id).apply()
        // تحميل الأذان المختار في الخلفية ليعمل بدون إنترنت عند دخول الوقت
        downloadMuezzin(ctx, id, silent = true)
    }

    fun isAdhanEnabled(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("adhan_enabled", true)

    fun setAdhanEnabled(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("adhan_enabled", enabled).apply()
    }

    // الفروقات اليدوية بالدقائق لكل صلاة (0: الفجر، 1: الشروق، 2: الظهر، 3: العصر، 4: المغرب، 5: العشاء)
    fun getOffsets(ctx: Context): IntArray {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return IntArray(6) { idx -> sp.getInt("offset_$idx", 0) }
    }

    fun setOffset(ctx: Context, prayerIdx: Int, minutes: Int) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("offset_$prayerIdx", minutes.coerceIn(-60, 60))
            .apply()
    }

    fun resetAllOffsets(ctx: Context) {
        val ed = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        for (i in 0..5) ed.putInt("offset_$i", 0)
        ed.apply()
    }

    fun adhanFile(ctx: Context, id: String): File =
        File(File(ctx.filesDir, "adhan").apply { mkdirs() }, "adhan_$id.mp3")

    fun hasCached(ctx: Context, id: String): Boolean =
        adhanFile(ctx, id).let { it.exists() && it.length() > 4096 }

    fun downloadMuezzin(ctx: Context, id: String, silent: Boolean = false) {
        val appCtx = ctx.applicationContext
        val m = muezzins.firstOrNull { it.id == id } ?: return
        if (hasCached(appCtx, id) || _downloadingId.value == id) return

        _downloadingId.value = id
        if (!silent) Toast.makeText(appCtx, "جارٍ حفظ ${m.name} للعمل بدون إنترنت...", Toast.LENGTH_SHORT).show()

        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                val out = adhanFile(appCtx, id)
                QuranAudioEngine.downloadWithResume(appCtx, m.url, out) ||
                    QuranAudioEngine.downloadWithResume(appCtx, m.backupUrl, out)
            }
            _downloadingId.value = null
            _cacheTick.update { it + 1 }
            if (!silent) {
                Toast.makeText(
                    appCtx,
                    if (ok) "تم حفظ ${m.name} بنجاح ✓" else "تعذر تحميل ملف الأذان، سيعمل عبر الإنترنت",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun playAdhan(ctx: Context, id: String = selectedId(ctx), prayerTitle: String? = null, useBackup: Boolean = false) {
        val appCtx = ctx.applicationContext
        val m = muezzins.firstOrNull { it.id == id } ?: muezzins.first()

        QuranAudioEngine.pauseIfPlaying(appCtx)
        stopAdhan(appCtx)

        _playingId.value = m.id
        if (prayerTitle != null) showAdhanNotification(appCtx, prayerTitle, m.name)

        val local = adhanFile(appCtx, m.id)
        val mp = MediaPlayer()
        adhanPlayer = mp

        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            mp.setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK)

            if (local.exists() && local.length() > 4096) {
                mp.setDataSource(local.absolutePath)
            } else {
                mp.setDataSource(if (useBackup) m.backupUrl else m.url)
            }

            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { stopAdhan(appCtx) }
            mp.setOnErrorListener { _, _, _ ->
                if (!useBackup && !(local.exists() && local.length() > 4096)) {
                    playAdhan(appCtx, m.id, prayerTitle, useBackup = true)
                } else {
                    stopAdhan(appCtx)
                }
                true
            }
            mp.prepareAsync()
        } catch (_: Exception) {
            if (!useBackup) playAdhan(appCtx, m.id, prayerTitle, useBackup = true)
            else stopAdhan(appCtx)
        }
    }

    fun stopAdhan(ctx: Context) {
        runCatching { adhanPlayer?.stop(); adhanPlayer?.release() }
        adhanPlayer = null
        _playingId.value = null
        NotificationManagerCompat.from(ctx.applicationContext).cancel(99)
    }

    private fun showAdhanNotification(ctx: Context, prayerTitle: String, muezzinName: String) {
        Notifs.ensureChannels(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return

        val stopPi = PendingIntent.getService(
            ctx, 99, Intent(ctx, PlayerService::class.java).setAction("STOP_ADHAN"),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, Notifs.CH_PLAYER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن موعد $prayerTitle")
            .setContentText("يرفع الأذان: $muezzinName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(Notifs.contentIntent(ctx))
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الأذان", stopPi)
            .build()
        try { nm.notify(99, notif) } catch (_: SecurityException) { }
    }

    // جدولة منبهات الأذان الدقيقة لليوم والغد
    fun scheduleExactAdhans(ctx: Context, adjustedTodayMillis: LongArray, adjustedTomorrowMillis: LongArray) {
        val appCtx = ctx.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val enabled = isAdhanEnabled(appCtx)
        val now = System.currentTimeMillis()
        val prayerIndices = listOf(0, 2, 3, 4, 5) // الفجر، الظهر، العصر، المغرب، العشاء
        val prayerNames = listOf("صلاة الفجر", "الشروق", "صلاة الظهر", "صلاة العصر", "صلاة المغرب", "صلاة العشاء")

        for (dayOffset in 0..1) {
            val times = if (dayOffset == 0) adjustedTodayMillis else adjustedTomorrowMillis
            for (pIdx in prayerIndices) {
                val reqCode = 8000 + dayOffset * 10 + pIdx
                val intent = Intent(appCtx, AdhanAlarmReceiver::class.java).apply {
                    putExtra("prayer_name", prayerNames[pIdx])
                    putExtra("prayer_idx", pIdx)
                }
                val pi = PendingIntent.getBroadcast(
                    appCtx, reqCode, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                am.cancel(pi)

                val triggerAt = times.getOrNull(pIdx) ?: 0L
                if (enabled && triggerAt > now + 3_000L) {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
                            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        } else {
                            am.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                        }
                    }
                }
            }
        }
    }
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AdhanEngine.isAdhanEnabled(context)) return
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        AdhanEngine.playAdhan(context, AdhanEngine.selectedId(context), prayerTitle = prayerName)
    }
}

// مساعد متطور لتحديد الموقع الجغرافي عبر GPS والشبكة واستخراج اسم المنطقة بالعربية
object SmartLocationResolver {
    @SuppressLint("MissingPermission")
    fun resolve(
        ctx: Context,
        onResult: (name: String, lat: Double, lng: Double) -> Unit,
        onError: (String) -> Unit
    ) {
        val appCtx = ctx.applicationContext
        val lm = appCtx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) {
            onError("خدمة الموقع غير مدعومة في هذا الجهاز")
            return
        }

        val gpsOn = runCatching { lm.isProviderEnabled(LocationManager.GPS_PROVIDER) }.getOrDefault(false)
        val netOn = runCatching { lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) }.getOrDefault(false)
        if (!gpsOn && !netOn) {
            onError("يرجى تفعيل GPS / الموقع من شريط الإشعارات أولاً")
            return
        }

        // 1. فحص آخر موقع مخزن إن كان حديثاً (أقل من 30 دقيقة)
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        val bestLast = providers.mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }

        if (bestLast != null && (System.currentTimeMillis() - bestLast.time) < 30 * 60_000L) {
            fetchPlaceNameAndDeliver(appCtx, bestLast.latitude, bestLast.longitude, onResult)
            return
        }

        // 2. طلب موقع فوري من GPS والشبكة معاً
        var delivered = false
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (delivered) return
                delivered = true
                runCatching { lm.removeUpdates(this) }
                fetchPlaceNameAndDeliver(appCtx, loc.latitude, loc.longitude, onResult)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (netOn) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())
            if (gpsOn) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper())

            CoroutineScope(Dispatchers.Main).launch {
                delay(12_000L)
                if (!delivered) {
                    delivered = true
                    runCatching { lm.removeUpdates(listener) }
                    if (bestLast != null) {
                        fetchPlaceNameAndDeliver(appCtx, bestLast.latitude, bestLast.longitude, onResult)
                    } else {
                        onError("تعذر التقاط إشارة الموقع، جرّب في مكان مفتوح أو فعّل الموقع عالي الدقة")
                    }
                }
            }
        } catch (_: Exception) {
            if (bestLast != null) {
                fetchPlaceNameAndDeliver(appCtx, bestLast.latitude, bestLast.longitude, onResult)
            } else {
                onError("تعذر الوصول إلى خدمة الموقع")
            }
        }
    }

    private fun fetchPlaceNameAndDeliver(
        ctx: Context,
        lat: Double,
        lng: Double,
        onResult: (name: String, lat: Double, lng: Double) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val placeName = runCatching {
                @Suppress("DEPRECATION")
                val list = Geocoder(ctx, Locale("ar")).getFromLocation(lat, lng, 1)
                val addr = list?.firstOrNull()
                val city = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea
                val country = addr?.countryName
                listOfNotNull(city, country).joinToString(" • ").ifBlank { null }
            }.getOrNull() ?: String.format(Locale.US, "موقعي (%.3f, %.3f)", lat, lng)

            withContext(Dispatchers.Main) {
                onResult(placeName, lat, lng)
            }
        }
    }
}

// ============================================================
// 4. شاشة مواقيت الصلاة والأذان والتعديل اليدوي الاحترافي (PrayerScreen)
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

    var offsetsTick by remember { mutableIntStateOf(0) }
    val offsets = remember(offsetsTick) { AdhanEngine.getOffsets(ctx) }

    val baseToday = remember(geo, method) { PrayerCalc.millis(geo, method, 0) }
    val baseTomorrow = remember(geo, method) { PrayerCalc.millis(geo, method, 1) }

    val times = remember(baseToday, offsets) {
        LongArray(6) { i -> baseToday[i] + offsets[i] * 60_000L }
    }
    val tomorrowTimes = remember(baseTomorrow, offsets) {
        LongArray(6) { i -> baseTomorrow[i] + offsets[i] * 60_000L }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        // تحميل صوت الأذان الافتراضي (الحرم المكي) تلقائياً في الخلفية ليعمل بدون نت
        AdhanEngine.downloadMuezzin(ctx, AdhanEngine.selectedId(ctx), silent = true)
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }

    // حساب الصلاة القادمة بناءً على الأوقات المعدلة يدوياً
    val nextPrayerInfo = remember(times, tomorrowTimes, now / 1000L) {
        val prayerOrder = listOf(0, 2, 3, 4, 5) // الفجر، الظهر، العصر، المغرب، العشاء
        val foundToday = prayerOrder.firstOrNull { idx -> times[idx] > now }
        if (foundToday != null) {
            foundToday to times[foundToday]
        } else {
            0 to tomorrowTimes[0]
        }
    }

    var adhanEnabled by remember { mutableStateOf(AdhanEngine.isAdhanEnabled(ctx)) }
    var selectedMuezzinId by remember { mutableStateOf(AdhanEngine.selectedId(ctx)) }
    val playingMuezzinId by AdhanEngine.playingId.collectAsState()
    val downloadingMuezzinId by AdhanEngine.downloadingId.collectAsState()
    val cacheTick by AdhanEngine.cacheTick.collectAsState()

    // إعادة جدولة منبهات الأذان عند أي تغيير في الموقع أو الطريقة أو التعديل اليدوي
    LaunchedEffect(times, tomorrowTimes, adhanEnabled) {
        AdhanEngine.scheduleExactAdhans(ctx, times, tomorrowTimes)
    }

    var isLocating by remember { mutableStateOf(false) }
    var showMuezzinSheet by remember { mutableStateOf(false) }
    var editingPrayerIdx by remember { mutableStateOf<Int?>(null) }
    val qibla = remember(geo) { PrayerCalc.qibla(geo.lat, geo.lng) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    val triggerLocate = {
        isLocating = true
        SmartLocationResolver.resolve(
            ctx = ctx,
            onResult = { placeName, lat, lng ->
                isLocating = false
                st.setLocation(placeName, lat, lng)
                Reminders.rescheduleAll(ctx)
                Toast.makeText(ctx, "تم تحديث الموقع: $placeName", Toast.LENGTH_SHORT).show()
            },
            onError = { msg ->
                isLocating = false
                Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                if (msg.contains("تفعيل GPS")) {
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                }
            }
        )
    }

    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) triggerLocate()
        else Toast.makeText(ctx, "يرجى منح إذن الموقع لتحديد مواقيت صلاتك بدقة", Toast.LENGTH_LONG).show()
    }

    val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    val prayerIcons = listOf(
        Icons.Default.NightsStay,
        Icons.Default.WbTwilight,
        Icons.Default.WbSunny,
        Icons.Default.LightMode,
        Icons.Default.Brightness4,
        Icons.Default.DarkMode
    )
    val selectedMuezzin = AdhanEngine.muezzins.firstOrNull { it.id == selectedMuezzinId } ?: AdhanEngine.muezzins.first()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("مواقيت الصلاة والأذان", onBack = { nav.back() })

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. بطاقة الصلاة القادمة والعد التنازلي الفاخر
            item {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    border = BorderStroke(1.5.dp, GoldWarm),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(Modifier.background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    "الصلاة القادمة",
                                    color = GoldShine,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                labels[nextPrayerInfo.first],
                                color = Color.White,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 32.sp
                            )
                            Text(
                                PrayerCalc.fmt(nextPrayerInfo.second, tz),
                                color = GoldShine,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = if (nextPrayerInfo.second > now) "المتبقي ${PrayerCalc.countdown(nextPrayerInfo.second - now)}" else "حان الآن وقت الأذان",
                                    color = GoldSoftBg,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 7.dp)
                                )
                            }

                            // شريط إيقاف الأذان الفوري إذا كان يصدح الآن
                            if (playingMuezzinId != null) {
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { fx.click(); AdhanEngine.stopAdhan(ctx) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.VolumeOff, null, tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text("إيقاف صوت الأذان الآن", color = Color.White, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            }

            // 2. بطاقة تحديد الموقع الجغرافي الذكي (بدون مدن ثابتة)
            item {
                QCard(shape = RoundedCornerShape(22.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(EmeraldVibrant.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = EmeraldVibrant, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الموقع الجغرافي الحالي", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            Text(geo.name, fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 14.sp, maxLines = 2)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                fx.click()
                                val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                val coarse = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (fine || coarse) triggerLocate()
                                else locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            enabled = !isLocating,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                        ) {
                            if (isLocating) {
                                CircularProgressIndicator(Modifier.size(16.dp), color = GoldShine, strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                                Text("جارٍ التحديد...", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.GpsFixed, null, tint = GoldShine, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تحديث موقعي", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. بطاقة الأذان التلقائي واختيار المؤذن (7 شيوخ - الافتراضي الحرم المكي)
            item {
                QCard(shape = RoundedCornerShape(22.dp), borderColor = GoldWarm.copy(alpha = 0.6f)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(42.dp).clip(CircleShape).background(GoldWarm.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.NotificationsActive, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("رفع الأذان تلقائياً عند دخول الوقت", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 14.sp)
                                Text(selectedMuezzin.name, fontSize = 12.sp, color = TurquoiseAccent, fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = adhanEnabled,
                                onCheckedChange = { on ->
                                    fx.click()
                                    adhanEnabled = on
                                    AdhanEngine.setAdhanEnabled(ctx, on)
                                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    AdhanEngine.scheduleExactAdhans(ctx, times, tomorrowTimes)
                                }
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { fx.click(); showMuezzinSheet = true },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.2.dp, EmeraldVibrant),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.RecordVoiceOver, null, tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("تغيير المؤذن (7 شيوخ)", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    fx.click()
                                    if (playingMuezzinId == selectedMuezzin.id) AdhanEngine.stopAdhan(ctx)
                                    else AdhanEngine.playAdhan(ctx, selectedMuezzin.id)
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (playingMuezzinId == selectedMuezzin.id) Color(0xFFC62828) else GoldWarm
                                )
                            ) {
                                Icon(
                                    if (playingMuezzinId == selectedMuezzin.id) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    null,
                                    tint = if (playingMuezzinId == selectedMuezzin.id) Color.White else RoyalForest,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    if (playingMuezzinId == selectedMuezzin.id) "إيقاف" else "معاينة",
                                    color = if (playingMuezzinId == selectedMuezzin.id) Color.White else RoyalForest,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. عنوان قسم المواقيت + تلميح التعديل اليدوي
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("مواقيت اليوم (اضغط للتعديل اليدوي)", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                    if (offsets.any { it != 0 }) {
                        TextButton(onClick = {
                            fx.click()
                            AdhanEngine.resetAllOffsets(ctx)
                            offsetsTick++
                            Toast.makeText(ctx, "تمت استعادة الأوقات الفلكية الأصلية", Toast.LENGTH_SHORT).show()
                        }) {
                            Text("تصفير التعديلات", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFC62828))
                        }
                    }
                }
            }

            // 5. بطاقات الصلوات الست (تفاعلية: عند الضغط عليها تفتح لوحة التعديل اليدوي)
            items(6) { idx ->
                val isNext = idx == nextPrayerInfo.first
                val offsetMin = offsets[idx]

                QCard(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isNext) GoldShine.copy(alpha = 0.16f) else null,
                    borderColor = if (isNext) GoldWarm else null,
                    onClick = { fx.click(); editingPrayerIdx = idx }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNext) EmeraldVibrant else p.bg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                prayerIcons[idx],
                                null,
                                tint = if (isNext) GoldShine else TurquoiseAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    labels[idx],
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = if (idx == 1) p.muted else p.ink
                                )
                                if (offsetMin != 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = GoldWarm.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = if (offsetMin > 0) "+$offsetMin د" else "$offsetMin د",
                                            color = GoldWarm,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                if (idx == 1) "وقت الشروق (بدون أذان)" else "اضغط لضبط الدقائق يدوياً",
                                fontSize = 11.sp,
                                color = p.muted,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            PrayerCalc.fmt(times[idx], tz),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isNext) TurquoiseAccent else EmeraldVibrant
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Tune, "تعديل الوقت", tint = p.muted, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // 6. بطاقة اتجاه القبلة
            item {
                QCard(shape = RoundedCornerShape(20.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(CircleShape).background(GoldWarm.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Navigation, "القبلة", tint = GoldWarm, modifier = Modifier.size(28.dp).rotate(qibla.toFloat()))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("اتجاه القبلة الشريفة", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 15.sp)
                            Text("${qibla.toInt()}° من الشمال باتجاه عقارب الساعة", fontSize = 12.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 7. طريقة الحساب الفلكي
            item { SectionTitle("طريقة الحساب الفلكي") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PrayerCalc.methodNames.size) { i ->
                        FilterChip(
                            selected = method == i,
                            onClick = { fx.click(); st.prayerMethod = i; Reminders.rescheduleAll(ctx) },
                            label = { Text(PrayerCalc.methodNames[i], fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // نافذة اختيار المؤذن (7 شيوخ - الافتراضي الحرم المكي)
    // ============================================================
    if (showMuezzinSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMuezzinSheet = false },
            containerColor = p.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "اختر صوت المؤذن (7 مؤذنين)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = p.ink,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    "يتم حفظ الأذان المختار تلقائياً ليعمل حتى بدون إنترنت",
                    fontSize = 12.sp,
                    color = p.muted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(AdhanEngine.muezzins, key = { it.id }) { m ->
                        val isSelected = m.id == selectedMuezzinId
                        val isPlayingThis = playingMuezzinId == m.id
                        val isDownloadingThis = downloadingMuezzinId == m.id
                        val isCached = remember(cacheTick, m.id) { AdhanEngine.hasCached(ctx, m.id) }

                        Surface(
                            onClick = {
                                fx.click()
                                selectedMuezzinId = m.id
                                AdhanEngine.setSelectedId(ctx, m.id)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) EmeraldVibrant else p.bg,
                            border = BorderStroke(1.4.dp, if (isSelected) GoldShine else p.border)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    null,
                                    tint = if (isSelected) GoldShine else p.muted
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        m.name,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 14.sp,
                                        color = if (isSelected) Color.White else p.ink
                                    )
                                    Text(
                                        if (isCached) "${m.title} • محفوظ بدون نت ✓" else m.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) GoldSoftBg else p.muted
                                    )
                                }

                                if (isDownloadingThis) {
                                    CircularProgressIndicator(Modifier.size(22.dp), color = GoldShine, strokeWidth = 2.5.dp)
                                } else if (!isCached) {
                                    IconButton(onClick = { fx.click(); AdhanEngine.downloadMuezzin(ctx, m.id, silent = false) }) {
                                        Icon(Icons.Default.DownloadForOffline, "تنزيل", tint = if (isSelected) GoldShine else GoldWarm)
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        fx.click()
                                        if (isPlayingThis) AdhanEngine.stopAdhan(ctx)
                                        else AdhanEngine.playAdhan(ctx, m.id)
                                    }
                                ) {
                                    Icon(
                                        if (isPlayingThis) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                                        "استماع",
                                        tint = if (isPlayingThis) Color(0xFFFF8A80) else if (isSelected) GoldShine else EmeraldVibrant,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ============================================================
    // لوحة التعديل اليدوي الاحترافية لوقت الصلاة عند الضغط عليها
    // ============================================================
    editingPrayerIdx?.let { idx ->
        var tempOffset by remember(idx) { mutableIntStateOf(offsets[idx]) }
        val previewMillis = baseToday[idx] + tempOffset * 60_000L

        ModalBottomSheet(
            onDismissRequest = { editingPrayerIdx = null },
            containerColor = p.card,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "ضبط وقت ${labels[idx]} يدوياً",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = p.ink
                )
                Text(
                    "الوقت الفلكي الأصلي: ${PrayerCalc.fmt(baseToday[idx], tz)}",
                    fontSize = 12.sp,
                    color = p.muted,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(16.dp))

                // بطاقة عرض الوقت المعدّل مباشرة
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RoyalForest,
                    border = BorderStroke(1.5.dp, GoldWarm),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الوقت المعتمد الجديد", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            PrayerCalc.fmt(previewMillis, tz),
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when {
                                tempOffset > 0 -> "تقديم +$tempOffset دقيقة"
                                tempOffset < 0 -> "تأخير $tempOffset دقيقة"
                                else -> "مطابق للتوقيت الفلكي (0 دقيقة)"
                            },
                            color = GoldShine,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // أزرار التحكم السريع بالدقائق (-5 / -1 / +1 / +5) + شريط تمرير سلس
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { fx.click(); tempOffset = (tempOffset - 5).coerceIn(-60, 60) },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("-5 د", fontWeight = FontWeight.ExtraBold) }

                    FilledTonalButton(
                        onClick = { fx.click(); tempOffset = (tempOffset - 1).coerceIn(-60, 60) },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("-1 د", fontWeight = FontWeight.ExtraBold) }

                    OutlinedButton(
                        onClick = { fx.click(); tempOffset = 0 },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("0", fontWeight = FontWeight.ExtraBold) }

                    FilledTonalButton(
                        onClick = { fx.click(); tempOffset = (tempOffset + 1).coerceIn(-60, 60) },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("+1 د", fontWeight = FontWeight.ExtraBold) }

                    FilledTonalButton(
                        onClick = { fx.click(); tempOffset = (tempOffset + 5).coerceIn(-60, 60) },
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("+5 د", fontWeight = FontWeight.ExtraBold) }
                }

                Spacer(Modifier.height(8.dp))

                Slider(
                    value = tempOffset.toFloat(),
                    onValueChange = { tempOffset = it.toInt() },
                    valueRange = -30f..30f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        thumbColor = GoldWarm,
                        activeTrackColor = EmeraldVibrant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        fx.click()
                        AdhanEngine.setOffset(ctx, idx, tempOffset)
                        offsetsTick++
                        editingPrayerIdx = null
                        Toast.makeText(ctx, "تم حفظ تعديل وقت ${labels[idx]} ✓", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = GoldShine)
                    Spacer(Modifier.width(8.dp))
                    Text("حفظ الوقت الجديد", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                }

                Spacer(Modifier.height(18.dp))
            }
        }
    }
}
