package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioRecitersScreen(
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }

    val reciters = remember {
        listOf(
            "مشاري راشد العفاسي",
            "ماهر المعيقلي",
            "عبد الباسط عبد الصمد",
            "ياسر الدوسري",
            "سعد الغامدي",
            "ناصر القطامي",
            "محمد صديق المنشاوي",
            "محمود خليل الحصري"
        )
    }

    val surahsIndex = remember {
        listOf(
            "الفاتحة", "البقرة", "آل عمران", "النساء", "المائدة", "الأنعام",
            "الكهف", "يس", "الرحمن", "الواقعة", "الملك", "الناس"
        )
    }

    var selectedReciter by remember { mutableStateOf(reciters.first()) }
    var showRecitersVerticalList by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var activeSurah by remember { mutableStateOf<String?>(null) }

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val gold = RoyalPalette.GoldRoyal
    val cream = RoyalPalette.CreamText

    val filteredReciters = remember(searchQuery, reciters) {
        if (searchQuery.isBlank()) reciters
        else reciters.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    val filteredSurahs = remember(searchQuery, surahsIndex) {
        if (searchQuery.isBlank()) surahsIndex
        else surahsIndex.filter { it.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(emeraldBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = cardColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBackHome) {
                        Icon(Icons.Rounded.ArrowForward, contentDescription = "رجوع", tint = gold)
                    }

                    Button(
                        onClick = { showRecitersVerticalList = !showRecitersVerticalList },
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = RoyalPalette.EmeraldRoyal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "تغيير القارئ",
                            fontFamily = indexFont,
                            fontSize = 14.sp,
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
                        placeholder = { Text("ابحث عن قارئ أو سورة...", color = cream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cream,
                            unfocusedTextColor = cream,
                            focusedBorderColor = gold,
                            unfocusedBorderColor = gold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (showRecitersVerticalList) {
            Text(
                text = "اختر القارئ",
                color = gold,
                fontFamily = indexFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredReciters) { reciter ->
                    val isSelected = reciter == selectedReciter
                    Surface(
                        color = if (isSelected) gold.copy(alpha = 0.2f) else cardColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isSelected) gold else gold.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedReciter = reciter
                                showRecitersVerticalList = false
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = reciter,
                                color = cream,
                                fontFamily = indexFont,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = gold)
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "القارئ الحالي: $selectedReciter",
                        color = cream,
                        fontFamily = indexFont,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (activeSurah != null) {
                        Text(
                            text = "يشغّل: $activeSurah",
                            color = gold,
                            fontFamily = indexFont,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredSurahs) { surah ->
                    val playing = activeSurah == surah
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (playing) gold else gold.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeSurah = if (playing) null else surah
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "سورة $surah",
                                color = cream,
                                fontFamily = indexFont,
                                fontSize = 16.sp
                            )
                            Icon(
                                imageVector = if (playing) Icons.Rounded.PauseCircleFilled else Icons.Rounded.PlayCircleFilled,
                                contentDescription = null,
                                tint = gold,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
