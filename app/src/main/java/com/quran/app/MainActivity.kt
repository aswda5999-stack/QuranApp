@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import kotlin.reflect.KProperty
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ============================================================
// data/QuranData.kt
// ============================================================
data class Surah(val num: Int, val name: String, val type: String, val ayahs: Int, val page: Int)
data class Reciter(val id: String, val name: String, val style: String, val url: String)
data class HadithItem(val title: String, val text: String, val source: String)
data class DhikrItem(val title: String, val text: String, val target: Int)
data class TasbihStage(val phrase: String, val subtitle: String, val defaultTarget: Int)
data class DailyVerse(val text: String, val ref: String, val page: Int)
data class City(val name: String, val lat: Double, val lng: Double)

/** يوحّد الحروف العربية للبحث (إزالة التشكيل وتوحيد الألف والياء والهاء). */
fun String.normalizeAr(): String = this.trim()
    .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    .replace('ى', 'ي').replace('ة', 'ه')

object QuranData {
    // ضع روابط قنواتك الحقيقية هنا
    const val YOUTUBE_URL = "https://www.youtube.com"
    const val TELEGRAM_URL = "https://t.me"

    val tasbihStages = listOf(
        TasbihStage("سُبْحَانَ اللَّهِ", "تنزيه الله عن كل نقص", 33),
        TasbihStage("الْحَمْدُ لِلَّهِ", "الشكر والثناء لله تعالى", 33),
        TasbihStage("اللَّهُ أَكْبَرُ", "الله أعظم من كل شيء", 33),
        TasbihStage("لَا إِلَهَ إِلَّا اللَّهُ", "كلمة التوحيد الخالصة", 10),
        TasbihStage("أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ", "طلب المغفرة والرحمة", 100),
        TasbihStage("اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", "الصلاة على رسول الله ﷺ", 10)
    )

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

    /** أول صفحة لكل جزء (30 جزءاً). */
    val juzStart: List<Int> = List(30) { if (it == 0) 1 else 2 + 20 * it }

    fun juzOfPage(page: Int): Int = if (page <= 21) 1 else ((page - 2) / 20 + 1).coerceAtMost(30)

    fun surahForPage(page: Int): Surah = surahs.lastOrNull { it.page <= page } ?: surahs.first()

    fun surah(num: Int): Surah? = surahs.firstOrNull { it.num == num }

    fun search(q: String): List<Surah> {
        val n = q.normalizeAr().removePrefix("سوره").trim()
        if (n.isEmpty()) return surahs
        val num = n.toIntOrNull()
        return surahs.filter { it.name.normalizeAr().contains(n) || (num != null && it.num == num) }
    }

    val dailyVerses = listOf(
        DailyVerse("﴿ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴾", "سورة الشرح • آية 6", 596),
        DailyVerse("﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾", "سورة الرعد • آية 28", 252),
        DailyVerse("﴿ وَقُل رَّبِّ زِدْنِي عِلْمًا ﴾", "سورة طه • آية 114", 320),
        DailyVerse("﴿ فَإِنِّي قَرِيبٌ أُجِيبُ دَعْوَةَ الدَّاعِ إِذَا دَعَانِ ﴾", "سورة البقرة • آية 186", 29),
        DailyVerse("﴿ وَلَسَوْفَ يُعْطِيكَ رَبُّكَ فَتَرْضَىٰ ﴾", "سورة الضحى • آية 5", 596)
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
        DhikrItem("الرضا بالله", "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالإِسْلامِ دِينًا، وَبِمُحَمَّدٍ ﷺ نَبِيًّا", 3),
        DhikrItem("حسبي الله", "حَسْبِيَ اللَّهُ لا إِلَهَ إِلا هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ", 7),
        DhikrItem("الباقيات الصالحات", "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَلا إِلَهَ إِلا اللَّهُ، وَاللَّهُ أَكْبَرُ", 33),
        DhikrItem("الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ", 10)
    )

    val cities = listOf(
        City("القدس", 31.7683, 35.2137), City("غزة", 31.5017, 34.4668), City("رام الله", 31.9038, 35.2034),
        City("عمّان", 31.9454, 35.9284), City("دمشق", 33.5138, 36.2765), City("بيروت", 33.8938, 35.5018),
        City("بغداد", 33.3152, 44.3661), City("القاهرة", 30.0444, 31.2357), City("الرياض", 24.7136, 46.6753),
        City("مكة المكرمة", 21.4225, 39.8262), City("المدينة المنورة", 24.4686, 39.6142), City("دبي", 25.2048, 55.2708),
        City("الدوحة", 25.2854, 51.5310), City("الكويت", 29.3759, 47.9774), City("إسطنبول", 41.0082, 28.9784),
        City("الدار البيضاء", 33.5731, -7.5898), City("الجزائر", 36.7538, 3.0588), City("تونس", 36.8065, 10.1815),
        City("الخرطوم", 15.5007, 32.5599), City("صنعاء", 15.3694, 44.1910)
    )

    fun pageUrl(page: Int) = "https://android.quran.com/data/width_1024/page${page.toString().padStart(3, '0')}.png"
}

// ============================================================
// data/AppState.kt
// ============================================================
const val PREFS_NAME = "quran_royal_v7"

data class GeoLoc(val name: String, val lat: Double, val lng: Double, val isDefault: Boolean)

fun SharedPreferences.geoLoc(): GeoLoc {
    val lat = getString("loc_lat", null)?.toDoubleOrNull()
    val lng = getString("loc_lng", null)?.toDoubleOrNull()
    val name = getString("loc_name", "") ?: ""
    return if (lat != null && lng != null && name.isNotBlank()) GeoLoc(name, lat, lng, false)
    else GeoLoc("مكة المكرمة (افتراضي)", 21.4225, 39.8262, true)
}

class PrefBool(private val sp: SharedPreferences, private val key: String, def: Boolean) {
    private var v by mutableStateOf(sp.getBoolean(key, def))
    operator fun getValue(t: Any?, p: KProperty<*>): Boolean = v
    operator fun setValue(t: Any?, p: KProperty<*>, n: Boolean) { v = n; sp.edit().putBoolean(key, n).apply() }
}

class PrefInt(private val sp: SharedPreferences, private val key: String, def: Int) {
    private var v by mutableIntStateOf(sp.getInt(key, def))
    operator fun getValue(t: Any?, p: KProperty<*>): Int = v
    operator fun setValue(t: Any?, p: KProperty<*>, n: Int) { v = n; sp.edit().putInt(key, n).apply() }
}

class PrefString(private val sp: SharedPreferences, private val key: String, def: String) {
    private var v by mutableStateOf(sp.getString(key, def) ?: def)
    operator fun getValue(t: Any?, p: KProperty<*>): String = v
    operator fun setValue(t: Any?, p: KProperty<*>, n: String) { v = n; sp.edit().putString(key, n).apply() }
}

private fun parseSet(s: String?): Set<Int> =
    s?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet() ?: emptySet()

class AppState(ctx: Context) {
    private val app = ctx.applicationContext
    val sp: SharedPreferences = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // الإعدادات
    var nightMode by PrefBool(sp, "night_mode", false)
    var paperWarmth by PrefBool(sp, "paper_warmth", true)
    var soundOn by PrefBool(sp, "sound_on", true)
    var vibOn by PrefBool(sp, "vib_on", true)
    var stageVib by PrefBool(sp, "stage_vib", true)
    var keepOn by PrefBool(sp, "keep_on", true)
    var dailyReminder by PrefBool(sp, "daily_reminder", true)
    var adhkarAlert by PrefBool(sp, "morning_alert", true)
    var prayerAlert by PrefBool(sp, "prayer_alert", false)
    var reminderHour by PrefInt(sp, "reminder_hour", 20)
    var dailyGoal by PrefInt(sp, "daily_goal", 5)
    var prayerMethod by PrefInt(sp, "prayer_method", 0)
    var reciterId by PrefString(sp, "reciter_id", "afs")
    var askedNotif by PrefBool(sp, "asked_notif", false)

    // التقدم والإحصائيات
    var lastPage by PrefInt(sp, "last_page", 1)
    var totalTasbih by PrefInt(sp, "total_tasbih", 0)
    var streak by PrefInt(sp, "streak", 0)
    private var lastReadDay by PrefInt(sp, "last_read_day", 0)
    private var todayDay by PrefInt(sp, "today_day", 0)
    private var todayCount by PrefInt(sp, "today_count", 0)

