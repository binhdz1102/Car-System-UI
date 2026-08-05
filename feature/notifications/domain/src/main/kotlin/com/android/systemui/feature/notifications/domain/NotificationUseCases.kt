package com.android.systemui.feature.notifications.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

class NotificationUseCases @Inject constructor(
    private val repository: NotificationRepository,
) {
    fun observe(): StateFlow<List<NotificationItem>> = repository.notifications

    fun dismiss(key: String) = repository.dismiss(key)

    fun clearAll() = repository.clearAll()
}
