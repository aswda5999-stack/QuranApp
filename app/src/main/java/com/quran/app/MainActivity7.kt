@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// ============================================================
// 1. شاشة الإعدادات الشاملة والمتطورة (SettingsScreen)
// ============================================================
@Composable
fun SettingsScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    var cachedFilesCount by remember { mutableIntStateOf(AudioCache.count(ctx)) }
    var cachedSizeMb by remember { mutableLongStateOf(AudioCache.sizeMb(ctx)) }
    var showClearAudioDialog by remember { mutableStateOf(false) }
    var showResetStatsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "الإعدادات والتخصيص",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); nav.push(Screen.Hifz) }) {
                    Icon(Icons.Default.Psychology, "مركز الحفظ", tint = GoldShine)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. بطاقة ملخص الإنجاز والتخزين
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = RoyalForest,
                border = BorderStroke(1.3.dp, GoldWarm.copy(alpha = 0.65f)),
                shadowElevation = 6.dp
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))
                ) {
                    IslamicPattern(Modifier.matchParentSize())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SummaryStatItem("${st.khatmaPercent()}%", "إنجاز الختمة", Icons.AutoMirrored.Filled.MenuBook)
                        Box(Modifier.width(1.dp).height(34.dp).background(GoldShine.copy(alpha = 0.35f)))
                        SummaryStatItem("${st.memorizedSurahs.size}/114", "سورة محفوظة", Icons.Default.WorkspacePremium)
                        Box(Modifier.width(1.dp).height(34.dp).background(GoldShine.copy(alpha = 0.35f)))
                        SummaryStatItem("$cachedFilesCount", "تلاوة محملة", Icons.Default.OfflinePin)
                    }
                }
            }

            // 2. قسم المظهر وحجم الخط العثماني مع معاينة حية
            SectionTitle("المظهر وتجربة القراءة")
            QCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingSwitchRow(
                        title = "الوضع الليلي المريح",
                        subtitle = "ألوان داكنة هادئة للقراءة الليلية",
                        icon = Icons.Default.DarkMode,
                        accent = Color(0xFF6366F1),
                        checked = st.nightMode,
                        onCheckedChange = { st.nightMode = it }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "لون ورق المصحف الدافئ",
                        subtitle = "خلفية عاجية مريحة للعين أثناء التلاوة النهارية",
                        icon = Icons.Default.Palette,
                        accent = GoldWarm,
                        checked = st.paperWarmth,
                        enabled = !st.nightMode,
                        onCheckedChange = { st.paperWarmth = it }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "إبقاء الشاشة مضاءة",
                        subtitle = "منع انطفاء الشاشة تلقائياً داخل المصحف",
                        icon = Icons.Default.BrightnessHigh,
                        accent = TurquoiseAccent,
                        checked = st.keepOn,
                        onCheckedChange = { st.keepOn = it }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    // التحكم التفاعلي بحجم الخط العثماني مع معاينة مباشرة
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, null, tint = EmeraldVibrant, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("حجم خط المصحف", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            }
                            Text(
                                text = "${(st.mushafFontScale * 100).toInt()}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldVibrant,
                                fontSize = 13.sp
                            )
                        }

                        Slider(
                            value = st.mushafFontScale,
                            onValueChange = { st.mushafFontScale = it },
                            valueRange = 0.8f..1.5f,
                            colors = SliderDefaults.colors(
                                thumbColor = GoldWarm,
                                activeTrackColor = EmeraldVibrant
                            )
                        )

                        // صندوق معاينة حية للخط العثماني
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = p.paper,
                            border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                fontFamily = UthmanicFontFamily,
                                fontSize = (21 * st.mushafFontScale).sp,
                                color = p.mushafInk,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp)
                            )
                        }
                    }
                }
            }

            // 3. قسم الورد اليومي والتنبيهات والأذان
            SectionTitle("الورد اليومي والتنبيهات")
            QCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // تحديد هدف القراءة اليومي
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("هدف الورد اليومي", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text("عدد الصفحات المستهدفة يومياً", fontSize = 11.5.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { fx.click(); st.dailyGoal -= 1 },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldVibrant.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Remove, "تقليل", tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = "${st.dailyGoal} ص",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = p.ink
                            )
                            IconButton(
                                onClick = { fx.click(); st.dailyGoal += 1 },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldVibrant.copy(alpha = 0.12f))
                            ) {
                                Icon(Icons.Default.Add, "زيادة", tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "تنبيهات الأذان التلقائي",
                        subtitle = "تشغيل صوت المؤذن المختار عند دخول وقت الصلاة",
                        icon = Icons.Default.NotificationsActive,
                        accent = GoldWarm,
                        checked = st.prayerAlert,
                        onCheckedChange = { enabled ->
                            st.prayerAlert = enabled
                            if (enabled) AdhanEngine.scheduleAll(ctx, st)
                            else AdhanEngine.cancelAll(ctx)
                        }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "تذكير الورد اليومي",
                        subtitle = String.format(Locale.US, "الساعة %02d:00 مساءً", st.reminderHour),
                        icon = Icons.Default.Alarm,
                        accent = EmeraldVibrant,
                        checked = st.dailyReminder,
                        onCheckedChange = { st.dailyReminder = it }
                    )

                    if (st.dailyReminder) {
                        OutlinedButton(
                            onClick = {
                                fx.click()
                                TimePickerDialog(
                                    ctx,
                                    { _, hour, _ ->
                                        st.reminderHour = hour
                                        Toast.makeText(ctx, "تم ضبط التذكير على الساعة $hour:00", Toast.LENGTH_SHORT).show()
                                    },
                                    st.reminderHour,
                                    0,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, EmeraldVibrant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Schedule, null, tint = EmeraldVibrant, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("تغيير وقت التذكير اليومي", fontWeight = FontWeight.ExtraBold, color = EmeraldVibrant, fontSize = 12.5.sp)
                        }
                    }
                }
            }

            // 4. قسم الصوت والتفاعل الحسي
            SectionTitle("الصوت والتفاعل الحسي")
            QCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingSwitchRow(
                        title = "أصوات النقر والتفاعل",
                        subtitle = "نغمة خفيفة عند التنقل والتسبيح",
                        icon = Icons.Default.VolumeUp,
                        accent = TurquoiseAccent,
                        checked = st.soundOn,
                        onCheckedChange = { st.soundOn = it }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "الاهتزاز التفاعلي",
                        subtitle = "اهتزاز لطيف مع كل تسبيحة",
                        icon = Icons.Default.Vibration,
                        accent = EmeraldVibrant,
                        checked = st.vibOn,
                        onCheckedChange = { st.vibOn = it }
                    )

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    SettingSwitchRow(
                        title = "اهتزاز اكتمال الدورة",
                        subtitle = "اهتزاز مميز عند الوصول لهدف المسبحة",
                        icon = Icons.Default.NotificationsPaused,
                        accent = GoldWarm,
                        checked = st.stageVib,
                        onCheckedChange = { st.stageVib = it }
                    )
                }
            }

            // 5. قسم التخزين والنسخ الاحتياطي السريع
            SectionTitle("التخزين والبيانات")
            QCard(shape = RoundedCornerShape(22.dp)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("التلاوات المحملة بدون إنترنت", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text("$cachedFilesCount سورة • المساحة $cachedSizeMb ميجابايت", fontSize = 11.5.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                        FilledTonalButton(
                            onClick = { fx.click(); showClearAudioDialog = true },
                            enabled = cachedFilesCount > 0,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFFB71C1C).copy(alpha = 0.12f),
                                contentColor = Color(0xFFB71C1C)
                            )
                        ) {
                            Text("تفريغ", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = p.border.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إعادة ضبط الإحصائيات", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text("تصفير سجل القراءة والتسبيح للبدء من جديد", fontSize = 11.5.sp, color = p.muted, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { fx.click(); showResetStatsDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFB71C1C))
                        ) {
                            Text("تصفير", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = Color(0xFFB71C1C))
                        }
                    }
                }
            }
        }
    }

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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("حذف الكل", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearAudioDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showResetStatsDialog) {
        AlertDialog(
            onDismissRequest = { showResetStatsDialog = false },
            shape = RoundedCornerShape(22.dp),
            title = { Text("تصفير الإحصائيات؟", fontWeight = FontWeight.ExtraBold) },
            text = { Text("سيتم تصفير عداد الختمة وإجمالي التسبيحات فقط دون حذف العلامات المرجعية.") },
            confirmButton = {
                Button(
                    onClick = {
                        st.resetStats()
                        showResetStatsDialog = false
                        Toast.makeText(ctx, "تمت إعادة الضبط بنجاح ✓", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                ) { Text("تأكيد", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showResetStatsDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun SummaryStatItem(value: String, label: String, icon: ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = GoldShine, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Text(label, color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.5.sp)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                fx.click()
                onCheckedChange(!checked)
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(accent.copy(alpha = 0.14f))
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = if (enabled) p.ink else p.muted)
            Text(subtitle, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = p.muted)
        }
        Switch(
            checked = checked,
            onCheckedChange = {
                fx.click()
                onCheckedChange(it)
            },
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldShine,
                checkedTrackColor = EmeraldVibrant
            )
        )
    }
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
