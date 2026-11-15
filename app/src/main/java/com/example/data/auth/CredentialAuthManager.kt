package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result of retrieving stored phone credentials via Android Credential Manager.
 */
data class StoredCredential(
    val phoneNumber: String,
    val tokenOrSecret: String
)

data class DetectedCredential(
    val phoneNumber: String,
    val displayName: String = ""
)

/**
 * Manages Android Credential Manager operations for ADT Pro:
 * - Storing validated phone credentials securely upon registration/login
 * - Fast 1-tap sign-in and retrieval of saved phone numbers via Credential Manager
 * - Clearing credentials on logout
 */
class CredentialAuthManager(private val context: Context) {
    private val tag = "CredentialAuthMgr"
    private val credentialManager: CredentialManager by lazy {
        CredentialManager.create(context)
    }

    /**
     * Saves the verified phone number credential into Android Credential Manager.
     * Uses the user's phone number as the unique credential ID.
     */
    suspend fun savePhoneCredential(
        activity: Activity,
        phoneNumber: String,
        displayName: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanPhone = phoneNumber.trim().replace(" ", "")
        if (cleanPhone.isBlank()) return@withContext false

        try {
            val passSecret = "ADT_SECURE_PHONE_TOKEN_${cleanPhone.takeLast(6)}"
            val createPasswordRequest = CreatePasswordRequest(
                id = cleanPhone,
                password = passSecret
            )

            credentialManager.createCredential(
                context = activity,
                request = createPasswordRequest
            )
            Log.d(tag, "Phone credential successfully saved in Credential Manager for $cleanPhone")
            true
        } catch (e: CreateCredentialCancellationException) {
            Log.d(tag, "User dismissed Credential Manager save prompt")
            false
        } catch (e: CreateCredentialException) {
            Log.w(tag, "Credential Manager create error: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.w(tag, "Credential Manager unexpected error: ${e.message}", e)
            false
        }
    }

    /**
     * Retrieves a stored phone number credential from Android Credential Manager.
     * Triggers the native system bottom sheet for 1-tap phone selection.
     */
    suspend fun getStoredPhoneCredential(activity: Activity): StoredCredential? = withContext(Dispatchers.IO) {
        try {
            val getPasswordOption = GetPasswordOption()
            val getCredentialRequest = GetCredentialRequest.Builder()
                .addCredentialOption(getPasswordOption)
                .build()

            val result = credentialManager.getCredential(
                context = activity,
                request = getCredentialRequest
            )

            val credential = result.credential
            if (credential is PasswordCredential) {
                Log.d(tag, "Retrieved credential from Credential Manager: id=${credential.id}")
                return@withContext StoredCredential(
                    phoneNumber = credential.id,
                    tokenOrSecret = credential.password
                )
            }
            null
        } catch (e: GetCredentialCancellationException) {
            Log.d(tag, "User cancelled Credential Manager selector")
            null
        } catch (e: GetCredentialException) {
            Log.i(tag, "No saved credential found or user cancelled: ${e.message}")
            null
        } catch (e: Exception) {
            Log.w(tag, "Error retrieving credential: ${e.message}", e)
            null
        }
    }

    /**
     * Clears credentials session state on logout.
     */
    suspend fun clearCredentialState(): Boolean = withContext(Dispatchers.IO) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Log.d(tag, "Credential Manager state cleared successfully")
            true
        } catch (e: ClearCredentialException) {
            Log.w(tag, "Failed to clear credential state: ${e.message}", e)
            false
        } catch (e: Exception) {
            Log.w(tag, "Unexpected error clearing credential state: ${e.message}", e)
            false
        }
    }
}
