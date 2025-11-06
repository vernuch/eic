package com.example.myapplication.data.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val task_id: Int,
    val subject_id: Int,
    val title: String,
    val description: String,
    val deadline: String,
    val status: String, // 'active' | 'archived'
    val integration_id: Int?
)
