package com.quran.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object RoyalPalette {
    val CreamBackground = Color(0xFFFBF7EC)
    val CreamSurface = Color(0xFFF3ECD8)
    val EmeraldDeep = Color(0xFF071C15)
    val EmeraldRoyal = Color(0xFF0B3B2B)
    val EmeraldCard = Color(0xFF0F4634)
    val EmeraldCardDark = Color(0xFF102A21)
    val GoldRoyal = Color(0xFFD4AF37)
    val GoldHighlight = Color(0xFFE5C158)
    val GoldMuted = Color(0xFFB89238)
    val CreamText = Color(0xFFFBF7EC)
    val InkEmerald = Color(0xFF0B3B2B)
    val InkMuted = Color(0xFF566B62)
}

private val LightRoyalScheme = lightColorScheme(
    primary = RoyalPalette.EmeraldRoyal,
    onPrimary = RoyalPalette.CreamText,
    secondary = RoyalPalette.GoldRoyal,
    onSecondary = RoyalPalette.EmeraldDeep,
    background = RoyalPalette.CreamBackground,
    onBackground = RoyalPalette.InkEmerald,
    surface = RoyalPalette.CreamSurface,
    onSurface = RoyalPalette.InkEmerald
)

private val DarkRoyalScheme = darkColorScheme(
    primary = RoyalPalette.GoldHighlight,
    onPrimary = RoyalPalette.EmeraldDeep,
    secondary = RoyalPalette.GoldRoyal,
    onSecondary = RoyalPalette.EmeraldDeep,
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
    alpha: Float = 0.06f,
    tint: Color = RoyalPalette.GoldRoyal
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = 112.dp.toPx()
        val outerR = step * 0.34f
        val innerR = outerR * 0.48f
        val strokeColor = tint.copy(alpha = alpha)

        var y = 0f
        while (y <= size.height + step) {
            var x = 0f
            while (x <= size.width + step) {
                val center = Offset(x, y)
                val starPath = Path()
                val points = 16
                for (i in 0 until points) {
                    val r = if (i % 2 == 0) outerR else innerR
                    val angle = i * (PI / 8.0)
                    val px = center.x + (r * cos(angle)).toFloat()
                    val py = center.y + (r * sin(angle)).toFloat()
                    if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
                }
                starPath.close()
                drawPath(
                    path = starPath,
                    color = strokeColor,
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = strokeColor.copy(alpha = alpha * 0.75f),
                    radius = outerR * 0.72f,
                    center = center,
                    style = Stroke(width = 0.9.dp.toPx())
                )
                x += step
            }
            y += step
        }
    }
}

enum class BottomNavTab(val route: String, val label: String) {
    HOME("home", "الرئيسية"),
    QURAN("quran_reader", "المصحف"),
    AUDIO("audio", "الصوتيات"),
    FOLLOW("follow", "تابعنا"),
    SHARE("share", "مشاركة")
}

