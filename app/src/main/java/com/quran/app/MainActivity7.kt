@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.app.TimePickerDialog
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ============================================================
// 1. شاشة الإعدادات الشاملة (SettingsScreen) — أقسام واضحة وتنقل سلس بينها
// ============================================================
private data class SettingsSection(val title: String, val icon: ImageVector)

private val SettingsSections = listOf(
    SettingsSection("الموقع", Icons.Default.LocationOn),
    SettingsSection("الوقت والتاريخ", Icons.Default.Schedule),
    SettingsSection("الصلاة والأذان", Icons.Default.Mosque),
    SettingsSection("التنبيهات", Icons.Default.Notifications),
    SettingsSection("المظهر والقراءة", Icons.Default.Palette),
    SettingsSection("الأذكار والمسبحة", Icons.Default.AutoAwesome),
    SettingsSection("الصوت والاهتزاز", Icons.Default.VolumeUp),
    SettingsSection("البيانات والتخزين", Icons.Default.Storage),
    SettingsSection("حول التطبيق", Icons.Default.Info)
)

@Composable
fun SettingsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val listState = rememberLazyListState()
    val chipsState = rememberLazyListState()

    // الانتقال التلقائي للقسم المطلوب عند فتح الإعدادات من شاشة أخرى
    LaunchedEffect(nav.settingsSection) {
        if (nav.settingsSection > 0) {
            delay(250)
            listState.animateScrollToItem(1 + nav.settingsSection.coerceIn(0, SettingsSections.lastIndex))
        }
    }

    val activeSection by remember {
        derivedStateOf { (listState.firstVisibleItemIndex - 1).coerceIn(0, SettingsSections.lastIndex) }
    }
    LaunchedEffect(activeSection) { chipsState.animateScrollToItem(activeSection) }

    // حوارات مشتركة
    var showCityDialog by remember { mutableStateOf(false) }
    var showCoordsDialog by remember { mutableStateOf(false) }
    var showMuezzinDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showClearAudioDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }

    var cachedFilesCount by remember { mutableIntStateOf(AudioCache.count(ctx)) }
    var cachedSizeMb by remember { mutableLongStateOf(AudioCache.sizeMb(ctx)) }

    fun reschedule() = AdhanEngine.scheduleAll(ctx, st)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "الإعدادات",
            subtitle = "تحكّم كامل بالموقع والوقت والمظهر والتنبيهات",
            onBack = { nav.back() }
        )

        // شريط تنقل الأقسام: اضغط لتنتقل بسلاسة إلى القسم
        LazyRow(
            state = chipsState,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(SettingsSections) { i, s ->
                val sel = i == activeSection
                val bg by animateColorAsState(if (sel) EmeraldVibrant else p.card, label = "settingsChipBg")
                Surface(
                    onClick = {
                        fx.click()
                        scope.launch { listState.animateScrollToItem(1 + i) }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = bg,
                    border = BorderStroke(1.2.dp, if (sel) GoldShine else p.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(s.icon, null, tint = if (sel) GoldShine else GoldWarm, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.title, color = if (sel) Color.White else p.ink, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "summary") { SettingsSummary() }

            item(key = "sec_location") {
                SectionCard(SettingsSections[0], "حدّد موقعك لتُحسب مواقيت الصلاة والقبلة بدقة") {
                    LocationSection(
                        onPickCity = { showCityDialog = true },
                        onPickCoords = { showCoordsDialog = true },
                        onChanged = { reschedule() }
                    )
                }
            }
            item(key = "sec_time") {
                SectionCard(SettingsSections[1], "اضبط الوقت يدوياً أو اتركه تلقائياً من هاتفك، وصحّح المواقيت والتاريخ الهجري") {
                    TimeSection(onChanged = { reschedule() })
                }
            }
            item(key = "sec_prayer") {
                SectionCard(SettingsSections[2], "طريقة الحساب والمذهب وصوت المؤذن") {
                    PrayerSection(
                        nav = nav,
                        onMethod = { showMethodDialog = true },
                        onMuezzin = { showMuezzinDialog = true },
                        onChanged = { reschedule() }
                    )
                }
            }
            item(key = "sec_alerts") {
                SectionCard(SettingsSections[3], "تذكيرات الورد اليومي والأذكار والإشعارات") {
                    AlertsSection(onChanged = { reschedule() })
                }
            }
            item(key = "sec_appearance") {
                SectionCard(SettingsSections[4], "راحة العين أثناء القراءة") {
                    AppearanceSection()
                }
            }
            item(key = "sec_adhkar") {
                SectionCard(SettingsSections[5], "حجم الخط والاهتزاز وإعادة ضبط تقدّم اليوم") {
                    AdhkarSection()
                }
            }
            item(key = "sec_sound") {
                SectionCard(SettingsSections[6], "أصوات النقر واهتزاز الجهاز") {
                    SoundSection()
                }
            }
            item(key = "sec_data") {
                SectionCard(SettingsSections[7], "إدارة التلاوات المحمّلة وإحصائيات القراءة") {
                    DataSection(
                        filesCount = cachedFilesCount,
                        sizeMb = cachedSizeMb,
                        onClearAudio = { showClearAudioDialog = true },
                        onResetStats = { showResetStatsDialog = true }
                    )
                }
            }
            item(key = "sec_about") {
                SectionCard(SettingsSections[8], "تطبيق القرآن الكريم") {
                    AboutSection()
                }
            }
        }
    }

    if (showCityDialog) LocationPickerDialog { showCityDialog = false }
    if (showCoordsDialog) ManualCoordsDialog(onDismiss = { showCoordsDialog = false }, onSaved = { showCoordsDialog = false })
    if (showMuezzinDialog) MuezzinPickerDialog { showMuezzinDialog = false }
    if (showMethodDialog) MethodPickerDialog { showMethodDialog = false }

    if (showClearAudioDialog) {
        AlertDialog(
            onDismissRequest = { showClearAudioDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("حذف التلاوات المحملة؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم حذف جميع الملفات الصوتية المحفوظة محلياً لتوفير مساحة التخزين، ويمكنك إعادة تنزيلها في أي وقت.") },
            confirmButton = {
                Button(
                    onClick = {
                        PlayerService.clearAll(ctx)
                        cachedFilesCount = 0
                        cachedSizeMb = 0
                        showClearAudioDialog = false
                        Toast.makeText(ctx, "تم تفريغ التلاوات المحملة ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("حذف الكل", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showClearAudioDialog = false }) { Text("إلغاء") } }
        )
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تصفير الإحصائيات؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم تصفير عداد الختمة وإجمالي التسبيحات وسلسلة الأيام فقط، دون حذف العلامات المرجعية.") },
            confirmButton = {
                Button(
                    onClick = {
                        st.resetStats()
                        showResetStatsDialog = false
                        Toast.makeText(ctx, "تمت إعادة الضبط بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) { Text("تأكيد", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showResetStatsDialog = false }) { Text("إلغاء") } }
        )
    }
}

// ------------------------------------------------------------
// ملخص علوي: الموقع الحالي + الوقت المعتمد + الإنجاز
// ------------------------------------------------------------
@Composable
private fun SettingsSummary() {
    val st = LocalApp.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(15_000L); nowMs = System.currentTimeMillis() }
    }
    val cal = AppClock.calendar(st, nowMs)

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = RoyalForest,
        border = BorderStroke(1.2.dp, GoldWarm.copy(alpha = 0.7f)),
        shadowElevation = 6.dp
    ) {
        Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
            IslamicPattern(Modifier.matchParentSize())
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${st.geo.name} • ${if (st.locationMode == 0) "موقع تلقائي" else "موقع يدوي"}",
                        color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${AppClock.formatClock(cal, st.use24h)} • ${AppClock.utcLabel(cal.timeZone.getOffset(cal.timeInMillis) / 60000)} • ${if (st.timeMode == 0) "من الجهاز" else "يدوي"}",
                        color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    SummaryStatItem("${st.khatmaPercent()}%", "الختمة", Icons.Default.AutoStories)
                    SummaryStatItem("${st.streak}", "أيام متتالية", Icons.Default.LocalFireDepartment)
                    SummaryStatItem("${st.totalTasbih}", "تسبيحة", Icons.Default.Spa)
                }
            }
        }
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = GoldShine, modifier = Modifier.size(20.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
    }
}

// ------------------------------------------------------------
// 2. مكونات الإعدادات المشتركة
// ------------------------------------------------------------
@Composable
private fun SectionCard(section: SettingsSection, hint: String, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    QCard(shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant)))
                        .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                ) { Icon(section.icon, null, tint = GoldShine, modifier = Modifier.size(22.dp)) }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(section.title, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = p.ink)
                    Text(hint, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted, lineHeight = 15.sp)
                }
            }
            HorizontalDivider(color = p.border.copy(alpha = 0.6f))
            content()
        }
    }
}

