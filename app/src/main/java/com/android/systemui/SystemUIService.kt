package com.android.systemui

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Package-compatible service entry point used by system_server for this
 * standalone development replacement. The full AOSP SystemUI service graph is
 * Soong-built and is deliberately not reproduced by this small Gradle project.
 */
class SystemUIService : Service() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Custom Gradle SystemUI service started")
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CustomSystemUI"
    }
}