    var readPages by mutableStateOf(parseSet(sp.getString("read_pages", "")))
        private set
    var favPages by mutableStateOf(parseSet(sp.getString("fav_pages", "")))
        private set
    var geo by mutableStateOf(sp.geoLoc())
        private set

    private fun dayKey(): Int {
        val now = System.currentTimeMillis()
        return ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
    }

    fun todayPages(): Int = if (todayDay == dayKey()) todayCount else 0

    /** تُستدعى عند فتح المصحف (counted=false) أو عند الانتقال لصفحة جديدة (counted=true). */
    fun onPageViewed(page: Int, counted: Boolean) {
        lastPage = page
        if (page !in readPages) {
            readPages = readPages + page
            sp.edit().putString("read_pages", readPages.joinToString(",")).apply()
        }
        if (!counted) return
        val today = dayKey()
        if (todayDay != today) { todayDay = today; todayCount = 0 }
        todayCount += 1
        if (lastReadDay != today) {
            streak = if (lastReadDay == today - 1) streak + 1 else 1
            lastReadDay = today
        }
    }

    fun isFav(page: Int) = page in favPages

    fun toggleFav(page: Int) {
        favPages = if (page in favPages) favPages - page else favPages + page
        sp.edit().putString("fav_pages", favPages.joinToString(",")).apply()
    }

    fun khatmaPercent(): Int = (readPages.size * 100 / 604).coerceIn(0, 100)

    fun setLocation(name: String, lat: Double, lng: Double) {
        sp.edit().putString("loc_name", name).putString("loc_lat", lat.toString()).putString("loc_lng", lng.toString()).apply()
        geo = sp.geoLoc()
        Reminders.rescheduleAll(app)
    }

    fun resetStats() {
        readPages = emptySet()
        sp.edit().putString("read_pages", "").apply()
        streak = 0; lastReadDay = 0; todayCount = 0; todayDay = 0; totalTasbih = 0
    }
}

// ============================================================
// util/PrayerCalc.kt
// ============================================================
object PrayerCalc {
    val names = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    val methodNames = listOf("رابطة العالم الإسلامي", "الهيئة المصرية العامة للمساحة", "أم القرى - مكة المكرمة")

    /** فهارس الصلوات الخمس داخل مصفوفة millis: [فجر، شروق، ظهر، عصر، مغرب، عشاء] */
    val prayerIdx = intArrayOf(0, 2, 3, 4, 5)

    private fun rad(d: Double) = Math.toRadians(d)
    private fun deg(r: Double) = Math.toDegrees(r)
    private fun fix(a: Double, b: Double): Double = a - b * floor(a / b)

    private fun julian(year: Int, month: Int, day: Int): Double {
        var y = year; var m = month
        if (m <= 2) { y -= 1; m += 12 }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    /** (الميل الشمسي، معادلة الزمن) */
    private fun sun(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fix(357.529 + 0.98560028 * d, 360.0)
        val q = fix(280.459 + 0.98564736 * d, 360.0)
        val l = fix(q + 1.915 * sin(rad(g)) + 0.020 * sin(rad(2 * g)), 360.0)
        val e = 23.439 - 0.00000036 * d
        val ra = fix(deg(atan2(cos(rad(e)) * sin(rad(l)), cos(rad(l)))) / 15.0, 24.0)
        val eqt = q / 15.0 - ra
        val decl = deg(asin(sin(rad(e)) * sin(rad(l))))
        return decl to eqt
    }

    fun tzFor(geo: GeoLoc): TimeZone =
        if (geo.isDefault) TimeZone.getTimeZone("GMT+03:00") else TimeZone.getDefault()

    /** أوقات اليوم بالمللي ثانية: [فجر، شروق، ظهر، عصر، مغرب، عشاء] (0 إذا تعذر الحساب). */
    fun millis(geo: GeoLoc, method: Int, dayOffset: Int): LongArray {
        val tz = tzFor(geo)
        val cal = Calendar.getInstance(tz)
        cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        val y = cal.get(Calendar.YEAR); val m = cal.get(Calendar.MONTH) + 1; val d = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val m0 = cal.timeInMillis
        val tzh = tz.getOffset(m0 + 12 * 3_600_000L) / 3_600_000.0
        val lat = geo.lat
        val jd = julian(y, m, d) - geo.lng / (15.0 * 24.0)

        fun midDay(t: Double) = fix(12 - sun(jd + t).second, 24.0)
        fun angleTime(angle: Double, t: Double, ccw: Boolean): Double {
            val decl = sun(jd + t).first
            val x = (-sin(rad(angle)) - sin(rad(decl)) * sin(rad(lat))) / (cos(rad(decl)) * cos(rad(lat)))
            val tt = deg(acos(x)) / 15.0
            return midDay(t) + if (ccw) -tt else tt
        }
        fun asr(t: Double): Double {
            val decl = sun(jd + t).first
            val ang = -deg(atan(1.0 / (1.0 + tan(rad(abs(lat - decl))))))
            return angleTime(ang, t, false)
        }

        val (fajrAngle, ishaAngle) = when (method) {
            1 -> 19.5 to 17.5
            2 -> 18.5 to 0.0
            else -> 18.0 to 17.0
        }
        val adj = tzh - geo.lng / 15.0
        val fajr = angleTime(fajrAngle, 5 / 24.0, true) + adj
        val sunrise = angleTime(0.833, 6 / 24.0, true) + adj
        val dhuhr = midDay(12 / 24.0) + adj + 1 / 60.0
        val asr = asr(13 / 24.0) + adj
        val maghrib = angleTime(0.833, 18 / 24.0, false) + adj
        val isha = if (method == 2) maghrib + 1.5 else angleTime(ishaAngle, 18 / 24.0, false) + adj

        return doubleArrayOf(fajr, sunrise, dhuhr, asr, maghrib, isha).map { h ->
            if (h.isNaN()) 0L else m0 + (fix(h, 24.0) * 3_600_000).toLong()
        }.toLongArray()
    }

    /** الصلاة القادمة: (رقمها 0..4، وقتها بالمللي ثانية). */
    fun nextPrayer(geo: GeoLoc, method: Int, now: Long): Pair<Int, Long> {
        for (d in 0..1) {
            val t = millis(geo, method, d)
            for (i in prayerIdx.indices) {
                val v = t[prayerIdx[i]]
                if (v > now) return i to v
            }
        }
        return 0 to 0L
    }

    fun fmt(ms: Long, tz: TimeZone): String {
        if (ms <= 0L) return "--:--"
        val c = Calendar.getInstance(tz).apply { timeInMillis = ms }
        val h24 = c.get(Calendar.HOUR_OF_DAY)
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return String.format(Locale.US, "%02d:%02d %s", h12, c.get(Calendar.MINUTE), if (h24 < 12) "ص" else "م")
    }

    fun countdown(ms: Long): String {
        val s = (ms / 1000).coerceAtLeast(0)
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    /** اتجاه القبلة بالدرجات من الشمال الحقيقي (باتجاه عقارب الساعة). */
    fun qibla(lat: Double, lng: Double): Double {
        val kLat = rad(21.4225); val kLng = rad(39.8262)
        val p1 = rad(lat); val dl = kLng - rad(lng)
        val y = sin(dl) * cos(kLat)
        val x = cos(p1) * sin(kLat) - sin(p1) * cos(kLat) * cos(dl)
        return fix(deg(atan2(y, x)), 360.0)
    }
}

object LocationHelper {
    @SuppressLint("MissingPermission")
    fun lastKnown(ctx: Context): Location? {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
            .maxByOrNull { it.time }
    }
}

// ============================================================
// util/Reminders.kt
// ============================================================
object Notifs {
    const val CH_PLAYER = "quran_player_v1"
    const val CH_REMIND = "quran_reminders_v1"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_PLAYER, "مشغل التلاوات", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "التذكيرات اليومية", NotificationManager.IMPORTANCE_DEFAULT))
    }

    fun contentIntent(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    fun showReminder(ctx: Context, id: Int, title: String, text: String) {
        ensureChannels(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val n = NotificationCompat.Builder(ctx, CH_REMIND)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(contentIntent(ctx))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try { nm.notify(id, n) } catch (_: SecurityException) { }
    }
}

object Reminders {
    private fun pi(ctx: Context, code: Int, kind: String): PendingIntent = PendingIntent.getBroadcast(
        ctx, code,
        Intent(ctx, ReminderReceiver::class.java).putExtra("kind", kind),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun arm(ctx: Context, am: AlarmManager, code: Int, kind: String, at: Long?) {
        val p = pi(ctx, code, kind)
        if (at == null || at <= 0L) am.cancel(p)
        else runCatching { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p) }
    }

    private fun nextAt(hour: Int, min: Int): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, min); c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= System.currentTimeMillis() + 30_000) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** يعيد جدولة كل التذكيرات حسب الإعدادات الحالية. */
    fun rescheduleAll(ctx: Context) {
        val app = ctx.applicationContext
        val sp = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val am = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        arm(app, am, 1, "wird", if (sp.getBoolean("daily_reminder", true)) nextAt(sp.getInt("reminder_hour", 20), 0) else null)
        val adhkar = sp.getBoolean("morning_alert", true)
        arm(app, am, 2, "morning", if (adhkar) nextAt(6, 0) else null)
        arm(app, am, 3, "evening", if (adhkar) nextAt(17, 30) else null)

        val prayerOn = sp.getBoolean("prayer_alert", false)
        if (prayerOn) {
            val geo = sp.geoLoc()
            val method = sp.getInt("prayer_method", 0)
            val now = System.currentTimeMillis()
            val today = PrayerCalc.millis(geo, method, 0)
            val tomorrow = PrayerCalc.millis(geo, method, 1)
            PrayerCalc.prayerIdx.forEachIndexed { i, idx ->
                val t = if (today[idx] > now + 30_000) today[idx] else tomorrow[idx]
                arm(app, am, 10 + i, "prayer_$i", t)
            }
        } else {
            for (i in 0..4) arm(app, am, 10 + i, "prayer_$i", null)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val msg: Pair<String, String> = when {
            kind == "wird" -> "وردك اليومي" to "حان وقت وردك من القرآن الكريم، ولو بصفحة واحدة."
            kind == "morning" -> "أذكار الصباح" to "ابدأ يومك بذكر الله، فبذكره تطمئن القلوب."
            kind == "evening" -> "أذكار المساء" to "حان وقت أذكار المساء، حصّن نفسك بذكر الله."
            kind.startsWith("prayer_") -> {
                val i = kind.removePrefix("prayer_").toIntOrNull() ?: return
                "حان وقت الصلاة" to "حان الآن موعد صلاة ${PrayerCalc.names.getOrElse(i) { "" }}"
            }
            else -> return
        }
        Notifs.showReminder(ctx, kind.hashCode(), msg.first, msg.second)
        Reminders.rescheduleAll(ctx)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Reminders.rescheduleAll(ctx)
        }
    }
}

