package com.example.myapplication.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.database.AppDatabase
import com.example.myapplication.data.entities.IntegrationEntity
import com.example.myapplication.databinding.FragmentEljurAuthBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EljurAuthFragment : Fragment() {

    private var _binding: FragmentEljurAuthBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEljurAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.post {
            requireActivity().findViewById<View>(R.id.bottomNavigationView)?.isVisible = false
            requireActivity().findViewById<View>(R.id.topAppBar)?.isVisible = false
        }

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.saveButton.setOnClickListener {
            val login = binding.loginInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (login.isNotEmpty() && password.isNotEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(requireContext())
                    val integrationDao = db.integrationDao()
                    val integration = IntegrationEntity(
                        integration_id = 1,
                        service = "eljur",
                        login = login,
                        password_enc = password,
                        token = ""
                    )
                    integrationDao.insertIntegration(integration)
                }
                findNavController().popBackStack()
            } else {
                binding.errorText.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}



