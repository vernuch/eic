package com.example.myapplication.data.repository

import android.util.Log
import com.example.myapplication.data.dao.*
import com.example.myapplication.data.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class EljurRepository(
    private val integrationDao: IntegrationDao,
    private val scheduleDao: ScheduleDao,
    private val subjectDao: SubjectDao,
    private val teacherDao: TeacherDao,
    private val taskDao: TaskDao,
    private val messageDao: MessageDao,
    private val fileDao: FileDao,
    private val replacementDao: ReplacementDao,
    private val studentInfoDao: StudentInfoDao
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

    // === МЕТОДЫ ДЛЯ ПОЛУЧЕНИЯ ЛОКАЛЬНЫХ ДАННЫХ ===

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

    // === ФИЛЬТРУЮЩИЕ МЕТОДЫ ===

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

    // === МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ ДАННЫХ ===

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

    // === СУЩЕСТВУЮЩИЕ МЕТОДЫ (остаются без изменений) ===

    suspend fun authorizeEljur(): Boolean = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext false.also {
                    Log.e(TAG, "No Eljur integration found")
                }

            Log.d(TAG, "Attempting authorization for user: ${integration.login}")

            val response = Jsoup.connect("$baseUrl/ajaxauthorize")
                .data("username", integration.login)
                .data("password", integration.password_enc)
                .data("remember", "1")
                .header("X-Requested-With", "XMLHttpRequest")
                .method(org.jsoup.Connection.Method.POST)
                .execute()

            val token = response.cookies()["auth_token"]
            if (token != null) {
                integrationDao.insertIntegration(integration.copy(token = token))
                Log.d(TAG, "Authorization success, token saved")
                true
            } else {
                Log.e(TAG, "No auth token received")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Authorization failed: ${e.message}", e)
            false
        }
    }

    suspend fun fetchStudentInfo(): StudentInfoEntity? = withContext(Dispatchers.IO) {
        try {
            if (!authorizeEljur()) {
                Log.e(TAG, "Cannot fetch student info - authorization failed")
                return@withContext null
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext null
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/")
                .cookies(cookies)
                .get()

            val userName = doc.select(".user-name, .profile-name, [class*='name']").firstOrNull()?.text()
            val groupInfo = doc.select(".group-info, .class-info, [class*='group']").firstOrNull()?.text()

            if (userName != null) {
                val studentInfo = StudentInfoEntity(
                    student_id = integration.integration_id ?: 0,
                    full_name = userName.trim(),
                    group_name = groupInfo?.trim() ?: "Неизвестно",
                    integration_id = integration.integration_id ?: 0,
                    last_updated = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
                )
                studentInfoDao.insertStudentInfo(studentInfo)
                Log.d(TAG, "Student info fetched: $userName, Group: $groupInfo")
                return@withContext studentInfo
            } else {
                Log.e(TAG, "Could not parse student name from Eljur")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching student info: ${e.message}", e)
            null
        }
    }

    suspend fun fetchSchedule() = withContext(Dispatchers.IO) {
        try {
            if (!authorizeEljur()) {
                Log.e(TAG, "Cannot fetch schedule - authorization failed")
                return@withContext
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/journal-schedule-action")
                .cookies(cookies)
                .get()

            val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val calendar = Calendar.getInstance()
            val weekType = if (calendar.get(Calendar.WEEK_OF_YEAR) % 2 == 0) 1 else 0

            val days = doc.select(".schedule-day, .day-schedule")
            Log.d(TAG, "Found ${days.size} days in schedule")

            for (day in days) {
                val dateElement = day.select(".schedule-date, .date").firstOrNull()
                val date = dateElement?.text() ?: currentDate

                val lessons = day.select(".schedule-lesson, .lesson, tr")
                for (lesson in lessons) {
                    val subjectElement = lesson.select(".subject, .lesson-subject").firstOrNull()
                    val subjectName = subjectElement?.text()?.trim() ?: continue

                    val timeElement = lesson.select(".time, .lesson-time").firstOrNull()
                    val timeText = timeElement?.text() ?: ""
                    val times = timeText.split("-")
                    val startTime = times.getOrNull(0)?.trim() ?: ""
                    val endTime = times.getOrNull(1)?.trim() ?: ""

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
                                teacher_id = teacherName.hashCode(),
                                name = teacherName
                            ).also { teacherDao.insertTeacher(it) }
                        teacherId = teacher.teacher_id
                    }

                    val subject = subjectDao.getSubjectByName(subjectName)
                        ?: SubjectEntity(
                            subject_id = subjectName.hashCode(),
                            name = subjectName,
                            teacher_id = teacherId,
                            integration_id = integration.integration_id
                        ).also { subjectDao.insertSubject(it) }

                    val schedule = ScheduleEntity(
                        schedule_id = UUID.randomUUID().hashCode(),
                        date = date,
                        subject_id = subject.subject_id,
                        start_time = startTime,
                        end_time = endTime,
                        week_type = weekType
                    )
                    scheduleDao.insertSchedule(schedule)

                    if (!homeworkText.isNullOrEmpty()) {
                        val task = TaskEntity(
                            task_id = UUID.randomUUID().hashCode(),
                            subject_id = subject.subject_id,
                            title = "ДЗ: ${subjectName.take(20)}",
                            description = homeworkText,
                            deadline = date,
                            status = "active",
                            integration_id = integration.integration_id
                        )
                        taskDao.insertTask(task)
                        Log.d(TAG, "Homework found: $subjectName - $homeworkText")
                    }
                }
            }
            Log.d(TAG, "Schedule fetched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching schedule: ${e.message}", e)
        }
    }

    suspend fun fetchTasks() = withContext(Dispatchers.IO) {
        try {
            if (!authorizeEljur()) {
                Log.e(TAG, "Cannot fetch tasks - authorization failed")
                return@withContext
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/journal-homework-action")
                .cookies(cookies)
                .get()

            val blocks = doc.select(".homework-day, .hw-day")
            Log.d(TAG, "Found ${blocks.size} homework blocks")

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
                            subject_id = subjectName.hashCode(),
                            name = subjectName,
                            teacher_id = null,
                            integration_id = integration.integration_id
                        ).also { subjectDao.insertSubject(it) }

                    val task = TaskEntity(
                        task_id = UUID.randomUUID().hashCode(),
                        subject_id = subject.subject_id,
                        title = "Задание: $subjectName",
                        description = description,
                        deadline = date,
                        status = "active",
                        integration_id = integration.integration_id
                    )
                    taskDao.insertTask(task)

                    for (a in attachments) {
                        val href = a.attr("href")
                        val name = a.text().trim()
                        if (name.isNotEmpty() && href.isNotEmpty()) {
                            fileDao.insertFile(
                                FileEntity(
                                    file_id = UUID.randomUUID().hashCode(),
                                    task_id = task.task_id,
                                    replacement_id = null,
                                    name = name,
                                    url = if (href.startsWith("http")) href else "$baseUrl$href"
                                )
                            )
                        }
                    }
                }
            }
            Log.d(TAG, "Tasks fetched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching tasks: ${e.message}", e)
        }
    }

    suspend fun fetchMessages() = withContext(Dispatchers.IO) {
        try {
            if (!authorizeEljur()) {
                Log.e(TAG, "Cannot fetch messages - authorization failed")
                return@withContext
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/messages")
                .cookies(cookies)
                .get()

            val messages = doc.select(".message, .msg-item, .announcement")
            Log.d(TAG, "Found ${messages.size} messages")

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
                    message_id = UUID.randomUUID().hashCode(),
                    integration_id = integration.integration_id,
                    source = "eljur",
                    content = fullContent,
                    received_at = date
                )
                messageDao.insertMessage(entity)

                if (containsExamKeywords(fullContent.lowercase(Locale.getDefault()))) {
                    createTaskFromMessage(entity, integration.integration_id ?: 0)
                }
            }
            Log.d(TAG, "Messages fetched successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching messages: ${e.message}", e)
        }
    }

    suspend fun fetchReplacements() = withContext(Dispatchers.IO) {
        try {
            if (!authorizeEljur()) {
                Log.e(TAG, "Cannot fetch replacements - authorization failed")
                return@withContext
            }

            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/announcements")
                .cookies(cookies)
                .get()

            val pdfLinks = doc.select("a[href$=.pdf]")
            Log.d(TAG, "Found ${pdfLinks.size} PDF files in announcements")

            for (link in pdfLinks) {
                val href = link.attr("href")
                val title = link.text().trim()
                val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"

                if (isReplacementFile(title)) {
                    fileDao.insertFile(
                        FileEntity(
                            file_id = UUID.randomUUID().hashCode(),
                            task_id = null,
                            replacement_id = null,
                            name = title,
                            url = fullUrl
                        )
                    )
                    Log.d(TAG, "Replacement PDF saved: $title")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching replacements: ${e.message}", e)
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
            task_id = UUID.randomUUID().hashCode(),
            subject_id = 0,
            title = "Вопросы для подготовки",
            description = message.content,
            deadline = message.received_at,
            status = "active",
            integration_id = integrationId
        )
        taskDao.insertTask(task)
    }

    suspend fun syncAllData(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting Eljur synchronization...")

            if (!authorizeEljur()) {
                Log.e(TAG, "Synchronization failed - authorization error")
                return@withContext false
            }

            fetchStudentInfo()
            fetchSchedule()
            fetchTasks()
            fetchMessages()
            fetchReplacements()

            Log.d(TAG, "All Eljur data synchronized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error during Eljur synchronization: ${e.message}", e)
            false
        }
    }
}