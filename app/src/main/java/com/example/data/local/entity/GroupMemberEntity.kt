package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "group_members",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["contactPhone"]),
        Index(value = ["groupId", "contactPhone"], unique = true)
    ]
)
data class GroupMemberEntity(
    @PrimaryKey val id: String, // e.g. "${groupId}_${contactPhone}"
    val groupId: String,
    val contactPhone: String,
    val name: String,
    val avatarUrl: String = "",
    val isAdmin: Boolean = false,
    val role: String = "MEMBER", // ADMIN, CREATOR, MEMBER
    val joinedTimestamp: Long = System.currentTimeMillis(),
    val bio: String = ""
)
