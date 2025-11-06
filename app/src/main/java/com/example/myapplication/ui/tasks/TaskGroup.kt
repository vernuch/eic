package com.example.myapplication.ui.tasks

import com.example.myapplication.data.entities.TaskEntity


data class TaskGroup(
    val subject: String,
    val tasks: List<TaskEntity>,
    var isExpanded: Boolean = false
)