package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.ReplacementEntity

@Dao
interface ReplacementDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReplacement(replacement: ReplacementEntity)

    @Query("SELECT * FROM replacements WHERE schedule_id = :scheduleId")
    suspend fun getReplacementsForSchedule(scheduleId: Int): List<ReplacementEntity>

    @Query("SELECT * FROM replacements")
    suspend fun getAllReplacements(): List<ReplacementEntity>

    @Query("DELETE FROM replacements")
    suspend fun clearAll()
}
