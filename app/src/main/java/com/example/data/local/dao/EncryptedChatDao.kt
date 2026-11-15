package com.example.data.local.dao

import com.example.data.crypto.RoomDatabaseSecurityManager
import com.example.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Transparent decorator for [ChatDao] that ensures conversation previews (lastMessage)
 * are encrypted with AES-256-GCM before writing to the local Room database,
 * and decrypted when reading from the database.
 */
class EncryptedChatDao(
    private val delegate: ChatDao,
    private val securityManager: RoomDatabaseSecurityManager
) : ChatDao {

    override fun getActiveChats(): Flow<List<ChatEntity>> {
        return delegate.getActiveChats().map { chats ->
            chats.map { securityManager.decryptChat(it) }
        }
    }

    override fun getLockedChats(): Flow<List<ChatEntity>> {
        return delegate.getLockedChats().map { chats ->
            chats.map { securityManager.decryptChat(it) }
        }
    }

    override suspend fun getLockedChatsSync(): List<ChatEntity> {
        val chats = delegate.getLockedChatsSync()
        return chats.map { securityManager.decryptChat(it) }
    }

    override fun getLockedChatsCountFlow(): Flow<Int> {
        return delegate.getLockedChatsCountFlow()
    }

    override suspend fun getLockedChatsCountSync(): Int {
        return delegate.getLockedChatsCountSync()
    }

    override suspend fun setChatLocked(chatId: String, isLocked: Boolean) {
        delegate.setChatLocked(chatId, isLocked)
    }

    override fun getArchivedChats(): Flow<List<ChatEntity>> {
        return delegate.getArchivedChats().map { chats ->
            chats.map { securityManager.decryptChat(it) }
        }
    }

    override suspend fun getArchivedChatsSync(): List<ChatEntity> {
        val chats = delegate.getArchivedChatsSync()
        return chats.map { securityManager.decryptChat(it) }
    }

    override fun getArchivedChatsCountFlow(): Flow<Int> {
        return delegate.getArchivedChatsCountFlow()
    }

    override suspend fun getArchivedChatsCountSync(): Int {
        return delegate.getArchivedChatsCountSync()
    }

    override suspend fun setChatArchived(chatId: String, isArchived: Boolean) {
        delegate.setChatArchived(chatId, isArchived)
    }

    override suspend fun getAllChatsSync(): List<ChatEntity> {
        val chats = delegate.getAllChatsSync()
        return chats.map { securityManager.decryptChat(it) }
    }

    override fun getChatFlow(chatId: String): Flow<ChatEntity?> {
        return delegate.getChatFlow(chatId).map { chat ->
            chat?.let { securityManager.decryptChat(it) }
        }
    }

    override suspend fun getChatById(chatId: String): ChatEntity? {
        val chat = delegate.getChatById(chatId) ?: return null
        return securityManager.decryptChat(chat)
    }

    override suspend fun insertChat(chat: ChatEntity) {
        val encrypted = securityManager.encryptChat(chat)
        delegate.insertChat(encrypted)
    }

    override suspend fun insertChats(chats: List<ChatEntity>) {
        val encryptedList = chats.map { securityManager.encryptChat(it) }
        delegate.insertChats(encryptedList)
    }

    override suspend fun updateChat(chat: ChatEntity) {
        val encrypted = securityManager.encryptChat(chat)
        delegate.updateChat(encrypted)
    }

    override suspend fun updateLastMessageIncoming(chatId: String, lastMessage: String, timestamp: Long) {
        val encryptedLastMsg = securityManager.encryptString(lastMessage)
        delegate.updateLastMessageIncoming(chatId, encryptedLastMsg, timestamp)
    }

    override suspend fun updateLastMessageOutgoing(chatId: String, lastMessage: String, timestamp: Long) {
        val encryptedLastMsg = securityManager.encryptString(lastMessage)
        delegate.updateLastMessageOutgoing(chatId, encryptedLastMsg, timestamp)
    }

    override suspend fun markChatAsRead(chatId: String) {
        delegate.markChatAsRead(chatId)
    }

    override suspend fun deleteChat(chatId: String) {
        delegate.deleteChat(chatId)
    }
}
