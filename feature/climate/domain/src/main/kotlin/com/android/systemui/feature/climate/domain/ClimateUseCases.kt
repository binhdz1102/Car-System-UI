package com.android.systemui.feature.climate.domain

import com.android.systemui.core.common.ActionResult
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class ClimateUseCases @Inject constructor(
    private val repository: ClimateRepository,
) {
    fun observe(): StateFlow<ClimateState> = repository.state

    suspend fun refresh() = repository.refresh()

    suspend fun adjustTemperature(areaId: Int, displayStep: Int): ActionResult =
        repository.adjustTemperature(areaId, displayStep)

    suspend fun setAc(enabled: Boolean): ActionResult = repository.setAc(enabled)

    suspend fun setAuto(enabled: Boolean): ActionResult = repository.setAuto(enabled)

    suspend fun setFanSpeed(speed: Int): ActionResult = repository.setFanSpeed(speed)
}
