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
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
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
// 1. خدمة الصوت والتخزين المحلي (PlayerService & AudioCache)
// ============================================================
data class PlayerState(
    val reciterId: String = "afs",
    val surah: Int = 0,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val repeatOne: Boolean = false,
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
        private const val A_REPEAT = "com.quran.app.REPEAT"
        private const val A_AUTONEXT = "com.quran.app.AUTONEXT"
        private const val A_SLEEP = "com.quran.app.SLEEP"
        private const val A_DOWNLOAD = "com.quran.app.DOWNLOAD"

        private val _state = MutableStateFlow(PlayerState())
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        private fun intent(ctx: Context, action: String) = Intent(ctx, PlayerService::class.java).setAction(action)
        private fun cmd(ctx: Context, i: Intent) { runCatching { ctx.startService(i) } }

        fun play(ctx: Context, reciterId: String, surah: Int) {
            runCatching { ContextCompat.startForegroundService(ctx, intent(ctx, A_PLAY).putExtra("r", reciterId).putExtra("s", surah)) }
        }
        fun download(ctx: Context, reciterId: String, surah: Int) {
            runCatching { ContextCompat.startForegroundService(ctx, intent(ctx, A_DOWNLOAD).putExtra("r", reciterId).putExtra("s", surah)) }
        }
        fun delete(ctx: Context, reciterId: String, surah: Int) { AudioCache.file(ctx, reciterId, surah).delete(); bump() }
        fun clearAll(ctx: Context) { AudioCache.clear(ctx); bump() }
        fun toggle(ctx: Context) = cmd(ctx, intent(ctx, A_TOGGLE))
        fun next(ctx: Context) = cmd(ctx, intent(ctx, A_NEXT))
        fun prev(ctx: Context) = cmd(ctx, intent(ctx, A_PREV))
        fun stop(ctx: Context) = cmd(ctx, intent(ctx, A_STOP))
        fun seek(ctx: Context, ms: Int) = cmd(ctx, intent(ctx, A_SEEK).putExtra("ms", ms))
        fun seekBy(ctx: Context, deltaMs: Int) = cmd(ctx, intent(ctx, A_SEEK_BY).putExtra("d", deltaMs))
        fun toggleRepeat(ctx: Context) = cmd(ctx, intent(ctx, A_REPEAT))
        fun toggleAutoNext(ctx: Context) = cmd(ctx, intent(ctx, A_AUTONEXT))
        fun setSleep(ctx: Context, minutes: Int) = cmd(ctx, intent(ctx, A_SLEEP).putExtra("m", minutes))
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
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            A_PLAY -> playSurah(intent.getStringExtra("r") ?: _state.value.reciterId, intent.getIntExtra("s", 1))
            A_DOWNLOAD -> startDownload(intent.getStringExtra("r") ?: _state.value.reciterId, intent.getIntExtra("s", 1))
            A_TOGGLE -> toggle()
            A_STOP -> stopAll()
            A_NEXT -> { val s = _state.value; if (s.surah in 1..113) playSurah(s.reciterId, s.surah + 1) }
            A_PREV -> { val s = _state.value; if (s.surah > 1) playSurah(s.reciterId, s.surah - 1) }
            A_SEEK -> seekTo(intent.getIntExtra("ms", 0))
            A_SEEK_BY -> mp?.let { seekTo(runCatching { it.currentPosition }.getOrDefault(0) + intent.getIntExtra("d", 0)) }
            A_REPEAT -> _state.update { it.copy(repeatOne = !it.repeatOne) }
            A_AUTONEXT -> _state.update { it.copy(autoNext = !it.autoNext) }
            A_SLEEP -> setSleep(intent.getIntExtra("m", 0))
        }
        if (mp == null && dl.isEmpty()) maybeStop()
        return START_NOT_STICKY
    }

    private fun urlOf(r: Reciter, num: Int) = "${r.url}/${num.toString().padStart(3, '0')}.mp3"

    private fun playSurah(reciterId: String, num: Int) {
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: return
        releasePlayer()
        _state.update { it.copy(reciterId = reciterId, surah = num, isLoading = true, isPlaying = false, positionMs = 0, durationMs = 0, error = null) }
        goForeground()
        runCatching { @Suppress("DEPRECATION") audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) }

        val file = AudioCache.file(this, reciterId, num)
        val p = MediaPlayer()
        mp = p
        try {
            p.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_MEDIA).build())
            p.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            if (file.exists() && file.length() > 1000) p.setDataSource(file.absolutePath) else p.setDataSource(urlOf(reciter, num))
            p.setOnPreparedListener { m ->
                m.start()
                _state.update { it.copy(isLoading = false, isPlaying = true, durationMs = runCatching { m.duration }.getOrDefault(0)) }
                startTicker(); goForeground()
            }
            p.setOnCompletionListener { onComplete() }
            p.setOnErrorListener { _, _, _ -> fail("تعذر تشغيل السورة، تحقق من اتصال الإنترنت"); true }
            p.prepareAsync()
        } catch (e: Exception) {
            fail("تعذر تشغيل السورة، تحقق من اتصال الإنترنت")
        }
    }

    private fun fail(msg: String) {
        releasePlayer()
        _state.update { it.copy(isLoading = false, isPlaying = false, error = msg) }
        goForeground()
    }

    private fun onComplete() {
        val s = _state.value
        when {
            s.repeatOne -> { mp?.seekTo(0); mp?.start() }
            s.autoNext && s.surah < 114 -> playSurah(s.reciterId, s.surah + 1)
            else -> { _state.update { it.copy(isPlaying = false, positionMs = 0) }; goForeground() }
        }
    }

    private fun toggle() {
        val s = _state.value
        val p = mp
        when {
            s.isLoading -> Unit
            p != null && s.isPlaying -> pause()
            p != null -> {
                runCatching { @Suppress("DEPRECATION") audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) }
                runCatching { p.start() }
                _state.update { it.copy(isPlaying = true) }
                startTicker(); goForeground()
            }
            s.surah > 0 -> playSurah(s.reciterId, s.surah)
        }
    }

    private fun pause() { runCatching { mp?.pause() }; _state.update { it.copy(isPlaying = false) }; goForeground() }

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
                    runCatching { _state.update { it.copy(positionMs = p.currentPosition, durationMs = p.duration.coerceAtLeast(0)) } }
                }
                delay(400)
            }
        }
    }

    private fun setSleep(minutes: Int) {
        sleepJob?.cancel()
        _state.update { it.copy(sleepLeft = minutes) }
        if (minutes <= 0) return
        sleepJob = scope.launch {
            var left = minutes
            while (left > 0) { delay(60_000L); left--; _state.update { it.copy(sleepLeft = left) } }
            pause()
        }
    }

    private fun releasePlayer() { ticker?.cancel(); runCatching { mp?.reset(); mp?.release() }; mp = null }

    private fun stopAll() {
        releasePlayer(); sleepJob?.cancel()
        runCatching { @Suppress("DEPRECATION") audioManager.abandonAudioFocus(focusListener) }
        _state.update { it.copy(surah = 0, isPlaying = false, isLoading = false, positionMs = 0, durationMs = 0, sleepLeft = 0) }
        maybeStop()
    }

    private fun maybeStop() { if (mp == null && dl.isEmpty()) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() } }

    private fun startDownload(reciterId: String, num: Int) {
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: return
        val key = "${reciterId}_$num"
        if (dl.containsKey(key) || AudioCache.has(this, reciterId, num)) return
        goForeground()
        _state.update { it.copy(downloads = it.downloads + (key to 0f), error = null) }
        dl[key] = scope.launch {
            val ok = withContext(Dispatchers.IO) { downloadFile(reciter, num, key) }
            dl.remove(key)
            _state.update { it.copy(downloads = it.downloads - key, cacheVersion = it.cacheVersion + 1, error = if (ok) null else "تعذر تنزيل السورة") }
            if (mp == null && dl.isEmpty()) maybeStop()
        }
    }

    private fun downloadFile(reciter: Reciter, num: Int, key: String): Boolean {
        val out = AudioCache.file(this, reciter.id, num)
        val part = File(out.path + ".part")
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlOf(reciter, num)).openConnection() as HttpURLConnection).apply { connectTimeout = 15_000; readTimeout = 20_000; instanceFollowRedirects = true }
            if (conn.responseCode !in 200..299) false
            else {
                val total = conn.contentLengthLong
                conn.inputStream.use { input ->
                    FileOutputStream(part).use { output ->
                        val buf = ByteArray(32 * 1024); var done = 0L; var last = -1
                        while (true) {
                            val read = input.read(buf); if (read == -1) break
                            output.write(buf, 0, read); done += read
                            if (total > 0) {
                                val pct = (done * 100 / total).toInt()
                                if (pct != last) { last = pct; _state.update { it.copy(downloads = it.downloads + (key to pct / 100f)) } }
                            }
                        }
                    }
                }
                out.delete(); part.renameTo(out)
            }
        } catch (e: Exception) { part.delete(); false } finally { conn?.disconnect() }
    }

    private fun actionPi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code, Intent(this, PlayerService::class.java).setAction(action), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private fun buildNotification(): Notification {
        val s = _state.value
        val surah = QuranData.surah(s.surah)
        val reciter = QuranData.reciters.firstOrNull { it.id == s.reciterId }?.name ?: ""
        val b = NotificationCompat.Builder(this, Notifs.CH_PLAYER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(if (surah != null) "سورة ${surah.name}" else "تنزيل التلاوات")
            .setContentText(if (surah != null) reciter else "جارٍ التنزيل...")
            .setContentIntent(Notifs.contentIntent(this))
            .setOngoing(s.isPlaying || s.downloads.isNotEmpty())
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (surah != null) {
            b.addAction(if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (s.isPlaying) "إيقاف مؤقت" else "تشغيل", actionPi(A_TOGGLE, 1))
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", actionPi(A_STOP, 2))
        }
        return b.build()
    }

    private fun goForeground() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIF_ID, n)
    }

    override fun onDestroy() { releasePlayer(); scope.cancel(); super.onDestroy() }
}

