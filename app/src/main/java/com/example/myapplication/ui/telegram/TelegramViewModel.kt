package com.example.myapplication.ui.telegram

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.entities.TelegramMessageEntity
import com.example.myapplication.data.repository.TelegramRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import androidx.lifecycle.ViewModelProvider

class TelegramViewModel(private val repository: TelegramRepository) : ViewModel() {

    private val _messages = MutableStateFlow<List<TelegramMessageEntity>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _chats = MutableStateFlow<List<TdApi.Chat>>(emptyList())
    val chats = _chats.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _messagesByType = MutableStateFlow<List<TelegramMessageEntity>>(emptyList())
    val messagesByType = _messagesByType.asStateFlow()

    val authState = repository.authState

    fun initTDLib(apiId: Int, apiHash: String) {
        repository.initTDLib(apiId, apiHash)
    }

    fun sendPhone(phone: String) = repository.sendPhoneNumber(phone)
    fun sendCode(code: String) = repository.sendAuthCode(code)
    fun sendPassword(password: String) = repository.sendPassword(password)

    fun refreshMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _messages.value = repository.getAllSavedMessages()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadChatMessages(chatId: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _messages.value = repository.getSavedMessagesForChat(chatId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchChats(limit: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val chatsList = repository.getAllChats(limit)
                _chats.value = chatsList
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchChatHistory(chatId: Long, limit: Int = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.getChatHistory(chatId, limit)
                _messages.value = repository.getSavedMessagesForChat(chatId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getSelectedChats(): Set<Long> {
        return repository.getSelectedChats()
    }

    fun setSelectedChats(chatIds: Set<Long>) {
        repository.setSelectedChats(chatIds)
    }

    fun syncSelectedChats() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.syncSelectedChatsHistory()
                refreshMessages()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadMessagesByType(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _messagesByType.value = repository.getMessagesByType(type)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getCachedMessagesByType(type: String): List<TelegramMessageEntity> {
        return messages.value.filter { it.message_type == type }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }

    class TelegramViewModelFactory(
        private val repository: TelegramRepository
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TelegramViewModel::class.java)) {
                return TelegramViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
