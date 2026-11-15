package com.example.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.RecoveryAccountType
import com.example.ui.theme.AdtAccentGreen
import com.example.ui.theme.AdtPrimary
import com.example.viewmodel.AdtViewModel

@Composable
fun TwoStepPinLockScreen(
    viewModel: AdtViewModel,
    onBiometricRequested: (() -> Unit)? = null
) {
    val state by viewModel.twoStepVerificationState.collectAsState()
    val recoveryNotice by viewModel.twoStepRecoveryNotification.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("twostep_lock_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP NOTIFICATION BANNER (IF RECOVERY CODE WAS SENT)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = recoveryNotice != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AdtAccentGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, AdtAccentGreen.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = AdtAccentGreen)
                            Text(
                                text = recoveryNotice ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AdtAccentGreen,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissRecoveryNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fermer", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // CENTER CONTENT: LOGO, TITLE, PIN DOTS, KEYPAD
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = AdtPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Verrouillage ADT Pro",
                            tint = AdtPrimary,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Text(
                    text = "Vérification en deux étapes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Entrez votre code PIN ADT Pro à 6 chiffres pour déverrouiller l'accès.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                PinDotsIndicator(pin = enteredPin)

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }

                CustomNumericKeypad(
                    currentValue = enteredPin,
                    maxLength = 6,
                    onValueChange = { newPin ->
                        enteredPin = newPin
                        errorMessage = null
                        if (newPin.length == 6) {
                            viewModel.verifyTwoStepPin(
                                pin = newPin,
                                onSuccess = {
                                    enteredPin = ""
                                    errorMessage = null
                                },
                                onError = { err ->
                                    errorMessage = err
                                    enteredPin = ""
                                }
                            )
                        }
                    }
                )
            }

            // BOTTOM BAR: RECOVERY BUTTON & BIOMETRIC FALLBACK
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                if (onBiometricRequested != null) {
                    FilledTonalButton(
                        onClick = onBiometricRequested,
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .testTag("btn_pin_screen_biometric"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Déverrouiller avec l'empreinte",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showForgotDialog = true },
                    modifier = Modifier.testTag("btn_forgot_pin")
                ) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp), tint = AdtPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Code PIN oublié ?", color = AdtPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // FORGOT PIN & CLOUD RECOVERY DIALOG
    if (showForgotDialog) {
        var recoveryStep by remember { mutableIntStateOf(1) }
        var inputCode by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf<String?>(null) }
        var sentAccountInfo by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showForgotDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (state.recoveryAccountType == RecoveryAccountType.GOOGLE) Icons.Default.AccountCircle else Icons.Default.Cloud,
                        contentDescription = null,
                        tint = AdtPrimary
                    )
                    Text(
                        text = if (recoveryStep == 1) "Récupération du compte" else "Nouveau code PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (recoveryStep == 1) {
                        val accountTypeLabel = if (state.recoveryAccountType == RecoveryAccountType.GOOGLE) "Compte Google" else "Compte iCloud"
                        Text(
                            text = "Un code de réinitialisation sécurisé peut être envoyé à votre $accountTypeLabel :",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (state.recoveryAccountType == RecoveryAccountType.GOOGLE) {
                                    Text("G", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF4285F4))
                                } else {
                                    Icon(Icons.Default.Cloud, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(20.dp))
                                }
                                Column {
                                    Text(accountTypeLabel, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        text = state.recoveryEmail.ifBlank { "traoreada49@gmail.com" },
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (sentAccountInfo.isNotEmpty()) {
                            Text(
                                text = "Code de sécurité envoyé ! Vérifiez la notification en haut de l'écran.",
                                fontSize = 12.sp,
                                color = AdtAccentGreen,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedTextField(
                                value = inputCode,
                                onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) inputCode = it },
                                label = { Text("Code de sécurité (6 chiffres)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (recoveryError != null) {
                            Text(recoveryError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    } else {
                        // STEP 2: DEFINE NEW PIN
                        Text(
                            text = "Votre identité a été vérifiée via votre compte Cloud. Choisissez votre nouveau code PIN à 6 chiffres :",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                            label = { Text("Nouveau code PIN (6 chiffres)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                            label = { Text("Confirmer le nouveau code PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (recoveryError != null) {
                            Text(recoveryError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                if (recoveryStep == 1) {
                    if (sentAccountInfo.isEmpty()) {
                        Button(
                            onClick = {
                                viewModel.sendTwoStepRecoveryCode { code, destination ->
                                    sentAccountInfo = destination
                                    recoveryError = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                        ) {
                            Text("Envoyer le code")
                        }
                    } else {
                        Button(
                            onClick = {
                                if (inputCode.length != 6) {
                                    recoveryError = "Veuillez entrer le code à 6 chiffres"
                                    return@Button
                                }
                                recoveryStep = 2
                                recoveryError = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                        ) {
                            Text("Continuer")
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (newPin != confirmPin) {
                                recoveryError = "Les codes ne correspondent pas"
                                return@Button
                            }
                            if (newPin.length != 6) {
                                recoveryError = "Le code PIN doit comporter 6 chiffres"
                                return@Button
                            }
                            viewModel.resetTwoStepPinWithRecovery(inputCode, newPin) { success, error ->
                                if (success) {
                                    showForgotDialog = false
                                } else {
                                    recoveryError = error ?: "Erreur de réinitialisation"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdtAccentGreen)
                    ) {
                        Text("Enregistrer et déverrouiller")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }
}
