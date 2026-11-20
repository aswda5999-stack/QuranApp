package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.util.Calendar

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
// بطاقة آية اليوم الاحترافية الثابتة بأزرار موحدة اللون والنمط
// ============================================================
@Composable
fun DailyVerseCard(nav: Nav) {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current

    // تتغير تلقائياً حسب اليوم بدون أسهم تنقل أو زر إعادة
    val verse = remember {
        val idx = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) % DailyVersesBank.size
        DailyVersesBank[idx]
    }
    var showTafsirDialog by remember { mutableStateOf(false) }
    val fullShareCopyText = "${verse.arabicText}\n[سورة ${verse.surahName} - آية ${verse.ayahNum}]"

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = RoyalForest,
        border = BorderStroke(1.5.dp, GoldWarm.copy(alpha = 0.78f)),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldDeep, EmeraldVibrant)))
        ) {
            IslamicPattern(Modifier.matchParentSize(), tint = GoldShine.copy(alpha = 0.08f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // عنوان البطاقة الثابت: "آية اليوم"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.AutoAwesome, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "آيَةُ الْيَوْمِ",
                        fontFamily = UthmanicFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = GoldShine
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Rounded.AutoAwesome, null, tint = GoldShine, modifier = Modifier.size(20.dp))
                }

                Spacer(Modifier.height(10.dp))

                // صندوق نص الآية العثماني
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black.copy(alpha = 0.24f))
                        .border(1.dp, GoldShine.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = verse.arabicText,
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 23.sp,
                            lineHeight = 38.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "سُورَةُ ${verse.surahName} • آيَةُ ${verse.ayahNum}",
                            fontFamily = UthmanicFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = GoldShine
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // 4 أزرار سفلية موحدة اللون والنمط تماماً (نسخ - عرض - مشاركة - التفسير)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UniformVerseButton(
                        label = "نسخ",
                        icon = Icons.Rounded.ContentCopy,
                        modifier = Modifier.weight(1f)
                    ) {
                        fx.click()
                        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("آية اليوم", fullShareCopyText))
                        Toast.makeText(ctx, "تم نسخ الآية ✓", Toast.LENGTH_SHORT).show()
                    }

                    UniformVerseButton(
                        label = "عرض",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        modifier = Modifier.weight(1f)
                    ) {
                        fx.click()
                        nav.openPage(verse.pageNum)
                    }

                    UniformVerseButton(
                        label = "مشاركة",
                        icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f)
                    ) {
                        fx.click()
                        shareText(ctx, fullShareCopyText)
                    }

                    UniformVerseButton(
                        label = "التفسير",
                        icon = Icons.Rounded.Description,
                        modifier = Modifier.weight(1f)
                    ) {
                        fx.click()
                        showTafsirDialog = true
                    }
                }
            }
        }
    }

    if (showTafsirDialog) {
        AlertDialog(
            onDismissRequest = { showTafsirDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = p.card,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Description, null, tint = GoldWarm, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "تفسير سورة ${verse.surahName} (${verse.ayahNum})",
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
                    Text("عرض في المصحف", fontWeight = FontWeight.ExtraBold, color = GoldShine)
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
private fun UniformVerseButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.1.dp, GoldShine.copy(alpha = 0.55f)),
        modifier = modifier.height(54.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = GoldShine,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp
            )
        }
    }
}
