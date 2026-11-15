package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ChannelPostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelPostDao {
    @Query("SELECT * FROM channel_posts WHERE channelId = :channelId ORDER BY isPinned DESC, timestamp DESC")
    fun getPostsForChannel(channelId: String): Flow<List<ChannelPostEntity>>

    @Query("SELECT * FROM channel_posts WHERE id = :postId LIMIT 1")
    suspend fun getPostById(postId: String): ChannelPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: ChannelPostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<ChannelPostEntity>)

    @Update
    suspend fun updatePost(post: ChannelPostEntity)

    @Query("DELETE FROM channel_posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    @Query("DELETE FROM channel_posts WHERE channelId = :channelId")
    suspend fun clearPostsForChannel(channelId: String)

    @Query("UPDATE channel_posts SET isPinned = :isPinned WHERE id = :postId")
    suspend fun setPinned(postId: String, isPinned: Boolean)

    @Query("UPDATE channel_posts SET viewsCount = viewsCount + 1 WHERE id = :postId")
    suspend fun incrementViews(postId: String)
}
