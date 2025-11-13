package com.example.myapplication.data.worker

import android.content.Context
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.TelegramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TelegramSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val repository = TelegramRepository(applicationContext, db.telegramDao())

            repository.initTDLib(
                apiId = 123456,
                apiHash = "abcdef1234567890"
            )

            repository.getAllMessages()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}


