package com.android.systemui.feature.notifications.data

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/** Optional privileged listener used when the AAOS image grants this package notification access. */
@AndroidEntryPoint
class NotificationsListenerService : NotificationListenerService() {
    @Inject
    lateinit var eventSink: com.android.systemui.feature.notifications.domain.NotificationEventSink

    private val handle = object : com.android.systemui.feature.notifications.domain.NotificationListenerHandle {
        override fun cancelNotification(key: String) {
            runCatching { this@NotificationsListenerService.cancelNotification(key) }
                .onFailure { Timber.tag(TAG).w(it, "Unable to dismiss notification") }
        }

        override fun cancelAllNotifications() {
            runCatching { this@NotificationsListenerService.cancelAllNotifications() }
                .onFailure { Timber.tag(TAG).w(it, "Unable to clear notifications") }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        eventSink.attach(handle)
        activeNotifications.orEmpty().forEach(::publish)
        Timber.tag(TAG).i("Notification listener connected with %s active entries", activeNotifications?.size ?: 0)
    }

    override fun onListenerDisconnected() {
        eventSink.detach(handle)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = publish(sbn)

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        eventSink.onRemoved(sbn.key)
    }

    private fun publish(sbn: StatusBarNotification) {
        val notification = sbn.notification
        val extras = notification.extras
        val appLabel = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(sbn.packageName, 0)).toString()
        }.getOrDefault(sbn.packageName)
        eventSink.onPosted(
            com.android.systemui.feature.notifications.domain.NotificationItem(
                key = sbn.key,
                packageName = sbn.packageName,
                appLabel = appLabel,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
                timestamp = sbn.postTime,
                ongoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            ),
        )
    }

    private companion object {
        const val TAG = "CarSystemUI.NotificationListener"
    }
}
