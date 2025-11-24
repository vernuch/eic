package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.FileEntity

@Dao
interface FileDao {

    @Query("SELECT * FROM files")
    suspend fun getAllFiles(): List<FileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Query("SELECT * FROM files WHERE task_id = :taskId")
    suspend fun getFilesForTask(taskId: Int): List<FileEntity>

    @Query("SELECT * FROM files WHERE replacement_id = :replacementId")
    suspend fun getFilesForReplacement(replacementId: Int): List<FileEntity>

    @Query("DELETE FROM files")
    suspend fun deleteAllFiles()
}