// ============================================================
// audio/PlayerService.kt
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
        private const val A_DELETE = "com.quran.app.DELETE"

        private val _state = MutableStateFlow(PlayerState())
        val state: StateFlow<PlayerState> = _state.asStateFlow()

        private fun intent(ctx: Context, action: String) = Intent(ctx, PlayerService::class.java).setAction(action)

        private fun cmd(ctx: Context, i: Intent) { runCatching { ctx.startService(i) } }

        fun play(ctx: Context, reciterId: String, surah: Int) {
            runCatching {
                ContextCompat.startForegroundService(ctx, intent(ctx, A_PLAY).putExtra("r", reciterId).putExtra("s", surah))
            }
        }
        fun download(ctx: Context, reciterId: String, surah: Int) {
            runCatching {
                ContextCompat.startForegroundService(ctx, intent(ctx, A_DOWNLOAD).putExtra("r", reciterId).putExtra("s", surah))
            }
        }
        fun delete(ctx: Context, reciterId: String, surah: Int) {
            AudioCache.file(ctx, reciterId, surah).delete()
            bump()
        }
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
        // إن بدأت الخدمة بـ startForegroundService ولم نبدأ تشغيلاً/تنزيلاً يجب إيقافها
        if (mp == null && dl.isEmpty()) maybeStop()
        return START_NOT_STICKY
    }

    // ---------- التشغيل ----------

    private fun urlOf(r: Reciter, num: Int) = "${r.url}/${num.toString().padStart(3, '0')}.mp3"

    private fun playSurah(reciterId: String, num: Int) {
        val reciter = QuranData.reciters.firstOrNull { it.id == reciterId } ?: return
        releasePlayer()
        _state.update {
            it.copy(reciterId = reciterId, surah = num, isLoading = true, isPlaying = false, positionMs = 0, durationMs = 0, error = null)
        }
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
                runCatching { audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) }
                runCatching { p.start() }
                _state.update { it.copy(isPlaying = true) }
                startTicker(); goForeground()
            }
            s.surah > 0 -> playSurah(s.reciterId, s.surah)
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
                        val pos = p.currentPosition
                        val dur = p.duration.coerceAtLeast(0)
                        _state.update { it.copy(positionMs = pos, durationMs = dur) }
                    }
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
        runCatching { mp?.reset(); mp?.release() }
        mp = null
    }

    private fun stopAll() {
        releasePlayer()
        sleepJob?.cancel()
        runCatching { @Suppress("DEPRECATION") audioManager.abandonAudioFocus(focusListener) }
        _state.update { it.copy(surah = 0, isPlaying = false, isLoading = false, positionMs = 0, durationMs = 0, sleepLeft = 0) }
        maybeStop()
    }

    private fun maybeStop() {
        if (mp == null && dl.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ---------- التنزيل ----------

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
                    error = if (ok) null else "تعذر تنزيل السورة، تحقق من اتصال الإنترنت"
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
            conn = (URL(urlOf(reciter, num)).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 20_000; instanceFollowRedirects = true
            }
            if (conn.responseCode !in 200..299) {
                false
            } else {
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

    // ---------- الإشعار ----------

    private fun actionPi(action: String, code: Int): PendingIntent =
        PendingIntent.getService(this, code, Intent(this, PlayerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

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
            b.addAction(
                if (s.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (s.isPlaying) "إيقاف مؤقت" else "تشغيل", actionPi(A_TOGGLE, 1)
            )
            b.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إغلاق", actionPi(A_STOP, 2))
        }
        return b.build()
    }

    private fun goForeground() {
        val n = buildNotification()
        if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(NOTIF_ID, n)
    }

    override fun onDestroy() {
        releasePlayer()
        scope.cancel()
        super.onDestroy()
    }
}

// ============================================================
// ui/Theme.kt
// ============================================================
val RoyalForest = Color(0xFF062D24)
val EmeraldVibrant = Color(0xFF0D4D3E)
val TurquoiseAccent = Color(0xFF136F5E)
val GoldWarm = Color(0xFFD9AE43)
val GoldShine = Color(0xFFF5D471)
val GoldSoftBg = Color(0xFFFCF6E3)
val PearlBg = Color(0xFFF7F5EE)
val CardWhite = Color(0xFFFFFFFF)
val InkDark = Color(0xFF10241E)
val InkMuted = Color(0xFF5A7068)
val IndigoStart = Color(0xFF3E357A)
val IndigoEnd = Color(0xFF5D50A8)
val TealStart = Color(0xFF0B5A5E)
val TealEnd = Color(0xFF158287)

@Immutable
data class Palette(val bg: Color, val card: Color, val ink: Color, val muted: Color, val border: Color, val dark: Boolean)

val LightPalette = Palette(PearlBg, CardWhite, InkDark, InkMuted, GoldWarm.copy(alpha = 0.6f), false)
val DarkPalette = Palette(Color(0xFF0B1512), Color(0xFF14231E), Color(0xFFEFEBDD), Color(0xFF9DB3AA), GoldWarm.copy(alpha = 0.4f), true)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }
val LocalFx = staticCompositionLocalOf<Fx> { error("Fx not provided") }

val QuranTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

/** نقر صوتي واهتزاز حسب إعدادات المستخدم. */
class Fx(private val view: View, private val ctx: Context, private val st: AppState) {
    fun click() { if (st.soundOn) view.playSoundEffect(SoundEffectConstants.CLICK) }

    @Suppress("DEPRECATION")
    fun vibrate(ms: Long = 35L) {
        if (!st.vibOn) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                else v.vibrate(ms)
            }
        }
    }
}

