package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.TaskEntity

@Dao
interface TaskDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE status = 'active'")
    suspend fun getActiveTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = 'archived'")
    suspend fun getArchivedTasks(): List<TaskEntity>

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
