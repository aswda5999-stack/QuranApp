package com.quran.app

import android.content.Context
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
    val audioUrl: String
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

data class AppSettings(
    val fontSize: Float = 24f,
    val isBoldFont: Boolean = false,
    val lineSpacing: Float = 1.6f,
    val isMushafMode: Boolean = false,
    val isDarkMode: Boolean = false,
    val themeIndex: Int = 0, // 0: Emerald, 1: Royal Blue, 2: Warm Gold, 3: Classic Paper
    val defaultReciter: String = "ar.alafasy",
    val autoPlayNext: Boolean = true,
    val keepScreenOn: Boolean = true,
    val showNavLabels: Boolean = true,
    val isGridView: Boolean = false,
    val vibrateOnTasbih: Boolean = true,
    val lastSurahNumber: Int = 1,
    val lastSurahName: String = "سُورَةُ ٱلْفَاتِحَةِ",
    val tasbihCount: Int = 0
)

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("quran_pro_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): AppSettings = AppSettings(
        fontSize = prefs.getFloat("fontSize", 24f),
        isBoldFont = prefs.getBoolean("isBoldFont", false),
        lineSpacing = prefs.getFloat("lineSpacing", 1.6f),
        isMushafMode = prefs.getBoolean("isMushafMode", false),
        isDarkMode = prefs.getBoolean("isDarkMode", false),
        themeIndex = prefs.getInt("themeIndex", 0),
        defaultReciter = prefs.getString("defaultReciter", "ar.alafasy") ?: "ar.alafasy",
        autoPlayNext = prefs.getBoolean("autoPlayNext", true),
        keepScreenOn = prefs.getBoolean("keepScreenOn", true),
        showNavLabels = prefs.getBoolean("showNavLabels", true),
        isGridView = prefs.getBoolean("isGridView", false),
        vibrateOnTasbih = prefs.getBoolean("vibrateOnTasbih", true),
        lastSurahNumber = prefs.getInt("lastSurahNumber", 1),
        lastSurahName = prefs.getString("lastSurahName", "سُورَةُ ٱلْفَاتِحَةِ") ?: "سُورَةُ ٱلْفَاتِحَةِ",
        tasbihCount = prefs.getInt("tasbihCount", 0)
    )

    fun saveSettings(s: AppSettings) {
        prefs.edit().apply {
            putFloat("fontSize", s.fontSize)
            putBoolean("isBoldFont", s.isBoldFont)
            putFloat("lineSpacing", s.lineSpacing)
            putBoolean("isMushafMode", s.isMushafMode)
            putBoolean("isDarkMode", s.isDarkMode)
            putInt("themeIndex", s.themeIndex)
            putString("defaultReciter", s.defaultReciter)
            putBoolean("autoPlayNext", s.autoPlayNext)
            putBoolean("keepScreenOn", s.keepScreenOn)
            putBoolean("showNavLabels", s.showNavLabels)
            putBoolean("isGridView", s.isGridView)
            putBoolean("vibrateOnTasbih", s.vibrateOnTasbih)
            putInt("lastSurahNumber", s.lastSurahNumber)
            putString("lastSurahName", s.lastSurahName)
            putInt("tasbihCount", s.tasbihCount)
            apply()
        }
    }

    fun saveLastRead(surahNumber: Int, surahName: String) {
        prefs.edit()
            .putInt("lastSurahNumber", surahNumber)
            .putString("lastSurahName", surahName)
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

object QuranRepository {
    val reciters = listOf(
        "ar.alafasy" to "مشاري راشد العفاسي",
        "ar.abdurrahmaansudais" to "عبدالرحمن السديس",
        "ar.husary" to "محمود خليل الحصري",
        "ar.minshawi" to "محمد صديق المنشاوي",
        "ar.mahermuaiqly" to "ماهر المعيقلي",
        "ar.shaatree" to "أبو بكر الشاطري"
    )

    val adhkarCategories = mapOf(
        "أذكار الصباح والمساء" to listOf(
            GuideItem("سيد الاستغفار", "اللهم أنت ربي لا إله إلا أنت، خلقتني وأنا عبدك، وأنا على عهدك ووعدك ما استطعت...", 1),
            GuideItem("آية الكرسي", "قراءة آية الكرسي مرة واحدة صباحاً ومساءً للحفظ من كل سوء.", 1),
            GuideItem("المعوذات الثلاث", "قراءة سورة الإخلاص والفلق والناس ثلاث مرات صباحاً ومساءً.", 3),
            GuideItem("التسبيح والتهليل", "سبحان الله وبحمده: مائة مرة، ولا إله إلا الله وحده لا شريك له: مائة مرة.", 100),
            GuideItem("الحفظ من الضرر", "بسم الله الذي لا يضر مع اسمه شيء في الأرض ولا في السماء وهو السميع العليم.", 3),
            GuideItem("الرضا بالله", "رضيت بالله رباً، وبالإسلام ديناً، وبمحمد صلى الله عليه وسلم نبياً.", 3)
        ),
        "أذكار بعد الصلاة" to listOf(
            GuideItem("الاستغفار بعد السلام", "أستغفر الله (ثلاثاً)، اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام.", 1),
            GuideItem("التسبيح عقب الصلاة", "سبحان الله (33)، والحمد لله (33)، والله أكبر (33)، وتمام المائة لا إله إلا الله.", 33),
            GuideItem("دعاء الإعانة", "اللهم أعني على ذكرك وشكرك وحسن عبادتك.", 1)
        ),
        "أذكار النوم" to listOf(
            GuideItem("باسمك اللهم", "باسمك ربي وضعت جنبي وبك أرفعه، فإن أمسكت نفسي فارحمها، وإن أرسلتها فاحفظها.", 1),
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
        val cacheFile = File(context.filesDir, "surahs_list.json")
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

    suspend fun getSurahDetails(context: Context, surahNumber: Int, reciter: String): List<AyahItem> =
        withContext(Dispatchers.IO) {
            val cacheFile = File(context.filesDir, "surah_${surahNumber}_$reciter.json")
            val jsonStr = try {
                val url = "https://api.alquran.cloud/v1/surah/$surahNumber/editions/quran-uthmani,ar.muyassar,$reciter"
                val fresh = URL(url).readText()
                cacheFile.writeText(fresh)
                fresh
            } catch (e: Exception) {
                if (cacheFile.exists()) cacheFile.readText() else throw e
            }
            val editions = JSONObject(jsonStr).getJSONArray("data")
            val uthmaniAyahs = editions.getJSONObject(0).getJSONArray("ayahs")
            val tafsirAyahs = editions.getJSONObject(1).getJSONArray("ayahs")
            val audioAyahs = editions.getJSONObject(2).getJSONArray("ayahs")

            val result = mutableListOf<AyahItem>()
            for (i in 0 until uthmaniAyahs.length()) {
                val u = uthmaniAyahs.getJSONObject(i)
                val t = tafsirAyahs.getJSONObject(i)
                val a = audioAyahs.getJSONObject(i)
                result.add(
                    AyahItem(
                        number = u.getInt("number"),
                        numberInSurah = u.getInt("numberInSurah"),
                        text = u.getString("text"),
                        tafsir = t.getString("text"),
                        audioUrl = a.optString("audio", "")
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
