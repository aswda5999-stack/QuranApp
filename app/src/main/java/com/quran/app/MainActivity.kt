package com.quran.app

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BrightnessLow
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QuranProApp()
            }
        }
    }
}

@Composable
fun QuranProApp() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.loadSettings()) }

    // تفعيل أو إيقاف إبقاء الشاشة مضاءة ديناميكياً
    LaunchedEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val colorScheme = remember(settings.isDarkMode, settings.themeIndex) {
        buildAppColorScheme(settings.isDarkMode, settings.themeIndex)
    }

    // حالة التنقل
    var currentTab by remember { mutableIntStateOf(0) } // 0: الرئيسية، 1: الأذكار، 2: إشارة، 3: الإعدادات
    var subScreen by remember { mutableStateOf("NONE") } // NONE, SURAH_LIST, SURAH_DETAIL, PRAYER
    var isAudioMode by remember { mutableStateOf(false) }
    var selectedSurahNum by remember { mutableIntStateOf(1) }
    var selectedSurahName by remember { mutableStateOf("سُورَةُ ٱلْفَاتِحَةِ") }

    BackHandler(enabled = subScreen != "NONE") {
        subScreen = when (subScreen) {
            "SURAH_DETAIL" -> "SURAH_LIST"
            else -> "NONE"
        }
    }

    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            bottomBar = {
                if (subScreen == "NONE") {
                    NavigationBar {
                        val navItems = listOf(
                            Triple("الرئيسية", Icons.Default.Home, 0),
                            Triple("الأذكار", Icons.Default.BrightnessLow, 1),
                            Triple("إشارة", Icons.Default.Bookmark, 2),
                            Triple("الإعدادات", Icons.Default.Settings, 3)
                        )
                        navItems.forEach { (label, icon, index) ->
                            NavigationBarItem(
                                selected = currentTab == index,
                                onClick = {
                                    currentTab = index
                                    settings = settingsManager.loadSettings()
                                },
                                icon = { Icon(icon, contentDescription = label) },
                                label = if (settings.showNavLabels) {
                                    { Text(label) }
                                } else null
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (subScreen) {
                    "SURAH_LIST" -> SurahListScreen(
                        isAudioMode = isAudioMode,
                        settings = settings,
                        onBack = { subScreen = "NONE" },
                        onSelectSurah = { surah ->
                            selectedSurahNum = surah.number
                            selectedSurahName = surah.name
                            subScreen = "SURAH_DETAIL"
                        }
                    )
                    "SURAH_DETAIL" -> SurahDetailScreen(
                        surahNumber = selectedSurahNum,
                        surahName = selectedSurahName,
                        autoStartAudio = isAudioMode,
                        settings = settings,
                        settingsManager = settingsManager,
                        onBack = {
                            settings = settingsManager.loadSettings()
                            subScreen = "SURAH_LIST"
                        }
                    )
                    "PRAYER" -> PrayerGuideScreen(
                        onBack = { subScreen = "NONE" }
                    )
                    else -> {
                        when (currentTab) {
                            0 -> HomeScreen(
                                settings = settings,
                                onOpenQuran = {
                                    isAudioMode = false
                                    subScreen = "SURAH_LIST"
                                },
                                onOpenAudioQuran = {
                                    isAudioMode = true
                                    subScreen = "SURAH_LIST"
                                },
                                onOpenAdhkar = { currentTab = 1 },
                                onOpenPrayer = { subScreen = "PRAYER" },
                                onContinueReading = { num, name ->
                                    isAudioMode = false
                                    selectedSurahNum = num
                                    selectedSurahName = name
                                    subScreen = "SURAH_DETAIL"
                                }
                            )
                            1 -> AdhkarScreen(
                                settings = settings,
                                settingsManager = settingsManager
                            )
                            2 -> BookmarksScreen(
                                settingsManager = settingsManager,
                                onOpenSurah = { num, name ->
                                    isAudioMode = false
                                    selectedSurahNum = num
                                    selectedSurahName = name
                                    subScreen = "SURAH_DETAIL"
                                }
                            )
                            3 -> SettingsScreen(
                                settings = settings,
                                settingsManager = settingsManager,
                                onUpdateSettings = { updated ->
                                    settings = updated
                                    settingsManager.saveSettings(updated)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildAppColorScheme(isDark: Boolean, themeIndex: Int): ColorScheme {
    val primaryColor = when (themeIndex) {
        1 -> Color(0xFF0D47A1) // الأزرق الملكي
        2 -> Color(0xFF8D6E63) // الذهبي الدافئ
        3 -> Color(0xFF5D4037) // الورقي الكلاسيكي
        else -> Color(0xFF1B5E20) // الأخضر الزمردي
    }
    val secondaryColor = when (themeIndex) {
        1 -> Color(0xFF1976D2)
        2 -> Color(0xFFA1887F)
        3 -> Color(0xFF795548)
        else -> Color(0xFF2E7D32)
    }

    return if (isDark) {
        darkColorScheme(
            primary = primaryColor.copy(alpha = 0.85f),
            secondary = secondaryColor,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2A2A2A)
        )
    } else {
        val bg = if (themeIndex == 3) Color(0xFFFDF6E3) else Color(0xFFF5F7F5)
        val surf = if (themeIndex == 3) Color(0xFFFAF0D7) else Color.White
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = bg,
            surface = surf,
            surfaceVariant = Color(0xFFE8EFE8)
        )
    }
}
