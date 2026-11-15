package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChannelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY followersCount DESC")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isFollowing = 1 ORDER BY latestPostTimestamp DESC")
    fun getFollowedChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE isOwner = 1 OR adminPhone = :phone ORDER BY followersCount DESC")
    fun getMyAdminChannels(phone: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    fun getChannelFlow(channelId: String): Flow<ChannelEntity?>

    @Query("SELECT * FROM channels WHERE id = :channelId LIMIT 1")
    suspend fun getChannelById(channelId: String): ChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE id = :channelId")
    suspend fun deleteChannel(channelId: String)

    @Query("UPDATE channels SET isFollowing = :isFollowing, followersCount = followersCount + (CASE WHEN :isFollowing THEN 1 ELSE -1 END) WHERE id = :channelId")
    suspend fun toggleFollow(channelId: String, isFollowing: Boolean)

    @Query("UPDATE channels SET isMuted = :isMuted WHERE id = :channelId")
    suspend fun toggleMute(channelId: String, isMuted: Boolean)

    @Query("UPDATE channels SET latestPostText = :text, latestPostTimestamp = :timestamp WHERE id = :channelId")
    suspend fun updateLatestPost(channelId: String, text: String, timestamp: Long)
}