@Composable
private fun RowShell(
    icon: ImageVector,
    accent: Color,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable { fx.click(); onClick() } else Modifier)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.14f))
        ) { Icon(icon, null, tint = accent, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
            if (subtitle != null) Text(subtitle, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun SwitchRow(icon: ImageVector, title: String, subtitle: String?, checked: Boolean, accent: Color = EmeraldVibrant, onChange: (Boolean) -> Unit) {
    val fx = LocalFx.current
    RowShell(icon, accent, title, subtitle, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = { fx.click(); onChange(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = GoldShine, checkedTrackColor = EmeraldVibrant)
        )
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String?, value: String? = null, accent: Color = EmeraldVibrant, onClick: () -> Unit) {
    val p = LocalPalette.current
    RowShell(icon, accent, title, subtitle, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = accent)
            Icon(Icons.Default.ChevronLeft, null, tint = p.muted)
        }
    }
}

@Composable
private fun StepperRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    valueText: String,
    accent: Color = EmeraldVibrant,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    RowShell(icon, accent, title, subtitle, onClick = null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledIconButton(
                onClick = { fx.click(); onPlus() },
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)
            ) { Icon(Icons.Default.Add, "زيادة", tint = Color.White, modifier = Modifier.size(18.dp)) }
            Text(
                valueText,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = p.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 62.dp)
            )
            FilledIconButton(
                onClick = { fx.click(); onMinus() },
                modifier = Modifier.size(32.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = accent)
            ) { Icon(Icons.Default.Remove, "إنقاص", tint = Color.White, modifier = Modifier.size(18.dp)) }
        }
    }
}

