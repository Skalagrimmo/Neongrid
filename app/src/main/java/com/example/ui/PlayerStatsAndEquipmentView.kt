package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.engine.*
import com.example.model.EquipmentItem
import com.example.model.EquipmentType
import com.example.ui.theme.*

/**
 * Stateful Jetpack Compose component that observes player stats, inventory items,
 * equipped loadout, unlocked skills, and game save state from [DataRepository].
 */
@Composable
fun PlayerStatsAndEquipmentView(
    dataRepository: DataRepository,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    val characterStats by dataRepository.characterStats.collectAsState(initial = null)
    val inventoryItems by dataRepository.inventoryItems.collectAsState(initial = emptyList())
    val equippedItems by dataRepository.equippedItems.collectAsState(initial = emptyList())
    val unlockedSkills by dataRepository.unlockedSkills.collectAsState(initial = emptyList())
    val saveState by dataRepository.playerSaveState.collectAsState(initial = null)

    PlayerStatsAndEquipmentContent(
        characterStats = characterStats,
        inventoryItems = inventoryItems,
        equippedItems = equippedItems,
        unlockedSkills = unlockedSkills,
        saveState = saveState,
        modifier = modifier,
        onBackClick = onBackClick
    )
}

/**
 * Stateless Jetpack Compose UI component for rendering player character details,
 * stats overview, equipped items, and unlocked skill tree progress.
 */
