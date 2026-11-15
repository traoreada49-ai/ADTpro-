package com.example.data.local.dao

import com.example.data.crypto.RoomDatabaseSecurityManager
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Transparent decorator for [MessageDao] that encrypts messages with AES-256-GCM
 * before writing to the local Room database, and decrypts messages when reading from the database.
 */
class EncryptedMessageDao(
    private val delegate: MessageDao,
    private val securityManager: RoomDatabaseSecurityManager
) : MessageDao {

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> {
        return delegate.getMessagesForChat(chatId).map { messages ->
            messages.map { securityManager.decryptMessage(it) }
        }
    }

    override suspend fun getMessagesForChatSync(chatId: String): List<MessageEntity> {
        val messages = delegate.getMessagesForChatSync(chatId)
        return messages.map { securityManager.decryptMessage(it) }
    }

    override suspend fun getAllMessagesSync(): List<MessageEntity> {
        val messages = delegate.getAllMessagesSync()
        return messages.map { securityManager.decryptMessage(it) }
    }

    override suspend fun getMessageById(messageId: String): MessageEntity? {
        val message = delegate.getMessageById(messageId) ?: return null
        return securityManager.decryptMessage(message)
    }

    override suspend fun getLatestMessageForChat(chatId: String): MessageEntity? {
        val message = delegate.getLatestMessageForChat(chatId) ?: return null
        return securityManager.decryptMessage(message)
    }

    override suspend fun insertMessage(message: MessageEntity) {
        val encrypted = securityManager.encryptMessage(message)
        delegate.insertMessage(encrypted)
    }

    override suspend fun insertMessages(messages: List<MessageEntity>) {
        val encryptedList = messages.map { securityManager.encryptMessage(it) }
        delegate.insertMessages(encryptedList)
    }

    override suspend fun updateMessage(message: MessageEntity) {
        val encrypted = securityManager.encryptMessage(message)
        delegate.updateMessage(encrypted)
    }

    override suspend fun updateReaction(messageId: String, reaction: String?) {
        delegate.updateReaction(messageId, reaction)
    }

    override suspend fun updateMessageStatus(messageId: String, status: String) {
        delegate.updateMessageStatus(messageId, status)
    }

    override suspend fun getMessagesToUpdate(
        chatId: String,
        myPhone: String,
        targetStatus: String
    ): List<MessageEntity> {
        val messages = delegate.getMessagesToUpdate(chatId, myPhone, targetStatus)
        return messages.map { securityManager.decryptMessage(it) }
    }

    override suspend fun searchMessagesByText(query: String): List<MessageEntity> {
        if (query.isBlank()) return emptyList()
        // Because text is encrypted with AES-256 at rest in SQLite, SQL LIKE queries on ciphertext won't match.
        // We load, decrypt, and perform full-text in-memory search for 100% accuracy.
        val allDecrypted = getAllMessagesSync()
        return allDecrypted.filter { msg ->
            msg.text.contains(query, ignoreCase = true)
        }.sortedByDescending { it.timestamp }.take(50)
    }

    override suspend fun getArchivedMessagesSync(): List<MessageEntity> {
        val messages = delegate.getArchivedMessagesSync()
        return messages.map { securityManager.decryptMessage(it) }
    }

    override suspend fun searchArchivedMessagesByText(query: String): List<MessageEntity> {
        if (query.isBlank()) return emptyList()
        val archivedDecrypted = getArchivedMessagesSync()
        return archivedDecrypted.filter { msg ->
            msg.text.contains(query, ignoreCase = true)
        }.sortedByDescending { it.timestamp }
    }

    override suspend fun getArchivedMessagesCountSync(): Int {
        return delegate.getArchivedMessagesCountSync()
    }

    override suspend fun getChatIdsWithMatchingMessages(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val allDecrypted = getAllMessagesSync()
        return allDecrypted.filter { msg ->
            msg.text.contains(query, ignoreCase = true)
        }.map { it.chatId }.distinct()
    }

    override suspend fun getLiveLocationMessagesSync(): List<MessageEntity> {
        val messages = delegate.getLiveLocationMessagesSync()
        return messages.map { securityManager.decryptMessage(it) }
    }

    override suspend fun deleteMessageById(messageId: String) {
        delegate.deleteMessageById(messageId)
    }

    override suspend fun clearMessagesForChat(chatId: String) {
        delegate.clearMessagesForChat(chatId)
    }
}
