package com.quran.app

import android.content.Context
import android.view.SoundEffectConstants
import android.view.View
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class SurahInfo(
    val number: Int,
    val name: String,
    val type: String,
    val ayahCount: Int,
    val startPage: Int
)

data class ReciterInfo(
    val id: String,
    val name: String,
    val style: String,
    val audioBaseUrl: String,
    val imageUrl: String
)

data class HadithItem(
    val id: Int,
    val title: String,
    val text: String,
    val narrator: String,
    val source: String,
    val explanation: String
)

data class DhikrItem(
    val id: Int,
    val category: String,
    val text: String,
    val targetCount: Int,
    val reward: String
)

data class AppSettingsState(
    val touchSoundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val nightMushafMode: Boolean = false,
    val keepScreenOn: Boolean = true,
    val lastReadPage: Int = 1,
    val lastReadSurahName: String = "سورة الفاتحة",
    val selectedReciterId: String = "afs",
    val totalTasbihCount: Int = 0
)

object AppPreferences {
    private const val PREFS_NAME = "quran_app_v3_prefs"

    private val _settings = MutableStateFlow(AppSettingsState())
    val settings: StateFlow<AppSettingsState> = _settings.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _settings.value = AppSettingsState(
            touchSoundEnabled = prefs.getBoolean("touch_sound", true),
            vibrationEnabled = prefs.getBoolean("vibration", true),
            nightMushafMode = prefs.getBoolean("night_mushaf", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", true),
            lastReadPage = prefs.getInt("last_read_page", 1).coerceIn(1, 604),
            lastReadSurahName = prefs.getString("last_read_surah", "سورة الفاتحة") ?: "سورة الفاتحة",
            selectedReciterId = prefs.getString("selected_reciter", "afs") ?: "afs",
            totalTasbihCount = prefs.getInt("total_tasbih", 0)
        )
    }

