package com.quran.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import java.util.Calendar

// لوحة الألوان الإسلامية الملكية
val EmeraldDeep = Color(0xFF042B20)
val EmeraldDark = Color(0xFF073E2E)
val EmeraldMid = Color(0xFF0E5C45)
val GoldBorder = Color(0xFFD4AF37)
val GoldBright = Color(0xFFF2CC59)
val GoldLight = Color(0xFFF9EED0)
val CreamCard = Color(0xFFFBF8F1)
val PageBg = Color(0xFFF4F0E6)
val TextPrimary = Color(0xFF10261E)
val TextSecondary = Color(0xFF536960)
val PrayerPurpleStart = Color(0xFF473388)
val PrayerPurpleEnd = Color(0xFF684EB3)
val AyahTealStart = Color(0xFF09585D)
val AyahTealEnd = Color(0xFF147B80)

// نظام خط عريض وواضح لكامل التطبيق
val RoyalBoldTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

data class Surah(val num: Int, val name: String, val type: String, val ayahs: Int, val page: Int)
data class Reciter(val id: String, val name: String, val style: String, val url: String)
data class HadithItem(val title: String, val text: String, val source: String)
data class DhikrItem(val title: String, val text: String, val target: Int)

object QuranRepository {
    // روابط القنوات الرسمية (يمكنك تعديل الروابط لاحقاً إلى روابط قنواتك الخاصة)
    const val YOUTUBE_OFFICIAL_URL = "https://www.youtube.com"
    const val TELEGRAM_OFFICIAL_URL = "https://t.me/xc25bb"

    val reciters = listOf(
        Reciter("afs", "مشاري العفاسي", "مرتل • حفص", "https://server8.mp3quran.net/afs"),
        Reciter("basit", "عبد الباسط عبد الصمد", "مجود • حفص", "https://server7.mp3quran.net/basit"),
        Reciter("maher", "ماهر المعيقلي", "مرتل • حفص", "https://server12.mp3quran.net/maher"),
        Reciter("yasser", "ياسر الدوسري", "مرتل • حفص", "https://server11.mp3quran.net/yasser"),
        Reciter("minsh", "محمد صديق المنشاوي", "مرتل • حفص", "https://server10.mp3quran.net/minsh"),
        Reciter("sudais", "عبد الرحمن السديس", "مرتل • حفص", "https://server11.mp3quran.net/sds")
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

    val dailyVerses = listOf(
        Triple("﴿ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴾", "سورة الشرح • آية 6", 596),
        Triple("﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾", "سورة الرعد • آية 28", 252),
        Triple("﴿ وَقُل رَّبِّ زِدْنِي عِلْمًا ﴾", "سورة طه • آية 114", 320),
        Triple("﴿ فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ ﴾", "سورة البقرة • آية 186", 29),
        Triple("﴿ وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ ﴾", "سورة الضحى • آية 5", 596)
    )

    val hadiths = listOf(
        HadithItem("الأعمال بالنيات", "«إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى»", "متفق عليه"),
        HadithItem("خيركم من تعلم القرآن", "«خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ»", "رواه البخاري"),
        HadithItem("شفاعة القرآن", "«اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لأَصْحَابِهِ»", "رواه مسلم"),
        HadithItem("حسن الإسلام", "«مِنْ حُسْنِ إِسْلامِ الْمَرْءِ تَرْكُهُ مَا لا يَعْنِيهِ»", "رواه الترمذي")
    )

    val adhkar = listOf(
        DhikrItem("سيد الاستغفار", "اللَّهُمَّ أَنْتَ رَبِّي لا إِلَهَ إِلا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ لَكَ بِذَنْبِي فَاغْفِرْ لِي فَإِنَّهُ لا يَغْفِرُ الذُّنُوبَ إِلا أَنْتَ", 1),
        DhikrItem("ذكر الحفظ والتحصين", "بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ", 3),
        DhikrItem("الباقيات الصالحات", "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلا إِلَهَ إِلا اللَّهُ، وَاللَّهُ أَكْبَرُ", 33),
        DhikrItem("الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ", 10)
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.statusBarColor = EmeraldDeep.toArgb()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = EmeraldDark,
                        secondary = GoldBorder,
                        background = PageBg
                    ),
                    typography = RoyalBoldTypography
                ) {
                    QuranMasterApp()
                }
            }
        }
    }
}

