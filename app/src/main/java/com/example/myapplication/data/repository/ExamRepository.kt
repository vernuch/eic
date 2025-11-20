package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.ExamDao
import com.example.myapplication.data.dao.FileDao
import com.example.myapplication.data.entities.ExamEntity
import com.example.myapplication.data.entities.FileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ExamRepository(
    private val examDao: ExamDao,
    private val fileDao: FileDao
) {

    suspend fun saveExam(
        subjectName: String,
        date: String,
        location: String,
        teacher: String,
        files: List<Pair<String, String>>
    ) = withContext(Dispatchers.IO) {
        val exam = ExamEntity(
            exam_id = UUID.randomUUID().hashCode(),
            subject_id = subjectName.hashCode(),
            exam_date = date,
            location = location,
            teacher_id = teacher.hashCode(),
            integration_id = null
        )
        examDao.insertExam(exam)

        files.forEach { (name, url) ->
            fileDao.insertFile(
                FileEntity(
                    file_id = UUID.randomUUID().hashCode(),
                    task_id = null,
                    replacement_id = null,
                    name = name,
                    url = url
                )
            )
        }
    }
}
