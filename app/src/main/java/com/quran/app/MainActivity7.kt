package com.quran.app

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsAndVoiceAdhkarScreen(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }

    val prefs = remember { context.getSharedPreferences(VoiceAdhkarScheduler.PREFS_NAME, Context.MODE_PRIVATE) }

    var voiceEnabled by remember { mutableStateOf(prefs.getBoolean(VoiceAdhkarScheduler.KEY_ENABLED, false)) }
    var intervalMinutes by remember { mutableIntStateOf(prefs.getInt(VoiceAdhkarScheduler.KEY_INTERVAL_MIN, 15)) }
    var selectedPhrase by remember {
        mutableStateOf(prefs.getString(VoiceAdhkarScheduler.KEY_PHRASE, "سبحان الله") ?: "سبحان الله")
    }
    var lockScreenVisible by remember { mutableStateOf(prefs.getBoolean(VoiceAdhkarScheduler.KEY_LOCK_SCREEN, true)) }
    var statusBanner by remember {
        mutableStateOf(if (voiceEnabled) "التنبيه الصوتي مفعّل كل $intervalMinutes دقيقة" else "التنبيه الصوتي متوقف حالياً")
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    fun applyVoiceSchedule(enabled: Boolean, mins: Int, phrase: String, lockScreen: Boolean) {
        if (enabled) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            VoiceAdhkarScheduler.schedule(context, mins, phrase, lockScreen)
            statusBanner = "تم جدولة الذكر الصوتي ($phrase) كل $mins دقيقة"
        } else {
            VoiceAdhkarScheduler.cancel(context)
            statusBanner = "تم إيقاف جدولة الأذكار الصوتية"
        }
    }

    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val cardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val lightCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val primaryInk = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val intervals = remember { listOf(5, 10, 15, 30, 60) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBg)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
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
                        .padding(14.dp)
                ) {
                    Text(
                        text = "الإعدادات والأذكار الصوتية",
                        color = RoyalPalette.CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusBanner,
                        color = RoyalPalette.GoldHighlight,
                        fontSize = 12.sp,
                        fontFamily = indexFont
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = lightCardBg,
                border = BorderStroke(1.3.dp, goldBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الوضع الليلي الزمردي الداكن",
                            color = primaryInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = indexFont
                        )
                        Text(
                            text = "التبديل بين المظهر الكريمي الدافئ والداكن",
                            color = primaryInk.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = indexFont
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onToggleDarkTheme,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RoyalPalette.EmeraldDeep,
                            checkedTrackColor = RoyalPalette.GoldRoyal
                        )
                    )
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = lightCardBg,
                border = BorderStroke(1.3.dp, goldBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تفعيل الأذكار الصوتية المجدولة",
                            color = primaryInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = indexFont
                        )
                        Switch(
                            checked = voiceEnabled,
                            onCheckedChange = { checked ->
                                voiceEnabled = checked
                                applyVoiceSchedule(checked, intervalMinutes, selectedPhrase, lockScreenVisible)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RoyalPalette.EmeraldDeep,
                                checkedTrackColor = RoyalPalette.GoldRoyal
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إظهار الإشعار على شاشة القفل",
                            color = primaryInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = indexFont
                        )
                        Switch(
                            checked = lockScreenVisible,
                            onCheckedChange = { checked ->
                                lockScreenVisible = checked
                                if (voiceEnabled) {
                                    applyVoiceSchedule(true, intervalMinutes, selectedPhrase, checked)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RoyalPalette.EmeraldDeep,
                                checkedTrackColor = RoyalPalette.GoldRoyal
                            )
                        )
                    }

                    Text(
                        text = "الفترة الزمنية للتكرار:",
                        color = primaryInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = indexFont
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        intervals.forEach { mins ->
                            val active = intervalMinutes == mins
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        intervalMinutes = mins
                                        if (voiceEnabled) {
                                            applyVoiceSchedule(true, mins, selectedPhrase, lockScreenVisible)
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = if (active) cardBg else screenBg,
                                border = BorderStroke(1.dp, goldBorder)
                            ) {
                                Text(
                                    text = "$mins د",
                                    color = if (active) RoyalPalette.CreamText else primaryInk,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = indexFont,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = "تخصيص الفاصل الزمني: $intervalMinutes دقيقة",
                        color = primaryInk,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = indexFont
                    )
                    Slider(
                        value = intervalMinutes.toFloat().coerceIn(1f, 240f),
                        onValueChange = { intervalMinutes = it.toInt().coerceIn(1, 240) },
                        onValueChangeFinished = {
                            if (voiceEnabled) applyVoiceSchedule(true, intervalMinutes, selectedPhrase, lockScreenVisible)
                        },
                        valueRange = 1f..240f
                    )

                    Text(
                        text = "اختر عبارة الذكر الصوتي:",
                        color = primaryInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = indexFont
                    )

                    VoiceAdhkarScheduler.availablePhrases.forEach { phrase ->
                        val picked = selectedPhrase == phrase
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedPhrase = phrase
                                    if (voiceEnabled) {
                                        applyVoiceSchedule(true, intervalMinutes, phrase, lockScreenVisible)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (picked) cardBg else screenBg,
                            border = BorderStroke(1.dp, goldBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = phrase,
                                    color = if (picked) RoyalPalette.CreamText else primaryInk,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = indexFont
                                )
                                if (picked) RoyalBadgePill(label = "مفعّل")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                voiceEnabled = true
                                applyVoiceSchedule(true, intervalMinutes, selectedPhrase, lockScreenVisible)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RoyalPalette.GoldRoyal,
                                contentColor = RoyalPalette.EmeraldDeep
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("حفظ الجدولة", fontWeight = FontWeight.Bold, fontFamily = indexFont)
                        }

                        OutlinedButton(
                            onClick = {
                                voiceEnabled = false
                                applyVoiceSchedule(false, intervalMinutes, selectedPhrase, lockScreenVisible)
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, goldBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("إيقاف الجدولة", color = primaryInk, fontFamily = indexFont)
                        }
                    }
                }
            }
        }
    }
}
