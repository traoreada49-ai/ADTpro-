package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import com.example.util.ThemeScheduleManager
import com.example.util.BiometricAuthManager
import com.example.ui.screens.security.BiometricLockScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.auth.AuthFlowScreen
import com.example.ui.screens.call.ActiveCallScreen
import com.example.ui.screens.channel.ChannelDetailScreen
import com.example.ui.screens.channel.CreateChannelScreen
import com.example.ui.screens.chat.ChatScreen
import com.example.ui.screens.contacts.ContactsScreen
import com.example.ui.screens.contacts.ContactsSyncScreen
import com.example.ui.screens.contacts.ContactQrCodeScreen
import com.example.ui.screens.group.CreateGroupScreen
import com.example.ui.screens.group.GroupInfoScreen
import com.example.ui.screens.live.TikTokLiveStreamScreen
import com.example.ui.screens.main.MainContainerScreen
import com.example.ui.screens.media.CameraStudioScreen
import com.example.ui.screens.settings.PrivacySettingsScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.StorageManagementScreen
import com.example.ui.screens.settings.AppUsageStatisticsScreen
import com.example.ui.screens.settings.WallpaperPickerScreen
import com.example.ui.screens.metaai.MetaAiDirectScreen
import com.example.ui.screens.settings.ChatBackupScreen
import com.example.ui.screens.settings.TwoStepVerificationScreen
import com.example.ui.screens.settings.TwoStepPinLockScreen
import com.example.ui.screens.settings.EncryptionKeysScreen
import com.example.ui.screens.chat.ArchivedChatsScreen
import com.example.ui.screens.chat.LockedChatsScreen
import com.example.ui.screens.status.CreateStatusScreen
import com.example.ui.screens.status.StatusViewerScreen
import com.example.ui.screens.tracking.LiveTrackingScreen
import com.example.ui.screens.tracking.LiveLocationSharingScreen
import com.example.ui.theme.ADTProTheme
import com.example.ui.util.LocaleHelper
import com.example.viewmodel.AdtViewModel
import com.example.viewmodel.AppScreen

class MainActivity : FragmentActivity() {

