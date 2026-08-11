package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
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
fun TransportControls(
    transposeOffset: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onReset: () -> Unit,
    aiStatusMessage: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF11141A))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Key Transpose:",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (transposeOffset > 0) "+$transposeOffset" else "$transposeOffset",
                    color = Color(0xFF00FF88),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Transpose Down", tint = Color.White)
                }

                TextButton(onClick = onReset) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Reset Transpose",
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("RESET", color = Color.Gray, fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Transpose Up", tint = Color.White)
                }
            }
        }

        if (aiStatusMessage.isNotBlank()) {
            Text(
                text = aiStatusMessage,
                color = Color(0xFF88FFBB),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161C24))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
