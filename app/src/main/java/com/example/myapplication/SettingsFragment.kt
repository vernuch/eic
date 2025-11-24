package com.example.myapplication.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import com.example.myapplication.R
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.myapplication.databinding.FragmentSettingsBinding
import com.google.android.material.appbar.MaterialToolbar

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        val activity = requireActivity()
        val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
        val topAppBar = activity.findViewById<MaterialToolbar>(R.id.topAppBar)

        bottomNav?.visibility = View.GONE
        topAppBar?.visibility = View.GONE

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEljurAuth.setOnClickListener {
            findNavController().navigate(R.id.navigation_eljur_auth)
        }

        binding.btnTelegramAuth.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_telegramAuth)
        }

        return binding.root
    }



    override fun onDestroyView() {
        super.onDestroyView()
        val activity = requireActivity()
        val bottomNav = activity.findViewById<View>(R.id.bottomNavigationView)
        val topAppBar = activity.findViewById<MaterialToolbar>(R.id.topAppBar)

        bottomNav?.visibility = View.VISIBLE
        topAppBar?.visibility = View.VISIBLE

        _binding = null
    }
}
