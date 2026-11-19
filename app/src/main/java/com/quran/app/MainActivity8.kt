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
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
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

object PrayerCalc {
    val methodNames = listOf(
        "رابطة العالم الإسلامي (MWL)",
        "أم القرى (مكة المكرمة)",
        "الهيئة المصرية العامة للمساحة",
        "جامعة العلوم الإسلامية بكراتشي",
        "الجمعية الإسلامية لأمريكا الشمالية (ISNA)"
    )

    val presets: List<GeoLocation> = listOf(
        GeoLocation("مكة المكرمة", 21.4225, 39.8262),
        GeoLocation("المدينة المنورة", 24.4672, 39.6111),
        GeoLocation("القدس الشريف", 31.7683, 35.2137),
        GeoLocation("حلب", 36.2021, 37.1343),
        GeoLocation("دمشق", 33.5138, 36.2765),
        GeoLocation("الرياض", 24.7136, 46.6753),
        GeoLocation("القاهرة", 30.0444, 31.2357),
        GeoLocation("بغداد", 33.3152, 44.3661),
        GeoLocation("عمّان", 31.9454, 35.9284),
        GeoLocation("الكويت", 29.3759, 47.9774),
        GeoLocation("دبي", 25.2048, 55.2708),
        GeoLocation("الدوحة", 25.2854, 51.5310),
        GeoLocation("إسطنبول", 41.0082, 28.9784)
    )

    private fun dtr(d: Double) = d * PI / 180.0
    private fun rtd(r: Double) = r * 180.0 / PI
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(h: Double): Double = h - 24.0 * floor(h / 24.0)

    fun compute(cal: Calendar, lat: Double, lng: Double, method: Int): PrayerTimes {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val tz = TimeZone.getDefault().getOffset(cal.timeInMillis) / 3_600_000.0

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

        val (fajrAngle, ishaAngle, ishaInterval) = when (method) {
            1 -> Triple(18.5, 0.0, 90.0)
            2 -> Triple(19.5, 17.5, 0.0)
            3 -> Triple(18.0, 18.0, 0.0)
            4 -> Triple(15.0, 15.0, 0.0)
            else -> Triple(18.0, 17.0, 0.0)
        }

        val fajr = sunAngleTime(fajrAngle, true)
        val sunrise = sunAngleTime(0.833, true)
        val maghrib = sunAngleTime(0.833, false)
        val isha = if (ishaInterval > 0) maghrib + ishaInterval / 60.0 else sunAngleTime(ishaAngle, false)

        val asrAngle = -rtd(atan(1.0 / (1.0 + tan(dtr(abs(lat - decl))))))
        val asr = sunAngleTime(asrAngle, false)

        return PrayerTimes(
            fajr = fixHour(fajr),
            sunrise = fixHour(sunrise),
            dhuhr = fixHour(dhuhr),
            asr = fixHour(asr),
            maghrib = fixHour(maghrib),
            isha = fixHour(isha)
        )
    }

