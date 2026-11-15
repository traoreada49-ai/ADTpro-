package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesForChatSync(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesSync(): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessageForChat(chatId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET reaction = :reaction WHERE id = :messageId")
    suspend fun updateReaction(messageId: String, reaction: String?)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("SELECT * FROM messages WHERE chatId = :chatId AND senderPhone != :myPhone AND status != :targetStatus")
    suspend fun getMessagesToUpdate(chatId: String, myPhone: String, targetStatus: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' ORDER BY timestamp DESC LIMIT 50")
    suspend fun searchMessagesByText(query: String): List<MessageEntity>

    @Query("SELECT m.* FROM messages m INNER JOIN chats c ON m.chatId = c.id WHERE c.isArchived = 1 ORDER BY m.timestamp DESC")
    suspend fun getArchivedMessagesSync(): List<MessageEntity>

    @Query("SELECT m.* FROM messages m INNER JOIN chats c ON m.chatId = c.id WHERE c.isArchived = 1 AND m.text LIKE '%' || :query || '%' ORDER BY m.timestamp DESC")
    suspend fun searchArchivedMessagesByText(query: String): List<MessageEntity>

    @Query("SELECT COUNT(m.id) FROM messages m INNER JOIN chats c ON m.chatId = c.id WHERE c.isArchived = 1")
    suspend fun getArchivedMessagesCountSync(): Int

    @Query("SELECT DISTINCT chatId FROM messages WHERE text LIKE '%' || :query || '%'")
    suspend fun getChatIdsWithMatchingMessages(query: String): List<String>

    @Query("SELECT * FROM messages WHERE mediaType = 'LIVE_LOCATION' ORDER BY timestamp DESC")
    suspend fun getLiveLocationMessagesSync(): List<MessageEntity>

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM messages WHERE chatId = :chatId")
    suspend fun clearMessagesForChat(chatId: String)
}