    private val viewModel: AdtViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.data.firebase.configureAppCheck(this, intent)
        try {
            com.example.data.firebase.FirebaseInitHelper.ensureInitialized(this)
            android.util.Log.i("MainActivity", "FirebaseApp verified via FirebaseInitHelper in MainActivity onCreate")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to initialize FirebaseApp in onCreate: ${e.message}", e)
        }
        enableEdgeToEdge()
        handleNotificationIntents(intent)
        setContent {
            val appSettings by viewModel.appSettings.collectAsState()
            val isBiometricLocked by viewModel.isAppLockedByBiometric.collectAsState()
            val isTwoStepLocked by viewModel.isAppLockedByTwoStep.collectAsState()
            val biometricError by viewModel.biometricAuthErrorMessage.collectAsState()
            val twoStepState by viewModel.twoStepVerificationState.collectAsState()

            // Automatically launch Biometric prompt when biometric lock screen activates
            LaunchedEffect(isBiometricLocked) {
                if (isBiometricLocked && BiometricAuthManager.isBiometricAvailable(this@MainActivity)) {
                    triggerBiometricPrompt()
                }
            }

            // Sync runtime locale when user changes language
            LaunchedEffect(appSettings?.language) {
                appSettings?.language?.let { langCode ->
                    LocaleHelper.applyLocale(this@MainActivity, langCode)
                }
            }

            // Periodic ticker for auto dark mode schedule transition
            var currentTimeTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(appSettings?.themeMode) {
                if (appSettings?.themeMode == ThemeScheduleManager.MODE_AUTO_SCHEDULE) {
                    while (true) {
                        delay(20_000L) // Re-evaluate every 20 seconds
                        currentTimeTick = System.currentTimeMillis()
                    }
                }
            }

            val isSystemDark = isSystemInDarkTheme()
            val isDarkTheme = remember(
                appSettings?.themeMode,
                appSettings?.darkScheduleType,
                appSettings?.darkScheduleStartHour,
                appSettings?.darkScheduleStartMinute,
                appSettings?.darkScheduleEndHour,
                appSettings?.darkScheduleEndMinute,
                isSystemDark,
                currentTimeTick
            ) {
                ThemeScheduleManager.shouldUseDarkTheme(
                    themeMode = appSettings?.themeMode ?: ThemeScheduleManager.MODE_DARK,
                    scheduleType = appSettings?.darkScheduleType ?: ThemeScheduleManager.SCHEDULE_SUNSET_TO_SUNRISE,
                    startHour = appSettings?.darkScheduleStartHour ?: ThemeScheduleManager.DEFAULT_SUNSET_HOUR,
                    startMinute = appSettings?.darkScheduleStartMinute ?: ThemeScheduleManager.DEFAULT_SUNSET_MINUTE,
                    endHour = appSettings?.darkScheduleEndHour ?: ThemeScheduleManager.DEFAULT_SUNRISE_HOUR,
                    endMinute = appSettings?.darkScheduleEndMinute ?: ThemeScheduleManager.DEFAULT_SUNRISE_MINUTE,
                    systemInDarkTheme = isSystemDark
                )
            }

            val homePaletteColor by viewModel.homeColorTheme.collectAsState()

            ADTProTheme(
                darkTheme = isDarkTheme,
                paletteColor = homePaletteColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (isBiometricLocked) {
                        BiometricLockScreen(
                            onScanRequested = { triggerBiometricPrompt() },
                            onFallbackToPin = {
                                viewModel.isAppLockedByBiometric.value = false
                                viewModel.isAppLockedByTwoStep.value = true
                            },
                            errorMessage = biometricError,
                            hasTwoStepConfigured = twoStepState.isEnabled
                        )
                    } else if (isTwoStepLocked) {
                        TwoStepPinLockScreen(
                            viewModel = viewModel,
                            onBiometricRequested = if (BiometricAuthManager.isBiometricAvailable(this@MainActivity)) {
                                { triggerBiometricPrompt() }
                            } else null
                        )
                    } else {
                        AdtAppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }

    fun triggerBiometricPrompt() {
        if (!BiometricAuthManager.isBiometricAvailable(this)) {
            viewModel.setBiometricAuthErrorMessage("Capteur biométrique non configuré ou indisponible")
            return
        }

        BiometricAuthManager.showBiometricPrompt(
            activity = this,
            title = "Déverrouillage ADT Pro",
            subtitle = "Confirmez votre empreinte digitale ou reconnaissance faciale",
            negativeButtonText = if (viewModel.twoStepVerificationState.value.isEnabled) "Utiliser le code PIN" else "Annuler",
            onSuccess = {
                viewModel.unlockAppWithBiometric()
            },
            onError = { errorCode, errString ->
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON && viewModel.twoStepVerificationState.value.isEnabled) {
                        viewModel.isAppLockedByBiometric.value = false
                        viewModel.isAppLockedByTwoStep.value = true
                    }
                } else {
                    viewModel.setBiometricAuthErrorMessage(errString.toString())
                }
            },
            onFailed = {
                viewModel.setBiometricAuthErrorMessage("Empreinte ou visage non reconnu. Réessayez.")
            }
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.onAppBackgrounded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onAppForegrounded()
        if (viewModel.isAppLockedByBiometric.value && BiometricAuthManager.isBiometricAvailable(this)) {
            triggerBiometricPrompt()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntents(intent)
    }

    private fun handleNotificationIntents(intent: android.content.Intent?) {
        if (intent == null) return
        val openScreen = intent.getStringExtra("OPEN_SCREEN")
        val isCallBack = intent.getBooleanExtra("ACTION_CALL_BACK", false)
        val isMessageContact = intent.getBooleanExtra("ACTION_MESSAGE_CONTACT", false)
        val callerPhone = intent.getStringExtra("CALLER_PHONE")
        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Contact"
        val callerAvatar = intent.getStringExtra("CALLER_AVATAR") ?: ""
        val isVideo = intent.getBooleanExtra("IS_VIDEO", false)

        if (isCallBack && !callerPhone.isNullOrBlank()) {
            viewModel.startCall(
                name = callerName,
                phone = callerPhone,
                avatar = callerAvatar,
                isVideo = isVideo
            )
        } else if (isMessageContact && !callerPhone.isNullOrBlank()) {
            val contact = viewModel.allContacts.value.find { it.phone == callerPhone }
                ?: com.example.data.local.entity.ContactEntity(
                    id = "contact_${callerPhone.hashCode()}",
                    name = callerName,
                    phone = callerPhone,
                    countryCode = if (callerPhone.startsWith("+")) callerPhone.take(4) else "+223",
                    avatarUrl = callerAvatar,
                    bio = "Contact ADT",
                    isAdtUser = true,
                    isOnline = true,
                    lastSeenText = "En ligne"
                )
            viewModel.openDirectChatWithContact(contact)
        } else if (openScreen == "CALLS") {
            viewModel.navigateTo(AppScreen.MAIN_TABS)
        } else if (openScreen == "CHAT_BACKUP") {
            viewModel.navigateTo(AppScreen.CHAT_BACKUP)
        }
    }
}

@Composable
fun AdtAppNavigation(viewModel: AdtViewModel) {
    val navController = rememberNavController()
    val currentScreen by viewModel.currentScreen.collectAsState()

    // Sync ViewModel navigation actions to NavController
    LaunchedEffect(currentScreen) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        val targetRoute = currentScreen.name.lowercase()
        if (currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                // Clear back stack to avoid memory issues and infinite loops when changing main branches
                if (currentScreen == AppScreen.MAIN_TABS || currentScreen == AppScreen.AUTH_PHONE) {
                    popUpTo(0) { inclusive = true }
                }
                launchSingleTop = true
            }
        }
    }

