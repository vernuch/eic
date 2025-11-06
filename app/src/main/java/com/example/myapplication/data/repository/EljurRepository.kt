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
    private val fileDao: FileDao
) {
    private val baseUrl = "https://kmpo.eljur.ru"

    suspend fun authorizeEljur(): Boolean = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" }
                ?: return@withContext false

            val response = Jsoup.connect("$baseUrl/ajaxauthorize")
                .data("login", integration.login)
                .data("password", integration.password_enc)
                .data("remember", "1")
                .method(org.jsoup.Connection.Method.POST)
                .execute()

            val token = response.cookies()["auth_token"]
            if (token != null) {
                integrationDao.insertIntegration(integration.copy(token = token))
                Log.d("EljurRepository", "Authorization success, token saved")
                true
            } else {
                Log.e("EljurRepository", "No token found")
                false
            }
        } catch (e: Exception) {
            Log.e("EljurRepository", "Authorization failed", e)
            false
        }
    }

    suspend fun fetchSchedule() = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/journal-schedule-action")
                .cookies(cookies)
                .get()

            val weekType = if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2 == 0) 1 else 0
            val rows = doc.select("table.schedule tr")

            for (row in rows) {
                val subjectName = row.select("td.subject").text()
                val startTime = row.select("td.start").text()
                val endTime = row.select("td.end").text()

                if (subjectName.isNotEmpty()) {
                    val subject = subjectDao.getSubjectByName(subjectName)
                        ?: SubjectEntity(
                            subject_id = subjectName.hashCode(),
                            name = subjectName,
                            teacher_id = null,
                            integration_id = integration.integration_id
                        ).also { subjectDao.insertSubject(it) }

                    val schedule = ScheduleEntity(
                        date = SimpleDateFormat("yyyy-MM-dd").format(Date()),
                        subject_id = subject.subject_id,
                        start_time = startTime,
                        end_time = endTime,
                        is_replacement = false,
                        replacement_note = null,
                        week_type = weekType
                    )
                    scheduleDao.insertSchedule(schedule)
                }
            }
            Log.d("EljurRepository", "Schedule fetched successfully")
        } catch (e: Exception) {
            Log.e("EljurRepository", "Error fetching schedule", e)
        }
    }

    suspend fun fetchTasks() = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/journal-homework-action")
                .cookies(cookies)
                .get()

            val blocks = doc.select(".homework-day")
            for (block in blocks) {
                val date = block.select(".homework-date").text()
                val items = block.select(".homework-item")

                for (item in items) {
                    val subjectName = item.select(".homework-subject").text()
                    val description = item.select(".homework-text").text()
                    val attachments = item.select("a[href]")

                    val subject = subjectDao.getSubjectByName(subjectName)
                        ?: SubjectEntity(subjectName.hashCode(), subjectName, null, integration.integration_id)
                            .also { subjectDao.insertSubject(it) }

                    val task = TaskEntity(
                        task_id = UUID.randomUUID().hashCode(),
                        subject_id = subject.subject_id,
                        title = description.take(30),
                        description = description,
                        deadline = date,
                        status = "active",
                        integration_id = integration.integration_id
                    )
                    taskDao.insertTask(task)

                    for (a in attachments) {
                        val href = a.attr("href")
                        val name = a.text()
                        fileDao.insertFile(
                            FileEntity(
                                file_id = UUID.randomUUID().hashCode(),
                                task_id = task.task_id,
                                replacement_id = null,
                                name = name,
                                url = "$baseUrl$href"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("EljurRepository", "Error fetching tasks", e)
        }
    }

    suspend fun fetchMessages() = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/messages")
                .cookies(cookies)
                .get()

            val messages = doc.select(".message")
            for (m in messages) {
                val title = m.select(".message-title").text()
                val content = m.select(".message-body").text()
                val date = m.select(".message-date").text()

                val entity = MessageEntity(
                    message_id = UUID.randomUUID().hashCode(),
                    integration_id = integration.integration_id,
                    source = "eljur",
                    content = "$title\n$content",
                    received_at = date
                )
                messageDao.insertMessage(entity)
            }
        } catch (e: Exception) {
            Log.e("EljurRepository", "Error fetching messages", e)
        }
    }

    suspend fun fetchReplacements() = withContext(Dispatchers.IO) {
        try {
            val integration = integrationDao.getAllIntegrations().find { it.service == "eljur" } ?: return@withContext
            val cookies = mapOf("auth_token" to integration.token)

            val doc = Jsoup.connect("$baseUrl/announcements")
                .cookies(cookies)
                .get()

            val pdfLinks = doc.select("a[href$=.pdf]")
            for (link in pdfLinks) {
                val href = link.attr("href")
                val title = link.text()
                fileDao.insertFile(
                    FileEntity(
                        file_id = UUID.randomUUID().hashCode(),
                        task_id = null,
                        replacement_id = null,
                        name = title,
                        url = "$baseUrl$href"
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("EljurRepository", "Error fetching replacements", e)
        }
    }
}
