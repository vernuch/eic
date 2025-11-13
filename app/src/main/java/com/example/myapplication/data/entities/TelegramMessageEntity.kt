package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telegram_messages")
data class TelegramMessageEntity(
    @PrimaryKey val message_id: Long,
    val chat_id: Long,
    val sender_name: String,
    val content: String?,
    val media_url: String?,
    val date: String
)