@Composable
fun QuranMasterApp() {
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { context.getSharedPreferences("quran_royal_v5", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf("home") }
    var searchQuery by remember { mutableStateOf("") }
    var activePage by remember { mutableIntStateOf(prefs.getInt("last_page", 1)) }
    var pagesReadCount by remember { mutableIntStateOf(prefs.getInt("pages_read", 1)) }
    var totalTasbih by remember { mutableIntStateOf(prefs.getInt("total_tasbih", 0)) }
    var favPages by remember {
        mutableStateOf(
            prefs.getString("fav_pages", "1,293,562")?.split(",")?.mapNotNull { it.toIntOrNull() }?.toSet()
                ?: setOf(1, 293, 562)
        )
    }

    var nightMode by remember { mutableStateOf(prefs.getBoolean("night_mode", false)) }
    var soundOn by remember { mutableStateOf(prefs.getBoolean("sound_on", true)) }
    var vibOn by remember { mutableStateOf(prefs.getBoolean("vib_on", true)) }
    var keepOn by remember { mutableStateOf(prefs.getBoolean("keep_on", true)) }
    var dailyReminderOn by remember { mutableStateOf(prefs.getBoolean("daily_reminder", true)) }

    // طلب إذن الإشعارات الحديث (Android 13+)
    var hasNotifPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPermission = granted
    }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // حالات المشغل الصوتي المتطور
    var selectedReciter by remember { mutableStateOf(QuranRepository.reciters.first()) }
    var playingSurah by remember { mutableStateOf<Surah?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var repeatOne by remember { mutableStateOf(false) }
    var autoPlayNext by remember { mutableStateOf(true) }
    var sleepMinutesLeft by remember { mutableIntStateOf(0) }
    var downloadingNum by remember { mutableIntStateOf(-1) }

    // النوافذ المنبثقة
    var showNotifications by remember { mutableStateOf(false) }
    var showGiftDialog by remember { mutableStateOf(false) }
    var showFollowDialog by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }
    val scope = rememberCoroutineScope()

    fun clickFx() { if (soundOn) view.playSoundEffect(SoundEffectConstants.CLICK) }
    fun vibrateFx() {
        if (!vibOn) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                (context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(35)
            }
        }
    }

    fun openExternalUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun playSurahAudio(surah: Surah) {
        scope.launch {
            val file = File(context.filesDir, "${selectedReciter.id}_${surah.num}.mp3")
            val ready = file.exists() && file.length() > 1000
            if (!ready) {
                downloadingNum = surah.num
                withContext(Dispatchers.IO) {
                    runCatching {
                        val url = URL("${selectedReciter.url}/${surah.num.toString().padStart(3, '0')}.mp3")
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
                    mediaPlayer.setAudioAttributes(
                        AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
                    )
                    mediaPlayer.setDataSource(file.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                    playingSurah = surah
                    isPlaying = true
                    durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L)
                    currentPosMs = 0L
                }
            }
        }
    }

    LaunchedEffect(isPlaying, playingSurah) {
        while (isPlaying) {
            runCatching {
                if (mediaPlayer.isPlaying) {
                    currentPosMs = mediaPlayer.currentPosition.toLong()
                    durationMs = mediaPlayer.duration.toLong().coerceAtLeast(1L)
                } else if (currentPosMs > 0 && currentPosMs >= durationMs - 800) {
                    val cur = playingSurah
                    if (cur != null) {
                        if (repeatOne) {
                            mediaPlayer.seekTo(0)
                            mediaPlayer.start()
                        } else if (autoPlayNext && cur.num < 114) {
                            val next = QuranRepository.surahs.first { it.num == cur.num + 1 }
                            playSurahAudio(next)
                        } else {
                            isPlaying = false
                        }
                    }
                }
            }
            delay(500)
        }
    }

    LaunchedEffect(sleepMinutesLeft) {
        if (sleepMinutesLeft > 0) {
            delay(60_000L)
            sleepMinutesLeft -= 1
            if (sleepMinutesLeft == 0 && mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            }
        }
    }

    LaunchedEffect(keepOn, screen) {
        val window = (context as? Activity)?.window
        if (keepOn && screen == "reader") window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(Unit) { onDispose { runCatching { mediaPlayer.release() } } }

    BackHandler(enabled = screen != "home") {
        clickFx()
        screen = if (screen == "reader") "index" else "home"
    }

    val dayIndex = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % QuranRepository.dailyVerses.size }
    val todayAyah = QuranRepository.dailyVerses[dayIndex]

    Scaffold(
        modifier = Modifier.fillMaxSize().systemBarsPadding(),
        containerColor = PageBg,
        bottomBar = {
            if (screen != "reader") {
                Column {
                    // مشغل مصغر عائم ناعم
                    if (playingSurah != null && screen != "audio") {
                        Surface(
                            color = EmeraldDeep,
                            border = BorderStroke(1.dp, GoldBorder),
                            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                            modifier = Modifier.fillMaxWidth().clickable { clickFx(); screen = "audio" }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.GraphicEq, null, tint = GoldBright, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text("سورة ${playingSurah!!.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                        Text(selectedReciter.name, color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                                Row {
                                    IconButton(onClick = {
                                        clickFx()
                                        if (mediaPlayer.isPlaying) { mediaPlayer.pause(); isPlaying = false }
                                        else { mediaPlayer.start(); isPlaying = true }
                                    }, modifier = Modifier.size(34.dp)) {
                                        Icon(if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, null, tint = GoldBright, modifier = Modifier.size(26.dp))
                                    }
                                    IconButton(onClick = {
                                        clickFx()
                                        runCatching { mediaPlayer.stop() }
                                        isPlaying = false
                                        playingSurah = null
                                    }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Close, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }

                    // شريط التنقل السفلي المقوس بأزرار مدمجة وناعمة
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                            .background(EmeraldDeep)
                            .border(BorderStroke(1.4.dp, GoldBorder), RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                            .padding(vertical = 5.dp, horizontal = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BottomNavTab("الصفحات", Icons.Default.Bookmark, screen == "reader") { clickFx(); screen = "reader" }
                            BottomNavTab("السور", Icons.AutoMirrored.Filled.MenuBook, screen == "index") { clickFx(); screen = "index" }

                            // زر الرئيسية المركزي البارز بحجم مدمج وأنيق
                            Surface(
                                onClick = { clickFx(); screen = "home" },
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                                color = if (screen == "home") EmeraldMid else EmeraldDark,
                                border = BorderStroke(1.8.dp, GoldBright),
                                modifier = Modifier.size(width = 68.dp, height = 54.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.Home, null, tint = GoldBright, modifier = Modifier.size(22.dp))
                                    Text("الرئيسية", color = GoldBright, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            BottomNavTab("المزيد", Icons.Default.GridView, screen == "more") { clickFx(); screen = "more" }
                            BottomNavTab("الإحصائيات", Icons.Default.BarChart, screen == "stats") { clickFx(); screen = "stats" }
                        }
                    }
                }
            }
        }
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().padding(pad)) {
            when (screen) {
                "home" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.verticalGradient(listOf(EmeraldDeep, EmeraldDark, PageBg, PageBg))
                        ),
                        contentPadding = PaddingValues(bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. الترويسة الإسلامية الملكية بحواف سفلية ناعمة
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                    .background(Brush.verticalGradient(listOf(EmeraldDeep, EmeraldMid)))
                                    .border(BorderStroke(1.5.dp, GoldBorder), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // زر التنبيهات الحديث بعدّاد
                                        Box {
                                            Surface(
                                                onClick = { clickFx(); showNotifications = true },
                                                shape = CircleShape,
                                                color = EmeraldDeep,
                                                border = BorderStroke(1.3.dp, GoldBorder),
                                                modifier = Modifier.size(38.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Notifications, null, tint = GoldLight, modifier = Modifier.size(20.dp))
                                                }
                                            }
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFD32F2F),
                                                modifier = Modifier.size(16.dp).align(Alignment.TopEnd)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("3", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }

                                        // شارة حالة الحفظ والختمة
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = EmeraldDeep.copy(alpha = 0.7f),
                                            border = BorderStroke(1.dp, GoldBorder.copy(alpha = 0.6f))
                                        ) {
                                            Text(
                                                "وصلت إلى صفحة $activePage",
                                                color = GoldLight,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                            )
                                        }

                                        // زر الإعدادات المدمج
                                        Surface(
                                            onClick = { clickFx(); screen = "settings" },
                                            shape = CircleShape,
                                            color = EmeraldDeep,
                                            border = BorderStroke(1.3.dp, GoldBorder),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Settings, null, tint = GoldLight, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(6.dp))
                                    Text("القرآن الكريم", color = GoldLight, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("— نورٌ قلبك .. وراحة روحك —", color = GoldBright, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(Modifier.height(6.dp))
                                }
                            }
                        }

                        // 2. شريط البحث العائم بحواف ناعمة ومقاس مدمج
                        item {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = CreamCard,
                                border = BorderStroke(1.4.dp, GoldBorder),
                                shadowElevation = 3.dp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = EmeraldDark, modifier = Modifier.size(20.dp).clickable { clickFx(); screen = "index" })
                                    Spacer(Modifier.width(8.dp))
                                    VerticalDivider(modifier = Modifier.height(20.dp), color = GoldBorder)
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("ابحث في القرآن الكريم...", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { clickFx(); screen = "index" }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Search, null, tint = EmeraldDeep, modifier = Modifier.size(22.dp))
                                    }
                                }
                            }
                        }

                        // 3. الشبكة السداسية (صف 1) بأزرار مصغرة وحواف ناعمة
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                FeatureTileCard("القرآن كاملاً", "بجودة عالية", Icons.AutoMirrored.Filled.MenuBook, EmeraldDark, Modifier.weight(1f)) {
                                    clickFx(); screen = "reader"
                                }
                                FeatureTileCard("قراءة القرآن", "بكل سهولة", Icons.Default.AutoStories, GoldBorder, Modifier.weight(1f)) {
                                    clickFx(); screen = "index"
                                }
                                FeatureTileCard("المكتبة الصوتية", "استمع للقرآن", Icons.Default.Mosque, EmeraldMid, Modifier.weight(1f)) {
                                    clickFx(); screen = "audio"
                                }
                            }
                        }

                        // 4. الشبكة السداسية (صف 2)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                FeatureTileCard("وضع الليل", "راحة لعينيك", Icons.Default.NightlightRound, Color(0xFF1A2A4A), Modifier.weight(1f)) {
                                    clickFx(); nightMode = !nightMode; prefs.edit().putBoolean("night_mode", nightMode).apply(); screen = "reader"
                                }
                                FeatureTileCard("المفضلة", "سورك المفضلة", Icons.Default.Bookmark, EmeraldDark, Modifier.weight(1f)) {
                                    clickFx(); screen = "favorites"
                                }
                                FeatureTileCard("الأذكار اليومية", "طمأنينة دائمة", Icons.Default.TouchApp, Color(0xFF795548), Modifier.weight(1f)) {
                                    clickFx(); screen = "adhkar"
                                }
                            }
                        }

                        // 5. بطاقتا آية اليوم ومواقيت الصلاة بحواف دائرية ناعمة
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Surface(
                                    onClick = { clickFx(); activePage = todayAyah.third; screen = "reader" },
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.3.dp, GoldBorder),
                                    modifier = Modifier.weight(1f).height(94.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(AyahTealStart, AyahTealEnd))).padding(11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("آية اليوم", color = GoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                            Spacer(Modifier.height(3.dp))
                                            Text(todayAyah.first, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = AyahTealStart, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                Surface(
                                    onClick = { clickFx(); screen = "prayer" },
                                    shape = RoundedCornerShape(22.dp),
                                    border = BorderStroke(1.3.dp, GoldBorder),
                                    modifier = Modifier.weight(1f).height(94.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(PrayerPurpleStart, PrayerPurpleEnd))).padding(11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("مواقيت الصلاة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                            Spacer(Modifier.height(3.dp))
                                            Text("في مدينتك", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                        Icon(Icons.Default.Schedule, null, tint = GoldBright, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }

                        // 6. شريط الخدمات السريعة السفلي (مع زر تابعنا الجديد بدل الدعم الفني)
                        item {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = CreamCard,
                                border = BorderStroke(1.3.dp, GoldBorder),
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    QuickUtilityItem("الهدايا اليومية", Icons.Default.CardGiftcard) { clickFx(); showGiftDialog = true }
                                    QuickUtilityItem("تابعنا", Icons.Default.Public) { clickFx(); showFollowDialog = true }
                                    QuickUtilityItem("قيّم التطبيق", Icons.Default.Star) { clickFx(); vibrateFx() }
                                    QuickUtilityItem("مشاركة التطبيق", Icons.Default.Share) {
                                        clickFx()
                                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, "تطبيق القرآن الكريم الشامل - تلاوات ومصحف وأذكار")
                                        }, "مشاركة التطبيق"))
                                    }
                                }
                            }
                        }
                    }
                }

                // شاشة فهرس السور
                "index" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("فهرس سور القرآن (114)") { clickFx(); screen = "home" }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldDark) },
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                        LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QuranRepository.surahs.filter { it.name.contains(searchQuery.trim()) || it.num.toString() == searchQuery.trim() }) { s ->
                                Surface(
                                    onClick = { clickFx(); activePage = s.page; screen = "reader" },
                                    shape = RoundedCornerShape(18.dp),
                                    color = CreamCard,
                                    border = BorderStroke(1.dp, GoldBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = CircleShape, color = EmeraldDark, modifier = Modifier.size(32.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text("${s.num}", color = GoldBright, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                                }
                                            }
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextPrimary)
                                                Text("${s.type} • ${s.ayahs} آية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                            }
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("ص ${s.page}", color = EmeraldMid, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                            IconButton(onClick = { clickFx(); playSurahAudio(s); screen = "audio" }, modifier = Modifier.size(34.dp)) {
                                                Icon(Icons.Default.PlayCircleOutline, null, tint = GoldBorder, modifier = Modifier.size(22.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // قارئ المصحف المصور (604 صفحات)
                "reader" -> {
                    val pagerState = rememberPagerState(initialPage = (activePage - 1).coerceIn(0, 603), pageCount = { 604 })
                    val curPage = pagerState.currentPage + 1
                    LaunchedEffect(curPage) {
                        if (activePage != curPage) {
                            pagesReadCount += 1
                            prefs.edit().putInt("pages_read", pagesReadCount).apply()
                        }
                        activePage = curPage
                        prefs.edit().putInt("last_page", curPage).apply()
                    }
                    val invert = remember { ColorMatrix(floatArrayOf(-1f,0f,0f,0f,255f, 0f,-1f,0f,0f,255f, 0f,0f,-1f,0f,255f, 0f,0f,0f,1f,0f)) }
                    Column(modifier = Modifier.fillMaxSize().background(if (nightMode) Color.Black else Color(0xFFF6EFE0))) {
                        Surface(color = EmeraldDeep) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { clickFx(); screen = "home" }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                                }
                                Text("صفحة $curPage من 604", color = GoldLight, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                Row {
                                    IconButton(onClick = {
                                        clickFx()
                                        favPages = if (favPages.contains(curPage)) favPages - curPage else favPages + curPage
                                        prefs.edit().putString("fav_pages", favPages.joinToString(",")).apply()
                                    }) {
                                        Icon(if (favPages.contains(curPage)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, null, tint = GoldBright)
                                    }
                                    IconButton(onClick = {
                                        clickFx(); nightMode = !nightMode; prefs.edit().putBoolean("night_mode", nightMode).apply()
                                    }) {
                                        Icon(if (nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, null, tint = GoldBright)
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
                                loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldBorder) } }
                            )
                        }
                    }
                }

                // المكتبة الصوتية والمشغل المتطور
                "audio" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("المكتبة الصوتية المتطورة") { clickFx(); screen = "home" }

                        LazyRow(contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QuranRepository.reciters) { r ->
                                FilterChip(
                                    selected = r.id == selectedReciter.id,
                                    onClick = { clickFx(); selectedReciter = r },
                                    shape = RoundedCornerShape(16.dp),
                                    label = { Text(r.name, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldDark, selectedLabelColor = GoldLight)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = EmeraldDeep,
                            border = BorderStroke(1.5.dp, GoldBorder),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = playingSurah?.let { "سورة ${it.name}" } ?: "اختر سورة لبدء التلاوة",
                                    color = GoldLight, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold
                                )
                                Text(selectedReciter.name + " • " + selectedReciter.style, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                                Spacer(Modifier.height(6.dp))

                                Slider(
                                    value = if (durationMs > 0) (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f,
                                    onValueChange = { frac ->
                                        if (playingSurah != null && durationMs > 0) {
                                            val newPos = (frac * durationMs).toLong()
                                            currentPosMs = newPos
                                            runCatching { mediaPlayer.seekTo(newPos.toInt()) }
                                        }
                                    },
                                    colors = SliderDefaults.colors(thumbColor = GoldBright, activeTrackColor = GoldBright, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(formatTimeMs(currentPosMs), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    Text(formatTimeMs(durationMs), color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = {
                                        clickFx()
                                        playingSurah?.let { if (it.num > 1) playSurahAudio(QuranRepository.surahs.first { s -> s.num == it.num - 1 }) }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipNext, null, tint = Color.White) }

                                    IconButton(onClick = {
                                        clickFx()
                                        runCatching { mediaPlayer.seekTo((mediaPlayer.currentPosition - 10000).coerceAtLeast(0)) }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Replay10, null, tint = GoldLight) }

                                    Surface(
                                        onClick = {
                                            clickFx()
                                            if (playingSurah == null) playSurahAudio(QuranRepository.surahs.first())
                                            else if (mediaPlayer.isPlaying) { mediaPlayer.pause(); isPlaying = false }
                                            else { mediaPlayer.start(); isPlaying = true }
                                        },
                                        shape = CircleShape,
                                        color = GoldBright,
                                        modifier = Modifier.size(46.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = EmeraldDeep, modifier = Modifier.size(28.dp))
                                        }
                                    }

                                    IconButton(onClick = {
                                        clickFx()
                                        runCatching { mediaPlayer.seekTo((mediaPlayer.currentPosition + 10000).coerceAtMost(mediaPlayer.duration)) }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Forward10, null, tint = GoldLight) }

                                    IconButton(onClick = {
                                        clickFx()
                                        playingSurah?.let { if (it.num < 114) playSurahAudio(QuranRepository.surahs.first { s -> s.num == it.num + 1 }) }
                                    }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White) }
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    TextButton(onClick = { clickFx(); repeatOne = !repeatOne }) {
                                        Icon(Icons.Default.RepeatOne, null, tint = if (repeatOne) GoldBright else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("تكرار السورة", color = if (repeatOne) GoldBright else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    TextButton(onClick = { clickFx(); autoPlayNext = !autoPlayNext }) {
                                        Icon(Icons.Default.PlaylistPlay, null, tint = if (autoPlayNext) GoldBright else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("ختمة متتالية", color = if (autoPlayNext) GoldBright else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                    TextButton(onClick = {
                                        clickFx()
                                        sleepMinutesLeft = when (sleepMinutesLeft) { 0 -> 15; 15 -> 30; 30 -> 60; else -> 0 }
                                    }) {
                                        Icon(Icons.Default.Bedtime, null, tint = if (sleepMinutesLeft > 0) GoldBright else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (sleepMinutesLeft > 0) "نوم: ${sleepMinutesLeft}د" else "مؤقت نوم", color = if (sleepMinutesLeft > 0) GoldBright else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(QuranRepository.surahs) { s ->
                                val file = File(context.filesDir, "${selectedReciter.id}_${s.num}.mp3")
                                val ready = file.exists() && file.length() > 1000
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = CreamCard,
                                    border = BorderStroke(1.dp, if (playingSurah?.num == s.num) EmeraldDark else GoldBorder)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(11.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${s.num}. سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
                                            Text(if (ready) "محملة على الجهاز (بدون إنترنت)" else "اضغط للتنزيل والاستماع", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                        }
                                        if (downloadingNum == s.num) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EmeraldDark)
                                        } else {
                                            Button(
                                                onClick = { clickFx(); playSurahAudio(s) },
                                                shape = RoundedCornerShape(14.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (ready) EmeraldDark else GoldBorder,
                                                    contentColor = if (ready) GoldLight else EmeraldDeep
                                                )
                                            ) { Text(if (ready) "تشغيل" else "تنزيل", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // الأذكار والمسبحة
                "adhkar" -> {
                    var tasbih by remember { mutableIntStateOf(0) }
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("الأذكار والمسبحة الإلكترونية") { clickFx(); screen = "home" }
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            item {
                                Surface(
                                    onClick = {
                                        clickFx(); vibrateFx(); tasbih++; totalTasbih++
                                        prefs.edit().putInt("total_tasbih", totalTasbih).apply()
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    color = EmeraldDark,
                                    border = BorderStroke(1.8.dp, GoldBorder)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("اضغط هنا للتسبيح", color = GoldLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("$tasbih", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
                                        TextButton(onClick = { clickFx(); tasbih = 0 }) { Text("تصفير العداد", color = GoldBright, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                            items(QuranRepository.adhkar) { d ->
                                var c by remember { mutableIntStateOf(0) }
                                Surface(
                                    onClick = { clickFx(); vibrateFx(); if (c < d.target) c++ },
                                    shape = RoundedCornerShape(20.dp),
                                    color = CreamCard,
                                    border = BorderStroke(1.dp, GoldBorder)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(d.title, color = EmeraldMid, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(d.text, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 24.sp)
                                        Spacer(Modifier.height(8.dp))
                                        Text("التكرار: $c / ${d.target}", color = if (c >= d.target) GoldBorder else EmeraldDark, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // المفضلة
                "favorites" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("الصفحات المحفوظة في المفضلة") { clickFx(); screen = "home" }
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(favPages.sorted()) { p ->
                                Surface(
                                    onClick = { clickFx(); activePage = p; screen = "reader" },
                                    shape = RoundedCornerShape(18.dp),
                                    color = CreamCard,
                                    border = BorderStroke(1.dp, GoldBorder)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("صفحة رقم $p من المصحف الشريف", fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                                        Text("فتح الصفحة", color = EmeraldMid, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // مواقيت الصلاة
                "prayer" -> {
                    val prayers = listOf(
                        Pair("الفجر", "04:52 ص"), Pair("الشروق", "06:18 ص"), Pair("الظهر", "11:42 ص"),
                        Pair("العصر", "02:38 م"), Pair("المغرب", "05:06 م"), Pair("العشاء", "06:28 م")
                    )
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("مواقيت الصلاة") { clickFx(); screen = "home" }
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(prayers) { (name, time) ->
                                Surface(shape = RoundedCornerShape(18.dp), color = CreamCard, border = BorderStroke(1.dp, GoldBorder)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = TextPrimary)
                                        Text(time, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = EmeraldDark)
                                    }
                                }
                            }
                        }
                    }
                }

                // المزيد (الأحاديث النبوية)
                "more" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("الأحاديث النبوية الشريفة") { clickFx(); screen = "home" }
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(QuranRepository.hadiths) { h ->
                                Surface(shape = RoundedCornerShape(20.dp), color = CreamCard, border = BorderStroke(1.dp, GoldBorder)) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(h.title, fontWeight = FontWeight.ExtraBold, color = EmeraldMid)
                                        Spacer(Modifier.height(6.dp))
                                        Text(h.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary, lineHeight = 25.sp)
                                        Spacer(Modifier.height(6.dp))
                                        Text(h.source, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }

                // الإحصائيات
                "stats" -> {
                    val downloadedSurahs = context.filesDir.listFiles()?.count { it.name.endsWith(".mp3") } ?: 0
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("إحصائيات الختمة والعبادة") { clickFx(); screen = "home" }
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StatCard("آخر صفحة وصلت إليها", "صفحة $activePage من 604")
                            StatCard("إجمالي الصفحات المقروءة", "$pagesReadCount صفحة")
                            StatCard("مجموع التسبيحات", "$totalTasbih تسبيحة")
                            StatCard("السور المحملة صوتياً", "$downloadedSurahs سورة")
                        }
                    }
                }

                // الإعدادات والأذونات
                "settings" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        TopHeaderBar("الإعدادات والأذونات") { clickFx(); screen = "home" }
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingToggle("صوت النقرات والتفاعل", soundOn) { soundOn = it; prefs.edit().putBoolean("sound_on", it).apply(); clickFx() }
                            SettingToggle("الاهتزاز عند التسبيح", vibOn) { vibOn = it; prefs.edit().putBoolean("vib_on", it).apply(); clickFx() }
                            SettingToggle("الوضع الليلي للمصحف", nightMode) { nightMode = it; prefs.edit().putBoolean("night_mode", it).apply(); clickFx() }
                            SettingToggle("إبقاء الشاشة مضاءة أثناء القراءة", keepOn) { keepOn = it; prefs.edit().putBoolean("keep_on", it).apply(); clickFx() }
                            SettingToggle("إشعارات التذكير بالورد اليومي", dailyReminderOn) {
                                dailyReminderOn = it
                                prefs.edit().putBoolean("daily_reminder", it).apply()
                                if (it && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotifPermission) {
                                    notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                clickFx()
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة تابعنا (يوتيوب وتلجرام)
    if (showFollowDialog) {
        AlertDialog(
            onDismissRequest = { showFollowDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = CreamCard,
            title = {
                Text(
                    "تابعنا عبر المنصات الرسمية",
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldDeep,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "انضم إلى قنواتنا الرسمية لمتابعة التلاوات والتحديثات اليومية:",
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    // زر يوتيوب الرسمي
                    Surface(
                        onClick = {
                            clickFx()
                            showFollowDialog = false
                            openExternalUrl(QuranRepository.YOUTUBE_OFFICIAL_URL)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFF0000),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("قناتنا على يوتيوب (YouTube)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }

                    // زر تلجرام الرسمي
                    Surface(
                        onClick = {
                            clickFx()
                            showFollowDialog = false
                            openExternalUrl(QuranRepository.TELEGRAM_OFFICIAL_URL)
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF0088CC),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("قناتنا على تلجرام (Telegram)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFollowDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
                }
            }
        )
    }

    // نافذة التنبيهات الحديثة
    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = CreamCard,
            title = { Text("التنبيهات والإشعارات اليومية", fontWeight = FontWeight.ExtraBold, color = EmeraldDeep) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• لا تنسَ قراءة وردك اليومي من القرآن الكريم.", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• قراءة سورة الملك قبل النوم منجية.", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("• أذكار الصباح والمساء حصن المسلم.", fontWeight = FontWeight.Bold, color = TextPrimary)
                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Button(
                            onClick = { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                        ) {
                            Text("تفعيل إذن الإشعارات", color = GoldLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotifications = false }) { Text("حسناً", fontWeight = FontWeight.ExtraBold) } }
        )
    }

    if (showGiftDialog) {
        AlertDialog(
            onDismissRequest = { showGiftDialog = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = CreamCard,
            title = { Text("هدية اليوم", fontWeight = FontWeight.ExtraBold, color = EmeraldDeep) },
            text = {
                Text(
                    "قال رسول الله ﷺ: «من قرأ حرفاً من كتاب الله فله به حسنة، والحسنة بعشر أمثالها».\n\nصفحتك المقترحة الآن: صفحة $activePage",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            confirmButton = {
                Button(
                    onClick = { showGiftDialog = false; screen = "reader" },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDark)
                ) { Text("افتح المصحف الآن", color = GoldLight, fontWeight = FontWeight.ExtraBold) }
            }
        )
    }
}

// بطاقة الشبكة السداسية بحجم مدمج وحواف ناعمة
@Composable
fun FeatureTileCard(title: String, subtitle: String, icon: ImageVector, iconColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = CreamCard,
        border = BorderStroke(1.3.dp, GoldBorder),
        shadowElevation = 2.dp,
        modifier = modifier.height(114.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = iconColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(24.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(subtitle, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Surface(shape = CircleShape, color = GoldBright, modifier = Modifier.size(19.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickUtilityItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Icon(icon, contentDescription = title, tint = GoldBorder, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(3.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
    }
}

@Composable
fun BottomNavTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable { onClick() }.padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) GoldBright else GoldLight.copy(alpha = 0.85f), modifier = Modifier.size(20.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) GoldBright else Color.White)
    }
}

@Composable
fun TopHeaderBar(title: String, onBack: () -> Unit) {
    Surface(color = EmeraldDeep, shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White) }
            Text(title, color = GoldLight, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = CreamCard, border = BorderStroke(1.dp, GoldBorder)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(value, fontWeight = FontWeight.ExtraBold, color = EmeraldDark)
        }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = CreamCard, border = BorderStroke(1.dp, GoldBorder)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = TextPrimary)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%02d:%02d", min, sec)
}
