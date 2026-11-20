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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
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

private data class SettingsCategoryMeta(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val SettingsCategories = listOf(
    SettingsCategoryMeta("الموقع الجغرافي", "تحديد المدينة أو GPS لحساب المواقيت بدقة", Icons.Rounded.LocationOn),
    SettingsCategoryMeta("الوقت والتاريخ", "الضبط اليدوي والتلقائي والتاريخ الهجري", Icons.Rounded.Schedule),
    SettingsCategoryMeta("الصلاة والأذان", "المؤذن وطريقة الحساب والمذهب", Icons.Rounded.Mosque),
    SettingsCategoryMeta("التنبيهات والورد", "تذكير الورد اليومي والأذكار", Icons.Rounded.NotificationsActive),
    SettingsCategoryMeta("المظهر والقراءة", "الوضع الليلي وورق المصحف وحجم الخط", Icons.Rounded.Palette),
    SettingsCategoryMeta("الأذكار والمسبحة", "حجم الخط والاهتزاز وتصفير اليوم", Icons.Rounded.AutoAwesome),
    SettingsCategoryMeta("الصوت والاهتزاز", "أصوات النقر والاهتزاز التفاعلي", Icons.Rounded.VolumeUp),
    SettingsCategoryMeta("البيانات والتخزين", "إدارة التلاوات المحملة والإحصائيات", Icons.Rounded.Storage),
    SettingsCategoryMeta("حول التطبيق", "معلومات التطبيق والإصدار", Icons.Rounded.Info)
)

// ============================================================
// 1. شاشة الإعدادات: تعرض العناوين الأساسية فقط، وعند الضغط يفتح القسم وحده
// ============================================================
@Composable
fun SettingsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    // -1 = القائمة الرئيسية للعناوين الأساسية، 0..8 = شاشة إعدادات ذلك القسم فقط
    var openedCategory by remember { mutableIntStateOf(nav.settingsSection) }

    LaunchedEffect(nav.settingsSection) {
        openedCategory = nav.settingsSection
    }

    var showCityDialog by remember { mutableStateOf(false) }
    var showCoordsDialog by remember { mutableStateOf(false) }
    var showMuezzinDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showClearAudioDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }

    var cachedFilesCount by remember { mutableIntStateOf(AudioCache.count(ctx)) }
    var cachedSizeMb by remember { mutableLongStateOf(AudioCache.sizeMb(ctx)) }

    fun reschedule() = AdhanEngine.scheduleAll(ctx, st)

    val currentMeta = SettingsCategories.getOrNull(openedCategory)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = currentMeta?.title ?: "الإعدادات",
            subtitle = currentMeta?.subtitle ?: "اختر القسم لتعديل إعداداته",
            onBack = {
                if (openedCategory >= 0) {
                    openedCategory = -1
                    nav.settingsSection = -1
                } else {
                    nav.back()
                }
            }
        )

        if (openedCategory < 0) {
            // القائمة الرئيسية: الأسماء الأساسية فقط بشكل نظيف ومنظم
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(SettingsCategories) { index, cat ->
                    QCard(
                        shape = RoundedCornerShape(20.dp),
                        onClick = { openedCategory = index }
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
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant)))
                                    .border(1.dp, GoldWarm.copy(alpha = 0.7f), RoundedCornerShape(13.dp))
                            ) {
                                Icon(cat.icon, null, tint = GoldShine, modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = p.ink)
                                Text(cat.subtitle, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = p.muted)
                            }
                            Icon(Icons.Rounded.ChevronLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        } else {
            // شاشة القسم المختار فقط
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QCard(shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        when (openedCategory) {
                            0 -> LocationSection(
                                onPickCity = { showCityDialog = true },
                                onPickCoords = { showCoordsDialog = true },
                                onChanged = { reschedule() }
                            )
                            1 -> TimeSection(onChanged = { reschedule() })
                            2 -> PrayerSection(
                                nav = nav,
                                onMethod = { showMethodDialog = true },
                                onMuezzin = { showMuezzinDialog = true },
                                onChanged = { reschedule() }
                            )
                            3 -> AlertsSection(onChanged = { reschedule() })
                            4 -> AppearanceSection()
                            5 -> AdhkarSection()
                            6 -> SoundSection()
                            7 -> DataSection(
                                filesCount = cachedFilesCount,
                                sizeMb = cachedSizeMb,
                                onClearAudio = { showClearAudioDialog = true },
                                onResetStats = { showResetStatsDialog = true }
                            )
                            8 -> AboutSection()
                        }
                    }
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
            text = { Text("سيتم حذف جميع الملفات الصوتية المحفوظة محلياً لتوفير مساحة التخزين.") },
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
            text = { Text("سيتم تصفير عداد الختمة وإجمالي التسبيحات وسلسلة الأيام فقط.") },
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
            Icon(Icons.Rounded.ChevronLeft, null, tint = p.muted)
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
            ) { Icon(Icons.Rounded.Add, "زيادة", tint = Color.White, modifier = Modifier.size(18.dp)) }
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
            ) { Icon(Icons.Rounded.Remove, "إنقاص", tint = Color.White, modifier = Modifier.size(18.dp)) }
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
            val bg by animateColorAsState(if (sel) GoldShine else Color.Transparent, label = "segBg")
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
                    color = if (sel) RoyalForest else p.ink,
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

    val detect = rememberAutoLocationAction { s, name ->
        busy = false
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

    Surface(shape = RoundedCornerShape(16.dp), color = p.bg, border = BorderStroke(1.dp, p.border)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MyLocation, null, tint = EmeraldVibrant)
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
        Button(
            onClick = { fx.click(); busy = true; status = "جارٍ تحديد موقعك..."; statusOk = true; detect() },
            enabled = !busy,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldWarm),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = RoyalForest, strokeWidth = 2.dp)
            else Icon(Icons.Rounded.Refresh, null, tint = RoyalForest)
            Spacer(Modifier.width(8.dp))
            Text("تحديث موقعي الآن", color = RoyalForest, fontWeight = FontWeight.ExtraBold)
        }
        if (status != null) InfoNote(status!!, danger = !statusOk)
        if (gpsOff) {
            OutlinedButton(onClick = { LocationHelper.openLocationSettings(ctx) }, shape = RoundedCornerShape(12.dp)) {
                Text("فتح إعدادات الموقع في الهاتف", fontWeight = FontWeight.ExtraBold)
            }
        }
        SwitchRow(Icons.Rounded.Autorenew, "تحديث الموقع تلقائياً", "يُحدَّث عند فتح التطبيق إن مرّت 6 ساعات", st.autoLocRefresh) { st.autoLocRefresh = it }
    } else {
        ActionRow(Icons.Rounded.LocationCity, "اختيار مدينة", "قائمة تضم أكثر من 50 مدينة مع بحث", st.geo.name, onClick = onPickCity)
        ActionRow(Icons.Rounded.EditLocationAlt, "إدخال الإحداثيات", "اكتب الاسم وخطي العرض والطول بنفسك", onClick = onPickCoords, accent = TurquoiseAccent)
    }
}

