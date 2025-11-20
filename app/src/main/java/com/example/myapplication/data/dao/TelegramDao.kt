package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.TelegramMessageEntity

@Dao
interface TelegramDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<TelegramMessageEntity>)

    @Query("SELECT * FROM telegram_messages ORDER BY date DESC")
    suspend fun getAllMessages(): List<TelegramMessageEntity>

    @Query("SELECT * FROM telegram_messages WHERE chat_id = :chatId ORDER BY date DESC")
    suspend fun getMessagesForChat(chatId: Long): List<TelegramMessageEntity>

    // Добавьте этот метод
    @Query("SELECT * FROM telegram_messages WHERE message_type = :type ORDER BY date DESC")
    suspend fun getMessagesByType(type: String): List<TelegramMessageEntity>
}
