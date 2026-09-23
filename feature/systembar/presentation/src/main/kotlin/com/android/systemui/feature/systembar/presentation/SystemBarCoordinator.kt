package com.android.systemui.feature.systembar.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.UserHandle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import com.android.systemui.core.common.ActionResult
import com.android.systemui.core.platform.SystemWindowFactory
import com.android.systemui.feature.climate.domain.ClimateState
import com.android.systemui.feature.climate.domain.ClimateUseCases
import com.android.systemui.feature.notifications.presentation.NotificationPanelBinder
import com.android.systemui.feature.climate.presentation.ClimatePanelBinder
import com.android.systemui.feature.systembar.domain.QuickControl
import com.android.systemui.feature.systembar.domain.SystemBarAction
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import timber.log.Timber

/** Connects the MVVM state to the XML top/bottom bars and routes user actions to platform APIs. */
@ServiceScoped
class SystemBarCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val windowFactory: SystemWindowFactory,
    private val windowController: SystemBarWindowController,
    private val viewModel: SystemBarViewModel,
    private val climateUseCases: ClimateUseCases,
    private val climatePanelBinder: ClimatePanelBinder,
    private val notificationPanelBinder: NotificationPanelBinder,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var panelScope: CoroutineScope? = null
    private var started = false
    private var topView: View? = null
    private var bottomView: View? = null

    fun start() {
        if (started) return
        started = true
        runCatching {
            val topContext = windowFactory.contextFor(SystemBarWindowController.TYPE_STATUS_BAR_ADDITIONAL)
            val bottomContext = windowFactory.contextFor(SystemBarWindowController.TYPE_NAVIGATION_BAR_PANEL)
            topView = LayoutInflater.from(topContext).inflate(R.layout.system_bar_top, null, false)
            bottomView = LayoutInflater.from(bottomContext).inflate(R.layout.system_bar_bottom, null, false)
            bindTop(topView!!)
            bindBottom(bottomView!!)
            windowController.attachTop(topView!!)
            windowController.attachBottom(bottomView!!)
            scope.launch { viewModel.uiState.collectLatest(::render) }
            scope.launch { viewModel.actions.collectLatest(::handleAction) }
            Timber.tag(TAG).i("Car-System-UI bars started")
        }.onFailure { throwable ->
            Timber.tag(TAG).e(throwable, "Unable to start Car-System-UI bars")
        }
    }

    fun stop() {
        if (!started) return
        started = false
        closePanel()
        scope.cancel()
        viewModel.close()
        windowController.release()
        topView = null
        bottomView = null
        Timber.tag(TAG).i("Car-System-UI bars stopped")
    }

    private fun bindTop(view: View) {
        view.findViewById<View>(R.id.quick_bluetooth).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenQuickControl(QuickControl.BLUETOOTH))
        }
        view.findViewById<View>(R.id.quick_connectivity).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenQuickControl(QuickControl.CONNECTIVITY))
        }
        view.findViewById<View>(R.id.quick_display).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenQuickControl(QuickControl.DISPLAY))
        }
        view.findViewById<View>(R.id.quick_volume).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenQuickControl(QuickControl.VOLUME))
        }
        view.findViewById<View>(R.id.system_bar_notifications).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenNotifications)
        }
        view.findViewById<View>(R.id.system_bar_user).setOnClickListener {
            viewModel.dispatch(SystemBarAction.OpenUserPicker)
        }
    }

    private fun bindBottom(view: View) {
        view.findViewById<View>(R.id.bottom_home).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchHome)
        }
        view.findViewById<View>(R.id.bottom_app_grid).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchAppGrid)
        }
        view.findViewById<View>(R.id.bottom_my_system_app).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchMySystemApp)
        }
        view.findViewById<View>(R.id.bottom_settings).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchSettings)
        }
        view.findViewById<View>(R.id.bottom_phone).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchPhone)
        }
        view.findViewById<View>(R.id.bottom_assistant).setOnClickListener {
            viewModel.dispatch(SystemBarAction.LaunchAssistant)
        }
        bindClimateGroup(view, R.id.bottom_driver_climate, ClimateState.DRIVER_AREA_ID)
        bindClimateGroup(view, R.id.bottom_passenger_climate, ClimateState.PASSENGER_AREA_ID)
    }

    private fun bindClimateGroup(view: View, rootId: Int, areaId: Int) {
        val root = view.findViewById<View>(rootId)
        root.setOnClickListener { viewModel.dispatch(SystemBarAction.OpenClimate) }
        root.findViewById<View>(R.id.climate_decrease).setOnClickListener {
            Timber.tag(TAG).d("Driver/passenger climate decrease clicked area=%s", areaId)
            viewModel.dispatch(SystemBarAction.AdjustTemperature(areaId, -1))
        }
        root.findViewById<View>(R.id.climate_increase).setOnClickListener {
            Timber.tag(TAG).d("Driver/passenger climate increase clicked area=%s", areaId)
            viewModel.dispatch(SystemBarAction.AdjustTemperature(areaId, 1))
        }
    }

    private fun render(state: SystemBarUiState) {
        topView?.let { top ->
            top.findViewById<TextView>(R.id.system_bar_clock).text = state.systemBar.time
            top.findViewById<TextView>(R.id.system_bar_user_name).text = state.systemBar.userName
            top.findViewById<ImageButton>(R.id.quick_bluetooth).isSelected = state.systemBar.bluetoothEnabled
            top.findViewById<ImageButton>(R.id.quick_connectivity).isSelected = state.systemBar.wifiConnected
            top.findViewById<ImageButton>(R.id.system_bar_location).isSelected = state.systemBar.locationEnabled
            top.findViewById<TextView>(R.id.system_bar_notification_count).apply {
                text = state.systemBar.notificationCount.toString()
                visibility = if (state.systemBar.notificationCount > 0) View.VISIBLE else View.GONE
            }
        }
        bottomView?.let { bottom ->
            bottom.findViewById<View>(R.id.bottom_driver_climate)
                .findViewById<TextView>(R.id.climate_temperature).text =
                state.climate.displayTemperature(state.climate.driver)
            bottom.findViewById<View>(R.id.bottom_passenger_climate)
                .findViewById<TextView>(R.id.climate_temperature).text =
                state.climate.displayTemperature(state.climate.passenger)
            bottom.findViewById<View>(R.id.bottom_driver_climate).isEnabled =
                state.climate.driver.available || !state.climate.connected
            bottom.findViewById<View>(R.id.bottom_passenger_climate).isEnabled =
                state.climate.passenger.available || !state.climate.connected
        }
    }

    private suspend fun handleAction(action: SystemBarAction) {
        when (action) {
            is SystemBarAction.OpenQuickControl -> showQuickControl(action.control)
            SystemBarAction.OpenNotifications -> showNotifications()
            SystemBarAction.OpenClimate -> showClimate()
            SystemBarAction.OpenUserPicker -> showUserPicker()
            SystemBarAction.LaunchHome -> launchHome()
            SystemBarAction.LaunchAppGrid -> launchAppGrid()
            SystemBarAction.LaunchMySystemApp -> launchComponent("com.android.mysystemapp/.MainActivity")
            SystemBarAction.LaunchSettings -> launchIntent(Intent(Settings.ACTION_SETTINGS))
            SystemBarAction.LaunchPhone -> launchComponent("com.android.car.dialer/.ui.TelecomActivity")
            SystemBarAction.LaunchAssistant -> launchIntent(Intent("android.intent.action.VOICE_ASSIST"))
            is SystemBarAction.AdjustTemperature -> {
                when (val result = climateUseCases.adjustTemperature(action.areaId, action.step)) {
                    ActionResult.Success -> Timber.tag(TAG).d("Temperature adjusted area=%s", action.areaId)
                    is ActionResult.Failure -> Timber.tag(TAG).w("Temperature update failed: %s", result.message)
                }
            }
            is SystemBarAction.AdjustVolume -> adjustVolume(action.step)
        }
    }

    private fun showQuickControl(control: QuickControl) {
        closePanel()
        val panelContext = windowFactory.contextFor(SystemBarWindowController.TYPE_SYSTEM_DIALOG)
        val view = LayoutInflater.from(panelContext).inflate(R.layout.quick_control_panel, null, false)
        val title = view.findViewById<TextView>(R.id.quick_control_title)
        val status = view.findViewById<TextView>(R.id.quick_control_status)
        val toggle = view.findViewById<Switch>(R.id.quick_control_switch)
        val seekBar = view.findViewById<SeekBar>(R.id.quick_control_seekbar)
        val settings = view.findViewById<View>(R.id.quick_control_settings)
        view.findViewById<View>(R.id.quick_control_close).setOnClickListener { closePanel() }
        when (control) {
            QuickControl.BLUETOOTH -> {
                title.text = "Bluetooth"
                val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
                status.text = if (adapter?.isEnabled == true) "Bluetooth is on" else "Bluetooth is off"
                toggle.isChecked = adapter?.isEnabled == true
                toggle.setOnCheckedChangeListener { _, enabled ->
                    runCatching { if (enabled) adapter?.enable() else adapter?.disable() }
                        .onFailure { Timber.tag(TAG).w(it, "Bluetooth toggle failed") }
                }
                settings.setOnClickListener { launchIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }
            }
            QuickControl.CONNECTIVITY -> {
                title.text = "Connectivity"
                status.text = "Wi-Fi and mobile network controls"
                toggle.visibility = View.GONE
                settings.setOnClickListener { launchIntent(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }
            }
            QuickControl.DISPLAY -> {
                title.text = "Display"
                status.text = "Brightness and display settings"
                toggle.visibility = View.GONE
                settings.setOnClickListener { launchIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS)) }
            }
            QuickControl.VOLUME -> {
                title.text = "Volume"
                toggle.visibility = View.GONE
                seekBar.visibility = View.VISIBLE
                val max = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100
                seekBar.max = max
                seekBar.progress = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
                status.text = "Media volume ${seekBar.progress}/$max"
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                            status.text = "Media volume $progress/$max"
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
                })
                settings.setOnClickListener { launchIntent(Intent(Settings.ACTION_SOUND_SETTINGS)) }
            }
        }
        val x = if (control == QuickControl.BLUETOOTH || control == QuickControl.CONNECTIVITY) 58 else 158
        windowController.showPanel(view, x)
    }

    private fun showClimate() {
        closePanel()
        val panelContext = windowFactory.contextFor(SystemBarWindowController.TYPE_SYSTEM_DIALOG)
        val view = climatePanelBinder.createView(panelContext) { closePanel() }
        panelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        climatePanelBinder.bind(view, panelScope!!)
        windowController.showPanel(view, centeredPanelX())
    }

    private fun showNotifications() {
        closePanel()
        val panelContext = windowFactory.contextFor(SystemBarWindowController.TYPE_SYSTEM_DIALOG)
        val view = notificationPanelBinder.createView(panelContext) { closePanel() }
        panelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        notificationPanelBinder.bind(view, panelScope!!)
        windowController.showPanel(view, centeredPanelX())
    }

    private fun showUserPicker() {
        closePanel()
        val panelContext = windowFactory.contextFor(SystemBarWindowController.TYPE_SYSTEM_DIALOG)
        val view = LayoutInflater.from(panelContext).inflate(R.layout.user_panel, null, false)
        view.findViewById<View>(R.id.user_panel_close).setOnClickListener { closePanel() }
        view.findViewById<View>(R.id.user_panel_settings).setOnClickListener {
            launchIntent(Intent("android.settings.USER_SETTINGS"))
        }
        windowController.showPanel(view, centeredPanelX())
    }

    private fun closePanel() {
        panelScope?.cancel()
        panelScope = null
        windowController.dismissPanel()
    }

    private fun adjustVolume(step: Int) {
        val direction = if (step >= 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER
        audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
    }

    private fun launchHome() {
        launchIntent(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
    }

    private fun launchAppGrid() {
        launchIntent(
            Intent("com.android.car.carlauncher.ACTION_APP_GRID")
                .setComponent(
                    ComponentName(
                        "com.android.car.carlauncher",
                        "com.android.car.carlauncher.feature.launcher.presentation.AppGridActivity",
                    ),
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
        )
    }

    private fun launchComponent(flattenedComponent: String) {
        val component = ComponentName.unflattenFromString(flattenedComponent) ?: return
        launchIntent(Intent(Intent.ACTION_MAIN).setComponent(component))
    }

    private fun launchIntent(intent: Intent) {
        runCatching {
            val userId = currentUserId()
            val userHandle = userHandleOf(userId)
            startActivityAsUser(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), userHandle)
            Timber.tag(TAG).d(
                "Launched intent=%s for user=%s",
                intent.component ?: intent.action,
                userId,
            )
        }
            .onFailure { Timber.tag(TAG).w(it, "Unable to launch %s", intent.component ?: intent.action) }
    }

    /** Starts user-facing activities in the foreground driver user, although SystemUI runs as u0. */
    private fun startActivityAsUser(intent: Intent, userHandle: UserHandle) {
        val method = Context::class.java.getDeclaredMethod(
            "startActivityAsUser",
            Intent::class.java,
            UserHandle::class.java,
        ).apply { isAccessible = true }
        method.invoke(context, intent, userHandle)
    }

    private fun userHandleOf(userId: Int): UserHandle =
        UserHandle::class.java.getDeclaredMethod(
            "of",
            Int::class.javaPrimitiveType,
        ).apply { isAccessible = true }
            .invoke(null, userId) as UserHandle

    private fun currentUserId(): Int = runCatching {
        val method = Class.forName("android.app.ActivityManager")
            .getDeclaredMethod("getCurrentUser")
        method.isAccessible = true
        method.invoke(null) as Int
    }.getOrElse { throwable ->
        Timber.tag(TAG).w(throwable, "Unable to resolve foreground user; using system user")
        0
    }

    private fun centeredPanelX(): Int {
        val metrics = context.resources.displayMetrics
        val width = context.resources.getDimensionPixelSize(R.dimen.systembar_panel_width)
        return ((metrics.widthPixels - width) / metrics.density / 2f).toInt().coerceAtLeast(0)
    }

    private companion object {
        const val TAG = "CarSystemUI.Coordinator"
    }
}
