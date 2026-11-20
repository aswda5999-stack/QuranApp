package com.quran.app

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.SimpleTimeZone
import java.util.TimeZone
import kotlin.reflect.KProperty
import org.json.JSONArray
import org.json.JSONObject

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
// 3. مفوّضات الحفظ الدائم (تحفظ تلقائياً في SharedPreferences وتحدّث الواجهة فوراً)
// ============================================================
class PersistedInt(
    private val sp: SharedPreferences,
    private val key: String,
    def: Int,
    private val range: IntRange? = null
) {
    private var v by mutableIntStateOf(sp.getInt(key, def).let { x -> range?.let { x.coerceIn(it) } ?: x })
    operator fun getValue(thisRef: Any?, p: KProperty<*>): Int = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: Int) {
        v = range?.let { value.coerceIn(it) } ?: value
        sp.edit().putInt(key, v).apply()
    }
}

class PersistedLong(private val sp: SharedPreferences, private val key: String, def: Long) {
    private var v by mutableLongStateOf(sp.getLong(key, def))
    operator fun getValue(thisRef: Any?, p: KProperty<*>): Long = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: Long) {
        v = value
        sp.edit().putLong(key, v).apply()
    }
}

class PersistedBool(private val sp: SharedPreferences, private val key: String, def: Boolean) {
    private var v by mutableStateOf(sp.getBoolean(key, def))
    operator fun getValue(thisRef: Any?, p: KProperty<*>): Boolean = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: Boolean) {
        v = value
        sp.edit().putBoolean(key, v).apply()
    }
}

class PersistedFloat(
    private val sp: SharedPreferences,
    private val key: String,
    def: Float,
    private val range: ClosedFloatingPointRange<Float>
) {
    private var v by mutableFloatStateOf(sp.getFloat(key, def).coerceIn(range))
    operator fun getValue(thisRef: Any?, p: KProperty<*>): Float = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: Float) {
        v = value.coerceIn(range)
        sp.edit().putFloat(key, v).apply()
    }
}

class PersistedString(private val sp: SharedPreferences, private val key: String, def: String) {
    private var v by mutableStateOf(sp.getString(key, def) ?: def)
    operator fun getValue(thisRef: Any?, p: KProperty<*>): String = v
    operator fun setValue(thisRef: Any?, p: KProperty<*>, value: String) {
        v = value
        sp.edit().putString(key, v).apply()
    }
}

// ============================================================
// 4. الساعة المعتمدة في التطبيق + التحية + التاريخ الهجري والميلادي
// ============================================================
object AppClock {
    /** المنطقة الزمنية المعتمدة: من الجهاز (تلقائي) أو إزاحة UTC يدوية */
    fun zone(st: AppState): TimeZone =
        if (st.timeMode == 1) SimpleTimeZone(st.manualUtcMin * 60_000, "UTC-MANUAL") else TimeZone.getDefault()

    fun calendar(st: AppState, millis: Long = System.currentTimeMillis()): Calendar =
        Calendar.getInstance(zone(st)).apply { timeInMillis = millis }

