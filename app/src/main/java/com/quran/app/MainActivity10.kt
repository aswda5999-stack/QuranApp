package com.quran.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private data class MushafWord(val text: String, val verseKey: String)
private data class MushafLine(val number: Int, val words: List<MushafWord>)

private suspend fun loadMushafPage(page: Int): List<MushafLine> = withContext(Dispatchers.IO) {
    val url = URL(
        "https://api.quran.com/api/v4/verses/by_page/$page" +
            "?mushaf=1&words=true&word_fields=text_uthmani,page_number,line_number" +
            "&fields=verse_key&per_page=50"
    )
    val connection = url.openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "QuranApp-Android/4.1")
        val root = JSONObject(connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() })
        val verses = root.optJSONArray("verses") ?: throw IllegalStateException("Missing verses")
        val grouped = linkedMapOf<Int, MutableList<MushafWord>>()
        for (i in 0 until verses.length()) {
            val verse = verses.optJSONObject(i) ?: continue
            val key = verse.optString("verse_key")
            val words = verse.optJSONArray("words") ?: continue
            for (j in 0 until words.length()) {
                val word = words.optJSONObject(j) ?: continue
                val line = word.optInt("line_number", 0)
                val text = word.optString("text_uthmani").trim()
                if (line in 1..15 && text.isNotBlank() && text != "۞") {
                    grouped.getOrPut(line) { mutableListOf() }.add(MushafWord(text, key))
                }
            }
        }
        (1..15).map { line -> MushafLine(line, grouped[line].orEmpty()) }
    } finally {
        connection.disconnect()
    }
}

@Composable
fun QuranOnlinePage(
    pageNumber: Int,
    isDarkTheme: Boolean,
    onVerseSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var lines by remember(pageNumber) { mutableStateOf<List<MushafLine>?>(null) }
    var error by remember(pageNumber) { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val quranFont = remember(context) {
        try { FontFamily(Font(R.font.uthmanic)) } catch (_: Exception) { CustomFontLoader.indexFont(context) }
    }
    val ink = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald
    val accent = if (isDarkTheme) RoyalPalette.GoldHighlight else RoyalPalette.EmeraldRoyal

    LaunchedEffect(pageNumber) {
        lines = null
        error = false
        try { lines = loadMushafPage(pageNumber) }
        catch (_: Exception) { error = true }
    }

    Box(modifier = modifier.padding(horizontal = 5.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
        when {
            lines == null && !error -> CircularProgressIndicator(color = accent)
            error -> Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تعذر تحميل صفحة المصحف. تحقق من الإنترنت ثم أعد فتح الصفحة.", color = ink, textAlign = TextAlign.Center)
            }
            else -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..15).forEach { lineNumber ->
                    val line = lines!!.firstOrNull { it.number == lineNumber }
                    val annotated = buildAnnotatedString {
                        line?.words?.forEach { word ->
                            val start = length
                            append(word.text)
                            val end = length
                            addStringAnnotation("verse", word.verseKey, start, end)
                            append(" ")
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ClickableText(
                            text = annotated,
                            modifier = Modifier.fillMaxWidth(),
                            style = androidx.compose.ui.text.TextStyle(
                                color = ink,
                                fontFamily = quranFont,
                                fontSize = 20.sp,
                                lineHeight = 28.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal
                            ),
                            onClick = { offset ->
                                annotated.getStringAnnotations("verse", offset, offset)
                                    .firstOrNull()?.let { onVerseSelected(it.item) }
                            }
                        )
                    }
                }
            }
        }
    }
}
