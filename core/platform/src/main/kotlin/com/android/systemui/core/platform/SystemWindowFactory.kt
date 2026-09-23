package com.android.systemui.core.platform

import android.content.Context
import android.view.LayoutInflater
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Boundary for windows owned by the privileged SystemUI process.
 *
 * A window context is required on modern Android for non-activity windows. Keeping that detail in
 * core:platform lets presentation modules remain testable and keeps WindowManager setup out of
 * domain code.
 */
@Singleton
class SystemWindowFactory @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {
    fun contextFor(type: Int): Context = applicationContext.createWindowContext(type, null)

    fun windowManagerFor(type: Int): WindowManager =
        contextFor(type).getSystemService(WindowManager::class.java)
            ?: error("WindowManager is unavailable for type=$type")

    fun inflaterFor(type: Int): LayoutInflater = LayoutInflater.from(contextFor(type))
}
