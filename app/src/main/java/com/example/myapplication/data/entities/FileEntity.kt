package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey val file_id: Int,
    val task_id: Int?,
    val replacement_id: Int?,
    val name: String,
    val url: String
)
