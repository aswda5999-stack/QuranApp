import os

base = "app/src/main/java/com/quran/app"
os.makedirs(base, exist_ok=True)

code = '''package com.quran.app

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

val EmeraldDark = Color(0xFF063527)
val EmeraldPrimary = Color(0xFF0B4F3B)
val GoldAccent = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF3E2A9)
val IvoryBg = Color(0xFFFAF7F0)
val CardCream = Color(0xFFFFFFFF)
val TextDark = Color(0xFF162922)
val TextMuted = Color(0xFF5D726A)

data class Surah(val num: Int, val name: String, val type: String, val ayahs: Int, val page: Int)
data class Reciter(val id: String, val name: String, val url: String)

object DataRepo {
    val reciters = listOf(
        Reciter("afs", "مشاري العفاسي", "https://server8.mp3quran.net/afs"),
        Reciter("basit", "عبد الباسط عبد الصمد", "https://server7.mp3quran.net/basit"),
        Reciter("maher", "ماهر المعيقلي", "https://server12.mp3quran.net/maher"),
        Reciter("yasser", "ياسر الدوسري", "https://server11.mp3quran.net/yasser")
    )
    val surahs = listOf(
        Surah(1,"الفاتحة","مكية",7,1), Surah(2,"البقرة","مدنية",286,2), Surah(3,"آل عمران","مدنية",200,50),
        Surah(4,"النساء","مدنية",176,77), Surah(5,"المائدة","مدنية",120,106), Surah(6,"الأنعام","مكية",165,128),
        Surah(7,"الأعراف","مكية",206,151), Surah(8,"الأنفال","مدنية",75,177), Surah(9,"التوبة","مدنية",129,187),
        Surah(10,"يونس","مكية",109,208), Surah(11,"هود","مكية",123,221), Surah(12,"يوسف","مكية",111,235),
        Surah(13,"الرعد","مدنية",43,249), Surah(14,"إبراهيم","مكية",52,255), Surah(15,"الحجر","مكية",99,262),
        Surah(16,"النحل","مكية",128,267), Surah(17,"الإسراء","مكية",111,282), Surah(18,"الكهف","مكية",110,293),
        Surah(19,"مريم","مكية",98,305), Surah(20,"طه","مكية",135,312), Surah(36,"يس","مكية",83,440),
        Surah(55,"الرحمن","مدنية",78,531), Surah(56,"الواقعة","مكية",96,534), Surah(67,"الملك","مكية",30,562),
        Surah(78,"النبأ","مكية",40,582), Surah(87,"الأعلى","مكية",19,591), Surah(93,"الضحى","مكية",11,596),
        Surah(97,"القدر","مكية",5,598), Surah(103,"العصر","مكية",3,601), Surah(108,"الكوثر","مكية",3,602),
        Surah(112,"الإخلاص","مكية",4,604), Surah(113,"الفلق","مكية",5,604), Surah(114,"الناس","مكية",6,604)
    )
    val hadiths = listOf(
        "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى» - متفق عليه",
        "«بُنِيَ الإِسْلامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لا إِلَهَ إِلا اللَّهُ...» - متفق عليه",
        "«مِنْ حُسْنِ إِسْلامِ الْمَرْءِ تَرْكُهُ مَا لا يَعْنِيهِ» - رواه الترمذي",
        "«اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لأَصْحَابِهِ» - رواه مسلم"
    )
    val adhkar = listOf(
        Pair("أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إِلاَّ اللَّهُ", 1),
        Pair("بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ", 3),
        Pair("أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", 3),
        Pair("سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَاللَّهُ أَكْبَرُ", 33)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = EmeraldDark.toArgb()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = EmeraldPrimary, secondary = GoldAccent, background = IvoryBg)) {
                    QuranAppRoot()
                }
            }
        }
    }
}

@Composable
fun QuranAppRoot() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf("home") }
    var activePage by remember { mutableIntStateOf(prefs.getInt("last_page", 1)) }
    var nightMode by remember { mutableStateOf(prefs.getBoolean("night_mode", false)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound_on", true)) }
    var vibOn by remember { mutableStateOf(prefs.getBoolean("vib_on", true)) }
    var keepOn by remember { mutableStateOf(prefs.getBoolean("keep_on", true)) }
    var selectedReciter by remember { mutableStateOf(DataRepo.reciters.first()) }
    var playingSurah by remember { mutableStateOf<Surah?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var downloadingNum by remember { mutableIntStateOf(-1) }
    val mediaPlayer = remember { MediaPlayer() }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) { onDispose { runCatching { mediaPlayer.release() } } }

    fun clickFx() { if (soundOn) view.playSoundEffect(SoundEffectConstants.CLICK) }
    fun vibrateFx() {
        if (!vibOn) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(35)
            }
        }
    }

    BackHandler(enabled = screen != "home") {
        clickFx()
        screen = if (screen == "reader") "index" else "home"
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        containerColor = IvoryBg,
        bottomBar = {
            if (screen != "reader") {
                NavigationBar(containerColor = EmeraldDark) {
                    val items = listOf(
                        Triple("home", "الرئيسية", Icons.Default.Home),
                        Triple("index", "المصحف", Icons.AutoMirrored.Filled.MenuBook),
                        Triple("audio", "المشغل", Icons.Default.Headphones),
                        Triple("adhkar", "الأذكار", Icons.Default.TouchApp),
                        Triple("settings", "الإعدادات", Icons.Default.Settings)
                    )
                    items.forEach { (id, label, icon) ->
                        NavigationBarItem(
                            selected = screen == id,
                            onClick = { clickFx(); screen = id },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = EmeraldDark, selectedTextColor = GoldAccent, indicatorColor = GoldAccent, unselectedIconColor = Color.White, unselectedTextColor = Color.White)
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
            when (screen) {
                "home" -> {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        item {
                            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = EmeraldDark), border = BorderStroke(1.5.dp, GoldAccent)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("القرآن الكريم", color = GoldLight, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("﴿ إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ ﴾", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(onClick = { clickFx(); screen = "reader" }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = EmeraldDark)) {
                                        Text("متابعة القراءة (صفحة $activePage)", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MenuBox("فهرس السور", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f)) { clickFx(); screen = "index" }
                                MenuBox("المشغل الصوتي", Icons.Default.Headphones, Modifier.weight(1f)) { clickFx(); screen = "audio" }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                MenuBox("الأحاديث النبوية", Icons.Default.FormatQuote, Modifier.weight(1f)) { clickFx(); screen = "hadith" }
                                MenuBox("الأذكار والمسبحة", Icons.Default.TouchApp, Modifier.weight(1f)) { clickFx(); screen = "adhkar" }
                            }
                        }
                    }
                }
                "index" -> {
                    var q by remember { mutableStateOf("") }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(color = EmeraldPrimary) {
                            OutlinedTextField(
                                value = q, onValueChange = { q = it },
                                placeholder = { Text("ابحث باسم السورة...", color = Color.White.copy(alpha = 0.7f)) },
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = GoldAccent)
                            )
                        }
                        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(DataRepo.surahs.filter { it.name.contains(q.trim()) }) { s ->
                                Card(modifier = Modifier.fillMaxWidth().clickable { clickFx(); activePage = s.page; screen = "reader" }, colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("${s.num}. سورة ${s.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                                        Text("صفحة ${s.page}", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                "reader" -> {
                    val pagerState = rememberPagerState(initialPage = (activePage - 1).coerceIn(0, 603), pageCount = { 604 })
                    val curPage = pagerState.currentPage + 1
                    LaunchedEffect(curPage) { activePage = curPage; prefs.edit().putInt("last_page", curPage).apply() }
                    val invert = remember { ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)) }
                    Column(modifier = Modifier.fillMaxSize().background(if (nightMode) Color.Black else Color(0xFFF6EFE0))) {
                        Surface(color = EmeraldDark) {
                            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { clickFx(); screen = "index" }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
                                Text("صفحة $curPage من 604", color = GoldLight, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { clickFx(); nightMode = !nightMode; prefs.edit().putBoolean("night_mode", nightMode).apply() }) {
                                    Icon(if (nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = GoldAccent)
                                }
                            }
                        }
                        HorizontalPager(state = pagerState, reverseLayout = true, modifier = Modifier.weight(1f).fillMaxWidth()) { idx ->
                            val pNum = idx + 1
                            val imgUrl = "https://android.quran.com/data/width_1024/page${pNum.toString().padStart(3, '0')}.png"
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(context).data(imgUrl).diskCachePolicy(CachePolicy.ENABLED).build(),
                                contentDescription = "صفحة $pNum",
                                contentScale = ContentScale.Fit,
                                colorFilter = if (nightMode) ColorFilter.colorMatrix(invert) else null,
                                modifier = Modifier.fillMaxSize().padding(6.dp),
                                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldAccent) } }
                            )
                        }
                    }
                }
                "audio" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DataRepo.reciters) { r ->
                                FilterChip(selected = r.id == selectedReciter.id, onClick = { clickFx(); selectedReciter = r }, label = { Text(r.name, fontWeight = FontWeight.Bold) })
                            }
                        }
                        playingSurah?.let { ps ->
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), colors = CardDefaults.cardColors(containerColor = EmeraldDark), border = BorderStroke(1.dp, GoldAccent)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("سورة ${ps.name} -${selectedReciter.name}", color = GoldLight, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        clickFx()
                                        if (mediaPlayer.isPlaying) { mediaPlayer.pause(); isPlaying = false } else { mediaPlayer.start(); isPlaying = true }
                                    }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = GoldAccent) }
                                }
                            }
                        }
                        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(DataRepo.surahs) { s ->
                                val file = File(context.filesDir, "${selectedReciter.id}_${s.num}.mp3")
                                val ready = file.exists() && file.length() > 1000
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("سورة ${s.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(if (ready) "محملة على الجهاز (بدون إنترنت)" else "اضغط للتنزيل والتشغيل", fontSize = 12.sp, color = TextMuted)
                                        }
                                        if (downloadingNum == s.num) {
                                            CircularProgressIndicator(modifier = Modifier.size(28.dp), color = EmeraldPrimary)
                                        } else {
                                            Button(
                                                onClick = {
                                                    clickFx()
                                                    scope.launch {
                                                        if (!ready) {
                                                            downloadingNum = s.num
                                                            withContext(Dispatchers.IO) {
                                                                runCatching {
                                                                    val url = URL("${selectedReciter.url}/${s.num.toString().padStart(3, '0')}.mp3")
                                                                    (url.openConnection() as HttpURLConnection).inputStream.use { inp ->
                                                                        FileOutputStream(file).use { out -> inp.copyTo(out) }
                                                                    }
                                                                }
                                                            }
                                                            downloadingNum = -1
                                                        }
                                                        if (file.exists() && file.length() > 1000) {
                                                            runCatching {
                                                                mediaPlayer.reset()
                                                                mediaPlayer.setAudioAttributes(AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                                                                mediaPlayer.setDataSource(file.absolutePath)
                                                                mediaPlayer.prepare()
                                                                mediaPlayer.start()
                                                                playingSurah = s
                                                                isPlaying = true
                                                            }
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (ready) EmeraldPrimary else GoldAccent, contentColor = if (ready) GoldLight else EmeraldDark)
                                            ) { Text(if (ready) "تشغيل" else "تنزيل") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "hadith" -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(DataRepo.hadiths) { h ->
                            Card(colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                Text(h, modifier = Modifier.padding(16.dp), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 28.sp)
                            }
                        }
                    }
                }
                "adhkar" -> {
                    var tasbih by remember { mutableIntStateOf(0) }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth().clickable { clickFx(); vibrateFx(); tasbih++ }, colors = CardDefaults.cardColors(containerColor = EmeraldPrimary), border = BorderStroke(2.dp, GoldAccent)) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("المسبحة الإلكترونية (اضغط هنا)", color = GoldLight, fontSize = 14.sp)
                                    Text("$tasbih", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { clickFx(); tasbih = 0 }) { Text("تصفير", color = GoldAccent) }
                                }
                            }
                        }
                        items(DataRepo.adhkar) { (txt, count) ->
                            var c by remember { mutableIntStateOf(0) }
                            Card(modifier = Modifier.fillMaxWidth().clickable { clickFx(); vibrateFx(); if (c < count) c++ }, colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(txt, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("التكرار: $c / $count", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "settings" -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingTile("صوت اللمس", soundOn) { soundOn = it; prefs.edit().putBoolean("sound_on", it).apply(); clickFx() }
                        SettingTile("الاهتزاز", vibOn) { vibOn = it; prefs.edit().putBoolean("vib_on", it).apply(); clickFx() }
                        SettingTile("الوضع الليلي للمصحف", nightMode) { nightMode = it; prefs.edit().putBoolean("night_mode", it).apply(); clickFx() }
                        SettingTile("إبقاء الشاشة مضاءة", keepOn) { keepOn = it; prefs.edit().putBoolean("keep_on", it).apply(); clickFx() }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuBox(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(110.dp).clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.2.dp, GoldAccent)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
        }
    }
}

@Composable
fun SettingTile(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardCream)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}
'''

with open(f"{base}/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(code.strip() + "\n")
