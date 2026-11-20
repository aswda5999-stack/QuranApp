@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ============================================================
// 1. نماذج صيغ التسبيح للمسبحة الإلكترونية
// ============================================================
data class TasbihPhrase(
    val title: String,
    val arabicText: String,
    val defaultTarget: Int
)

private val DefaultTasbihPhrases = listOf(
    TasbihPhrase("التسبيح", "سُبْحَانَ اللَّهِ", 33),
    TasbihPhrase("التحميد", "الْحَمْدُ لِلَّهِ", 33),
    TasbihPhrase("التكبير", "اللَّهُ أَكْبَرُ", 34),
    TasbihPhrase("التهليل", "لَا إِلَٰهَ إِلَّا اللَّهُ", 100),
    TasbihPhrase("الاستغفار", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ وَأَتُوبُ إِلَيْهِ", 100),
    TasbihPhrase("الحوقلة", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ", 100),
    TasbihPhrase("الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَىٰ نَبِيِّنَا مُحَمَّدٍ", 100),
    TasbihPhrase("تسبيح مضاعف", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", 100),
    TasbihPhrase("التسبيح الجامع", "سُبْحَانَ اللَّهِ وَالْحَمْدُ لِلَّهِ وَلَا إِلَٰهَ إِلَّا اللَّهُ وَاللَّهُ أَكْبَرُ", 100),
    TasbihPhrase("التوكل", "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", 100),
    TasbihPhrase("دعوة ذي النون", "لَا إِلَٰهَ إِلَّا أَنْتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ", 100),
    TasbihPhrase("التوبة", "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ", 100)
)

private val TargetOptions = listOf(10, 33, 99, 100, 1000, 0)

/** حجم الخط يتناسب عكسياً مع طول الذكر ليبقى كبيراً وواضحاً دائماً */
private fun phraseFontSize(len: Int): Float = when {
    len <= 14 -> 52f
    len <= 24 -> 44f
    len <= 40 -> 36f
    len <= 60 -> 30f
    else -> 25f
}

// ============================================================
// 2. شاشة المسبحة الإلكترونية (TasbihScreen)
// ============================================================
@Composable
fun TasbihScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val phrases = DefaultTasbihPhrases + st.customPhrases.toList()
    val phrasesNow by rememberUpdatedState(phrases)
    val index = st.tasbihIndex.coerceIn(0, phrases.lastIndex)

    val pagerState = rememberPagerState(initialPage = index, pageCount = { phrases.size })

    // عند سحب بطاقة الذكر لصيغة أخرى: نغيّرها ونصفّر العدّاد ونضبط هدفها الافتراضي
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            if (page != st.tasbihIndex) {
                st.tasbihIndex = page
                st.tasbihCount = 0
                st.tasbihTarget = phrasesNow.getOrNull(page)?.defaultTarget ?: 33
            }
        }
    }

    var jumpTo by remember { mutableIntStateOf(-1) }
    LaunchedEffect(jumpTo, phrases.size) {
        if (jumpTo in 0 until phrases.size) {
            pagerState.animateScrollToPage(jumpTo)
            jumpTo = -1
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    val count = st.tasbihCount
    val target = st.tasbihTarget
    val progress by animateFloatAsState(
        targetValue = if (target > 0) (count.toFloat() / target).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(180),
        label = "tasbihProgress"
    )

    fun tap() {
        fx.click()
        st.totalTasbih += 1
        val next = count + 1
        if (target > 0 && next >= target) {
            st.tasbihCount = 0
            st.tasbihRounds += 1
            if (st.stageVib) fx.vibrate(180L) else fx.vibrate(40L)
        } else {
            st.tasbihCount = next
            fx.vibrate(22L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "المسبحة الإلكترونية",
            subtitle = "اسحب البطاقة لتغيير الذكر • اضغط الدائرة للتسبيح",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); st.tasbihFontScale -= 0.1f }) {
                    Icon(Icons.Default.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); st.tasbihFontScale += 0.1f }) {
                    Icon(Icons.Default.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // إحصائيات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPillCard("إجمالي التسبيحات", "${st.totalTasbih}", Icons.Default.Spa, EmeraldVibrant, Modifier.weight(1f))
                StatPillCard("الجولات المكتملة", "${st.tasbihRounds}", Icons.Default.Loop, GoldWarm, Modifier.weight(1f))
            }

            // بطاقة الذكر الكبيرة بالخط العثماني العريض (قابلة للسحب)
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 18.dp),
                pageSpacing = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val ph = phrases[page.coerceIn(0, phrases.lastIndex)]
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = RoyalForest,
                    border = BorderStroke(1.6.dp, GoldWarm.copy(alpha = 0.8f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldVibrant)))
                    ) {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.14f)) {
                                Text(
                                    text = "${ph.title} • الهدف ${ph.defaultTarget}",
                                    color = GoldSoftBg,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                val size = phraseFontSize(ph.arabicText.length) * st.tasbihFontScale
                                Text(
                                    text = ph.arabicText,
                                    fontFamily = UthmanicFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = size.sp,
                                    lineHeight = (size * 1.65f).sp,
                                    color = GoldShine,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                        }
                    }
                }
            }

            // مؤشر الصفحات
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                phrases.indices.forEach { i ->
                    val sel = i == pagerState.currentPage
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (sel) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (sel) EmeraldVibrant else p.border)
                    )
                }
            }

            // أسماء الأذكار للانتقال السريع
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(phrases) { i, ph ->
                    val sel = i == pagerState.currentPage
                    Surface(
                        onClick = { fx.click(); scope.launch { pagerState.animateScrollToPage(i) } },
                        shape = RoundedCornerShape(14.dp),
                        color = if (sel) EmeraldVibrant else p.card,
                        border = BorderStroke(1.dp, if (sel) GoldShine else p.border)
                    ) {
                        Text(
                            text = ph.title,
                            color = if (sel) Color.White else p.ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // اختيار العدد المستهدف
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("العدد المستهدف", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = p.muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(TargetOptions) { _, t ->
                        val sel = target == t
                        Surface(
                            onClick = { fx.click(); st.tasbihTarget = t; st.tasbihCount = 0 },
                            shape = RoundedCornerShape(12.dp),
                            color = if (sel) GoldWarm else p.card,
                            border = BorderStroke(1.dp, if (sel) GoldShine else p.border)
                        ) {
                            Text(
                                text = if (t == 0) "مفتوح ∞" else "$t",
                                color = if (sel) RoyalForest else p.ink,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // العداد الدائري
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 16.dp.toPx()
                    val inset = stroke / 2f
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    drawArc(
                        color = p.border,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(EmeraldVibrant, GoldShine, GoldWarm)),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                Surface(
                    onClick = { tap() },
                    shape = CircleShape,
                    color = RoyalForest,
                    border = BorderStroke(2.dp, GoldWarm),
                    shadowElevation = 8.dp,
                    modifier = Modifier.size(196.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("$count", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp)
                        Text(
                            text = if (target > 0) "من $target" else "بدون حد",
                            color = GoldSoftBg,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("اضغط للتسبيح", color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            // أزرار التحكم
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ControlButton("تراجع", Icons.Default.Undo, Modifier.weight(1f)) {
                    fx.click()
                    if (st.tasbihCount > 0) st.tasbihCount -= 1
                }
                ControlButton("تصفير", Icons.Default.Refresh, Modifier.weight(1f)) {
                    fx.click()
                    st.tasbihCount = 0
                }
                ControlButton("ذكر جديد", Icons.Default.Add, Modifier.weight(1f)) {
                    fx.click()
                    showAddDialog = true
                }
                if (pagerState.currentPage >= DefaultTasbihPhrases.size) {
                    ControlButton("حذف", Icons.Default.Delete, Modifier.weight(1f), danger = true) {
                        fx.click()
                        val page = pagerState.currentPage
                        st.removeCustomPhrase(page - DefaultTasbihPhrases.size)
                        st.tasbihIndex = (page - 1).coerceAtLeast(0)
                        st.tasbihCount = 0
                        jumpTo = (page - 1).coerceAtLeast(0)
                        Toast.makeText(ctx, "تم حذف الذكر المخصص", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAddDialog) {
        var text by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = { Text("إضافة ذكر مخصص", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        label = { Text("نص الذكر") },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title, onValueChange = { title = it }, singleLine = true,
                        label = { Text("عنوان (اختياري)") },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetText, onValueChange = { targetText = it.filter { c -> c.isDigit() }.take(6) },
                        singleLine = true,
                        label = { Text("العدد المستهدف (0 = مفتوح)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val t = text.trim()
                        if (t.isNotEmpty()) {
                            val n = targetText.toIntOrNull() ?: 100
                            st.addCustomPhrase(TasbihPhrase(title.trim().ifEmpty { t.take(18) }, t, n))
                            jumpTo = DefaultTasbihPhrases.size + st.customPhrases.size - 1
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                ) { Text("إضافة", color = Color.White, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("إلغاء", color = p.muted) } }
        )
    }
}

@Composable
private fun StatPillCard(label: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(accent.copy(alpha = 0.14f))
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = p.ink)
                Text(label, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = p.muted)
            }
        }
    }
}

@Composable
private fun ControlButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = p.card,
        border = BorderStroke(1.dp, if (danger) DangerRed.copy(alpha = 0.6f) else p.border),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if (danger) DangerRed else EmeraldVibrant, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(3.dp))
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, color = p.ink)
        }
    }
}
