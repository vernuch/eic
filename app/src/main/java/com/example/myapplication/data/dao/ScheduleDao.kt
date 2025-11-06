package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.ScheduleEntity

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedule WHERE date = :date")
    suspend fun getSchedulesByDate(date: String): List<ScheduleEntity>

    @Query("DELETE FROM schedule")
    suspend fun deleteAllSchedules()

    @Query("SELECT * FROM schedule WHERE date = :date AND week_type = :weekType")
    suspend fun getSchedulesByDateAndWeek(date: String, weekType: Int): List<ScheduleEntity>

}
