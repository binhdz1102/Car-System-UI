package com.android.systemui.feature.launcher.data

import com.android.systemui.feature.launcher.domain.LauncherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LauncherDataModule {
    @Binds
    @Singleton
    abstract fun bindLauncherRepository(
        repository: AndroidLauncherRepository,
    ): LauncherRepository
}
