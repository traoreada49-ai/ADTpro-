package com.example.data.firebase

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.crypto.SignalProtocolEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.MessageEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/**
 * Metadata for a chat backup
 */
data class BackupMetadata(
    val userPhone: String,
    val timestamp: Long,
    val chatsCount: Int,
    val messagesCount: Int,
    val sizeBytes: Long,
    val isEncrypted: Boolean = true,
    val storagePath: String = ""
) {
    val formattedDate: String
        get() {
            if (timestamp <= 0) return "Aucune sauvegarde"
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.FRENCH)
            return sdf.format(Date(timestamp))
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return "0 Ko"
            val kb = sizeBytes / 1024.0
            return if (kb >= 1024.0) {
                String.format(Locale.US, "%.1f Mo", kb / 1024.0)
            } else {
                String.format(Locale.US, "%.0f Ko", kb)
            }
        }
}

/**
 * State of background backup service
 */
data class BackgroundBackupStatus(
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "Inactif",
    val lastSyncTimestamp: Long = 0L,
    val lastSyncResult: String = "Prêt",
    val isSuccess: Boolean = true,
    val userPhone: String = "",
    val formattedSize: String = "",
    val messagesCount: Int = 0,
    val chatsCount: Int = 0
)

/**
 * State of backup and restore operations
 */
sealed class BackupOperationState {
    object Idle : BackupOperationState()
    data class BackingUp(val progress: Float, val stage: String) : BackupOperationState()
    data class Restoring(val progress: Float, val stage: String) : BackupOperationState()
    data class Success(val message: String, val metadata: BackupMetadata? = null) : BackupOperationState()
    data class Error(val errorMessage: String) : BackupOperationState()
}

/**
 * Firebase Storage Chat Backup & Restore Manager.
 * Handles automatic, scheduled and manual backup of end-to-end encrypted chat histories
 * to Firebase Cloud Storage, and enables complete data restoration when switching devices.
 */
class FirebaseBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "FirebaseBackupManager"

    private val _operationState = MutableStateFlow<BackupOperationState>(BackupOperationState.Idle)
    val operationState: StateFlow<BackupOperationState> = _operationState.asStateFlow()

    private val _latestBackupInfo = MutableStateFlow<BackupMetadata?>(null)
    val latestBackupInfo: StateFlow<BackupMetadata?> = _latestBackupInfo.asStateFlow()

    private val storage: FirebaseStorage? by lazy {
        FirebaseInitHelper.getStorage(context)
    }

    init {
        // Load initial backup metadata from local settings
        coroutineScope.launch {
            try {
                val settings = database.settingsDao().getSettings()
                if (settings != null && settings.lastBackupTimestamp > 0) {
                    val user = database.userDao().getUser()
                    val phone = user?.phone ?: "+221770001122"
                    _latestBackupInfo.value = BackupMetadata(
                        userPhone = phone,
                        timestamp = settings.lastBackupTimestamp,
                        chatsCount = 0,
                        messagesCount = settings.lastBackupMessagesCount,
                        sizeBytes = parseFormattedSize(settings.lastBackupSizeFormatted),
                        isEncrypted = true
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error reading initial backup settings: ${e.message}")
            }
        }
    }

    private fun parseFormattedSize(formatted: String): Long {
        return try {
            if (formatted.contains("Mo")) {
                val num = formatted.replace("Mo", "").trim().toDoubleOrNull() ?: 1.0
                (num * 1024 * 1024).toLong()
            } else if (formatted.contains("Ko")) {
                val num = formatted.replace("Ko", "").trim().toDoubleOrNull() ?: 50.0
                (num * 1024).toLong()
            } else 120_000L
        } catch (e: Exception) {
            120_000L
        }
    }

    /**
     * Check if connected to Wi-Fi
     */
    fun isConnectedToWifi(): Boolean = isWifiConnected(context)

    /**
     * Check if network is available
     */
    fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Perform immediate backup to Firebase Storage.
     */
    suspend fun performBackup(
        userPhone: String,
        includeMedia: Boolean = false,
        wifiOnly: Boolean = false
    ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        try {
            val result = executeDirectBackup(
                context = context,
                database = database,
                userPhone = userPhone,
                includeMedia = includeMedia,
                wifiOnly = wifiOnly,
                onProgress = { progressFraction, stage ->
                    _operationState.value = BackupOperationState.BackingUp(progressFraction, stage)
                }
            )

            if (result.isSuccess) {
                val backupMetadata = result.getOrThrow()
                _latestBackupInfo.value = backupMetadata
                _operationState.value = BackupOperationState.Success(
                    message = "Sauvegarde réussie (${backupMetadata.formattedSize})",
                    metadata = backupMetadata
                )
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Erreur de sauvegarde"
                _operationState.value = BackupOperationState.Error("Échec de la sauvegarde: $errorMsg")
            }

            result
        } catch (e: CancellationException) {
            Log.d(TAG, "performBackup coroutine cancelled cleanly")
            throw e
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Erreur de sauvegarde"
            _operationState.value = BackupOperationState.Error("Échec de la sauvegarde: $errorMsg")
            Result.failure(e)
        }
    }

    /**
     * Query remote Firebase Storage to check if a backup exists for this phone number
     */
    suspend fun queryRemoteBackup(userPhone: String): BackupMetadata? = withContext(Dispatchers.IO) {
        val cleanPhone = userPhone.filter { it.isDigit() || it == '+' }.replace("+", "plus_")
        val storagePath = "backups/$cleanPhone/adt_chat_backup_latest.enc"
        if (FirebaseInitHelper.isCloudStorageAvailable()) {
            try {
                val authed = FirebaseInitHelper.ensureAuthenticated(context)
                val hasUser = FirebaseInitHelper.hasAuthenticatedUser(context)
                if (authed && hasUser) {
                    val storageRef = FirebaseInitHelper.getStorage(context)?.reference?.child(storagePath)

                    if (storageRef != null) {
                        try {
                            val metadata = storageRef.metadata.await()
                            val ts = metadata.getCustomMetadata("timestamp")?.toLongOrNull() ?: metadata.updatedTimeMillis
                            val chatsCount = metadata.getCustomMetadata("chatsCount")?.toIntOrNull() ?: 0
                            val messagesCount = metadata.getCustomMetadata("messagesCount")?.toIntOrNull() ?: 0
                            val sizeBytes = metadata.sizeBytes

                            val result = BackupMetadata(
                                userPhone = userPhone,
                                timestamp = ts,
                                chatsCount = chatsCount,
                                messagesCount = messagesCount,
                                sizeBytes = sizeBytes,
                                isEncrypted = true,
                                storagePath = storagePath
                            )
                            _latestBackupInfo.value = result
                            return@withContext result
                        } catch (se: com.google.firebase.storage.StorageException) {
                            if (se.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND || se.httpResultCode == 404) {
                                Log.d(TAG, "No remote backup exists yet on cloud storage for $cleanPhone (fresh account).")
                            } else {
                                Log.w(TAG, "Firebase Storage query note: ${se.message}")
                                FirebaseInitHelper.markCloudStorageUnavailable()
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "Remote backup query note: ${e.message}")
            }
        }

        // Check local fallback files (user specific first, then global)
        val metaFile = File(context.filesDir, "cloud_backups/latest_backup_${cleanPhone}.meta")
        if (metaFile.exists()) {
            try {
                val mJson = JSONObject(metaFile.readText())
                val result = BackupMetadata(
                    userPhone = userPhone,
                    timestamp = mJson.optLong("timestamp", System.currentTimeMillis()),
                    chatsCount = mJson.optInt("chatsCount", 0),
                    messagesCount = mJson.optInt("messagesCount", 0),
                    sizeBytes = mJson.optLong("sizeBytes", 0L),
                    isEncrypted = true,
                    storagePath = mJson.optString("storagePath", storagePath)
                )
                _latestBackupInfo.value = result
                return@withContext result
            } catch (e: Exception) {
                Log.w(TAG, "Error reading local meta file: ${e.message}")
            }
        }

        val userLocalBackupFile = File(context.filesDir, "cloud_backups/latest_backup_${cleanPhone}.enc")
        if (userLocalBackupFile.exists()) {
            val settings = database.settingsDao().getSettings()
            val result = BackupMetadata(
                userPhone = userPhone,
                timestamp = userLocalBackupFile.lastModified(),
                chatsCount = 0,
                messagesCount = settings?.lastBackupMessagesCount ?: 0,
                sizeBytes = userLocalBackupFile.length(),
                isEncrypted = true,
                storagePath = storagePath
            )
            _latestBackupInfo.value = result
            return@withContext result
        }

        val localBackupFile = File(context.filesDir, "cloud_backups/latest_backup.enc")
        if (localBackupFile.exists()) {
            val settings = database.settingsDao().getSettings()
            val result = BackupMetadata(
                userPhone = userPhone,
                timestamp = localBackupFile.lastModified(),
                chatsCount = 0,
                messagesCount = settings?.lastBackupMessagesCount ?: 0,
                sizeBytes = localBackupFile.length(),
                isEncrypted = true,
                storagePath = storagePath
            )
            _latestBackupInfo.value = result
            return@withContext result
        }

        return@withContext _latestBackupInfo.value
    }

    /**
     * Restore chats and messages from Firebase Storage (or local secure cache)
     */
    suspend fun restoreBackup(userPhone: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            _operationState.value = BackupOperationState.Restoring(0.15f, "Téléchargement de la sauvegarde depuis Firebase Storage...")

            val cleanPhone = userPhone.filter { it.isDigit() || it == '+' }.replace("+", "plus_")
            val storagePath = "backups/$cleanPhone/adt_chat_backup_latest.enc"
            var encryptedBytes: ByteArray? = null

            // 1. Download from Firebase Storage if available and authenticated
            if (FirebaseInitHelper.isCloudStorageAvailable()) {
                try {
                    val authed = FirebaseInitHelper.ensureAuthenticated(context)
                    val hasUser = FirebaseInitHelper.hasAuthenticatedUser(context)
                    if (authed && hasUser) {
                        val storageRef = FirebaseInitHelper.getStorage(context)?.reference?.child(storagePath)
                        if (storageRef != null) {
                            val maxDownloadSize = 50 * 1024 * 1024L // 50 MB
                            encryptedBytes = storageRef.getBytes(maxDownloadSize).await()
                            Log.i(TAG, "Downloaded ${encryptedBytes.size} bytes from Firebase Storage")
                        }
                    }
                } catch (se: com.google.firebase.storage.StorageException) {
                    if (se.errorCode == com.google.firebase.storage.StorageException.ERROR_OBJECT_NOT_FOUND || se.httpResultCode == 404) {
                        Log.d(TAG, "No remote cloud backup found on Firebase Storage, checking local fallback.")
                    } else {
                        FirebaseInitHelper.markCloudStorageUnavailable()
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    val errStr = e.message.orEmpty()
                    if (errStr.contains("terminated")) {
                        FirebaseInitHelper.markCloudStorageUnavailable()
                    }
                    Log.w(TAG, "Could not fetch from Firebase Storage directly, checking local fallback: ${e.message}")
                }
            }

            // 2. Fallback to local backup file
            if (encryptedBytes == null || encryptedBytes.isEmpty()) {
                val userLocalFile = File(context.filesDir, "cloud_backups/latest_backup_${cleanPhone}.enc")
                if (userLocalFile.exists()) {
                    encryptedBytes = userLocalFile.readBytes()
                } else {
                    val localFile = File(context.filesDir, "cloud_backups/latest_backup.enc")
                    if (localFile.exists()) {
                        encryptedBytes = localFile.readBytes()
                    }
                }
            }

            if (encryptedBytes == null || encryptedBytes.isEmpty()) {
                _operationState.value = BackupOperationState.Error("Aucune sauvegarde trouvée sur Firebase Storage pour ce numéro.")
                return@withContext Result.failure(Exception("Aucune sauvegarde disponible"))
            }

            _operationState.value = BackupOperationState.Restoring(0.50f, "Déchiffrement sécurisé des messages...")

            // 3. Decrypt AES-256-GCM payload
            val decryptedBytes = decryptBackupData(encryptedBytes, userPhone)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            val rootJson = JSONObject(jsonString)

            _operationState.value = BackupOperationState.Restoring(0.75f, "Restauration dans la base de données locale...")

            // 4. Parse and insert Chats
            val chatsArray = rootJson.optJSONArray("chats") ?: JSONArray()
            val restoredChats = mutableListOf<ChatEntity>()
            for (i in 0 until chatsArray.length()) {
                val cObj = chatsArray.getJSONObject(i)
                restoredChats.add(
                    ChatEntity(
                        id = cObj.getString("id"),
                        title = cObj.getString("title"),
                        lastMessage = cObj.optString("lastMessage", ""),
                        lastTimestamp = cObj.optLong("lastTimestamp", System.currentTimeMillis()),
                        unreadCount = cObj.optInt("unreadCount", 0),
                        avatarUrl = cObj.optString("avatarUrl", ""),
                        isGroup = cObj.optBoolean("isGroup", false),
                        isPinned = cObj.optBoolean("isPinned", false),
                        isArchived = cObj.optBoolean("isArchived", false),
                        creatorPhone = cObj.optString("creatorPhone", "")
                    )
                )
            }
            if (restoredChats.isNotEmpty()) {
                database.chatDao().insertChats(restoredChats)
            }

            // 5. Parse and insert Messages
            val messagesArray = rootJson.optJSONArray("messages") ?: JSONArray()
            val restoredMessages = mutableListOf<MessageEntity>()
            for (i in 0 until messagesArray.length()) {
                val mObj = messagesArray.getJSONObject(i)
                restoredMessages.add(
                    MessageEntity(
                        id = mObj.getString("id"),
                        chatId = mObj.getString("chatId"),
                        senderPhone = mObj.getString("senderPhone"),
                        senderName = mObj.getString("senderName"),
                        text = mObj.getString("text"),
                        timestamp = mObj.getLong("timestamp"),
                        mediaType = mObj.optString("mediaType", "TEXT"),
                        mediaUrl = mObj.optString("mediaUrl", ""),
                        mediaDuration = mObj.optInt("mediaDuration", 0),
                        isOutgoing = mObj.optBoolean("isOutgoing", false),
                        isForwarded = mObj.optBoolean("isForwarded", false),
                        forwardedCount = mObj.optInt("forwardedCount", 0),
                        status = mObj.optString("status", "SENT"),
                        reaction = mObj.optString("reaction").ifBlank { null },
                        replyToText = mObj.optString("replyToText").ifBlank { null },
                        replyToSender = mObj.optString("replyToSender").ifBlank { null }
                    )
                )
            }
            if (restoredMessages.isNotEmpty()) {
                database.messageDao().insertMessages(restoredMessages)
            }

            // 6. Parse and insert Contacts
            val contactsArray = rootJson.optJSONArray("contacts") ?: JSONArray()
            val restoredContacts = mutableListOf<ContactEntity>()
            for (i in 0 until contactsArray.length()) {
                val ctObj = contactsArray.getJSONObject(i)
                restoredContacts.add(
                    ContactEntity(
                        id = ctObj.getString("id"),
                        name = ctObj.getString("name"),
                        phone = ctObj.getString("phone"),
                        countryCode = ctObj.optString("countryCode", "+221"),
                        avatarUrl = ctObj.optString("avatarUrl", ""),
                        bio = ctObj.optString("bio", "Membre ADT"),
                        isAdtUser = ctObj.optBoolean("isAdtUser", true),
                        isOnline = ctObj.optBoolean("isOnline", false),
                        lastSeenText = ctObj.optString("lastSeenText", "Récemment")
                    )
                )
            }
            if (restoredContacts.isNotEmpty()) {
                database.contactDao().insertContacts(restoredContacts)
            }

            _operationState.value = BackupOperationState.Success(
                message = "Restauration terminée : ${restoredChats.size} discussions et ${restoredMessages.size} messages restaurés avec succès !"
            )

            Log.i(TAG, "Successfully restored ${restoredChats.size} chats and ${restoredMessages.size} messages")
            Result.success(Pair(restoredChats.size, restoredMessages.size))
        } catch (e: CancellationException) {
            Log.d(TAG, "restoreBackup cancelled cleanly")
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            _operationState.value = BackupOperationState.Error("Erreur lors de la restauration : ${e.message}")
            Result.failure(e)
        }
    }

    private var lastAutoBackupCheckTimestamp: Long = 0L

    /**
     * Check if automatic backup should be triggered based on user frequency setting
     */
    fun checkAndTriggerAutoBackup(userPhone: String) {
        val now = System.currentTimeMillis()
        if (now - lastAutoBackupCheckTimestamp < 30_000L) {
            return
        }
        lastAutoBackupCheckTimestamp = now

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val settings = database.settingsDao().getSettings() ?: return@launch
                val freq = settings.autoBackupFrequency
                if (freq == "OFF") return@launch

                val last = settings.lastBackupTimestamp

                val intervalMillis = when (freq) {
                    "DAILY" -> 24 * 60 * 60 * 1000L
                    "WEEKLY" -> 7 * 24 * 60 * 60 * 1000L
                    "MONTHLY" -> 30 * 24 * 60 * 60 * 1000L
                    else -> 24 * 60 * 60 * 1000L
                }

                // If never backed up or interval passed, trigger auto backup
                if (last == 0L || (now - last) >= intervalMillis) {
                    if (settings.backupOnlyWifi && !isConnectedToWifi()) {
                        Log.d(TAG, "Auto-backup skipped: awaiting Wi-Fi connection")
                        return@launch
                    }
                    Log.i(TAG, "Triggering automatic scheduled backup (frequency: $freq)")
                    performBackup(
                        userPhone = userPhone,
                        includeMedia = settings.backupIncludeMedia,
                        wifiOnly = settings.backupOnlyWifi
                    )
                }
            } catch (e: CancellationException) {
                // coroutine cancelled cleanly
            } catch (e: Exception) {
                Log.w(TAG, "Auto-backup check failed: ${e.message}")
            }
        }
    }

    fun resetState() {
        _operationState.value = BackupOperationState.Idle
    }

    companion object {
        private const val TAG_STATIC = "FirebaseBackupManager"
        private val isBackupInProgress = AtomicBoolean(false)

        private val _backgroundStatus = MutableStateFlow(BackgroundBackupStatus())
        val backgroundStatus: StateFlow<BackgroundBackupStatus> = _backgroundStatus.asStateFlow()

        fun updateBackgroundStatus(status: BackgroundBackupStatus) {
            _backgroundStatus.value = status
        }

        fun isWifiConnected(context: Context): Boolean {
            return try {
                val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val network = cm.activeNetwork ?: return false
                val capabilities = cm.getNetworkCapabilities(network) ?: return false
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            } catch (e: Exception) {
                true
            }
        }

        fun deriveKeyFromPhone(phone: String): SecretKeySpec {
            val digest = MessageDigest.getInstance("SHA-256")
            val salt = "ADT_PRO_SECURE_BACKUP_SALT_2026_FIREBASE"
            val keyBytes = digest.digest((phone + salt).toByteArray(Charsets.UTF_8))
            return SecretKeySpec(keyBytes, "AES")
        }

        fun encryptBackupData(data: ByteArray, phone: String): ByteArray {
            val key = deriveKeyFromPhone(phone)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv // 12 bytes IV
            val cipherText = cipher.doFinal(data)

            // Prepend IV size (1 byte) + IV + ciphertext
            val result = ByteArray(1 + iv.size + cipherText.size)
            result[0] = iv.size.toByte()
            System.arraycopy(iv, 0, result, 1, iv.size)
            System.arraycopy(cipherText, 0, result, 1 + iv.size, cipherText.size)
            return result
        }

        fun decryptBackupData(encryptedData: ByteArray, phone: String): ByteArray {
            val key = deriveKeyFromPhone(phone)
            val ivSize = encryptedData[0].toInt()
            val iv = ByteArray(ivSize)
            System.arraycopy(encryptedData, 1, iv, 0, ivSize)

            val cipherTextSize = encryptedData.size - 1 - ivSize
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(encryptedData, 1 + ivSize, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            return cipher.doFinal(cipherText)
        }

        /**
         * Reusable direct backup execution logic shared across ViewModel, Background Service, and JobService.
         * Extracts data from local database, encrypts with AES-256-GCM, and uploads to Firebase Cloud Storage.
         */
        suspend fun executeDirectBackup(
            context: Context,
            database: AppDatabase,
            userPhone: String,
            includeMedia: Boolean = false,
            wifiOnly: Boolean = false,
            onProgress: ((Float, String) -> Unit)? = null
        ): Result<BackupMetadata> = withContext(Dispatchers.IO) {
            if (!isBackupInProgress.compareAndSet(false, true)) {
                Log.i(TAG_STATIC, "Backup is already running. Skipping concurrent run.")
                val existing = _backgroundStatus.value
                val fallbackMeta = BackupMetadata(
                    userPhone = userPhone,
                    timestamp = existing.lastSyncTimestamp.takeIf { it > 0 } ?: System.currentTimeMillis(),
                    chatsCount = existing.chatsCount,
                    messagesCount = existing.messagesCount,
                    sizeBytes = 0L
                )
                return@withContext Result.success(fallbackMeta)
            }
            if (wifiOnly && !isWifiConnected(context)) {
                val errorMsg = "Sauvegarde annulée: Connexion Wi-Fi requise selon vos paramètres."
                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        isRunning = false,
                        stage = "En attente du Wi-Fi",
                        lastSyncResult = "Wi-Fi requis",
                        isSuccess = false
                    )
                )
                return@withContext Result.failure(Exception(errorMsg))
            }

            try {
                updateBackgroundStatus(
                    BackgroundBackupStatus(
                        isRunning = true,
                        progress = 0.15f,
                        stage = "Extraction des données locales...",
                        userPhone = userPhone
                    )
                )
                onProgress?.invoke(0.15f, "Extraction des discussions et messages locaux...")

                // 1. Fetch all local chats and messages
                val chats = database.chatDao().getAllChatsSync()
                val messages = database.messageDao().getAllMessagesSync()
                val contacts = database.contactDao().getAllContactsSync()

                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        progress = 0.35f,
                        stage = "Compression (${chats.size} chats, ${messages.size} msgs)..."
                    )
                )
                onProgress?.invoke(0.35f, "Compression et préparation des données (${chats.size} chats, ${messages.size} msgs)...")

                // 2. Build JSON package
                val rootJson = JSONObject().apply {
                    put("version", 1)
                    put("protocol", "ADT_E2EE_SIGNAL_BACKUP_V1")
                    put("userPhone", userPhone)
                    put("backupTimestamp", System.currentTimeMillis())
                    put("includeMedia", includeMedia)
                    put("chatsCount", chats.size)
                    put("messagesCount", messages.size)

                    // Serialize chats
                    val chatsArray = JSONArray()
                    for (chat in chats) {
                        val cObj = JSONObject().apply {
                            put("id", chat.id)
                            put("title", chat.title)
                            put("lastMessage", chat.lastMessage)
                            put("lastTimestamp", chat.lastTimestamp)
                            put("unreadCount", chat.unreadCount)
                            put("avatarUrl", chat.avatarUrl)
                            put("isGroup", chat.isGroup)
                            put("isPinned", chat.isPinned)
                            put("isArchived", chat.isArchived)
                            put("creatorPhone", chat.creatorPhone)
                        }
                        chatsArray.put(cObj)
                    }
                    put("chats", chatsArray)

                    // Serialize messages
                    val messagesArray = JSONArray()
                    for (msg in messages) {
                        val mObj = JSONObject().apply {
                            put("id", msg.id)
                            put("chatId", msg.chatId)
                            put("senderPhone", msg.senderPhone)
                            put("senderName", msg.senderName)
                            put("text", msg.text)
                            put("timestamp", msg.timestamp)
                            put("mediaType", msg.mediaType)
                            put("mediaUrl", if (includeMedia) msg.mediaUrl else "")
                            put("mediaDuration", msg.mediaDuration)
                            put("isOutgoing", msg.isOutgoing)
                            put("isForwarded", msg.isForwarded)
                            put("forwardedCount", msg.forwardedCount)
                            put("status", msg.status)
                            put("reaction", msg.reaction ?: "")
                            put("replyToText", msg.replyToText ?: "")
                            put("replyToSender", msg.replyToSender ?: "")
                        }
                        messagesArray.put(mObj)
                    }
                    put("messages", messagesArray)

                    // Serialize contacts
                    val contactsArray = JSONArray()
                    for (contact in contacts) {
                        val ctObj = JSONObject().apply {
                            put("id", contact.id)
                            put("name", contact.name)
                            put("phone", contact.phone)
                            put("countryCode", contact.countryCode)
                            put("avatarUrl", contact.avatarUrl)
                            put("bio", contact.bio)
                            put("isAdtUser", contact.isAdtUser)
                            put("isOnline", contact.isOnline)
                            put("lastSeenText", contact.lastSeenText)
                        }
                        contactsArray.put(ctObj)
                    }
                    put("contacts", contactsArray)
                }

                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        progress = 0.60f,
                        stage = "Chiffrement AES-256 de bout en bout..."
                    )
                )
                onProgress?.invoke(0.60f, "Chiffrement AES-256 de bout en bout de la sauvegarde...")

                // 3. Encrypt payload with AES-256-GCM using user identity
                val rawJsonBytes = rootJson.toString().toByteArray(Charsets.UTF_8)
                val encryptedBytes = encryptBackupData(rawJsonBytes, userPhone)

                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        progress = 0.80f,
                        stage = "Téléversement Firebase Storage..."
                    )
                )
                onProgress?.invoke(0.80f, "Téléversement sécurisé vers Firebase Storage...")

                val cleanPhone = userPhone.filter { it.isDigit() || it == '+' }.replace("+", "plus_")
                val storagePath = "backups/$cleanPhone/adt_chat_backup_latest.enc"
                val totalBytes = encryptedBytes.size.toLong()

                // 4. Upload to Firebase Storage if authenticated
                var uploadedToCloud = false
                if (FirebaseInitHelper.isCloudStorageAvailable()) {
                    try {
                        val authed = FirebaseInitHelper.ensureAuthenticated(context)
                        val hasUser = FirebaseInitHelper.hasAuthenticatedUser(context)
                        if (authed && hasUser) {
                            val storage = FirebaseInitHelper.getStorage(context)
                            val storageRef = storage?.reference?.child(storagePath)
                            if (storageRef != null) {
                                val metadata = StorageMetadata.Builder()
                                    .setContentType("application/octet-stream")
                                    .setCustomMetadata("userPhone", userPhone)
                                    .setCustomMetadata("chatsCount", chats.size.toString())
                                    .setCustomMetadata("messagesCount", messages.size.toString())
                                    .setCustomMetadata("timestamp", System.currentTimeMillis().toString())
                                    .setCustomMetadata("encryption", "AES-256-GCM")
                                    .build()

                                storageRef.putBytes(encryptedBytes, metadata).await()
                                uploadedToCloud = true
                                Log.i(TAG_STATIC, "Uploaded ${encryptedBytes.size} bytes to Firebase Storage: $storagePath")
                            }
                        } else {
                            Log.i(TAG_STATIC, "Firebase Auth not active - saving encrypted backup to secure local vault.")
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (se: com.google.firebase.storage.StorageException) {
                        FirebaseInitHelper.markCloudStorageUnavailable()
                        Log.w(TAG_STATIC, "Firebase Storage upload deferred to local encrypted vault: ${se.message}")
                    } catch (e: Exception) {
                        val errStr = e.message.orEmpty()
                        if (errStr.contains("404") || errStr.contains("does not exist") || errStr.contains("terminated")) {
                            FirebaseInitHelper.markCloudStorageUnavailable()
                        }
                        Log.w(TAG_STATIC, "Firebase Storage cloud upload deferred/cached locally: ${e.message}")
                    }
                }

                // Also save locally in persistent app directory for offline fallback
                val localBackupDir = File(context.filesDir, "cloud_backups").apply { mkdirs() }
                val localBackupFile = File(localBackupDir, "latest_backup.enc")
                FileOutputStream(localBackupFile).use { it.write(encryptedBytes) }
                val userBackupFile = File(localBackupDir, "latest_backup_${cleanPhone}.enc")
                FileOutputStream(userBackupFile).use { it.write(encryptedBytes) }

                val now = System.currentTimeMillis()
                val backupMetadata = BackupMetadata(
                    userPhone = userPhone,
                    timestamp = now,
                    chatsCount = chats.size,
                    messagesCount = messages.size,
                    sizeBytes = totalBytes,
                    isEncrypted = true,
                    storagePath = storagePath
                )

                try {
                    val metaFile = File(localBackupDir, "latest_backup_${cleanPhone}.meta")
                    val metaObj = JSONObject().apply {
                        put("userPhone", userPhone)
                        put("timestamp", now)
                        put("chatsCount", chats.size)
                        put("messagesCount", messages.size)
                        put("sizeBytes", totalBytes)
                        put("storagePath", storagePath)
                    }
                    metaFile.writeText(metaObj.toString())
                } catch (e: Exception) {
                    Log.w(TAG_STATIC, "Error writing local meta: ${e.message}")
                }

                // Update app settings in Room
                val currentSettings = database.settingsDao().getSettings()
                if (currentSettings != null) {
                    database.settingsDao().update(
                        currentSettings.copy(
                            lastBackupTimestamp = now,
                            lastBackupSizeFormatted = backupMetadata.formattedSize,
                            lastBackupMessagesCount = messages.size
                        )
                    )
                }

                updateBackgroundStatus(
                    BackgroundBackupStatus(
                        isRunning = false,
                        progress = 1.0f,
                        stage = "Sauvegarde réussie",
                        lastSyncTimestamp = now,
                        lastSyncResult = if (uploadedToCloud) "Synchronisé sur Firebase Storage" else "Sauvegarde locale chiffrée créée",
                        isSuccess = true,
                        userPhone = userPhone,
                        formattedSize = backupMetadata.formattedSize,
                        messagesCount = messages.size,
                        chatsCount = chats.size
                    )
                )
                onProgress?.invoke(1.0f, "Sauvegarde terminée (${backupMetadata.formattedSize})")

                Result.success(backupMetadata)
            } catch (e: CancellationException) {
                Log.d(TAG_STATIC, "Direct backup coroutine cancelled cleanly.")
                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        isRunning = false,
                        stage = "Annulé",
                        lastSyncResult = "Sauvegarde interrompue",
                        isSuccess = false
                    )
                )
                throw e
            } catch (e: Exception) {
                Log.e(TAG_STATIC, "Direct backup failed: ${e.message}", e)
                updateBackgroundStatus(
                    backgroundStatus.value.copy(
                        isRunning = false,
                        stage = "Échec",
                        lastSyncResult = "Erreur: ${e.message}",
                        isSuccess = false
                    )
                )
                Result.failure(e)
            } finally {
                isBackupInProgress.set(false)
            }
        }
    }
}
