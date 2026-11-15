package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.BackupMetadata
import com.example.data.firebase.BackupOperationState
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.ui.util.getStringsForLanguage
import com.example.viewmodel.AdtViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatBackupScreen(viewModel: AdtViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val backupState by viewModel.backupOperationState.collectAsState()
    val remoteBackupInfo by viewModel.latestBackupInfo.collectAsState()
    val dbSecurityStatus by viewModel.databaseSecurityStatus.collectAsState()
    val bgBackupStatus by viewModel.backgroundBackupStatus.collectAsState()
    val syncEnergyMode by viewModel.syncEnergyMode.collectAsState()

    val strings = remember(appSettings?.language) {
        getStringsForLanguage(appSettings?.language)
    }

    var showFrequencyDialog by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    val isProcessing = backupState is BackupOperationState.BackingUp || backupState is BackupOperationState.Restoring

    // Refresh remote metadata on enter
    LaunchedEffect(currentUser?.phone) {
        if (!currentUser?.phone.isNullOrBlank()) {
            viewModel.queryRemoteBackup(currentUser?.phone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sauvegarde des discussions",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Firebase Storage • Chiffrement AES-256",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("btn_back_backup")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour aux paramètres")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HERO STATUS CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(AdtPrimary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dernière sauvegarde Cloud",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = if (remoteBackupInfo != null && remoteBackupInfo!!.timestamp > 0) {
                                        remoteBackupInfo!!.formattedDate
                                    } else if ((appSettings?.lastBackupTimestamp ?: 0L) > 0L) {
                                        val sdf = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.FRENCH)
                                        sdf.format(java.util.Date(appSettings!!.lastBackupTimestamp))
                                    } else {
                                        "Aucune sauvegarde enregistrée"
                                    },
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // E2EE Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AdtAccentGreen.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AdtAccentGreen.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = AdtAccentGreen,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "AES-256",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdtAccentGreen
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Taille de l'archive",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (remoteBackupInfo != null && remoteBackupInfo!!.sizeBytes > 0) {
                                        remoteBackupInfo!!.formattedSize
                                    } else if (!appSettings?.lastBackupSizeFormatted.isNullOrBlank()) {
                                        appSettings!!.lastBackupSizeFormatted
                                    } else {
                                        "0 Ko"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Messages chiffrés",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (remoteBackupInfo != null && remoteBackupInfo!!.messagesCount > 0) {
                                        "${remoteBackupInfo!!.messagesCount} messages"
                                    } else if ((appSettings?.lastBackupMessagesCount ?: 0) > 0) {
                                        "${appSettings!!.lastBackupMessagesCount} messages"
                                    } else {
                                        "Non calculé"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }

                            Column {
                                Text(
                                    text = "Destination",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Firebase Storage",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = AdtPrimary
                                )
                            }
                        }
                    }
                }
            }

            // LOCAL ROOM DATABASE ENCRYPTION STATUS CARD
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    color = AdtAccentGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = AdtAccentGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Chiffrement local Room",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Protection SQLite au repos sur l'appareil",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AES-256-GCM",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Tous les messages et discussions stockés dans la base de données Room de votre smartphone sont scellés par chiffrement AES-256 avec authentification AEAD pour empêcher toute lecture non autorisée ou extraction physique.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Statut : ${dbSecurityStatus.statusDescription}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = AdtAccentGreen
                            )
                            TextButton(
                                onClick = {
                                    viewModel.forceEncryptLocalDatabase()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp), tint = AdtPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vérifier intégrité", fontSize = 11.sp, color = AdtPrimary)
                            }
                        }
                    }
                }
            }

            // OPERATION IN PROGRESS CARD
            item {
                AnimatedVisibility(
                    visible = isProcessing,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = AdtPrimary.copy(alpha = 0.12f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val (progress, message) = when (val s = backupState) {
                                is BackupOperationState.BackingUp -> s.progress to s.stage
                                is BackupOperationState.Restoring -> s.progress to s.stage
                                else -> 0f to ""
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = AdtPrimary
                                    )
                                    Text(
                                        text = if (backupState is BackupOperationState.BackingUp) "Sauvegarde en cours..." else "Restauration en cours...",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = AdtPrimary
                                    )
                                }
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = AdtPrimary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = AdtPrimary,
                                trackColor = AdtPrimary.copy(alpha = 0.2f)
                            )

                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // SUCCESS OR ERROR BANNER
            item {
                when (val state = backupState) {
                    is BackupOperationState.Success -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AdtAccentGreen.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdtAccentGreen.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AdtAccentGreen)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Opération réussie", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AdtAccentGreen)
                                    Text(state.message, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { viewModel.resetBackupOperationState() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    is BackupOperationState.Error -> {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Erreur", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                    Text(state.errorMessage, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                                IconButton(onClick = { viewModel.resetBackupOperationState() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    else -> Unit
                }
            }

            // ACTION BUTTONS (Sauvegarder & Restaurer)
            item {
                Text(
                    text = "Actions de sauvegarde et restauration",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdtPrimary
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.performManualBackup() },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_backup_now"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Sauvegarder maintenant sur Firebase",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { showRestoreConfirmDialog = true },
                            enabled = !isProcessing,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_restore_backup"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Restaurer les discussions",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // BACKGROUND SERVICE CARD (Service de synchronisation et sauvegarde en arrière-plan)
            item {
                Text(
                    text = "Service en arrière-plan (Synchronisation)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdtPrimary
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    color = if (bgBackupStatus.isRunning) AdtPrimary.copy(alpha = 0.2f) else AdtAccentGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (bgBackupStatus.isRunning) Icons.Default.Sync else Icons.Default.CloudSync,
                                            contentDescription = null,
                                            tint = if (bgBackupStatus.isRunning) AdtPrimary else AdtAccentGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Synchronisation automatique",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Foreground Service & JobScheduler",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (bgBackupStatus.isRunning) AdtPrimary.copy(alpha = 0.15f) else AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (bgBackupStatus.isRunning) "EN COURS" else "ACTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bgBackupStatus.isRunning) AdtPrimary else AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Le service en arrière-plan s'exécute de façon autonome sur votre appareil pour chiffrer (AES-256) et synchroniser l'historique de vos discussions vers Firebase Storage, même lorsque l'application est en arrière-plan ou fermée.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (syncEnergyMode == com.example.data.model.SyncEnergyMode.HIGH_PERFORMANCE) AdtPrimary.copy(alpha = 0.12f) else Color(0xFF25D366).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (syncEnergyMode == com.example.data.model.SyncEnergyMode.HIGH_PERFORMANCE) Icons.Default.Bolt else Icons.Default.BatterySaver,
                                    contentDescription = null,
                                    tint = if (syncEnergyMode == com.example.data.model.SyncEnergyMode.HIGH_PERFORMANCE) AdtPrimary else Color(0xFF25D366),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Profil synchronisation : ${syncEnergyMode.label} (${syncEnergyMode.estimatedSavings})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Progress when running
                        if (bgBackupStatus.isRunning) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = bgBackupStatus.stage,
                                        fontSize = 12.sp,
                                        color = AdtPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${(bgBackupStatus.progress * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AdtPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { bgBackupStatus.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = AdtPrimary,
                                    trackColor = AdtPrimary.copy(alpha = 0.2f)
                                )
                            }
                        }

                        // Status Info row
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Dernier état :",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = bgBackupStatus.lastSyncResult.ifBlank { "Prêt pour la synchronisation" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (bgBackupStatus.isSuccess) AdtAccentGreen else MaterialTheme.colorScheme.error
                                    )
                                }

                                if (bgBackupStatus.lastSyncTimestamp > 0L) {
                                    val sdf = remember { java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault()) }
                                    Text(
                                        text = sdf.format(java.util.Date(bgBackupStatus.lastSyncTimestamp)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Background Service Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (bgBackupStatus.isRunning) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelBackgroundBackup() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_stop_bg_backup"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Arrêter le service", fontSize = 12.sp)
                                }
                            } else {
                                Button(
                                    onClick = { viewModel.triggerBackgroundBackup() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_trigger_bg_backup"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdtAccentGreen)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        "Lancer le service en arrière-plan",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // AUTOMATIC BACKUP CONFIGURATION
            item {
                Text(
                    text = "Paramètres de sauvegarde automatique",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AdtPrimary
                )

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Frequency Selector Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFrequencyDialog = true }
                                .testTag("btn_frequency_selector")
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Update, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(
                                        text = "Fréquence automatique",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    val freqLabel = when (appSettings?.autoBackupFrequency) {
                                        "DAILY" -> "Quotidienne (Tous les jours)"
                                        "WEEKLY" -> "Hebdomadaire (Une fois par semaine)"
                                        "MONTHLY" -> "Mensuelle (Une fois par mois)"
                                        "OFF" -> "Désactivée"
                                        else -> "Quotidienne"
                                    }
                                    Text(
                                        text = freqLabel,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Wi-Fi Only Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(
                                        text = "Sauvegarder uniquement via Wi-Fi",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Évite d'utiliser vos données cellulaires mobiles",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = appSettings?.backupOnlyWifi ?: true,
                                onCheckedChange = { viewModel.updateBackupOnlyWifi(it) },
                                modifier = Modifier.testTag("switch_wifi_only")
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Include Media Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PermMedia, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(
                                        text = "Inclure les fichiers médias",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Sauvegarde les photos et documents (augmente la taille)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = appSettings?.backupIncludeMedia ?: false,
                                onCheckedChange = { viewModel.updateBackupIncludeMedia(it) },
                                modifier = Modifier.testTag("switch_include_media")
                            )
                        }
                    }
                }
            }

            // ENCRYPTION & SECURITY GUARANTEE
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = AdtAccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Chiffrement militaire de bout en bout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Vos sauvegardes sont chiffrées avec la norme AES-256-GCM avant d'être envoyées sur Firebase Storage. Ni Google ni ADT Pro ne peuvent lire vos discussions ou avoir accès à vos fichiers sans votre numéro authentifié et vos clés de session.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Emplacement Cloud : backups/${currentUser?.phone?.filter { it.isDigit() || it == '+' } ?: "user"}/adt_chat_backup_latest.enc",
                                fontSize = 10.sp,
                                color = AdtPrimary,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG: SELECT FREQUENCY
    if (showFrequencyDialog) {
        val frequencies = listOf(
            "DAILY" to "Quotidienne (Tous les jours)",
            "WEEKLY" to "Hebdomadaire (Chaque semaine)",
            "MONTHLY" to "Mensuelle (Chaque mois)",
            "OFF" to "Désactivée (Sauvegarde manuelle uniquement)"
        )
        val currentFreq = appSettings?.autoBackupFrequency ?: "DAILY"

        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = { Text("Fréquence de sauvegarde automatique", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    frequencies.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.updateAutoBackupFrequency(code)
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentFreq == code,
                                onClick = {
                                    viewModel.updateAutoBackupFrequency(code)
                                    showFrequencyDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (currentFreq == code) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFrequencyDialog = false }) {
                    Text("Fermer", color = AdtPrimary)
                }
            }
        )
    }

    // DIALOG: CONFIRM RESTORE
    if (showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.Restore, contentDescription = null, tint = AdtPrimary) },
            title = { Text("Restaurer vos discussions ?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Voulez-vous restaurer vos discussions depuis la sauvegarde Firebase Storage ?",
                        fontSize = 13.sp
                    )
                    if (remoteBackupInfo != null && remoteBackupInfo!!.timestamp > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("📅 Date : ${remoteBackupInfo!!.formattedDate}", fontSize = 12.sp)
                                Text("💾 Taille : ${remoteBackupInfo!!.formattedSize}", fontSize = 12.sp)
                                Text("💬 Messages : ${remoteBackupInfo!!.messagesCount}", fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        text = "Vos messages et contacts locaux seront fusionnés et synchronisés.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestoreConfirmDialog = false
                        viewModel.restoreChatBackup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Restaurer maintenant")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
