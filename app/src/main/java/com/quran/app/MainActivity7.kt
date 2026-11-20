package com.quran.app

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val prefs = remember { context.getSharedPreferences(VoiceAdhkarScheduler.PREFS_NAME, Context.MODE_PRIVATE) }

    var voiceEnabled by remember { mutableStateOf(prefs.getBoolean(VoiceAdhkarScheduler.KEY_ENABLED, false)) }
    var intervalMinutes by remember { mutableIntStateOf(prefs.getInt(VoiceAdhkarScheduler.KEY_INTERVAL_MIN, 15)) }
    var selectedPhrase by remember {
        mutableStateOf(prefs.getString(VoiceAdhkarScheduler.KEY_PHRASE, "سبحان الله") ?: "سبحان الله")
    }
    var lockScreenNotify by remember { mutableStateOf(prefs.getBoolean(VoiceAdhkarScheduler.KEY_LOCK_SCREEN, true)) }
    var searchExpanded by remember { mutableStateOf(false) }

    val intervals = listOf(5, 10, 15, 30, 60)

    val emeraldBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.EmeraldRoyal
    val cardColor = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldCard
    val gold = RoyalPalette.GoldRoyal
    val cream = RoyalPalette.CreamText

    fun applyVoiceSchedule(enabled: Boolean, mins: Int, phrase: String, lockScreen: Boolean) {
        if (enabled) {
            VoiceAdhkarScheduler.schedule(context, mins, phrase, lockScreen)
        } else {
            VoiceAdhkarScheduler.cancel(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(emeraldBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = cardColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
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
                            text = "الإعدادات والأذكار الصوتية",
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
                        placeholder = { Text("ابحث في الإعدادات...", color = cream.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = cream,
                            unfocusedTextColor = cream,
                            focusedBorderColor = gold,
                            unfocusedBorderColor = gold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Surface(
            color = cardColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("الوضع الليلي", color = cream, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onToggleDarkTheme,
                    colors = SwitchDefaults.colors(checkedThumbColor = gold, checkedTrackColor = Color(0xFF1F634B))
                )
            }
        }

        Surface(
            color = cardColor,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, gold.copy(alpha = 0.45f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الأذكار الصوتية التلقائية", color = gold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("نطق الذكر تلقائياً حسب الوقت المحدد", color = cream.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    Switch(
                        checked = voiceEnabled,
                        onCheckedChange = { checked ->
                            voiceEnabled = checked
                            applyVoiceSchedule(checked, intervalMinutes, selectedPhrase, lockScreenNotify)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = gold, checkedTrackColor = Color(0xFF1F634B))
                    )
                }

                Text("الفترة الزمنية (بالدقائق):", color = cream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    intervals.forEach { mins ->
                        val active = intervalMinutes == mins
                        Surface(
                            color = if (active) gold else Color(0xFF0A241B),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, gold.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    intervalMinutes = mins
                                    applyVoiceSchedule(voiceEnabled, mins, selectedPhrase, lockScreenNotify)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(
                                    text = "${mins}د",
                                    color = if (active) RoyalPalette.EmeraldRoyal else cream,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Text("اختر الذكر الصوتي:", color = cream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                VoiceAdhkarScheduler.availablePhrases.forEach { phrase ->
                    val active = selectedPhrase == phrase
                    Surface(
                        color = if (active) gold.copy(alpha = 0.2f) else Color(0xFF0A241B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (active) gold else gold.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedPhrase = phrase
                                applyVoiceSchedule(voiceEnabled, intervalMinutes, phrase, lockScreenNotify)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = phrase, color = cream, fontSize = 14.sp)
                            if (active) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = gold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "إشعار على الشاشة حتى والهاتف مقفل",
                        color = cream,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = lockScreenNotify,
                        onCheckedChange = { checked ->
                            lockScreenNotify = checked
                            applyVoiceSchedule(voiceEnabled, intervalMinutes, selectedPhrase, checked)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = gold, checkedTrackColor = Color(0xFF1F634B))
                    )
                }
            }
        }
    }
}
