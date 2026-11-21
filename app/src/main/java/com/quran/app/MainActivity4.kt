package com.quran.app

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.app.data.AppPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File

private data class AudioMoshaf(val name: String, val server: String, val surahs: Set<Int>)
private data class AudioReciter(val name: String, val moshaf: List<AudioMoshaf>)
private data class AudioSurah(val id: Int, val name: String)

private val audioSurahs = listOf(
"الفاتحة","البقرة","آل عمران","النساء","المائدة","الأنعام","الأعراف","الأنفال","التوبة","يونس","هود","يوسف","الرعد","إبراهيم","الحجر","النحل","الإسراء","الكهف","مريم","طه","الأنبياء","الحج","المؤمنون","النور","الفرقان","الشعراء","النمل","القصص","العنكبوت","الروم","لقمان","السجدة","الأحزاب","سبأ","فاطر","يس","الصافات","ص","الزمر","غافر","فصلت","الشورى","الزخرف","الدخان","الجاثية","الأحقاف","محمد","الفتح","الحجرات","ق","الذاريات","الطور","النجم","القمر","الرحمن","الواقعة","الحديد","المجادلة","الحشر","الممتحنة","الصف","الجمعة","المنافقون","التغابن","الطلاق","التحريم","الملك","القلم","الحاقة","المعارج","نوح","الجن","المزمل","المدثر","القيامة","الإنسان","المرسلات","النبأ","النازعات","عبس","التكوير","الانفطار","المطففين","الانشقاق","البروج","الطارق","الأعلى","الغاشية","الفجر","البلد","الشمس","الليل","الضحى","الشرح","التين","العلق","القدر","البينة","الزلزلة","العاديات","القارعة","التكاثر","العصر","الهمزة","الفيل","قريش","الماعون","الكوثر","الكافرون","النصر","المسد","الإخلاص","الفلق","الناس"
).mapIndexed { i, n -> AudioSurah(i + 1, n) }

private suspend fun loadMp3QuranReciters(): List<AudioReciter> = withContext(Dispatchers.IO) {
    val c = URL("https://www.mp3quran.net/api/v3/reciters?language=ar").openConnection() as HttpURLConnection
    try {
        c.connectTimeout = 15000; c.readTimeout = 20000
        val json = JSONObject(c.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        val arr = json.optJSONArray("reciters") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                val editions = r.optJSONArray("moshaf") ?: continue
                val mos = buildList {
                    for (j in 0 until editions.length()) {
                        val m = editions.optJSONObject(j) ?: continue
                        val server = m.optString("server").trim()
                        if (server.isBlank()) continue
                        val ids = m.optString("surah_list").split(',').mapNotNull { it.trim().toIntOrNull() }.toSet()
                        add(AudioMoshaf(m.optString("name", "رواية حفص"), server, ids))
                    }
                }
                if (mos.isNotEmpty()) add(AudioReciter(r.optString("name"), mos))
            }
        }
    } finally { c.disconnect() }
}

