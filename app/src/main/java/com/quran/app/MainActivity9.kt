@file:OptIn(ExperimentalMaterial3Api::class)

package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ============================================================
// 1. نموذج بيانات الذكر وقائمتي الصباح والمساء (3 لكل قسم)
// ============================================================
data class DhikrItem(
    val id: String,
    val text: String,
    val virtue: String,
    val targetCount: Int
)

private val MorningAdhkarList: List<DhikrItem> = listOf(
    DhikrItem(
        id = "m1",
        text = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ.",
        virtue = "من أذكار الصباح الثابتة • مرة واحدة",
        targetCount = 1
    ),
    DhikrItem(
        id = "m2",
        text = "اللَّهُمَّ بِكَ أَصْبَحْنَا، وَبِكَ أَمْسَيْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ النُّشُورُ.",
        virtue = "سنة نبوية عند الصباح • مرة واحدة",
        targetCount = 1
    ),
    DhikrItem(
        id = "m3",
        text = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ: عَدَدَ خَلْقِهِ، وَرِضَا نَفْسِهِ، وَزِنَةَ عَرْشِهِ، وَمِدَادَ كَلِمَاتِهِ.",
        virtue = "تُقال ثلاث مرات صباحاً",
        targetCount = 3
    )
)

private val EveningAdhkarList: List<DhikrItem> = listOf(
    DhikrItem(
        id = "e1",
        text = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لَا إِلَٰهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَىٰ كُلِّ شَيْءٍ قَدِيرٌ.",
        virtue = "من أذكار المساء الثابتة • مرة واحدة",
        targetCount = 1
    ),
    DhikrItem(
        id = "e2",
        text = "اللَّهُمَّ بِكَ أَمْسَيْنَا، وَبِكَ أَصْبَحْنَا، وَبِكَ نَحْيَا، وَبِكَ نَمُوتُ، وَإِلَيْكَ الْمَصِيرُ.",
        virtue = "سنة نبوية عند المساء • مرة واحدة",
        targetCount = 1
    ),
    DhikrItem(
        id = "e3",
        text = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ.",
        virtue = "تُقال ثلاث مرات مساءً للحفظ والتحصين",
        targetCount = 3
    )
)