    fun playTouchSound(view: View?) {
        if (_settings.value.touchSoundEnabled) {
            view?.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    fun setTouchSound(context: Context, enabled: Boolean) {
        _settings.value = _settings.value.copy(touchSoundEnabled = enabled)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("touch_sound", enabled).apply()
    }

    fun setVibration(context: Context, enabled: Boolean) {
        _settings.value = _settings.value.copy(vibrationEnabled = enabled)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("vibration", enabled).apply()
    }

    fun setNightMushafMode(context: Context, enabled: Boolean) {
        _settings.value = _settings.value.copy(nightMushafMode = enabled)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("night_mushaf", enabled).apply()
    }

    fun setKeepScreenOn(context: Context, enabled: Boolean) {
        _settings.value = _settings.value.copy(keepScreenOn = enabled)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean("keep_screen_on", enabled).apply()
    }

    fun updateLastRead(context: Context, page: Int, surahName: String) {
        val safePage = page.coerceIn(1, 604)
        _settings.value = _settings.value.copy(
            lastReadPage = safePage,
            lastReadSurahName = surahName
        )
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("last_read_page", safePage)
            .putString("last_read_surah", surahName)
            .apply()
    }

    fun setSelectedReciter(context: Context, reciterId: String) {
        _settings.value = _settings.value.copy(selectedReciterId = reciterId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("selected_reciter", reciterId).apply()
    }

    fun incrementTotalTasbih(context: Context) {
        val updated = _settings.value.totalTasbihCount + 1
        _settings.value = _settings.value.copy(totalTasbihCount = updated)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putInt("total_tasbih", updated).apply()
    }

    fun getFavoritePages(context: Context): Set<Int> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet("favorite_pages", emptySet()) ?: emptySet()
        return raw.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleFavoritePage(context: Context, page: Int): Set<Int> {
        val current = getFavoritePages(context).toMutableSet()
        if (current.contains(page)) current.remove(page) else current.add(page)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putStringSet("favorite_pages", current.map { it.toString() }.toSet()).apply()
        return current
    }
}

object AudioDownloadManager {
    private val _downloadProgress = MutableStateFlow>(emptyMap())
    val downloadProgress: StateFlow> = _downloadProgress.asStateFlow()

    private fun key(reciterId: String, surahNumber: Int): String = "${reciterId}_$surahNumber"

    fun getLocalAudioFile(context: Context, reciterId: String, surahNumber: Int): File {
        val dir = File(context.filesDir, "quran_audio/$reciterId")
        if (!dir.exists()) dir.mkdirs()
        val padded = surahNumber.toString().padStart(3, '0')
        return File(dir, "$padded.mp3")
    }

    fun isSurahDownloaded(context: Context, reciterId: String, surahNumber: Int): Boolean {
        val file = getLocalAudioFile(context, reciterId, surahNumber)
        return file.exists() && file.length() > 2048L
    }

    fun getDownloadedCount(context: Context, reciterId: String): Int {
        val dir = File(context.filesDir, "quran_audio/$reciterId")
        if (!dir.exists()) return 0
        return dir.listFiles()?.count { it.extension == "mp3" && it.length() > 2048L } ?: 0
    }

    suspend fun downloadSurah(
        context: Context,
        reciter: ReciterInfo,
        surahNumber: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val taskKey = key(reciter.id, surahNumber)
        if (_downloadProgress.value.containsKey(taskKey)) return@withContext false

        val targetFile = getLocalAudioFile(context, reciter.id, surahNumber)
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        val padded = surahNumber.toString().padStart(3, '0')
        val urlString = "${reciter.audioBaseUrl}/$padded.mp3"

        try {
            _downloadProgress.value = _downloadProgress.value + (taskKey to 1)
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 20000
                instanceFollowRedirects = true
                connect()
            }

            if (connection.responseCode !in 200..299) {
                _downloadProgress.value = _downloadProgress.value - taskKey
                return@withContext false
            }

            val totalBytes = connection.contentLength.toLong()
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesCopied = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesCopied += read
                        val percent = if (totalBytes > 0L) {
                            ((bytesCopied * 100L) / totalBytes).toInt().coerceIn(1, 99)
                        } else {
                            50
                        }
                        _downloadProgress.value = _downloadProgress.value + (taskKey to percent)
                    }
                    output.flush()
                }
            }

            if (tempFile.exists() && tempFile.length() > 2048L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                _downloadProgress.value = _downloadProgress.value + (taskKey to 100)
                _downloadProgress.value = _downloadProgress.value - taskKey
                return@withContext true
            } else {
                tempFile.delete()
                _downloadProgress.value = _downloadProgress.value - taskKey
                return@withContext false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tempFile.delete()
            _downloadProgress.value = _downloadProgress.value - taskKey
            return@withContext false
        }
    }
}

object QuranRepository {

    const val TOTAL_MUSHAF_PAGES = 604

    fun getMushafPageImageUrl(pageNumber: Int): String {
        val safePage = pageNumber.coerceIn(1, TOTAL_MUSHAF_PAGES)
        val padded = safePage.toString().padStart(3, '0')
        return "https://android.quran.com/data/width_1024/page$padded.png"
    }

    val reciters = listOf(
        ReciterInfo(
            id = "afs",
            name = "مشاري راشد العفاسي",
            style = "حفص عن عاصم - مرتل",
            audioBaseUrl = "https://server8.mp3quran.net/afs",
            imageUrl = "https://static.qurancdn.com/images/reciters/6/mishary-rashid-alafasy-profile.jpeg"
        ),
        ReciterInfo(
            id = "basit",
            name = "عبد الباسط عبد الصمد",
            style = "المصحف المرتل",
            audioBaseUrl = "https://server7.mp3quran.net/basit",
            imageUrl = "https://static.qurancdn.com/images/reciters/1/abdulbaset-abdulsamad-profile.jpeg"
        ),
        ReciterInfo(
            id = "maher",
            name = "ماهر المعيقلي",
            style = "حفص عن عاصم - مرتل",
            audioBaseUrl = "https://server12.mp3quran.net/maher",
            imageUrl = "https://static.qurancdn.com/images/reciters/10/maher-al-muaiqly-profile.jpeg"
        ),
        ReciterInfo(
            id = "s_gmd",
            name = "سعد الغامدي",
            style = "حفص عن عاصم - مرتل",
            audioBaseUrl = "https://server7.mp3quran.net/s_gmd",
            imageUrl = "https://static.qurancdn.com/images/reciters/7/saad-al-ghamdi-profile.jpeg"
        ),
        ReciterInfo(
            id = "yasser",
            name = "ياسر الدوسري",
            style = "حفص عن عاصم - مرتل",
            audioBaseUrl = "https://server11.mp3quran.net/yasser",
            imageUrl = "https://static.qurancdn.com/images/reciters/11/yasser-ad-dussary-profile.jpeg"
        )
    )

