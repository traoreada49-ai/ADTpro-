package com.example.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom

val Context.twoStepDataStore: DataStore<Preferences> by preferencesDataStore(name = "two_step_verification_prefs")

enum class RecoveryAccountType(val id: String, val displayName: String, val domainHint: String) {
    GOOGLE("GOOGLE", "Compte Google", "@gmail.com"),
    ICLOUD("ICLOUD", "Compte iCloud", "@icloud.com"),
    NONE("NONE", "Aucun", "");

    companion object {
        fun fromId(id: String?): RecoveryAccountType {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: NONE
        }
    }
}

sealed class PinVerificationResult {
    object Success : PinVerificationResult()
    data class WrongPin(val remainingAttempts: Int) : PinVerificationResult()
    data class LockedOut(val remainingSeconds: Long) : PinVerificationResult()
    object NotConfigured : PinVerificationResult()
}

data class TwoStepVerificationState(
    val isEnabled: Boolean = false,
    val recoveryAccountType: RecoveryAccountType = RecoveryAccountType.NONE,
    val recoveryEmail: String = "",
    val isEmailVerified: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntilTimestamp: Long = 0L,
    val requirePinOnLaunch: Boolean = true,
    val hasPin: Boolean = false
)

class TwoStepVerificationManager(private val context: Context) {

    private object PreferencesKeys {
        val IS_ENABLED = booleanPreferencesKey("two_step_is_enabled")
        val PIN_SALT = stringPreferencesKey("two_step_pin_salt")
        val PIN_HASH = stringPreferencesKey("two_step_pin_hash")
        val RECOVERY_ACCOUNT_TYPE = stringPreferencesKey("two_step_recovery_type")
        val RECOVERY_EMAIL = stringPreferencesKey("two_step_recovery_email")
        val IS_EMAIL_VERIFIED = booleanPreferencesKey("two_step_email_verified")
        val FAILED_ATTEMPTS = intPreferencesKey("two_step_failed_attempts")
        val LOCKOUT_UNTIL = longPreferencesKey("two_step_lockout_until")
        val REQUIRE_PIN_ON_LAUNCH = booleanPreferencesKey("two_step_require_pin_launch")
        val ACTIVE_RESET_CODE = stringPreferencesKey("two_step_active_reset_code")
        val RESET_CODE_TIMESTAMP = longPreferencesKey("two_step_reset_code_ts")
    }

    val verificationStateFlow: Flow<TwoStepVerificationState> = context.twoStepDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: false
            val pinHash = preferences[PreferencesKeys.PIN_HASH] ?: ""
            val typeStr = preferences[PreferencesKeys.RECOVERY_ACCOUNT_TYPE] ?: "NONE"
            val email = preferences[PreferencesKeys.RECOVERY_EMAIL] ?: ""
            val emailVerified = preferences[PreferencesKeys.IS_EMAIL_VERIFIED] ?: false
            val failedAttempts = preferences[PreferencesKeys.FAILED_ATTEMPTS] ?: 0
            val lockoutUntil = preferences[PreferencesKeys.LOCKOUT_UNTIL] ?: 0L
            val requirePin = preferences[PreferencesKeys.REQUIRE_PIN_ON_LAUNCH] ?: true

