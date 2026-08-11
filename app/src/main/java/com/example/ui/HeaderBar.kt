package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HeaderBar(
    title: String,
    artist: String,
    key: String?,
    bpm: Int?,
    isAiAnalyzing: Boolean,
    onAiAnalyzeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF14171D))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "CHORDFLY V2",
                    color = Color(0xFF00FF88),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.width(8.dp))

                key?.let {
                    Surface(
                        color = Color(0xFF232B36),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "KEY $it",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                bpm?.let {
                    Surface(
                        color = Color(0xFF232B36),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "$it BPM",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "$title • $artist",
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        IconButton(
            onClick = onAiAnalyzeClick,
            enabled = !isAiAnalyzing
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Gemini AI Correction",
                tint = if (isAiAnalyzing) Color.Gray else Color(0xFF00FF88)
            )
        }
    }
}
