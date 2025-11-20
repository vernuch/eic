package com.example.myapplication.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.entities.StudentInfoEntity

@Dao
interface StudentInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudentInfo(studentInfo: StudentInfoEntity)

    @Update
    suspend fun updateStudentInfo(studentInfo: StudentInfoEntity)

    @Query("SELECT * FROM student_info WHERE student_id = :studentId")
    suspend fun getStudentInfoById(studentId: Int): StudentInfoEntity?

    @Query("SELECT * FROM student_info WHERE integration_id = :integrationId")
    suspend fun getStudentInfoByIntegration(integrationId: Int): StudentInfoEntity?

    @Query("SELECT * FROM student_info WHERE group_name = :groupName")
    suspend fun getStudentsByGroup(groupName: String): List<StudentInfoEntity>

    @Query("SELECT * FROM student_info")
    suspend fun getAllStudentInfo(): List<StudentInfoEntity>

    @Query("DELETE FROM student_info WHERE student_id = :studentId")
    suspend fun deleteStudentInfo(studentId: Int)

    @Query("DELETE FROM student_info WHERE integration_id = :integrationId")
    suspend fun deleteStudentInfoByIntegration(integrationId: Int)

    @Query("SELECT DISTINCT group_name FROM student_info")
    suspend fun getAllGroups(): List<String>

    @Query("SELECT * FROM student_info WHERE full_name LIKE '%' || :name || '%'")
    suspend fun searchStudentsByName(name: String): List<StudentInfoEntity>

    @Query("SELECT * FROM student_info WHERE group_name = :groupName AND full_name LIKE '%' || :name || '%'")
    suspend fun searchStudentsInGroup(groupName: String, name: String): List<StudentInfoEntity>

    @Query("UPDATE student_info SET last_updated = :timestamp WHERE student_id = :studentId")
    suspend fun updateLastUpdated(studentId: Int, timestamp: String)
}