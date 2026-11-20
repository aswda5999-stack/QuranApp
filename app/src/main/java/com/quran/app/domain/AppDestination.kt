package com.quran.app.domain

/** Central route contract used by the presentation layer. */
enum class AppDestination(val route: String) {
    HOME("home"), QURAN_INDEX("quran_index"), QURAN_READER("quran_reader"),
    AUDIO("audio"), PRAYER("adhan"), QIBLA("qibla"), ADHKAR("adhkar"), SETTINGS("settings")
}
