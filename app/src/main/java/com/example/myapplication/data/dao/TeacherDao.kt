package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.entities.TeacherEntity

@Dao
interface TeacherDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity)

    @Query("SELECT * FROM teachers")
    suspend fun getAllTeachers(): List<TeacherEntity>

    @Query("DELETE FROM teachers")
    suspend fun deleteAllTeachers()

    @Query("SELECT * FROM teachers WHERE name = :name")
    suspend fun getTeacherByName(name: String): TeacherEntity?

}
