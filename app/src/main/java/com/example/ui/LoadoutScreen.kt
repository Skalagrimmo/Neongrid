package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomLoadoutEntity
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.abs

/**
 * Interactive Loadout & Inventory Management Screen backed by Room Database.
 * Allows players to select equipment, purchase upgrades, and persist/swap custom multi-slot loadouts.
 */
@Composable
fun LoadoutScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val player = viewModel.player
    val customLoadouts by viewModel.customLoadouts.collectAsStateWithLifecycle()

    var selectedCategory by remember { mutableStateOf(EquipmentType.WEAPON) }
    val categoryItems = EquipmentItem.ALL_ITEMS.filter { it.type == selectedCategory }
    var selectedItem by remember { mutableStateOf(categoryItems.firstOrNull()) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var newLoadoutName by remember { mutableStateOf("") }

    // Synchronize selected item if category tab switches
    LaunchedEffect(selectedCategory) {
        selectedItem = categoryItems.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBgDark)
            .padding(14.dp)
            .testTag("loadout_screen_root")
    ) {
        // =========================================================================
        // 1. TOP HEADER & CREDITS STATS
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackToGame,
                    modifier = Modifier
                        .size(40.dp)
                        .background(ImmersiveBgHeader, RoundedCornerShape(8.dp))
                        .border(1.dp, ImmersiveLavender, RoundedCornerShape(8.dp))
                        .testTag("loadout_back_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ImmersiveLavender)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "OPERATIVE LOADOUT MATRIX",
                        color = ImmersiveSlateLight,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Room DB Persisted Inventory & Gear Presets",
                        color = ImmersiveSlateMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Credits Counter Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.dp, ImmersiveAmber),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("loadout_credits_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Credits",
                        tint = ImmersiveAmber,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${player.credits} CREDITS",
                        color = ImmersiveAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =========================================================================
        // 2. ROOM DB PERSISTED CUSTOM LOADOUTS PRESET BAR
        // =========================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader.copy(alpha = 0.9f)),
            border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            tint = ImmersiveLavender,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROOM DB LOADOUT PRESETS",
                            color = ImmersiveLavender,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Button to open "Save Loadout" Dialog
                    Button(
                        onClick = {
                            newLoadoutName = "LOADOUT #${customLoadouts.size + 1}"
                            showSaveDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveLavender.copy(alpha = 0.2f),
                            contentColor = ImmersiveLavender
                        ),
                        border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.testTag("save_loadout_preset_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Save", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SAVE CURRENT SETUP", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Horizontal list of saved Room DB Custom Loadouts
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(customLoadouts, key = { it.id }) { loadout ->
                        val isCurrentlyEquipped =
                            player.equippedWeapon.id == loadout.weaponId &&
                                    player.equippedCore.id == loadout.coreId &&
                                    player.equippedSystem.id == loadout.systemId

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentlyEquipped) ImmersiveLavender.copy(alpha = 0.18f) else ImmersiveBgDark
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrentlyEquipped) ImmersiveLavender else Color.White.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .width(210.dp)
                                .testTag("custom_loadout_item_${loadout.id}")
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = loadout.name.uppercase(),
                                        color = Color.White,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteCustomLoadout(loadout.id) },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Loadout",
                                            tint = CyberNeonRed.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "W: ${loadout.weaponName}",
                                    color = ImmersiveSlateMuted,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                                Text(
                                    text = "C: ${loadout.coreName}",
                                    color = ImmersiveSlateMuted,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = { viewModel.applyCustomLoadout(loadout) },
                                    enabled = !isCurrentlyEquipped,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ImmersiveLavender,
                                        disabledContainerColor = ImmersiveGreen.copy(alpha = 0.2f),
                                        disabledContentColor = ImmersiveGreen
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    contentPadding = PaddingValues(vertical = 2.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(26.dp)
                                ) {
                                    if (isCurrentlyEquipped) {
                                        Text("ACTIVE LOADOUT", fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    } else {
                                        Text("APPLY PRESET", color = Color.Black, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // =========================================================================
        // 3. CATEGORIES SELECTOR TABS (WEAPON, CORE, SYSTEM)
        // =========================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EquipmentType.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) ImmersiveBgHeader else ImmersiveBgDark,
                            RoundedCornerShape(6.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) ImmersiveLavender else Color(0x1AFFFFFF),
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 8.dp)
                        .testTag("equipment_tab_${cat.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.name,
                        color = if (isSelected) ImmersiveLavender else ImmersiveSlateMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // =========================================================================
        // 4. MAIN SPLIT: LEFT GEAR CATALOG / RIGHT INSPECTOR PANEL
        // =========================================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left list of gear cards
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categoryItems.forEach { item ->
                    val isEquipped = when (selectedCategory) {
                        EquipmentType.WEAPON -> player.equippedWeapon.id == item.id
                        EquipmentType.CORE -> player.equippedCore.id == item.id
                        EquipmentType.SYSTEM -> player.equippedSystem.id == item.id
                        else -> player.equippedWeapon.id == item.id || player.equippedCore.id == item.id || player.equippedSystem.id == item.id
                    }
                    val isInspected = selectedItem?.id == item.id

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isInspected) ImmersiveBgHeader else ImmersiveBgDark
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            when {
                                isInspected -> ImmersiveLavender
                                isEquipped -> ImmersiveGreen
                                else -> Color(0x0DFFFFFF)
                            }
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedItem = item }
                            .testTag("equipment_item_card_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = item.name.uppercase(),
                                    color = ImmersiveSlateLight,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (item.costCredits == 0) "STANDARD ISSUE" else "${item.costCredits}C",
                                    color = if (item.costCredits == 0) ImmersiveSlateMuted else ImmersiveAmber,
                                    fontSize = 9.5.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (isEquipped) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = ImmersiveGreen.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, ImmersiveGreen.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "EQUIPPED",
                                        color = ImmersiveGreen,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right Inspector Panel
            Column(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight()
                    .background(ImmersiveBgHeader, RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0x0DFFFFFF), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                selectedItem?.let { item ->
                    Column {
                        Text(
                            text = item.name.uppercase(),
                            color = ImmersiveSlateLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description,
                            color = ImmersiveSlateMuted,
                            fontSize = 10.5.sp,
                            lineHeight = 14.5.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        HorizontalDivider(color = Color(0x0DFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("STAT AUGMENTS:", color = ImmersiveSlateMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 6.dp))

                        // Stat bars
                        StatAugmentRow("DAMAGE", item.statBoostDamage, 80f, ImmersiveRed)
                        StatAugmentRow("SPEED BOOST", item.statBoostSpeed * 10f, 10f, ImmersiveBlue)
                        StatAugmentRow("STEALTH COEFF", item.statBoostStealth, 100f, ImmersiveLavender)
                        StatAugmentRow("HEALTH CHIP", item.statBoostHealth, 100f, ImmersiveGreen)
                        StatAugmentRow("ENERGY CORE", item.statBoostEnergy, 40f, ImmersiveDeepViolet)
                    }

                    // Equip action button
                    val isEquipped = when (selectedCategory) {
                        EquipmentType.WEAPON -> player.equippedWeapon.id == item.id
                        EquipmentType.CORE -> player.equippedCore.id == item.id
                        EquipmentType.SYSTEM -> player.equippedSystem.id == item.id
                        else -> player.equippedWeapon.id == item.id || player.equippedCore.id == item.id || player.equippedSystem.id == item.id
                    }

                    val canAfford = player.credits >= item.costCredits

                    Button(
                        onClick = {
                            if (item.costCredits > 0 && player.credits >= item.costCredits) {
                                viewModel.buyEquipmentItem(item)
                            } else {
                                viewModel.equipItem(item)
                            }
                        },
                        enabled = !isEquipped && (item.costCredits == 0 || canAfford),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ImmersiveLavender,
                            disabledContainerColor = Color(0xFF16161B)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("loadout_equip_action_button")
                    ) {
                        if (isEquipped) {
                            Text("EQUIPPED", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        } else if (item.costCredits > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Buy", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PURCHASE & EQUIP (${item.costCredits}C)", color = Color.Black, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Text("EQUIP MODULE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                } ?: Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SELECT UPGRADE CHIP", color = ImmersiveSlateMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }

    // Save Preset Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "SAVE ROOM DB PRESET",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column {
                    Text(
                        text = "Store current equipped setup (Weapon, Core, System) to Room database for rapid switching.",
                        color = ImmersiveSlateMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newLoadoutName,
                        onValueChange = { newLoadoutName = it },
                        label = { Text("Preset Designation", fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ImmersiveLavender,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            focusedLabelColor = ImmersiveLavender
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("loadout_preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCurrentLoadoutAsCustom(newLoadoutName)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveLavender),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("loadout_dialog_confirm_button")
                ) {
                    Text("SAVE PRESET", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = ImmersiveSlateMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            },
            containerColor = ImmersiveBgHeader,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

@Composable
private fun StatAugmentRow(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color
) {
    if (value == 0f) return
    val displayValue = if (value > 0f) "+${value.toInt()}" else "${value.toInt()}"

    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = ImmersiveSlateLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(displayValue, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        val pct = (abs(value) / maxValue).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { pct },
            color = color,
            trackColor = ImmersiveBgDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        )
    }
}
