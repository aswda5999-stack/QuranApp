@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// 1. الشاشة الرئيسية (HomeScreen)
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
        FeatureItem("الحفظ", Icons.Default.School, listOf(Color(0xFFB45309), Color(0xFFF59E0B)), Color(0xFFF59E0B)) {
            nav.push(Screen.Hifz)
        }
    )

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                            Text(greetText, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(2.dp))
                            Text("اليوم: ${dateInfo.dayName}  •  ${dateInfo.slashDate}", color = GoldShine, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                            Text("${dateInfo.gregorianMonth}  |  ${dateInfo.hijriFull}", color = GoldSoftBg.copy(alpha = 0.92f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        HeaderIconButton(Icons.Default.Settings, 0, "الإعدادات") { nav.push(Screen.Settings) }
                    }
                    Spacer(Modifier.height(14.dp))
                    NextPrayerHeroCard(nav)
                    Spacer(Modifier.height(10.dp))
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
                        if (pageNum != null) SearchRow("الذهاب إلى صفحة $pageNum", "المصحف الشريف", null) { query = ""; nav.openPage(pageNum) }
                        results.forEach { s -> SearchRow("سورة ${s.name}", "${s.type} • ${s.ayahs} آية • ص ${s.page}", s.num) { query = ""; nav.openPage(s.page) } }
                        if (results.isEmpty() && pageNum == null) Text("لا توجد نتائج مطابقة", color = p.muted, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp))
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                features.chunked(4).forEach { rowItems ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowItems.forEach { item -> ProFeatureTile(item, Modifier.weight(1f)) }
                    }
                }
            }
        }

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
                        Surface(shape = CircleShape, color = GoldShine.copy(alpha = 0.16f), border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.6f))) {
                            Text("✦ آية اليوم ✦", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(verse.text, color = Color.White, fontFamily = FontFamily.Serif, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
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

        item {
            QCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(24.dp), borderColor = GoldWarm) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(GoldWarm.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
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
                    Text(hadith.text, color = p.ink, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 28.sp)
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
                            onClick = { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                        ) { Text("تفعيل الإشعارات", color = GoldSoftBg, fontWeight = FontWeight.Bold) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotif = false; nav.push(Screen.Settings) }) { Text("إعدادات التذكير", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent) } },
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
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(GoldShine.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AccessTimeFilled, null, tint = GoldShine, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("متبقي لصلاة ${PrayerCalc.names[next.first]}", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("موعد الأذان: ${PrayerCalc.fmt(next.second, tz)}  •  ${st.geo.name}", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(14.dp), color = RoyalForest.copy(alpha = 0.75f), border = BorderStroke(1.dp, GoldWarm)) {
                Text(if (next.second > 0) PrayerCalc.countdown(next.second - now) else "--:--:--", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
        }
    }
}

@Composable
private fun ProFeatureTile(item: FeatureItem, modifier: Modifier) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Column(
        modifier = modifier.clip(RoundedCornerShape(18.dp)).clickable { fx.click(); item.onClick() }.padding(vertical = 4.dp),
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
        Text(item.title, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = p.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SearchRow(title: String, sub: String, num: Int?, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { fx.click(); onClick() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
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
    Surface(onClick = { fx.click(); onClick() }, shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.16f), border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.75f))) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = GoldShine, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
    }
}

// ============================================================
// 2. قارئ المصحف الشريف (ReaderScreen)
// ============================================================
private val InvertMatrix = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
private val WarmMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0.96f, 0f, 0f, 0f, 0f, 0f, 0.84f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))

