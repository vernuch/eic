package com.example.myapplication.data.entities
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val setting_id: Int,
    val theme: String
)
