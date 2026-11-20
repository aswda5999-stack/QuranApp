package com.quran.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var showReader by remember(startDirectlyInReader) { mutableStateOf(startDirectlyInReader) }
    var currentPage by remember(initialPage) { mutableIntStateOf(initialPage.coerceIn(1, 604)) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchExpanded by remember { mutableStateOf(false) }

    val tabs = listOf("الأجزاء", "الآيات", "الفهرس العام")

    val sampleEntries = remember {
        listOf(
            QuranIndexEntry("الجزء الأول", "سورة الفاتحة • البقرة", 1, "الأجزاء"),
            QuranIndexEntry("الجزء الثاني", "سيقول السفهاء • البقرة", 22, "الأجزاء"),
            QuranIndexEntry("الجزء الثالث", "تلك الرسل • البقرة", 42, "الأجزاء"),
            QuranIndexEntry("الجزء الثلاثون", "عمّ يتساءلون • الناس", 582, "الأجزاء"),
            QuranIndexEntry("آية الكرسي", "البقرة • الآية 255", 42, "الآيات"),
            QuranIndexEntry("آية الدين", "البقرة • الآية 282", 48, "الآيات"),
            QuranIndexEntry("آية النور", "النور • الآية 35", 354, "الآيات"),
            QuranIndexEntry("سورة الكهف", "بداية السورة", 293, "الفهرس العام"),
            QuranIndexEntry("سورة يس", "بداية السورة", 440, "الفهرس العام"),
            QuranIndexEntry("سورة الملك", "بداية السورة", 562, "الفهرس العام")
        )
    }

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val gold = RoyalPalette.GoldRoyal
    val cream = RoyalPalette.CreamText

    if (!showReader) {
        val currentCategory = tabs[selectedTab]
        val visibleItems = sampleEntries.filter { entry ->
            val matchesCategory = if (selectedTab == 2) true else entry.category == currentCategory
            val matchesQuery = searchQuery.isBlank() ||
                entry.title.contains(searchQuery.trim(), ignoreCase = true) ||
                entry.subtitle.contains(searchQuery.trim(), ignoreCase = true)
            matchesCategory && matchesQuery
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
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                text = "فهرس القرآن الكريم",
                                color = cream,
                                fontFamily = indexFont,
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
                            placeholder = { Text("ابحث في الفهرس...", color = cream.copy(alpha = 0.6f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = cream,
                                unfocusedTextColor = cream,
                                focusedBorderColor = gold,
                                unfocusedBorderColor = gold.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = gold,
                        edgePadding = 4.dp
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = {
                                    Text(
                                        text = title,
                                        fontFamily = indexFont,
                                        fontSize = 15.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedTab == index) gold else cream
                                    )
                                }
                            )
                        }
                    }
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(visibleItems) { item ->
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, gold.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentPage = item.page.coerceIn(1, 604)
                                onPageChanged(currentPage)
                                showReader = true
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = item.title,
                                    color = cream,
                                    fontFamily = indexFont,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = item.subtitle,
                                    color = gold.copy(alpha = 0.85f),
                                    fontFamily = indexFont,
                                    fontSize = 13.sp
                                )
                            }
                            Text(
                                text = "صـ ${item.page}",
                                color = gold,
                                fontFamily = indexFont,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    } else {
        var chromeVisible by remember { mutableStateOf(false) }
        var scale by remember { mutableFloatStateOf(1f) }
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }
        var dragAccumulator by remember { mutableFloatStateOf(0f) }

        val renderedBitmap = remember(currentPage, context) {
            renderMushafPageBitmap(context, currentPage)
        }

        val nightMatrix = remember {
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkTheme) Color(0xFF0A0F0D) else Color(0xFFFBF7EC))
                .pointerInput(currentPage) {
                    detectTapGestures(onTap = { chromeVisible = !chromeVisible })
                }
                .pointerInput(currentPage) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextScale = (scale * zoom).coerceIn(1f, 3.5f)
                        scale = nextScale
                        if (nextScale > 1f) {
                            offsetX += pan.x
                            offsetY += pan.y
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
                .pointerInput(currentPage, scale) {
                    if (scale <= 1.05f) {
                        detectHorizontalDragGestures(
                            onDragStart = { dragAccumulator = 0f },
                            onHorizontalDrag = { _, dragAmount -> dragAccumulator += dragAmount },
                            onDragEnd = {
                                if (dragAccumulator < -90f && currentPage < 604) {
                                    currentPage += 1
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    onPageChanged(currentPage)
                                } else if (dragAccumulator > 90f && currentPage > 1) {
                                    currentPage -= 1
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                    onPageChanged(currentPage)
                                }
                            }
                        )
                    }
                }
        ) {
            if (renderedBitmap != null) {
                Image(
                    bitmap = renderedBitmap.asImageBitmap(),
                    contentDescription = "صفحة $currentPage",
                    colorFilter = if (isDarkTheme) ColorFilter.colorMatrix(nightMatrix) else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            } else {
                Text(
                    text = "صفحة $currentPage",
                    color = if (isDarkTheme) cream else RoyalPalette.EmeraldCard,
                    fontFamily = indexFont,
                    fontSize = 22.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            if (chromeVisible) {
                Surface(
                    color = cardColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                        .fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = { showReader = false }) {
                            Icon(Icons.Rounded.List, contentDescription = "الفهرس", tint = gold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onToggleFavorite(currentPage) }) {
                                val isFav = favorites.contains(currentPage)
                                Icon(
                                    imageVector = if (isFav) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                                    contentDescription = "المفضلة",
                                    tint = gold
                                )
                            }
                            IconButton(onClick = onToggleDarkTheme) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                    contentDescription = "الوضع الليلي",
                                    tint = gold
                                )
                            }
                            IconButton(onClick = onOpenSettings) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = "الضبط",
                                    tint = gold
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = cardColor.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "الصفحة $currentPage / 604",
                            color = cream,
                            fontFamily = indexFont,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = {
                                val p = it.toInt().coerceIn(1, 604)
                                currentPage = p
                                onPageChanged(p)
                            },
                            valueRange = 1f..604f,
                            colors = SliderDefaults.colors(
                                thumbColor = gold,
                                activeTrackColor = gold
                            ),
                            modifier = Modifier.width(150.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun renderMushafPageBitmap(context: Context, pageNumber: Int): Bitmap? {
    return try {
        val cacheFile = File(context.cacheDir, "mushaf_cached_reader.bin")
        if (!cacheFile.exists() || cacheFile.length() == 0L) {
            context.assets.open("mushaf.pdf").use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        val pfd = ParcelFileDescriptor.open(cacheFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val safeIndex = (pageNumber - 1).coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
        val page = renderer.openPage(safeIndex)
        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()
        bitmap
    } catch (_: Exception) {
        null
    }
}
