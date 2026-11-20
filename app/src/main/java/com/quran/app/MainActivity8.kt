package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoyalBadgePill(label: String, modifier: Modifier = Modifier) {
    Surface(
        color = RoyalPalette.GoldRoyal.copy(alpha = 0.16f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, RoyalPalette.GoldRoyal.copy(alpha = 0.55f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = RoyalPalette.GoldRoyal,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
