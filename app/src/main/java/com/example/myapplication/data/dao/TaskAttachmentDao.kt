package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.TaskAttachmentEntity

@Dao
interface TaskAttachmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: TaskAttachmentEntity)

    @Query("SELECT * FROM task_attachments WHERE task_id = :taskId")
    suspend fun getAttachmentsByTask(taskId: Int): List<TaskAttachmentEntity>

    @Query("DELETE FROM task_attachments")
    suspend fun deleteAllAttachments()
}
