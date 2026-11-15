package com.example.ui.screens.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.crypto.ChatEncryptionFingerprint
import com.example.ui.components.AdtAvatar
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.util.QrCodeGenerator
import com.example.util.QrTheme
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

/**
 * Dedicated settings screen for viewing and verifying unique End-to-End
 * Encryption (E2EE) key fingerprints across all active chat sessions.
 * Implements Signal Double Ratchet 60-digit Safety Numbers & SHA-256 digests.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EncryptionKeysScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val verifiedChatIds by viewModel.verifiedChatFingerprints.collectAsState()
    val activeChats by viewModel.activeChats.collectAsState()
    
    // Compute current chat fingerprints dynamically
    val fingerprints = remember(activeChats, verifiedChatIds) {
        viewModel.getChatEncryptionFingerprints()
    }
    
    val myIdentityFingerprint = remember {
        viewModel.getMyIdentityFingerprint()
    }
    val myPublicKeyBase64 = remember {
        viewModel.getMyIdentityPublicKey()
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, VERIFIED, UNVERIFIED
    var selectedSessionForVerification by remember { mutableStateOf<ChatEncryptionFingerprint?>(null) }
    var showMyIdentityQrDialog by remember { mutableStateOf(false) }
    var showProtocolInfoDialog by remember { mutableStateOf(false) }

    val filteredList = remember(fingerprints, searchQuery, selectedFilter) {
        fingerprints.filter { item ->
            val matchesSearch = item.title.contains(searchQuery, ignoreCase = true) ||
                    item.subtitle.contains(searchQuery, ignoreCase = true) ||
                    item.hexFingerprint.contains(searchQuery, ignoreCase = true) ||
                    item.safetyNumber.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "VERIFIED" -> item.isVerified
                "UNVERIFIED" -> !item.isVerified
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    val verifiedCount = fingerprints.count { it.isVerified }
    val unverifiedCount = fingerprints.size - verifiedCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Clés de chiffrement E2EE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Protocole Signal • Double Ratchet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_encryption_keys")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour aux Paramètres")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showProtocolInfoDialog = true },
                        modifier = Modifier.testTag("btn_encryption_info")
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Informations sur le chiffrement", tint = AdtPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // =====================================================================
            // 1. MASTER IDENTITY KEY CARD (Votre clé d'identité locale)
            // =====================================================================
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdtPrimary.copy(alpha = 0.08f)
                    ),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth().testTag("card_my_identity_fingerprint")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = AdtPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = AdtPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Votre Clé d'Identité Publique",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "Empreinte unique générée sur cet appareil",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ACTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Visual Identicon Palette Bar
                        val myColors = remember(myIdentityFingerprint) {
                            ChatEncryptionFingerprint.deriveVisualColors(myIdentityFingerprint)
                        }
                        VisualIdenticonBar(colors = myColors)

                        // Formatted SHA-256 Hex Digest
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "EMPREINTE SHA-256 :",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = myIdentityFingerprint,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        // Actions for my key
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    copyToClipboard(context, "Empreinte E2EE ADT Pro", myIdentityFingerprint)
                                    Toast.makeText(context, "Empreinte copiée dans le presse-papier", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("btn_copy_my_fingerprint")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copier", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showMyIdentityQrDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                modifier = Modifier.weight(1f).testTag("btn_show_my_qr")
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("QR Code", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // =====================================================================
            // 2. METRICS & STATUS SUMMARY
            // =====================================================================
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Sessions",
                        value = "${fingerprints.size}",
                        icon = Icons.AutoMirrored.Filled.Chat,
                        color = AdtPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Vérifiées",
                        value = "$verifiedCount",
                        icon = Icons.Default.VerifiedUser,
                        color = AdtAccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Algorithme",
                        value = "AES-GCM",
                        icon = Icons.Default.Lock,
                        color = Color(0xFF1E88E5),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // =====================================================================
            // 3. SEARCH & FILTER CONTROLS
            // =====================================================================
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("input_search_encryption_sessions"),
                        placeholder = { Text("Rechercher un contact, groupe ou empreinte...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AdtPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Effacer")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdtPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    // Filter chips
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedFilter == "ALL",
                                onClick = { selectedFilter = "ALL" },
                                label = { Text("Toutes (${fingerprints.size})", fontSize = 12.sp) },
                                leadingIcon = {
                                    if (selectedFilter == "ALL") {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "VERIFIED",
                                onClick = { selectedFilter = "VERIFIED" },
                                label = { Text("Vérifiées ($verifiedCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AdtAccentGreen.copy(alpha = 0.15f),
                                    selectedLabelColor = AdtAccentGreen
                                ),
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (selectedFilter == "VERIFIED") AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )
                        }
                        item {
                            FilterChip(
                                selected = selectedFilter == "UNVERIFIED",
                                onClick = { selectedFilter = "UNVERIFIED" },
                                label = { Text("À vérifier ($unverifiedCount)", fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SESSIONS DE DISCUSSION (${filteredList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Chiffrement Signal actif",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // =====================================================================
            // 4. CHAT SESSIONS FINGERPRINTS LIST
            // =====================================================================
            if (filteredList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.SearchOff, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Aucune session trouvée", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Modifiez votre recherche ou démarrez une nouvelle discussion pour générer une empreinte de sécurité.", fontSize = 12.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.chatId }) { item ->
                    ChatSessionFingerprintCard(
                        item = item,
                        onCompareClicked = { selectedSessionForVerification = item },
                        onToggleVerified = {
                            viewModel.toggleFingerprintVerified(item.chatId)
                            val statusMsg = if (item.isVerified) "Empreinte non vérifiée" else "Empreinte vérifiée avec succès !"
                            Toast.makeText(context, statusMsg, Toast.LENGTH_SHORT).show()
                        },
                        onCopyFingerprint = {
                            copyToClipboard(context, "Empreinte ${item.title}", item.toSharableSummary())
                            Toast.makeText(context, "Empreinte et numéro de sécurité copiés", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // =========================================================================
    // DIALOG: COMPARE & VERIFY SAFETY NUMBER / QR CODE
    // =========================================================================
    selectedSessionForVerification?.let { session ->
        CompareFingerprintQrDialog(
            session = session,
            onDismiss = { selectedSessionForVerification = null },
            onToggleVerified = {
                viewModel.toggleFingerprintVerified(session.chatId)
                selectedSessionForVerification = session.copy(isVerified = !session.isVerified)
            }
        )
    }

    // =========================================================================
    // DIALOG: MY MASTER IDENTITY QR CODE
    // =========================================================================
    if (showMyIdentityQrDialog) {
        MyIdentityQrDialog(
            myIdentityFingerprint = myIdentityFingerprint,
            myPublicKeyBase64 = myPublicKeyBase64,
            onDismiss = { showMyIdentityQrDialog = false }
        )
    }

    // =========================================================================
    // DIALOG: PROTOCOL EXPLANATION & AUDIT INFO
    // =========================================================================
    if (showProtocolInfoDialog) {
        ProtocolInfoDialog(onDismiss = { showProtocolInfoDialog = false })
    }
}

/**
 * Modern Card displaying a single chat session's unique E2EE key fingerprint
 */
