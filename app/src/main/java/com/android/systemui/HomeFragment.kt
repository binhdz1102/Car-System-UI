package com.android.systemui

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.home_open_climate).setOnClickListener {
            findNavController().navigate(R.id.climateFragment)
        }
        view.findViewById<Button>(R.id.home_open_launcher).setOnClickListener {
            findNavController().navigate(R.id.launcherFragment)
        }
        view.findViewById<Button>(R.id.home_open_task_view).setOnClickListener {
            startActivity(com.android.systemui.feature.launcher.presentation.TaskViewTestActivity.intent(requireContext()))
        }
    }
}
