@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ============================================================
// الألوان والخطوط والهوية البصرية
// ============================================================
val RoyalForest = Color(0xFF052920)
val EmeraldVibrant = Color(0xFF0C5242)
val TurquoiseAccent = Color(0xFF147A67)
val GoldWarm = Color(0xFFD9AE43)
val GoldShine = Color(0xFFF7D97E)
val GoldSoftBg = Color(0xFFFCF6E3)
val PearlBg = Color(0xFFF6F4EC)
val CardWhite = Color(0xFFFFFFFF)
val InkDark = Color(0xFF0F231D)
val InkMuted = Color(0xFF566E65)
val TealStart = Color(0xFF095256)
val TealEnd = Color(0xFF147E83)

@Immutable
data class Palette(val bg: Color, val card: Color, val ink: Color, val muted: Color, val border: Color, val dark: Boolean)

val LightPalette = Palette(PearlBg, CardWhite, InkDark, InkMuted, GoldWarm.copy(alpha = 0.55f), false)
val DarkPalette = Palette(Color(0xFF0A1411), Color(0xFF13221D), Color(0xFFEFEBDD), Color(0xFF9BB3A9), GoldWarm.copy(alpha = 0.4f), true)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }
val LocalFx = staticCompositionLocalOf<Fx> { error("Fx not provided") }

val QuranTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp)
)

class Fx(private val view: View, private val ctx: Context, private val st: AppState) {
    fun click() { if (st.soundOn) view.playSoundEffect(SoundEffectConstants.CLICK) }

    @Suppress("DEPRECATION")
    fun vibrate(ms: Long = 35L) {
        if (!st.vibOn) return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
                else v.vibrate(ms)
            }
        }
    }
}

@Composable
fun QuranTheme(dark: Boolean, content: @Composable () -> Unit) {
    val pal = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides pal, LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = if (dark) darkColorScheme(primary = GoldShine, secondary = GoldWarm, background = pal.bg, surface = pal.card, onSurface = pal.ink)
            else lightColorScheme(primary = EmeraldVibrant, secondary = GoldWarm, background = PearlBg, surface = CardWhite, onSurface = InkDark),
            typography = QuranTypography,
            content = content
        )
    }
}

// ============================================================
// نظام التنقل (Nav & Screen)
// ============================================================
enum class Screen { Home, Reader, Index, Audio, Tasbih, Favorites, Prayer, Hadith, Hifz, Settings }

class Nav(val stack: SnapshotStateList<Screen>, private val st: AppState) {
    var tasbihTab by mutableIntStateOf(0)
    val current: Screen get() = stack.lastOrNull() ?: Screen.Home

    fun push(s: Screen) { if (current != s) stack.add(s) }

    fun go(s: Screen) {
        stack.clear()
        stack.add(Screen.Home)
        if (s != Screen.Home) stack.add(s)
    }

    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }

    fun openPage(page: Int) {
        st.lastPage = page.coerceIn(1, 604)
        push(Screen.Reader)
    }
}

// ============================================================
// المكونات المشتركة وشريط التنقل السفلي
// ============================================================
fun shareText(ctx: Context, text: String, title: String = "مشاركة") {
    runCatching {
        val i = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
        }, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun QCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    onClick: (() -> Unit)? = null,
    color: Color? = null,
    borderColor: Color? = null,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val c = color ?: p.card
    val b = BorderStroke(1.dp, borderColor ?: p.border)
    if (onClick != null) {
        Surface(onClick = { fx.click(); onClick() }, modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 2.dp, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 2.dp, content = content)
    }
}

@Composable
fun ScreenHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    val fx = LocalFx.current
    Surface(color = RoyalForest, shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp), shadowElevation = 4.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { fx.click(); onBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Color.White) }
            Text(title, color = GoldSoftBg, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp, start = 4.dp))
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = GoldWarm, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, color = p.muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun NumberBadge(n: Int, size: Int = 34) {
    Surface(shape = CircleShape, color = EmeraldVibrant, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) { Text("$n", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }
    }
}

@Composable
fun SettingToggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = EmeraldVibrant, checkedThumbColor = GoldShine))
        }
    }
}

@Composable
fun StepperRow(title: String, display: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.ink, modifier = Modifier.weight(1f))
            IconButton(onClick = { fx.click(); onMinus() }) { Icon(Icons.Default.Remove, "إنقاص", tint = TurquoiseAccent) }
            Text(display, fontWeight = FontWeight.ExtraBold, color = p.ink, modifier = Modifier.widthIn(min = 56.dp), textAlign = TextAlign.Center)
            IconButton(onClick = { fx.click(); onPlus() }) { Icon(Icons.Default.Add, "زيادة", tint = TurquoiseAccent) }
        }
    }
}

@Composable
fun IslamicPattern(modifier: Modifier, color: Color = GoldShine.copy(alpha = 0.08f)) {
    Canvas(modifier) {
        val step = 44.dp.toPx()
        val r = step * 0.42f
        val cols = (size.width / step).toInt() + 1
        val rows = (size.height / step).toInt() + 1
        val stroke = Stroke(1.dp.toPx())
        for (i in 0..cols) for (j in 0..rows) {
            val c = Offset(i * step, j * step)
            drawRect(color, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = stroke)
            rotate(45f, c) { drawRect(color, topLeft = Offset(c.x - r, c.y - r), size = Size(2 * r, 2 * r), style = stroke) }
        }
    }
}

