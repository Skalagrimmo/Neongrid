package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun GameMenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onStartNewGame: () -> Unit,
    onLoadGame: () -> Unit,
    onViewControls: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
    ) {
        // Aesthetic: Subtle custom grid canvas backdrop using Immersive Theme style
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridW = 50f
            val gridH = 25f
            // Draw isometric background matrix lines in subtle white/lavender
            for (i in -10..30) {
                // Diagonal left-to-right
                drawLine(
                    color = Color(0x06D0BCFF),
                    start = Offset(0f, i * gridH),
                    end = Offset(size.width, i * gridH + size.width * (gridH / gridW)),
                    strokeWidth = 1f
                )
                // Diagonal right-to-left
                drawLine(
                    color = Color(0x06D0BCFF),
                    start = Offset(size.width, i * gridH),
                    end = Offset(0f, i * gridH + size.width * (gridH / gridW)),
                    strokeWidth = 1f
                )
            }
        }

        // Top Neon Cyber Header Banner (Immersive palette)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NEONGRID",
                fontSize = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = ImmersiveLavender,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "ISOMETRIC COGNITIVE-RPG",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ImmersiveSlateMuted,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
        }

        // Core Interaction Panel (Centered)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .width(280.dp)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // New Run Card Button (Immersive Primary Lavender)
            Button(
                onClick = onStartNewGame,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveLavender),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("menu_new_run_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "New Run", tint = Color.Black)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NEW COGNITIVE RUN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Load Run Card Button (Immersive Header Bordered style)
            Button(
                onClick = onLoadGame,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.8f)),
                border = BorderStroke(1.5.dp, ImmersiveLavender),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("menu_load_run_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Refresh, contentDescription = "Load Run", tint = ImmersiveLavender)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "RESUME SAVED INSTANCE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Directives & Instructions button (Immersive Amber highlight style)
            Button(
                onClick = onViewControls,
                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, ImmersiveAmber.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("menu_controls_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = "Directives", tint = ImmersiveAmber)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "DIRECTIVES MATRIX",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Bottom Decorative Info Label
        Text(
            text = "OPTIMIZED FOR older 5.0\" ANDROID RETRO_CELL SYSTEMS\nVER_1.4.2 // COMPOSE_ENGINE // NO SHADER_OVERHEAD",
            color = ImmersiveSlateMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )
    }
}
