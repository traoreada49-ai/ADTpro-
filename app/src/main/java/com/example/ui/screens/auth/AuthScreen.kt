package com.example.ui.screens.auth

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.auth.PhoneAuthState
import com.example.data.auth.PhoneNumberValidator
import com.example.data.auth.PhoneValidationResult
import com.example.ui.components.AdtAvatar
import com.example.ui.components.HomeColorPickerDialog
import com.example.ui.components.OperatorBadge
import com.example.ui.components.PhoneWithDialCodeInput
import com.example.ui.theme.*
import com.example.ui.util.AppLanguage
import com.example.ui.util.getStringsForLanguage
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

private data class QuickCountryPreset(
    val countryName: String,
    val flag: String,
    val dialCode: String,
    val nationalNumber: String,
    val operator: String,
    val firstName: String,
    val lastName: String
)

private val authQuickPresets = listOf(
    QuickCountryPreset("Mali", "🇲🇱", "+223", "66123456", "Orange", "Amadou", "Traoré"),
    QuickCountryPreset("Sénégal", "🇸🇳", "+221", "770001122", "Orange", "Moussa", "Diop"),
    QuickCountryPreset("Côte d'Ivoire", "🇨🇮", "+225", "0788990011", "Moov Africa", "Amina", "Koné"),
    QuickCountryPreset("Cameroun", "🇨🇲", "+237", "699123456", "Orange", "Samuel", "Mbarga"),
    QuickCountryPreset("RD Congo", "🇨🇩", "+243", "812345678", "Vodacom", "Kabongo", "Mukendi"),
    QuickCountryPreset("France", "🇫🇷", "+33", "0612345678", "Orange", "Sophie", "Laurent"),
    QuickCountryPreset("États-Unis", "🇺🇸", "+1", "2025550123", "AT&T", "Michael", "Smith")
)

