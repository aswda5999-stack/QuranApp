package com.quran.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RoyalBadgePill(
    label: String,
    modifier: Modifier = Modifier,
    containerColor: Color = RoyalPalette.GoldRoyal.copy(alpha = 0.18f),
    contentColor: Color = RoyalPalette.GoldHighlight
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, RoyalPalette.GoldRoyal.copy(alpha = 0.65f)),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = contentColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RoyalSectionDivider(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = RoyalPalette.GoldRoyal
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = accentColor.copy(alpha = 0.45f)
        )
        Text(
            text = "  ✦ $title ✦  ",
            color = accentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = accentColor.copy(alpha = 0.45f)
        )
    }
}
