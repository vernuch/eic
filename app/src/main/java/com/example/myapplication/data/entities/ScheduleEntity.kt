package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val schedule_id: Int = 0,
    val date: String,
    val subject_id: Int,
    val start_time: String,
    val end_time: String,
    val is_replacement: Boolean = false,
    val replacement_note: String? = null,
    val week_type: Int // 0 нечётная, 1 чётная
)
