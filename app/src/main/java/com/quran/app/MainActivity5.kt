package com.quran.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

@Composable
fun AdhanAndQiblaScreen(
    startOnQibla: Boolean,
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }

    var qiblaIsolatedMode by remember(startOnQibla) { mutableStateOf(startOnQibla) }
    var showMuezzinList by remember { mutableStateOf(false) }
    var selectedMuezzin by remember { mutableStateOf("أذان المسجد الحرام") }
    val savedCoords = remember { PrayerTimesEngine.savedLocation(context) }
    var userLat by remember { mutableDoubleStateOf(savedCoords?.first ?: 0.0) }
    var userLon by remember { mutableDoubleStateOf(savedCoords?.second ?: 0.0) }
    var prayerSchedule by remember { mutableStateOf<List<PrayerTimeItem>>(emptyList()) }
    var prayerLoadError by remember { mutableStateOf(false) }
    var adhanEnabled by remember { mutableStateOf(PrayerTimesEngine.isAdhanEnabled(context)) }
    var azimuth by remember { mutableFloatStateOf(0f) }

    fun refreshLastLocation() {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager ?: return
        val providers = lm.getProviders(true)
        var best: Location? = null
        for (p in providers) {
            val loc = try { lm.getLastKnownLocation(p) } catch (_: SecurityException) { null }
            if (loc != null && (best == null || loc.accuracy < best.accuracy)) {
                best = loc
            }
        }
        if (best != null) {
            userLat = best.latitude
            userLon = best.longitude
            PrayerTimesEngine.saveLocation(context, best.latitude, best.longitude)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> refreshLastLocation() }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
        }
    }

    DisposableEffect(Unit) {
        refreshLastLocation()
        val tracker = CompassTracker(context) { newAz -> azimuth = newAz }
        tracker.start()
        onDispose { tracker.stop() }
    }

    val qiblaBearing = remember(userLat, userLon) { QiblaCalculator.bearingToKaaba(userLat, userLon) }
    val relativeAngle = ((qiblaBearing - azimuth) + 360f) % 360f

    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val cardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val lightCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val primaryInk = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val muezzins = remember {
        listOf("حسن خلفان – مؤذن جامع الإمام محمد بن عبدالوهاب", "صالح النابت – مؤذن جامع الإمام محمد بن عبدالوهاب")
    }

    LaunchedEffect(userLat, userLon) {
        if (userLat in -90.0..90.0 && userLon in -180.0..180.0 && (userLat != 0.0 || userLon != 0.0)) {
            prayerLoadError = false
            try {
                val times = PrayerTimesEngine.fetchToday(userLat, userLon)
                prayerSchedule = times
                PrayerTimesEngine.saveLocation(context, userLat, userLon)
                PrayerTimesEngine.scheduleToday(context, times)
            } catch (_: Exception) {
                prayerLoadError = true
            }
        }
    }

    if (qiblaIsolatedMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                    Text(
                        text = "بوصلة القبلة المعزولة",
                        color = RoyalPalette.CreamText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    OutlinedButton(
                        onClick = { qiblaIsolatedMode = false },
                        border = BorderStroke(1.dp, RoyalPalette.GoldHighlight),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("مواقيت الأذان", color = RoyalPalette.GoldHighlight, fontSize = 12.sp, fontFamily = indexFont)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2f, size.height / 2f)
                        val r = size.minDimension * 0.44f
                        drawCircle(
                            color = goldBorder.copy(alpha = 0.25f),
                            radius = r,
                            center = c
                        )
                        drawCircle(
                            color = goldBorder,
                            radius = r,
                            center = c,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        rotate(degrees = relativeAngle, pivot = c) {
                            val arrow = Path().apply {
                                moveTo(c.x, c.y - r * 0.78f)
                                lineTo(c.x + r * 0.16f, c.y + r * 0.12f)
                                lineTo(c.x, c.y - r * 0.04f)
                                lineTo(c.x - r * 0.16f, c.y + r * 0.12f)
                                close()
                            }
                            drawPath(arrow, color = RoyalPalette.GoldRoyal)
                            drawLine(
                                color = RoyalPalette.GoldHighlight,
                                start = Offset(c.x, c.y - r * 0.78f),
                                end = Offset(c.x, c.y + r * 0.55f),
                                strokeWidth = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "زاوية القبلة: ${qiblaBearing.toInt()}° • اتجاه الجهاز: ${azimuth.toInt()}°",
                    color = primaryInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = indexFont,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = cardBg, contentColor = RoyalPalette.CreamText),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تحديث الموقع الجغرافي للقبلة", fontFamily = indexFont, fontWeight = FontWeight.Bold)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.6.dp, goldBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "المؤذن ومواقيت الصلاة",
                        color = RoyalPalette.CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Text(
                        text = "المؤذن المختار: $selectedMuezzin",
                        color = RoyalPalette.GoldHighlight,
                        fontSize = 13.sp,
                        fontFamily = indexFont
                    )
                    Text(
                        text = "معاينة التسجيلات متاحة من مكتبة الأذان الخارجية. تنبيه الصلاة يعتمد حالياً صوت المنبّه في الجهاز.",
                        color = RoyalPalette.CreamText.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontFamily = indexFont
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMuezzinList = !showMuezzinList },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalPalette.GoldRoyal,
                                contentColor = RoyalPalette.EmeraldDeep
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("اختيار المؤذن", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = indexFont)
                        }

                        OutlinedButton(
                            onClick = { qiblaIsolatedMode = true },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, RoyalPalette.GoldHighlight),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("بوصلة القبلة", color = RoyalPalette.GoldHighlight, fontSize = 12.sp, fontFamily = indexFont)
                        }
                    }
                }
            }
        }

        if (showMuezzinList) {
            items(muezzins) { mName ->
                val isSel = mName == selectedMuezzin
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedMuezzin = mName
                            showMuezzinList = false
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://audio.islamweb.net/audio/adhan/مختارات-من-الأذان"))
                                context.startActivity(intent)
                            } catch (_: Exception) { }
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSel) cardBg else lightCardBg,
                    border = BorderStroke(1.2.dp, goldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mName,
                            color = if (isSel) RoyalPalette.CreamText else primaryInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = indexFont
                        )
                        if (isSel) RoyalBadgePill(label = "مختار")
                    }
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = lightCardBg,
                border = BorderStroke(1.dp, goldBorder)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (userLat != 0.0 || userLon != 0.0) "الموقع التلقائي: ${"%.4f".format(userLat)}, ${"%.4f".format(userLon)}" else "يرجى السماح بالوصول إلى الموقع لحساب المواقيت",
                        color = primaryInk, fontSize = 12.sp, fontFamily = indexFont
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("تشغيل تنبيهات الصلاة", color = primaryInk, fontWeight = FontWeight.Bold, fontFamily = indexFont)
                        Switch(checked = adhanEnabled, onCheckedChange = {
                            adhanEnabled = it
                            PrayerTimesEngine.setAdhanEnabled(context, it)
                            if (it && prayerSchedule.isNotEmpty()) PrayerTimesEngine.scheduleToday(context, prayerSchedule)
                        })
                    }
                    OutlinedButton(
                        onClick = {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.POST_NOTIFICATIONS))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("تحديث موقعي ومواقيت الصلاة", fontFamily = indexFont) }
                    OutlinedButton(
                        onClick = {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                                try {
                                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                                } catch (_: Exception) { }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("السماح بالتنبيهات الدقيقة", fontFamily = indexFont) }
                    if (prayerLoadError) Text("تعذر جلب المواقيت حالياً. تحقق من الإنترنت ثم أعد المحاولة.", color = Color.Red, fontSize = 12.sp)
                    if (prayerSchedule.isEmpty() && !prayerLoadError) Text("جارٍ تحميل مواقيت الصلاة…", color = primaryInk, fontSize = 12.sp)
                }
            }
        }

        items(prayerSchedule) { prayer -> 
            val prayerName = prayer.name
            val prayerTime = prayer.time
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = lightCardBg,
                border = BorderStroke(1.2.dp, goldBorder.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = prayerName,
                        color = primaryInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = indexFont
                    )
                    RoyalBadgePill(label = prayerTime)
                }
            }
        }
    }
}
