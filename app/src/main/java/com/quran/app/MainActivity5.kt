@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import java.util.Locale

// ============================================================
// 1. المسبحة الإلكترونية والأذكار (TasbihScreen)
// ============================================================
@Composable
fun TasbihScreen(nav: Nav) {
    val fx = LocalFx.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (nav.tasbihTab == 0) "المسبحة الإلكترونية" else "الأذكار اليومية", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("المسبحة" to Icons.Default.TouchApp, "الأذكار" to Icons.Default.WbSunny).forEachIndexed { i, (label, icon) ->
                FilterChip(
                    selected = nav.tasbihTab == i, onClick = { fx.click(); nav.tasbihTab = i },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg, selectedLeadingIconColor = GoldShine)
                )
            }
        }
        if (nav.tasbihTab == 0) TasbihCounter() else AdhkarList()
    }
}

@Composable
private fun TasbihCounter() {
    val st = LocalApp.current
    val fx = LocalFx.current
    val stages = QuranData.tasbihStages

    var stage by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(stages[0].defaultTarget) }
    var count by rememberSaveable { mutableStateOf(0) }
    var rounds by rememberSaveable { mutableStateOf(0) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }

    val active = stages[stage]
    val next = stages[(stage + 1) % stages.size]
    val progress by animateFloatAsState((count / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), label = "ring")

    fun goStage(i: Int) { stage = i; target = stages[i].defaultTarget; count = 0 }

    fun tap() {
        fx.click()
        if (!autoAdvance && count >= target) count = 0
        st.totalTasbih += 1
        if (count + 1 >= target) {
            fx.vibrate(if (st.stageVib) 140L else 35L)
            rounds += 1
            if (autoAdvance) goStage((stage + 1) % stages.size) else count = target
        } else {
            fx.vibrate(35L)
            count += 1
        }
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(stages) { idx, s ->
                    FilterChip(selected = idx == stage, onClick = { fx.click(); goStage(idx) }, shape = RoundedCornerShape(16.dp), label = { Text(s.phrase, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldShine))
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = RoyalForest, border = BorderStroke(1.8.dp, GoldWarm), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Box {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 10, 33, 100).forEach { goal ->
                                    Surface(onClick = { fx.click(); target = goal; count = 0 }, shape = RoundedCornerShape(12.dp), color = if (target == goal) GoldShine else EmeraldVibrant, border = BorderStroke(1.dp, GoldWarm)) {
                                        Text("$goal", color = if (target == goal) RoyalForest else GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Surface(onClick = { fx.click(); autoAdvance = !autoAdvance }, shape = RoundedCornerShape(12.dp), color = if (autoAdvance) TurquoiseAccent else EmeraldVibrant.copy(alpha = 0.5f), border = BorderStroke(1.dp, GoldWarm)) {
                                Text(if (autoAdvance) "انتقال تلقائي ✓" else "تكرار ثابت", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(active.phrase, color = GoldShine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        Text(active.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                            CircularProgressIndicator(progress = { progress }, strokeWidth = 10.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.18f), modifier = Modifier.fillMaxSize())
                            Surface(onClick = { tap() }, shape = CircleShape, color = EmeraldVibrant, border = BorderStroke(2.dp, GoldShine), modifier = Modifier.size(136.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$count", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("من $target", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("الجولات المكتملة: $rounds • إجمالي التسبيحات: ${st.totalTasbih}", color = GoldSoftBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { fx.click(); count = 0 }) { Icon(Icons.Default.Refresh, null, tint = GoldShine, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("تصفير", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            TextButton(onClick = { fx.click(); if (count > 0) { count -= 1; if (st.totalTasbih > 0) st.totalTasbih -= 1 } }) { Icon(Icons.Default.Undo, null, tint = GoldShine, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("تراجع", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            TextButton(onClick = { fx.click(); goStage((stage + 1) % stages.size) }) { Text("تخطي: ${next.phrase.take(14)}", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(18.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarList() {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val counts = remember { mutableStateMapOf<Int, Int>() }
    val adhkar = QuranData.adhkar

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val done = adhkar.indices.count { (counts[it] ?: 0) >= adhkar[it].target }
                Text("أنجزت $done من ${adhkar.size}", color = p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                TextButton(onClick = { fx.click(); counts.clear() }) { Icon(Icons.Default.Refresh, null, tint = TurquoiseAccent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("إعادة الكل", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold) }
            }
        }
        itemsIndexed(adhkar) { i, d ->
            val c = counts[i] ?: 0
            val done = c >= d.target
            QCard(shape = RoundedCornerShape(20.dp), borderColor = if (done) GoldWarm else null, onClick = { if (c < d.target) { counts[i] = c + 1; fx.vibrate(if (c + 1 >= d.target) 100L else 30L) } }) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.title, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (done) Icon(Icons.Default.CheckCircle, "تم", tint = GoldWarm)
                        else if (c > 0) IconButton(onClick = { fx.click(); counts[i] = 0 }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, "إعادة", tint = p.muted, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(d.text, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { (c / d.target.toFloat()).coerceIn(0f, 1f) }, color = if (done) GoldWarm else EmeraldVibrant, trackColor = p.border.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape))
                    Spacer(Modifier.height(4.dp))
                    Text("اضغط للعد • $c / ${d.target}", color = if (done) GoldWarm else p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================
// 2. نظام الحفظ الموسّع: اختبار الحفظ + سجل السور المحفوظة + الإحصائيات (HifzScreen)
// ============================================================
@Composable
fun HifzScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    var subTab by remember { mutableIntStateOf(0) }
    var quizIdx by remember { mutableIntStateOf(0) }
    var selectedOpt by remember { mutableIntStateOf(-1) }
    var confirmReset by remember { mutableStateOf(false) }

    val pct = st.khatmaPercent()
    val memPct = (st.memorizedSurahs.size * 100 / 114).coerceIn(0, 100)

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("مركز الحفظ والختمة", onBack = { nav.back() })

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("اختبار الحفظ", "سجل السور (${st.memorizedSurahs.size})", "الإحصائيات").forEachIndexed { i, title ->
                FilterChip(
                    selected = subTab == i,
                    onClick = { fx.click(); subTab = i },
                    label = { Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                )
            }
        }

        when (subTab) {
            0 -> {
                val q = QuranData.hifzQuizzes[quizIdx % QuranData.hifzQuizzes.size]
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, border = BorderStroke(1.3.dp, GoldWarm), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("اختبار تثبيت الحفظ (${(quizIdx % QuranData.hifzQuizzes.size) + 1}/${QuranData.hifzQuizzes.size})", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(q.question, color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center, lineHeight = 34.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(q.surahRef, color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    itemsIndexed(q.options) { idx, opt ->
                        val isChosen = selectedOpt == idx
                        val isRight = idx == q.correctIndex
                        val cardBg = when {
                            selectedOpt == -1 -> null
                            isRight -> Color(0xFF10B981).copy(alpha = 0.22f)
                            isChosen -> Color(0xFFE11D48).copy(alpha = 0.22f)
                            else -> null
                        }
                        QCard(shape = RoundedCornerShape(18.dp), color = cardBg, borderColor = if (selectedOpt != -1 && isRight) Color(0xFF10B981) else null, onClick = {
                            if (selectedOpt == -1) {
                                selectedOpt = idx
                                fx.vibrate(if (isRight) 50L else 120L)
                            }
                        }) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(opt, fontFamily = FontFamily.Serif, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = p.ink)
                                if (selectedOpt != -1 && isRight) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669))
                                else if (isChosen) Icon(Icons.Default.Cancel, null, tint = Color(0xFFE11D48))
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { fx.click(); selectedOpt = -1; quizIdx += 1 },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("السؤال التالي", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            1 -> {
                LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        QCard(shape = RoundedCornerShape(20.dp), borderColor = GoldWarm) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("نسبة حفظ السور: $memPct% (${st.memorizedSurahs.size} من 114 سورة)", fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent)
                            }
                        }
                    }
                    items(QuranData.surahs, key = { it.num }) { s ->
                        val isMem = s.num in st.memorizedSurahs
                        QCard(shape = RoundedCornerShape(18.dp), borderColor = if (isMem) Color(0xFF10B981) else null, onClick = { st.toggleMemorizedSurah(s.num) }) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                NumberBadge(s.num, 32)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, color = p.ink)
                                    Text("${s.ayahs} آية • ص ${s.page}", fontSize = 11.sp, color = p.muted, fontWeight = FontWeight.Bold)
                                }
                                Checkbox(checked = isMem, onCheckedChange = { fx.click(); st.toggleMemorizedSurah(s.num) }, colors = CheckboxDefaults.colors(checkedColor = EmeraldVibrant))
                            }
                        }
                    }
                }
            }

            2 -> {
                val downloaded = remember { AudioCache.count(ctx) }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Surface(shape = RoundedCornerShape(24.dp), color = RoyalForest, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(progress = { pct / 100f }, strokeWidth = 8.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.2f), modifier = Modifier.fillMaxSize())
                                    Text("$pct%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("تقدّم الختمة", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text("${st.readPages.size} من 604 صفحة", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    item { StatRow("آخر صفحة وصلت إليها", "صفحة ${st.lastPage} • ${QuranData.surahForPage(st.lastPage).name}") }
                    item { StatRow("صفحات اليوم", "${st.todayPages()} من ${st.dailyGoal}") }
                    item { StatRow("أيام القراءة المتتالية", "${st.streak} يوم") }
                    item { StatRow("السور المحفوظة غيباً", "${st.memorizedSurahs.size} سورة") }
                    item { StatRow("مجموع التسبيحات", "${st.totalTasbih} تسبيحة") }
                    item { StatRow("السور المحملة صوتياً", "$downloaded سورة") }
                    item {
                        OutlinedButton(onClick = { fx.click(); confirmReset = true }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.RestartAlt, null, tint = Color(0xFFB71C1C))
                            Spacer(Modifier.width(6.dp))
                            Text("تصفير إحصائيات القراءة", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("تصفير الإحصائيات؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم مسح تقدم الختمة والتسبيحات. لن تتأثر السور المحفوظة أو المفضلة.", color = p.ink) },
            confirmButton = { TextButton(onClick = { st.resetStats(); confirmReset = false }) { Text("تصفير", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontWeight = FontWeight.Bold, color = p.ink)
            Text(value, fontWeight = FontWeight.ExtraBold, color = EmeraldVibrant)
        }
    }
}

// ============================================================
// 3. الأحاديث النبوية (HadithScreen)
// ============================================================
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

// ============================================================
// 4. الإعدادات (SettingsScreen)
// ============================================================
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
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false }, shape = RoundedCornerShape(24.dp),
            title = { Text("حذف التلاوات المحملة؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم حذف $files ملف صوتي. يمكنك الاستماع بالبث المباشر أو إعادة التنزيل لاحقاً.", color = p.ink) },
            confirmButton = { TextButton(onClick = { PlayerService.clearAll(ctx); confirmClear = false }) { Text("حذف", color = Color(0xFFB71C1C), fontWeight = FontWeight.ExtraBold) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("إلغاء") } }
        )
    }
}
