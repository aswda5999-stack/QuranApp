@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

// ============================================================
// 1. الشاشة الرئيسية المتطورة (HomeScreen)
// ============================================================
@Composable
fun HomeScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current

    // تحديث الوقت كل 20 ثانية (التحية والصلاة القادمة والتاريخ)
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val cal = AppClock.calendar(st, nowMs)
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val greeting = AppClock.greeting(cal)
    val hijriText = HijriCalendar.format(cal, st.hijriOffset)
    val gregText = ArabicDate.gregorian(cal, st.monthStyle)
    val weekday = ArabicDate.weekday(cal)
    val next = PrayerCalc.next(st, nowMs)

    val lastSurah = remember(st.lastPage) { QuranData.surahForPage(st.lastPage) }
    val lastJuz = remember(st.lastPage) { QuranData.juzForPage(st.lastPage) }
    val pct = st.khatmaPercent()
    val todayRead = st.todayPages()

    // اقتراح الأذكار المناسبة لوقت اليوم
    val suggestion: Triple<String, Int, ImageVector> = when (hour) {
        in 4..10 -> Triple("أذكار الصباح", AdhkarTabs.MORNING, Icons.Default.WbSunny)
        in 11..14 -> Triple("أذكار بعد الصلاة", AdhkarTabs.AFTER_PRAYER, Icons.Default.Mosque)
        in 15..19 -> Triple("أذكار المساء", AdhkarTabs.EVENING, Icons.Default.NightsStay)
        else -> Triple("أذكار النوم", AdhkarTabs.SLEEP, Icons.Default.Bedtime)
    }
    val suggestionLeft = adhkarCategoryProgress(st, suggestion.second)

    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. الترويسة الملكية: التحية + التاريخ الهجري والميلادي + متابعة التلاوة
        item {
            Surface(
                color = RoyalForest,
                shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp),
                border = BorderStroke(1.3.dp, GoldWarm.copy(alpha = 0.65f)),
                shadowElevation = 10.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant, TealStart)))
                        .statusBarsPadding()
                ) {
                    IslamicPattern(Modifier.matchParentSize())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                // التحية بالخط العثماني العريض
                                Text(
                                    text = greeting,
                                    fontFamily = UthmanicFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 38.sp,
                                    color = GoldShine,
                                    lineHeight = 52.sp
                                )
                                Text(
                                    text = AppClock.greetingSub(cal),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = GoldSoftBg.copy(alpha = 0.95f)
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                HeaderIconButton(Icons.Default.Bookmarks, "المفضلة") { fx.click(); nav.push(Screen.Bookmarks) }
                                HeaderIconButton(Icons.Default.Settings, "الإعدادات") { fx.click(); nav.openSettings(0) }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // شريط التاريخ: هجري | ميلادي
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.24f))
                                .border(1.dp, GoldShine.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DateBlock(
                                icon = Icons.Default.NightsStay,
                                label = "التاريخ الهجري",
                                value = hijriText,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                Modifier
                                    .padding(horizontal = 8.dp)
                                    .width(1.dp)
                                    .height(38.dp)
                                    .background(GoldShine.copy(alpha = 0.4f))
                            )
                            DateBlock(
                                icon = Icons.Default.CalendarMonth,
                                label = "$weekday • الميلادي",
                                value = gregText,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // بطاقة متابعة التلاوة والختمة السريعة
                        Surface(
                            onClick = { fx.click(); nav.openPage(st.lastPage) },
                            shape = RoundedCornerShape(22.dp),
                            color = Color.Black.copy(alpha = 0.24f),
                            border = BorderStroke(1.1.dp, GoldShine.copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(15.dp))
                                        .background(Brush.linearGradient(listOf(GoldShine, GoldWarm)))
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = RoyalForest, modifier = Modifier.size(26.dp))
                                }

                                Spacer(Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "متابعة التلاوة • سورة ${lastSurah.name}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "صفحة ${st.lastPage} • ${lastJuz.name} • الختمة $pct%",
                                        color = GoldSoftBg,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Surface(shape = RoundedCornerShape(12.dp), color = GoldShine) {
                                    Text(
                                        text = "أكمل",
                                        color = RoyalForest,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. الصلاة القادمة
        item {
            QCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                borderColor = GoldWarm.copy(alpha = 0.5f),
                onClick = { nav.push(Screen.Prayer) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant)))
                            .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                    ) {
                        Icon(Icons.Default.Mosque, null, tint = GoldShine, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("الصلاة القادمة: ${next.name}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.ink)
                        Text(
                            "بعد ${PrayerCalc.remainingWords(next.millis - nowMs)} • ${st.geo.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = p.muted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        PrayerCalc.formatMillis(st, next.millis),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = EmeraldVibrant
                    )
                }
            }
        }

        // 3. إحصائيات سريعة
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HomeStat("${st.streak}", "أيام متتالية", Icons.Default.LocalFireDepartment, GoldWarm, Modifier.weight(1f))
                HomeStat("$todayRead/${st.dailyGoal}", "صفحات اليوم", Icons.Default.AutoStories, EmeraldVibrant, Modifier.weight(1f))
                HomeStat("$pct%", "إنجاز الختمة", Icons.Default.WorkspacePremium, TurquoiseAccent, Modifier.weight(1f))
            }
        }

        // 4. أذكار مناسبة لهذا الوقت
        item {
            QCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                borderColor = EmeraldVibrant.copy(alpha = 0.5f),
                onClick = { nav.openAdhkar(suggestion.second) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(EmeraldVibrant.copy(alpha = 0.14f))
                    ) {
                        Icon(suggestion.third, null, tint = EmeraldVibrant, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("حان وقت ${suggestion.first}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.ink)
                        Text(
                            if (suggestionLeft.second > 0 && suggestionLeft.first == suggestionLeft.second) "أتممتها اليوم ✓ تقبّل الله منك"
                            else "أنجزت ${suggestionLeft.first} من ${suggestionLeft.second} اليوم",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = p.muted
                        )
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = p.muted)
                }
            }
        }

        // 5. آية اليوم
        item { DailyVerseCard(nav = nav) }

        // 6. الورد اليومي
        item {
            QCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                shape = RoundedCornerShape(22.dp),
                borderColor = GoldWarm.copy(alpha = 0.4f),
                onClick = { nav.openPage(st.lastPage) }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoGraph, null, tint = TurquoiseAccent, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("الورد اليومي والختمة", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                        }
                        Text(
                            "$todayRead / ${st.dailyGoal} صفحات",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = EmeraldVibrant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (todayRead / st.dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f) },
                        color = EmeraldVibrant,
                        trackColor = p.border.copy(alpha = 0.45f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // 7. شبكة أقسام التطبيق
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SectionTitle("أقسام التطبيق")

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("فهرس السور", "114 سورة • 30 جزءاً", Icons.AutoMirrored.Filled.MenuBook, EmeraldVibrant, Modifier.weight(1f)) {
                        nav.indexTab = 0; nav.push(Screen.Index)
                    }
                    QuickTile("المكتبة الصوتية", "${QuranData.reciters.size} قارئاً • بدون نت", Icons.Default.Headphones, TurquoiseAccent, Modifier.weight(1f)) {
                        nav.push(Screen.Audio)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("مواقيت الصلاة", "الأذان • القبلة", Icons.Default.Mosque, GoldWarm, Modifier.weight(1f)) {
                        nav.push(Screen.Prayer)
                    }
                    QuickTile("الأذكار", "صباح • مساء • نوم • وأكثر", Icons.Default.AutoAwesome, Color(0xFF0F766E), Modifier.weight(1f)) {
                        nav.openAdhkar(AdhkarTabs.MORNING)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("المسبحة الإلكترونية", "${st.totalTasbih} تسبيحة", Icons.Default.Spa, Color(0xFF7C3AED), Modifier.weight(1f)) {
                        nav.push(Screen.Tasbih)
                    }
                    QuickTile("مركز الحفظ", "اختبارات • سجل السور", Icons.Default.Psychology, RoyalForest, Modifier.weight(1f)) {
                        nav.push(Screen.Hifz)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickTile("العلامات المرجعية", "${st.bookmarks.size} صفحة محفوظة", Icons.Default.Bookmarks, Color(0xFFB45309), Modifier.weight(1f)) {
                        nav.push(Screen.Bookmarks)
                    }
                    QuickTile("الإعدادات", "الموقع • الوقت • المظهر", Icons.Default.Tune, Color(0xFF334155), Modifier.weight(1f)) {
                        nav.openSettings(0)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.dp, GoldShine.copy(alpha = 0.45f), CircleShape)
    ) {
        Icon(icon, desc, tint = GoldShine, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun DateBlock(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = GoldShine, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, maxLines = 1)
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun HomeStat(value: String, label: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = p.ink)
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp, color = p.muted)
        }
    }
}

@Composable
private fun QuickTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Surface(
        onClick = { fx.click(); onClick() },
        shape = RoundedCornerShape(20.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(accent.copy(alpha = 0.14f))
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ============================================================
// 2. فهرس السور والأجزاء والبحث السريع (IndexScreen)
// ============================================================
@Composable
fun IndexScreen(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableIntStateOf(0) }

    val filteredSurahs = remember(query, typeFilter) {
        QuranData.search(query).filter { s ->
            when (typeFilter) {
                1 -> s.type == "مكية"
                2 -> s.type == "مدنية"
                else -> true
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "المصحف الشريف",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); nav.push(Screen.Bookmarks) }) {
                    Icon(Icons.Default.Bookmarks, "المفضلة", tint = GoldShine)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("السور (114)", "الأجزاء (30)").forEachIndexed { idx, label ->
                FilterChip(
                    selected = nav.indexTab == idx,
                    onClick = { fx.click(); nav.indexTab = idx },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldVibrant,
                        selectedLabelColor = GoldShine
                    )
                )
            }
        }

        if (nav.indexTab == 0) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            )

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "مكية", "مدنية").forEachIndexed { i, t ->
                    FilterChip(
                        selected = typeFilter == i,
                        onClick = { fx.click(); typeFilter = i },
                        label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldWarm,
                            selectedLabelColor = RoyalForest
                        )
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredSurahs.isEmpty()) {
                    item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة مطابقة لبحثك") }
                }
                items(filteredSurahs, key = { it.num }) { s ->
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { nav.openPage(s.page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(s.num, 38)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "سُورَةُ ${s.name}",
                                    fontFamily = UthmanicFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = p.ink
                                )
                                Text(
                                    text = "${s.type} • ${s.ayahs} آية",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.muted
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldVibrant.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "ص ${s.page}",
                                    color = EmeraldVibrant,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuranData.ajza, key = { it.num }) { j ->
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { nav.openPage(j.page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(j.num, 38)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(j.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("يبدأ بسورة ${j.startSurah}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص ${j.page}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 3. قارئ صفحات المصحف الشريف التفاعلي (ReaderScreen)
// ============================================================
private object MushafPageLoader {
    private val cache = mutableMapOf<Int, String>()

    suspend fun loadPageText(page: Int): String = withContext(Dispatchers.IO) {
        cache[page]?.let { return@withContext it }
        runCatching {
            val json = URL("https://api.alquran.cloud/v1/page/$page/quran-uthmani").readText()
            val ayahs = JSONObject(json).getJSONObject("data").getJSONArray("ayahs")
            val sb = StringBuilder()
            var lastSurahNum = -1
            for (i in 0 until ayahs.length()) {
                val obj = ayahs.getJSONObject(i)
                val surahObj = obj.getJSONObject("surah")
                val sNum = surahObj.getInt("number")
                val sName = surahObj.getString("name")
                val numInSurah = obj.getInt("numberInSurah")
                var text = obj.getString("text")

                if (sNum != lastSurahNum) {
                    if (sb.isNotEmpty()) sb.append("\n\n")
                    sb.append("﴿ $sName ﴾\n")
                    if (sNum != 1 && sNum != 9 && numInSurah == 1) {
                        sb.append("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ\n")
                        text = text.removePrefix("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ").trim()
                    }
                    lastSurahNum = sNum
                }
                sb.append("$text ﴿$numInSurah﴾ ")
            }
            val result = sb.toString().trim()
            cache[page] = result
            result
        }.getOrElse {
            "تعذر تحميل نص الصفحة $page — يرجى التحقق من اتصال الإنترنت عند الفتح لأول مرة."
        }
    }
}

@Composable
fun ReaderScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = (nav.readerPage - 1).coerceIn(0, 603),
        pageCount = { 604 }
    )

    val currentPage = pagerState.currentPage + 1
    val currentSurah = remember(currentPage) { QuranData.surahForPage(currentPage) }
    val currentJuz = remember(currentPage) { QuranData.juzForPage(currentPage) }
    val isBookmarked = currentPage in st.bookmarks

    LaunchedEffect(currentPage) {
        nav.readerPage = currentPage
        st.markPageRead(currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.paper)
            .navigationBarsPadding()
    ) {
        // شريط علوي للمصحف (يمتد لونه الأخضر تحت شريط الحالة)
        Surface(
            color = RoyalForest,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { fx.click(); nav.back() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "رجوع", tint = GoldShine)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سورة ${currentSurah.name}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${currentJuz.name} • صفحة $currentPage من 604",
                        color = GoldSoftBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { fx.click(); st.mushafFontScale -= 0.1f }) {
                    Icon(Icons.Default.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); st.mushafFontScale += 0.1f }) {
                    Icon(Icons.Default.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); st.toggleBookmark(currentPage) }) {
                    Icon(
                        if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        "علامة مرجعية",
                        tint = if (isBookmarked) GoldShine else Color.White
                    )
                }
            }
        }

        // صفحات المصحف القابلة للتمرير الأفقي
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            var pageText by remember(pageNum) { mutableStateOf<String?>(null) }
            var reloadTick by remember(pageNum) { mutableIntStateOf(0) }

            LaunchedEffect(pageNum, reloadTick) {
                pageText = null
                pageText = MushafPageLoader.loadPageText(pageNum)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(p.paper)
                    .border(1.5.dp, GoldWarm.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                if (pageText == null) {
                    CircularProgressIndicator(
                        color = EmeraldVibrant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (pageText!!.startsWith("تعذر تحميل")) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = pageText!!,
                            color = p.muted,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { fx.click(); reloadTick += 1 },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                        ) {
                            Text("إعادة المحاولة", fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = pageText!!,
                            fontFamily = UthmanicFontFamily,
                            color = p.mushafInk,
                            fontSize = (22 * st.mushafFontScale).sp,
                            lineHeight = (38 * st.mushafFontScale).sp,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // شريط سفلي للتنقل السريع والاستماع للسورة
        Surface(
            color = RoyalForest,
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        fx.click()
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    enabled = currentPage > 1
                ) {
                    Text("الصفحة السابقة", color = GoldSoftBg, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        fx.click()
                        PlayerService.play(ctx, st.reciterId, currentSurah.num)
                        nav.push(Screen.Audio)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = GoldShine)
                ) {
                    Icon(Icons.Default.VolumeUp, null, tint = RoyalForest, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("استمع للسورة", color = RoyalForest, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                TextButton(
                    onClick = {
                        fx.click()
                        if (pagerState.currentPage < 603) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    enabled = currentPage < 604
                ) {
                    Text("الصفحة التالية", color = GoldSoftBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// 4. شاشة العلامات المرجعية والمفضلة (BookmarksScreen)
// ============================================================
@Composable
fun BookmarksScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("العلامات المرجعية", onBack = { nav.back() })

        if (st.bookmarks.isEmpty()) {
            EmptyState(Icons.Default.BookmarkBorder, "لا توجد صفحات محفوظة في المفضلة بعد")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(st.bookmarks, key = { it }) { page ->
                    val s = QuranData.surahForPage(page)
                    val j = QuranData.juzForPage(page)
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { nav.openPage(page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(page, 38)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("صفحة $page • سورة ${s.name}", fontWeight = FontWeight.ExtraBold, color = p.ink)
                                Text(j.name, fontSize = 12.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { fx.click(); st.toggleBookmark(page) }) {
                                Icon(Icons.Default.BookmarkRemove, "إزالة", tint = Color(0xFFB71C1C))
                            }
                        }
                    }
                }
            }
        }
    }
}
