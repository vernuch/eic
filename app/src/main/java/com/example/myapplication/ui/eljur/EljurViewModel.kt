package com.example.myapplication.ui.eljur

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.EljurRepository
import kotlinx.coroutines.launch

class EljurViewModel(private val repository: EljurRepository) : ViewModel() {

    fun refreshAllData() {
        viewModelScope.launch {
            repository.fetchSchedule()
        }
    }
}