@Composable
private fun ChatSessionFingerprintCard(
    item: ChatEncryptionFingerprint,
    onCompareClicked: () -> Unit,
    onToggleVerified: () -> Unit,
    onCopyFingerprint: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_session_fingerprint_${item.chatId}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Avatar, Name, and Verification Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    AdtAvatar(name = item.title, avatarKey = item.avatarUrl, sizeDp = 44)
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (item.isGroup) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = AdtPrimary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "GROUPE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdtPrimary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Verification Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (item.isVerified) AdtAccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier.clickable { onToggleVerified() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isVerified) Icons.Default.VerifiedUser else Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = if (item.isVerified) AdtAccentGreen else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (item.isVerified) "VÉRIFIÉ" else "À COMPARER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isVerified) AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Visual Identicon Bar
            VisualIdenticonBar(colors = item.visualFingerprintColors)

            // Monospace Safety Number Display (60 digits in 12 blocks)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NUMÉRO DE SÉCURITÉ SIGNAL (60 CHIFFRES) :",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = item.shortFingerprint,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = AdtPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.safetyNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Cryptographic Metadata pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CryptoTag(text = "Double Ratchet", icon = Icons.Default.Lock)
                CryptoTag(text = "Étape #${item.ratchetStep}", icon = Icons.Default.Sync)
                CryptoTag(text = item.ephemeralKeySnippet, icon = Icons.Default.VpnKey)
            }

            // Bottom Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onCompareClicked,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f).testTag("btn_compare_qr_${item.chatId}")
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Comparer & QR Code", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedIconButton(
                    onClick = onCopyFingerprint,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp).testTag("btn_copy_${item.chatId}")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copier l'empreinte", modifier = Modifier.size(16.dp))
                }

                IconButton(
                    onClick = onToggleVerified,
                    modifier = Modifier.size(36.dp).testTag("btn_verify_${item.chatId}")
                ) {
                    Icon(
                        imageVector = if (item.isVerified) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Marquer comme vérifié",
                        tint = if (item.isVerified) AdtAccentGreen else MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

/**
 * 4-segment visual color bar providing human optical recognition of cryptographic hashes
 */
@Composable
private fun VisualIdenticonBar(colors: List<Color>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(color)
            )
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun CryptoTag(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * Modal to display high-fidelity QR Code and 60-digit safety number grid for verbal or camera verification
 */
@Composable
private fun CompareFingerprintQrDialog(
    session: ChatEncryptionFingerprint,
    onDismiss: () -> Unit,
    onToggleVerified: () -> Unit
) {
    val context = LocalContext.current
    val qrContent = remember(session) {
        "adt-signal-safety-number:${session.chatId}:${session.safetyNumber.replace(" ", "")}"
    }
    val qrBitmap = remember(qrContent) {
        try {
            QrCodeGenerator.generateQrBitmap(
                content = qrContent,
                size = 560,
                theme = QrTheme.EMERALD,
                withCenterBadge = true
            )
        } catch (e: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("dialog_compare_fingerprint")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AdtAvatar(name = session.title, avatarKey = session.avatarUrl, sizeDp = 36)
                        Column {
                            Text(session.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                            Text("Vérification de sécurité", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider()

                // QR Code Display
                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(200.dp).padding(6.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code de vérification du numéro de sécurité",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "Scannez ce code ou comparez les chiffres ci-dessous avec votre correspondant pour garantir qu'aucun tiers n'intercepte la discussion.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                // 60-digit number broken into 3x4 grid for ease of reading aloud
                val digitBlocks = remember(session.safetyNumber) {
                    session.safetyNumber.split(" ")
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        digitBlocks.chunked(4).forEach { rowBlocks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                rowBlocks.forEach { block ->
                                    Text(
                                        text = block,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, session.toSharableSummary())
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Partager l'empreinte"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            onToggleVerified()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (session.isVerified) MaterialTheme.colorScheme.outline else AdtAccentGreen
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_dialog_toggle_verify")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (session.isVerified) "Dévérifier" else "Marquer vérifié", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Dialog displaying the user's master Signal Identity Public Key as a QR code
 */
@Composable
private fun MyIdentityQrDialog(
    myIdentityFingerprint: String,
    myPublicKeyBase64: String,
    onDismiss: () -> Unit
) {
    val qrContent = remember(myIdentityFingerprint, myPublicKeyBase64) {
        "adt-signal-identity:$myIdentityFingerprint:$myPublicKeyBase64"
    }
    val qrBitmap = remember(qrContent) {
        try {
            QrCodeGenerator.generateQrBitmap(
                content = qrContent,
                size = 560,
                theme = QrTheme.CYAN,
                withCenterBadge = true
            )
        } catch (e: Exception) {
            null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("dialog_my_identity_qr")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Votre Clé Publique E2EE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider()

                if (qrBitmap != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(220.dp).padding(6.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "QR Code Clé d'identité",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Text(
                    text = "Ce QR Code contient votre clé publique secp256r1. Les correspondants peuvent le scanner pour valider votre identité.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = myIdentityFingerprint,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(8.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Terminer")
                }
            }
        }
    }
}

/**
 * Information modal detailing the cryptographic guarantees of Signal Double Ratchet
 */
@Composable
private fun ProtocolInfoDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().testTag("dialog_protocol_info")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = AdtAccentGreen)
                        Text("Sécurité Cryptographique", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                HorizontalDivider()

                ProtocolInfoItem(
                    title = "Protocole Signal Double Ratchet",
                    desc = "Chaque message utilise une nouvelle clé éphémère à usage unique dérivée par un cliquet cryptographique symétrique et Diffie-Hellman."
                )
                ProtocolInfoItem(
                    title = "Confidentialité Persistante (Forward Secrecy)",
                    desc = "Même si une clé de session venait à être compromise à l'instant T, les messages passés et futurs restent parfaitement indéchiffrables."
                )
                ProtocolInfoItem(
                    title = "Chiffrement Authentifié AES-256-GCM",
                    desc = "Assure à la fois la confidentialité totale du contenu et la détection infaillible de toute altération avec une balise d'authentification 128-bit."
                )
                ProtocolInfoItem(
                    title = "Vérification par Numéro de Sécurité",
                    desc = "Les 60 chiffres sont calculés par hachage SHA-512 de vos clés d'identité respectives, empêchant formellement toute attaque de type Man-in-the-Middle (MitM)."
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("J'ai compris")
                }
            }
        }
    }
}

@Composable
private fun ProtocolInfoItem(title: String, desc: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = AdtAccentGreen, modifier = Modifier.size(14.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Text(
            text = desc,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp),
            lineHeight = 15.sp
        )
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
