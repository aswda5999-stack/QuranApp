package com.quran.app.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.app.RoyalPalette
import com.quran.app.data.AppPreferencesRepository
import androidx.compose.ui.platform.LocalContext
import kotlin.math.ceil

@Composable
fun KhatmaScreen(
    lastReadPage: Int,
    isDarkTheme: Boolean,
    onOpenReader: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { AppPreferencesRepository(context) }
    var target by remember { mutableIntStateOf(repository.khatmaTargetPages()) }
    val done = (lastReadPage - 1).coerceAtLeast(0)
    val remaining = (604 - done).coerceAtLeast(0)
    val days = ceil(remaining.toDouble() / target.coerceAtLeast(1)).toInt()
    val background = if (isDarkTheme) RoyalPalette.EmeraldDeep else RoyalPalette.CreamBackground
    val card = if (isDarkTheme) RoyalPalette.EmeraldCardDark else Color.White
    val ink = if (isDarkTheme) RoyalPalette.CreamText else RoyalPalette.InkEmerald
    val progress = done / 604f

    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("خطة ختم القرآن", color = ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        Text("نظّم وردك اليومي وتابع تقدّمك في قراءة المصحف.", color = ink.copy(alpha = .75f), fontSize = 14.sp)
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = card)
        ) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تقدّم القراءة المحفوظ", color = ink, fontWeight = FontWeight.Bold)
                Text("${(progress * 100).toInt()}%", color = RoyalPalette.GoldMuted, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                androidx.compose.material3.LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    color = RoyalPalette.GoldRoyal,
                    trackColor = ink.copy(alpha = .12f)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("الصفحة ${lastReadPage.coerceIn(1,604)}", color = ink, fontSize = 13.sp)
                    Text("604 صفحة", color = ink.copy(alpha = .65f), fontSize = 13.sp)
                }
            }
        }
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = card)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("الورد اليومي", color = ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("$target صفحة يومياً", color = RoyalPalette.GoldMuted, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = target.toFloat(),
                    onValueChange = { target = it.toInt().coerceIn(1,100) },
                    valueRange = 1f..100f,
                    steps = 98
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("صفحة واحدة", color = ink.copy(alpha = .65f), fontSize = 12.sp)
                    Text("100 صفحة", color = ink.copy(alpha = .65f), fontSize = 12.sp)
                }
                Text("المدة التقديرية لإكمال الختمة: $days يوم", color = ink, fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = { repository.saveKhatmaTargetPages(target) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalPalette.EmeraldRoyal),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("حفظ الورد اليومي", color = Color.White) }
            }
        }
        Spacer(Modifier.height(2.dp))
        Button(
            onClick = onOpenReader,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RoyalPalette.GoldRoyal),
            shape = RoundedCornerShape(14.dp)
        ) { Text("متابعة قراءة المصحف", color = RoyalPalette.EmeraldDeep, fontWeight = FontWeight.Bold) }
        Text(
            "ملاحظة: يعتمد تقدّم القراءة على آخر صفحة حفظها التطبيق، ويمكنك تحديثه بمتابعة القراءة من المصحف.",
            color = ink.copy(alpha = .65f), fontSize = 12.sp, textAlign = TextAlign.Center
        )
    }
}
