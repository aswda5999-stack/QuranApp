@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
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
// 1. الخط العثماني ولوحة الألوان المعتمدة (أخضر زمردي ملكي + ذهبي + نقش إسلامي)
// ============================================================
val UthmanicFontFamily = FontFamily(
    Font(R.font.uthmanic, FontWeight.Normal)
)

val RoyalForest       = Color(0xFF042419)
val EmeraldDeep       = Color(0xFF073B2A)
val EmeraldVibrant    = Color(0xFF0C5A40)
val TealStart         = Color(0xFF117554)
val TurquoiseAccent   = Color(0xFF0E7490)
val GoldShine         = Color(0xFFF6D365)
val GoldWarm          = Color(0xFFD4A017)
val GoldDeep          = Color(0xFFB6830B)
val GoldSoftBg        = Color(0xFFFFF8E1)
val SelectedHighlight = Color(0xFFFFDF73)
val DangerRed         = Color(0xFFB71C1C)

data class Palette(
    val bg: Color,
    val card: Color,
    val ink: Color,
    val muted: Color,
    val border: Color,
    val paper: Color,
    val mushafInk: Color,
    val selectedBg: Color,
    val selectedInk: Color,
    val selectedBorder: Color,
    val isDark: Boolean
)

val LightPalette = Palette(
    bg = Color(0xFFF1F5F2),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF072B1F),
    muted = Color(0xFF4A635A),
    border = Color(0xFFCDE0D7),
    paper = Color(0xFFFFFCF2),
    mushafInk = Color(0xFF101916),
    selectedBg = Color(0xFF073B2A),
    selectedInk = Color(0xFFF6D365),
    selectedBorder = Color(0xFFD4A017),
    isDark = false
)

