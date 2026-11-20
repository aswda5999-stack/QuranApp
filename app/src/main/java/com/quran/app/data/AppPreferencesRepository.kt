package com.quran.app.data

import android.content.Context

/**
 * Versioned local persistence boundary. UI code never accesses SharedPreferences directly.
 * All writes use apply() so user interactions stay off the main thread.
 */
class AppPreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("quran_app_v3", Context.MODE_PRIVATE)

    init {
        if (!prefs.getBoolean("migration.v2.done", false)) {
            val old = appContext.getSharedPreferences("quran_app_v2", Context.MODE_PRIVATE)
            val editor = prefs.edit()
                .putBoolean("appearance.dark", old.getBoolean("appearance.dark", false))
                .putInt("reading.last_page", old.getInt("reading.last_page", 1).coerceIn(1, 604))
            old.getStringSet("reading.favorites", emptySet())?.let {
                editor.putStringSet("reading.favorites", it.toSet())
            }
            editor.putBoolean("migration.v2.done", true).apply()
        }
    }

    fun isDarkTheme() = prefs.getBoolean("appearance.dark", false)
    fun saveDarkTheme(value: Boolean) { prefs.edit().putBoolean("appearance.dark", value).apply() }

    fun lastReadPage() = prefs.getInt("reading.last_page", 1).coerceIn(1, 604)
    fun saveLastReadPage(value: Int) { prefs.edit().putInt("reading.last_page", value.coerceIn(1, 604)).apply() }

    fun favorites(): Set<Int> = prefs.getStringSet("reading.favorites", emptySet())
        .orEmpty().mapNotNull { it.toIntOrNull() }.filter { it in 1..604 }.toSet()

    fun saveFavorites(value: Set<Int>) {
        prefs.edit().putStringSet("reading.favorites", value.map(Int::toString).toSet()).apply()
    }

    fun dhikrCount(id: Int): Int = prefs.getInt("dhikr.count.$id", 0).coerceAtLeast(0)
    fun saveDhikrCount(id: Int, count: Int) {
        prefs.edit().putInt("dhikr.count.$id", count.coerceAtLeast(0)).apply()
    }
    fun resetDhikrCounts(ids: Collection<Int>) {
        prefs.edit().apply { ids.forEach { remove("dhikr.count.$it") } }.apply()
    }

    fun khatmaTargetPages(): Int = prefs.getInt("khatma.target_pages", 20).coerceIn(1, 100)
    fun saveKhatmaTargetPages(value: Int) {
        prefs.edit().putInt("khatma.target_pages", value.coerceIn(1, 100)).apply()
    }

    fun audioReciterIndex(): Int = prefs.getInt("audio.reciter", 0).coerceAtLeast(0)
    fun saveAudioReciterIndex(value: Int) { prefs.edit().putInt("audio.reciter", value.coerceAtLeast(0)).apply() }
    fun audioSurahIndex(): Int = prefs.getInt("audio.surah", 0).coerceAtLeast(0)
    fun saveAudioSurahIndex(value: Int) { prefs.edit().putInt("audio.surah", value.coerceAtLeast(0)).apply() }

    fun voiceRemindersEnabled(): Boolean = prefs.getBoolean("voice.enabled", false)
}
