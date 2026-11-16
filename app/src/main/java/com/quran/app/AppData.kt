package com.quran.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.FontFamily
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL

data class SurahInfo(
    val number: Int,
    val name: String,
    val englishName: String,
    val numberOfAyahs: Int,
    val revelationType: String
)

data class AyahItem(
    val number: Int,
    val numberInSurah: Int,
    val text: String,
    val tafsir: String,
    val juz: Int,
    val page: Int
)

data class BookmarkItem(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val ayahText: String
)

data class GuideItem(
    val title: String,
    val subtitle: String,
    val count: Int = 1
)

data class ReciterInfo(
    val id: String,
    val name: String,
    val serverUrl: String
)

data class AppSettings(
    val fontSize: Float = 26f,
    val useUthmanicFont: Boolean = true,
    val isBoldFont: Boolean = false,
    val lineSpacing: Float = 1.75f,
    val isMushafMode: Boolean = false,
    val showAyahMarkers: Boolean = true,
    val isDarkMode: Boolean = false,
    val themeIndex: Int = 0, // 0..5
    val cardCornerRadius: Float = 20f,
    val defaultReciterId: String = "afs",
    val keepScreenOn: Boolean = true,
    val showNavLabels: Boolean = true,
    val isGridView: Boolean = false,
    val vibrateOnTasbih: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val lastSurahNumber: Int = 1,
    val lastSurahName: String = "سُورَةُ ٱلْفَاتِحَةِ",
    val tasbihCount: Int = 0,
    val totalReadAyahs: Int = 0
)

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("quran_v2_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings = AppSettings(
        fontSize = prefs.getFloat("fontSize", 26f),
        useUthmanicFont = prefs.getBoolean("useUthmanicFont", true),
        isBoldFont = prefs.getBoolean("isBoldFont", false),
        lineSpacing = prefs.getFloat("lineSpacing", 1.75f),
        isMushafMode = prefs.getBoolean("isMushafMode", false),
        showAyahMarkers = prefs.getBoolean("showAyahMarkers", true),
        isDarkMode = prefs.getBoolean("isDarkMode", false),
        themeIndex = prefs.getInt("themeIndex", 0),
        cardCornerRadius = prefs.getFloat("cardCornerRadius", 20f),
        defaultReciterId = prefs.getString("defaultReciterId", "afs") ?: "afs",
        keepScreenOn = prefs.getBoolean("keepScreenOn", true),
        showNavLabels = prefs.getBoolean("showNavLabels", true),
        isGridView = prefs.getBoolean("isGridView", false),
        vibrateOnTasbih = prefs.getBoolean("vibrateOnTasbih", true),
        notificationsEnabled = prefs.getBoolean("notificationsEnabled", true),
        lastSurahNumber = prefs.getInt("lastSurahNumber", 1),
        lastSurahName = prefs.getString("lastSurahName", "سُورَةُ ٱلْفَاتِحَةِ") ?: "سُورَةُ ٱلْفَاتِحَةِ",
        tasbihCount = prefs.getInt("tasbihCount", 0),
        totalReadAyahs = prefs.getInt("totalReadAyahs", 0)
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit().apply {
            putFloat("fontSize", s.fontSize)
            putBoolean("useUthmanicFont", s.useUthmanicFont)
            putBoolean("isBoldFont", s.isBoldFont)
            putFloat("lineSpacing", s.lineSpacing)
            putBoolean("isMushafMode", s.isMushafMode)
            putBoolean("showAyahMarkers", s.showAyahMarkers)
            putBoolean("isDarkMode", s.isDarkMode)
            putInt("themeIndex", s.themeIndex)
            putFloat("cardCornerRadius", s.cardCornerRadius)
            putString("defaultReciterId", s.defaultReciterId)
            putBoolean("keepScreenOn", s.keepScreenOn)
            putBoolean("showNavLabels", s.showNavLabels)
            putBoolean("isGridView", s.isGridView)
            putBoolean("vibrateOnTasbih", s.vibrateOnTasbih)
            putBoolean("notificationsEnabled", s.notificationsEnabled)
            putInt("lastSurahNumber", s.lastSurahNumber)
            putString("lastSurahName", s.lastSurahName)
            putInt("tasbihCount", s.tasbihCount)
            putInt("totalReadAyahs", s.totalReadAyahs)
            apply()
        }
    }

    fun saveLastRead(surahNumber: Int, surahName: String, ayahsCount: Int = 0) {
        val prevTotal = prefs.getInt("totalReadAyahs", 0)
        prefs.edit()
            .putInt("lastSurahNumber", surahNumber)
            .putString("lastSurahName", surahName)
            .putInt("totalReadAyahs", prevTotal + ayahsCount)
            .apply()
    }

    fun saveTasbihCount(count: Int) {
        prefs.edit().putInt("tasbihCount", count).apply()
    }

    fun getBookmarks(): List<BookmarkItem> {
        val raw = prefs.getString("bookmarks_json", "[]") ?: "[]"
        val list = mutableListOf<BookmarkItem>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    BookmarkItem(
                        surahNumber = obj.getInt("surahNumber"),
                        surahName = obj.getString("surahName"),
                        ayahNumber = obj.getInt("ayahNumber"),
                        ayahText = obj.getString("ayahText")
                    )
                )
            }
        } catch (_: Exception) { }
        return list
    }

    fun addBookmark(item: BookmarkItem) {
        val current = getBookmarks().toMutableList()
        if (current.none { it.surahNumber == item.surahNumber && it.ayahNumber == item.ayahNumber }) {
            current.add(0, item)
            saveBookmarks(current)
        }
    }

    fun removeBookmark(item: BookmarkItem) {
        val current = getBookmarks().filterNot {
            it.surahNumber == item.surahNumber && it.ayahNumber == item.ayahNumber
        }
        saveBookmarks(current)
    }

    private fun saveBookmarks(list: List<BookmarkItem>) {
        val arr = JSONArray()
        list.forEach {
            val obj = JSONObject()
            obj.put("surahNumber", it.surahNumber)
            obj.put("surahName", it.surahName)
            obj.put("ayahNumber", it.ayahNumber)
            obj.put("ayahText", it.ayahText)
            arr.put(obj)
        }
        prefs.edit().putString("bookmarks_json", arr.toString()).apply()
    }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}

