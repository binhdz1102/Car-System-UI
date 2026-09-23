package com.android.systemui.feature.climate.domain

enum class ClimateUnit {
    CELSIUS,
    FAHRENHEIT,
}

data class ClimateZoneState(
    val areaId: Int,
    val temperatureCelsius: Float? = null,
    val available: Boolean = false,
    val writable: Boolean = false,
)

data class ClimateState(
    val connected: Boolean = false,
    val unit: ClimateUnit = ClimateUnit.CELSIUS,
    val driver: ClimateZoneState = ClimateZoneState(areaId = DRIVER_AREA_ID),
    val passenger: ClimateZoneState = ClimateZoneState(areaId = PASSENGER_AREA_ID),
    val fanSpeed: Int? = null,
    val acEnabled: Boolean? = null,
    val autoEnabled: Boolean? = null,
    val lastError: String? = null,
) {
    fun displayTemperature(zone: ClimateZoneState): String =
        zone.temperatureCelsius?.let { value ->
            when (unit) {
                ClimateUnit.CELSIUS -> "%.1f°C".format(value)
                ClimateUnit.FAHRENHEIT -> "%.0f°".format(value * 9f / 5f + 32f)
            }
        } ?: "--"

    companion object {
        const val DRIVER_AREA_ID = 49
        const val PASSENGER_AREA_ID = 68
    }
}

interface ClimateRepository {
    val state: kotlinx.coroutines.flow.StateFlow<ClimateState>

    suspend fun refresh()

    suspend fun adjustTemperature(areaId: Int, displayStep: Int): com.android.systemui.core.common.ActionResult

    suspend fun setAc(enabled: Boolean): com.android.systemui.core.common.ActionResult

    suspend fun setAuto(enabled: Boolean): com.android.systemui.core.common.ActionResult

    suspend fun setFanSpeed(speed: Int): com.android.systemui.core.common.ActionResult
}
