@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// نماذج صيغ التسبيح للمسبحة الإلكترونية المتطورة
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
    TasbihPhrase("الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ", 100),
    TasbihPhrase("تسبيح مضاعف", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ ، سُبْحَانَ اللَّهِ الْعَظِيمِ", 100)
)

// ============================================================
// شاشة المسبحة الإلكترونية الملكية المتطورة (TasbihScreen)
// ============================================================
@Composable
fun TasbihScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    val phrases = remember {
        mutableStateListOf<TasbihPhrase>().apply { addAll(DefaultTasbihPhrases) }
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    var count by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(33) }
    var rounds by remember { mutableIntStateOf(0) }
    var phraseScale by remember { mutableFloatStateOf(1.0f) }

    var showCustomDialog by remember { mutableStateOf(false) }
    var customPhraseInput by remember { mutableStateOf("") }
    var customTargetInput by remember { mutableStateOf("100") }

    val activePhrase = phrases.getOrElse(selectedIndex) { phrases.first() }

    val progressFraction by animateFloatAsState(
        targetValue = if (target > 0) (count.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tasbihProgress"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tasbihBtnScale"
    )

    fun incrementCounter() {
        fx.click()
        fx.vibrate(28L)
        count += 1
        st.totalTasbih += 1

        if (target > 0 && count >= target) {
            rounds += 1
            if (st.stageVib) fx.vibrate(140L)
            count = 0
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
                IconButton(onClick = { fx.click(); phraseScale = (phraseScale - 0.1f).coerceAtLeast(0.8f) }) {
                    Icon(Icons.Default.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); phraseScale = (phraseScale + 0.1f).coerceAtMost(1.4f) }) {
                    Icon(Icons.Default.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); showCustomDialog = true }) {
                    Icon(Icons.Default.AddCircleOutline, "إضافة صيغة", tint = GoldShine)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. بطاقة الإحصائيات الفورية (المجموع الكلي + الدورات المكتملة)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPillCard(
                    title = "إجمالي التسبيحات",
                    value = "${st.totalTasbih}",
                    icon = Icons.Default.AutoAwesome,
                    accent = EmeraldVibrant,
                    modifier = Modifier.weight(1f)
                )
                StatPillCard(
                    title = "الدورات المكتملة",
                    value = "$rounds",
                    icon = Icons.Default.WorkspacePremium,
                    accent = GoldWarm,
                    modifier = Modifier.weight(1f)
                )
            }

            // 2. شريط اختيار صيغة الذكر
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(phrases) { index, item ->
                    val isSelected = index == selectedIndex
                    Surface(
                        onClick = {
                            fx.click()
                            selectedIndex = index
                            count = 0
                            target = item.defaultTarget
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) EmeraldVibrant else p.card,
                        border = BorderStroke(1.2.dp, if (isSelected) GoldShine else p.border),
                        shadowElevation = if (isSelected) 4.dp else 1.dp
                    ) {
                        Text(
                            text = item.title,
                            color = if (isSelected) GoldShine else p.ink,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                        )
                    }
                }
            }

            // 3. بطاقة عرض نص الذكر بالخط العثماني العريض الواضح
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = RoyalForest,
                border = BorderStroke(1.4.dp, GoldWarm.copy(alpha = 0.7f)),
                shadowElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(RoyalForest, EmeraldVibrant)))
                ) {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activePhrase.arabicText,
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (26 * phraseScale).sp,
                            lineHeight = (40 * phraseScale).sp,
                            color = GoldShine,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (target > 0) "الهدف: $target" else "عداد مفتوح ∞",
                                    color = GoldSoftBg,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            IconButton(
                                onClick = { fx.click(); shareText(ctx, activePhrase.arabicText) },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(Icons.Default.Share, "مشاركة", tint = GoldSoftBg, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 4. أزرار تحديد الهدف السريع (33 - 100 - 1000 - مفتوح)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(33 to "33", 100 to "100", 1000 to "1000", 0 to "مفتوح ∞").forEach { (goalVal, label) ->
                    val activeGoal = target == goalVal
                    FilterChip(
                        selected = activeGoal,
                        onClick = {
                            fx.click()
                            target = goalVal
                            if (goalVal > 0 && count >= goalVal) count = 0
                        },
                        label = { Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldWarm,
                            selectedLabelColor = RoyalForest
                        )
                    )
                }
            }

            // 5. حلقة المسبحة التفاعلية الكبرى (زر التسبيح المركزي)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(235.dp)
                    .scale(buttonScale)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { incrementCounter() }
            ) {
                // الحلقة الخارجية والعداد الدائري الذهبي
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawArc(
                        color = p.border.copy(alpha = 0.55f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(listOf(GoldWarm, GoldShine, EmeraldVibrant, GoldWarm)),
                        startAngle = -90f,
                        sweepAngle = 360f * progressFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // القرص الداخلي الملكي القابل للضغط
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(192.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(EmeraldVibrant, RoyalForest)
                            )
                        )
                        .border(2.dp, GoldShine.copy(alpha = 0.75f), CircleShape)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$count",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        if (target > 0) {
                            Text(
                                text = "من $target",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldShine
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "اضغط للتسبيح",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldSoftBg.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // 6. أزرار التحكم السفلية (تصفير العداد الحالي / تصفير الدورات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        fx.click()
                        fx.vibrate(45L)
                        count = 0
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, GoldWarm),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Refresh, null, tint = GoldWarm, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير العداد", fontWeight = FontWeight.ExtraBold, color = p.ink, fontSize = 12.5.sp)
                }

                OutlinedButton(
                    onClick = {
                        fx.click()
                        fx.vibrate(45L)
                        count = 0
                        rounds = 0
                    },
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, p.border),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.RestartAlt, null, tint = p.muted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("بدء جلسة جديدة", fontWeight = FontWeight.ExtraBold, color = p.muted, fontSize = 12.5.sp)
                }
            }
        }
    }

    // نافذة إضافة صيغة تسبيح مخصصة
    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = { Text("إضافة صيغة تسبيح مخصصة", fontWeight = FontWeight.ExtraBold, color = p.ink) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customPhraseInput,
                        onValueChange = { customPhraseInput = it },
                        label = { Text("اكتب صيغة الذكر هنا") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customTargetInput,
                        onValueChange = { customTargetInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("الهدف المطلوب (مثال: 100)") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val txt = customPhraseInput.trim()
                        if (txt.isNotEmpty()) {
                            val tVal = customTargetInput.toIntOrNull() ?: 100
                            phrases.add(TasbihPhrase(txt.take(18), txt, tVal))
                            selectedIndex = phrases.lastIndex
                            count = 0
                            target = tVal
                            customPhraseInput = ""
                            showCustomDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                ) {
                    Text("إضافة", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("إلغاء", fontWeight = FontWeight.Bold, color = p.muted)
                }
            }
        )
    }
}

@Composable
private fun StatPillCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
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
                Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = p.ink)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = p.muted)
            }
        }
    }
}