@Composable
fun ReaderScreen(nav: Nav) {
    val st = LocalApp.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = (st.lastPage - 1).coerceIn(0, 603), pageCount = { 604 })
    val curPage = pagerState.currentPage + 1
    var chrome by remember { mutableStateOf(true) }
    var showJump by remember { mutableStateOf(false) }
    var sliderDrag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(pagerState) {
        var first = true
        snapshotFlow { pagerState.currentPage }.collect { idx ->
            st.onPageViewed(idx + 1, counted = !first)
            first = false
        }
    }
    LaunchedEffect(curPage) {
        for (n in listOf(curPage + 1, curPage + 2, curPage - 1)) {
            if (n in 1..604) ctx.imageLoader.enqueue(ImageRequest.Builder(ctx).data(QuranData.pageUrl(n)).build())
        }
    }
    DisposableEffect(st.keepOn) {
        val w = (ctx as? Activity)?.window
        if (st.keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val bg = when {
        st.nightMode -> Color.Black
        st.paperWarmth -> Color(0xFFF6EFE0)
        else -> Color.White
    }
    val surah = QuranData.surahForPage(curPage)
    val displayPage = sliderDrag?.toInt() ?: curPage

    Column(Modifier.fillMaxSize().background(bg)) {
        AnimatedVisibility(visible = chrome, enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut()) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(bottomStart = 26.dp, bottomEnd = 26.dp), border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.7f)), shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(onClick = { fx.click(); nav.back() }, shape = CircleShape, color = EmeraldVibrant, border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Color.White, modifier = Modifier.size(20.dp)) }
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, tint = GoldShine, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("سورة ${surah.name}", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(2.dp))
                        Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f)) {
                            Text("صفحة $displayPage / 604  •  الجزء ${QuranData.juzOfPage(displayPage)}", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp))
                        }
                    }
                    Surface(onClick = { fx.click(); st.nightMode = !st.nightMode }, shape = CircleShape, color = EmeraldVibrant, border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.6f)), modifier = Modifier.size(38.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(if (st.nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, "الوضع الليلي", tint = GoldShine, modifier = Modifier.size(20.dp)) }
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { idx ->
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { chrome = !chrome }) }) {
                MushafPage(idx + 1, st.nightMode, st.paperWarmth)
            }
        }

        AnimatedVisibility(visible = chrome, enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), border = BorderStroke(1.3.dp, GoldWarm), shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = { fx.click(); if (curPage < 604) scope.launch { pagerState.animateScrollToPage(curPage) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ChevronRight, "التالية", tint = GoldShine)
                        }
                        Slider(
                            value = (sliderDrag ?: curPage.toFloat()).coerceIn(1f, 604f),
                            onValueChange = { sliderDrag = it },
                            onValueChangeFinished = {
                                val target = sliderDrag?.toInt() ?: curPage
                                sliderDrag = null
                                scope.launch { pagerState.scrollToPage(target.coerceIn(1, 604) - 1) }
                            },
                            valueRange = 1f..604f,
                            colors = SliderDefaults.colors(thumbColor = GoldShine, activeTrackColor = GoldShine, inactiveTrackColor = Color.White.copy(alpha = 0.25f)),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        )
                        IconButton(onClick = { fx.click(); if (curPage > 1) scope.launch { pagerState.animateScrollToPage(curPage - 2) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ChevronLeft, "السابقة", tint = GoldShine)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val isSaved = st.isFav(curPage)
                        ProReaderNavButton(if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (isSaved) "محفوظة" else "المفضلة", isSaved) { fx.click(); st.toggleFav(curPage) }
                        ProReaderNavButton(Icons.Default.FormatListNumbered, "الفهرس", false) { fx.click(); nav.push(Screen.Index) }
                        ProReaderNavButton(Icons.Default.MyLocation, "انتقال", false) { fx.click(); showJump = true }
                        ProReaderNavButton(Icons.Default.Headphones, "استماع", false) { fx.click(); PlayerService.play(ctx, st.reciterId, surah.num); nav.push(Screen.Audio) }
                        ProReaderNavButton(Icons.Default.Home, "الرئيسية", false) { fx.click(); nav.go(Screen.Home) }
                    }
                }
            }
        }
    }

    if (showJump) {
        var text by remember { mutableStateOf("") }
        val n = text.toIntOrNull()
        val valid = n != null && n in 1..604
        AlertDialog(
            onDismissRequest = { showJump = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("الانتقال إلى صفحة", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("رقم الصفحة (1 - 604)") }, isError = text.isNotEmpty() && !valid
                )
            },
            confirmButton = { TextButton(enabled = valid, onClick = { showJump = false; scope.launch { pagerState.scrollToPage((n ?: 1) - 1) } }) { Text("انتقال", fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { showJump = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ProReaderNavButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (active) TurquoiseAccent else EmeraldVibrant.copy(alpha = 0.65f)).clickable { onClick() }.padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, label, tint = if (active) GoldShine else GoldSoftBg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (active) GoldShine else Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MushafPage(page: Int, night: Boolean, warm: Boolean) {
    val ctx = LocalContext.current
    var retry by remember { mutableIntStateOf(0) }
    val filter = remember(night, warm) {
        when {
            night -> ColorFilter.colorMatrix(InvertMatrix)
            warm -> ColorFilter.colorMatrix(WarmMatrix)
            else -> null
        }
    }
    key(retry) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(ctx).data(QuranData.pageUrl(page)).crossfade(true).diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = "صفحة $page", contentScale = ContentScale.Fit, colorFilter = filter, modifier = Modifier.fillMaxSize().padding(4.dp),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldWarm) } },
            error = {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Refresh, null, tint = GoldWarm, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("تعذر تحميل الصفحة $page", color = if (night) Color.White else InkDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { retry++ }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)) { Text("إعادة المحاولة", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold) }
                }
            }
        )
    }
}

// ============================================================
// 3. الفهرس والمفضلة (IndexScreen & FavoritesScreen)
// ============================================================
@Composable
fun IndexScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("الكل") }
    val list = remember(query, typeFilter) { QuranData.search(query).filter { typeFilter == "الكل" || it.type == typeFilter } }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الفهرس", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("السور (114)", "الأجزاء (30)").forEachIndexed { i, label ->
                FilterChip(selected = tab == i, onClick = { fx.click(); tab = i }, label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg))
            }
        }
        if (tab == 0) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("الكل", "مكية", "مدنية")) { t ->
                    FilterChip(selected = typeFilter == t, onClick = { fx.click(); typeFilter = t }, label = { Text(t, fontWeight = FontWeight.Bold, fontSize = 12.sp) }, shape = RoundedCornerShape(14.dp), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldWarm, selectedLabelColor = RoyalForest))
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة بهذا الاسم") }
                items(list, key = { it.num }) { s ->
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(s.page) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(s.num)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("${s.type} • ${s.ayahs} آية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص ${s.page}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            IconButton(onClick = { fx.click(); PlayerService.play(ctx, st.reciterId, s.num); nav.push(Screen.Audio) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PlayCircleOutline, "استماع", tint = GoldWarm, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(30) { i ->
                    val start = QuranData.juzStart[i]
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(start) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(i + 1)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الجزء ${i + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("يبدأ من سورة ${QuranData.surahForPage(start).name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص $start", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val favs = st.favPages.sorted()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الصفحات المحفوظة في المفضلة", onBack = { nav.back() })
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
