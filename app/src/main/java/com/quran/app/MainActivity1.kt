@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// 1. الخط العثماني ولوحة الألوان الملكية
// ============================================================
// ملاحظة مهمة: نعرّف الخط بوزن عادي واحد فقط، فيقوم النظام بتغليظه تلقائياً (Synthetic Bold)
// عند طلب FontWeight.Bold أو ExtraBold. كان تعريفه سابقاً بثلاثة أوزان لنفس الملف يمنع التغليظ.
val UthmanicFontFamily = FontFamily(
    Font(R.font.uthmanic, FontWeight.Normal)
)

val RoyalForest     = Color(0xFF062C20)
val EmeraldVibrant  = Color(0xFF0B5D42)
val TealStart       = Color(0xFF107856)
val TurquoiseAccent = Color(0xFF0E7490)
val GoldShine       = Color(0xFFF6D365)
val GoldWarm        = Color(0xFFD4A017)
val GoldSoftBg      = Color(0xFFFFF8E1)
val DangerRed       = Color(0xFFB71C1C)

data class Palette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val paper: Color,
    val mushafInk: Color,
    val isDark: Boolean
)

val LightPalette = Palette(
    bg = Color(0xFFF4F6F4),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF0F241D),
    muted = Color(0xFF52665E),
    border = Color(0xFFD6E0DC),
    paper = Color(0xFFFFFCF2),
    mushafInk = Color(0xFF121A17),
    isDark = false
)

val DarkPalette = Palette(
    bg = Color(0xFF07130F),
    card = Color(0xFF0E211B),
    ink = Color(0xFFF1F6F3),
    muted = Color(0xFF94ABA2),
    border = Color(0xFF1C382E),
    paper = Color(0xFF0B1914),
    mushafInk = Color(0xFFF4EFE0),
    isDark = true
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }
val LocalFx = staticCompositionLocalOf<SoundFx> { error("SoundFx not provided") }

// خطوط عريضة في كل التطبيق
private val BoldTypography = Typography().let { t ->
    t.copy(
        displayLarge = t.displayLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.Bold),
        bodyLarge = t.bodyLarge.copy(fontWeight = FontWeight.Bold),
        bodyMedium = t.bodyMedium.copy(fontWeight = FontWeight.Bold),
        bodySmall = t.bodySmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        labelMedium = t.labelMedium.copy(fontWeight = FontWeight.Bold),
        labelSmall = t.labelSmall.copy(fontWeight = FontWeight.Bold)
    )
}

// ============================================================
// 2. نظام التنقل والشاشات (Screen & Nav) مع اتجاه الحركة
// ============================================================
enum class Screen {
    Home, Reader, Index, Bookmarks, Audio, Prayer, Tasbih, Adhkar, Hifz, Settings
}

/** التبويب السفلي الذي تتبع له كل شاشة (لتمييزه في الشريط السفلي) */
private fun Screen.tabOwner(): Screen = when (this) {
    Screen.Reader, Screen.Bookmarks, Screen.Hifz, Screen.Index -> Screen.Index
    Screen.Tasbih, Screen.Adhkar -> Screen.Adhkar
    Screen.Settings, Screen.Home -> Screen.Home
    else -> this
}

class Nav {
    var current by mutableStateOf(Screen.Home)
    private val stack = mutableStateListOf<Screen>()

    /** 1 = تقدّم، -1 = رجوع، 0 = تبديل تبويب (يستخدم لاختيار حركة الانتقال) */
    var direction by mutableIntStateOf(0)
        private set

    var readerPage by mutableIntStateOf(1)
    var indexTab by mutableIntStateOf(0)
    var tasbihTab by mutableIntStateOf(0)
    var adhkarTab by mutableIntStateOf(0)
    var settingsSection by mutableIntStateOf(0)

    fun push(screen: Screen) {
        if (current != screen) {
            stack.add(current)
            direction = 1
            current = screen
        }
    }

    fun switchTab(screen: Screen) {
        if (current != screen) {
            stack.clear()
            if (screen != Screen.Home) stack.add(Screen.Home)
            direction = 0
            current = screen
        }
    }

    fun openPage(page: Int) {
        readerPage = page.coerceIn(1, 604)
        push(Screen.Reader)
    }

    fun openAdhkar(tab: Int) {
        adhkarTab = tab
        push(Screen.Adhkar)
    }

    fun openSettings(section: Int = 0) {
        settingsSection = section
        push(Screen.Settings)
    }

    fun back(): Boolean {
        direction = -1
        return if (stack.isNotEmpty()) {
            current = stack.removeAt(stack.lastIndex)
            true
        } else if (current != Screen.Home) {
            current = Screen.Home
            true
        } else {
            false
        }
    }
}

