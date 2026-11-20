package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AdhanAndQiblaScreen(
    startOnQibla: Boolean,
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    var qiblaIsolatedMode by remember(startOnQibla) { mutableStateOf(startOnQibla) }
    var showMuezzinList by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }

    val muezzins = remember {
        listOf(
            "مؤذن المسجد الحرام • علي الملا",
            "مؤذن المسجد النبوي • عصام بخاري",
            "مشاري راشد العفاسي",
            "ماجد الحمداني",
            "منصور الزهراني"
        )
    }
    var selectedMuezzin by remember { mutableStateOf(muezzins.first()) }

    val prayerTimes = remember {
        listOf(
            "الفجر" to "04:58 ص",
            "الظهر" to "11:42 ص",
            "العصر" to "02:36 م",
            "المغرب" to "04:51 م",
            "العشاء" to "06:14 م"
        )
    }

    var azimuth by remember { mutableFloatStateOf(0f) }
    val qiblaBearing = remember { QiblaCalculator.bearingToKaaba(36.2021, 37.1343) }
    val relativeNeedle = ((qiblaBearing - azimuth + 360f) % 360f)

    DisposableEffect(qiblaIsolatedMode) {
        val tracker = CompassTracker(context) { newAzimuth -> azimuth = newAzimuth }
        if (qiblaIsolatedMode) tracker.start()
        onDispose { tracker.stop() }
    }

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val gold = RoyalPalette.GoldRoyal
    val cream = RoyalPalette.CreamText

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(emeraldBg)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            text = if (qiblaIsolatedMode) "القبلة والبوصلة" else "المؤذن ومواقيت الصلاة",
                            color = cream,
                            fontSize = 17.sp,
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
                        placeholder = { Text("ابحث عن مؤذن أو صلاة...", color = cream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cream,
                            unfocusedTextColor = cream,
                            focusedBorderColor = gold,
                            unfocusedBorderColor = gold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (qiblaIsolatedMode) {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text(
                        text = "اتجاه القبلة: ${qiblaBearing.toInt()}° | اتجاه الجهاز: ${azimuth.toInt()}°",
                        color = gold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Canvas(modifier = Modifier.size(230.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = size.minDimension / 2f - 14f

                        drawCircle(
                            color = gold.copy(alpha = 0.18f),
                            radius = radius,
                            center = center
                        )
                        drawCircle(
                            color = gold,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 4.dp.toPx())
                        )

                        val rad = Math.toRadians((relativeNeedle - 90.0))
                        val tipX = center.x + (radius * 0.82f * cos(rad)).toFloat()
                        val tipY = center.y + (radius * 0.82f * sin(rad)).toFloat()

                        drawLine(
                            color = Color(0xFF4ADE80),
                            start = center,
                            end = Offset(tipX, tipY),
                            strokeWidth = 7.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = gold,
                            radius = 9.dp.toPx(),
                            center = center
                        )
                    }

                    Text(
                        text = "حرّك الهاتف ببطء بشكل أفقي لتحديد الاتجاه بدقة",
                        color = cream.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )

                    OutlinedButton(
                        onClick = { qiblaIsolatedMode = false },
                        border = BorderStroke(1.dp, gold)
                    ) {
                        Text("العودة إلى شاشة المؤذن", color = gold)
                    }
                }
            }
        } else {
            Surface(
                color = cardColor,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, gold.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "المؤذن المحدد", color = gold, fontSize = 12.sp)
                        Text(
                            text = selectedMuezzin,
                            color = cream,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { showMuezzinList = !showMuezzinList },
                        colors = ButtonDefaults.buttonColors(containerColor = gold, contentColor = RoyalPalette.EmeraldRoyal),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (showMuezzinList) "إغلاق القائمة" else "تغيير المؤذن",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (showMuezzinList) {
                val filteredMuezzins = muezzins.filter {
                    searchQuery.isBlank() || it.contains(searchQuery.trim(), ignoreCase = true)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredMuezzins) { m ->
                        val active = m == selectedMuezzin
                        Surface(
                            color = if (active) gold.copy(alpha = 0.2f) else cardColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (active) gold else gold.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedMuezzin = m
                                    showMuezzinList = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = m, color = cream, fontSize = 15.sp)
                                if (active) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = gold)
                                }
                            }
                        }
                    }
                }
            } else {
                val filteredTimes = prayerTimes.filter {
                    searchQuery.isBlank() || it.first.contains(searchQuery.trim(), ignoreCase = true)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredTimes) { (prayer, time) ->
                        Surface(
                            color = cardColor,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, gold.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = prayer, color = cream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = time, color = gold, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
