@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.quran.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.*
import kotlin.reflect.KProperty
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


// ============================================================
private val InvertMatrix = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
private val WarmMatrix = ColorMatrix(floatArrayOf(1f, 0f, 0f, 0f, 0f, 0f, 0.96f, 0f, 0f, 0f, 0f, 0f, 0.84f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))

@Composable
fun ReaderScreen(nav: Nav) {
    val st = LocalApp.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = (st.lastPage - 1).coerceIn(0, 603), pageCount = { 604 })
    val curPage = pagerState.currentPage + 1
    var chrome by remember { mutableStateOf(true) }
    var showJump by remember { mutableStateOf(false) }
    var sliderDrag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(pagerState) {
        var first = true
        snapshotFlow { pagerState.currentPage }.collect { idx ->
            st.onPageViewed(idx + 1, counted = !first)
            first = false
        }
    }
    LaunchedEffect(curPage) {
        for (n in listOf(curPage + 1, curPage + 2, curPage - 1)) {
            if (n in 1..604) ctx.imageLoader.enqueue(ImageRequest.Builder(ctx).data(QuranData.pageUrl(n)).build())
        }
    }
    DisposableEffect(st.keepOn) {
        val w = (ctx as? Activity)?.window
        if (st.keepOn) w?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { w?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    val bg = when {
        st.nightMode -> Color.Black
        st.paperWarmth -> Color(0xFFF6EFE0)
        else -> Color.White
    }
    val surah = QuranData.surahForPage(curPage)

    Column(Modifier.fillMaxSize().background(bg)) {
        AnimatedVisibility(visible = chrome) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { fx.click(); nav.back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "رجوع", tint = Color.White) }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("سورة ${surah.name}", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text("صفحة $curPage • الجزء ${QuranData.juzOfPage(curPage)}", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    IconButton(onClick = { fx.click(); st.nightMode = !st.nightMode }) {
                        Icon(if (st.nightMode) Icons.Default.LightMode else Icons.Default.DarkMode, "الوضع الليلي", tint = GoldShine)
                    }
                }
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { idx ->
            Box(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { chrome = !chrome }) }) {
                MushafPage(idx + 1, st.nightMode, st.paperWarmth)
            }
        }

        AnimatedVisibility(visible = chrome) {
            Surface(color = RoyalForest, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Slider(
                        value = (sliderDrag ?: curPage.toFloat()).coerceIn(1f, 604f),
                        onValueChange = { sliderDrag = it },
                        onValueChangeFinished = {
                            val target = sliderDrag?.toInt() ?: curPage
                            sliderDrag = null
                            scope.launch { pagerState.scrollToPage(target.coerceIn(1, 604) - 1) }
                        },
                        valueRange = 1f..604f,
                        colors = SliderDefaults.colors(thumbColor = GoldShine, activeTrackColor = GoldShine, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                        ReaderAction(if (st.isFav(curPage)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, if (st.isFav(curPage)) "محفوظة" else "حفظ") {
                            fx.click(); st.toggleFav(curPage)
                        }
                        ReaderAction(Icons.Default.Search, "اذهب لصفحة") { fx.click(); showJump = true }
                        ReaderAction(Icons.Default.Headphones, "استمع للسورة") {
                            fx.click()
                            PlayerService.play(ctx, st.reciterId, surah.num)
                            nav.push(Screen.Audio)
                        }
                        ReaderAction(Icons.Default.Home, "الرئيسية") { fx.click(); nav.go(Screen.Home) }
                    }
                }
            }
        }
    }

    if (showJump) {
        var text by remember { mutableStateOf("") }
        val n = text.toIntOrNull()
        val valid = n != null && n in 1..604
        AlertDialog(
            onDismissRequest = { showJump = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("الانتقال إلى صفحة", fontWeight = FontWeight.ExtraBold) },
            text = {
                OutlinedTextField(
                    value = text, onValueChange = { text = it.filter { c -> c.isDigit() }.take(3) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("رقم الصفحة (1 - 604)") }, isError = text.isNotEmpty() && !valid
                )
            },
            confirmButton = {
                TextButton(enabled = valid, onClick = {
                    showJump = false
                    scope.launch { pagerState.scrollToPage((n ?: 1) - 1) }
                }) { Text("انتقال", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showJump = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun ReaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) { Icon(icon, label, tint = GoldShine, modifier = Modifier.size(22.dp)) }
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MushafPage(page: Int, night: Boolean, warm: Boolean) {
    val ctx = LocalContext.current
    var retry by remember { mutableIntStateOf(0) }
    val filter = remember(night, warm) {
        when {
            night -> ColorFilter.colorMatrix(InvertMatrix)
            warm -> ColorFilter.colorMatrix(WarmMatrix)
            else -> null
        }
    }
    key(retry) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(ctx).data(QuranData.pageUrl(page)).crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED).memoryCachePolicy(CachePolicy.ENABLED).build(),
            contentDescription = "صفحة $page",
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = Modifier.fillMaxSize().padding(4.dp),
            loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = GoldWarm) } },
            error = {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Refresh, null, tint = GoldWarm, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("تعذر تحميل الصفحة $page", color = if (night) Color.White else InkDark, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { retry++ }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldVibrant)) {
                        Text("إعادة المحاولة", color = GoldSoftBg, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        )
    }
}

@Composable
fun IndexScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf("الكل") }

    val list = remember(query, typeFilter) {
        QuranData.search(query).filter { typeFilter == "الكل" || it.type == typeFilter }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("الفهرس", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("السور (114)", "الأجزاء (30)").forEachIndexed { i, label ->
                FilterChip(
                    selected = tab == i, onClick = { fx.click(); tab = i },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                )
            }
        }
        if (tab == 0) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                placeholder = { Text("ابحث باسم السورة أو رقمها...", fontWeight = FontWeight.Bold) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            )
            LazyRow(contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("الكل", "مكية", "مدنية")) { t ->
                    FilterChip(
                        selected = typeFilter == t, onClick = { fx.click(); typeFilter = t },
                        label = { Text(t, fontWeight = FontWeight.Bold, fontSize = 12.sp) }, shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldWarm, selectedLabelColor = RoyalForest)
                    )
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (list.isEmpty()) item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة بهذا الاسم") }
                items(list, key = { it.num }) { s ->
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(s.page) }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(s.num)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("${s.type} • ${s.ayahs} آية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص ${s.page}", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            IconButton(onClick = { fx.click(); PlayerService.play(ctx, st.reciterId, s.num); nav.push(Screen.Audio) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.PlayCircleOutline, "استماع", tint = GoldWarm, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(30) { i ->
                    val start = QuranData.juzStart[i]
                    QCard(shape = RoundedCornerShape(18.dp), onClick = { nav.openPage(start) }) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberBadge(i + 1)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الجزء ${i + 1}", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = p.ink)
                                Text("يبدأ من سورة ${QuranData.surahForPage(start).name}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = p.muted)
                            }
                            Text("ص $start", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldWarm)
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// ui/AudioScreen.kt
// ============================================================
private fun fmtMs(ms: Int): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", s / 60, s % 60)
}

@Composable
fun AudioScreen(nav: Nav) {
    val st = LocalApp.current
    val p = LocalPalette.current
    val fx = LocalFx.current
    val ctx = LocalContext.current
    val ps by PlayerService.state.collectAsState()
    var query by remember { mutableStateOf("") }
    var showSleep by remember { mutableStateOf(false) }
    var drag by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(ps.error) {
        ps.error?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); PlayerService.clearError() }
    }

    val reciter = QuranData.reciters.firstOrNull { it.id == st.reciterId } ?: QuranData.reciters.first()
    val nowSurah = QuranData.surah(ps.surah)
    val nowReciter = QuranData.reciters.firstOrNull { it.id == ps.reciterId } ?: reciter
    val list = remember(query) { QuranData.search(query) }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader("المكتبة الصوتية", onBack = { nav.back() })

        LazyRow(contentPadding = PaddingValues(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(QuranData.reciters, key = { it.id }) { r ->
                FilterChip(
                    selected = r.id == st.reciterId, onClick = { fx.click(); st.reciterId = r.id },
                    shape = RoundedCornerShape(16.dp),
                    label = { Text(r.name, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg)
                )
            }
        }

        // ---------- بطاقة المشغل ----------
        Surface(
            shape = RoundedCornerShape(24.dp), color = RoyalForest, border = BorderStroke(1.5.dp, GoldWarm),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        ) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(nowSurah?.let { "سورة ${it.name}" } ?: "اختر سورة لبدء التلاوة", color = GoldSoftBg, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (nowSurah != null) "${nowReciter.name} • ${nowReciter.style}" else "${reciter.name} • ${reciter.style}", color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 11.sp)

                // عناصر التحكم تبقى بترتيب عالمي (يسار → يمين)
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(Modifier.fillMaxWidth()) {
                        val frac = drag ?: if (ps.durationMs > 0) ps.positionMs / ps.durationMs.toFloat() else 0f
                        Slider(
                            value = frac.coerceIn(0f, 1f),
                            onValueChange = { drag = it },
                            onValueChangeFinished = { drag?.let { PlayerService.seek(ctx, (it * ps.durationMs).toInt()) }; drag = null },
                            enabled = ps.surah > 0 && ps.durationMs > 0 && !ps.isLoading,
                            colors = SliderDefaults.colors(thumbColor = GoldShine, activeTrackColor = GoldShine, inactiveTrackColor = Color.White.copy(alpha = 0.3f))
                        )
                        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(fmtMs(if (drag != null) (drag!! * ps.durationMs).toInt() else ps.positionMs), color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text(fmtMs(ps.durationMs), color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { fx.click(); PlayerService.prev(ctx) }, enabled = ps.surah > 1) {
                                Icon(Icons.Default.SkipPrevious, "السورة السابقة", tint = Color.White)
                            }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, -10_000) }, enabled = ps.surah > 0) {
                                Icon(Icons.Default.Replay10, "رجوع 10 ثوانٍ", tint = GoldSoftBg)
                            }
                            Surface(
                                onClick = {
                                    fx.click()
                                    if (ps.surah == 0) PlayerService.play(ctx, st.reciterId, 1) else PlayerService.toggle(ctx)
                                },
                                shape = CircleShape, color = GoldShine, modifier = Modifier.size(52.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (ps.isLoading) CircularProgressIndicator(Modifier.size(26.dp), color = RoyalForest, strokeWidth = 3.dp)
                                    else Icon(if (ps.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, "تشغيل/إيقاف", tint = RoyalForest, modifier = Modifier.size(30.dp))
                                }
                            }
                            IconButton(onClick = { fx.click(); PlayerService.seekBy(ctx, 10_000) }, enabled = ps.surah > 0) {
                                Icon(Icons.Default.Forward10, "تقديم 10 ثوانٍ", tint = GoldSoftBg)
                            }
                            IconButton(onClick = { fx.click(); PlayerService.next(ctx) }, enabled = ps.surah in 1..113) {
                                Icon(Icons.Default.SkipNext, "السورة التالية", tint = Color.White)
                            }
                        }
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    ModeButton("تكرار السورة", Icons.Default.RepeatOne, ps.repeatOne) { PlayerService.toggleRepeat(ctx) }
                    ModeButton("ختمة متتالية", Icons.Default.PlaylistPlay, ps.autoNext) { PlayerService.toggleAutoNext(ctx) }
                    ModeButton(if (ps.sleepLeft > 0) "نوم: ${ps.sleepLeft}د" else "مؤقت نوم", Icons.Default.Bedtime, ps.sleepLeft > 0) { showSleep = true }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it }, singleLine = true,
            placeholder = { Text("ابحث عن سورة...", fontWeight = FontWeight.Bold) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = EmeraldVibrant) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "مسح") } },
            shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )

        LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (list.isEmpty()) item { EmptyState(Icons.Default.SearchOff, "لا توجد سورة بهذا الاسم") }
            items(list, key = { it.num }) { s ->
                val cached = remember(ps.cacheVersion, st.reciterId, s.num) { AudioCache.has(ctx, st.reciterId, s.num) }
                val progress = ps.downloads["${st.reciterId}_${s.num}"]
                val isCurrent = ps.surah == s.num && ps.reciterId == st.reciterId
                QCard(
                    shape = RoundedCornerShape(18.dp),
                    borderColor = if (isCurrent) EmeraldVibrant else null,
                    onClick = { if (isCurrent) PlayerService.toggle(ctx) else PlayerService.play(ctx, st.reciterId, s.num) }
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberBadge(s.num, 32)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("سورة ${s.name}", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = p.ink)
                            Text(
                                when {
                                    progress != null -> "جارٍ التنزيل ${(progress * 100).toInt()}%"
                                    cached -> "محملة (تعمل بدون إنترنت)"
                                    else -> "بث مباشر • ${s.ayahs} آية"
                                },
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (cached) TurquoiseAccent else p.muted
                            )
                        }
                        when {
                            progress != null -> CircularProgressIndicator(progress = { progress }, modifier = Modifier.size(24.dp), color = EmeraldVibrant, strokeWidth = 3.dp)
                            cached -> IconButton(onClick = { fx.click(); PlayerService.delete(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.DeleteOutline, "حذف التنزيل", tint = Color(0xFFB71C1C), modifier = Modifier.size(22.dp))
                            }
                            else -> IconButton(onClick = { fx.click(); PlayerService.download(ctx, st.reciterId, s.num) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Default.Download, "تنزيل", tint = GoldWarm, modifier = Modifier.size(22.dp))
                            }
                        }
                        IconButton(onClick = { fx.click(); if (isCurrent) PlayerService.toggle(ctx) else PlayerService.play(ctx, st.reciterId, s.num) }, modifier = Modifier.size(38.dp)) {
                            Icon(if (isCurrent && ps.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "تشغيل", tint = EmeraldVibrant, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        }
    }

    if (showSleep) {
        AlertDialog(
            onDismissRequest = { showSleep = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("مؤقت النوم", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0 to "إلغاء المؤقت", 15 to "بعد 15 دقيقة", 30 to "بعد 30 دقيقة", 60 to "بعد ساعة").forEach { (m, label) ->
                        TextButton(onClick = { PlayerService.setSleep(ctx, m); showSleep = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(label, fontWeight = FontWeight.ExtraBold, color = TurquoiseAccent)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSleep = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun ModeButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, active: Boolean, onClick: () -> Unit) {
    val fx = LocalFx.current
    TextButton(onClick = { fx.click(); onClick() }) {
        Icon(icon, null, tint = if (active) GoldShine else Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = if (active) GoldShine else Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

// ============================================================
// ui/TasbihScreen.kt
// ============================================================
@Composable
fun TasbihScreen(nav: Nav) {
    val fx = LocalFx.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(if (nav.tasbihTab == 0) "المسبحة الإلكترونية" else "الأذكار اليومية", onBack = { nav.back() })
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("المسبحة" to Icons.Default.TouchApp, "الأذكار" to Icons.Default.WbSunny).forEachIndexed { i, (label, icon) ->
                FilterChip(
                    selected = nav.tasbihTab == i, onClick = { fx.click(); nav.tasbihTab = i },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) }, shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldSoftBg, selectedLeadingIconColor = GoldShine)
                )
            }
        }
        if (nav.tasbihTab == 0) TasbihCounter() else AdhkarList()
    }
}

@Composable
private fun TasbihCounter() {
    val st = LocalApp.current
    val fx = LocalFx.current
    val stages = QuranData.tasbihStages

    var stage by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(stages[0].defaultTarget) }
    var count by rememberSaveable { mutableStateOf(0) }
    var rounds by rememberSaveable { mutableStateOf(0) }
    var autoAdvance by rememberSaveable { mutableStateOf(true) }

    val active = stages[stage]
    val next = stages[(stage + 1) % stages.size]
    val progress by animateFloatAsState((count / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f), label = "ring")

    fun goStage(i: Int) { stage = i; target = stages[i].defaultTarget; count = 0 }

    fun tap() {
        fx.click()
        if (!autoAdvance && count >= target) count = 0
        st.totalTasbih += 1
        if (count + 1 >= target) {
            fx.vibrate(if (st.stageVib) 140L else 35L)
            rounds += 1
            if (autoAdvance) goStage((stage + 1) % stages.size) else count = target
        } else {
            fx.vibrate(35L)
            count += 1
        }
    }

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(stages) { idx, s ->
                    FilterChip(
                        selected = idx == stage, onClick = { fx.click(); goStage(idx) },
                        shape = RoundedCornerShape(16.dp),
                        label = { Text(s.phrase, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldVibrant, selectedLabelColor = GoldShine)
                    )
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(28.dp), color = RoyalForest, border = BorderStroke(1.8.dp, GoldWarm), shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                Box {
                    IslamicPattern(Modifier.matchParentSize())
                    Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(3, 10, 33, 100).forEach { goal ->
                                    Surface(
                                        onClick = { fx.click(); target = goal; count = 0 },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (target == goal) GoldShine else EmeraldVibrant,
                                        border = BorderStroke(1.dp, GoldWarm)
                                    ) {
                                        Text("$goal", color = if (target == goal) RoyalForest else GoldSoftBg, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                            Surface(
                                onClick = { fx.click(); autoAdvance = !autoAdvance },
                                shape = RoundedCornerShape(12.dp),
                                color = if (autoAdvance) TurquoiseAccent else EmeraldVibrant.copy(alpha = 0.5f),
                                border = BorderStroke(1.dp, GoldWarm)
                            ) {
                                Text(if (autoAdvance) "انتقال تلقائي ✓" else "تكرار ثابت", color = GoldSoftBg, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(active.phrase, color = GoldShine, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, lineHeight = 36.sp)
                        Text(active.subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(170.dp)) {
                            CircularProgressIndicator(progress = { progress }, strokeWidth = 10.dp, color = GoldShine, trackColor = Color.White.copy(alpha = 0.18f), modifier = Modifier.fillMaxSize())
                            Surface(
                                onClick = { tap() }, shape = CircleShape, color = EmeraldVibrant,
                                border = BorderStroke(2.dp, GoldShine), modifier = Modifier.size(136.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text("$count", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
                                    Text("من $target", color = GoldSoftBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("الجولات المكتملة: $rounds • إجمالي التسبيحات: ${st.totalTasbih}", color = GoldSoftBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { fx.click(); count = 0 }) {
                                Icon(Icons.Default.Refresh, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تصفير", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(onClick = { fx.click(); if (count > 0) { count -= 1; if (st.totalTasbih > 0) st.totalTasbih -= 1 } }) {
                                Icon(Icons.Default.Undo, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("تراجع", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            TextButton(onClick = { fx.click(); goStage((stage + 1) % stages.size) }) {
                                Text("تخطي: ${next.phrase.take(14)}", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = GoldShine, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdhkarList() {
    val p = LocalPalette.current
    val fx = LocalFx.current
    val counts = remember { mutableStateMapOf<Int, Int>() }
    val adhkar = QuranData.adhkar

    LazyColumn(contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                val done = adhkar.indices.count { (counts[it] ?: 0) >= adhkar[it].target }
                Text("أنجزت $done من ${adhkar.size}", color = p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                TextButton(onClick = { fx.click(); counts.clear() }) {
                    Icon(Icons.Default.Refresh, null, tint = TurquoiseAccent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("إعادة الكل", color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
        itemsIndexed(adhkar) { i, d ->
            val c = counts[i] ?: 0
            val done = c >= d.target
            QCard(
                shape = RoundedCornerShape(20.dp), borderColor = if (done) GoldWarm else null,
                onClick = {
                    if (c < d.target) {
                        counts[i] = c + 1
                        fx.vibrate(if (c + 1 >= d.target) 100L else 30L)
                    }
                }
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(d.title, color = TurquoiseAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        if (done) Icon(Icons.Default.CheckCircle, "تم", tint = GoldWarm)
                        else if (c > 0) IconButton(onClick = { fx.click(); counts[i] = 0 }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Refresh, "إعادة", tint = p.muted, modifier = Modifier.size(18.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(d.text, color = p.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 26.sp)
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (c / d.target.toFloat()).coerceIn(0f, 1f) },
                        color = if (done) GoldWarm else EmeraldVibrant, trackColor = p.border.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("اضغط للعد • $c / ${d.target}", color = if (done) GoldWarm else p.muted, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================================================
// ui/MoreScreens.kt
// ============================================================
