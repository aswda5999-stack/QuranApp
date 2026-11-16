package com.quran.app

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    settings: AppSettings,
    uthmanicFont: FontFamily,
    onOpenQuran: () -> Unit,
    onOpenAudioQuran: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenPrayer: () -> Unit,
    onContinueReading: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val corner = settings.cardCornerRadius.dp
    val quranFont = if (settings.useUthmanicFont) uthmanicFont else FontFamily.Default

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة الإضاءة القرآنية بالخط العثماني
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(corner + 4.dp),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(primary, secondary)))
                    .padding(22.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "إضاءة قرآنية",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "﴿ فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ ﴾ [البقرة: 152]")
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة الآية"))
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
                        }
                    }
                    Text(
                        text = "﴿ فَاذْكُرُونِي أَذْكُرْكُمْ وَاشْكُرُوا لِي وَلَا تَكْفُرُونِ ﴾",
                        color = Color.White,
                        fontFamily = quranFont,
                        fontSize = 23.sp,
                        lineHeight = 38.sp
                    )
                    Text(
                        text = "سورة البقرة • الآية 152",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // شريط الإحصائيات السريعة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMiniCard(
                label = "الآيات المقروءة",
                value = "${settings.totalReadAyahs}",
                icon = Icons.Default.AutoStories,
                corner = corner,
                modifier = Modifier.weight(1f)
            )
            StatMiniCard(
                label = "مجموع التسبيح",
                value = "${settings.tasbihCount}",
                icon = Icons.Default.Fingerprint,
                corner = corner,
                modifier = Modifier.weight(1f)
            )
        }

        // بطاقة متابعة آخر قراءة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onContinueReading(settings.lastSurahNumber, settings.lastSurahName) },
            shape = RoundedCornerShape(corner),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = primary)
                    }
                    Column {
                        Text("متابعة آخر قراءة", style = MaterialTheme.typography.labelMedium, color = primary)
                        Text(
                            text = settings.lastSurahName,
                            fontFamily = quranFont,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(Icons.Default.ChevronLeft, contentDescription = "فتح", tint = primary)
            }
        }

        Text(
            text = "الأقسام الرئيسية",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // شبكة البطاقات الأربع (2×2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "القرآن الكريم",
                subtitle = "المصحف والتفسير الميسر",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = primary,
                corner = corner,
                modifier = Modifier.weight(1f),
                onClick = onOpenQuran
            )
            DashboardCard(
                title = "المشغل الصوتي",
                subtitle = "تلاوات كاملة متصلة",
                icon = Icons.Default.Headphones,
                accentColor = Color(0xFF0277BD),
                corner = corner,
                modifier = Modifier.weight(1f),
                onClick = onOpenAudioQuran
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "الأذكار والسبحة",
                subtitle = "حصن المسلم والتسبيح",
                icon = Icons.Default.BrightnessLow,
                accentColor = Color(0xFF00796B),
                corner = corner,
                modifier = Modifier.weight(1f),
                onClick = onOpenAdhkar
            )
            DashboardCard(
                title = "تعليم الصلاة",
                subtitle = "الوضوء والصلاة خطوة بخطوة",
                icon = Icons.Default.Mosque,
                accentColor = Color(0xFF8D6E63),
                corner = corner,
                modifier = Modifier.weight(1f),
                onClick = onOpenPrayer
            )
        }
    }
}

