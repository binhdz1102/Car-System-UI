package com.android.systemui.feature.systembar.presentation

import com.android.systemui.feature.climate.domain.ClimateState
import com.android.systemui.feature.climate.domain.ClimateUseCases
import com.android.systemui.feature.systembar.domain.SystemBarAction
import com.android.systemui.feature.systembar.domain.SystemBarState
import com.android.systemui.feature.systembar.domain.SystemBarUseCases
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.cancel

data class SystemBarUiState(
    val systemBar: SystemBarState,
    val climate: ClimateState,
)

/** Service-scoped MVVM state holder shared by both system-bar windows. */
@ServiceScoped
class SystemBarViewModel @Inject constructor(
    systemBarUseCases: SystemBarUseCases,
    climateUseCases: ClimateUseCases,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val uiState: StateFlow<SystemBarUiState> = combine(
        systemBarUseCases.observe(),
        climateUseCases.observe(),
    ) { systemBar, climate -> SystemBarUiState(systemBar, climate) }
        .stateIn(scope, SharingStarted.Eagerly, SystemBarUiState(SystemBarState(), ClimateState()))

    private val mutableActions = MutableSharedFlow<SystemBarAction>(extraBufferCapacity = 32)
    val actions = mutableActions.asSharedFlow()

    fun dispatch(action: SystemBarAction) {
        mutableActions.tryEmit(action)
    }

    fun close() {
        scope.cancel()
    }
}
