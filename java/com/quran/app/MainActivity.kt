package com.quran.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL

data class SurahInfo(val number: Int, val name: String, val englishName: String, val numberOfAyahs: Int, val revelationType: String)
data class AyahItem(val numberInSurah: Int, val text: String, val tafsir: String, val audioUrl: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF1B5E20),
                        secondary = Color(0xFFD4AF37),
                        background = Color(0xFFFDFBF7),
                        surface = Color(0xFFFFFFFF)
                    )
                ) {
                    QuranAppScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }
    var surahs by remember { mutableStateOf<List<SurahInfo>>(emptyList()) }
    var selectedSurah by remember { mutableStateOf<SurahInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lastSurahNum by remember { mutableIntStateOf(prefs.getInt("last_surah", -1)) }
    var lastSurahName by remember { mutableStateOf(prefs.getString("last_surah_name", "") ?: "") }

    LaunchedEffect(Unit) {
        try {
            surahs = fetchSurahList(context)
            isLoading = false
        } catch (e: Exception) {
            errorMsg = "تعذر تحميل السور. تأكد من الاتصال بالإنترنت في المرة الأولى."
            isLoading = false
        }
    }

    if (selectedSurah != null) {
        SurahDetailScreen(
            surah = selectedSurah!!,
            onBack = {
                lastSurahNum = prefs.getInt("last_surah", -1)
                lastSurahName = prefs.getString("last_surah_name", "") ?: ""
                selectedSurah = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("القرآن الكريم", fontWeight = FontWeight.Bold, color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFFDFBF7))
                    .padding(padding)
            ) {
                if (lastSurahNum != -1 && lastSurahName.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clickable {
                                surahs.find { it.number == lastSurahNum }?.let { selectedSurah = it }
                            },
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("متابعة الختمة (آخر قراءة)", fontSize = 13.sp, color = Color(0xFF2E7D32))
                                Text(lastSurahName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            }
                        }
                    }
                }

                when {
                    isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF1B5E20))
                    }
                    errorMsg != null -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(errorMsg!!, textAlign = TextAlign.Center, color = Color.Red)
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(surahs) { surah ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedSurah = surah },
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("${surah.number}", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                                        }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column {
                                            Text(surah.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                            Text(
                                                "${if (surah.revelationType == "Meccan") "مكية" else "مدنية"} • ${surah.numberOfAyahs} آية",
                                                fontSize = 13.sp,
                                                color = Color.Gray
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(surah: SurahInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }

    var ayahs by remember { mutableStateOf<List<AyahItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReciter by remember { mutableStateOf("ar.alafasy") }
    var selectedAyahTafsir by remember { mutableStateOf<AyahItem?>(null) }
    var playingAyah by remember { mutableIntStateOf(-1) }
    val mediaPlayer = remember { MediaPlayer() }

    val reciters = listOf(
        "ar.alafasy" to "مشاري العفاسي",
        "ar.abdurrahmaansudais" to "عبدالرحمن السديس",
        "ar.husary" to "محمود خليل الحصري",
        "ar.minshawi" to "محمد صديق المنشاوي"
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(surah.number, selectedReciter) {
        isLoading = true
        prefs.edit().putInt("last_surah", surah.number).putString("last_surah_name", surah.name).apply()
        try {
            ayahs = fetchSurahAyahs(context, surah.number, selectedReciter)
        } catch (_: Exception) {}
        isLoading = false
    }

    if (selectedAyahTafsir != null) {
        AlertDialog(
            onDismissRequest = { selectedAyahTafsir = null },
            title = { Text("التفسير الميسر - آية ${selectedAyahTafsir!!.numberInSurah}", fontWeight = FontWeight.Bold) },
            text = { Text(selectedAyahTafsir!!.tafsir, fontSize = 16.sp, lineHeight = 26.sp) },
            confirmButton = {
                TextButton(onClick = { selectedAyahTafsir = null }) {
                    Text("إغلاق", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah.name, fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1B5E20))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFDFBF7))
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("القارئ:", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(reciters.first { it.first == selectedReciter }.second, color = Color(0xFF1B5E20))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        reciters.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedReciter = id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF1B5E20))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ayahs) { ayah ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAyahTafsir = ayah },
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "${ayah.text} ﴿${ayah.numberInSurah}﴾",
                                    fontSize = 22.sp,
                                    lineHeight = 38.sp,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("اضغط لعرض التفسير", fontSize = 12.sp, color = Color.Gray)
                                    IconButton(onClick = {
                                        scope.launch {
                                            try {
                                                if (playingAyah == ayah.numberInSurah && mediaPlayer.isPlaying) {
                                                    mediaPlayer.pause()
                                                    playingAyah = -1
                                                } else {
                                                    mediaPlayer.reset()
                                                    mediaPlayer.setAudioAttributes(
                                                        AudioAttributes.Builder()
                                                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                            .build()
                                                    )
                                                    mediaPlayer.setDataSource(ayah.audioUrl)
                                                    mediaPlayer.prepareAsync()
                                                    mediaPlayer.setOnPreparedListener {
                                                        it.start()
                                                        playingAyah = ayah.numberInSurah
                                                    }
                                                    mediaPlayer.setOnCompletionListener { playingAyah = -1 }
                                                }
                                            } catch (_: Exception) {}
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (playingAyah == ayah.numberInSurah) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = Color(0xFF1B5E20)
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
}

suspend fun fetchSurahList(context: Context): List<SurahInfo> = withContext(Dispatchers.IO) {
    val cacheFile = File(context.filesDir, "surahs_cache.json")
    val jsonStr = if (cacheFile.exists()) {
        cacheFile.readText()
    } else {
        val text = URL("https://api.alquran.cloud/v1/surah").readText()
        cacheFile.writeText(text)
        text
    }
    val data = JSONObject(jsonStr).getJSONArray("data")
    val list = mutableListOf<SurahInfo>()
    for (i in 0 until data.length()) {
        val obj = data.getJSONObject(i)
        list.add(
            SurahInfo(
                number = obj.getInt("number"),
                name = obj.getString("name"),
                englishName = obj.getString("englishName"),
                numberOfAyahs = obj.getInt("numberOfAyahs"),
                revelationType = obj.getString("revelationType")
            )
        )
    }
    list
}

suspend fun fetchSurahAyahs(context: Context, surahNumber: Int, reciter: String): List<AyahItem> = withContext(Dispatchers.IO) {
    val cacheFile = File(context.filesDir, "surah_${surahNumber}_$reciter.json")
    val jsonStr = if (cacheFile.exists()) {
        cacheFile.readText()
    } else {
        val text = URL("https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,ar.muyassar,$reciter").readText()
        cacheFile.writeText(text)
        text
    }
    val editions = JSONObject(jsonStr).getJSONArray("data")
    val quranAyahs = editions.getJSONObject(0).getJSONArray("ayahs")
    val tafsirAyahs = editions.getJSONObject(1).getJSONArray("ayahs")
    val audioAyahs = editions.getJSONObject(2).getJSONArray("ayahs")

    val list = mutableListOf<AyahItem>()
    for (i in 0 until quranAyahs.length()) {
        val q = quranAyahs.getJSONObject(i)
        val t = tafsirAyahs.getJSONObject(i)
        val a = audioAyahs.getJSONObject(i)
        list.add(
            AyahItem(
                numberInSurah = q.getInt("numberInSurah"),
                text = q.getString("text"),
                tafsir = t.getString("text"),
                audioUrl = a.getString("audio")
            )
        )
    }
    list
}
