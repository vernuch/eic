package com.example.myapplication.data.repository

import com.example.myapplication.data.dao.ReplacementDao
import com.example.myapplication.data.dao.ScheduleDao
import com.example.myapplication.data.dao.SubjectDao
import com.example.myapplication.data.entities.ScheduleEntity
import com.example.myapplication.data.entities.ReplacementEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ScheduleRepository(
    private val scheduleDao: ScheduleDao,
    private val replacementDao: ReplacementDao,
    private val subjectDao: SubjectDao
) {

    suspend fun getScheduleForGroup(group: String, date: String): List<ScheduleEntity> =
        withContext(Dispatchers.IO) {
            val mainSchedule = scheduleDao.getSchedulesByDate(date)
            val replacements = replacementDao.getAllReplacements()

            val scheduleWithReplacements = mainSchedule.map { schedule ->
                val replacement: ReplacementEntity? = replacements.find { replacement ->
                    replacement.schedule_id == schedule.schedule_id
                }

                if (replacement != null) {
                    schedule.copy(
                        is_replacement = true,
                        replacement_note = replacement.note ?: "Замена"
                    )
                } else {
                    schedule
                }
            }

            scheduleWithReplacements.sortedBy { it.start_time }
        }

    suspend fun getScheduleForWeek(group: String, startDate: String, weekType: Int? = null): Map<String, List<ScheduleEntity>> {
        return withContext(Dispatchers.IO) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(startDate) ?: Date()

            val weekSchedule = mutableMapOf<String, List<ScheduleEntity>>()

            for (i in 0 until 7) {
                val currentDate = calendar.time
                val dateString = dateFormat.format(currentDate)

                val currentWeekType = weekType ?: getWeekTypeForDate(dateString)

                val daySchedule = getScheduleForDateWithWeekType(group, dateString, currentWeekType)
                weekSchedule[dateString] = daySchedule

                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            weekSchedule
        }
    }

    suspend fun getScheduleForDateWithWeekType(group: String, date: String, weekType: Int): List<ScheduleEntity> =
        withContext(Dispatchers.IO) {
            val allSchedules = scheduleDao.getSchedulesByDate(date)

            val filteredSchedules = allSchedules.filter { schedule ->
                schedule.week_type == weekType || schedule.week_type == 2
            }

            val replacements = replacementDao.getAllReplacements()

            val scheduleWithReplacements = filteredSchedules.map { schedule ->
                val replacement: ReplacementEntity? = replacements.find { replacement ->
                    replacement.schedule_id == schedule.schedule_id
                }

                if (replacement != null) {
                    schedule.copy(
                        is_replacement = true,
                        replacement_note = replacement.note ?: "Замена"
                    )
                } else {
                    schedule
                }
            }

            scheduleWithReplacements.sortedBy { it.start_time }
        }

    private fun getWeekTypeForDate(date: String): Int {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(date) ?: Date()

            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weekOfYear % 2 == 0) 1 else 0
        } catch (e: Exception) {
            0
        }
    }

    fun getCurrentWeekType(): Int {
        return getWeekTypeForDate(getCurrentDate())
    }

    fun getNextWeekType(): Int {
        val currentType = getCurrentWeekType()
        return if (currentType == 0) 1 else 0
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    suspend fun getReplacementsForDate(date: String): List<ReplacementEntity> =
        withContext(Dispatchers.IO) {
            val schedulesOnDate = scheduleDao.getSchedulesByDate(date)
            val allReplacements = replacementDao.getAllReplacements()

            allReplacements.filter { replacement ->
                schedulesOnDate.any { schedule ->
                    schedule.schedule_id == replacement.schedule_id
                }
            }
        }

    suspend fun getScheduleWithReplacements(date: String): List<ScheduleEntity> =
        withContext(Dispatchers.IO) {
            val weekType = getWeekTypeForDate(date)
            getScheduleForDateWithWeekType("", date, weekType)
        }

    suspend fun getTodaySchedule(group: String): List<ScheduleEntity> {
        val today = getCurrentDate()
        val weekType = getWeekTypeForDate(today)
        return getScheduleForDateWithWeekType(group, today, weekType)
    }

    suspend fun getTomorrowSchedule(group: String): List<ScheduleEntity> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrow = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val weekType = getWeekTypeForDate(tomorrow)
        return getScheduleForDateWithWeekType(group, tomorrow, weekType)
    }

    suspend fun getCurrentWeekSchedule(group: String): Map<String, List<ScheduleEntity>> {
        val today = getCurrentDate()
        val currentWeekType = getCurrentWeekType()
        return getScheduleForWeek(group, today, currentWeekType)
    }

    suspend fun getNextWeekSchedule(group: String): Map<String, List<ScheduleEntity>> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val nextWeekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        val nextWeekType = getNextWeekType()
        return getScheduleForWeek(group, nextWeekStart, nextWeekType)
    }

    suspend fun getTwoWeeksSchedule(group: String): Map<String, List<ScheduleEntity>> {
        val today = getCurrentDate()
        val currentWeekType = getCurrentWeekType()

        val twoWeeksSchedule = mutableMapOf<String, List<ScheduleEntity>>()

        twoWeeksSchedule.putAll(getScheduleForWeek(group, today, currentWeekType))

        val nextWeekSchedule = getNextWeekSchedule(group)
        twoWeeksSchedule.putAll(nextWeekSchedule)

        return twoWeeksSchedule
    }

    suspend fun hasDifferentSchedulesForWeeks(date: String): Boolean {
        return withContext(Dispatchers.IO) {
            val schedules = scheduleDao.getSchedulesByDate(date)
            val weekTypes = schedules.map { it.week_type }.distinct()
            weekTypes.size > 1 && weekTypes.any { it != 2 }
        }
    }
}