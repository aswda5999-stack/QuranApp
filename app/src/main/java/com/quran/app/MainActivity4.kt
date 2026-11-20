package com.quran.app

import android.media.MediaPlayer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AudioRecitersScreen(
    isDarkTheme: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onBackHome: () -> Unit
) {
    val context = LocalContext.current
    val indexFont = remember(context) { CustomFontLoader.indexFont(context) }
    val titleFont = remember(context) { CustomFontLoader.homeTitleFont(context) }

    val reciters = remember {
        listOf(
            "مشاري راشد العفاسي",
            "ماهر المعيقلي",
            "عبد الباسط عبد الصمد",
            "ياسر الدوسري",
            "محمود خليل الحصري",
            "ناصر القطامي",
            "إسلام صبحي",
            "سعد الغامدي"
        )
    }

    val surahTracks = remember {
        listOf(
            "سورة الفاتحة" to "001",
            "سورة البقرة" to "002",
            "سورة الكهف" to "018",
            "سورة يس" to "036",
            "سورة الرحمن" to "055",
            "سورة الواقعة" to "056",
            "سورة الملك" to "067",
            "سورة الإخلاص" to "112"
        )
    }

    var reciterIndex by remember { mutableIntStateOf(0) }
    var selectedSurahIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var statusNote by remember { mutableStateOf("اختر السورة للتشغيل أو بدّل القارئ") }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    fun releasePlayer() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
        isPlaying = false
    }

    DisposableEffect(Unit) {
        onDispose { releasePlayer() }
    }

    fun startOrToggleStream(recIdx: Int, surIdx: Int) {
        val reciter = reciters[recIdx]
        val (surahName, code) = surahTracks[surIdx]
        if (reciterIndex == recIdx && selectedSurahIndex == surIdx && isPlaying) {
            releasePlayer()
            statusNote = "تم إيقاف التشغيل • $reciter"
            return
        }
        releasePlayer()
        reciterIndex = recIdx
        selectedSurahIndex = surIdx
        statusNote = "جاري تجهيز بث $surahName بصوت $reciter..."
        try {
            val mp = MediaPlayer().apply {
                setDataSource("https://server8.mp3quran.net/afs/$code.mp3")
                setOnPreparedListener {
                    start()
                    isPlaying = true
                    statusNote = "يعمل الآن: $surahName • $reciter"
                }
                setOnCompletionListener {
                    isPlaying = false
                    statusNote = "اكتملت تلاوة $surahName"
                }
                setOnErrorListener { _, _, _ ->
                    isPlaying = false
                    statusNote = "تعذر الاتصال بالبث المباشر (تحقق من الإنترنت)"
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (_: Exception) {
            statusNote = "تعذر تهيئة المشغل الصوتي"
        }
    }

    val screenBg = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val cardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else RoyalPalette.EmeraldRoyal
    val lightCardBg = if (isDarkTheme) RoyalPalette.EmeraldCardDark else androidx.compose.ui.graphics.Color.White
    val goldBorder = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.GoldRoyal
    val primaryInk = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald

    val filteredTracks = remember(searchQuery, surahTracks) {
        val q = searchQuery.trim()
        if (q.isEmpty()) surahTracks else surahTracks.filter { it.first.contains(q) }
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
            border = BorderStroke(1.6.dp, goldBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "الصوتيات والقراء",
                        color = RoyalPalette.CreamText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = titleFont
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "القارئ: ${reciters[reciterIndex]}",
                        color = RoyalPalette.GoldHighlight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = indexFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = statusNote,
                        color = RoyalPalette.CreamText.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        fontFamily = indexFont,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val nextRec = (reciterIndex + 1) % reciters.size
                            startOrToggleStream(nextRec, selectedSurahIndex)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoyalPalette.GoldRoyal,
                            contentColor = RoyalPalette.EmeraldDeep
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("تغيير القارئ", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = indexFont)
                    }

                    if (isPlaying) {
                        OutlinedButton(
                            onClick = { releasePlayer(); statusNote = "تم الإيقاف" },
                            border = BorderStroke(1.dp, RoyalPalette.GoldHighlight),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("إيقاف", color = RoyalPalette.GoldHighlight, fontSize = 11.sp, fontFamily = indexFont)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            itemsIndexed(filteredTracks) { idx, (surahTitle, _) ->
                val actualIdx = surahTracks.indexOfFirst { it.first == surahTitle }.coerceAtLeast(idx)
                val activeRow = actualIdx == selectedSurahIndex && isPlaying
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { startOrToggleStream(reciterIndex, actualIdx) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (activeRow) cardBg else lightCardBg,
                    border = BorderStroke(1.3.dp, goldBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = surahTitle,
                            color = if (activeRow) RoyalPalette.CreamText else primaryInk,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = indexFont
                        )
                        RoyalBadgePill(label = if (activeRow) "يُشغّل" else "تشغيل")
                    }
                }
            }
        }
    }
}
