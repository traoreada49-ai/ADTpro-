package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val countryCode: String,
    val avatarUrl: String,
    val bio: String,
    val isAdtUser: Boolean = true,
    val isOnline: Boolean = false,
    val lastSeenText: String = "Hier à 18:30"
)
