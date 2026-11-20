package com.quran.app.presentation

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import com.quran.app.data.AppPreferencesRepository

/**
 * Single source of truth for app-wide state. Screen composables remain stateless
 * and receive values/callbacks, making navigation and persistence predictable.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppPreferencesRepository(application)

    val isDarkTheme = mutableStateOf(repository.isDarkTheme())
    val currentRoute = mutableStateOf("home")
    val lastReadPage = mutableIntStateOf(repository.lastReadPage())
    val startReaderDirectly = mutableStateOf(false)
    val favorites = mutableStateOf(repository.favorites())
    val globalSearchQuery = mutableStateOf("")
    val qiblaIsolated = mutableStateOf(false)

    fun setDarkTheme(value: Boolean) {
        isDarkTheme.value = value
        repository.saveDarkTheme(value)
    }

    fun toggleDarkTheme() = setDarkTheme(!isDarkTheme.value)

    fun setLastReadPage(page: Int) {
        val safePage = page.coerceIn(1, 604)
        lastReadPage.intValue = safePage
        repository.saveLastReadPage(safePage)
    }

    fun toggleFavorite(page: Int) {
        val safePage = page.coerceIn(1, 604)
        val next = if (favorites.value.contains(safePage)) favorites.value - safePage else favorites.value + safePage
        favorites.value = next
        repository.saveFavorites(next)
    }

    fun openReader(page: Int = lastReadPage.intValue) {
        setLastReadPage(page)
        startReaderDirectly.value = true
        currentRoute.value = "quran_reader"
    }

    fun openHome() {
        qiblaIsolated.value = false
        currentRoute.value = "home"
    }
}
