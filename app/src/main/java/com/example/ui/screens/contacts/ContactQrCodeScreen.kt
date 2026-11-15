package com.example.ui.screens.contacts

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.ContactEntity
import com.example.data.model.CountryData
import com.example.data.model.CountryItem
import com.example.ui.components.PhoneWithDialCodeInput
import com.example.ui.theme.AdtPrimary
import com.example.ui.theme.AdtPrimaryDark
import com.example.util.QrCodeGenerator
import com.example.util.QrContactPayload
import com.example.util.QrPayloadFormat
import com.example.util.QrTheme
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactQrCodeScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentUser by viewModel.currentUser.collectAsState()
    val initialTab by viewModel.qrInitialTab.collectAsState()
    val scannedHistory by viewModel.qrScannedHistory.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var selectedTheme by remember { mutableStateOf(QrTheme.EMERALD) }
    var selectedFormat by remember { mutableStateOf(QrPayloadFormat.ADT_DIRECT) }
    var showPhoneInQr by remember { mutableStateOf(true) }
    var showRoleInQr by remember { mutableStateOf(true) }
    var dynamicToken by remember { mutableStateOf(UUID.randomUUID().toString().take(8)) }
    var tokenGenerationTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Dialog state when a QR code is scanned or detected
    var detectedContactPayload by remember { mutableStateOf<QrContactPayload?>(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Synchronize tab if view model requested a specific tab
    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }

    // Prepare current user contact payload
    val userPhone = currentUser?.phone?.ifBlank { "+221 77 123 45 67" } ?: "+221 77 123 45 67"
    val userName = currentUser?.name?.ifBlank { "Membre ADT Pro" } ?: "Membre ADT Pro"
    val userRole = currentUser?.businessCategory?.ifBlank { "Professionnel ADT" } ?: "Professionnel ADT"
    val userOrg = "Réseau ADT Pro"
    val userBio = currentUser?.bio?.ifBlank { "Disponible sur ADT Pro" } ?: "Disponible sur ADT Pro"

    val currentPayload = remember(userName, userPhone, userRole, dynamicToken, showPhoneInQr, showRoleInQr) {
        QrContactPayload(
            id = "adt_user_" + userPhone.filter { it.isDigit() },
            name = userName,
            phone = if (showPhoneInQr) userPhone else "",
            countryCode = currentUser?.countryCode ?: "+221",
            role = if (showRoleInQr) userRole else "",
            organization = if (showRoleInQr) userOrg else "",
            bio = userBio,
            timestamp = tokenGenerationTime,
            token = dynamicToken,
            isVerified = true
        )
    }

    // Photo picker for decoding QR from gallery
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val bitmap = BitmapFactory.decodeStream(stream)
                    val rawText = QrCodeGenerator.decodeQrFromBitmap(bitmap)
                    if (rawText != null) {
                        val parsed = QrCodeGenerator.parsePayload(rawText)
                        if (parsed != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            detectedContactPayload = parsed
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("QR Code détecté mais format non reconnu.")
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Aucun code QR lisible n'a été trouvé sur cette image.")
                        }
                    }
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Impossible de lire l'image : ${e.message}")
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Code QR & Réseau Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Partage dynamique & ajout instantané",
                            fontSize = 11.sp,
                            color = AdtPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CONTACTS_LIST) },
                        modifier = Modifier.testTag("btn_back_qr_screen")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("btn_qr_info")
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Informations de sécurité", tint = AdtPrimary)
                    }
                    if (selectedTab == 0) {
                        IconButton(
                            onClick = {
                                val cardBitmap = QrCodeGenerator.createDigitalBusinessCardBitmap(
                                    context,
                                    currentPayload,
                                    selectedTheme
                                )
                                QrCodeGenerator.shareDigitalCard(context, cardBitmap, currentPayload)
                            },
                            modifier = Modifier.testTag("btn_top_share_qr_card")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Partager la carte")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = AdtPrimary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AdtPrimary
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Mon Code QR", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_my_qr_code")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Scanner", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) },
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_scan_qr_code")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Historique", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (scannedHistory.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(AdtPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${scannedHistory.size}",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.testTag("tab_qr_history")
                )
            }

            // Tab Contents
            when (selectedTab) {
                0 -> MyQrCodeTabContent(
                    payload = currentPayload,
                    theme = selectedTheme,
                    format = selectedFormat,
                    showPhone = showPhoneInQr,
                    showRole = showRoleInQr,
                    onThemeChange = { selectedTheme = it },
                    onFormatChange = { selectedFormat = it },
                    onTogglePhone = { showPhoneInQr = it },
                    onToggleRole = { showRoleInQr = it },
                    onRefreshToken = {
                        dynamicToken = UUID.randomUUID().toString().take(8)
                        tokenGenerationTime = System.currentTimeMillis()
                        scope.launch {
                            snackbarHostState.showSnackbar("Jeton dynamique de sécurité actualisé avec succès.")
                        }
                    },
                    onShareCard = {
                        val cardBitmap = QrCodeGenerator.createDigitalBusinessCardBitmap(
                            context,
                            currentPayload,
                            selectedTheme
                        )
                        QrCodeGenerator.shareDigitalCard(context, cardBitmap, currentPayload)
                    },
                    onCopyLink = {
                        val link = "https://adtpro.app/join?user=${currentPayload.phone.filter { it.isDigit() }}&tk=${currentPayload.token}"
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Lien ADT Pro", link))
                        scope.launch {
                            snackbarHostState.showSnackbar("Lien d'invitation pro copié dans le presse-papier !")
                        }
                    },
                    onSaveCard = {
                        val cardBitmap = QrCodeGenerator.createDigitalBusinessCardBitmap(
                            context,
                            currentPayload,
                            selectedTheme
                        )
                        Toast.makeText(context, "Carte digitale prête pour l'export et la sauvegarde !", Toast.LENGTH_SHORT).show()
                        QrCodeGenerator.shareDigitalCard(context, cardBitmap, currentPayload)
                    }
                )
                1 -> ScanQrTabContent(
                    onScanSuccess = { payload ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        detectedContactPayload = payload
                    },
                    onPickFromGallery = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onManualInput = { showManualInputDialog = true }
                )
                2 -> QrHistoryTabContent(
                    history = scannedHistory,
                    onStartChat = { contact ->
                        viewModel.openDirectChatWithContact(contact)
                    },
                    onStartCall = { contact ->
                        viewModel.startCall(contact.name, contact.phone, contact.avatarUrl, isVideo = false)
                    },
                    onOpenScanner = { selectedTab = 1 }
                )
            }
        }
    }

    // Scanned contact preview modal
    detectedContactPayload?.let { payload ->
        val alreadyExists = allContacts.any { it.phone == payload.phone || it.id == payload.id }
        ScannedContactDialog(
            payload = payload,
            alreadyInContacts = alreadyExists,
            onDismiss = { detectedContactPayload = null },
            onAddToNetwork = { contact ->
                viewModel.addContactFromQr(contact) { success ->
                    if (success) {
                        scope.launch {
                            snackbarHostState.showSnackbar("${contact.name} a été ajouté(e) avec succès à votre réseau ADT Pro !")
                        }
                    }
                }
            },
            onStartChat = { contact ->
                detectedContactPayload = null
                viewModel.openDirectChatWithContact(contact)
            },
            onStartCall = { contact ->
                detectedContactPayload = null
                viewModel.startCall(contact.name, contact.phone, contact.avatarUrl, isVideo = false)
            }
        )
    }

    // Manual input dialog
    if (showManualInputDialog) {
        ManualCodeInputDialog(
            onDismiss = { showManualInputDialog = false },
            onSubmit = { input ->
                val parsed = QrCodeGenerator.parsePayload(input)
                if (parsed != null) {
                    detectedContactPayload = parsed
                    showManualInputDialog = false
                } else {
                    Toast.makeText(context, "Code ou numéro non reconnu", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Security & Info dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary)
                    Text("Partage Sécurisé ADT Pro", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Le système de Code QR Dynamique d'ADT Pro vous permet d'échanger vos coordonnées professionnelles en toute confiance.",
                        fontSize = 13.sp
                    )
                    Text(
                        "• Jeton dynamique anti-usurpation : chaque code comporte un horodatage et un jeton unique que vous pouvez renouveler à tout moment.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Chiffrement de bout en bout : dès l'ajout d'un contact via son code QR, un canal chiffré E2EE est instantanément initialisé.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "• Formats universels : compatible avec les appareils Android, iOS et les formats d'entreprise vCard 3.0.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Compris")
                }
            }
        )
    }
}