@Composable
fun QuranTheme(dark: Boolean, content: @Composable () -> Unit) {
    val pal = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides pal, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = GoldShine, secondary = GoldWarm, background = pal.bg, surface = pal.card, onSurface = pal.ink)
            else lightColorScheme(primary = EmeraldVibrant, secondary = GoldWarm, background = PearlBg, surface = CardWhite, onSurface = InkDark),
            typography = QuranTypography,
            content = content
        )
    }
}

// ============================================================
// ui/Nav.kt
// ============================================================
enum class Screen { Home, Reader, Index, Audio, Tasbih, Favorites, Prayer, Hadith, Stats, Settings }

class Nav(val stack: SnapshotStateList<Screen>, private val st: AppState) {
    /** 0 = المسبحة، 1 = الأذكار */
    var tasbihTab by mutableIntStateOf(0)

    val current: Screen get() = stack.lastOrNull() ?: Screen.Home

    fun push(s: Screen) { if (current != s) stack.add(s) }

    fun go(s: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        if (s != Screen.Home) stack.add(s)
    }

    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    fun openPage(page: Int) {
        st.lastPage = page.coerceIn(1, 604)
        push(Screen.Reader)
    }
}

// ============================================================
// ui/Components.kt
// ============================================================
fun shareText(ctx: Context, text: String, title: String = "مشاركة") {
    runCatching {
        val i = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun QCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    onClick: (() -> Unit)? = null,
    color: Color? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val c = color ?: p.card
    val b = BorderStroke(1.dp, borderColor ?: p.border)
    if (onClick != null) {
        Surface(onClick = { fx.click(); onClick() }, modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 1.dp, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 1.dp, content = content)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val fx = LocalFx.current
    Surface(color = RoyalForest, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { fx.click(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Color.White) }
            Text(title, color = GoldSoftBg, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp))
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = GoldWarm, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, color = p.muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun NumberBadge(n: Int, size: Int = 34) {
    Surface(shape = CircleShape, color = EmeraldVibrant, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) { Text("$n", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = EmeraldVibrant, checkedThumbColor = GoldShine))
        }
    }
}

@Composable
fun StepperRow(title: String, display: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
            IconButton(onClick = { fx.click(); onMinus() }) { Icon(Icons.Default.Remove, "إنقاص", tint = TurquoiseAccent) }
            Text(display, fontWeight = FontWeight.ExtraBold, color = p.ink, modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { fx.click(); onPlus() }) { Icon(Icons.Default.Add, "زيادة", tint = TurquoiseAccent) }
        }
    }
}

/** نقش إسلامي هندسي خفيف للخلفيات. */
@Composable
fun IslamicPattern(modifier: Modifier, color: Color = GoldShine.copy(alpha = 0.07f)) {
    Canvas(modifier) {
        val step = 46.dp.toPx()
        val r = step * 0.42f
        val cols = (size.width / step).toInt() + 1
        val rows = (size.height / step).toInt() + 1
        val stroke = Stroke(1.dp.toPx())
        for (i in 0..cols) for (j in 0..rows) {
            val c = Offset(i * step, j * step)
            drawRect(color, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = stroke)
            rotate(45f, c) { drawRect(color, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = stroke) }
        }
    }
}

@Composable
private fun BarTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip14().clickable { fx.click(); onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, label, tint = if (selected) GoldShine else GoldSoftBg.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) GoldShine else Color.White)
    }
}

private fun Modifier.clip14(): Modifier = this.then(Modifier.clip(RoundedCornerShape(14.dp)))

