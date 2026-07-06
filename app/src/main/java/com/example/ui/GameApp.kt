package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun GameApp(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = ImmersiveBgDark
    ) {
        Crossfade(targetState = viewModel.currentScreen) { screen ->
            when (screen) {
                GameViewModel.Screen.MENU -> {
                    GameMenuScreen(
                        viewModel = viewModel,
                        onStartNewGame = {
                            viewModel.resetGameEntities()
                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                        },
                        onLoadGame = {
                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                        },
                        onViewControls = {
                            viewModel.changeScreen(GameViewModel.Screen.CONTROLS)
                        }
                    )
                }

                GameViewModel.Screen.PLAY -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Custom Interactive Canvas Viewport
                        GameCanvas(
                            viewModel = viewModel,
                            onMovePlayer = { dx, dy ->
                                viewModel.movePlayer(dx, dy)
                            }
                        )

                        // Top Overlay gameplay interface HUD
                        GameHud(
                            viewModel = viewModel,
                            onBackToMenu = {
                                viewModel.changeScreen(GameViewModel.Screen.MENU)
                            },
                            onOpenSkillTree = {
                                viewModel.changeScreen(GameViewModel.Screen.SKILL_TREE)
                            },
                            onOpenLoadout = {
                                viewModel.changeScreen(GameViewModel.Screen.LOADOUT)
                            }
                        )

                        // --- GAME OVER SCREEN OVERLAY ---
                        if (viewModel.isGameOver) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ImmersiveBgDark.copy(alpha = 0.92f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "CRITICAL DESYNC",
                                        color = ImmersiveRed,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "COGNITIVE MATRIX SHUTDOWN. MEMORY TRACE WIPED.",
                                        color = ImmersiveSlateMuted,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.resetGameEntities()
                                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveRed),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(220.dp).height(48.dp).testTag("game_over_respawn_button")
                                    ) {
                                        Text("RESPAWN CLONE", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.changeScreen(GameViewModel.Screen.MENU) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, ImmersiveSlateMuted),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(220.dp).height(44.dp).testTag("game_over_menu_button")
                                    ) {
                                        Text("BACK TO MAIN HUB", color = ImmersiveSlateLight, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }

                        // --- GAME WON / MISSION COMPLETED OVERLAY ---
                        if (viewModel.isGameWon) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ImmersiveBgDark.copy(alpha = 0.94f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "UPLINK EXTRACTED",
                                        color = ImmersiveGreen,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 4.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "COGNITIVE RUN COMPLETED successfully.\nSECURE UPLINK DECRYPTED: +200C BONUS",
                                        color = ImmersiveSlateLight,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = {
                                            viewModel.resetGameEntities()
                                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(220.dp).height(48.dp).testTag("victory_next_run_button")
                                    ) {
                                        Text("INITIATE NEXT RUN", color = Color.Black, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.changeScreen(GameViewModel.Screen.MENU) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                        border = BorderStroke(1.dp, ImmersiveSlateMuted),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.width(220.dp).height(44.dp).testTag("victory_menu_button")
                                    ) {
                                        Text("RETURN TO MAIN HUB", color = ImmersiveSlateLight, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }

                GameViewModel.Screen.SKILL_TREE -> {
                    SkillTreeScreen(
                        viewModel = viewModel,
                        onBackToGame = {
                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                        }
                    )
                }

                GameViewModel.Screen.LOADOUT -> {
                    LoadoutScreen(
                        viewModel = viewModel,
                        onBackToGame = {
                            viewModel.changeScreen(GameViewModel.Screen.PLAY)
                        }
                    )
                }

                GameViewModel.Screen.CONTROLS -> {
                    ControlsScreen(
                        onBackToMenu = {
                            viewModel.changeScreen(GameViewModel.Screen.MENU)
                        }
                    )
                }
            }
        }
    }
}

// Visual directives matrix control overlay page
@Composable
fun ControlsScreen(onBackToMenu: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .size(40.dp)
                    .background(ImmersiveBgHeader, RoundedCornerShape(8.dp))
                    .border(1.dp, ImmersiveLavender, RoundedCornerShape(8.dp))
                    .testTag("controls_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ImmersiveLavender)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = "OPERATIVE DIRECTIVES MATRIX",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DirectiveCard(
                title = "1. MOVEMENT & INTERACTION",
                description = "Drag or swipe anywhere on the custom isometric map to slide your operative across grid lanes. Tap ACTION on stairs or ladders (orange shafts) to climb up/down Z-levels.",
                color = ImmersiveLavender
            )

            DirectiveCard(
                title = "2. TACTICAL COMMANDOS STEALTH",
                description = "Toggle SNEAK to silence your movement. Running generates sound ripple waves (teal rings) that attract patrolling sentries. Sneaking inside enemy vision cones (translucent arcs) fills their Alert Meter. Slipping behind an enemy while sneaking and clicking STRIKE triggers a critical 5x Damage backstab!",
                color = ImmersiveBlue
            )

            DirectiveCard(
                title = "3. ENVIRONMENTAL INTERACTIONS",
                description = "Laser lattices block major pathways on Z1, Z2, Z3. Locate connected hacking terminals (turquoise consoles), walk next to them and click ACTION to deactivate the laser fence. Click strike on Red Plasma canisters to detonate high explosive yields, instantly clearing adjacent walls and targets.",
                color = ImmersiveAmber
            )

            DirectiveCard(
                title = "4. MULTI-CLASS MATRIX UPGRADES",
                description = "Collect XP from targets and terminate data gates to level up. You earn Skill Points (SP) that you can freely distribute across Cyber-Ronin (melee speed), Tech-Necromancer (hacking range/EMP), and Ghost-Infiltrator (active cloaks) matrices, creating hybrid combat archetypes.",
                color = ImmersiveDeepViolet
            )
        }
    }
}

@Composable
private fun DirectiveCard(title: String, description: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.2.dp, color),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                color = color,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = description,
                color = ImmersiveSlateLight,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
        }
    }
}