@Composable
fun AudioRecitersScreen(isDarkTheme: Boolean, searchQuery: String, onSearchQueryChange: (String) -> Unit, onBackHome: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { AppPreferencesRepository(context) }
    val scope = rememberCoroutineScope()
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    var reciters by remember { mutableStateOf<List<AudioReciter>>(emptyList()) }
    var reciter by remember { mutableStateOf<AudioReciter?>(null) }
    var edition by remember { mutableStateOf<AudioMoshaf?>(null) }
    var selectedSurah by remember { mutableIntStateOf(1) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("اختر القارئ والسورة") }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var playing by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var showReciters by remember { mutableStateOf(false) }
    var showEditions by remember { mutableStateOf(false) }

    fun stopPlayer() { try { player?.stop() } catch (_: Exception) {}; try { player?.release() } catch (_: Exception) {}; player = null; playing = false }
    DisposableEffect(Unit) { onDispose { stopPlayer() } }
    suspend fun refresh() {
        loading = true; error = ""
        try {
            reciters = loadMp3QuranReciters()
            reciter = reciters.firstOrNull(); edition = reciter?.moshaf?.firstOrNull()
            if (reciters.isEmpty()) error = "لا توجد قائمة قراء متاحة حالياً."
        } catch (_: Exception) { error = "تعذر تحميل القراء. تحقق من الإنترنت ثم أعد المحاولة." }
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    fun play(id: Int) {
        val m = edition ?: return
        if (id !in m.surahs) { status = "هذه السورة غير متاحة بهذه الرواية"; return }
        stopPlayer(); selectedSurah = id
        val url = m.server.trimEnd('/') + "/" + id.toString().padStart(3, '0') + ".mp3"
        val safeReciter = (reciter?.name ?: "reciter").hashCode().toUInt().toString(16)
        val safeEdition = m.name.hashCode().toUInt().toString(16)
        val audioDir = File(context.filesDir, "audio_cache").apply { mkdirs() }
        val cached = File(audioDir, "${safeReciter}_${safeEdition}_${id.toString().padStart(3, '0')}.mp3")
        status = if (cached.exists() && cached.length() > 0L) "تشغيل النسخة المحفوظة دون إعادة تنزيل…" else "جارٍ تنزيل السورة لأول مرة…"
        downloading = true
        scope.launch {
            try {
                val audioFile = withContext(Dispatchers.IO) {
                    if (!cached.exists() || cached.length() == 0L) {
                        val temp = File(audioDir, cached.name + ".part")
                        val connection = URL(url).openConnection() as HttpURLConnection
                        try {
                            connection.connectTimeout = 20000
                            connection.readTimeout = 60000
                            connection.setRequestProperty("User-Agent", "QuranApp-Android")
                            connection.inputStream.use { input ->
                                temp.outputStream().buffered().use { output -> input.copyTo(output) }
                            }
                            if (!temp.renameTo(cached)) {
                                temp.copyTo(cached, overwrite = true)
                                temp.delete()
                            }
                        } finally { connection.disconnect() }
                    }
                    if (!cached.exists() || cached.length() == 0L) error("empty audio")
                    cached
                }
                downloading = false
                player = MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                    setWakeMode(context, android.os.PowerManager.PARTIAL_WAKE_LOCK)
                    setDataSource(audioFile.absolutePath)
                    setOnPreparedListener { it.start(); playing = true; status = "يُشغّل الآن دون إنترنت: ${audioSurahs[id - 1].name} • ${reciter?.name}" }
                    setOnCompletionListener { playing = false; status = "اكتملت السورة • محفوظة للاستماع لاحقاً" }
                    setOnErrorListener { _, _, _ -> playing = false; status = "تعذر تشغيل الملف المحفوظ؛ احذفه وأعد تنزيله"; true }
                    prepareAsync()
                }
            } catch (_: Exception) {
                downloading = false
                status = "تعذر تنزيل التلاوة. تحقق من الاتصال وحاول مجدداً."
            }
        }
    }

    val bg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val card = if (isDarkTheme) RoyalPalette.EmeraldCardDark else androidx.compose.ui.graphics.Color.White
    val ink = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald
    val accent = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    if (showReciters) AlertDialog(onDismissRequest = { showReciters = false }, containerColor = card, title = { Text("اختر القارئ", color = ink) }, text = {
        LazyColumn(Modifier.heightIn(max = 420.dp)) { items(reciters) { r -> TextButton(onClick = { reciter = r; edition = r.moshaf.firstOrNull(); showReciters = false; status = "اختر الرواية والسورة" }, modifier = Modifier.fillMaxWidth()) { Text(r.name, color = ink) } } }
    }, confirmButton = { TextButton(onClick = { showReciters = false }) { Text("إغلاق") } })
    if (showEditions) AlertDialog(onDismissRequest = { showEditions = false }, containerColor = card, title = { Text("اختر الرواية", color = ink) }, text = {
        LazyColumn(Modifier.heightIn(max = 360.dp)) { items(reciter?.moshaf ?: emptyList()) { m -> TextButton(onClick = { edition = m; showEditions = false; status = "تم اختيار ${m.name}" }, modifier = Modifier.fillMaxWidth()) { Text(m.name, color = ink) } } }
    }, confirmButton = { TextButton(onClick = { showEditions = false }) { Text("إغلاق") } })

    Column(Modifier.fillMaxSize().background(bg).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = RoyalPalette.EmeraldRoyal, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.5.dp, accent)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("المكتبة الصوتية", color = RoyalPalette.GoldHighlight, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("جميع سور القرآن الكريم • قائمة قراء من MP3Quran", color = RoyalPalette.CreamText, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showReciters = true }, modifier = Modifier.weight(1f), enabled = reciters.isNotEmpty()) { Text(reciter?.name ?: "اختيار القارئ", color = RoyalPalette.CreamText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(onClick = { showEditions = true }, modifier = Modifier.weight(1f), enabled = reciter != null) { Text(edition?.name ?: "الرواية", color = RoyalPalette.CreamText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                Text(status, color = RoyalPalette.GoldHighlight, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { if (playing) { stopPlayer(); status = "تم إيقاف التلاوة" } else play(selectedSurah) }, enabled = edition != null, colors = ButtonDefaults.buttonColors(containerColor = RoyalPalette.GoldRoyal, contentColor = RoyalPalette.EmeraldDeep)) { Text(if (playing) "إيقاف" else "تشغيل المحدد") }
                    OutlinedButton(onClick = { stopPlayer(); status = "تم إيقاف التلاوة" }, enabled = player != null) { Text("إيقاف نهائي", color = RoyalPalette.CreamText) }
                }
            }
        }
        if (loading) Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = accent) }
        else if (error.isNotEmpty()) Column(Modifier.fillMaxWidth().padding(12.dp)) { Text(error, color = ink); Button(onClick = { scope.launch { refresh() } }) { Text("إعادة المحاولة") } }
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            items(audioSurahs.filter { searchQuery.isBlank() || it.name.contains(searchQuery.trim()) || it.id.toString() == searchQuery.trim() }) { s ->
                val available = s.id in (edition?.surahs ?: emptySet())
                val active = s.id == selectedSurah && playing
                Surface(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).clickable(enabled = available) { play(s.id) }, color = if (active) RoyalPalette.EmeraldRoyal else card, shape = RoundedCornerShape(15.dp), border = BorderStroke(1.dp, accent)) {
                    Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${s.id.toString().padStart(3, '0')}  ${s.name}", color = if (active) RoyalPalette.CreamText else ink, fontWeight = FontWeight.Bold, fontFamily = indexFont)
                            Text(if (available) "متاحة للتشغيل" else "غير متاحة بهذه الرواية", color = if (active) RoyalPalette.GoldHighlight else RoyalPalette.InkMuted, fontSize = 10.sp)
                        }
                        Text(if (active) "يُشغّل الآن" else if (available) "▶ تشغيل" else "—", color = accent, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
