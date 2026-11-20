package com.quran.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EssentialDhikr(
    val id: Int,
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

    val essentialList = remember {
        listOf(
            EssentialDhikr(1, "سبحان الله وبحمده، سبحان الله العظيم", 3),
            EssentialDhikr(2, "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك المصير", 1),
            EssentialDhikr(3, "أستغفر الله العظيم الذي لا إله إلا هو الحي القيوم وأتوب إليه", 3),
            EssentialDhikr(4, "لا إله إلا الله وحده لا شريك له، له الملك وله الحمد وهو على كل شيء قدير", 10),
            EssentialDhikr(5, "اللهم صلِّ وسلم على نبينا محمد", 10),
            EssentialDhikr(6, "حسبي الله لا إله إلا هو عليه توكلت وهو رب العرش العظيم", 7)
        )
    }

    val counts = remember { mutableStateMapOf<Int, Int>() }
    var searchExpanded by remember { mutableStateOf(false) }

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val gold = RoyalPalette.GoldRoyal
    val cream = RoyalPalette.CreamText
    val counterActiveBg = Color(0xFFD99B26)
    val counterDoneBg = Color(0xFF2E7D5B)

    val filtered = remember(searchQuery, essentialList) {
        if (searchQuery.isBlank()) essentialList
        else essentialList.filter { it.text.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(emeraldBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            color = cardColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackHome) {
                            Icon(Icons.Rounded.ArrowForward, contentDescription = "رجوع", tint = gold)
                        }
                        Text(
                            text = "أهم الأذكار",
                            color = cream,
                            fontFamily = adhkarFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            searchExpanded = !searchExpanded
                            if (!searchExpanded) onSearchQueryChange("")
                        }
                    ) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "بحث",
                            tint = gold
                        )
                    }
                }

                if (searchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        singleLine = true,
                        placeholder = { Text("ابحث في الأذكار...", color = cream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cream,
                            unfocusedTextColor = cream,
                            focusedBorderColor = gold,
                            unfocusedBorderColor = gold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(filtered, key = { _, item -> item.id }) { _, item ->
                val currentCount = counts[item.id] ?: 0
                val isCompleted = currentCount >= item.target

                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val next = (currentCount + 1).coerceAtMost(item.target)
                            counts[item.id] = next
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = item.text,
                                color = cream,
                                fontFamily = adhkarFont,
                                fontSize = 16.sp,
                                lineHeight = 26.sp,
                                modifier = Modifier.weight(1f)
                            )

                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(
                                        color = when {
                                            isCompleted -> counterDoneBg
                                            currentCount > 0 -> counterActiveBg
                                            else -> Color(0xFF0A241B)
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Text(
                                    text = "$currentCount / ${item.target}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color(0xFF1F634B),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("dhikr", item.text))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "نسخ",
                                        tint = gold,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text("نسخ", color = cream, fontSize = 12.sp)
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = Color(0xFF8C651C),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.clickable {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, item.text)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة الذكر"))
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "مشاركة",
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text("مشاركة", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