    fun formatTime(hours: Double): String {
        val totalMin = ((hours * 60.0).roundToInt() % 1440 + 1440) % 1440
        val h = totalMin / 60
        val m = totalMin % 60
        val suffix = if (h >= 12) "م" else "ص"
        val h12 = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format(Locale.US, "%d:%02d %s", h12, m, suffix)
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
// 3. محرك الأذان الدقيق ومستقبل التنبيهات (AdhanEngine & AdhanAlarmReceiver)
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

    // ============================================================
    // جدولة الأذان الدقيق باستخدام setAlarmClock لاختراق وضع السكون
    // ============================================================
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleAll(ctx: Context, st: AppState) {
        val appCtx = ctx.applicationContext
        if (!st.prayerAlert) {
            cancelAll(appCtx)
            return
        }

        // تحميل صوت المؤذن مسبقاً ليكون جاهزاً بدون إنترنت
        val voice = getSelectedMuezzin(appCtx)
        scope.launch {
            val f = localVoiceFile(appCtx, voice.id)
            if (!f.exists() || f.length() < 4096) {
                QuranAudioEngine.downloadWithResume(appCtx, voice.url, f)
            }
        }

        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = Calendar.getInstance()
        val todayTimes = PrayerCalc.compute(now, st.geo.lat, st.geo.lng, st.prayerMethod)

        val prayers = listOf(
            101 to ("الفجر" to todayTimes.fajr),
            102 to ("الظهر" to todayTimes.dhuhr),
            103 to ("العصر" to todayTimes.asr),
            104 to ("المغرب" to todayTimes.maghrib),
            105 to ("العشاء" to todayTimes.isha)
        )

        for ((reqCode, pair) in prayers) {
            val (prayerName, hourDecimal) = pair
            val totalMin = ((hourDecimal * 60.0).roundToInt() % 1440 + 1440) % 1440
            val targetCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, totalMin / 60)
                set(Calendar.MINUTE, totalMin % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // إذا كان وقت الصلاة قد مضى اليوم، نجدوله لليوم التالي
            if (targetCal.timeInMillis <= System.currentTimeMillis() + 5_000L) {
                targetCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val intent = Intent(appCtx, AdhanAlarmReceiver::class.java).apply {
                action = "ACTION_TRIGGER_ADHAN"
                putExtra("prayer_name", prayerName)
            }
            val pi = PendingIntent.getBroadcast(
                appCtx, reqCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(targetCal.timeInMillis, Notifs.contentIntent(appCtx))
                am.setAlarmClock(alarmClockInfo, pi)
            } catch (_: SecurityException) {
                // في حال عدم منح إذن المنبه الدقيق، نستخدم الجدولة البديلة
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pi)
                }
            }
        }
    }

    fun cancelAll(ctx: Context) {
        val appCtx = ctx.applicationContext
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (reqCode in 101..105) {
            val intent = Intent(appCtx, AdhanAlarmReceiver::class.java).apply { action = "ACTION_TRIGGER_ADHAN" }
            val pi = PendingIntent.getBroadcast(
                appCtx, reqCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pi != null) am.cancel(pi)
        }
    }

    fun ensureSchedulesOnBoot(ctx: Context) {
        val appCtx = ctx.applicationContext
        val st = AppState(appCtx)
        if (st.prayerAlert) {
            scheduleAll(appCtx, st)
        }
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

                val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
                AdhanEngine.playAdhanNow(appCtx, prayerName)

                // إعادة جدولة الصلوات لليوم التالي تلقائياً
                AdhanEngine.ensureSchedulesOnBoot(appCtx)
            }
        }
    }
}

