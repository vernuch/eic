package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replacements")
data class ReplacementEntity(
    @PrimaryKey val replacement_id: Int,
    val schedule_id: Int,
    val new_time: String? = null,
    val new_subject: String? = null,
    val location: String? = null,
    val note: String? = null,
    val integration_id: Int? = null
)
