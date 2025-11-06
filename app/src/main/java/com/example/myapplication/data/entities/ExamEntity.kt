
package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey val exam_id: Int,
    val subject_id: Int,
    val exam_date: String,
    val location: String,
    val teacher_id: Int,
    val integration_id: Int?
)
