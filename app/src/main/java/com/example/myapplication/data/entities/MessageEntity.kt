package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val message_id: Int,
    val integration_id: Int?,
    val source: String,
    val content: String,
    val received_at: String
)
