package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChordTimestamp
import com.example.music.ChordTransposer
import kotlinx.coroutines.launch

@Composable
fun ChordTimelineGrid(
    chords: List<ChordTimestamp>,
    activeChord: ChordTimestamp?,
    transposeOffset: Int,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val activeIndex = if (activeChord != null) chords.indexOf(activeChord) else -1

    LaunchedEffect(activeIndex) {
        if (activeIndex in chords.indices) {
            coroutineScope.launch {
                gridState.animateScrollToItem(activeIndex)
            }
        }
    }

    Column(modifier = modifier.padding(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIMELINE CHORD (${chords.size} BAR)",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (activeIndex >= 0) {
                Text(
                    text = "BAR ${activeIndex + 1} / ${chords.size}",
                    color = Color(0xFF00FF88),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(chords) { index, item ->
                val isActive = index == activeIndex
                val transposed = ChordTransposer.transpose(item.chord, transposeOffset)
                val bgColor by animateColorAsState(
                    targetValue = if (isActive) Color(0xFF00FF88) else Color(0xFF1E232B),
                    label = "bg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isActive) Color.Black else Color.White,
                    label = "text"
                )
                val scaleFactor by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1.0f,
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .scale(scaleFactor)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgColor)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color = if (isActive) Color.White else Color(0xFF2C3440),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B${index + 1}",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(2.dp),
                        fontFamily = FontFamily.Monospace
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = transposed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = String.format("%.1fs", item.timeSec),
                            fontSize = 9.sp,
                            color = textColor.copy(alpha = 0.7f),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
