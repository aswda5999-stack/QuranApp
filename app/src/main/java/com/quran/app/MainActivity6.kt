package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

// ============================================================
// 1. نموذج بيانات آية اليوم ومستودع الآيات المختارة وتفسيرها
// ============================================================
data class DailyVerseItem(
    val arabicText: String,
    val surahName: String,
    val surahNum: Int,
    val ayahNum: Int,
    val pageNum: Int,
    val tafsir: String
)

private val DailyVersesBank: List<DailyVerseItem> = listOf(
    DailyVerseItem(
        arabicText = "﴿ فَإِنَّ مَعَ الْعُسْرِ يُسْرًا ۝ إِنَّ مَعَ الْعُسْرِ يُسْرًا ﴾",
        surahName = "الشرح",
        surahNum = 94,
        ayahNum = 5,
        pageNum = 596,
        tafsir = "بشارة ربانية عظيمة بأن كل ضيقٍ وشدةٍ يصحبهما فرجٌ قريب وتيسيرٌ من الله تعالى، ولن يغلب عسرٌ واحدٌ يسرين."
    ),
    DailyVerseItem(
        arabicText = "﴿ وَمَن يَتَّقِ اللَّهَ يَجْعَل لَّهُ مَخْرَجًا ۝ وَيَرْزُقْهُ مِنْ حَيْثُ لَا يَحْتَسِبُ ﴾",
        surahName = "الطلاق",
        surahNum = 65,
        ayahNum = 2,
        pageNum = 558,
        tafsir = "من راقب الله في سره وعلانيته، هيّأ الله له أبواب الفرج من حيث لا يتوقع، وكفاه أمر دنياه وآخرته."
    ),
    DailyVerseItem(
        arabicText = "﴿ أَلَا بِذِكْرِ اللَّهِ تَطْمَئِنُّ الْقُلُوبُ ﴾",
        surahName = "الرعد",
        surahNum = 13,
        ayahNum = 28,
        pageNum = 252,
        tafsir = "سكون النفس وزوال القلق والاضطراب لا يتحققان إلا بدوام ذكر الله وتلاوة كتابه وحسن التوكل عليه."
    ),
    DailyVerseItem(
        arabicText = "﴿ وَقُل رَّبِّ زِدْنِي عِلْمًا ﴾",
        surahName = "طه",
        surahNum = 20,
        ayahNum = 114,
        pageNum = 320,
        tafsir = "لم يأمر الله نبيه ﷺ بطلب الزيادة من شيء في الدنيا إلا من العلم النافع؛ لما فيه من رفعة الدرجات ونور البصيرة."
    ),
    DailyVerseItem(
        arabicText = "﴿ ادْعُونِي أَسْتَجِبْ لَكُمْ ﴾",
        surahName = "غافر",
        surahNum = 40,
        ayahNum = 60,
        pageNum = 474,
        tafsir = "وعدٌ صادق من الله بإجابة دعاء السائلين متى أخلصوا له الدعاء وأحسنوا الظن بكرمه وعطائه."
    ),
    DailyVerseItem(
        arabicText = "﴿ إِنَّ اللَّهَ مَعَ الصَّابِرِينَ ﴾",
        surahName = "البقرة",
        surahNum = 2,
        ayahNum = 153,
        pageNum = 23,
        tafsir = "معية الله الخاصة بالنصر والتأييد والسكينة لأهل الصبر والثبات عند الابتلاءات ومشاق الطاعات."
    ),
    DailyVerseItem(
        arabicText = "﴿ وَاصْبِرْ فَإِنَّ اللَّهَ لَا يُضِيعُ أَجْرَ الْمُحْسِنِينَ ﴾",
        surahName = "هود",
        surahNum = 11,
        ayahNum = 115,
        pageNum = 234,
        tafsir = "تثبيت للقلب على الطاعة والإحسان بأن كل عملٍ صالحٍ وصبرٍ جميل محفوظٌ عند الله ومُجازًى عليه بأوفى الثواب."
    )
)

