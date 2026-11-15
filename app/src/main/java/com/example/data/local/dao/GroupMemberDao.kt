package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.GroupMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupMemberDao {
    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY isAdmin DESC, name ASC")
    fun getMembersForGroupFlow(groupId: String): Flow<List<GroupMemberEntity>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId ORDER BY isAdmin DESC, name ASC")
    suspend fun getMembersForGroup(groupId: String): List<GroupMemberEntity>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND contactPhone = :contactPhone LIMIT 1")
    suspend fun getMember(groupId: String, contactPhone: String): GroupMemberEntity?

    @Query("SELECT COUNT(*) FROM group_members WHERE groupId = :groupId")
    suspend fun getMemberCount(groupId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: GroupMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GroupMemberEntity>)

    @Update
    suspend fun updateMember(member: GroupMemberEntity)

    @Query("UPDATE group_members SET isAdmin = :isAdmin, role = :role WHERE groupId = :groupId AND contactPhone = :contactPhone")
    suspend fun updateAdminStatus(groupId: String, contactPhone: String, isAdmin: Boolean, role: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND contactPhone = :contactPhone")
    suspend fun deleteMember(groupId: String, contactPhone: String)

    @Query("DELETE FROM group_members WHERE groupId = :groupId")
    suspend fun deleteMembersForGroup(groupId: String)
}