// ============================================================
// 4. شاشة مواقيت الصلاة والمؤذنين والقبلة (PrayerScreen)
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

    // فحص إذن المنبه الدقيق في أندرويد 12+
    val canScheduleExact = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            am?.canScheduleExactAlarms() ?: true
        } else true
    }

    // تحديث العد التنازلي للصلاة القادمة كل ثانية
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        AdhanEngine.scheduleAll(ctx, st)
        while (true) {
            kotlinx.coroutines.delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val cal = remember(nowMillis / 60_000L) { Calendar.getInstance() }
    val times = remember(st.geo, st.prayerMethod, cal.get(Calendar.DAY_OF_YEAR)) {
        PrayerCalc.compute(cal, st.geo.lat, st.geo.lng, st.prayerMethod)
    }

    val currentHourDecimal = remember(nowMillis) {
        val c = Calendar.getInstance()
        c.get(Calendar.HOUR_OF_DAY) + c.get(Calendar.MINUTE) / 60.0 + c.get(Calendar.SECOND) / 3600.0
    }

    val prayerList = listOf(
        Triple("الفجر", times.fajr, Icons.Default.NightsStay),
        Triple("الشروق", times.sunrise, Icons.Default.WbTwilight),
        Triple("الظهر", times.dhuhr, Icons.Default.WbSunny),
        Triple("العصر", times.asr, Icons.Default.LightMode),
        Triple("المغرب", times.maghrib, Icons.Default.Brightness4),
        Triple("العشاء", times.isha, Icons.Default.Bedtime)
    )

    val nextPrayerInfo = remember(currentHourDecimal, times) {
        val candidates = listOf(
            "الفجر" to times.fajr,
            "الظهر" to times.dhuhr,
            "العصر" to times.asr,
            "المغرب" to times.maghrib,
            "العشاء" to times.isha
        )
        val upcoming = candidates.firstOrNull { it.second > currentHourDecimal }
        val (name, targetH) = upcoming ?: ("الفجر" to (times.fajr + 24.0))
        val diffSec = ((targetH - currentHourDecimal) * 3600.0).toInt().coerceAtLeast(0)
        val hh = diffSec / 3600
        val mm = (diffSec % 3600) / 60
        val ss = diffSec % 60
        name to String.format(Locale.US, "%02d:%02d:%02d", hh, mm, ss)
    }

    // طلب إذن الإشعارات والموقع
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            detectGpsLocation(ctx, st)
            AdhanEngine.scheduleAll(ctx, st)
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "مواقيت الصلاة والأذان",
            onBack = { nav.back() },
            trailing = {
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
            // تنبيه مهم في حال إيقاف إذن المنبه الدقيق في إعدادات أندرويد
            if (!canScheduleExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Surface(
                    onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFB71C1C),
                    border = BorderStroke(1.dp, GoldShine)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, null, tint = GoldShine, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("تفعيل إذن الأذان الدقيق", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            Text("اضغط هنا للسماح للتطبيق بالأذان في الوقت المحدد بدقة حتى والشاشة مغلقة", color = GoldSoftBg, fontSize = 11.5.sp)
                        }
                    }
                }
            }

            // 1. بطاقة الصلاة القادمة والعد التنازلي والمدينة
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
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
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
                        Text("الصلاة القادمة: ${nextPrayerInfo.first}", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = nextPrayerInfo.second,
                            color = GoldShine,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 38.sp
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

            // 2. بطاقة اختيار المؤذن (15 شيخاً) + اختبار صوت الأذان وإيقافه
            QCard(
                shape = RoundedCornerShape(22.dp),
                borderColor = GoldWarm.copy(alpha = 0.55f)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                if (isAdhanPlaying) AdhanEngine.stopAdhan(ctx)
                                else AdhanEngine.playAdhanNow(ctx, "التجربة")
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isAdhanPlaying) Color(0xFFB71C1C) else GoldWarm,
                                contentColor = if (isAdhanPlaying) Color.White else RoyalForest
                            )
                        ) {
                            Icon(
                                if (isAdhanPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (isAdhanPlaying) "إيقاف" else "تجربة الصوت",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }

            // 3. جدول أوقات الصلوات الخمس والشروق
            SectionTitle("مواقيت اليوم")
            prayerList.forEach { (name, timeH, icon) ->
                val isNext = name == nextPrayerInfo.first
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
                            fontSize = 16.sp,
                            color = p.ink,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = PrayerCalc.formatTime(timeH),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isNext) GoldWarm else EmeraldVibrant
                        )
                    }
                }
            }
        }
    }

    // نافذة اختيار المؤذن من أشهر 15 شيخاً
    if (showMuezzinDialog) {
        AlertDialog(
            onDismissRequest = { showMuezzinDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = { Text("اختر المؤذن (${Top15Muezzins.size} شيخاً)", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(Top15Muezzins, key = { it.id }) { m ->
                        val active = m.id == selectedMuezzinId
                        Surface(
                            onClick = {
                                fx.click()
                                AdhanEngine.setSelectedMuezzin(ctx, m.id)
                                AdhanEngine.scheduleAll(ctx, st)
                                showMuezzinDialog = false
                                Toast.makeText(ctx, "تم اختيار المؤذن: ${m.name} ✓", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (active) EmeraldVibrant else p.bg,
                            border = BorderStroke(1.dp, if (active) GoldShine else p.border)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
            confirmButton = {
                TextButton(onClick = { showMuezzinDialog = false }) { Text("إغلاق", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }

    // نافذة اختيار المدينة أو تحديد الموقع تلقائياً بـ GPS
    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { showCityDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = { Text("تحديد الموقع والمدينة", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Button(
                            onClick = {
                                fx.click()
                                permLauncher.launch(
                                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                )
                                detectGpsLocation(ctx, st)
                                AdhanEngine.scheduleAll(ctx, st)
                                showCityDialog = false
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = RoyalForest)
                            Spacer(Modifier.width(8.dp))
                            Text("تحديد موقعي تلقائياً (GPS)", color = RoyalForest, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    items(PrayerCalc.presets, key = { it.name }) { city ->
                        Surface(
                            onClick = {
                                fx.click()
                                st.setLocation(city.name, city.lat, city.lng)
                                AdhanEngine.scheduleAll(ctx, st)
                                showCityDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = p.bg,
                            border = BorderStroke(1.dp, p.border)
                        ) {
                            Text(
                                text = city.name,
                                fontWeight = FontWeight.ExtraBold,
                                color = p.ink,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCityDialog = false }) { Text("إغلاق") }
            }
        )
    }

    // نافذة طريقة حساب الصلاة
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("طريقة حساب مواقيت الصلاة", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PrayerCalc.methodNames.forEachIndexed { idx, mName ->
                        Surface(
                            onClick = {
                                fx.click()
                                st.prayerMethod = idx
                                AdhanEngine.scheduleAll(ctx, st)
                                showMethodDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (st.prayerMethod == idx) EmeraldVibrant else p.bg
                        ) {
                            Text(
                                text = mName,
                                color = if (st.prayerMethod == idx) Color.White else p.ink,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMethodDialog = false }) { Text("إغلاق") } }
        )
    }

    // نافذة بوصلة القبلة التفاعلية
    if (showQiblaDialog) {
        QiblaCompassDialog(
            lat = st.geo.lat,
            lng = st.geo.lng,
            cityName = st.geo.name,
            onDismiss = { showQiblaDialog = false }
        )
    }
}

@SuppressLint("MissingPermission")
private fun detectGpsLocation(ctx: Context, st: AppState) {
    runCatching {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (loc != null) {
            st.setLocation("موقعي الحالي (GPS)", loc.latitude, loc.longitude)
            Toast.makeText(ctx, "تم تحديث الموقع الجغرافي بنجاح ✓", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
private fun QiblaCompassDialog(lat: Double, lng: Double, cityName: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val p = LocalPalette.current
    val qiblaAngle = remember(lat, lng) { PrayerCalc.qiblaBearing(lat, lng).toFloat() }
    var azimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) azimuth = event.values[0]
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sm?.unregisterListener(listener) }
    }

    val needleRotation = qiblaAngle - azimuth

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
                    text = "زاوية الكعبة المشرفة: ${qiblaAngle.toInt()}° من الشمال",
                    fontWeight = FontWeight.Bold,
                    color = EmeraldVibrant,
                    fontSize = 13.5.sp
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .background(RoyalForest)
                        .border(2.dp, GoldWarm, CircleShape)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(GoldShine.copy(alpha = 0.25f), radius = size.minDimension * 0.42f, style = Stroke(2.dp.toPx()))
                    }
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "سهم القبلة",
                        tint = GoldShine,
                        modifier = Modifier
                            .size(74.dp)
                            .rotate(needleRotation)
                    )
                }
                Text(
                    text = "ضع الهاتف أفقياً وأدره حتى يشير السهم الذهبي للأمام",
                    fontSize = 12.sp,
                    color = p.muted,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold) }
        }
    )
}
