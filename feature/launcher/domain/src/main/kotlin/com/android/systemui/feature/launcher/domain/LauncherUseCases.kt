package com.android.systemui.feature.launcher.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class LauncherUseCases @Inject constructor(
    private val repository: LauncherRepository,
) {
    fun observeApps(): StateFlow<List<LaunchableApp>> = repository.apps

    suspend fun refresh() = repository.refresh()

    fun launch(app: LaunchableApp): Result<Unit> = repository.launch(app)
}
