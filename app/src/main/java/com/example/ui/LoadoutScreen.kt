package com.example.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*
import kotlin.math.abs

@Composable
fun LoadoutScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit
) {
    val player = viewModel.player
    var selectedCategory by remember { mutableStateOf(EquipmentType.WEAPON) }
    val categoryItems = EquipmentItem.ALL_ITEMS.filter { it.type == selectedCategory }
    var selectedItem by remember { mutableStateOf(categoryItems.firstOrNull()) }

    // Ensure selected item refreshes if category changes
    LaunchedEffect(selectedCategory) {
        selectedItem = categoryItems.firstOrNull()
    }

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
                    .testTag("loadout_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ImmersiveLavender)
            }
            Text(
                text = "OPERATIVE LOADOUT Matrix",
                color = ImmersiveSlateLight,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
            // Credits Tracker
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.dp, ImmersiveAmber),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "${player.credits} CREDITS",
                    color = ImmersiveAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Categories selector tab (Weapon, Core, System)
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
                        .padding(vertical = 10.dp),
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

        Spacer(modifier = Modifier.height(14.dp))

        // Main splits: Left items list, Right items stat inspector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Left list of category gear cards
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categoryItems.forEach { item ->
                    val isEquipped = when (selectedCategory) {
                        EquipmentType.WEAPON -> player.equippedWeapon.id == item.id
                        EquipmentType.CORE -> player.equippedCore.id == item.id
                        EquipmentType.SYSTEM -> player.equippedSystem.id == item.id
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
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (item.costCredits == 0) "STANDARD ISSUE" else "${item.costCredits}C",
                                    color = if (item.costCredits == 0) ImmersiveSlateMuted else ImmersiveAmber,
                                    fontSize = 10.sp,
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
                    }
                }
            }

            // Right inspector panel
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
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Divider(color = Color(0x0DFFFFFF), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("STAT AUGMENTS:", color = ImmersiveSlateMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 6.dp))

                        // Render Stat bars (Health, Energy, Damage, Speed, Stealth)
                        StatAugmentRow("DAMAGE", item.statBoostDamage, 80f, ImmersiveRed)
                        StatAugmentRow("SPEED BOOST", item.statBoostSpeed * 10f, 10f, ImmersiveBlue)
                        StatAugmentRow("STEALTH COEFF", item.statBoostStealth, 100f, ImmersiveLavender)
                        StatAugmentRow("HEALTH CHIP", item.statBoostHealth, 100f, ImmersiveGreen)
                        StatAugmentRow("ENERGY CORE", item.statBoostEnergy, 40f, ImmersiveDeepViolet)
                    }

                    // Equip action buttons
                    val isEquipped = when (selectedCategory) {
                        EquipmentType.WEAPON -> player.equippedWeapon.id == item.id
                        EquipmentType.CORE -> player.equippedCore.id == item.id
                        EquipmentType.SYSTEM -> player.equippedSystem.id == item.id
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
                            .height(44.dp)
                            .testTag("loadout_equip_action_button")
                    ) {
                        if (isEquipped) {
                            Text("EQUIPPED", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        } else if (item.costCredits > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ShoppingCart, contentDescription = "Buy", tint = Color.Black, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PURCHASE & EQUIP (${item.costCredits}C)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
}

@Composable
private fun StatAugmentRow(
    label: String,
    value: Float,
    maxValue: Float,
    color: Color
) {
    if (value == 0f) return // Skip rendering if neutral stats
    val displayValue = if (value > 0f) "+${value.toInt()}" else "${value.toInt()}"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = ImmersiveSlateLight, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(displayValue, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
        val pct = (abs(value) / maxValue).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = pct,
            color = color,
            trackColor = ImmersiveBgDark,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
        )
    }
}