@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.bg)
            .border(1.dp, p.border, RoundedCornerShape(14.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) EmeraldVibrant else Color.Transparent, label = "segBg")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(bg)
                    .clickable { fx.click(); onSelect(i) }
                    .padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Text(
                    label,
                    color = if (sel) Color.White else p.ink,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SliderRow(icon: ImageVector, title: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = EmeraldVibrant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink, modifier = Modifier.weight(1f))
            Text(valueText, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = EmeraldVibrant)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = GoldWarm, activeTrackColor = EmeraldVibrant)
        )
    }
}

@Composable
private fun InfoNote(text: String, danger: Boolean = false) {
    val p = LocalPalette.current
    Text(
        text,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        color = if (danger) DangerRed else p.muted,
        lineHeight = 16.sp
    )
}

private fun signed(v: Int): String = when {
    v > 0 -> "+$v د"
    v < 0 -> "$v د"
    else -> "0 د"
}

// ------------------------------------------------------------
// 3. قسم الموقع: تلقائي (GPS) أو يدوي (مدينة / إحداثيات)
// ------------------------------------------------------------
@Composable
private fun ColumnScope.LocationSection(onPickCity: () -> Unit, onPickCoords: () -> Unit, onChanged: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    var status by remember { mutableStateOf<String?>(null) }
    var statusOk by remember { mutableStateOf(true) }
    var gpsOff by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }

    val detect = rememberAutoLocationAction { s, name ->
        busy = false
        tick++
        gpsOff = s == LocStatus.GPS_OFF
        statusOk = s == LocStatus.OK
        status = locStatusMessage(s, name)
        if (s == LocStatus.OK) st.locationMode = 0
    }

    Segmented(
        options = listOf("تلقائي (GPS)", "يدوي"),
        selected = st.locationMode,
        onSelect = { idx ->
            st.locationMode = idx
            if (idx == 0) {
                busy = true
                status = "جارٍ تحديد موقعك..."
                statusOk = true
                detect()
            } else {
                onChanged()
            }
        }
    )

    // الموقع الحالي
    Surface(shape = RoundedCornerShape(16.dp), color = p.bg, border = BorderStroke(1.dp, p.border)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MyLocation, null, tint = EmeraldVibrant)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(st.geo.name, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                Text(
                    String.format(Locale.US, "%.4f° ، %.4f°", st.geo.lat, st.geo.lng),
                    fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = p.muted
                )
            }
        }
    }

    if (st.locationMode == 0) {
        val permOk = remember(tick) { LocationHelper.hasPermission(ctx) }
        val gpsOn = remember(tick) { LocationHelper.isServiceEnabled(ctx) }
        InfoNote("إذن الموقع: ${if (permOk) "ممنوح ✓" else "غير ممنوح"}   •   خدمة GPS: ${if (gpsOn) "مفعّلة ✓" else "متوقفة"}")

        Button(
            onClick = { fx.click(); busy = true; status = "جارٍ تحديد موقعك..."; statusOk = true; detect() },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = RoyalForest, strokeWidth = 2.dp)
            else Icon(Icons.Default.Refresh, null, tint = RoyalForest)
            Spacer(Modifier.width(8.dp))
            Text("تحديث موقعي الآن", color = RoyalForest, fontWeight = FontWeight.ExtraBold)
        }

        if (status != null) InfoNote(status!!, danger = !statusOk)
        if (gpsOff) {
            OutlinedButton(onClick = { LocationHelper.openLocationSettings(ctx) }, shape = RoundedCornerShape(12.dp)) {
                Text("فتح إعدادات الموقع في الهاتف", fontWeight = FontWeight.ExtraBold)
            }
        }
        SwitchRow(
            Icons.Default.Autorenew,
            "تحديث الموقع تلقائياً",
            "يُحدَّث موقعك عند فتح التطبيق إن مرّت 6 ساعات (مفيد عند السفر)",
            st.autoLocRefresh
        ) { st.autoLocRefresh = it }
    } else {
        ActionRow(Icons.Default.LocationCity, "اختيار مدينة", "اختر من قائمة تضم أكثر من 50 مدينة مع بحث", st.geo.name, onClick = onPickCity)
        ActionRow(Icons.Default.EditLocationAlt, "إدخال الإحداثيات", "اكتب الاسم وخطي العرض والطول بنفسك لأي مكان في العالم", onClick = onPickCoords, accent = TurquoiseAccent)
    }
}