    val surahs = listOf(
        SurahInfo(1, "سورة الفاتحة", "مكية", 7, 1),
        SurahInfo(2, "سورة البقرة", "مدنية", 286, 2),
        SurahInfo(3, "سورة آل عمران", "مدنية", 200, 50),
        SurahInfo(4, "سورة النساء", "مدنية", 176, 77),
        SurahInfo(5, "سورة المائدة", "مدنية", 120, 106),
        SurahInfo(6, "سورة الأنعام", "مكية", 165, 128),
        SurahInfo(7, "سورة الأعراف", "مكية", 206, 151),
        SurahInfo(8, "سورة الأنفال", "مدنية", 75, 177),
        SurahInfo(9, "سورة التوبة", "مدنية", 129, 187),
        SurahInfo(10, "سورة يونس", "مكية", 109, 208),
        SurahInfo(11, "سورة هود", "مكية", 123, 221),
        SurahInfo(12, "سورة يوسف", "مكية", 111, 235),
        SurahInfo(13, "سورة الرعد", "مدنية", 43, 249),
        SurahInfo(14, "سورة إبراهيم", "مكية", 52, 255),
        SurahInfo(15, "سورة الحجر", "مكية", 99, 262),
        SurahInfo(16, "سورة النحل", "مكية", 128, 267),
        SurahInfo(17, "سورة الإسراء", "مكية", 111, 282),
        SurahInfo(18, "سورة الكهف", "مكية", 110, 293),
        SurahInfo(19, "سورة مريم", "مكية", 98, 305),
        SurahInfo(20, "سورة طه", "مكية", 135, 312),
        SurahInfo(21, "سورة الأنبياء", "مكية", 112, 322),
        SurahInfo(22, "سورة الحج", "مدنية", 78, 332),
        SurahInfo(23, "سورة المؤمنون", "مكية", 118, 342),
        SurahInfo(24, "سورة النور", "مدنية", 64, 350),
        SurahInfo(25, "سورة الفرقان", "مكية", 77, 359),
        SurahInfo(26, "سورة الشعراء", "مكية", 227, 367),
        SurahInfo(27, "سورة النمل", "مكية", 93, 377),
        SurahInfo(28, "سورة القصص", "مكية", 88, 385),
        SurahInfo(29, "سورة العنكبوت", "مكية", 69, 396),
        SurahInfo(30, "سورة الروم", "مكية", 60, 404),
        SurahInfo(31, "سورة لقمان", "مكية", 34, 411),
        SurahInfo(32, "سورة السجدة", "مكية", 30, 415),
        SurahInfo(33, "سورة الأحزاب", "مدنية", 73, 418),
        SurahInfo(34, "سورة سبأ", "مكية", 54, 428),
        SurahInfo(35, "سورة فاطر", "مكية", 45, 434),
        SurahInfo(36, "سورة يس", "مكية", 83, 440),
        SurahInfo(37, "سورة الصافات", "مكية", 182, 446),
        SurahInfo(38, "سورة ص", "مكية", 88, 453),
        SurahInfo(39, "سورة الزمر", "مكية", 75, 458),
        SurahInfo(40, "سورة غافر", "مكية", 85, 467),
        SurahInfo(41, "سورة فصلت", "مكية", 54, 477),
        SurahInfo(42, "سورة الشورى", "مكية", 53, 483),
        SurahInfo(43, "سورة الزخرف", "مكية", 89, 489),
        SurahInfo(44, "سورة الدخان", "مكية", 59, 496),
        SurahInfo(45, "سورة الجاثية", "مكية", 37, 499),
        SurahInfo(46, "سورة الأحقاف", "مكية", 35, 502),
        SurahInfo(47, "سورة محمد", "مدنية", 38, 507),
        SurahInfo(48, "سورة الفتح", "مدنية", 29, 511),
        SurahInfo(49, "سورة الحجرات", "مدنية", 18, 515),
        SurahInfo(50, "سورة ق", "مكية", 45, 518),
        SurahInfo(51, "سورة الذاريات", "مكية", 60, 520),
        SurahInfo(52, "سورة الطور", "مكية", 49, 523),
        SurahInfo(53, "سورة النجم", "مكية", 62, 526),
        SurahInfo(54, "سورة القمر", "مكية", 55, 528),
        SurahInfo(55, "سورة الرحمن", "مدنية", 78, 531),
        SurahInfo(56, "سورة الواقعة", "مكية", 96, 534),
        SurahInfo(57, "سورة الحديد", "مدنية", 29, 537),
        SurahInfo(58, "سورة المجادلة", "مدنية", 22, 542),
        SurahInfo(59, "سورة الحشر", "مدنية", 24, 545),
        SurahInfo(60, "سورة الممتحنة", "مدنية", 13, 549),
        SurahInfo(61, "سورة الصف", "مدنية", 14, 551),
        SurahInfo(62, "سورة الجمعة", "مدنية", 11, 553),
        SurahInfo(63, "سورة المنافقون", "مدنية", 11, 554),
        SurahInfo(64, "سورة التغابن", "مدنية", 18, 556),
        SurahInfo(65, "سورة الطلاق", "مدنية", 12, 558),
        SurahInfo(66, "سورة التحريم", "مدنية", 12, 560),
        SurahInfo(67, "سورة الملك", "مكية", 30, 562),
        SurahInfo(68, "سورة القلم", "مكية", 52, 564),
        SurahInfo(69, "سورة الحاقة", "مكية", 52, 566),
        SurahInfo(70, "سورة المعارج", "مكية", 44, 568),
        SurahInfo(71, "سورة نوح", "مكية", 28, 570),
        SurahInfo(72, "سورة الجن", "مكية", 28, 572),
        SurahInfo(73, "سورة المزمل", "مكية", 20, 574),
        SurahInfo(74, "سورة المدثر", "مكية", 56, 575),
        SurahInfo(75, "سورة القيامة", "مكية", 40, 577),
        SurahInfo(76, "سورة الإنسان", "مدنية", 31, 578),
        SurahInfo(77, "سورة المرسلات", "مكية", 50, 580),
        SurahInfo(78, "سورة النبأ", "مكية", 40, 582),
        SurahInfo(79, "سورة النازعات", "مكية", 46, 583),
        SurahInfo(80, "سورة عبس", "مكية", 42, 585),
        SurahInfo(81, "سورة التكوير", "مكية", 29, 586),
        SurahInfo(82, "سورة الانفطار", "مكية", 19, 587),
        SurahInfo(83, "سورة المطففين", "مكية", 36, 587),
        SurahInfo(84, "سورة الانشقاق", "مكية", 25, 589),
        SurahInfo(85, "سورة البروج", "مكية", 22, 590),
        SurahInfo(86, "سورة الطارق", "مكية", 17, 591),
        SurahInfo(87, "سورة الأعلى", "مكية", 19, 591),
        SurahInfo(88, "سورة الغاشية", "مكية", 26, 592),
        SurahInfo(89, "سورة الفجر", "مكية", 30, 593),
        SurahInfo(90, "سورة البلد", "مكية", 20, 594),
        SurahInfo(91, "سورة الشمس", "مكية", 15, 595),
        SurahInfo(92, "سورة الليل", "مكية", 21, 595),
        SurahInfo(93, "سورة الضحى", "مكية", 11, 596),
        SurahInfo(94, "سورة الشرح", "مكية", 8, 596),
        SurahInfo(95, "سورة التين", "مكية", 8, 597),
        SurahInfo(96, "سورة العلق", "مكية", 19, 597),
        SurahInfo(97, "سورة القدر", "مكية", 5, 598),
        SurahInfo(98, "سورة البينة", "مدنية", 8, 598),
        SurahInfo(99, "سورة الزلزلة", "مدنية", 8, 599),
        SurahInfo(100, "سورة العاديات", "مكية", 11, 599),
        SurahInfo(101, "سورة القارعة", "مكية", 11, 600),
        SurahInfo(102, "سورة التكاثر", "مكية", 8, 600),
        SurahInfo(103, "سورة العصر", "مكية", 3, 601),
        SurahInfo(104, "سورة الهمزة", "مكية", 9, 601),
        SurahInfo(105, "سورة الفيل", "مكية", 5, 601),
        SurahInfo(106, "سورة قريش", "مكية", 4, 602),
        SurahInfo(107, "سورة الماعون", "مكية", 7, 602),
        SurahInfo(108, "سورة الكوثر", "مكية", 3, 602),
        SurahInfo(109, "سورة الكافرون", "مكية", 6, 603),
        SurahInfo(110, "سورة النصر", "مدنية", 3, 603),
        SurahInfo(111, "سورة المسد", "مكية", 5, 603),
        SurahInfo(112, "سورة الإخلاص", "مكية", 4, 604),
        SurahInfo(113, "سورة الفلق", "مكية", 5, 604),
        SurahInfo(114, "سورة الناس", "مكية", 6, 604)
    )

