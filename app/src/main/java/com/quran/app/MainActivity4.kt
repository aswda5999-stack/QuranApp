@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
// 1. محرك الصوت والتنزيل المرن لجميع الشبكات (QuranAudioEngine & AudioCache)
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
        Toast.makeText(appCtx, "جارٍ تنزيل سورة ${QuranData.surah(num)?.name ?: ""}...", Toast.LENGTH_SHORT).show()

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
                Toast.makeText(appCtx, "اكتمل التنزيل ✓ جاهزة للتشغيل", Toast.LENGTH_SHORT).show()
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
// 2. شاشة المكتبة الصوتية والمشغل السفلي المطور (AudioScreen)
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

            // شريط اختيار القارئ
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
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

            // مربع البحث عن السورة
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            )

            // تصنيفات السور
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

            // قائمة السور (زر تنزيل + نسبة مئوية دائرية + تحوّل لزر تشغيل فقط بعد الاكتمال بدون زر حذف)
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 4.dp,
                    bottom = if (isPlayerExpanded) 245.dp else 92.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (filteredSurahs.isEmpty()) {
                    item { EmptyState(Icons.Default.MusicOff, "لا توجد سور مطابقة في هذا القسم") }
                }
                items(filteredSurahs, key = { it.num }) { s ->
                    val cached = remember(ps.cacheVersion, st.reciterId, s.num) { AudioCache.has(ctx, st.reciterId, s.num) }
                    val progress = ps.downloads["${st.reciterId}_${s.num}"]
                    val isCurrent = ps.surah == s.num && ps.reciterId == st.reciterId
                    val pctInt = ((progress ?: 0f) * 100).toInt().coerceIn(1, 99)

                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isCurrent) EmeraldVibrant.copy(alpha = 0.14f) else null,
                        borderColor = if (isCurrent) GoldWarm else if (cached) TurquoiseAccent.copy(alpha = 0.55f) else null,
                        onClick = {
                            if (isCurrent) PlayerService.toggle(ctx)
                            else PlayerService.play(ctx, st.reciterId, s.num)
                        }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(s.num, 36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "سُورَةُ ${s.name}",
                                    fontFamily = UthmanicFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = if (isCurrent) TurquoiseAccent else p.ink
                                )
                                Text(
                                    text = when {
                                        progress != null -> "جارٍ التنزيل ($pctInt%)..."
                                        cached -> "محملة • جاهزة للتشغيل بدون إنترنت ✓"
                                        else -> "${s.type} • ${s.ayahs} آية"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        progress != null -> GoldWarm
                                        cached -> TurquoiseAccent
                                        else -> p.muted
                                    }
                                )
                            }

                            // حالة التحميل / التشغيل:
                            // 1. أثناء التنزيل: حلقة تقدم دائرية مع النسبة المئوية بالمنتصف
                            // 2. بعد التنزيل: زر تشغيل بارز فقط (بدون زر حذف نهائياً)
                            // 3. قبل التنزيل: زر تنزيل + زر تشغيل مباشر
                            when {
                                progress != null -> {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(RoyalForest.copy(alpha = 0.1f))
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.size(38.dp),
                                            color = GoldWarm,
                                            trackColor = EmeraldVibrant.copy(alpha = 0.25f),
                                            strokeWidth = 3.2.dp
                                        )
                                        Text(
                                            text = "$pctInt%",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = p.ink
                                        )
                                    }
                                }

                                cached -> {
                                    // بعد اكتمال التنزيل يظهر زر التشغيل فقط بتصميم مميز
                                    Surface(
                                        onClick = {
                                            fx.click()
                                            if (isCurrent) PlayerService.toggle(ctx)
                                            else PlayerService.play(ctx, st.reciterId, s.num)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isCurrent && ps.isPlaying) GoldWarm else EmeraldVibrant,
                                        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.7f)),
                                        shadowElevation = 3.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCurrent && ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = "تشغيل",
                                                tint = if (isCurrent && ps.isPlaying) RoyalForest else GoldShine,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isCurrent && ps.isPlaying) "إيقاف" else "تشغيل",
                                                color = if (isCurrent && ps.isPlaying) RoyalForest else Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(
                                            onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) },
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(GoldWarm.copy(alpha = 0.14f))
                                        ) {
                                            Icon(
                                                Icons.Default.DownloadForOffline,
                                                "تنزيل",
                                                tint = GoldWarm,
                                                modifier = Modifier.size(22.dp)
                                            )
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
                                                modifier = Modifier.size(34.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ============================================================
        // المشغل السفلي الملكي المتحرك (Mini & Expanded Player)
        // ============================================================
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
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { fx.click(); manualExpanded = false }
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(42.dp).height(4.dp).clip(CircleShape).background(GoldShine.copy(alpha = 0.6f)))
                        }

                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
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
                                    text = playingSurah?.let { "سُورَةُ ${it.name}" } ?: "سُورَةُ الْفَاتِحَةِ (اضغط تشغيل)",
                                    color = Color.White,
                                    fontFamily = UthmanicFontFamily,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
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
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, "اقرأ السورة", tint = GoldShine, modifier = Modifier.size(18.dp))
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
                                    modifier = Modifier.height(26.dp)
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
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            if (ps.isLoading) CircularProgressIndicator(Modifier.size(24.dp), color = RoyalForest, strokeWidth = 3.dp)
                                            else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل", tint = RoyalForest, modifier = Modifier.size(30.dp))
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
