package com.android.systemui.feature.notifications.data

import com.android.systemui.feature.notifications.domain.NotificationEventSink
import com.android.systemui.feature.notifications.domain.NotificationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NotificationsDataModule {
    @Binds
    @Singleton
    abstract fun bindNotificationRepository(
        implementation: AndroidNotificationRepository,
    ): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindNotificationEventSink(
        implementation: AndroidNotificationRepository,
    ): NotificationEventSink
}
