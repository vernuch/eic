package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.FileDao
import com.example.myapplication.data.dao.SubjectDao
import com.example.myapplication.data.dao.TaskDao
import com.example.myapplication.data.entities.FileEntity
import com.example.myapplication.data.entities.SubjectEntity
import com.example.myapplication.data.entities.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class TaskRepository(
    private val taskDao: TaskDao,
    private val subjectDao: SubjectDao,
    private val fileDao: FileDao
) {

    suspend fun saveTask(
        subjectName: String,
        description: String,
        attachments: List<Pair<String, String>> // name to URL
    ) = withContext(Dispatchers.IO) {
        val subject = subjectDao.getSubjectByName(subjectName) ?: SubjectEntity(
            subject_id = subjectName.hashCode(),
            name = subjectName,
            teacher_id = null,
            integration_id = null
        ).also { subjectDao.insertSubject(it) }

        val task = TaskEntity(
            task_id = UUID.randomUUID().hashCode(),
            subject_id = subject.subject_id,
            title = description.take(30),
            description = description,
            deadline = "",
            status = "active",
            integration_id = null
        )
        taskDao.insertTask(task)

        attachments.forEach { (name, url) ->
            fileDao.insertFile(
                FileEntity(
                    file_id = UUID.randomUUID().hashCode(),
                    task_id = task.task_id,
                    replacement_id = null,
                    name = name,
                    url = url
                )
            )
        }
    }
}