            TwoStepVerificationState(
                isEnabled = isEnabled && pinHash.isNotBlank(),
                recoveryAccountType = RecoveryAccountType.fromId(typeStr),
                recoveryEmail = email,
                isEmailVerified = emailVerified,
                failedAttempts = failedAttempts,
                lockoutUntilTimestamp = lockoutUntil,
                requirePinOnLaunch = requirePin,
                hasPin = pinHash.isNotBlank()
            )
        }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("$pin:$salt:ADT_PRO_2FA_PEPPER_2026".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Enables Two-Step Verification with a 6-digit PIN and linked Google or iCloud recovery account.
     */
    suspend fun enableTwoStep(
        pin: String,
        accountType: RecoveryAccountType,
        recoveryEmail: String
    ): Result<Unit> {
        val cleanPin = pin.trim()
        if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Le code PIN doit comporter exactement 6 chiffres"))
        }

        val cleanEmail = recoveryEmail.trim()
        if (cleanEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Format d'adresse e-mail invalide"))
        }

        val salt = generateSalt()
        val hash = hashPin(cleanPin, salt)

        context.twoStepDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = true
            preferences[PreferencesKeys.PIN_SALT] = salt
            preferences[PreferencesKeys.PIN_HASH] = hash
            preferences[PreferencesKeys.RECOVERY_ACCOUNT_TYPE] = accountType.id
            preferences[PreferencesKeys.RECOVERY_EMAIL] = cleanEmail
            preferences[PreferencesKeys.IS_EMAIL_VERIFIED] = cleanEmail.isNotBlank()
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL] = 0L
            preferences[PreferencesKeys.REQUIRE_PIN_ON_LAUNCH] = true
        }

        return Result.success(Unit)
    }

    /**
     * Disables Two-Step Verification after validating current PIN.
     */
    suspend fun disableTwoStep(currentPin: String): Result<Unit> {
        val prefs = context.twoStepDataStore.data.first()
        val storedHash = prefs[PreferencesKeys.PIN_HASH] ?: ""
        val salt = prefs[PreferencesKeys.PIN_SALT] ?: ""

        if (storedHash.isNotBlank()) {
            val inputHash = hashPin(currentPin.trim(), salt)
            if (inputHash != storedHash) {
                return Result.failure(IllegalArgumentException("Code PIN actuel incorrect"))
            }
        }

        context.twoStepDataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_ENABLED] = false
            preferences[PreferencesKeys.PIN_HASH] = ""
            preferences[PreferencesKeys.PIN_SALT] = ""
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL] = 0L
        }

        return Result.success(Unit)
    }

    /**
     * Changes the 6-digit PIN. Requires current PIN.
     */
    suspend fun changePin(currentPin: String, newPin: String): Result<Unit> {
        val cleanNew = newPin.trim()
        if (cleanNew.length != 6 || !cleanNew.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Le nouveau code PIN doit comporter 6 chiffres"))
        }

        val prefs = context.twoStepDataStore.data.first()
        val storedHash = prefs[PreferencesKeys.PIN_HASH] ?: ""
        val salt = prefs[PreferencesKeys.PIN_SALT] ?: ""

        if (storedHash.isNotBlank()) {
            val inputHash = hashPin(currentPin.trim(), salt)
            if (inputHash != storedHash) {
                return Result.failure(IllegalArgumentException("Code PIN actuel incorrect"))
            }
        }

        val newSalt = generateSalt()
        val newHash = hashPin(cleanNew, newSalt)

        context.twoStepDataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_SALT] = newSalt
            preferences[PreferencesKeys.PIN_HASH] = newHash
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = 0
            preferences[PreferencesKeys.LOCKOUT_UNTIL] = 0L
        }

        return Result.success(Unit)
    }

    /**
     * Changes the linked Google or iCloud recovery account.
     */
    suspend fun changeRecoveryAccount(
        currentPin: String,
        newAccountType: RecoveryAccountType,
        newEmail: String
    ): Result<Unit> {
        val cleanEmail = newEmail.trim()
        if (cleanEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Format d'adresse e-mail invalide"))
        }

        val prefs = context.twoStepDataStore.data.first()
        val storedHash = prefs[PreferencesKeys.PIN_HASH] ?: ""
        val salt = prefs[PreferencesKeys.PIN_SALT] ?: ""

        if (storedHash.isNotBlank()) {
            val inputHash = hashPin(currentPin.trim(), salt)
            if (inputHash != storedHash) {
                return Result.failure(IllegalArgumentException("Code PIN actuel incorrect"))
            }
        }

        context.twoStepDataStore.edit { preferences ->
            preferences[PreferencesKeys.RECOVERY_ACCOUNT_TYPE] = newAccountType.id
            preferences[PreferencesKeys.RECOVERY_EMAIL] = cleanEmail
            preferences[PreferencesKeys.IS_EMAIL_VERIFIED] = cleanEmail.isNotBlank()
        }

        return Result.success(Unit)
    }

    /**
     * Validates input PIN against stored hash with rate-limiting protection.
     */
    suspend fun verifyPin(inputPin: String): PinVerificationResult {
        val prefs = context.twoStepDataStore.data.first()
        val isEnabled = prefs[PreferencesKeys.IS_ENABLED] ?: false
        val storedHash = prefs[PreferencesKeys.PIN_HASH] ?: ""
        val salt = prefs[PreferencesKeys.PIN_SALT] ?: ""

        if (!isEnabled || storedHash.isBlank()) {
            return PinVerificationResult.NotConfigured
        }

        val now = System.currentTimeMillis()
        val lockoutUntil = prefs[PreferencesKeys.LOCKOUT_UNTIL] ?: 0L
        if (now < lockoutUntil) {
            val remainingSec = (lockoutUntil - now) / 1000L
            return PinVerificationResult.LockedOut(remainingSec.coerceAtLeast(1L))
        }

        val inputHash = hashPin(inputPin.trim(), salt)
        return if (inputHash == storedHash) {
            // Reset failed counter on success
            context.twoStepDataStore.edit { p ->
                p[PreferencesKeys.FAILED_ATTEMPTS] = 0
                p[PreferencesKeys.LOCKOUT_UNTIL] = 0L
            }
            PinVerificationResult.Success
        } else {
            val currentAttempts = (prefs[PreferencesKeys.FAILED_ATTEMPTS] ?: 0) + 1
            val maxAttempts = 5
            val remaining = (maxAttempts - currentAttempts).coerceAtLeast(0)

            val newLockout = if (currentAttempts >= maxAttempts) {
                // Lock out for 60 seconds
                now + 60_000L
            } else {
                0L
            }

            context.twoStepDataStore.edit { p ->
                p[PreferencesKeys.FAILED_ATTEMPTS] = currentAttempts
                p[PreferencesKeys.LOCKOUT_UNTIL] = newLockout
            }

            if (newLockout > 0L) {
                PinVerificationResult.LockedOut(60L)
            } else {
                PinVerificationResult.WrongPin(remaining)
            }
        }
    }

    /**
     * Generates a 6-digit emergency recovery code and records it for verification.
     */
    suspend fun generateRecoveryCode(): String {
        val random = SecureRandom()
        val code = String.format("%06d", random.nextInt(1_000_000))
        val now = System.currentTimeMillis()

        context.twoStepDataStore.edit { p ->
            p[PreferencesKeys.ACTIVE_RESET_CODE] = code
            p[PreferencesKeys.RESET_CODE_TIMESTAMP] = now
        }

        return code
    }

    /**
     * Resets the PIN using the recovery code sent to the linked Google/iCloud email.
     */
    suspend fun resetPinWithRecoveryCode(code: String, newPin: String): Result<Unit> {
        val cleanCode = code.trim()
        val cleanPin = newPin.trim()

        if (cleanPin.length != 6 || !cleanPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Le nouveau code PIN doit comporter 6 chiffres"))
        }

        val prefs = context.twoStepDataStore.data.first()
        val activeCode = prefs[PreferencesKeys.ACTIVE_RESET_CODE] ?: ""
        val timestamp = prefs[PreferencesKeys.RESET_CODE_TIMESTAMP] ?: 0L

        // Code expires after 15 minutes
        val isExpired = System.currentTimeMillis() - timestamp > 15 * 60 * 1000L

        if (activeCode.isBlank() || isExpired || cleanCode != activeCode) {
            return Result.failure(IllegalArgumentException("Code de récupération invalide ou expiré"))
        }

        val newSalt = generateSalt()
        val newHash = hashPin(cleanPin, newSalt)

        context.twoStepDataStore.edit { p ->
            p[PreferencesKeys.PIN_SALT] = newSalt
            p[PreferencesKeys.PIN_HASH] = newHash
            p[PreferencesKeys.ACTIVE_RESET_CODE] = ""
            p[PreferencesKeys.FAILED_ATTEMPTS] = 0
            p[PreferencesKeys.LOCKOUT_UNTIL] = 0L
        }

        return Result.success(Unit)
    }

    suspend fun setRequirePinOnLaunch(require: Boolean) {
        context.twoStepDataStore.edit { preferences ->
            preferences[PreferencesKeys.REQUIRE_PIN_ON_LAUNCH] = require
        }
    }
}
