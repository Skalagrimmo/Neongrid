package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PlayerStatsEntity
import com.example.ui.theme.*

/**
 * Stateful Jetpack Compose component that connects to [SkillTreeViewModel]
 * to visualize and unlock skills from the database using a tiered graph structure.
 */
@Composable
fun SkillTreeGraphComponent(
    viewModel: SkillTreeViewModel,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val characterStats by viewModel.characterStats.collectAsState()
    val unlockedSkillIds by viewModel.unlockedSkillIds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    SkillTreeGraphContent(
        allSkills = viewModel.allSkills,
        unlockedSkillIds = unlockedSkillIds,
        characterStats = characterStats,
        selectedSkillId = uiState.selectedSkillId,
        message = uiState.message,
        isError = uiState.isError,
        onSelectSkill = { viewModel.selectSkill(it) },
        onUnlockSkill = { viewModel.unlockSkill(it) },
        onResetSkills = { viewModel.resetSkills() },
        modifier = modifier,
        onBackClick = onBackClick
    )
}

/**
 * Stateless UI component rendering a tiered skill tree graph with node progression,
 * prerequisite connections, and interactive unlock controls.
 */
@Composable
fun SkillTreeGraphContent(
    allSkills: List<SkillNodeData>,
    unlockedSkillIds: Set<String>,
    characterStats: PlayerStatsEntity?,
    selectedSkillId: String?,
    message: String?,
    isError: Boolean,
    onSelectSkill: (String) -> Unit,
    onUnlockSkill: (String) -> Unit,
    onResetSkills: () -> Unit,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val selectedSkill = remember(selectedSkillId, allSkills) {
        allSkills.find { it.id == selectedSkillId } ?: allSkills.firstOrNull()
    }

    val availableSkillPoints = characterStats?.skillPoints ?: 0

    // Group skills by Tier (1, 2, 3)
    val skillsByTier = remember(allSkills) {
        allSkills.groupBy { it.tier }.toSortedMap()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(16.dp)
            .testTag("skill_tree_graph_root")
    ) {
        // --- Top Navigation Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(ImmersiveBgHeader, RoundedCornerShape(12.dp))
                            .border(1.dp, ImmersiveLavender.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("skill_tree_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ImmersiveLavender
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = "NEURAL SKILL GRAPH",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Tiered Cognitive Augmentations",
                        fontSize = 11.sp,
                        color = ImmersiveSlateMuted
                    )
                }
            }

            // Skill Points Counter Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ImmersiveBgHeader,
                border = BorderStroke(1.dp, ImmersiveCyan.copy(alpha = 0.6f)),
                modifier = Modifier.testTag("skill_points_counter")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = "Skill Points",
                        tint = ImmersiveCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$availableSkillPoints SP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveCyan
                    )
                }
            }
        }

        // --- Status Feedback Banner ---
        AnimatedVisibility(
            visible = !message.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isError) ImmersiveRed.copy(alpha = 0.2f) else ImmersiveGreen.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isError) ImmersiveRed else ImmersiveGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .testTag("skill_tree_feedback_message")
            ) {
                Text(
                    text = message ?: "",
                    fontSize = 12.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // --- Tiered Graph Canvas & Node Layout ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Tier Columns
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    skillsByTier.forEach { (tier, skillsInTier) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Tier Header
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = ImmersiveBgDark,
                                border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.3f)),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Text(
                                    text = "TIER $tier",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ImmersiveLavender,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Skills in this Tier
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceAround,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                skillsInTier.forEach { skill ->
                                    val isUnlocked = unlockedSkillIds.contains(skill.id)
                                    val isSelected = skill.id == selectedSkillId
                                    val arePrereqsMet = skill.prerequisites.all { unlockedSkillIds.contains(it) }

                                    SkillGraphNodeItem(
                                        skill = skill,
                                        isUnlocked = isUnlocked,
                                        isSelected = isSelected,
                                        arePrereqsMet = arePrereqsMet,
                                        onSelect = { onSelectSkill(skill.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Selected Skill Detail Panel ---
        if (selectedSkill != null) {
            val isUnlocked = unlockedSkillIds.contains(selectedSkill.id)
            val arePrereqsMet = selectedSkill.prerequisites.all { unlockedSkillIds.contains(it) }
            val hasEnoughSp = availableSkillPoints >= selectedSkill.cost

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("selected_skill_detail_panel"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(
                    1.dp,
                    if (isUnlocked) ImmersiveGreen.copy(alpha = 0.5f)
                    else if (arePrereqsMet) ImmersiveCyan.copy(alpha = 0.5f)
                    else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = selectedSkill.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = ImmersiveBgDark,
                                    border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "Tier ${selectedSkill.tier}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ImmersiveLavender,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Class: ${selectedSkill.characterClass}",
                                fontSize = 11.sp,
                                color = ImmersiveSlateMuted
                            )
                        }

                        // Unlock / Status Action Button
                        Button(
                            onClick = { onUnlockSkill(selectedSkill.id) },
                            enabled = !isUnlocked && arePrereqsMet && hasEnoughSp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUnlocked) ImmersiveGreen.copy(alpha = 0.3f) else ImmersiveCyan,
                                disabledContainerColor = ImmersiveBgDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("unlock_skill_button")
                        ) {
                            Text(
                                text = when {
                                    isUnlocked -> "UNLOCKED"
                                    !arePrereqsMet -> "LOCKED"
                                    !hasEnoughSp -> "NEED ${selectedSkill.cost} SP"
                                    else -> "UNLOCK (${selectedSkill.cost} SP)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isUnlocked) ImmersiveGreen else Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = selectedSkill.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 16.sp
                    )

                    if (selectedSkill.prerequisites.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "PREREQUISITES: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveSlateMuted,
                                fontFamily = FontFamily.Monospace
                            )
                            selectedSkill.prerequisites.forEach { reqId ->
                                val reqSkill = allSkills.find { it.id == reqId }
                                val isReqMet = unlockedSkillIds.contains(reqId)
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isReqMet) ImmersiveGreen.copy(alpha = 0.15f) else ImmersiveRed.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (isReqMet) ImmersiveGreen else ImmersiveRed),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = reqSkill?.name ?: reqId,
                                        fontSize = 10.sp,
                                        color = if (isReqMet) ImmersiveGreen else ImmersiveRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
private fun SkillGraphNodeItem(
    skill: SkillNodeData,
    isUnlocked: Boolean,
    isSelected: Boolean,
    arePrereqsMet: Boolean,
    onSelect: () -> Unit
) {
    val nodeColor by animateColorAsState(
        targetValue = when {
            isUnlocked -> ImmersiveGreen
            arePrereqsMet -> ImmersiveCyan
            else -> ImmersiveSlateMuted
        },
        animationSpec = tween(300),
        label = "nodeColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .testTag("skill_node_${skill.id}")
            .clickable { onSelect() }
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) nodeColor.copy(alpha = 0.25f) else ImmersiveBgDark
                )
                .border(
                    width = if (isSelected) 2.5.dp else 1.5.dp,
                    color = if (isSelected) Color.White else nodeColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (skill.iconName) {
                    "Shield" -> Icons.Default.Shield
                    "Bolt" -> Icons.Default.Bolt
                    "FlashOn" -> Icons.Default.FlashOn
                    "Security" -> Icons.Default.Security
                    "Psychology" -> Icons.Default.Psychology
                    "VisibilityOff" -> Icons.Default.VisibilityOff
                    "AutoAwesome" -> Icons.Default.AutoAwesome
                    else -> Icons.Default.Visibility
                },
                contentDescription = skill.name,
                tint = nodeColor,
                modifier = Modifier.size(24.dp)
            )

            // Status Badge Icon
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) ImmersiveGreen else if (arePrereqsMet) ImmersiveCyan else ImmersiveBgDark)
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Default.Check else if (arePrereqsMet) Icons.Default.Star else Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp).align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = skill.name,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color.White else ImmersiveSlateMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