object QuranNotificationHelper {
    private const val CHANNEL_ID = "quran_daily_reminders"

    fun showReminderNotification(context: Context, title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تذكيرات القرآن والأذكار",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات يومية للأذكار وورد القرآن الكريم"
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_quran)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1001, notification)
    }
}

object QuranRepository {
    // قراء بتلاوات كاملة متصلة للسورة بدون تقطيع
    val fullSurahReciters = listOf(
        ReciterInfo("afs", "مشاري راشد العفاسي", "https://server8.mp3quran.net/afs/"),
        ReciterInfo("sds", "عبدالرحمن السديس", "https://server11.mp3quran.net/sds/"),
        ReciterInfo("shur", "سعود الشريم", "https://server7.mp3quran.net/shur/"),
        ReciterInfo("maher", "ماهر المعيقلي", "https://server12.mp3quran.net/maher/"),
        ReciterInfo("husr", "محمود خليل الحصري", "https://server13.mp3quran.net/husr/"),
        ReciterInfo("minsh", "محمد صديق المنشاوي", "https://server10.mp3quran.net/minsh/"),
        ReciterInfo("yasser", "ياسر الدوسري", "https://server11.mp3quran.net/yasser/"),
        ReciterInfo("ajm", "أحمد بن علي العجمي", "https://server10.mp3quran.net/ajm/")
    )

    fun buildFullSurahAudioUrl(reciterId: String, surahNumber: Int): String {
        val reciter = fullSurahReciters.find { it.id == reciterId } ?: fullSurahReciters.first()
        val padded = surahNumber.toString().padStart(3, '0')
        return "${reciter.serverUrl}$padded.mp3"
    }

