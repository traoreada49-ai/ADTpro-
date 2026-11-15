package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ChatMessage
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Repository interfacing with Room database to manage chat histories,
 * providing reactive flows and asynchronous methods for message retrieval, insertion, and updates.
 */
class ChatRepository(
    private val database: AppDatabase
) {
    private val chatDao: ChatDao = database.chatDao()
    private val messageDao: MessageDao = database.messageDao()

    /**
     * Observes all active (unarchived, unlocked) conversations, sorted by pinned state and last message timestamp.
     */
    val activeChats: Flow<List<ChatEntity>> = chatDao.getActiveChats()

    /**
     * Observes all biometric-locked conversations.
     */
    val lockedChats: Flow<List<ChatEntity>> = chatDao.getLockedChats()

    /**
     * Observes count of biometric-locked conversations.
     */
    val lockedChatsCount: Flow<Int> = chatDao.getLockedChatsCountFlow()

    /**
     * Locks or unlocks a conversation behind biometric authentication.
     */
    suspend fun setChatLocked(chatId: String, isLocked: Boolean) = withContext(Dispatchers.IO) {
        chatDao.setChatLocked(chatId, isLocked)
    }

    /**
     * Observes all archived conversations.
     */
    val archivedChats: Flow<List<ChatEntity>> = chatDao.getArchivedChats()

    /**
     * Observes the count of archived conversations.
     */
    val archivedChatsCount: Flow<Int> = chatDao.getArchivedChatsCountFlow()

    /**
     * Archives or unarchives a conversation in the local Room database.
     */
    suspend fun setChatArchived(chatId: String, isArchived: Boolean) = withContext(Dispatchers.IO) {
        chatDao.setChatArchived(chatId, isArchived)
    }

    /**
     * Observes a specific conversation by its unique ID.
     */
    fun observeChat(chatId: String): Flow<ChatEntity?> {
        return chatDao.getChatFlow(chatId)
    }

    /**
     * Observes messages in a conversation in chronological order.
     */
    fun observeMessages(chatId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesForChat(chatId)
    }

    /**
     * Synchronously loads all messages for a given chat ID on IO thread.
     */
    suspend fun getMessagesForChat(chatId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        messageDao.getMessagesForChatSync(chatId)
    }

    /**
     * Retrieves a single message by ID.
     */
    suspend fun getMessageById(messageId: String): ChatMessage? = withContext(Dispatchers.IO) {
        messageDao.getMessageById(messageId)
    }

    /**
     * Inserts a new chat message and updates the corresponding conversation's last message and timestamp.
     */
    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(message)
        val chat = chatDao.getChatById(message.chatId)
        if (chat != null) {
            val previewText = message.text.ifEmpty { "[${message.mediaType}]" }
            if (message.isOutgoing) {
                chatDao.updateLastMessageOutgoing(
                    chatId = message.chatId,
                    lastMessage = previewText,
                    timestamp = message.timestamp
                )
            } else {
                chatDao.updateLastMessageIncoming(
                    chatId = message.chatId,
                    lastMessage = previewText,
                    timestamp = message.timestamp
                )
            }
        }
    }

    /**
     * Creates and inserts an outgoing text message into the database.
     */
    suspend fun sendTextMessage(
        chatId: String,
        senderPhone: String,
        senderName: String,
        text: String,
        replyToText: String? = null,
        replyToSender: String? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderPhone = senderPhone,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isOutgoing = true,
            status = "SENT",
            mediaType = "TEXT",
            replyToText = replyToText,
            replyToSender = replyToSender
        )
        insertMessage(message)
        message
    }

    /**
     * Batch inserts a list of messages into Room.
     */
    suspend fun insertMessages(messages: List<ChatMessage>) = withContext(Dispatchers.IO) {
        messageDao.insertMessages(messages)
    }

    /**
     * Updates an existing message record in Room.
     */
    suspend fun updateMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        messageDao.updateMessage(message)
    }

    /**
     * Adds, changes, or clears an emoji reaction on a message.
     */
    suspend fun setMessageReaction(messageId: String, reaction: String?) = withContext(Dispatchers.IO) {
        messageDao.updateReaction(messageId, reaction)
    }

    /**
     * Deletes a single message by ID.
     */
    suspend fun deleteMessage(messageId: String) = withContext(Dispatchers.IO) {
        messageDao.deleteMessageById(messageId)
    }

    /**
     * Clears all message history for a specific conversation.
     */
    suspend fun clearChatHistory(chatId: String) = withContext(Dispatchers.IO) {
        messageDao.clearMessagesForChat(chatId)
    }

    /**
     * Inserts or updates a chat conversation.
     */
    suspend fun insertChat(chat: ChatEntity) = withContext(Dispatchers.IO) {
        chatDao.insertChat(chat)
    }

    /**
     * Marks a conversation as read (resets unread counter to 0).
     */
    suspend fun markChatAsRead(chatId: String) = withContext(Dispatchers.IO) {
        chatDao.markChatAsRead(chatId)
    }

    /**
     * Deletes a chat conversation and all associated messages.
     */
    suspend fun deleteChat(chatId: String) = withContext(Dispatchers.IO) {
        messageDao.clearMessagesForChat(chatId)
        chatDao.deleteChat(chatId)
    }
}
