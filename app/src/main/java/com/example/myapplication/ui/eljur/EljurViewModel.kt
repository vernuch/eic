package com.example.myapplication.ui.eljur

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entities.*
import com.example.myapplication.data.repository.EljurRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EljurViewModel(private val repository: EljurRepository) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _studentInfo = MutableStateFlow<StudentInfoEntity?>(null)
    val studentInfo = _studentInfo.asStateFlow()

    private val _schedule = MutableStateFlow<List<ScheduleEntity>>(emptyList())
    val schedule = _schedule.asStateFlow()

    private val _tasks = MutableStateFlow<List<TaskEntity>>(emptyList())
    val tasks = _tasks.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _files = MutableStateFlow<List<FileEntity>>(emptyList())
    val files = _files.asStateFlow()

    private val _replacements = MutableStateFlow<List<ReplacementEntity>>(emptyList())
    val replacements = _replacements.asStateFlow()

    private val _subjects = MutableStateFlow<List<SubjectEntity>>(emptyList())
    val subjects = _subjects.asStateFlow()

    private val _teachers = MutableStateFlow<List<TeacherEntity>>(emptyList())
    val teachers = _teachers.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.IDLE)
    val syncStatus = _syncStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    sealed class SyncStatus {
        object IDLE : SyncStatus()
        object SYNCING : SyncStatus()
        object SUCCESS : SyncStatus()
        data class ERROR(val message: String) : SyncStatus()
    }

    init {
        loadLocalData()
    }

    fun refreshAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = SyncStatus.SYNCING
            _errorMessage.value = null

            try {
                val success = repository.syncAllData()

                if (success) {
                    _syncStatus.value = SyncStatus.SUCCESS
                    loadLocalData()
                } else {
                    _syncStatus.value = SyncStatus.ERROR("Ошибка синхронизации")
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR("Ошибка: ${e.message}")
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSchedule() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchSchedule()
                loadSchedule()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки расписания: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchTasks()
                loadTasks()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заданий: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchMessages()
                loadMessages()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки сообщений: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshReplacements() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.fetchReplacements()
                loadReplacements()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки замен: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadStudentInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = repository.fetchStudentInfo()
                _studentInfo.value = info
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки информации: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadLocalData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _studentInfo.value = repository.getLocalStudentInfo()
                _schedule.value = repository.getLocalSchedule()
                _tasks.value = repository.getLocalTasks()
                _messages.value = repository.getLocalMessages()
                _replacements.value = repository.getLocalReplacements()
                _files.value = repository.getLocalFiles()
                _subjects.value = repository.getSubjects()
                _teachers.value = repository.getTeachers()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки локальных данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSchedule() {
        _schedule.value = repository.getLocalSchedule()
    }

    private suspend fun loadTasks() {
        _tasks.value = repository.getLocalTasks()
    }

    private suspend fun loadMessages() {
        _messages.value = repository.getLocalMessages()
    }

    private suspend fun loadReplacements() {
        _replacements.value = repository.getLocalReplacements()
    }

    fun getTasksForDate(date: String): List<TaskEntity> {
        return tasks.value.filter { it.deadline == date }
    }

    fun getScheduleForDate(date: String): List<ScheduleEntity> {
        return schedule.value.filter { it.date == date }
    }

    fun getActiveTasks(): List<TaskEntity> {
        return tasks.value.filter { it.status == "active" }
    }

    fun getCompletedTasks(): List<TaskEntity> {
        return tasks.value.filter { it.status == "completed" }
    }

    fun updateTaskStatus(taskId: Int, status: String) {
        viewModelScope.launch {
            try {
                repository.updateTaskStatus(taskId, status)
                _tasks.value = repository.getLocalTasks()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка обновления задачи: ${e.message}"
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            try {
                repository.deleteTask(taskId)
                _tasks.value = repository.getLocalTasks()
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка удаления задачи: ${e.message}"
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.clearAllData()
                _schedule.value = emptyList()
                _tasks.value = emptyList()
                _messages.value = emptyList()
                _replacements.value = emptyList()
                _files.value = emptyList()
                _studentInfo.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка очистки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSubjectById(subjectId: Int): SubjectEntity? {
        return subjects.value.find { it.subject_id == subjectId }
    }

    fun getTeacherById(teacherId: Int): TeacherEntity? {
        return teachers.value.find { it.teacher_id == teacherId }
    }

    fun getScheduleWithDetails(date: String): List<ScheduleWithDetails> {
        return getScheduleForDate(date).map { schedule ->
            val subject = getSubjectById(schedule.subject_id)
            val teacher = subject?.teacher_id?.let { getTeacherById(it) }
            ScheduleWithDetails(schedule, subject, teacher)
        }
    }

    data class ScheduleWithDetails(
        val schedule: ScheduleEntity,
        val subject: SubjectEntity?,
        val teacher: TeacherEntity?
    )

    fun clearError() {
        _errorMessage.value = null
        _syncStatus.value = SyncStatus.IDLE
    }

    suspend fun authorize(): Boolean {
        return repository.authorizeEljur()
    }
}
