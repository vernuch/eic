package com.example.myapplication.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.EljurRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TasksWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repo = EljurRepository(
                integrationDao = db.integrationDao(),
                scheduleDao = db.scheduleDao(),
                subjectDao = db.subjectDao(),
                teacherDao = db.teacherDao(),
                taskDao = db.taskDao(),
                messageDao = db.messageDao(),
                fileDao = db.fileDao(),
                replacementDao = db.replacementDao(),
                studentInfoDao = db.studentInfoDao()
            )

            when (repo.authorizeEljur()) {
                is EljurRepository.AuthResult.Success -> {
                    repo.fetchTasks()
                    Result.success()
                }
                is EljurRepository.AuthResult.Error -> {
                    Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
