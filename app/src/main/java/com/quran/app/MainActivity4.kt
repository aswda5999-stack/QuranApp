@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import java.util.Locale
import kotlinx.coroutines.delay

// ======================= المفضلة =======================
@Composable
fun FavoritesScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val favs = st.favPages.sorted()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الصفحات المحفوظة", onBack = { nav.back() })
        if (favs.isEmpty()) {
            EmptyState(Icons.Default.BookmarkBorder, "لا توجد صفحات محفوظة بعد.\nاضغط على «المفضلة» أثناء القراءة لإضافة الصفحة هنا.")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favs, key = { it }) { pg ->
                    val s = QuranData.surahForPage(pg)
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(pg) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Bookmark, null, tint = GoldWarm)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("صفحة $pg", fontWeight = FontWeight.ExtraBold, color = p.ink)
                                Text("سورة ${s.name} • الجزء ${QuranData.juzOfPage(pg)}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted)
                            }
                            IconButton(onClick = { fx.click(); st.toggleFav(pg) }) { Icon(Icons.Default.DeleteOutline, "إزالة", tint = Color(0xFFB71C1C)) }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        }
    }
}

// ======================= مواقيت الصلاة =======================
@Composable
fun PrayerScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val geo = st.geo
    val method = st.prayerMethod
    val tz = remember(geo) { PrayerCalc.tzFor(geo) }
    val times = remember(geo, method) { PrayerCalc.millis(geo, method, 0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    val next = remember(geo, method, now / 60_000) { PrayerCalc.nextPrayer(geo, method, now) }
    var showCities by remember { mutableStateOf(false) }
    val qibla = remember(geo) { PrayerCalc.qibla(geo.lat, geo.lng) }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val useGps = {
        val l = LocationHelper.lastKnown(ctx)
        if (l != null) st.setLocation("موقعي الحالي", l.latitude, l.longitude)
        else Toast.makeText(ctx, "تعذر تحديد موقعك، فعّل الموقع أو اختر مدينتك يدوياً", Toast.LENGTH_LONG).show()
    }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useGps() else Toast.makeText(ctx, "لم يُمنح إذن الموقع، اختر مدينتك يدوياً", Toast.LENGTH_SHORT).show()
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("مواقيت الصلاة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, modifier = Modifier.fillMaxWidth()) {
                    Box {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الصلاة القادمة", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(PrayerCalc.names[next.first], color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
                            Text(PrayerCalc.fmt(next.second, tz), color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(if (next.second > 0) "بعد ${PrayerCalc.countdown(next.second - now)}" else "--:--:--", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                    }
                }
            }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = GoldWarm)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(geo.name, fontWeight = FontWeight.ExtraBold, color = p.ink)
                                if (geo.isDefault) Text("اختر مدينتك أو حدّد موقعك لدقة أعلى", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    fx.click()
                                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useGps()
                                    else locLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant), modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(16.dp), tint = GoldShine)
                                Spacer(Modifier.width(4.dp))
                                Text("موقعي", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                            OutlinedButton(onClick = { fx.click(); showCities = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("اختيار مدينة", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            val labels = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
            val nextIdx = PrayerCalc.prayerIdx[next.first]
            items(6) { idx ->
                val isNext = idx == nextIdx
                QCard(shape = RoundedCornerShape(18.dp), color = if (isNext) GoldShine.copy(alpha = 0.18f) else null, borderColor = if (isNext) GoldWarm else null) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(labels[idx], fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (idx == 1) p.muted else p.ink)
                        Text(PrayerCalc.fmt(times[idx], tz), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = if (isNext) TurquoiseAccent else EmeraldVibrant)
                    }
                }
            }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Navigation, "القبلة", tint = GoldWarm, modifier = Modifier.size(40.dp).rotate(qibla.toFloat()))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("اتجاه القبلة", fontWeight = FontWeight.ExtraBold, color = p.ink)
                            Text("${qibla.toInt()}° من الشمال باتجاه عقارب الساعة", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item { SectionTitle("طريقة الحساب") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PrayerCalc.methodNames.size) { i ->
                        FilterChip(
                            selected = method == i,
                            onClick = { fx.click(); st.prayerMethod = i; Reminders.rescheduleAll(ctx) },
                            label = { Text(PrayerCalc.methodNames[i], fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                        )
                    }
                }
            }
            item {
                SettingToggle("تنبيه عند دخول وقت كل صلاة", st.prayerAlert) { on ->
                    st.prayerAlert = on
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Reminders.rescheduleAll(ctx)
                }
            }
            item {
                Text("الأوقات محسوبة فلكياً داخل التطبيق دون إنترنت، وقد تختلف دقيقة أو دقيقتين عن التقويم المحلي لمسجدك.", color = p.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showCities) {
        AlertDialog(
            onDismissRequest = { showCities = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("اختر مدينتك", fontWeight = FontWeight.ExtraBold) },
            text = {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(QuranData.cities, key = { it.name }) { c ->
                        TextButton(onClick = { fx.click(); st.setLocation(c.name, c.lat, c.lng); showCities = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(c.name, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCities = false }) { Text("إغلاق") } }
        )
    }
}

// ======================= الأحاديث =======================
@Composable
fun HadithScreen(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الأحاديث النبوية الشريفة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(QuranData.hadiths) { h ->
                QCard(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(h.title, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent, modifier = Modifier.weight(1f))
                            IconButton(onClick = { fx.click(); shareText(ctx, "${h.text}\n${h.source}") }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Share, "مشاركة", tint = p.muted, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(h.text, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = p.ink, lineHeight = 26.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(h.source, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.muted)
                    }
                }
            }
        }
    }
}

// ======================= الإحصائيات والحفظ =======================
@Composable
fun StatsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()
    val downloaded = remember(ps.cacheVersion) { AudioCache.count(ctx) }
    var confirmReset by remember { mutableStateOf(false) }
    val pct = st.khatmaPercent()

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("سجل الحفظ والختمة", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 8.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                            Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("تقدّم الختمة والحفظ", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            Text("${st.readPages.size} من 604 صفحة", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { StatCard("آخر صفحة وصلت إليها", "صفحة ${st.lastPage} • ${QuranData.surahForPage(st.lastPage).name}") }
            item { StatCard("صفحات اليوم", "${st.todayPages()} من ${st.dailyGoal}") }
            item { StatCard("أيام القراءة المتتالية", "${st.streak} يوم") }
            item { StatCard("مجموع التسبيحات", "${st.totalTasbih} تسبيحة") }
            item { StatCard("السور المحملة صوتياً", "$downloaded سورة") }
            item { StatCard("الصفحات المحفوظة في المفضلة", "${st.favPages.size} صفحة") }
            item {
                OutlinedButton(onClick = { fx.click(); confirmReset = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.RestartAlt, null, tint = Color(0xFFB71C1C))
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير الإحصائيات", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("تصفير الإحصائيات؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم مسح تقدم الختمة والتسبيحات وأيام القراءة. لن تتأثر الصفحات المحفوظة.", color = p.ink) },
            confirmButton = { TextButton(onClick = { st.resetStats(); confirmReset = false }) { Text("تصفير", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = p.ink)
            Text(value, fontWeight = FontWeight.ExtraBold, color = EmeraldVibrant)
        }
    }
}

// ======================= الإعدادات =======================
@Composable
fun SettingsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()
    val files = remember(ps.cacheVersion) { AudioCache.count(ctx) }
    val sizeMb = remember(ps.cacheVersion) { AudioCache.sizeMb(ctx) }
    var notifOn by remember { mutableStateOf(NotificationManagerCompat.from(ctx).areNotificationsEnabled()) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        notifOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }
    var confirmClear by remember { mutableStateOf(false) }

    fun askNotif() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationManagerCompat.from(ctx).areNotificationsEnabled()) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الإعدادات", onBack = { nav.back() })
        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionTitle("القراءة والمظهر") }
            item { SettingToggle("الوضع الليلي (التطبيق والمصحف)", st.nightMode) { st.nightMode = it; fx.click() } }
            item { SettingToggle("ورق المصحف الكريمي الدافئ", st.paperWarmth) { st.paperWarmth = it; fx.click() } }
            item { SettingToggle("إبقاء الشاشة مضاءة أثناء القراءة", st.keepOn) { st.keepOn = it; fx.click() } }
            item { StepperRow("هدف الورد اليومي", "${st.dailyGoal} صفحات", { if (st.dailyGoal > 1) st.dailyGoal -= 1 }, { if (st.dailyGoal < 60) st.dailyGoal += 1 }) }

            item { SectionTitle("الصوت والاهتزاز") }
            item { SettingToggle("صوت النقرات", st.soundOn) { st.soundOn = it } }
            item { SettingToggle("اهتزاز خفيف عند كل تسبيحة", st.vibOn) { st.vibOn = it; fx.click() } }
            item { SettingToggle("اهتزاز طويل عند إتمام العدد", st.stageVib) { st.stageVib = it; fx.click() } }

            item { SectionTitle("التذكيرات") }
            if (!notifOn) {
                item {
                    QCard(shape = RoundedCornerShape(18.dp), borderColor = Color(0xFFB71C1C), onClick = { askNotif() }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.NotificationsOff, null, tint = Color(0xFFB71C1C))
                            Spacer(Modifier.width(10.dp))
                            Text("الإشعارات متوقفة. اضغط للتفعيل حتى تصلك التذكيرات.", color = p.ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            item { SettingToggle("تذكير الورد اليومي", st.dailyReminder) { st.dailyReminder = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            if (st.dailyReminder) {
                item {
                    StepperRow("وقت تذكير الورد", String.format(Locale.US, "%02d:00", st.reminderHour),
                        { st.reminderHour = (st.reminderHour + 23) % 24; Reminders.rescheduleAll(ctx) },
                        { st.reminderHour = (st.reminderHour + 1) % 24; Reminders.rescheduleAll(ctx) })
                }
            }
            item { SettingToggle("تنبيه أذكار الصباح (6:00) والمساء (5:30)", st.adhkarAlert) { st.adhkarAlert = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            item { SettingToggle("تنبيه أوقات الصلاة", st.prayerAlert) { st.prayerAlert = it; if (it) askNotif(); Reminders.rescheduleAll(ctx) } }
            item {
                QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.push(Screen.Prayer) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = GoldWarm)
                        Spacer(Modifier.width(10.dp))
                        Text("المدينة وطريقة حساب الصلاة: ${st.geo.name}", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                    }
                }
            }

            item { SectionTitle("التخزين") }
            item {
                QCard(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("التلاوات المحملة", fontWeight = FontWeight.ExtraBold, color = p.ink)
                            Text("$files ملف • $sizeMb م.ب", fontWeight = FontWeight.Bold, color = TurquoiseAccent)
                        }
                        Button(
                            onClick = { fx.click(); confirmClear = true }, enabled = files > 0,
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)), modifier = Modifier.fillMaxWidth()
                        ) { Text("حذف التلاوات المحملة لتوفير المساحة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    }
                }
            }
            item { SectionTitle("عن التطبيق") }
            item {
                QCard(shape = RoundedCornerShape(18.dp), onClick = { shareText(ctx, "تطبيق القرآن الكريم الشامل - مصحف وتلاوات ومسبحة ومواقيت الصلاة", "مشاركة التطبيق") }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Share, null, tint = GoldWarm)
                        Spacer(Modifier.width(10.dp))
                        Text("شارك التطبيق مع أحبّائك", color = p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("حذف التلاوات المحملة؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم حذف $files ملف صوتي. يمكنك الاستماع بالبث المباشر أو إعادة التنزيل لاحقاً.", color = p.ink) },
            confirmButton = { TextButton(onClick = { PlayerService.clearAll(ctx); confirmClear = false }) { Text("حذف", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } }
        )
    }
}

// ======================= MainActivity & AppRoot =======================
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

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !st.askedNotif) {
            st.askedNotif = true
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

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
                    Screen.Stats -> StatsScreen(nav)
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
