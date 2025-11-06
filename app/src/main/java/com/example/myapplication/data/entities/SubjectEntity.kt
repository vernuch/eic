package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(entity = TeacherEntity::class,
            parentColumns = ["teacher_id"],
            childColumns = ["teacher_id"],
            onDelete = ForeignKey.SET_NULL),
        ForeignKey(entity = IntegrationEntity::class,
            parentColumns = ["integration_id"],
            childColumns = ["integration_id"],
            onDelete = ForeignKey.SET_NULL)
    ]
)
data class SubjectEntity(
    @PrimaryKey val subject_id: Int,
    val name: String,
    val teacher_id: Int?,
    val integration_id: Int?
)
