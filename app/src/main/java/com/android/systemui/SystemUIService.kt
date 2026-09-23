package com.android.systemui

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.android.systemui.feature.systembar.presentation.SystemBarCoordinator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import timber.log.Timber

/**
 * Package-compatible service entry point used by system_server for the standalone replacement.
 * The coordinator owns the same top/bottom/panel window contract as the AOSP CarSystemUI pods.
 */
@AndroidEntryPoint
class SystemUIService : Service() {
    @Inject
    lateinit var coordinator: SystemBarCoordinator

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).i("Car-System-UI service created")
        coordinator.start()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        Timber.tag(TAG).d("Car-System-UI service start id=%s", startId)
        return START_STICKY
    }

    override fun onDestroy() {
        coordinator.stop()
        Timber.tag(TAG).i("Car-System-UI service destroyed")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CarSystemUI.Service"
    }
}