// ============================================================
// 3. المؤثرات الصوتية والحسية وإشعارات النظام
// ============================================================
class SoundFx(private val ctx: Context, private val st: AppState) {
    private val tone by lazy {
        runCatching { ToneGenerator(AudioManager.STREAM_SYSTEM, 42) }.getOrNull()
    }

    fun click() {
        if (st.soundOn) {
            runCatching { tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 28) }
        }
    }

    fun vibrate(ms: Long = 30L) {
        if (!st.vibOn) return
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(ms)
            }
        }
    }
}

object Notifs {
    const val CH_PLAYER = "quran_player_channel"
    const val CH_ADHAN = "quran_adhan_alarm_channel"
    const val CH_REMINDER = "quran_reminders_channel"
    const val EXTRA_OPEN = "open_screen"

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val playerCh = NotificationChannel(
                CH_PLAYER,
                "مشغل التلاوات القرآنية",
                NotificationManager.IMPORTANCE_LOW
            )
            val adhanCh = NotificationChannel(
                CH_ADHAN,
                "تنبيهات الأذان ومواقيت الصلاة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableVibration(true)
                setBypassDnd(true)
            }
            val reminderCh = NotificationChannel(
                CH_REMINDER,
                "تذكير الأذكار والورد اليومي",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { enableVibration(true) }
            nm.createNotificationChannel(playerCh)
            nm.createNotificationChannel(adhanCh)
            nm.createNotificationChannel(reminderCh)
        }
    }

    /** openScreen: adhkar_morning | adhkar_evening | wird — لفتح التطبيق على الشاشة المناسبة */
    fun contentIntent(ctx: Context, openScreen: String? = null, requestCode: Int = 0): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (openScreen != null) putExtra(EXTRA_OPEN, openScreen)
        }
        return PendingIntent.getActivity(
            ctx,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}

fun shareText(ctx: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(intent, "مشاركة عبر").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

// ============================================================
// 4. مكونات التصميم المشتركة (Header, Cards, Pattern, BottomNav)
// ============================================================
@Composable
fun IslamicPattern(modifier: Modifier = Modifier, tint: Color = GoldShine.copy(alpha = 0.08f)) {
    Canvas(modifier = modifier) {
        val step = 64.dp.toPx()
        val r = 20.dp.toPx()
        var y = 0f
        while (y < size.height + step) {
            var x = 0f
            while (x < size.width + step) {
                val cx = x + step / 2f
                val cy = y + step / 2f
                val path = Path()
                for (i in 0 until 8) {
                    val angle = i * PI / 4.0
                    val rad = if (i % 2 == 0) r else r * 0.55f
                    val px = cx + (rad * cos(angle)).toFloat()
                    val py = cy + (rad * sin(angle)).toFloat()
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.close()
                drawPath(path, tint, style = Stroke(width = 1.dp.toPx()))
                drawCircle(tint, radius = 2.dp.toPx(), center = Offset(cx, cy))
                x += step
            }
            y += step
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val fx = LocalFx.current
    Surface(
        color = RoyalForest,
        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.5f)),
        shadowElevation = 6.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))
                .statusBarsPadding()
        ) {
            IslamicPattern(Modifier.matchParentSize())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBack != null) {
                    IconButton(onClick = { fx.click(); onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "رجوع",
                            tint = GoldShine
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = GoldSoftBg.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                trailing()
            }
        }
    }
}

@Composable
fun QCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    color: Color? = null,
    borderColor: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val bg = color ?: p.card
    val border = borderColor ?: p.border

    if (onClick != null) {
        Surface(
            onClick = { fx.click(); onClick() },
            shape = shape,
            color = bg,
            border = BorderStroke(1.dp, border),
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = bg,
            border = BorderStroke(1.dp, border),
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun NumberBadge(num: Int, sizeDp: Int = 36) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant)))
            .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
    ) {
        Text(
            text = "$num",
            color = GoldShine,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(GoldWarm)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = p.ink,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun EmptyState(icon: ImageVector, message: String) {
    val p = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = p.muted.copy(alpha = 0.6f), modifier = Modifier.size(54.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = p.muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
fun RoyalBottomNavBar(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current

    val items = listOf(
        Triple(Screen.Home, "الرئيسية", Icons.Default.Home),
        Triple(Screen.Index, "المصحف", Icons.AutoMirrored.Filled.MenuBook),
        Triple(Screen.Audio, "الصوتيات", Icons.Default.Headphones),
        Triple(Screen.Prayer, "الصلاة", Icons.Default.Mosque),
        Triple(Screen.Adhkar, "الأذكار", Icons.Default.AutoAwesome)
    )

    Surface(
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (screen, label, icon) ->
                val selected = nav.current.tabOwner() == screen
                val tint by animateColorAsState(if (selected) EmeraldVibrant else p.muted, label = "navTint")
                val bgTint by animateColorAsState(
                    if (selected) EmeraldVibrant.copy(alpha = 0.14f) else Color.Transparent,
                    label = "navBg"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            fx.click()
                            nav.switchTab(screen)
                        }
                        .background(bgTint)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = tint,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tint
                    )
                }
            }
        }
    }
}

