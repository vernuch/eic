package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_attachments")
data class TaskAttachmentEntity(
    @PrimaryKey val attachment_id: Int,
    val task_id: Int,
    val file_url: String,
    val type: String
)