@Composable
fun PlayerStatsAndEquipmentContent(
    characterStats: PlayerStatsEntity?,
    inventoryItems: List<InventoryItemEntity>,
    equippedItems: List<InventoryItemEntity>,
    unlockedSkills: List<UnlockedSkillEntity>,
    saveState: PlayerSaveState?,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Overview & Stats, 1: Equipment, 2: Skills

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(16.dp)
            .testTag("player_stats_and_equipment_root")
    ) {
        // --- Header Navigation & Title ---
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
                            .size(48.dp)
                            .background(ImmersiveBgHeader, RoundedCornerShape(12.dp))
                            .border(1.dp, ImmersiveLavender.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("stats_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                            tint = ImmersiveLavender
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Column {
                    Text(
                        text = "PLAYER PROFILE",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = saveState?.questTitle ?: "Active Agent Status",
                        fontSize = 12.sp,
                        color = ImmersiveSlateMuted
                    )
                }
            }

            // Quick Credits Badge
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ImmersiveBgHeader,
                border = BorderStroke(1.dp, ImmersiveAmber.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Credits",
                        tint = ImmersiveAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${characterStats?.credits ?: saveState?.credits ?: 0}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveAmber
                    )
                }
            }
        }

        // --- Character Summary Header Card ---
        CharacterHeaderCard(
            characterStats = characterStats,
            saveState = saveState
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Category Tabs ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ImmersiveBgHeader,
            contentColor = ImmersiveLavender,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tab_overview"),
                text = { Text("Overview", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Person, contentDescription = "Overview Tab") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tab_equipment"),
                text = { Text("Equipped (${equippedItems.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Shield, contentDescription = "Equipment Tab") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("tab_skills"),
                text = { Text("Skills (${unlockedSkills.size})", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Star, contentDescription = "Skills Tab") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Tab Content Area ---
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OverviewTabContent(
                    characterStats = characterStats,
                    equippedCount = equippedItems.size,
                    totalInventoryCount = inventoryItems.size,
                    unlockedSkillsCount = unlockedSkills.size
                )
                1 -> EquipmentTabContent(
                    equippedItems = equippedItems,
                    allInventoryItems = inventoryItems
                )
                2 -> SkillsTabContent(
                    unlockedSkills = unlockedSkills
                )
            }
        }
    }
}

@Composable
private fun CharacterHeaderCard(
    characterStats: PlayerStatsEntity?,
    saveState: PlayerSaveState?
) {
    val level = characterStats?.level ?: saveState?.level ?: 1
    val xp = characterStats?.xp ?: saveState?.xp ?: 0
    val health = characterStats?.health ?: 100f
    val maxHealth = characterStats?.maxHealth ?: 100f
    val healthRatio = (health / maxHealth.coerceAtLeast(1f)).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("character_header_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(ImmersiveDeepViolet, ImmersiveLavender)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "LVL $level",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Cyber operative",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "XP: $xp pts",
                            fontSize = 12.sp,
                            color = ImmersiveLavender
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ImmersiveCyan.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, ImmersiveCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "STATUS: ONLINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Health Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Health",
                        tint = ImmersiveRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HEALTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveSlateMuted
                    )
                }

                Text(
                    text = "${health.toInt()} / ${maxHealth.toInt()} HP",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { healthRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("health_progress_bar"),
                color = if (healthRatio < 0.3f) ImmersiveRed else ImmersiveGreen,
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun OverviewTabContent(
    characterStats: PlayerStatsEntity?,
    equippedCount: Int,
    totalInventoryCount: Int,
    unlockedSkillsCount: Int
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "CHARACTER STATS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveSlateMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Level",
                    value = "${characterStats?.level ?: 1}",
                    subtitle = "Combat Tier",
                    icon = Icons.Default.MilitaryTech,
                    accentColor = ImmersiveLavender,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Credits",
                    value = "${characterStats?.credits ?: 0}",
                    subtitle = "Currency",
                    icon = Icons.Default.MonetizationOn,
                    accentColor = ImmersiveAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Skill Points",
                    value = "${characterStats?.skillPoints ?: 0}",
                    subtitle = "Available",
                    icon = Icons.Default.Stars,
                    accentColor = ImmersiveCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "XP Progress",
                    value = "${characterStats?.xp ?: 0}",
                    subtitle = "Experience",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    accentColor = ImmersiveGreen,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "LOADOUT SUMMARY",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveSlateMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatMetricCard(
                    title = "Equipped Items",
                    value = "$equippedCount / 4",
                    subtitle = "Active Loadout",
                    icon = Icons.Default.Shield,
                    accentColor = ImmersiveBlue,
                    modifier = Modifier.weight(1f)
                )
                StatMetricCard(
                    title = "Total Inventory",
                    value = "$totalInventoryCount items",
                    subtitle = "Storage",
                    icon = Icons.Default.Inventory2,
                    accentColor = ImmersiveLavender,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            StatMetricCard(
                title = "Unlocked Skills",
                value = "$unlockedSkillsCount skills active",
                subtitle = "Skill Tree Augmentations",
                icon = Icons.Default.Psychology,
                accentColor = ImmersiveCyan,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun EquipmentTabContent(
    equippedItems: List<InventoryItemEntity>,
    allInventoryItems: List<InventoryItemEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("equipped_items_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Equipment System & Slot Restriction Enforcement Engine Section
        item {
            EquipmentSystemSection()
        }

        item {
            Text(
                text = "PERSISTED DATABASE SLOTS (${equippedItems.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveSlateMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        if (equippedItems.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ImmersiveBgHeader,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No equipment currently equipped.",
                        fontSize = 13.sp,
                        color = ImmersiveSlateMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(equippedItems, key = { it.itemId }) { item ->
                EquipmentItemCard(item = item, isEquipped = true)
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "INVENTORY ITEMS (${allInventoryItems.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveSlateMuted,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        items(allInventoryItems, key = { "all_${it.itemId}" }) { item ->
            EquipmentItemCard(item = item, isEquipped = item.isEquipped)
        }
    }
}

@Composable
private fun SkillsTabContent(
    unlockedSkills: List<UnlockedSkillEntity>
) {
    if (unlockedSkills.isEmpty()) {
        EmptyStateCard(
            title = "No Skills Unlocked",
            message = "Spend skill points in the Skill Tree to unlock new combat augmentations.",
            icon = Icons.Default.StarBorder
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("unlocked_skills_list"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "UNLOCKED AUGMENTATIONS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveSlateMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(unlockedSkills, key = { it.skillId }) { skill ->
                SkillItemCard(skill = skill)
            }
        }
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveSlateMuted,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = accentColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
private fun EquipmentItemCard(
    item: InventoryItemEntity,
    isEquipped: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(
            1.dp,
            if (isEquipped) ImmersiveGreen.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isEquipped) ImmersiveGreen.copy(alpha = 0.15f) else ImmersiveBgDark
                        )
                        .border(
                            1.dp,
                            if (isEquipped) ImmersiveGreen else ImmersiveSlateMuted.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (item.equipmentType.uppercase()) {
                            "WEAPON" -> Icons.Default.GpsFixed
                            "ARMOR" -> Icons.Default.Security
                            "CORE" -> Icons.Default.Bolt
                            else -> Icons.Default.Inventory2
                        },
                        contentDescription = item.equipmentType,
                        tint = if (isEquipped) ImmersiveGreen else ImmersiveLavender,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (item.quantity > 1) {
                            Text(
                                text = " x${item.quantity}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveAmber
                            )
                        }
                    }

                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = ImmersiveSlateMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isEquipped) ImmersiveGreen.copy(alpha = 0.2f) else ImmersiveBgDark,
                border = BorderStroke(
                    1.dp,
                    if (isEquipped) ImmersiveGreen else ImmersiveSlateMuted.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = if (isEquipped) "EQUIPPED" else item.equipmentType,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEquipped) ImmersiveGreen else ImmersiveSlateMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SkillItemCard(
    skill: UnlockedSkillEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.dp, ImmersiveCyan.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(ImmersiveCyan.copy(alpha = 0.15f))
                        .border(1.dp, ImmersiveCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Skill",
                        tint = ImmersiveCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = skill.skillName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Class: ${skill.characterClass}",
                        fontSize = 11.sp,
                        color = ImmersiveSlateMuted
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = ImmersiveCyan.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, ImmersiveCyan)
            ) {
                Text(
                    text = "UNLOCKED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ImmersiveCyan,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ImmersiveLavender,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 12.sp,
                color = ImmersiveSlateMuted
            )
        }
    }
}

@Composable
private fun EquipmentSystemSection() {
    val equipmentSystem = remember { EquipmentSystem() }
    val currentLoadout by equipmentSystem.loadoutState.collectAsState()
    val combinedStats by equipmentSystem.combinedStatsState.collectAsState()

    var selectedSlot by remember { mutableStateOf(EquipmentSlot.WEAPON) }
    var candidateItem by remember { mutableStateOf<EquipmentItem?>(EquipmentItem.ALL_ITEMS.firstOrNull { it.type == EquipmentType.WEAPON }) }
    var operationFeedback by remember { mutableStateOf<String?>(null) }

    val validationResult = remember(candidateItem, selectedSlot) {
        candidateItem?.let { equipmentSystem.canEquip(it, selectedSlot) } ?: EquipValidationResult.Allowed
    }

    val statDelta = remember(candidateItem, selectedSlot, currentLoadout) {
        candidateItem?.let { item ->
            equipmentSystem.previewStatChange(
                currentLoadout = currentLoadout,
                newItem = item,
                targetSlot = selectedSlot
            )
        } ?: StatDelta()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("equipment_system_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
        border = BorderStroke(1.5.dp, ImmersiveGreen.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Equipment System Matrix",
                        tint = ImmersiveGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "EQUIPMENT SYSTEM & SLOT RESTRICTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ImmersiveGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "SLOT ENFORCED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveGreen,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // 1. Combined Base Stats Overview Matrix
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ImmersiveBgDark,
                border = BorderStroke(1.dp, ImmersiveSlateMuted.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "COMBINED BASE + EQUIPMENT STATS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ImmersiveLavender
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip("HP", "${combinedStats.totalHealth.toInt()}", "+${combinedStats.totalBonusHealth.toInt()}", ImmersiveGreen)
                        StatChip("ENERGY", "${combinedStats.totalEnergy.toInt()}", "+${combinedStats.totalBonusEnergy.toInt()}", ImmersiveDeepViolet)
                        StatChip("DAMAGE", "${combinedStats.totalDamage.toInt()}", "+${combinedStats.totalBonusDamage.toInt()}", ImmersiveRed)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatChip("ARMOR", "${combinedStats.totalArmor.toInt()}", "+${combinedStats.totalBonusArmor.toInt()}", ImmersiveCyan)
                        StatChip("SPEED", String.format("%.1fx", combinedStats.totalSpeed), "+${String.format("%.1f", combinedStats.totalBonusSpeed)}", ImmersiveBlue)
                        StatChip("STEALTH", "${combinedStats.totalStealth.toInt()}%", "+${combinedStats.totalBonusStealth.toInt()}%", ImmersiveAmber)
                    }
                }
            }

            // 2. Equipment Slots Selector Row
            Text(
                text = "SELECT EQUIPMENT SLOT TO CONFIGURE",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ImmersiveSlateMuted
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(EquipmentSlot.values(), key = { it.name }) { slot ->
                    val isSelected = selectedSlot == slot
                    val equippedInSlot = currentLoadout.getEquipped(slot)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) ImmersiveGreen.copy(alpha = 0.2f) else ImmersiveBgDark,
                        border = BorderStroke(1.5.dp, if (isSelected) ImmersiveGreen else Color(0x33FFFFFF)),
                        modifier = Modifier
                            .clickable {
                                selectedSlot = slot
                                candidateItem = equippedInSlot ?: EquipmentItem.ALL_ITEMS.firstOrNull { slot.accepts(it.type) }
                            }
                            .testTag("slot_chip_${slot.name}")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = slot.name,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) ImmersiveGreen else Color.White
                            )
                            Text(
                                text = equippedInSlot?.name ?: "EMPTY",
                                fontSize = 8.5.sp,
                                color = if (equippedInSlot != null) ImmersiveCyan else ImmersiveSlateMuted,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 3. Slot Type Restrictions & Item Selection
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ImmersiveBgDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EQUIP ITEM INTO: ${selectedSlot.displayName.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveAmber
                        )

                        Text(
                            text = "ALLOWED: ${selectedSlot.allowedTypes.joinToString { it.name }}",
                            fontSize = 8.5.sp,
                            color = ImmersiveSlateMuted
                        )
                    }

                    // Candidate Item Picker Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(EquipmentItem.ALL_ITEMS, key = { "candidate_${it.id}" }) { item ->
                            val isChosen = candidateItem?.id == item.id
                            val isAllowed = selectedSlot.accepts(item.type)

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    isChosen -> ImmersiveLavender.copy(alpha = 0.25f)
                                    !isAllowed -> Color.Red.copy(alpha = 0.1f)
                                    else -> ImmersiveBgHeader
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when {
                                        isChosen -> ImmersiveLavender
                                        !isAllowed -> Color.Red.copy(alpha = 0.4f)
                                        else -> Color(0x22FFFFFF)
                                    }
                                ),
                                modifier = Modifier
                                    .clickable { candidateItem = item }
                                    .testTag("candidate_item_${item.id}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAllowed) Color.White else Color.Red
                                        )
                                        if (!isAllowed) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.Block,
                                                contentDescription = "Incompatible",
                                                tint = Color.Red,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = item.type.name,
                                        fontSize = 7.5.sp,
                                        color = if (isAllowed) ImmersiveCyan else Color.Red.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Validation Feedback Status Box
                    candidateItem?.let { item ->
                        when (val valResult = validationResult) {
                            is EquipValidationResult.Allowed -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = ImmersiveGreen.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, ImmersiveGreen.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Allowed",
                                            tint = ImmersiveGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "SLOT MATCH: '${item.name}' (${item.type}) fits in ${selectedSlot.displayName}.",
                                            fontSize = 9.sp,
                                            color = ImmersiveGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Preview Stat Deltas
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (statDelta.damageDelta != 0f) DeltaChip("DMG", statDelta.damageDelta)
                                    if (statDelta.armorDelta != 0f) DeltaChip("ARMOR", statDelta.armorDelta)
                                    if (statDelta.healthDelta != 0f) DeltaChip("HP", statDelta.healthDelta)
                                    if (statDelta.energyDelta != 0f) DeltaChip("NRG", statDelta.energyDelta)
                                    if (statDelta.speedDelta != 0f) DeltaChip("SPD", statDelta.speedDelta)
                                    if (statDelta.stealthDelta != 0f) DeltaChip("STL", statDelta.stealthDelta)
                                }
                            }
                            is EquipValidationResult.Denied -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Red.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Denied",
                                            tint = Color.Red,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = valResult.reason,
                                            fontSize = 8.5.sp,
                                            color = Color.Red,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 5. Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                candidateItem?.let { item ->
                                    val res = equipmentSystem.setEquippedItem(item, selectedSlot)
                                    operationFeedback = when (res) {
                                        is EquipOperationResult.Success -> "Equipped ${item.name} in ${selectedSlot.displayName}!"
                                        is EquipOperationResult.Failure -> "Failed: ${res.reason}"
                                    }
                                }
                            },
                            enabled = validationResult is EquipValidationResult.Allowed && candidateItem != null,
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveGreen),
                            modifier = Modifier.testTag("equip_slot_action_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Equip to ${selectedSlot.name}", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val res = equipmentSystem.removeEquippedItem(selectedSlot)
                                operationFeedback = when (res) {
                                    is EquipOperationResult.Success -> "Unequipped item from ${selectedSlot.displayName}."
                                    is EquipOperationResult.Failure -> res.reason
                                }
                            },
                            enabled = currentLoadout.getEquipped(selectedSlot) != null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveRed)
                        ) {
                            Text("Unequip Slot", fontSize = 10.sp)
                        }
                    }

                    operationFeedback?.let { fb ->
                        Text(
                            text = fb,
                            fontSize = 8.5.sp,
                            color = ImmersiveCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, bonus: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 8.sp, color = ImmersiveSlateMuted, fontWeight = FontWeight.Bold)
        Text(text = value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        Text(text = bonus, fontSize = 7.5.sp, color = color.copy(alpha = 0.8f))
    }
}

@Composable
private fun DeltaChip(label: String, delta: Float) {
    val isPositive = delta > 0
    val textColor = if (isPositive) ImmersiveGreen else ImmersiveRed
    val prefix = if (isPositive) "+" else ""
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = textColor.copy(alpha = 0.15f)
    ) {
        Text(
            text = "$label: $prefix${if (delta % 1f == 0f) delta.toInt().toString() else String.format("%.1f", delta)}",
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}