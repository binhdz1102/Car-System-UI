package com.android.systemui.feature.launcher.presentation

import android.app.Activity
import android.app.ActivityManager
import android.car.Car
import android.car.app.CarActivityManager
import android.car.app.CarTaskViewController
import android.car.app.CarTaskViewControllerCallback
import android.car.app.CarTaskViewControllerHostLifecycle
import android.car.app.ControlledRemoteCarTaskView
import android.car.app.ControlledRemoteCarTaskViewCallback
import android.car.app.ControlledRemoteCarTaskViewConfig
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import java.util.concurrent.Executor
import timber.log.Timber

/**
 * Small XML-only verification host for the same ControlledRemoteCarTaskView contract used by
 * My-System-App. It deliberately lives in launcher:presentation so the platform dependency stays
 * at the Android boundary and never leaks into launcher:domain.
 */
class TaskViewTestActivity : Activity() {
    private var host: AndroidTaskViewHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_view_test)
        val status = findViewById<TextView>(R.id.task_view_status)
        val container = findViewById<FrameLayout>(R.id.task_view_container)
        host = AndroidTaskViewHost(
            activity = this,
            onStatus = { message -> runOnUiThread { status.text = message } },
        ).also { embeddedHost ->
            container.addView(embeddedHost.view)
            embeddedHost.load(DEFAULT_TARGET)
        }
    }

    override fun onDestroy() {
        host?.release()
        host = null
        super.onDestroy()
    }

    companion object {
        private val DEFAULT_TARGET = EmbeddedTarget(
            componentName = "com.android.car.settings/.Settings_Launcher_Homepage",
            label = "Car Settings",
        )

        fun intent(context: Context): Intent = Intent(context, TaskViewTestActivity::class.java)
    }
}

private data class EmbeddedTarget(
    val componentName: String,
    val label: String,
)

private class AndroidTaskViewHost(
    private val activity: Activity,
    private val onStatus: (String) -> Unit,
) {
    val view = FrameLayout(activity).apply { setBackgroundColor(Color.BLACK) }
    private val windowContext = activity.createWindowContext(
        WindowManager.LayoutParams.TYPE_APPLICATION_STARTING,
        null,
    )
    private val executor: Executor = windowContext.mainExecutor
    private val hostLifecycle = CarTaskViewControllerHostLifecycle()
    private var car: Car? = null
    private var controller: CarTaskViewController? = null
    private var taskView: ControlledRemoteCarTaskView? = null
    private var target: EmbeddedTarget? = null
    private var released = false

    init {
        runCatching {
            enableTrustedOverlay()
            car = Car.createCar(windowContext)
            val manager = car?.getCarManager(CarActivityManager::class.java)
                ?: error("Car activity service is unavailable")
            manager.getCarTaskViewController(windowContext, hostLifecycle, executor, controllerCallback)
            onStatus("CarTaskViewController connected; waiting for TaskView…")
        }.onFailure { error ->
            onStatus("TaskView unavailable: ${error.message ?: error.javaClass.simpleName}")
            Timber.tag(TAG).e(error, "Unable to initialise TaskView host")
        }
    }

    fun load(newTarget: EmbeddedTarget) {
        target = newTarget
        val current = taskView
        if (current == null) {
            createTaskViewIfReady()
            return
        }
        runCatching {
            current.replaceActivityIntent(newTarget.toIntent())
            onStatus("Replacing embedded task with ${newTarget.label}…")
        }.onFailure { error ->
            onStatus("TaskView replace failed: ${error.message ?: error.javaClass.simpleName}")
            Timber.tag(TAG).e(error, "Unable to replace embedded task")
        }
    }

    fun release() {
        if (released) return
        released = true
        taskView?.release()
        controller?.release()
        hostLifecycle.hostDestroyed()
        car?.disconnect()
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
    }

    private val controllerCallback = object : CarTaskViewControllerCallback {
        override fun onConnected(connectedController: CarTaskViewController) {
            if (released) {
                connectedController.release()
                return
            }
            controller = connectedController
            onStatus("TaskView controller connected; creating embedded surface…")
            createTaskViewIfReady()
        }

        override fun onDisconnected(disconnectedController: CarTaskViewController) {
            controller = null
            if (!released) onStatus("TaskView controller disconnected")
        }
    }

    private fun createTaskViewIfReady() {
        val connectedController = controller ?: return
        val requestedTarget = target ?: return
        if (taskView != null || released) return
        runCatching {
            connectedController.createControlledRemoteCarTaskView(
                ControlledRemoteCarTaskViewConfig.Builder()
                    .setActivityIntent(requestedTarget.toIntent())
                    .setShouldAutoRestartOnTaskRemoval(false)
                    .build(),
                executor,
                taskCallback,
            )
        }.onFailure { error ->
            onStatus("TaskView creation failed: ${error.message ?: error.javaClass.simpleName}")
            Timber.tag(TAG).e(error, "Unable to create ControlledRemoteCarTaskView")
        }
    }

    private val taskCallback = object : ControlledRemoteCarTaskViewCallback {
        override fun onTaskViewCreated(created: ControlledRemoteCarTaskView) {
            taskView = created
            view.addView(
                created,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            view.post {
                created.updateWindowBounds()
                created.startActivity()
            }
            onStatus("TaskView surface created; launching ${target?.label ?: "target"}…")
        }

        override fun onTaskAppeared(taskInfo: ActivityManager.RunningTaskInfo) {
            taskView?.updateWindowBounds()
            val component = taskInfo.topActivity?.flattenToShortString()
                ?: taskInfo.baseIntent.component?.flattenToShortString()
                ?: "unknown"
            onStatus("TaskView displayed: $component")
            Timber.tag(TAG).i("Embedded task appeared: %s", component)
        }

        override fun onTaskVanished(taskInfo: ActivityManager.RunningTaskInfo) {
            onStatus("TaskView task vanished")
            Timber.tag(TAG).w("Embedded task vanished: %s", taskInfo)
        }

        override fun onTaskViewReleased() {
            taskView = null
            if (!released) onStatus("TaskView surface released")
        }
    }

    private fun EmbeddedTarget.toIntent(): Intent {
        val component = ComponentName.unflattenFromString(componentName)
            ?: error("Invalid target component: $componentName")
        return Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    private fun enableTrustedOverlay() {
        runCatching {
            Window::class.java.getDeclaredMethod("addPrivateFlags", Int::class.javaPrimitiveType)
                .apply { isAccessible = true }
                .invoke(activity.window, PRIVATE_FLAG_TRUSTED_OVERLAY)
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
        }.onFailure { error ->
            Timber.tag(TAG).w(error, "Trusted overlay flag is unavailable")
            onStatus("TaskView input may be unavailable: trusted overlay was not granted")
        }
    }

    private companion object {
        const val PRIVATE_FLAG_TRUSTED_OVERLAY = 0x20000000
        const val TAG = "CarSystemUI.TaskView"
    }
}