val DarkPalette = Palette(
    bg = Color(0xFF041710),
    card = Color(0xFF09281D),
    ink = Color(0xFFF5F8F6),
    muted = Color(0xFFA2BDB2),
    border = Color(0xFF184A38),
    paper = Color(0xFF071E16),
    mushafInk = Color(0xFFF6F1E2),
    selectedBg = Color(0xFFF6D365),
    selectedInk = Color(0xFF042419),
    selectedBorder = Color(0xFFFFFFFF),
    isDark = true
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalApp = staticCompositionLocalOf<AppState> { error("AppState not provided") }
val LocalFx = staticCompositionLocalOf<SoundFx> { error("SoundFx not provided") }

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
// 2. نظام التنقل والشاشات (Screen & Nav)
// ============================================================
enum class Screen {
    Home, Reader, Index, Bookmarks, Audio, Prayer, Tasbih, Adhkar, Hifz, Settings, FollowUs
}

private fun Screen.tabOwner(): Screen = when (this) {
    Screen.Reader, Screen.Bookmarks, Screen.Hifz, Screen.Index -> Screen.Index
    Screen.Tasbih, Screen.Adhkar -> Screen.Adhkar
    Screen.Settings, Screen.Home -> Screen.Home
    Screen.FollowUs -> Screen.FollowUs
    else -> this
}

class Nav {
    var current by mutableStateOf(Screen.Home)
    private val stack = mutableStateListOf<Screen>()

    var direction by mutableIntStateOf(0)
        private set

    var readerPage by mutableIntStateOf(1)
    var indexTab by mutableIntStateOf(0)
    var focusIndexSearch by mutableStateOf(false)
    var adhkarTab by mutableIntStateOf(0)
    var settingsSection by mutableIntStateOf(-1) // -1 = القائمة الرئيسية للأقسام
    var openQiblaOnPrayer by mutableStateOf(false)

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

    fun openIndex(tab: Int = 0, focusSearch: Boolean = false) {
        indexTab = tab
        focusIndexSearch = focusSearch
        push(Screen.Index)
    }

    fun openAdhkar(tab: Int) {
        adhkarTab = tab
        push(Screen.Adhkar)
    }

    fun openPrayer(openQibla: Boolean = false) {
        openQiblaOnPrayer = openQibla
        push(Screen.Prayer)
    }

    fun openSettings(section: Int = -1) {
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
// 4. مكونات التصميم المشتركة (شعار القرآن، النقش الإسلامي، الترويسة، الشريط السفلي)
// ============================================================
@Composable
fun IslamicPattern(
    modifier: Modifier = Modifier,
    tint: Color = GoldShine.copy(alpha = 0.09f)
) {
    Canvas(modifier = modifier) {
        val step = 62.dp.toPx()
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
                    val rad = if (i % 2 == 0) r else r * 0.54f
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

/** شعار القرآن الكريم المفتوح فوق الحامل الخشبي (مرسوم بدقة هندسية بالذهبي) */
@Composable
fun HolyQuranEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeMain = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeThin = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)

        // هالة خلفية ناعمة
        drawCircle(
            brush = Brush.radialGradient(listOf(GoldShine.copy(alpha = 0.25f), Color.Transparent)),
            radius = w * 0.48f,
            center = Offset(w * 0.5f, h * 0.48f)
        )

        // الحامل الخشبي المتقاطع (الرحل)
        val standPath = Path().apply {
            moveTo(w * 0.24f, h * 0.88f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.76f, h * 0.88f)
            moveTo(w * 0.32f, h * 0.62f)
            lineTo(w * 0.50f, h * 0.75f)
            lineTo(w * 0.68f, h * 0.62f)
        }
        drawPath(standPath, GoldWarm, style = strokeMain)

        // دفتا المصحف المفتوح (الصفحة اليمنى واليسرى)
        val bookPath = Path().apply {
            // الصفحة اليسرى
            moveTo(w * 0.50f, h * 0.24f)
            cubicTo(w * 0.36f, h * 0.16f, w * 0.20f, h * 0.18f, w * 0.12f, h * 0.24f)
            lineTo(w * 0.12f, h * 0.60f)
            cubicTo(w * 0.22f, h * 0.54f, w * 0.36f, h * 0.55f, w * 0.50f, h * 0.64f)
            // الصفحة اليمنى
            cubicTo(w * 0.64f, h * 0.55f, w * 0.78f, h * 0.54f, w * 0.88f, h * 0.60f)
            lineTo(w * 0.88f, h * 0.24f)
            cubicTo(w * 0.80f, h * 0.18f, w * 0.64f, h * 0.16f, w * 0.50f, h * 0.24f)
            // خط المنتصف
            moveTo(w * 0.50f, h * 0.24f)
            lineTo(w * 0.50f, h * 0.64f)
        }
        drawPath(
            bookPath,
            Brush.linearGradient(listOf(GoldShine.copy(alpha = 0.22f), GoldWarm.copy(alpha = 0.12f)))
        )
        drawPath(bookPath, GoldShine, style = strokeMain)

        // زخرفة سطور داخل الصفحتين
        for (i in 1..3) {
            val dy = h * (0.26f + i * 0.08f)
            drawLine(GoldShine.copy(alpha = 0.7f), Offset(w * 0.20f, dy), Offset(w * 0.42f, dy + 2.dp.toPx()), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
            drawLine(GoldShine.copy(alpha = 0.7f), Offset(w * 0.58f, dy + 2.dp.toPx()), Offset(w * 0.80f, dy), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
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
        border = BorderStroke(1.2.dp, GoldWarm.copy(alpha = 0.55f)),
        shadowElevation = 6.dp
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldDeep, EmeraldVibrant)))
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
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val targetBg = when {
        selected -> p.selectedBg
        color != null -> color
        else -> p.card
    }
    val targetBorder = when {
        selected -> p.selectedBorder
        borderColor != null -> borderColor
        else -> p.border
    }
    val animBg by animateColorAsState(targetBg, label = "qCardBg")
    val animBorder by animateColorAsState(targetBorder, label = "qCardBorder")

    if (onClick != null) {
        Surface(
            onClick = { fx.click(); onClick() },
            shape = shape,
            color = animBg,
            border = BorderStroke(if (selected) 1.6.dp else 1.dp, animBorder),
            shadowElevation = if (selected) 5.dp else 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    } else {
        Surface(
            shape = shape,
            color = animBg,
            border = BorderStroke(if (selected) 1.6.dp else 1.dp, animBorder),
            shadowElevation = if (selected) 5.dp else 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

@Composable
fun NumberBadge(num: Int, sizeDp: Int = 36, highlighted: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(
                if (highlighted) Brush.linearGradient(listOf(GoldShine, GoldWarm))
                else Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant))
            )
            .border(1.dp, GoldWarm.copy(alpha = 0.75f), RoundedCornerShape(11.dp))
    ) {
        Text(
            text = "$num",
            color = if (highlighted) RoyalForest else GoldShine,
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

// ============================================================
// 5. شريط التنقل السفلي العائم الاحترافي (يضم زر "تابعنا")
// ============================================================
@Composable
fun RoyalBottomNavBar(nav: Nav) {
    val fx = LocalFx.current

    val items = listOf(
        Triple(Screen.Home, "الرئيسية", Icons.Rounded.Home),
        Triple(Screen.Index, "المصحف", Icons.AutoMirrored.Rounded.MenuBook),
        Triple(Screen.Audio, "الصوتيات", Icons.Rounded.Headphones),
        Triple(Screen.Adhkar, "الأذكار", Icons.Rounded.AutoAwesome),
        Triple(Screen.FollowUs, "تابعنا", Icons.Rounded.Share)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = RoyalForest,
            border = BorderStroke(1.3.dp, GoldWarm.copy(alpha = 0.7f)),
            shadowElevation = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldDeep, RoyalForest)))) {
                IslamicPattern(Modifier.matchParentSize())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEach { (screen, label, icon) ->
                        val selected = nav.current.tabOwner() == screen
                        val pillBg by animateColorAsState(
                            if (selected) GoldShine else Color.Transparent,
                            label = "navPillBg"
                        )
                        val contentTint by animateColorAsState(
                            if (selected) RoyalForest else GoldSoftBg.copy(alpha = 0.85f),
                            label = "navContentTint"
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(pillBg)
                                .clickable {
                                    fx.click()
                                    nav.switchTab(screen)
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = contentTint,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = contentTint
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 6. شاشة "تابعنا" والتواصل ومشاركة التطبيق (FollowUsScreen)
// ============================================================
@Composable
fun FollowUsScreen(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    fun openUrl(url: String) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.onFailure {
            Toast.makeText(ctx, "تعذر فتح الرابط حالياً", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "تابعنا وشارك الأجر",
            subtitle = "الدالّ على الخير كفاعله",
            onBack = { nav.back() }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = RoyalForest,
                border = BorderStroke(1.4.dp, GoldWarm.copy(alpha = 0.7f)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(Modifier.background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HolyQuranEmblem(Modifier.size(72.dp))
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "القرآن الكريم",
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
                            color = GoldShine
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "ساهم في نشر كتاب الله بمشاركة التطبيق مع من تحب، ومتابعة قنواتنا الرسمية للتحديثات المستمرة.",
                            color = GoldSoftBg,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            FollowCard(
                title = "مشاركة التطبيق مع الأصدقاء",
                subtitle = "انشر التطبيق صدقةً جارية لك ولمن تحب",
                icon = Icons.Rounded.Share,
                accent = GoldWarm
            ) {
                shareText(
                    ctx,
                    "تطبيق القرآن الكريم الشامل: مصحف كامل بدون إنترنت، تلاوات خاشعة، مواقيت الصلاة والأذان، والأذكار اليومية."
                )
            }

            FollowCard(
                title = "قناتنا على تيليجرام",
                subtitle = "تحديثات التطبيق وفوائد قرآنية يومية",
                icon = Icons.Rounded.Send,
                accent = Color(0xFF0088CC)
            ) {
                openUrl("https://t.me/")
            }

            FollowCard(
                title = "قناتنا على يوتيوب",
                subtitle = "تلاوات مختارة وشروحات التطبيق",
                icon = Icons.Rounded.PlayCircleFilled,
                accent = Color(0xFFD32F2F)
            ) {
                openUrl("https://www.youtube.com/")
            }

            FollowCard(
                title = "صفحتنا على واتساب",
                subtitle = "تواصل معنا للاقتراحات والملاحظات",
                icon = Icons.Rounded.Chat,
                accent = Color(0xFF1B8A4B)
            ) {
                openUrl("https://wa.me/")
            }

            FollowCard(
                title = "تقييم التطبيق",
                subtitle = "ادعمنا بتقييم 5 نجوم ليصل التطبيق للمزيد من المسلمين",
                icon = Icons.Rounded.StarRate,
                accent = GoldDeep
            ) {
                fx.click()
                Toast.makeText(ctx, "جزاكم الله خيراً على دعمكم وتقييمكم ✓", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun FollowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(20.dp), onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.15f))
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = p.muted)
            }
            Icon(Icons.Rounded.ChevronLeft, null, tint = GoldWarm)
        }
    }
}

// ============================================================
// 7. نقطة الدخول الرئيسية للتطبيق (MainActivity)
// ============================================================
class MainActivity : ComponentActivity() {

    private var openScreenExtra by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            LaunchedEffect(Unit) {
                AdhanEngine.scheduleAll(ctx, appState)
                LocationHelper.refreshIfNeeded(ctx, appState)
            }

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
                            AnimatedContent(
                                targetState = nav.current,
                                transitionSpec = {
                                    when {
                                        nav.direction > 0 ->
                                            (slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300))) togetherWith
                                                (slideOutHorizontally(tween(300)) { it / 6 } + fadeOut(tween(180)))
                                        nav.direction < 0 ->
                                            (slideInHorizontally(tween(300)) { it / 6 } + fadeIn(tween(300))) togetherWith
                                                (slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(180)))
                                        else -> fadeIn(tween(240)) togetherWith fadeOut(tween(150))
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
                                        Screen.FollowUs -> FollowUsScreen(nav)
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
