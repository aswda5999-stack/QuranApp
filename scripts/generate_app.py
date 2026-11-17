import os

base = "app/src/main/java/com/quran/app"
os.makedirs(base, exist_ok=True)

data_kt = '''package com.quran.app

data class Surah(val num: Int, val name: String, val type: String, val ayahs: Int, val page: Int)
data class Reciter(val id: String, val name: String, val url: String)
data class HadithItem(val title: String, val text: String, val source: String)
data class DhikrItem(val title: String, val text: String, val target: Int)

object QuranRepository {
    val reciters = listOf(
        Reciter("afs", "مشاري راشد العفاسي", "https://server8.mp3quran.net/afs"),
        Reciter("basit", "عبد الباسط عبد الصمد", "https://server7.mp3quran.net/basit"),
        Reciter("maher", "ماهر المعيقلي", "https://server12.mp3quran.net/maher"),
        Reciter("yasser", "ياسر الدوسري", "https://server11.mp3quran.net/yasser"),
        Reciter("minsh", "محمد صديق المنشاوي", "https://server10.mp3quran.net/minsh")
    )

    val surahs = listOf(
        Surah(1,"الفاتحة","مكية",7,1), Surah(2,"البقرة","مدنية",286,2), Surah(3,"آل عمران","مدنية",200,50),
        Surah(4,"النساء","مدنية",176,77), Surah(5,"المائدة","مدنية",120,106), Surah(6,"الأنعام","مكية",165,128),
        Surah(7,"الأعراف","مكية",206,151), Surah(8,"الأنفال","مدنية",75,177), Surah(9,"التوبة","مدنية",129,187),
        Surah(10,"يونس","مكية",109,208), Surah(11,"هود","مكية",123,221), Surah(12,"يوسف","مكية",111,235),
        Surah(13,"الرعد","مدنية",43,249), Surah(14,"إبراهيم","مكية",52,255), Surah(15,"الحجر","مكية",99,262),
        Surah(16,"النحل","مكية",128,267), Surah(17,"الإسراء","مكية",111,282), Surah(18,"الكهف","مكية",110,293),
        Surah(19,"مريم","مكية",98,305), Surah(20,"طه","مكية",135,312), Surah(21,"الأنبياء","مكية",112,322),
        Surah(22,"الحج","مدنية",78,332), Surah(23,"المؤمنون","مكية",118,342), Surah(24,"النور","مدنية",64,350),
        Surah(25,"الفرقان","مكية",77,359), Surah(26,"الشعراء","مكية",227,367), Surah(27,"النمل","مكية",93,377),
        Surah(28,"القصص","مكية",88,385), Surah(29,"العنكبوت","مكية",69,396), Surah(30,"الروم","مكية",60,404),
        Surah(31,"لقمان","مكية",34,411), Surah(32,"السجدة","مكية",30,415), Surah(33,"الأحزاب","مدنية",73,418),
        Surah(34,"سبأ","مكية",54,428), Surah(35,"فاطر","مكية",45,434), Surah(36,"يس","مكية",83,440),
        Surah(37,"الصافات","مكية",182,446), Surah(38,"ص","مكية",88,453), Surah(39,"الزمر","مكية",75,458),
        Surah(40,"غافر","مكية",85,467), Surah(41,"فصلت","مكية",54,477), Surah(42,"الشورى","مكية",53,483),
        Surah(43,"الزخرف","مكية",89,489), Surah(44,"الدخان","مكية",59,496), Surah(45,"الجاثية","مكية",37,499),
        Surah(46,"الأحقاف","مكية",35,502), Surah(47,"محمد","مدنية",38,507), Surah(48,"الفتح","مدنية",29,511),
        Surah(49,"الحجرات","مدنية",18,515), Surah(50,"ق","مكية",45,518), Surah(51,"الذاريات","مكية",60,520),
        Surah(52,"الطور","مكية",49,523), Surah(53,"النجم","مكية",62,526), Surah(54,"القمر","مكية",55,528),
        Surah(55,"الرحمن","مدنية",78,531), Surah(56,"الواقعة","مكية",96,534), Surah(57,"الحديد","مدنية",29,537),
        Surah(58,"المجادلة","مدنية",22,542), Surah(59,"الحشر","مدنية",24,545), Surah(60,"الممتحنة","مدنية",13,549),
        Surah(61,"الصف","مدنية",14,551), Surah(62,"الجمعة","مدنية",11,553), Surah(63,"المنافقون","مدنية",11,554),
        Surah(64,"التغابن","مدنية",18,556), Surah(65,"الطلاق","مدنية",12,558), Surah(66,"التحريم","مدنية",12,560),
        Surah(67,"الملك","مكية",30,562), Surah(68,"القلم","مكية",52,564), Surah(69,"الحاقة","مكية",52,566),
        Surah(70,"المعارج","مكية",44,568), Surah(71,"نوح","مكية",28,570), Surah(72,"الجن","مكية",28,572),
        Surah(73,"المزمل","مكية",20,574), Surah(74,"المدثر","مكية",56,575), Surah(75,"القيامة","مكية",40,577),
        Surah(76,"الإنسان","مدنية",31,578), Surah(77,"المرسلات","مكية",50,580), Surah(78,"النبأ","مكية",40,582),
        Surah(79,"النازعات","مكية",46,583), Surah(80,"عبس","مكية",42,585), Surah(81,"التكوير","مكية",29,586),
        Surah(82,"الانفطار","مكية",19,587), Surah(83,"المطففين","مكية",36,587), Surah(84,"الانشقاق","مكية",25,589),
        Surah(85,"البروج","مكية",22,590), Surah(86,"الطارق","مكية",17,591), Surah(87,"الأعلى","مكية",19,591),
        Surah(88,"الغاشية","مكية",26,592), Surah(89,"الفجر","مكية",30,593), Surah(90,"البلد","مكية",20,594),
        Surah(91,"الشمس","مكية",15,595), Surah(92,"الليل","مكية",21,595), Surah(93,"الضحى","مكية",11,596),
        Surah(94,"الشرح","مكية",8,596), Surah(95,"التين","مكية",8,597), Surah(96,"العلق","مكية",19,597),
        Surah(97,"القدر","مكية",5,598), Surah(98,"البينة","مدنية",8,598), Surah(99,"الزلزلة","مدنية",8,599),
        Surah(100,"العاديات","مكية",11,599), Surah(101,"القارعة","مكية",11,600), Surah(102,"التكاثر","مكية",8,600),
        Surah(103,"العصر","مكية",3,601), Surah(104,"الهمزة","مكية",9,601), Surah(105,"الفيل","مكية",5,601),
        Surah(106,"قريش","مكية",4,602), Surah(107,"الماعون","مكية",7,602), Surah(108,"الكوثر","مكية",3,602),
        Surah(109,"الكافرون","مكية",6,603), Surah(110,"النصر","مدنية",3,603), Surah(111,"المسد","مكية",5,603),
        Surah(112,"الإخلاص","مكية",4,604), Surah(113,"الفلق","مكية",5,604), Surah(114,"الناس","مكية",6,604)
    )

    val hadiths = listOf(
        HadithItem("الأعمال بالنيات", "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»", "متفق عليه"),
        HadithItem("أركان الإسلام", "«بُنِيَ الإِسْلامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لا إِلَهَ إِلا اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ»", "متفق عليه"),
        HadithItem("فضل القرآن", "«خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ»", "رواه البخاري"),
        HadithItem("حسن الإسلام", "«مِنْ حُسْنِ إِسْلامِ الْمَرْءِ تَرْكُهُ مَا لا يَعْنِيهِ»", "رواه الترمذي"),
        HadithItem("شفاعة القرآن", "«اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لأَصْحَابِهِ»", "رواه مسلم")
    )

    val adhkar = listOf(
        DhikrItem("سيد الاستغفار", "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لا يَغْفِرُ الذُّنُوبَ إِلا أَنْتَ", 1),
        DhikrItem("ذكر الحفظ", "بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", 3),
        DhikrItem("التعوذ النبوي", "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ", 3),
        DhikrItem("الباقيات الصالحات", "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلا إِلَهَ إِلا اللَّهُ، وَاللَّهُ أَكْبَرُ", 33),
        DhikrItem("الصلاة على النبي", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ", 10)
    )
}
'''

