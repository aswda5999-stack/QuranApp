package com.quran.app

import android.content.Context
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ============================================================
// 1. النماذج والبيانات الأساسية (Surah, Juz, Reciter, GeoLocation)
// ============================================================
data class Surah(
    val num: Int,
    val name: String,
    val type: String,
    val ayahs: Int,
    val page: Int
)

data class Juz(
    val num: Int,
    val name: String,
    val startSurah: String,
    val page: Int
)

data class Reciter(
    val id: String,
    val name: String,
    val style: String,
    val url: String
)

data class GeoLocation(
    val name: String,
    val lat: Double,
    val lng: Double
)

fun String.normalizeAr(): String = this
    .replace(Regex("[\\u064B-\\u065F\\u0670\\u0640]"), "")
    .replace('أ', 'ا')
    .replace('إ', 'ا')
    .replace('آ', 'ا')
    .replace('ى', 'ي')
    .replace('ة', 'ه')
    .trim()
    .lowercase(Locale.ROOT)

// ============================================================
// 2. مستودع بيانات القرآن الكريم والقراء (QuranData)
// ============================================================
object QuranData {

    val reciters: List<Reciter> = listOf(
        Reciter("afs", "مشاري راشد العفاسي", "مرتل • دولة الكويت", "https://server8.mp3quran.net/afs"),
        Reciter("basit", "عبد الباسط عبد الصمد", "مرتل • مصر", "https://server7.mp3quran.net/basit"),
        Reciter("minsh", "محمد صديق المنشاوي", "مرتل • مصر", "https://server10.mp3quran.net/minsh"),
        Reciter("husary", "محمود خليل الحصري", "مرتل • مصر", "https://server13.mp3quran.net/husr"),
        Reciter("sudais", "عبد الرحمن السديس", "إمام الحرم المكي", "https://server11.mp3quran.net/sds"),
        Reciter("shur", "سعود الشريم", "إمام الحرم المكي", "https://server7.mp3quran.net/shur"),
        Reciter("maher", "ماهر المعيقلي", "إمام الحرم المكي", "https://server12.mp3quran.net/maher"),
        Reciter("yasser", "ياسر الدوسري", "إمام الحرم المكي", "https://server11.mp3quran.net/yasser"),
        Reciter("qatami", "ناصر القطامي", "مرتل • الرياض", "https://server6.mp3quran.net/qtm"),
        Reciter("ajmy", "أحمد بن علي العجمي", "مرتل • السعودية", "https://server10.mp3quran.net/ajm"),
        Reciter("ghamdi", "سعد الغامدي", "مرتل • السعودية", "https://server7.mp3quran.net/s_gmd"),
        Reciter("banna", "محمود علي البنا", "مرتل • مصر", "https://server8.mp3quran.net/bna"),
        Reciter("tablawi", "محمد محمود الطبلاوي", "مرتل • مصر", "https://server12.mp3quran.net/tblawi"),
        Reciter("juhany", "عبد الله عواد الجهني", "إمام الحرم المكي", "https://server13.mp3quran.net/jhn"),
        Reciter("budair", "صلاح البدير", "إمام الحرم النبوي", "https://server6.mp3quran.net/s_bud"),
        Reciter("hudhaify", "علي عبد الرحمن الحذيفي", "إمام الحرم النبوي", "https://server9.mp3quran.net/hthfi"),
        Reciter("fares", "فارس عباد", "مرتل • اليمن", "https://server8.mp3quran.net/frs_a"),
        Reciter("khaled", "خالد الجليل", "مرتل • الرياض", "https://server10.mp3quran.net/jleel"),
        Reciter("baleela", "بندر بليلة", "إمام الحرم المكي", "https://server6.mp3quran.net/balilah"),
        Reciter("luhaidan", "محمد اللحيدان", "مرتل • الرياض", "https://server8.mp3quran.net/lhdan")
    )