@Composable
fun GlobalBottomNavigationBar(
    currentRoute: String,
    isDarkTheme: Boolean,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val barBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.CreamBackground
    val borderCol = if (isDarkTheme) RoyalPalette.GoldRoyal.copy(alpha = 0.45f) else RoyalPalette.GoldRoyal.copy(alpha = 0.65f)
    val activePillBg = if (isDarkTheme) RoyalPalette.GoldRoyal else RoyalPalette.EmeraldRoyal
    val activeContent = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamText
    val inactiveContent = if (isDarkTheme) RoyalPalette.CreamText.copy(alpha = 0.78f) else RoyalPalette.InkEmerald.copy(alpha = 0.78f)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = barBg,
        shadowElevation = 8.dp,
        border = BorderStroke(1.2.dp, borderCol)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                val isSelected = when (tab) {
                    BottomNavTab.HOME -> currentRoute == "home"
                    BottomNavTab.QURAN -> currentRoute == "quran_index" || currentRoute == "quran_reader"
                    BottomNavTab.AUDIO -> currentRoute == "audio"
                    BottomNavTab.FOLLOW -> false
                    BottomNavTab.SHARE -> false
                }

                val itemBg = if (isSelected) activePillBg else Color.Transparent
                val fg = if (isSelected) activeContent else inactiveContent

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(itemBg)
                        .clickable { onSelectRoute(tab.route) }
                        .padding(vertical = 7.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    NavTabGlyph(tab = tab, tint = fg, isSelected = isSelected)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = tab.label,
                        color = fg,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun NavTabGlyph(tab: BottomNavTab, tint: Color, isSelected: Boolean) {
    val sizeDp = if (isSelected) 21.dp else 20.dp
    Canvas(modifier = Modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = if (isSelected) 2.2f else 1.8f, cap = StrokeCap.Round)
        when (tab) {
            BottomNavTab.HOME -> {
                val roof = Path().apply {
                    moveTo(w * 0.14f, h * 0.48f)
                    lineTo(w * 0.5f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.48f)
                }
                drawPath(roof, color = tint, style = stroke)
                drawRect(
                    color = tint,
                    topLeft = Offset(w * 0.24f, h * 0.46f),
                    size = androidx.compose.ui.geometry.Size(w * 0.52f, h * 0.38f),
                    style = stroke
                )
            }
            BottomNavTab.QURAN -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.18f),
                    size = androidx.compose.ui.geometry.Size(w * 0.68f, h * 0.66f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.2f), Offset(w * 0.5f, h * 0.82f), strokeWidth = 1.8f)
            }
            BottomNavTab.AUDIO -> {
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.2f, h * 0.22f),
                    size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.55f),
                    style = stroke
                )
                drawRoundRect(tint, Offset(w * 0.14f, h * 0.5f), androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                drawRoundRect(tint, Offset(w * 0.68f, h * 0.5f), androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.32f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
            }
            BottomNavTab.FOLLOW -> {
                val bell = Path().apply {
                    moveTo(w * 0.24f, h * 0.68f)
                    lineTo(w * 0.76f, h * 0.68f)
                    lineTo(w * 0.68f, h * 0.54f)
                    lineTo(w * 0.68f, h * 0.38f)
                    cubicTo(w * 0.68f, h * 0.22f, w * 0.32f, h * 0.22f, w * 0.32f, h * 0.38f)
                    lineTo(w * 0.32f, h * 0.54f)
                    close()
                }
                drawPath(bell, color = tint, style = stroke)
                drawCircle(color = tint, radius = w * 0.08f, center = Offset(w * 0.5f, h * 0.78f))
            }
            BottomNavTab.SHARE -> {
                val pTop = Offset(w * 0.72f, h * 0.24f)
                val pMid = Offset(w * 0.26f, h * 0.50f)
                val pBot = Offset(w * 0.72f, h * 0.76f)
                drawLine(tint, pMid, pTop, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(tint, pMid, pBot, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawCircle(color = tint, radius = w * 0.11f, center = pTop)
                drawCircle(color = tint, radius = w * 0.11f, center = pMid)
                drawCircle(color = tint, radius = w * 0.11f, center = pBot)
            }
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
            var startReaderDirectly by remember { mutableStateOf(false) }
            var favorites by remember { mutableStateOf(setOf(1, 18, 67, 112)) }
            val context = LocalContext.current
            var globalSearchQuery by remember { mutableStateOf("") }
            var qiblaIsolated by remember { mutableStateOf(false) }
            var showFollowDialog by remember { mutableStateOf(false) }

            val dailyVerses = remember {
                listOf(
                    "إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ" to "سورة الإسراء • الآية ٩",
                    "فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ" to "سورة البقرة • الآية ١٥٢",
                    "وَلَذِكْرُ اللَّهِ أَكْبَرُ ۗ وَاللَّهُ يَعْلَمُ مَا تَصْنَعُونَ" to "سورة العنكبوت • الآية ٤٥",
                    "أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ" to "سورة الرعد • الآية ٢٨"
                )
            }
            val verseOfDay = remember(dailyVerses) {
                val idx = (System.currentTimeMillis() / 86_400_000L).toInt().mod(dailyVerses.size)
                dailyVerses[idx]
            }

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QuranRoyalTheme(darkTheme = isDarkTheme) {
                    val rootBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(rootBg)
                    ) {
                        IslamicGeometricOverlay(
                            alpha = if (isDarkTheme) 0.05f else 0.06f,
                            tint = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
                        )

                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding(),
                            containerColor = Color.Transparent,
                            bottomBar = {
                                GlobalBottomNavigationBar(
                                    currentRoute = currentRoute,
                                    isDarkTheme = isDarkTheme,
                                    onSelectRoute = { target ->
                                        when (target) {
                                            "home" -> {
                                                qiblaIsolated = false
                                                currentRoute = "home"
                                            }
                                            "quran_reader" -> {
                                                startReaderDirectly = true
                                                currentRoute = "quran_reader"
                                            }
                                            "audio" -> currentRoute = "audio"
                                            "follow" -> {
                                                showFollowDialog = true
                                            }
                                            "share" -> {
                                                val shareText = "تطبيق القرآن الكريم — المصحف الشريف، التلاوات الصوتية، الأذكار، ومواقيت الصلاة"
                                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                                    type = "text/plain"
                                                }
                                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة التطبيق"))
                                            }
                                        }
                                    }
                                )
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentRoute) {
                                    "home" -> HomeScreen(
                                        lastReadPage = lastReadPage,
                                        verseOfDay = verseOfDay,
                                        isDarkTheme = isDarkTheme,
                                        searchQuery = globalSearchQuery,
                                        onSearchQueryChange = { globalSearchQuery = it },
                                        onOpenQuranIndex = {
                                            startReaderDirectly = false
                                            currentRoute = "quran_index"
                                        },
                                        onContinueLastRead = {
                                            startReaderDirectly = true
                                            currentRoute = "quran_reader"
                                        },
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
                                        onOpenFavorites = {
                                            startReaderDirectly = false
                                            currentRoute = "quran_index"
                                        },
                                        onOpenSettings = { currentRoute = "settings" },
                                        onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                                        onSelectPageFromSearch = { page ->
                                            lastReadPage = page.coerceIn(1, 604)
                                            startReaderDirectly = true
                                            currentRoute = "quran_reader"
                                        }
                                    )

                                    "quran_index", "quran_reader" -> QuranIndexAndReaderScreen(
                                        initialPage = lastReadPage,
                                        startDirectlyInReader = startReaderDirectly || currentRoute == "quran_reader",
                                        favorites = favorites,
                                        isDarkTheme = isDarkTheme,
                                        searchQuery = globalSearchQuery,
                                        onSearchQueryChange = { globalSearchQuery = it },
                                        onToggleFavorite = { page ->
                                            favorites = if (favorites.contains(page)) favorites - page else favorites + page
                                        },
                                        onToggleDarkTheme = { isDarkTheme = !isDarkTheme },
                                        onPageChanged = { newPage -> lastReadPage = newPage.coerceIn(1, 604) },
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
                                        startOnQibla = qiblaIsolated || currentRoute == "qibla",
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
                        if (showFollowDialog) {
                            AlertDialog(
                                onDismissRequest = { showFollowDialog = false },
                                containerColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.CreamBackground,
                                title = {
                                    Text(
                                        text = "تابعنا",
                                        color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(
                                            text = "يسعدنا تواصلكم ومتابعتكم لآخر التحديثات والإصدارات:",
                                            color = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald,
                                            fontSize = 14.sp
                                        )
                                        Button(
                                            onClick = {
                                                showFollowDialog = false
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/")))
                                                } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RoyalPalette.EmeraldRoyal,
                                                contentColor = RoyalPalette.CreamText
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("قناتنا على تيليجرام", fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                showFollowDialog = false
                                                try {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://youtube.com/")))
                                                } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            border = BorderStroke(1.2.dp, RoyalPalette.GoldRoyal),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "قناتنا على يوتيوب",
                                                color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showFollowDialog = false }) {
                                        Text(
                                            text = "إغلاق",
                                            color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