@Composable
private fun ColumnScope.TimeSection(onChanged: () -> Unit) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val cal = AppClock.calendar(st)
    var showPerPrayer by remember { mutableStateOf(false) }

    Segmented(
        options = listOf("تلقائي من الهاتف", "ضبط يدوي"),
        selected = st.timeMode,
        onSelect = { st.timeMode = it; onChanged() }
    )

    if (st.timeMode == 1) {
        StepperRow(
            Icons.Rounded.Public, "المنطقة الزمنية (UTC)", "كل ضغطة نصف ساعة",
            AppClock.utcLabel(st.manualUtcMin), TurquoiseAccent,
            onMinus = { st.manualUtcMin -= 30; onChanged() },
            onPlus = { st.manualUtcMin += 30; onChanged() }
        )
        ActionRow(
            Icons.Rounded.PhoneAndroid, "مطابقة توقيت الهاتف", "اضبط الإزاحة على توقيت جهازك الحالي",
            onClick = {
                st.manualUtcMin = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60_000
                onChanged()
            }
        )
    }

    StepperRow(
        Icons.Rounded.Tune, "تصحيح عام للمواقيت", "تقديم أو تأخير جميع الصلوات بالدقائق",
        signed(st.minuteAdjust),
        onMinus = { st.minuteAdjust -= 1; onChanged() },
        onPlus = { st.minuteAdjust += 1; onChanged() }
    )

    ActionRow(
        Icons.Rounded.Timer, "تصحيح كل صلاة على حدة", "ضبط دقيق لكل وقت منفرداً",
        value = if (showPerPrayer) "إخفاء" else "عرض",
        onClick = { showPerPrayer = !showPerPrayer }
    )
    AnimatedVisibility(visible = showPerPrayer, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        val times = PrayerCalc.timesFor(st, cal)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            StepperRow(Icons.Rounded.NightsStay, "الفجر • ${PrayerCalc.formatTime(times.fajr, st.use24h)}", null, signed(st.adjFajr),
                onMinus = { st.adjFajr -= 1; onChanged() }, onPlus = { st.adjFajr += 1; onChanged() })
            StepperRow(Icons.Rounded.WbTwilight, "الشروق • ${PrayerCalc.formatTime(times.sunrise, st.use24h)}", null, signed(st.adjSunrise),
                onMinus = { st.adjSunrise -= 1; onChanged() }, onPlus = { st.adjSunrise += 1; onChanged() })
            StepperRow(Icons.Rounded.WbSunny, "الظهر • ${PrayerCalc.formatTime(times.dhuhr, st.use24h)}", null, signed(st.adjDhuhr),
                onMinus = { st.adjDhuhr -= 1; onChanged() }, onPlus = { st.adjDhuhr += 1; onChanged() })
            StepperRow(Icons.Rounded.LightMode, "العصر • ${PrayerCalc.formatTime(times.asr, st.use24h)}", null, signed(st.adjAsr),
                onMinus = { st.adjAsr -= 1; onChanged() }, onPlus = { st.adjAsr += 1; onChanged() })
            StepperRow(Icons.Rounded.Brightness4, "المغرب • ${PrayerCalc.formatTime(times.maghrib, st.use24h)}", null, signed(st.adjMaghrib),
                onMinus = { st.adjMaghrib -= 1; onChanged() }, onPlus = { st.adjMaghrib += 1; onChanged() })
            StepperRow(Icons.Rounded.Bedtime, "العشاء • ${PrayerCalc.formatTime(times.isha, st.use24h)}", null, signed(st.adjIsha),
                onMinus = { st.adjIsha -= 1; onChanged() }, onPlus = { st.adjIsha += 1; onChanged() })
            TextButton(onClick = { st.resetTimeAdjustments(); onChanged() }) {
                Text("تصفير كل التصحيحات", color = DangerRed, fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    StepperRow(
        Icons.Rounded.CalendarMonth, "تصحيح التاريخ الهجري", "مطابقة رؤية الهلال في بلدك",
        when { st.hijriOffset > 0 -> "+${st.hijriOffset} يوم"; st.hijriOffset < 0 -> "${st.hijriOffset} يوم"; else -> "0 يوم" },
        GoldWarm,
        onMinus = { st.hijriOffset -= 1 },
        onPlus = { st.hijriOffset += 1 }
    )
    SwitchRow(Icons.Rounded.AccessTime, "نظام 24 ساعة", "عرض الوقت بصيغة 14:30 بدل 2:30 م", st.use24h) { st.use24h = it }
    Segmented(
        options = listOf("يناير • فبراير", "كانون الثاني • شباط"),
        selected = st.monthStyle,
        onSelect = { st.monthStyle = it }
    )
}

@Composable
private fun ColumnScope.PrayerSection(nav: Nav, onMethod: () -> Unit, onMuezzin: () -> Unit, onChanged: () -> Unit) {
    val st = LocalApp.current
    val ctx = LocalContext.current
    val isAdhanPlaying by AdhanEngine.isPlaying.collectAsState()
    val selectedId by AdhanEngine.activeMuezzinId.collectAsState()
    val muezzin = remember(selectedId) { AdhanEngine.getSelectedMuezzin(ctx) }

    ActionRow(
        Icons.Rounded.Calculate, "طريقة حساب المواقيت", "اختر الجهة المعتمدة في بلدك",
        PrayerCalc.methodNames[st.prayerMethod.coerceIn(0, PrayerCalc.methodNames.lastIndex)].substringBefore(" ("),
        onClick = onMethod
    )
    Segmented(
        options = listOf("الجمهور (ظل = المثل)", "الحنفي (ظل = المثلان)"),
        selected = if (st.asrHanafi) 1 else 0,
        onSelect = { st.asrHanafi = it == 1; onChanged() }
    )
    SwitchRow(
        Icons.Rounded.NotificationsActive, "الأذان التلقائي الفوري",
        "يؤذن بصوت المؤذن مباشرة عند دخول وقت الصلاة",
        st.prayerAlert
    ) { enabled ->
        st.prayerAlert = enabled
        if (enabled) AdhanEngine.scheduleAll(ctx, st) else AdhanEngine.cancelAll(ctx)
    }
    ActionRow(Icons.Rounded.RecordVoiceOver, "صوت المؤذن", "${Top15Muezzins.size} مؤذناً للاختيار", muezzin.name, accent = TurquoiseAccent, onClick = onMuezzin)
    ActionRow(
        if (isAdhanPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
        if (isAdhanPlaying) "إيقاف الأذان" else "تجربة صوت الأذان", "استمع لصوت المؤذن المختار الآن",
        accent = if (isAdhanPlaying) DangerRed else GoldWarm,
        onClick = { if (isAdhanPlaying) AdhanEngine.stopAdhan(ctx) else AdhanEngine.playAdhanNow(ctx, "التجربة") }
    )
    ActionRow(Icons.Rounded.Explore, "مواقيت اليوم والقبلة", "افتح شاشة المواقيت الكاملة", onClick = { nav.openPrayer(false) })
}

@Composable
private fun ColumnScope.AlertsSection(onChanged: () -> Unit) {
    val st = LocalApp.current
    val ctx = LocalContext.current

    SwitchRow(Icons.Rounded.AutoStories, "تذكير الورد اليومي", "إشعار يومي لقراءة وردك من القرآن", st.dailyReminder) {
        st.dailyReminder = it; onChanged()
    }
    if (st.dailyReminder) {
        ActionRow(
            Icons.Rounded.Alarm, "وقت التذكير", "اضغط لاختيار الساعة والدقيقة",
            String.format(Locale.US, "%02d:%02d", st.reminderHour, st.reminderMinute),
            onClick = {
                TimePickerDialog(ctx, { _, h, m -> st.reminderHour = h; st.reminderMinute = m; onChanged() }, st.reminderHour, st.reminderMinute, true).show()
            }
        )
    }
    StepperRow(Icons.Rounded.Flag, "هدف الورد اليومي", "عدد الصفحات المستهدفة يومياً", "${st.dailyGoal} صفحات", onMinus = { st.dailyGoal -= 1 }, onPlus = { st.dailyGoal += 1 })
    SwitchRow(Icons.Rounded.AutoAwesome, "تذكير الأذكار", "تذكير الصباح والمساء", st.adhkarAlert, GoldWarm) { st.adhkarAlert = it; onChanged() }
    ActionRow(
        Icons.Rounded.NotificationsNone, "إشعارات التطبيق في النظام", "فتح إعدادات الإشعارات",
        accent = TurquoiseAccent,
        onClick = {
            runCatching {
                ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    )
}

@Composable
private fun ColumnScope.AppearanceSection() {
    val st = LocalApp.current
    SwitchRow(Icons.Rounded.DarkMode, "الوضع الليلي", "ألوان داكنة مريحة للعين", st.nightMode) { st.nightMode = it }
    SwitchRow(Icons.Rounded.Description, "ورق المصحف الدافئ", "لون كريمي هادئ لصفحة المصحف", st.paperWarmth, GoldWarm) { st.paperWarmth = it }
    SwitchRow(Icons.Rounded.ScreenLockPortrait, "إبقاء الشاشة مضاءة", "منع انطفاء الشاشة أثناء القراءة", st.keepOn) { st.keepOn = it }
}

@Composable
private fun ColumnScope.AdhkarSection() {
    val st = LocalApp.current
    val ctx = LocalContext.current
    SliderRow(Icons.Rounded.TextFields, "حجم خط الأذكار", "${(st.adhkarFontScale * 100).toInt()}%", st.adhkarFontScale, 0.8f..1.5f) { st.adhkarFontScale = it }
    SliderRow(Icons.Rounded.Spa, "حجم خط المسبحة", "${(st.tasbihFontScale * 100).toInt()}%", st.tasbihFontScale, 0.8f..1.6f) { st.tasbihFontScale = it }
    SwitchRow(Icons.Rounded.Vibration, "اهتزاز عند إتمام العدد", "اهتزاز مميز عند إكمال الجولة أو الذكر", st.stageVib) { st.stageVib = it }
    ActionRow(
        Icons.Rounded.RestartAlt, "إعادة ضبط أذكار اليوم", "تصفير عدادات جميع الأذكار",
        accent = DangerRed,
        onClick = { st.adhkarResetAll(); Toast.makeText(ctx, "تمت إعادة ضبط أذكار اليوم ✓", Toast.LENGTH_SHORT).show() }
    )
}

@Composable
private fun ColumnScope.SoundSection() {
    val st = LocalApp.current
    SwitchRow(Icons.Rounded.VolumeUp, "أصوات النقر", "صوت خفيف عند الضغط على الأزرار", st.soundOn) { st.soundOn = it }
    SwitchRow(Icons.Rounded.Vibration, "الاهتزاز", "اهتزاز خفيف عند التفاعل", st.vibOn, GoldWarm) { st.vibOn = it }
}

@Composable
private fun ColumnScope.DataSection(filesCount: Int, sizeMb: Long, onClearAudio: () -> Unit, onResetStats: () -> Unit) {
    val st = LocalApp.current
    ActionRow(Icons.Rounded.Headphones, "التلاوات المحمّلة", "$filesCount سورة • $sizeMb ميغابايت", accent = TurquoiseAccent, onClick = onClearAudio)
    ActionRow(Icons.Rounded.DeleteSweep, "تصفير الإحصائيات", "الختمة (${st.readPages.size} صفحة) والتسبيح (${st.totalTasbih})", accent = DangerRed, onClick = onResetStats)
}

@Composable
private fun ColumnScope.AboutSection() {
    val p = LocalPalette.current
    Text(
        "تطبيق القرآن الكريم الشامل: مصحف PDF كامل بدون إنترنت، تلاوات بأصوات ${QuranData.reciters.size} قارئاً، مواقيت الصلاة والأذان والقبلة، أذكار شاملة، ومسبحة إلكترونية.",
        fontWeight = FontWeight.Bold, fontSize = 13.sp, color = p.ink, lineHeight = 20.sp
    )
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
    var filterMode by remember { mutableIntStateOf(0) }
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

    Column(modifier = Modifier.fillMaxSize().background(p.bg)) {
        ScreenHeader("مركز الحفظ والمراجعة", onBack = { nav.back() })

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RoyalForest,
                    border = BorderStroke(1.3.dp, GoldWarm.copy(alpha = 0.65f))
                ) {
                    Box(Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
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

            item {
                QCard(shape = RoundedCornerShape(22.dp), borderColor = GoldWarm.copy(alpha = 0.55f)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Quiz, null, tint = EmeraldVibrant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("اختبار المعلومات القرآنية", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            }
                            IconButton(
                                onClick = { fx.click(); quizSurah = QuranData.surahs.random(); quizAnswerRevealed = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Rounded.Refresh, "سؤال جديد", tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
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
                                color = GoldShine.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, GoldWarm)
                            ) {
                                Text(
                                    text = "ترتيبها: ${quizSurah.num} • نوعها: ${quizSurah.type} • آياتها: ${quizSurah.ayahs} آية • تبدأ صفحة ${quizSurah.page}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.5.sp,
                                    color = p.ink,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { fx.click(); quizAnswerRevealed = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إظهار الإجابة", fontWeight = FontWeight.ExtraBold, color = GoldShine)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        placeholder = { Text("ابحث لتحديد السور التي أتممت حفظها...", fontSize = 12.5.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = EmeraldVibrant) },
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
                                    selectedContainerColor = GoldShine,
                                    selectedLabelColor = RoyalForest
                                )
                            )
                        }
                    }
                }
            }

            items(filteredSurahs, key = { it.num }) { s ->
                val isMemorized = s.num in st.memorizedSurahs
                QCard(
                    shape = RoundedCornerShape(18.dp),
                    selected = isMemorized,
                    onClick = { st.toggleMemorizedSurah(s.num) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberBadge(s.num, 36, highlighted = isMemorized)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سُورَةُ ${s.name}",
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (isMemorized) p.selectedInk else p.ink
                            )
                            Text(
                                text = "${s.ayahs} آية • صفحة ${s.page}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isMemorized) p.selectedInk.copy(alpha = 0.85f) else p.muted
                            )
                        }
                        IconButton(onClick = { fx.click(); nav.openPage(s.page) }) {
                            Icon(Icons.AutoMirrored.Rounded.MenuBook, "اقرأ", tint = if (isMemorized) p.selectedInk else EmeraldVibrant, modifier = Modifier.size(20.dp))
                        }
                        Icon(
                            imageVector = if (isMemorized) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = "حالة الحفظ",
                            tint = if (isMemorized) GoldShine else p.muted,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
}
