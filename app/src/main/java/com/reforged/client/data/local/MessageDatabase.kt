package com.reforged.client.data.local

import androidx.room.*

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val peerId: Long,
    val lastMessageText: String,
    val lastMessageDate: Long,
    val title: String,
    val photoUrl: String?,
    val unreadCount: Int
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: Long,
    val peerId: Long,
    val fromId: Long,
    val text: String,
    val date: Long,
    val isOut: Boolean
)

@Dao
interface MessageDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageDate DESC")
    suspend fun getConversations(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM messages WHERE peerId = :peerId ORDER BY date DESC LIMIT :limit OFFSET :offset")
    suspend fun getHistory(peerId: Long, limit: Int, offset: Int): List<MessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE peerId = :peerId")
    suspend fun clearHistory(peerId: Long)
}

@Database(entities = [ConversationEntity::class, MessageEntity::class], version = 1)
abstract class MessageDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
