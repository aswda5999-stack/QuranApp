@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// ============================================================
// 1. الشاشة الرئيسية الجديدة كلياً (HomeScreen)
// - ترويسة المصحف الكريم + "القرآن الكريم"
// - شبكة 12 زراً احترافياً (3 صفوف × 4 أزرار متجاورة)
// - بطاقة آية اليوم الثابتة بأزرار موحدة
// ============================================================
private data class HomeGridItem(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000L)
            nowMs = System.currentTimeMillis()
        }
    }

    val cal = AppClock.calendar(st, nowMs)
    val hijriText = HijriCalendar.format(cal, st.hijriOffset)
    val gregText = ArabicDate.gregorian(cal, st.monthStyle)
    val weekday = ArabicDate.weekday(cal)
    val next = PrayerCalc.next(st, nowMs)
    val lastSurah = remember(st.lastPage) { QuranData.surahForPage(st.lastPage) }

    // 12 زراً منظماً (4 أزرار في كل صف × 3 صفوف)
    val gridButtons = remember(st.lastPage) {
        listOf(
            // الصف الأول (4 أزرار)
            HomeGridItem("المصحف", Icons.AutoMirrored.Rounded.MenuBook) { nav.openPage(st.lastPage) },
            HomeGridItem("السور", Icons.Rounded.FormatListNumberedRtl) { nav.openIndex(0, false) },
            HomeGridItem("الأجزاء", Icons.Rounded.GridView) { nav.openIndex(1, false) },
            HomeGridItem("الصوتيات", Icons.Rounded.Headphones) { nav.push(Screen.Audio) },

            // الصف الثاني (4 أزرار)
            HomeGridItem("مواقيت الصلاة", Icons.Rounded.Mosque) { nav.openPrayer(false) },
            HomeGridItem("الأذكار", Icons.Rounded.AutoAwesome) { nav.openAdhkar(AdhkarTabs.MORNING) },
            HomeGridItem("المسبحة", Icons.Rounded.Spa) { nav.push(Screen.Tasbih) },
            HomeGridItem("مركز الحفظ", Icons.Rounded.Psychology) { nav.push(Screen.Hifz) },

            // الصف الثالث (4 أزرار)
            HomeGridItem("القبلة", Icons.Rounded.Explore) { nav.openPrayer(true) },
            HomeGridItem("العلامات", Icons.Rounded.Bookmarks) { nav.push(Screen.Bookmarks) },
            HomeGridItem("البحث", Icons.Rounded.Search) { nav.openIndex(0, true) },
            HomeGridItem("الإعدادات", Icons.Rounded.Settings) { nav.openSettings(-1) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg)
    ) {
        // نقش إسلامي خفيف يزين الخلفية
        IslamicPattern(
            modifier = Modifier.fillMaxSize(),
            tint = if (p.isDark) GoldShine.copy(alpha = 0.05f) else EmeraldVibrant.copy(alpha = 0.05f)
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. الترويسة الملكية: صورة المصحف الكريم وبجانبها "القرآن الكريم"
            item {
                Surface(
                    color = RoyalForest,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                    border = BorderStroke(1.4.dp, GoldWarm.copy(alpha = 0.7f)),
                    shadowElevation = 10.dp
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(RoyalForest, EmeraldDeep, EmeraldVibrant)))
                            .statusBarsPadding()
                    ) {
                        IslamicPattern(Modifier.matchParentSize(), tint = GoldShine.copy(alpha = 0.10f))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            // صف العنوان الرئيسي: شعار المصحف + عنوان "القرآن الكريم"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                HolyQuranEmblem(modifier = Modifier.size(66.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "الْقُرْآنُ الْكَرِيمُ",
                                        fontFamily = UthmanicFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 34.sp,
                                        color = GoldShine,
                                        lineHeight = 44.sp
                                    )
                                    Text(
                                        text = "نُورُ الْهُدَى وَضِيَاءُ الْقُلُوبِ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = GoldSoftBg.copy(alpha = 0.9f)
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // شريط مختصر للتاريخ والصلاة القادمة ومتابعة آخر صفحة
                            Surface(
                                onClick = { fx.click(); nav.openPage(st.lastPage) },
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Black.copy(alpha = 0.25f),
                                border = BorderStroke(1.dp, GoldShine.copy(alpha = 0.45f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$weekday • $hijriText",
                                            color = GoldShine,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$gregText • ${next.name} بعد ${PrayerCalc.remainingWords(next.millis - nowMs)}",
                                            color = GoldSoftBg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(shape = RoundedCornerShape(10.dp), color = GoldShine) {
                                        Text(
                                            text = "ص ${st.lastPage} (${lastSurah.name})",
                                            color = RoyalForest,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. شبكة الـ 12 زراً (3 صفوف × 4 أزرار جنب بعض)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridButtons.chunked(4).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { item ->
                                HomeGridTile(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 3. بطاقة آية اليوم الاحترافية الثابتة بأزرار موحدة
            item {
                DailyVerseCard(nav = nav)
            }
        }
    }
}

@Composable
private fun HomeGridTile(item: HomeGridItem, modifier: Modifier = Modifier) {
    val fx = LocalFx.current
    Surface(
        onClick = { fx.click(); item.onClick() },
        shape = RoundedCornerShape(18.dp),
        color = EmeraldDeep,
        border = BorderStroke(1.2.dp, GoldWarm.copy(alpha = 0.72f)),
        shadowElevation = 4.dp,
        modifier = modifier.height(84.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(EmeraldVibrant, RoyalForest))),
            contentAlignment = Alignment.Center
        ) {
            IslamicPattern(Modifier.matchParentSize(), tint = GoldShine.copy(alpha = 0.06f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GoldShine.copy(alpha = 0.16f))
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = GoldShine,
                        modifier = Modifier.size(21.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = item.label,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ============================================================
// 2. فهرس السور والأجزاء والبحث (IndexScreen)
// ============================================================
@Composable
fun IndexScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableIntStateOf(0) }
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(nav.focusIndexSearch) {
        if (nav.focusIndexSearch) {
            nav.focusIndexSearch = false
            delay(200)
            runCatching { searchFocus.requestFocus() }
        }
    }

    val filteredSurahs = remember(query, typeFilter) {
        QuranData.search(query).filter { s ->
            when (typeFilter) {
                1 -> s.type == "مكية"
                2 -> s.type == "مدنية"
                else -> true
            }
        }
    }

    val currentSurahNum = remember(st.lastPage) { QuranData.surahForPage(st.lastPage).num }
    val currentJuzNum = remember(st.lastPage) { QuranData.juzForPage(st.lastPage).num }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        ScreenHeader(
            title = "المصحف الشريف (PDF)",
            subtitle = "يعمل بالكامل بدون إنترنت • 604 صفحات",
            onBack = { nav.back() },
            trailing = {
                IconButton(onClick = { fx.click(); nav.push(Screen.Bookmarks) }) {
                    Icon(Icons.Rounded.Bookmarks, "المفضلة", tint = GoldShine)
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("السور (114)", "الأجزاء (30)").forEachIndexed { idx, label ->
                val sel = nav.indexTab == idx
                FilterChip(
                    selected = sel,
                    onClick = { fx.click(); nav.indexTab = idx },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) },
                    shape = RoundedCornerShape(14.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GoldShine,
                        selectedLabelColor = RoyalForest
                    )
                )
            }
        }

        if (nav.indexTab == 0) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = EmeraldVibrant) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "مسح") }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .focusRequester(searchFocus)
            )

            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("الكل", "مكية", "مدنية").forEachIndexed { i, t ->
                    FilterChip(
                        selected = typeFilter == i,
                        onClick = { fx.click(); typeFilter = i },
                        label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldShine,
                            selectedLabelColor = RoyalForest
                        )
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filteredSurahs.isEmpty()) {
                    item { EmptyState(Icons.Rounded.SearchOff, "لا توجد سورة مطابقة لبحثك") }
                }
                items(filteredSurahs, key = { it.num }) { s ->
                    val isSelected = s.num == currentSurahNum
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        selected = isSelected,
                        onClick = { nav.openPage(s.page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(s.num, 38, highlighted = isSelected)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "سُورَةُ ${s.name}",
                                    fontFamily = UthmanicFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = if (isSelected) p.selectedInk else p.ink
                                )
                                Text(
                                    text = "${s.type} • ${s.ayahs} آية",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) p.selectedInk.copy(alpha = 0.85f) else p.muted
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) GoldWarm else EmeraldVibrant.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "ص ${s.page}",
                                    color = if (isSelected) RoyalForest else EmeraldVibrant,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(QuranData.ajza, key = { it.num }) { j ->
                    val isSelected = j.num == currentJuzNum
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        selected = isSelected,
                        onClick = { nav.openPage(j.page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(j.num, 38, highlighted = isSelected)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    j.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = if (isSelected) p.selectedInk else p.ink
                                )
                                Text(
                                    "يبدأ بسورة ${j.startSurah}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) p.selectedInk.copy(alpha = 0.85f) else p.muted
                                )
                            }
                            Text(
                                "ص ${j.page}",
                                color = if (isSelected) p.selectedInk else GoldWarm,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 3. قارئ المصحف الشريف من ملف PDF محلي بالكامل (بدون أي مواقع خارجية)
// ضع ملف المصحف باسم mushaf.pdf داخل مجلد app/src/main/assets/
// ============================================================
object LocalMushafPdfEngine {
    const val ASSET_FILE_NAME = "mushaf.pdf"
    private val mutex = Mutex()
    private var renderer: PdfRenderer? = null
    private var pfd: ParcelFileDescriptor? = null
    private var totalPagesCount: Int = 0
    private var missingAsset: Boolean = false

    private fun ensureOpen(ctx: Context): PdfRenderer? {
        renderer?.let { return it }
        if (missingAsset) return null
        return runCatching {
            val outFile = File(ctx.filesDir, ASSET_FILE_NAME)
            if (!outFile.exists() || outFile.length() < 1024) {
                ctx.assets.open(ASSET_FILE_NAME).use { input ->
                    FileOutputStream(outFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            val descriptor = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdf = PdfRenderer(descriptor)
            pfd = descriptor
            renderer = pdf
            totalPagesCount = pdf.pageCount
            pdf
        }.getOrElse {
            missingAsset = true
            null
        }
    }

    fun pageCount(ctx: Context): Int {
        ensureOpen(ctx)
        return if (totalPagesCount > 0) totalPagesCount else 604
    }

    suspend fun renderPage(ctx: Context, pageNumber1Based: Int, nightMode: Boolean): Bitmap? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val pdf = ensureOpen(ctx) ?: return@withLock null
                val index = (pageNumber1Based - 1).coerceIn(0, (pdf.pageCount - 1).coerceAtLeast(0))
                runCatching {
                    pdf.openPage(index).use { page ->
                        val scale = 2.2f
                        val width = (page.width * scale).toInt().coerceAtLeast(1)
                        val height = (page.height * scale).toInt().coerceAtLeast(1)
                        val rawBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(rawBitmap)
                        canvas.drawColor(android.graphics.Color.parseColor("#FFFCF2"))
                        page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        if (nightMode) {
                            val darkBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            val darkCanvas = Canvas(darkBitmap)
                            val paint = Paint().apply {
                                colorFilter = ColorMatrixColorFilter(
                                    ColorMatrix(
                                        floatArrayOf(
                                            -0.92f, 0f, 0f, 0f, 240f,
                                            0f, -0.90f, 0f, 0f, 238f,
                                            0f, 0f, -0.92f, 0f, 228f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                    )
                                )
                            }
                            darkCanvas.drawBitmap(rawBitmap, 0f, 0f, paint)
                            rawBitmap.recycle()
                            darkBitmap
                        } else {
                            rawBitmap
                        }
                    }
                }.getOrNull()
            }
        }
}

@Composable
fun ReaderScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val pdfTotalPages = remember { LocalMushafPdfEngine.pageCount(ctx) }
    val pagerState = rememberPagerState(
        initialPage = (nav.readerPage - 1).coerceIn(0, (pdfTotalPages - 1).coerceAtLeast(0)),
        pageCount = { pdfTotalPages }
    )

    val currentPage = pagerState.currentPage + 1
    val currentSurah = remember(currentPage) { QuranData.surahForPage(currentPage) }
    val currentJuz = remember(currentPage) { QuranData.juzForPage(currentPage) }
    val isBookmarked = currentPage in st.bookmarks

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(currentPage) {
        nav.readerPage = currentPage
        st.markPageRead(currentPage)
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(p.paper)
            .navigationBarsPadding()
    ) {
        Surface(color = RoyalForest, shadowElevation = 4.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { fx.click(); nav.back() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, "رجوع", tint = GoldShine)
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "سورة ${currentSurah.name}",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${currentJuz.name} • صفحة $currentPage من $pdfTotalPages",
                        color = GoldSoftBg,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = {
                    fx.click()
                    zoomScale = if (zoomScale > 1.05f) 1f else 1.75f
                    panOffset = Offset.Zero
                }) {
                    Icon(
                        if (zoomScale > 1.05f) Icons.Rounded.ZoomOut else Icons.Rounded.ZoomIn,
                        "تكبير الصفحة",
                        tint = GoldSoftBg
                    )
                }

                IconButton(onClick = { fx.click(); st.nightMode = !st.nightMode }) {
                    Icon(
                        if (st.nightMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        "الوضع الليلي",
                        tint = GoldSoftBg
                    )
                }

                IconButton(onClick = { fx.click(); st.toggleBookmark(currentPage) }) {
                    Icon(
                        if (isBookmarked) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        "علامة مرجعية",
                        tint = if (isBookmarked) GoldShine else Color.White
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = zoomScale <= 1.05f,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            var pageBitmap by remember(pageNum, st.nightMode) { mutableStateOf<Bitmap?>(null) }
            var loading by remember(pageNum, st.nightMode) { mutableStateOf(true) }

            LaunchedEffect(pageNum, st.nightMode) {
                loading = true
                pageBitmap = LocalMushafPdfEngine.renderPage(ctx, pageNum, st.nightMode)
                loading = false
            }

            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                val newScale = (zoomScale * zoomChange).coerceIn(1f, 3.2f)
                zoomScale = newScale
                panOffset = if (newScale > 1.02f) panOffset + panChange else Offset.Zero
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (st.nightMode) Color(0xFF0A1D16) else Color(0xFFFFFCF2))
                    .border(1.5.dp, GoldWarm.copy(alpha = 0.65f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    loading -> CircularProgressIndicator(color = EmeraldVibrant)
                    pageBitmap != null -> {
                        Image(
                            bitmap = pageBitmap!!.asImageBitmap(),
                            contentDescription = "صفحة المصحف $pageNum",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                                .transformable(state = transformState)
                        )
                    }
                    else -> {
                        // رسالة إرشادية واضحة في حال لم يضع المطور ملف mushaf.pdf بعد في مجلد assets
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HolyQuranEmblem(Modifier.size(72.dp))
                            Text(
                                text = "المصحف الشريف (PDF محلي)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = p.ink
                            )
                            Text(
                                text = "للعرض الكامل بدون إنترنت، ضع ملف المصحف بصيغة PDF داخل مجلد:\napp/src/main/assets/mushaf.pdf",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = p.muted,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldVibrant.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, GoldWarm)
                            ) {
                                Text(
                                    text = "سورة ${currentSurah.name} • صفحة $pageNum • ${currentJuz.name}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldVibrant,
                                    fontSize = 12.5.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(color = RoyalForest, shadowElevation = 6.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        fx.click()
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    },
                    enabled = currentPage > 1
                ) {
                    Text("الصفحة السابقة", color = GoldSoftBg, fontWeight = FontWeight.Bold)
                }

                FilledTonalButton(
                    onClick = {
                        fx.click()
                        PlayerService.play(ctx, st.reciterId, currentSurah.num)
                        nav.push(Screen.Audio)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = GoldShine)
                ) {
                    Icon(Icons.Rounded.VolumeUp, null, tint = RoyalForest, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("استمع للسورة", color = RoyalForest, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }

                TextButton(
                    onClick = {
                        fx.click()
                        if (pagerState.currentPage < pdfTotalPages - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    enabled = currentPage < pdfTotalPages
                ) {
                    Text("الصفحة التالية", color = GoldSoftBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ============================================================
// 4. شاشة العلامات المرجعية والمفضلة (BookmarksScreen)
// ============================================================
@Composable
fun BookmarksScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current

    Column(Modifier.fillMaxSize().background(p.bg)) {
        ScreenHeader("العلامات المرجعية", onBack = { nav.back() })

        if (st.bookmarks.isEmpty()) {
            EmptyState(Icons.Rounded.BookmarkBorder, "لا توجد صفحات محفوظة في المفضلة بعد")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(st.bookmarks, key = { it }) { page ->
                    val s = QuranData.surahForPage(page)
                    val j = QuranData.juzForPage(page)
                    QCard(
                        shape = RoundedCornerShape(18.dp),
                        onClick = { nav.openPage(page) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NumberBadge(page, 38)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("صفحة $page • سورة ${s.name}", fontWeight = FontWeight.ExtraBold, color = p.ink)
                                Text(j.name, fontSize = 12.sp, color = p.muted, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { fx.click(); st.toggleBookmark(page) }) {
                                Icon(Icons.Rounded.BookmarkRemove, "إزالة", tint = DangerRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
