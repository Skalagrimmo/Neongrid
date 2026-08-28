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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun CyberwareScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val player = viewModel.player
    val cyberwareList = viewModel.cyberwareList
    var selectedSlot by remember { mutableStateOf<CyberwareSlot?>(null) }

    val filteredList = remember(selectedSlot, cyberwareList) {
        if (selectedSlot == null) cyberwareList else cyberwareList.filter { it.slot == selectedSlot }
    }

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
                        .testTag("cyberware_back_button")
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
                        text = "NEURAL CYBERWARE LAB",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "BIOMECHANICAL AUGMENTATION & OVERCLOCK MATRIX",
                        color = ImmersiveSlateMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Operative Credits & Overclock Status
            Surface(
                color = ImmersiveBgHeader,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, ImmersiveAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = "Credits", tint = ImmersiveAmber, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${player.credits}C",
                        color = ImmersiveAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // STATS OVERVIEW RIBBON
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
            border = BorderStroke(1.dp, Color(0x22FFFFFF)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatPill(title = "HP BOOST", value = "+${viewModel.getCyberwareHealthBonus().toInt()}", color = ImmersiveGreen)
                StatPill(title = "ENERGY", value = "+${viewModel.getCyberwareEnergyBonus().toInt()}", color = CyberNeonCyan)
                StatPill(title = "DMG BOOST", value = "+${viewModel.getCyberwareDamageBonus().toInt()}", color = ImmersiveRed)
                StatPill(title = "ARMOR", value = "+${viewModel.getCyberwareArmorBonus().toInt()}%", color = ImmersiveBlue)
                StatPill(title = "STEALTH", value = "+${viewModel.getCyberwareStealthBonus().toInt()}%", color = ImmersiveLavender)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SLOT FILTER CHIPS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSlot == null,
                onClick = { selectedSlot = null },
                label = { Text("ALL SLOTS", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ImmersiveLavender,
                    selectedLabelColor = Color.Black
                )
            )

            CyberwareSlot.values().forEach { slot ->
                FilterChip(
                    selected = selectedSlot == slot,
                    onClick = { selectedSlot = slot },
                    label = { Text(slot.displayName, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(slot.iconColor),
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CYBERWARE IMPLANT CARDS LIST
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredList) { implant ->
                val isInstalled = implant.isInstalled
                val canAfford = player.credits >= implant.costCredits

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isInstalled) ImmersiveDeepViolet.copy(alpha = 0.4f) else ImmersiveBgHeader
                    ),
                    border = BorderStroke(
                        if (isInstalled) 1.5.dp else 1.dp,
                        if (isInstalled) ImmersiveGreen else Color(implant.slot.iconColor).copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("cyberware_card_${implant.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(implant.slot.iconColor), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = implant.name,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Surface(
                                color = Color(implant.slot.iconColor).copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.8.dp, Color(implant.slot.iconColor))
                            ) {
                                Text(
                                    text = implant.slot.displayName,
                                    color = Color(implant.slot.iconColor),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = implant.description,
                            color = ImmersiveSlateLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif
                        )

                        if (implant.specialEffectDescription.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = ImmersiveAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = implant.specialEffectDescription,
                                    color = ImmersiveAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stat tags
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (implant.statBoostDamage > 0f) Text("+${implant.statBoostDamage.toInt()} DMG", color = ImmersiveRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                if (implant.statBoostHealth > 0f) Text("+${implant.statBoostHealth.toInt()} HP", color = ImmersiveGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                if (implant.statBoostEnergy > 0f) Text("+${implant.statBoostEnergy.toInt()} EP", color = CyberNeonCyan, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                if (implant.statBoostSpeed > 0f) Text("+${(implant.statBoostSpeed * 100).toInt()}% SPD", color = ImmersiveAmber, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                if (implant.statBoostStealth > 0f) Text("+${implant.statBoostStealth.toInt()}% STL", color = ImmersiveLavender, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }

                            // Install / Uninstall Action Button
                            if (isInstalled) {
                                Button(
                                    onClick = {
                                        viewModel.uninstallCyberware(implant.id)
                                        AudioManager.playInteract()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgDark),
                                    border = BorderStroke(1.dp, ImmersiveRed),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text("REMOVE", color = ImmersiveRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        viewModel.installCyberware(implant.id)
                                        AudioManager.playLevelUp()
                                    },
                                    enabled = canAfford,
                                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveLavender),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(
                                        text = "INSTALL (${implant.costCredits}C)",
                                        color = if (canAfford) Color.Black else Color.Gray,
                                        fontSize = 10.sp,
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
    }
}

@Composable
private fun StatPill(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = title, color = ImmersiveSlateMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
