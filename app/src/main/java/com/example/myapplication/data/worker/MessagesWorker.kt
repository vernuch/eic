package com.example.myapplication.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.EljurRepository
import com.example.myapplication.data.repository.TelegramRepository
import com.example.myapplication.data.telegram.TelegramConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MessagesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)

            val eljurRepo = EljurRepository(
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

            when (eljurRepo.authorizeEljur()) {
                is EljurRepository.AuthResult.Success -> {
                    eljurRepo.fetchMessages()
                }
                is EljurRepository.AuthResult.Error -> {
                }
            }

            val telegramRepo = TelegramRepository(applicationContext, db.telegramDao())

            telegramRepo.initTDLib(
                apiId = TelegramConfig.API_ID,
                apiHash = TelegramConfig.API_HASH
            )

            val isTelegramReady = telegramRepo.waitForAuthReady(30000)
            if (!isTelegramReady) {
                return@withContext Result.success()
            }

            val syncSuccess = telegramRepo.syncSelectedChatsHistory(limitPerChat = 50)

            if (syncSuccess) {
                Result.success()
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
