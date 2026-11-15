package com.example.data.repository

import com.example.data.crypto.DatabaseSecurityStatus
import com.example.data.crypto.RoomDatabaseSecurityManager
import com.example.data.local.AppDatabase
import com.example.data.local.SampleDataFactory
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelPostEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity
import com.example.data.search.ArchivedFtsSearchEngine
import com.example.data.search.ArchivedSearchOptions
import com.example.data.search.ArchivedSearchResult
import com.example.data.search.ArchivedSearchStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

class AdtRepository(private val database: AppDatabase) {

    val roomSecurityManager: RoomDatabaseSecurityManager = RoomDatabaseSecurityManager.getInstance()
    val databaseSecurityStatus: StateFlow<DatabaseSecurityStatus> = roomSecurityManager.securityStatus

    val currentUserFlow: Flow<UserEntity?> = database.userDao().getUserFlow()
    val activeChatsFlow: Flow<List<ChatEntity>> = database.chatDao().getActiveChats()
    val lockedChatsFlow: Flow<List<ChatEntity>> = database.chatDao().getLockedChats()
    val lockedChatsCountFlow: Flow<Int> = database.chatDao().getLockedChatsCountFlow()
    val archivedChatsFlow: Flow<List<ChatEntity>> = database.chatDao().getArchivedChats()
    val archivedChatsCountFlow: Flow<Int> = database.chatDao().getArchivedChatsCountFlow()
    val allContactsFlow: Flow<List<ContactEntity>> = database.contactDao().getAllContacts()
    val allStatusesFlow: Flow<List<StatusEntity>> = database.statusDao().getAllStatuses()
    val allChannelsFlow: Flow<List<ChannelEntity>> = database.channelDao().getAllChannels()
    val followedChannelsFlow: Flow<List<ChannelEntity>> = database.channelDao().getFollowedChannels()
    val allCallsFlow: Flow<List<CallDaoCalls>> = database.callDao().getAllCalls()
    val appSettingsFlow: Flow<AppSettingsEntity?> = database.settingsDao().getSettingsFlow()

    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        database.messageDao().getMessagesForChat(chatId)

    fun getChatFlow(chatId: String): Flow<ChatEntity?> =
        database.chatDao().getChatFlow(chatId)

    fun getGroupMembersFlow(groupId: String): Flow<List<GroupMemberEntity>> =
        database.groupMemberDao().getMembersForGroupFlow(groupId)

    suspend fun getGroupMembers(groupId: String): List<GroupMemberEntity> = withContext(Dispatchers.IO) {
        database.groupMemberDao().getMembersForGroup(groupId)
    }

    fun getChannelFlow(channelId: String): Flow<ChannelEntity?> =
        database.channelDao().getChannelFlow(channelId)

    fun getPostsForChannelFlow(channelId: String): Flow<List<ChannelPostEntity>> =
        database.channelPostDao().getPostsForChannel(channelId)

    suspend fun enrichAndSeedAllData(force: Boolean = true) = withContext(Dispatchers.IO) {
        val contacts = SampleDataFactory.getEnrichedContacts()
        database.contactDao().insertContacts(contacts)

        val chats = SampleDataFactory.getEnrichedChats()
        database.chatDao().insertChats(chats)

        val groupMembers = SampleDataFactory.getEnrichedGroupMembers()
        database.groupMemberDao().insertMembers(groupMembers)

        val messages = SampleDataFactory.getEnrichedMessages()
        database.messageDao().insertMessages(messages)

        val channels = SampleDataFactory.getEnrichedChannels()
        database.channelDao().insertChannels(channels)

        val channelPosts = SampleDataFactory.getEnrichedChannelPosts()
        database.channelPostDao().insertPosts(channelPosts)

        val statuses = SampleDataFactory.getEnrichedStatuses()
        database.statusDao().insertStatuses(statuses)

        val calls = SampleDataFactory.getEnrichedCalls()
        database.callDao().insertCalls(calls)

        // Automatic AES-256 encryption pass on all local messages and records in Room SQLite
        roomSecurityManager.migrateUnencryptedMessages(database.rawMessageDao())
    }

    suspend fun initializeDefaultDataIfEmpty() = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        if (user == null) {
            database.userDao().insertOrUpdate(
                UserEntity(
                    id = 1,
                    phone = "",
                    countryCode = "+223",
                    firstName = "",
                    lastName = "",
                    isRegistered = false,
                    operatorName = "Orange"
                )
            )
        }

