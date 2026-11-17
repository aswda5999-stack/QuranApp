package com.quran.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private val IslamicColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldDark,
    onPrimaryContainer = GoldLight,
    secondary = GoldAccent,
    onSecondary = EmeraldDark,
    background = IvoryBg,
    onBackground = TextDark,
    surface = CardCream,
    onSurface = TextDark
)

private val ArabicAppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(applicationContext)

        enableEdgeToEdge()
        window.statusBarColor = EmeraldDark.toArgb()

        setContent {
            val settings by AppPreferences.settings.collectAsState()

            // التحكم بإبقاء الشاشة مضاءة أثناء التلاوة
            LaunchedEffect(settings.keepScreenOn) {
                if (settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = IslamicColorScheme,
                    typography = ArabicAppTypography
                ) {
                    QuranMainApp()
                }
            }
        }
    }
}

@Composable
fun QuranMainApp() {
    val context = LocalContext.current
    val view = LocalView.current
    var currentRoute by remember { mutableStateOf<AppRoute>(AppRoute.Home) }

    // طلب إذن الإشعارات في أندرويد 13+ ليظهر مشغل الصوت في شريط الإشعارات
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // زر الرجوع في الهاتف يعود للشاشة السابقة أو الرئيسية بسلاسة
    BackHandler(enabled = currentRoute !is AppRoute.Home) {
        AppPreferences.playTouchSound(view)
        currentRoute = when (currentRoute) {
            is AppRoute.MushafPage -> AppRoute.SurahIndex
            is AppRoute.AudioPlayer -> AppRoute.AudioSurahs
            else -> AppRoute.Home
        }
    }

    val showBottomBar = when (currentRoute) {
        is AppRoute.MushafPage,
        is AppRoute.AudioPlayer -> false
        else -> true
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding(),
        containerColor = IvoryBg,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = EmeraldDark,
                    contentColor = GoldLight,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute is AppRoute.Home,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            currentRoute = AppRoute.Home
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                        label = { Text("الرئيسية", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                            unselectedTextColor = Color.White.copy(alpha = 0.75f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute is AppRoute.SurahIndex,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            currentRoute = AppRoute.SurahIndex
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "المصحف") },
                        label = { Text("المصحف", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                            unselectedTextColor = Color.White.copy(alpha = 0.75f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute is AppRoute.AudioSurahs,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            currentRoute = AppRoute.AudioSurahs
                        },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = "المشغل") },
                        label = { Text("المشغل", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                            unselectedTextColor = Color.White.copy(alpha = 0.75f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute is AppRoute.AdhkarTasbih,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            currentRoute = AppRoute.AdhkarTasbih
                        },
                        icon = { Icon(Icons.Default.TouchApp, contentDescription = "الأذكار") },
                        label = { Text("الأذكار", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                            unselectedTextColor = Color.White.copy(alpha = 0.75f)
                        )
                    )

                    NavigationBarItem(
                        selected = currentRoute is AppRoute.Settings,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            currentRoute = AppRoute.Settings
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                        label = { Text("الإعدادات", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = EmeraldDark,
                            selectedTextColor = GoldAccent,
                            indicatorColor = GoldAccent,
                            unselectedIconColor = Color.White.copy(alpha = 0.75f),
                            unselectedTextColor = Color.White.copy(alpha = 0.75f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val route = currentRoute) {
                is AppRoute.Home -> HomeScreen(
                    onNavigate = { target -> currentRoute = target }
                )
                is AppRoute.SurahIndex -> SurahIndexScreen(
                    onBack = { currentRoute = AppRoute.Home },
                    onOpenMushafPage = { page -> currentRoute = AppRoute.MushafPage(page) }
                )
                is AppRoute.MushafPage -> MushafReaderScreen(
                    initialPage = route.initialPage,
                    onBack = { currentRoute = AppRoute.SurahIndex }
                )
                is AppRoute.AudioSurahs -> AudioSurahsScreen(
                    onBack = { currentRoute = AppRoute.Home },
                    onOpenAudioPlayer = { surahNum, reciterId ->
                        currentRoute = AppRoute.AudioPlayer(surahNum, reciterId)
                    }
                )
                is AppRoute.AudioPlayer -> AudioPlayerScreen(
                    initialSurahNumber = route.surahNumber,
                    reciterId = route.reciterId,
                    onBack = { currentRoute = AppRoute.AudioSurahs }
                )
                is AppRoute.Hadith -> HadithScreen(
                    onBack = { currentRoute = AppRoute.Home }
                )
                is AppRoute.AdhkarTasbih -> AdhkarAndTasbihScreen(
                    onBack = { currentRoute = AppRoute.Home }
                )
                is AppRoute.Favorites -> FavoritesScreen(
                    onBack = { currentRoute = AppRoute.Home },
                    onOpenPage = { page -> currentRoute = AppRoute.MushafPage(page) }
                )
                is AppRoute.Settings -> SettingsScreen(
                    onBack = { currentRoute = AppRoute.Home }
                )
            }
        }
    }
}
