package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_posts")
data class ChannelPostEntity(
    @PrimaryKey val id: String,
    val channelId: String,
    val authorName: String = "Administrateur ADT",
    val authorRole: String = "Administrateur",
    val text: String,
    val mediaType: String = "TEXT", // "TEXT", "IMAGE", "ANNOUNCEMENT", "POLL", "DOCUMENT"
    val mediaUrl: String? = null,
    val mediaTitle: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val viewsCount: Int = 1450,
    val reactionsHeart: Int = 0,
    val reactionsLike: Int = 0,
    val reactionsRocket: Int = 0,
    val reactionsFire: Int = 0,
    val reactionsClap: Int = 0,
    val userReaction: String? = null, // "❤️", "👍", "🚀", "🔥", "👏"
    val isPinned: Boolean = false,
    val pollQuestion: String? = null,
    val pollOption1: String? = null,
    val pollOption2: String? = null,
    val pollOption3: String? = null,
    val pollOption4: String? = null,
    val pollVotes1: Int = 0,
    val pollVotes2: Int = 0,
    val pollVotes3: Int = 0,
    val pollVotes4: Int = 0,
    val userVotedOption: Int? = null // 1, 2, 3, 4
)
