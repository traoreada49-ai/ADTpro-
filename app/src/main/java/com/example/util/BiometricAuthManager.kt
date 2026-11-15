package com.example.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Result state of biometric availability check.
 */
sealed class BiometricStatus {
    object Available : BiometricStatus()
    object NoneEnrolled : BiometricStatus()
    object NoHardware : BiometricStatus()
    object HardwareUnavailable : BiometricStatus()
    object SecurityUpdateRequired : BiometricStatus()
    data class Unknown(val code: Int) : BiometricStatus()
}

/**
 * Helper utility to manage Biometric Authentication (Fingerprint & Face Unlock)
 * using AndroidX Biometric.
 */
object BiometricAuthManager {

    /**
     * Allowed authenticators: STRONG and WEAK biometrics.
     * We don't include DEVICE_CREDENTIAL here if using setNegativeButtonText.
     */
    const val AUTHENTICATORS_BIOMETRIC_ONLY = BIOMETRIC_STRONG or BIOMETRIC_WEAK
    const val AUTHENTICATORS_WITH_DEVICE_CREDENTIAL = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    /**
     * Checks if biometric hardware is present and user has enrolled biometric credentials.
     */
    fun checkBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(AUTHENTICATORS_BIOMETRIC_ONLY)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            else -> BiometricStatus.Unknown(biometricManager.canAuthenticate(AUTHENTICATORS_BIOMETRIC_ONLY))
        }
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return checkBiometricStatus(context) is BiometricStatus.Available
    }

    fun isHardwarePresent(context: Context): Boolean {
        val status = checkBiometricStatus(context)
        return status is BiometricStatus.Available || status is BiometricStatus.NoneEnrolled
    }

    fun getStatusDescription(context: Context): String {
        return when (checkBiometricStatus(context)) {
            is BiometricStatus.Available -> "Empreinte digitale / Visage configuré et prêt"
            is BiometricStatus.NoneEnrolled -> "Matériel présent mais aucune empreinte enregistrée dans Android"
            is BiometricStatus.NoHardware -> "Aucun capteur biométrique détecté sur l'appareil"
            is BiometricStatus.HardwareUnavailable -> "Capteur biométrique temporairement indisponible"
            is BiometricStatus.SecurityUpdateRequired -> "Mise à jour de sécurité requise pour la biométrie"
            is BiometricStatus.Unknown -> "État biométrique non disponible"
        }
    }

    /**
     * Launches the system settings screen allowing user to enroll fingerprints/face.
     */
    fun openBiometricEnrollSettings(context: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    AUTHENTICATORS_BIOMETRIC_ONLY
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }
        try {
            if (context !is Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(fallback)
            } catch (_: Exception) {}
        }
    }

    /**
     * Shows the BiometricPrompt dialog.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Déverrouillage ADT Pro",
        subtitle: String = "Confirmez votre empreinte digitale ou reconnaissance faciale pour continuer",
        negativeButtonText: String = "Utiliser le code PIN",
        onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
        onError: (errorCode: Int, errString: CharSequence) -> Unit,
        onFailed: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess(result)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Sécurité renforcée de l'application ADT Pro")
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(false)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
        return biometricPrompt
    }

    /**
     * Authenticates the user specifically to access locked conversations (fingerprint / face recognition).
     */
    fun authenticateForLockedChats(
        activity: FragmentActivity,
        title: String = "Conversations verrouillées",
        subtitle: String = "Confirmez votre empreinte digitale ou visage pour accéder aux conversations verrouillées",
        negativeButtonText: String = "Annuler",
        onSuccess: () -> Unit,
        onError: ((errorCode: Int, errString: CharSequence) -> Unit)? = null,
        onFailed: (() -> Unit)? = null
    ): BiometricPrompt {
        return showBiometricPrompt(
            activity = activity,
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            onSuccess = { onSuccess() },
            onError = { code, msg -> onError?.invoke(code, msg) },
            onFailed = { onFailed?.invoke() }
        )
    }
}
