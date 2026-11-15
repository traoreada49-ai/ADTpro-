package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["chatId"]),
        Index(value = ["timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderPhone: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mediaType: String = "TEXT", // TEXT, AUDIO, PHOTO, VIDEO, STICKER, DOC
    val mediaUrl: String = "",
    val mediaDuration: Int = 0, // In seconds for voice note or video
    val isOutgoing: Boolean = false,
    val isForwarded: Boolean = false,
    val forwardedCount: Int = 0,
    val status: String = "READ", // SENT, DELIVERED, READ
    val replyToText: String? = null,
    val replyToSender: String? = null,
    val reaction: String? = null
)

typealias ChatMessage = MessageEntity
typealias ChatMessageEntity = MessageEntity
