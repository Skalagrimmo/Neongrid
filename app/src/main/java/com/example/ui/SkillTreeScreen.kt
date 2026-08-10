package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CharacterClass
import com.example.model.SkillNode
import com.example.ui.theme.*

/**
 * Filter mode for displaying all classes or a specific archetype tree.
 */
enum class SkillClassFilter(val label: String, val classType: CharacterClass?) {
    ALL("ALL MATRIX TREES", null),
    RONIN("CYBER-RONIN", CharacterClass.CYBER_RONIN),
    TECH("TECH-NECROMANCER", CharacterClass.TECH_NECROMANCER),
    GHOST("GHOST-INFILTRATOR", CharacterClass.GHOST_INFILTRATOR)
}

/**
 * Interactive Multi-Class Skill Tree Screen built with Jetpack Compose.
 * Displays tiered skill trees for Cyber-Ronin, Tech-Necromancer, and Ghost-Infiltrator classes.
 * Players spend skill points (SP) to unlock nodes, satisfying prerequisite chains and boosting stats.
 */
@Composable
fun SkillTreeScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val player = viewModel.player
    val skillTree = viewModel.skillNodes
    var activeFilter by remember { mutableStateOf(SkillClassFilter.ALL) }
    var selectedNode by remember { mutableStateOf<SkillNode?>(skillTree.firstOrNull()) }

    // Synchronize selected node when skill tree updates
    LaunchedEffect(skillTree) {
        selectedNode?.let { current ->
            selectedNode = skillTree.find { it.id == current.id } ?: skillTree.firstOrNull()
        }
    }

    // Filter nodes based on active archetype tab
    val displayedTree = remember(skillTree, activeFilter) {
        if (activeFilter.classType == null) {
            skillTree
        } else {
            skillTree.filter { it.characterClass == activeFilter.classType }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(14.dp)
            .testTag("skill_tree_screen_root")
    ) {
        // =========================================================================
        // 1. TOP CONTROL & SP SCOREBOARD HEADER
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToGame,
                    modifier = Modifier
                        .size(42.dp)
                        .background(ImmersiveBgHeader, RoundedCornerShape(10.dp))
                        .border(1.dp, CyberNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .testTag("skill_back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyberNeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "COGNITIVE SKILL MATRIX",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Multi-Class Directive Neural Upgrades",
                        color = CyberTextMedium,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Available Skill Points & Quick Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // SP Counter Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ImmersiveBgHeader,
                    border = BorderStroke(1.2.dp, CyberNeonCyan),
                    modifier = Modifier.testTag("skill_sp_counter_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Skill Points",
                            tint = CyberNeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${player.skillPoints} SP",
                            color = CyberNeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Grant +1 SP Button (For quick testing/demo)
                Button(
                    onClick = { viewModel.grantSkillPoint(1) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberNeonCyan.copy(alpha = 0.2f), contentColor = CyberNeonCyan),
                    border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("grant_sp_button")
                ) {
                    Text("+1 SP", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                // Respec / Reset SP Button
                OutlinedButton(
                    onClick = { viewModel.respecSkills() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberNeonRed),
                    border = BorderStroke(1.dp, CyberNeonRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("respec_sp_button")
                ) {
                    Text("RESPEC", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // =========================================================================
        // 2. MULTI-CLASS ARCHETYPE FILTER TABS
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkillClassFilter.values().forEach { filter ->
                val isSelected = activeFilter == filter
                val activeColor = when (filter.classType) {
                    CharacterClass.CYBER_RONIN -> ImmersiveLavender
                    CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                    CharacterClass.GHOST_INFILTRATOR -> ImmersiveDeepViolet
                    null -> CyberNeonCyan
                }

                Surface(
                    onClick = { activeFilter = filter },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) activeColor.copy(alpha = 0.22f) else ImmersiveBgHeader.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) activeColor else Color.White.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(
                            when (filter) {
                                SkillClassFilter.ALL -> "class_tab_all"
                                SkillClassFilter.RONIN -> "class_tab_cyber_ronin"
                                SkillClassFilter.TECH -> "class_tab_tech_necromancer"
                                SkillClassFilter.GHOST -> "class_tab_ghost_infiltrator"
                            }
                        )
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            color = if (isSelected) activeColor else CyberTextMedium,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // =========================================================================
        // 3. MAIN CONTENT SPLIT: LEFT GRAPH / RIGHT DETAIL INSPECTOR
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // LEFT COLUMN: Scrollable Skill Trees (Ronin, Tech, Ghost)
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeFilter.classType == null || activeFilter.classType == CharacterClass.CYBER_RONIN) {
                    ArchetypeClassSection(
                        title = "CYBER-RONIN MATRIX",
                        subtitle = "Melee Lethality, Blade Arts & Speed Boosts",
                        accentColor = ImmersiveLavender,
                        nodes = skillTree.filter { it.characterClass == CharacterClass.CYBER_RONIN },
                        unlockedSkills = player.unlockedSkills,
                        selectedNode = selectedNode,
                        onSelectNode = { selectedNode = it }
                    )
                }

                if (activeFilter.classType == null || activeFilter.classType == CharacterClass.TECH_NECROMANCER) {
                    ArchetypeClassSection(
                        title = "TECH-NECROMANCER MATRIX",
                        subtitle = "Terminal Hacking, Sentry Drones & EMP Overloads",
                        accentColor = ImmersiveAmber,
                        nodes = skillTree.filter { it.characterClass == CharacterClass.TECH_NECROMANCER },
                        unlockedSkills = player.unlockedSkills,
                        selectedNode = selectedNode,
                        onSelectNode = { selectedNode = it }
                    )
                }

                if (activeFilter.classType == null || activeFilter.classType == CharacterClass.GHOST_INFILTRATOR) {
                    ArchetypeClassSection(
                        title = "GHOST-INFILTRATOR MATRIX",
                        subtitle = "Stealth Dampening, Cloak Modules & Backstabs",
                        accentColor = ImmersiveDeepViolet,
                        nodes = skillTree.filter { it.characterClass == CharacterClass.GHOST_INFILTRATOR },
                        unlockedSkills = player.unlockedSkills,
                        selectedNode = selectedNode,
                        onSelectNode = { selectedNode = it }
                    )
                }
            }

            // RIGHT COLUMN: Selected Node Detailed Inspector & Unlock Action Card
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.95f)),
                border = BorderStroke(1.2.dp, CyberNeonCyan.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
                    .testTag("selected_skill_inspector_panel")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    selectedNode?.let { node ->
                        val isUnlocked = player.unlockedSkills.contains(node.id)
                        val parentsUnlocked = node.parents.all { p -> player.unlockedSkills.contains(p) }
                        val canAfford = player.skillPoints >= node.costPoints

                        val classColor = when (node.characterClass) {
                            CharacterClass.CYBER_RONIN -> ImmersiveLavender
                            CharacterClass.TECH_NECROMANCER -> ImmersiveAmber
                            CharacterClass.GHOST_INFILTRATOR -> ImmersiveDeepViolet
                        }

                        Column {
                            // Header badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = classColor.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, classColor)
                                ) {
                                    Text(
                                        text = node.characterClass.name.replace("_", " "),
                                        color = classColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ImmersiveBgDark,
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "${node.costPoints} SP COST",
                                        color = CyberNeonCyan,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Skill Node Title
                            Text(
                                text = node.name.uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Skill Description
                            Text(
                                text = node.description,
                                color = CyberTextMedium,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                fontFamily = FontFamily.SansSerif
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Prerequisites Status List
                            Text(
                                text = "PREREQUISITE DIRECTIVES:",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (node.parents.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ImmersiveGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ImmersiveGreen)
                                ) {
                                    Text(
                                        text = "NONE (CORE DIRECTIVE)",
                                        color = ImmersiveGreen,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    node.parents.forEach { parentId ->
                                        val parentNode = skillTree.find { it.id == parentId }
                                        val isParentUnlocked = player.unlockedSkills.contains(parentId)

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isParentUnlocked) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (isParentUnlocked) ImmersiveGreen else CyberNeonRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = parentNode?.name ?: parentId,
                                                color = if (isParentUnlocked) ImmersiveGreen else CyberNeonRed,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Action Button
                        Button(
                            onClick = {
                                viewModel.buySkill(node.id)
                                AudioManager.playLevelUp()
                            },
                            enabled = !isUnlocked && parentsUnlocked && canAfford,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = classColor,
                                disabledContainerColor = ImmersiveBgDark
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("skill_unlock_action_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (isUnlocked) {
                                    Icon(Icons.Default.Check, contentDescription = "Active", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Text("DIRECTIVE UNLOCKED", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                } else if (!parentsUnlocked) {
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = CyberTextMedium, modifier = Modifier.size(14.dp))
                                    Text("LOCKED - PREREQ MISSING", color = CyberTextMedium, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                } else if (!canAfford) {
                                    Text("INSUFFICIENT SP (${node.costPoints} SP)", color = CyberTextMedium, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                } else {
                                    Icon(Icons.Default.Bolt, contentDescription = "Unlock", tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Text("UNLOCK (+${node.costPoints} SP)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SELECT DIRECTIVE NODE TO INSPECT",
                            color = CyberTextMedium,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchetypeClassSection(
    title: String,
    subtitle: String,
    accentColor: Color,
    nodes: List<SkillNode>,
    unlockedSkills: Set<String>,
    selectedNode: SkillNode?,
    onSelectNode: (SkillNode) -> Unit
) {
    val unlockedCount = nodes.count { unlockedSkills.contains(it.id) }

    Card(
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.85f)),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 18.dp)
                            .background(accentColor, RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = title,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = subtitle,
                            color = CyberTextMedium,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }

                // Progress Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "$unlockedCount / ${nodes.size} ACTIVE",
                        color = accentColor,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid Layout of Nodes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                nodes.forEach { node ->
                    val isUnlocked = unlockedSkills.contains(node.id)
                    val isSelected = selectedNode?.id == node.id
                    val parentsUnlocked = node.parents.all { unlockedSkills.contains(it) }

                    val nodeColor by animateColorAsState(
                        targetValue = when {
                            isUnlocked -> accentColor
                            parentsUnlocked -> CyberNeonCyan
                            else -> Color.White.copy(alpha = 0.2f)
                        },
                        animationSpec = tween(300),
                        label = "nodeColor"
                    )

                    val icon: ImageVector = when (node.id) {
                        "ronin_base" -> Icons.Default.GpsFixed
                        "ronin_speed" -> Icons.Default.DirectionsRun
                        "ronin_crit" -> Icons.Default.Bolt
                        "ronin_ultimate" -> Icons.Default.AutoAwesome
                        "tech_base" -> Icons.Default.Terminal
                        "tech_shrapnel" -> Icons.Default.LocalFireDepartment
                        "tech_drone" -> Icons.Default.Air
                        "tech_ultimate" -> Icons.Default.FlashOn
                        "ghost_base" -> Icons.Default.VisibilityOff
                        "ghost_smoke" -> Icons.Default.Cloud
                        "ghost_backstab" -> Icons.Default.Security
                        else -> Icons.Default.Star
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.2f) else ImmersiveBgDark)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color.White else nodeColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                onSelectNode(node)
                                AudioManager.playInteract()
                            }
                            .padding(6.dp)
                            .testTag("skill_node_item_${node.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = node.name,
                                tint = nodeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = node.name,
                                color = if (isUnlocked) Color.White else CyberTextMedium,
                                fontSize = 9.sp,
                                fontWeight = if (isUnlocked || isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Top right status indicator dot
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) ImmersiveGreen
                                    else if (parentsUnlocked) CyberNeonCyan
                                    else CyberNeonRed.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}
