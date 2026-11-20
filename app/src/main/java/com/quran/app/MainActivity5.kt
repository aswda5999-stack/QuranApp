@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

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
    TasbihPhrase("التوكل", "حَسْبُنَا اللَّهُ وَنِعْمَ الْوَكِيلُ", 100)
)

private val TargetOptions = listOf(10, 33, 99, 100, 1000, 0)

private fun compactPhraseFontSize(len: Int): Float = when {
    len <= 16 -> 32f
    len <= 28 -> 26f
    len <= 45 -> 22f
    else -> 19f
}

// ============================================================
// شاشة المسبحة الإلكترونية المضغوطة (شاشة واحدة بدون تمرير للسفل وللعلى)
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
        animationSpec = tween(160),
        label = "tasbihProgress"
    )

    fun tap() {
        fx.click()
        st.totalTasbih += 1
        val next = count + 1
        if (target > 0 && next >= target) {
            st.tasbihCount = 0
            st.tasbihRounds += 1
            if (st.stageVib) fx.vibrate(170L) else fx.vibrate(40L)
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
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); st.tasbihFontScale -= 0.1f }) {
                    Icon(Icons.Rounded.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); st.tasbihFontScale += 0.1f }) {
                    Icon(Icons.Rounded.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
            }
        )

        // محتوى موزع بمرونة ليتسع في شاشة واحدة تماماً بدون أي Scroll
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. شريط الإحصائيات المدمج
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatChip("المجموع: ${st.totalTasbih}", Icons.Rounded.Spa, Modifier.weight(1f))
                CompactStatChip("الجولات: ${st.tasbihRounds}", Icons.Rounded.Loop, Modifier.weight(1f))
            }

            // 2. بطاقة الذكر المدمجة (قابلة للسحب يميناً ويساراً)
            HorizontalPager(
                state = pagerState,
                pageSpacing = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
            ) { page ->
                val ph = phrases[page.coerceIn(0, phrases.lastIndex)]
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = RoyalForest,
                    border = BorderStroke(1.4.dp, GoldWarm.copy(alpha = 0.8f)),
                    shadowElevation = 5.dp,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(Modifier.background(Brush.horizontalGradient(listOf(RoyalForest, EmeraldVibrant)))) {
                        IslamicPattern(Modifier.matchParentSize())
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = ph.title,
                                color = GoldSoftBg,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            val sz = compactPhraseFontSize(ph.arabicText.length) * st.tasbihFontScale
                            Text(
                                text = ph.arabicText,
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = sz.sp,
                                lineHeight = (sz * 1.45f).sp,
                                color = GoldShine,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. شريط اختيار صيغة الذكر (تمييز ذهبي واضح للمختار)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(phrases) { i, ph ->
                    val sel = i == pagerState.currentPage
                    val bg by animateColorAsState(if (sel) GoldShine else p.card, label = "phBg")
                    Surface(
                        onClick = { fx.click(); scope.launch { pagerState.animateScrollToPage(i) } },
                        shape = RoundedCornerShape(12.dp),
                        color = bg,
                        border = BorderStroke(1.2.dp, if (sel) RoyalForest else p.border)
                    ) {
                        Text(
                            text = ph.title,
                            color = if (sel) RoyalForest else p.ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // 4. شريط اختيار الهدف (تمييز ذهبي واضح للمختار)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(TargetOptions) { _, t ->
                    val sel = target == t
                    val bg by animateColorAsState(if (sel) GoldShine else p.card, label = "tgBg")
                    Surface(
                        onClick = { fx.click(); st.tasbihTarget = t; st.tasbihCount = 0 },
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        border = BorderStroke(1.2.dp, if (sel) RoyalForest else p.border)
                    ) {
                        Text(
                            text = if (t == 0) "مفتوح ∞" else "هدف $t",
                            color = if (sel) RoyalForest else p.ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // 5. حلقة التسبيح التفاعلية المركزية
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(196.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 13.dp.toPx()
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
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(156.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("$count", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 48.sp)
                        Text(
                            text = if (target > 0) "من $target" else "مفتوح",
                            color = GoldSoftBg,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                        Text("اضغط للتسبيح", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }

            // 6. أزرار التحكم السفلية المدمجة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactControlBtn("تراجع", Icons.Rounded.Undo, Modifier.weight(1f)) {
                    fx.click()
                    if (st.tasbihCount > 0) st.tasbihCount -= 1
                }
                CompactControlBtn("تصفير", Icons.Rounded.Refresh, Modifier.weight(1f)) {
                    fx.click()
                    st.tasbihCount = 0
                }
                CompactControlBtn("ذكر جديد", Icons.Rounded.Add, Modifier.weight(1f)) {
                    fx.click()
                    showAddDialog = true
                }
                if (pagerState.currentPage >= DefaultTasbihPhrases.size) {
                    CompactControlBtn("حذف", Icons.Rounded.Delete, Modifier.weight(1f), danger = true) {
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
private fun CompactStatChip(text: String, icon: ImageVector, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = p.card,
        border = BorderStroke(1.dp, p.border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, null, tint = GoldWarm, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp, color = p.ink)
        }
    }
}

@Composable
private fun CompactControlBtn(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = p.card,
        border = BorderStroke(1.dp, if (danger) DangerRed.copy(alpha = 0.6f) else p.border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if (danger) DangerRed else EmeraldVibrant, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = p.ink, maxLines = 1)
        }
    }
}
