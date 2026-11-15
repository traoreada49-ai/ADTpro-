package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val contactName: String,
    val contactPhone: String,
    val contactAvatar: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val isVideo: Boolean = false,
    val isOutgoing: Boolean = true,
    val isMissed: Boolean = false
)
