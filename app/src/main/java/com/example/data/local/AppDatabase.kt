package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.crypto.RoomDatabaseSecurityManager
import com.example.data.local.dao.CallDao
import com.example.data.local.dao.ChannelDao
import com.example.data.local.dao.ChannelPostDao
import com.example.data.local.dao.ChatDao
import com.example.data.local.dao.ContactDao
import com.example.data.local.dao.EncryptedChatDao
import com.example.data.local.dao.EncryptedMessageDao
import com.example.data.local.dao.GroupMemberDao
import com.example.data.local.dao.MessageDao
import com.example.data.local.dao.SettingsDao
import com.example.data.local.dao.StatusDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.AppSettingsEntity
import com.example.data.local.entity.CallLogEntity
import com.example.data.local.entity.ChannelEntity
import com.example.data.local.entity.ChannelPostEntity
import com.example.data.local.entity.ChatEntity
import com.example.data.local.entity.ContactEntity
import com.example.data.local.entity.GroupMemberEntity
import com.example.data.local.entity.MessageEntity
import com.example.data.local.entity.StatusEntity
import com.example.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        ContactEntity::class,
        ChatEntity::class,
        GroupMemberEntity::class,
        MessageEntity::class,
        StatusEntity::class,
        ChannelEntity::class,
        ChannelPostEntity::class,
        CallLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun contactDao(): ContactDao
    abstract fun rawChatDao(): ChatDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun rawMessageDao(): MessageDao
    abstract fun statusDao(): StatusDao
    abstract fun channelDao(): ChannelDao
    abstract fun channelPostDao(): ChannelPostDao
    abstract fun callDao(): CallDao
    abstract fun settingsDao(): SettingsDao

    @Volatile
    private var _encryptedMessageDao: MessageDao? = null

    @Volatile
    private var _encryptedChatDao: ChatDao? = null

    /**
     * Transparent AES-256 encrypted MessageDao wrapper.
     */
    open fun messageDao(): MessageDao {
        return _encryptedMessageDao ?: synchronized(this) {
            _encryptedMessageDao ?: EncryptedMessageDao(
                delegate = rawMessageDao(),
                securityManager = RoomDatabaseSecurityManager.getInstance()
            ).also { _encryptedMessageDao = it }
        }
    }

    /**
     * Transparent AES-256 encrypted ChatDao wrapper.
     */
    open fun chatDao(): ChatDao {
        return _encryptedChatDao ?: synchronized(this) {
            _encryptedChatDao ?: EncryptedChatDao(
                delegate = rawChatDao(),
                securityManager = RoomDatabaseSecurityManager.getInstance()
            ).also { _encryptedChatDao = it }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val appContext = context.applicationContext
            RoomDatabaseSecurityManager.init(appContext)
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "adt_pro_social_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
