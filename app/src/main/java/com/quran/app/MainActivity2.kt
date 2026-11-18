@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import kotlin.reflect.KProperty
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


// ============================================================
// ui/Theme.kt
// ============================================================
val RoyalForest = Color(0xFF062D24)
val EmeraldVibrant = Color(0xFF0D4D3E)
val TurquoiseAccent = Color(0xFF136F5E)
val GoldWarm = Color(0xFFD9AE43)
val GoldShine = Color(0xFFF5D471)
val GoldSoftBg = Color(0xFFFCF6E3)
val PearlBg = Color(0xFFF7F5EE)
val CardWhite = Color(0xFFFFFFFF)
val InkDark = Color(0xFF10241E)
val InkMuted = Color(0xFF5A7068)
val IndigoStart = Color(0xFF3E357A)
val IndigoEnd = Color(0xFF5D50A8)
val TealStart = Color(0xFF0B5A5E)
val TealEnd = Color(0xFF158287)

@Immutable
data class Palette(val bg: Color, val card: Color, val ink: Color, val muted: Color, val border: Color, val dark: Boolean)

val LightPalette = Palette(PearlBg, CardWhite, InkDark, InkMuted, GoldWarm.copy(alpha = 0.6f), false)
val DarkPalette = Palette(Color(0xFF0B1512), Color(0xFF14231E), Color(0xFFEFEBDD), Color(0xFF9DB3AA), GoldWarm.copy(alpha = 0.4f), true)

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

/** نقر صوتي واهتزاز حسب إعدادات المستخدم. */
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
// ui/Nav.kt
// ============================================================
enum class Screen { Home, Reader, Index, Audio, Tasbih, Favorites, Prayer, Hadith, Stats, Settings }

class Nav(val stack: SnapshotStateList<Screen>, private val st: AppState) {
    /** 0 = المسبحة، 1 = الأذكار */
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
// ui/Components.kt
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
    shape: Shape = RoundedCornerShape(20.dp),
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
        Surface(onClick = { fx.click(); onClick() }, modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 1.dp, content = content)
    } else {
        Surface(modifier = modifier, shape = shape, color = c, border = b, shadowElevation = 1.dp, content = content)
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

/** نقش إسلامي هندسي خفيف للخلفيات. */
@Composable
fun IslamicPattern(modifier: Modifier, color: Color = GoldShine.copy(alpha = 0.07f)) {
    Canvas(modifier) {
        val step = 46.dp.toPx()
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
private fun BarTab(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip14().clickable { fx.click(); onClick() }.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, label, tint = if (selected) GoldShine else GoldSoftBg.copy(alpha = 0.8f), modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) GoldShine else Color.White)
    }
}

private fun Modifier.clip14(): Modifier = this.then(Modifier.clip(RoundedCornerShape(14.dp)))

@Composable
fun AppBottomBar(nav: Nav) {
    val fx = LocalFx.current
    Surface(
        color = RoyalForest,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        border = BorderStroke(1.2.dp, GoldWarm),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarTab("المصحف", Icons.AutoMirrored.Filled.MenuBook, nav.current == Screen.Reader) { nav.go(Screen.Reader) }
            BarTab("السور", Icons.Default.AutoStories, nav.current == Screen.Index) { nav.go(Screen.Index) }
            Surface(
                onClick = { fx.click(); nav.go(Screen.Home) },
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 14.dp, bottomEnd = 14.dp),
                color = if (nav.current == Screen.Home) TurquoiseAccent else EmeraldVibrant,
                border = BorderStroke(1.6.dp, GoldShine),
                modifier = Modifier.size(width = 64.dp, height = 50.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Home, "الرئيسية", tint = GoldShine, modifier = Modifier.size(22.dp))
                    Text("الرئيسية", color = GoldShine, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
            BarTab("المسبحة", Icons.Default.TouchApp, nav.current == Screen.Tasbih) { nav.tasbihTab = 0; nav.go(Screen.Tasbih) }
            BarTab("الاستماع", Icons.Default.Headphones, nav.current == Screen.Audio) { nav.go(Screen.Audio) }
        }
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

// ============================================================
// ui/HomeScreen.kt
// ============================================================
private data class Tile(val title: String, val icon: ImageVector, val colors: List<Color>, val onClick: () -> Unit)

private fun hijriText(): String = runCatching {
    val ic = android.icu.util.IslamicCalendar()
    val months = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    "${ic.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${months[ic.get(android.icu.util.Calendar.MONTH)]} ${ic.get(android.icu.util.Calendar.YEAR)} هـ"
}.getOrDefault("")

private fun greeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 5 -> "مساء الخير"
        h < 12 -> "صباح الخير"
        h < 17 -> "طاب يومك"
        else -> "مساء الخير"
    }
}

