package com.quran.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.launch

// لوحة الألوان الإسلامية الملكية (الزمردي والذهبي)
val EmeraldDark = Color(0xFF063527)
val EmeraldPrimary = Color(0xFF0B4F3B)
val EmeraldLight = Color(0xFF146B52)
val GoldAccent = Color(0xFFD4AF37)
val GoldLight = Color(0xFFF3E2A9)
val IvoryBg = Color(0xFFFAF7F0)
val CardCream = Color(0xFFFFFFFF)
val TextDark = Color(0xFF162922)
val TextMuted = Color(0xFF5D726A)

sealed class AppRoute {
    object Home : AppRoute()
    object SurahIndex : AppRoute()
    data class MushafPage(val initialPage: Int) : AppRoute()
    object AudioSurahs : AppRoute()
    data class AudioPlayer(val surahNumber: Int, val reciterId: String) : AppRoute()
    object Hadith : AppRoute()
    object AdhkarTasbih : AppRoute()
    object Favorites : AppRoute()
    object Settings : AppRoute()
}

@Composable
fun HomeScreen(
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by AppPreferences.settings.collectAsState()
    val audioState by QuranAudioService.playbackState.collectAsState()

    val progressFraction = (settings.lastReadPage / 604f).coerceIn(0.02f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // الهيدر الملكي الزمردي مع بطاقة آية اليوم
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(EmeraldDark, EmeraldPrimary)
                        ),
                        shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                    )
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                AppPreferences.playTouchSound(view)
                                onNavigate(AppRoute.Settings)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "الإعدادات",
                                tint = GoldLight
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "القرآن الكريم",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "مصحف المدينة المنورة والتلاوات",
                                color = GoldLight.copy(alpha = 0.85f),
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                AppPreferences.playTouchSound(view)
                                onNavigate(AppRoute.SurahIndex)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث السور",
                                tint = GoldLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // بطاقة آية اليوم
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = EmeraldDark.copy(alpha = 0.72f)
                        ),
                        border = BorderStroke(1.5.dp, GoldAccent)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = GoldAccent.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "✦ آية اليوم ✦",
                                    color = GoldLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "﴿ إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ ﴾",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سورة الإسراء • الآية ٩",
                                    color = GoldLight,
                                    fontSize = 12.sp
                                )
                                IconButton(
                                    onClick = {
                                        AppPreferences.playTouchSound(view)
                                        shareText(
                                            context,
                                            "﴿ إِنَّ هَٰذَا الْقُرْآنَ يَهْدِي لِلَّتِي هِيَ أَقْوَمُ ﴾ [سورة الإسراء: 9]"
                                        )
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(GoldAccent, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "مشاركة الآية",
                                        tint = EmeraldDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // بطاقة متابعة القراءة من حيث توقفت
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        AppPreferences.playTouchSound(view)
                        onNavigate(AppRoute.MushafPage(settings.lastReadPage))
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "متابعة القراءة",
                            color = GoldLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.lastReadSurahName,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "صفحة ${settings.lastReadPage} من ٦٠٤ • الجزء ${QuranRepository.getJuzByPage(settings.lastReadPage)}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = GoldAccent,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }

                    Button(
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.MushafPage(settings.lastReadPage))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = EmeraldDark
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "متابعة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // شريط مصغر للتلاوة النشطة في الخلفية إن وجدت
        if (audioState.isPrepared) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.AudioPlayer(audioState.surahNumber, audioState.reciterId))
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.5.dp, GoldAccent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(EmeraldPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "جاري التلاوة: ${audioState.surahName}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = audioState.reciterName,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                AppPreferences.playTouchSound(view)
                                QuranAudioService.sendCommand(context, QuranAudioService.ACTION_TOGGLE_PLAY)
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .background(GoldAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "تشغيل/إيقاف",
                                tint = EmeraldDark
                            )
                        }
                    }
                }
            }
        }

        // الشبكة الرئيسية للأقسام
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "أقسام التطبيق",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeSectionCard(
                        title = "القرآن الكريم",
                        subtitle = "صفحات المصحف الشريف",
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.SurahIndex)
                        }
                    )
                    HomeSectionCard(
                        title = "المشغل الصوتي",
                        subtitle = "تلاوات بدون إنترنت",
                        icon = Icons.Default.Headphones,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.AudioSurahs)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeSectionCard(
                        title = "الأحاديث النبوية",
                        subtitle = "الأربعون النووية وشرحها",
                        icon = Icons.Default.FormatQuote,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.Hadith)
                        }
                    )
                    HomeSectionCard(
                        title = "الأذكار والمسبحة",
                        subtitle = "حصن المسلم والتسبيح",
                        icon = Icons.Default.TouchApp,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.AdhkarTasbih)
                        }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HomeSectionCard(
                        title = "الصفحات المحفوظة",
                        subtitle = "علامات التلاوة والمفضلة",
                        icon = Icons.Default.Bookmark,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.Favorites)
                        }
                    )
                    HomeSectionCard(
                        title = "الإعدادات",
                        subtitle = "الصوت والخط والمظهر",
                        icon = Icons.Default.Settings,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            onNavigate(AppRoute.Settings)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeSectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(132.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardCream),
        border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(listOf(EmeraldPrimary, EmeraldDark)),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, GoldAccent, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = GoldLight,
                    modifier = Modifier.size(26.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =========================================================================
// 1. فهرس القرآن الكريم (للدخول المباشر لصفحات المصحف المصورة)
// =========================================================================
@Composable
fun SurahIndexScreen(
    onBack: () -> Unit,
    onOpenMushafPage: (Int) -> Unit
) {
    val view = LocalView.current
    var searchQuery by remember { mutableStateOf("") }
    var pageInput by remember { mutableStateOf("") }

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) QuranRepository.surahs
        else QuranRepository.surahs.filter {
            it.name.contains(searchQuery.trim()) || it.number.toString() == searchQuery.trim()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        AppPreferences.playTouchSound(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Text(
                        text = "فهرس المصحف الشريف",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث باسم السورة...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight) },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                        )
                    )

                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { if (it.length <= 3) pageInput = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("صفحة", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(14.dp),
                        trailingIcon = {
                            if (pageInput.isNotEmpty()) {
                                IconButton(onClick = {
                                    val p = pageInput.toIntOrNull()?.coerceIn(1, 604) ?: 1
                                    AppPreferences.playTouchSound(view)
                                    onOpenMushafPage(p)
                                }) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "انتقال", tint = GoldAccent)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSurahs, key = { it.number }) { surah ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppPreferences.playTouchSound(view)
                            onOpenMushafPage(surah.startPage)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(EmeraldPrimary, CircleShape)
                                    .border(1.5.dp, GoldAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${surah.number}",
                                    color = GoldLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = surah.name,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "${surah.type} • ${surah.ayahCount} آية",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Surface(
                            color = IvoryBg,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GoldAccent)
                        ) {
                            Text(
                                text = "صفحة ${surah.startPage}",
                                color = EmeraldDark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 2. عارض صفحات المصحف الحقيقية المصورة (604 صفحات بالتقليب الأفقي)
// =========================================================================
@Composable
fun MushafReaderScreen(
    initialPage: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by AppPreferences.settings.collectAsState()
    var favorites by remember { mutableStateOf(AppPreferences.getFavoritePages(context)) }
    var isZoomed by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, QuranRepository.TOTAL_MUSHAF_PAGES - 1),
        pageCount = { QuranRepository.TOTAL_MUSHAF_PAGES }
    )

    val currentPage = pagerState.currentPage + 1
    val currentSurah = remember(currentPage) { QuranRepository.getSurahByPage(currentPage) }
    val currentJuz = remember(currentPage) { QuranRepository.getJuzByPage(currentPage) }
    val isBookmarked = favorites.contains(currentPage)

    LaunchedEffect(currentPage) {
        AppPreferences.updateLastRead(context, currentPage, currentSurah.name)
    }

    val nightInvertMatrix = remember {
        ColorMatrix(
            floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (settings.nightMushafMode) Color(0xFF101714) else Color(0xFFF6EFE0))
    ) {
        Surface(
            color = EmeraldDark,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    AppPreferences.playTouchSound(view)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentSurah.name,
                        color = GoldLight,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "الجزء $currentJuz • صفحة $currentPage من ٦٠٤",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                Row {
                    IconButton(onClick = {
                        AppPreferences.playTouchSound(view)
                        AppPreferences.setNightMushafMode(context, !settings.nightMushafMode)
                    }) {
                        Icon(
                            imageVector = if (settings.nightMushafMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "الوضع الليلي",
                            tint = GoldLight
                        )
                    }
                    IconButton(onClick = {
                        AppPreferences.playTouchSound(view)
                        favorites = AppPreferences.toggleFavoritePage(context, currentPage)
                    }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "حفظ الصفحة",
                            tint = if (isBookmarked) GoldAccent else Color.White
                        )
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val pageNumber = pageIndex + 1
            val imageUrl = QuranRepository.getMushafPageImageUrl(pageNumber)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .border(2.dp, GoldAccent.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (settings.nightMushafMode) Color.Black else Color(0xFFFFFDF7))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                AppPreferences.playTouchSound(view)
                                isZoomed = !isZoomed
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = "صفحة المصحف $pageNumber",
                    contentScale = ContentScale.Fit,
                    colorFilter = if (settings.nightMushafMode) ColorFilter.colorMatrix(nightInvertMatrix) else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (isZoomed) 1.35f else 1f,
                            scaleY = if (isZoomed) 1.35f else 1f
                        ),
                    loading = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = GoldAccent)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "جاري تحميل صفحة $pageNumber من المصحف...",
                                color = if (settings.nightMushafMode) Color.White else TextDark,
                                fontSize = 13.sp
                            )
                        }
                    },
                    error = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "تحتاج الصفحة $pageNumber للاتصال بالإنترنت لأول مرة فقط، وبعدها تُحفظ في هاتفك للأبد.",
                                color = if (settings.nightMushafMode) Color.White else TextDark,
                                textAlign = TextAlign.Center,
                                fontSize = 14.sp
                            )
                        }
                    }
                )
            }
        }

        Surface(color = EmeraldDark) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "اسحب لليمين أو اليسار لتقليب الصفحات • انقر مرتين للتكبير",
                    color = GoldLight.copy(alpha = 0.9f),
                    fontSize = 11.sp
                )
                Text(
                    text = "﴿ $currentPage ﴾",
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// =========================================================================
// 3. المشغل الصوتي المعزول (اختيار القارئ + تنزيل أول مرة بالنسب المئوية)
// =========================================================================
@Composable
fun AudioSurahsScreen(
    onBack: () -> Unit,
    onOpenAudioPlayer: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings by AppPreferences.settings.collectAsState()
    val downloadProgress by AudioDownloadManager.downloadProgress.collectAsState()

    var selectedReciterId by remember { mutableStateOf(settings.selectedReciterId) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val currentReciter = remember(selectedReciterId) {
        QuranRepository.reciters.find { it.id == selectedReciterId } ?: QuranRepository.reciters.first()
    }

    val filteredSurahs = remember(searchQuery) {
        if (searchQuery.isBlank()) QuranRepository.surahs
        else QuranRepository.surahs.filter { it.name.contains(searchQuery.trim()) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        AppPreferences.playTouchSound(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Text(
                        text = "المشغل الصوتي المستقل",
                        color = Color.White,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(QuranRepository.reciters) { reciter ->
                        val isSelected = reciter.id == selectedReciterId
                        Card(
                            modifier = Modifier
                                .width(155.dp)
                                .clickable {
                                    AppPreferences.playTouchSound(view)
                                    selectedReciterId = reciter.id
                                    AppPreferences.setSelectedReciter(context, reciter.id)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) GoldAccent else EmeraldDark.copy(alpha = 0.7f)
                            ),
                            border = BorderStroke(1.dp, GoldLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubcomposeAsyncImage(
                                    model = reciter.imageUrl,
                                    contentDescription = reciter.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    error = {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(EmeraldLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = reciter.name,
                                    color = if (isSelected) EmeraldDark else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن السورة للاستماع أو التنزيل...", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GoldLight) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f)
                    )
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredSurahs, key = { "${selectedReciterId}_${it.number}_$refreshTrigger" }) { surah ->
                val taskKey = "${selectedReciterId}_${surah.number}"
                val progress: Int? = downloadProgress[taskKey]
                val isDownloaded = remember(selectedReciterId, surah.number, refreshTrigger, progress) {
                    AudioDownloadManager.isSurahDownloaded(context, selectedReciterId, surah.number)
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppPreferences.playTouchSound(view)
                            if (isDownloaded) {
                                val file = AudioDownloadManager.getLocalAudioFile(context, selectedReciterId, surah.number)
                                QuranAudioService.startSurah(
                                    context = context,
                                    surahNumber = surah.number,
                                    surahName = surah.name,
                                    reciterId = currentReciter.id,
                                    reciterName = currentReciter.name,
                                    filePath = file.absolutePath
                                )
                                onOpenAudioPlayer(surah.number, currentReciter.id)
                            } else if (progress == null) {
                                scope.launch {
                                    val ok = AudioDownloadManager.downloadSurah(context, currentReciter, surah.number)
                                    if (ok) refreshTrigger++
                                }
                            }
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(
                        1.dp,
                        if (isDownloaded) EmeraldLight.copy(alpha = 0.5f) else GoldAccent.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(EmeraldPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${surah.number}",
                                        color = GoldLight,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = surah.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (isDownloaded) "محفوظة على الجهاز • تعمل بدون إنترنت"
                                        else "اضغط على السهم للتنزيل مرة واحدة",
                                        fontSize = 11.sp,
                                        color = if (isDownloaded) EmeraldPrimary else TextMuted
                                    )
                                }
                            }

                            when {
                                progress != null -> {
                                    Surface(
                                        color = GoldLight.copy(alpha = 0.45f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "$progress%",
                                            color = EmeraldDark,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                isDownloaded -> {
                                    IconButton(
                                        onClick = {
                                            AppPreferences.playTouchSound(view)
                                            val file = AudioDownloadManager.getLocalAudioFile(context, selectedReciterId, surah.number)
                                            QuranAudioService.startSurah(
                                                context = context,
                                                surahNumber = surah.number,
                                                surahName = surah.name,
                                                reciterId = currentReciter.id,
                                                reciterName = currentReciter.name,
                                                filePath = file.absolutePath
                                            )
                                            onOpenAudioPlayer(surah.number, currentReciter.id)
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(EmeraldPrimary, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = GoldAccent
                                        )
                                    }
                                }
                                else -> {
                                    IconButton(
                                        onClick = {
                                            AppPreferences.playTouchSound(view)
                                            scope.launch {
                                                val ok = AudioDownloadManager.downloadSurah(context, currentReciter, surah.number)
                                                if (ok) refreshTrigger++
                                            }
                                        },
                                        modifier = Modifier
                                            .size(42.dp)
                                            .background(GoldLight.copy(alpha = 0.45f), CircleShape)
                                            .border(1.dp, GoldAccent, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = "تنزيل السورة",
                                            tint = EmeraldDark
                                        )
                                    }
                                }
                            }
                        }

                        if (progress != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = EmeraldPrimary,
                                trackColor = GoldLight
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. شاشة المشغل الصوتي الاحترافي (صورة الشيخ القارئ + أزرار التحكم الحديثة)
// =========================================================================
@Composable
fun AudioPlayerScreen(
    initialSurahNumber: Int,
    reciterId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val audioState by QuranAudioService.playbackState.collectAsState()
    val downloadProgress by AudioDownloadManager.downloadProgress.collectAsState()

    val activeSurahNumber = if (audioState.isPrepared) audioState.surahNumber else initialSurahNumber
    val activeReciter = remember(reciterId, audioState.reciterId) {
        val targetId = if (audioState.isPrepared) audioState.reciterId else reciterId
        QuranRepository.reciters.find { it.id == targetId } ?: QuranRepository.reciters.first()
    }
    val surahName = QuranRepository.getSurahNameByNumber(activeSurahNumber)

    val nextSurahNum = (activeSurahNumber + 1).coerceAtMost(114)
    val prevSurahNum = (activeSurahNumber - 1).coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldDark, EmeraldPrimary, Color(0xFF072D21))
                )
            )
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    AppPreferences.playTouchSound(view)
                    onBack()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "تصغير", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "المشغل الصوتي",
                    color = GoldLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "يعمل في الخلفية وشريط الإشعارات",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.size(42.dp))
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .shadow(16.dp, CircleShape)
                    .border(4.dp, GoldAccent, CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(EmeraldDark),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = activeReciter.imageUrl,
                    contentDescription = activeReciter.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    error = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = GoldLight,
                            modifier = Modifier.size(110.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = surahName,
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "الشيخ ${activeReciter.name}",
                color = GoldLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activeReciter.style,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val duration = audioState.durationMs.coerceAtLeast(1L)
            val position = audioState.currentPositionMs.coerceIn(0L, duration)

            Slider(
                value = position.toFloat(),
                onValueChange = { newValue ->
                    QuranAudioService.sendCommand(
                        context,
                        QuranAudioService.ACTION_SEEK_TO,
                        newValue.toLong()
                    )
                },
                valueRange = 0f..duration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = GoldAccent,
                    activeTrackColor = GoldAccent,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatMillis(position), color = GoldLight, fontSize = 12.sp)
                Text(text = formatMillis(duration), color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        if (AudioDownloadManager.isSurahDownloaded(context, activeReciter.id, prevSurahNum)) {
                            val file = AudioDownloadManager.getLocalAudioFile(context, activeReciter.id, prevSurahNum)
                            QuranAudioService.startSurah(
                                context, prevSurahNum,
                                QuranRepository.getSurahNameByNumber(prevSurahNum),
                                activeReciter.id, activeReciter.name, file.absolutePath
                            )
                        } else {
                            scope.launch {
                                if (AudioDownloadManager.downloadSurah(context, activeReciter, prevSurahNum)) {
                                    val file = AudioDownloadManager.getLocalAudioFile(context, activeReciter.id, prevSurahNum)
                                    QuranAudioService.startSurah(
                                        context, prevSurahNum,
                                        QuranRepository.getSurahNameByNumber(prevSurahNum),
                                        activeReciter.id, activeReciter.name, file.absolutePath
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "السورة السابقة", tint = Color.White, modifier = Modifier.size(28.dp))
                }

                IconButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        QuranAudioService.sendCommand(context, QuranAudioService.ACTION_SEEK_BY, -10_000L)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "ترجيع 10 ثوان", tint = GoldLight, modifier = Modifier.size(26.dp))
                }

                IconButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        if (audioState.isPrepared) {
                            QuranAudioService.sendCommand(context, QuranAudioService.ACTION_TOGGLE_PLAY)
                        } else if (AudioDownloadManager.isSurahDownloaded(context, activeReciter.id, activeSurahNumber)) {
                            val file = AudioDownloadManager.getLocalAudioFile(context, activeReciter.id, activeSurahNumber)
                            QuranAudioService.startSurah(
                                context, activeSurahNumber, surahName,
                                activeReciter.id, activeReciter.name, file.absolutePath
                            )
                        }
                    },
                    modifier = Modifier
                        .size(76.dp)
                        .shadow(12.dp, CircleShape)
                        .background(GoldAccent, CircleShape)
                ) {
                    Icon(
                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "تشغيل / إيقاف",
                        tint = EmeraldDark,
                        modifier = Modifier.size(42.dp)
                    )
                }

                IconButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        QuranAudioService.sendCommand(context, QuranAudioService.ACTION_SEEK_BY, 10_000L)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثوان", tint = GoldLight, modifier = Modifier.size(26.dp))
                }

                IconButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        if (AudioDownloadManager.isSurahDownloaded(context, activeReciter.id, nextSurahNum)) {
                            val file = AudioDownloadManager.getLocalAudioFile(context, activeReciter.id, nextSurahNum)
                            QuranAudioService.startSurah(
                                context, nextSurahNum,
                                QuranRepository.getSurahNameByNumber(nextSurahNum),
                                activeReciter.id, activeReciter.name, file.absolutePath
                            )
                        } else {
                            scope.launch {
                                if (AudioDownloadManager.downloadSurah(context, activeReciter, nextSurahNum)) {
                                    val file = AudioDownloadManager.getLocalAudioFile(context, activeReciter.id, nextSurahNum)
                                    QuranAudioService.startSurah(
                                        context, nextSurahNum,
                                        QuranRepository.getSurahNameByNumber(nextSurahNum),
                                        activeReciter.id, activeReciter.name, file.absolutePath
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = "السورة التالية", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }

            val activeDownload: Int? = downloadProgress.values.firstOrNull()
            if (activeDownload != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "جاري تنزيل السورة: $activeDownload%",
                    color = GoldLight,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

// =========================================================================
// 5. قسم الأحاديث النبوية الشريفة
// =========================================================================
@Composable
fun HadithScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    AppPreferences.playTouchSound(view)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text(
                    text = "الأحاديث النبوية الشريفة",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(QuranRepository.hadiths) { hadith ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = EmeraldPrimary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "الحديث ${hadith.id} • ${hadith.title}",
                                    color = GoldLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }

                            IconButton(onClick = {
                                AppPreferences.playTouchSound(view)
                                shareText(
                                    context,
                                    "${hadith.title}\n\n${hadith.text}\n\nالراوي: ${hadith.narrator} (${hadith.source})"
                                )
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = EmeraldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = hadith.text,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = GoldAccent.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "الشرح: ${hadith.explanation}",
                            fontSize = 13.sp,
                            color = TextMuted,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${hadith.narrator} • ${hadith.source}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. قسم الأذكار المطور + المسبحة الإلكترونية التفاعلية
// =========================================================================
@Composable
fun AdhkarAndTasbihScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by AppPreferences.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf("أذكار الصباح") }
    val countsMap = remember { mutableStateMapOf<Int, Int>() }

    val tasbihPhrases = listOf(
        "سُبْحَانَ اللَّهِ",
        "الْحَمْدُ لِلَّهِ",
        "لَا إِلَهَ إِلَّا اللَّهُ",
        "اللَّهُ أَكْبَرُ",
        "أَسْتَغْفِرُ اللَّهَ الْعَظِيمَ",
        "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ"
    )
    var currentPhraseIndex by remember { mutableIntStateOf(0) }
    var sessionTasbih by remember { mutableIntStateOf(0) }
    var targetGoal by remember { mutableIntStateOf(33) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        AppPreferences.playTouchSound(view)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                    Text(
                        text = "الأذكار والمسبحة الإلكترونية",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EmeraldDark, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("الأذكار اليومية", "المسبحة الإلكترونية").forEachIndexed { index, title ->
                        val active = selectedTab == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    AppPreferences.playTouchSound(view)
                                    selectedTab = index
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) GoldAccent else Color.Transparent
                        ) {
                            Text(
                                text = title,
                                color = if (active) EmeraldDark else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        if (selectedTab == 0) {
            val categories = listOf("أذكار الصباح", "أذكار المساء", "أذكار بعد الصلاة", "أذكار النوم")
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSel = cat == selectedCategory
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            AppPreferences.playTouchSound(view)
                            selectedCategory = cat
                        },
                        label = { Text(cat, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = GoldLight
                        )
                    )
                }
            }

            val itemsInCat = QuranRepository.adhkarList.filter { it.category == selectedCategory }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(itemsInCat, key = { it.id }) { dhikr ->
                    val current = countsMap[dhikr.id] ?: 0
                    val done = current >= dhikr.targetCount
                    val cardBg by animateColorAsState(
                        if (done) Color(0xFFE6F4EE) else CardCream,
                        label = "dhikrCard"
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppPreferences.playTouchSound(view)
                                if (settings.vibrationEnabled) triggerVibration(context, 35L)
                                if (current < dhikr.targetCount) {
                                    countsMap[dhikr.id] = current + 1
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = BorderStroke(1.2.dp, if (done) EmeraldPrimary else GoldAccent.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = dhikr.text,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                lineHeight = 29.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = dhikr.reward,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        AppPreferences.playTouchSound(view)
                                        countsMap[dhikr.id] = 0
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "إعادة", tint = TextMuted)
                                }

                                Surface(
                                    color = if (done) EmeraldPrimary else GoldAccent,
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = if (done) "✓ اكتمل (${dhikr.targetCount})" else "$current / ${dhikr.targetCount}",
                                        color = if (done) GoldLight else EmeraldDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val animatedProgress by animateFloatAsState(
                targetValue = ((sessionTasbih % targetGoal).toFloat() / targetGoal.toFloat()).coerceIn(0f, 1f),
                label = "tasbihProgress"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.2.dp, GoldAccent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إجمالي تسبيحاتك: ${settings.totalTasbihCount}",
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(33, 100, 1000).forEach { goal ->
                                    Surface(
                                        modifier = Modifier.clickable {
                                            AppPreferences.playTouchSound(view)
                                            targetGoal = goal
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (targetGoal == goal) EmeraldPrimary else IvoryBg
                                    ) {
                                        Text(
                                            text = "$goal",
                                            color = if (targetGoal == goal) GoldLight else TextDark,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = {
                                AppPreferences.playTouchSound(view)
                                currentPhraseIndex = (currentPhraseIndex - 1 + tasbihPhrases.size) % tasbihPhrases.size
                            }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "السابق", tint = EmeraldPrimary)
                            }
                            Text(
                                text = tasbihPhrases[currentPhraseIndex],
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldDark,
                                textAlign = TextAlign.Center
                            )
                            IconButton(onClick = {
                                AppPreferences.playTouchSound(view)
                                currentPhraseIndex = (currentPhraseIndex + 1) % tasbihPhrases.size
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "التالي", tint = EmeraldPrimary)
                            }
                        }
                    }
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(230.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = GoldAccent,
                        trackColor = EmeraldPrimary.copy(alpha = 0.15f),
                        strokeWidth = 10.dp
                    )

                    Surface(
                        modifier = Modifier
                            .size(195.dp)
                            .shadow(12.dp, CircleShape)
                            .clickable {
                                AppPreferences.playTouchSound(view)
                                sessionTasbih++
                                AppPreferences.incrementTotalTasbih(context)
                                if (settings.vibrationEnabled) {
                                    val duration = if (sessionTasbih % targetGoal == 0) 120L else 35L
                                    triggerVibration(context, duration)
                                }
                            },
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        border = BorderStroke(3.dp, GoldAccent)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$sessionTasbih",
                                color = Color.White,
                                fontSize = 46.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اضغط للتسبيح • الهدف $targetGoal",
                                color = GoldLight,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        AppPreferences.playTouchSound(view)
                        sessionTasbih = 0
                    },
                    border = BorderStroke(1.5.dp, EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = EmeraldPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تصفير العداد الحالي", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================================
// 7. شاشة الصفحات المحفوظة (المفضلة)
// =========================================================================
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onOpenPage: (Int) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    var pages by remember { mutableStateOf(AppPreferences.getFavoritePages(context).sorted()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    AppPreferences.playTouchSound(view)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text(
                    text = "الصفحات المحفوظة",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        if (pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "لا توجد صفحات محفوظة بعد.\nاضغط على أيقونة العلامة أثناء قراءة المصحف لحفظ الصفحة.",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pages) { page ->
                    val surah = QuranRepository.getSurahByPage(page)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppPreferences.playTouchSound(view)
                                onOpenPage(page)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = CardCream),
                        border = BorderStroke(1.dp, GoldAccent)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${surah.name} • صفحة $page",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = "الجزء ${QuranRepository.getJuzByPage(page)}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }

                            IconButton(onClick = {
                                AppPreferences.playTouchSound(view)
                                pages = AppPreferences.toggleFavoritePage(context, page).sorted()
                            }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = Color(0xFFB71C1C))
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 8. شاشة الإعدادات المطورة بتصميم البطاقات العصرية
// =========================================================================
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings by AppPreferences.settings.collectAsState()

    val downloadedSurahsCount = remember(settings.selectedReciterId) {
        AudioDownloadManager.getDownloadedCount(context, settings.selectedReciterId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IvoryBg)
    ) {
        Surface(color = EmeraldPrimary, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    AppPreferences.playTouchSound(view)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
                Text(
                    text = "إعدادات التطبيق",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDark),
                    border = BorderStroke(1.5.dp, GoldAccent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${settings.lastReadPage}", color = GoldAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("آخر صفحة", color = Color.White, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$downloadedSurahsCount", color = GoldAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("سور محملة", color = Color.White, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${settings.totalTasbihCount}", color = GoldAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text("التسبيحات", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            item {
                Text("التفاعل والصوت", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Column {
                        SettingSwitchRow(
                            title = "صوت اللمس التفاعلي",
                            subtitle = "إصدار صوت نقرة لطيفة عند الضغط على الأزرار",
                            icon = Icons.Default.VolumeUp,
                            checked = settings.touchSoundEnabled,
                            onCheckedChange = {
                                AppPreferences.setTouchSound(context, it)
                                if (it) AppPreferences.playTouchSound(view)
                            }
                        )
                        HorizontalDivider(color = IvoryBg)
                        SettingSwitchRow(
                            title = "اهتزاز المسبحة والأذكار",
                            subtitle = "اهتزاز خفيف عند كل تسبيحة وعند اكتمال الهدف",
                            icon = Icons.Default.Vibration,
                            checked = settings.vibrationEnabled,
                            onCheckedChange = {
                                AppPreferences.playTouchSound(view)
                                AppPreferences.setVibration(context, it)
                            }
                        )
                    }
                }
            }

            item {
                Text("المصحف الشريف والقراءة", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Column {
                        SettingSwitchRow(
                            title = "الوضع الليلي لصفحات المصحف",
                            subtitle = "خلفية داكنة مريحة للعين أثناء القراءة الليلية",
                            icon = Icons.Default.DarkMode,
                            checked = settings.nightMushafMode,
                            onCheckedChange = {
                                AppPreferences.playTouchSound(view)
                                AppPreferences.setNightMushafMode(context, it)
                            }
                        )
                        HorizontalDivider(color = IvoryBg)
                        SettingSwitchRow(
                            title = "إبقاء الشاشة مضاءة",
                            subtitle = "منع انطفاء الشاشة تلقائياً أثناء تلاوة القرآن",
                            icon = Icons.Default.LightMode,
                            checked = settings.keepScreenOn,
                            onCheckedChange = {
                                AppPreferences.playTouchSound(view)
                                AppPreferences.setKeepScreenOn(context, it)
                            }
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = CardCream),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
