package com.example.myapplication.ui.messages

data class MessageItem(
    val type: String,
    val content: String,
    var isExpanded: Boolean = false
)
