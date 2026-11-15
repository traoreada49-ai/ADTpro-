package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int = 1,
    val phone: String = "",
    val countryCode: String = "+223",
    val firstName: String = "",
    val lastName: String = "",
    val businessCategory: String = "Professionnel ADT",
    val bio: String = "Disponible sur ADT Pro",
    val avatarUrl: String = "",
    val isRegistered: Boolean = false,
    val operatorName: String = "Orange"
) {
    @get:Ignore
    val name: String
        get() = if (firstName.isNotBlank() || lastName.isNotBlank()) "$firstName $lastName".trim() else "Membre ADT"
}

typealias User = UserEntity
