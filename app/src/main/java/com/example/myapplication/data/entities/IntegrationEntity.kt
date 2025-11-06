package com.example.myapplication.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "integrations")
data class IntegrationEntity(
    @PrimaryKey val integration_id: Int,
    val service: String,
    val login: String,
    val password_enc: String,
    val token: String
)
