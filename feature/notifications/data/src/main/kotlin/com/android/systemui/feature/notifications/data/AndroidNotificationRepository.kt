package com.android.systemui.feature.notifications.data

import com.android.systemui.feature.notifications.domain.NotificationEventSink
import com.android.systemui.feature.notifications.domain.NotificationItem
import com.android.systemui.feature.notifications.domain.NotificationListenerHandle
import com.android.systemui.feature.notifications.domain.NotificationRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

@Singleton
class AndroidNotificationRepository @Inject constructor() :
    NotificationRepository,
    NotificationEventSink {
    private val mutableNotifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    private var listener: NotificationListenerHandle? = null

    override val notifications: StateFlow<List<NotificationItem>> = mutableNotifications.asStateFlow()

    override fun onPosted(item: NotificationItem) {
        mutableNotifications.update { current ->
            (current.filterNot { it.key == item.key } + item).sortedByDescending { it.timestamp }
        }
        Timber.tag(TAG).d("Notification posted key=%s package=%s", item.key, item.packageName)
    }

    override fun onRemoved(key: String) {
        mutableNotifications.update { it.filterNot { item -> item.key == key } }
        Timber.tag(TAG).d("Notification removed key=%s", key)
    }

    override fun attach(listener: NotificationListenerHandle) {
        this.listener = listener
        Timber.tag(TAG).i("Notification listener connected")
    }

    override fun detach(listener: NotificationListenerHandle) {
        if (this.listener === listener) this.listener = null
        Timber.tag(TAG).i("Notification listener disconnected")
    }

    override fun dismiss(key: String) {
        listener?.cancelNotification(key)
        onRemoved(key)
    }

    override fun clearAll() {
        listener?.cancelAllNotifications()
        mutableNotifications.value = emptyList()
    }

    private companion object {
        const val TAG = "CarSystemUI.Notifications"
    }
}
