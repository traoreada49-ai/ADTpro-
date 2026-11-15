package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.data.datastore.VisibilityOption
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.ui.util.getStringsForLanguage
import com.example.util.BiometricAuthManager
import com.example.util.BiometricStatus
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(viewModel: AdtViewModel) {
    val privacyPrefs by viewModel.privacyPreferences.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val dbSecurityStatus by viewModel.databaseSecurityStatus.collectAsState()
    val strings = remember(appSettings?.language) {
        getStringsForLanguage(appSettings?.language)
    }

    var selectedVisibilityDialogTarget by remember { mutableStateOf<String?>(null) } // "LAST_SEEN", "PROFILE_PHOTO", "GROUPS"
    var showMessageTimerDialog by remember { mutableStateOf(false) }
    var showBlockedContactsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricStatus = remember(context) { BiometricAuthManager.checkBiometricStatus(context) }
    val lockedChatsCount by viewModel.lockedChatsCount.collectAsState()
    var showBiometricTimeoutDialog by remember { mutableStateOf(false) }
    var showEnrollBiometricDialog by remember { mutableStateOf(false) }
    var biometricTestMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = strings.privacySecurity,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Préférences stockées dans DataStore",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_privacy")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("privacy_settings_topbar")
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // Banner E2EE & DataStore Guarantee
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AdtPrimary.copy(alpha = 0.08f)
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
                            color = AdtPrimary.copy(alpha = 0.15f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = AdtPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Confidentialité & Chiffrement Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AdtPrimary
                            )
                            Text(
                                text = "Contrôlez qui peut voir votre statut et vos informations personnelles. Vos choix sont synchronisés en temps réel via Jetpack DataStore.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // SECTION 1: DERNIÈRE PRÉSENCE & EN LIGNE (LAST SEEN & ONLINE)
            item {
                PrivacySectionHeader(title = "QUI PEUT VOIR MES INFORMATIONS")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // Last Seen
                        val lastSeenOption = VisibilityOption.fromCode(privacyPrefs.lastSeenVisibility)
                        PrivacySettingRow(
                            icon = Icons.Default.Schedule,
                            title = "Dernière présence",
                            subtitle = lastSeenOption.titleFr,
                            testTag = "row_last_seen",
                            onClick = { selectedVisibilityDialogTarget = "LAST_SEEN" }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )

                        // Profile Photo Visibility
                        val profilePhotoOption = VisibilityOption.fromCode(privacyPrefs.profilePhotoVisibility)
                        PrivacySettingRow(
                            icon = Icons.Default.AccountCircle,
                            title = "Photo de profil",
                            subtitle = profilePhotoOption.titleFr,
                            testTag = "row_profile_photo",
                            onClick = { selectedVisibilityDialogTarget = "PROFILE_PHOTO" }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )

                        // Online Status
                        PrivacySettingRow(
                            icon = Icons.Default.Circle,
                            title = "Statut En ligne",
                            subtitle = if (privacyPrefs.onlineStatusVisibility == "EVERYONE") "Tout le monde" else "Identique à la dernière présence",
                            testTag = "row_online_status",
                            onClick = { selectedVisibilityDialogTarget = "ONLINE_STATUS" }
                        )
                    }
                }
                Text(
                    text = "Si vous ne partagez pas votre dernière présence, vous ne pourrez pas voir la dernière présence des autres utilisateurs.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // SECTION 2: CONFIRMATIONS DE LECTURE (READ RECEIPTS)
            item {
                PrivacySectionHeader(title = "MESSAGERIE & INTERACTIONS")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (privacyPrefs.readReceipts) AdtAccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = if (privacyPrefs.readReceipts) AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Confirmations de lecture",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (privacyPrefs.readReceipts) "Activé (Coches bleues visibles)" else "Désactivé (Coches discrètes)",
                                        fontSize = 12.sp,
                                        color = if (privacyPrefs.readReceipts) AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = privacyPrefs.readReceipts,
                                onCheckedChange = { viewModel.updateReadReceipts(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = AdtPrimary,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag("read_receipts_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Si vous désactivez les confirmations de lecture, vous ne pourrez pas voir ni envoyer de coches bleues de confirmation de lecture. Les confirmations de lecture sont toujours transmises dans les discussions de groupe.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // SECTION 3: MESSAGES ÉPHÉMÈRES ET GROUPES
            item {
                PrivacySectionHeader(title = "GROUPES & MESSAGES TEMPORAIRES")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        // Default message timer
                        PrivacySettingRow(
                            icon = Icons.Default.Timer,
                            title = "Délai par défaut des messages",
                            subtitle = when (privacyPrefs.defaultMessageTimer) {
                                "24H" -> "24 heures"
                                "7D" -> "7 jours"
                                "90D" -> "90 jours"
                                else -> "Désactivé"
                            },
                            testTag = "row_default_message_timer",
                            onClick = { showMessageTimerDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )

                        // Groups permission
                        val groupsOption = VisibilityOption.fromCode(privacyPrefs.groupsAddPermission)
                        PrivacySettingRow(
                            icon = Icons.Default.Group,
                            title = "Groupes",
                            subtitle = groupsOption.titleFr,
                            testTag = "row_groups_permission",
                            onClick = { selectedVisibilityDialogTarget = "GROUPS" }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )

                        // Blocked contacts
                        PrivacySettingRow(
                            icon = Icons.Default.Block,
                            title = "Contacts bloqués",
                            subtitle = "Aucun contact bloqué",
                            testTag = "row_blocked_contacts",
                            onClick = { showBlockedContactsDialog = true }
                        )
                    }
                }
            }

            // SECTION: VERROUILLAGE BIOMÉTRIQUE (EMPREINTE DIGITALE & RECONNAISSANCE FACIALE)
            item {
                PrivacySectionHeader(title = "VERROUILLAGE BIOMÉTRIQUE (EMPREINTE & VISAGE)")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_biometric_security")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Main Switch Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    color = if (privacyPrefs.isBiometricEnabled) AdtAccentGreen.copy(alpha = 0.15f) else AdtPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = if (privacyPrefs.isBiometricEnabled) AdtAccentGreen else AdtPrimary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Déverrouillage par empreinte / visage",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = BiometricAuthManager.getStatusDescription(context),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = privacyPrefs.isBiometricEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (biometricStatus is BiometricStatus.NoneEnrolled) {
                                            showEnrollBiometricDialog = true
                                        } else {
                                            viewModel.setBiometricLockEnabled(true)
                                        }
                                    } else {
                                        viewModel.setBiometricLockEnabled(false)
                                    }
                                },
                                modifier = Modifier.testTag("switch_biometric_lock")
                            )
                        }

                        if (privacyPrefs.isBiometricEnabled) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                            // Timeout setting
                            val timeoutLabel = when (privacyPrefs.biometricTimeout) {
                                "IMMEDIATELY" -> "Immédiatement"
                                "1_MIN" -> "Après 1 minute"
                                "5_MIN" -> "Après 5 minutes"
                                "15_MIN" -> "Après 15 minutes"
                                "1_HOUR" -> "Après 1 heure"
                                else -> "Immédiatement"
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showBiometricTimeoutDialog = true }
                                    .padding(vertical = 4.dp)
                                    .testTag("row_biometric_timeout"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Verrouillage automatique", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text(timeoutLabel, fontSize = 11.sp, color = AdtAccentGreen, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            }

                            // Notification privacy switch
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(20.dp))
                                    Column {
                                        Text("Masquer les aperçus en notification", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("Protège le contenu quand l'appli est verrouillée", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Switch(
                                    checked = privacyPrefs.biometricHideNotifications,
                                    onCheckedChange = { viewModel.setBiometricHideNotifications(it) },
                                    modifier = Modifier.testTag("switch_biometric_hide_notifs")
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                            // Test & Lock Now Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (activity != null && BiometricAuthManager.isBiometricAvailable(context)) {
                                            BiometricAuthManager.showBiometricPrompt(
                                                activity = activity,
                                                title = "Test biométrique ADT Pro",
                                                subtitle = "Validez pour tester votre capteur d'empreinte",
                                                negativeButtonText = "Annuler",
                                                onSuccess = {
                                                    biometricTestMessage = "Authentification biométrique réussie !"
                                                },
                                                onError = { _, err ->
                                                    biometricTestMessage = "Erreur : $err"
                                                },
                                                onFailed = {
                                                    biometricTestMessage = "Empreinte non reconnue"
                                                }
                                            )
                                        } else {
                                            biometricTestMessage = "Capteur non disponible ou non configuré"
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_test_biometric"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tester le capteur", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { viewModel.lockAppWithBiometric() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_lock_app_now"),
                                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verrouiller", fontSize = 12.sp)
                                }
                            }

                            if (biometricTestMessage != null) {
                                Text(
                                    text = biometricTestMessage ?: "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (biometricTestMessage?.contains("réussie") == true) AdtAccentGreen else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        // Row: Conversations verrouillées (Accès direct & Gestion)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openLockedChatsScreen() }
                                .padding(vertical = 4.dp)
                                .testTag("row_settings_locked_chats"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AdtAccentGreen, modifier = Modifier.size(20.dp))
                                Column {
                                    Text("Conversations verrouillées", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                                    Text(
                                        "$lockedChatsCount conversation(s) protégée(s) par biométrie",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // SECTION: SÉCURITÉ DU CHIFFREMENT DE BOUT EN BOUT (E2EE)
            item {
                PrivacySectionHeader(title = "CHIFFREMENT SIGNAL & EMPREINTES")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openEncryptionKeysScreen() }
                                .testTag("btn_open_encryption_keys_from_privacy"),
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
                                    color = AdtAccentGreen.copy(alpha = 0.15f),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = null,
                                            tint = AdtAccentGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Empreintes de clés E2EE",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Consulter les 60 chiffres & clés de chaque discussion",
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
                                    text = "OUVRIR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // SECTION 4: CHIFFREMENT LOCAL DE LA BASE DE DONNÉES ROOM (AES-256)
            item {
                PrivacySectionHeader(title = "CHIFFREMENT LOCAL ROOM (AES-256)")
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
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
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.EnhancedEncryption,
                                            contentDescription = null,
                                            tint = AdtAccentGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Chiffrement AES-256-GCM local",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Protection de la base SQLite interne",
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
                                    text = "ACTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Text(
                            text = "Un niveau supplémentaire de chiffrement AES-256 avec clé 256-bit générée localement et balise d'authentification AEAD protège l'ensemble des messages et aperçus dans la base de données Room. Les messages restent illisibles en cas d'extraction brute du système de fichiers.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Couverture des messages",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dbSecurityStatus.statusDescription,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AdtAccentGreen
                                )
                            }

                            Button(
                                onClick = { viewModel.forceEncryptLocalDatabase() },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Vérifier", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // DataStore State Debug / Summary Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Storage, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "État Jetpack DataStore (Preferences)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = AdtPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "• read_receipts: ${privacyPrefs.readReceipts}\n• profile_photo: ${privacyPrefs.profilePhotoVisibility}\n• last_seen: ${privacyPrefs.lastSeenVisibility}\n• online_status: ${privacyPrefs.onlineStatusVisibility}\n• default_timer: ${privacyPrefs.defaultMessageTimer}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // VISIBILITY SELECTION DIALOG
    if (selectedVisibilityDialogTarget != null) {
        val target = selectedVisibilityDialogTarget!!
        val dialogTitle = when (target) {
            "LAST_SEEN" -> "Qui peut voir ma dernière présence"
            "PROFILE_PHOTO" -> "Qui peut voir ma photo de profil"
            "GROUPS" -> "Qui peut m'ajouter aux groupes"
            else -> "Qui peut voir mon statut En ligne"
        }

        val currentValue = when (target) {
            "LAST_SEEN" -> privacyPrefs.lastSeenVisibility
            "PROFILE_PHOTO" -> privacyPrefs.profilePhotoVisibility
            "GROUPS" -> privacyPrefs.groupsAddPermission
            else -> privacyPrefs.onlineStatusVisibility
        }

        AlertDialog(
            onDismissRequest = { selectedVisibilityDialogTarget = null },
            title = {
                Text(text = dialogTitle, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (target == "ONLINE_STATUS") {
                        // Options for online status
                        val onlineOptions = listOf(
                            "EVERYONE" to "Tout le monde",
                            "SAME_AS_LAST_SEEN" to "Identique à la dernière présence"
                        )
                        onlineOptions.forEach { (code, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateOnlineStatusVisibility(code)
                                        selectedVisibilityDialogTarget = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(label, fontSize = 14.sp, fontWeight = if (currentValue == code) FontWeight.Bold else FontWeight.Normal)
                                RadioButton(
                                    selected = currentValue == code,
                                    onClick = {
                                        viewModel.updateOnlineStatusVisibility(code)
                                        selectedVisibilityDialogTarget = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AdtPrimary)
                                )
                            }
                        }
                    } else {
                        VisibilityOption.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (target) {
                                            "LAST_SEEN" -> viewModel.updateLastSeenVisibility(option.code)
                                            "PROFILE_PHOTO" -> viewModel.updateProfilePhotoVisibility(option.code)
                                            "GROUPS" -> viewModel.updateGroupsAddPermission(option.code)
                                        }
                                        selectedVisibilityDialogTarget = null
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.titleFr,
                                        fontSize = 14.sp,
                                        fontWeight = if (currentValue == option.code) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = option.descFr,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = currentValue == option.code,
                                    onClick = {
                                        when (target) {
                                            "LAST_SEEN" -> viewModel.updateLastSeenVisibility(option.code)
                                            "PROFILE_PHOTO" -> viewModel.updateProfilePhotoVisibility(option.code)
                                            "GROUPS" -> viewModel.updateGroupsAddPermission(option.code)
                                        }
                                        selectedVisibilityDialogTarget = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = AdtPrimary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedVisibilityDialogTarget = null }) {
                    Text("Fermer", color = AdtPrimary)
                }
            }
        )
    }

    // DISAPPEARING MESSAGES TIMER DIALOG
    if (showMessageTimerDialog) {
        val timerOptions = listOf(
            "OFF" to "Désactivé (Conserver toujours)",
            "24H" to "24 heures",
            "7D" to "7 jours",
            "90D" to "90 jours"
        )
        AlertDialog(
            onDismissRequest = { showMessageTimerDialog = false },
            title = {
                Text("Délai par défaut des messages", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Les nouveaux messages dans les nouvelles discussions disparaîtront automatiquement après la durée choisie.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    timerOptions.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultMessageTimer(code)
                                    showMessageTimerDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 14.sp, fontWeight = if (privacyPrefs.defaultMessageTimer == code) FontWeight.Bold else FontWeight.Normal)
                            RadioButton(
                                selected = privacyPrefs.defaultMessageTimer == code,
                                onClick = {
                                    viewModel.updateDefaultMessageTimer(code)
                                    showMessageTimerDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AdtPrimary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMessageTimerDialog = false }) {
                    Text("Fermer", color = AdtPrimary)
                }
            }
        )
    }

    // BLOCKED CONTACTS DIALOG
    if (showBlockedContactsDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedContactsDialog = false },
            icon = { Icon(Icons.Default.Block, contentDescription = null, tint = AdtPrimary) },
            title = { Text("Contacts bloqués", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Aucun contact bloqué pour le moment. Les contacts bloqués ne peuvent plus vous appeler ni vous envoyer de messages.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showBlockedContactsDialog = false }) {
                    Text("OK", color = AdtPrimary)
                }
            }
        )
    }

    // BIOMETRIC TIMEOUT SELECTION DIALOG
    if (showBiometricTimeoutDialog) {
        val timeoutOptions = listOf(
            "IMMEDIATELY" to "Immédiatement (dès la fermeture)",
            "1_MIN" to "Après 1 minute en arrière-plan",
            "5_MIN" to "Après 5 minutes en arrière-plan",
            "15_MIN" to "Après 15 minutes en arrière-plan",
            "1_HOUR" to "Après 1 heure en arrière-plan"
        )
        AlertDialog(
            onDismissRequest = { showBiometricTimeoutDialog = false },
            icon = { Icon(Icons.Default.Timer, contentDescription = null, tint = AdtPrimary) },
            title = { Text("Délai de verrouillage automatique", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeoutOptions.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setBiometricLockTimeout(code)
                                    showBiometricTimeoutDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            RadioButton(
                                selected = privacyPrefs.biometricTimeout == code,
                                onClick = {
                                    viewModel.setBiometricLockTimeout(code)
                                    showBiometricTimeoutDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = AdtPrimary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBiometricTimeoutDialog = false }) {
                    Text("Fermer", color = AdtPrimary)
                }
            }
        )
    }

    // BIOMETRIC ENROLL GUIDANCE DIALOG
    if (showEnrollBiometricDialog) {
        AlertDialog(
            onDismissRequest = { showEnrollBiometricDialog = false },
            icon = { Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AdtPrimary) },
            title = { Text("Configurer l'empreinte digitale", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Aucune empreinte digitale ni reconnaissance faciale n'est encore enregistrée sur cet appareil. Pour activer cette protection dans ADT Pro, configurez d'abord la biométrie dans les paramètres de sécurité Android.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEnrollBiometricDialog = false
                        BiometricAuthManager.openBiometricEnrollSettings(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Ouvrir les Paramètres")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEnrollBiometricDialog = false }) {
                    Text("Plus tard")
                }
            }
        )
    }
}

@Composable
fun PrivacySectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = AdtPrimary,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 4.dp)
    )
}

@Composable
fun PrivacySettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AdtPrimary,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
