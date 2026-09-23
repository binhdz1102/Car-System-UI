package com.android.systemui.feature.climate.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.android.systemui.feature.climate.domain.ClimateState
import com.android.systemui.feature.climate.domain.ClimateUseCases
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** XML destination used by Android Studio's Run action for direct HVAC verification. */
@AndroidEntryPoint
class ClimateFragment : Fragment(R.layout.fragment_climate) {
    @Inject
    lateinit var useCases: ClimateUseCases

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val connection = view.findViewById<TextView>(R.id.climate_connection_state)
        val driver = view.findViewById<TextView>(R.id.climate_driver_temperature)
        val passenger = view.findViewById<TextView>(R.id.climate_passenger_temperature)
        view.findViewById<Button>(R.id.climate_refresh).setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch { useCases.refresh() }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            useCases.observe().collectLatest { state ->
                connection.text = state.lastError ?: if (state.connected) "Connected" else "Connecting…"
                driver.text = "Driver: ${state.displayTemperature(state.driver)}"
                passenger.text = "Passenger: ${state.displayTemperature(state.passenger)}"
            }
        }
    }
}
