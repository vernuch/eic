package com.example.myapplication.ui.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entities.TelegramMessageEntity
import com.example.myapplication.data.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TelegramViewModel(private val repository: TelegramRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<TelegramMessageEntity>>(emptyList())
    val messages = _messages.asStateFlow()

    val authState = repository.authState

    fun sendPhone(phone: String) = repository.sendPhoneNumber(phone)
    fun sendCode(code: String) = repository.sendAuthCode(code)
    fun sendPassword(password: String) = repository.sendPassword(password)

    fun refreshMessages() {
        viewModelScope.launch {
            _messages.value = repository.getAllMessages()
        }
    }

    fun loadChatMessages(chatId: Long) {
        viewModelScope.launch {
            _messages.value = repository.getMessagesForChat(chatId)
        }
    }

    fun fetchChats(limit: Int = 50) {
        repository.fetchChats(limit)
    }

    fun fetchChatHistory(chatId: Long, limit: Int = 50) {
        repository.fetchChatHistory(chatId, limit)
    }
}

