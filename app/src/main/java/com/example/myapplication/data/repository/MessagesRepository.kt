package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.MessageDao
import com.example.myapplication.data.dao.TelegramDao
import com.example.myapplication.data.entities.MessageEntity
import com.example.myapplication.data.entities.TelegramMessageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class MessagesRepository(
    private val messageDao: MessageDao,
    private val telegramDao: TelegramDao
) {

    suspend fun saveEljurMessage(content: String) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(
            MessageEntity(
                message_id = UUID.randomUUID().hashCode(),
                integration_id = null,
                source = "eljur",
                content = content,
                received_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            )
        )
    }

    suspend fun saveTelegramMessages(messages: List<TelegramMessageEntity>) =
        withContext(Dispatchers.IO) {
            telegramDao.insertMessages(messages)
        }
}