// ------------------------------------------------------------
// 4. قسم الوقت والتاريخ: يدوي أو تلقائي + تصحيحات المواقيت + الهجري
// ------------------------------------------------------------
@Composable
private fun ColumnScope.TimeSection(onChanged: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { delay(10_000L); nowMs = System.currentTimeMillis() }
    }
    val cal = AppClock.calendar(st, nowMs)
    var showPerPrayer by remember { mutableStateOf(false) }

    Segmented(
        options = listOf("تلقائي من الهاتف", "ضبط يدوي"),
        selected = st.timeMode,
        onSelect = { st.timeMode = it; onChanged() }
    )

    Surface(shape = RoundedCornerShape(16.dp), color = p.bg, border = BorderStroke(1.dp, p.border)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("الوقت المعتمد في التطبيق الآن", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = p.muted)
            Text(AppClock.formatClock(cal, st.use24h), fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = EmeraldVibrant)
            Text(
                "${ArabicDate.weekday(cal)} • ${HijriCalendar.format(cal, st.hijriOffset)} • ${ArabicDate.gregorian(cal, st.monthStyle)}",
                fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = p.ink
            )
        }
    }

    if (st.timeMode == 1) {
        StepperRow(
            Icons.Default.Public, "المنطقة الزمنية (UTC)",
            "اضبطها لتوافق بلد الموقع المختار، كل ضغطة نصف ساعة",
            AppClock.utcLabel(st.manualUtcMin), TurquoiseAccent,
            onMinus = { st.manualUtcMin -= 30; onChanged() },
            onPlus = { st.manualUtcMin += 30; onChanged() }
        )
        ActionRow(
            Icons.Default.PhoneAndroid, "مطابقة توقيت الهاتف", "اضبط الإزاحة على توقيت جهازك الحالي",
            onClick = {
                st.manualUtcMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
                onChanged()
            }
        )
    } else {
        InfoNote("الوضع التلقائي يعتمد توقيت هاتفك ومنطقته الزمنية، وهو الأنسب لمعظم المستخدمين.")
    }

    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

    StepperRow(
        Icons.Default.Tune, "تصحيح عام للمواقيت",
        "تقديم أو تأخير جميع الصلوات بالدقائق لمطابقة تقويم مسجدك",
        signed(st.minuteAdjust),
        onMinus = { st.minuteAdjust -= 1; onChanged() },
        onPlus = { st.minuteAdjust += 1; onChanged() }
    )

    ActionRow(
        Icons.Default.Timer, "تصحيح كل صلاة على حدة", "ضبط دقيق لكل وقت منفرداً",
        value = if (showPerPrayer) "إخفاء" else "عرض",
        onClick = { showPerPrayer = !showPerPrayer }
    )
    AnimatedVisibility(
        visible = showPerPrayer,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val times = PrayerCalc.timesFor(st, cal)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StepperRow(Icons.Default.NightsStay, "الفجر • ${PrayerCalc.formatTime(times.fajr, st.use24h)}", null, signed(st.adjFajr),
                onMinus = { st.adjFajr -= 1; onChanged() }, onPlus = { st.adjFajr += 1; onChanged() })
            StepperRow(Icons.Default.WbTwilight, "الشروق • ${PrayerCalc.formatTime(times.sunrise, st.use24h)}", null, signed(st.adjSunrise),
                onMinus = { st.adjSunrise -= 1; onChanged() }, onPlus = { st.adjSunrise += 1; onChanged() })
            StepperRow(Icons.Default.WbSunny, "الظهر • ${PrayerCalc.formatTime(times.dhuhr, st.use24h)}", null, signed(st.adjDhuhr),
                onMinus = { st.adjDhuhr -= 1; onChanged() }, onPlus = { st.adjDhuhr += 1; onChanged() })
            StepperRow(Icons.Default.LightMode, "العصر • ${PrayerCalc.formatTime(times.asr, st.use24h)}", null, signed(st.adjAsr),
                onMinus = { st.adjAsr -= 1; onChanged() }, onPlus = { st.adjAsr += 1; onChanged() })
            StepperRow(Icons.Default.Brightness4, "المغرب • ${PrayerCalc.formatTime(times.maghrib, st.use24h)}", null, signed(st.adjMaghrib),
                onMinus = { st.adjMaghrib -= 1; onChanged() }, onPlus = { st.adjMaghrib += 1; onChanged() })
            StepperRow(Icons.Default.Bedtime, "العشاء • ${PrayerCalc.formatTime(times.isha, st.use24h)}", null, signed(st.adjIsha),
                onMinus = { st.adjIsha -= 1; onChanged() }, onPlus = { st.adjIsha += 1; onChanged() })
            TextButton(onClick = { st.resetTimeAdjustments(); onChanged() }) {
                Text("تصفير كل التصحيحات", color = DangerRed, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

    StepperRow(
        Icons.Default.CalendarMonth, "تصحيح التاريخ الهجري",
        "إن اختلف التاريخ الهجري عن بلدك صحّحه بيوم أو يومين",
        when {
            st.hijriOffset > 0 -> "+${st.hijriOffset} يوم"
            st.hijriOffset < 0 -> "${st.hijriOffset} يوم"
            else -> "0 يوم"
        },
        GoldWarm,
        onMinus = { st.hijriOffset -= 1 },
        onPlus = { st.hijriOffset += 1 }
    )
    SwitchRow(Icons.Default.AccessTime, "نظام 24 ساعة", "عرض الوقت بصيغة 14:30 بدل 2:30 م", st.use24h) { st.use24h = it }
    Text("أسماء الأشهر الميلادية", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.ink)
    Segmented(
        options = listOf("يناير • فبراير", "كانون الثاني • شباط"),
        selected = st.monthStyle,
        onSelect = { st.monthStyle = it }
    )
}

// ------------------------------------------------------------
// 5. قسم الصلاة والأذان
// ------------------------------------------------------------
@Composable
private fun ColumnScope.PrayerSection(nav: Nav, onMethod: () -> Unit, onMuezzin: () -> Unit, onChanged: () -> Unit) {
    val st = LocalApp.current
    val ctx = LocalContext.current
    val isAdhanPlaying by AdhanEngine.isPlaying.collectAsState()
    val selectedId by AdhanEngine.activeMuezzinId.collectAsState()
    val muezzin = remember(selectedId) { AdhanEngine.getSelectedMuezzin(ctx) }

    ActionRow(
        Icons.Default.Calculate, "طريقة حساب المواقيت", "اختر الجهة المعتمدة في بلدك",
        PrayerCalc.methodNames[st.prayerMethod.coerceIn(0, PrayerCalc.methodNames.lastIndex)].substringBefore(" ("),
        onClick = onMethod
    )
    Text("وقت العصر", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = LocalPalette.current.ink)
    Segmented(
        options = listOf("الجمهور (ظل = المثل)", "الحنفي (ظل = المثلان)"),
        selected = if (st.asrHanafi) 1 else 0,
        onSelect = { st.asrHanafi = it == 1; onChanged() }
    )
    SwitchRow(
        Icons.Default.NotificationsActive, "الأذان التلقائي",
        "يُرفع الأذان بصوت المؤذن عند دخول وقت كل صلاة حتى والتطبيق مغلق",
        st.prayerAlert
    ) { enabled ->
        st.prayerAlert = enabled
        if (enabled) AdhanEngine.scheduleAll(ctx, st) else AdhanEngine.cancelAll(ctx)
    }
    ActionRow(Icons.Default.RecordVoiceOver, "صوت المؤذن", "${Top15Muezzins.size} مؤذناً للاختيار", muezzin.name, accent = TurquoiseAccent, onClick = onMuezzin)
    ActionRow(
        if (isAdhanPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
        if (isAdhanPlaying) "إيقاف الأذان" else "تجربة صوت الأذان", "استمع لصوت المؤذن المختار الآن",
        accent = if (isAdhanPlaying) DangerRed else GoldWarm,
        onClick = { if (isAdhanPlaying) AdhanEngine.stopAdhan(ctx) else AdhanEngine.playAdhanNow(ctx, "التجربة") }
    )
    ActionRow(Icons.Default.Explore, "مواقيت اليوم والقبلة", "افتح شاشة المواقيت الكاملة", onClick = { nav.push(Screen.Prayer) })
}

// ------------------------------------------------------------
// 6. قسم التنبيهات
// ------------------------------------------------------------
@Composable
private fun ColumnScope.AlertsSection(onChanged: () -> Unit) {
    val st = LocalApp.current
    val ctx = LocalContext.current

    SwitchRow(
        Icons.Default.AutoStories, "تذكير الورد اليومي",
        "إشعار يومي يذكّرك بقراءة وردك من القرآن",
        st.dailyReminder
    ) { st.dailyReminder = it; onChanged() }

    if (st.dailyReminder) {
        ActionRow(
            Icons.Default.Alarm, "وقت التذكير", "اضغط لاختيار الساعة والدقيقة",
            String.format(Locale.US, "%02d:%02d", st.reminderHour, st.reminderMinute),
            onClick = {
                TimePickerDialog(ctx, { _, h, m ->
                    st.reminderHour = h
                    st.reminderMinute = m
                    onChanged()
                }, st.reminderHour, st.reminderMinute, true).show()
            }
        )
    }

    StepperRow(
        Icons.Default.Flag, "هدف الورد اليومي", "عدد الصفحات التي تريد قراءتها كل يوم",
        "${st.dailyGoal} صفحات",
        onMinus = { st.dailyGoal -= 1 },
        onPlus = { st.dailyGoal += 1 }
    )

    SwitchRow(
        Icons.Default.AutoAwesome, "تذكير الأذكار",
        "أذكار الصباح بعد الفجر بنصف ساعة، وأذكار المساء بعد العصر بنصف ساعة",
        st.adhkarAlert, GoldWarm
    ) { st.adhkarAlert = it; onChanged() }

    ActionRow(
        Icons.Default.NotificationsNone, "إشعارات التطبيق في النظام",
        "إن لم تصلك التنبيهات تأكد من تفعيلها من إعدادات الهاتف",
        accent = TurquoiseAccent,
        onClick = {
            runCatching {
                ctx.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    )
}

// ------------------------------------------------------------
// 7. قسم المظهر والقراءة
// ------------------------------------------------------------
@Composable
private fun ColumnScope.AppearanceSection() {
    val st = LocalApp.current
    val p = LocalPalette.current

    SwitchRow(Icons.Default.DarkMode, "الوضع الليلي", "ألوان داكنة مريحة للعين في الإضاءة المنخفضة", st.nightMode) { st.nightMode = it }
    SwitchRow(Icons.Default.Description, "ورق المصحف الدافئ", "لون كريمي هادئ لصفحة المصحف بدل الأبيض الصريح", st.paperWarmth, GoldWarm) { st.paperWarmth = it }
    SwitchRow(Icons.Default.ScreenLockPortrait, "إبقاء الشاشة مضاءة", "لا تنطفئ الشاشة أثناء قراءة المصحف", st.keepOn) { st.keepOn = it }
    SliderRow(
        Icons.Default.FormatSize, "حجم خط المصحف", "${(st.mushafFontScale * 100).toInt()}%",
        st.mushafFontScale, 0.8f..1.5f
    ) { st.mushafFontScale = it }
    Surface(shape = RoundedCornerShape(16.dp), color = p.paper, border = BorderStroke(1.dp, p.border)) {
        Text(
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            fontFamily = UthmanicFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (24 * st.mushafFontScale).sp,
            color = p.mushafInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        )
    }
}

// ------------------------------------------------------------
// 8. قسم الأذكار والمسبحة
// ------------------------------------------------------------
@Composable
private fun ColumnScope.AdhkarSection() {
    val st = LocalApp.current
    val p = LocalPalette.current
    val ctx = LocalContext.current

    SliderRow(
        Icons.Default.TextFields, "حجم خط الأذكار", "${(st.adhkarFontScale * 100).toInt()}%",
        st.adhkarFontScale, 0.8f..1.5f
    ) { st.adhkarFontScale = it }
    SliderRow(
        Icons.Default.Spa, "حجم خط المسبحة", "${(st.tasbihFontScale * 100).toInt()}%",
        st.tasbihFontScale, 0.8f..1.6f
    ) { st.tasbihFontScale = it }
    Surface(shape = RoundedCornerShape(16.dp), color = RoyalForest) {
        Text(
            "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
            fontFamily = UthmanicFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (30 * st.tasbihFontScale).sp,
            color = GoldShine,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(14.dp)
        )
    }
    SwitchRow(
        Icons.Default.Vibration, "اهتزاز عند إتمام العدد",
        "اهتزاز أقوى عند إكمال الجولة في المسبحة أو الذكر في الأذكار",
        st.stageVib
    ) { st.stageVib = it }
    ActionRow(
        Icons.Default.RestartAlt, "إعادة ضبط أذكار اليوم", "تصفير عدادات جميع الأذكار لتبدأ من جديد",
        accent = DangerRed,
        onClick = {
            st.adhkarResetAll()
            Toast.makeText(ctx, "تمت إعادة ضبط أذكار اليوم ✓", Toast.LENGTH_SHORT).show()
        }
    )
    InfoNote("تُصفَّر عدادات الأذكار تلقائياً كل يوم جديد.")
}

// ------------------------------------------------------------
// 9. قسم الصوت والاهتزاز
// ------------------------------------------------------------
@Composable
private fun ColumnScope.SoundSection() {
    val st = LocalApp.current
    SwitchRow(Icons.Default.VolumeUp, "أصوات النقر", "صوت خفيف عند الضغط على الأزرار والمسبحة", st.soundOn) { st.soundOn = it }
    SwitchRow(Icons.Default.Vibration, "الاهتزاز", "اهتزاز خفيف عند التفاعل مع التطبيق", st.vibOn, GoldWarm) { st.vibOn = it }
}

// ------------------------------------------------------------
// 10. قسم البيانات والتخزين
// ------------------------------------------------------------
@Composable
private fun ColumnScope.DataSection(filesCount: Int, sizeMb: Long, onClearAudio: () -> Unit, onResetStats: () -> Unit) {
    val st = LocalApp.current
    ActionRow(
        Icons.Default.Headphones, "التلاوات المحمّلة", "$filesCount سورة • $sizeMb ميغابايت — اضغط لحذفها وتوفير المساحة",
        accent = TurquoiseAccent, onClick = onClearAudio
    )
    ActionRow(
        Icons.Default.DeleteSweep, "تصفير الإحصائيات",
        "الختمة (${st.readPages.size} صفحة) والتسبيح (${st.totalTasbih}) وسلسلة الأيام",
        accent = DangerRed, onClick = onResetStats
    )
}

// ------------------------------------------------------------
// 11. حول التطبيق
// ------------------------------------------------------------
@Composable
private fun ColumnScope.AboutSection() {
    val p = LocalPalette.current
    Text(
        "تطبيق القرآن الكريم: مصحف كامل، تلاوات بأصوات ${QuranData.reciters.size} قارئاً، مواقيت الصلاة والأذان والقبلة، أذكار شاملة، ومسبحة إلكترونية.",
        fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = p.ink, lineHeight = 19.sp
    )
    InfoNote("نصوص الأذكار منقولة من كتب الأذكار المعتمدة، ويُستحسن مراجعتها مع مصدر موثوق. تقبّل الله منا ومنكم صالح الأعمال.")
}

// ============================================================
// 2. مركز الحفظ والمراجعة التفاعلي (HifzScreen)
// ============================================================
@Composable
fun HifzScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current

    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableIntStateOf(0) } // 0: الكل، 1: المحفوظة، 2: قيد الحفظ
    var quizSurah by remember { mutableStateOf(QuranData.surahs.random()) }
    var quizAnswerRevealed by remember { mutableStateOf(false) }

    val filteredSurahs = remember(searchQuery, filterMode, st.memorizedSurahs.size) {
        QuranData.search(searchQuery).filter { s ->
            when (filterMode) {
                1 -> s.num in st.memorizedSurahs
                2 -> s.num !in st.memorizedSurahs
                else -> true
            }
        }
    }

    val memorizedPct = ((st.memorizedSurahs.size * 100) / 114).coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader("مركز الحفظ والمراجعة", onBack = { nav.back() })

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. بطاقة التقدم في الحفظ
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RoyalForest,
                    border = BorderStroke(1.3.dp, GoldWarm.copy(alpha = 0.65f))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))
                    ) {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("سجل الحفظ القرآني", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text("${st.memorizedSurahs.size} / 114 سورة ($memorizedPct%)", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { st.memorizedSurahs.size / 114f },
                                color = GoldShine,
                                trackColor = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                            )
                        }
                    }
                }
            }

            // 2. بطاقة اختبار الحفظ السريع
            item {
                QCard(
                    shape = RoundedCornerShape(22.dp),
                    borderColor = TurquoiseAccent.copy(alpha = 0.55f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Quiz, null, tint = TurquoiseAccent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("اختبار المعلومات القرآنية", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            }
                            IconButton(
                                onClick = {
                                    fx.click()
                                    quizSurah = QuranData.surahs.random()
                                    quizAnswerRevealed = false
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, "سؤال جديد", tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = "ما هو ترتيب سورة «${quizSurah.name}» في المصحف، وهل هي مكية أم مدنية، وكم عدد آياتها؟",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = p.ink
                        )

                        if (quizAnswerRevealed) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = EmeraldVibrant.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, EmeraldVibrant)
                            ) {
                                Text(
                                    text = "ترتيبها: ${quizSurah.num} • نوعها: ${quizSurah.type} • آياتها: ${quizSurah.ayahs} آية • تبدأ صفحة ${quizSurah.page}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = EmeraldVibrant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { fx.click(); quizAnswerRevealed = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TurquoiseAccent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إظهار الإجابة", fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. شريط البحث والتصفية لتحديد السور المحفوظة
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("ابحث لتحديد السور التي أتممت حفظها...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("الكل (114)", "المحفوظة (${st.memorizedSurahs.size})", "قيد الحفظ").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = filterMode == idx,
                                onClick = { fx.click(); filterMode = idx },
                                label = { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldVibrant,
                                    selectedLabelColor = GoldShine
                                )
                            )
                        }
                    }
                }
            }

            // 4. قائمة السور مع زر تحديد الحفظ بنقرة واحدة
            items(filteredSurahs, key = { it.num }) { s ->
                val isMemorized = s.num in st.memorizedSurahs
                val cardBg by animateColorAsState(
                    targetValue = if (isMemorized) EmeraldVibrant.copy(alpha = 0.12f) else p.card,
                    label = "hifzCard"
                )
                QCard(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    borderColor = if (isMemorized) GoldWarm else p.border,
                    onClick = { st.toggleMemorizedSurah(s.num) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberBadge(s.num, 36)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سُورَةُ ${s.name}",
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = p.ink
                            )
                            Text(
                                text = "${s.ayahs} آية • صفحة ${s.page}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = p.muted
                            )
                        }
                        IconButton(onClick = { fx.click(); nav.openPage(s.page) }) {
                            Icon(Icons.AutoMirrored.Filled.MenuBook, "اقرأ", tint = TurquoiseAccent, modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            imageVector = if (isMemorized) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "حالة الحفظ",
                            tint = if (isMemorized) GoldWarm else p.muted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
