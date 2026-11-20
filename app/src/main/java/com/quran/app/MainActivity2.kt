package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class HomeActionItem(
    val id: String,
    val title: String,
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
    onOpenSettings: () -> Unit,
    onOpenSectionById: (String) -> Unit
) {
    val context = LocalContext.current
    val homeTitleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }
    val adhkarFont = remember(context) { CustomFontLoader.adhkarFont(context) }

    var searchExpanded by remember { mutableStateOf(false) }
    var clockText by remember { mutableStateOf(DateTimeHelper.currentClock()) }
    var hijriText by remember { mutableStateOf(DateTimeHelper.currentHijriDate()) }
    var dayNameText by remember { mutableStateOf(DateTimeHelper.currentDayName()) }

    LaunchedEffect(Unit) {
        while (true) {
            clockText = DateTimeHelper.currentClock()
            hijriText = DateTimeHelper.currentHijriDate()
            dayNameText = DateTimeHelper.currentDayName()
            delay(15_000L)
        }
    }

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardSurface = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val goldAccent = RoyalPalette.GoldRoyal
    val softCream = RoyalPalette.CreamText

    val gridItems = remember {
        listOf(
            HomeActionItem("quran", "القرآن الكريم", Icons.Rounded.MenuBook),
            HomeActionItem("audio", "الصوتيات", Icons.Rounded.Headphones),
            HomeActionItem("adhan", "المؤذن والوقت", Icons.Rounded.NotificationsActive),
            HomeActionItem("qibla", "القبلة", Icons.Rounded.Explore),
            HomeActionItem("adhkar", "الأذكار", Icons.Rounded.AutoAwesome),
            HomeActionItem("bookmarks", "المفضلة", Icons.Rounded.Bookmark),
            HomeActionItem("khatma", "الختمة", Icons.Rounded.DoneAll),
            HomeActionItem("duas", "أدعية مأثورة", Icons.Rounded.VolunteerActivism),
            HomeActionItem("names", "أسماء الله", Icons.Rounded.Stars),
            HomeActionItem("tafseer", "التفسير", Icons.Rounded.LibraryBooks),
            HomeActionItem("alerts", "التنبيهات الصوتية", Icons.Rounded.Alarm),
            HomeActionItem("settings", "الإعدادات", Icons.Rounded.Settings)
        )
    }

    val filteredItems = remember(searchQuery, gridItems) {
        if (searchQuery.isBlank()) gridItems
        else gridItems.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize().background(emeraldBg)) {
        IslamicGeometricOverlay(alpha = if (isDarkTheme) 0.05f else 0.08f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = cardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "$dayNameText • $clockText",
                            color = goldAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = hijriText,
                            color = softCream.copy(alpha = 0.9f),
                            fontSize = 12.sp
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
                            tint = goldAccent
                        )
                    }
                }
            }

            if (searchExpanded) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    placeholder = { Text("ابحث في الأقسام...", color = softCream.copy(alpha = 0.6f)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = softCream,
                        unfocusedTextColor = softCream,
                        focusedBorderColor = goldAccent,
                        unfocusedBorderColor = goldAccent.copy(alpha = 0.5f),
                        cursorColor = goldAccent
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Surface(
                color = cardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenQuranIndex() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.quran_realistic_photo),
                        contentDescription = "القرآن الكريم",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "القرآن الكريم",
                            color = goldAccent,
                            fontFamily = homeTitleFont,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تصفح الفهرس والصفحات بوضوح وسلاسة",
                            color = softCream.copy(alpha = 0.88f),
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = null,
                        tint = goldAccent
                    )
                }
            }

            Surface(
                color = cardSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "آية اليوم",
                        color = goldAccent,
                        fontFamily = homeTitleFont,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = verseOfDay.first,
                        color = softCream,
                        fontFamily = adhkarFont,
                        fontSize = 18.sp,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = verseOfDay.second,
                        color = goldAccent.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            Surface(
                color = goldAccent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onContinueLastRead() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = null,
                            tint = goldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "متابعة آخر قراءة • صفحة $lastReadPage",
                            color = softCream,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = null,
                        tint = goldAccent
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    val titleFont = if (item.id == "quran") homeTitleFont else FontFamily.SansSerif
                    Surface(
                        color = cardSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .height(90.dp)
                            .clickable {
                                when (item.id) {
                                    "quran" -> onOpenQuranIndex()
                                    "audio" -> onOpenAudioReciters()
                                    "adhan" -> onOpenAdhanAndMuezzin()
                                    "qibla" -> onOpenQibla()
                                    "adhkar" -> onOpenAdhkar()
                                    "settings" -> onOpenSettings()
                                    else -> onOpenSectionById(item.id)
                                }
                            }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = goldAccent,
                                modifier = Modifier.size(25.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.title,
                                color = softCream,
                                fontFamily = titleFont,
                                fontSize = if (item.id == "quran") 14.sp else 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
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
