@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ============================================================
// 1. قائمة أشهر 15 مؤذناً بأصوات عالية ومميزة
// ============================================================
data class MuezzinVoice(
    val id: String,
    val name: String,
    val title: String,
    val url: String
)

val Top15Muezzins: List<MuezzinVoice> = listOf(
    MuezzinVoice("afasy", "مشاري راشد العفاسي", "الكويت • صوت نديّ وعالٍ", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/mishary-rashid-alafasy-adhan.mp3"),
    MuezzinVoice("mulla", "علي أحمد ملا", "شيخ مؤذني الحرم المكي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/ali-ahmed-mulla-adhan.mp3"),
    MuezzinVoice("basit", "عبد الباسط عبد الصمد", "مصر • أذان خاشع وقوي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/abdul-basit-abdus-samad-adhan.mp3"),
    MuezzinVoice("qatami", "ناصر القطامي", "الرياض • نبرة مؤثرة ومميزة", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/nasser-al-qatami-adhan.mp3"),
    MuezzinVoice("dosari", "ياسر الدوسري", "إمام الحرم المكي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/yasser-al-dosari-adhan.mp3"),
    MuezzinVoice("maher", "ماهر المعيقلي", "إمام الحرم المكي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/maher-al-mueaqly-adhan.mp3"),
    MuezzinVoice("sudais", "عبد الرحمن السديس", "إمام الحرم المكي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/abdurrahman-as-sudais-adhan.mp3"),
    MuezzinVoice("minshawi", "محمد صديق المنشاوي", "مصر • المدرسة الكلاسيكية", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/muhammad-siddiq-al-minshawi-adhan.mp3"),
    MuezzinVoice("husary", "محمود خليل الحصري", "مصر • أداء متقن وواضح", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/mahmoud-khalil-al-husary-adhan.mp3"),
    MuezzinVoice("fares", "فارس عباد", "اليمن • صوت شجيّ ومرتفع", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/fares-abbad-adhan.mp3"),
    MuezzinVoice("rifat", "محمد رفعت", "مصر • قيثارة السماء", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/muhammad-rifat-adhan.mp3"),
    MuezzinVoice("daghriri", "حمد الدغريري", "مؤذن الحرم المكي", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/hamad-aldaghreeri-adhan.mp3"),
    MuezzinVoice("bukhari", "عصام البخاري", "مؤذن الحرم النبوي الشريف", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/essam-al-bukhari-adhan.mp3"),
    MuezzinVoice("aqsa", "أذان المسجد الأقصى", "القدس الشريف", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/al-aqsa-mosque-adhan.mp3"),
    MuezzinVoice("makkah", "أذان الحرم المكي الشريف", "مكة المكرمة", "https://media.blubrry.com/muslim_central_quran/podcasts.qurancentral.com/adhan/makkah-adhan.mp3")
)

// ============================================================
// 2. محرك الحسابات الفلكية لمواقيت الصلاة واتجاه القبلة (PrayerCalc)
// ============================================================
data class PrayerTimes(
    val fajr: Double,
    val sunrise: Double,
    val dhuhr: Double,
    val asr: Double,
    val maghrib: Double,
    val isha: Double
)

/** حدث صلاة محدد بلحظة زمنية مطلقة. index: 0 فجر، 1 ظهر، 2 عصر، 3 مغرب، 4 عشاء */
data class PrayerEvent(val index: Int, val name: String, val millis: Long)

object PrayerCalc {
    val methodNames = listOf(
        "رابطة العالم الإسلامي (MWL)",
        "أم القرى (مكة المكرمة)",
        "الهيئة المصرية العامة للمساحة",
        "جامعة العلوم الإسلامية بكراتشي",
        "الجمعية الإسلامية لأمريكا الشمالية (ISNA)",
        "الكويت",
        "قطر",
        "رئاسة الشؤون الدينية التركية"
    )

    val presets: List<GeoLocation> = listOf(
        GeoLocation("مكة المكرمة", 21.4225, 39.8262),
        GeoLocation("المدينة المنورة", 24.4672, 39.6111),
        GeoLocation("جدة", 21.4858, 39.1925),
        GeoLocation("الرياض", 24.7136, 46.6753),
        GeoLocation("الدمام", 26.4207, 50.0888),
        GeoLocation("القدس الشريف", 31.7683, 35.2137),
        GeoLocation("غزة", 31.5017, 34.4668),
        GeoLocation("رام الله", 31.9038, 35.2034),
        GeoLocation("عمّان", 31.9454, 35.9284),
        GeoLocation("بيروت", 33.8938, 35.5018),
        GeoLocation("طرابلس (لبنان)", 34.4367, 35.8497),
        GeoLocation("حلب", 36.2021, 37.1343),
        GeoLocation("دمشق", 33.5138, 36.2765),
        GeoLocation("حمص", 34.7324, 36.7137),
        GeoLocation("حماة", 35.1318, 36.7578),
        GeoLocation("إدلب", 35.9306, 36.6339),
        GeoLocation("اللاذقية", 35.5317, 35.7900),
        GeoLocation("طرطوس", 34.8890, 35.8866),
        GeoLocation("الرقة", 35.9528, 39.0079),
        GeoLocation("دير الزور", 35.3359, 40.1408),
        GeoLocation("الحسكة", 36.5024, 40.7477),
        GeoLocation("درعا", 32.6189, 36.1021),
        GeoLocation("بغداد", 33.3152, 44.3661),
        GeoLocation("الموصل", 36.3350, 43.1189),
        GeoLocation("البصرة", 30.5085, 47.7835),
        GeoLocation("أربيل", 36.1911, 44.0092),
        GeoLocation("الكويت", 29.3759, 47.9774),
        GeoLocation("المنامة", 26.2285, 50.5860),
        GeoLocation("الدوحة", 25.2854, 51.5310),
        GeoLocation("دبي", 25.2048, 55.2708),
        GeoLocation("أبوظبي", 24.4539, 54.3773),
        GeoLocation("مسقط", 23.5880, 58.3829),
        GeoLocation("صنعاء", 15.3694, 44.1910),
        GeoLocation("عدن", 12.7855, 45.0187),
        GeoLocation("القاهرة", 30.0444, 31.2357),
        GeoLocation("الإسكندرية", 31.2001, 29.9187),
        GeoLocation("الخرطوم", 15.5007, 32.5599),
        GeoLocation("طرابلس (ليبيا)", 32.8872, 13.1913),
        GeoLocation("تونس", 36.8065, 10.1815),
        GeoLocation("الجزائر", 36.7538, 3.0588),
        GeoLocation("الرباط", 34.0209, -6.8416),
        GeoLocation("الدار البيضاء", 33.5731, -7.5898),
        GeoLocation("إسطنبول", 41.0082, 28.9784),
        GeoLocation("أنقرة", 39.9334, 32.8597),
        GeoLocation("غازي عنتاب", 37.0662, 37.3833),
        GeoLocation("طهران", 35.6892, 51.3890),
        GeoLocation("كراتشي", 24.8607, 67.0011),
        GeoLocation("إسلام آباد", 33.6844, 73.0479),
        GeoLocation("كوالالمبور", 3.1390, 101.6869),
        GeoLocation("جاكرتا", -6.2088, 106.8456),
        GeoLocation("لندن", 51.5074, -0.1278),
        GeoLocation("باريس", 48.8566, 2.3522),
        GeoLocation("برلين", 52.5200, 13.4050),
        GeoLocation("نيويورك", 40.7128, -74.0060),
        GeoLocation("تورنتو", 43.6532, -79.3832)
    )

    private fun dtr(d: Double) = d * PI / 180.0
    private fun rtd(r: Double) = r * 180.0 / PI
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double): Double = h - 24.0 * floor(h / 24.0)

    /** يحسب مواقيت يوم التقويم cal (بمنطقته الزمنية) بدون أي تصحيح يدوي */
    fun compute(cal: Calendar, lat: Double, lng: Double, method: Int, hanafi: Boolean = false): PrayerTimes {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val tz = cal.timeZone.getOffset(cal.timeInMillis) / 3_600_000.0

        var yr = y
        var mo = m
        if (mo <= 2) { yr -= 1; mo += 12 }
        val a = floor(yr / 100.0)
        val b = 2 - a + floor(a / 4.0)
        val jd = floor(365.25 * (yr + 4716)) + floor(30.6001 * (mo + 1)) + d + b - 1524.5 - lng / 360.0

        val n = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * n)
        val q = fixAngle(280.459 + 0.98564736 * n)
        val l = fixAngle(q + 1.915 * sin(dtr(g)) + 0.020 * sin(dtr(2 * g)))
        val e = 23.439 - 0.00000036 * n
        val decl = rtd(asin(sin(dtr(e)) * sin(dtr(l))))
        var ra = rtd(atan2(cos(dtr(e)) * sin(dtr(l)), cos(dtr(l)))) / 15.0
        ra = fixHour(ra)
        val eqTime = q / 15.0 - ra

        val dhuhr = fixHour(12.0 + tz - lng / 15.0 - eqTime)

        fun sunAngleTime(angle: Double, ccw: Boolean): Double {
            val v = (-sin(dtr(angle)) - sin(dtr(decl)) * sin(dtr(lat))) / (cos(dtr(decl)) * cos(dtr(lat)))
            val t = rtd(acos(v.coerceIn(-1.0, 1.0))) / 15.0
            return dhuhr + if (ccw) -t else t
        }

        // (زاوية الفجر، زاوية العشاء، فاصل العشاء بالدقائق بعد المغرب إن وُجد)
        val (fajrAngle, ishaAngle, ishaInterval) = when (method) {
            1 -> Triple(18.5, 0.0, 90.0)
            2 -> Triple(19.5, 17.5, 0.0)
            3 -> Triple(18.0, 18.0, 0.0)
            4 -> Triple(15.0, 15.0, 0.0)
            5 -> Triple(18.0, 17.5, 0.0)
            6 -> Triple(18.0, 0.0, 90.0)
            7 -> Triple(18.0, 17.0, 0.0)
            else -> Triple(18.0, 17.0, 0.0)
        }

        val fajr = sunAngleTime(fajrAngle, true)
        val sunrise = sunAngleTime(0.833, true)
        val maghrib = sunAngleTime(0.833, false)
        val isha = if (ishaInterval > 0) maghrib + ishaInterval / 60.0 else sunAngleTime(ishaAngle, false)

        val factor = if (hanafi) 2.0 else 1.0
        val asrAngle = -rtd(atan(1.0 / (factor + tan(dtr(abs(lat - decl))))))
        val asr = sunAngleTime(asrAngle, false)

        val maghribFixed = fixHour(maghrib)
        var ishaFixed = fixHour(isha)
        if (ishaFixed < maghribFixed) ishaFixed += 24.0   // عشاء بعد منتصف الليل

        return PrayerTimes(
            fajr = fixHour(fajr),
            sunrise = fixHour(sunrise),
            dhuhr = fixHour(dhuhr),
            asr = fixHour(asr),
            maghrib = maghribFixed,
            isha = ishaFixed
        )
    }

    /** المواقيت النهائية مع التصحيحات اليدوية (عامة + لكل صلاة) */
    fun timesFor(st: AppState, cal: Calendar): PrayerTimes {
        val t = compute(cal, st.geo.lat, st.geo.lng, st.prayerMethod, st.asrHanafi)
        fun adj(h: Double, i: Int) = h + st.totalAdjust(i) / 60.0
        return PrayerTimes(
            adj(t.fajr, 0), adj(t.sunrise, 1), adj(t.dhuhr, 2),
            adj(t.asr, 3), adj(t.maghrib, 4), adj(t.isha, 5)
        )
    }

    fun dayStartMillis(st: AppState, base: Long, dayOffset: Int): Long {
        val c = AppClock.calendar(st, base)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        c.add(Calendar.DAY_OF_YEAR, dayOffset)
        return c.timeInMillis
    }

    /** أحداث الصلوات الخمس (من أمس حتى days-1 يوماً قادمة) مرتبة زمنياً بلحظات مطلقة */
    fun events(st: AppState, fromMillis: Long, days: Int = 3): List<PrayerEvent> {
        val out = mutableListOf<PrayerEvent>()
        for (off in -1 until days) {
            val start = dayStartMillis(st, fromMillis, off)
            val cal = AppClock.calendar(st, start + 12 * 3_600_000L)
            val t = timesFor(st, cal)
            listOf("الفجر" to t.fajr, "الظهر" to t.dhuhr, "العصر" to t.asr, "المغرب" to t.maghrib, "العشاء" to t.isha)
                .forEachIndexed { i, (name, h) ->
                    out.add(PrayerEvent(i, name, start + (h * 3_600_000.0).toLong()))
                }
        }
        return out.sortedBy { it.millis }
    }

    fun next(st: AppState, nowMillis: Long): PrayerEvent =
        events(st, nowMillis, 3).first { it.millis > nowMillis }

    fun formatTime(hours: Double, use24h: Boolean = false): String {
        val totalMin = ((hours * 60.0).roundToInt() % 1440 + 1440) % 1440
        val h = totalMin / 60
        val m = totalMin % 60
        if (use24h) return String.format(Locale.US, "%02d:%02d", h, m)
        val suffix = if (h >= 12) "م" else "ص"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, suffix)
    }

    fun formatMillis(st: AppState, millis: Long): String =
        AppClock.formatClock(AppClock.calendar(st, millis), st.use24h)

    fun remaining(ms: Long): String {
        val s = (ms / 1000L).coerceAtLeast(0L)
        return String.format(Locale.US, "%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    fun remainingWords(ms: Long): String {
        val totalMin = (ms / 60_000L).coerceAtLeast(0L)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 && m > 0 -> "$h س و$m د"
            h > 0 -> "$h س"
            else -> "$m د"
        }
    }

    fun qiblaBearing(lat: Double, lng: Double): Double {
        val mLat = dtr(21.4225)
        val mLng = dtr(39.8262)
        val uLat = dtr(lat)
        val dLng = mLng - dtr(lng)
        val y = sin(dLng)
        val x = cos(uLat) * tan(mLat) - sin(uLat) * cos(dLng)
        return (rtd(atan2(y, x)) + 360.0) % 360.0
    }
}

// ============================================================
// 3. تحديد الموقع: تلقائي (GPS/الشبكة) أو يدوي — مع إذن الموقع واسم المدينة
// ============================================================
enum class LocStatus { OK, NO_PERMISSION, GPS_OFF, FAILED }

object LocationHelper {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun hasPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isServiceEnabled(ctx: Context): Boolean {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return runCatching { LocationManagerCompat.isLocationEnabled(lm) }.getOrDefault(false)
    }

    fun openLocationSettings(ctx: Context) {
        runCatching {
            ctx.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** يطلب موقعاً حقيقياً (لا يكتفي بآخر موقع محفوظ) من GPS والشبكة معاً، مع مهلة 15 ثانية */
    @SuppressLint("MissingPermission")
    private fun requestFix(ctx: Context, callback: (Location?) -> Unit) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        if (lm == null) { callback(null); return }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) { callback(null); return }

        val last = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        var delivered = false
        var remaining = providers.size
        val signals = mutableListOf<CancellationSignal>()
        val handler = Handler(Looper.getMainLooper())

        fun finish(loc: Location?) {
            if (delivered) return
            delivered = true
            signals.forEach { runCatching { it.cancel() } }
            callback(loc ?: last)
        }

        providers.forEach { prov ->
            val cs = CancellationSignal()
            signals.add(cs)
            runCatching {
                LocationManagerCompat.getCurrentLocation(lm, prov, cs, ContextCompat.getMainExecutor(ctx)) { loc ->
                    if (loc != null) finish(loc) else {
                        remaining--
                        if (remaining <= 0) finish(null)
                    }
                }
            }.onFailure {
                remaining--
                if (remaining <= 0) finish(null)
            }
        }
        handler.postDelayed({ finish(null) }, 15_000L)
    }

    private suspend fun cityName(ctx: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            if (!Geocoder.isPresent()) return@runCatching null
            @Suppress("DEPRECATION")
            val list = Geocoder(ctx, Locale("ar")).getFromLocation(lat, lng, 1)
            val a = list?.firstOrNull()
            a?.locality ?: a?.subAdminArea ?: a?.adminArea
        }.getOrNull()
    }

    /** يحدّد الموقع ويحفظه ويعيد جدولة الأذان. onResult(الحالة، اسم المدينة) */
    fun detectAndSave(ctx: Context, st: AppState, onResult: (LocStatus, String) -> Unit) {
        val appCtx = ctx.applicationContext
        if (!hasPermission(appCtx)) { onResult(LocStatus.NO_PERMISSION, ""); return }
        if (!isServiceEnabled(appCtx)) { onResult(LocStatus.GPS_OFF, ""); return }
        requestFix(appCtx) { loc ->
            if (loc == null) {
                onResult(LocStatus.FAILED, "")
            } else {
                st.setLocation("موقعي الحالي", loc.latitude, loc.longitude)
                st.lastLocFetch = System.currentTimeMillis()
                scope.launch {
                    val name = cityName(appCtx, loc.latitude, loc.longitude)
                    if (name != null) st.setLocation(name, loc.latitude, loc.longitude)
                    AdhanEngine.scheduleAll(appCtx, st)
                    onResult(LocStatus.OK, name ?: "موقعي الحالي")
                }
            }
        }
    }

    /** يُستدعى عند فتح التطبيق: يحدّث الموقع بصمت إن كان الوضع تلقائياً ومرّت 6 ساعات */
    fun refreshIfNeeded(ctx: Context, st: AppState) {
        if (st.locationMode != 0 || !st.autoLocRefresh) return
        if (System.currentTimeMillis() - st.lastLocFetch < 6 * 3_600_000L) return
        if (!hasPermission(ctx) || !isServiceEnabled(ctx)) return
        detectAndSave(ctx, st) { _, _ -> }
    }

    fun parseCoord(raw: String): Double? {
        val sb = StringBuilder()
        raw.trim().forEach { ch ->
            when (ch) {
                in '٠'..'٩' -> sb.append('0' + (ch - '٠'))
                '٫', ',', '،' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString().toDoubleOrNull()
    }
}

/** يعيد دالة تطلب إذن الموقع عند الحاجة ثم تحدّد الموقع تلقائياً وتبلّغ بالنتيجة */
@Composable
fun rememberAutoLocationAction(onStatus: (LocStatus, String) -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val st = LocalApp.current
    val currentOnStatus by rememberUpdatedState(onStatus)
    val run: () -> Unit = remember {
        { LocationHelper.detectAndSave(ctx, st) { s, n -> currentOnStatus(s, n) } }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        if (perms.values.any { it }) run() else currentOnStatus(LocStatus.NO_PERMISSION, "")
    }
    return remember(launcher) {
        {
            if (LocationHelper.hasPermission(ctx)) run()
            else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}

fun locStatusMessage(s: LocStatus, name: String): String = when (s) {
    LocStatus.OK -> "تم تحديد موقعك: $name ✓"
    LocStatus.NO_PERMISSION -> "لم يتم منح إذن الموقع. اسمح به من نافذة الإذن أو من إعدادات التطبيق."
    LocStatus.GPS_OFF -> "خدمة الموقع (GPS) متوقفة في هاتفك. فعّلها ثم أعد المحاولة."
    LocStatus.FAILED -> "تعذّر تحديد الموقع. جرّب في مكان مكشوف أو اختر مدينتك يدوياً."
}

// ============================================================
// 4. محرك الأذان والتذكيرات (AdhanEngine & AdhanAlarmReceiver)
// ============================================================
object AdhanEngine {
    private const val PREF_NAME = "adhan_engine_prefs"
    private var adhanPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeMuezzinId = MutableStateFlow("afasy")
    val activeMuezzinId: StateFlow<String> = _activeMuezzinId.asStateFlow()

    fun getSelectedMuezzin(ctx: Context): MuezzinVoice {
        val sp = ctx.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val id = sp.getString("muezzin_id", "afasy") ?: "afasy"
        _activeMuezzinId.value = id
        return Top15Muezzins.firstOrNull { it.id == id } ?: Top15Muezzins.first()
    }

    fun setSelectedMuezzin(ctx: Context, id: String) {
        val appCtx = ctx.applicationContext
        appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit().putString("muezzin_id", id).apply()
        _activeMuezzinId.value = id
        val voice = getSelectedMuezzin(appCtx)
        // تحميل صوت المؤذن فوراً في الخلفية ليعمل بدون إنترنت لحظة دخول وقت الصلاة
        scope.launch {
            val outFile = localVoiceFile(appCtx, voice.id)
            if (!outFile.exists() || outFile.length() < 4096) {
                QuranAudioEngine.downloadWithResume(appCtx, voice.url, outFile)
            }
        }
    }

    private fun localVoiceFile(ctx: Context, id: String): File {
        val dir = File(ctx.filesDir, "adhan_voices").apply { mkdirs() }
        return File(dir, "muezzin_$id.mp3")
    }

    fun playAdhanNow(ctx: Context, prayerName: String = "الصلاة") {
        val appCtx = ctx.applicationContext
        QuranAudioEngine.pauseIfPlaying(appCtx)
        stopAdhan(appCtx)

        val voice = getSelectedMuezzin(appCtx)
        val localFile = localVoiceFile(appCtx, voice.id)
        val player = MediaPlayer()
        adhanPlayer = player

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK)

            if (localFile.exists() && localFile.length() > 4096) {
                player.setDataSource(localFile.absolutePath)
            } else {
                player.setDataSource(voice.url)
                // حفظ نسخة محلية للمرات القادمة
                scope.launch { QuranAudioEngine.downloadWithResume(appCtx, voice.url, localFile) }
            }

            player.setOnPreparedListener { m ->
                m.start()
                _isPlaying.value = true
                showAdhanNotification(appCtx, prayerName, voice.name)
            }
            player.setOnCompletionListener {
                stopAdhan(appCtx)
            }
            player.setOnErrorListener { _, _, _ ->
                playFallbackAlarmTone(appCtx, prayerName)
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            playFallbackAlarmTone(appCtx, prayerName)
        }
    }

    private fun playFallbackAlarmTone(ctx: Context, prayerName: String) {
        runCatching {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(ctx, alarmUri)
                isLooping = false
                prepare()
                start()
            }
            adhanPlayer = mp
            _isPlaying.value = true
            showAdhanNotification(ctx, prayerName, "تنبيه دخول وقت الصلاة")
        }
    }

    fun stopAdhan(ctx: Context) {
        runCatching {
            adhanPlayer?.stop()
            adhanPlayer?.reset()
            adhanPlayer?.release()
        }
        adhanPlayer = null
        _isPlaying.value = false
        NotificationManagerCompat.from(ctx.applicationContext).cancel(88)
    }

    private fun showAdhanNotification(ctx: Context, prayerName: String, muezzinName: String) {
        Notifs.ensureChannels(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return

        val stopIntent = Intent(ctx, AdhanAlarmReceiver::class.java).setAction("ACTION_STOP_ADHAN")
        val stopPi = PendingIntent.getBroadcast(
            ctx, 880, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(ctx, Notifs.CH_ADHAN)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("حان الآن موعد أذان $prayerName")
            .setContentText("بصوت المؤذن: $muezzinName")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(Notifs.contentIntent(ctx))
            .setFullScreenIntent(Notifs.contentIntent(ctx), true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "إيقاف الأذان", stopPi)
            .build()

        try { nm.notify(88, notif) } catch (_: SecurityException) { }
    }

    fun showReminder(ctx: Context, kind: String) {
        Notifs.ensureChannels(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val (title, text, open) = when (kind) {
            "morning" -> Triple("أذكار الصباح", "حان وقت أذكار الصباح، حصّن يومك بذكر الله.", "adhkar_morning")
            "evening" -> Triple("أذكار المساء", "حان وقت أذكار المساء، اختم نهارك بالذكر.", "adhkar_evening")
            else -> Triple("وردك اليومي من القرآن", "لا تنسَ وردك اليوم، صفحات يسيرة تنير يومك.", "wird")
        }
        val id = when (kind) { "morning" -> 91; "evening" -> 92; else -> 93 }
        val notif = NotificationCompat.Builder(ctx, Notifs.CH_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(Notifs.contentIntent(ctx, open, id))
            .build()
        try { nm.notify(id, notif) } catch (_: SecurityException) { }
    }

    // ============================================================
    // الجدولة: الأذان بـ setAlarmClock (دقيق ويخترق وضع السكون) + تذكيرات الأذكار والورد
    // ============================================================
    private fun alarmIntent(ctx: Context, code: Int, action: String, extra: String? = null, flags: Int): PendingIntent? {
        val intent = Intent(ctx, AdhanAlarmReceiver::class.java).apply {
            this.action = action
            if (extra != null) putExtra("prayer_name", extra)
        }
        return PendingIntent.getBroadcast(ctx, code, intent, flags)
    }

    private fun cancelCode(ctx: Context, am: AlarmManager, code: Int, action: String) {
        val pi = alarmIntent(ctx, code, action, null, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE)
        if (pi != null) am.cancel(pi)
    }

    private fun setSoftAlarm(am: AlarmManager, millis: Long, pi: PendingIntent) {
        runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms() ->
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
                else -> am.set(AlarmManager.RTC_WAKEUP, millis, pi)
            }
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAll(ctx: Context, st: AppState) {
        val appCtx = ctx.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        val events = PrayerCalc.events(st, now, 3)
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        // ---- الأذان ----
        if (st.prayerAlert) {
            val voice = getSelectedMuezzin(appCtx)
            scope.launch {
                val f = localVoiceFile(appCtx, voice.id)
                if (!f.exists() || f.length() < 4096) {
                    QuranAudioEngine.downloadWithResume(appCtx, voice.url, f)
                }
            }
            for (idx in 0..4) {
                val ev = events.firstOrNull { it.index == idx && it.millis > now + 5_000L } ?: continue
                val pi = alarmIntent(appCtx, 101 + idx, "ACTION_TRIGGER_ADHAN", ev.name, flags) ?: continue
                try {
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(ev.millis, Notifs.contentIntent(appCtx, null, 90)), pi)
                } catch (_: SecurityException) {
                    setSoftAlarm(am, ev.millis, pi)
                }
            }
        } else {
            for (idx in 0..4) cancelCode(appCtx, am, 101 + idx, "ACTION_TRIGGER_ADHAN")
        }

        // ---- تذكير أذكار الصباح (بعد الفجر بنصف ساعة) وأذكار المساء (بعد العصر بنصف ساعة) ----
        val half = 30 * 60_000L
        if (st.adhkarAlert) {
            events.filter { it.index == 0 && it.millis + half > now + 5_000L }.minByOrNull { it.millis }?.let { ev ->
                alarmIntent(appCtx, 201, "ACTION_REMINDER", "morning", flags)?.let { setSoftAlarm(am, ev.millis + half, it) }
            }
            events.filter { it.index == 2 && it.millis + half > now + 5_000L }.minByOrNull { it.millis }?.let { ev ->
                alarmIntent(appCtx, 202, "ACTION_REMINDER", "evening", flags)?.let { setSoftAlarm(am, ev.millis + half, it) }
            }
        } else {
            cancelCode(appCtx, am, 201, "ACTION_REMINDER")
            cancelCode(appCtx, am, 202, "ACTION_REMINDER")
        }

        // ---- تذكير الورد اليومي بالوقت الذي يختاره المستخدم ----
        if (st.dailyReminder) {
            val c = AppClock.calendar(st, now).apply {
                set(Calendar.HOUR_OF_DAY, st.reminderHour)
                set(Calendar.MINUTE, st.reminderMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (c.timeInMillis <= now + 5_000L) c.add(Calendar.DAY_OF_YEAR, 1)
            alarmIntent(appCtx, 203, "ACTION_REMINDER", "wird", flags)?.let { setSoftAlarm(am, c.timeInMillis, it) }
        } else {
            cancelCode(appCtx, am, 203, "ACTION_REMINDER")
        }
    }

    fun cancelAll(ctx: Context) {
        val appCtx = ctx.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (idx in 0..4) cancelCode(appCtx, am, 101 + idx, "ACTION_TRIGGER_ADHAN")
    }

    fun ensureSchedulesOnBoot(ctx: Context) {
        val appCtx = ctx.applicationContext
        scheduleAll(appCtx, AppState(appCtx))
    }
}

class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appCtx = context.applicationContext
        when (intent?.action) {
            "ACTION_STOP_ADHAN" -> AdhanEngine.stopAdhan(appCtx)
            "ACTION_TRIGGER_ADHAN" -> {
                // إيقاظ المعالج فوراً لضمان تشغيل الأذان حتى والشاشة مغلقة
                val pm = appCtx.getSystemService(Context.POWER_SERVICE) as? PowerManager
                val wl = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "QuranApp:AdhanWakeLock")
                wl?.acquire(60_000L)

                val prayerName = intent?.getStringExtra("prayer_name") ?: "الصلاة"
                AdhanEngine.playAdhanNow(appCtx, prayerName)
                AdhanEngine.ensureSchedulesOnBoot(appCtx)
            }
            "ACTION_REMINDER" -> {
                AdhanEngine.showReminder(appCtx, intent?.getStringExtra("prayer_name") ?: "wird")
                AdhanEngine.ensureSchedulesOnBoot(appCtx)
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> AdhanEngine.ensureSchedulesOnBoot(appCtx)
        }
    }
}

// ============================================================
// 5. نوافذ مشتركة: اختيار المدينة/الموقع، الإحداثيات اليدوية، المؤذن، طريقة الحساب
// ============================================================
@Composable
fun LocationPickerDialog(onDismiss: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<String?>(null) }
    var gpsOff by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showCoords by remember { mutableStateOf(false) }

    val detect = rememberAutoLocationAction { s, name ->
        busy = false
        gpsOff = s == LocStatus.GPS_OFF
        status = locStatusMessage(s, name)
        if (s == LocStatus.OK) {
            st.locationMode = 0
            Toast.makeText(ctx, "تم تحديث الموقع: $name ✓", Toast.LENGTH_SHORT).show()
            onDismiss()
        }
    }

    val cities = remember(query) {
        val q = query.normalizeAr()
        if (q.isEmpty()) PrayerCalc.presets else PrayerCalc.presets.filter { it.name.normalizeAr().contains(q) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = p.card,
        title = { Text("تحديد الموقع", fontWeight = FontWeight.ExtraBold, color = p.ink) },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Button(
                        onClick = { fx.click(); busy = true; status = "جارٍ تحديد موقعك..."; detect() },
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = RoyalForest, strokeWidth = 2.dp)
                        else Icon(Icons.Default.MyLocation, null, tint = RoyalForest)
                        Spacer(Modifier.width(8.dp))
                        Text("تحديد موقعي تلقائياً (GPS)", color = RoyalForest, fontWeight = FontWeight.ExtraBold)
                    }
                }
                if (status != null) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(status!!, color = if (gpsOff) DangerRed else EmeraldVibrant, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            if (gpsOff) {
                                OutlinedButton(
                                    onClick = { LocationHelper.openLocationSettings(ctx) },
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("فتح إعدادات الموقع", fontWeight = FontWeight.ExtraBold) }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = { fx.click(); showCoords = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EditLocationAlt, null, tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("إدخال الاسم والإحداثيات يدوياً", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 13.sp)
                    }
                }
                item {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("ابحث عن مدينتك...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(cities, key = { it.name }) { city ->
                    val active = city.name == st.geo.name
                    Surface(
                        onClick = {
                            fx.click()
                            st.setLocation(city.name, city.lat, city.lng)
                            st.locationMode = 1
                            AdhanEngine.scheduleAll(ctx, st)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) EmeraldVibrant else p.bg,
                        border = BorderStroke(1.dp, if (active) GoldShine else p.border)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                city.name,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (active) Color.White else p.ink,
                                modifier = Modifier.weight(1f)
                            )
                            if (active) Icon(Icons.Default.CheckCircle, null, tint = GoldShine)
                        }
                    }
                }
                if (cities.isEmpty()) {
                    item { Text("لا توجد مدينة بهذا الاسم، استخدم الإحداثيات اليدوية.", color = p.muted, fontSize = 12.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold) } }
    )

    if (showCoords) {
        ManualCoordsDialog(
            onDismiss = { showCoords = false },
            onSaved = { showCoords = false; onDismiss() }
        )
    }
}

@Composable
fun ManualCoordsDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val ctx = LocalContext.current

    var name by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(String.format(Locale.US, "%.4f", st.geo.lat)) }
    var lng by remember { mutableStateOf(String.format(Locale.US, "%.4f", st.geo.lng)) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = p.card,
        title = { Text("إدخال الموقع يدوياً", fontWeight = FontWeight.ExtraBold, color = p.ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("اسم المكان (اختياري)") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lat, onValueChange = { lat = it }, singleLine = true,
                    label = { Text("خط العرض (من -90 إلى 90)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lng, onValueChange = { lng = it }, singleLine = true,
                    label = { Text("خط الطول (من -180 إلى 180)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "تجد إحداثيات مكانك من تطبيق الخرائط بالضغط المطوّل على الموقع.",
                    color = p.muted, fontSize = 11.5.sp
                )
                if (error != null) Text(error!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val la = LocationHelper.parseCoord(lat)
                    val lo = LocationHelper.parseCoord(lng)
                    if (la == null || lo == null || la !in -90.0..90.0 || lo !in -180.0..180.0) {
                        error = "تحقق من الإحداثيات: خط العرض بين -90 و90، وخط الطول بين -180 و180."
                    } else {
                        st.setLocation(name.trim().ifEmpty { "موقع مخصص" }, la, lo)
                        st.locationMode = 1
                        AdhanEngine.scheduleAll(ctx, st)
                        Toast.makeText(ctx, "تم حفظ الموقع ✓", Toast.LENGTH_SHORT).show()
                        onSaved()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
            ) { Text("حفظ", color = Color.White, fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = p.muted) } }
    )
}

@Composable
fun MuezzinPickerDialog(onDismiss: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val selectedMuezzinId by AdhanEngine.activeMuezzinId.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = p.card,
        title = { Text("اختر المؤذن (${Top15Muezzins.size} شيخاً)", fontWeight = FontWeight.ExtraBold, color = p.ink) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Top15Muezzins, key = { it.id }) { m ->
                    val active = m.id == selectedMuezzinId
                    Surface(
                        onClick = {
                            fx.click()
                            AdhanEngine.setSelectedMuezzin(ctx, m.id)
                            AdhanEngine.scheduleAll(ctx, st)
                            onDismiss()
                            Toast.makeText(ctx, "تم اختيار المؤذن: ${m.name} ✓", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (active) EmeraldVibrant else p.bg,
                        border = BorderStroke(1.dp, if (active) GoldShine else p.border)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = if (active) GoldShine else TurquoiseAccent)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.ExtraBold, color = if (active) Color.White else p.ink, fontSize = 14.sp)
                                Text(m.title, fontSize = 11.sp, color = if (active) GoldSoftBg else p.muted, fontWeight = FontWeight.Bold)
                            }
                            if (active) Icon(Icons.Default.CheckCircle, null, tint = GoldShine)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold) } }
    )
}

@Composable
fun MethodPickerDialog(onDismiss: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = p.card,
        title = { Text("طريقة حساب مواقيت الصلاة", fontWeight = FontWeight.ExtraBold, color = p.ink) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PrayerCalc.methodNames.forEachIndexed { idx, mName ->
                    Surface(
                        onClick = {
                            fx.click()
                            st.prayerMethod = idx
                            AdhanEngine.scheduleAll(ctx, st)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (st.prayerMethod == idx) EmeraldVibrant else p.bg
                    ) {
                        Text(
                            text = mName,
                            color = if (st.prayerMethod == idx) Color.White else p.ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

// ============================================================
// 6. شاشة مواقيت الصلاة والمؤذنين والقبلة (PrayerScreen)
// ============================================================
@Composable
fun PrayerScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    val isAdhanPlaying by AdhanEngine.isPlaying.collectAsState()
    val selectedMuezzinId by AdhanEngine.activeMuezzinId.collectAsState()
    val selectedMuezzin = remember(selectedMuezzinId) { AdhanEngine.getSelectedMuezzin(ctx) }

    var showMuezzinDialog by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showQiblaDialog by remember { mutableStateOf(false) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        AdhanEngine.scheduleAll(ctx, st)
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val notificationsOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    val cal = AppClock.calendar(st, nowMillis)
    val dayKey = cal.get(Calendar.DAY_OF_YEAR) * 10000 + cal.get(Calendar.YEAR)
    val times = remember(
        dayKey, st.geo, st.prayerMethod, st.asrHanafi, st.minuteAdjust, st.adjFajr, st.adjSunrise,
        st.adjDhuhr, st.adjAsr, st.adjMaghrib, st.adjIsha, st.timeMode, st.manualUtcMin
    ) { PrayerCalc.timesFor(st, AppClock.calendar(st)) }

    val next = remember(nowMillis / 1000L, times) { PrayerCalc.next(st, nowMillis) }
    val currentHour = AppClock.hourDecimal(cal)

    val prayerList = listOf(
        Triple("الفجر", times.fajr, Icons.Default.NightsStay),
        Triple("الشروق", times.sunrise, Icons.Default.WbTwilight),
        Triple("الظهر", times.dhuhr, Icons.Default.WbSunny),
        Triple("العصر", times.asr, Icons.Default.LightMode),
        Triple("المغرب", times.maghrib, Icons.Default.Brightness4),
        Triple("العشاء", times.isha, Icons.Default.Bedtime)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "مواقيت الصلاة والأذان",
            subtitle = "${st.geo.name} • ${if (st.locationMode == 0) "تلقائي" else "يدوي"}",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); nav.openSettings(0) }) {
                    Icon(Icons.Default.Tune, "إعدادات الموقع والوقت", tint = GoldShine)
                }
                IconButton(onClick = { fx.click(); showQiblaDialog = true }) {
                    Icon(Icons.Default.Explore, "اتجاه القبلة", tint = GoldShine)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!notificationsOn) {
                Surface(
                    onClick = {
                        runCatching {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = DangerRed,
                    border = BorderStroke(1.dp, GoldShine)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsOff, null, tint = GoldShine, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الإشعارات متوقفة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("فعّلها حتى يصلك تنبيه الأذان والتذكيرات. اضغط هنا لفتح الإعدادات.", color = GoldSoftBg, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // 1. بطاقة الصلاة القادمة والعد التنازلي
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = RoyalForest,
                border = BorderStroke(1.4.dp, GoldWarm.copy(alpha = 0.7f)),
                shadowElevation = 8.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant)))
                ) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { fx.click(); showCityDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, tint = GoldShine, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(st.geo.name, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                                }
                            }
                            Surface(
                                onClick = { fx.click(); showMethodDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "طريقة الحساب",
                                    color = GoldSoftBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(14.dp))
                        Text("الصلاة القادمة: ${next.name}", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(
                            text = PrayerCalc.remaining(next.millis - nowMillis),
                            color = GoldShine,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 40.sp
                        )
                        Text(
                            text = "عند ${PrayerCalc.formatMillis(st, next.millis)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${ArabicDate.weekday(cal)} • ${HijriCalendar.format(cal, st.hijriOffset)} • ${ArabicDate.gregorian(cal, st.monthStyle)}",
                            color = GoldSoftBg,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (st.prayerAlert) "منبّه الأذان التلقائي مفعّل ✓" else "منبّه الأذان متوقف حالياً",
                            color = if (st.prayerAlert) GoldShine else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. بطاقة اختيار المؤذن + تجربة الصوت
            QCard(shape = RoundedCornerShape(22.dp), borderColor = GoldWarm.copy(alpha = 0.55f)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("صوت المؤذن (${Top15Muezzins.size} شيخاً)", fontSize = 11.5.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            Text(selectedMuezzin.name, fontSize = 16.sp, color = p.ink, fontWeight = FontWeight.ExtraBold)
                            Text(selectedMuezzin.title, fontSize = 11.5.sp, color = EmeraldVibrant, fontWeight = FontWeight.Bold)
                        }
                        Switch(
                            checked = st.prayerAlert,
                            onCheckedChange = { enabled ->
                                fx.click()
                                st.prayerAlert = enabled
                                if (enabled) {
                                    AdhanEngine.scheduleAll(ctx, st)
                                    Toast.makeText(ctx, "تم تفعيل الأذان التلقائي لجميع الصلوات ✓", Toast.LENGTH_SHORT).show()
                                } else {
                                    AdhanEngine.cancelAll(ctx)
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = GoldShine, checkedTrackColor = EmeraldVibrant)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { fx.click(); showMuezzinDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("تغيير المؤذن", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                        }
                        FilledTonalButton(
                            onClick = {
                                fx.click()
                                if (isAdhanPlaying) AdhanEngine.stopAdhan(ctx) else AdhanEngine.playAdhanNow(ctx, "التجربة")
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isAdhanPlaying) DangerRed else GoldWarm,
                                contentColor = if (isAdhanPlaying) Color.White else RoyalForest
                            )
                        ) {
                            Icon(if (isAdhanPlaying) Icons.Default.Stop else Icons.Default.VolumeUp, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isAdhanPlaying) "إيقاف" else "تجربة الصوت", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // 3. جدول الصلوات
            SectionTitle("مواقيت اليوم")
            prayerList.forEach { (name, timeH, icon) ->
                val isNext = name == next.name && name != "الشروق"
                val passed = timeH < currentHour && !isNext
                QCard(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isNext) EmeraldVibrant.copy(alpha = 0.14f) else p.card,
                    borderColor = if (isNext) GoldWarm else p.border
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isNext) EmeraldVibrant else TurquoiseAccent.copy(alpha = 0.12f))
                        ) {
                            Icon(icon, null, tint = if (isNext) GoldShine else TurquoiseAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (passed) p.muted else p.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = PrayerCalc.formatTime(timeH, st.use24h),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isNext) GoldWarm else if (passed) p.muted else EmeraldVibrant
                        )
                    }
                }
            }

            Text(
                text = "يمكنك تعديل الوقت والمنطقة الزمنية وتصحيح الدقائق من: الإعدادات ◂ الموقع والوقت.",
                color = p.muted,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showMuezzinDialog) MuezzinPickerDialog { showMuezzinDialog = false }
    if (showCityDialog) LocationPickerDialog { showCityDialog = false }
    if (showMethodDialog) MethodPickerDialog { showMethodDialog = false }
    if (showQiblaDialog) {
        QiblaCompassDialog(
            lat = st.geo.lat,
            lng = st.geo.lng,
            cityName = st.geo.name,
            onDismiss = { showQiblaDialog = false }
        )
    }
}

@Composable
private fun QiblaCompassDialog(lat: Double, lng: Double, cityName: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val qibla = remember(lat, lng) { PrayerCalc.qiblaBearing(lat, lng).toFloat() }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotation = sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        @Suppress("DEPRECATION")
        val orientation = sm?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val matrix = FloatArray(9)
        val ov = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                val raw: Float = if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(matrix, event.values)
                    SensorManager.getOrientation(matrix, ov)
                    ((Math.toDegrees(ov[0].toDouble()) + 360.0) % 360.0).toFloat()
                } else event.values[0]
                val diff = ((raw - azimuth + 540f) % 360f) - 180f
                azimuth = (azimuth + diff * 0.22f + 360f) % 360f
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val used = rotation ?: orientation
        if (sm != null && used != null) sm.registerListener(listener, used, SensorManager.SENSOR_DELAY_UI)
        else hasSensor = false
        onDispose { sm?.unregisterListener(listener) }
    }

    val relative = ((qibla - azimuth + 540f) % 360f) - 180f
    val aligned = hasSensor && abs(relative) < 4f

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = p.card,
        title = { Text("اتجاه القبلة • $cityName", fontWeight = FontWeight.ExtraBold, color = p.ink) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "زاوية الكعبة المشرفة: ${qibla.toInt()}° من الشمال",
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldVibrant,
                    fontSize = 13.5.sp
                )
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(230.dp)) {
                    // القرص يدور عكس اتجاه الهاتف ليبقى الشمال ثابتاً في الواقع
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(-azimuth)
                            .clip(CircleShape)
                            .background(RoyalForest)
                            .border(3.dp, if (aligned) GoldShine else GoldWarm, CircleShape)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val c = center
                            val r = size.minDimension / 2f
                            for (i in 0 until 72) {
                                val a = Math.toRadians(i * 5.0)
                                val major = i % 6 == 0
                                val len = if (major) 14.dp.toPx() else 7.dp.toPx()
                                val s = Offset(c.x + ((r - 6.dp.toPx()) * sin(a)).toFloat(), c.y - ((r - 6.dp.toPx()) * cos(a)).toFloat())
                                val e = Offset(c.x + ((r - 6.dp.toPx() - len) * sin(a)).toFloat(), c.y - ((r - 6.dp.toPx() - len) * cos(a)).toFloat())
                                drawLine(GoldShine.copy(alpha = if (major) 0.8f else 0.35f), s, e, strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                        Text(
                            "ش",
                            color = GoldShine,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp)
                        )
                        Box(Modifier.fillMaxSize().rotate(qibla), contentAlignment = Alignment.TopCenter) {
                            Icon(
                                Icons.Default.Mosque,
                                contentDescription = "القبلة",
                                tint = if (aligned) GoldShine else Color.White,
                                modifier = Modifier.padding(top = 40.dp).size(34.dp)
                            )
                        }
                    }
                    // مؤشر ثابت يمثل اتجاه الهاتف
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-6).dp)
                            .size(width = 6.dp, height = 20.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (aligned) EmeraldVibrant else DangerRed)
                    )
                }
                Text(
                    text = when {
                        !hasSensor -> "جهازك لا يحتوي على حساس البوصلة. اتجاه القبلة بالأعلى بالدرجات من الشمال."
                        aligned -> "أنت الآن باتجاه القبلة ✓"
                        else -> "ضع الهاتف أفقياً وأدره حتى تقف أيقونة المسجد عند المؤشر الأعلى"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (aligned) EmeraldVibrant else p.muted,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold) } }
    )
}