main_kt = '''package com.quran.app

import android.app.Activity
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = EmeraldDark.toArgb()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = lightColorScheme(primary = EmeraldPrimary, secondary = GoldAccent, background = IvoryBg)) {
                    QuranAppMain()
                }
            }
        }
    }
}

@Composable
fun QuranAppMain() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("quran_v3_prefs", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf("home") }
    var activePage by remember { mutableIntStateOf(prefs.getInt("last_page", 1)) }
    var bookmarkPage by remember { mutableIntStateOf(prefs.getInt("bookmark_page", 1)) }
    var nightMode by remember { mutableStateOf(prefs.getBoolean("night_mode", false)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound_on", true)) }
    var vibOn by remember { mutableStateOf(prefs.getBoolean("vib_on", true)) }
    var keepOn by remember { mutableStateOf(prefs.getBoolean("keep_on", true)) }

    var selectedReciter by remember { mutableStateOf(QuranRepository.reciters.first()) }
    var playingSurah by remember { mutableStateOf<Surah?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var downloadingNum by remember { mutableIntStateOf(-1) }
    val mediaPlayer = remember { MediaPlayer() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(keepOn, screen) {
        val window = (context as? Activity)?.window
        if (keepOn && screen == "reader") window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { mediaPlayer.release() } }
    }

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
                    val navs = listOf(
                        Triple("home", "الرئيسية", Icons.Default.Home),
                        Triple("index", "المصحف", Icons.AutoMirrored.Filled.MenuBook),
                        Triple("audio", "المشغل", Icons.Default.Headphones),
                        Triple("adhkar", "الأذكار", Icons.Default.TouchApp),
                        Triple("settings", "الإعدادات", Icons.Default.Settings)
                    )
                    navs.forEach { (id, label, icon) ->
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
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Button(onClick = { clickFx(); screen = "reader" }, colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = EmeraldDark)) {
                                            Text("متابعة (ص $activePage)", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(onClick = { clickFx(); activePage = bookmarkPage; screen = "reader" }, border = BorderStroke(1.dp, GoldLight)) {
                                            Text("الفاصل (ص $bookmarkPage)", color = GoldLight, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeGridCard("فهرس السور (114)", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f)) { clickFx(); screen = "index" }
                                HomeGridCard("المشغل الصوتي", Icons.Default.Headphones, Modifier.weight(1f)) { clickFx(); screen = "audio" }
                            }
                        }
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                HomeGridCard("الأحاديث النبوية", Icons.Default.FormatQuote, Modifier.weight(1f)) { clickFx(); screen = "hadith" }
                                HomeGridCard("الأذكار والمسبحة", Icons.Default.TouchApp, Modifier.weight(1f)) { clickFx(); screen = "adhkar" }
                            }
                        }
                    }
                }
                "index" -> {
                    var query by remember { mutableStateOf("") }
                    Column(modifier = Modifier.fillMaxSize()) {
                        Surface(color = EmeraldPrimary) {
                            OutlinedTextField(
                                value = query, onValueChange = { query = it },
                                placeholder = { Text("ابحث باسم السورة أو رقمها...", color = Color.White.copy(alpha = 0.75f)) },
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = GoldAccent)
                            )
                        }
                        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(QuranRepository.surahs.filter { it.name.contains(query.trim()) || it.num.toString() == query.trim() }) { s ->
                                Card(modifier = Modifier.fillMaxWidth().clickable { clickFx(); activePage = s.page; screen = "reader" }, colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("${s.num}. سورة ${s.name}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                                            Text("${s.type} • ${s.ayahs} آية", fontSize = 12.sp, color = TextMuted)
                                        }
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
                                Row {
                                    IconButton(onClick = { clickFx(); bookmarkPage = curPage; prefs.edit().putInt("bookmark_page", curPage).apply() }) {
                                        Icon(if (bookmarkPage == curPage) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = GoldAccent)
                                    }
                                    IconButton(onClick = { clickFx(); nightMode = !nightMode; prefs.edit().putBoolean("night_mode", nightMode).apply() }) {
                                        Icon(if (nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = GoldAccent)
                                    }
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
                                modifier = Modifier.fillMaxSize().padding(4.dp),
                                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldAccent) } }
                            )
                        }
                    }
                }
                "audio" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QuranRepository.reciters) { r ->
                                FilterChip(selected = r.id == selectedReciter.id, onClick = { clickFx(); selectedReciter = r }, label = { Text(r.name, fontWeight = FontWeight.Bold) })
                            }
                        }
                        playingSurah?.let { ps ->
                            Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), colors = CardDefaults.cardColors(containerColor = EmeraldDark), border = BorderStroke(1.dp, GoldAccent)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("سورة ${ps.name} - ${selectedReciter.name}", color = GoldLight, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        clickFx()
                                        if (mediaPlayer.isPlaying) { mediaPlayer.pause(); isPlaying = false } else { mediaPlayer.start(); isPlaying = true }
                                    }) { Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = GoldAccent) }
                                }
                            }
                        }
                        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(QuranRepository.surahs) { s ->
                                val file = File(context.filesDir, "${selectedReciter.id}_${s.num}.mp3")
                                val ready = file.exists() && file.length() > 1000
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Column {
                                            Text("${s.num}. سورة ${s.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            Text(if (ready) "محملة على الجهاز (بدون إنترنت)" else "اضغط للتنزيل والاستماع", fontSize = 12.sp, color = TextMuted)
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
                        items(QuranRepository.hadiths) { h ->
                            Card(colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(h.title, fontWeight = FontWeight.Bold, color = EmeraldPrimary, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(h.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 28.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text(h.source, fontSize = 12.sp, color = TextMuted)
                                        TextButton(onClick = {
                                            clickFx()
                                            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "${h.title}\\n${h.text}\\n(${h.source})") }, "مشاركة"))
                                        }) { Text("مشاركة") }
                                    }
                                }
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
                                    Text("المسبحة الإلكترونية (اضغط للتسبيح)", color = GoldLight, fontSize = 14.sp)
                                    Text("$tasbih", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.Bold)
                                    TextButton(onClick = { clickFx(); tasbih = 0 }) { Text("تصفير العداد", color = GoldAccent) }
                                }
                            }
                        }
                        items(QuranRepository.adhkar) { d ->
                            var c by remember { mutableIntStateOf(0) }
                            Card(modifier = Modifier.fillMaxWidth().clickable { clickFx(); vibrateFx(); if (c < d.target) c++ }, colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.dp, GoldAccent)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(d.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(d.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark, lineHeight = 26.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("التكرار: $c / ${d.target}", color = if (c >= d.target) GoldAccent else EmeraldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                "settings" -> {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingSwitchCard("صوت اللمس", soundOn) { soundOn = it; prefs.edit().putBoolean("sound_on", it).apply(); clickFx() }
                        SettingSwitchCard("الاهتزاز", vibOn) { vibOn = it; prefs.edit().putBoolean("vib_on", it).apply(); clickFx() }
                        SettingSwitchCard("الوضع الليلي للمصحف", nightMode) { nightMode = it; prefs.edit().putBoolean("night_mode", it).apply(); clickFx() }
                        SettingSwitchCard("إبقاء الشاشة مضاءة", keepOn) { keepOn = it; prefs.edit().putBoolean("keep_on", it).apply(); clickFx() }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeGridCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(110.dp).clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardCream), border = BorderStroke(1.2.dp, GoldAccent)) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SettingSwitchCard(title: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardCream)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    }
}
'''

with open(f"{base}/QuranRepository.kt", "w", encoding="utf-8") as f:
    f.write(data_kt.strip() + "\n")

with open(f"{base}/MainActivity.kt", "w", encoding="utf-8") as f:
    f.write(main_kt.strip() + "\n")