// ============================================================
// 2. شاشة الأذكار التفاعلية المتطورة (AdhkarScreen)
// ============================================================
@Composable
fun AdhkarScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: أذكار الصباح، 1: أذكار المساء
    var fontScale by remember { mutableFloatStateOf(1.0f) }

    // خريطة تخزين العداد المتبقي لكل ذكر
    val remainingMap = remember {
        mutableStateMapOf<String, Int>().apply {
            (MorningAdhkarList + EveningAdhkarList).forEach { put(it.id, it.targetCount) }
        }
    }

    val activeList = if (selectedTab == 0) MorningAdhkarList else EveningAdhkarList
    val completedCount = activeList.count { (remainingMap[it.id] ?: it.targetCount) == 0 }
    val progressAnim by animateFloatAsState(
        targetValue = if (activeList.isNotEmpty()) completedCount.toFloat() / activeList.size else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "adhkarProgress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        ScreenHeader(
            title = "أذكار الصباح والمساء",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); fontScale = (fontScale - 0.1f).coerceAtLeast(0.8f) }) {
                    Icon(Icons.Default.TextDecrease, "تصغير الخط", tint = GoldSoftBg)
                }
                IconButton(onClick = { fx.click(); fontScale = (fontScale + 0.1f).coerceAtMost(1.4f) }) {
                    Icon(Icons.Default.TextIncrease, "تكبير الخط", tint = GoldSoftBg)
                }
                IconButton(
                    onClick = {
                        fx.click()
                        activeList.forEach { remainingMap[it.id] = it.targetCount }
                        Toast.makeText(ctx, "تمت إعادة ضبط عدادات القسم ✓", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Refresh, "إعادة الضبط", tint = GoldShine)
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. أزرار التبديل بين أذكار الصباح والمساء
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CategoryTabButton(
                    title = "أذكار الصباح",
                    icon = Icons.Default.WbSunny,
                    selected = selectedTab == 0,
                    modifier = Modifier.weight(1f),
                    onClick = { fx.click(); selectedTab = 0 }
                )
                CategoryTabButton(
                    title = "أذكار المساء",
                    icon = Icons.Default.NightsStay,
                    selected = selectedTab == 1,
                    modifier = Modifier.weight(1f),
                    onClick = { fx.click(); selectedTab = 1 }
                )
            }

            // 2. بطاقة شريط التقدم للقسم الحالي
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = p.card,
                border = BorderStroke(1.dp, GoldWarm.copy(alpha = 0.45f)),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTab == 0) "إنجاز أذكار الصباح" else "إنجاز أذكار المساء",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.5.sp,
                            color = p.ink
                        )
                        Text(
                            text = "$completedCount / ${activeList.size} مكتمل",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp,
                            color = EmeraldVibrant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressAnim },
                        color = EmeraldVibrant,
                        trackColor = p.border.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(CircleShape)
                    )
                }
            }

            // 3. قائمة بطاقات الأذكار التفاعلية
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(activeList, key = { _, item -> item.id }) { index, item ->
                    val left = remainingMap[item.id] ?: item.targetCount
                    val isDone = left == 0

                    val cardBg by animateColorAsState(
                        targetValue = if (isDone) EmeraldVibrant.copy(alpha = 0.13f) else p.card,
                        label = "dhikrCardBg"
                    )
                    val borderClr by animateColorAsState(
                        targetValue = if (isDone) EmeraldVibrant else GoldWarm.copy(alpha = 0.55f),
                        label = "dhikrCardBorder"
                    )

                    Surface(
                        onClick = {
                            if (left > 0) {
                                fx.click()
                                fx.vibrate(28L)
                                st.totalTasbih += 1
                                val nextVal = left - 1
                                remainingMap[item.id] = nextVal
                                if (nextVal == 0 && st.stageVib) {
                                    fx.vibrate(110L)
                                }
                            }
                        },
                        shape = RoundedCornerShape(22.dp),
                        color = cardBg,
                        border = BorderStroke(1.3.dp, borderClr),
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // صف علوي: رقم الذكر + شارة العداد التفاعلي
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NumberBadge(index + 1, 32)

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDone) EmeraldVibrant else GoldWarm,
                                    border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                            contentDescription = null,
                                            tint = if (isDone) GoldShine else RoyalForest,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isDone) "تم ✓" else "المتبقي: $left",
                                            color = if (isDone) Color.White else RoyalForest,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            // نص الذكر بالخط العثماني العريض
                            Text(
                                text = item.text,
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = (21 * fontScale).sp,
                                lineHeight = (36 * fontScale).sp,
                                color = p.ink,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = p.border.copy(alpha = 0.45f))

                            // صف سفلي: الفضل + أيقونتا النسخ والمشاركة
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.virtue,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = p.muted,
                                    modifier = Modifier.weight(1f)
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    IconButton(
                                        onClick = {
                                            fx.click()
                                            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            cm.setPrimaryClip(ClipData.newPlainText("ذكر", item.text))
                                            Toast.makeText(ctx, "تم نسخ الذكر ✓", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldVibrant.copy(alpha = 0.1f))
                                    ) {
                                        Icon(Icons.Default.ContentCopy, "نسخ", tint = EmeraldVibrant, modifier = Modifier.size(16.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            fx.click()
                                            shareText(ctx, item.text)
                                        },
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(GoldWarm.copy(alpha = 0.14f))
                                    ) {
                                        Icon(Icons.Default.Share, "مشاركة", tint = GoldWarm, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) EmeraldVibrant else p.card,
        border = BorderStroke(1.3.dp, if (selected) GoldShine else p.border),
        shadowElevation = if (selected) 4.dp else 1.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 11.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) GoldShine else GoldWarm,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = title,
                color = if (selected) Color.White else p.ink,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.5.sp
            )
        }
    }
}