    val surahs: List<Surah> = listOf(
        Surah(1, "الفاتحة", "مكية", 7, 1),
        Surah(2, "البقرة", "مدنية", 286, 2),
        Surah(3, "آل عمران", "مدنية", 200, 50),
        Surah(4, "النساء", "مدنية", 176, 77),
        Surah(5, "المائدة", "مدنية", 120, 106),
        Surah(6, "الأنعام", "مكية", 165, 128),
        Surah(7, "الأعراف", "مكية", 206, 151),
        Surah(8, "الأنفال", "مدنية", 75, 177),
        Surah(9, "التوبة", "مدنية", 129, 187),
        Surah(10, "يونس", "مكية", 109, 208),
        Surah(11, "هود", "مكية", 123, 221),
        Surah(12, "يوسف", "مكية", 111, 235),
        Surah(13, "الرعد", "مدنية", 43, 249),
        Surah(14, "إبراهيم", "مكية", 52, 255),
        Surah(15, "الحجر", "مكية", 99, 262),
        Surah(16, "النحل", "مكية", 128, 267),
        Surah(17, "الإسراء", "مكية", 111, 282),
        Surah(18, "الكهف", "مكية", 110, 293),
        Surah(19, "مريم", "مكية", 98, 305),
        Surah(20, "طه", "مكية", 135, 312),
        Surah(21, "الأنبياء", "مكية", 112, 322),
        Surah(22, "الحج", "مدنية", 78, 332),
        Surah(23, "المؤمنون", "مكية", 118, 342),
        Surah(24, "النور", "مدنية", 64, 350),
        Surah(25, "الفرقان", "مكية", 77, 359),
        Surah(26, "الشعراء", "مكية", 227, 367),
        Surah(27, "النمل", "مكية", 93, 377),
        Surah(28, "القصص", "مكية", 88, 385),
        Surah(29, "العنكبوت", "مكية", 69, 396),
        Surah(30, "الروم", "مكية", 60, 404),
        Surah(31, "لقمان", "مكية", 34, 411),
        Surah(32, "السجدة", "مكية", 30, 415),
        Surah(33, "الأحزاب", "مدنية", 73, 418),
        Surah(34, "سبأ", "مكية", 54, 428),
        Surah(35, "فاطر", "مكية", 45, 434),
        Surah(36, "يس", "مكية", 83, 440),
        Surah(37, "الصافات", "مكية", 182, 446),
        Surah(38, "ص", "مكية", 88, 453),
        Surah(39, "الزمر", "مكية", 75, 458),
        Surah(40, "غافر", "مكية", 85, 467),
        Surah(41, "فصلت", "مكية", 54, 477),
        Surah(42, "الشورى", "مكية", 53, 483),
        Surah(43, "الزخرف", "مكية", 89, 489),
        Surah(44, "الدخان", "مكية", 59, 496),
        Surah(45, "الجاثية", "مكية", 37, 499),
        Surah(46, "الأحقاف", "مكية", 35, 502),
        Surah(47, "محمد", "مدنية", 38, 507),
        Surah(48, "الفتح", "مدنية", 29, 511),
        Surah(49, "الحجرات", "مدنية", 18, 515),
        Surah(50, "ق", "مكية", 45, 518),
        Surah(51, "الذاريات", "مكية", 60, 520),
        Surah(52, "الطور", "مكية", 49, 523),
        Surah(53, "النجم", "مكية", 62, 526),
        Surah(54, "القمر", "مكية", 55, 528),
        Surah(55, "الرحمن", "مدنية", 78, 531),
        Surah(56, "الواقعة", "مكية", 96, 534),
        Surah(57, "الحديد", "مدنية", 29, 537),
        Surah(58, "المجادلة", "مدنية", 22, 542),
        Surah(59, "الحشر", "مدنية", 24, 545),
        Surah(60, "الممتحنة", "مدنية", 13, 549),
        Surah(61, "الصف", "مدنية", 14, 551),
        Surah(62, "الجمعة", "مدنية", 11, 553),
        Surah(63, "المنافقون", "مدنية", 11, 554),
        Surah(64, "التغابن", "مدنية", 18, 556),
        Surah(65, "الطلاق", "مدنية", 12, 558),
        Surah(66, "التحريم", "مدنية", 12, 560),
        Surah(67, "الملك", "مكية", 30, 562),
        Surah(68, "القلم", "مكية", 52, 564),
        Surah(69, "الحاقة", "مكية", 52, 566),
        Surah(70, "المعارج", "مكية", 44, 568),
        Surah(71, "نوح", "مكية", 28, 570),
        Surah(72, "الجن", "مكية", 28, 572),
        Surah(73, "المزمل", "مكية", 20, 574),
        Surah(74, "المدثر", "مكية", 56, 575),
        Surah(75, "القيامة", "مكية", 40, 577),
        Surah(76, "الإنسان", "مدنية", 31, 578),
        Surah(77, "المرسلات", "مكية", 50, 580),
        Surah(78, "النبأ", "مكية", 40, 582),
        Surah(79, "النازعات", "مكية", 46, 583),
        Surah(80, "عبس", "مكية", 42, 585),
        Surah(81, "التكوير", "مكية", 29, 586),
        Surah(82, "الانفطار", "مكية", 19, 587),
        Surah(83, "المطففين", "مكية", 36, 587),
        Surah(84, "الانشقاق", "مكية", 25, 589),
        Surah(85, "البروج", "مكية", 22, 590),
        Surah(86, "الطارق", "مكية", 17, 591),
        Surah(87, "الأعلى", "مكية", 19, 591),
        Surah(88, "الغاشية", "مكية", 26, 592),
        Surah(89, "الفجر", "مكية", 30, 593),
        Surah(90, "البلد", "مكية", 20, 594),
        Surah(91, "الشمس", "مكية", 15, 595),
        Surah(92, "الليل", "مكية", 21, 595),
        Surah(93, "الضحى", "مكية", 11, 596),
        Surah(94, "الشرح", "مكية", 8, 596),
        Surah(95, "التين", "مكية", 8, 597),
        Surah(96, "العلق", "مكية", 19, 597),
        Surah(97, "القدر", "مكية", 5, 598),
        Surah(98, "البينة", "مدنية", 8, 598),
        Surah(99, "الزلزلة", "مدنية", 8, 599),
        Surah(100, "العاديات", "مكية", 11, 599),
        Surah(101, "القارعة", "مكية", 11, 600),
        Surah(102, "التكاثر", "مكية", 8, 600),
        Surah(103, "العصر", "مكية", 3, 601),
        Surah(104, "الهمزة", "مكية", 9, 601),
        Surah(105, "الفيل", "مكية", 5, 601),
        Surah(106, "قريش", "مكية", 4, 602),
        Surah(107, "الماعون", "مكية", 7, 602),
        Surah(108, "الكوثر", "مكية", 3, 602),
        Surah(109, "الكافرون", "مكية", 6, 603),
        Surah(110, "النصر", "مدنية", 3, 603),
        Surah(111, "المسد", "مكية", 5, 603),
        Surah(112, "الإخلاص", "مكية", 4, 604),
        Surah(113, "الفلق", "مكية", 5, 604),
        Surah(114, "الناس", "مكية", 6, 604)
    )