        val settings = database.settingsDao().getSettings()
        if (settings == null) {
            database.settingsDao().insertOrUpdate(AppSettingsEntity())
        }

        // Always ensure enriched data is seeded
        val existingContacts = database.contactDao().getContactById("+221770001122")
        if (existingContacts == null) {
            enrichAndSeedAllData(force = false)
        } else {
            // Also ensure any newly added contacts/chats/channels are merged seamlessly
            enrichAndSeedAllData(force = false)
        }
    }

    suspend fun auditDatabaseSecurity(): DatabaseSecurityStatus = withContext(Dispatchers.IO) {
        roomSecurityManager.auditDatabaseSecurity(database.rawMessageDao())
    }

    suspend fun forceEncryptLocalDatabase(): Int = withContext(Dispatchers.IO) {
        roomSecurityManager.migrateUnencryptedMessages(database.rawMessageDao())
    }

    // Register / Save User Profile
    suspend fun registerUser(
        phone: String,
        countryCode: String,
        firstName: String,
        lastName: String,
        businessCategory: String,
        bio: String,
        operatorName: String,
        avatarUrl: String = ""
    ) = withContext(Dispatchers.IO) {
        val user = UserEntity(
            id = 1,
            phone = phone,
            countryCode = countryCode,
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            businessCategory = businessCategory,
            bio = if (bio.isBlank()) "Disponible sur ADT Pro" else bio,
            avatarUrl = avatarUrl,
            isRegistered = true,
            operatorName = operatorName
        )
        database.userDao().insertOrUpdate(user)
    }

    suspend fun updateProfile(
        firstName: String,
        lastName: String,
        bio: String,
        businessCategory: String,
        avatarUrl: String
    ) = withContext(Dispatchers.IO) {
        val currentUser = database.userDao().getUser() ?: return@withContext
        val updated = currentUser.copy(
            firstName = firstName,
            lastName = lastName,
            bio = bio,
            businessCategory = businessCategory,
            avatarUrl = avatarUrl
        )
        database.userDao().insertOrUpdate(updated)
    }

    suspend fun logoutUserAccount() = withContext(Dispatchers.IO) {
        val currentUser = database.userDao().getUser() ?: return@withContext
        database.userDao().insertOrUpdate(currentUser.copy(isRegistered = false))
    }

    // Send Message
    suspend fun sendMessage(
        chatId: String,
        text: String,
        mediaType: String = "TEXT",
        mediaUrl: String = "",
        mediaDuration: Int = 0,
        replyToText: String? = null,
        replyToSender: String? = null
    ) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val userName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Moi"
        val userPhone = user?.phone ?: "+22100000000"

        val msgId = UUID.randomUUID().toString()
        val message = MessageEntity(
            id = msgId,
            chatId = chatId,
            senderPhone = userPhone,
            senderName = userName,
            text = text,
            timestamp = System.currentTimeMillis(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            mediaDuration = mediaDuration,
            isOutgoing = true,
            isForwarded = false,
            forwardedCount = 0,
            status = "SENT",
            replyToText = replyToText,
            replyToSender = replyToSender
        )
        database.messageDao().insertMessage(message)

        val lastSummary = when (mediaType) {
            "AUDIO" -> "🎤 Message vocal (${mediaDuration}s)"
            "PHOTO" -> "📷 Photo : ${text.take(20)}"
            "VIDEO" -> "🎥 Vidéo (${mediaDuration}s)"
            "STICKER" -> "🏷️ Autocollant"
            "LIVE_LOCATION" -> "📍 Localisation en direct"
            "LOCATION" -> "📍 Position partagée"
            "DOCUMENT", "DOC" -> "📄 Document : ${text.take(25)}"
            else -> text
        }
        database.chatDao().updateLastMessageOutgoing(chatId, lastSummary, System.currentTimeMillis())
        message
    }

    // Forward Message (Transfert de message, photo, vidéo)
    suspend fun forwardMessage(
        sourceMessage: MessageEntity,
        targetChatId: String
    ) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val userName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Moi"
        val userPhone = user?.phone ?: "+22100000000"

        val forwardId = UUID.randomUUID().toString()
        val forwardedMessage = MessageEntity(
            id = forwardId,
            chatId = targetChatId,
            senderPhone = userPhone,
            senderName = userName,
            text = sourceMessage.text,
            timestamp = System.currentTimeMillis(),
            mediaType = sourceMessage.mediaType,
            mediaUrl = sourceMessage.mediaUrl,
            mediaDuration = sourceMessage.mediaDuration,
            isOutgoing = true,
            isForwarded = true,
            forwardedCount = sourceMessage.forwardedCount + 1,
            status = "READ"
        )
        database.messageDao().insertMessage(forwardedMessage)

        val summary = "➡️ Transféré : " + when (sourceMessage.mediaType) {
            "AUDIO" -> "Message vocal"
            "PHOTO" -> "Photo"
            "VIDEO" -> "Vidéo"
            "STICKER" -> "Autocollant"
            "DOCUMENT", "DOC" -> "Document"
            else -> sourceMessage.text
        }
        database.chatDao().updateLastMessageOutgoing(targetChatId, summary, System.currentTimeMillis())
    }

    suspend fun reactToMessage(messageId: String, reaction: String?): MessageEntity? = withContext(Dispatchers.IO) {
        database.messageDao().updateReaction(messageId, reaction)
        database.messageDao().getMessageById(messageId)
    }

    suspend fun getMessageById(messageId: String): MessageEntity? = withContext(Dispatchers.IO) {
        database.messageDao().getMessageById(messageId)
    }

    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        database.messageDao().deleteMessageById(messageId)
    }

    suspend fun deleteExpiredMessages(chatId: String, cutoffTime: Long) = withContext(Dispatchers.IO) {
        val messages = database.messageDao().getMessagesForChatSync(chatId)
        val expired = messages.filter { it.timestamp < cutoffTime }
        for (msg in expired) {
            database.messageDao().deleteMessageById(msg.id)
        }
    }

    suspend fun updateMessageStatus(messageId: String, status: String) = withContext(Dispatchers.IO) {
        database.messageDao().updateMessageStatus(messageId, status)
    }

    suspend fun markChatAsRead(chatId: String) = withContext(Dispatchers.IO) {
        database.chatDao().markChatAsRead(chatId)
    }

    suspend fun getChatIdsMatchingMessageText(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else database.messageDao().getChatIdsWithMatchingMessages(query)
    }

    suspend fun searchMessagesByText(query: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList()
        else database.messageDao().searchMessagesByText(query)
    }

    suspend fun setChatArchived(chatId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        database.chatDao().setChatArchived(chatId, isArchived)
    }

    suspend fun setChatLocked(chatId: String, isLocked: Boolean) = withContext(Dispatchers.IO) {
        database.chatDao().setChatLocked(chatId, isLocked)
    }

    suspend fun getArchivedChats(): List<ChatEntity> = withContext(Dispatchers.IO) {
        database.chatDao().getArchivedChatsSync()
    }

    suspend fun getArchivedMessages(): List<MessageEntity> = withContext(Dispatchers.IO) {
        database.messageDao().getArchivedMessagesSync()
    }

    suspend fun searchArchivedFullText(
        query: String,
        options: ArchivedSearchOptions = ArchivedSearchOptions()
    ): Pair<List<ArchivedSearchResult>, ArchivedSearchStats> = withContext(Dispatchers.IO) {
        val archivedChats = database.chatDao().getArchivedChatsSync()
        val archivedMessages = database.messageDao().getArchivedMessagesSync()
        ArchivedFtsSearchEngine.search(
            query = query,
            archivedChats = archivedChats,
            archivedMessages = archivedMessages,
            options = options
        )
    }

    suspend fun updateLiveLocationPayload(messageId: String, payload: com.example.data.model.LiveLocationPayload) = withContext(Dispatchers.IO) {
        val msg = database.messageDao().getMessageById(messageId) ?: return@withContext
        val updatedMsg = msg.copy(
            mediaUrl = payload.serialize(),
            text = if (payload.isActive) "📍 En direct • ${payload.address}" else "📍 Partage de localisation terminé"
        )
        database.messageDao().updateMessage(updatedMsg)
    }

    suspend fun stopLiveLocation(messageId: String) = withContext(Dispatchers.IO) {
        val msg = database.messageDao().getMessageById(messageId) ?: return@withContext
        val payload = com.example.data.model.LiveLocationPayload.deserialize(msg.mediaUrl)
        if (payload != null) {
            val updatedPayload = payload.copy(isActive = false)
            val updatedMsg = msg.copy(
                mediaUrl = updatedPayload.serialize(),
                text = "📍 Partage de localisation terminé"
            )
            database.messageDao().updateMessage(updatedMsg)
            database.chatDao().updateLastMessageOutgoing(msg.chatId, "📍 Partage de localisation terminé", System.currentTimeMillis())
        }
    }

    // Create or find a direct chat with a contact
    suspend fun getOrCreateDirectChat(contact: ContactEntity): String = withContext(Dispatchers.IO) {
        val chatId = "chat_" + contact.phone.filter { it.isDigit() }
        val existing = database.chatDao().getChatById(chatId)
        if (existing == null) {
            val newChat = ChatEntity(
                id = chatId,
                title = contact.name,
                isGroup = false,
                avatarUrl = contact.avatarUrl,
                lastMessage = "Discussion sécurisée créée",
                lastTimestamp = System.currentTimeMillis(),
                unreadCount = 0,
                membersCount = 2,
                creatorPhone = contact.phone
            )
            database.chatDao().insertChat(newChat)
        }
        chatId
    }

    suspend fun insertContact(contact: ContactEntity) = withContext(Dispatchers.IO) {
        database.contactDao().insertContact(contact)
    }

    suspend fun getContactById(id: String): ContactEntity? = withContext(Dispatchers.IO) {
        database.contactDao().getContactById(id)
    }

    // Create Group
    suspend fun createGroup(
        name: String,
        selectedContacts: List<ContactEntity>,
        category: String,
        description: String,
        avatarUrl: String = "group_custom"
    ): String = withContext(Dispatchers.IO) {
        val groupId = "group_" + UUID.randomUUID().toString().take(8)
        val user = database.userDao().getUser()
        val creatorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Vous"
        val creatorPhone = user?.phone?.ifBlank { "+221 77 000 00 00" } ?: "+221 77 000 00 00"

        val groupChat = ChatEntity(
            id = groupId,
            title = name,
            isGroup = true,
            avatarUrl = avatarUrl,
            lastMessage = "$creatorName a créé le groupe « $name »",
            lastTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            membersCount = selectedContacts.size + 1,
            description = "$description ($category)",
            creatorPhone = creatorPhone
        )
        database.chatDao().insertChat(groupChat)

        // Add creator as Admin
        val creatorMember = GroupMemberEntity(
            id = "${groupId}_$creatorPhone",
            groupId = groupId,
            contactPhone = creatorPhone,
            name = if (creatorName == "Vous") "Vous (Créateur)" else "$creatorName (Vous)",
            avatarUrl = user?.avatarUrl ?: "",
            isAdmin = true,
            role = "CREATOR",
            bio = user?.bio ?: "Créateur du groupe"
        )
        database.groupMemberDao().insertMember(creatorMember)

        // Add selected contacts as Members
        val members = selectedContacts.map { contact ->
            GroupMemberEntity(
                id = "${groupId}_${contact.phone}",
                groupId = groupId,
                contactPhone = contact.phone,
                name = contact.name,
                avatarUrl = contact.avatarUrl,
                isAdmin = false,
                role = "MEMBER",
                bio = contact.bio
            )
        }
        database.groupMemberDao().insertMembers(members)

        // System creation message
        val welcomeMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = "🔒 $creatorName a créé le groupe « $name » avec ${selectedContacts.size} participants. Les messages sont chiffrés de bout en bout.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(welcomeMsg)
        groupId
    }

    // Add members to an existing group
    suspend fun addGroupMembers(
        groupId: String,
        newContacts: List<ContactEntity>
    ) = withContext(Dispatchers.IO) {
        if (newContacts.isEmpty()) return@withContext
        val user = database.userDao().getUser()
        val actorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Vous"

        val newMembers = newContacts.map { contact ->
            GroupMemberEntity(
                id = "${groupId}_${contact.phone}",
                groupId = groupId,
                contactPhone = contact.phone,
                name = contact.name,
                avatarUrl = contact.avatarUrl,
                isAdmin = false,
                role = "MEMBER",
                bio = contact.bio
            )
        }
        database.groupMemberDao().insertMembers(newMembers)

        val totalCount = database.groupMemberDao().getMemberCount(groupId)
        val chat = database.chatDao().getChatById(groupId)
        if (chat != null) {
            database.chatDao().updateChat(chat.copy(membersCount = totalCount))
        }

        val namesJoined = newContacts.joinToString(", ") { it.name }
        val systemMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = "👥 $actorName a ajouté $namesJoined au groupe.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(systemMsg)
        database.chatDao().updateLastMessageOutgoing(groupId, "👥 $actorName a ajouté $namesJoined", System.currentTimeMillis())
    }

    // Remove a member from a group
    suspend fun removeGroupMember(
        groupId: String,
        memberPhone: String,
        memberName: String
    ) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val actorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Vous"

        database.groupMemberDao().deleteMember(groupId, memberPhone)
        val totalCount = database.groupMemberDao().getMemberCount(groupId)
        val chat = database.chatDao().getChatById(groupId)
        if (chat != null) {
            database.chatDao().updateChat(chat.copy(membersCount = totalCount))
        }

        val systemMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = "🚫 $actorName a retiré $memberName du groupe.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(systemMsg)
        database.chatDao().updateLastMessageOutgoing(groupId, "🚫 $memberName a été retiré(e)", System.currentTimeMillis())
    }

    // Toggle group admin privilege
    suspend fun toggleGroupAdmin(
        groupId: String,
        memberPhone: String,
        memberName: String,
        makeAdmin: Boolean
    ) = withContext(Dispatchers.IO) {
        database.groupMemberDao().updateAdminStatus(
            groupId = groupId,
            contactPhone = memberPhone,
            isAdmin = makeAdmin,
            role = if (makeAdmin) "ADMIN" else "MEMBER"
        )

        val actionText = if (makeAdmin) {
            "⭐ $memberName est désormais administrateur du groupe."
        } else {
            "ℹ️ $memberName n'est plus administrateur du groupe."
        }
        val systemMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = actionText,
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(systemMsg)
    }

    // Update Group Title & Description
    suspend fun updateGroupInfo(
        groupId: String,
        newTitle: String,
        newDescription: String
    ) = withContext(Dispatchers.IO) {
        val chat = database.chatDao().getChatById(groupId) ?: return@withContext
        val updated = chat.copy(
            title = newTitle.trim(),
            description = newDescription.trim()
        )
        database.chatDao().updateChat(updated)

        val systemMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = "✏️ Les informations du groupe ont été modifiées.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(systemMsg)
    }

    // Leave a group
    suspend fun leaveGroup(groupId: String) = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val myPhone = user?.phone ?: ""
        val myName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Vous"

        database.groupMemberDao().deleteMember(groupId, myPhone)
        val totalCount = database.groupMemberDao().getMemberCount(groupId)
        val chat = database.chatDao().getChatById(groupId)
        if (chat != null) {
            database.chatDao().updateChat(chat.copy(membersCount = totalCount.coerceAtLeast(1)))
        }

        val systemMsg = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupId,
            senderPhone = "system",
            senderName = "Système ADT Pro",
            text = "👋 $myName a quitté le groupe.",
            timestamp = System.currentTimeMillis(),
            isOutgoing = false,
            status = "READ"
        )
        database.messageDao().insertMessage(systemMsg)
    }

    // Delete a group completely
    suspend fun deleteGroup(groupId: String) = withContext(Dispatchers.IO) {
        database.groupMemberDao().deleteMembersForGroup(groupId)
        database.messageDao().clearMessagesForChat(groupId)
        database.chatDao().deleteChat(groupId)
    }

    // Create Broadcast Channel (with admin ownership)
    suspend fun createChannel(
        name: String,
        description: String,
        category: String
    ): String = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val creatorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Administrateur"
        val creatorPhone = user?.phone ?: "+221 77 000 00 00"

        val channelId = "chan_" + UUID.randomUUID().toString().take(8)
        val newChan = ChannelEntity(
            id = channelId,
            name = name,
            description = description,
            avatarUrl = "chan_custom",
            category = category,
            followersCount = 1,
            isVerified = false,
            isFollowing = true,
            adminPhone = creatorPhone,
            adminName = "$creatorName (Créateur)",
            isOwner = true,
            latestPostText = "Bienvenue sur la chaîne de diffusion $name.",
            latestPostTimestamp = System.currentTimeMillis()
        )
        database.channelDao().insertChannel(newChan)

        // Initial broadcast post from the admin
        val welcomePost = ChannelPostEntity(
            id = "post_" + UUID.randomUUID().toString().take(8),
            channelId = channelId,
            authorName = creatorName,
            authorRole = "Créateur & Administrateur",
            text = "📢 Bienvenue sur la chaîne $name ! Suivez nos diffusions en direct pour recevoir les actualités et annonces officielles.",
            mediaType = "ANNOUNCEMENT",
            timestamp = System.currentTimeMillis(),
            viewsCount = 1,
            reactionsLike = 1,
            isPinned = true
        )
        database.channelPostDao().insertPost(welcomePost)
        channelId
    }

    // Broadcast a new message / post to a channel (Admin only)
    suspend fun broadcastChannelPost(
        channelId: String,
        text: String,
        mediaType: String = "TEXT",
        mediaUrl: String? = null,
        mediaTitle: String? = null,
        isPinned: Boolean = false,
        pollQuestion: String? = null,
        pollOption1: String? = null,
        pollOption2: String? = null,
        pollOption3: String? = null,
        pollOption4: String? = null
    ): ChannelPostEntity = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val authorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Administrateur ADT"

        val postId = "post_" + UUID.randomUUID().toString().take(8)
        val post = ChannelPostEntity(
            id = postId,
            channelId = channelId,
            authorName = authorName,
            authorRole = "Administrateur",
            text = text.trim(),
            mediaType = mediaType,
            mediaUrl = mediaUrl,
            mediaTitle = mediaTitle,
            timestamp = System.currentTimeMillis(),
            viewsCount = 1,
            isPinned = isPinned,
            pollQuestion = pollQuestion?.trim(),
            pollOption1 = pollOption1?.trim(),
            pollOption2 = pollOption2?.trim(),
            pollOption3 = pollOption3?.trim(),
            pollOption4 = pollOption4?.trim()
        )
        database.channelPostDao().insertPost(post)
        database.channelDao().updateLatestPost(channelId, text.trim().take(100), System.currentTimeMillis())
        post
    }

    // React to a channel broadcast post
    suspend fun reactToChannelPost(postId: String, emoji: String) = withContext(Dispatchers.IO) {
        val post = database.channelPostDao().getPostById(postId) ?: return@withContext
        val updated = when (emoji) {
            "❤️" -> post.copy(reactionsHeart = post.reactionsHeart + 1, userReaction = "❤️")
            "👍" -> post.copy(reactionsLike = post.reactionsLike + 1, userReaction = "👍")
            "🚀" -> post.copy(reactionsRocket = post.reactionsRocket + 1, userReaction = "🚀")
            "🔥" -> post.copy(reactionsFire = post.reactionsFire + 1, userReaction = "🔥")
            "👏" -> post.copy(reactionsClap = post.reactionsClap + 1, userReaction = "👏")
            else -> post.copy(reactionsLike = post.reactionsLike + 1, userReaction = emoji)
        }
        database.channelPostDao().updatePost(updated)
    }

    // Vote in a channel poll
    suspend fun voteChannelPoll(postId: String, optionIndex: Int) = withContext(Dispatchers.IO) {
        val post = database.channelPostDao().getPostById(postId) ?: return@withContext
        if (post.userVotedOption != null) return@withContext // already voted

        val updated = when (optionIndex) {
            1 -> post.copy(pollVotes1 = post.pollVotes1 + 1, userVotedOption = 1)
            2 -> post.copy(pollVotes2 = post.pollVotes2 + 1, userVotedOption = 2)
            3 -> post.copy(pollVotes3 = post.pollVotes3 + 1, userVotedOption = 3)
            4 -> post.copy(pollVotes4 = post.pollVotes4 + 1, userVotedOption = 4)
            else -> post
        }
        database.channelPostDao().updatePost(updated)
    }

    // Pin or unpin post
    suspend fun togglePinChannelPost(postId: String, isPinned: Boolean) = withContext(Dispatchers.IO) {
        database.channelPostDao().setPinned(postId, isPinned)
    }

    // Delete post
    suspend fun deleteChannelPost(postId: String) = withContext(Dispatchers.IO) {
        database.channelPostDao().deletePost(postId)
    }

    // Toggle Follow
    suspend fun toggleChannelFollow(channelId: String, isFollowing: Boolean) = withContext(Dispatchers.IO) {
        database.channelDao().toggleFollow(channelId, isFollowing)
    }

    // Toggle Mute Notifications
    suspend fun toggleChannelMute(channelId: String, isMuted: Boolean) = withContext(Dispatchers.IO) {
        database.channelDao().toggleMute(channelId, isMuted)
    }

    // Delete Channel (Admin only)
    suspend fun deleteChannel(channelId: String) = withContext(Dispatchers.IO) {
        database.channelPostDao().clearPostsForChannel(channelId)
        database.channelDao().deleteChannel(channelId)
    }

    // Post Status
    suspend fun postStatus(text: String, mediaUrl: String? = null, bgHex: String = "#075E54") = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val authorName = if (user != null && user.firstName.isNotBlank()) "${user.firstName} ${user.lastName}" else "Mon Statut"
        val status = StatusEntity(
            id = UUID.randomUUID().toString(),
            authorName = authorName,
            authorPhone = user?.phone ?: "",
            authorAvatar = user?.avatarUrl ?: "",
            text = text,
            mediaUrl = mediaUrl,
            backgroundColorHex = bgHex,
            timestamp = System.currentTimeMillis(),
            viewsCount = 0,
            isSelf = true
        )
        database.statusDao().insertStatus(status)
    }

    suspend fun viewStatus(statusId: String) = withContext(Dispatchers.IO) {
        database.statusDao().incrementViewsCount(statusId)
    }

    suspend fun deleteStatus(statusId: String) = withContext(Dispatchers.IO) {
        database.statusDao().deleteStatus(statusId)
    }

    suspend fun purgeExpiredStatuses() = withContext(Dispatchers.IO) {
        val twentyFourHoursAgo = System.currentTimeMillis() - (24L * 60L * 60L * 1000L)
        database.statusDao().deleteExpiredStatuses(twentyFourHoursAgo)
    }

    // Log Call
    suspend fun logCall(
        contactName: String,
        contactPhone: String,
        contactAvatar: String,
        durationSeconds: Int,
        isVideo: Boolean,
        isOutgoing: Boolean,
        isMissed: Boolean
    ) = withContext(Dispatchers.IO) {
        val log = CallLogEntity(
            id = UUID.randomUUID().toString(),
            contactName = contactName,
            contactPhone = contactPhone,
            contactAvatar = contactAvatar,
            timestamp = System.currentTimeMillis(),
            durationSeconds = durationSeconds,
            isVideo = isVideo,
            isOutgoing = isOutgoing,
            isMissed = isMissed
        )
        database.callDao().insertCall(log)
    }

    // Settings
    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) = withContext(Dispatchers.IO) {
        val current = database.settingsDao().getSettings() ?: AppSettingsEntity()
        val updated = transform(current)
        database.settingsDao().insertOrUpdate(updated)
    }

    // Contacts Synchronization via ContactManager
    suspend fun syncContactsWithDevice(context: android.content.Context): com.example.util.ContactSyncResult = withContext(Dispatchers.IO) {
        com.example.util.ContactManager.syncAndPersistContacts(context, this@AdtRepository, database)
    }

    suspend fun getAllMessagesSync(): List<MessageEntity> = withContext(Dispatchers.IO) {
        database.messageDao().getAllMessagesSync()
    }

    suspend fun getAllChatsSync(): List<ChatEntity> = withContext(Dispatchers.IO) {
        database.chatDao().getAllChatsSync()
    }

    suspend fun getExportSnapshot(
        selectedChatId: String? = null,
        includeArchived: Boolean = true
    ): ConversationExportSnapshot = withContext(Dispatchers.IO) {
        val user = database.userDao().getUser()
        val allChats = database.chatDao().getAllChatsSync()
        val filteredChats = when {
            !selectedChatId.isNullOrBlank() -> allChats.filter { it.id == selectedChatId }
            includeArchived -> allChats
            else -> allChats.filter { !it.isArchived }
        }.sortedByDescending { it.lastTimestamp }

        val messagesByChat = if (!selectedChatId.isNullOrBlank()) {
            mapOf(selectedChatId to database.messageDao().getMessagesForChatSync(selectedChatId))
        } else {
            val allMessages = database.messageDao().getAllMessagesSync()
            val grouped = allMessages.groupBy { it.chatId }
            filteredChats.associate { chat ->
                chat.id to (grouped[chat.id]?.sortedBy { it.timestamp } ?: emptyList())
            }
        }

        ConversationExportSnapshot(
            user = user,
            chats = filteredChats,
            messagesByChat = messagesByChat
        )
    }
}

data class ConversationExportSnapshot(
    val user: UserEntity?,
    val chats: List<ChatEntity>,
    val messagesByChat: Map<String, List<MessageEntity>>
)

typealias CallDaoCalls = CallLogEntity
