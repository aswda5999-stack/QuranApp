package com.quran.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QuranProAppV2()
            }
        }
    }
}

@Composable
fun QuranProAppV2() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var settings by remember { mutableStateOf(settingsManager.loadSettings()) }
    var uthmanicFont by remember { mutableStateOf<FontFamily>(FontFamily.Serif) }

    // تحميل الخط العثماني الأصلي تلقائياً عند التشغيل
    LaunchedEffect(Unit) {
        uthmanicFont = QuranRepository.getUthmanicFontFamily(context)
    }

    // طلب إذن الإشعارات لأندرويد 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (settings.notificationsEnabled) {
            requestNotificationPermissionIfNeeded()
        }
    }

    // إبقاء الشاشة مضاءة
    LaunchedEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val colorScheme = remember(settings.isDarkMode, settings.themeIndex) {
        buildAppColorSchemeV2(settings.isDarkMode, settings.themeIndex)
    }

    var currentTab by remember { mutableIntStateOf(0) }
    var subScreen by remember { mutableStateOf("NONE") }
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
                        uthmanicFont = uthmanicFont,
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
                        uthmanicFont = uthmanicFont,
                        onBack = {
                            settings = settingsManager.loadSettings()
                            subScreen = "SURAH_LIST"
                        }
                    )
                    "PRAYER" -> PrayerGuideScreen(
                        settings = settings,
                        onBack = { subScreen = "NONE" }
                    )
                    else -> {
                        when (currentTab) {
                            0 -> HomeScreen(
                                settings = settings,
                                uthmanicFont = uthmanicFont,
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
                                settings = settings,
                                uthmanicFont = uthmanicFont,
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
                                uthmanicFont = uthmanicFont,
                                settingsManager = settingsManager,
                                onRequestNotificationPermission = { requestNotificationPermissionIfNeeded() },
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

private fun buildAppColorSchemeV2(isDark: Boolean, themeIndex: Int): ColorScheme {
    val primaryColor = when (themeIndex) {
        1 -> Color(0xFF0D47A1) // الكحلي الملكي
        2 -> Color(0xFF8D6E63) // الذهبي الأندلسي
        3 -> Color(0xFF5D4037) // الورقي الكلاسيكي
        4 -> Color(0xFF00695C) // الفيروزي
        5 -> Color(0xFF4A148C) // الأرجواني الهادئ
        else -> Color(0xFF1B5E20) // الأخضر الملكي
    }
    val secondaryColor = when (themeIndex) {
        1 -> Color(0xFF1976D2)
        2 -> Color(0xFFA1887F)
        3 -> Color(0xFF795548)
        4 -> Color(0xFF00897B)
        5 -> Color(0xFF7B1FA2)
        else -> Color(0xFF2E7D32)
    }

    return if (isDark) {
        darkColorScheme(
            primary = primaryColor.copy(alpha = 0.88f),
            secondary = secondaryColor,
            background = Color(0xFF101211),
            surface = Color(0xFF1A1D1B),
            surfaceVariant = Color(0xFF252A27)
        )
    } else {
        val bg = if (themeIndex == 3) Color(0xFFFDF6E3) else Color(0xFFF4F7F5)
        val surf = if (themeIndex == 3) Color(0xFFFAF0D7) else Color.White
        lightColorScheme(
            primary = primaryColor,
            secondary = secondaryColor,
            background = bg,
            surface = surf,
            surfaceVariant = Color(0xFFE6EFE9)
        )
    }
}
