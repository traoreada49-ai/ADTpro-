package com.example.ui.screens.contacts

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ContactEntity
import com.example.data.model.CountryData
import com.example.data.model.CountryItem
import com.example.ui.components.AdtAvatar
import com.example.ui.components.PhoneWithDialCodeInput
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.util.ContactManager
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsSyncScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val contacts by viewModel.allContacts.collectAsState()
    val isSyncing by viewModel.isSyncingContacts.collectAsState()
    val syncMessage by viewModel.syncResultMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: À inviter, 1: Tous, 2: Sur ADT Pro
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showDirectInviteDialog by remember { mutableStateOf(false) }
    var showCustomMessageDialog by remember { mutableStateOf(false) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    val selectedContactIds = remember { mutableStateListOf<String>() }

    var customInviteMessage by remember {
        mutableStateOf(
            "Bonjour ! Rejoins-moi sur ADT Pro, la messagerie chiffrée de bout en bout, appels HD et canaux professionnels sécurisés : https://adtpro.app/join"
        )
    }

    var hasPermission by remember {
        mutableStateOf(ContactManager.hasContactsPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        viewModel.syncContactsWithDevice()
    }

    LaunchedEffect(syncMessage) {
        syncMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSyncMessage()
        }
    }

    val userPhone = currentUser?.phone.orEmpty().ifBlank { "+221770000000" }
    val referralLink = "https://adtpro.app/join?ref=${userPhone.filter { it.isDigit() || it == '+' }}"

    val adtUsers = remember(contacts) { contacts.filter { it.isAdtUser } }
    val inviteUsers = remember(contacts) { contacts.filter { !it.isAdtUser } }

    val filteredContacts = remember(contacts, searchQuery, selectedFilter) {
        val baseList = when (selectedFilter) {
            0 -> inviteUsers
            1 -> contacts
            2 -> adtUsers
            else -> inviteUsers
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Synchronisation des Contacts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Inviter des amis sur ADT Pro",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.testTag("btn_back_sync_contacts")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openContactQrCode(initialTab = 0) },
                        modifier = Modifier.testTag("btn_sync_open_qr")
                    ) {
                        Icon(
                            Icons.Default.QrCode2,
                            contentDescription = "Code QR Dynamique",
                            tint = AdtPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.shareGeneralInvite(context, customInviteMessage) },
                        modifier = Modifier.testTag("btn_top_share_invite")
                    ) {
                        Icon(
                            Icons.Default.Share,
                            contentDescription = "Partager lien d'invitation",
                            tint = AdtPrimary
                        )
                    }
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 12.dp),
                            strokeWidth = 2.5.dp,
                            color = AdtPrimary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (ContactManager.hasContactsPermission(context)) {
                                    viewModel.syncContactsWithDevice()
                                } else {
                                    permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                }
                            },
                            modifier = Modifier.testTag("btn_top_sync_refresh")
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Actualiser la synchronisation",
                                tint = AdtPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = isMultiSelectMode && selectedContactIds.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedContactIds.size} ami(s) sélectionné(s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Envoi direct par SMS groupé",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selectedContactIds.clear()
                                    isMultiSelectMode = false
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Annuler", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val selectedContacts = inviteUsers.filter { selectedContactIds.contains(it.id) }
                                    viewModel.inviteMultipleContactsBySms(context, selectedContacts, customInviteMessage)
                                    selectedContactIds.clear()
                                    isMultiSelectMode = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_send_bulk_invite")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inviter (${selectedContactIds.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Dynamic QR Code Quick Access
            item {
                Spacer(modifier = Modifier.height(2.dp))
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdtPrimary.copy(alpha = 0.12f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
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
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Code QR Dynamique Sécurisé",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Partage sans contact avec jeton dynamique anti-usurpation",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = { viewModel.openContactQrCode(initialTab = 0) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_sync_my_qr")
                            ) {
                                Text("Mon QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.openContactQrCode(initialTab = 1) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                modifier = Modifier.testTag("btn_sync_scan_qr")
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scanner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Permission & Privacy Card
            item {
                Spacer(modifier = Modifier.height(2.dp))
                if (!hasPermission) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AdtPrimary,
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Contacts,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Accès au carnet d'adresses",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Autorisez la lecture des contacts pour identifier instantanément vos amis déjà sur ADT Pro et inviter vos proches.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                    modifier = Modifier.testTag("btn_request_contacts_permission")
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Autoriser l'accès", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = AdtAccentGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AdtAccentGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = AdtAccentGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Carnet d'adresses synchronisé",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AdtAccentGreen
                                )
                                Text(
                                    text = "Confidentialité garantie : vos contacts restent sur votre téléphone.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Hero Invitation Link Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        AdtPrimary,
                                        Color(0xFF0F766E),
                                        Color(0xFF042F2E)
                                    )
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Inviter des amis sur ADT Pro",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Lien Direct",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Partagez votre lien d'invitation personnel pour inviter rapidement vos collègues, amis et équipes sur le réseau sécurisé ADT Pro.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )

                            // Referral Link box
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.35f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = referralLink,
                                        color = Color(0xFF80CBC4),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(referralLink))
                                        },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("btn_copy_referral_link")
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copier le lien",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.shareGeneralInvite(context, customInviteMessage) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_invite_intent")
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Partager",
                                        color = AdtPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showQrCodeDialog = true },
                                    border = BorderStroke(1.dp, Color.White),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("btn_show_invite_qrcode")
                                ) {
                                    Icon(
                                        Icons.Default.QrCode2,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("QR Code", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showCustomMessageDialog = true },
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("btn_custom_message_dialog")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Texte", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Sync Metric Highlights (3 summary cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Total Contacts
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = 1 }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${contacts.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Répertoire",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // On ADT Pro
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AdtAccentGreen.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = 2 }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${adtUsers.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AdtAccentGreen
                            )
                            Text(
                                text = "Sur ADT Pro",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdtAccentGreen
                            )
                        }
                    }

                    // To invite
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AdtPrimary.copy(alpha = 0.12f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedFilter = 0 }
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${inviteUsers.size}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = AdtPrimary
                            )
                            Text(
                                text = "À inviter",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdtPrimary
                            )
                        }
                    }
                }
            }

            // Sync Action Bar (Manual sync & Direct phone invite)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (ContactManager.hasContactsPermission(context)) {
                                viewModel.syncContactsWithDevice()
                            } else {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                        },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_sync_contacts_now")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Scan en cours...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Synchroniser maintenant", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = { showDirectInviteDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_open_direct_invite_dialog")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Numéro", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un ami par nom ou numéro...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Effacer")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_search_contacts_sync")
                )
            }

            // Filters & Multi-select Toolbar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = selectedFilter == 0,
                            onClick = { selectedFilter = 0 },
                            label = { Text("À inviter (${inviteUsers.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_filter_to_invite")
                        )
                        FilterChip(
                            selected = selectedFilter == 1,
                            onClick = { selectedFilter = 1 },
                            label = { Text("Tous (${contacts.size})", fontSize = 11.sp) },
                            modifier = Modifier.testTag("chip_filter_all")
                        )
                        FilterChip(
                            selected = selectedFilter == 2,
                            onClick = { selectedFilter = 2 },
                            label = { Text("Sur ADT (${adtUsers.size})", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AdtAccentGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            modifier = Modifier.testTag("chip_filter_registered")
                        )
                    }

                    // Multi-select toggle
                    if (selectedFilter != 2 && inviteUsers.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                isMultiSelectMode = !isMultiSelectMode
                                if (!isMultiSelectMode) {
                                    selectedContactIds.clear()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isMultiSelectMode) "Annuler" else "Sélection",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdtPrimary
                            )
                        }
                    }
                }

                if (isMultiSelectMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${selectedContactIds.size} sélectionné(s)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    selectedContactIds.clear()
                                    filteredContacts.filter { !it.isAdtUser }.forEach {
                                        selectedContactIds.add(it.id)
                                    }
                                }
                            ) {
                                Text("Tout sélectionner", fontSize = 11.sp)
                            }
                            TextButton(onClick = { selectedContactIds.clear() }) {
                                Text("Désélectionner", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Header Section for Contact List
            item {
                Text(
                    text = when (selectedFilter) {
                        0 -> "AMIS À INVITER PAR SMS OU LIEN"
                        1 -> "TOUS LES CONTACTS DU RÉPERTOIRE LOCAL"
                        2 -> "CONTACTS DÉJÀ INSCRITS SUR ADT PRO"
                        else -> "CONTACTS"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Contact Items
            if (filteredContacts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Aucun contact correspondant" else "Aucun contact dans cette catégorie",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showDirectInviteDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                        ) {
                            Text("Inviter par numéro direct", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                items(filteredContacts, key = { it.id }) { contact ->
                    val isSelected = selectedContactIds.contains(contact.id)
                    ContactSyncRowItem(
                        contact = contact,
                        isMultiSelectMode = isMultiSelectMode && !contact.isAdtUser,
                        isSelected = isSelected,
                        onToggleSelect = {
                            if (isSelected) {
                                selectedContactIds.remove(contact.id)
                            } else {
                                selectedContactIds.add(contact.id)
                            }
                        },
                        onInviteSms = {
                            viewModel.inviteContact(context, contact, customInviteMessage)
                        },
                        onInviteShare = {
                            val msg = "Bonjour ${contact.name} ! Rejoins-moi sur ADT Pro, la messagerie chiffrée, appels HD et chaînes sécurisées : https://adtpro.app/join"
                            viewModel.shareGeneralInvite(context, msg)
                        },
                        onOpenChat = {
                            viewModel.openDirectChatWithContact(contact)
                        },
                        onStartVoiceCall = {
                            viewModel.startCall(contact.name, contact.phone, contact.avatarUrl, false)
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // QR Code Dialog
    if (showQrCodeDialog) {
        AlertDialog(
            onDismissRequest = { showQrCodeDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, tint = AdtPrimary)
                    Text("QR Code d'invitation ADT Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdtAvatar(name = currentUser?.name ?: "ADT Pro", sizeDp = 56)
                    Text(
                        text = currentUser?.name ?: "Utilisateur ADT Pro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = userPhone,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, AdtPrimary.copy(alpha = 0.3f)),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.QrCode2,
                                contentDescription = "QR Code d'invitation",
                                tint = AdtPrimary,
                                modifier = Modifier.size(180.dp)
                            )
                        }
                    }

                    Text(
                        text = "Faites scanner ce code à vos amis pour qu'ils rejoignent directement votre réseau ADT Pro.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.shareGeneralInvite(context, customInviteMessage)
                        showQrCodeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Partager le lien")
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrCodeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }

    // Direct Phone Invite Dialog
    if (showDirectInviteDialog) {
        var directPhoneInput by remember { mutableStateOf("") }
        var directNameInput by remember { mutableStateOf("") }
        var inviteSelectedCountry by remember { mutableStateOf(CountryData.supportedCountries.first()) }
        var inviteEnteredDialCode by remember { mutableStateOf(CountryData.supportedCountries.first().dialCode.removePrefix("+")) }

        AlertDialog(
            onDismissRequest = { showDirectInviteDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = AdtPrimary)
                    Text("Inviter par numéro direct", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Saisissez le nom et le numéro avec indicatif à part pour inviter sur ADT Pro :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = directNameInput,
                        onValueChange = { directNameInput = it },
                        label = { Text("Nom complet (optionnel)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_direct_invite_name")
                    )

                    // Phone input with separate country dial code
                    PhoneWithDialCodeInput(
                        selectedCountry = inviteSelectedCountry,
                        enteredDialCode = inviteEnteredDialCode,
                        phoneNumber = directPhoneInput,
                        onCountrySelected = { inviteSelectedCountry = it },
                        onDialCodeChanged = { inviteEnteredDialCode = it },
                        onPhoneNumberChanged = { directPhoneInput = it },
                        label = "Numéro du contact",
                        placeholder = inviteSelectedCountry.exampleNumber
                    )
                }
            },
            confirmButton = {
                val fullPhone = if (directPhoneInput.isNotBlank()) {
                    val rawCode = if (inviteEnteredDialCode.isNotBlank()) inviteEnteredDialCode.trim().removePrefix("+") else inviteSelectedCountry.dialCode.removePrefix("+")
                    val code = "+$rawCode"
                    var digits = directPhoneInput.trim().replace(" ", "").replace("-", "")
                    if (digits.startsWith(rawCode)) {
                        digits = digits.removePrefix(rawCode)
                    }
                    "$code$digits"
                } else ""

                Button(
                    onClick = {
                        if (fullPhone.isNotBlank()) {
                            viewModel.inviteDirectPhone(
                                context = context,
                                phone = fullPhone,
                                recipientName = directNameInput.trim(),
                                customMessage = customInviteMessage
                            )
                            showDirectInviteDialog = false
                        }
                    },
                    enabled = directPhoneInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.testTag("btn_confirm_direct_invite")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Envoyer l'invitation SMS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDirectInviteDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Custom Message Edit Dialog
    if (showCustomMessageDialog) {
        var tempMessage by remember { mutableStateOf(customInviteMessage) }

        AlertDialog(
            onDismissRequest = { showCustomMessageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = AdtPrimary)
                    Text("Personnaliser le message d'invitation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Ce message sera envoyé lors des invitations par SMS et partages :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempMessage,
                        onValueChange = { tempMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .testTag("input_custom_invite_message"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        customInviteMessage = tempMessage
                        showCustomMessageDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomMessageDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun ContactSyncRowItem(
    contact: ContactEntity,
    isMultiSelectMode: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onInviteSms: () -> Unit,
    onInviteShare: () -> Unit,
    onOpenChat: () -> Unit,
    onStartVoiceCall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isMultiSelectMode) {
                    onToggleSelect()
                } else if (contact.isAdtUser) {
                    onOpenChat()
                } else {
                    onInviteSms()
                }
            }
            .padding(vertical = 8.dp)
            .testTag("contact_sync_item_${contact.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (isMultiSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp)
                )
            }

            AdtAvatar(name = contact.name, sizeDp = 44, isOnline = contact.isOnline)

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.isAdtUser) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = AdtAccentGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "ADT Pro",
                                color = AdtAccentGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = contact.phone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!contact.isAdtUser) {
                    Text(
                        text = "Carnet d'adresses local • Prêt à inviter",
                        fontSize = 10.sp,
                        color = AdtPrimary
                    )
                }
            }
        }

        if (contact.isAdtUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onStartVoiceCall,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "Appel vocal",
                        tint = AdtPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Button(
                    onClick = onOpenChat,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtAccentGreen)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Discuter", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (!isMultiSelectMode) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onInviteShare,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("btn_share_contact_${contact.id}")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Partager lien",
                        tint = AdtPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Button(
                    onClick = onInviteSms,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                    modifier = Modifier.testTag("btn_sms_contact_${contact.id}")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
