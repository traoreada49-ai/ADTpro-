package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats WHERE isArchived = 0 AND isLocked = 0 ORDER BY isPinned DESC, lastTimestamp DESC")
    fun getActiveChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isLocked = 1 ORDER BY lastTimestamp DESC")
    fun getLockedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isLocked = 1 ORDER BY lastTimestamp DESC")
    suspend fun getLockedChatsSync(): List<ChatEntity>

    @Query("SELECT COUNT(*) FROM chats WHERE isLocked = 1")
    fun getLockedChatsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chats WHERE isLocked = 1")
    suspend fun getLockedChatsCountSync(): Int

    @Query("UPDATE chats SET isLocked = :isLocked WHERE id = :chatId")
    suspend fun setChatLocked(chatId: String, isLocked: Boolean)

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastTimestamp DESC")
    fun getArchivedChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE isArchived = 1 ORDER BY lastTimestamp DESC")
    suspend fun getArchivedChatsSync(): List<ChatEntity>

    @Query("SELECT COUNT(*) FROM chats WHERE isArchived = 1")
    fun getArchivedChatsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM chats WHERE isArchived = 1")
    suspend fun getArchivedChatsCountSync(): Int

    @Query("UPDATE chats SET isArchived = :isArchived WHERE id = :chatId")
    suspend fun setChatArchived(chatId: String, isArchived: Boolean)

    @Query("SELECT * FROM chats")
    suspend fun getAllChatsSync(): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    fun getChatFlow(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Update
    suspend fun updateChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastTimestamp = :timestamp, unreadCount = unreadCount + 1 WHERE id = :chatId")
    suspend fun updateLastMessageIncoming(chatId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastTimestamp = :timestamp WHERE id = :chatId")
    suspend fun updateLastMessageOutgoing(chatId: String, lastMessage: String, timestamp: Long)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markChatAsRead(chatId: String)

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun deleteChat(chatId: String)
}
