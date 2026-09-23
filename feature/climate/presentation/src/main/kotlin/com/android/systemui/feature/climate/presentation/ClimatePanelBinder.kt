package com.android.systemui.feature.climate.presentation

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.android.systemui.feature.climate.domain.ClimateState
import com.android.systemui.feature.climate.domain.ClimateUseCases
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/** Binds the XML HVAC panel to the same VHAL-backed use cases as the My-System-App feature. */
@ServiceScoped
class ClimatePanelBinder @Inject constructor(
    private val useCases: ClimateUseCases,
) {
    fun createView(context: Context, onClose: () -> Unit): View =
        LayoutInflater.from(context).inflate(R.layout.climate_panel, null, false).also { view ->
            view.findViewById<ImageButton>(R.id.climate_close).setOnClickListener { onClose() }
            bindTemperatureControl(
                view,
                R.id.driver_temperature_control,
                ClimateState.DRIVER_AREA_ID,
            )
            bindTemperatureControl(
                view,
                R.id.passenger_temperature_control,
                ClimateState.PASSENGER_AREA_ID,
            )
            view.findViewById<Switch>(R.id.climate_ac).setOnCheckedChangeListener { _, checked ->
                Timber.tag(TAG).d("A/C requested=%s", checked)
            }
        }

    fun bind(view: View, scope: CoroutineScope) {
        scope.launch {
            useCases.observe().collectLatest { state ->
                render(view, state)
            }
        }

        view.findViewById<Switch>(R.id.climate_ac).setOnCheckedChangeListener { _, checked ->
            scope.launch { useCases.setAc(checked) }
        }
        view.findViewById<Switch>(R.id.climate_auto).setOnCheckedChangeListener { _, checked ->
            scope.launch { useCases.setAuto(checked) }
        }
        view.findViewById<SeekBar>(R.id.climate_fan).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) view.findViewById<TextView>(R.id.climate_fan_label).text =
                        "Fan $progress"
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    scope.launch { useCases.setFanSpeed(seekBar.progress) }
                }
            },
        )

        bindTemperatureClicks(view, scope, R.id.driver_temperature_control, ClimateState.DRIVER_AREA_ID)
        bindTemperatureClicks(view, scope, R.id.passenger_temperature_control, ClimateState.PASSENGER_AREA_ID)
    }

    private fun bindTemperatureControl(view: View, rootId: Int, areaId: Int) {
        view.findViewById<View>(rootId).findViewById<TextView>(R.id.climate_zone_label).text =
            if (areaId == ClimateState.DRIVER_AREA_ID) "Driver" else "Passenger"
    }

    private fun bindTemperatureClicks(view: View, scope: CoroutineScope, rootId: Int, areaId: Int) {
        val root = view.findViewById<View>(rootId)
        root.findViewById<ImageButton>(R.id.climate_decrease).setOnClickListener {
            scope.launch { useCases.adjustTemperature(areaId, -1) }
        }
        root.findViewById<ImageButton>(R.id.climate_increase).setOnClickListener {
            scope.launch { useCases.adjustTemperature(areaId, 1) }
        }
    }

    private fun render(view: View, state: ClimateState) {
        val driverRoot = view.findViewById<View>(R.id.driver_temperature_control)
        val passengerRoot = view.findViewById<View>(R.id.passenger_temperature_control)
        driverRoot.findViewById<TextView>(R.id.climate_temperature).text =
            state.displayTemperature(state.driver)
        passengerRoot.findViewById<TextView>(R.id.climate_temperature).text =
            state.displayTemperature(state.passenger)
        view.findViewById<Switch>(R.id.climate_ac).apply {
            setOnCheckedChangeListener(null)
            isChecked = state.acEnabled == true
            isEnabled = state.acEnabled != null
        }
        view.findViewById<Switch>(R.id.climate_auto).apply {
            setOnCheckedChangeListener(null)
            isChecked = state.autoEnabled == true
            isEnabled = state.autoEnabled != null
        }
        view.findViewById<SeekBar>(R.id.climate_fan).apply {
            progress = state.fanSpeed ?: 0
            isEnabled = state.fanSpeed != null
        }
        view.findViewById<TextView>(R.id.climate_fan_label).text =
            state.fanSpeed?.let { "Fan $it" } ?: state.lastError.orEmpty()
    }

    private companion object {
        const val TAG = "CarSystemUI.ClimatePanel"
    }
}