@Composable
fun AppBottomBar(nav: Nav, onFollowClick: () -> Unit) {
    val fx = LocalFx.current
    val ctx = LocalContext.current
    Surface(
        color = RoyalForest,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        border = BorderStroke(1.3.dp, GoldWarm),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavSideItem("تابعنا", Icons.Default.Public, Color(0xFF38BDF8)) {
                fx.click(); onFollowClick()
            }

            val isHome = nav.current == Screen.Home
            Surface(
                onClick = { fx.click(); nav.go(Screen.Home) },
                shape = RoundedCornerShape(22.dp),
                color = if (isHome) TurquoiseAccent else EmeraldVibrant,
                border = BorderStroke(1.8.dp, GoldShine),
                shadowElevation = 6.dp,
                modifier = Modifier.height(54.dp).width(120.dp)
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                    Icon(Icons.Default.Home, "الرئيسية", tint = GoldShine, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("الرئيسية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            BottomNavSideItem("شارك التطبيق", Icons.Default.Share, Color(0xFF34D399)) {
                fx.click()
                shareText(ctx, "تطبيق القرآن الكريم الشامل - مصحف وتلاوات ومسبحة ومواقيت الصلاة", "مشاركة التطبيق")
            }
        }
    }
}

@Composable
private fun BottomNavSideItem(label: String, icon: ImageVector, accentColor: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = accentColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GoldSoftBg)
    }
}

@Composable
fun MiniPlayer(ps: PlayerState, onOpen: () -> Unit) {
    val ctx = LocalContext.current
    val fx = LocalFx.current
    val s = QuranData.surah(ps.surah) ?: return
    val reciter = QuranData.reciters.firstOrNull { it.id == ps.reciterId }
    Surface(
        color = EmeraldVibrant,
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        modifier = Modifier.fillMaxWidth().clickable { fx.click(); onOpen() }
    ) {
        Column {
            LinearProgressIndicator(
                progress = { if (ps.durationMs > 0) (ps.positionMs / ps.durationMs.toFloat()).coerceIn(0f, 1f) else 0f },
                color = GoldShine, trackColor = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth().height(2.dp)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.GraphicEq, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("سورة ${s.name}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (ps.isLoading) "جارٍ التحميل..." else reciter?.name ?: "", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                if (ps.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = GoldShine, strokeWidth = 2.dp)
                } else {
                    IconButton(onClick = { fx.click(); PlayerService.toggle(ctx) }, modifier = Modifier.size(36.dp)) {
                        Icon(if (ps.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "تشغيل/إيقاف", tint = GoldShine, modifier = Modifier.size(28.dp))
                    }
                }
                IconButton(onClick = { fx.click(); PlayerService.stop(ctx) }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, "إغلاق", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun FollowUsDialog(onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val fx = LocalFx.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = p.card,
        title = { Text("تابعنا عبر المنصات الرسمية", fontWeight = FontWeight.ExtraBold, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(onClick = { fx.click(); onDismiss(); openUrl(ctx, QuranData.YOUTUBE_URL) }, shape = RoundedCornerShape(18.dp), color = Color(0xFFFF0000), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("قناتنا على يوتيوب", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
                Surface(onClick = { fx.click(); onDismiss(); openUrl(ctx, QuranData.TELEGRAM_URL) }, shape = RoundedCornerShape(18.dp), color = Color(0xFF0088CC), modifier = Modifier.fillMaxWidth().height(46.dp)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("قناتنا على تلجرام", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) } }
    )
}

// ============================================================
// نقطة دخول التطبيق (MainActivity & AppRoot)
// ============================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifs.ensureChannels(this)
        Reminders.rescheduleAll(this)
        setContent { QuranApp() }
    }
}

@Composable
fun QuranApp() {
    val ctx = LocalContext.current
    val view = LocalView.current
    val st = remember { AppState(ctx.applicationContext) }
    val fx = remember { Fx(view, ctx, st) }
    QuranTheme(dark = st.nightMode) {
        CompositionLocalProvider(LocalApp provides st, LocalFx provides fx) { AppRoot() }
    }
}

@Composable
private fun AppRoot() {
    val st = LocalApp.current
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val view = LocalView.current
    val stack = remember { mutableStateListOf(Screen.Home) }
    val nav = remember { Nav(stack, st) }
    val ps by PlayerService.state.collectAsState()
    var showFollowDialog by remember { mutableStateOf(false) }

    SideEffect {
        val window = (ctx as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
    }

    // بوابة الصلاحيات التلقائية: تطلب الموقع، الإشعارات، المنبه الدقيق، واستثناء البطارية عند أول تشغيل
    StartupPermissionsGate()

    BackHandler(enabled = stack.size > 1) { nav.back() }

    val showBars = nav.current != Screen.Reader

    Box(Modifier.fillMaxSize().background(RoyalForest)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
            Box(Modifier.weight(1f).fillMaxWidth().background(p.bg)) {
                when (nav.current) {
                    Screen.Home -> HomeScreen(nav)
                    Screen.Reader -> ReaderScreen(nav)
                    Screen.Index -> IndexScreen(nav)
                    Screen.Audio -> AudioScreen(nav)
                    Screen.Tasbih -> TasbihScreen(nav)
                    Screen.Favorites -> FavoritesScreen(nav)
                    Screen.Prayer -> PrayerScreen(nav)
                    Screen.Hadith -> HadithScreen(nav)
                    Screen.Hifz -> HifzScreen(nav)
                    Screen.Settings -> SettingsScreen(nav)
                }
            }
            if (showBars) {
                if (ps.surah > 0 && nav.current != Screen.Audio) MiniPlayer(ps) { nav.push(Screen.Audio) }
                AppBottomBar(nav = nav, onFollowClick = { showFollowDialog = true })
            } else {
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (showFollowDialog) {
        FollowUsDialog(onDismiss = { showFollowDialog = false })
    }
}