/**
 * Tab 0: Displays user's dynamic QR Code with interactive themes, options and actions.
 */
@Composable
private fun MyQrCodeTabContent(
    payload: QrContactPayload,
    theme: QrTheme,
    format: QrPayloadFormat,
    showPhone: Boolean,
    showRole: Boolean,
    onThemeChange: (QrTheme) -> Unit,
    onFormatChange: (QrPayloadFormat) -> Unit,
    onTogglePhone: (Boolean) -> Unit,
    onToggleRole: (Boolean) -> Unit,
    onRefreshToken: () -> Unit,
    onShareCard: () -> Unit,
    onCopyLink: () -> Unit,
    onSaveCard: () -> Unit
) {
    val payloadString = remember(payload, format) {
        QrCodeGenerator.encodePayload(payload, format)
    }

    val qrBitmap = remember(payloadString, theme) {
        QrCodeGenerator.generateQrBitmap(
            content = payloadString,
            size = 580,
            theme = theme,
            withCenterBadge = true
        )
    }

    val dateFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dynamic QR Card container
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, theme.composeColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_my_qr_code")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Header inside the Card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(theme.composeColor, AdtPrimaryDark)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = payload.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = payload.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Vérifié",
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (payload.role.isNotBlank()) {
                                Text(
                                    text = payload.role,
                                    fontSize = 12.sp,
                                    color = theme.composeColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (payload.phone.isNotBlank()) {
                                Text(
                                    text = payload.phone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // QR Code Image in white container
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "Mon Code QR ADT Pro",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dynamic token & Security live chip
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(theme.composeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(AdtPrimary, CircleShape)
                        )
                        Text(
                            text = "Jeton dynamique #${payload.token} • Émis à ${dateFormat.format(Date(payload.timestamp))}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = theme.composeColor
                        )
                        IconButton(
                            onClick = onRefreshToken,
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("btn_refresh_token")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualiser le jeton",
                                tint = theme.composeColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Quick Primary Actions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onShareCard,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_share_digital_card"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.composeColor)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager la Carte", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_copy_qr_link"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, theme.composeColor)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = theme.composeColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier le Lien", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = theme.composeColor)
                }
            }
        }

        // Theme Customization Palette
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Thème visuel du Code QR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.composeColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QrTheme.entries.forEach { t ->
                            val isSelected = t == theme
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable { onThemeChange(t) }
                                    .testTag("theme_${t.name.lowercase()}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(t.composeColor, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = t.title.substringBefore(" "),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        // Configuration & Privacy Toggles
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Données incluses & Format",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = theme.composeColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Toggle Phone
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Afficher mon numéro de téléphone", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Permet aux contacts d'enregistrer votre ligne directe", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showPhone,
                            onCheckedChange = onTogglePhone,
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.composeColor, checkedTrackColor = theme.composeColor.copy(alpha = 0.5f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Toggle Role
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Inclure le titre & entreprise", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("Affiche votre fonction professionnelle sur la carte", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showRole,
                            onCheckedChange = onToggleRole,
                            colors = SwitchDefaults.colors(checkedThumbColor = theme.composeColor, checkedTrackColor = theme.composeColor.copy(alpha = 0.5f))
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Format selection chips
                    Text("Format d'encodage :", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QrPayloadFormat.entries.forEach { f ->
                            val isSelected = f == format
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) theme.composeColor else MaterialTheme.colorScheme.surface)
                                    .border(1.dp, if (isSelected) theme.composeColor else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                    .clickable { onFormatChange(f) }
                                    .padding(vertical = 8.dp, horizontal = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (f) {
                                        QrPayloadFormat.ADT_DIRECT -> "ADT Direct"
                                        QrPayloadFormat.UNIVERSAL_WEB -> "Web Universel"
                                        QrPayloadFormat.VCARD -> "vCard 3.0"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 1: Interactive Scanner with animated viewfinder, photo picker and quick test sample contacts.
 */
@Composable
private fun ScanQrTabContent(
    onScanSuccess: (QrContactPayload) -> Unit,
    onPickFromGallery: () -> Unit,
    onManualInput: () -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }

    // Sample contacts for instant testability in emulator or environment without camera
    val demoContacts = remember {
        listOf(
            QrContactPayload(
                id = "adt_demo_aminata",
                name = "Aminata Diallo",
                phone = "+221 77 456 78 90",
                role = "Directrice des Systèmes d'Information",
                organization = "Sonatel / Orange Sénégal",
                bio = "Membre certifiée réseau ADT Pro Enterprise",
                isVerified = true
            ),
            QrContactPayload(
                id = "adt_demo_ousmane",
                name = "Ousmane Faye",
                phone = "+221 78 888 12 34",
                role = "Architecte Sécurité Cloud",
                organization = "Orange Cyberdefense",
                bio = "Spécialiste E2EE & Infrastructures Résilientes",
                isVerified = true
            ),
            QrContactPayload(
                id = "adt_demo_fatou",
                name = "Fatou Ndiaye",
                phone = "+221 76 345 67 89",
                role = "Consultante Télécoms & FinTech",
                organization = "Wave Digital Finance",
                bio = "Partenaire Réseau ADT Pro",
                isVerified = true
            ),
            QrContactPayload(
                id = "adt_demo_aissatou",
                name = "Dr. Aïssatou Ndiaye",
                phone = "+221 77 999 44 55",
                role = "Directrice Santé & Télémédecine",
                organization = "Clinique Internationale Dakar",
                bio = "Échanges médicaux chiffrés E2EE",
                isVerified = true
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Toolbar inside Scanner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scanner un Contact ADT",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { isFlashOn = !isFlashOn },
                    modifier = Modifier
                        .size(38.dp)
                        .background(if (isFlashOn) AdtPrimary else Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("btn_toggle_flashlight")
                ) {
                    Icon(
                        imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Torche",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onPickFromGallery,
                    modifier = Modifier
                        .size(38.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .testTag("btn_pick_qr_gallery")
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Importer depuis la galerie",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Viewfinder with high-tech corners and animated laser
        Box(
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                .border(2.dp, AdtPrimary.copy(alpha = 0.6f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Viewfinder laser beam
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp)
                    .padding(top = (260 * laserOffsetY).dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                AdtPrimary,
                                Color(0xFF25D366),
                                AdtPrimary,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Center target icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = AdtPrimary.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Cadrez le code QR",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Scannez la carte digitale ou l'écran d'un utilisateur ADT Pro pour l'ajouter instantanément.",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Instant test demo contact cards
        Text(
            text = "OU TESTER UN SCAN INSTANTANÉ (SIMULATION PRO) :",
            color = AdtPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(demoContacts) { contact ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .width(180.dp)
                        .clickable { onScanSuccess(contact) }
                        .testTag("demo_scan_${contact.id}")
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(AdtPrimary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    contact.name.take(1),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Column {
                                Text(
                                    contact.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    contact.role.take(18) + "...",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Scannez",
                                color = AdtPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription = null,
                                tint = AdtPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Manual Input trigger button
        OutlinedButton(
            onClick = onManualInput,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("btn_manual_code_input"),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, Color(0xFF475569))
        ) {
            Text("Saisir manuellement un numéro ou identifiant", color = Color(0xFFCBD5E1), fontSize = 12.sp)
        }
    }
}

/**
 * Tab 2: History of contacts scanned and added via QR codes.
 */
@Composable
private fun QrHistoryTabContent(
    history: List<ContactEntity>,
    onStartChat: (ContactEntity) -> Unit,
    onStartCall: (ContactEntity) -> Unit,
    onOpenScanner: () -> Unit
) {
    if (history.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Aucun contact scanné récemment",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Les contacts ajoutés via QR Code s'afficheront ici avec leurs raccourcis de discussion sécurisée.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onOpenScanner,
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_history_open_scanner")
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ouvrir le Scanner")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(history) { contact ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_item_${contact.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(AdtPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = contact.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = contact.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                text = contact.phone,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (contact.bio.isNotBlank()) {
                                Text(
                                    text = contact.bio,
                                    fontSize = 11.sp,
                                    color = AdtPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { onStartChat(contact) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AdtPrimary.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = AdtPrimary, modifier = Modifier.size(18.dp))
                            }

                            IconButton(
                                onClick = { onStartCall(contact) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(AdtPrimary.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Appel", tint = AdtPrimary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Detailed modal dialog presenting the parsed contact card with quick add, chat, and call triggers.
 */
@Composable
private fun ScannedContactDialog(
    payload: QrContactPayload,
    alreadyInContacts: Boolean,
    onDismiss: () -> Unit,
    onAddToNetwork: (ContactEntity) -> Unit,
    onStartChat: (ContactEntity) -> Unit,
    onStartCall: (ContactEntity) -> Unit
) {
    var isAdded by remember { mutableStateOf(alreadyInContacts) }
    val contactEntity = remember(payload) { payload.toContactEntity() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("dialog_scanned_contact")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = AdtPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (alreadyInContacts) "Contact Déjà Enregistré" else "Nouveau Membre Détecté",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (alreadyInContacts) MaterialTheme.colorScheme.onSurfaceVariant else AdtPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fermer")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Avatar Monogram
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(AdtPrimary, AdtPrimaryDark)),
                            shape = CircleShape
                        )
                        .border(3.dp, AdtPrimary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = payload.name.take(2).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Name & Verified Badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = payload.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Vérifié",
                        tint = AdtPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (payload.role.isNotBlank()) {
                    Text(
                        text = if (payload.organization.isNotBlank()) "${payload.role} • ${payload.organization}" else payload.role,
                        fontSize = 13.sp,
                        color = AdtPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = payload.phone,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (payload.bio.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "« ${payload.bio} »",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Security & Token confirmation chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Protocole ADT Pro certifié • Jeton #${payload.token}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions
                if (!isAdded) {
                    Button(
                        onClick = {
                            isAdded = true
                            onAddToNetwork(contactEntity)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_dialog_add_to_network"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ajouter au Réseau ADT Pro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(AdtPrimary.copy(alpha = 0.12f))
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Membre enregistré dans vos contacts",
                            color = AdtPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onStartChat(contactEntity) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_dialog_start_chat"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Discussion", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    OutlinedButton(
                        onClick = { onStartCall(contactEntity) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_dialog_start_call"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AdtPrimary)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = AdtPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Appel HD", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = AdtPrimary)
                    }
                }
            }
        }
    }
}

/**
 * Manual input dialog when camera is unavailable or code string/phone number is entered.
 * Allows adding a contact by international phone number (with separate dial code)
 * or by pasting an ADT Pro QR code/link.
 */
@Composable
private fun ManualCodeInputDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Numéro avec indicatif, 1: Coller Code QR / Lien
    var rawInputText by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryData.supportedCountries.first()) }
    var enteredDialCode by remember { mutableStateOf(CountryData.supportedCountries.first().dialCode.removePrefix("+")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AdtPrimary)
                Text("Ajouter un contact", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    contentColor = AdtPrimary,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AdtPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Numéro de tél.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Code QR / Lien", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    )
                }

                if (selectedTab == 0) {
                    Text(
                        "Saisissez l'indicatif à part et le numéro de téléphone pour ajouter le contact :",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PhoneWithDialCodeInput(
                        selectedCountry = selectedCountry,
                        enteredDialCode = enteredDialCode,
                        phoneNumber = phoneInput,
                        onCountrySelected = { selectedCountry = it },
                        onDialCodeChanged = { enteredDialCode = it },
                        onPhoneNumberChanged = { phoneInput = it },
                        label = "Numéro de téléphone",
                        placeholder = selectedCountry.exampleNumber
                    )
                } else {
                    Text(
                        "Collez un code QR textuel, un lien d'invitation ou vCard :",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = rawInputText,
                        onValueChange = { rawInputText = it },
                        placeholder = { Text("adtpro://contact?... ou lien") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_manual_qr_code"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdtPrimary
                        ),
                        singleLine = false,
                        maxLines = 4
                    )
                }
            }
        },
        confirmButton = {
            val isValid = if (selectedTab == 0) phoneInput.isNotBlank() else rawInputText.isNotBlank()
            Button(
                onClick = {
                    if (selectedTab == 0) {
                        val rawCode = if (enteredDialCode.isNotBlank()) enteredDialCode.trim().removePrefix("+") else selectedCountry.dialCode.removePrefix("+")
                        val code = "+$rawCode"
                        var cleanPhone = phoneInput.trim().replace(" ", "").replace("-", "")
                        if (cleanPhone.startsWith(rawCode)) {
                            cleanPhone = cleanPhone.removePrefix(rawCode)
                        }
                        onSubmit("$code$cleanPhone")
                    } else {
                        onSubmit(rawInputText.trim())
                    }
                },
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                modifier = Modifier.testTag("btn_submit_manual_code")
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
