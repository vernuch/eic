package com.example.myapplication.ui.schedule

import org.threeten.bp.LocalDate

data class WeekDayItem(
    val date: LocalDate,
    val isSelected: Boolean = false,
    val isToday: Boolean = false
)