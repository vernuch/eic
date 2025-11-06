package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentMessagesBinding
import com.example.myapplication.ui.messages.MessageAdapter
import com.example.myapplication.ui.messages.MessageItem

class MessagesFragment : Fragment() {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!

    private val adapter by lazy { MessageAdapter() }

    private var showingType = "telegram"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        binding.buttonTelegram.setOnClickListener {
            showingType = "telegram"
            loadMessages()
        }
        binding.buttonEljur.setOnClickListener {
            showingType = "eljur"
            loadMessages()
        }
        binding.buttonNotes.setOnClickListener {
            showingType = "notes"
            loadMessages()
        }

        loadMessages()
    }

    private fun loadMessages() {
        // заглушка
        val messages = when (showingType) {
            "telegram" -> listOf(
                MessageItem("Telegram", "Сообщение из ТГ 1"),
                MessageItem("Telegram", "Сообщение из ТГ 2")
            )
            "eljur" -> listOf(
                MessageItem("Eljur", "Сообщение из Элжура 1"),
                MessageItem("Eljur", "Сообщение из Элжура 2")
            )
            "notes" -> listOf(
                MessageItem("Заметка", "Моя заметка 1"),
                MessageItem("Заметка", "Моя заметка 2")
            )
            else -> emptyList()
        }

        adapter.submitList(messages)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
