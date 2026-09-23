package com.android.systemui.feature.climate.data

import com.android.systemui.feature.climate.domain.ClimateRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ClimateDataModule {
    @Binds
    @Singleton
    abstract fun bindClimateRepository(implementation: AndroidClimateRepository): ClimateRepository
}
