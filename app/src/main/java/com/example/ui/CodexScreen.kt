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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun CodexScreen(
    viewModel: GameViewModel,
    onBackToGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val datashards = viewModel.datashards
    var selectedShard by remember { mutableStateOf(datashards.first()) }
    var currentBufferInput by remember { mutableStateOf<List<String>>(emptyList()) }
    var decryptionError by remember { mutableStateOf(false) }

    // Cyberdeck Hex Memory Matrix (Available bytes to pick)
    val hexMemoryMatrix = remember(selectedShard) {
        listOf("1C", "E9", "7A", "55", "BD", "1A", "AA", "9C", "FF", "00", "44", "3C")
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
                        .testTag("codex_back_button")
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
                        text = "CORPORATE CODEX & CYBERDECK",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "ENCRYPTED MEMORY SHARDS & BLACK ICE ARCHIVES",
                        color = ImmersiveSlateMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Surface(
                color = ImmersiveBgHeader,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, CyberNeonCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = "Decrypted", tint = CyberNeonCyan, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${datashards.count { it.isDecrypted }}/${datashards.size} DECRYPTED",
                        color = CyberNeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // TWO-PANE LAYOUT
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // LEFT COLUMN: DATASHARD DIRECTORY (40% width)
            Column(
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "DISCOVERED DATASHARDS",
                    color = ImmersiveLavender,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(datashards) { shard ->
                        val isSelected = selectedShard.id == shard.id

                        Card(
                            onClick = {
                                SoundManager.playMenuClick()
                                selectedShard = shard
                                currentBufferInput = emptyList()
                                decryptionError = false
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ImmersiveDeepViolet.copy(alpha = 0.5f) else ImmersiveBgHeader
                            ),
                            border = BorderStroke(
                                if (isSelected) 1.5.dp else 1.dp,
                                if (shard.isDecrypted) ImmersiveGreen else if (isSelected) ImmersiveLavender else Color(0x22FFFFFF)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shard_item_${shard.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = shard.corporation,
                                        color = if (shard.isDecrypted) ImmersiveGreen else ImmersiveAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Icon(
                                        if (shard.isDecrypted) Icons.Default.LockOpen else Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = if (shard.isDecrypted) ImmersiveGreen else ImmersiveRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = shard.title,
                                    color = Color.White,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // RIGHT COLUMN: CYBERDECK DECRYPTION & LORE TERMINAL (60% width)
            Card(
                colors = CardDefaults.cardColors(containerColor = ImmersiveBgHeader),
                border = BorderStroke(1.2.dp, ImmersiveLavender.copy(alpha = 0.6f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.2f)
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
                        // Title & Security Level
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedShard.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                color = if (selectedShard.isDecrypted) ImmersiveGreen.copy(alpha = 0.2f) else ImmersiveRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(1.dp, if (selectedShard.isDecrypted) ImmersiveGreen else ImmersiveRed)
                            ) {
                                Text(
                                    text = if (selectedShard.isDecrypted) "STATUS: UNLOCKED" else selectedShard.securityLevel,
                                    color = if (selectedShard.isDecrypted) ImmersiveGreen else ImmersiveRed,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color(0x22FFFFFF))
                        Spacer(modifier = Modifier.height(10.dp))

                        if (selectedShard.isDecrypted) {
                            // DECRYPTED LORE TEXT
                            Text(
                                text = "DECRYPTED ARCHIVE PAYLOAD:",
                                color = ImmersiveGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedShard.decryptedLore,
                                color = ImmersiveSlateLight,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        } else {
                            // CYBERDECK HACKING MINIGAME INTERFACE
                            Text(
                                text = "CYBERDECK BUFFER BYPASS PROTOCOL",
                                color = CyberNeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Match the target hex sequence to decrypt corporate cipher:",
                                color = ImmersiveSlateMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // TARGET SEQUENCE DISPLAY
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("TARGET CIPHER:", color = ImmersiveAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                selectedShard.hexSequenceTarget.forEach { hex ->
                                    Surface(
                                        color = ImmersiveAmber.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(4.dp),
                                        border = BorderStroke(1.dp, ImmersiveAmber)
                                    ) {
                                        Text(
                                            text = hex,
                                            color = ImmersiveAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // CURRENT BUFFER INPUT
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("BUFFER INPUT:", color = CyberNeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                if (currentBufferInput.isEmpty()) {
                                    Text("[EMPTY BUFFER]", color = ImmersiveSlateMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                } else {
                                    currentBufferInput.forEach { hex ->
                                        Surface(
                                            color = CyberNeonCyan.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            border = BorderStroke(1.dp, CyberNeonCyan)
                                        ) {
                                            Text(
                                                text = hex,
                                                color = CyberNeonCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (decryptionError) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "CIPHER BUFFER MISMATCH - RETRY SEQUENCE",
                                    color = ImmersiveRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // HEX SELECTION GRID
                            Text(
                                text = "MEMORY BUFFER NODES",
                                color = ImmersiveLavender,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // 4-column hex keypad
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val chunked = hexMemoryMatrix.chunked(4)
                                chunked.forEach { row ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        row.forEach { byteCode ->
                                            Button(
                                                onClick = {
                                                    SoundManager.playMenuClick()
                                                    decryptionError = false
                                                    if (currentBufferInput.size < selectedShard.hexSequenceTarget.size) {
                                                        val next = currentBufferInput + byteCode
                                                        currentBufferInput = next
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgDark),
                                                border = BorderStroke(1.dp, ImmersiveLavender.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(36.dp)
                                            ) {
                                                Text(
                                                    text = byteCode,
                                                    color = Color.White,
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

                    // BOTTOM ACTIONS
                    if (!selectedShard.isDecrypted) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    currentBufferInput = emptyList()
                                    decryptionError = false
                                    AudioManager.playInteract()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBgDark),
                                border = BorderStroke(1.dp, ImmersiveSlateMuted),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.4f)
                                    .height(42.dp)
                            ) {
                                Text("CLEAR", color = ImmersiveSlateLight, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    val success = viewModel.attemptDecryptDatashard(selectedShard.id, currentBufferInput)
                                    if (success) {
                                        AudioManager.playLevelUp()
                                        selectedShard = selectedShard.copy(isDecrypted = true)
                                        decryptionError = false
                                    } else {
                                        decryptionError = true
                                        currentBufferInput = emptyList()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ImmersiveLavender),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(0.6f)
                                    .height(42.dp)
                                    .testTag("execute_decrypt_button")
                            ) {
                                Text(
                                    text = "EXECUTE BYPASS",
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
}