// ============================================================
// 2. بطاقة آية اليوم المستقلة المتطورة (DailyVerseCard)
// ============================================================
@Composable
fun DailyVerseCard(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    val dayIndex = remember {
        Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % DailyVersesBank.size
    }
    var verseIndex by remember { mutableIntStateOf(dayIndex) }
    var showTafsirDialog by remember { mutableStateOf(false) }

    val verse = DailyVersesBank[verseIndex]
    val fullShareCopyText = "${verse.arabicText}\n[سورة ${verse.surahName} - آية ${verse.ayahNum}]"

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = RoyalForest,
        border = BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.75f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(RoyalForest, EmeraldVibrant, Color(0xFF0E4D38))
                    )
                )
        ) {
            IslamicPattern(Modifier.matchParentSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 1. صف الترويسة: عنوان "آية اليوم" الكبير بالخط العثماني العريض + زر تبديل الآية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Brush.linearGradient(listOf(GoldShine, GoldWarm)))
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = RoyalForest,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        // كلمة "آية اليوم" كبيرة وعريضة بالخط العثماني
                        Text(
                            text = "آيَةُ الْيَوْمِ",
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 26.sp,
                            color = GoldShine
                        )
                    }

                    // زر تجديد الآية بتصميم أنيق ومتوازن
                    Surface(
                        onClick = {
                            fx.click()
                            verseIndex = (verseIndex + 1) % DailyVersesBank.size
                        },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.4f)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "آية أخرى",
                                tint = GoldSoftBg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 2. صندوق الآية بالخط العثماني الذي أضفته (UthmanicFontFamily)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.22f))
                        .border(1.dp, GoldShine.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verse.arabicText,
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 24.sp,
                            lineHeight = 40.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(10.dp))

                        // شارة اسم السورة ورقم الآية
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GoldShine.copy(alpha = 0.16f),
                            border = BorderStroke(0.8.dp, GoldShine.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "سُورَةُ ${verse.surahName} • آيَةُ ${verse.ayahNum}",
                                fontFamily = UthmanicFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = GoldShine,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // 3. شريط الأزرار المتوازن والمنظّم (نسخ، مشاركة، عرض في المصحف + زر الشرح)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // مجموعة الأيقونات الثلاث المتساوية (نسخ - مشاركة - عرض)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VerseActionCircleButton(
                            icon = Icons.Default.ContentCopy,
                            contentDesc = "نسخ الآية",
                            onClick = {
                                fx.click()
                                val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("آية اليوم", fullShareCopyText))
                                Toast.makeText(ctx, "تم نسخ الآية بنجاح ✓", Toast.LENGTH_SHORT).show()
                            }
                        )

                        VerseActionCircleButton(
                            icon = Icons.Default.Share,
                            contentDesc = "مشاركة الآية",
                            onClick = {
                                fx.click()
                                shareText(ctx, fullShareCopyText)
                            }
                        )

                        VerseActionCircleButton(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            contentDesc = "عرض في المصحف",
                            onClick = {
                                fx.click()
                                nav.openPage(verse.pageNum)
                            }
                        )
                    }

                    // زر الشرح والتفسير البارز والمتوازن
                    Surface(
                        onClick = {
                            fx.click()
                            showTafsirDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = GoldShine,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = "الشرح والتدبر",
                                tint = RoyalForest,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "الشرح والتدبر",
                                color = RoyalForest,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة التفسير والشرح المختصر للآية
    if (showTafsirDialog) {
        AlertDialog(
            onDismissRequest = { showTafsirDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "تدبر سورة ${verse.surahName} (${verse.ayahNum})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = p.ink
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = EmeraldVibrant.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, EmeraldVibrant.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = verse.arabicText,
                            fontFamily = UthmanicFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 34.sp,
                            color = p.ink,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                    Text(
                        text = verse.tafsir,
                        fontSize = 14.5.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = p.ink
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTafsirDialog = false; nav.openPage(verse.pageNum) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)
                ) {
                    Text("اقرأ في المصحف", fontWeight = FontWeight.ExtraBold, color = GoldShine)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTafsirDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.Bold, color = p.muted)
                }
            }
        )
    }
}

@Composable
private fun VerseActionCircleButton(
    icon: ImageVector,
    contentDesc: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.1.dp, GoldShine.copy(alpha = 0.55f)),
        modifier = Modifier.size(42.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = GoldShine,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