// ============================================================
// 5. نقطة الدخول الرئيسية للتطبيق (MainActivity)
// ============================================================
class MainActivity : ComponentActivity() {

    private var openScreenExtra by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // الترويسات خضراء داكنة دائماً، لذا نجعل أيقونات شريط الحالة فاتحة
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )
        Notifs.ensureChannels(this)
        openScreenExtra = intent?.getStringExtra(Notifs.EXTRA_OPEN)

        setContent {
            val ctx = LocalContext.current
            val appState = remember { AppState(ctx) }
            val soundFx = remember { SoundFx(ctx, appState) }
            val nav = remember { Nav() }

            val palette = remember(appState.nightMode, appState.paperWarmth) {
                if (appState.nightMode) {
                    DarkPalette
                } else {
                    LightPalette.copy(
                        paper = if (appState.paperWarmth) Color(0xFFFFFCF2) else Color(0xFFFFFFFF)
                    )
                }
            }

            LaunchedEffect(appState.keepOn, nav.current) {
                if (appState.keepOn && nav.current == Screen.Reader) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // جدولة الأذان والتذكيرات + تحديث الموقع تلقائياً إن كان الوضع تلقائياً
            LaunchedEffect(Unit) {
                AdhanEngine.scheduleAll(ctx, appState)
                LocationHelper.refreshIfNeeded(ctx, appState)
            }

            // فتح الشاشة المناسبة عند الضغط على إشعار تذكير
            LaunchedEffect(openScreenExtra) {
                when (openScreenExtra) {
                    "adhkar_morning" -> nav.openAdhkar(AdhkarTabs.MORNING)
                    "adhkar_evening" -> nav.openAdhkar(AdhkarTabs.EVENING)
                    "wird" -> nav.openPage(appState.lastPage)
                }
                openScreenExtra = null
            }

            BackHandler(enabled = nav.current != Screen.Home) {
                nav.back()
            }

            CompositionLocalProvider(
                LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalPalette provides palette,
                LocalApp provides appState,
                LocalFx provides soundFx
            ) {
                val colorScheme = if (palette.isDark) {
                    darkColorScheme(
                        primary = EmeraldVibrant,
                        secondary = GoldWarm,
                        background = palette.bg,
                        surface = palette.card
                    )
                } else {
                    lightColorScheme(
                        primary = EmeraldVibrant,
                        secondary = GoldWarm,
                        background = palette.bg,
                        surface = palette.card
                    )
                }

                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = BoldTypography
                ) {
                    val showBottomBar = nav.current != Screen.Reader
                    Scaffold(
                        containerColor = palette.bg,
                        bottomBar = {
                            if (showBottomBar) {
                                RoyalBottomNavBar(nav)
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // انتقال سلس بين الشاشات: انزلاق + تلاشٍ حسب اتجاه التنقل (RTL)
                            AnimatedContent(
                                targetState = nav.current,
                                transitionSpec = {
                                    when {
                                        nav.direction > 0 ->
                                            (slideInHorizontally(tween(320)) { -it / 4 } + fadeIn(tween(320))) togetherWith
                                                (slideOutHorizontally(tween(320)) { it / 6 } + fadeOut(tween(180)))
                                        nav.direction < 0 ->
                                            (slideInHorizontally(tween(320)) { it / 6 } + fadeIn(tween(320))) togetherWith
                                                (slideOutHorizontally(tween(320)) { -it / 4 } + fadeOut(tween(180)))
                                        else -> fadeIn(tween(260)) togetherWith fadeOut(tween(160))
                                    }
                                },
                                label = "screens"
                            ) { screen ->
                                Box(Modifier.fillMaxSize().background(palette.bg)) {
                                    when (screen) {
                                        Screen.Home -> HomeScreen(nav)
                                        Screen.Reader -> ReaderScreen(nav)
                                        Screen.Index -> IndexScreen(nav)
                                        Screen.Bookmarks -> BookmarksScreen(nav)
                                        Screen.Audio -> AudioScreen(nav)
                                        Screen.Prayer -> PrayerScreen(nav)
                                        Screen.Tasbih -> TasbihScreen(nav)
                                        Screen.Adhkar -> AdhkarScreen(nav)
                                        Screen.Hifz -> HifzScreen(nav)
                                        Screen.Settings -> SettingsScreen(nav)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openScreenExtra = intent.getStringExtra(Notifs.EXTRA_OPEN)
    }
}