    fun hourDecimal(cal: Calendar): Double =
        cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) / 60.0 + cal.get(Calendar.SECOND) / 3600.0

    fun greeting(cal: Calendar): String = when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 4..11 -> "صَبَاحُ الْخَيْرِ"
        in 12..16 -> "طَابَ يَوْمُكَ"
        in 17..21 -> "مَسَاءُ الْخَيْرِ"
        else -> "طَابَتْ لَيْلَتُكَ"
    }

    fun greetingSub(cal: Calendar): String = when (cal.get(Calendar.HOUR_OF_DAY)) {
        in 4..11 -> "ابدأ يومك بذكر الله وتلاوة وِردك"
        in 12..16 -> "نَوَّرَ اللَّهُ يَوْمَكَ بِالْقُرْآنِ وَالذِّكْرِ"
        in 17..21 -> "اختم يومك بالأذكار وصفحات من القرآن"
        else -> "لا تنسَ أذكار النوم وقيام الليل"
    }

    fun formatClock(cal: Calendar, use24h: Boolean): String {
        val h = cal.get(Calendar.HOUR_OF_DAY)
        val m = cal.get(Calendar.MINUTE)
        return if (use24h) String.format(Locale.US, "%02d:%02d", h, m)
        else {
            val h12 = when { h == 0 -> 12; h > 12 -> h - 12; else -> h }
            String.format(Locale.US, "%d:%02d %s", h12, m, if (h >= 12) "م" else "ص")
        }
    }

    fun utcLabel(minutes: Int): String {
        val sign = if (minutes >= 0) "+" else "-"
        val a = kotlin.math.abs(minutes)
        return String.format(Locale.US, "UTC%s%02d:%02d", sign, a / 60, a % 60)
    }
}

object ArabicDate {
    val weekdays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    private val monthsGeneral = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )
    private val monthsLevant = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"
    )

    fun weekday(cal: Calendar): String = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]

    fun gregorian(cal: Calendar, style: Int): String {
        val months = if (style == 1) monthsLevant else monthsGeneral
        return "${cal.get(Calendar.DAY_OF_MONTH)} ${months[cal.get(Calendar.MONTH)]} ${cal.get(Calendar.YEAR)} م"
    }
}

object HijriCalendar {
    val monthNames = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
        "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /** يحوّل التاريخ الميلادي إلى هجري (حساب جدولي) ويعيد Triple(سنة، شهر 1..12، يوم) */
    fun fromGregorian(y: Int, m: Int, d: Int): Triple<Int, Int, Int> {
        val a = (14 - m) / 12
        val yy = y + 4800 - a
        val mm = m + 12 * a - 3
        val jd = d + (153 * mm + 2) / 5 + 365 * yy + yy / 4 - yy / 100 + yy / 400 - 32045
        var l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        l = l - 10631 * n + 354
        val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * l) / 709
        val day = l - (709 * month) / 24
        val year = 30 * n + j - 30
        return Triple(year, month, day)
    }

    /** offsetDays: تصحيح يدوي بين -2 و +2 يوم لمطابقة رؤية الهلال في بلدك */
    fun format(cal: Calendar, offsetDays: Int): String {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, offsetDays)
        val (y, m, d) = fromGregorian(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
        return "$d ${monthNames[(m - 1).coerceIn(0, 11)]} $y هـ"
    }
}

