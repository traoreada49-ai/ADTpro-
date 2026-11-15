package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.RecoveryAccountType
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoStepVerificationScreen(viewModel: AdtViewModel) {
    val twoStepState by viewModel.twoStepVerificationState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSetupWizard by remember { mutableStateOf(false) }
    var showDisableDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showChangeAccountDialog by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Vérification en deux étapes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = if (twoStepState.isEnabled) "Protection active • Code PIN & Cloud" else "Sécurisation recommandée",
                            fontSize = 11.sp,
                            color = if (twoStepState.isEnabled) AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        modifier = Modifier.testTag("btn_back_twostep")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.testTag("twostep_topbar")
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(2.dp)) }

            // HERO STATUS CARD
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (twoStepState.isEnabled) {
                            AdtAccentGreen.copy(alpha = 0.09f)
                        } else {
                            AdtPrimary.copy(alpha = 0.08f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (twoStepState.isEnabled) AdtAccentGreen.copy(alpha = 0.35f) else AdtPrimary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (twoStepState.isEnabled) AdtAccentGreen.copy(alpha = 0.18f) else AdtPrimary.copy(alpha = 0.18f),
                                modifier = Modifier.size(54.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (twoStepState.isEnabled) Icons.Default.VerifiedUser else Icons.Default.Security,
                                        contentDescription = null,
                                        tint = if (twoStepState.isEnabled) AdtAccentGreen else AdtPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (twoStepState.isEnabled) "Sécurité Activée" else "Sécurité en deux étapes",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (twoStepState.isEnabled) AdtAccentGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = if (twoStepState.isEnabled) "ACTIF" else "DÉSACTIVÉ",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (twoStepState.isEnabled) AdtAccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (twoStepState.isEnabled) {
                                        "Votre compte ADT Pro est verrouillé par un code PIN à 6 chiffres avec compte de récupération sécurisé."
                                    } else {
                                        "Empêchez tout piratage ou transfert non autorisé en configurant un code PIN et un compte de secours."
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        if (!twoStepState.isEnabled) {
                            Button(
                                onClick = { showSetupWizard = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_start_twostep_wizard"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Activer la vérification en 2 étapes", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // RECOVERY ACCOUNT LINKED (GOOGLE OR ICLOUD)
            if (twoStepState.isEnabled) {
                item {
                    Text(
                        text = "COMPTE DE RÉCUPÉRATION LIÉ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (twoStepState.recoveryAccountType == RecoveryAccountType.GOOGLE) {
                                        // Google Logo Badge
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "G",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 20.sp,
                                                    color = Color(0xFF4285F4)
                                                )
                                            }
                                        }
                                    } else {
                                        // iCloud Logo Badge
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF007AFF).copy(alpha = 0.15f),
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Default.Cloud,
                                                    contentDescription = "iCloud",
                                                    tint = Color(0xFF007AFF),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = twoStepState.recoveryAccountType.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = twoStepState.recoveryEmail.ifBlank { "Compte lié" },
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AdtAccentGreen.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = AdtAccentGreen, modifier = Modifier.size(12.dp))
                                        Text("Vérifié", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AdtAccentGreen)
                                    }
                                }
                            }

                            Text(
                                text = "En cas d'oubli de votre code PIN, un code de réinitialisation sécurisé sera immédiatement envoyé à cette adresse.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // ACTIONS MANAGEMENT
                item {
                    Text(
                        text = "GESTION DU CODE PIN & OPTIONS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AdtPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            // Changer le code PIN
                            TwoStepActionRow(
                                icon = Icons.Default.Pin,
                                title = "Changer le code PIN",
                                subtitle = "Modifier votre code secret à 6 chiffres",
                                testTag = "row_change_pin",
                                onClick = { showChangePinDialog = true }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )

                            // Changer le compte de récupération (Google / iCloud)
                            TwoStepActionRow(
                                icon = if (twoStepState.recoveryAccountType == RecoveryAccountType.GOOGLE) Icons.Default.AccountCircle else Icons.Default.Cloud,
                                title = "Changer le compte de récupération",
                                subtitle = "Basculer entre compte Google et compte iCloud",
                                testTag = "row_change_account",
                                onClick = { showChangeAccountDialog = true }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )

                            // Verrouiller maintenant (Test)
                            TwoStepActionRow(
                                icon = Icons.Default.LockClock,
                                title = "Verrouiller l'application maintenant",
                                subtitle = "Tester le déverrouillage immédiat par code PIN",
                                testTag = "row_lock_now",
                                onClick = {
                                    viewModel.lockAppWithTwoStep()
                                }
                            )

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )

                            // Exiger le PIN au lancement (Switch)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = AdtPrimary)
                                    Column {
                                        Text("Demander le PIN au démarrage", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(
                                            "Sécurise l'accès dès l'ouverture de l'application",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Switch(
                                    checked = twoStepState.requirePinOnLaunch,
                                    onCheckedChange = { viewModel.setRequireTwoStepPinOnLaunch(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = AdtPrimary
                                    )
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )

                            // Désactiver la vérification en 2 étapes
                            TwoStepActionRow(
                                icon = Icons.Default.NoEncryption,
                                title = "Désactiver la vérification en 2 étapes",
                                subtitle = "Supprime la protection par PIN (déconseillé)",
                                iconTint = MaterialTheme.colorScheme.error,
                                titleColor = MaterialTheme.colorScheme.error,
                                testTag = "row_disable_twostep",
                                onClick = { showDisableDialog = true }
                            )
                        }
                    }
                }
            }

            // EDUCATIONAL BANNER: GOOGLE & ICLOUD SYNC
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "Pourquoi associer un compte Google ou iCloud ?",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AdtPrimary
                            )
                        }
                        Text(
                            text = "Pour protéger votre vie privée, ADT Pro ne conserve pas votre code PIN en clair sur ses serveurs. La liaison avec votre compte Google ou iCloud permet d'envoyer un code de déverrouillage sécurisé en cas d'oubli sans réinitialiser vos conversations chiffrées.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // SETUP WIZARD DIALOG
    if (showSetupWizard) {
        TwoStepSetupWizardDialog(
            defaultEmail = "traoreada49@gmail.com",
            onDismiss = { showSetupWizard = false },
            onComplete = { pin, accountType, email ->
                viewModel.enableTwoStepVerification(pin, accountType, email) { success, error ->
                    if (success) {
                        showSetupWizard = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Vérification en deux étapes activée avec succès !")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(error ?: "Erreur lors de l'activation")
                        }
                    }
                }
            }
        )
    }

    // DISABLE DIALOG
    if (showDisableDialog) {
        var inputPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showDisableDialog = false },
            title = { Text("Désactiver la vérification en 2 étapes", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Pour confirmer la désactivation, veuillez saisir votre code PIN actuel à 6 chiffres :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) inputPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text("Code PIN (6 chiffres)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.disableTwoStepVerification(inputPin) { success, error ->
                            if (success) {
                                showDisableDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Vérification en deux étapes désactivée")
                                }
                            } else {
                                errorMessage = error ?: "Code PIN incorrect"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Désactiver")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisableDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // CHANGE PIN DIALOG
    if (showChangePinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Modifier le code PIN", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) oldPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text("Code PIN actuel (6 chiffres)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text("Nouveau code PIN (6 chiffres)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text("Confirmer le nouveau code PIN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin != confirmPin) {
                            errorMessage = "Les nouveaux codes PIN ne correspondent pas"
                            return@Button
                        }
                        if (newPin.length != 6) {
                            errorMessage = "Le nouveau code PIN doit comporter 6 chiffres"
                            return@Button
                        }
                        viewModel.changeTwoStepPin(oldPin, newPin) { success, error ->
                            if (success) {
                                showChangePinDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Code PIN modifié avec succès")
                                }
                            } else {
                                errorMessage = error ?: "Erreur lors de la modification"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Valider")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    // CHANGE ACCOUNT DIALOG (GOOGLE OR ICLOUD)
    if (showChangeAccountDialog) {
        var currentPin by remember { mutableStateOf("") }
        var selectedType by remember { mutableStateOf(twoStepState.recoveryAccountType) }
        var emailInput by remember { mutableStateOf(twoStepState.recoveryEmail.ifBlank { "traoreada49@gmail.com" }) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangeAccountDialog = false },
            title = { Text("Compte de récupération", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Choisissez le compte associé pour la récupération :",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedType == RecoveryAccountType.GOOGLE,
                            onClick = {
                                selectedType = RecoveryAccountType.GOOGLE
                                if (!emailInput.contains("@gmail.com")) {
                                    emailInput = "traoreada49@gmail.com"
                                }
                            },
                            label = { Text("Compte Google") },
                            leadingIcon = {
                                Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF4285F4))
                            },
                            modifier = Modifier.weight(1f)
                        )

                        FilterChip(
                            selected = selectedType == RecoveryAccountType.ICLOUD,
                            onClick = {
                                selectedType = RecoveryAccountType.ICLOUD
                                if (!emailInput.contains("@icloud.com")) {
                                    emailInput = "traoreada49@icloud.com"
                                }
                            },
                            label = { Text("Compte iCloud") },
                            leadingIcon = {
                                Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(16.dp))
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text(if (selectedType == RecoveryAccountType.GOOGLE) "Adresse Google (Gmail)" else "Adresse iCloud (Apple ID)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = currentPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) currentPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        label = { Text("Code PIN actuel pour valider") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (emailInput.isBlank()) {
                            errorMessage = "Veuillez renseigner une adresse e-mail"
                            return@Button
                        }
                        viewModel.changeTwoStepRecoveryAccount(currentPin, selectedType, emailInput) { success, error ->
                            if (success) {
                                showChangeAccountDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Compte de récupération mis à jour !")
                                }
                            } else {
                                errorMessage = error ?: "Code PIN incorrect"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeAccountDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}

@Composable
fun TwoStepActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    testTag: String,
    iconTint: Color = AdtPrimary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag(testTag),
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
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
            Column {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = titleColor)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

/**
 * Full Step-by-Step setup dialog for 2-Step Verification:
 * Step 1: Enter 6-digit PIN
 * Step 2: Confirm 6-digit PIN
 * Step 3: Choose Recovery Account (Google or iCloud)
 */
@Composable
fun TwoStepSetupWizardDialog(
    defaultEmail: String,
    onDismiss: () -> Unit,
    onComplete: (pin: String, accountType: RecoveryAccountType, email: String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var selectedAccountType by remember { mutableStateOf(RecoveryAccountType.GOOGLE) }
    var emailInput by remember { mutableStateOf(defaultEmail) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = AdtPrimary)
                Text(
                    text = when (step) {
                        1 -> "Étape 1/3 : Créer votre code PIN"
                        2 -> "Étape 2/3 : Confirmer votre code PIN"
                        else -> "Étape 3/3 : Compte de récupération"
                    },
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
                when (step) {
                    1 -> {
                        Text(
                            text = "Saisissez un code secret à 6 chiffres qui vous sera demandé pour protéger votre compte :",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PinDotsIndicator(pin = enteredPin)

                        CustomNumericKeypad(
                            currentValue = enteredPin,
                            maxLength = 6,
                            onValueChange = {
                                enteredPin = it
                                errorMessage = null
                                if (it.length == 6) {
                                    step = 2
                                }
                            }
                        )
                    }
                    2 -> {
                        Text(
                            text = "Confirmez votre code secret à 6 chiffres :",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        PinDotsIndicator(pin = confirmedPin)

                        CustomNumericKeypad(
                            currentValue = confirmedPin,
                            maxLength = 6,
                            onValueChange = {
                                confirmedPin = it
                                errorMessage = null
                                if (it.length == 6) {
                                    if (confirmedPin == enteredPin) {
                                        step = 3
                                    } else {
                                        errorMessage = "Les codes PIN ne correspondent pas. Réessayez."
                                        confirmedPin = ""
                                    }
                                }
                            }
                        )
                    }
                    3 -> {
                        Text(
                            text = "Liez un compte Google ou iCloud pour recevoir un lien de déblocage si vous oubliez votre code PIN :",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Google & iCloud Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedAccountType == RecoveryAccountType.GOOGLE) {
                                        Color(0xFF4285F4).copy(alpha = 0.12f)
                                    } else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (selectedAccountType == RecoveryAccountType.GOOGLE) {
                                    BorderStroke(1.5.dp, Color(0xFF4285F4))
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedAccountType = RecoveryAccountType.GOOGLE
                                        if (!emailInput.contains("@gmail.com")) {
                                            emailInput = defaultEmail
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("G", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF4285F4))
                                    Text("Compte Google", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedAccountType == RecoveryAccountType.ICLOUD) {
                                        Color(0xFF007AFF).copy(alpha = 0.12f)
                                    } else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (selectedAccountType == RecoveryAccountType.ICLOUD) {
                                    BorderStroke(1.5.dp, Color(0xFF007AFF))
                                } else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedAccountType = RecoveryAccountType.ICLOUD
                                        if (!emailInput.contains("@icloud.com")) {
                                            emailInput = "traoreada49@icloud.com"
                                        }
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(26.dp))
                                    Text("Compte iCloud", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text(if (selectedAccountType == RecoveryAccountType.GOOGLE) "Adresse e-mail Google" else "Adresse e-mail iCloud") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (errorMessage != null) {
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        },
        confirmButton = {
            if (step == 3) {
                Button(
                    onClick = {
                        if (emailInput.isBlank()) {
                            errorMessage = "Veuillez renseigner une adresse e-mail"
                            return@Button
                        }
                        onComplete(enteredPin, selectedAccountType, emailInput)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Text("Activer la protection")
                }
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
fun PinDotsIndicator(pin: String, length: Int = 6) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        for (i in 0 until length) {
            val isFilled = i < pin.length
            Surface(
                shape = CircleShape,
                color = if (isFilled) AdtPrimary else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    1.5.dp,
                    if (isFilled) AdtPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                ),
                modifier = Modifier.size(16.dp)
            ) {}
        }
    }
}

@Composable
fun CustomNumericKeypad(
    currentValue: String,
    maxLength: Int = 6,
    onValueChange: (String) -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.widthIn(max = 260.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (key.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (key == "DEL") {
                                            if (currentValue.isNotEmpty()) {
                                                onValueChange(currentValue.dropLast(1))
                                            }
                                        } else {
                                            if (currentValue.length < maxLength) {
                                                onValueChange(currentValue + key)
                                            }
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (key == "DEL") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Effacer",
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
