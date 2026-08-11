package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChordTimestamp
import com.example.music.ChordTransposer

@Composable
fun NextChordsRow(
    nextChords: List<ChordTimestamp>,
    transposeOffset: Int,
    modifier: Modifier = Modifier
) {
    if (nextChords.isEmpty()) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF14171E))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "NEXT",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            nextChords.forEach { chordItem ->
                val transposed = ChordTransposer.transpose(chordItem.chord, transposeOffset)
                Surface(
                    color = Color(0xFF222934),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = transposed,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.0fs", chordItem.timeSec),
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
