package com.quran.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.ui.text.font.FontFamily
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object CustomFontLoader {
    private val cache = mutableMapOf<String, FontFamily>()

    fun loadFromAssets(context: Context, assetFileName: String): FontFamily {
        cache[assetFileName]?.let { return it }
        return try {
            val tf = Typeface.createFromAsset(context.assets, assetFileName)
            FontFamily(tf)
        } catch (_: Exception) {
            FontFamily.SansSerif
        }.also { cache[assetFileName] = it }
    }

    fun indexFont(context: Context): FontFamily = loadFromAssets(context, "index_font.ttf")
    fun homeTitleFont(context: Context): FontFamily = loadFromAssets(context, "home_title_font.ttf")
    fun adhkarFont(context: Context): FontFamily = loadFromAssets(context, "adhkar_font.ttf")
}

object DateTimeHelper {
    fun currentClock(): String = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
    fun currentDayName(): String = SimpleDateFormat("EEEE", Locale("ar")).format(Date())
    fun currentHijriDate(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                HijrahDate.from(LocalDate.now()).format(DateTimeFormatter.ofPattern("d MMMM yyyy هـ", Locale("ar")))
            } catch (_: Exception) {
                "التاريخ الهجري"
            }
        } else {
            "التاريخ الهجري"
        }
    }
}

object QiblaCalculator {
    private const val KAABA_LAT = 21.4225
    private const val KAABA_LON = 39.8262

    fun bearingToKaaba(lat: Double, lon: Double): Float {
        val lat1 = Math.toRadians(lat)
        val lat2 = Math.toRadians(KAABA_LAT)
        val diffLon = Math.toRadians(KAABA_LON - lon)
        val y = sin(diffLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(diffLon)
        return (((Math.toDegrees(atan2(y, x))) + 360.0) % 360.0).toFloat()
    }
}

class CompassTracker(context: Context, private val onAzimuthChanged: (Float) -> Unit) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private val rotMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    fun start() {
        rotationSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            return
        }
        accelSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                SensorManager.getOrientation(rotMatrix, orientation)
                onAzimuthChanged(((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat())
            }
            Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, 3)
            Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, 3)
        }
        if (rotationSensor == null && SensorManager.getRotationMatrix(rotMatrix, null, gravity, geomagnetic)) {
            SensorManager.getOrientation(rotMatrix, orientation)
            onAzimuthChanged(((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

object VoiceAdhkarScheduler {
    const val CHANNEL_ID = "voice_adhkar_channel"
    const val PREFS_NAME = "voice_adhkar_prefs"
    const val KEY_ENABLED = "voice_adhkar_enabled"
    const val KEY_INTERVAL_MIN = "voice_adhkar_interval_min"
    const val KEY_PHRASE = "voice_adhkar_phrase"
    const val KEY_LOCK_SCREEN = "voice_adhkar_lock_screen"

    val availablePhrases = listOf(
        "سبحان الله",
        "الحمد لله",
        "لا إله إلا الله",
        "الله أكبر",
        "أستغفر الله العظيم وأتوب إليه",
        "سبحان الله وبحمده سبحان الله العظيم",
        "لا حول ولا قوة إلا بالله",
        "اللهم صل وسلم على نبينا محمد",
        "رضيت بالله رباً وبالإسلام ديناً وبمحمد صلى الله عليه وسلم نبياً",
        "أعوذ بكلمات الله التامات من شر ما خلق",
        "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم",
        "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور",
        "أصبحنا وأصبح الملك لله والحمد لله",
        "يا حي يا قيوم برحمتك أستغيث أصلح لي شأني كله",
        "سبحان الله العظيم وبحمده"
    )

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "الأذكار الصوتية",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات الأذكار الصوتية المجدولة"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun schedule(context: Context, intervalMinutes: Int, phrase: String, lockScreen: Boolean) {
        ensureChannel(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .putInt(KEY_INTERVAL_MIN, intervalMinutes.coerceAtLeast(1))
            .putString(KEY_PHRASE, phrase)
            .putBoolean(KEY_LOCK_SCREEN, lockScreen)
            .apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, VoiceAdhkarReceiver::class.java).apply {
            putExtra("phrase", phrase)
            putExtra("lockScreen", lockScreen)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val intervalMs = intervalMinutes.coerceAtLeast(1) * 60_000L
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + intervalMs,
            intervalMs,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, false).apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context, 9001, Intent(context, VoiceAdhkarReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}

class VoiceAdhkarReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prefs = context.getSharedPreferences(VoiceAdhkarScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(VoiceAdhkarScheduler.KEY_ENABLED, false)) return

        val dhikrIndex = prefs.getInt("voice_adhkar_cycle_index", 0)
        val phrase = VoiceAdhkarScheduler.availablePhrases[
            dhikrIndex.mod(VoiceAdhkarScheduler.availablePhrases.size)
        ]
        prefs.edit()
            .putInt("voice_adhkar_cycle_index", (dhikrIndex + 1) % VoiceAdhkarScheduler.availablePhrases.size)
            .apply()
        val showOnLockScreen = intent?.getBooleanExtra("lockScreen", true)
            ?: prefs.getBoolean(VoiceAdhkarScheduler.KEY_LOCK_SCREEN, true)

        VoiceAdhkarScheduler.ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 9002, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, VoiceAdhkarScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ذكر صوتي")
            .setContentText(phrase)
            .setStyle(NotificationCompat.BigTextStyle().bigText(phrase))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(if (showOnLockScreen) NotificationCompat.VISIBILITY_PUBLIC else NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify((System.currentTimeMillis() % 100000).toInt(), notification)
        } catch (_: SecurityException) {
        }

        val pendingResult = goAsync()
        var speech: TextToSpeech? = null
        speech = TextToSpeech(context.applicationContext) { status ->
            val engine = speech
            if (engine == null || status != TextToSpeech.SUCCESS) {
                engine?.shutdown()
                pendingResult.finish()
                return@TextToSpeech
            }
            val languageStatus = engine.setLanguage(Locale("ar"))
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA ||
                languageStatus == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.shutdown()
                pendingResult.finish()
                return@TextToSpeech
            }
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    pendingResult.finish()
                }
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    pendingResult.finish()
                }
            })
            engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "dhikr_utterance")
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val prefs = context.getSharedPreferences(VoiceAdhkarScheduler.PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(VoiceAdhkarScheduler.KEY_ENABLED, false)) return
        val interval = prefs.getInt(VoiceAdhkarScheduler.KEY_INTERVAL_MIN, 60)
        val phrase = prefs.getString(VoiceAdhkarScheduler.KEY_PHRASE, "سبحان الله") ?: "سبحان الله"
        val lockScreen = prefs.getBoolean(VoiceAdhkarScheduler.KEY_LOCK_SCREEN, true)
        VoiceAdhkarScheduler.schedule(context, interval, phrase, lockScreen)
    }
}
