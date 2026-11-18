package com.quran.app

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import kotlin.reflect.KProperty

// ============================================================
// نماذج البيانات الأساسية
// ============================================================
data class Surah(val num: Int, val name: String, val type: String, val ayahs: Int, val page: Int)
data class Reciter(val id: String, val name: String, val style: String, val url: String)
data class HadithItem(val title: String, val text: String, val source: String)
data class DhikrItem(val title: String, val text: String, val target: Int)
data class TasbihStage(val phrase: String, val subtitle: String, val defaultTarget: Int)
data class DailyVerse(val text: String, val ref: String, val page: Int)
data class City(val name: String, val lat: Double, val lng: Double)
data class HifzQuizItem(val question: String, val options: List<String>, val correctIndex: Int, val surahRef: String)

fun String.normalizeAr(): String = this.trim()
    .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
    .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا').replace('ٱ', 'ا')
    .replace('ى', 'ي').replace('ة', 'ه')

object QuranData {
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
        Reciter("sudais", "عبد الرحمن السديس", "مرتل • حفص", "https://server11.mp3quran.net/sds"),
        Reciter("shur", "سعود الشريم", "مرتل • حفص", "https://server11.mp3quran.net/shur"),
        Reciter("ajm", "أحمد العجمي", "مرتل • حفص", "https://server6.mp3quran.net/ajm"),
        Reciter("husr", "محمود خليل الحصري", "مرتل • حفص", "https://server5.mp3quran.net/husr"),
        Reciter("ghamdi", "سعد الغامدي", "مرتل • حفص", "https://server11.mp3quran.net/ghamdi"),
        Reciter("hud", "علي الحذيفي", "مرتل • حفص", "https://server13.mp3quran.net/hudhaify"),
        Reciter("jhn", "عبد الله عواد الجهني", "مرتل • حفص", "https://server13.mp3quran.net/jhn"),
        Reciter("bna", "محمود علي البنا", "مجود • حفص", "https://server12.mp3quran.net/bna"),
        Reciter("rifai", "محمد رفعت", "مجود • حفص", "https://server7.mp3quran.net/rifai"),
        Reciter("tblawi", "محمد محمود الطبلاوي", "مجود • حفص", "https://server12.mp3quran.net/tblawi")
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

    val hifzQuizzes = listOf(
        HifzQuizItem("أكمل الآية: ﴿ إِنَّا أَعْطَيْنَاكَ ... ﴾", listOf("الْكَوْثَرَ", "الْحِكْمَةَ", "الْفُرْقَانَ"), 0, "سورة الكوثر"),
        HifzQuizItem("أكمل الآية: ﴿ قُلْ هُوَ اللَّهُ ... ﴾", listOf("الصَّمَدُ", "أَحَدٌ", "الْوَاحِدُ"), 1, "سورة الإخلاص"),
        HifzQuizItem("أكمل الآية: ﴿ وَالْعَصْرِ * إِنَّ الْإِنسَانَ لَفِي ... ﴾", listOf("ضَلَالٍ", "خُسْرٍ", "غَفْلَةٍ"), 1, "سورة العصر"),
        HifzQuizItem("أكمل الآية: ﴿ أَلَمْ نَشْرَحْ لَكَ ... ﴾", listOf("قَلْبَكَ", "صَدْرَكَ", "أَمْرَكَ"), 1, "سورة الشرح"),
        HifzQuizItem("أكمل الآية: ﴿ سَبِّحِ اسْمَ رَبِّكَ ... ﴾", listOf("الْعَظِيمِ", "الْكَرِيمِ", "الْأَعْلَى"), 2, "سورة الأعلى")
    )

    val cities = listOf(
        City("القدس", 31.7683, 35.2137), City("غزة", 31.5017, 34.4668), City("رام الله", 31.9038, 35.2034),
        City("عمّان", 31.9454, 35.9284), City("دمشق", 33.5138, 36.2765), City("حلب", 36.2021, 37.1343),
        City("بيروت", 33.8938, 35.5018), City("بغداد", 33.3152, 44.3661), City("القاهرة", 30.0444, 31.2357),
        City("الرياض", 24.7136, 46.6753), City("مكة المكرمة", 21.4225, 39.8262), City("المدينة المنورة", 24.4686, 39.6142),
        City("دبي", 25.2048, 55.2708), City("الدوحة", 25.2854, 51.5310), City("الكويت", 29.3759, 47.9774),
        City("إسطنبول", 41.0082, 28.9784), City("الدار البيضاء", 33.5731, -7.5898), City("الجزائر", 36.7538, 3.0588),
        City("تونس", 36.8065, 10.1815), City("الخرطوم", 15.5007, 32.5599), City("صنعاء", 15.3694, 44.1910)
    )

    fun pageUrl(page: Int) = "https://android.quran.com/data/width_1024/page${page.toString().padStart(3, '0')}.png"
}

// ============================================================
// حالة التطبيق (AppState)
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

    var lastPage by PrefInt(sp, "last_page", 1)
    var totalTasbih by PrefInt(sp, "total_tasbih", 0)
    var streak by PrefInt(sp, "streak", 0)
    var hifzNote by PrefString(sp, "hifz_note", "")
    private var lastReadDay by PrefInt(sp, "last_read_day", 0)
    private var todayDay by PrefInt(sp, "today_day", 0)
    private var todayCount by PrefInt(sp, "today_count", 0)

    var readPages by mutableStateOf(parseSet(sp.getString("read_pages", "")))
        private set
    var favPages by mutableStateOf(parseSet(sp.getString("fav_pages", "")))
        private set
    var memorizedSurahs by mutableStateOf(parseSet(sp.getString("memorized_surahs", "")))
        private set
    var geo by mutableStateOf(sp.geoLoc())
        private set

    private fun dayKey(): Int {
        val now = System.currentTimeMillis()
        return ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
    }

    fun todayPages(): Int = if (todayDay == dayKey()) todayCount else 0

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

    fun toggleMemorizedSurah(surahNum: Int) {
        memorizedSurahs = if (surahNum in memorizedSurahs) memorizedSurahs - surahNum else memorizedSurahs + surahNum
        sp.edit().putString("memorized_surahs", memorizedSurahs.joinToString(",")).apply()
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
// حسابات الصلاة والقبلة (PrayerCalc)
// ============================================================
object PrayerCalc {
    val names = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
    val methodNames = listOf("رابطة العالم الإسلامي", "الهيئة المصرية العامة للمساحة", "أم القرى - مكة المكرمة")
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
// الإشعارات والتذكيرات (Notifs & Reminders)
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