    val ajza: List<Juz> = listOf(
        Juz(1, "الجزء الأول", "الفاتحة", 1),
        Juz(2, "الجزء الثاني", "البقرة", 22),
        Juz(3, "الجزء الثالث", "البقرة", 42),
        Juz(4, "الجزء الرابع", "آل عمران", 62),
        Juz(5, "الجزء الخامس", "النساء", 82),
        Juz(6, "الجزء السادس", "النساء", 102),
        Juz(7, "الجزء السابع", "المائدة", 121),
        Juz(8, "الجزء الثامن", "الأنعام", 142),
        Juz(9, "الجزء التاسع", "الأعراف", 162),
        Juz(10, "الجزء العاشر", "الأنفال", 182),
        Juz(11, "الجزء الحادي عشر", "التوبة", 201),
        Juz(12, "الجزء الثاني عشر", "هود", 222),
        Juz(13, "الجزء الثالث عشر", "يوسف", 242),
        Juz(14, "الجزء الرابع عشر", "الحجر", 262),
        Juz(15, "الجزء الخامس عشر", "الإسراء", 282),
        Juz(16, "الجزء السادس عشر", "الكهف", 302),
        Juz(17, "الجزء السابع عشر", "الأنبياء", 322),
        Juz(18, "الجزء الثامن عشر", "المؤمنون", 342),
        Juz(19, "الجزء التاسع عشر", "الفرقان", 362),
        Juz(20, "الجزء العشرون", "النمل", 382),
        Juz(21, "الجزء الحادي والعشرون", "العنكبوت", 402),
        Juz(22, "الجزء الثاني والعشرون", "الأحزاب", 422),
        Juz(23, "الجزء الثالث والعشرون", "يس", 442),
        Juz(24, "الجزء الرابع والعشرون", "الزمر", 462),
        Juz(25, "الجزء الخامس والعشرون", "فصلت", 482),
        Juz(26, "الجزء السادس والعشرون", "الأحقاف", 502),
        Juz(27, "الجزء السابع والعشرون", "الذاريات", 522),
        Juz(28, "الجزء الثامن والعشرون", "المجادلة", 542),
        Juz(29, "الجزء التاسع والعشرون", "الملك", 562),
        Juz(30, "الجزء الثلاثون", "النبأ", 582)
    )

