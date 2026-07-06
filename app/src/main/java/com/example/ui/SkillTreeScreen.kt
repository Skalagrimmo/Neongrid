package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun SkillTreeScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val player = viewModel.player
    val skillTree = viewModel.skillNodes
    var selectedNode by remember { mutableStateOf<SkillNode?>(skillTree.firstOrNull()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToGame,
                modifier = Modifier
                    .size(40.dp)
                    .background(ImmersiveBgHeader, RoundedCornerShape(8.dp))
                    .border(1.dp, ImmersiveLavender, RoundedCornerShape(8.dp))
                    .testTag("skill_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ImmersiveLavender)
            }
            Text(
                text = "COGNITIVE UPGRADES",
                color = ImmersiveSlateLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            // Available Core Skill Points
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.dp, ImmersiveDeepViolet),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${player.skillPoints} SP Available",
                    color = ImmersiveLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Left-Right splits: Left list of classes & skills, Right node detail panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left list (Scrollable matrix trees)
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // CYBER-RONIN
                ClassSectionHeader("CYBER-RONIN MATRIX", ImmersiveLavender)
                ClassSkillGrid(
                    skillTree.filter { it.characterClass == CharacterClass.CYBER_RONIN },
                    player.unlockedSkills,
                    selectedNode,
                    onSelect = { selectedNode = it }
                )

                // TECH-NECROMANCER
                ClassSectionHeader("TECH-NECROMANCER MATRIX", ImmersiveAmber)
                ClassSkillGrid(
                    skillTree.filter { it.characterClass == CharacterClass.TECH_NECROMANCER },
                    player.unlockedSkills,
                    selectedNode,
                    onSelect = { selectedNode = it }
                )

                // GHOST-INFILTRATOR
                ClassSectionHeader("GHOST-INFILTRATOR MATRIX", ImmersiveDeepViolet)
                ClassSkillGrid(
                    skillTree.filter { it.characterClass == CharacterClass.GHOST_INFILTRATOR },
                    player.unlockedSkills,
                    selectedNode,
                    onSelect = { selectedNode = it }
                )
            }

            // Right detail inspector card
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .background(ImmersiveBgHeader, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                selectedNode?.let { node ->
                    Column {
                        val classColor = when (node.characterClass) {
                            CharacterClass.CYBER_RONIN -> ImmersiveLavender
                            CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                            CharacterClass.GHOST_INFILTRATOR -> ImmersiveDeepViolet
                        }

                        Text(
                            text = node.name.uppercase(),
                            color = ImmersiveSlateLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = node.characterClass.name.replace("_", " "),
                            color = classColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Divider(color = Color(0x0DFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = node.description,
                            color = ImmersiveSlateMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))
                        if (node.parents.isNotEmpty()) {
                            Text(
                                text = "PRE-REQUISITES:\n${node.parents.joinToString { it.replace("_", " ").uppercase() }}",
                                color = ImmersiveSlateMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Bottom Action button
                    val isUnlocked = player.unlockedSkills.contains(node.id)
                    val parentsUnlocked = node.parents.all { p -> player.unlockedSkills.contains(p) }
                    val canAfford = player.skillPoints >= node.costPoints

                    Button(
                        onClick = { viewModel.buySkill(node.id) },
                        enabled = !isUnlocked && (parentsUnlocked || node.parents.isEmpty()) && canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (node.characterClass) {
                                CharacterClass.CYBER_RONIN -> ImmersiveLavender
                                CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                                else -> ImmersiveDeepViolet
                            },
                            disabledContainerColor = Color(0xFF16161B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("skill_unlock_action_button")
                    ) {
                        if (isUnlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("DIRECTIVE UNLOCKED", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else if (!parentsUnlocked && node.parents.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = ImmersiveSlateMuted, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("MATRIX LOCKED", color = ImmersiveSlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else if (!canAfford) {
                            Text("INSUFFICIENT SP", color = ImmersiveSlateMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        } else {
                            Text("UNLOCK (+${node.costPoints} SP)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELECT DIRECTIVE NODE", color = ImmersiveSlateMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun ClassSectionHeader(text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 6.dp, height = 16.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = ImmersiveSlateLight,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun ClassSkillGrid(
    nodes: List<SkillNode>,
    unlockedList: Set<String>,
    selected: SkillNode?,
    onSelect: (SkillNode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        nodes.forEach { node ->
            val isUnlocked = unlockedList.contains(node.id)
            val isSelected = selected?.id == node.id

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .background(
                        if (isSelected) ImmersiveBgHeader else ImmersiveBgDark,
                        RoundedCornerShape(8.dp)
                    )
                    .border(
                        1.5.dp,
                        when {
                            isSelected -> ImmersiveLavender
                            isUnlocked -> when (node.characterClass) {
                                CharacterClass.CYBER_RONIN -> ImmersiveLavender
                                CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                                CharacterClass.GHOST_INFILTRATOR -> ImmersiveDeepViolet
                            }
                            else -> Color(0x1AFFFFFF)
                        },
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(node) }
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (isUnlocked) Icons.Default.Star else Icons.Default.Lock,
                        contentDescription = node.name,
                        tint = if (isUnlocked) {
                            when (node.characterClass) {
                                CharacterClass.CYBER_RONIN -> ImmersiveLavender
                                CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                                else -> ImmersiveDeepViolet
                            }
                        } else ImmersiveSlateMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = node.name.split(" ").firstOrNull() ?: "",
                        color = if (isUnlocked) ImmersiveSlateLight else ImmersiveSlateMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