    fun getSurahNameByNumber(number: Int): String {
        return surahs.find { it.number == number }?.name ?: "سورة الفاتحة"
    }

    fun getSurahByPage(page: Int): SurahInfo {
        return surahs.lastOrNull { it.startPage <= page } ?: surahs.first()
    }

    fun getJuzByPage(page: Int): Int {
        return ((page - 2) / 20 + 1).coerceIn(1, 30)
    }

    val hadiths = listOf(
        HadithItem(
            id = 1,
            title = "إنما الأعمال بالنيات",
            text = "عَنْ أَمِيرِ الْمُؤْمِنِينَ أَبِي حَفْصٍ عُمَرَ بْنِ الْخَطَّابِ رَضِيَ اللَّهُ عَنْهُ قَالَ: سَمِعْتُ رَسُولَ اللَّهِ ﷺ يَقُولُ: «إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ فَهِجْرَتُهُ إِلَى اللَّهِ وَرَسُولِهِ، وَمَنْ كَانَتْ هِجْرَتُهُ لِدُنْيَا يُصِيبُهَا أَوْ امْرَأَةٍ يَنْكِحُهَا فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ».",
            narrator = "عمر بن الخطاب رضي الله عنه",
            source = "متفق عليه (البخاري ومسلم)",
            explanation = "أصل عظيم في الإسلام يبين أن قبول الأعمال وثوابها عند الله مرتبط بإخلاص النية لوجهه الكريم."
        ),
        HadithItem(
            id = 2,
            title = "بني الإسلام على خمس",
            text = "عَنْ أَبِي عَبْدِ الرَّحْمَنِ عَبْدِ اللَّهِ بْنِ عُمَرَ بْنِ الْخَطَّابِ رَضِيَ اللَّهُ عَنْهُمَا قَالَ: سَمِعْتُ رَسُولَ اللَّهِ ﷺ يَقُولُ: «بُنِيَ الإِسْلامُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لا إِلَهَ إِلا اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاةِ، وَإِيتَاءِ الزَّكَاةِ، وَحَجِّ الْبَيْتِ، وَصَوْمِ رَمَضَانَ».",
            narrator = "عبد الله بن عمر رضي الله عنهما",
            source = "متفق عليه (البخاري ومسلم)",
            explanation = "يوضح الحديث أركان الإسلام الخمسة التي يقوم عليها دين المسلم."
        ),
        HadithItem(
            id = 3,
            title = "من حسن إسلام المرء",
            text = "عَنْ أَبِي هُرَيْرَةَ رَضِيَ اللَّهُ عَنْهُ قَالَ: قَالَ رَسُولُ اللَّهِ ﷺ: «مِنْ حُسْنِ إِسْلامِ الْمَرْءِ تَرْكُهُ مَا لا يَعْنِيهِ».",
            narrator = "أبو هريرة رضي الله عنه",
            source = "رواه الترمذي (حديث حسن)",
            explanation = "من علامات كمال إيمان المسلم انشغاله بما ينفعه في دينه ودنياه وتركه الفضول فيما لا يخصه."
        ),
        HadithItem(
            id = 4,
            title = "لا يؤمن أحدكم حتى يحب لأخيه",
            text = "عَنْ أَبِي حَمْزَةَ أَنَسِ بْنِ مَالِكٍ رَضِيَ اللَّهُ عَنْهُ خَادِمِ رَسُولِ اللَّهِ ﷺ عَنِ النَّبِيِّ ﷺ قَالَ: «لا يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبَّ لِنَفْسِهِ».",
            narrator = "أنس بن مالك رضي الله عنه",
            source = "متفق عليه (البخاري ومسلم)",
            explanation = "يربي الحديث قلب المؤمن على سلامة الصدر ونقاء السريرة ومحبة الخير لجميع المسلمين."
        ),
        HadithItem(
            id = 5,
            title = "الكلمة الطيبة صدقة",
            text = "عَنْ أَبِي هُرَيْرَةَ رَضِيَ اللَّهُ عَنْهُ قَالَ: قَالَ رَسُولُ اللَّهِ ﷺ: «كُلُّ سُلامَى مِنَ النَّاسِ عَلَيْهِ صَدَقَةٌ، كُلَّ يَوْمٍ تَطْلُعُ فِيهِ الشَّمْسُ: تَعْدِلُ بَيْنَ الاثْنَيْنِ صَدَقَةٌ، وَتُعِينُ الرَّجُلَ فِي دَابَّتِهِ فَتَحْمِلُهُ عَلَيْهَا أَوْ تَرْفَعُ لَهُ عَلَيْهَا مَتَاعَهُ صَدَقَةٌ، وَالْكَلِمَةُ الطَّيِّبَةُ صَدَقَةٌ، وَبِكُلِّ خُطْوَةٍ تَمْشِيهَا إِلَى الصَّلاةِ صَدَقَةٌ، وَتُمِيطُ الأَذَى عَنِ الطَّرِيقِ صَدَقَةٌ».",
            narrator = "أبو هريرة رضي الله عنه",
            source = "متفق عليه (البخاري ومسلم)",
            explanation = "يفتح الحديث أبواب الخير والصدقة اليومية لكل مسلم بيسر وسهولة."
        ),
        HadithItem(
            id = 6,
            title = "اتق الله حيثما كنت",
            text = "عَنْ أَبِي ذَرٍّ جُنْدَبِ بْنِ جُنَادَةَ وَأَبِي عَبْدِ الرَّحْمَنِ مُعَاذِ بْنِ جَبَلٍ رَضِيَ اللَّهُ عَنْهُمَا عَنْ رَسُولِ اللَّهِ ﷺ قَالَ: «اتَّقِ اللَّهَ حَيْثُمَا كُنْتَ، وَأَتْبِعِ السَّيِّئَةَ الْحَسَنَةَ تَمْحُهَا، وَخَالِقِ النَّاسَ بِخُلُقٍ حَسَنٍ».",
            narrator = "أبو ذر ومعاذ بن جبل رضي الله عنهما",
            source = "رواه الترمذي",
            explanation = "وصية نبوية جامعة تجمع حقوق الله بالتقوى والاستغفار، وحقوق العباد بحسن الخلق."
        ),
        HadithItem(
            id = 7,
            title = "الدين النصيحة",
            text = "عَنْ أَبِي رُقَيَّةَ تَمِيمِ بْنِ أَوْسٍ الدَّارِيِّ رَضِيَ اللَّهُ عَنْهُ أَنَّ النَّبِيَّ ﷺ قَالَ: «الدِّينُ النَّصِيحَةُ» قُلْنَا: لِمَنْ؟ قَالَ: «لِلَّهِ وَلِكِتَابِهِ وَلِرَسُولِهِ وَلأَئِمَّةِ الْمُسْلِمِينَ وَعَامَّتِهِمْ».",
            narrator = "تميم بن أوس الداري رضي الله عنه",
            source = "رواه مسلم",
            explanation = "يبين الحديث أن قوام الدين وكماله في صدق التوجه لله والعمل بكتابه وسنة نبيه وبذل الخير للناس."
        ),
        HadithItem(
            id = 8,
            title = "فضل قراءة القرآن",
            text = "عَنْ أَبِي أُمَامَةَ الْبَاهِلِيِّ رَضِيَ اللَّهُ عَنْهُ قَالَ: سَمِعْتُ رَسُولَ اللَّهِ ﷺ يَقُولُ: «اقْرَءُوا الْقُرْآنَ فَإِنَّهُ يَأْتِي يَوْمَ الْقِيَامَةِ شَفِيعًا لأَصْحَابِهِ».",
            narrator = "أبو أمامة الباهلي رضي الله عنه",
            source = "رواه مسلم",
            explanation = "حثٌّ عظيم على ملازمة تلاوة القرآن الكريم وتدبره لينال المسلم شفاعته يوم القيامة."
        )
    )

