package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun MissionDispatchScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMission = viewModel.activeMission
    val missions = viewModel.availableMissions
    var selectedMission by remember { mutableStateOf(activeMission ?: missions.first()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(16.dp)
    ) {
        // TOP HEADER BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        AudioManager.playInteract()
                        onBackToGame()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(ImmersiveBgHeader, RoundedCornerShape(8.dp))
                        .border(1.dp, ImmersiveLavender, RoundedCornerShape(8.dp))
                        .testTag("mission_dispatch_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ImmersiveLavender
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TACTICAL OPERATIONS DISPATCH",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "SECTOR GIBSON // GRID INFILTRATION CONTRACTS",
                        color = ImmersiveSlateMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Operative Credits & Rank Badge
            Surface(
                color = ImmersiveBgHeader,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Credits", tint = ImmersiveAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${viewModel.player.credits}C",
                        color = ImmersiveAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // CONTENT: TWO-COLUMN / SCROLLABLE LAYOUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // LEFT COLUMN: CONTRACT LIST (45% width)
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "AVAILABLE CONTRACTS",
                    color = ImmersiveLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(missions) { mission ->
                        val isSelected = selectedMission.id == mission.id
                        val isActive = activeMission?.id == mission.id

                        Card(
                            onClick = {
                                SoundManager.playMenuClick()
                                selectedMission = mission
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ImmersiveDeepViolet.copy(alpha = 0.5f) else ImmersiveBgHeader
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (isActive) ImmersiveGreen else if (isSelected) ImmersiveLavender else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mission_card_${mission.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mission.codename,
                                        color = Color(mission.type.badgeColor),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    if (isActive) {
                                        Surface(
                                            color = ImmersiveGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(0.8.dp, ImmersiveGreen)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                color = ImmersiveGreen,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = mission.title,
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "TARGET: Z=${mission.targetZLevel}",
                                        color = ImmersiveSlateMuted,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "+${mission.rewardCredits}C | +${mission.rewardXp}XP",
                                        color = ImmersiveAmber,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: CONTRACT DOSSIER & BRIEFING (55% width)
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.2.dp, ImmersiveLavender.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header Dossier Tag
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OPERATION DOSSIER",
                                color = ImmersiveLavender,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp
                            )
                            Surface(
                                color = Color(selectedMission.type.badgeColor).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, Color(selectedMission.type.badgeColor))
                            ) {
                                Text(
                                    text = selectedMission.type.displayName,
                                    color = Color(selectedMission.type.badgeColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = selectedMission.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = selectedMission.description,
                            color = ImmersiveSlateLight,
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.SansSerif
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(14.dp))

                        // OBJECTIVES MATRIX
                        Text(
                            text = "PRIMARY OBJECTIVES",
                            color = ImmersiveLavender,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ObjectiveRow(
                            icon = Icons.Default.Terminal,
                            text = "Breach Terminals: ${selectedMission.currentTerminalsHacked}/${selectedMission.targetTerminals}",
                            color = CyberNeonCyan
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ObjectiveRow(
                            icon = Icons.Default.Shield,
                            text = "Neutralize Sentries: ${selectedMission.currentEnemiesEliminated}/${selectedMission.targetEnemies}",
                            color = ImmersiveRed
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        ObjectiveRow(
                            icon = Icons.Default.LocationOn,
                            text = "Extraction Conduits: Sector Z=${selectedMission.targetZLevel}",
                            color = ImmersiveGreen
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(14.dp))

                        // HAZARD PROTOCOL
                        Text(
                            text = "GRID HAZARDS & ANOMALIES",
                            color = ImmersiveAmber,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = Color(selectedMission.hazard.iconColor).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(selectedMission.hazard.iconColor))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = selectedMission.hazard.title,
                                    color = Color(selectedMission.hazard.iconColor),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = selectedMission.hazard.description,
                                    color = ImmersiveSlateLight,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }

                    // BOTTOM ACTION: ENGAGE CONTRACT BUTTON
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        val isCurrentActive = activeMission?.id == selectedMission.id

                        Button(
                            onClick = {
                                viewModel.selectMission(selectedMission)
                                AudioManager.playLevelUp()
                                onBackToGame()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrentActive) ImmersiveGreen else ImmersiveLavender
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("deploy_operation_button")
                        ) {
                            Icon(
                                if (isCurrentActive) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                                contentDescription = "Deploy",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isCurrentActive) "OPERATION IN PROGRESS (DEPLOY)" else "ACCEPT CONTRACT & DEPLOY",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ObjectiveRow(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Text(
            text = text,
            color = ImmersiveSlateLight,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
