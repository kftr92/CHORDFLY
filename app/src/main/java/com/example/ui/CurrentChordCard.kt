package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChordTimestamp
import com.example.music.ChordTransposer

@Composable
fun CurrentChordCard(
    currentChord: ChordTimestamp?,
    transposeOffset: Int,
    currentTimeSec: Float,
    notes: List<String>,
    modifier: Modifier = Modifier
) {
    val transposedChordName = currentChord?.let {
        ChordTransposer.transpose(it.chord, transposeOffset)
    } ?: "N.C."

    val confidence = currentChord?.confidence ?: 1.0f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF191D24))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "CURRENT CHORD",
                color = Color.Gray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = transposedChordName,
                color = Color(0xFF00FF88),
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            if (notes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    notes.forEach { note ->
                        Surface(
                            color = Color(0xFF2B3340),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = note,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = String.format("%.1f s", currentTimeSec),
                color = Color.White,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Confidence Progress Meter
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(confidence * 100).toInt()}%",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                LinearProgressIndicator(
                    progress = { confidence.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .width(48.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF00FF88),
                    trackColor = Color(0xFF2A313C)
                )
            }
        }
    }
}