@Composable
fun HomeScreen(nav: Nav) {
    val st = LocalApp.current
    val fx = LocalFx.current
    val p = LocalPalette.current
    val ctx = LocalContext.current

    val dayIdx = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val verse = QuranData.dailyVerses[dayIdx % QuranData.dailyVerses.size]
    val hadith = QuranData.hadiths[dayIdx % QuranData.hadiths.size]
    val hijri = remember { hijriText() }

    var query by remember { mutableStateOf("") }
    var showNotif by remember { mutableStateOf(false) }
    var showFollow by remember { mutableStateOf(false) }
    var notifOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    val goal = st.dailyGoal
    val today = st.todayPages()
    val badge = (if (!notifOn) 1 else 0) + (if (today < goal) 1 else 0)
    val page = st.lastPage
    val curSurah = QuranData.surahForPage(page)
    val pct = st.khatmaPercent()

    val results = remember(query) { if (query.isBlank()) emptyList() else QuranData.search(query).take(5) }
    val pageNum = query.trim().toIntOrNull()?.takeIf { it in 1..604 }

    // لوحة خدمات مرتبة كما طلب المستخدم: 3 عناصر في كل صف ثم القبلة.
    val tiles = listOf(
        Tile("قراءة القرآن", Icons.AutoMirrored.Filled.MenuBook, listOf(Color(0xFF087F5B), Color(0xFF16A085))) { nav.openPage(st.lastPage) },
        Tile("استماع القرآن", Icons.Default.Headphones, listOf(Color(0xFF2457A6), Color(0xFF4388D6))) { nav.push(Screen.Audio) },
        Tile("الأذكار", Icons.Default.WbSunny, listOf(Color(0xFFB66A13), Color(0xFFE6A23C))) { nav.tasbihTab = 1; nav.push(Screen.Tasbih) },
        Tile("التسبيح", Icons.Default.Fingerprint, listOf(Color(0xFF7851A9), Color(0xFFAA78D1))) { nav.tasbihTab = 0; nav.push(Screen.Tasbih) },
        Tile("الأحاديث", Icons.Default.AutoStories, listOf(Color(0xFF315A9B), Color(0xFF557BC2))) { nav.push(Screen.Hadith) },
        Tile("مواقيت الصلاة", Icons.Default.AccessTime, listOf(Color(0xFF087E83), Color(0xFF24A6A0))) { nav.push(Screen.Prayer) },
        Tile("اتجاه القبلة", Icons.Default.Explore, listOf(Color(0xFF9A641D), Color(0xFFD49A37))) { nav.push(Screen.Prayer) }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

        // ---------- الترويسة الملكية ----------
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant, TurquoiseAccent)))
            ) {
                IslamicPattern(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        HeaderIconButton(Icons.Default.Notifications, badge, "التنبيهات") { showNotif = true }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("القرآن الكريم", color = GoldSoftBg, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${greeting()} • $hijri", color = GoldShine, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        HeaderIconButton(Icons.Default.Settings, 0, "الإعدادات") { nav.push(Screen.Settings) }
                    }
                    Spacer(Modifier.height(14.dp))
                    NextPrayerChip(nav)
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { fx.click(); nav.openPage(st.lastPage) },
                        shape = RoundedCornerShape(22.dp),
                        color = RoyalForest.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 5.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                                Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تابع القراءة", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("سورة ${curSurah.name}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                                Text("صفحة $page من 604 • الجزء ${QuranData.juzOfPage(page)}", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }
        }

        // ---------- البحث ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), borderColor = GoldWarm) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("ابحث باسم السورة أو رقمها أو رقم الصفحة...", color = p.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TurquoiseAccent) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح", tint = p.muted) } },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (query.isNotBlank()) {
            item {
                QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(6.dp)) {
                        if (pageNum != null) {
                            SearchRow("الذهاب إلى صفحة $pageNum", "المصحف الشريف", null) { query = ""; nav.openPage(pageNum) }
                        }
                        results.forEach { s ->
                            SearchRow("سورة ${s.name}", "${s.type} • ${s.ayahs} آية • ص ${s.page}", s.num) { query = ""; nav.openPage(s.page) }
                        }
                        if (results.isEmpty() && pageNum == null) {
                            Text("لا توجد نتائج مطابقة", color = p.muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }
        }

        // ---------- شبكة الخدمات ----------
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tiles.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t -> QuickTile(t, Modifier.weight(1f)) }
                    }
                }
            }
        }

        // ---------- الورد اليومي ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("وردك اليوم", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        if (st.streak > 0) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFE5732A), modifier = Modifier.size(18.dp))
                            Text(" ${st.streak} يوم متتالي", color = Color(0xFFE5732A), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (today / goal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        color = if (today >= goal) GoldWarm else EmeraldVibrant, trackColor = p.border.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(if (today >= goal) "أتممت وردك اليوم، تقبّل الله منك" else "قرأت $today من $goal صفحات", color = p.muted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        TextButton(onClick = { fx.click(); nav.openPage(st.lastPage) }) { Text("ابدأ", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }

        // ---------- آية اليوم ----------
        item {
            Surface(
                onClick = { fx.click(); nav.openPage(verse.page) },
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.3.dp, GoldWarm),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Box(Modifier.background(Brush.horizontalGradient(listOf(TealStart, TealEnd)))) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("آية اليوم", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(verse.text, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(verse.ref, color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallPill("اقرأ في المصحف", Icons.AutoMirrored.Filled.MenuBook) { nav.openPage(verse.page) }
                            SmallPill("مشاركة", Icons.Default.Share) { shareText(ctx, "${verse.text}\n${verse.ref}") }
                        }
                    }
                }
            }
        }

        // ---------- مواقيت الصلاة ----------
        item { PrayerStrip(nav) }

        // ---------- حديث اليوم ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatQuote, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("حديث اليوم • ${hadith.title}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { fx.click(); shareText(ctx, "${hadith.text}\n${hadith.source}") }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Share, "مشاركة", tint = p.muted, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(hadith.text, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(hadith.source, color = p.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        // ---------- الخدمات السريعة ----------
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), borderColor = GoldWarm) {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    UtilityItem("تابعنا", Icons.Default.Public) { fx.click(); showFollow = true }
                    UtilityItem("الأحاديث", Icons.Default.FormatQuote) { fx.click(); nav.push(Screen.Hadith) }
                    UtilityItem("الإعدادات", Icons.Default.Settings) { fx.click(); nav.push(Screen.Settings) }
                    UtilityItem("شارك التطبيق", Icons.Default.Share) {
                        fx.click(); shareText(ctx, "تطبيق القرآن الكريم الشامل - مصحف وتلاوات ومسبحة ومواقيت الصلاة", "مشاركة التطبيق")
                    }
                }
            }
        }
    }

    if (showFollow) {
        AlertDialog(
            onDismissRequest = { showFollow = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = p.card,
            title = { Text("تابعنا عبر المنصات الرسمية", fontWeight = FontWeight.ExtraBold, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SocialButton("قناتنا على يوتيوب", Icons.Default.PlayArrow, Color(0xFFFF0000)) { showFollow = false; openUrl(ctx, QuranData.YOUTUBE_URL) }
                    SocialButton("قناتنا على تلجرام", Icons.AutoMirrored.Filled.Send, Color(0xFF0088CC)) { showFollow = false; openUrl(ctx, QuranData.TELEGRAM_URL) }
                }
            },
            confirmButton = { TextButton(onClick = { showFollow = false }) { Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) } }
        )
    }

    if (showNotif) {
        AlertDialog(
            onDismissRequest = { showNotif = false },
            shape = RoundedCornerShape(26.dp),
            containerColor = p.card,
            title = { Text("التنبيهات", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (today < goal) Text("• بقي لك ${goal - today} صفحات لإتمام وردك اليوم.", fontWeight = FontWeight.Bold, color = p.ink)
                    else Text("• أتممت وردك اليوم، بارك الله فيك.", fontWeight = FontWeight.Bold, color = p.ink)
                    Text("• قراءة سورة الملك قبل النوم من السنن المأثورة.", fontWeight = FontWeight.Bold, color = p.ink)
                    if (!notifOn) {
                        Text("• إشعارات التطبيق متوقفة، فعّلها لتصلك التذكيرات.", fontWeight = FontWeight.Bold, color = Color(0xFFB71C1C))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                        ) { Text("تفعيل الإشعارات", color = GoldSoftBg, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotif = false; nav.push(Screen.Settings) }) { Text("إعدادات التذكير", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) }
            },
            dismissButton = { TextButton(onClick = { showNotif = false }) { Text("حسناً", color = p.muted, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, badge: Int, desc: String, onClick: () -> Unit) {
    val fx = LocalFx.current
    Box {
        Surface(
            onClick = { fx.click(); onClick() }, shape = CircleShape, color = RoyalForest.copy(alpha = 0.7f),
            border = BorderStroke(1.2.dp, GoldWarm), modifier = Modifier.size(40.dp)
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, desc, tint = GoldSoftBg, modifier = Modifier.size(20.dp)) } }
        if (badge > 0) {
            Surface(shape = CircleShape, color = Color(0xFFD32F2F), modifier = Modifier.size(16.dp).align(Alignment.TopEnd)) {
                Box(contentAlignment = Alignment.Center) { Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun NextPrayerChip(nav: Nav) {
    val st = LocalApp.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val next = remember(st.geo, st.prayerMethod, now / 60_000) { PrayerCalc.nextPrayer(st.geo, st.prayerMethod, now) }
    val tz = remember(st.geo) { PrayerCalc.tzFor(st.geo) }
    Surface(
        onClick = { nav.push(Screen.Prayer) },
        shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, null, tint = GoldShine, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("الصلاة القادمة: ${PrayerCalc.names[next.first]}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Text("${PrayerCalc.fmt(next.second, tz)} • ${st.geo.name}", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(if (next.second > 0) PrayerCalc.countdown(next.second - now) else "--:--:--", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun PrayerStrip(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val times = remember(st.geo, st.prayerMethod) { PrayerCalc.millis(st.geo, st.prayerMethod, 0) }
    val tz = remember(st.geo) { PrayerCalc.tzFor(st.geo) }
    val nextIdx = PrayerCalc.nextPrayer(st.geo, st.prayerMethod, System.currentTimeMillis()).first
    QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(22.dp), onClick = { nav.push(Screen.Prayer) }) {
        Column(Modifier.padding(14.dp)) {
            Text("مواقيت الصلاة • ${st.geo.name}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                PrayerCalc.prayerIdx.forEachIndexed { i, idx ->
                    val isNext = i == nextIdx
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (isNext) GoldShine.copy(alpha = 0.25f) else Color.Transparent).padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(PrayerCalc.names[i], color = if (isNext) TurquoiseAccent else p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(PrayerCalc.fmt(times[idx], tz), color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(title: String, sub: String, num: Int?, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { fx.click(); onClick() }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (num != null) NumberBadge(num, 30) else Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = GoldWarm, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 14.sp)
            Text(sub, color = p.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
    }
}

@Composable
private fun QuickTile(t: Tile, modifier: Modifier) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Column(modifier.clip(RoundedCornerShape(18.dp)).clickable { fx.click(); t.onClick() }.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(62.dp).clip(RoundedCornerShape(21.dp)).background(Brush.linearGradient(t.colors))
                .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(21.dp)),
            contentAlignment = Alignment.Center
        ) { Icon(t.icon, t.title, tint = Color.White, modifier = Modifier.size(28.dp)) }
        Spacer(Modifier.height(6.dp))
        Text(t.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SmallPill(text: String, icon: ImageVector, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(onClick = { fx.click(); onClick() }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.16f), border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.7f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldShine, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

@Composable
private fun UtilityItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, title, tint = GoldWarm, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = p.ink)
    }
}

@Composable
private fun SocialButton(text: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(onClick = { fx.click(); onClick() }, shape = RoundedCornerShape(18.dp), color = color, modifier = Modifier.fillMaxWidth().height(46.dp)) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
    }
}

// ============================================================
// ui/ReaderScreens.kt
