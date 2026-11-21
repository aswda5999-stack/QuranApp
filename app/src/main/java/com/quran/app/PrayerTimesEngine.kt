package com.quran.app

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

data class PrayerTimeItem(val name: String, val time: String, val hour: Int, val minute: Int)

object PrayerTimesEngine {
    private const val PREFS = "prayer_times_settings"
    private const val KEY_LAT = "latitude"
    private const val KEY_LON = "longitude"
    private const val KEY_ENABLED = "adhan_enabled"

    fun saveLocation(context: Context, latitude: Double, longitude: Double) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LAT, latitude.toString())
            .putString(KEY_LON, longitude.toString()).apply()
    }

    fun savedLocation(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getString(KEY_LAT, null)?.toDoubleOrNull()
        val lon = prefs.getString(KEY_LON, null)?.toDoubleOrNull()
        return if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
    }

    fun setAdhanEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isAdhanEnabled(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    suspend fun fetchToday(latitude: Double, longitude: Double): List<PrayerTimeItem> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(Date())
            val url = URL("https://api.aladhan.com/v1/timings/$date?latitude=$latitude&longitude=$longitude&method=3&latitudeAdjustmentMethod=3")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 12000
                readTimeout = 15000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val timings = JSONObject(response).getJSONObject("data").getJSONObject("timings")
                listOf(
                    "الفجر" to "Fajr", "الظهر" to "Dhuhr", "العصر" to "Asr",
                    "المغرب" to "Maghrib", "العشاء" to "Isha"
                ).mapNotNull { (arabic, key) ->
                    val raw = timings.optString(key).substringBefore(" ").trim()
                    val parts = raw.split(":")
                    val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                    val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                    if (hour !in 0..23 || minute !in 0..59) null
                    else PrayerTimeItem(arabic, "%02d:%02d".format(hour, minute), hour, minute)
                }
            } finally { connection.disconnect() }
        }

    fun scheduleToday(context: Context, prayers: List<PrayerTimeItem>) {
        if (!isAdhanEnabled(context)) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        prayers.forEachIndexed { index, prayer ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, prayer.hour)
                set(Calendar.MINUTE, prayer.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }
            val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                putExtra("prayer_name", prayer.name)
                putExtra("prayer_id", index)
            }
            val pending = PendingIntent.getBroadcast(
                context, 7100 + index, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pending)
            }
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!PrayerTimesEngine.isAdhanEnabled(context)) return
        val channelId = "prayer_adhan_channel"
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "تنبيهات الصلاة", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيه عند دخول وقت الصلاة"
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), null)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }
        val prayerName = intent.getStringExtra("prayer_name") ?: "الصلاة"
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_quran)
            .setContentTitle("حان الآن وقت صلاة $prayerName")
            .setContentText("حيّ على الصلاة")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .build()
        manager.notify(8100 + intent.getIntExtra("prayer_id", 0), notification)
    }
}


class PrayerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED) return
        val coords = PrayerTimesEngine.savedLocation(context) ?: return
        if (!PrayerTimesEngine.isAdhanEnabled(context)) return
        val pending = goAsync()
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val prayers = PrayerTimesEngine.fetchToday(coords.first, coords.second)
                PrayerTimesEngine.scheduleToday(context, prayers)
            } catch (_: Exception) {
                // The next app launch will refresh and restore the schedule.
            } finally {
                pending.finish()
            }
        }
    }
}
