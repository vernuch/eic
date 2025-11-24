package com.example.myapplication.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.repository.TelegramRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

class TelegramSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "TelegramSyncWorker"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Telegram sync worker...")

            val db = AppDatabase.getDatabase(applicationContext)
            val repository = TelegramRepository(applicationContext, db.telegramDao())

            val apiId = inputData.getInt("api_id", 33509625)
            val apiHash = inputData.getString("api_hash") ?: "15f188573a4c526e73560eb271aa8a35"

            repository.initTDLib(apiId = apiId, apiHash = apiHash)

            val isReady = repository.waitForAuthReady(30000)
            if (!isReady) {
                Log.w(TAG, "TDLib not ready within timeout")
                return@withContext Result.retry()
            }

            val selectedChatIds = getSelectedChats(repository)
            if (selectedChatIds.isEmpty()) {
                Log.d(TAG, "No selected chats to sync")
                return@withContext Result.success()
            }

            Log.d(TAG, "Syncing ${selectedChatIds.size} selected chats")

            val syncSuccess = repository.syncSelectedChatsHistory(limitPerChat = 100)

            if (syncSuccess) {
                Log.d(TAG, "Telegram sync completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "Telegram sync failed")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in Telegram sync worker: ${e.message}", e)
            Result.retry()
        }
    }

    private fun getSelectedChats(repository: TelegramRepository): Set<Long> {
        return repository.getSelectedChats()
    }

    private suspend fun cacheMedia(repository: TelegramRepository, chatId: Long) {
        try {
            val messages = repository.getSavedMessagesForChat(chatId)
            messages.forEach { msg ->
                msg.media_url?.let { url ->
                    if (url.startsWith("http")) {
                        try {
                            val fileName = url.substringAfterLast("/")
                            val cacheDir = File(applicationContext.cacheDir, "telegram/$chatId")
                            if (!cacheDir.exists()) cacheDir.mkdirs()
                            val file = File(cacheDir, fileName)

                            if (!file.exists()) {
                                downloadFile(url, file)
                                Log.d(TAG, "Cached media file: ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to cache media for chat $chatId: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching media for chat $chatId: ${e.message}")
        }
    }

    private fun downloadFile(url: String, destFile: File) {
        try {
            URL(url).openStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file from $url: ${e.message}")
            throw e
        }
    }
}




