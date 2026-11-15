package com.example.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

/**
 * UI State representing the authentic Firebase Phone Authentication lifecycle.
 */
sealed interface PhoneAuthState {
    data object Idle : PhoneAuthState
    data class SendingCode(val phoneNumber: String) : PhoneAuthState
    data class CodeSent(
        val verificationId: String,
        val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
        val phoneNumber: String,
        val autoRetrievedSmsCode: String? = null
    ) : PhoneAuthState
    data class VerifyingCode(val code: String) : PhoneAuthState
    data class Verified(
        val firebaseUid: String,
        val phoneNumber: String,
        val isNewUser: Boolean
    ) : PhoneAuthState
    data class Error(
        val message: String,
        val isRecoverable: Boolean = true,
        val rawCode: String? = null,
        val isProviderNotEnabled: Boolean = false
    ) : PhoneAuthState
}

/**
 * Manages real Firebase Phone Authentication (SMS OTP verification & sign-in).
 */
class FirebasePhoneAuthManager(
    private val context: Context
) {
    private val tag = "FirebasePhoneAuthMgr"

    private val _authState = MutableStateFlow<PhoneAuthState>(PhoneAuthState.Idle)
    val authState: StateFlow<PhoneAuthState> = _authState.asStateFlow()

    private var currentVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var lastRequestedPhone: String = ""

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "Erreur d'initialisation FirebaseApp/FirebaseAuth : ${e.message}", e)
            null
        }
    }

    val currentFirebaseUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    /**
     * Initiates Firebase Phone Authentication SMS verification for an E.164 phone number.
     */
    fun sendVerificationCode(
        activity: Activity?,
        phoneNumberE164: String,
        isResend: Boolean = false
    ) {
        val cleanPhone = phoneNumberE164.trim().replace(" ", "")
        if (!cleanPhone.startsWith("+") || cleanPhone.drop(1).length !in 7..15 || !cleanPhone.drop(1).all { it.isDigit() }) {
            _authState.value = PhoneAuthState.Error(
                message = "Format international E.164 invalide ($cleanPhone). Vérifiez l'indicatif pays et le numéro.",
                rawCode = "INVALID_E164"
            )
            return
        }

        val auth = firebaseAuth
        if (auth == null) {
            _authState.value = PhoneAuthState.Error(
                message = "Firebase Authentication n'est pas initialisé dans cette application.",
                isRecoverable = false,
                rawCode = "FIREBASE_NOT_INITIALIZED"
            )
            return
        }

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            _authState.value = PhoneAuthState.Error(
                message = "Une activité Android active est requise pour exécuter la vérification SMS / reCAPTCHA Firebase.",
                rawCode = "MISSING_ACTIVITY"
            )
            return
        }

        lastRequestedPhone = cleanPhone
        _authState.value = PhoneAuthState.SendingCode(cleanPhone)

        try {
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    Log.d(tag, "onVerificationCompleted: Vérification instantanée ou auto-lecture SMS")
                    val smsCode = credential.smsCode
                    if (!smsCode.isNullOrBlank() && currentVerificationId != null) {
                        _authState.value = PhoneAuthState.CodeSent(
                            verificationId = currentVerificationId!!,
                            resendToken = resendToken,
                            phoneNumber = cleanPhone,
                            autoRetrievedSmsCode = smsCode
                        )
                    }
                    signInWithCredential(credential, cleanPhone)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Log.e(tag, "onVerificationFailed: ${e::class.java.simpleName} - ${e.message}", e)
                    _authState.value = mapVerificationException(e)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    Log.d(tag, "onCodeSent: verificationId reçu pour $cleanPhone")
                    currentVerificationId = verificationId
                    resendToken = token
                    _authState.value = PhoneAuthState.CodeSent(
                        verificationId = verificationId,
                        resendToken = token,
                        phoneNumber = cleanPhone
                    )
                }

                override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
                    Log.d(tag, "onCodeAutoRetrievalTimeOut: verificationId=$verificationId")
                    currentVerificationId = verificationId
                }
            }

            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(cleanPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (isResend && resendToken != null) {
                optionsBuilder.setForceResendingToken(resendToken!!)
            }

            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        } catch (e: Exception) {
            Log.e(tag, "Exception verifyPhoneNumber: ${e.message}", e)
            _authState.value = PhoneAuthState.Error(
                message = "Impossible de lancer la vérification Firebase : ${e.localizedMessage ?: "erreur inconnue"}",
                rawCode = "VERIFY_PHONE_EXCEPTION"
            )
        }
    }

    /**
     * Verifies the 6-digit SMS OTP code entered by the user against Firebase Authentication.
     */
    fun verifySmsCode(smsCode: String, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        val cleanCode = smsCode.trim().filter { it.isDigit() }
        val verificationId = currentVerificationId

        if (cleanCode.length != 6) {
            val err = "Le code de vérification doit comporter exactement 6 chiffres."
            _authState.value = PhoneAuthState.Error(message = err, rawCode = "INVALID_CODE_LENGTH")
            onComplete(false, err)
            return
        }

        if (verificationId.isNullOrBlank()) {
            val err = "Session de vérification introuvable ou expirée. Veuillez redemander un code SMS."
            _authState.value = PhoneAuthState.Error(message = err, rawCode = "MISSING_VERIFICATION_ID")
            onComplete(false, err)
            return
        }

        if (firebaseAuth == null) {
            val err = "Firebase Authentication n'est pas disponible."
            _authState.value = PhoneAuthState.Error(message = err, isRecoverable = false, rawCode = "FIREBASE_NOT_INITIALIZED")
            onComplete(false, err)
            return
        }

        _authState.value = PhoneAuthState.VerifyingCode(cleanCode)

        try {
            val credential = PhoneAuthProvider.getCredential(verificationId, cleanCode)
            signInWithCredential(credential, lastRequestedPhone, onComplete)
        } catch (e: Exception) {
            val err = "Code de validation invalide : ${e.localizedMessage ?: "erreur de format"}"
            _authState.value = PhoneAuthState.Error(message = err, rawCode = "CREDENTIAL_BUILD_ERROR")
            onComplete(false, err)
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        phoneNumber: String,
        onComplete: (Boolean, String?) -> Unit = { _, _ -> }
    ) {
        val auth = firebaseAuth
        if (auth == null) {
            val err = "Firebase Authentication n'est pas initialisé."
            _authState.value = PhoneAuthState.Error(message = err, isRecoverable = false)
            onComplete(false, err)
            return
        }

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val uid = user?.uid
                    if (uid.isNullOrBlank()) {
                        val err = "Connexion Firebase incomplète : identifiant utilisateur manquant."
                        _authState.value = PhoneAuthState.Error(message = err, rawCode = "EMPTY_UID")
                        onComplete(false, err)
                        return@addOnCompleteListener
                    }
                    val isNew = task.result?.additionalUserInfo?.isNewUser ?: false
                    val verifiedPhone = user.phoneNumber?.takeIf { it.isNotBlank() } ?: phoneNumber
                    Log.d(tag, "Firebase Phone Auth réussie : uid=$uid, phone=$verifiedPhone, isNew=$isNew")
                    _authState.value = PhoneAuthState.Verified(
                        firebaseUid = uid,
                        phoneNumber = verifiedPhone,
                        isNewUser = isNew
                    )
                    onComplete(true, null)
                } else {
                    val mapped = mapSignInException(task.exception)
                    Log.e(tag, "Échec signInWithCredential: ${mapped.rawCode} - ${mapped.message}", task.exception)
                    _authState.value = mapped
                    onComplete(false, mapped.message)
                }
            }
    }

    private fun mapVerificationException(e: FirebaseException): PhoneAuthState.Error {
        val errorCode = (e as? FirebaseAuthException)?.errorCode
        val rawMessage = e.message.orEmpty()

        return when {
            rawMessage.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                errorCode == "ERROR_OPERATION_NOT_ALLOWED" -> {
                PhoneAuthState.Error(
                    message = "L'authentification par téléphone n'est pas encore activée dans votre console Firebase (Authentication > Sign-in method > Téléphone).",
                    isRecoverable = true,
                    rawCode = errorCode ?: "ERROR_OPERATION_NOT_ALLOWED",
                    isProviderNotEnabled = true
                )
            }
            e is FirebaseAuthInvalidCredentialsException -> {
                val msg = when (errorCode) {
                    "ERROR_INVALID_PHONE_NUMBER" -> "Le numéro saisi n'est pas reconnu comme un numéro valide par Firebase."
                    "ERROR_MISSING_PHONE_NUMBER" -> "Veuillez saisir un numéro de téléphone complet avec son indicatif pays."
                    else -> "Requête rejetée par Firebase (${errorCode ?: "identifiants ou certificat SHA invalide"})."
                }
                PhoneAuthState.Error(message = msg, rawCode = errorCode)
            }
            e is FirebaseTooManyRequestsException -> {
                PhoneAuthState.Error(
                    message = "Quota SMS dépassé ou trop de tentatives pour ce numéro. Patientez avant de réessayer.",
                    rawCode = "ERROR_TOO_MANY_REQUESTS"
                )
            }
            e is FirebaseAuthMissingActivityForRecaptchaException -> {
                PhoneAuthState.Error(
                    message = "Impossible d'afficher le contrôle de sécurité reCAPTCHA : activité Android requise.",
                    rawCode = "ERROR_MISSING_ACTIVITY_FOR_RECAPTCHA"
                )
            }
            e is FirebaseNetworkException -> {
                PhoneAuthState.Error(
                    message = "Erreur réseau : vérifiez votre connexion Internet pour contacter Firebase.",
                    rawCode = "ERROR_NETWORK_REQUEST_FAILED"
                )
            }
            rawMessage.contains("BILLING_NOT_ENABLED", ignoreCase = true) -> {
                PhoneAuthState.Error(
                    message = "L'envoi de SMS réels nécessite l'activation de la facturation Firebase ou l'ajout d'un numéro de test dans Authentication > Téléphone.",
                    rawCode = "BILLING_NOT_ENABLED",
                    isProviderNotEnabled = true
                )
            }
            else -> {
                PhoneAuthState.Error(
                    message = "Échec de l'envoi du code SMS Firebase : ${e.localizedMessage ?: "erreur inconnue"}",
                    rawCode = errorCode ?: e::class.java.simpleName
                )
            }
        }
    }

    private fun mapSignInException(e: Exception?): PhoneAuthState.Error {
        val errorCode = (e as? FirebaseAuthException)?.errorCode
        return when (errorCode) {
            "ERROR_INVALID_VERIFICATION_CODE" -> PhoneAuthState.Error(
                message = "Code SMS incorrect. Vérifiez les 6 chiffres reçus par SMS.",
                rawCode = errorCode
            )
            "ERROR_SESSION_EXPIRED" -> PhoneAuthState.Error(
                message = "Ce code SMS a expiré. Veuillez demander un nouveau code.",
                rawCode = errorCode
            )
            "ERROR_INVALID_CREDENTIAL" -> PhoneAuthState.Error(
                message = "Jeton de vérification invalide. Demandez un nouveau code SMS.",
                rawCode = errorCode
            )
            else -> PhoneAuthState.Error(
                message = e?.localizedMessage ?: "Échec de validation du code SMS auprès de Firebase.",
                rawCode = errorCode
            )
        }
    }

    fun clearError() {
        if (_authState.value is PhoneAuthState.Error) {
            _authState.value = if (currentVerificationId != null) {
                PhoneAuthState.CodeSent(
                    verificationId = currentVerificationId!!,
                    resendToken = resendToken,
                    phoneNumber = lastRequestedPhone
                )
            } else {
                PhoneAuthState.Idle
            }
        }
    }

    fun resetState() {
        _authState.value = PhoneAuthState.Idle
        currentVerificationId = null
        resendToken = null
        lastRequestedPhone = ""
    }

    fun signOut() {
        try {
            firebaseAuth?.signOut()
        } catch (e: Exception) {
            Log.w(tag, "Erreur lors de la déconnexion Firebase : ${e.message}")
        }
        resetState()
    }
}
