package com.example.ui.screens.settings

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import com.example.data.model.MediaAutoDownloadRules
import com.example.data.model.MediaDownloadPreset
import com.example.data.model.MediaSyncSchedule
import com.example.data.model.NetworkConnectionType
import com.example.data.model.SchedulePreset
import com.example.data.model.SyncEnergyMode
import com.example.data.model.EphemeralMessageDuration
import com.example.data.model.VideoQualityThrottle
import com.example.data.model.DownloadConcurrencyThrottle
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.AdtAvatar
import com.example.ui.components.OperatorBadge
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.getStringsForLanguage
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AdtViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()
    val dbSecurityStatus by viewModel.databaseSecurityStatus.collectAsState()
    val twoStepState by viewModel.twoStepVerificationState.collectAsState()
    val verifiedFingerprints by viewModel.verifiedChatFingerprints.collectAsState()
    val privacyPrefs by viewModel.privacyPreferences.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val strings = remember(appSettings?.language) {
        getStringsForLanguage(appSettings?.language)
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showWallpaperDialog by remember { mutableStateOf(false) }
    var showSecurityInfoDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showStickersDialog by remember { mutableStateOf(false) }

    val userFullName = remember(currentUser) {
        if (currentUser != null && currentUser!!.firstName.isNotBlank()) {
            "${currentUser!!.firstName} ${currentUser!!.lastName}"
        } else {
            "Utilisateur ADT Pro"
        }
    }

    val currentLang = remember(appSettings?.language) {
        AppLanguage.fromCode(appSettings?.language)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.MAIN_TABS) },
                        modifier = Modifier.testTag("btn_back_settings")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
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
            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            AdtAvatar(name = userFullName, avatarKey = currentUser?.avatarUrl ?: "", sizeDp = 56)
                            Column {
                                Text(userFullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (!currentUser?.bio.isNullOrBlank()) {
                                    Text(currentUser!!.bio, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(currentUser?.phone ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                OperatorBadge(currentUser?.operatorName ?: "Orange")
                            }
                        }
                        Row {
                            IconButton(onClick = { showEditProfileDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modifier le profil", tint = AdtPrimary)
                            }
                            IconButton(onClick = { viewModel.openContactQrCode(initialTab = 0) }) {
                                Icon(Icons.Default.QrCode2, contentDescription = "QR Code ADT Pro", tint = AdtPrimary, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }

            // LANGUAGE SETTINGS (France, Franglais, USA, Russia, etc.)
            item {
                Text(strings.languageSetting, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLanguageDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(currentLang.flag, fontSize = 26.sp)
                                Column {
                                    Text(
                                        "${currentLang.displayName} (${currentLang.countryName})",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        currentLang.description,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick switcher chips
                        Text("Accès rapide aux langues :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AppLanguage.entries) { lang ->
                                FilterChip(
                                    selected = currentLang == lang,
                                    onClick = { viewModel.updateLanguage(lang.code) },
                                    label = {
                                        Text(
                                            "${lang.flag} ${lang.displayName}",
                                            fontSize = 11.sp,
                                            fontWeight = if (currentLang == lang) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AdtPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = AdtPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Theme Settings
            item {
                Text(strings.appearanceTheme, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DarkScheduleSettingsCard(
                            appSettings = appSettings,
                            strings = strings,
                            onUpdateThemeMode = { mode -> viewModel.updateTheme(mode) },
                            onUpdateSchedule = { type, startH, startM, endH, endM ->
                                viewModel.updateDarkSchedule(type, startH, startM, endH, endM)
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Palette de couleurs (Rouge, Bleu, Blanche, Noir, Violet, Vert, Rose)
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text("Couleurs du thème & discussions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Rouge, Bleu, Blanche, Noir, Violet, Vert, Rose (Ross)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(AdtPaletteColor.ALL_COLORS) { palColor ->
                                    val isColorSelected = appSettings?.wallpaperTheme == palColor.wallpaperThemeId
                                    Surface(
                                        onClick = {
                                            viewModel.updateWallpaper(palColor.wallpaperThemeId)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isColorSelected) palColor.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isColorSelected) 2.dp else 1.dp,
                                            color = if (isColorSelected) palColor.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        ),
                                        modifier = Modifier.testTag("color_chip_${palColor.id.lowercase()}")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(palColor.primary)
                                                    .then(
                                                        if (palColor == AdtPaletteColor.BLANCHE) {
                                                            Modifier.border(1.dp, Color.Gray, CircleShape)
                                                        } else Modifier
                                                    )
                                            )
                                            Text(
                                                text = palColor.frenchName,
                                                fontSize = 12.sp,
                                                fontWeight = if (isColorSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isColorSelected) palColor.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Wallpaper picker row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openWallpaperPicker() }
                                .testTag("btn_open_wallpaper_picker"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Wallpaper, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(strings.chatWallpaper, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Fleurs, Arbres, Voitures, Étoiles, Lune, Soleil...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Camera Studio & Video Effects row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openCamera() }
                                .testTag("btn_open_camera_studio"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text("Appareil Photo & Vidéo (Effets)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Citron, Fraisier, Orange, White, Noir & Blanc, Bleu...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Stickers Selection Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStickersDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.EmojiEmotions, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text("Pack d'autocollants actif", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Actuel: " + when (appSettings?.activeStickerPack) {
                                            "FUN" -> "Fun & Casual (Café, Fête, Étoiles)"
                                            "TECH" -> "Sécurité & Crypto (Chiffré, Clé)"
                                            else -> "Professionnel ADT (Affaires, Pro)"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Default Ephemeral Messages Row & Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.HistoryToggleOff, contentDescription = null, tint = AdtPrimary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Messages éphémères par défaut", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Délai d'autodestruction appliqué par défaut aux nouvelles conversations",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // 4 options: 24h, 7j, 90j, Désactivé
                            val currentDefaultTimer = privacyPrefs.defaultMessageTimer
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                EphemeralMessageDuration.values().forEach { duration ->
                                    val isSelected = when (duration) {
                                        EphemeralMessageDuration.OFF -> currentDefaultTimer == "OFF"
                                        EphemeralMessageDuration.HOURS_24 -> currentDefaultTimer == "24H"
                                        EphemeralMessageDuration.DAYS_7 -> currentDefaultTimer == "7D"
                                        EphemeralMessageDuration.DAYS_90 -> currentDefaultTimer == "90D"
                                    }
                                    val chipTag = when (duration) {
                                        EphemeralMessageDuration.HOURS_24 -> "btn_default_ephemeral_24h"
                                        EphemeralMessageDuration.DAYS_7 -> "btn_default_ephemeral_7d"
                                        EphemeralMessageDuration.DAYS_90 -> "btn_default_ephemeral_90d"
                                        EphemeralMessageDuration.OFF -> "btn_default_ephemeral_off"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                val timerCode = when (duration) {
                                                    EphemeralMessageDuration.OFF -> "OFF"
                                                    EphemeralMessageDuration.HOURS_24 -> "24H"
                                                    EphemeralMessageDuration.DAYS_7 -> "7D"
                                                    EphemeralMessageDuration.DAYS_90 -> "90D"
                                                }
                                                viewModel.updateDefaultMessageTimer(timerCode)
                                            }
                                            .testTag(chipTag)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = duration.shortLabel,
                                                fontSize = 12.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Contacts & Invitations
            item {
                Text("Contacts & Invitations", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openSyncContacts() }
                                .testTag("btn_open_contacts_sync_settings"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text("Synchroniser les contacts locaux", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Scannez votre carnet d'adresses et invitez facilement vos amis sur ADT Pro",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.shareGeneralInvite(context) }
                                .testTag("btn_share_invite_from_settings"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text("Partager le lien d'invitation ADT Pro", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Envoyez votre lien par SMS, WhatsApp ou réseaux sociaux",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }

            // Privacy & Security Settings
            item {
                val privacyPrefs by viewModel.privacyPreferences.collectAsState()
                Text(strings.privacySecurity, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.navigateTo(AppScreen.PRIVACY_SETTINGS) }
                                .testTag("btn_open_privacy_settings"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(strings.privacyOptions, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "Dernière présence (${privacyPrefs.lastSeenVisibility}), Photo (${privacyPrefs.profilePhotoVisibility}), Vu (${if (privacyPrefs.readReceipts) "Activé" else "Désactivé"})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Dedicated E2EE Key Fingerprints for Current Chat Sessions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openEncryptionKeysScreen() }
                                .testTag("btn_open_encryption_keys"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AdtAccentGreen)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Clés de chiffrement des discussions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AdtAccentGreen.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Signal E2EE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdtAccentGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Empreintes uniques & numéros de sécurité des sessions",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Two-Step Verification
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openTwoStepVerification() }
                                .testTag("btn_open_two_step_settings"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Vérification en deux étapes", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        if (twoStepState.isEnabled) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AdtAccentGreen.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "ACTIF",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AdtAccentGreen,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (twoStepState.isEnabled) {
                                            "Code PIN actif • Lié au ${twoStepState.recoveryAccountType.displayName}"
                                        } else {
                                            "Sécuriser avec un code PIN et compte Google ou iCloud"
                                        },
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSecurityInfoDialog = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Text(strings.endToEndEncryption, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Audit des clés de sécurité & base chiffrée", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Statistiques d'utilisation (Temps d'écran, Messages, Données consommées)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openUsageStatistics() }
                                .testTag("btn_open_usage_statistics"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.QueryStats, contentDescription = null, tint = AdtPrimary)
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Statistiques d'utilisation", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AdtPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "GRAPHIQUES",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdtPrimary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Temps passé, messages envoyés/reçus & données consommées",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }
                    }
                }
            }

            // Operators Info
            item {
                Text(strings.telecomNetwork, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            strings.networkOptimizationDesc,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item { OperatorBadge("Orange") }
                            item { OperatorBadge("Moov Africa") }
                            item { OperatorBadge("Mauritel") }
                            item { OperatorBadge("Vodacom") }
                            item { OperatorBadge("MTN") }
                            item { OperatorBadge("Camtel") }
                            item { OperatorBadge("Airtel") }
                            item { OperatorBadge("Chinguitel") }
                            item { OperatorBadge("Telecel") }
                        }
                    }
                }
            }

            // Gestion de l'Énergie & Synchronisation (Haute Performance vs Économie d'Énergie)
            item {
                val syncEnergyMode by viewModel.syncEnergyMode.collectAsState()
                val isManualSyncInProgress by viewModel.isManualSyncInProgress.collectAsState()
                val mediaDownloadRules by viewModel.mediaAutoDownloadRules.collectAsState()

                EnergySyncManagementCard(
                    currentMode = syncEnergyMode,
                    isSyncing = isManualSyncInProgress,
                    throttleSummary = mediaDownloadRules.getThrottleSummary(syncEnergyMode == SyncEnergyMode.BATTERY_SAVER),
                    onSelectMode = { viewModel.setSyncEnergyMode(it) },
                    onForceSync = { viewModel.triggerImmediateManualSync() }
                )
            }

            // Téléchargement Automatique des Médias & Économiseur de Données
            item {
                val mediaDownloadRules by viewModel.mediaAutoDownloadRules.collectAsState()
                val syncEnergyMode by viewModel.syncEnergyMode.collectAsState()

                MediaAutoDownloadSettingsCard(
                    rules = mediaDownloadRules,
                    isBatterySaverActive = syncEnergyMode == SyncEnergyMode.BATTERY_SAVER,
                    onSelectPreset = { viewModel.setMediaDownloadPreset(it) },
                    onToggleRule = { type, onWifi, enabled -> viewModel.setMediaAutoDownloadRule(type, onWifi, enabled) },
                    onBatchUpdate = { isWifi, photos, audio, videos, docs -> viewModel.setMediaAutoDownloadRulesBatch(isWifi, photos, audio, videos, docs) },
                    onToggleBatterySaverOverride = { viewModel.setDisableAutoDownloadOnBatterySaver(it) },
                    onToggleRoamingOverride = { viewModel.setDisableAutoDownloadOnRoaming(it) },
                    onSelectNetwork = { viewModel.setSimulatedNetworkConnection(it) },
                    onToggleAutoAdjustThrottles = { viewModel.setAutoAdjustThrottlesWithEnergyMode(it) },
                    onSetMaxFileSizeThrottle = { viewModel.setMaxFileSizeThrottle(it) },
                    onSetVideoQualityThrottle = { viewModel.setVideoQualityThrottle(it) },
                    onSetConcurrencyThrottle = { viewModel.setConcurrencyThrottle(it) },
                    onApplyThrottlePreset = { viewModel.applyThrottlePreset(it) }
                )
            }

            // Plage Horaire de Synchronisation Complète des Médias (Période de Recharge)
            item {
                val mediaSyncSchedule by viewModel.mediaSyncSchedule.collectAsState()
                val isDeviceCharging by viewModel.isDeviceCharging.collectAsState()
                val isFullSyncRunning by viewModel.isFullScheduledSyncRunning.collectAsState()
                val lastFullSyncTime by viewModel.lastFullSyncCompletedTime.collectAsState()
                val mediaRules by viewModel.mediaAutoDownloadRules.collectAsState()

                MediaSyncScheduleCard(
                    schedule = mediaSyncSchedule,
                    isDeviceCharging = isDeviceCharging,
                    isWifi = mediaRules.simulatedNetwork == NetworkConnectionType.WIFI,
                    isSyncRunning = isFullSyncRunning,
                    lastSyncTimestamp = lastFullSyncTime,
                    onToggleEnabled = { viewModel.setMediaSyncScheduleEnabled(it) },
                    onSelectPreset = { viewModel.setMediaSyncSchedulePreset(it) },
                    onChangeTimes = { startH, startM, endH, endM -> viewModel.setMediaSyncScheduleTimes(startH, startM, endH, endM) },
                    onToggleRequireCharging = { viewModel.setMediaSyncScheduleRequireCharging(it) },
                    onToggleRequireWifi = { viewModel.setMediaSyncScheduleRequireWifi(it) },
                    onToggleSimulatedCharging = { viewModel.toggleSimulatedCharging() },
                    onTriggerFullSync = { viewModel.triggerFullMediaSyncNow() }
                )
            }

            // Intelligence Artificielle & Réponses Rapides Firebase AI (Gemini)
            item {
                val quickRepliesEnabled by viewModel.quickRepliesEnabled.collectAsState()
                val quickRepliesTone by viewModel.quickRepliesTone.collectAsState()

                Text("Intelligence Artificielle & Productivité", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(AdtPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Réponses rapides Firebase AI",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Suggestions instantanées propulsées par Gemini",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = quickRepliesEnabled,
                                onCheckedChange = { viewModel.toggleQuickRepliesEnabled(it) },
                                modifier = Modifier.testTag("switch_quick_replies_enabled")
                            )
                        }

                        if (quickRepliesEnabled) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.8.dp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Modèle actif",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AdtPrimary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Gemini 2.5 Flash (Firebase AI SDK)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = AdtPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Ton des suggestions",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val tones = listOf(
                                        "FRIENDLY" to "Chaleureux",
                                        "PROFESSIONAL" to "Professionnel",
                                        "SHORT" to "Court"
                                    )
                                    tones.forEach { (key, label) ->
                                        val selected = quickRepliesTone == key
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (selected) AdtPrimary else MaterialTheme.colorScheme.surface,
                                            border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                            modifier = Modifier
                                                .clickable { viewModel.setQuickRepliesTone(key) }
                                                .testTag("tone_chip_$key")
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 12.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Synthèse Vocale & Accessibilité (Text-to-Speech)
            item {
                val ttsLongMessagesEnabled by viewModel.ttsEnabledForLongMessages.collectAsState()
                val isTtsPlaying by viewModel.isTtsPlaying.collectAsState()
                val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()

                Text("Accessibilité & Synthèse Vocale (TTS)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(AdtPrimary.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.RecordVoiceOver,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Lecture vocale des messages longs",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Affiche un bouton d'écoute audio sur les messages longs",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = ttsLongMessagesEnabled,
                                onCheckedChange = { viewModel.toggleTtsForLongMessages(it) },
                                modifier = Modifier.testTag("switch_tts_long_messages")
                            )
                        }

                        if (ttsLongMessagesEnabled) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.8.dp
                            )

                            // Speech rate selection
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    "Vitesse de lecture vocale",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val speeds = listOf(
                                        0.8f to "0.8x Lent",
                                        1.0f to "1.0x Normal",
                                        1.25f to "1.25x Rapide",
                                        1.5f to "1.5x Très rapide"
                                    )
                                    speeds.forEach { (rate, label) ->
                                        val selected = (ttsSpeechRate == rate)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) AdtPrimary else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                1.dp,
                                                if (selected) AdtPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { viewModel.setTtsSpeechRate(rate) }
                                                .testTag("rate_option_$rate")
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.5.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Test voice action
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isTtsPlaying) "Lecture d'un exemple en cours..." else "Tester la synthèse vocale :",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isTtsPlaying) {
                                    Button(
                                        onClick = { viewModel.stopTts() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_stop_tts_test")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Arrêter", fontSize = 12.sp)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.speakMessage(
                                                "sample_tts_test",
                                                "Bonjour ! Ceci est un exemple de synthèse vocale pour vous faciliter la lecture des messages longs dans ADT Pro."
                                            )
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AdtPrimary),
                                        border = BorderStroke(1.dp, AdtPrimary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_test_tts_speech")
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Écouter un extrait", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Firebase Encrypted Chat Backup on Firebase Storage
            item {
                val remoteBackupInfo by viewModel.latestBackupInfo.collectAsState()
                Text("Sauvegarde & Restauration Cloud", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openChatBackup() }
                                .testTag("btn_open_chat_backup_settings"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(AdtPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(24.dp))
                                }
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Sauvegarde Firebase Storage", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = AdtAccentGreen.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "AES-256",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AdtAccentGreen,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    val backupSummary = if (remoteBackupInfo != null && remoteBackupInfo!!.timestamp > 0) {
                                        "${remoteBackupInfo!!.formattedDate} • ${remoteBackupInfo!!.formattedSize}"
                                    } else if ((appSettings?.lastBackupTimestamp ?: 0L) > 0L) {
                                        "${appSettings!!.lastBackupSizeFormatted} (${appSettings!!.lastBackupMessagesCount} messages)"
                                    } else {
                                        "Aucune sauvegarde effectuée"
                                    }
                                    Text(
                                        backupSummary,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    val freqLabel = when (appSettings?.autoBackupFrequency) {
                                        "DAILY" -> "Quotidienne"
                                        "WEEKLY" -> "Hebdomadaire"
                                        "MONTHLY" -> "Mensuelle"
                                        "OFF" -> "Désactivée"
                                        else -> "Quotidienne"
                                    }
                                    Text(
                                        "Auto: $freqLabel • ${if (appSettings?.backupOnlyWifi == true) "Wi-Fi uniquement" else "Cellulaire + Wi-Fi"}",
                                        fontSize = 11.sp,
                                        color = AdtPrimary
                                    )
                                }
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdtPrimary)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { viewModel.performManualBackup() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_quick_backup_now"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sauvegarder", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.openChatBackup() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_manage_backup"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restaurer / Gérer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Local Conversation Export (JSON / PDF)
            item {
                ConversationExportSettingsSection(viewModel = viewModel)
            }

            // Firebase Cloud Sync Status
            item {
                val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
                Text("Synchronisation Cloud & Firestore", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = when (firestoreSyncStatus) {
                                is com.example.data.firebase.FirestoreSyncStatus.Connected -> Color(0xFF25D366)
                                is com.example.data.firebase.FirestoreSyncStatus.Syncing -> AdtPrimary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Column {
                            Text(
                                "Synchronisation en temps réel",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                when (firestoreSyncStatus) {
                                    is com.example.data.firebase.FirestoreSyncStatus.Connected -> "Connecté à Firebase Firestore • Messages en direct"
                                    is com.example.data.firebase.FirestoreSyncStatus.Syncing -> "Synchronisation des discussions en cours..."
                                    is com.example.data.firebase.FirestoreSyncStatus.Error -> "Mode local persistant (hors-ligne)"
                                    else -> "Prêt pour la synchronisation"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Local Room Database AES-256 Encryption
            item {
                Text("Sécurité de la Base de Données Room", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AdtAccentGreen
                                )
                                Column {
                                    Text(
                                        "Chiffrement local AES-256-GCM",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        "Messages stockés scellés sur l'appareil",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdtAccentGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    "ACTIF",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtAccentGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = dbSecurityStatus.statusDescription,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Firebase Phone Account & Logout
            item {
                Text("Compte & Authentification", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AdtPrimary)
                            Column {
                                Text(
                                    "Compte vérifié par Firebase",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    currentUser?.phone ?: "Non configuré",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.logoutUser()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_logout_account"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Se déconnecter / Changer de numéro", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // About & App Version with Logo
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon),
                            contentDescription = "Icône ADT Pro",
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        Text("ADT Pro", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Version 2.4.0 • Sécurisé & Chiffré", fontSize = 11.sp, color = AdtPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Messagerie instantanée d'entreprise, appels voix/vidéo HD et canaux de diffusion sécurisés.",
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Language Selection Dialog (France, Franglais, USA, Russia, España, Arabia)
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, tint = AdtPrimary)
                    Text(strings.selectLanguageTitle, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val isSelected = currentLang == lang
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AdtPrimary.copy(alpha = 0.15f) else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLanguage(lang.code)
                                    showLanguageDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(lang.flag, fontSize = 24.sp)
                                    Column {
                                        Text(
                                            "${lang.displayName} (${lang.countryName})",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            lang.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AdtPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("OK", fontWeight = FontWeight.Bold, color = AdtPrimary)
                }
            }
        )
    }

    // Wallpaper Dialog
    if (showWallpaperDialog) {
        val wallpapers = listOf(
            "EMERALD" to "Émeraude ADT Pro",
            "DOODLE" to "Doodle Graphique",
            "MIDNIGHT" to "Minuit Sombre",
            "SUNSET" to "Coucher de Soleil",
            "SAPPHIRE" to "Saphir Bleu"
        )
        AlertDialog(
            onDismissRequest = { showWallpaperDialog = false },
            title = { Text(strings.chatWallpaper, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    wallpapers.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateWallpaper(key)
                                    showWallpaperDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (appSettings?.wallpaperTheme == key) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AdtPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWallpaperDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var dialogFirstName by remember { mutableStateOf(currentUser?.firstName ?: "") }
        var dialogLastName by remember { mutableStateOf(currentUser?.lastName ?: "") }
        var dialogBio by remember { mutableStateOf(currentUser?.bio ?: "Disponible sur ADT Pro") }
        var dialogBusinessCategory by remember { mutableStateOf(currentUser?.businessCategory ?: "Professionnel ADT") }
        var dialogAvatarUrl by remember { mutableStateOf(currentUser?.avatarUrl ?: "") }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                dialogAvatarUrl = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Modifier votre profil ADT Pro", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                AdtAvatar(
                                    name = if (dialogFirstName.isNotBlank()) dialogFirstName else "U",
                                    avatarKey = dialogAvatarUrl,
                                    sizeDp = 80
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = AdtPrimary,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { imagePickerLauncher.launch("image/*") }
                                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Changer la photo",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                                Text("Sélectionner ou importer une photo", fontSize = 12.sp, color = AdtPrimary)
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = dialogFirstName,
                            onValueChange = { dialogFirstName = it },
                            label = { Text("Prénom") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = dialogLastName,
                            onValueChange = { dialogLastName = it },
                            label = { Text("Nom") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = dialogBio,
                            onValueChange = { dialogBio = it },
                            label = { Text("Activité / Statut") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        val categories = listOf(
                            "Professionnel ADT",
                            "Secteur Télécoms",
                            "Services Financiers",
                            "Distribution / Commerce",
                            "Santé / Pro",
                            "Administration"
                        )
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dialogBusinessCategory,
                                onValueChange = {},
                                label = { Text("Secteur d'activité") },
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { expanded = !expanded }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded }
                            )
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                categories.forEach { category ->
                                    DropdownMenuItem(
                                        text = { Text(category) },
                                        onClick = {
                                            dialogBusinessCategory = category
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateUserProfile(
                            firstName = dialogFirstName.trim(),
                            lastName = dialogLastName.trim(),
                            bio = dialogBio.trim(),
                            businessCategory = dialogBusinessCategory,
                            avatarUrl = dialogAvatarUrl
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold, color = AdtPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Sticker Selection Dialog
    if (showStickersDialog) {
        val packs = listOf(
            "PRO" to "Professionnel ADT pro (Affaires, Pro)",
            "FUN" to "Fun & Casual (Café, Fête, Étoiles, Pause)",
            "TECH" to "Sécurité & Crypto (Données, Clé, Chiffrement)"
        )
        AlertDialog(
            onDismissRequest = { showStickersDialog = false },
            title = { Text("Pack d'autocollants actif", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    packs.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateStickerPack(key)
                                    showStickersDialog = false
                                }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            if (appSettings?.activeStickerPack == key) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AdtPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStickersDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // Privacy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text(strings.privacyOptions, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Dernière présence & En ligne : Mes contacts", fontSize = 13.sp)
                    Text("Photo de profil : Tout le monde", fontSize = 13.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Confirmations de lecture (Vu)", fontSize = 13.sp)
                        Switch(
                            checked = appSettings?.readReceipts ?: true,
                            onCheckedChange = { viewModel.updatePrivacySettings("EVERYONE", "EVERYONE", it) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Enregistrer")
                }
            }
        )
    }

    // Security Encryption Dialog
    if (showSecurityInfoDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityInfoDialog = false },
            title = { Text(strings.endToEndEncryption, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        strings.encryptedNotice,
                        fontSize = 13.sp
                    )
                    Text(
                        "Clé de sécurité de votre compte :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "8492-3104-9912-7481-0239-ADT-PRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityInfoDialog = false }) {
                    Text("Compris")
                }
            }
        )
    }

    // QR Code Dialog
    if (showQrCodeDialog) {
        AlertDialog(
            onDismissRequest = { showQrCodeDialog = false },
            title = { Text(strings.qrCodeTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AdtAvatar(name = userFullName, sizeDp = 64)
                    Text(userFullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(currentUser?.phone ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(
                        Icons.Default.QrCode2,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(160.dp),
                        tint = AdtPrimary
                    )
                    Text("Scannez ce QR Code pour ajouter ce contact sur ADT Pro", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = { showQrCodeDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

/**
 * Energy & Sync Management Card (High Performance vs Battery Saver)
 */
@Composable
fun EnergySyncManagementCard(
    currentMode: SyncEnergyMode,
    isSyncing: Boolean,
    throttleSummary: String = "",
    onSelectMode: (SyncEnergyMode) -> Unit,
    onForceSync: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_angle"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Gestion de l'Énergie & Synchronisation",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AdtPrimary
            )
            // Real-time diagnostic badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (currentMode == SyncEnergyMode.BATTERY_SAVER) Color(0xFF25D366).copy(alpha = 0.15f) else AdtPrimary.copy(alpha = 0.15f)
            ) {
                Text(
                    text = currentMode.estimatedBatteryGain,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentMode == SyncEnergyMode.BATTERY_SAVER) Color(0xFF25D366) else AdtPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Two mode options side by side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // High Performance Option
                    val isHighPerf = currentMode == SyncEnergyMode.HIGH_PERFORMANCE
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHighPerf) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isHighPerf) 2.dp else 1.dp,
                            if (isHighPerf) AdtPrimary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectMode(SyncEnergyMode.HIGH_PERFORMANCE) }
                            .testTag("sync_mode_high_perf_btn")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⚡", fontSize = 18.sp)
                                Text(
                                    "Haute Perf.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isHighPerf) AdtPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "Temps réel direct, latence 0s, médias immédiats",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Battery Saver Option
                    val isBatterySaver = currentMode == SyncEnergyMode.BATTERY_SAVER
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isBatterySaver) Color(0xFF25D366).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            if (isBatterySaver) 2.dp else 1.dp,
                            if (isBatterySaver) Color(0xFF25D366) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectMode(SyncEnergyMode.BATTERY_SAVER) }
                            .testTag("sync_mode_battery_saver_btn")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("🔋", fontSize = 18.sp)
                                Text(
                                    "Économiseur",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isBatterySaver) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "-60% radio, cycles 15 min, +2h à +4h d'autonomie",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                // Comparative Details Box
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Politique active : ${currentMode.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = currentMode.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        if (throttleSummary.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(if (currentMode == SyncEnergyMode.BATTERY_SAVER) "🔋" else "⚡", fontSize = 12.sp)
                                Text(
                                    text = "Bridage médias auto : $throttleSummary",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (currentMode == SyncEnergyMode.BATTERY_SAVER) Color(0xFF25D366) else AdtPrimary
                                )
                            }
                        }
                    }
                }

                // Force Manual Sync Button
                OutlinedButton(
                    onClick = onForceSync,
                    enabled = !isSyncing,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_manual_force_sync")
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Synchroniser maintenant",
                        modifier = Modifier
                            .size(16.dp)
                            .then(if (isSyncing) Modifier.rotate(rotation) else Modifier)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncing) "Synchronisation en cours..." else "Forcer la synchronisation manuelle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Carte de configuration complète du téléchargement automatique des médias
 * (Wi-Fi, Données mobiles, Roaming, Économie d'énergie).
 */
/**
 * Carte de configuration complète du téléchargement automatique des médias
 * permettant de contrôler finement les photos, vidéos, documents et audios
 * sur données mobiles et Wi-Fi pour préserver la batterie et le forfait.
 */
@Composable
fun MediaAutoDownloadSettingsCard(
    rules: MediaAutoDownloadRules,
    isBatterySaverActive: Boolean,
    onSelectPreset: (MediaDownloadPreset) -> Unit,
    onToggleRule: (mediaType: String, onWifi: Boolean, enabled: Boolean) -> Unit,
    onBatchUpdate: (isWifi: Boolean, photos: Boolean, audio: Boolean, videos: Boolean, docs: Boolean) -> Unit,
    onToggleBatterySaverOverride: (Boolean) -> Unit,
    onToggleRoamingOverride: (Boolean) -> Unit,
    onSelectNetwork: (NetworkConnectionType) -> Unit,
    onToggleAutoAdjustThrottles: (Boolean) -> Unit = {},
    onSetMaxFileSizeThrottle: (Int) -> Unit = {},
    onSetVideoQualityThrottle: (VideoQualityThrottle) -> Unit = {},
    onSetConcurrencyThrottle: (DownloadConcurrencyThrottle) -> Unit = {},
    onApplyThrottlePreset: (Boolean) -> Unit = {}
) {
    var showMobileDialog by remember { mutableStateOf(false) }
    var showWifiDialog by remember { mutableStateOf(false) }

    Text(
        text = "Utilisation des données & Économie d'énergie",
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
            .testTag("card_media_auto_download")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AdtPrimary.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = AdtPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Téléchargement auto des médias",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Contrôlez les photos, vidéos et documents pour économiser l'énergie",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Savings & Active Policy Diagnostic Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF25D366).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🔋", fontSize = 24.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${rules.dataSavedMb} Mo économisés",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B8748)
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF1B8748).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = rules.getBatteryHoursGained().uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1B8748),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = rules.getEnergyConsumptionLabel(),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val diagnosticText = when {
                            isBatterySaverActive && rules.disableWhenBatterySaverActive ->
                                "🔋 Mode Économiseur actif : Téléchargements auto suspendus pour préserver la batterie"
                            rules.simulatedNetwork == NetworkConnectionType.ROAMING && rules.disableOnRoaming ->
                                "✈️ En itinérance : Médias bloqués pour éviter les surcoûts"
                            rules.preset == MediaDownloadPreset.WIFI_ONLY && rules.simulatedNetwork == NetworkConnectionType.MOBILE_DATA ->
                                "📶 En attente du Wi-Fi : Téléchargement différé sur données mobiles"
                            rules.preset == MediaDownloadPreset.NEVER ->
                                "👆 Manuel au clic : Aucun média lourd n'est préchargé (Éco max)"
                            else ->
                                "✅ Prêt : Règles actives sur ${rules.simulatedNetwork.label}"
                        }
                        Text(
                            text = diagnosticText,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // WhatsApp Style Network Section Cards (clickable to configure batch in dialog)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Téléchargement automatique selon le réseau :",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Mobile Data Tile
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMobileDialog = true }
                        .testTag("btn_open_mobile_download_dialog")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📱", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "En utilisant les données mobiles",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = rules.getMobileSummary(),
                                    fontSize = 11.5.sp,
                                    color = AdtPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Wi-Fi Tile
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showWifiDialog = true }
                        .testTag("btn_open_wifi_download_dialog")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📶", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "En connexion Wi-Fi",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = rules.getWifiSummary(),
                                    fontSize = 11.5.sp,
                                    color = AdtPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Modifier",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            // Fine-grained per-media direct toggle table
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contrôles fins par média & réseau :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("📱 Données", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("📶 Wi-Fi", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Photos & Images
                MediaDownloadRow(
                    title = "Photos & Images",
                    icon = "📷",
                    energyLabel = "Impact modéré • 2.4 Mo",
                    mobileChecked = rules.photosOnMobile,
                    wifiChecked = rules.photosOnWifi,
                    onMobileChange = { onToggleRule("photos", false, it) },
                    onWifiChange = { onToggleRule("photos", true, it) },
                    mobileTag = "switch_photos_mobile",
                    wifiTag = "switch_photos_wifi"
                )

                // Vidéos HD
                MediaDownloadRow(
                    title = "Vidéos HD",
                    icon = "🎥",
                    energyLabel = "⚡ Énergivore • 14.8 Mo",
                    isHeavy = true,
                    mobileChecked = rules.videosOnMobile,
                    wifiChecked = rules.videosOnWifi,
                    onMobileChange = { onToggleRule("videos", false, it) },
                    onWifiChange = { onToggleRule("videos", true, it) },
                    mobileTag = "switch_videos_mobile",
                    wifiTag = "switch_videos_wifi"
                )

                // Documents & PDF
                MediaDownloadRow(
                    title = "Documents & PDF",
                    icon = "📄",
                    energyLabel = "Fichiers de travail • 1.8 Mo",
                    mobileChecked = rules.docsOnMobile,
                    wifiChecked = rules.docsOnWifi,
                    onMobileChange = { onToggleRule("docs", false, it) },
                    onWifiChange = { onToggleRule("docs", true, it) },
                    mobileTag = "switch_docs_mobile",
                    wifiTag = "switch_docs_wifi"
                )

                // Audio & Vocaux
                MediaDownloadRow(
                    title = "Audio & Vocaux",
                    icon = "🎤",
                    energyLabel = "Faible impact • 450 Ko",
                    mobileChecked = rules.audioOnMobile,
                    wifiChecked = rules.audioOnWifi,
                    onMobileChange = { onToggleRule("audio", false, it) },
                    onWifiChange = { onToggleRule("audio", true, it) },
                    mobileTag = "switch_audio_mobile",
                    wifiTag = "switch_audio_wifi"
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            // Preset Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Préréglages d'énergie recommandés :",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Wi-Fi Only
                    val isWifiOnly = rules.preset == MediaDownloadPreset.WIFI_ONLY
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isWifiOnly) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isWifiOnly) 1.5.dp else 1.dp, if (isWifiOnly) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPreset(MediaDownloadPreset.WIFI_ONLY) }
                            .testTag("btn_preset_wifi_only")
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📶 Wi-Fi seul", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isWifiOnly) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                            Text("Éco-énergie", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Battery Saver Off
                    val isBatterySaverPreset = rules.preset == MediaDownloadPreset.BATTERY_SAVER_OFF
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBatterySaverPreset) Color(0xFF25D366).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isBatterySaverPreset) 1.5.dp else 1.dp, if (isBatterySaverPreset) Color(0xFF25D366) else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPreset(MediaDownloadPreset.BATTERY_SAVER_OFF) }
                            .testTag("btn_preset_battery_saver")
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔋 Éco-batterie", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isBatterySaverPreset) Color(0xFF25D366) else MaterialTheme.colorScheme.onSurface)
                            Text("Stop si éco", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Manual Never
                    val isNever = rules.preset == MediaDownloadPreset.NEVER
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isNever) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isNever) 1.5.dp else 1.dp, if (isNever) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPreset(MediaDownloadPreset.NEVER) }
                            .testTag("btn_preset_manual_only")
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👆 Manuel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isNever) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                            Text("Éco max", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // All networks
                    val isAll = rules.preset == MediaDownloadPreset.ALL_NETWORKS
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isAll) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(if (isAll) 1.5.dp else 1.dp, if (isAll) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectPreset(MediaDownloadPreset.ALL_NETWORKS) }
                            .testTag("btn_preset_all")
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("⚡ Tout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isAll) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                            Text("Performance", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Text(
                    text = rules.preset.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            // Global Overrides
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Battery Saver Override
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("🔋", fontSize = 14.sp)
                            Text(
                                "Désactiver en mode Économie d'Énergie",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Suspend automatiquement tout média lourd dès que l'économiseur est actif pour préserver la batterie.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = rules.disableWhenBatterySaverActive,
                        onCheckedChange = onToggleBatterySaverOverride,
                        modifier = Modifier.testTag("switch_disable_on_battery_saver")
                    )
                }

                // Roaming Override
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("✈️", fontSize = 14.sp)
                            Text(
                                "Désactiver en itinérance (Roaming)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            "Bloque les téléchargements à l'étranger pour éviter les facturations et surconsommations imprévues.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = rules.disableOnRoaming,
                        onCheckedChange = onToggleRoamingOverride,
                        modifier = Modifier.testTag("switch_disable_on_roaming")
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            // Network Simulator Switcher
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Tester la connexion active :",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NetworkConnectionType.values().forEach { net ->
                        val isSelected = rules.simulatedNetwork == net
                        val tag = when (net) {
                            NetworkConnectionType.WIFI -> "btn_network_wifi"
                            NetworkConnectionType.MOBILE_DATA -> "btn_network_mobile"
                            NetworkConnectionType.ROAMING -> "btn_network_roaming"
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectNetwork(net) }
                                .testTag(tag)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(net.icon, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when (net) {
                                        NetworkConnectionType.WIFI -> "Wi-Fi"
                                        NetworkConnectionType.MOBILE_DATA -> "Données"
                                        NetworkConnectionType.ROAMING -> "Roaming"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

            // NOUVELLE SECTION : Bridage dynamique du téléchargement (Throttles)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isBatterySaverActive) Color(0xFF25D366).copy(alpha = 0.15f) else AdtPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isBatterySaverActive) "🔋" else "⚡", fontSize = 16.sp)
                        }
                        Column {
                            Text(
                                text = "Bridage des médias (Throttles)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (rules.autoAdjustThrottlesWithEnergyMode) {
                                    if (isBatterySaverActive) "Ajusté auto : Mode Économie d'Énergie actif"
                                    else "Ajusté auto : Mode Haute Performance actif"
                                } else {
                                    "Ajustement manuel personnalisé"
                                },
                                fontSize = 11.sp,
                                color = if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Mode indicator badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isBatterySaverActive) Color(0xFF25D366).copy(alpha = 0.15f) else AdtPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (isBatterySaverActive) Color(0xFF25D366).copy(alpha = 0.4f) else AdtPrimary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = if (isBatterySaverActive) "BRIDÉ" else "DÉBRIDÉ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Switch: Ajustement automatique selon le mode d'énergie
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = "Ajustement automatique selon l'énergie",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Adapte instantanément les plafonds (3 Mo en Éco vs 50 Mo en Haute Perf) pour préserver la batterie.",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 13.sp
                        )
                    }
                    Switch(
                        checked = rules.autoAdjustThrottlesWithEnergyMode,
                        onCheckedChange = onToggleAutoAdjustThrottles,
                        modifier = Modifier.testTag("switch_auto_adjust_throttles")
                    )
                }

                // Summary Pill Banner
                val currentMaxMb = rules.getActiveMaxFileSizeMb(isBatterySaverActive)
                val currentVidQuality = rules.getActiveVideoQuality(isBatterySaverActive)
                val currentConcurrency = rules.getActiveConcurrency(isBatterySaverActive)

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📊", fontSize = 14.sp)
                            Text(
                                text = "Plafonds actifs en cours d'application :",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Taille max", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("$currentMaxMb Mo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Vidéos", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currentVidQuality.badge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AdtPrimary.copy(alpha = 0.12f),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Flux réseau", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(if (currentConcurrency.maxParallel == 1) "1 séquentiel" else "3 parallèle", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)
                                }
                            }
                        }
                    }
                }

                // 1. Configurable Max File Size Throttle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "1. Plafond de taille max auto-téléchargée :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Les fichiers plus lourds nécessitent un clic manuel pour être téléchargés.",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 3, 5, 10, 25, 50).forEach { sizeMb ->
                            val isSelected = currentMaxMb == sizeMb
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetMaxFileSizeThrottle(sizeMb) }
                                    .testTag("btn_throttle_size_${sizeMb}mb")
                            ) {
                                Text(
                                    text = "$sizeMb Mo",
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // 2. Configurable Video Quality Throttle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "2. Qualité vidéo autorisée en arrière-plan :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VideoQualityThrottle.values().forEach { vq ->
                            val isSelected = currentVidQuality == vq
                            val tag = when (vq) {
                                VideoQualityThrottle.THUMBNAIL_ONLY -> "btn_throttle_vid_thumbnail"
                                VideoQualityThrottle.STANDARD_DEF -> "btn_throttle_vid_sd"
                                VideoQualityThrottle.FULL_HD -> "btn_throttle_vid_hd"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetVideoQualityThrottle(vq) }
                                    .testTag(tag)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = vq.badge,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Configurable Concurrency Throttle
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "3. Gestion des flux réseau (Concurrence) :",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DownloadConcurrencyThrottle.values().forEach { conc ->
                            val isSelected = currentConcurrency == conc
                            val tag = if (conc == DownloadConcurrencyThrottle.SEQUENTIAL_ECO) "btn_throttle_conc_sequential" else "btn_throttle_conc_parallel"
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (isSelected) (if (isBatterySaverActive) Color(0xFF25D366) else AdtPrimary) else MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSetConcurrencyThrottle(conc) }
                                    .testTag(tag)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = conc.label,
                                        fontSize = 10.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Dialog for Mobile Data Fine-Grained Auto-Download
    if (showMobileDialog) {
        MediaAutoDownloadConfigDialog(
            title = "En utilisant les données mobiles",
            icon = "📱",
            energyTip = "La suspension des vidéos HD et documents lourds sur réseau cellulaire économise jusqu'à 60% d'énergie radio.",
            photosChecked = rules.photosOnMobile,
            audioChecked = rules.audioOnMobile,
            videosChecked = rules.videosOnMobile,
            docsChecked = rules.docsOnMobile,
            onDismiss = { showMobileDialog = false },
            onConfirm = { photos, audio, videos, docs ->
                onBatchUpdate(false, photos, audio, videos, docs)
                showMobileDialog = false
            }
        )
    }

    // Modal Dialog for Wi-Fi Fine-Grained Auto-Download
    if (showWifiDialog) {
        MediaAutoDownloadConfigDialog(
            title = "En connexion Wi-Fi",
            icon = "📶",
            energyTip = "Le Wi-Fi offre une efficacité énergétique jusqu'à 3x supérieure au réseau cellulaire.",
            photosChecked = rules.photosOnWifi,
            audioChecked = rules.audioOnWifi,
            videosChecked = rules.videosOnWifi,
            docsChecked = rules.docsOnWifi,
            onDismiss = { showWifiDialog = false },
            onConfirm = { photos, audio, videos, docs ->
                onBatchUpdate(true, photos, audio, videos, docs)
                showWifiDialog = false
            }
        )
    }
}

/**
 * Dialogue modal de sélection des types de médias (WhatsApp style).
 */
@Composable
fun MediaAutoDownloadConfigDialog(
    title: String,
    icon: String,
    energyTip: String,
    photosChecked: Boolean,
    audioChecked: Boolean,
    videosChecked: Boolean,
    docsChecked: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (photos: Boolean, audio: Boolean, videos: Boolean, docs: Boolean) -> Unit
) {
    var pChecked by remember { mutableStateOf(photosChecked) }
    var aChecked by remember { mutableStateOf(audioChecked) }
    var vChecked by remember { mutableStateOf(videosChecked) }
    var dChecked by remember { mutableStateOf(docsChecked) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(icon, fontSize = 20.sp)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AdtPrimary.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = energyTip,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Photos checkbox item
                MediaCheckboxRow(
                    label = "Photos & Images",
                    icon = "📷",
                    sizeInfo = "2.4 Mo",
                    checked = pChecked,
                    onCheckedChange = { pChecked = it }
                )

                // Audio checkbox item
                MediaCheckboxRow(
                    label = "Audio & Messages Vocaux",
                    icon = "🎤",
                    sizeInfo = "450 Ko",
                    checked = aChecked,
                    onCheckedChange = { aChecked = it }
                )

                // Videos checkbox item
                MediaCheckboxRow(
                    label = "Vidéos HD (Énergivore)",
                    icon = "🎥",
                    sizeInfo = "14.8 Mo ⚡",
                    checked = vChecked,
                    onCheckedChange = { vChecked = it }
                )

                // Documents checkbox item
                MediaCheckboxRow(
                    label = "Documents & PDF",
                    icon = "📄",
                    sizeInfo = "1.8 Mo",
                    checked = dChecked,
                    onCheckedChange = { dChecked = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pChecked, aChecked, vChecked, dChecked) },
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
            ) {
                Text("Enregistrer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun MediaCheckboxRow(
    label: String,
    icon: String,
    sizeInfo: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
            Text(icon, fontSize = 14.sp)
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(
            text = sizeInfo,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MediaDownloadRow(
    title: String,
    icon: String,
    energyLabel: String = "",
    isHeavy: Boolean = false,
    mobileChecked: Boolean,
    wifiChecked: Boolean,
    onMobileChange: (Boolean) -> Unit,
    onWifiChange: (Boolean) -> Unit,
    mobileTag: String,
    wifiTag: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(icon, fontSize = 16.sp)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        if (isHeavy) {
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = Color(0xFFE65100).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "ÉNERGIVORE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    if (energyLabel.isNotBlank()) {
                        Text(energyLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = mobileChecked,
                    onCheckedChange = onMobileChange,
                    modifier = Modifier.testTag(mobileTag)
                )
                Switch(
                    checked = wifiChecked,
                    onCheckedChange = onWifiChange,
                    modifier = Modifier.testTag(wifiTag)
                )
            }
        }
    }
}

/**
 * Carte de configuration des plages horaires spécifiques pour la synchronisation complète des médias,
 * permettant de privilégier les périodes où l'appareil est en charge.
 */
@Composable
fun MediaSyncScheduleCard(
    schedule: MediaSyncSchedule,
    isDeviceCharging: Boolean,
    isWifi: Boolean,
    isSyncRunning: Boolean,
    lastSyncTimestamp: Long,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (SchedulePreset) -> Unit,
    onChangeTimes: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onToggleRequireCharging: (Boolean) -> Unit,
    onToggleRequireWifi: (Boolean) -> Unit,
    onToggleSimulatedCharging: () -> Unit,
    onTriggerFullSync: () -> Unit
) {
    var showTimeDialog by remember { mutableStateOf(false) }
    var isSelectingStartTime by remember { mutableStateOf(true) }

    val now = remember { Calendar.getInstance() }
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)

    val isAuthorizedNow = schedule.isFullSyncAuthorized(currentHour, currentMinute, isDeviceCharging, isWifi)
    val isInWindow = schedule.isCurrentlyInWindow(currentHour, currentMinute)

    Text(
        text = "Synchronisation Planifiée des Médias & Recharge",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = AdtPrimary
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (schedule.enabled) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (schedule.requireCharging) Icons.Default.BatteryChargingFull else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (schedule.enabled) AdtPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Plage Horaire de Recharge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isAuthorizedNow) Color(0xFF25D366).copy(alpha = 0.2f) else AdtPrimary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isAuthorizedNow) "ACTIVE ⚡" else schedule.getDurationSummary(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAuthorizedNow) Color(0xFF1B8748) else AdtPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Téléchargement complet des photos, vidéos HD et documents pendant la charge",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.5.sp
                        )
                    }
                }
                Switch(
                    checked = schedule.enabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("switch_media_sync_schedule")
                )
            }

            // Real-Time Energy & Charging Status Banner
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = when {
                    !schedule.enabled -> MaterialTheme.colorScheme.surface
                    isAuthorizedNow -> Color(0xFF25D366).copy(alpha = 0.15f)
                    isInWindow && schedule.requireCharging && !isDeviceCharging -> Color(0xFFFF9800).copy(alpha = 0.15f)
                    isInWindow && schedule.requireWifi && !isWifi -> Color(0xFF0288D1).copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surface
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        !schedule.enabled -> MaterialTheme.colorScheme.outlineVariant
                        isAuthorizedNow -> Color(0xFF25D366).copy(alpha = 0.5f)
                        isInWindow && schedule.requireCharging && !isDeviceCharging -> Color(0xFFFF9800).copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    }
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusIcon = when {
                        !schedule.enabled -> "⚪"
                        isAuthorizedNow -> "⚡"
                        isInWindow && schedule.requireCharging && !isDeviceCharging -> "🔌"
                        isInWindow && schedule.requireWifi && !isWifi -> "📶"
                        else -> "🌙"
                    }
                    Text(statusIcon, fontSize = 16.sp)
                    Text(
                        text = schedule.getStatusDescription(currentHour, currentMinute, isDeviceCharging, isWifi),
                        fontSize = 11.5.sp,
                        fontWeight = if (isAuthorizedNow) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            !schedule.enabled -> MaterialTheme.colorScheme.onSurfaceVariant
                            isAuthorizedNow -> Color(0xFF1B8748)
                            isInWindow && schedule.requireCharging && !isDeviceCharging -> Color(0xFFE65100)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        lineHeight = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (schedule.enabled) {
                // Preset Quick Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Préréglages de synchronisation :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Night & Charge
                        val isNight = schedule.preset == SchedulePreset.NIGHT_CHARGE
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isNight) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (isNight) 1.5.dp else 1.dp, if (isNight) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectPreset(SchedulePreset.NIGHT_CHARGE) }
                                .testTag("chip_schedule_night")
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌙 Nuit", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isNight) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                                Text("23h - 7h", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Off-Peak
                        val isOffPeak = schedule.preset == SchedulePreset.OFF_PEAK
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOffPeak) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (isOffPeak) 1.5.dp else 1.dp, if (isOffPeak) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectPreset(SchedulePreset.OFF_PEAK) }
                                .testTag("chip_schedule_off_peak")
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚡ Creuses", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isOffPeak) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                                Text("1h - 6h", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Lunch
                        val isLunch = schedule.preset == SchedulePreset.LUNCH_REST
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isLunch) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (isLunch) 1.5.dp else 1.dp, if (isLunch) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectPreset(SchedulePreset.LUNCH_REST) }
                                .testTag("chip_schedule_lunch")
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("☀️ Midi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isLunch) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                                Text("12h30-14h", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Custom
                        val isCustom = schedule.preset == SchedulePreset.CUSTOM
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isCustom) AdtPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(if (isCustom) 1.5.dp else 1.dp, if (isCustom) AdtPrimary else MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectPreset(SchedulePreset.CUSTOM) }
                                .testTag("chip_schedule_custom")
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚙️ Mesure", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isCustom) AdtPrimary else MaterialTheme.colorScheme.onSurface)
                                Text("Manuel", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(
                        text = schedule.preset.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.5.sp
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                // Interactive Scheduled Window Time Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Créneau horaire de synchronisation :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Start Time Selector
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isSelectingStartTime = true
                                    showTimeDialog = true
                                }
                                .testTag("btn_select_sync_start_time")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Heure de début", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", schedule.startHour, schedule.startMinute),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Jusqu'à",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )

                        // End Time Selector
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    isSelectingStartTime = false
                                    showTimeDialog = true
                                }
                                .testTag("btn_select_sync_end_time")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Heure de fin", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = String.format(Locale.getDefault(), "%02d:%02d", schedule.endHour, schedule.endMinute),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                // Conditions: Charging and Wi-Fi Requirements
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Charging Requirement Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("🔌", fontSize = 14.sp)
                                Text(
                                    "Uniquement sur chargeur secteur",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "Ne déclenche la synchronisation complète que si le téléphone est branché sur son chargeur pour ne pas vider la batterie pendant la nuit.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = schedule.requireCharging,
                            onCheckedChange = onToggleRequireCharging,
                            modifier = Modifier.testTag("switch_require_charging")
                        )
                    }

                    // Wi-Fi Requirement Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("📶", fontSize = 14.sp)
                                Text(
                                    "Uniquement en connexion Wi-Fi",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "Télécharge tous les médias volumineux sur réseau Wi-Fi pour préserver votre forfait mobile.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = schedule.requireWifi,
                            onCheckedChange = onToggleRequireWifi,
                            modifier = Modifier.testTag("switch_require_wifi")
                        )
                    }

                    // Charging State Simulation Button (for testing & emulator)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isDeviceCharging) "⚡" else "🔋", fontSize = 15.sp)
                                Column {
                                    Text(
                                        text = if (isDeviceCharging) "Statut : Appareil en charge" else "Statut : Sur batterie",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isDeviceCharging) Color(0xFF1B8748) else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isDeviceCharging) "Conditions de recharge remplies" else "Branchez le chargeur ou simulez la charge",
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            OutlinedButton(
                                onClick = onToggleSimulatedCharging,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_toggle_simulated_charging")
                            ) {
                                Text(
                                    text = if (isDeviceCharging) "Débrancher" else "Brancher 🔌",
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.8.dp)

                // Force Immediate Full Sync Trigger
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onTriggerFullSync,
                        enabled = !isSyncRunning,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_trigger_full_sync_now")
                    ) {
                        if (isSyncRunning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Synchronisation complète des médias...", fontSize = 12.5.sp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Lancer la synchronisation complète maintenant", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    val lastSyncText = remember(lastSyncTimestamp) {
                        if (lastSyncTimestamp <= 0L) {
                            "Aucune synchronisation complète récente effectuée"
                        } else {
                            val diffMinutes = (System.currentTimeMillis() - lastSyncTimestamp) / 60000
                            when {
                                diffMinutes < 1 -> "Dernière synchronisation réussie : À l'instant"
                                diffMinutes < 60 -> "Dernière synchronisation réussie : Il y a $diffMinutes min"
                                else -> {
                                    val cal = Calendar.getInstance().apply { timeInMillis = lastSyncTimestamp }
                                    String.format(Locale.getDefault(), "Dernière synchronisation : Aujourd'hui à %02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                                }
                            }
                        }
                    }

                    Text(
                        text = lastSyncText,
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    // Modal Time Selector Dialog
    if (showTimeDialog) {
        val initialH = if (isSelectingStartTime) schedule.startHour else schedule.endHour
        val initialM = if (isSelectingStartTime) schedule.startMinute else schedule.endMinute

        ScheduleTimeSelectorDialog(
            isStartTime = isSelectingStartTime,
            initialHour = initialH,
            initialMinute = initialM,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m ->
                if (isSelectingStartTime) {
                    onChangeTimes(h, m, schedule.endHour, schedule.endMinute)
                } else {
                    onChangeTimes(schedule.startHour, schedule.startMinute, h, m)
                }
                showTimeDialog = false
            }
        )
    }
}

/**
 * Dialogue intuitif pour sélectionner l'heure et la minute de la plage de synchronisation.
 */
@Composable
fun ScheduleTimeSelectorDialog(
    isStartTime: Boolean,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = AdtPrimary)
                Text(
                    text = if (isStartTime) "Heure de début de synchronisation" else "Heure de fin de synchronisation",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isStartTime) {
                        "Choisissez l'heure à partir de laquelle le téléchargement complet est autorisé pendant la recharge."
                    } else {
                        "Choisissez l'heure à laquelle la synchronisation nocturne prend fin pour reprendre le mode économe."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )

                // Large Time Display with Controls
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdtPrimary.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Hour Control
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("HEURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedHour = (selectedHour - 1 + 24) % 24 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", selectedHour),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { selectedHour = (selectedHour + 1) % 24 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Text(":", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = AdtPrimary)

                        // Minute Control
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("MINUTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { selectedMinute = (selectedMinute - 15 + 60) % 60 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Moins", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AdtPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp)
                                )
                                IconButton(
                                    onClick = { selectedMinute = (selectedMinute + 15) % 60 },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Plus", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Quick Hour Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Suggestions rapides :", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = if (isStartTime) {
                            listOf(Pair(22, 0), Pair(23, 0), Pair(0, 0), Pair(1, 0))
                        } else {
                            listOf(Pair(6, 0), Pair(6, 30), Pair(7, 0), Pair(8, 0))
                        }
                        presets.forEach { (h, m) ->
                            val isSelected = selectedHour == h && selectedMinute == m
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AdtPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedHour = h
                                        selectedMinute = m
                                    }
                            ) {
                                Text(
                                    text = String.format(Locale.getDefault(), "%02d:%02d", h, m),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedHour, selectedMinute) },
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary),
                modifier = Modifier.testTag("btn_dialog_confirm_time")
            ) {
                Text("Valider")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_dialog_cancel_time")
            ) {
                Text("Annuler")
            }
        }
    )
}


