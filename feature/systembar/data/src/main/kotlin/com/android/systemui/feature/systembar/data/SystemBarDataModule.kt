package com.android.systemui.feature.systembar.data

import com.android.systemui.feature.systembar.domain.SystemBarRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SystemBarDataModule {
    @Binds
    @Singleton
    abstract fun bindSystemBarRepository(implementation: AndroidSystemBarRepository): SystemBarRepository
}
