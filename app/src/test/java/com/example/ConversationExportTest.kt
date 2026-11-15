package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.export.ConversationExportManager
import com.example.data.export.ExportFormat
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConversationExportTest {

    private fun sampleUser() = UserEntity(
        id = 1,
        phone = "+22370001122",
        countryCode = "+223",
        firstName = "Ada",
        lastName = "Traoré",
        isRegistered = true
    )

    private fun sampleChats() = listOf(
        ChatEntity(
            id = "chat_1",
            title = "Équipe Projet",
            isGroup = true,
            membersCount = 4,
            lastMessage = "Réunion confirmée à 15h",
            lastTimestamp = 1757100000000L
        ),
        ChatEntity(
            id = "chat_2",
            title = "Moussa Diallo",
            isGroup = false,
            isArchived = true,
            membersCount = 2,
            lastMessage = "À demain matin",
            lastTimestamp = 1757050000000L
        )
    )

    private fun sampleMessages() = mapOf(
        "chat_1" to listOf(
            MessageEntity(
                id = "msg_1",
                chatId = "chat_1",
                senderPhone = "+22370001122",
                senderName = "Ada Traoré",
                text = "Bonjour l'équipe, tout est prêt pour le déploiement.",
                timestamp = 1757099900000L,
                isOutgoing = true
            ),
            MessageEntity(
                id = "msg_2",
                chatId = "chat_1",
                senderPhone = "+221770001122",
                senderName = "Moussa Diallo",
                text = "Parfait ! Réunion confirmée à 15h.",
                timestamp = 1757100000000L,
                isOutgoing = false,
                replyToSender = "Ada Traoré",
                replyToText = "Bonjour l'équipe",
                reaction = "👍"
            )
        ),
        "chat_2" to listOf(
            MessageEntity(
                id = "msg_3",
                chatId = "chat_2",
                senderPhone = "+221770001122",
                senderName = "Moussa Diallo",
                text = "À demain matin",
                timestamp = 1757050000000L,
                isOutgoing = false,
                mediaType = "AUDIO",
                mediaDuration = 12
            )
        )
    )

    @Test
    fun exportConversations_json_writesValidStructuredFileAndListsLocally() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val result = ConversationExportManager.exportConversations(
                context = context,
                format = ExportFormat.JSON,
                user = sampleUser(),
                chats = sampleChats(),
                messagesByChat = sampleMessages(),
                scopeLabel = "Toutes les conversations"
            )

            val file = File(result.filePath)
            assertTrue("JSON file should exist", file.exists())
            assertTrue("JSON file should be non-empty", file.length() > 0)
            assertEquals(2, result.conversationsCount)
            assertEquals(3, result.messagesCount)
            assertTrue(result.fileName.endsWith(".json"))

            val json = JSONObject(file.readText(Charsets.UTF_8))
            assertEquals("ADT Pro", json.getString("app"))
            assertEquals(2, json.getInt("totalConversations"))
            assertEquals(3, json.getInt("totalMessages"))
            assertEquals("Ada Traoré", json.getJSONObject("exportedBy").getString("name"))

            val conversations = json.getJSONArray("conversations")
            assertEquals("Équipe Projet", conversations.getJSONObject(0).getString("title"))
            val msgs = conversations.getJSONObject(0).getJSONArray("messages")
            assertEquals(2, msgs.length())
            assertEquals("Parfait ! Réunion confirmée à 15h.", msgs.getJSONObject(1).getString("text"))
            assertEquals("👍", msgs.getJSONObject(1).getString("reaction"))

            val listed = ConversationExportManager.listLocalExports(context)
            assertTrue(listed.any { it.filePath == result.filePath && it.format == ExportFormat.JSON })

            assertTrue(ConversationExportManager.deleteLocalExport(result.filePath))
            assertFalse(file.exists())
        }
    }
}
