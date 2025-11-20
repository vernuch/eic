package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "student_info")
data class StudentInfoEntity(
    @PrimaryKey val student_id: Int,
    val full_name: String,
    val group_name: String,
    val integration_id: Int,
    val last_updated: String
)