package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String = "",
    val category: String = "Actualités & Business",
    val followersCount: Int = 1250,
    val isVerified: Boolean = true,
    val isFollowing: Boolean = false,
    val adminPhone: String = "",
    val adminName: String = "",
    val isOwner: Boolean = false,
    val isMuted: Boolean = false,
    val channelLink: String = "",
    val latestPostText: String = "",
    val latestPostTimestamp: Long = System.currentTimeMillis()
)