@Composable
private fun StatMiniCard(
    label: String,
    value: String,
    icon: ImageVector,
    corner: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = primary)
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    corner: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(148.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(corner),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(28.dp))
            }
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahListScreen(
    isAudioMode: Boolean,
    settings: AppSettings,
    uthmanicFont: FontFamily,
    onBack: () -> Unit,
    onSelectSurah: (SurahInfo) -> Unit
) {
    val context = LocalContext.current
    var surahs by remember { mutableStateOf<List<SurahInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }
    val corner = settings.cardCornerRadius.dp
    val quranFont = if (settings.useUthmanicFont) uthmanicFont else FontFamily.Default

    LaunchedEffect(Unit) {
        try {
            surahs = QuranRepository.getSurahList(context)
            isLoading = false
        } catch (_: Exception) {
            errorMsg = "تعذر تحميل السور. تأكد من اتصالك بالإنترنت."
            isLoading = false
        }
    }

    val filteredSurahs = surahs.filter {
        val matchesSearch = it.name.contains(searchQuery) || it.number.toString() == searchQuery
        val matchesType = filterType == "ALL" || it.revelationType.equals(filterType, ignoreCase = true)
        matchesSearch && matchesType
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isAudioMode) "المشغل الصوتي المتصل" else "المصحف الشريف") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("ابحث باسم السورة أو رقمها...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "مسح")
                        }
                    }
                },
                shape = RoundedCornerShape(corner),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = filterType == "ALL", onClick = { filterType = "ALL" }, label = { Text("الكل (114)") })
                FilterChip(selected = filterType == "Meccan", onClick = { filterType = "Meccan" }, label = { Text("مكية") })
                FilterChip(selected = filterType == "Medinan", onClick = { filterType = "Medinan" }, label = { Text("مدنية") })
            }

            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                errorMsg != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
                settings.isGridView -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            SurahGridItem(surah, isAudioMode, corner, quranFont) { onSelectSurah(surah) }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            SurahRowItem(surah, isAudioMode, corner, quranFont) { onSelectSurah(surah) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahRowItem(
    surah: SurahInfo,
    isAudioMode: Boolean,
    corner: androidx.compose.ui.unit.Dp,
    quranFont: FontFamily,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(corner),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${surah.number}", fontWeight = FontWeight.Bold, color = primary)
                }
                Column {
                    Text(surah.name, fontFamily = quranFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(
                        text = "${if (surah.revelationType == "Meccan") "مكية" else "مدنية"} • ${surah.numberOfAyahs} آية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (isAudioMode) Icons.Default.PlayCircleOutline else Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = primary
            )
        }
    }
}