// ============================================================
// 2. شاشة الاستماع والمكتبة الصوتية (AudioScreen)
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
    var showSleep by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(ps.error) { ps.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); PlayerService.clearError() } }

    val reciter = QuranData.reciters.firstOrNull { it.id == st.reciterId } ?: QuranData.reciters.first()
    val nowSurah = QuranData.surah(ps.surah)
    val nowReciter = QuranData.reciters.firstOrNull { it.id == ps.reciterId } ?: reciter
    val list = remember(query) { QuranData.search(query) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("المكتبة الصوتية", onBack = { nav.back() })

        LazyRow(contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QuranData.reciters, key = { it.id }) { r ->
                FilterChip(
                    selected = r.id == st.reciterId, onClick = { fx.click(); st.reciterId = r.id },
                    shape = RoundedCornerShape(16.dp),
                    label = { Text(r.name, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                )
            }
        }

        Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, border = BorderStroke(1.5.dp, GoldWarm), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(nowSurah?.let { "سورة ${it.name}" } ?: "اختر سورة لبدء التلاوة", color = GoldSoftBg, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (nowSurah != null) "${nowReciter.name} • ${nowReciter.style}" else "${reciter.name} • ${reciter.style}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(Modifier.fillMaxWidth()) {
                        val frac = drag ?: if (ps.durationMs > 0) ps.positionMs / ps.durationMs.toFloat() else 0f
                        Slider(
                            value = frac.coerceIn(0f, 1f),
                            onValueChange = { drag = it },
                            onValueChangeFinished = { drag?.let { PlayerService.seek(ctx, (it * ps.durationMs).toInt()) }; drag = null },
                            enabled = ps.surah > 0 && ps.durationMs > 0 && !ps.isLoading,
                            colors = SliderDefaults.colors(thumbColor = GoldShine, activeTrackColor = GoldShine, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fmtMs(if (drag != null) (drag!! * ps.durationMs).toInt() else ps.positionMs), color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(fmtMs(ps.durationMs), color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { fx.click(); PlayerService.prev(ctx) }, enabled = ps.surah > 1) { Icon(Icons.Default.SkipPrevious, "السابقة", tint = Color.White) }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, -10_000) }, enabled = ps.surah > 0) { Icon(Icons.Default.Replay10, "رجوع 10ث", tint = GoldSoftBg) }
                            Surface(
                                onClick = { fx.click(); if (ps.surah == 0) PlayerService.play(ctx, st.reciterId, 1) else PlayerService.toggle(ctx) },
                                shape = CircleShape, color = GoldShine, modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (ps.isLoading) CircularProgressIndicator(Modifier.size(26.dp), color = RoyalForest, strokeWidth = 3.dp)
                                    else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل/إيقاف", tint = RoyalForest, modifier = Modifier.size(30.dp))
                                }
                            }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, 10_000) }, enabled = ps.surah > 0) { Icon(Icons.Default.Forward10, "تقديم 10ث", tint = GoldSoftBg) }
                            IconButton(onClick = { fx.click(); PlayerService.next(ctx) }, enabled = ps.surah in 1..113) { Icon(Icons.Default.SkipNext, "التالية", tint = Color.White) }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ModeButton("تكرار السورة", Icons.Default.RepeatOne, ps.repeatOne) { PlayerService.toggleRepeat(ctx) }
                    ModeButton("ختمة متتالية", Icons.Default.PlaylistPlay, ps.autoNext) { PlayerService.toggleAutoNext(ctx) }
                    ModeButton(if (ps.sleepLeft > 0) "نوم: ${ps.sleepLeft}د" else "مؤقت نوم", Icons.Default.Bedtime, ps.sleepLeft > 0) { showSleep = true }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("ابحث عن سورة...", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
            shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة بهذا الاسم") }
            items(list, key = { it.num }) { s ->
                val cached = remember(ps.cacheVersion, st.reciterId, s.num) { AudioCache.has(ctx, st.reciterId, s.num) }
                val progress = ps.downloads["${st.reciterId}_${s.num}"]
                val isCurrent = ps.surah == s.num && ps.reciterId == st.reciterId
                QCard(shape = RoundedCornerShape(18.dp), borderColor = if (isCurrent) EmeraldVibrant else null, onClick = { if (isCurrent) PlayerService.toggle(ctx) else PlayerService.play(ctx, st.reciterId, s.num) }) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(s.num, 32)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text(when { progress != null -> "جارٍ التنزيل ${(progress * 100).toInt()}%"; cached -> "محملة (تعمل بدون إنترنت)"; else -> "بث مباشر • ${s.ayahs} آية" }, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (cached) TurquoiseAccent else p.muted)
                        }
                        when {
                            progress != null -> CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp), color = EmeraldVibrant, strokeWidth = 3.dp)
                            cached -> IconButton(onClick = { fx.click(); PlayerService.delete(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.DeleteOutline, "حذف", tint = Color(0xFFB71C1C), modifier = Modifier.size(22.dp)) }
                            else -> IconButton(onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Download, "تنزيل", tint = GoldWarm, modifier = Modifier.size(22.dp)) }
                        }
                        IconButton(onClick = { fx.click(); if (isCurrent) PlayerService.toggle(ctx) else PlayerService.play(ctx, st.reciterId, s.num) }, modifier = Modifier.size(38.dp)) {
                            Icon(if (isCurrent && ps.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "تشغيل", tint = EmeraldVibrant, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("مؤقت النوم", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إلغاء المؤقت", 15 to "بعد 15 دقيقة", 30 to "بعد 30 دقيقة", 60 to "بعد ساعة").forEach { (m, label) ->
                        TextButton(onClick = { PlayerService.setSleep(ctx, m); showSleep = false }, modifier = Modifier.fillMaxWidth()) { Text(label, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun ModeButton(label: String, icon: ImageVector, active: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    TextButton(onClick = { fx.click(); onClick() }) {
        Icon(icon, null, tint = if (active) GoldShine else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) GoldShine else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
