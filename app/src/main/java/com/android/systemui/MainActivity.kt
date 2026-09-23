package com.android.systemui

import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/** XML/navigation entry point used by Android Studio and by manual feature verification. */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // The persistent service is boot-started for the system user. A diagnostic activity can
        // also be launched under the foreground driver user; do not create a second bar service
        // in that per-user process.
        if (Process.myUid() < PER_USER_UID_RANGE) {
            runCatching {
                startService(Intent(this, SystemUIService::class.java))
            }.onFailure {
                // A persistent system package may also be started by system_server; the activity
                // remains useful on a regular debuggable install if the service start is restricted.
                Timber.tag(TAG).w(it, "Unable to start SystemUI service from activity")
            }
        }
    }

    private companion object {
        const val TAG = "CarSystemUI.MainActivity"
        const val PER_USER_UID_RANGE = 100_000
    }
}
