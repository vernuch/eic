package com.example.myapplication.data.validation

import com.example.myapplication.data.entities.*
import java.text.SimpleDateFormat
import java.util.*

object DataValidator {

    fun validateTask(task: TaskEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (task.title.isBlank()) {
            errors.add("Название задания не может быть пустым")
        } else if (task.title.length > 200) {
            errors.add("Название задания слишком длинное")
        }

        if (task.description.length > 1000) {
            errors.add("Описание задания слишком длинное")
        }

        if (task.deadline.isNotBlank() && !isValidDate(task.deadline)) {
            errors.add("Неверный формат даты дедлайна")
        }

        if (task.status !in listOf("active", "completed", "archived")) {
            errors.add("Неверный статус задания")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    fun validateSchedule(schedule: ScheduleEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (!isValidDate(schedule.date)) {
            errors.add("Неверный формат даты расписания")
        }

        if (!isValidTime(schedule.start_time) || !isValidTime(schedule.end_time)) {
            errors.add("Неверный формат времени")
        }

        if (schedule.week_type !in 0..2) {
            errors.add("Неверный тип недели")
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun validateStudentInfo(info: StudentInfoEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (info.full_name.isBlank()) {
            errors.add("ФИО не может быть пустым")
        }

        if (info.group_name.isBlank()) {
            errors.add("Название группы не может быть пустым")
        }

        if (!isValidDateTime(info.last_updated)) {
            errors.add("Неверный формат времени обновления")
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun validateTeacher(teacher: TeacherEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (teacher.name.isBlank()) {
            errors.add("Имя преподавателя не может быть пустым")
        } else if (teacher.name.length > 100) {
            errors.add("Имя преподавателя слишком длинное")
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun validateSubject(subject: SubjectEntity): ValidationResult {
        val errors = mutableListOf<String>()

        if (subject.name.isBlank()) {
            errors.add("Название предмета не может быть пустым")
        } else if (subject.name.length > 200) {
            errors.add("Название предмета слишком длинное")
        }

        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    fun isValidTime(time: String): Boolean {
        return try {
            if (time.isBlank()) return true // Пустое время допустимо
            SimpleDateFormat("HH:mm", Locale.getDefault()).parse(time)
            // Дополнительная проверка формата
            time.matches(Regex("""^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"""))
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidDate(date: String): Boolean {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidDateTime(dateTime: String): Boolean {
        return try {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(dateTime)
            true
        } catch (e: Exception) {
            false
        }
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}