@Composable
fun AuthFlowScreen(viewModel: AdtViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    when (currentScreen) {
        AppScreen.AUTH_PHONE -> PhoneInputScreen(viewModel)
        AppScreen.AUTH_OTP -> OtpVerificationScreen(viewModel)
        AppScreen.AUTH_PROFILE -> ProfileSetupScreen(viewModel)
        else -> PhoneInputScreen(viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneInputScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val isLoginMode by viewModel.isLoginMode.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val enteredDialCode by viewModel.enteredDialCode.collectAsState()
    val enteredPhone by viewModel.enteredPhoneNumber.collectAsState()
    val selectedOperator by viewModel.selectedOperator.collectAsState()
    val authState by viewModel.phoneAuthState.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val isCredentialLoading by viewModel.isCredentialLoading.collectAsState()
    val credentialMessage by viewModel.credentialManagerMessage.collectAsState()
    val detectedCredential by viewModel.detectedCredential.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val currentLang = remember(appSettings?.language) {
        AppLanguage.fromCode(appSettings?.language)
    }
    val strings = remember(appSettings?.language) {
        getStringsForLanguage(appSettings?.language)
    }

    val homePaletteColor by viewModel.homeColorTheme.collectAsState()
    val homePrimary = when (homePaletteColor) {
        AdtPaletteColor.BLANCHE -> Color(0xFF2E2E2E)
        AdtPaletteColor.VERT -> AdtPrimary
        else -> homePaletteColor.primary
    }

    var showHomeColorPicker by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val phoneValidation = remember(enteredDialCode, enteredPhone, selectedCountry) {
        PhoneNumberValidator.validate(
            rawDialCode = enteredDialCode,
            rawPhoneNumber = enteredPhone,
            preferredCountry = selectedCountry
        )
    }
    val isPhoneValid = phoneValidation is PhoneValidationResult.Valid
    val isLoading = authState is PhoneAuthState.SendingCode

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = homePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isLoginMode) strings.authPhoneTitle else strings.authProfileTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showHomeColorPicker = true },
                        modifier = Modifier.testTag("btn_auth_color_picker")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(homePaletteColor.primary)
                                .border(
                                    1.dp,
                                    if (homePaletteColor == AdtPaletteColor.BLANCHE) Color.Gray else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Palette,
                                contentDescription = "Changer la couleur d'accueil",
                                tint = if (homePaletteColor == AdtPaletteColor.BLANCHE) Color.Black else Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showLanguageDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(currentLang.flag, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(currentLang.displayName.take(8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Switch: Login vs Sign Up
            item {
                TabRow(
                    selectedTabIndex = if (isLoginMode) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = homePrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[if (isLoginMode) 0 else 1]),
                            color = homePrimary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = isLoginMode,
                        onClick = { viewModel.toggleAuthMode(true) },
                        text = {
                            Text(
                                "Se Connecter",
                                fontWeight = if (isLoginMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("auth_tab_login")
                    )
                    Tab(
                        selected = !isLoginMode,
                        onClick = { viewModel.toggleAuthMode(false) },
                        text = {
                            Text(
                                "Créer un Compte",
                                fontWeight = if (!isLoginMode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        },
                        modifier = Modifier.testTag("auth_tab_signup")
                    )
                }
            }

            // Branding Header
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_logo_adt),
                        contentDescription = "Logo ADT Pro",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )

                    Text(
                        text = if (isLoginMode) "Connexion par numéro mobile" else "Inscription par numéro de téléphone",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = if (isLoginMode)
                            "Saisissez votre indicatif pays et votre numéro pour recevoir un code de vérification SMS via Firebase Authentication."
                        else
                            "Validez votre indicatif pays et votre numéro de téléphone afin de créer votre compte sécurisé via Firebase Auth.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            // Firebase Phone Auth Badge
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdtPrimary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = AdtPrimary,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Firebase Phone Authentication",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdtPrimary
                            )
                            Text(
                                text = "Validation E.164 de l'indicatif pays • Vérification SMS OTP",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Reusable Phone Input Component with Country Calling Code Validation
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Indicatif pays et numéro de téléphone :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    PhoneWithDialCodeInput(
                        selectedCountry = selectedCountry,
                        enteredDialCode = enteredDialCode,
                        phoneNumber = enteredPhone,
                        onCountrySelected = { viewModel.selectCountry(it) },
                        onDialCodeChanged = { viewModel.onDialCodeChange(it) },
                        onPhoneNumberChanged = { viewModel.onPhoneNumberChange(it) },
                        label = "Numéro mobile",
                        placeholder = selectedCountry.exampleNumber,
                        showValidationStatus = true,
                        onDone = {
                            if (isPhoneValid && !isLoading) {
                                viewModel.requestOtp(activity)
                            }
                        }
                    )
                }
            }

            // Operator Selector Chips
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Opérateur téléphonique :",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedCountry.defaultOperators) { op ->
                            FilterChip(
                                selected = selectedOperator == op,
                                onClick = { viewModel.selectOperator(op) },
                                label = { Text(op, fontSize = 12.sp) },
                                leadingIcon = {
                                    if (selectedOperator == op) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Firebase Auth Error Banner
            if (authError != null) {
                val isProviderDisabled = (authState as? PhoneAuthState.Error)?.isProviderNotEnabled == true
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_error_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = authError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (isProviderDisabled) {
                                Text(
                                    text = "Conseil : Activez le fournisseur « Téléphone » dans Firebase Console > Authentication > Sign-in method (ou configurez des numéros de test Firebase).",
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // Credential Manager Card (Android Saved Credentials)
            item {
                OutlinedCard(
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = AdtPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Gestionnaire d'identifiants Android",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = { viewModel.fetchCredentialManagerAccount(activity) },
                            enabled = !isCredentialLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("btn_credential_manager"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCredentialLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AdtPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recherche d'identifiant...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = AdtPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Remplir avec Credential Manager",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (credentialMessage != null) {
                            Text(
                                text = credentialMessage ?: "",
                                fontSize = 11.sp,
                                color = AdtPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            if (detectedCredential != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AdtPrimary.copy(alpha = 0.10f)),
                        border = BorderStroke(1.5.dp, AdtPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Identifiant détecté : ${detectedCredential?.phoneNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AdtPrimary
                            )
                            Button(
                                onClick = { viewModel.loginWithDetectedCredential(activity) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("btn_1tap_credential_login"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Envoyer le code SMS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Quick Format Examples by Country
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Exemples de formats par indicatif :",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(authQuickPresets, key = { it.countryName }) { preset ->
                            SuggestionChip(
                                onClick = {
                                    viewModel.applyPresetPhoneNumber(
                                        countryName = preset.countryName,
                                        phoneWithoutDial = preset.nationalNumber,
                                        operator = preset.operator,
                                        fName = preset.firstName,
                                        lName = preset.lastName
                                    )
                                },
                                label = {
                                    Text(
                                        "${preset.flag} ${preset.dialCode} ${preset.nationalNumber}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Submit Button (Gated by Country Dial Code + Phone Validation)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = {
                        if (!isLoading) {
                            viewModel.requestOtp(activity)
                        }
                    },
                    enabled = isPhoneValid && !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("btn_request_code"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Envoi du SMS Firebase...", color = Color.White, fontSize = 14.sp)
                    } else {
                        Icon(Icons.Default.Sms, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoginMode) "Recevoir le code de connexion SMS" else "S'inscrire et vérifier par SMS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

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

    if (showHomeColorPicker) {
        HomeColorPickerDialog(
            currentPaletteColor = homePaletteColor,
            onSelectColor = { selected -> viewModel.setHomeColorTheme(selected) },
            onDismiss = { showHomeColorPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current

    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val enteredDialCode by viewModel.enteredDialCode.collectAsState()
    val enteredPhone by viewModel.enteredPhoneNumber.collectAsState()
    val selectedOperator by viewModel.selectedOperator.collectAsState()
    val enteredOtp by viewModel.enteredOtpCode.collectAsState()
    val authState by viewModel.phoneAuthState.collectAsState()
    val authError by viewModel.authErrorMessage.collectAsState()
    val resendCountdown by viewModel.resendCountdownSeconds.collectAsState()
    val isLoginMode by viewModel.isLoginMode.collectAsState()

    val isVerifying = authState is PhoneAuthState.VerifyingCode
    val isSending = authState is PhoneAuthState.SendingCode
    val validation = remember(enteredDialCode, enteredPhone, selectedCountry) {
        PhoneNumberValidator.validate(enteredDialCode, enteredPhone, selectedCountry)
    }
    val displayNumber = (validation as? PhoneValidationResult.Valid)?.e164Number
        ?: "+${enteredDialCode.removePrefix("+").ifBlank { selectedCountry.dialCode.removePrefix("+") }} $enteredPhone"

    BackHandler {
        viewModel.navigateTo(AppScreen.AUTH_PHONE)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Vérification SMS Firebase", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.AUTH_PHONE) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Authentic Firebase SMS Verification Dispatch Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = AdtPrimary.copy(alpha = 0.08f)),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("firebase_sms_status_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OperatorBadge(selectedOperator)
                                Text(
                                    text = "Firebase SMS OTP • ${selectedCountry.flagEmoji}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AdtPrimary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (isSending) "Envoi..." else "Code envoyé",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AdtPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Un SMS de vérification Firebase Authentication a été expédié vers $displayNumber. Saisissez le code à 6 chiffres reçu.",
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedButton(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text?.filter { it.isDigit() }?.take(6).orEmpty()
                                if (pasted.length == 6) {
                                    viewModel.enteredOtpCode.value = pasted
                                    viewModel.verifyOtp(activity, pasted)
                                } else {
                                    viewModel.authErrorMessage.value = "Aucun code à 6 chiffres trouvé dans le presse-papiers."
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 40.dp)
                                .testTag("btn_paste_otp"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Coller le code du presse-papiers", fontSize = 12.sp)
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = AdtPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Pin,
                            contentDescription = null,
                            tint = AdtPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Text(
                    text = "Code de validation à 6 chiffres",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Destinataire : $displayNumber",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // 6-digit segmented PIN box
                BasicTextField(
                    value = enteredOtp,
                    onValueChange = {
                        val digits = it.filter { ch -> ch.isDigit() }.take(6)
                        viewModel.enteredOtpCode.value = digits
                        viewModel.authErrorMessage.value = null
                        if (digits.length == 6 && !isVerifying) {
                            viewModel.verifyOtp(activity, digits)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (enteredOtp.length == 6 && !isVerifying) {
                                viewModel.verifyOtp(activity)
                            }
                        }
                    ),
                    modifier = Modifier.testTag("otp_input_field"),
                    decorationBox = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (index in 0 until 6) {
                                val digit = enteredOtp.getOrNull(index)?.toString() ?: ""
                                val isFocused = index == enteredOtp.length
                                val hasError = authError != null

                                val borderColor = when {
                                    hasError -> MaterialTheme.colorScheme.error
                                    isFocused -> AdtPrimary
                                    digit.isNotEmpty() -> AdtPrimary.copy(alpha = 0.6f)
                                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                }
                                val borderWidth = if (isFocused || hasError) 2.dp else 1.dp
                                val containerColor = if (isFocused) AdtPrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surface

                                Box(
                                    modifier = Modifier
                                        .width(46.dp)
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(containerColor)
                                        .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                                        .testTag("otp_digit_box_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit.isNotEmpty()) {
                                        Text(
                                            text = digit,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    } else if (isFocused) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(22.dp)
                                                .background(AdtPrimary)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                )

                if (authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_error_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = authError ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (resendCountdown > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Renvoyer le code SMS dans ${resendCountdown}s",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    TextButton(
                        onClick = { viewModel.resendOtp(activity) },
                        enabled = !isSending,
                        modifier = Modifier.testTag("btn_resend_otp")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = AdtPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Renvoyer un nouveau code SMS", color = AdtPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = { viewModel.navigateTo(AppScreen.AUTH_PHONE) }) {
                    Text("Modifier le numéro de téléphone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = {
                    if (enteredOtp.length == 6 && !isVerifying) {
                        viewModel.verifyOtp(activity)
                    }
                },
                enabled = enteredOtp.length == 6 && !isVerifying,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp)
                    .testTag("btn_verify_otp"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Validation Firebase en cours...", color = Color.White, fontSize = 14.sp)
                } else {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isLoginMode) "Valider et se connecter" else "Valider le code SMS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(viewModel: AdtViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    val firstName by viewModel.profileFirstName.collectAsState()
    val lastName by viewModel.profileLastName.collectAsState()
    val category by viewModel.profileBusinessCategory.collectAsState()
    val bio by viewModel.profileBio.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val enteredDialCode by viewModel.enteredDialCode.collectAsState()
    val enteredPhone by viewModel.enteredPhoneNumber.collectAsState()
    val selectedOperator by viewModel.selectedOperator.collectAsState()
    val verifiedUid by viewModel.verifiedFirebaseUid.collectAsState()

    val validation = remember(enteredDialCode, enteredPhone, selectedCountry) {
        PhoneNumberValidator.validate(enteredDialCode, enteredPhone, selectedCountry)
    }
    val verifiedNumberLabel = (validation as? PhoneValidationResult.Valid)?.e164Number
        ?: "${selectedCountry.dialCode} $enteredPhone"

    val categories = listOf(
        "Compte Professionnel ADT",
        "Entreprise & Télécom",
        "Commerce & Ventes",
        "Consultant & Services",
        "Étudiant & Particulier"
    )

    BackHandler {
        viewModel.navigateTo(AppScreen.AUTH_PHONE)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Finalisation du profil", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Votre numéro a été vérifié avec succès par Firebase Authentication ! Renseignez votre profil pour terminer l'inscription.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(contentAlignment = Alignment.BottomEnd) {
                    AdtAvatar(
                        name = if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName" else "ADT",
                        sizeDp = 92
                    )
                    Surface(
                        shape = CircleShape,
                        color = AdtPrimary,
                        modifier = Modifier.size(30.dp)
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
            }

            item {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { viewModel.profileFirstName.value = it },
                    label = { Text("Prénom *") },
                    placeholder = { Text("Ex: Amadou") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_first_name"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { viewModel.profileLastName.value = it },
                    label = { Text("Nom de famille *") },
                    placeholder = { Text("Ex: Diallo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_last_name"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Text(
                    text = "Catégorie professionnelle :",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { viewModel.profileBusinessCategory.value = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { viewModel.profileBio.value = it },
                    label = { Text("Statut / Bio professionnelle") },
                    placeholder = { Text("Ex: Disponible sur ADT Pro") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_bio"),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Surface(
                    color = AdtPrimary.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, AdtPrimary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = AdtPrimary)
                        Column {
                            Text(
                                text = "Numéro vérifié : $verifiedNumberLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AdtPrimary
                            )
                            Text(
                                text = buildString {
                                    append("Opérateur : $selectedOperator")
                                    if (!verifiedUid.isNullOrBlank()) {
                                        append(" • Firebase UID : ${verifiedUid?.take(10)}…")
                                    }
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.completeRegistration(activity) },
                    enabled = firstName.isNotBlank() && lastName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("btn_complete_profile"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdtPrimary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Finaliser mon inscription",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
