package com.example.myapplication.ui.telegram

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.repository.TelegramRepository
import com.example.myapplication.databinding.FragmentTelegramAuthBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TelegramAuthFragment : Fragment() {

    private var _binding: FragmentTelegramAuthBinding? = null
    private val binding get() = _binding!!

    // TODO: Provide ViewModel via factory / DI. For simplicity assume you have ViewModelProvider set up.
    private val viewModel: TelegramViewModel by viewModels({ requireActivity() })

    private val API_ID = 33509625
    private val API_HASH = "15f188573a4c526e73560eb271aa8a35"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTelegramAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initTDLib(API_ID, API_HASH)

        binding.phoneNextBtn.setOnClickListener {
            val phone = binding.phoneInput.text.toString().trim()
            if (phone.isBlank()) {
                binding.phoneError.visibility = View.VISIBLE
                binding.phoneError.text = "Введите номер"
                return@setOnClickListener
            }
            binding.phoneError.visibility = View.GONE
            viewModel.sendPhone(phone)
        }

        binding.codeNextBtn.setOnClickListener {
            val code = binding.codeInput.text.toString().trim()
            if (code.isBlank()) {
                binding.codeError.visibility = View.VISIBLE
                binding.codeError.text = "Введите код"
                return@setOnClickListener
            }
            binding.codeError.visibility = View.GONE
            viewModel.sendCode(code)
        }

        binding.passwordNextBtn.setOnClickListener {
            val pass = binding.passwordInput.text.toString()
            if (pass.isBlank()) {
                binding.passwordError.visibility = View.VISIBLE
                binding.passwordError.text = "Введите пароль"
                return@setOnClickListener
            }
            binding.passwordError.visibility = View.GONE
            viewModel.sendPassword(pass)
        }

        lifecycleScope.launch {
            viewModel.authState.collectLatest { state ->
                updateUiForState(state)
                if (state == TelegramRepository.AuthState.READY) {
                    findNavController().navigate(
                        R.id.action_telegramAuthFragment_to_chatSelectionFragment
                    )
                }
            }
        }
    }

    private fun updateUiForState(state: TelegramRepository.AuthState) {
        binding.progress.visibility = View.GONE
        binding.phoneBlock.visibility = View.GONE
        binding.codeBlock.visibility = View.GONE
        binding.passwordBlock.visibility = View.GONE
        binding.genericError.visibility = View.GONE

        when (state) {
            TelegramRepository.AuthState.WAIT_PHONE -> {
                binding.phoneBlock.visibility = View.VISIBLE
            }
            TelegramRepository.AuthState.WAIT_CODE -> {
                binding.codeBlock.visibility = View.VISIBLE
            }
            TelegramRepository.AuthState.WAIT_PASSWORD -> {
                binding.passwordBlock.visibility = View.VISIBLE
            }
            TelegramRepository.AuthState.READY -> {
                Toast.makeText(requireContext(), "Авторизация успешна", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}