package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.app.data.AppPreferencesRepository

data class EssentialDhikr(
    val id: Int,
    val title: String,
    val text: String,
    val target: Int
)

@Composable
fun EssentialAdhkarScreen(
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val adhkarFont = remember(context) { CustomFontLoader.adhkarFont(context) }
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }

    val essentialList = remember {
        listOf(
            EssentialDhikr(1, "تسبيح وتنزيه", "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ", 3),
            EssentialDhikr(2, "سيد الاستغفار", "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ", 1),
            EssentialDhikr(3, "الرضا بالله", "رَضِيتُ بِاللَّهِ رَبًّا، وَبِالْإِسْلَامِ دِينًا، وَبِمُحَمَّدٍ صَلَّى اللَّهُ عَلَيْهِ وَسَلَّمَ نَبِيًّا", 3),
            EssentialDhikr(4, "حوقلة ودعاء", "لَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ الْعَلِيِّ الْعَظِيمِ", 10),
            EssentialDhikr(5, "التهليل", "لَا إِلَهَ إِلَّا اللَّهُ وَحْدَهُ لَا شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ", 10),
            EssentialDhikr(6, "الصلاة على النبي ﷺ", "اللَّهُمَّ صَلِّ وَسَلِّمْ وَبَارِكْ عَلَى نَبِيِّنَا مُحَمَّدٍ وَعَلَى آلِهِ وَصَحْبِهِ أَجْمَعِينَ", 10),
            EssentialDhikr(7, "الاستغفار", "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ الَّذِي لَا إِلَهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ وَأَتُوبُ إِلَيْهِ", 10)
        )
    }

    val preferences = remember(context) { AppPreferencesRepository(context) }
    val counts = remember {
        mutableStateMapOf<Int, Int>().apply {
            essentialList.forEach { put(it.id, preferences.dhikrCount(it.id)) }
        }
    }
    var lastActionMessage by remember { mutableStateOf("اضغط على مربع العداد لزيادة الذكر") }

    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val cardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val lightCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val primaryInk = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val filteredAdhkar = remember(searchQuery, essentialList) {
        val q = searchQuery.trim()
        if (q.isEmpty()) essentialList else essentialList.filter { it.text.contains(q) || it.title.contains(q) }
    }

    fun copyDhikr(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("dhikr", text))
        lastActionMessage = "تم نسخ الذكر إلى الحافظة"
    }

    fun shareDhikr(text: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "مشاركة الذكر"))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.6.dp, goldBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "أهم الأذكار والمسبحة",
                            color = RoyalPalette.CreamText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont
                        )
                        Text(
                            text = lastActionMessage,
                            color = RoyalPalette.GoldHighlight,
                            fontSize = 12.sp,
                            fontFamily = indexFont
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            counts.clear()
                            preferences.resetDhikrCounts(essentialList.map { it.id })
                            lastActionMessage = "تم تصفير جميع العدادات"
                        },
                        border = BorderStroke(1.dp, RoyalPalette.GoldHighlight),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("تصفير الكل", color = RoyalPalette.GoldHighlight, fontSize = 11.sp, fontFamily = indexFont)
                    }
                }
            }
        }

        items(filteredAdhkar, key = { it.id }) { dhikr ->
            val currentCount = counts[dhikr.id] ?: 0
            val completed = currentCount >= dhikr.target
            val counterBoxBg = if (completed) RoyalPalette.GoldRoyal else cardBg
            val counterTextColor = if (completed) RoyalPalette.EmeraldDeep else RoyalPalette.CreamText

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = lightCardBg,
                border = BorderStroke(1.3.dp, goldBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dhikr.title,
                            color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = indexFont
                        )
                        RoyalBadgePill(label = "المطلوب: ${dhikr.target}")
                    }

                    Text(
                        text = dhikr.text,
                        color = primaryInk,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = adhkarFont,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Start
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1.4f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    val next = (currentCount + 1).coerceAtMost(dhikr.target)
                                    counts[dhikr.id] = next
                                    preferences.saveDhikrCount(dhikr.id, next)
                                    lastActionMessage = if (next >= dhikr.target) "أتممت ${dhikr.title}" else "تم تسجيل ذكر ($next / ${dhikr.target})"
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = counterBoxBg,
                            border = BorderStroke(1.2.dp, goldBorder)
                        ) {
                            Text(
                                text = if (completed) "✓ مكتمل ($currentCount / ${dhikr.target})" else "تسبيح ($currentCount / ${dhikr.target})",
                                color = counterTextColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = indexFont,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }

                        Button(
                            onClick = { copyDhikr(dhikr.text) },
                            modifier = Modifier.weight(0.8f),
                            colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = RoyalPalette.CreamText),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("نسخ", fontSize = 12.sp, fontFamily = indexFont)
                        }

                        OutlinedButton(
                            onClick = { shareDhikr(dhikr.text) },
                            modifier = Modifier.weight(0.8f),
                            border = BorderStroke(1.dp, goldBorder),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Text("مشاركة", color = primaryInk, fontSize = 12.sp, fontFamily = indexFont)
                        }
                    }
                }
            }
        }
    }
}
