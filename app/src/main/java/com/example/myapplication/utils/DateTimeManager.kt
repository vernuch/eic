package com.example.myapplication.utils

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class DateTimeManager {

    companion object {
        private const val TAG = "DateTimeManager"

        private const val ELJUR_DATE_FORMAT = "dd.MM.yyyy"
        private const val DATABASE_DATE_FORMAT = "yyyy-MM-dd"
        private const val DISPLAY_DATE_FORMAT = "dd MMMM yyyy"
        private const val DISPLAY_SHORT_FORMAT = "dd.MM"
        private const val TIME_FORMAT = "HH:mm"
        private const val DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

        private val RUSSIAN_MONTHS = mapOf(
            "января" to 0, "февраля" to 1, "марта" to 2, "апреля" to 3,
            "мая" to 4, "июня" to 5, "июля" to 6, "августа" to 7,
            "сентября" to 8, "октября" to 9, "ноября" to 10, "декабря" to 11
        )

        private val RUSSIAN_DAYS = mapOf(
            "понедельник" to Calendar.MONDAY,
            "вторник" to Calendar.TUESDAY,
            "среду" to Calendar.WEDNESDAY, "среда" to Calendar.WEDNESDAY,
            "четверг" to Calendar.THURSDAY,
            "пятницу" to Calendar.FRIDAY, "пятница" to Calendar.FRIDAY,
            "субботу" to Calendar.SATURDAY, "суббота" to Calendar.SATURDAY,
            "воскресенье" to Calendar.SUNDAY
        )

        private val RELATIVE_DAYS = mapOf(
            "сегодня" to 0, "завтра" to 1, "послезавтра" to 2,
            "вчера" to -1, "позавчера" to -2
        )
    }

    //дата из формата Eljur в стандартный формат бд
    fun convertEljurDateToDatabase(eljurDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat(ELJUR_DATE_FORMAT, Locale.getDefault())
            val outputFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val date = inputFormat.parse(eljurDate)
            outputFormat.format(date)
        } catch (e: ParseException) {
            // если не удалось распарсить, возвращаем как есть
            eljurDate
        }
    }

    //дата из бд в красивый формат для отображения
    fun formatDateForDisplay(databaseDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val outputFormat = SimpleDateFormat(DISPLAY_DATE_FORMAT, Locale("ru"))
            val date = inputFormat.parse(databaseDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            databaseDate
        }
    }

    //форматирует дату в короткий формат
    fun formatDateShort(databaseDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM", Locale("ru"))
            val date = inputFormat.parse(databaseDate)
            outputFormat.format(date)
        } catch (e: Exception) {
            databaseDate
        }
    }

    //парсит относительные даты из текста
    fun parseRelativeDate(text: String): String? {
        val lowerText = text.lowercase(Locale.getDefault())

        //относительные дни (сегодня, завтра итд)
        RELATIVE_DAYS.forEach { (keyword, daysOffset) ->
            if (lowerText.contains(keyword)) {
                return getDateWithOffset(daysOffset)
            }
        }

        //проверяем дни недели
        RUSSIAN_DAYS.forEach { (dayName, dayOfWeek) ->
            if (lowerText.contains(dayName)) {
                return getNextDayOfWeek(dayOfWeek)
            }
        }

        return null
    }

    //дата со смещением от текущей
    fun getDateWithOffset(daysOffset: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, daysOffset)
        return SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault()).format(calendar.time)
    }

    //следующий указанный день недели
    private fun getNextDayOfWeek(targetDayOfWeek: Int): String {
        val calendar = Calendar.getInstance()
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        var daysToAdd = targetDayOfWeek - currentDayOfWeek
        if (daysToAdd <= 0) {
            daysToAdd += 7
        }

        calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
        return SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault()).format(calendar.time)
    }

    //текущая дата в формате бд
    fun getCurrentDate(): String {
        return SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault()).format(Date())
    }

    //завтрашняя дата
    fun getTomorrowDate(): String {
        return getDateWithOffset(1)
    }

    //является ли дата сегодняшней
    fun isToday(date: String): Boolean {
        return date == getCurrentDate()
    }

    //является ли дата завтрашней
    fun isTomorrow(date: String): Boolean {
        return date == getTomorrowDate()
    }

    //прошла ли дата
    fun isDatePassed(date: String): Boolean {
        return try {
            val dateFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val targetDate = dateFormat.parse(date)
            val currentDate = dateFormat.parse(getCurrentDate())
            targetDate.before(currentDate)
        } catch (e: Exception) {
            false
        }
    }

    //разница в днях между двумя датами
    fun getDaysBetween(date1: String, date2: String): Int {
        return try {
            val dateFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val d1 = dateFormat.parse(date1)
            val d2 = dateFormat.parse(date2)
            val diff = abs(d1.time - d2.time)
            (diff / (24 * 60 * 60 * 1000)).toInt()
        } catch (e: Exception) {
            -1
        }
    }

    //даты недели (пн-вс) для указанной даты
    fun getWeekDates(forDate: String = getCurrentDate()): List<String> {
        return try {
            val dateFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(forDate) ?: Date()

            //устанавливаем на понедельник
            calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

            val weekDates = mutableListOf<String>()
            for (i in 0 until 7) {
                weekDates.add(dateFormat.format(calendar.time))
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }

            weekDates
        } catch (e: Exception) {
            emptyList()
        }
    }

    //время для отображения
    fun formatTime(timeString: String): String {
        return try {
            timeString.replace("\\s".toRegex(), "").trim()
        } catch (e: Exception) {
            timeString
        }
    }

    //сравнивает два времени
    fun compareTimes(time1: String, time2: String): Int {
        return try {
            val format = SimpleDateFormat(TIME_FORMAT, Locale.getDefault())
            val t1 = format.parse(time1)
            val t2 = format.parse(time2)
            t1.compareTo(t2)
        } catch (e: Exception) {
            0
        }
    }

    //текущее время в формате HH:mm
    fun getCurrentTime(): String {
        return SimpleDateFormat(TIME_FORMAT, Locale.getDefault()).format(Date())
    }

    //тип недели
    fun getWeekTypeForDate(date: String): Int {
        return try {
            val dateFormat = SimpleDateFormat(DATABASE_DATE_FORMAT, Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = dateFormat.parse(date) ?: Date()

            val weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR)
            if (weekOfYear % 2 == 0) 1 else 0
        } catch (e: Exception) {
            0
        }
    }

    //текущий тип недели
    fun getCurrentWeekType(): Int {
        return getWeekTypeForDate(getCurrentDate())
    }
}