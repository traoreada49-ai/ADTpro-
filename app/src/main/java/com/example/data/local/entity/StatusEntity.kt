package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val id: String,
    val authorName: String,
    val authorPhone: String,
    val authorAvatar: String,
    val text: String,
    val mediaUrl: String? = null,
    val backgroundColorHex: String = "#075E54",
    val timestamp: Long = System.currentTimeMillis(),
    val viewsCount: Int = 0,
    val isSelf: Boolean = false
)
