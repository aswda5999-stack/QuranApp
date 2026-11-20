package com.quran.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

object RoyalPalette {
    val EmeraldDeep = Color(0xFF071C15)
    val EmeraldRoyal = Color(0xFF0B3B2B)
    val EmeraldCard = Color(0xFF134937)
    val EmeraldCardDark = Color(0xFF102A21)
    val GoldRoyal = Color(0xFFE5C158)
    val GoldMuted = Color(0xFFB89238)
    val CreamText = Color(0xFFF7F3E6)
}

private val LightRoyalScheme = lightColorScheme(
    primary = RoyalPalette.GoldRoyal,
    onPrimary = RoyalPalette.EmeraldRoyal,
    background = RoyalPalette.EmeraldRoyal,
    onBackground = RoyalPalette.CreamText,
    surface = RoyalPalette.EmeraldCard,
    onSurface = RoyalPalette.CreamText
)

private val DarkRoyalScheme = darkColorScheme(
    primary = RoyalPalette.GoldRoyal,
    onPrimary = RoyalPalette.EmeraldDeep,
    background = RoyalPalette.EmeraldDeep,
    onBackground = RoyalPalette.CreamText,
    surface = RoyalPalette.EmeraldCardDark,
    onSurface = RoyalPalette.CreamText
)

@Composable
fun QuranRoyalTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkRoyalScheme else LightRoyalScheme,
        content = content
    )
}

@Composable
fun IslamicGeometricOverlay(
    modifier: Modifier = Modifier,
    alpha: Float = 0.08f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 110.dp.toPx()
        val radius = step * 0.34f
        val strokeColor = RoyalPalette.GoldRoyal.copy(alpha = alpha)

        var y = 0f
        while (y <= size.height + step) {
            var x = 0f
            while (x <= size.width + step) {
                val starPath = Path()
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45.0))
                    val r = if (i % 2 == 0) radius else radius * 0.58f
                    val px = x + (r * cos(angle)).toFloat()
                    val py = y + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(path = starPath, color = strokeColor, style = Stroke(width = 1.2.dp.toPx()))
                drawCircle(
                    color = strokeColor,
                    radius = radius * 0.25f,
                    center = Offset(x, y),
                    style = Stroke(width = 1.dp.toPx())
                )
                x += step
            }
            y += step
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        VoiceAdhkarScheduler.ensureChannel(this)

        setContent {
            var isDarkTheme by remember { mutableStateOf(false) }
            var currentRoute by remember { mutableStateOf("home") }
            var lastReadPage by remember { mutableIntStateOf(1) }
            var favorites by remember { mutableStateOf(setOf(1, 18, 67)) }
            var globalSearchQuery by remember { mutableStateOf("") }
            var qiblaIsolated by remember { mutableStateOf(false) }

            val verseOfDay = remember {
                "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ" to "سورة الإسراء • الآية 9"
            }

            QuranRoyalTheme(darkTheme = isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentRoute) {
                        "home" -> HomeScreen(
                            lastReadPage = lastReadPage,
                            verseOfDay = verseOfDay,
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onOpenQuranIndex = { currentRoute = "quran" },
                            onContinueLastRead = { currentRoute = "quran_direct" },
                            onOpenAudioReciters = { currentRoute = "audio" },
                            onOpenAdhanAndMuezzin = {
                                qiblaIsolated = false
                                currentRoute = "adhan"
                            },
                            onOpenQibla = {
                                qiblaIsolated = true
                                currentRoute = "qibla"
                            },
                            onOpenAdhkar = { currentRoute = "adhkar" },
                            onOpenSettings = { currentRoute = "settings" },
                            onOpenSectionById = { sectionId ->
                                when (sectionId) {
                                    "bookmarks" -> currentRoute = "quran"
                                    "alerts" -> currentRoute = "settings"
                                    else -> currentRoute = "adhkar"
                                }
                            }
                        )

                        "quran" -> QuranIndexAndReaderScreen(
                            initialPage = lastReadPage,
                            startDirectlyInReader = false,
                            favorites = favorites,
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onToggleFavorite = { page ->
                                favorites = if (favorites.contains(page)) favorites - page else favorites + page
                            },
                            onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                            onPageChanged = { page -> lastReadPage = page },
                            onOpenSettings = { currentRoute = "settings" },
                            onBackHome = { currentRoute = "home" }
                        )

                        "quran_direct" -> QuranIndexAndReaderScreen(
                            initialPage = lastReadPage,
                            startDirectlyInReader = true,
                            favorites = favorites,
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onToggleFavorite = { page ->
                                favorites = if (favorites.contains(page)) favorites - page else favorites + page
                            },
                            onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                            onPageChanged = { page -> lastReadPage = page },
                            onOpenSettings = { currentRoute = "settings" },
                            onBackHome = { currentRoute = "home" }
                        )

                        "audio" -> AudioRecitersScreen(
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onBackHome = { currentRoute = "home" }
                        )

                        "adhan", "qibla" -> AdhanAndQiblaScreen(
                            startOnQibla = (currentRoute == "qibla") || qiblaIsolated,
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onBackHome = { currentRoute = "home" }
                        )

                        "adhkar" -> EssentialAdhkarScreen(
                            isDarkTheme = isDarkTheme,
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onBackHome = { currentRoute = "home" }
                        )

                        "settings" -> SettingsAndVoiceAdhkarScreen(
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { isDarkTheme = it },
                            searchQuery = globalSearchQuery,
                            onSearchQueryChange = { globalSearchQuery = it },
                            onBackHome = { currentRoute = "home" }
                        )
                    }
                }
            }
        }
    }
}
