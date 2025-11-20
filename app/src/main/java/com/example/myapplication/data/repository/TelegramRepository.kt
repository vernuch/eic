package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.myapplication.data.dao.TelegramDao
import com.example.myapplication.data.entities.TelegramMessageEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.TdApi
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.ss.usermodel.WorkbookFactory

class TelegramRepository(
    private val context: Context,
    private val telegramDao: TelegramDao
) {

    companion object {
        private const val TAG = "TelegramRepository"
        private const val PREFS_NAME = "telegram_repo_prefs"
        private const val PREF_KEY_SELECTED_CHATS = "selected_chats"
        private const val AUTH_WAIT_TIMEOUT_MS = 60_000L
        private const val REQUEST_TIMEOUT_MS = 30_000L

        private const val TYPE_SCHEDULE = "SCHEDULE"
        private const val TYPE_REPLACEMENT = "REPLACEMENT"
        private const val TYPE_HOMEWORK = "HOMEWORK"
        private const val TYPE_EXAM = "EXAM"
        private const val TYPE_PRACTICE = "PRACTICE"
        private const val TYPE_OTHER = "OTHER"

        private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp")
    }

    private var client: Client? = null

    enum class AuthState { WAIT_PHONE, WAIT_CODE, WAIT_PASSWORD, READY }
    private val _authState = MutableStateFlow(AuthState.WAIT_PHONE)
    val authState = _authState.asStateFlow()

    private val repoScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Volatile
    private var selectedChats: Set<Long> = emptySet()

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val pendingRequests = ConcurrentHashMap<Int, CompletableDeferred<TdApi.Object>>()
    private val pendingFileUpdates = ConcurrentHashMap<Int, CompletableDeferred<TdApi.UpdateFile>>()

    private val tempFilesDir by lazy {
        File(context.cacheDir, "telegram_temp").apply { mkdirs() }
    }

    init {
        loadSelectedChatsFromPrefs()
    }

    private fun persistSelectedChats() {
        prefs.edit().putString(PREF_KEY_SELECTED_CHATS, selectedChats.joinToString(",")).apply()
    }

    private fun loadSelectedChatsFromPrefs() {
        val s = prefs.getString(PREF_KEY_SELECTED_CHATS, "") ?: ""
        selectedChats = if (s.isBlank()) emptySet() else s.split(",").mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun setSelectedChats(chatIds: Collection<Long>) {
        selectedChats = chatIds.toSet()
        persistSelectedChats()
    }

    fun getSelectedChats(): Set<Long> = selectedChats

    fun initTDLib(apiId: Int, apiHash: String) {
        if (client != null) return
        try {
            System.loadLibrary("tdjni")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load tdjni: ${e.message}")
            return
        }

        client = Client.create({ update ->
            onUpdate(update)
        }, null, null)

        Log.d(TAG, "TDLib client created")

        val params = TdApi.SetTdlibParameters().apply {
            useTestDc = false
            databaseDirectory = getTdlibPath()
            filesDirectory = getTdlibPath()
            useMessageDatabase = true
            useSecretChats = false
            this.apiId = apiId
            this.apiHash = apiHash
            systemLanguageCode = "en"
            deviceModel = "Android"
            systemVersion = android.os.Build.VERSION.RELEASE
            applicationVersion = "1.0"
        }

        client?.send(
            params,
            Client.ResultHandler { res -> authCallback(res) }
        )

        client?.send(
            TdApi.GetAuthorizationState(),
            Client.ResultHandler { res -> authCallback(res) }
        )
    }

    private fun getTdlibPath(): String {
        val dir = File(context.filesDir, "tdlib")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    fun sendPhoneNumber(phone: String) {
        client?.send(
            TdApi.SetAuthenticationPhoneNumber(phone, null),
            Client.ResultHandler { obj -> authCallback(obj) }
        )
    }

    fun sendAuthCode(code: String) {
        client?.send(
            TdApi.CheckAuthenticationCode(code),
            Client.ResultHandler { obj -> authCallback(obj) }
        )
    }

    fun sendPassword(password: String) {
        client?.send(
            TdApi.CheckAuthenticationPassword(password),
            Client.ResultHandler { obj -> authCallback(obj) }
        )
    }

    private fun authCallback(obj: TdApi.Object) {
        when (obj) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = AuthState.WAIT_PHONE
            is TdApi.AuthorizationStateWaitCode -> _authState.value = AuthState.WAIT_CODE
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = AuthState.WAIT_PASSWORD
            is TdApi.AuthorizationStateReady -> {
                _authState.value = AuthState.READY
                Log.d(TAG, "Authorization ready")
            }
            is TdApi.Error -> Log.e(TAG, "Auth error: ${obj.message}")
            else -> {
                Log.d(TAG, "Auth callback other: $obj")
            }
        }
    }

    private fun handleAuthState(state: TdApi.AuthorizationState) {
        when (state) {
            is TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = AuthState.WAIT_PHONE
            is TdApi.AuthorizationStateWaitCode -> _authState.value = AuthState.WAIT_CODE
            is TdApi.AuthorizationStateWaitPassword -> _authState.value = AuthState.WAIT_PASSWORD
            is TdApi.AuthorizationStateReady -> _authState.value = AuthState.READY
            is TdApi.AuthorizationStateClosed -> _authState.value = AuthState.WAIT_PHONE
            else -> Unit
        }
    }

    suspend fun waitForAuthReady(timeoutMs: Long = AUTH_WAIT_TIMEOUT_MS): Boolean = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (_authState.value == AuthState.READY) return@withContext true
            delay(500)
        }
        return@withContext false
    }

    private suspend fun sendRequestAwait(
        request: TdApi.Function<*>,
        timeoutMs: Long = REQUEST_TIMEOUT_MS
    ): TdApi.Object =
        withContext(Dispatchers.IO) {
            val deferred = CompletableDeferred<TdApi.Object>()
            val key = System.identityHashCode(deferred)
            pendingRequests[key] = deferred
            try {
                client?.send(
                    request,
                    Client.ResultHandler { res ->
                        if (!deferred.isCompleted) deferred.complete(res)
                    }
                )

                withTimeout(timeoutMs) {
                    deferred.await()
                }
            } finally {
                pendingRequests.remove(key)
            }
        }

    private fun onUpdate(update: TdApi.Object) {
        try {
            when (update) {
                is TdApi.UpdateNewMessage -> {
                    handleMessage(update.message)
                }
                is TdApi.UpdateMessageContent -> {
                }
                is TdApi.UpdateFile -> {
                    val file = update.file
                    val deferred = pendingFileUpdates.remove(file.id)
                    deferred?.complete(update)
                }
                is TdApi.UpdateAuthorizationState -> handleAuthState(update.authorizationState)
                else -> {
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "onUpdate error", e)
        }
    }

    private data class ClassificationResult(
        val type: String,
        val confidence: Float,
        val extractedData: Map<String, Any> = emptyMap()
    )

    private fun classifyMessage(content: String, fileName: String? = null): ClassificationResult {
        val lowerContent = content.lowercase(Locale.getDefault())
        val lowerFileName = fileName?.lowercase(Locale.getDefault()) ?: ""

        val scores = mutableMapOf(
            TYPE_SCHEDULE to 0f,
            TYPE_REPLACEMENT to 0f,
            TYPE_HOMEWORK to 0f,
            TYPE_EXAM to 0f,
            TYPE_PRACTICE to 0f,
            TYPE_OTHER to 0f
        )

        val extractedData = mutableMapOf<String, Any>()

        val scheduleKeywords = listOf("расписание", "расписани", "schedule", "понедельник", "вторник", "среда",
            "четверг", "пятница", "суббота", "неделя", "пар", "урок")
        val replacementKeywords = listOf("замена", "добавление", "добавлен", "отмена", "перенос", "пересдача", "вместо", "изменение")
        val homeworkKeywords = listOf("домаш", "дз", "домашка", "задание", "упражнение", "лабораторная",  "практическая", "лаб", "проект", "курсовая", "exercise", "homework")
        val examKeywords = listOf("экзамен", "зачёт", "зачет", "билет", "вопрос", "тест", "практика")

        scheduleKeywords.forEach { if (lowerContent.contains(it)) scores[TYPE_SCHEDULE] = scores[TYPE_SCHEDULE]!! + 1f }
        replacementKeywords.forEach { if (lowerContent.contains(it)) scores[TYPE_REPLACEMENT] = scores[TYPE_REPLACEMENT]!! + 1f }
        homeworkKeywords.forEach { if (lowerContent.contains(it)) scores[TYPE_HOMEWORK] = scores[TYPE_HOMEWORK]!! + 1f }
        examKeywords.forEach { if (lowerContent.contains(it)) scores[TYPE_EXAM] = scores[TYPE_EXAM]!! + 1f }

        if (fileName != null) {
            when {
                lowerFileName.contains("расписание") || lowerFileName.contains("schedule") ->
                    scores[TYPE_SCHEDULE] = scores[TYPE_SCHEDULE]!! + 2f
                lowerFileName.contains("замен") || lowerFileName.contains("replacement") ->
                    scores[TYPE_REPLACEMENT] = scores[TYPE_REPLACEMENT]!! + 2f
                lowerFileName.contains("домаш") || lowerFileName.contains("homework") || lowerFileName.contains("дз") ->
                    scores[TYPE_HOMEWORK] = scores[TYPE_HOMEWORK]!! + 2f
                lowerFileName.contains("экзамен") || lowerFileName.contains("exam") || lowerFileName.contains("зачет") ->
                    scores[TYPE_EXAM] = scores[TYPE_EXAM]!! + 2f
                lowerFileName.contains("практик") || lowerFileName.contains("practice") || lowerFileName.contains("лаб") ->
                    scores[TYPE_PRACTICE] = scores[TYPE_PRACTICE]!! + 2f
            }
        }

        extractStructuredData(content, extractedData)

        val maxEntry = scores.maxByOrNull { it.value }!!

        val confidence = maxEntry.value / (scheduleKeywords.size + 1)
        val finalType = if (confidence < 0.3f && maxEntry.value < 2f) TYPE_OTHER else maxEntry.key

        return ClassificationResult(finalType, confidence, extractedData)
    }

    private fun extractStructuredData(content: String, data: MutableMap<String, Any>) {
        val groupRegex = Regex("\\b\\d{1,3}[А-ЯЁа-яёA-Za-z]{0,3}-?\\d{0,2}\\b")
        val groups = groupRegex.findAll(content).map { it.value }.toSet()
        if (groups.isNotEmpty()) {
            data["groups"] = groups.toList()
        }

        val pairRegex = Regex("([1-6])[-\\s]?я?\\s*пара", RegexOption.IGNORE_CASE)
        val pairs = pairRegex.findAll(content).map { it.groups[1]?.value?.toIntOrNull() }.filterNotNull().toSet()
        if (pairs.isNotEmpty()) {
            data["pairs"] = pairs.toList()
        }

        val roomRegex = Regex("(?:каб\\.?|ауд\\.?|room)\\s*(\\d{1,4}[A-Za-zА-Яа-яёЁ]?)", RegexOption.IGNORE_CASE)
        val rooms = roomRegex.findAll(content).map { it.groups[1]?.value }.filterNotNull().toSet()
        if (rooms.isNotEmpty()) {
            data["rooms"] = rooms.toList()
        }

        val dateRegex = Regex("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{2,4}\\b")
        val dates = dateRegex.findAll(content).map { it.value }.toSet()
        if (dates.isNotEmpty()) {
            data["dates"] = dates.toList()
        }

        val teacherRegex = Regex("\\b[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?\\b")
        val teachers = teacherRegex.findAll(content).map { it.value }.toSet()
        if (teachers.isNotEmpty()) {
            data["teachers"] = teachers.toList()
        }
    }

    private suspend fun processFileContent(filePath: String, fileName: String): String {
        return try {
            val extension = fileName.substringAfterLast('.', "").lowercase()

            when {
                extension == "pdf" -> extractTextFromPdf(filePath)
                extension in setOf("doc", "docx") -> extractTextFromWord(filePath)
                extension in setOf("xls", "xlsx") -> extractTextFromExcel(filePath)
                extension in IMAGE_EXTENSIONS -> extractTextFromImage(filePath)
                else -> {
                    Log.d(TAG, "Unsupported file format: $extension")
                    "Файл формата $extension (содержимое не извлечено)"
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file $fileName: ${e.message}", e)
            "Ошибка обработки файла: ${e.message}"
        }
    }

    private fun extractTextFromPdf(filePath: String): String {
        return try {
            FileInputStream(filePath).use { fis ->
                val document = PDDocument.load(fis)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from PDF", e)
            "PDF файл (текст не извлечен: ${e.message})"
        }
    }

    private fun extractTextFromWord(filePath: String): String {
        return try {
            FileInputStream(filePath).use { fis ->
                val document = XWPFDocument(fis)
                val extractor = XWPFWordExtractor(document)
                val text = extractor.text
                extractor.close()
                text.trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from Word document", e)
            "Word документ (текст не извлечен: ${e.message})"
        }
    }

    private fun extractTextFromExcel(filePath: String): String {
        return try {
            FileInputStream(filePath).use { fis ->
                val workbook = WorkbookFactory.create(fis)
                val text = StringBuilder()

                for (sheetNum in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(sheetNum)
                    text.append("Лист: ${sheet.sheetName}\n")

                    for (row in sheet) {
                        val rowText = StringBuilder()
                        for (cell in row) {
                            when (cell.cellType) {
                                org.apache.poi.ss.usermodel.CellType.STRING -> rowText.append(cell.stringCellValue).append("\t")
                                org.apache.poi.ss.usermodel.CellType.NUMERIC -> rowText.append(cell.numericCellValue).append("\t")
                                else -> rowText.append("\t")
                            }
                        }
                        text.append(rowText.toString().trim()).append("\n")
                    }
                    text.append("\n")
                }
                workbook.close()
                text.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from Excel document", e)
            "Excel документ (текст не извлечен: ${e.message})"
        }
    }

    private fun extractTextFromImage(filePath: String): String {
        // TODO: Реализовать OCR для извлечения текста из изображений
        return "Изображение (текст не распознан - требуется настройка OCR)"
    }

    private suspend fun parseTdMessage(msg: TdApi.Message, chatId: Long): TelegramMessageEntity? {
        val content = msg.content
        var textContent = ""
        var fileName: String? = null
        var filePath: String? = null
        var requiresFileProcessing = false
        var fileProcessingFailed = false

        when (content) {
            is TdApi.MessageText -> {
                textContent = content.text.text
            }
            is TdApi.MessagePhoto -> {
                textContent = content.caption?.text ?: "Фото"
                requiresFileProcessing = true
                filePath = downloadPhoto(content.photo)
                if (filePath == null) {
                    fileProcessingFailed = true
                    textContent += " [Фото не скачано]"
                }
            }
            is TdApi.MessageDocument -> {
                textContent = content.caption?.text ?: (content.document.fileName ?: "Документ")
                fileName = content.document.fileName
                requiresFileProcessing = true
                filePath = downloadDocument(content.document)
                if (filePath == null) {
                    fileProcessingFailed = true
                    textContent += " [Документ не скачан]"
                }
            }
            else -> {
                textContent = "Неподдерживаемый тип сообщения: ${content.javaClass.simpleName}"
            }
        }

        var finalTextContent = textContent

        if (requiresFileProcessing && filePath != null && fileName != null) {
            try {
                val fileContent = processFileContent(filePath, fileName)
                finalTextContent += "\n\n[Содержимое файла]:\n$fileContent"

                if (fileContent.contains("не извлечен") || fileContent.contains("Ошибка обработки")) {
                    fileProcessingFailed = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "File processing failed for $fileName", e)
                finalTextContent += "\n\n[Ошибка обработки файла: ${e.message}]"
                fileProcessingFailed = true
            }
        }

        val classification = if (fileProcessingFailed) {
            ClassificationResult(TYPE_OTHER, 0.1f)
        } else {
            classifyMessage(finalTextContent, fileName)
        }

        val senderName = when (val sender = msg.senderId) {
            is TdApi.MessageSenderUser -> {
                try {
                    val user = sendRequestAwait(TdApi.GetUser(sender.userId)) as? TdApi.User
                    "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim()
                } catch (e: Exception) {
                    "user_${sender.userId}"
                }
            }
            is TdApi.MessageSenderChat -> "chat_${sender.chatId}"
            else -> "unknown"
        }

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date(msg.date.toLong() * 1000))

        return TelegramMessageEntity(
            message_id = msg.id,
            chat_id = chatId,
            sender_name = senderName,
            content = finalTextContent,
            media_url = fileName ?: filePath,
            date = dateStr,
            message_type = classification.type,
            confidence = classification.confidence,
            extracted_data = classification.extractedData.toString()
        )
    }

    private suspend fun downloadPhoto(photo: TdApi.Photo): String? {
        val largestSize = photo.sizes.maxByOrNull { it.width * it.height } ?: return null
        return downloadFile(largestSize.photo)
    }

    private suspend fun downloadDocument(document: TdApi.Document): String? {
        return downloadFile(document.document)
    }

    private suspend fun downloadFile(file: TdApi.File): String? {
        return withContext(Dispatchers.IO) {
            try {
                if (file.local.isDownloadingCompleted) {
                    return@withContext file.local.path
                }

                val deferred = CompletableDeferred<TdApi.UpdateFile>()
                pendingFileUpdates[file.id] = deferred

                client?.send(TdApi.DownloadFile(file.id, 1, 0, 0, true), Client.ResultHandler {})

                val update = withTimeout(60_000L) { deferred.await() }
                if (update.file.local.isDownloadingCompleted) {
                    update.file.local.path
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file", e)
                null
            } finally {
                pendingFileUpdates.remove(file.id)
            }
        }
    }

    suspend fun getAllChats(limit: Int = 200): List<TdApi.Chat> = withContext(Dispatchers.IO) {
        val chats = ArrayList<TdApi.Chat>()
        if (_authState.value != AuthState.READY) {
            Log.w(TAG, "getAllChats: TDLib not ready")
            return@withContext chats
        }

        val res = sendRequestAwait(TdApi.GetChats(TdApi.ChatListMain(), limit))
        if (res is TdApi.Chats) {
            val chatIds = res.chatIds
            for (id in chatIds) {
                val chatObj = sendRequestAwait(TdApi.GetChat(id))
                if (chatObj is TdApi.Chat) {
                    chats.add(chatObj)
                }
            }
        } else if (res is TdApi.Error) {
            Log.e(TAG, "GetChats error: ${res.message}")
        }
        return@withContext chats
    }

    suspend fun getChatHistory(chatId: Long, limit: Int = 100): List<TdApi.Message> = withContext(Dispatchers.IO) {
        val messages = ArrayList<TdApi.Message>()
        if (_authState.value != AuthState.READY) {
            Log.w(TAG, "getChatHistory: TDLib not ready")
            return@withContext messages
        }

        val res = sendRequestAwait(TdApi.GetChatHistory(chatId, 0, 0, limit, false))
        if (res is TdApi.Messages) {
            messages.addAll(res.messages)
        } else if (res is TdApi.Error) {
            Log.e(TAG, "GetChatHistory error for $chatId: ${res.message}")
        }
        return@withContext messages
    }

    suspend fun syncSelectedChatsHistory(limitPerChat: Int = 100): Boolean = withContext(Dispatchers.IO) {
        if (_authState.value != AuthState.READY) {
            Log.w(TAG, "syncSelectedChatsHistory: not authorized")
            return@withContext false
        }

        if (selectedChats.isEmpty()) {
            Log.i(TAG, "No selected chats to sync")
            return@withContext true
        }

        for (chatId in selectedChats) {
            try {
                val messages = getChatHistory(chatId, limitPerChat)
                val toInsert = ArrayList<TelegramMessageEntity>()
                for (msg in messages) {
                    val parsed = parseTdMessage(msg, chatId)
                    if (parsed != null) toInsert.add(parsed)
                }

                if (toInsert.isNotEmpty()) {
                    telegramDao.insertMessages(toInsert)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error syncing chat $chatId", e)
            }
        }
        return@withContext true
    }

    private fun handleMessage(msg: TdApi.Message) {
        try {
            if (selectedChats.isNotEmpty() && !selectedChats.contains(msg.chatId)) return

            repoScope.launch {
                try {
                    val entity = parseTdMessage(msg, msg.chatId) ?: return@launch
                    telegramDao.insertMessages(listOf(entity))
                } catch (e: Throwable) {
                    Log.e(TAG, "DB insert failed for message ${msg.id}", e)
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "handleMessage error", e)
        }
    }

    suspend fun getAllSavedMessages(): List<TelegramMessageEntity> = withContext(Dispatchers.IO) {
        telegramDao.getAllMessages()
    }

    suspend fun getSavedMessagesForChat(chatId: Long): List<TelegramMessageEntity> = withContext(Dispatchers.IO) {
        telegramDao.getMessagesForChat(chatId)
    }

    suspend fun getMessagesByType(type: String): List<TelegramMessageEntity> = withContext(Dispatchers.IO) {
        telegramDao.getMessagesByType(type)
    }

    suspend fun getOtherMessages(): List<TelegramMessageEntity> = withContext(Dispatchers.IO) {
        telegramDao.getMessagesByType(TYPE_OTHER)
    }

    fun close() {
        try {
            client?.send(TdApi.Close(), Client.ResultHandler { })
        } catch (e: Exception) {
            Log.e(TAG, "Error closing TDLib client", e)
        } finally {
            client = null
            repoScope.cancel()
            tempFilesDir.listFiles()?.forEach { it.delete() }
        }
    }
}