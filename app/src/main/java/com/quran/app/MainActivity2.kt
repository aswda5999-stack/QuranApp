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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

    val background = if (isDarkTheme) RoyalPalette.EmeraldDeep else Color(0xFFF7F5EE)
    val surface = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val primary = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal
    val secondary = if (isDarkTheme) RoyalPalette.CreamText.copy(alpha = 0.72f) else RoyalPalette.InkMuted
    val border = RoyalPalette.GoldRoyal.copy(alpha = if (isDarkTheme) 0.55f else 0.48f)

    val actions = remember {
        listOf(
            HomeActionItem("quran", "القرآن الكريم", "الفهرس والمصحف", Icons.Filled.MenuBook),
            HomeActionItem("audio", "الصوتيات", "استمع للتلاوات", Icons.Filled.Headphones),
            HomeActionItem("adhan", "مواقيت الصلاة", "الأذان والتنبيهات", Icons.Filled.Notifications),
            HomeActionItem("qibla", "اتجاه القبلة", "حدد اتجاه الكعبة", Icons.Filled.Explore),
            HomeActionItem("adhkar", "الأذكار", "أذكار ومسبحة", Icons.Filled.Favorite),
            HomeActionItem("favorites", "المفضلة", "صفحاتك المحفوظة", Icons.Filled.Bookmark),
            HomeActionItem("khatma", "خطة الختمة", "تابع وردك اليومي", Icons.Filled.AutoStories),
            HomeActionItem("settings", "الإعدادات", "خصص تجربتك", Icons.Filled.Settings)
        )
    }

    val quickSurahs = remember {
        listOf(
            "الفاتحة" to 1, "البقرة" to 2, "آل عمران" to 50,
            "الكهف" to 293, "يس" to 440, "الرحمن" to 531,
            "الواقعة" to 534, "الملك" to 562, "الإخلاص" to 604
        )
    }
    val matches = remember(searchQuery) {
        val query = searchQuery.trim()
        if (query.isEmpty()) emptyList()
        else quickSurahs.filter {
            it.first.contains(query, ignoreCase = true) || it.second.toString() == query
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "السلام عليكم ورحمة الله",
                        color = primary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "$dayName  •  $hijriDate",
                        color = secondary,
                        fontSize = 12.sp,
                        fontFamily = indexFont
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White,
                    border = BorderStroke(1.dp, border),
                    shadowElevation = 2.dp
                ) {
                    IconButton(onClick = { searchExpanded = !searchExpanded }) {
                        Icon(
                            imageVector = if (searchExpanded) Icons.Filled.Close else Icons.Filled.Search,
                            contentDescription = "بحث",
                            tint = primary
                        )
                    }
                }
            }
            if (searchExpanded || searchQuery.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("ابحث عن سورة أو رقم صفحة") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RoyalPalette.GoldRoyal,
                        unfocusedBorderColor = border,
                        focusedContainerColor = surface,
                        unfocusedContainerColor = surface,
                        focusedTextColor = primary,
                        unfocusedTextColor = primary
                    )
                )
                if (matches.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = surface),
                        border = BorderStroke(1.dp, border),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            matches.forEach { (name, page) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onSelectPageFromSearch(page) }
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سورة $name", color = primary, fontWeight = FontWeight.SemiBold)
                                    Text("صفحة $page", color = secondary, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp))
                    .clickable { onOpenQuranIndex() },
                shape = RoundedCornerShape(26.dp),
                color = RoyalPalette.EmeraldRoyal,
                border = BorderStroke(1.4.dp, RoyalPalette.GoldHighlight),
                shadowElevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = RoyalPalette.EmeraldDeep,
                        border = BorderStroke(1.dp, RoyalPalette.GoldRoyal)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.quran_realistic_photo),
                            contentDescription = "القرآن الكريم",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(18.dp))
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "رفيقك مع القرآن",
                            color = RoyalPalette.GoldHighlight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "القرآن الكريم",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "افتح المصحف وتابع تلاوتك",
                            color = Color.White.copy(alpha = 0.82f),
                            fontSize = 12.sp
                        )
                    }
                    Text("‹", color = RoyalPalette.GoldHighlight, fontSize = 34.sp)
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = surface),
                border = BorderStroke(1.dp, border),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("آية اليوم", color = primary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        verseOfDay.first,
                        color = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald,
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(verseOfDay.second, color = RoyalPalette.GoldMuted, fontSize = 12.sp)
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .clickable { onContinueLastRead() },
                shape = RoundedCornerShape(18.dp),
                color = if (isDarkTheme) RoyalPalette.GoldRoyal else Color(0xFFE9D28C),
                border = BorderStroke(1.dp, RoyalPalette.GoldHighlight)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("أكمل من حيث وصلت", color = RoyalPalette.EmeraldDeep, fontWeight = FontWeight.Bold)
                        Text("آخر قراءة: الصفحة $lastReadPage", color = RoyalPalette.EmeraldDeep.copy(alpha = 0.75f), fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = RoyalPalette.EmeraldDeep, modifier = Modifier.size(25.dp))
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("استكشف التطبيق", color = primary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("٨ خدمات", color = secondary, fontSize = 11.sp)
            }
        }

        items(actions.chunked(2).size) { rowIndex ->
            val rowActions = actions.chunked(2)[rowIndex]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowActions.forEach { action ->
                    Surface(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(19.dp))
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
                        shape = RoundedCornerShape(19.dp),
                        color = surface,
                        border = BorderStroke(1.dp, border),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp, horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(43.dp),
                                shape = CircleShape,
                                color = if (isDarkTheme) RoyalPalette.GoldRoyal.copy(alpha = 0.16f) else RoyalPalette.CreamSurface
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(action.icon, contentDescription = action.title, tint = primary, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(
                                action.title,
                                color = primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                action.subtitle,
                                color = secondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                "اللهم اجعل القرآن ربيع قلوبنا ونور صدورنا",
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                color = secondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
