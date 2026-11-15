package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    indices = [
        Index(value = ["isArchived", "isPinned", "lastTimestamp"]),
        Index(value = ["lastTimestamp"]),
        Index(value = ["isLocked"])
    ]
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val isChannel: Boolean = false,
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val lastTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val membersCount: Int = 1,
    val description: String = "",
    val creatorPhone: String = "",
    val wallpaperId: String = "DEFAULT",
    val isLocked: Boolean = false
)

typealias ChatConversation = ChatEntity
typealias ChatConversationEntity = ChatEntity
