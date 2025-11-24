package com.example.myapplication.ui.telegram

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentChatSelectionBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatSelectionFragment : Fragment() {

    private var _binding: FragmentChatSelectionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TelegramViewModel by viewModels({ requireActivity() })

    private val adapter by lazy {
        ChatSelectionAdapter { selected ->
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChatSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.chatsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.chatsRecycler.adapter = adapter

        binding.saveBtn.setOnClickListener {
            val sel = adapter.getSelected()
            viewModel.setSelectedChats(sel)
            // optionally sync immediately
            viewModel.syncSelectedChats()
            Toast.makeText(requireContext(), "Сохранено ${sel.size} чатов", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressed() // or navigate up
        }

        viewModel.fetchChats(200)
        lifecycleScope.launch {
            viewModel.chats.collectLatest { list ->
                adapter.setItems(list, viewModel.getSelectedChats())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