@Composable
private fun SurahGridItem(
    surah: SurahInfo,
    isAudioMode: Boolean,
    corner: androidx.compose.ui.unit.Dp,
    quranFont: FontFamily,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(corner)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${surah.number}", fontWeight = FontWeight.Bold, color = primary)
            }
            Text(surah.name, fontFamily = quranFont, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1)
            Text(
                text = "${surah.numberOfAyahs} آية",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isAudioMode) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    surahNumber: Int,
    surahName: String,
    autoStartAudio: Boolean,
    settings: AppSettings,
    settingsManager: SettingsManager,
    uthmanicFont: FontFamily,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var ayahs by remember { mutableStateOf<List<AyahItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReciterId by remember { mutableStateOf(settings.defaultReciterId) }
    var selectedTafsirAyah by remember { mutableStateOf<AyahItem?>(null) }
    var showReciterSheet by remember { mutableStateOf(false) }

    // حالة المشغل الصوتي المستقل المتصل للسورة كاملة
    var showPlayerBar by remember { mutableStateOf(autoStartAudio) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf(1) }

    val corner = settings.cardCornerRadius.dp
    val quranFont = if (settings.useUthmanicFont) uthmanicFont else FontFamily.Default
    val mediaPlayer = remember { MediaPlayer() }

    fun formatTime(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%02d:%02d".format(min, sec)
    }

    fun startFullSurahPlayback() {
        val url = QuranRepository.buildFullSurahAudioUrl(selectedReciterId, surahNumber)
        showPlayerBar = true
        isBuffering = true
        try {
            mediaPlayer.reset()
            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                isBuffering = false
                totalDurationMs = mp.duration.coerceAtLeast(1)
                mp.start()
                isPlaying = true
            }
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
                currentPosMs = 0
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                isBuffering = false
                isPlaying = false
                Toast.makeText(context, "تعذر تشغيل التلاوة، تأكد من الاتصال", Toast.LENGTH_SHORT).show()
                true
            }
        } catch (_: Exception) {
            isBuffering = false
            isPlaying = false
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            try {
                if (mediaPlayer.isPlaying) {
                    currentPosMs = mediaPlayer.currentPosition
                }
            } catch (_: Exception) { }
            delay(500)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(surahNumber) {
        isLoading = true
        try {
            ayahs = QuranRepository.getSurahDetails(context, surahNumber)
            settingsManager.saveLastRead(surahNumber, surahName, ayahs.size)
            isLoading = false
            if (autoStartAudio) {
                startFullSurahPlayback()
            }
        } catch (_: Exception) {
            isLoading = false
        }
    }

    val currentReciterName = QuranRepository.fullSurahReciters.find { it.id == selectedReciterId }?.name ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(surahName, fontFamily = quranFont, fontSize = 20.sp)
                        if (ayahs.isNotEmpty()) {
                            Text(
                                "الجزء ${ayahs.first().juz} • الصفحة ${ayahs.first().page}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showReciterSheet = true }) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "القارئ")
                    }
                    IconButton(
                        onClick = {
                            if (showPlayerBar) {
                                try {
                                    if (mediaPlayer.isPlaying) mediaPlayer.stop()
                                } catch (_: Exception) { }
                                isPlaying = false
                                showPlayerBar = false
                            } else {
                                startFullSurahPlayback()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (showPlayerBar) Icons.Default.Close else Icons.Default.Headphones,
                            contentDescription = "المشغل الصوتي"
                        )
                    }
                }
            )
        },
        bottomBar = {
            // المشغل الصوتي المستقل المتكامل للسورة كاملة بدون تقطّع
            if (showPlayerBar) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = corner + 4.dp, topEnd = corner + 4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = currentReciterName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showReciterSheet = true }
                                )
                            }
                            Text(
                                text = "${formatTime(currentPosMs)} / ${formatTime(totalDurationMs)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Slider(
                            value = currentPosMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                            onValueChange = { newPos ->
                                currentPosMs = newPos.toInt()
                                try { mediaPlayer.seekTo(currentPosMs) } catch (_: Exception) { }
                            },
                            valueRange = 0f..totalDurationMs.toFloat()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val target = (currentPosMs - 10000).coerceAtLeast(0)
                                    try { mediaPlayer.seekTo(target); currentPosMs = target } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.Default.Replay10, contentDescription = "تأخير 10 ثواني")
                            }

                            FilledIconButton(
                                onClick = {
                                    if (isBuffering) return@FilledIconButton
                                    try {
                                        if (isPlaying) {
                                            mediaPlayer.pause()
                                            isPlaying = false
                                        } else if (currentPosMs > 0) {
                                            mediaPlayer.start()
                                            isPlaying = true
                                        } else {
                                            startFullSurahPlayback()
                                        }
                                    } catch (_: Exception) {
                                        startFullSurahPlayback()
                                    }
                                },
                                modifier = Modifier.size(52.dp)
                            ) {
                                if (isBuffering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل/إيقاف",
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val target = (currentPosMs + 10000).coerceAtMost(totalDurationMs)
                                    try { mediaPlayer.seekTo(target); currentPosMs = target } catch (_: Exception) { }
                                }
                            ) {
                                Icon(Icons.Default.Forward10, contentDescription = "تقديم 10 ثواني")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (settings.isMushafMode) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(corner),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    val combinedText = buildString {
                        ayahs.forEach { ayah ->
                            append(ayah.text)
                            if (settings.showAyahMarkers) append(" ﴿${ayah.numberInSurah}﴾ ") else append(" ")
                        }
                    }
                    Text(
                        text = combinedText,
                        fontFamily = quranFont,
                        fontSize = settings.fontSize.sp,
                        fontWeight = if (settings.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(ayahs) { ayah ->
                    Card(
                        shape = RoundedCornerShape(corner),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${ayah.numberInSurah}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row {
                                    IconButton(onClick = { selectedTafsirAyah = ayah }, modifier = Modifier.size(34.dp)) {
                                        Icon(Icons.Default.Info, contentDescription = "التفسير", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            settingsManager.addBookmark(
                                                BookmarkItem(surahNumber, surahName, ayah.numberInSurah, ayah.text)
                                            )
                                            Toast.makeText(context, "تم حفظ الإشارة المرجعية", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.BookmarkBorder, contentDescription = "إشارة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = {
                                            val share = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, "﴿ ${ayah.text} ﴾ [$surahName: ${ayah.numberInSurah}]")
                                            }
                                            context.startActivity(Intent.createChooser(share, "مشاركة الآية"))
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Text(
                                text = if (settings.showAyahMarkers) "${ayah.text} ﴿${ayah.numberInSurah}﴾" else ayah.text,
                                fontFamily = quranFont,
                                fontSize = settings.fontSize.sp,
                                fontWeight = if (settings.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                                lineHeight = (settings.fontSize * settings.lineSpacing).sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedTafsirAyah != null) {
        AlertDialog(
            onDismissRequest = { selectedTafsirAyah = null },
            title = { Text("التفسير الميسر • آية ${selectedTafsirAyah!!.numberInSurah}") },
            text = { Text(selectedTafsirAyah!!.tafsir, lineHeight = 26.sp) },
            confirmButton = {
                TextButton(onClick = { selectedTafsirAyah = null }) { Text("إغلاق") }
            }
        )
    }

    if (showReciterSheet) {
        AlertDialog(
            onDismissRequest = { showReciterSheet = false },
            title = { Text("اختر القارئ (تلاوة متصلة)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    QuranRepository.fullSurahReciters.forEach { reciter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReciterId = reciter.id
                                    showReciterSheet = false
                                    if (showPlayerBar) startFullSurahPlayback()
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedReciterId == reciter.id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(reciter.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciterSheet = false }) { Text("إغلاق") }
            }
        )
    }
}

@Composable
fun AdhkarScreen(settings: AppSettings, settingsManager: SettingsManager) {
    val context = LocalContext.current
    val categories = QuranRepository.adhkarCategories.keys.toList() + "السبحة الإلكترونية"
    var selectedTab by remember { mutableIntStateOf(0) }
    var tasbihCount by remember { mutableIntStateOf(settings.tasbihCount) }
    val countsMap = remember { mutableStateMapOf<String, Int>() }
    val corner = settings.cardCornerRadius.dp

    fun triggerVibrate() {
        if (!settings.vibrateOnTasbih) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                @Suppress("DEPRECATION")
                v.vibrate(35)
            }
        } catch (_: Exception) { }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("الأذكار والسبحة", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories.size) { index ->
                FilterChip(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    label = { Text(categories[index]) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (categories[selectedTab] == "السبحة الإلكترونية") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("سبّح بحمد ربك", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable {
                            tasbihCount++
                            settingsManager.saveTasbihCount(tasbihCount)
                            triggerVibrate()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$tasbihCount",
                            fontSize = 50.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("اضغط للتسبيح", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = {
                        tasbihCount = 0
                        settingsManager.saveTasbihCount(0)
                    }
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("تصفير العداد")
                }
            }
        } else {
            val itemsList = QuranRepository.adhkarCategories[categories[selectedTab]] ?: emptyList()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(itemsList) { item ->
                    val key = "${selectedTab}_${item.title}"
                    val remaining = countsMap[key] ?: item.count
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (remaining > 0) {
                                    countsMap[key] = remaining - 1
                                    triggerVibrate()
                                }
                            },
                        shape = RoundedCornerShape(corner)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Surface(
                                    shape = CircleShape,
                                    color = if (remaining == 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (remaining == 0) "تم ✓" else "المتبقي: $remaining",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = if (remaining == 0) Color.White else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Text(item.subtitle, lineHeight = 25.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerGuideScreen(settings: AppSettings, onBack: () -> Unit) {
    val tabs = QuranRepository.prayerSteps.keys.toList()
    var selectedTab by remember { mutableIntStateOf(0) }
    val corner = settings.cardCornerRadius.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تعليم الوضوء والصلاة") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tabs.forEachIndexed { idx, title ->
                    FilterChip(selected = selectedTab == idx, onClick = { selectedTab = idx }, label = { Text(title) })
                }
            }
            Spacer(Modifier.height(12.dp))
            val steps = QuranRepository.prayerSteps[tabs[selectedTab]] ?: emptyList()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(steps) { step ->
                    Card(shape = RoundedCornerShape(corner), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(step.title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            Text(step.subtitle, lineHeight = 25.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksScreen(
    settings: AppSettings,
    uthmanicFont: FontFamily,
    settingsManager: SettingsManager,
    onOpenSurah: (Int, String) -> Unit
) {
    var bookmarks by remember { mutableStateOf(settingsManager.getBookmarks()) }
    val corner = settings.cardCornerRadius.dp
    val quranFont = if (settings.useUthmanicFont) uthmanicFont else FontFamily.Default

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("الإشارات المرجعية", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        if (bookmarks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد إشارات محفوظة بعد", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(bookmarks) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenSurah(item.surahNumber, item.surahName) },
                        shape = RoundedCornerShape(corner)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${item.surahName} • آية ${item.ayahNumber}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = {
                                        settingsManager.removeBookmark(item)
                                        bookmarks = settingsManager.getBookmarks()
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text(item.ayahText, fontFamily = quranFont, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    uthmanicFont: FontFamily,
    settingsManager: SettingsManager,
    onRequestNotificationPermission: () -> Unit,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val corner = settings.cardCornerRadius.dp
    val quranFont = if (settings.useUthmanicFont) uthmanicFont else FontFamily.Default

    val themeColors = listOf(
        Color(0xFF1B5E20), // الأخضر الملكي
        Color(0xFF0D47A1), // الكحلي
        Color(0xFF8D6E63), // الذهبي الأندلسي
        Color(0xFF5D4037), // الورقي الكلاسيكي
        Color(0xFF00695C), // الفيروزي
        Color(0xFF4A148C)  // الأرجواني الهادئ
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("مركز التحكم والإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(
                    "الإصدار 2.0",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 1. الخط العثماني والمعاينة الحية
        Card(shape = RoundedCornerShape(corner)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الخط العثماني والقراءة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(corner))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "﴿ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ﴾",
                        fontFamily = quranFont,
                        fontSize = settings.fontSize.sp,
                        fontWeight = if (settings.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (settings.fontSize * settings.lineSpacing).sp
                    )
                }
                SettingSwitchRow("استخدام الخط العثماني الأصلي (مجمع الملك فهد)", settings.useUthmanicFont) {
                    onUpdateSettings(settings.copy(useUthmanicFont = it))
                }
                Text("حجم خط الآيات: ${settings.fontSize.toInt()}")
                Slider(
                    value = settings.fontSize,
                    onValueChange = { onUpdateSettings(settings.copy(fontSize = it)) },
                    valueRange = 18f..38f
                )
                Text("تباعد الأسطر: ${String.format("%.1f", settings.lineSpacing)}")
                Slider(
                    value = settings.lineSpacing,
                    onValueChange = { onUpdateSettings(settings.copy(lineSpacing = it)) },
                    valueRange = 1.3f..2.3f
                )
                SettingSwitchRow("خط عريض وواضح", settings.isBoldFont) {
                    onUpdateSettings(settings.copy(isBoldFont = it))
                }
                SettingSwitchRow("وضع المصحف المتصل (صفحة مترابطة)", settings.isMushafMode) {
                    onUpdateSettings(settings.copy(isMushafMode = it))
                }
                SettingSwitchRow("إظهار فواصل وأرقام الآيات ﴿١﴾", settings.showAyahMarkers) {
                    onUpdateSettings(settings.copy(showAyahMarkers = it))
                }
            }
        }

        // 2. المظهر والألوان والزوايا
        Card(shape = RoundedCornerShape(corner)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الألوان والزوايا والتصميم", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("الوضع الليلي المريح للعين", settings.isDarkMode) {
                    onUpdateSettings(settings.copy(isDarkMode = it))
                }
                Text("ثيم ألوان التطبيق (6 ألوان):", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (settings.themeIndex == idx) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { onUpdateSettings(settings.copy(themeIndex = idx)) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.themeIndex == idx) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Text("انحناء زوايا البطاقات: ${settings.cardCornerRadius.toInt()}dp")
                Slider(
                    value = settings.cardCornerRadius,
                    onValueChange = { onUpdateSettings(settings.copy(cardCornerRadius = it)) },
                    valueRange = 8f..30f
                )
                SettingSwitchRow("إظهار النصوص تحت أيقونات الشريط السفلي", settings.showNavLabels) {
                    onUpdateSettings(settings.copy(showNavLabels = it))
                }
                SettingSwitchRow("عرض السور كشبكة بطاقات مزدوجة", settings.isGridView) {
                    onUpdateSettings(settings.copy(isGridView = it))
                }
                SettingSwitchRow("إبقاء الشاشة مضاءة أثناء القراءة", settings.keepScreenOn) {
                    onUpdateSettings(settings.copy(keepScreenOn = it))
                }
            }
        }

        // 3. الإشعارات والأذونات
        Card(shape = RoundedCornerShape(corner)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الإشعارات والتنبيهات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("تفعيل إشعارات الأذكار والورد اليومي", settings.notificationsEnabled) { enabled ->
                    onUpdateSettings(settings.copy(notificationsEnabled = enabled))
                    if (enabled) onRequestNotificationPermission()
                }
                OutlinedButton(
                    onClick = {
                        onRequestNotificationPermission()
                        QuranNotificationHelper.showReminderNotification(
                            context,
                            "وردك القرآني اليومي",
                            "لا تنسَ قراءة وردك من القرآن الكريم وأذكار الصباح والمساء."
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("إرسال إشعار تجريبي الآن")
                }
            }
        }

        // 4. الصوتيات والتسبيح
        Card(shape = RoundedCornerShape(corner)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المشغل الصوتي والتسبيح", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("الاهتزاز الخفيف عند التسبيح", settings.vibrateOnTasbih) {
                    onUpdateSettings(settings.copy(vibrateOnTasbih = it))
                }
                Text("القارئ الافتراضي للتلاوة المتصلة:", fontWeight = FontWeight.Medium)
                QuranRepository.fullSurahReciters.forEach { reciter ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateSettings(settings.copy(defaultReciterId = reciter.id)) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.defaultReciterId == reciter.id, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(reciter.name)
                    }
                }
            }
        }

        // 5. إدارة التخزين والبيانات
        Card(shape = RoundedCornerShape(corner)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("إدارة التخزين والبيانات", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(
                    onClick = {
                        QuranRepository.clearCache(context)
                        Toast.makeText(context, "تم تنظيف ذاكرة التخزين المؤقت", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حذف السور المحفوظة مؤقتاً (Cache)")
                }
                OutlinedButton(
                    onClick = {
                        settingsManager.resetAll()
                        onUpdateSettings(AppSettings())
                        Toast.makeText(context, "تمت إعادة ضبط المصنع", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("إعادة ضبط جميع الإعدادات")
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