    val adhkarList = listOf(
        DhikrItem(
            id = 1,
            category = "أذكار الصباح",
            text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 1,
            reward = "بداية اليوم بالتوحيد وحمد الله وطلب خيره."
        ),
        DhikrItem(
            id = 2,
            category = "أذكار الصباح",
            text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
            targetCount = 3,
            reward = "من أعظم الأذكار المضاعفة في الأجر."
        ),
        DhikrItem(
            id = 3,
            category = "أذكار الصباح",
            text = "بِسْمِ اللَّهِ الَّذِي لاَ يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الأَرْضِ وَلاَ فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ.",
            targetCount = 3,
            reward = "من قالها ثلاثاً لم يضره شيء بإذن الله."
        ),
        DhikrItem(
            id = 4,
            category = "أذكار المساء",
            text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ.",
            targetCount = 1,
            reward = "حفظ وتحصين للمسلم في مسائه."
        ),
        DhikrItem(
            id = 5,
            category = "أذكار المساء",
            text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
            targetCount = 3,
            reward = "تحصين من كل شر وآفة في الليل."
        ),
        DhikrItem(
            id = 6,
            category = "أذكار المساء",
            text = "حَسْبِيَ اللَّهُ لاَ إِلَهَ إِلاَّ هُوَ عَلَيْهِ تَوَكَّلْتُ وَهُوَ رَبُّ الْعَرْشِ الْعَظِيمِ.",
            targetCount = 7,
            reward = "من قالها سبعاً كفاه الله ما أهمه من أمر الدنيا والآخرة."
        ),
        DhikrItem(
            id = 7,
            category = "أذكار بعد الصلاة",
            text = "أَسْتَغْفِرُ اللَّهَ (ثلاثاً) اللَّهُمَّ أَنْتَ السَّلاَمُ وَمِنْكَ السَّلاَمُ، تَبَارَكْتَ يَا ذَا الْجَلاَلِ وَالإِكْرَامِ.",
            targetCount = 3,
            reward = "سنة نبوية ثابتة عقب السلام من كل صلاة مفروضة."
        ),
        DhikrItem(
            id = 8,
            category = "أذكار بعد الصلاة",
            text = "سُبْحَانَ اللَّهِ، وَالْحَمْدُ لِلَّهِ، وَاللَّهُ أَكْبَرُ.",
            targetCount = 33,
            reward = "غُفرت خطاياه وإن كانت مثل زبد البحر."
        ),
        DhikrItem(
            id = 9,
            category = "أذكار النوم",
            text = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا.",
            targetCount = 1,
            reward = "تفويض الأمر لله عند النوم."
        ),
        DhikrItem(
            id = 10,
            category = "أذكار النوم",
            text = "سُبْحَانَ اللَّهِ (33) وَالْحَمْدُ لِلَّهِ (33) وَاللَّهُ أَكْبَرُ (34).",
            targetCount = 33,
            reward = "خير للمؤمن من خادم وتعينه على نشاطه."
        )
    )
}
