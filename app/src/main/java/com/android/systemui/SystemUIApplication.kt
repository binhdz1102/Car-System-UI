package com.android.systemui

import android.app.Application
import com.android.systemui.logging.AppTimberTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/** Minimal Application entry point for the Gradle development replacement. */
@HiltAndroidApp
class SystemUIApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(
            AppTimberTree(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                gitCommitHash = BuildConfig.GIT_COMMIT_HASH,
            ),
        )
    }
}
