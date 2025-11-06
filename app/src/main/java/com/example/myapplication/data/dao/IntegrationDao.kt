package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.IntegrationEntity

@Dao
interface IntegrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegration(integration: IntegrationEntity)

    @Query("SELECT * FROM integrations")
    suspend fun getAllIntegrations(): List<IntegrationEntity>

    @Query("DELETE FROM integrations")
    suspend fun deleteAllIntegrations()
}