    fun surah(num: Int): Surah? = surahs.firstOrNull { it.num == num }

    fun surahForPage(page: Int): Surah =
        surahs.lastOrNull { it.page <= page.coerceIn(1, 604) } ?: surahs.first()

    fun juzForPage(page: Int): Juz =
        ajza.lastOrNull { it.page <= page.coerceIn(1, 604) } ?: ajza.first()

    fun search(query: String): List<Surah> {
        val q = query.normalizeAr()
        if (q.isEmpty()) return surahs
        return surahs.filter {
            it.name.normalizeAr().contains(q) ||
                it.num.toString() == q ||
                it.type.normalizeAr().contains(q)
        }
    }
}

// ============================================================
// 3. إدارة الحالة المشتركة والتخزين الدائم (AppState)
// ============================================================
class AppState(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("quran_app_v3_prefs", Context.MODE_PRIVATE)

    private fun intState(key: String, def: Int) = mutableIntStateOf(sp.getInt(key, def))
    private fun boolState(key: String, def: Boolean) = mutableStateOf(sp.getBoolean(key, def))
    private fun strState(key: String, def: String) = mutableStateOf(sp.getString(key, def) ?: def)

    private var _lastPage by intState("last_page", 1)
    var lastPage: Int
        get() = _lastPage
        set(v) { _lastPage = v.coerceIn(1, 604); sp.edit().putInt("last_page", _lastPage).apply() }

    private var _dailyGoal by intState("daily_goal", 5)
    var dailyGoal: Int
        get() = _dailyGoal
        set(v) { _dailyGoal = v.coerceIn(1, 60); sp.edit().putInt("daily_goal", _dailyGoal).apply() }

    private var _streak by intState("streak", 1)
    var streak: Int
        get() = _streak
        set(v) { _streak = v.coerceAtLeast(0); sp.edit().putInt("streak", _streak).apply() }

    private var _totalTasbih by intState("total_tasbih", 0)
    var totalTasbih: Int
        get() = _totalTasbih
        set(v) { _totalTasbih = v.coerceAtLeast(0); sp.edit().putInt("total_tasbih", _totalTasbih).apply() }

    private var _reciterId by strState("reciter_id", "afs")
    var reciterId: String
        get() = _reciterId
        set(v) { _reciterId = v; sp.edit().putString("reciter_id", v).apply() }

    // إعدادات المظهر والقراءة
    private var _nightMode by boolState("night_mode", false)
    var nightMode: Boolean
        get() = _nightMode
        set(v) { _nightMode = v; sp.edit().putBoolean("night_mode", v).apply() }

    private var _paperWarmth by boolState("paper_warmth", true)
    var paperWarmth: Boolean
        get() = _paperWarmth
        set(v) { _paperWarmth = v; sp.edit().putBoolean("paper_warmth", v).apply() }

    private var _keepOn by boolState("keep_on", true)
    var keepOn: Boolean
        get() = _keepOn
        set(v) { _keepOn = v; sp.edit().putBoolean("keep_on", v).apply() }

    private var _mushafFontScale by mutableFloatStateOf(sp.getFloat("mushaf_font_scale", 1.0f))
    var mushafFontScale: Float
        get() = _mushafFontScale
        set(v) { _mushafFontScale = v.coerceIn(0.8f, 1.5f); sp.edit().putFloat("mushaf_font_scale", _mushafFontScale).apply() }

    // إعدادات الصوت والاهتزاز
    private var _soundOn by boolState("sound_on", true)
    var soundOn: Boolean
        get() = _soundOn
        set(v) { _soundOn = v; sp.edit().putBoolean("sound_on", v).apply() }

    private var _vibOn by boolState("vib_on", true)
    var vibOn: Boolean
        get() = _vibOn
        set(v) { _vibOn = v; sp.edit().putBoolean("vib_on", v).apply() }

    private var _stageVib by boolState("stage_vib", true)
    var stageVib: Boolean
        get() = _stageVib
        set(v) { _stageVib = v; sp.edit().putBoolean("stage_vib", v).apply() }

    // إعدادات التذكيرات
    private var _dailyReminder by boolState("daily_reminder", true)
    var dailyReminder: Boolean
        get() = _dailyReminder
        set(v) { _dailyReminder = v; sp.edit().putBoolean("daily_reminder", v).apply() }

    private var _reminderHour by intState("reminder_hour", 20)
    var reminderHour: Int
        get() = _reminderHour
        set(v) { _reminderHour = v.coerceIn(0, 23); sp.edit().putInt("reminder_hour", _reminderHour).apply() }

    private var _adhkarAlert by boolState("adhkar_alert", true)
    var adhkarAlert: Boolean
        get() = _adhkarAlert
        set(v) { _adhkarAlert = v; sp.edit().putBoolean("adhkar_alert", v).apply() }

    private var _prayerAlert by boolState("prayer_alert", true)
    var prayerAlert: Boolean
        get() = _prayerAlert
        set(v) { _prayerAlert = v; sp.edit().putBoolean("prayer_alert", v).apply() }

    // إعدادات الموقع وطريقة حساب الصلاة
    private var _prayerMethod by intState("prayer_method", 0)
    var prayerMethod: Int
        get() = _prayerMethod
        set(v) { _prayerMethod = v; sp.edit().putInt("prayer_method", v).apply() }

    private var _geoName by strState("geo_name", "مكة المكرمة")
    private var _geoLat by mutableDoubleStateOf(Double.fromBits(sp.getLong("geo_lat", 21.4225.toBits())))
    private var _geoLng by mutableDoubleStateOf(Double.fromBits(sp.getLong("geo_lng", 39.8262.toBits())))

    val geo: GeoLocation
        get() = GeoLocation(_geoName, _geoLat, _geoLng)

    fun setLocation(name: String, lat: Double, lng: Double) {
        _geoName = name
        _geoLat = lat
        _geoLng = lng
        sp.edit()
            .putString("geo_name", name)
            .putLong("geo_lat", lat.toBits())
            .putLong("geo_lng", lng.toBits())
            .apply()
    }

    // المفضلة، الصفحات المقروءة، والسور المحفوظة
    val bookmarks = mutableStateListOf<Int>().apply {
        addAll((sp.getStringSet("bookmarks", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted())
    }

    fun toggleBookmark(page: Int) {
        if (page in bookmarks) bookmarks.remove(page) else { bookmarks.add(page); bookmarks.sort() }
        sp.edit().putStringSet("bookmarks", bookmarks.map { it.toString() }.toSet()).apply()
    }

    val readPages = mutableStateListOf<Int>().apply {
        addAll((sp.getStringSet("read_pages", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() })
    }

    val memorizedSurahs = mutableStateListOf<Int>().apply {
        addAll((sp.getStringSet("memorized_surahs", emptySet()) ?: emptySet()).mapNotNull { it.toIntOrNull() }.sorted())
    }

    fun toggleMemorizedSurah(num: Int) {
        if (num in memorizedSurahs) memorizedSurahs.remove(num) else { memorizedSurahs.add(num); memorizedSurahs.sort() }
        sp.edit().putStringSet("memorized_surahs", memorizedSurahs.map { it.toString() }.toSet()).apply()
    }

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun todayPages(): Int {
        val savedDay = sp.getString("today_date", "")
        return if (savedDay == todayKey()) sp.getInt("today_count", 0) else 0
    }

    fun markPageRead(page: Int) {
        lastPage = page
        if (page !in readPages) {
            readPages.add(page)
            sp.edit().putStringSet("read_pages", readPages.map { it.toString() }.toSet()).apply()
        }
        val today = todayKey()
        val prevDay = sp.getString("today_date", "")
        val newCount = if (prevDay == today) sp.getInt("today_count", 0) + 1 else 1
        sp.edit().putString("today_date", today).putInt("today_count", newCount).apply()
    }

    fun khatmaPercent(): Int = ((readPages.size * 100) / 604).coerceIn(0, 100)

    fun resetStats() {
        readPages.clear()
        totalTasbih = 0
        streak = 0
        sp.edit()
            .remove("read_pages")
            .putInt("today_count", 0)
            .apply()
    }
}
