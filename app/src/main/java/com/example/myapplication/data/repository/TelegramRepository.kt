package com.example.myapplication.data.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.data.dao.TelegramDao
import com.example.myapplication.data.entities.TelegramMessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class TelegramRepository(
    private val context: Context,
    private val telegramDao: TelegramDao
) {

    private var client: Client? = null
    private var isAuthorized = false

    enum class AuthState { WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY }
    private val _authState = MutableStateFlow(AuthState.WAIT_PHONE)
    val authState = _authState.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun initTDLib(apiId: Int, apiHash: String) {
        if (client != null) return

        try {
            System.loadLibrary("tdjni") 
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TelegramRepository", "Failed to load tdjni: ${e.message}")
            return
        }

        client = Client.create({ update -> onUpdate(update) }, null, null)
        Log.d("TelegramRepository", "TDLib client created")

        val params = TdApi.SetTdlibParameters()
        params.useTestDc = false
        params.databaseDirectory = getTdlibPath()
        params.filesDirectory = getTdlibPath()
        params.useMessageDatabase = true
        params.useSecretChats = false
        params.apiId = apiId
        params.apiHash = apiHash
        params.systemLanguageCode = "en"
        params.deviceModel = "Android"
        params.systemVersion = android.os.Build.VERSION.RELEASE
        params.applicationVersion = "1.0"

        client?.send(params) { obj ->
            authCallback(obj)
        }
    }


    private fun getTdlibPath(): String {
        val dir = File(context.filesDir, "tdlib")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    fun sendPhoneNumber(phone: String) {
        client?.send(TdApi.SetAuthenticationPhoneNumber(phone, null)) { obj ->
            authCallback(obj)
        }
    }

    fun sendAuthCode(code: String) {
        client?.send(TdApi.CheckAuthenticationCode(code)) { obj ->
            authCallback(obj)
        }
    }

    fun sendPassword(password: String) {
        client?.send(TdApi.CheckAuthenticationPassword(password)) { obj ->
            authCallback(obj)
        }
    }

    private fun authCallback(obj: TdApi.Object) {
        when (obj) {
            is TdApi.AuthorizationStateWaitCode -> _authState.value = AuthState.WAIT_CODE
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = AuthState.WAIT_PASSWORD
            is TdApi.AuthorizationStateReady -> {
                _authState.value = AuthState.READY
                isAuthorized = true
                Log.d("TelegramRepository", "Authorization complete")
            }
            is TdApi.Error -> Log.e("TelegramRepository", "Auth failed: ${obj.message}")
            else -> Log.d("TelegramRepository", "Auth callback: $obj")
        }
    }

    private fun onUpdate(update: TdApi.Object) {
        when (update) {
            is TdApi.UpdateNewMessage -> handleMessage(update.message)
            is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateReady -> {
                isAuthorized = true
                _authState.value = AuthState.READY
            }
            is TdApi.AuthorizationStateClosed -> {
                isAuthorized = false
                _authState.value = AuthState.WAIT_PHONE
            }
        }
    }

    fun fetchChats(limit: Int = 50) {
        if (!isAuthorized) return
        client?.send(TdApi.GetChats(TdApi.ChatListMain(), limit)) { obj ->
            onChatsReceived(obj)
        }
    }

    private fun onChatsReceived(obj: TdApi.Object) {
        when (obj) {
            is TdApi.Chats -> obj.chatIds.forEach { chatId ->
                client?.send(TdApi.GetChat(chatId)) { chatObj ->
                    onChatInfo(chatObj)
                }
            }
            is TdApi.Error -> Log.e("TelegramRepository", "GetChats error: ${obj.message}")
        }
    }

    private fun onChatInfo(obj: TdApi.Object) {
        if (obj is TdApi.Chat) {
            Log.d("TelegramRepository", "Chat: ${obj.title} (id=${obj.id})")
        }
    }

    fun fetchChatHistory(chatId: Long, limit: Int = 50) {
        if (!isAuthorized) return
        client?.send(TdApi.GetChatHistory(chatId, 0, 0, limit, false)) { obj ->
            onMessagesReceived(obj)
        }
    }

    private fun onMessagesReceived(obj: TdApi.Object) {
        when (obj) {
            is TdApi.Messages -> obj.messages.forEach { handleMessage(it) }
            is TdApi.Error -> Log.e("TelegramRepository", "GetChatHistory error: ${obj.message}")
        }
    }

    private fun handleMessage(msg: TdApi.Message) {
        val contentText = when (val content = msg.content) {
            is TdApi.MessageText -> content.text.text
            is TdApi.MessagePhoto -> "Photo"
            is TdApi.MessageDocument -> " ${content.document.fileName}"
            else -> "(unsupported type)"
        }

        val senderName = when (val sender = msg.senderId) {
            is TdApi.MessageSenderUser -> "User ${sender.userId}"
            is TdApi.MessageSenderChat -> "Chat ${sender.chatId}"
            else -> "Unknown"
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(msg.date.toLong() * 1000))

        val mediaUrl = when (val content = msg.content) {
            is TdApi.MessagePhoto -> "Photo received"
            is TdApi.MessageDocument -> content.document.fileName
            else -> null
        }

        val entity = TelegramMessageEntity(
            message_id = msg.id,
            chat_id = msg.chatId,
            sender_name = senderName,
            content = contentText,
            media_url = mediaUrl,
            date = dateStr
        )

        repoScope.launch {
            telegramDao.insertMessages(listOf(entity))
        }
    }

    suspend fun getAllMessages(): List<TelegramMessageEntity> =
        withContext(Dispatchers.IO) { telegramDao.getAllMessages() }

    suspend fun getMessagesForChat(chatId: Long): List<TelegramMessageEntity> =
        withContext(Dispatchers.IO) { telegramDao.getMessagesForChat(chatId) }
}

