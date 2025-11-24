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

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deadline = :date")
    suspend fun getTasksByDate(date: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE status = :status")
    suspend fun getTasksByStatus(status: String): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE subject_id = :subjectId")
    suspend fun getTasksBySubject(subjectId: Int): List<TaskEntity>

    @Query("UPDATE tasks SET status = :status WHERE task_id = :taskId")
    suspend fun updateTaskStatus(taskId: Int, status: String)

    @Query("DELETE FROM tasks WHERE task_id = :taskId")
    suspend fun deleteTask(taskId: Int)

}
