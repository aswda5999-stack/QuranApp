package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class HomeActionItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun HomeScreen(
    lastReadPage: Int,
    verseOfDay: Pair<String, String>,
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenQuranIndex: () -> Unit,
    onContinueLastRead: () -> Unit,
    onOpenAudioReciters: () -> Unit,
    onOpenAdhanAndMuezzin: () -> Unit,
    onOpenQibla: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenKhatma: () -> Unit,
    onToggleDarkTheme: () -> Unit,
    onSelectPageFromSearch: (Int) -> Unit
) {
    val context = LocalContext.current
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }
    val adhkarFont = remember(context) { CustomFontLoader.adhkarFont(context) }
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }

    var clockText by remember { mutableStateOf(DateTimeHelper.currentClock()) }
    var dayName by remember { mutableStateOf(DateTimeHelper.currentDayName()) }
    var hijriDate by remember { mutableStateOf(DateTimeHelper.currentHijriDate()) }
    var searchExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            clockText = DateTimeHelper.currentClock()
            dayName = DateTimeHelper.currentDayName()
            hijriDate = DateTimeHelper.currentHijriDate()
            delay(30_000L)
        }
    }

    // التصميم الأول: خلفية كريمية دافئة (#FBF7EC)، بطاقات زمردية عميقة (#0B3B2B)، وحدود ذهبية ناعمة (#D4AF37)
    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val emeraldBanner = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val verseCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.CreamBackground
    val verseTitleColor = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.InkEmerald
    val verseTextColor = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val quickActions = remember {
        listOf(
            HomeActionItem("quran", "القرآن", "الفهرس والمصحف", Icons.Filled.MenuBook),
            HomeActionItem("audio", "الصوتيات", "تلاوات القراء", Icons.Filled.Headphones),
            HomeActionItem("adhan", "المؤذن", "مواقيت الصلاة", Icons.Filled.Notifications),
            HomeActionItem("qibla", "القبلة", "اتجاه الكعبة", Icons.Filled.Explore),
            HomeActionItem("adhkar", "الأذكار", "أذكار ومسبحة", Icons.Filled.Favorite),
            HomeActionItem("favorites", "المفضلة", "صفحات محفوظة", Icons.Filled.Bookmark),
            HomeActionItem("khatma", "الختمة", "متابعة الورد", Icons.Filled.AutoStories),
            HomeActionItem("settings", "الإعدادات", "الخط والمظهر", Icons.Filled.Settings)
        )
    }

    val quickSurahs = remember {
        listOf(
            "الفاتحة" to 1,
            "البقرة" to 2,
            "آل عمران" to 50,
            "الكهف" to 293,
            "يس" to 440,
            "الرحمن" to 531,
            "الواقعة" to 534,
            "الملك" to 562,
            "الإخلاص" to 604
        )
    }
    val matchingSurahs = remember(searchQuery, quickSurahs) {
        val q = searchQuery.trim()
        if (q.isEmpty()) emptyList()
        else quickSurahs.filter { it.first.contains(q) || it.second.toString() == q }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. شريط التاريخ والوقت العلوي + عدسة البحث (التصميم الأول)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    color = emeraldBanner,
                    border = BorderStroke(1.5.dp, goldBorder),
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$dayName • $hijriDate",
                            color = RoyalPalette.CreamText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = indexFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        RoyalBadgePill(label = clockText)
                    }
                }

                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = emeraldBanner,
                    border = BorderStroke(1.5.dp, goldBorder),
                    shadowElevation = 2.dp
                ) {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "بحث",
                            tint = RoyalPalette.GoldHighlight,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            if (searchExpanded || searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = {
                        Text(
                            text = "ابحث عن سورة أو رقم صفحة (١ - ٦٠٤)...",
                            fontSize = 13.sp,
                            fontFamily = indexFont
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = goldBorder,
                        unfocusedBorderColor = goldBorder.copy(alpha = 0.7f),
                        focusedContainerColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White,
                        unfocusedContainerColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White,
                        focusedTextColor = verseTitleColor,
                        unfocusedTextColor = verseTitleColor
                    )
                )

                if (matchingSurahs.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White,
                        border = BorderStroke(1.2.dp, goldBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            matchingSurahs.forEach { (surahName, pageNum) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectPageFromSearch(pageNum) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "سورة $surahName",
                                        color = verseTitleColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = indexFont
                                    )
                                    RoyalBadgePill(label = "صفحة $pageNum")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. البطاقة الزمردية الرئيسية للقرآن الكريم مع الصورة الواقعية (التصميم الأول)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { onOpenQuranIndex() },
                shape = RoundedCornerShape(22.dp),
                color = emeraldBanner,
                border = BorderStroke(1.8.dp, goldBorder),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = RoyalPalette.EmeraldDeep,
                        border = BorderStroke(1.5.dp, goldBorder)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.quran_realistic_photo),
                            contentDescription = "القرآن الكريم",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "القرآن الكريم",
                            color = RoyalPalette.CreamText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "تصفح الفهرس والصفحات والمصحف الشريف ◀",
                            color = RoyalPalette.GoldHighlight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = indexFont,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 3. بطاقة آية اليوم بإطار ذهبي أنيق (التصميم الأول)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = verseCardBg,
                border = BorderStroke(1.6.dp, goldBorder),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "آية اليوم",
                        color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = verseOfDay.first,
                        color = verseTextColor,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = adhkarFont,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = verseOfDay.second,
                        color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = indexFont
                    )
                }
            }
        }

        // 4. شريط متابعة آخر قراءة الذهبي (التصميم الأول)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onContinueLastRead() },
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkTheme) RoyalPalette.GoldRoyal else RoyalPalette.GoldRoyal.copy(alpha = 0.92f),
                border = BorderStroke(1.2.dp, if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldMuted),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "متابعة آخر قراءة • صفحة $lastReadPage",
                        color = RoyalPalette.EmeraldDeep,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = indexFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 5. شبكة الأقسام السريعة (صفوف متناسقة على طراز التصميم الأول)
        val rows = quickActions.chunked(4)
        items(rows.size) { rowIndex ->
            val rowItems = rows[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { action ->
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                when (action.id) {
                                    "quran" -> onOpenQuranIndex()
                                    "audio" -> onOpenAudioReciters()
                                    "adhan" -> onOpenAdhanAndMuezzin()
                                    "qibla" -> onOpenQibla()
                                    "adhkar" -> onOpenAdhkar()
                                    "favorites" -> onOpenFavorites()
                                    "khatma" -> onOpenKhatma()
                                    "settings" -> onOpenSettings()
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = emeraldBanner,
                        border = BorderStroke(1.4.dp, goldBorder),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 3.dp, vertical = 9.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                modifier = Modifier.size(28.dp),
                                shape = CircleShape,
                                color = RoyalPalette.GoldRoyal.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, RoyalPalette.GoldHighlight.copy(alpha = 0.7f))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = action.icon,
                                        contentDescription = action.title,
                                        tint = RoyalPalette.GoldHighlight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = action.title,
                                color = RoyalPalette.CreamText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = indexFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = action.subtitle,
                                color = RoyalPalette.GoldHighlight.copy(alpha = 0.9f),
                                fontSize = 8.sp,
                                fontFamily = indexFont,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
