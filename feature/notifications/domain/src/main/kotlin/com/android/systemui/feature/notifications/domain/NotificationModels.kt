package com.android.systemui.feature.notifications.domain

import kotlinx.coroutines.flow.StateFlow

data class NotificationItem(
    val key: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val ongoing: Boolean,
)

interface NotificationRepository {
    val notifications: StateFlow<List<NotificationItem>>

    fun dismiss(key: String)

    fun clearAll()
}

/** Event sink kept separate so the platform listener does not depend on presentation code. */
interface NotificationEventSink {
    fun onPosted(item: NotificationItem)

    fun onRemoved(key: String)

    fun attach(listener: NotificationListenerHandle)

    fun detach(listener: NotificationListenerHandle)
}

interface NotificationListenerHandle {
    fun cancelNotification(key: String)

    fun cancelAllNotifications()
}
