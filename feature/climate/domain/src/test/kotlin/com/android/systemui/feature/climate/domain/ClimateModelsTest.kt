package com.android.systemui.feature.climate.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ClimateModelsTest {
    @Test
    fun fahrenheitDisplayUsesWholeDegreeForCarSystemBar() {
        val state = ClimateState(unit = ClimateUnit.FAHRENHEIT)
        val zone = ClimateZoneState(ClimateState.DRIVER_AREA_ID, temperatureCelsius = 16.6667f)

        assertEquals("62°", state.displayTemperature(zone))
    }
}
