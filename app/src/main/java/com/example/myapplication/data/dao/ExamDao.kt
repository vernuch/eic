package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.ExamEntity

@Dao
interface ExamDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: ExamEntity)

    @Query("SELECT * FROM exams ORDER BY exam_date ASC")
    suspend fun getAllExams(): List<ExamEntity>

    @Query("DELETE FROM exams")
    suspend fun deleteAllExams()
}
