package com.example.myapplication.ui.schedule

data class ScheduleItem(
    val time: String,
    val subject: String,
    val room: String,
    val isReplacement: Boolean = false,
    val replacementSubject: String = "",
    val replacementRoom: String = ""
)