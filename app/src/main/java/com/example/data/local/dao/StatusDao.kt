package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.StatusEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusEntity>)

    @Query("DELETE FROM statuses WHERE id = :id")
    suspend fun deleteStatus(id: String)

    @Query("DELETE FROM statuses WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteExpiredStatuses(cutoffTimestamp: Long)

    @Query("UPDATE statuses SET viewsCount = viewsCount + 1 WHERE id = :id")
    suspend fun incrementViewsCount(id: String)
}
