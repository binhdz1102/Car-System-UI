package com.android.systemui.feature.systembar.presentation

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import com.android.systemui.core.platform.SystemWindowFactory
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import timber.log.Timber

/** Owns the three privileged windows that make up the Automotive SystemUI surface. */
@ServiceScoped
class SystemBarWindowController @Inject constructor(
    private val windowFactory: SystemWindowFactory,
) {
    private data class AttachedWindow(
        val manager: WindowManager,
        val view: View,
    )

    private var top: AttachedWindow? = null
    private var bottom: AttachedWindow? = null
    private var panel: AttachedWindow? = null

    fun attachTop(view: View) {
        top = attach(view, TYPE_STATUS_BAR_ADDITIONAL, Gravity.TOP, barHeight(view, "status_bar_height", 48))
    }

    fun attachBottom(view: View) {
        bottom = attach(view, TYPE_NAVIGATION_BAR_PANEL, Gravity.BOTTOM, barHeight(view, "navigation_bar_height", 64))
    }

    fun showPanel(view: View, xDp: Int = 0) {
        dismissPanel()
        val manager = windowFactory.windowManagerFor(TYPE_SYSTEM_DIALOG)
        val context = windowFactory.contextFor(TYPE_SYSTEM_DIALOG)
        val width = context.resources.getDimensionPixelSize(R.dimen.systembar_panel_width)
        val height = context.resources.getDimensionPixelSize(R.dimen.systembar_panel_height)
        val params = WindowManager.LayoutParams(
            width,
            height,
            TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_DIM_BEHIND or
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_LAYOUT_ATTACHED_IN_DECOR,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (xDp * context.resources.displayMetrics.density).toInt()
            y = topBarHeight(context)
            dimAmount = 0.45f
            title = "Car-System-UI panel"
            packageName = context.packageName
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        view.isFocusableInTouchMode = true
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismissPanel()
                true
            } else {
                false
            }
        }
        runCatching {
            manager.addView(view, params)
            view.requestFocus()
            panel = AttachedWindow(manager, view)
            Timber.tag(TAG).d("Panel attached xDp=%s", xDp)
        }.onFailure { throwable ->
            Timber.tag(TAG).e(throwable, "Unable to attach system dialog panel")
        }
    }

    fun dismissPanel() {
        panel?.let { attached ->
            runCatching { attached.manager.removeViewImmediate(attached.view) }
                .onFailure { Timber.tag(TAG).w(it, "Unable to remove panel") }
        }
        panel = null
    }

    fun release() {
        dismissPanel()
        remove(top)
        remove(bottom)
        top = null
        bottom = null
    }

    private fun attach(view: View, type: Int, gravity: Int, height: Int): AttachedWindow? {
        val manager = windowFactory.windowManagerFor(type)
        val context = windowFactory.contextFor(type)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            height,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_SPLIT_TOUCH or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            this.gravity = gravity
            title = if (type == TYPE_STATUS_BAR_ADDITIONAL) "Car-System-UI top bar" else "Car-System-UI bottom bar"
            packageName = context.packageName
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        return runCatching {
            manager.addView(view, params)
            Timber.tag(TAG).i("Attached window type=%s height=%s", type, height)
            AttachedWindow(manager, view)
        }.onFailure { throwable ->
            Timber.tag(TAG).e(throwable, "Unable to attach SystemUI window type=%s", type)
        }.getOrNull()
    }

    private fun remove(attached: AttachedWindow?) {
        attached ?: return
        runCatching { attached.manager.removeViewImmediate(attached.view) }
            .onFailure { Timber.tag(TAG).w(it, "Unable to remove SystemUI window") }
    }

    private fun barHeight(view: View, resourceName: String, fallbackDp: Int): Int {
        val resources = view.resources
        val resourceId = resources.getIdentifier(resourceName, "dimen", "android")
        return if (resourceId != 0) resources.getDimensionPixelSize(resourceId)
        else (fallbackDp * resources.displayMetrics.density).toInt()
    }

    private fun topBarHeight(context: android.content.Context): Int {
        val resources = context.resources
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId != 0) resources.getDimensionPixelSize(resourceId)
        else (48 * resources.displayMetrics.density).toInt()
    }

    companion object {
        // WindowManager.LayoutParams keeps these values hidden on the public SDK. They are the
        // same window types used by AOSP CarSystemUI's status/navigation bar pods.
        const val TYPE_NAVIGATION_BAR_PANEL = 2024
        const val TYPE_SYSTEM_DIALOG = 2008
        const val TYPE_STATUS_BAR_ADDITIONAL = 2041
        private const val TAG = "CarSystemUI.Windows"
    }
}