// ============================================================
// 5. إدارة الحالة المشتركة والتخزين الدائم (AppState)
// ============================================================
class AppState(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("quran_app_v3_prefs", Context.MODE_PRIVATE)

    // ---- القراءة والإنجاز ----
    var lastPage by PersistedInt(sp, "last_page", 1, 1..604)
    var dailyGoal by PersistedInt(sp, "daily_goal", 5, 1..60)
    var streak by PersistedInt(sp, "streak", 0, 0..100000)
    var totalTasbih by PersistedInt(sp, "total_tasbih", 0, 0..Int.MAX_VALUE)
    var reciterId by PersistedString(sp, "reciter_id", "afs")

    // ---- المظهر والقراءة ----
    var nightMode by PersistedBool(sp, "night_mode", false)
    var paperWarmth by PersistedBool(sp, "paper_warmth", true)
    var keepOn by PersistedBool(sp, "keep_on", true)
    var mushafFontScale by PersistedFloat(sp, "mushaf_font_scale", 1.0f, 0.8f..1.5f)
    var adhkarFontScale by PersistedFloat(sp, "adhkar_font_scale", 1.0f, 0.8f..1.5f)
    var tasbihFontScale by PersistedFloat(sp, "tasbih_font_scale", 1.0f, 0.8f..1.6f)
    var monthStyle by PersistedInt(sp, "month_style", 0, 0..1)     // 0 يناير.. | 1 كانون الثاني..
    var use24h by PersistedBool(sp, "use_24h", false)

    // ---- الصوت والاهتزاز ----
    var soundOn by PersistedBool(sp, "sound_on", true)
    var vibOn by PersistedBool(sp, "vib_on", true)
    var stageVib by PersistedBool(sp, "stage_vib", true)

    // ---- التذكيرات ----
    var dailyReminder by PersistedBool(sp, "daily_reminder", true)
    var reminderHour by PersistedInt(sp, "reminder_hour", 20, 0..23)
    var reminderMinute by PersistedInt(sp, "reminder_minute", 0, 0..59)
    var adhkarAlert by PersistedBool(sp, "adhkar_alert", true)
    var prayerAlert by PersistedBool(sp, "prayer_alert", true)

    // ---- الموقع ----
    var locationMode by PersistedInt(sp, "location_mode", 1, 0..1)   // 0 تلقائي GPS | 1 يدوي
    var autoLocRefresh by PersistedBool(sp, "auto_loc_refresh", true)
    var lastLocFetch by PersistedLong(sp, "last_loc_fetch", 0L)

    private var _geoName by mutableStateOf(sp.getString("geo_name", "مكة المكرمة") ?: "مكة المكرمة")
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

    // ---- الوقت والمواقيت ----
    var timeMode by PersistedInt(sp, "time_mode", 0, 0..1)           // 0 تلقائي من الجهاز | 1 يدوي
    var manualUtcMin by PersistedInt(sp, "manual_utc_min", 180, -720..840)
    var prayerMethod by PersistedInt(sp, "prayer_method", 0, 0..7)
    var asrHanafi by PersistedBool(sp, "asr_hanafi", false)
    var hijriOffset by PersistedInt(sp, "hijri_offset", 0, -2..2)
    var minuteAdjust by PersistedInt(sp, "minute_adjust", 0, -60..60)
    var adjFajr by PersistedInt(sp, "adj_fajr", 0, -30..30)
    var adjSunrise by PersistedInt(sp, "adj_sunrise", 0, -30..30)
    var adjDhuhr by PersistedInt(sp, "adj_dhuhr", 0, -30..30)
    var adjAsr by PersistedInt(sp, "adj_asr", 0, -30..30)
    var adjMaghrib by PersistedInt(sp, "adj_maghrib", 0, -30..30)
    var adjIsha by PersistedInt(sp, "adj_isha", 0, -30..30)

    /** مجموع التصحيح بالدقائق للصلاة: 0 فجر، 1 شروق، 2 ظهر، 3 عصر، 4 مغرب، 5 عشاء */
    fun totalAdjust(index: Int): Int = minuteAdjust + when (index) {
        0 -> adjFajr
        1 -> adjSunrise
        2 -> adjDhuhr
        3 -> adjAsr
        4 -> adjMaghrib
        else -> adjIsha
    }

    fun resetTimeAdjustments() {
        minuteAdjust = 0; adjFajr = 0; adjSunrise = 0; adjDhuhr = 0; adjAsr = 0; adjMaghrib = 0; adjIsha = 0
    }

    // ---- المسبحة (تُحفظ لتبقى بعد إغلاق التطبيق) ----
    var tasbihIndex by PersistedInt(sp, "tasbih_index", 0, 0..1000)
    var tasbihCount by PersistedInt(sp, "tasbih_count", 0, 0..Int.MAX_VALUE)
    var tasbihTarget by PersistedInt(sp, "tasbih_target", 33, 0..100000)
    var tasbihRounds by PersistedInt(sp, "tasbih_rounds", 0, 0..Int.MAX_VALUE)

    val customPhrases = mutableStateListOf<TasbihPhrase>().apply {
        runCatching {
            val arr = JSONArray(sp.getString("custom_tasbih", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(TasbihPhrase(o.getString("t"), o.getString("a"), o.getInt("n")))
            }
        }
    }

    private fun saveCustomPhrases() {
        val arr = JSONArray()
        customPhrases.forEach { arr.put(JSONObject().put("t", it.title).put("a", it.arabicText).put("n", it.defaultTarget)) }
        sp.edit().putString("custom_tasbih", arr.toString()).apply()
    }

    fun addCustomPhrase(p: TasbihPhrase) { customPhrases.add(p); saveCustomPhrases() }
    fun removeCustomPhrase(index: Int) {
        if (index in customPhrases.indices) { customPhrases.removeAt(index); saveCustomPhrases() }
    }

    // ---- المفضلة والصفحات المقروءة والسور المحفوظة ----
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

    // ---- الورد اليومي (صفحات مختلفة في اليوم) + سلسلة الأيام المتتالية ----
    private fun dayKey(offsetDays: Int = 0): String {
        val c = Calendar.getInstance()
        c.add(Calendar.DAY_OF_YEAR, offsetDays)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time)
    }

    private var todayPagesCount by mutableIntStateOf(
        if (sp.getString("today_date", "") == dayKey()) (sp.getStringSet("today_pages", emptySet())?.size ?: 0) else 0
    )

    fun todayPages(): Int = todayPagesCount

    fun markPageRead(page: Int) {
        lastPage = page
        if (page !in readPages) {
            readPages.add(page)
            sp.edit().putStringSet("read_pages", readPages.map { it.toString() }.toSet()).apply()
        }
        val today = dayKey()
        val set = if (sp.getString("today_date", "") == today)
            (sp.getStringSet("today_pages", emptySet()) ?: emptySet()).toMutableSet() else mutableSetOf()
        val isNewDay = sp.getString("today_date", "") != today
        set.add(page.toString())
        sp.edit().putString("today_date", today).putStringSet("today_pages", set).apply()
        todayPagesCount = set.size

        if (isNewDay) {
            val last = sp.getString("streak_last_day", "")
            streak = if (last == dayKey(-1)) streak + 1 else 1
            sp.edit().putString("streak_last_day", today).apply()
        } else if (streak == 0) {
            streak = 1
            sp.edit().putString("streak_last_day", today).apply()
        }
    }

    fun khatmaPercent(): Int = ((readPages.size * 100) / 604).coerceIn(0, 100)

    // ---- تقدّم الأذكار اليومي (يُصفَّر تلقائياً كل يوم جديد) ----
    private val adhkarLeft = mutableStateMapOf<String, Int>().apply {
        if (sp.getString("adhkar_date", "") == dayKey()) {
            sp.all.forEach { (k, v) -> if (k.startsWith("ad_") && v is Int) put(k.removePrefix("ad_"), v) }
        } else {
            sp.edit().apply { sp.all.keys.filter { it.startsWith("ad_") }.forEach { remove(it) } }
                .putString("adhkar_date", dayKey()).apply()
        }
    }

    fun adhkarRemaining(id: String, target: Int): Int = adhkarLeft[id] ?: target

    fun adhkarTick(id: String, target: Int): Int {
        val next = ((adhkarLeft[id] ?: target) - 1).coerceAtLeast(0)
        adhkarLeft[id] = next
        sp.edit().putString("adhkar_date", dayKey()).putInt("ad_$id", next).apply()
        return next
    }

    fun adhkarReset(items: List<DhikrItem>) {
        val e = sp.edit()
        items.forEach { adhkarLeft.remove(it.id); e.remove("ad_${it.id}") }
        e.apply()
    }

    fun adhkarResetAll() {
        adhkarLeft.clear()
        sp.edit().apply { sp.all.keys.filter { it.startsWith("ad_") }.forEach { remove(it) } }.apply()
    }

    fun resetStats() {
        readPages.clear()
        totalTasbih = 0
        streak = 0
        tasbihRounds = 0
        tasbihCount = 0
        todayPagesCount = 0
        sp.edit().remove("read_pages").remove("today_pages").remove("streak_last_day").apply()
    }
}