    // تحميل وتوفير الخط العثماني الأصلي لمجمع الملك فهد (Hafs Uthmanic)
    suspend fun getUthmanicFontFamily(context: Context): FontFamily = withContext(Dispatchers.IO) {
        val fontFile = File(context.filesDir, "UthmanicHafs.ttf")
        try {
            if (!fontFile.exists() || fontFile.length() < 10000) {
                val urls = listOf(
                    "https://raw.githubusercontent.com/thetruetruth/quran-data-kfgqpc/main/hafs/font/hafs.18.ttf",
                    "https://cdn.jsdelivr.net/gh/fawazahmed0/quran-api@1/fonts/uthmanic_hafs_v20.ttf"
                )
                for (u in urls) {
                    try {
                        val bytes = URL(u).readBytes()
                        if (bytes.size > 10000) {
                            fontFile.writeBytes(bytes)
                            break
                        }
                    } catch (_: Exception) { }
                }
            }
            if (fontFile.exists() && fontFile.length() > 10000) {
                val tf = Typeface.createFromFile(fontFile)
                FontFamily(tf)
            } else {
                FontFamily.Serif
            }
        } catch (_: Exception) {
            FontFamily.Serif
        }
    }

    val adhkarCategories = mapOf(
        "أذكار الصباح والمساء" to listOf(
            GuideItem("سيد الاستغفار", "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت، أعوذ بك من شر ما صنعت، أبوء لك بنعمتك علي، وأبوء بذنبي فاغفر لي فإنه لا يغفر الذنوب إلا أنت.", 1),
            GuideItem("آية الكرسي", "قراءة آية الكرسي مرة واحدة صباحاً ومساءً للحفظ من كل سوء.", 1),
            GuideItem("المعوذات الثلاث", "قراءة سورة الإخلاص والفلق والناس ثلاث مرات صباحاً ومساءً.", 3),
            GuideItem("التسبيح والتهليل", "سبحان الله وبحمده: مائة مرة، ولا إله إلا الله وحده لا شريك له: مائة مرة.", 100),
            GuideItem("الحفظ من الضرر", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.", 3),
            GuideItem("الرضا بالله", "رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً.", 3)
        ),
        "أذكار بعد الصلاة" to listOf(
            GuideItem("الاستغفار بعد السلام", "أستغفر الله (ثلاثاً)، اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام.", 1),
            GuideItem("التسبيح عقب الصلاة", "سبحان الله (33)، والحمد لله (33)، والله أكبر (33)، وتمام المائة لا إله إلا الله وحده لا شريك له.", 33),
            GuideItem("دعاء الإعانة", "اللهم أعني على ذكرك وشكرك وحسن عبادتك.", 1)
        ),
        "أذكار النوم" to listOf(
            GuideItem("باسمك اللهم", "باسمك ربي وضعت جنبي وبك أرفعه، فإن أمسكت نفسي فارحمها، وإن أرسلتها فاحفظها بما تحفظ به عبادك الصالحين.", 1),
            GuideItem("تسبيح النوم", "سبحان الله (33)، والحمد لله (33)، والله أكبر (34) قبل المنام.", 33),
            GuideItem("النفث بالمعوذات", "جمع الكفين والنفث فيهما وقراءة الإخلاص والفلق والناس ومسح الجسد ثلاثاً.", 3)
        )
    )

    val prayerSteps = mapOf(
        "صفة الوضوء" to listOf(
            GuideItem("1. النية والتسمية", "ينوي المسلم الوضوء بقلبه لرفع الحدث ثم يقول: بسم الله."),
            GuideItem("2. غسل الكفين والمضمضة والاستنشاق", "يغسل كفيه ثلاثاً، ثم يتمضمض ويستنشق ثلاث مرات."),
            GuideItem("3. غسل الوجه واليدين", "يغسل وجهه كاملاً ثلاثاً، ثم يغسل يديه من أطراف الأصابع إلى المرفقين ثلاثاً."),
            GuideItem("4. مسح الرأس والأذنين وغسل الرجلين", "يمسح رأسه وأذنيه مرة واحدة، ثم يغسل رجليه مع الكعبين ثلاثاً.")
        ),
        "صفة الصلاة" to listOf(
            GuideItem("1. استقبال القبلة وتكبيرة الإحرام", "يستقبل القبلة قائماً ويرفع يديه حذو منكبيه قائلاً: الله أكبر."),
            GuideItem("2. الاستفتاح وقراءة الفاتحة", "يقرأ دعاء الاستفتاح، ثم سورة الفاتحة (ركن)، وما تيسر من القرآن."),
            GuideItem("3. الركوع والاعتدال منه", "يكبر ويركع مطمئناً ويقول: سبحان ربي العظيم (ثلاثاً)، ثم يرفع قائلاً: سمع الله لمن حمده."),
            GuideItem("4. السجود والجلوس بين السجدتين", "يسجد على أعضائه السبعة قائلاً: سبحان ربي الأعلى (ثلاثاً)، ويجلس بينهما مطمئناً."),
            GuideItem("5. التشهد والتسليم", "يقرأ التشهد والصلاة الإبراهيمية في الجلسة الأخيرة، ثم يسلم عن يمينه وشماله: السلام عليكم ورحمة الله.")
        ),
        "أركان الصلاة" to listOf(
            GuideItem("القيام مع القدرة", "الوقوف في صلاة الفريضة لمن استطاع ذلك."),
            GuideItem("تكبيرة الإحرام", "قول (الله أكبر) في بداية الدخول في الصلاة."),
            GuideItem("قراءة الفاتحة", "قراءتها في كل ركعة من ركعات الصلاة."),
            GuideItem("الركوع والسجود والطمأنينة", "الاطمئنان في كل ركن وعدم الاستعجال فيه، والترتيب بين الأركان.")
        )
    )

    suspend fun getSurahList(context: Context): List<SurahInfo> = withContext(Dispatchers.IO) {
        val cacheFile = File(context.filesDir, "surahs_list_v2.json")
        val jsonStr = try {
            val fresh = URL("https://api.alquran.cloud/v1/surah").readText()
            cacheFile.writeText(fresh)
            fresh
        } catch (e: Exception) {
            if (cacheFile.exists()) cacheFile.readText() else throw e
        }
        val data = JSONObject(jsonStr).getJSONArray("data")
        val list = mutableListOf<SurahInfo>()
        for (i in 0 until data.length()) {
            val obj = data.getJSONObject(i)
            list.add(
                SurahInfo(
                    number = obj.getInt("number"),
                    name = obj.getString("name"),
                    englishName = obj.getString("englishName"),
                    numberOfAyahs = obj.getInt("numberOfAyahs"),
                    revelationType = obj.getString("revelationType")
                )
            )
        }
        list
    }

    suspend fun getSurahDetails(context: Context, surahNumber: Int): List<AyahItem> =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, "surah_v2_$surahNumber.json")
            val jsonStr = try {
                val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,ar.muyassar"
                val fresh = URL(url).readText()
                cacheFile.writeText(fresh)
                fresh
            } catch (e: Exception) {
                if (cacheFile.exists()) cacheFile.readText() else throw e
            }
            val editions = JSONObject(jsonStr).getJSONArray("data")
            val uthmaniAyahs = editions.getJSONObject(0).getJSONArray("ayahs")
            val tafsirAyahs = editions.getJSONObject(1).getJSONArray("ayahs")

            val result = mutableListOf<AyahItem>()
            for (i in 0 until uthmaniAyahs.length()) {
                val u = uthmaniAyahs.getJSONObject(i)
                val t = tafsirAyahs.getJSONObject(i)
                result.add(
                    AyahItem(
                        number = u.getInt("number"),
                        numberInSurah = u.getInt("numberInSurah"),
                        text = u.getString("text"),
                        tafsir = t.getString("text"),
                        juz = u.optInt("juz", 1),
                        page = u.optInt("page", 1)
                    )
                )
            }
            result
        }

    fun clearCache(context: Context) {
        context.filesDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".json")) file.delete()
        }
    }
}