    // Sync NavController back press / navigation gestures to the ViewModel
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            val route = backStackEntry.destination.route
            if (route != null) {
                val matchedScreen = AppScreen.values().find { it.name.lowercase() == route }
                if (matchedScreen != null && matchedScreen != viewModel.currentScreen.value) {
                    viewModel.navigateTo(matchedScreen)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = AppScreen.MAIN_TABS.name.lowercase(),
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppScreen.AUTH_PHONE.name.lowercase()) {
            AuthFlowScreen(viewModel = viewModel)
        }
        composable(AppScreen.AUTH_OTP.name.lowercase()) {
            AuthFlowScreen(viewModel = viewModel)
        }
        composable(AppScreen.AUTH_PROFILE.name.lowercase()) {
            AuthFlowScreen(viewModel = viewModel)
        }
        composable(AppScreen.MAIN_TABS.name.lowercase()) {
            MainContainerScreen(viewModel = viewModel)
        }
        composable(AppScreen.CHAT_ROOM.name.lowercase()) {
            ChatScreen(viewModel = viewModel)
        }
        composable(AppScreen.GROUP_INFO.name.lowercase()) {
            GroupInfoScreen(viewModel = viewModel)
        }
        composable(AppScreen.CHANNEL_ROOM.name.lowercase()) {
            ChannelDetailScreen(viewModel = viewModel)
        }
        composable(AppScreen.ACTIVE_CALL.name.lowercase()) {
            ActiveCallScreen(viewModel = viewModel)
        }
        composable(AppScreen.STATUS_VIEWER.name.lowercase()) {
            StatusViewerScreen(viewModel = viewModel)
        }
        composable(AppScreen.CREATE_GROUP.name.lowercase()) {
            CreateGroupScreen(viewModel = viewModel)
        }
        composable(AppScreen.CREATE_CHANNEL.name.lowercase()) {
            CreateChannelScreen(viewModel = viewModel)
        }
        composable(AppScreen.CREATE_STATUS.name.lowercase()) {
            CreateStatusScreen(viewModel = viewModel)
        }
        composable(AppScreen.CONTACTS_LIST.name.lowercase()) {
            ContactsScreen(viewModel = viewModel)
        }
        composable(AppScreen.SETTINGS.name.lowercase()) {
            SettingsScreen(viewModel = viewModel)
        }
        composable(AppScreen.PRIVACY_SETTINGS.name.lowercase()) {
            PrivacySettingsScreen(viewModel = viewModel)
        }
        composable(AppScreen.CAMERA_STUDIO.name.lowercase()) {
            CameraStudioScreen(viewModel = viewModel)
        }
        composable(AppScreen.WALLPAPER_PICKER.name.lowercase()) {
            WallpaperPickerScreen(viewModel = viewModel)
        }
        composable(AppScreen.TIKTOK_LIVE.name.lowercase()) {
            TikTokLiveStreamScreen(viewModel = viewModel)
        }
        composable(AppScreen.SYNC_CONTACTS.name.lowercase()) {
            ContactsSyncScreen(viewModel = viewModel)
        }
        composable(AppScreen.CONTACT_QR_CODE.name.lowercase()) {
            ContactQrCodeScreen(viewModel = viewModel)
        }
        composable(AppScreen.LIVE_TRACKING.name.lowercase()) {
            LiveTrackingScreen(viewModel = viewModel)
        }
        composable(AppScreen.CHAT_BACKUP.name.lowercase()) {
            ChatBackupScreen(viewModel = viewModel)
        }
        composable(AppScreen.TWO_STEP_VERIFICATION.name.lowercase()) {
            TwoStepVerificationScreen(viewModel = viewModel)
        }
        composable(AppScreen.ENCRYPTION_KEYS.name.lowercase()) {
            EncryptionKeysScreen(viewModel = viewModel)
        }
        composable(AppScreen.ARCHIVED_CHATS.name.lowercase()) {
            ArchivedChatsScreen(viewModel = viewModel)
        }
        composable(AppScreen.LIVE_LOCATION_SHARING.name.lowercase()) {
            LiveLocationSharingScreen(viewModel = viewModel)
        }
        composable(AppScreen.LOCKED_CHATS.name.lowercase()) {
            LockedChatsScreen(viewModel = viewModel)
        }
        composable(AppScreen.STORAGE_MANAGEMENT.name.lowercase()) {
            StorageManagementScreen(viewModel = viewModel)
        }
        composable(AppScreen.META_AI_DIRECT.name.lowercase()) {
            MetaAiDirectScreen(viewModel = viewModel)
        }
        composable(AppScreen.APP_USAGE_STATISTICS.name.lowercase()) {
            AppUsageStatisticsScreen(viewModel = viewModel)
        }
    }
}