@Composable
fun AppBottomBar(nav: Nav) {
    val fx = LocalFx.current
    Surface(
        color = RoyalForest,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        border = BorderStroke(1.2.dp, GoldWarm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarTab("المصحف", Icons.AutoMirrored.Filled.MenuBook, nav.current == Screen.Reader) { nav.go(Screen.Reader) }
            BarTab("السور", Icons.Default.AutoStories, nav.current == Screen.Index) { nav.go(Screen.Index) }
            Surface(
                onClick = { fx.click(); nav.go(Screen.Home) },
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                color = if (nav.current == Screen.Home) TurquoiseAccent else EmeraldVibrant,
                border = BorderStroke(1.6.dp, GoldShine),
                modifier = Modifier.size(width = 64.dp, height = 50.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Home, "الرئيسية", tint = GoldShine, modifier = Modifier.size(22.dp))
                    Text("الرئيسية", color = GoldShine, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            BarTab("المسبحة", Icons.Default.TouchApp, nav.current == Screen.Tasbih) { nav.tasbihTab = 0; nav.go(Screen.Tasbih) }
            BarTab("الاستماع", Icons.Default.Headphones, nav.current == Screen.Audio) { nav.go(Screen.Audio) }
        }
    }
}

@Composable
fun MiniPlayer(ps: PlayerState, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val fx = LocalFx.current
    val s = QuranData.surah(ps.surah) ?: return
    val reciter = QuranData.reciters.firstOrNull { it.id == ps.reciterId }
    Surface(
        color = EmeraldVibrant,
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier.fillMaxWidth().clickable { fx.click(); onOpen() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (ps.durationMs > 0) (ps.positionMs / ps.durationMs.toFloat()).coerceIn(0f, 1f) else 0f },
                color = GoldShine, trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("سورة ${s.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (ps.isLoading) "جارٍ التحميل..." else reciter?.name ?: "", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                if (ps.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = GoldShine, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { fx.click(); PlayerService.toggle(ctx) }, modifier = Modifier.size(36.dp)) {
                        Icon(if (ps.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "تشغيل/إيقاف", tint = GoldShine, modifier = Modifier.size(28.dp))
                    }
                }
                IconButton(onClick = { fx.click(); PlayerService.stop(ctx) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "إغلاق", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// ============================================================
// ui/HomeScreen.kt
// ============================================================
private data class Tile(val title: String, val icon: ImageVector, val colors: List<Color>, val onClick: () -> Unit)

private fun hijriText(): String = runCatching {
    val ic = android.icu.util.IslamicCalendar()
    val months = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    "${ic.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${months[ic.get(android.icu.util.Calendar.MONTH)]} ${ic.get(android.icu.util.Calendar.YEAR)} هـ"
}.getOrDefault("")

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "مساء الخير"
        h < 12 -> "صباح الخير"
        h < 17 -> "طاب يومك"
        else -> "مساء الخير"
    }
}

@Composable
fun HomeScreen(nav: Nav) {
    val st = LocalApp.current
    val fx = LocalFx.current
    val p = LocalPalette.current
    val ctx = LocalContext.current

    val dayIdx = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val verse = QuranData.dailyVerses[dayIdx % QuranData.dailyVerses.size]
    val hadith = QuranData.hadiths[dayIdx % QuranData.hadiths.size]
    val hijri = remember { hijriText() }

    var query by remember { mutableStateOf("") }
    var showNotif by remember { mutableStateOf(false) }
    var showFollow by remember { mutableStateOf(false) }
    var notifOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    val goal = st.dailyGoal
    val today = st.todayPages()
    val badge = (if (!notifOn) 1 else 0) + (if (today < goal) 1 else 0)
    val page = st.lastPage
    val curSurah = QuranData.surahForPage(page)
    val pct = st.khatmaPercent()

    val results = remember(query) { if (query.isBlank()) emptyList() else QuranData.search(query).take(5) }
    val pageNum = query.trim().toIntOrNull()?.takeIf { it in 1..604 }

    val tiles = listOf(
        Tile("المصحف", Icons.AutoMirrored.Filled.MenuBook, listOf(EmeraldVibrant, TurquoiseAccent)) { nav.openPage(st.lastPage) },
        Tile("فهرس السور", Icons.Default.AutoStories, listOf(Color(0xFFB8892B), GoldWarm)) { nav.push(Screen.Index) },
        Tile("الاستماع", Icons.Default.Headphones, listOf(TealStart, TealEnd)) { nav.push(Screen.Audio) },
        Tile("المسبحة", Icons.Default.TouchApp, listOf(Color(0xFF8C5A2B), Color(0xFFB57B3D))) { nav.tasbihTab = 0; nav.push(Screen.Tasbih) },
        Tile("الأذكار", Icons.Default.WbSunny, listOf(Color(0xFFC77A1A), Color(0xFFE5A23A))) { nav.tasbihTab = 1; nav.push(Screen.Tasbih) },
        Tile("المفضلة", Icons.Default.Bookmark, listOf(EmeraldVibrant, TurquoiseAccent)) { nav.push(Screen.Favorites) },
        Tile("الأحاديث", Icons.Default.FormatQuote, listOf(IndigoStart, IndigoEnd)) { nav.push(Screen.Hadith) },
        Tile("الإحصائيات", Icons.Default.BarChart, listOf(Color(0xFF1F3358), Color(0xFF34508A))) { nav.push(Screen.Stats) }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ---------- الترويسة الملكية ----------
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant, TurquoiseAccent)))
            ) {
                IslamicPattern(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        HeaderIconButton(Icons.Default.Notifications, badge, "التنبيهات") { showNotif = true }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("القرآن الكريم", color = GoldSoftBg, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${greeting()} • $hijri", color = GoldShine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HeaderIconButton(Icons.Default.Settings, 0, "الإعدادات") { nav.push(Screen.Settings) }
                    }
                    Spacer(Modifier.height(14.dp))
                    NextPrayerChip(nav)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { fx.click(); nav.openPage(st.lastPage) },
                        shape = RoundedCornerShape(22.dp),
                        color = RoyalForest.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 5.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                                Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تابع القراءة", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("سورة ${curSurah.name}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text("صفحة $page من 604 • الجزء ${QuranData.juzOfPage(page)}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        // ---------- البحث ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), borderColor = GoldWarm) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("ابحث باسم السورة أو رقمها أو رقم الصفحة...", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TurquoiseAccent) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح", tint = p.muted) } },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (query.isNotBlank()) {
            item {
                QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        if (pageNum != null) {
                            SearchRow("الذهاب إلى صفحة $pageNum", "المصحف الشريف", null) { query = ""; nav.openPage(pageNum) }
                        }
                        results.forEach { s ->
                            SearchRow("سورة ${s.name}", "${s.type} • ${s.ayahs} آية • ص ${s.page}", s.num) { query = ""; nav.openPage(s.page) }
                        }
                        if (results.isEmpty() && pageNum == null) {
                            Text("لا توجد نتائج مطابقة", color = p.muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }
        }

        // ---------- شبكة الخدمات ----------
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t -> QuickTile(t, Modifier.weight(1f)) }
                    }
                }
            }
        }

        // ---------- الورد اليومي ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("وردك اليوم", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (st.streak > 0) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFE5732A), modifier = Modifier.size(18.dp))
                            Text(" ${st.streak} يوم متتالي", color = Color(0xFFE5732A), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (today / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        color = if (today >= goal) GoldWarm else EmeraldVibrant, trackColor = p.border.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (today >= goal) "أتممت وردك اليوم، تقبّل الله منك" else "قرأت $today من $goal صفحات", color = p.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        TextButton(onClick = { fx.click(); nav.openPage(st.lastPage) }) { Text("ابدأ", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }

        // ---------- آية اليوم ----------
        item {
            Surface(
                onClick = { fx.click(); nav.openPage(verse.page) },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.3.dp, GoldWarm),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(TealStart, TealEnd)))) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("آية اليوم", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(verse.text, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(verse.ref, color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallPill("اقرأ في المصحف", Icons.AutoMirrored.Filled.MenuBook) { nav.openPage(verse.page) }
                            SmallPill("مشاركة", Icons.Default.Share) { shareText(ctx, "${verse.text}\n${verse.ref}") }
                        }
                    }
                }
            }
        }

        // ---------- مواقيت الصلاة ----------
        item { PrayerStrip(nav) }

        // ---------- حديث اليوم ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatQuote, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("حديث اليوم • ${hadith.title}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { fx.click(); shareText(ctx, "${hadith.text}\n${hadith.source}") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, "مشاركة", tint = p.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(hadith.text, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(hadith.source, color = p.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // ---------- الخدمات السريعة ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), borderColor = GoldWarm) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    UtilityItem("تابعنا", Icons.Default.Public) { fx.click(); showFollow = true }
                    UtilityItem("الأحاديث", Icons.Default.FormatQuote) { fx.click(); nav.push(Screen.Hadith) }
                    UtilityItem("الإعدادات", Icons.Default.Settings) { fx.click(); nav.push(Screen.Settings) }
                    UtilityItem("شارك التطبيق", Icons.Default.Share) {
                        fx.click(); shareText(ctx, "تطبيق القرآن الكريم الشامل - مصحف وتلاوات ومسبحة ومواقيت الصلاة", "مشاركة التطبيق")
                    }
                }
            }
        }
    }

    if (showFollow) {
        AlertDialog(
            onDismissRequest = { showFollow = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = p.card,
            title = { Text("تابعنا عبر المنصات الرسمية", fontWeight = FontWeight.ExtraBold, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SocialButton("قناتنا على يوتيوب", Icons.Default.PlayArrow, Color(0xFFFF0000)) { showFollow = false; openUrl(ctx, QuranData.YOUTUBE_URL) }
                    SocialButton("قناتنا على تلجرام", Icons.AutoMirrored.Filled.Send, Color(0xFF0088CC)) { showFollow = false; openUrl(ctx, QuranData.TELEGRAM_URL) }
                }
            },
            confirmButton = { TextButton(onClick = { showFollow = false }) { Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) } }
        )
    }

    if (showNotif) {
        AlertDialog(
            onDismissRequest = { showNotif = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = p.card,
            title = { Text("التنبيهات", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (today < goal) Text("• بقي لك ${goal - today} صفحات لإتمام وردك اليوم.", fontWeight = FontWeight.Bold, color = p.ink)
                    else Text("• أتممت وردك اليوم، بارك الله فيك.", fontWeight = FontWeight.Bold, color = p.ink)
                    Text("• قراءة سورة الملك قبل النوم من السنن المأثورة.", fontWeight = FontWeight.Bold, color = p.ink)
                    if (!notifOn) {
                        Text("• إشعارات التطبيق متوقفة، فعّلها لتصلك التذكيرات.", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                        ) { Text("تفعيل الإشعارات", color = GoldSoftBg, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotif = false; nav.push(Screen.Settings) }) { Text("إعدادات التذكير", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) }
            },
            dismissButton = { TextButton(onClick = { showNotif = false }) { Text("حسناً", color = p.muted, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, badge: Int, desc: String, onClick: () -> Unit) {
    val fx = LocalFx.current
    Box {
        Surface(
            onClick = { fx.click(); onClick() }, shape = CircleShape, color = RoyalForest.copy(alpha = 0.7f),
            border = BorderStroke(1.2.dp, GoldWarm), modifier = Modifier.size(40.dp)
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, desc, tint = GoldSoftBg, modifier = Modifier.size(20.dp)) } }
        if (badge > 0) {
            Surface(shape = CircleShape, color = Color(0xFFD32F2F), modifier = Modifier.size(16.dp).align(Alignment.TopEnd)) {
                Box(contentAlignment = Alignment.Center) { Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun NextPrayerChip(nav: Nav) {
    val st = LocalApp.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val next = remember(st.geo, st.prayerMethod, now / 60_000) { PrayerCalc.nextPrayer(st.geo, st.prayerMethod, now) }
    val tz = remember(st.geo) { PrayerCalc.tzFor(st.geo) }
    Surface(
        onClick = { nav.push(Screen.Prayer) },
        shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = GoldShine, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("الصلاة القادمة: ${PrayerCalc.names[next.first]}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("${PrayerCalc.fmt(next.second, tz)} • ${st.geo.name}", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (next.second > 0) PrayerCalc.countdown(next.second - now) else "--:--:--", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PrayerStrip(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val times = remember(st.geo, st.prayerMethod) { PrayerCalc.millis(st.geo, st.prayerMethod, 0) }
    val tz = remember(st.geo) { PrayerCalc.tzFor(st.geo) }
    val nextIdx = PrayerCalc.nextPrayer(st.geo, st.prayerMethod, System.currentTimeMillis()).first
    QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), onClick = { nav.push(Screen.Prayer) }) {
        Column(Modifier.padding(14.dp)) {
            Text("مواقيت الصلاة • ${st.geo.name}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrayerCalc.prayerIdx.forEachIndexed { i, idx ->
                    val isNext = i == nextIdx
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (isNext) GoldShine.copy(alpha = 0.25f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(PrayerCalc.names[i], color = if (isNext) TurquoiseAccent else p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(PrayerCalc.fmt(times[idx], tz), color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(title: String, sub: String, num: Int?, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { fx.click(); onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (num != null) NumberBadge(num, 30) else Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = GoldWarm, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 14.sp)
            Text(sub, color = p.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
    }
}

@Composable
private fun QuickTile(t: Tile, modifier: Modifier) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable { fx.click(); t.onClick() }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(62.dp).clip(RoundedCornerShape(21.dp)).background(Brush.linearGradient(t.colors))
                .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(t.icon, t.title, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(t.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmallPill(text: String, icon: ImageVector, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(onClick = { fx.click(); onClick() }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.16f), border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.7f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldShine, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun UtilityItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, title, tint = GoldWarm, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = p.ink)
    }
}

@Composable
private fun SocialButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(onClick = { fx.click(); onClick() }, shape = RoundedCornerShape(18.dp), color = color, modifier = Modifier.fillMaxWidth().height(46.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

// ============================================================
// ui/ReaderScreens.kt
// ============================================================
private val InvertMatrix = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
private val WarmMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0.96f, 0f, 0f, 0f, 0f, 0f, 0.84f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))

@Composable
fun ReaderScreen(nav: Nav) {
    val st = LocalApp.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = (st.lastPage - 1).coerceIn(0, 603), pageCount = { 604 })
    val curPage = pagerState.currentPage + 1
    var chrome by remember { mutableStateOf(true) }
    var showJump by remember { mutableStateOf(false) }
    var sliderDrag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(pagerState) {
        var first = true
        snapshotFlow { pagerState.currentPage }.collect { idx ->
            st.onPageViewed(idx + 1, counted = !first)
            first = false
        }
    }
    LaunchedEffect(curPage) {
        for (n in listOf(curPage + 1, curPage + 2, curPage - 1)) {
            if (n in 1..604) ctx.imageLoader.enqueue(ImageRequest.Builder(ctx).data(QuranData.pageUrl(n)).build())
        }
    }
    DisposableEffect(st.keepOn) {
        val w = (ctx as? Activity)?.window
        if (st.keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val bg = when {
        st.nightMode -> Color.Black
        st.paperWarmth -> Color(0xFFF6EFE0)
        else -> Color.White
    }
    val surah = QuranData.surahForPage(curPage)

    Column(Modifier.fillMaxSize().background(bg)) {
        AnimatedVisibility(visible = chrome) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { fx.click(); nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Color.White) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سورة ${surah.name}", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text("صفحة $curPage • الجزء ${QuranData.juzOfPage(curPage)}", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    IconButton(onClick = { fx.click(); st.nightMode = !st.nightMode }) {
                        Icon(if (st.nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, "الوضع الليلي", tint = GoldShine)
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { idx ->
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { chrome = !chrome }) }) {
                MushafPage(idx + 1, st.nightMode, st.paperWarmth)
            }
        }

        AnimatedVisibility(visible = chrome) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Slider(
                        value = (sliderDrag ?: curPage.toFloat()).coerceIn(1f, 604f),
                        onValueChange = { sliderDrag = it },
                        onValueChangeFinished = {
                            val target = sliderDrag?.toInt() ?: curPage
                            sliderDrag = null
                            scope.launch { pagerState.scrollToPage(target.coerceIn(1, 604) - 1) }
                        },
                        valueRange = 1f..604f,
                        colors = SliderDefaults.colors(thumbColor = GoldShine, activeTrackColor = GoldShine, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        ReaderAction(if (st.isFav(curPage)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (st.isFav(curPage)) "محفوظة" else "حفظ") {
                            fx.click(); st.toggleFav(curPage)
                        }
                        ReaderAction(Icons.Default.Search, "اذهب لصفحة") { fx.click(); showJump = true }
                        ReaderAction(Icons.Default.Headphones, "استمع للسورة") {
                            fx.click()
                            PlayerService.play(ctx, st.reciterId, surah.num)
                            nav.push(Screen.Audio)
                        }
                        ReaderAction(Icons.Default.Home, "الرئيسية") { fx.click(); nav.go(Screen.Home) }
                    }
                }
            }
        }
    }

    if (showJump) {
        var text by remember { mutableStateOf("") }
        val n = text.toIntOrNull()
        val valid = n != null && n in 1..604
        AlertDialog(
            onDismissRequest = { showJump = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("الانتقال إلى صفحة", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("رقم الصفحة (1 - 604)") }, isError = text.isNotEmpty() && !valid
                )
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    showJump = false
                    scope.launch { pagerState.scrollToPage((n ?: 1) - 1) }
                }) { Text("انتقال", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showJump = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ReaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) { Icon(icon, label, tint = GoldShine, modifier = Modifier.size(22.dp)) }
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MushafPage(page: Int, night: Boolean, warm: Boolean) {
    val ctx = LocalContext.current
    var retry by remember { mutableIntStateOf(0) }
    val filter = remember(night, warm) {
        when {
            night -> ColorFilter.colorMatrix(InvertMatrix)
            warm -> ColorFilter.colorMatrix(WarmMatrix)
            else -> null
        }
    }
    key(retry) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(ctx).data(QuranData.pageUrl(page)).crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = "صفحة $page",
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldWarm) } },
            error = {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Refresh, null, tint = GoldWarm, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("تعذر تحميل الصفحة $page", color = if (night) Color.White else InkDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { retry++ }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)) {
                        Text("إعادة المحاولة", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        )
    }
}

@Composable
fun IndexScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("الكل") }

    val list = remember(query, typeFilter) {
        QuranData.search(query).filter { typeFilter == "الكل" || it.type == typeFilter }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الفهرس", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("السور (114)", "الأجزاء (30)").forEachIndexed { i, label ->
                FilterChip(
                    selected = tab == i, onClick = { fx.click(); tab = i },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                )
            }
        }
        if (tab == 0) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("الكل", "مكية", "مدنية")) { t ->
                    FilterChip(
                        selected = typeFilter == t, onClick = { fx.click(); typeFilter = t },
                        label = { Text(t, fontWeight = FontWeight.Bold, fontSize = 12.sp) }, shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldWarm, selectedLabelColor = RoyalForest)
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة بهذا الاسم") }
                items(list, key = { it.num }) { s ->
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(s.page) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(s.num)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("${s.type} • ${s.ayahs} آية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص ${s.page}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            IconButton(onClick = { fx.click(); PlayerService.play(ctx, st.reciterId, s.num); nav.push(Screen.Audio) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PlayCircleOutline, "استماع", tint = GoldWarm, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(30) { i ->
                    val start = QuranData.juzStart[i]
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(start) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(i + 1)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الجزء ${i + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("يبدأ من سورة ${QuranData.surahForPage(start).name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص $start", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// ui/AudioScreen.kt
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

    LaunchedEffect(ps.error) {
        ps.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); PlayerService.clearError() }
    }

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

        // ---------- بطاقة المشغل ----------
        Surface(
            shape = RoundedCornerShape(24.dp), color = RoyalForest, border = BorderStroke(1.5.dp, GoldWarm),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(nowSurah?.let { "سورة ${it.name}" } ?: "اختر سورة لبدء التلاوة", color = GoldSoftBg, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (nowSurah != null) "${nowReciter.name} • ${nowReciter.style}" else "${reciter.name} • ${reciter.style}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                // عناصر التحكم تبقى بترتيب عالمي (يسار → يمين)
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
                            IconButton(onClick = { fx.click(); PlayerService.prev(ctx) }, enabled = ps.surah > 1) {
                                Icon(Icons.Default.SkipPrevious, "السورة السابقة", tint = Color.White)
                            }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, -10_000) }, enabled = ps.surah > 0) {
                                Icon(Icons.Default.Replay10, "رجوع 10 ثوانٍ", tint = GoldSoftBg)
                            }
                            Surface(
                                onClick = {
                                    fx.click()
                                    if (ps.surah == 0) PlayerService.play(ctx, st.reciterId, 1) else PlayerService.toggle(ctx)
                                },
                                shape = CircleShape, color = GoldShine, modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (ps.isLoading) CircularProgressIndicator(Modifier.size(26.dp), color = RoyalForest, strokeWidth = 3.dp)
                                    else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل/إيقاف", tint = RoyalForest, modifier = Modifier.size(30.dp))
                                }
                            }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, 10_000) }, enabled = ps.surah > 0) {
                                Icon(Icons.Default.Forward10, "تقديم 10 ثوانٍ", tint = GoldSoftBg)
                            }
                            IconButton(onClick = { fx.click(); PlayerService.next(ctx) }, enabled = ps.surah in 1..113) {
                                Icon(Icons.Default.SkipNext, "السورة التالية", tint = Color.White)
                            }
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
                QCard(
                    shape = RoundedCornerShape(18.dp),
                    borderColor = if (isCurrent) EmeraldVibrant else null,
                    onClick = { if (isCurrent) PlayerService.toggle(ctx) else PlayerService.play(ctx, st.reciterId, s.num) }
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(s.num, 32)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text(
                                when {
                                    progress != null -> "جارٍ التنزيل ${(progress * 100).toInt()}%"
                                    cached -> "محملة (تعمل بدون إنترنت)"
                                    else -> "بث مباشر • ${s.ayahs} آية"
                                },
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (cached) TurquoiseAccent else p.muted
                            )
                        }
                        when {
                            progress != null -> CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp), color = EmeraldVibrant, strokeWidth = 3.dp)
                            cached -> IconButton(onClick = { fx.click(); PlayerService.delete(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteOutline, "حذف التنزيل", tint = Color(0xFFB71C1C), modifier = Modifier.size(22.dp))
                            }
                            else -> IconButton(onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Download, "تنزيل", tint = GoldWarm, modifier = Modifier.size(22.dp))
                            }
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
            onDismissRequest = { showSleep = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("مؤقت النوم", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إلغاء المؤقت", 15 to "بعد 15 دقيقة", 30 to "بعد 30 دقيقة", 60 to "بعد ساعة").forEach { (m, label) ->
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
private fun ModeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    TextButton(onClick = { fx.click(); onClick() }) {
        Icon(icon, null, tint = if (active) GoldShine else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) GoldShine else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// ============================================================
// ui/TasbihScreen.kt
// ============================================================
@Composable
fun TasbihScreen(nav: Nav) {
    val fx = LocalFx.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (nav.tasbihTab == 0) "المسبحة الإلكترونية" else "الأذكار اليومية", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("المسبحة" to Icons.Default.TouchApp, "الأذكار" to Icons.Default.WbSunny).forEachIndexed { i, (label, icon) ->
                FilterChip(
                    selected = nav.tasbihTab == i, onClick = { fx.click(); nav.tasbihTab = i },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg, selectedLeadingIconColor = GoldShine)
                )
            }
        }
        if (nav.tasbihTab == 0) TasbihCounter() else AdhkarList()
    }
}

@Composable
private fun TasbihCounter() {
    val st = LocalApp.current
    val fx = LocalFx.current
    val stages = QuranData.tasbihStages

    var stage by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(stages[0].defaultTarget) }
    var count by rememberSaveable { mutableStateOf(0) }
    var rounds by rememberSaveable { mutableStateOf(0) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }

    val active = stages[stage]
    val next = stages[(stage + 1) % stages.size]
    val progress by animateFloatAsState((count / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), label = "ring")

    fun goStage(i: Int) { stage = i; target = stages[i].defaultTarget; count = 0 }

    fun tap() {
        fx.click()
        if (!autoAdvance && count >= target) count = 0
        st.totalTasbih += 1
        if (count + 1 >= target) {
            fx.vibrate(if (st.stageVib) 140L else 35L)
            rounds += 1
            if (autoAdvance) goStage((stage + 1) % stages.size) else count = target
        } else {
            fx.vibrate(35L)
            count += 1
        }
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(stages) { idx, s ->
                    FilterChip(
                        selected = idx == stage, onClick = { fx.click(); goStage(idx) },
                        shape = RoundedCornerShape(16.dp),
                        label = { Text(s.phrase, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldShine)
                    )
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = RoyalForest, border = BorderStroke(1.8.dp, GoldWarm), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Box {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 10, 33, 100).forEach { goal ->
                                    Surface(
                                        onClick = { fx.click(); target = goal; count = 0 },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (target == goal) GoldShine else EmeraldVibrant,
                                        border = BorderStroke(1.dp, GoldWarm)
                                    ) {
                                        Text("$goal", color = if (target == goal) RoyalForest else GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Surface(
                                onClick = { fx.click(); autoAdvance = !autoAdvance },
                                shape = RoundedCornerShape(12.dp),
                                color = if (autoAdvance) TurquoiseAccent else EmeraldVibrant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, GoldWarm)
                            ) {
                                Text(if (autoAdvance) "انتقال تلقائي ✓" else "تكرار ثابت", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(active.phrase, color = GoldShine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        Text(active.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                            CircularProgressIndicator(progress = { progress }, strokeWidth = 10.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.18f), modifier = Modifier.fillMaxSize())
                            Surface(
                                onClick = { tap() }, shape = CircleShape, color = EmeraldVibrant,
                                border = BorderStroke(2.dp, GoldShine), modifier = Modifier.size(136.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$count", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("من $target", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("الجولات المكتملة: $rounds • إجمالي التسبيحات: ${st.totalTasbih}", color = GoldSoftBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { fx.click(); count = 0 }) {
                                Icon(Icons.Default.Refresh, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تصفير", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(onClick = { fx.click(); if (count > 0) { count -= 1; if (st.totalTasbih > 0) st.totalTasbih -= 1 } }) {
                                Icon(Icons.Default.Undo, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تراجع", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(onClick = { fx.click(); goStage((stage + 1) % stages.size) }) {
                                Text("تخطي: ${next.phrase.take(14)}", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarList() {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val counts = remember { mutableStateMapOf<Int, Int>() }
    val adhkar = QuranData.adhkar

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val done = adhkar.indices.count { (counts[it] ?: 0) >= adhkar[it].target }
                Text("أنجزت $done من ${adhkar.size}", color = p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                TextButton(onClick = { fx.click(); counts.clear() }) {
                    Icon(Icons.Default.Refresh, null, tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إعادة الكل", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        itemsIndexed(adhkar) { i, d ->
            val c = counts[i] ?: 0
            val done = c >= d.target
            QCard(
                shape = RoundedCornerShape(20.dp), borderColor = if (done) GoldWarm else null,
                onClick = {
                    if (c < d.target) {
                        counts[i] = c + 1
                        fx.vibrate(if (c + 1 >= d.target) 100L else 30L)
                    }
                }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.title, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (done) Icon(Icons.Default.CheckCircle, "تم", tint = GoldWarm)
                        else if (c > 0) IconButton(onClick = { fx.click(); counts[i] = 0 }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, "إعادة", tint = p.muted, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(d.text, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (c / d.target.toFloat()).coerceIn(0f, 1f) },
                        color = if (done) GoldWarm else EmeraldVibrant, trackColor = p.border.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("اضغط للعد • $c / ${d.target}", color = if (done) GoldWarm else p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================
// ui/MoreScreens.kt
// ============================================================
// ======================= المفضلة =======================
@Composable
fun FavoritesScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val favs = st.favPages.sorted()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الصفحات المحفوظة", onBack = { nav.back() })
        if (favs.isEmpty()) {
            EmptyState(Icons.Default.BookmarkBorder, "لا توجد صفحات محفوظة بعد.\nاضغط على «حفظ» أثناء القراءة لإضافة الصفحة هنا.")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favs, key = { it }) { pg ->
                    val s = QuranData.surahForPage(pg)
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(pg) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, null, tint = GoldWarm)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("صفحة $pg", fontWeight = FontWeight.ExtraBold, color = p.ink)
                                Text("سورة ${s.name} • الجزء ${QuranData.juzOfPage(pg)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted)
                            }
                            IconButton(onClick = { fx.click(); st.toggleFav(pg) }) { Icon(Icons.Default.DeleteOutline, "إزالة", tint = Color(0xFFB71C1C)) }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        }
    }
}

// ======================= مواقيت الصلاة =======================
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
        ScreenHeader("مواقيت الصلاة", onBack = { nav.back() })
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
            item {
                SettingToggle("تنبيه عند دخول وقت كل صلاة", st.prayerAlert) { on ->
                    st.prayerAlert = on
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Reminders.rescheduleAll(ctx)
                }
            }
            item {
                Text("الأوقات محسوبة فلكياً داخل التطبيق دون إنترنت، وقد تختلف دقيقة أو دقيقتين عن التقويم المحلي لمسجدك.", color = p.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCities) {
        AlertDialog(
            onDismissRequest = { showCities = false },
            shape = RoundedCornerShape(24.dp),
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

// ======================= الأحاديث =======================
@Composable
fun HadithScreen(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الأحاديث النبوية الشريفة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(QuranData.hadiths) { h ->
                QCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.title, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent, modifier = Modifier.weight(1f))
                            IconButton(onClick = { fx.click(); shareText(ctx, "${h.text}\n${h.source}") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, "مشاركة", tint = p.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(h.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = p.ink, lineHeight = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(h.source, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.muted)
                    }
                }
            }
        }
    }
}

// ======================= الإحصائيات =======================
@Composable
fun StatsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()
    val downloaded = remember(ps.cacheVersion) { AudioCache.count(ctx) }
    var confirmReset by remember { mutableStateOf(false) }
    val pct = st.khatmaPercent()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("إحصائيات الختمة والعبادة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 8.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                            Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("تقدّم الختمة", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("${st.readPages.size} من 604 صفحة", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { StatCard("آخر صفحة وصلت إليها", "صفحة ${st.lastPage} • ${QuranData.surahForPage(st.lastPage).name}") }
            item { StatCard("صفحات اليوم", "${st.todayPages()} من ${st.dailyGoal}") }
            item { StatCard("أيام القراءة المتتالية", "${st.streak} يوم") }
            item { StatCard("مجموع التسبيحات", "${st.totalTasbih} تسبيحة") }
            item { StatCard("السور المحملة صوتياً", "$downloaded سورة") }
            item { StatCard("الصفحات المحفوظة", "${st.favPages.size} صفحة") }
            item {
                OutlinedButton(onClick = { fx.click(); confirmReset = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.RestartAlt, null, tint = Color(0xFFB71C1C))
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير الإحصائيات", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("تصفير الإحصائيات؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم مسح تقدم الختمة والتسبيحات وأيام القراءة. لن تتأثر الصفحات المحفوظة.", color = p.ink) },
            confirmButton = { TextButton(onClick = { st.resetStats(); confirmReset = false }) { Text("تصفير", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = p.ink)
            Text(value, fontWeight = FontWeight.ExtraBold, color = EmeraldVibrant)
        }
    }
}

// ======================= الإعدادات =======================
@Composable
fun SettingsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()
    val files = remember(ps.cacheVersion) { AudioCache.count(ctx) }
    val sizeMb = remember(ps.cacheVersion) { AudioCache.sizeMb(ctx) }
    var notifOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }
    var confirmClear by remember { mutableStateOf(false) }

    fun askNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الإعدادات", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionTitle("القراءة والمظهر") }
            item { SettingToggle("الوضع الليلي (التطبيق والمصحف)", st.nightMode) { st.nightMode = it; fx.click() } }
            item { SettingToggle("ورق المصحف الكريمي الدافئ", st.paperWarmth) { st.paperWarmth = it; fx.click() } }
            item { SettingToggle("إبقاء الشاشة مضاءة أثناء القراءة", st.keepOn) { st.keepOn = it; fx.click() } }
            item { StepperRow("هدف الورد اليومي", "${st.dailyGoal} صفحات", { if (st.dailyGoal > 1) st.dailyGoal -= 1 }, { if (st.dailyGoal < 60) st.dailyGoal += 1 }) }

            item { SectionTitle("الصوت والاهتزاز") }
            item { SettingToggle("صوت النقرات", st.soundOn) { st.soundOn = it } }
            item { SettingToggle("اهتزاز خفيف عند كل تسبيحة", st.vibOn) { st.vibOn = it; fx.click() } }
            item { SettingToggle("اهتزاز طويل عند إتمام العدد", st.stageVib) { st.stageVib = it; fx.click() } }

            item { SectionTitle("التذكيرات") }
            if (!notifOn) {
                item {
                    QCard(shape = RoundedCornerShape(18.dp), borderColor = Color(0xFFB71C1C), onClick = { askNotif() }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFFB71C1C))
                            Spacer(Modifier.width(10.dp))
                            Text("الإشعارات متوقفة. اضغط للتفعيل حتى تصلك التذكيرات.", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { SettingToggle("تذكير الورد اليومي", st.dailyReminder) { st.dailyReminder = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            if (st.dailyReminder) {
                item {
                    StepperRow("وقت تذكير الورد", String.format(Locale.US, "%02d:00", st.reminderHour),
                        { st.reminderHour = (st.reminderHour + 23) % 24; Reminders.rescheduleAll(ctx) },
                        { st.reminderHour = (st.reminderHour + 1) % 24; Reminders.rescheduleAll(ctx) })
                }
            }
            item { SettingToggle("تنبيه أذكار الصباح (6:00) والمساء (5:30)", st.adhkarAlert) { st.adhkarAlert = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            item { SettingToggle("تنبيه أوقات الصلاة", st.prayerAlert) { st.prayerAlert = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            item {
                QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.push(Screen.Prayer) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = GoldWarm)
                        Spacer(Modifier.width(10.dp))
                        Text("المدينة وطريقة حساب الصلاة: ${st.geo.name}", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                    }
                }
            }

            item { SectionTitle("التخزين") }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("التلاوات المحملة", fontWeight = FontWeight.ExtraBold, color = p.ink)
                            Text("$files ملف • $sizeMb م.ب", fontWeight = FontWeight.Bold, color = TurquoiseAccent)
                        }
                        Button(
                            onClick = { fx.click(); confirmClear = true }, enabled = files > 0,
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)), modifier = Modifier.fillMaxWidth()
                        ) { Text("حذف التلاوات المحملة لتوفير المساحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                }
            }
            item { SectionTitle("عن التطبيق") }
            item {
                QCard(shape = RoundedCornerShape(18.dp), onClick = { shareText(ctx, "تطبيق القرآن الكريم الشامل - مصحف وتلاوات ومسبحة ومواقيت الصلاة", "مشاركة التطبيق") }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null, tint = GoldWarm)
                        Spacer(Modifier.width(10.dp))
                        Text("شارك التطبيق مع أحبّائك", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("حذف التلاوات المحملة؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم حذف $files ملف صوتي. يمكنك الاستماع بالبث المباشر أو إعادة التنزيل لاحقاً.", color = p.ink) },
            confirmButton = { TextButton(onClick = { PlayerService.clearAll(ctx); confirmClear = false }) { Text("حذف", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } }
        )
    }
}

// ============================================================
// MainActivity.kt
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifs.ensureChannels(this)
        Reminders.rescheduleAll(this)
        setContent { QuranApp() }
    }
}

@Composable
fun QuranApp() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st = remember { AppState(ctx.applicationContext) }
    val fx = remember { Fx(view, ctx, st) }
    QuranTheme(dark = st.nightMode) {
        CompositionLocalProvider(LocalApp provides st, LocalFx provides fx) { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val st = LocalApp.current
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val stack = remember { mutableStateListOf(Screen.Home) }
    val nav = remember { Nav(stack, st) }
    val ps by PlayerService.state.collectAsState()

    // أيقونات شريط الحالة فاتحة لأن الترويسة خضراء داكنة دائماً
    SideEffect {
        val window = (ctx as? android.app.Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    // طلب إذن الإشعارات مرة واحدة (أندرويد 13+)
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !st.askedNotif) {
            st.askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = stack.size > 1) { nav.back() }

    val showBars = nav.current != Screen.Reader

    Box(Modifier.fillMaxSize().background(RoyalForest)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(p.bg)) {
                when (nav.current) {
                    Screen.Home -> HomeScreen(nav)
                    Screen.Reader -> ReaderScreen(nav)
                    Screen.Index -> IndexScreen(nav)
                    Screen.Audio -> AudioScreen(nav)
                    Screen.Tasbih -> TasbihScreen(nav)
                    Screen.Favorites -> FavoritesScreen(nav)
                    Screen.Prayer -> PrayerScreen(nav)
                    Screen.Hadith -> HadithScreen(nav)
                    Screen.Stats -> StatsScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                }
            }
            if (showBars) {
                if (ps.surah > 0 && nav.current != Screen.Audio) MiniPlayer(ps) { nav.push(Screen.Audio) }
                AppBottomBar(nav)
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
