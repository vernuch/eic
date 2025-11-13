package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_messages")
data class TelegramMessageEntity(
    @PrimaryKey val message_id: Long,  // используем Long для уникальности TDLib ID
    val chat_id: Long,                 // ID чата
    val sender_name: String,
    val content: String?,
    val media_url: String?,            // ссылка на файл/фото, если есть
    val date: String                   // дата/время в формате ISO
)
