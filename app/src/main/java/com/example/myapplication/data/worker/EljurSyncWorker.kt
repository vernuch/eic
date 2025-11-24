package com.example.myapplication.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.EljurRepository

class EljurSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
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

        try {
            when (val authResult = repo.authorizeEljur()) {
                is EljurRepository.AuthResult.Success -> {
                    // Авторизация успешна, продолжаем синхронизацию
                    repo.fetchSchedule()
                    repo.fetchTasks()
                    repo.fetchMessages()
                    repo.fetchReplacements()
                    return Result.success()
                }
                is EljurRepository.AuthResult.Error -> {
                    return Result.retry()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }
}