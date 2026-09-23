package com.android.systemui.feature.systembar.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class SystemBarUseCases @Inject constructor(
    private val repository: SystemBarRepository,
) {
    fun observe(): StateFlow<SystemBarState> = repository.state
}
