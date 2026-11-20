package com.quran.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class QuranIndexEntry(
    val title: String,
    val subtitle: String,
    val page: Int,
    val category: String
)

@Composable
fun QuranIndexAndReaderScreen(
    initialPage: Int,
    startDirectlyInReader: Boolean = false,
    favorites: Set<Int>,
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleFavorite: (Int) -> Unit,
    onToggleDarkTheme: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onOpenSettings: () -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }

    var readerMode by remember(startDirectlyInReader) { mutableStateOf(startDirectlyInReader) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: السور، 1: الأجزاء، 2: المفضلة
    var currentPage by remember(initialPage) { mutableIntStateOf(initialPage.coerceIn(1, 604)) }
    var hideMargins by remember { mutableStateOf(false) }

    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val cardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val lightCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val primaryInk = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val surahEntries = remember {
        listOf(
            QuranIndexEntry("الفاتحة", "مكية • ٧ آيات", 1, "سور"),
            QuranIndexEntry("البقرة", "مدنية • ٢٨٦ آية", 2, "سور"),
            QuranIndexEntry("آل عمران", "مدنية • ٢٠٠ آية", 50, "سور"),
            QuranIndexEntry("النساء", "مدنية • ١٧٦ آية", 77, "سور"),
            QuranIndexEntry("المائدة", "مدنية • ١٢٠ آية", 106, "سور"),
            QuranIndexEntry("الأنعام", "مكية • ١٦٥ آية", 128, "سور"),
            QuranIndexEntry("الأعراف", "مكية • ٢٠٦ آيات", 151, "سور"),
            QuranIndexEntry("الأنفال", "مدنية • ٧٥ آية", 177, "سور"),
            QuranIndexEntry("التوبة", "مدنية • ١٢٩ آية", 187, "سور"),
            QuranIndexEntry("يونس", "مكية • ١٠٩ آيات", 208, "سور"),
            QuranIndexEntry("هود", "مكية • ١٢٣ آية", 221, "سور"),
            QuranIndexEntry("يوسف", "مكية • ١١١ آية", 235, "سور"),
            QuranIndexEntry("الرعد", "مدنية • ٤٣ آية", 249, "سور"),
            QuranIndexEntry("إبراهيم", "مكية • ٥٢ آية", 255, "سور"),
            QuranIndexEntry("الحجر", "مكية • ٩٩ آية", 262, "سور"),
            QuranIndexEntry("النحل", "مكية • ١٢٨ آية", 267, "سور"),
            QuranIndexEntry("الإسراء", "مكية • ١١١ آية", 282, "سور"),
            QuranIndexEntry("الكهف", "مكية • ١١٠ آيات", 293, "سور"),
            QuranIndexEntry("مريم", "مكية • ٩٨ آية", 305, "سور"),
            QuranIndexEntry("طه", "مكية • ١٣٥ آية", 312, "سور"),
            QuranIndexEntry("الأنبياء", "مكية • ١١٢ آية", 322, "سور"),
            QuranIndexEntry("الحج", "مدنية • ٧٨ آية", 332, "سور"),
            QuranIndexEntry("المؤمنون", "مكية • ١١٨ آية", 342, "سور"),
            QuranIndexEntry("النور", "مدنية • ٦٤ آية", 350, "سور"),
            QuranIndexEntry("الفرقان", "مكية • ٧٧ آية", 359, "سور"),
            QuranIndexEntry("الشعراء", "مكية • ٢٢٧ آية", 367, "سور"),
            QuranIndexEntry("النمل", "مكية • ٩٣ آية", 377, "سور"),
            QuranIndexEntry("القصص", "مكية • ٨٨ آية", 385, "سور"),
            QuranIndexEntry("العنكبوت", "مكية • ٦٩ آية", 396, "سور"),
            QuranIndexEntry("الروم", "مكية • ٦٠ آية", 404, "سور"),
            QuranIndexEntry("لقمان", "مكية • ٣٤ آية", 411, "سور"),
            QuranIndexEntry("السجدة", "مكية • ٣٠ آية", 415, "سور"),
            QuranIndexEntry("الأحزاب", "مدنية • ٧٣ آية", 418, "سور"),
            QuranIndexEntry("سبأ", "مكية • ٥٤ آية", 428, "سور"),
            QuranIndexEntry("فاطر", "مكية • ٤٥ آية", 434, "سور"),
            QuranIndexEntry("يس", "مكية • ٨٣ آية", 440, "سور"),
            QuranIndexEntry("الصافات", "مكية • ١٨٢ آية", 446, "سور"),
            QuranIndexEntry("ص", "مكية • ٨٨ آية", 453, "سور"),
            QuranIndexEntry("الزمر", "مكية • ٧٥ آية", 458, "سور"),
            QuranIndexEntry("غافر", "مكية • ٨٥ آية", 467, "سور"),
            QuranIndexEntry("فصلت", "مكية • ٥٤ آية", 477, "سور"),
            QuranIndexEntry("الشورى", "مكية • ٥٣ آية", 483, "سور"),
            QuranIndexEntry("الزخرف", "مكية • ٨٩ آية", 489, "سور"),
            QuranIndexEntry("الدخان", "مكية • ٥٩ آية", 496, "سور"),
            QuranIndexEntry("الجاثية", "مكية • ٣٧ آية", 499, "سور"),
            QuranIndexEntry("الأحقاف", "مكية • ٣٥ آية", 502, "سور"),
            QuranIndexEntry("محمد", "مدنية • ٣٨ آية", 507, "سور"),
            QuranIndexEntry("الفتح", "مدنية • ٢٩ آية", 511, "سور"),
            QuranIndexEntry("الحجرات", "مدنية • ١٨ آية", 515, "سور"),
            QuranIndexEntry("ق", "مكية • ٤٥ آية", 518, "سور"),
            QuranIndexEntry("الذاريات", "مكية • ٦٠ آية", 520, "سور"),
            QuranIndexEntry("الطور", "مكية • ٤٩ آية", 523, "سور"),
            QuranIndexEntry("النجم", "مكية • ٦٢ آية", 526, "سور"),
            QuranIndexEntry("القمر", "مكية • ٥٥ آية", 528, "سور"),
            QuranIndexEntry("الرحمن", "مدنية • ٧٨ آية", 531, "سور"),
            QuranIndexEntry("الواقعة", "مكية • ٩٦ آية", 534, "سور"),
            QuranIndexEntry("الحديد", "مدنية • ٢٩ آية", 537, "سور"),
            QuranIndexEntry("المجادلة", "مدنية • ٢٢ آية", 542, "سور"),
            QuranIndexEntry("الحشر", "مدنية • ٢٤ آية", 545, "سور"),
            QuranIndexEntry("الممتحنة", "مدنية • ١٣ آية", 549, "سور"),
            QuranIndexEntry("الصف", "مدنية • ١٤ آية", 551, "سور"),
            QuranIndexEntry("الجمعة", "مدنية • ١١ آية", 553, "سور"),
            QuranIndexEntry("المنافقون", "مدنية • ١١ آية", 554, "سور"),
            QuranIndexEntry("التغابن", "مدنية • ١٨ آية", 556, "سور"),
            QuranIndexEntry("الطلاق", "مدنية • ١٢ آية", 558, "سور"),
            QuranIndexEntry("التحريم", "مدنية • ١٢ آية", 560, "سور"),
            QuranIndexEntry("الملك", "مكية • ٣٠ آية", 562, "سور"),
            QuranIndexEntry("القلم", "مكية • ٥٢ آية", 564, "سور"),
            QuranIndexEntry("الحاقة", "مكية • ٥٢ آية", 566, "سور"),
            QuranIndexEntry("المعارج", "مكية • ٤٤ آية", 568, "سور"),
            QuranIndexEntry("نوح", "مكية • ٢٨ آية", 570, "سور"),
            QuranIndexEntry("الجن", "مكية • ٢٨ آية", 572, "سور"),
            QuranIndexEntry("المزمل", "مكية • ٢٠ آية", 574, "سور"),
            QuranIndexEntry("المدثر", "مكية • ٥٦ آية", 575, "سور"),
            QuranIndexEntry("القيامة", "مكية • ٤٠ آية", 577, "سور"),
            QuranIndexEntry("الإنسان", "مدنية • ٣١ آية", 578, "سور"),
            QuranIndexEntry("المرسلات", "مكية • ٥٠ آية", 580, "سور"),
            QuranIndexEntry("النبأ", "مكية • ٤٠ آية", 582, "سور"),
            QuranIndexEntry("النازعات", "مكية • ٤٦ آية", 583, "سور"),
            QuranIndexEntry("عبس", "مكية • ٤٢ آية", 585, "سور"),
            QuranIndexEntry("التكوير", "مكية • ٢٩ آية", 586, "سور"),
            QuranIndexEntry("الإنفطار", "مكية • ١٩ آية", 587, "سور"),
            QuranIndexEntry("المطففين", "مكية • ٣٦ آية", 587, "سور"),
            QuranIndexEntry("الإنشقاق", "مكية • ٢٥ آية", 589, "سور"),
            QuranIndexEntry("البروج", "مكية • ٢٢ آية", 590, "سور"),
            QuranIndexEntry("الطارق", "مكية • ١٧ آية", 591, "سور"),
            QuranIndexEntry("الأعلى", "مكية • ١٩ آية", 591, "سور"),
            QuranIndexEntry("الغاشية", "مكية • ٢٦ آية", 592, "سور"),
            QuranIndexEntry("الفجر", "مكية • ٣٠ آية", 593, "سور"),
            QuranIndexEntry("البلد", "مكية • ٢٠ آية", 594, "سور"),
            QuranIndexEntry("الشمس", "مكية • ١٥ آية", 595, "سور"),
            QuranIndexEntry("الليل", "مكية • ٢١ آية", 595, "سور"),
            QuranIndexEntry("الضحى", "مكية • ١١ آية", 596, "سور"),
            QuranIndexEntry("الشرح", "مكية • ٨ آيات", 596, "سور"),
            QuranIndexEntry("التين", "مكية • ٨ آيات", 597, "سور"),
            QuranIndexEntry("العلق", "مكية • ١٩ آية", 597, "سور"),
            QuranIndexEntry("القدر", "مكية • ٥ آيات", 598, "سور"),
            QuranIndexEntry("البينة", "مدنية • ٨ آيات", 598, "سور"),
            QuranIndexEntry("الزلزلة", "مدنية • ٨ آيات", 599, "سور"),
            QuranIndexEntry("العاديات", "مكية • ١١ آية", 599, "سور"),
            QuranIndexEntry("القارعة", "مكية • ١١ آية", 600, "سور"),
            QuranIndexEntry("التكاثر", "مكية • ٨ آيات", 600, "سور"),
            QuranIndexEntry("العصر", "مكية • ٣ آيات", 601, "سور"),
            QuranIndexEntry("الهمزة", "مكية • ٩ آيات", 601, "سور"),
            QuranIndexEntry("الفيل", "مكية • ٥ آيات", 601, "سور"),
            QuranIndexEntry("قريش", "مكية • ٤ آيات", 602, "سور"),
            QuranIndexEntry("الماعون", "مكية • ٧ آيات", 602, "سور"),
            QuranIndexEntry("الكوثر", "مكية • ٣ آيات", 602, "سور"),
            QuranIndexEntry("الكافرون", "مكية • ٦ آيات", 603, "سور"),
            QuranIndexEntry("النصر", "مدنية • ٣ آيات", 603, "سور"),
            QuranIndexEntry("المسد", "مكية • ٥ آيات", 603, "سور"),
            QuranIndexEntry("الإخلاص", "مكية • ٤ آيات", 604, "سور"),
            QuranIndexEntry("الفلق", "مكية • ٥ آيات", 604, "سور"),
            QuranIndexEntry("الناس", "مكية • ٦ آيات", 604, "سور")
        )
    }

    val juzEntries = remember {
        (1..30).map { j ->
            val startPage = ((j - 1) * 20 + 1).coerceIn(1, 604)
            QuranIndexEntry("الجزء $j", "يبدأ من صفحة $startPage", startPage, "أجزاء")
        }
    }

    if (readerMode) {
        val pagerState = rememberPagerState(
            initialPage = (currentPage - 1).coerceIn(0, 603),
            pageCount = { 604 }
        )
        val scope = rememberCoroutineScope()

        LaunchedEffect(pagerState.currentPage) {
            val pageNum = pagerState.currentPage + 1
            currentPage = pageNum
            onPageChanged(pageNum)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(horizontal = if (hideMargins) 4.dp else 12.dp, vertical = 8.dp)
        ) {
            if (!hideMargins) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    border = BorderStroke(1.4.dp, goldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { readerMode = false },
                            border = BorderStroke(1.dp, RoyalPalette.GoldHighlight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("الفهرس", color = RoyalPalette.GoldHighlight, fontSize = 12.sp, fontFamily = indexFont)
                        }

                        Text(
                            text = "صفحة $currentPage / 604",
                            color = RoyalPalette.CreamText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = titleFont
                        )

                        val isFav = favorites.contains(currentPage)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isFav) RoyalPalette.GoldRoyal else RoyalPalette.GoldRoyal.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { onToggleFavorite(currentPage) }
                        ) {
                            Text(
                                text = if (isFav) "★ محفوظة" else "☆ حفظ",
                                color = if (isFav) RoyalPalette.EmeraldDeep else RoyalPalette.GoldHighlight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = indexFont,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIdx ->
                val pageNumber = pageIdx + 1
                var zoom by remember(pageNumber) { mutableFloatStateOf(1f) }
                var offset by remember(pageNumber) { mutableStateOf(Offset.Zero) }
                var pageBitmap by remember(pageNumber) { mutableStateOf<Bitmap?>(null) }

                LaunchedEffect(pageNumber) {
                    pageBitmap = withContext(Dispatchers.IO) {
                        renderMushafPageBitmap(context, pageNumber)
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { hideMargins = !hideMargins },
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color(0xFFFFFDF7),
                    border = BorderStroke(1.5.dp, goldBorder)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(pageNumber) {
                                detectTransformGestures { _, pan, gestureZoom, _ ->
                                    val newZoom = (zoom * gestureZoom).coerceIn(1f, 3.2f)
                                    zoom = newZoom
                                    offset = if (newZoom <= 1f) Offset.Zero else offset + pan
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (pageBitmap != null) {
                            Image(
                                bitmap = pageBitmap!!.asImageBitmap(),
                                contentDescription = "صفحة $pageNumber",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoom,
                                        scaleY = zoom,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "المصحف الشريف • صفحة $pageNumber",
                                    color = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = titleFont
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "ضع ملف mushaf.pdf داخل مجلد assets لعرض صفحات المصحف المصورة تلقائياً، ويمكنك السحب بين الصفحات أو التكبير بإصبعين.",
                                    color = primaryInk,
                                    fontSize = 14.sp,
                                    fontFamily = indexFont,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                                if (zoom > 1f) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedButton(onClick = {
                                        zoom = 1f
                                        offset = Offset.Zero
                                    }) {
                                        Text("إعادة ضبط التكبير", fontFamily = indexFont)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!hideMargins) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                            scope.launch { pagerState.animateScrollToPage(prev) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = RoyalPalette.CreamText)
                    ) {
                        Text("السابقة", fontFamily = indexFont)
                    }

                    Text(
                        text = "اسحب يميناً أو يساراً للتنقل",
                        color = primaryInk.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        fontFamily = indexFont
                    )

                    Button(
                        onClick = {
                            val next = (pagerState.currentPage + 1).coerceAtMost(603)
                            scope.launch { pagerState.animateScrollToPage(next) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = RoyalPalette.CreamText)
                    ) {
                        Text("التالية", fontFamily = indexFont)
                    }
                }
            }
        }
        return
    }

    val filteredList = remember(selectedTab, searchQuery, surahEntries, juzEntries, favorites) {
        val q = searchQuery.trim()
        val base = when (selectedTab) {
            0 -> surahEntries
            1 -> juzEntries
            else -> favorites.sorted().map { p ->
                val matchedSurah = surahEntries.lastOrNull { it.page <= p }
                QuranIndexEntry(
                    title = matchedSurah?.let { "قرب سورة ${it.title}" } ?: "صفحة محفوظة",
                    subtitle = "صفحة رقم $p",
                    page = p,
                    category = "مفضلة"
                )
            }
        }
        if (q.isEmpty()) base else base.filter { it.title.contains(q) || it.page.toString() == q }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.5.dp, goldBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "فهرس القرآن الكريم",
                        color = RoyalPalette.CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Text(
                        text = "آخر قراءة: صفحة $lastReadPage • المفضلة: ${favorites.size}",
                        color = RoyalPalette.GoldHighlight,
                        fontSize = 12.sp,
                        fontFamily = indexFont
                    )
                }

                Button(
                    onClick = {
                        currentPage = lastReadPage
                        readerMode = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = RoyalPalette.GoldRoyal,
                        contentColor = RoyalPalette.EmeraldDeep
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("فتح المصحف", fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = indexFont)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("السور (١١٤)", "الأجزاء (٣٠)", "المفضلة (${favorites.size})").forEachIndexed { idx, label ->
                val selected = selectedTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedTab = idx },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) cardBg else lightCardBg,
                    border = BorderStroke(1.3.dp, goldBorder)
                ) {
                    Text(
                        text = label,
                        color = if (selected) RoyalPalette.CreamText else primaryInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = indexFont,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(filteredList, key = { "${it.category}_${it.title}_${it.page}" }) { item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            currentPage = item.page
                            onPageChanged(item.page)
                            readerMode = true
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = lightCardBg,
                    border = BorderStroke(1.2.dp, goldBorder.copy(alpha = 0.85f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                modifier = Modifier.size(36.dp),
                                shape = CircleShape,
                                color = cardBg,
                                border = BorderStroke(1.dp, goldBorder)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = item.page.toString(),
                                        color = RoyalPalette.GoldHighlight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = item.title,
                                    color = primaryInk,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = indexFont
                                )
                                Text(
                                    text = item.subtitle,
                                    color = primaryInk.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontFamily = indexFont
                                )
                            }
                        }

                        RoyalBadgePill(label = "ص ${item.page}")
                    }
                }
            }
        }
    }
}

private fun renderMushafPageBitmap(context: Context, pageNumber: Int): Bitmap? {
    return try {
        val outFile = File(context.cacheDir, "mushaf_cached.pdf")
        if (!outFile.exists() || outFile.length() == 0L) {
            context.assets.open("mushaf.pdf").use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
        }
        val pfd = ParcelFileDescriptor.open(outFile, ParcelFileDescriptor.MODE_READ_ONLY) ?: return null
        val renderer = PdfRenderer(pfd)
        val idx = (pageNumber - 1).coerceIn(0, renderer.pageCount - 1)
        val page = renderer.openPage(idx)
        val scale = 2
        val bmp = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.parseColor("#FFFDF7"))
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()
        bmp
    } catch (_: Exception) {
        null
    }
}
