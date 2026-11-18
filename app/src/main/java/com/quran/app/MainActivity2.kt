@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay

// ============================================================
// ui/Theme.kt
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
// ui/Nav.kt
// ============================================================
enum class Screen { Home, Reader, Index, Audio, Tasbih, Favorites, Prayer, Hadith, Stats, Settings }

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

// ============================================================
// شريط التنقل السفلي الاحترافي (تابعنا | الرئيسية | شارك التطبيق)
// ============================================================
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
            // 1. تابعنا
            BottomNavSideItem(
                label = "تابعنا",
                icon = Icons.Default.Public,
                accentColor = Color(0xFF38BDF8)
            ) {
                fx.click()
                onFollowClick()
            }

            // 2. الرئيسية (زر مركزي ملكي بارز)
            val isHome = nav.current == Screen.Home
            Surface(
                onClick = { fx.click(); nav.go(Screen.Home) },
                shape = RoundedCornerShape(22.dp),
                color = if (isHome) TurquoiseAccent else EmeraldVibrant,
                border = BorderStroke(1.8.dp, GoldShine),
                shadowElevation = 6.dp,
                modifier = Modifier.height(54.dp).width(120.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Home, "الرئيسية", tint = GoldShine, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("الرئيسية", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // 3. شارك التطبيق
            BottomNavSideItem(
                label = "شارك التطبيق",
                icon = Icons.Default.Share,
                accentColor = Color(0xFF34D399)
            ) {
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
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
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

// ============================================================
// ui/HomeScreen.kt (الشاشة الرئيسية المطورة بالكامل)
// ============================================================
private data class FeatureItem(
    val title: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val glowColor: Color,
    val onClick: () -> Unit
)

private data class DateDetails(
    val dayName: String,
    val slashDate: String,
    val gregorianMonth: String,
    val hijriFull: String
)

private fun getFullDateDetails(): DateDetails {
    val cal = Calendar.getInstance()
    val days = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    val gMonths = listOf(
        "كانون الثاني", "شباط", "آذار", "نيسان", "أيار", "حزيران",
        "تموز", "آب", "أيلول", "تشرين الأول", "تشرين الثاني", "كانون الأول"
    )
    val dayName = days[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
    val year = cal.get(Calendar.YEAR)
    val monthIdx = cal.get(Calendar.MONTH)
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val slashDate = String.format(Locale.US, "%d/%d/%d", year, monthIdx + 1, day)
    val gregorianMonth = gMonths[monthIdx.coerceIn(0, 11)]

    val hijriFull = runCatching {
        val ic = android.icu.util.IslamicCalendar()
        val hMonths = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة",
            "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        "${ic.get(android.icu.util.Calendar.DAY_OF_MONTH)} ${hMonths[ic.get(android.icu.util.Calendar.MONTH)]} ${ic.get(android.icu.util.Calendar.YEAR)} هـ"
    }.getOrDefault("")

    return DateDetails(dayName, slashDate, gregorianMonth, hijriFull)
}

private fun dynamicGreeting(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h in 5..11 -> "صباح الخير"
        h in 12..16 -> "طاب يومك"
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
    val dateInfo = remember { getFullDateDetails() }
    val greetText = remember { dynamicGreeting() }

    var query by remember { mutableStateOf("") }
    var showNotif by remember { mutableStateOf(false) }
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

    // 8 ميزات احترافية بألوان زاهية مميزة تُعرض كل 4 أزرار بجانب بعضها
    val features = listOf(
        FeatureItem("قراءة القرآن", Icons.AutoMirrored.Filled.MenuBook, listOf(Color(0xFF059669), Color(0xFF10B981)), Color(0xFF10B981)) {
            nav.openPage(st.lastPage)
        },
        FeatureItem("استماع القرآن", Icons.Default.Headphones, listOf(Color(0xFF2563EB), Color(0xFF38BDF8)), Color(0xFF38BDF8)) {
            nav.push(Screen.Audio)
        },
        FeatureItem("الأذكار", Icons.Default.WbSunny, listOf(Color(0xFFD97706), Color(0xFFFBBF24)), Color(0xFFFBBF24)) {
            nav.tasbihTab = 1; nav.push(Screen.Tasbih)
        },
        FeatureItem("التسبيح", Icons.Default.Fingerprint, listOf(Color(0xFF7C3AED), Color(0xFFA78BFA)), Color(0xFFA78BFA)) {
            nav.tasbihTab = 0; nav.push(Screen.Tasbih)
        },
        FeatureItem("الأحاديث", Icons.Default.AutoStories, listOf(Color(0xFF0284C7), Color(0xFF22D3EE)), Color(0xFF22D3EE)) {
            nav.push(Screen.Hadith)
        },
        FeatureItem("مواقيت الصلاة", Icons.Default.AccessTime, listOf(Color(0xFF0D9488), Color(0xFF2DD4BF)), Color(0xFF2DD4BF)) {
            nav.push(Screen.Prayer)
        },
        FeatureItem("المفضلة", Icons.Default.Favorite, listOf(Color(0xFFE11D48), Color(0xFFFB7185)), Color(0xFFFB7185)) {
            nav.push(Screen.Favorites)
        },
        FeatureItem("الحفظ", Icons.Default.BookmarkAdded, listOf(Color(0xFFB45309), Color(0xFFF59E0B)), Color(0xFFF59E0B)) {
            nav.push(Screen.Stats)
        }
    )

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ============================================================
        // 1. الترويسة الملكية: التحية + التاريخ الميلادي والهجري + الصلاة القادمة
        // ============================================================
        item {
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant, TurquoiseAccent)))
            ) {
                IslamicPattern(Modifier.matchParentSize())
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {

                    // الصف العلوي: الإشعارات + التحية العريضة + الإعدادات
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        HeaderIconButton(Icons.Default.Notifications, badge, "التنبيهات") { showNotif = true }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = greetText,
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            // اليوم + التاريخ بصيغة 2026/5/22
                            Text(
                                text = "اليوم: ${dateInfo.dayName}  •  ${dateInfo.slashDate}",
                                color = GoldShine,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            // الشهر الميلادي + التقويم الهجري
                            Text(
                                text = "${dateInfo.gregorianMonth}  |  ${dateInfo.hijriFull}",
                                color = GoldSoftBg.copy(alpha = 0.92f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HeaderIconButton(Icons.Default.Settings, 0, "الإعدادات") { nav.push(Screen.Settings) }
                    }

                    Spacer(Modifier.height(14.dp))

                    // بطاقة المتبقي للصلاة القادمة + الساعة
                    NextPrayerHeroCard(nav)

                    Spacer(Modifier.height(10.dp))

                    // بطاقة متابعة القراءة المختصرة
                    Surface(
                        onClick = { fx.click(); nav.openPage(st.lastPage) },
                        shape = RoundedCornerShape(20.dp),
                        color = RoyalForest.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.75f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 4.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                                Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تابع القراءة • سورة ${curSurah.name}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                                Text("صفحة $page من 604 • الجزء ${QuranData.juzOfPage(page)}", color = GoldSoftBg.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }

        // ============================================================
        // 2. شريط البحث الذكي
        // ============================================================
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(20.dp), borderColor = GoldWarm) {
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

        // ============================================================
        // 3. شبكة الميزات (4 أزرار بجانب بعضها بأيقونات زاهية احترافية)
        // ============================================================
        item {
            Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.chunked(4).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item ->
                            ProFeatureTile(item, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ============================================================
        // 4. آية اليوم (بالرسم العثماني وتتغير يومياً)
        // ============================================================
        item {
            Surface(
                onClick = { fx.click(); nav.openPage(verse.page) },
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.5.dp, GoldWarm),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) {
                Box(Modifier.background(Brush.linearGradient(listOf(RoyalForest, TealStart, TealEnd)))) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = GoldShine.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.6f))
                        ) {
                            Text(
                                "✦ آية اليوم ✦",
                                color = GoldShine,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        // عرض الآية بالخط العثماني (Serif) بتشكيلها الكامل
                        Text(
                            text = verse.text,
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 40.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(verse.ref, color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SmallPill("اقرأ في المصحف", Icons.AutoMirrored.Filled.MenuBook) { nav.openPage(verse.page) }
                            SmallPill("مشاركة الآية", Icons.Default.Share) { shareText(ctx, "${verse.text}\n${verse.ref}") }
                        }
                    }
                }
            }
        }

        // ============================================================
        // 5. حديث اليوم
        // ============================================================
        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), borderColor = GoldWarm) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(GoldWarm.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FormatQuote, null, tint = GoldWarm, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("حديث اليوم", color = GoldWarm, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                            Text(hadith.title, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { fx.click(); shareText(ctx, "${hadith.text}\n${hadith.source}") }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Share, "مشاركة", tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = hadith.text,
                        color = p.ink,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(hadith.source, color = p.muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
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
            onClick = { fx.click(); onClick() }, shape = CircleShape, color = RoyalForest.copy(alpha = 0.75f),
            border = BorderStroke(1.2.dp, GoldWarm), modifier = Modifier.size(42.dp)
        ) { Box(contentAlignment = Alignment.Center) { Icon(icon, desc, tint = GoldSoftBg, modifier = Modifier.size(21.dp)) } }
        if (badge > 0) {
            Surface(shape = CircleShape, color = Color(0xFFE11D48), modifier = Modifier.size(17.dp).align(Alignment.TopEnd)) {
                Box(contentAlignment = Alignment.Center) { Text("$badge", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold) }
            }
        }
    }
}

@Composable
private fun NextPrayerHeroCard(nav: Nav) {
    val st = LocalApp.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val next = remember(st.geo, st.prayerMethod, now / 60_000) { PrayerCalc.nextPrayer(st.geo, st.prayerMethod, now) }
    val tz = remember(st.geo) { PrayerCalc.tzFor(st.geo) }

    Surface(
        onClick = { nav.push(Screen.Prayer) },
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.2.dp, GoldShine.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(GoldShine.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AccessTimeFilled, null, tint = GoldShine, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "متبقي لصلاة ${PrayerCalc.names[next.first]}",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "موعد الأذان: ${PrayerCalc.fmt(next.second, tz)}  •  ${st.geo.name}",
                    color = GoldSoftBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = RoyalForest.copy(alpha = 0.75f),
                border = BorderStroke(1.dp, GoldWarm)
            ) {
                Text(
                    text = if (next.second > 0) PrayerCalc.countdown(next.second - now) else "--:--:--",
                    color = GoldShine,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ProFeatureTile(item: FeatureItem, modifier: Modifier) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { fx.click(); item.onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = item.glowColor, spotColor = item.glowColor)
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(item.gradient))
                .border(1.3.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.icon, item.title, tint = Color.White, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = p.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
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
private fun SmallPill(text: String, icon: ImageVector, onClick: () -> Unit) {
    val fx = LocalFx.current
    Surface(
        onClick = { fx.click(); onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.75f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldShine, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

@Composable
fun FollowUsDialog(onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = p.card,
        title = { Text("تابعنا عبر المنصات الرسمية", fontWeight = FontWeight.ExtraBold, color = p.ink, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                SocialButton("قناتنا على يوتيوب", Icons.Default.PlayArrow, Color(0xFFFF0000)) { onDismiss(); openUrl(ctx, QuranData.YOUTUBE_URL) }
                SocialButton("قناتنا على تلجرام", Icons.AutoMirrored.Filled.Send, Color(0xFF0088CC)) { onDismiss(); openUrl(ctx, QuranData.TELEGRAM_URL) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) } }
    )
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
