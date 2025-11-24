package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.entities.*
import com.example.myapplication.data.sync.ConflictResolver
import com.example.myapplication.utils.DateTimeManager
import com.example.myapplication.data.validation.DataValidator
import com.example.myapplication.data.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

class EljurRepository(
    private val integrationDao: IntegrationDao,
    private val scheduleDao: ScheduleDao,
    private val subjectDao: SubjectDao,
    private val teacherDao: TeacherDao,
    private val taskDao: TaskDao,
    private val messageDao: MessageDao,
    private val fileDao: FileDao,
    private val replacementDao: ReplacementDao,
    private val studentInfoDao: StudentInfoDao,
    private val conflictResolver: ConflictResolver = ConflictResolver(taskDao)
) {
    private val baseUrl = "https://kmpo.eljur.ru"
    private val TAG = "EljurRepository"

    companion object {
        private const val TYPE_HOMEWORK = "HOMEWORK"
        private const val TYPE_EXAM = "EXAM"
        private const val TYPE_REPLACEMENT = "REPLACEMENT"
        private const val TYPE_ANNOUNCEMENT = "ANNOUNCEMENT"
        private const val TYPE_OTHER = "OTHER"
    }

    // Добавляем sealed классы в начало файла
    sealed class AuthResult {
        object Success : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    sealed class DataResult<out T> {
        data class Success<out T>(val data: T) : DataResult<T>()
        data class Error(val message: String) : DataResult<Nothing>()
    }

    suspend fun getLocalStudentInfo(): StudentInfoEntity? = withContext(Dispatchers.IO) {
        try {
            studentInfoDao.getStudentInfo()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local student info: ${e.message}")
            null
        }
    }

    suspend fun getLocalSchedule(): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.getAllSchedules()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local schedule: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLocalTasks(): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            taskDao.getAllTasks()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local tasks: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLocalMessages(): List<MessageEntity> = withContext(Dispatchers.IO) {
        try {
            messageDao.getAllMessages()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local messages: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLocalFiles(): List<FileEntity> = withContext(Dispatchers.IO) {
        try {
            fileDao.getAllFiles()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local files: ${e.message}")
            emptyList()
        }
    }

    suspend fun getLocalReplacements(): List<ReplacementEntity> = withContext(Dispatchers.IO) {
        try {
            replacementDao.getAllReplacements()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local replacements: ${e.message}")
            emptyList()
        }
    }

    suspend fun getScheduleForDate(date: String): List<ScheduleEntity> = withContext(Dispatchers.IO) {
        try {
            scheduleDao.getSchedulesByDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting schedule for date: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTasksForDate(date: String): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            taskDao.getTasksByDate(date)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks for date: ${e.message}")
            emptyList()
        }
    }

    suspend fun getActiveTasks(): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            taskDao.getTasksByStatus("active")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active tasks: ${e.message}")
            emptyList()
        }
    }

    suspend fun getCompletedTasks(): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            taskDao.getTasksByStatus("completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting completed tasks: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTasksBySubject(subjectId: Int): List<TaskEntity> = withContext(Dispatchers.IO) {
        try {
            taskDao.getTasksBySubject(subjectId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks by subject: ${e.message}")
            emptyList()
        }
    }

    suspend fun getSubjects(): List<SubjectEntity> = withContext(Dispatchers.IO) {
        try {
            subjectDao.getAllSubjects()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subjects: ${e.message}")
            emptyList()
        }
    }

    suspend fun getTeachers(): List<TeacherEntity> = withContext(Dispatchers.IO) {
        try {
            teacherDao.getAllTeachers()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting teachers: ${e.message}")
            emptyList()
        }
    }

    suspend fun updateTaskStatus(taskId: Int, status: String) = withContext(Dispatchers.IO) {
        try {
            taskDao.updateTaskStatus(taskId, status)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task status: ${e.message}")
        }
    }

    suspend fun deleteTask(taskId: Int) = withContext(Dispatchers.IO) {
        try {
            taskDao.deleteTask(taskId)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task: ${e.message}")
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        try {
            scheduleDao.deleteAllSchedules()
            taskDao.deleteAllTasks()
            messageDao.deleteAllMessages()
            replacementDao.deleteAllReplacements()
            fileDao.deleteAllFiles()
            Log.d(TAG, "All local data cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing data: ${e.message}")
        }
    }

    suspend fun authorizeEljur(): AuthResult = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext AuthResult.Error("Интеграция Eljur не настроена")

            Log.d(TAG, "Attempting authorization for user: ${integration.login}")

            val response = Jsoup.connect("$baseUrl/ajaxauthorize")
                .data("username", integration.login)
                .data("password", integration.password_enc)
                .data("remember", "1")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(org.jsoup.Connection.Method.POST)
                .execute()

            val responseBody = response.body()
            when {
                responseBody.contains("неверный пароль", ignoreCase = true) ->
                    AuthResult.Error("Неверный логин или пароль")
                responseBody.contains("error", ignoreCase = true) ->
                    AuthResult.Error("Ошибка авторизации")
                response.statusCode() != 200 ->
                    AuthResult.Error("Сервер недоступен: ${response.statusCode()}")
                else -> {
                    val token = response.cookies()["auth_token"]
                    if (token != null) {
                        integrationDao.insertIntegration(integration.copy(token = token))
                        Log.d(TAG, "Authorization success, token saved")
                        AuthResult.Success
                    } else {
                        AuthResult.Error("Токен авторизации не получен")
                    }
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            AuthResult.Error("Таймаут подключения к Eljur")
        } catch (e: java.net.UnknownHostException) {
            AuthResult.Error("Проверьте подключение к интернету")
        } catch (e: Exception) {
            Log.e(TAG, "Authorization failed: ${e.message}", e)
            AuthResult.Error("Ошибка авторизации: ${e.message}")
        }
    }

    suspend fun fetchStudentInfo(): DataResult<StudentInfoEntity> = withContext(Dispatchers.IO) {
        try {
            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Cannot fetch student info - authorization failed: ${authResult.message}")
                    return@withContext DataResult.Error(authResult.message)
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, proceeding with student info fetch")
                }
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext DataResult.Error("Интеграция Eljur не найдена")
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/")
                .cookies(cookies)
                .get()

            val userName = doc.select(".user-name, .profile-name, [class*='name']").firstOrNull()?.text()
            val groupInfo = doc.select(".group-info, .class-info, [class*='group']").firstOrNull()?.text()

            if (userName != null) {
                val studentInfo = StudentInfoEntity(
                    student_id = integration.integration_id,
                    full_name = userName.trim(),
                    group_name = groupInfo?.trim() ?: "Неизвестно",
                    integration_id = integration.integration_id,
                    last_updated = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                )

                when (val validation = DataValidator.validateStudentInfo(studentInfo)) {
                    is ValidationResult.Valid -> {
                        studentInfoDao.insertStudentInfo(studentInfo)
                        Log.d(TAG, "Student info fetched: $userName, Group: $groupInfo")
                        return@withContext DataResult.Success(studentInfo)
                    }
                    is ValidationResult.Invalid -> {
                        Log.w(TAG, "Invalid student info: ${validation.errors.joinToString()}")
                        return@withContext DataResult.Error("Некорректные данные студента")
                    }
                }
            } else {
                Log.e(TAG, "Could not parse student name from Eljur")
                return@withContext DataResult.Error("Не удалось получить информацию о студенте")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching student info: ${e.message}", e)
            DataResult.Error("Ошибка загрузки информации: ${e.message}")
        }
    }

    suspend fun fetchSchedule(): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Cannot fetch schedule - authorization failed: ${authResult.message}")
                    return@withContext DataResult.Error(authResult.message)
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, proceeding with schedule fetch")
                }
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext DataResult.Error("Интеграция Eljur не найдена")

            val cookies = mapOf("auth_token" to integration.token)

            val doc = try {
                Jsoup.connect("$baseUrl/journal-schedule-action")
                    .cookies(cookies)
                    .timeout(30000)
                    .get()
            } catch (e: java.net.SocketTimeoutException) {
                return@withContext DataResult.Error("Таймаут при загрузке расписания")
            } catch (e: java.net.UnknownHostException) {
                return@withContext DataResult.Error("Нет подключения к интернету")
            } catch (e: Exception) {
                return@withContext DataResult.Error("Ошибка загрузки страницы: ${e.message}")
            }

            val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val calendar = Calendar.getInstance()
            val weekType = if (calendar.get(Calendar.WEEK_OF_YEAR) % 2 == 0) 1 else 0

            val days = doc.select(".schedule-day, .day-schedule")
            Log.d(TAG, "Found ${days.size} days in schedule")

            if (days.isEmpty()) {
                Log.w(TAG, "No schedule days found - possible parsing issue")
            }

            var processedLessons = 0
            var processedHomeworks = 0

            for (day in days) {
                val dateElement = day.select(".schedule-date, .date").firstOrNull()
                val rawDate = dateElement?.text() ?: currentDate

                val date = try {
                    DateTimeManager().convertEljurDateToDatabase(rawDate)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse date '$rawDate', using current date")
                    currentDate
                }

                val lessons = day.select(".schedule-lesson, .lesson, tr")
                Log.d(TAG, "Processing ${lessons.size} lessons for date $date")

                for (lesson in lessons) {
                    try {
                        val subjectElement = lesson.select(".subject, .lesson-subject").firstOrNull()
                        val subjectName = subjectElement?.text()?.trim() ?: continue

                        if (subjectName.isBlank()) {
                            continue
                        }

                        val timeElement = lesson.select(".time, .lesson-time").firstOrNull()
                        val timeText = timeElement?.text() ?: ""
                        val times = timeText.split("-")
                        val startTime = times.getOrNull(0)?.trim()?.take(5) ?: "" // Ограничиваем до HH:mm
                        val endTime = times.getOrNull(1)?.trim()?.take(5) ?: ""

                        if (startTime.isNotBlank() && !DataValidator.isValidTime(startTime)) {
                            Log.w(TAG, "Invalid start time format: $startTime")
                            continue
                        }

                        val teacherElement = lesson.select(".teacher, .lesson-teacher").firstOrNull()
                        val teacherName = teacherElement?.text()?.trim()

                        val classroomElement = lesson.select(".classroom, .room").firstOrNull()
                        val classroom = classroomElement?.text()?.trim()

                        val homeworkElement = lesson.select(".homework, .hw").firstOrNull()
                        val homeworkText = homeworkElement?.text()?.trim()

                        var teacherId: Int? = null
                        if (!teacherName.isNullOrEmpty()) {
                            val teacher = teacherDao.getTeacherByName(teacherName)
                                ?: TeacherEntity(
                                    teacher_id = teacherName.hashCode().absoluteValue,
                                    name = teacherName
                                ).also {
                                    if (DataValidator.validateTeacher(it) is ValidationResult.Valid) {
                                        teacherDao.insertTeacher(it)
                                        Log.d(TAG, "Created new teacher: $teacherName")
                                    }
                                }
                            teacherId = teacher?.teacher_id
                        }

                        val subject = subjectDao.getSubjectByName(subjectName)
                            ?: SubjectEntity(
                                subject_id = subjectName.hashCode().absoluteValue,
                                name = subjectName,
                                teacher_id = teacherId,
                                integration_id = integration.integration_id
                            ).also {
                                if (DataValidator.validateSubject(it) is ValidationResult.Valid) {
                                    subjectDao.insertSubject(it)
                                    Log.d(TAG, "Created new subject: $subjectName")
                                }
                            }

                        val schedule = ScheduleEntity(
                            schedule_id = UUID.randomUUID().hashCode().absoluteValue,
                            date = date,
                            subject_id = subject.subject_id,
                            start_time = startTime,
                            end_time = endTime,
                            week_type = weekType
                        )

                        when (val validation = DataValidator.validateSchedule(schedule)) {
                            is ValidationResult.Valid -> {
                                scheduleDao.insertSchedule(schedule)
                                processedLessons++
                                Log.d(TAG, "Saved schedule: $subjectName at $startTime-$endTime")
                            }
                            is ValidationResult.Invalid -> {
                                Log.w(TAG, "Invalid schedule skipped: ${validation.errors.joinToString()}")
                                continue
                            }
                        }

                        if (!homeworkText.isNullOrEmpty() && homeworkText.length <= 1000) {
                            val task = TaskEntity(
                                task_id = UUID.randomUUID().hashCode().absoluteValue,
                                subject_id = subject.subject_id,
                                title = "ДЗ: ${subjectName.take(20)}",
                                description = homeworkText,
                                deadline = date,
                                status = "active",
                                integration_id = integration.integration_id
                            )

                            when (val taskValidation = DataValidator.validateTask(task)) {
                                is ValidationResult.Valid -> {
                                    taskDao.insertTask(task)
                                    processedHomeworks++
                                    Log.d(TAG, "Homework saved: $subjectName - ${homeworkText.take(50)}...")
                                }
                                is ValidationResult.Invalid -> {
                                    Log.w(TAG, "Invalid homework skipped: ${taskValidation.errors.joinToString()}")
                                }
                            }
                        }

                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing lesson: ${e.message}", e)
                        continue
                    }
                }
            }

            Log.d(TAG, "Schedule fetch completed: $processedLessons lessons, $processedHomeworks homeworks")

            if (processedLessons == 0) {
                DataResult.Error("Расписание не найдено или пустое")
            } else {
                DataResult.Success(Unit)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in fetchSchedule: ${e.message}", e)
            DataResult.Error("Неожиданная ошибка: ${e.message}")
        }
    }

    suspend fun fetchTasks(): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Cannot fetch tasks - authorization failed: ${authResult.message}")
                    return@withContext DataResult.Error(authResult.message)
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, proceeding with tasks fetch")
                }
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext DataResult.Error("Интеграция Eljur не найдена")

            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/journal-homework-action")
                .cookies(cookies)
                .get()

            val blocks = doc.select(".homework-day, .hw-day")
            Log.d(TAG, "Found ${blocks.size} homework blocks")

            var processedTasks = 0
            var processedFiles = 0

            for (block in blocks) {
                val dateElement = block.select(".homework-date, .date").firstOrNull()
                val date = dateElement?.text() ?: continue

                val items = block.select(".homework-item, .hw-item")
                for (item in items) {
                    val subjectElement = item.select(".homework-subject, .subject").firstOrNull()
                    val subjectName = subjectElement?.text()?.trim() ?: continue

                    val descriptionElement = item.select(".homework-text, .hw-text, .description").firstOrNull()
                    val description = descriptionElement?.text()?.trim() ?: ""

                    val attachments = item.select("a[href]")

                    val subject = subjectDao.getSubjectByName(subjectName)
                        ?: SubjectEntity(
                            subject_id = subjectName.hashCode().absoluteValue,
                            name = subjectName,
                            teacher_id = null,
                            integration_id = integration.integration_id
                        ).also {
                            if (DataValidator.validateSubject(it) is ValidationResult.Valid) {
                                subjectDao.insertSubject(it)
                            }
                        }

                    val task = TaskEntity(
                        task_id = UUID.randomUUID().hashCode().absoluteValue,
                        subject_id = subject.subject_id,
                        title = "Задание: $subjectName",
                        description = description,
                        deadline = date,
                        status = "active",
                        integration_id = integration.integration_id
                    )

                    when (val validation = DataValidator.validateTask(task)) {
                        is ValidationResult.Valid -> {
                            taskDao.insertTask(task)
                            processedTasks++

                            for (a in attachments) {
                                val href = a.attr("href")
                                val name = a.text().trim()
                                if (name.isNotEmpty() && href.isNotEmpty()) {
                                    fileDao.insertFile(
                                        FileEntity(
                                            file_id = UUID.randomUUID().hashCode().absoluteValue,
                                            task_id = task.task_id,
                                            replacement_id = null,
                                            name = name,
                                            url = if (href.startsWith("http")) href else "$baseUrl$href"
                                        )
                                    )
                                    processedFiles++
                                }
                            }
                        }
                        is ValidationResult.Invalid -> {
                            Log.w(TAG, "Invalid task skipped: ${validation.errors.joinToString()}")
                        }
                    }
                }
            }

            Log.d(TAG, "Tasks fetched successfully: $processedTasks tasks, $processedFiles files")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks: ${e.message}", e)
            DataResult.Error("Ошибка загрузки заданий: ${e.message}")
        }
    }

    suspend fun fetchMessages(): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Cannot fetch messages - authorization failed: ${authResult.message}")
                    return@withContext DataResult.Error(authResult.message)
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, proceeding with messages fetch")
                }
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext DataResult.Error("Интеграция Eljur не найдена")

            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/messages")
                .cookies(cookies)
                .get()

            val messages = doc.select(".message, .msg-item, .announcement")
            Log.d(TAG, "Found ${messages.size} messages")

            var processedMessages = 0

            for (m in messages) {
                val titleElement = m.select(".message-title, .title, .msg-title").firstOrNull()
                val title = titleElement?.text()?.trim() ?: ""

                val contentElement = m.select(".message-body, .content, .msg-body").firstOrNull()
                val content = contentElement?.text()?.trim() ?: ""

                val dateElement = m.select(".message-date, .date, .msg-date").firstOrNull()
                val date = dateElement?.text()?.trim() ?: ""

                val senderElement = m.select(".message-sender, .sender, .author").firstOrNull()
                val sender = senderElement?.text()?.trim() ?: ""

                val fullContent = buildString {
                    if (title.isNotEmpty()) append("$title\n")
                    if (content.isNotEmpty()) append(content)
                }

                val entity = MessageEntity(
                    message_id = UUID.randomUUID().hashCode().absoluteValue,
                    integration_id = integration.integration_id,
                    source = "eljur",
                    content = fullContent,
                    received_at = date
                )

                messageDao.insertMessage(entity)
                processedMessages++

                if (containsExamKeywords(fullContent.lowercase(Locale.getDefault()))) {
                    createTaskFromMessage(entity, integration.integration_id)
                }
            }

            Log.d(TAG, "Messages fetched successfully: $processedMessages messages")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: ${e.message}", e)
            DataResult.Error("Ошибка загрузки сообщений: ${e.message}")
        }
    }

    suspend fun fetchReplacements(): DataResult<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Cannot fetch replacements - authorization failed: ${authResult.message}")
                    return@withContext DataResult.Error(authResult.message)
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, proceeding with replacements fetch")
                }
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext DataResult.Error("Интеграция Eljur не найдена")

            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/announcements")
                .cookies(cookies)
                .get()

            val pdfLinks = doc.select("a[href$=.pdf]")
            Log.d(TAG, "Found ${pdfLinks.size} PDF files in announcements")

            var processedFiles = 0

            for (link in pdfLinks) {
                val href = link.attr("href")
                val title = link.text().trim()
                val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"

                if (isReplacementFile(title)) {
                    fileDao.insertFile(
                        FileEntity(
                            file_id = UUID.randomUUID().hashCode().absoluteValue,
                            task_id = null,
                            replacement_id = null,
                            name = title,
                            url = fullUrl
                        )
                    )
                    processedFiles++
                    Log.d(TAG, "Replacement PDF saved: $title")
                }
            }

            Log.d(TAG, "Replacements fetched successfully: $processedFiles files")
            DataResult.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replacements: ${e.message}", e)
            DataResult.Error("Ошибка загрузки замен: ${e.message}")
        }
    }

    private fun containsExamKeywords(text: String): Boolean {
        val keywords = listOf("экзамен", "зачет", "билет", "вопрос", "тест", "контрольная", "пересдача")
        return keywords.any { text.contains(it) }
    }

    private fun containsHomeworkKeywords(text: String): Boolean {
        val keywords = listOf("домаш", "дз", "задание", "упражнение", "лабораторная", "лаб", "проект")
        return keywords.any { text.contains(it) }
    }

    private fun containsReplacementKeywords(text: String): Boolean {
        val keywords = listOf("замена", "отмена", "перенос", "вместо", "изменение", "замен")
        return keywords.any { text.contains(it) }
    }

    private fun isReplacementFile(title: String): Boolean {
        val lowerTitle = title.lowercase(Locale.getDefault())
        return lowerTitle.contains("замен") || lowerTitle.contains("replacement") ||
                lowerTitle.contains("изменен") || lowerTitle.contains("change")
    }

    private suspend fun createTaskFromMessage(message: MessageEntity, integrationId: Int) {
        val task = TaskEntity(
            task_id = UUID.randomUUID().hashCode().absoluteValue,
            subject_id = 0,
            title = "Вопросы для подготовки",
            description = message.content,
            deadline = message.received_at,
            status = "active",
            integration_id = integrationId
        )

        when (val validation = DataValidator.validateTask(task)) {
            is ValidationResult.Valid -> {
                taskDao.insertTask(task)
                Log.d(TAG, "Created task from message: ${task.title}")
            }
            is ValidationResult.Invalid -> {
                Log.w(TAG, "Invalid task from message skipped: ${validation.errors.joinToString()}")
            }
        }
    }

    suspend fun syncAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Eljur synchronization...")

            when (val authResult = authorizeEljur()) {
                is AuthResult.Error -> {
                    Log.e(TAG, "Synchronization failed - authorization error: ${authResult.message}")
                    return@withContext false
                }
                is AuthResult.Success -> {
                    Log.d(TAG, "Authorization successful, starting data sync")
                }
            }

            val localTasksBeforeSync = taskDao.getAllTasks()

            val scheduleResult = fetchSchedule()
            val tasksResult = fetchTasks()
            val messagesResult = fetchMessages()
            val replacementsResult = fetchReplacements()

            val localTasksAfterSync = taskDao.getAllTasks()

            syncWithConflictResolution(localTasksBeforeSync, localTasksAfterSync)

            val hasErrors = listOf(scheduleResult, tasksResult, messagesResult, replacementsResult)
                .any { it is DataResult.Error }

            if (hasErrors) {
                Log.w(TAG, "Some sync operations completed with errors")
                return@withContext false
            }

            Log.d(TAG, "All Eljur data synchronized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during Eljur synchronization: ${e.message}", e)
            false
        }
    }

    suspend fun syncWithConflictResolution(localTasks: List<TaskEntity>, remoteTasks: List<TaskEntity>) {
        conflictResolver.clearConflicts()

        for (localTask in localTasks) {
            val remoteTask = remoteTasks.find { it.task_id == localTask.task_id }
            remoteTask?.let {
                val conflict = conflictResolver.detectTaskConflicts(localTask, it)
                conflict?.let { conflictResolver.addConflict(it) }
            }
        }

        conflictResolver.autoResolveSimpleConflicts()

        val remainingConflictsCount = conflictResolver.getConflictsCount()
        Log.d(TAG, "Remaining conflicts after auto-resolution: $remainingConflictsCount")
    }
}