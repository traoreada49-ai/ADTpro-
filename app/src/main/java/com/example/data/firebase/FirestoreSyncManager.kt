package com.example.data.firebase

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.util.Log
import com.example.data.crypto.SignalProtocolEngine
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.model.SyncEnergyMode
import com.example.data.webrtc.WebRtcSignalingMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Firestore data sync status representation
 */
sealed class FirestoreSyncStatus {
    object Idle : FirestoreSyncStatus()
    object Syncing : FirestoreSyncStatus()
    data class Connected(val lastSyncTimestamp: Long = System.currentTimeMillis()) : FirestoreSyncStatus()
    data class Error(val message: String) : FirestoreSyncStatus()
}

/**
 * Real-time Firebase Firestore Sync Manager.
 * Synchronizes user chat conversations, real-time message streams, and metadata
 * between Firebase Firestore cloud collections and Room local SQLite database.
 */
class FirestoreSyncManager(
    private val context: android.content.Context,
    private val database: AppDatabase,
    private val coroutineScope: CoroutineScope
) {
    private val TAG = "FirestoreSyncManager"

    var activeChatId: String? = null

    private val cryptoEngine = SignalProtocolEngine.getInstance(context)
    val signalCryptoEngine: SignalProtocolEngine get() = cryptoEngine

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val instance = FirebaseInitHelper.getFirestore(context)
            if (instance != null) {
                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    instance.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Firestore settings already applied or offline mode: ${e.message}")
                }
            }
            instance
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Firestore unavailable, running in local database mode: ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        FirebaseInitHelper.getAuth(context)
    }

    private val _syncStatus = MutableStateFlow<FirestoreSyncStatus>(FirestoreSyncStatus.Idle)
    val syncStatus: StateFlow<FirestoreSyncStatus> = _syncStatus.asStateFlow()

    private var chatsListener: ListenerRegistration? = null
    private val messageListeners = mutableMapOf<String, ListenerRegistration>()

    // Battery & Energy Management Preferences
    private val syncPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("adt_sync_energy_prefs", Context.MODE_PRIVATE)
    }

    private var pollingJob: Job? = null
    private var currentUserPhone: String = ""

    private val _pollingIntervalMinutes = MutableStateFlow(15)
    val pollingIntervalMinutes: StateFlow<Int> = _pollingIntervalMinutes.asStateFlow()

    private val _nextPollingTimestamp = MutableStateFlow<Long>(0L)
    val nextPollingTimestamp: StateFlow<Long> = _nextPollingTimestamp.asStateFlow()

    private val _lastPollingTimestamp = MutableStateFlow<Long>(0L)
    val lastPollingTimestamp: StateFlow<Long> = _lastPollingTimestamp.asStateFlow()

    private val _isPollingActive = MutableStateFlow(false)
    val isPollingActive: StateFlow<Boolean> = _isPollingActive.asStateFlow()

    private var currentEnergyMode: SyncEnergyMode = SyncEnergyMode.HIGH_PERFORMANCE
    val energyMode: SyncEnergyMode get() = currentEnergyMode

    private val _currentEnergyModeState = MutableStateFlow(SyncEnergyMode.HIGH_PERFORMANCE)
    val currentEnergyModeState: StateFlow<SyncEnergyMode> = _currentEnergyModeState.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            "sync_energy_mode", "sync_interval_minutes", "auto_battery_saver_on_low_battery" -> {
                Log.i(TAG, "Battery mode preference '$key' changed, adjusting network polling strategy.")
                coroutineScope.launch(Dispatchers.IO) {
                    reloadPreferencesAndAdjustPolling()
                }
            }
        }
    }

    init {
        try {
            syncPrefs.registerOnSharedPreferenceChangeListener(prefChangeListener)
            val savedModeId = syncPrefs.getString("sync_energy_mode", SyncEnergyMode.HIGH_PERFORMANCE.id)
            currentEnergyMode = SyncEnergyMode.fromId(savedModeId)
            _currentEnergyModeState.value = currentEnergyMode
            val savedInterval = syncPrefs.getInt("sync_interval_minutes", 15).coerceIn(5, 120)
            _pollingIntervalMinutes.value = savedInterval
            _lastPollingTimestamp.value = syncPrefs.getLong("last_manual_sync_timestamp", 0L)
        } catch (e: Exception) {
            Log.w(TAG, "Failed initializing sync prefs listener: ${e.message}")
        }
    }

    /**
     * Start listening to Firestore real-time updates for chats and messages.
     */
    fun startRealtimeSync(userPhone: String) {
        if (userPhone.isNotBlank()) {
            currentUserPhone = userPhone
        }
        stopPeriodicNetworkPolling()
        if (userPhone.isBlank()) {
            Log.d(TAG, "Cannot start Firestore sync: userPhone is empty")
            return
        }

        if (firestore == null) {
            Log.i(TAG, "Firestore instance not available, running in local database mode")
            _syncStatus.value = FirestoreSyncStatus.Connected()
            return
        }

        _syncStatus.value = FirestoreSyncStatus.Syncing
        Log.d(TAG, "Starting Firestore real-time sync for user: $userPhone")

        stopRealtimeSync()

        try {
            // 1. Synchronize Chats metadata collection
            chatsListener = firestore?.collection("chats")
                ?.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to chats: ${error.message}")
                        _syncStatus.value = FirestoreSyncStatus.Error(error.localizedMessage ?: "Erreur de synchronisation Firestore")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            for (change in snapshots.documentChanges) {
                                val doc = change.document
                                val chatId = doc.id
                                val title = doc.getString("title") ?: "Discussion"
                                val isGroup = doc.getBoolean("isGroup") ?: false
                                val isChannel = doc.getBoolean("isChannel") ?: false
                                val avatarUrl = doc.getString("avatarUrl") ?: ""
                                val lastMessage = doc.getString("lastMessage") ?: ""
                                val lastTimestamp = doc.getLong("lastTimestamp") ?: System.currentTimeMillis()
                                val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                                val isPinned = doc.getBoolean("isPinned") ?: false
                                val isArchived = doc.getBoolean("isArchived") ?: false
                                val description = doc.getString("description") ?: ""
                                val membersCount = doc.getLong("membersCount")?.toInt() ?: 2
                                val creatorPhone = doc.getString("creatorPhone") ?: ""
                                val wallpaperId = doc.getString("wallpaperId") ?: "DEFAULT"

                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val existing = database.chatDao().getChatById(chatId)
                                        val chatEntity = ChatEntity(
                                            id = chatId,
                                            title = title,
                                            isGroup = isGroup,
                                            isChannel = isChannel,
                                            avatarUrl = avatarUrl,
                                            lastMessage = lastMessage,
                                            lastTimestamp = lastTimestamp,
                                            unreadCount = if (existing != null) existing.unreadCount else unreadCount,
                                            isPinned = if (existing != null) existing.isPinned else isPinned,
                                            isArchived = if (existing != null) existing.isArchived else isArchived,
                                            description = description,
                                            membersCount = membersCount,
                                            creatorPhone = creatorPhone,
                                            wallpaperId = if (existing != null) existing.wallpaperId else wallpaperId
                                        )
                                        database.chatDao().insertChat(chatEntity)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        database.chatDao().deleteChat(chatId)
                                    }
                                }
                            }
                        }
                        _syncStatus.value = FirestoreSyncStatus.Connected()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach chats listener: ${e.message}")
            _syncStatus.value = FirestoreSyncStatus.Error(e.localizedMessage ?: "Erreur réseau")
        }
    }

    /**
     * Listen in real-time to messages for a specific active chat room.
     */
    fun attachChatRoomListener(chatId: String) {
        if (messageListeners.containsKey(chatId)) return

        try {
            val listener = firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.orderBy("timestamp")
                ?.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.e(TAG, "Error listening to messages in chat $chatId: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshots != null) {
                        coroutineScope.launch(Dispatchers.IO) {
                            val user = database.userDao().getUser()
                            val myPhone = user?.phone ?: ""

                            for (change in snapshots.documentChanges) {
                                val doc = change.document
                                val msgId = doc.id
                                val senderPhone = doc.getString("senderPhone") ?: ""
                                val senderName = doc.getString("senderName") ?: "Contact"
                                val rawText = doc.getString("text") ?: ""
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                                val isEncrypted = doc.getBoolean("isEncrypted") ?: false
                                val encryptedPayload = doc.getString("encryptedPayload")
                                val ciphertext = doc.getString("ciphertext")
                                val isGroup = doc.getBoolean("isGroup") ?: false

                                // Decrypt message payload via Signal Protocol E2EE engine if encrypted
                                val (resolvedText, resolvedMediaType, resolvedMediaUrl, resolvedMediaDuration, resolvedForwarded, resolvedForwardedCount, resolvedReaction, resolvedReplyToText, resolvedReplyToSender) =
                                    if (isEncrypted || !encryptedPayload.isNullOrBlank() || !ciphertext.isNullOrBlank()) {
                                        val payloadJson = encryptedPayload ?: run {
                                            val iv = doc.getString("iv") ?: ""
                                            val ephem = doc.getString("senderEphemeralKey") ?: ""
                                            val rStep = doc.getLong("ratchetStep")?.toInt() ?: 0
                                            val fp = doc.getString("fingerprint") ?: ""
                                            SignalProtocolEngine.EncryptedMessageEnvelope(
                                                ciphertext = ciphertext ?: "",
                                                iv = iv,
                                                senderPhone = senderPhone,
                                                senderEphemeralKey = ephem,
                                                ratchetStep = rStep,
                                                isGroup = isGroup,
                                                fingerprint = fp
                                            ).toJsonString()
                                        }

                                        val decrypted = cryptoEngine.decryptMessagePayload(
                                            chatId = chatId,
                                            envelopeJson = payloadJson,
                                            fallbackText = rawText,
                                            senderPhone = senderPhone,
                                            timestamp = timestamp
                                        )

                                        DecryptedTuple(
                                            text = decrypted.text,
                                            mediaType = decrypted.mediaType,
                                            mediaUrl = if (decrypted.mediaUrl.isNotBlank()) decrypted.mediaUrl else (doc.getString("mediaUrl") ?: ""),
                                            mediaDuration = if (decrypted.mediaDuration > 0) decrypted.mediaDuration else (doc.getLong("mediaDuration")?.toInt() ?: 0),
                                            isForwarded = decrypted.isForwarded || (doc.getBoolean("isForwarded") ?: false),
                                            forwardedCount = if (decrypted.forwardedCount > 0) decrypted.forwardedCount else (doc.getLong("forwardedCount")?.toInt() ?: 0),
                                            reaction = decrypted.reaction ?: doc.getString("reaction"),
                                            replyToText = decrypted.replyToText ?: doc.getString("replyToText"),
                                            replyToSender = decrypted.replyToSender ?: doc.getString("replyToSender")
                                        )
                                    } else {
                                        DecryptedTuple(
                                            text = rawText,
                                            mediaType = doc.getString("mediaType") ?: "TEXT",
                                            mediaUrl = doc.getString("mediaUrl") ?: "",
                                            mediaDuration = doc.getLong("mediaDuration")?.toInt() ?: 0,
                                            isForwarded = doc.getBoolean("isForwarded") ?: false,
                                            forwardedCount = doc.getLong("forwardedCount")?.toInt() ?: 0,
                                            reaction = doc.getString("reaction"),
                                            replyToText = doc.getString("replyToText"),
                                            replyToSender = doc.getString("replyToSender")
                                        )
                                    }

                                val isOutgoing = if (myPhone.isNotBlank() && senderPhone.isNotBlank()) {
                                    myPhone.filter { it.isDigit() } == senderPhone.filter { it.isDigit() }
                                } else {
                                    doc.getBoolean("isOutgoing") ?: false
                                }

                                // Check readBy map from Firestore for multi-user / group read receipts
                                val readByMap = doc.get("readBy") as? Map<String, Any> ?: emptyMap()
                                val otherReaders = readByMap.keys.filter { readerPhone ->
                                    val cleanedReader = readerPhone.filter { it.isDigit() }
                                    val cleanedSender = senderPhone.filter { it.isDigit() }
                                    cleanedReader.isNotBlank() && cleanedReader != cleanedSender
                                }

                                var resolvedStatus = doc.getString("status") ?: "SENT"
                                if (isOutgoing) {
                                    if (otherReaders.isNotEmpty() || resolvedStatus == "READ") {
                                        resolvedStatus = "READ"
                                    }
                                } else {
                                    val isCurrentActiveChat = activeChatId == chatId
                                    if (isCurrentActiveChat) {
                                        resolvedStatus = "READ"
                                        coroutineScope.launch {
                                            markMessageAsReadInFirestore(chatId, msgId, myPhone)
                                        }
                                    } else {
                                        if (resolvedStatus == "SENT") {
                                            resolvedStatus = "DELIVERED"
                                            coroutineScope.launch {
                                                markMessageAsDeliveredInFirestore(chatId, msgId, myPhone)
                                            }
                                        }
                                    }
                                }

                                when (change.type) {
                                    DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                        val messageEntity = MessageEntity(
                                            id = msgId,
                                            chatId = chatId,
                                            senderPhone = senderPhone,
                                            senderName = senderName,
                                            text = resolvedText,
                                            timestamp = timestamp,
                                            mediaType = resolvedMediaType,
                                            mediaUrl = resolvedMediaUrl,
                                            mediaDuration = resolvedMediaDuration,
                                            isOutgoing = isOutgoing,
                                            isForwarded = resolvedForwarded,
                                            forwardedCount = resolvedForwardedCount,
                                            status = resolvedStatus,
                                            reaction = resolvedReaction,
                                            replyToText = resolvedReplyToText,
                                            replyToSender = resolvedReplyToSender
                                        )
                                        database.messageDao().insertMessage(messageEntity)
                                    }
                                    DocumentChange.Type.REMOVED -> {
                                        database.messageDao().deleteMessageById(msgId)
                                    }
                                }
                            }
                        }
                    }
                }
            listener?.let {
                messageListeners[chatId] = it
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed attaching message listener for chat $chatId: ${e.message}")
        }
    }

    /**
     * Detach real-time listener for a specific chat room.
     */
    fun detachChatRoomListener(chatId: String) {
        messageListeners[chatId]?.remove()
        messageListeners.remove(chatId)
    }

    /**
     * Update a message status directly in Firestore.
     */
    suspend fun updateMessageStatusInFirestore(chatId: String, messageId: String, status: String) {
        try {
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)
                ?.update("status", status)
                ?.await()
            Log.d(TAG, "Successfully updated status of message $messageId to $status in Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update status in Firestore: ${e.message}")
        }
    }

    /**
     * Mark message as delivered for a specific user.
     */
    suspend fun markMessageAsDeliveredInFirestore(chatId: String, messageId: String, myPhone: String) {
        try {
            val updates = hashMapOf<String, Any>(
                "status" to "DELIVERED",
                "deliveredAt" to System.currentTimeMillis()
            )
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)
                ?.set(updates, SetOptions.merge())
                ?.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark message as delivered in Firestore: ${e.message}")
        }
    }

    /**
     * Mark message as read in Firestore with real-time read timestamp and reader phone.
     */
    suspend fun markMessageAsReadInFirestore(chatId: String, messageId: String, myPhone: String) {
        try {
            val now = System.currentTimeMillis()
            val updates = hashMapOf<String, Any>(
                "status" to "READ",
                "readAt" to now,
                "readBy.$myPhone" to now
            )
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)
                ?.update(updates)
                ?.await()
        } catch (e: Exception) {
            // If document doesn't have readBy yet, use merge
            try {
                val updates = hashMapOf<String, Any>(
                    "status" to "READ",
                    "readAt" to System.currentTimeMillis(),
                    "readBy" to hashMapOf(myPhone to System.currentTimeMillis())
                )
                firestore?.collection("chats")
                    ?.document(chatId)
                    ?.collection("messages")
                    ?.document(messageId)
                    ?.set(updates, SetOptions.merge())
                    ?.await()
            } catch (e2: Exception) {
                Log.w(TAG, "Failed to mark message as read in Firestore: ${e2.message}")
            }
        }
    }

    /**
     * Update or clear a message emoji reaction in Firestore.
     */
    suspend fun updateMessageReactionInFirestore(chatId: String, messageId: String, reaction: String?) {
        try {
            val updates = hashMapOf<String, Any?>(
                "reaction" to reaction,
                "reactionUpdatedAt" to System.currentTimeMillis()
            )
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)
                ?.set(updates, SetOptions.merge())
                ?.await()
            Log.d(TAG, "Reaction updated for message $messageId: $reaction")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update reaction in Firestore: ${e.message}")
        }
    }

    /**
     * Mark all incoming messages in a chat as read.
     */
    suspend fun markIncomingMessagesAsRead(chatId: String, myPhone: String) {
        try {
            val toUpdate = database.messageDao().getMessagesToUpdate(chatId, myPhone, "READ")
            for (msg in toUpdate) {
                database.messageDao().updateMessageStatus(msg.id, "READ")
                markMessageAsReadInFirestore(chatId, msg.id, myPhone)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking incoming messages as read: ${e.message}")
        }
    }

    private data class DecryptedTuple(
        val text: String,
        val mediaType: String,
        val mediaUrl: String,
        val mediaDuration: Int,
        val isForwarded: Boolean,
        val forwardedCount: Int,
        val reaction: String?,
        val replyToText: String?,
        val replyToSender: String?
    )

    /**
     * Upload an outgoing message to Firestore and update conversation metadata.
     * All messages are end-to-end encrypted with Signal Protocol / AES-256-GCM before syncing.
     */
    suspend fun uploadMessageToFirestore(
        message: MessageEntity,
        chatTitle: String,
        isGroup: Boolean = false,
        avatarUrl: String = ""
    ) {
        try {
            val now = System.currentTimeMillis()

            // 1. End-to-End Encrypt message with Signal Protocol Double Ratchet / Sender Keys
            val envelope = cryptoEngine.encryptMessage(message, isGroup)
            val envelopeJson = envelope.toJsonString()

            val messageData = hashMapOf(
                "id" to message.id,
                "chatId" to message.chatId,
                "senderPhone" to message.senderPhone,
                "senderName" to message.senderName,
                // The plaintext in Firestore is masked so the database host never sees raw text
                "text" to "🔒 [Message chiffré - Protocole Signal]",
                "ciphertext" to envelope.ciphertext,
                "iv" to envelope.iv,
                "encryptedPayload" to envelopeJson,
                "isEncrypted" to true,
                "encryptionProtocol" to envelope.protocol,
                "ratchetStep" to envelope.ratchetStep,
                "senderEphemeralKey" to envelope.senderEphemeralKey,
                "fingerprint" to envelope.fingerprint,
                "timestamp" to message.timestamp,
                "mediaType" to message.mediaType,
                "mediaUrl" to "", // Masked media URL in Firestore cloud
                "mediaDuration" to message.mediaDuration,
                "isOutgoing" to message.isOutgoing,
                "isForwarded" to message.isForwarded,
                "forwardedCount" to message.forwardedCount,
                "status" to message.status,
                "reaction" to message.reaction,
                "replyToText" to message.replyToText,
                "replyToSender" to message.replyToSender,
                "isGroup" to isGroup,
                "readBy" to hashMapOf(message.senderPhone to now)
            )

            // 2. Upload encrypted message document to chat sub-collection
            firestore?.collection("chats")
                ?.document(message.chatId)
                ?.collection("messages")
                ?.document(message.id)
                ?.set(messageData, SetOptions.merge())
                ?.await()

            // 3. Update parent conversation metadata in Firestore
            val chatMetadata = hashMapOf(
                "id" to message.chatId,
                "title" to chatTitle,
                "isGroup" to isGroup,
                "avatarUrl" to avatarUrl,
                "lastMessage" to "🔒 Message chiffré (Signal E2EE)",
                "lastTimestamp" to message.timestamp,
                "creatorPhone" to message.senderPhone
            )

            firestore?.collection("chats")
                ?.document(message.chatId)
                ?.set(chatMetadata, SetOptions.merge())
                ?.await()

            Log.d(TAG, "Successfully synced Signal E2EE encrypted message ${message.id} (step ${envelope.ratchetStep}) to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore message upload deferred (offline or permission): ${e.message}")
        }
    }

    /**
     * Update message reaction in Firestore.
     */
    suspend fun updateReactionInFirestore(chatId: String, messageId: String, reaction: String?) {
        try {
            firestore?.collection("chats")
                ?.document(chatId)
                ?.collection("messages")
                ?.document(messageId)
                ?.update("reaction", reaction)
                ?.await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update reaction in Firestore: ${e.message}")
        }
    }

    /**
     * Upload a broadcast channel post to Firestore.
     */
    suspend fun uploadChannelPostToFirestore(
        post: com.example.data.local.entity.ChannelPostEntity,
        channelName: String
    ) {
        try {
            val postData = hashMapOf(
                "id" to post.id,
                "channelId" to post.channelId,
                "authorName" to post.authorName,
                "authorRole" to post.authorRole,
                "text" to post.text,
                "mediaType" to post.mediaType,
                "mediaUrl" to (post.mediaUrl ?: ""),
                "mediaTitle" to (post.mediaTitle ?: ""),
                "timestamp" to post.timestamp,
                "viewsCount" to post.viewsCount,
                "isPinned" to post.isPinned,
                "pollQuestion" to (post.pollQuestion ?: ""),
                "pollOption1" to (post.pollOption1 ?: ""),
                "pollOption2" to (post.pollOption2 ?: ""),
                "pollOption3" to (post.pollOption3 ?: ""),
                "pollOption4" to (post.pollOption4 ?: ""),
                "pollVotes1" to post.pollVotes1,
                "pollVotes2" to post.pollVotes2,
                "pollVotes3" to post.pollVotes3,
                "pollVotes4" to post.pollVotes4
            )

            firestore?.collection("channels")
                ?.document(post.channelId)
                ?.collection("posts")
                ?.document(post.id)
                ?.set(postData, SetOptions.merge())
                ?.await()

            // Update parent channel latest post metadata
            val channelMetadata = hashMapOf<String, Any>(
                "latestPostText" to post.text,
                "latestPostTimestamp" to post.timestamp
            )
            firestore?.collection("channels")
                ?.document(post.channelId)
                ?.set(channelMetadata, SetOptions.merge())
                ?.await()

            Log.d(TAG, "Successfully synced broadcast post ${post.id} to Firestore")
        } catch (e: Exception) {
            Log.w(TAG, "Firestore channel post sync deferred: ${e.message}")
        }
    }

    // =========================================================================
    // WEBRTC E2EE CALL SIGNALING CLOUD METHODS
    // =========================================================================

    private var callsListener: ListenerRegistration? = null
    private var activeCallSessionListener: ListenerRegistration? = null

    /**
     * Listen for incoming WebRTC call offers addressed to current user.
     */
    fun listenForIncomingCalls(
        myPhone: String,
        onIncomingCall: (callId: String, callerName: String, callerPhone: String, callerAvatar: String, isVideo: Boolean) -> Unit
    ) {
        if (myPhone.isBlank()) return
        callsListener?.remove()

        try {
            callsListener = firestore?.collection("calls")
                ?.whereEqualTo("calleePhone", myPhone)
                ?.whereEqualTo("status", "OFFERING")
                ?.addSnapshotListener { snapshots, error ->
                    if (error != null) {
                        Log.w(TAG, "Incoming calls listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshots != null && !snapshots.isEmpty) {
                        for (doc in snapshots.documents) {
                            val callId = doc.getString("callId") ?: doc.id
                            val callerName = doc.getString("callerName") ?: "Contact ADT"
                            val callerPhone = doc.getString("callerPhone") ?: ""
                            val callerAvatar = doc.getString("callerAvatar") ?: ""
                            val isVideo = doc.getBoolean("isVideo") ?: false
                            val timestamp = doc.getLong("timestamp") ?: 0L

                            // Only react to fresh calls (less than 60 seconds old)
                            if (System.currentTimeMillis() - timestamp < 60_000L) {
                                Log.i(TAG, "Incoming WebRTC call received from $callerName ($callerPhone)")
                                onIncomingCall(callId, callerName, callerPhone, callerAvatar, isVideo)
                                break
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Could not attach incoming calls listener: ${e.message}")
        }
    }

    /**
     * Publish an outgoing WebRTC call offer to Firestore.
     */
    fun publishCallOfferToFirestore(
        callId: String,
        callerPhone: String,
        callerName: String,
        callerAvatar: String,
        calleePhone: String,
        isVideo: Boolean,
        sdpOffer: String,
        e2eeFingerprint: String
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val callDoc = hashMapOf<String, Any>(
                    "callId" to callId,
                    "callerPhone" to callerPhone,
                    "callerName" to callerName,
                    "callerAvatar" to callerAvatar,
                    "calleePhone" to calleePhone,
                    "isVideo" to isVideo,
                    "status" to "OFFERING",
                    "sdpOffer" to sdpOffer,
                    "e2eeFingerprint" to e2eeFingerprint,
                    "cipherSuite" to "DTLS 1.3 / SRTP-AES-256-GCM",
                    "timestamp" to System.currentTimeMillis()
                )
                firestore?.collection("calls")?.document(callId)?.set(callDoc)?.await()
                Log.d(TAG, "Published WebRTC call offer $callId to Firestore")
            } catch (e: Exception) {
                Log.w(TAG, "Failed publishing WebRTC call offer: ${e.message}")
            }
        }
    }

    /**
     * Publish an SDP Answer when callee accepts the call.
     */
    fun publishCallAnswerToFirestore(callId: String, sdpAnswer: String) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val answerData = hashMapOf<String, Any>(
                    "status" to "ANSWERED",
                    "sdpAnswer" to sdpAnswer,
                    "answeredTimestamp" to System.currentTimeMillis()
                )
                firestore?.collection("calls")?.document(callId)?.set(answerData, SetOptions.merge())?.await()
                Log.d(TAG, "Published WebRTC call answer $callId")
            } catch (e: Exception) {
                Log.w(TAG, "Failed publishing WebRTC call answer: ${e.message}")
            }
        }
    }

    /**
     * Update call status (ENDED, REJECTED, CONNECTED) in Firestore.
     */
    fun updateCallStatusInFirestore(callId: String, status: String, reason: String = "Normal") {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val updateData = hashMapOf<String, Any>(
                    "status" to status,
                    "endReason" to reason,
                    "endedTimestamp" to System.currentTimeMillis()
                )
                firestore?.collection("calls")?.document(callId)?.set(updateData, SetOptions.merge())?.await()
                Log.d(TAG, "Updated WebRTC call status $callId -> $status")
            } catch (e: Exception) {
                Log.w(TAG, "Failed updating call status: ${e.message}")
            }
        }
    }

    /**
     * Publish an ICE candidate to Firestore.
     */
    fun publishIceCandidateToFirestore(callId: String, candidate: WebRtcSignalingMessage.IceCandidate, isCaller: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val candidateDoc = hashMapOf<String, Any>(
                    "sdpMid" to candidate.sdpMid,
                    "sdpMLineIndex" to candidate.sdpMLineIndex,
                    "sdpCandidate" to candidate.sdpCandidate,
                    "isCaller" to isCaller,
                    "timestamp" to System.currentTimeMillis()
                )
                val targetCollection = if (isCaller) "callerCandidates" else "calleeCandidates"
                firestore?.collection("calls")?.document(callId)?.collection(targetCollection)?.add(candidateDoc)?.await()
                Log.d(TAG, "Published ICE candidate for call $callId ($targetCollection)")
            } catch (e: Exception) {
                Log.w(TAG, "Failed publishing ICE candidate: ${e.message}")
            }
        }
    }

    /**
     * Attach a real-time listener to the active call document and candidate collections in Firestore.
     */
    fun listenToActiveCallSession(
        callId: String,
        isCaller: Boolean,
        onAnswerReceived: (String) -> Unit,
        onCandidateReceived: (WebRtcSignalingMessage.IceCandidate) -> Unit,
        onStatusChanged: (String, String?) -> Unit
    ) {
        activeCallSessionListener?.remove()
        try {
            // Listen to main call doc
            activeCallSessionListener = firestore?.collection("calls")?.document(callId)
                ?.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Call session listener error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status") ?: "OFFERING"
                        val endReason = snapshot.getString("endReason")
                        val sdpAnswer = snapshot.getString("sdpAnswer")

                        if (isCaller && status == "ANSWERED" && sdpAnswer != null) {
                            onAnswerReceived(sdpAnswer)
                        }

                        onStatusChanged(status, endReason)
                    }
                }

            // Listen to remote ICE candidates collection
            val remoteCandidatesCol = if (isCaller) "calleeCandidates" else "callerCandidates"
            firestore?.collection("calls")?.document(callId)?.collection(remoteCandidatesCol)
                ?.addSnapshotListener { snapshots, error ->
                    if (error != null || snapshots == null) return@addSnapshotListener
                    for (change in snapshots.documentChanges) {
                        if (change.type == DocumentChange.Type.ADDED) {
                            val doc = change.document
                            val sdpMid = doc.getString("sdpMid") ?: "audio"
                            val sdpMLineIndex = doc.getLong("sdpMLineIndex")?.toInt() ?: 0
                            val sdpCandidate = doc.getString("sdpCandidate") ?: ""
                            if (sdpCandidate.isNotBlank()) {
                                onCandidateReceived(
                                    WebRtcSignalingMessage.IceCandidate(
                                        callId = callId,
                                        sdpMid = sdpMid,
                                        sdpMLineIndex = sdpMLineIndex,
                                        sdpCandidate = sdpCandidate
                                    )
                                )
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "Error attaching active call session listener: ${e.message}")
        }
    }

    /**
     * Detach active call session listener.
     */
    fun detachActiveCallSessionListener() {
        activeCallSessionListener?.remove()
        activeCallSessionListener = null
    }

    /**
     * Stop all active real-time Firestore listeners.
     */
    fun stopRealtimeSync() {
        callsListener?.remove()
        callsListener = null
        activeCallSessionListener?.remove()
        activeCallSessionListener = null
        chatsListener?.remove()
        chatsListener = null
        messageListeners.values.forEach { it.remove() }
        messageListeners.clear()
        _syncStatus.value = FirestoreSyncStatus.Idle
    }

    /**
     * Check if the device is currently running on a critical battery level without charging.
     */
    private fun isLowBatteryDischarging(): Boolean {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val statusIntent = context.registerReceiver(null, filter)
            val level = statusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = statusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
            val status = statusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            pct in 1..20 && !isCharging
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Re-evaluates battery mode preferences and dynamically adjusts between
     * zero-latency real-time listeners and cadenced network polling.
     */
    suspend fun reloadPreferencesAndAdjustPolling() {
        val userPhone = if (currentUserPhone.isNotBlank()) {
            currentUserPhone
        } else {
            database.userDao().getUser()?.phone ?: ""
        }
        if (userPhone.isNotBlank()) {
            currentUserPhone = userPhone
        }

        val savedModeStr = syncPrefs.getString("sync_energy_mode", SyncEnergyMode.HIGH_PERFORMANCE.id)
        var targetMode = SyncEnergyMode.fromId(savedModeStr)
        val autoBatterySaver = syncPrefs.getBoolean("auto_battery_saver_on_low_battery", true)
        val interval = syncPrefs.getInt("sync_interval_minutes", 15).coerceIn(5, 120)
        _pollingIntervalMinutes.value = interval

        if (autoBatterySaver && isLowBatteryDischarging()) {
            Log.i(TAG, "Critical battery level detected (< 20%) without charging: enforcing BATTERY_SAVER polling mode.")
            targetMode = SyncEnergyMode.BATTERY_SAVER
        }

        currentEnergyMode = targetMode
        _currentEnergyModeState.value = targetMode

        if (targetMode == SyncEnergyMode.HIGH_PERFORMANCE) {
            stopPeriodicNetworkPolling()
            if (currentUserPhone.isNotBlank()) {
                startRealtimeSync(currentUserPhone)
            }
            Log.i(TAG, "Sync policy set to HIGH_PERFORMANCE: persistent real-time listeners active (< 100 ms).")
        } else {
            stopRealtimeSync()
            startPeriodicNetworkPolling(currentUserPhone, interval)
            Log.i(TAG, "Sync policy set to BATTERY_SAVER: continuous streams disconnected, polling interval adjusted to $interval minutes.")
        }
    }

    /**
     * Switch synchronization behavior according to the energy mode.
     * High Performance attaches full real-time listeners.
     * Battery Saver stops continuous background listening to conserve radio and CPU,
     * and sets up cadenced network polling at the preferred interval.
     */
    fun setEnergyMode(mode: SyncEnergyMode, userPhone: String) {
        if (userPhone.isNotBlank()) currentUserPhone = userPhone
        currentEnergyMode = mode
        _currentEnergyModeState.value = mode
        syncPrefs.edit().putString("sync_energy_mode", mode.id).apply()
        Log.i(TAG, "Sync energy mode explicitly set to: ${mode.name}")
        coroutineScope.launch(Dispatchers.IO) {
            reloadPreferencesAndAdjustPolling()
        }
    }

    /**
     * Dynamically adjusts the network polling interval in minutes and reschedules
     * the polling timer if running in Battery Saver mode.
     */
    fun setPollingInterval(minutes: Int, userPhone: String = "") {
        if (userPhone.isNotBlank()) currentUserPhone = userPhone
        val clamped = minutes.coerceIn(5, 120)
        _pollingIntervalMinutes.value = clamped
        syncPrefs.edit().putInt("sync_interval_minutes", clamped).apply()
        Log.i(TAG, "Adjusting network polling interval to $clamped minutes")
        if (currentEnergyMode == SyncEnergyMode.BATTERY_SAVER) {
            startPeriodicNetworkPolling(currentUserPhone, clamped)
        }
    }

    /**
     * Starts periodic network polling at the specified interval in minutes.
     * Conserves radio power by grouping requests and letting the device enter deep sleep between cycles.
     */
    fun startPeriodicNetworkPolling(userPhone: String, intervalMinutes: Int) {
        if (userPhone.isNotBlank()) currentUserPhone = userPhone
        stopPeriodicNetworkPolling()

        val intervalMs = intervalMinutes.coerceIn(5, 120) * 60 * 1000L
        _nextPollingTimestamp.value = System.currentTimeMillis() + intervalMs
        _isPollingActive.value = true

        pollingJob = coroutineScope.launch(Dispatchers.IO) {
            Log.i(TAG, "Periodic network polling started with interval: $intervalMinutes min ($intervalMs ms)")
            while (isActive && currentEnergyMode == SyncEnergyMode.BATTERY_SAVER) {
                val currentIntervalMs = _pollingIntervalMinutes.value.coerceIn(5, 120) * 60 * 1000L
                _nextPollingTimestamp.value = System.currentTimeMillis() + currentIntervalMs
                delay(currentIntervalMs)

                if (!isActive || currentEnergyMode != SyncEnergyMode.BATTERY_SAVER) break

                Log.i(TAG, "Executing scheduled Battery Saver network poll (every ${_pollingIntervalMinutes.value}m)...")
                try {
                    triggerManualSync(currentUserPhone)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Periodic network poll cycle note: ${e.message}")
                }
            }
            _isPollingActive.value = false
        }
    }

    /**
     * Cancels any active periodic network polling loop.
     */
    fun stopPeriodicNetworkPolling() {
        pollingJob?.cancel()
        pollingJob = null
        _isPollingActive.value = false
    }

    /**
     * Helper to parse and decrypt an individual Firestore document into a local MessageEntity.
     */
    private fun parseAndDecryptDocument(
        chatId: String,
        doc: DocumentSnapshot,
        myPhone: String
    ): MessageEntity {
        val msgId = doc.id
        val senderPhone = doc.getString("senderPhone") ?: ""
        val senderName = doc.getString("senderName") ?: "Contact"
        val rawText = doc.getString("text") ?: ""
        val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
        val isEncrypted = doc.getBoolean("isEncrypted") ?: false
        val encryptedPayload = doc.getString("encryptedPayload")
        val ciphertext = doc.getString("ciphertext")
        val isGroup = doc.getBoolean("isGroup") ?: false

        val (resolvedText, resolvedMediaType, resolvedMediaUrl, resolvedMediaDuration, resolvedForwarded, resolvedForwardedCount, resolvedReaction, resolvedReplyToText, resolvedReplyToSender) =
            if (isEncrypted || !encryptedPayload.isNullOrBlank() || !ciphertext.isNullOrBlank()) {
                val payloadJson = encryptedPayload ?: run {
                    val iv = doc.getString("iv") ?: ""
                    val ephem = doc.getString("senderEphemeralKey") ?: ""
                    val rStep = doc.getLong("ratchetStep")?.toInt() ?: 0
                    val fp = doc.getString("fingerprint") ?: ""
                    SignalProtocolEngine.EncryptedMessageEnvelope(
                        ciphertext = ciphertext ?: "",
                        iv = iv,
                        senderPhone = senderPhone,
                        senderEphemeralKey = ephem,
                        ratchetStep = rStep,
                        isGroup = isGroup,
                        fingerprint = fp
                    ).toJsonString()
                }

                val decrypted = cryptoEngine.decryptMessagePayload(
                    chatId = chatId,
                    envelopeJson = payloadJson,
                    fallbackText = rawText,
                    senderPhone = senderPhone,
                    timestamp = timestamp
                )

                DecryptedTuple(
                    text = decrypted.text,
                    mediaType = decrypted.mediaType,
                    mediaUrl = if (decrypted.mediaUrl.isNotBlank()) decrypted.mediaUrl else (doc.getString("mediaUrl") ?: ""),
                    mediaDuration = if (decrypted.mediaDuration > 0) decrypted.mediaDuration else (doc.getLong("mediaDuration")?.toInt() ?: 0),
                    isForwarded = decrypted.isForwarded || (doc.getBoolean("isForwarded") ?: false),
                    forwardedCount = if (decrypted.forwardedCount > 0) decrypted.forwardedCount else (doc.getLong("forwardedCount")?.toInt() ?: 0),
                    reaction = decrypted.reaction ?: doc.getString("reaction"),
                    replyToText = decrypted.replyToText ?: doc.getString("replyToText"),
                    replyToSender = decrypted.replyToSender ?: doc.getString("replyToSender")
                )
            } else {
                DecryptedTuple(
                    text = rawText,
                    mediaType = doc.getString("mediaType") ?: "TEXT",
                    mediaUrl = doc.getString("mediaUrl") ?: "",
                    mediaDuration = doc.getLong("mediaDuration")?.toInt() ?: 0,
                    isForwarded = doc.getBoolean("isForwarded") ?: false,
                    forwardedCount = doc.getLong("forwardedCount")?.toInt() ?: 0,
                    reaction = doc.getString("reaction"),
                    replyToText = doc.getString("replyToText"),
                    replyToSender = doc.getString("replyToSender")
                )
            }

        val isOutgoing = if (myPhone.isNotBlank() && senderPhone.isNotBlank()) {
            myPhone.filter { it.isDigit() } == senderPhone.filter { it.isDigit() }
        } else {
            doc.getBoolean("isOutgoing") ?: false
        }

        val readByMap = doc.get("readBy") as? Map<String, Any> ?: emptyMap()
        val otherReaders = readByMap.keys.filter { readerPhone ->
            val cleanedReader = readerPhone.filter { it.isDigit() }
            val cleanedSender = senderPhone.filter { it.isDigit() }
            cleanedReader.isNotBlank() && cleanedReader != cleanedSender
        }

        var resolvedStatus = doc.getString("status") ?: "SENT"
        if (isOutgoing) {
            if (otherReaders.isNotEmpty() || resolvedStatus == "READ") {
                resolvedStatus = "READ"
            }
        } else {
            val isCurrentActiveChat = activeChatId == chatId
            if (isCurrentActiveChat) {
                resolvedStatus = "READ"
                coroutineScope.launch {
                    markMessageAsReadInFirestore(chatId, msgId, myPhone)
                }
            } else {
                if (resolvedStatus == "SENT") {
                    resolvedStatus = "DELIVERED"
                    coroutineScope.launch {
                        markMessageAsDeliveredInFirestore(chatId, msgId, myPhone)
                    }
                }
            }
        }

        return MessageEntity(
            id = msgId,
            chatId = chatId,
            senderPhone = senderPhone,
            senderName = senderName,
            text = resolvedText,
            timestamp = timestamp,
            mediaType = resolvedMediaType,
            mediaUrl = resolvedMediaUrl,
            mediaDuration = resolvedMediaDuration,
            isOutgoing = isOutgoing,
            isForwarded = resolvedForwarded,
            forwardedCount = resolvedForwardedCount,
            status = resolvedStatus,
            reaction = resolvedReaction,
            replyToText = resolvedReplyToText,
            replyToSender = resolvedReplyToSender
        )
    }

    /**
     * Executes a one-time cloud synchronization without keeping persistent background listeners.
     * Synchronizes all chats and polls for new encrypted messages across conversations.
     * Ideal for Battery Saver mode on demand or periodic cycles.
     */
    suspend fun triggerManualSync(userPhone: String) {
        if (userPhone.isNotBlank()) {
            currentUserPhone = userPhone
        }
        if (firestore == null) {
            _syncStatus.value = FirestoreSyncStatus.Connected()
            return
        }
        _syncStatus.value = FirestoreSyncStatus.Syncing
        try {
            val myPhone = if (userPhone.isNotBlank()) userPhone else (database.userDao().getUser()?.phone ?: "")

            // 1. Fetch all chats metadata
            val snapshots = firestore?.collection("chats")?.get()?.await()
            if (snapshots != null) {
                for (doc in snapshots.documents) {
                    val chatId = doc.id
                    val title = doc.getString("title") ?: "Discussion"
                    val isGroup = doc.getBoolean("isGroup") ?: false
                    val isChannel = doc.getBoolean("isChannel") ?: false
                    val avatarUrl = doc.getString("avatarUrl") ?: ""
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastTimestamp = doc.getLong("lastTimestamp") ?: System.currentTimeMillis()
                    val unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                    val isPinned = doc.getBoolean("isPinned") ?: false
                    val isArchived = doc.getBoolean("isArchived") ?: false
                    val description = doc.getString("description") ?: ""
                    val membersCount = doc.getLong("membersCount")?.toInt() ?: 2
                    val creatorPhone = doc.getString("creatorPhone") ?: ""
                    val wallpaperId = doc.getString("wallpaperId") ?: "DEFAULT"

                    val existing = database.chatDao().getChatById(chatId)
                    val chatEntity = ChatEntity(
                        id = chatId,
                        title = title,
                        isGroup = isGroup,
                        isChannel = isChannel,
                        avatarUrl = avatarUrl,
                        lastMessage = lastMessage,
                        lastTimestamp = lastTimestamp,
                        unreadCount = existing?.unreadCount ?: unreadCount,
                        isPinned = existing?.isPinned ?: isPinned,
                        isArchived = existing?.isArchived ?: isArchived,
                        description = description,
                        membersCount = membersCount,
                        creatorPhone = creatorPhone,
                        wallpaperId = existing?.wallpaperId ?: wallpaperId
                    )
                    database.chatDao().insertChat(chatEntity)

                    // 2. Poll recent messages for this chat
                    try {
                        val latestMsg = database.messageDao().getLatestMessageForChat(chatId)
                        val sinceTs = latestMsg?.timestamp ?: 0L
                        val msgsQuery = firestore?.collection("chats")
                            ?.document(chatId)
                            ?.collection("messages")
                            ?.whereGreaterThan("timestamp", sinceTs)
                            ?.orderBy("timestamp")
                            ?.limit(50)
                            ?.get()
                            ?.await()

                        if (msgsQuery != null && !msgsQuery.isEmpty) {
                            val newEntities = mutableListOf<MessageEntity>()
                            for (mDoc in msgsQuery.documents) {
                                val entity = parseAndDecryptDocument(chatId, mDoc, myPhone)
                                newEntities.add(entity)
                            }
                            if (newEntities.isNotEmpty()) {
                                database.messageDao().insertMessages(newEntities)
                                Log.d(TAG, "Polled ${newEntities.size} new messages for chat $chatId")
                            }
                        }
                    } catch (me: Exception) {
                        Log.d(TAG, "Message polling for chat $chatId note: ${me.message}")
                    }
                }
            }
            val now = System.currentTimeMillis()
            _lastPollingTimestamp.value = now
            syncPrefs.edit().putLong("last_manual_sync_timestamp", now).apply()
            _syncStatus.value = FirestoreSyncStatus.Connected(now)
            Log.i(TAG, "Manual network synchronization successfully completed at $now")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Manual sync error: ${e.message}")
            _syncStatus.value = FirestoreSyncStatus.Connected()
        }
    }
}
