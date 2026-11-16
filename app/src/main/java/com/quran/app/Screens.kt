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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    settings: AppSettings,
    onOpenQuran: () -> Unit,
    onOpenAudioQuran: () -> Unit,
    onOpenAdhkar: () -> Unit,
    onOpenPrayer: () -> Unit,
    onContinueReading: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // بطاقة الترحيب وآية اليوم
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(primary, secondary)))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إضاءة قرآنية",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelLarge
                        )
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp
                    )
                    Text(
                        text = "سورة البقرة - آية 152",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // بطاقة متابعة آخر قراءة
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onContinueReading(settings.lastSurahNumber, settings.lastSurahName) },
            shape = RoundedCornerShape(18.dp),
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
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = primary)
                    }
                    Column {
                        Text("متابعة آخر قراءة", style = MaterialTheme.typography.labelMedium, color = primary)
                        Text(
                            text = settings.lastSurahName,
                            style = MaterialTheme.typography.titleMedium,
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
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )

        // شبكة البطاقات الأربع (2×2)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DashboardCard(
                title = "القرآن الكريم",
                subtitle = "تصفح السور والتفسير",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                accentColor = Color(0xFF1B5E20),
                modifier = Modifier.weight(1f),
                onClick = onOpenQuran
            )
            DashboardCard(
                title = "سماع القرآن",
                subtitle = "تلاوات بأعذب الأصوات",
                icon = Icons.Default.Headphones,
                accentColor = Color(0xFF0D47A1),
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
                accentColor = Color(0xFF00695C),
                modifier = Modifier.weight(1f),
                onClick = onOpenAdhkar
            )
            DashboardCard(
                title = "تعليم الصلاة",
                subtitle = "الوضوء والصلاة خطوة بخطوة",
                icon = Icons.Default.Mosque,
                accentColor = Color(0xFF8D6E63),
                modifier = Modifier.weight(1f),
                onClick = onOpenPrayer
            )
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(145.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
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
    onBack: () -> Unit,
    onSelectSurah: (SurahInfo) -> Unit
) {
    val context = LocalContext.current
    var surahs by remember { mutableStateOf<List<SurahInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, Meccan, Medinan

    LaunchedEffect(Unit) {
        try {
            surahs = QuranRepository.getSurahList(context)
            isLoading = false
        } catch (e: Exception) {
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
                title = { Text(if (isAudioMode) "سماع القرآن الكريم" else "فهرس السور") },
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
                shape = RoundedCornerShape(14.dp),
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
                            SurahGridItem(surah = surah, isAudioMode = isAudioMode, onClick = { onSelectSurah(surah) })
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredSurahs) { surah ->
                            SurahRowItem(surah = surah, isAudioMode = isAudioMode, onClick = { onSelectSurah(surah) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SurahRowItem(surah: SurahInfo, isAudioMode: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
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
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${surah.number}", fontWeight = FontWeight.Bold, color = primary)
                }
                Column {
                    Text(surah.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
private fun SurahGridItem(surah: SurahInfo, isAudioMode: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("${surah.number}", fontWeight = FontWeight.Bold, color = primary)
            }
            Text(surah.name, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
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
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var ayahs by remember { mutableStateOf<List<AyahItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedReciter by remember { mutableStateOf(settings.defaultReciter) }
    var playingAyahIndex by remember { mutableIntStateOf(-1) }
    var selectedTafsirAyah by remember { mutableStateOf<AyahItem?>(null) }
    var showReciterSheet by remember { mutableStateOf(false) }

    val mediaPlayer = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
                mediaPlayer.release()
            } catch (_: Exception) { }
        }
    }

    fun playAyahAt(index: Int) {
        if (index < 0 || index >= ayahs.size) {
            playingAyahIndex = -1
            return
        }
        val url = ayahs[index].audioUrl
        if (url.isBlank()) return
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
            mediaPlayer.setOnPreparedListener {
                playingAyahIndex = index
                it.start()
            }
            mediaPlayer.setOnCompletionListener {
                if (settings.autoPlayNext && index + 1 < ayahs.size) {
                    playAyahAt(index + 1)
                } else {
                    playingAyahIndex = -1
                }
            }
        } catch (_: Exception) {
            playingAyahIndex = -1
        }
    }

    LaunchedEffect(surahNumber, selectedReciter) {
        isLoading = true
        settingsManager.saveLastRead(surahNumber, surahName)
        try {
            ayahs = QuranRepository.getSurahDetails(context, surahNumber, selectedReciter)
            isLoading = false
            if (autoStartAudio && ayahs.isNotEmpty()) {
                playAyahAt(0)
            }
        } catch (_: Exception) {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surahName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showReciterSheet = true }) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = "اختيار القارئ")
                    }
                    IconButton(
                        onClick = {
                            if (playingAyahIndex >= 0) {
                                try { mediaPlayer.stop() } catch (_: Exception) { }
                                playingAyahIndex = -1
                            } else if (ayahs.isNotEmpty()) {
                                playAyahAt(0)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (playingAyahIndex >= 0) Icons.Default.StopCircle else Icons.Default.PlayCircleFilled,
                            contentDescription = "تشغيل السورة"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (settings.isMushafMode) {
            // وضع المصحف المتصل
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    val combinedText = buildString {
                        ayahs.forEach { ayah ->
                            append("${ayah.text} ﴿${ayah.numberInSurah}﴾ ")
                        }
                    }
                    Text(
                        text = combinedText,
                        fontSize = settings.fontSize.sp,
                        fontWeight = if (settings.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (settings.fontSize * settings.lineSpacing).sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        } else {
            // وضع البطاقات المنفصلة لكل آية
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(ayahs.size) { idx ->
                    val ayah = ayahs[idx]
                    val isPlaying = playingAyahIndex == idx
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlaying)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("${ayah.numberInSurah}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row {
                                    IconButton(onClick = { playAyahAt(idx) }, modifier = Modifier.size(34.dp)) {
                                        Icon(
                                            if (isPlaying) Icons.Default.GraphicEq else Icons.Default.VolumeUp,
                                            contentDescription = "استماع",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
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
                                text = ayah.text,
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
            title = { Text("التفسير الميسر - آية ${selectedTafsirAyah!!.numberInSurah}") },
            text = { Text(selectedTafsirAyah!!.tafsir, lineHeight = 26.sp) },
            confirmButton = {
                TextButton(onClick = { selectedTafsirAyah = null }) { Text("إغلاق") }
            }
        )
    }

    if (showReciterSheet) {
        AlertDialog(
            onDismissRequest = { showReciterSheet = false },
            title = { Text("اختر القارئ") },
            text = {
                Column {
                    QuranRepository.reciters.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReciter = id
                                    showReciterSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selectedReciter == id, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(name)
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
                        .size(210.dp)
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
                            fontSize = 48.sp,
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
                        shape = RoundedCornerShape(16.dp)
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
                            Text(item.subtitle, lineHeight = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerGuideScreen(onBack: () -> Unit) {
    val tabs = QuranRepository.prayerSteps.keys.toList()
    var selectedTab by remember { mutableIntStateOf(0) }

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
                    Card(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
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
    settingsManager: SettingsManager,
    onOpenSurah: (Int, String) -> Unit
) {
    var bookmarks by remember { mutableStateOf(settingsManager.getBookmarks()) }

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
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${item.surahName} - آية ${item.ayahNumber}",
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
                            Text(item.ayahText, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    settingsManager: SettingsManager,
    onUpdateSettings: (AppSettings) -> Unit
) {
    val context = LocalContext.current
    val themeNames = listOf("الأخضر الزمردي", "الأزرق الملكي", "الذهبي الدافئ", "الورقي الكلاسيكي")
    val themeColors = listOf(Color(0xFF1B5E20), Color(0xFF0D47A1), Color(0xFF8D6E63), Color(0xFF5D4037))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("مركز التحكم والإعدادات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        // 1. معاينة الخط وتخصيصه
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الخط والقراءة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "﴿ بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ﴾",
                        fontSize = settings.fontSize.sp,
                        fontWeight = if (settings.isBoldFont) FontWeight.Bold else FontWeight.Normal,
                        lineHeight = (settings.fontSize * settings.lineSpacing).sp
                    )
                }
                Text("حجم خط الآيات: ${settings.fontSize.toInt()}")
                Slider(
                    value = settings.fontSize,
                    onValueChange = { onUpdateSettings(settings.copy(fontSize = it)) },
                    valueRange = 18f..36f
                )
                SettingSwitchRow("خط عريض وواضح", settings.isBoldFont) {
                    onUpdateSettings(settings.copy(isBoldFont = it))
                }
                SettingSwitchRow("وضع المصحف المتصل (صفحة مترابطة)", settings.isMushafMode) {
                    onUpdateSettings(settings.copy(isMushafMode = it))
                }
            }
        }

        // 2. المظهر والألوان
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("المظهر والألوان", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("الوضع الليلي المريح للعين", settings.isDarkMode) {
                    onUpdateSettings(settings.copy(isDarkMode = it))
                }
                Text("لون ثيم التطبيق:", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    themeColors.forEachIndexed { idx, color ->
                        Box(
                            modifier = Modifier
                                .size(42.dp)
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
                                Icon(Icons.Default.Check, contentDescription = themeNames[idx], tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 3. طريقة التنقل والعرض
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("التنقل وطريقة العرض", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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

        // 4. الصوتيات والتنبيهات
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("الصوتيات والتسبيح", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                SettingSwitchRow("التشغيل التلقائي المتواصل للسورة", settings.autoPlayNext) {
                    onUpdateSettings(settings.copy(autoPlayNext = it))
                }
                SettingSwitchRow("الاهتزاز الخفيف عند التسبيح", settings.vibrateOnTasbih) {
                    onUpdateSettings(settings.copy(vibrateOnTasbih = it))
                }
                Text("القارئ الافتراضي:", fontWeight = FontWeight.Medium)
                QuranRepository.reciters.forEach { (id, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateSettings(settings.copy(defaultReciter = id)) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = settings.defaultReciter == id, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(name)
                    }
                }
            }
        }

        // 5. إدارة البيانات
        Card(shape = RoundedCornerShape(18.dp)) {
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
