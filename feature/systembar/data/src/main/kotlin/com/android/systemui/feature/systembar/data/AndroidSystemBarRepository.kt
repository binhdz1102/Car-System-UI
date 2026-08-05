package com.android.systemui.feature.systembar.data

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.UserManager
import com.android.systemui.core.common.DefaultDispatcher
import com.android.systemui.core.common.IoDispatcher
import com.android.systemui.feature.notifications.domain.NotificationRepository
import com.android.systemui.feature.systembar.domain.SystemBarRepository
import com.android.systemui.feature.systembar.domain.SystemBarState
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/** Android callback adapters for the dynamic status data shown by the XML system bars. */
@Singleton
class AndroidSystemBarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher,
) : SystemBarRepository {
    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val bluetoothAdapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private val userManager = context.getSystemService(UserManager::class.java)
    private val locationManager = context.getSystemService(LocationManager::class.java)

    private val clock = timeFlow()
        .map { formatTime() }
        .onStart { emit(formatTime()) }
        .distinctUntilChanged()

    private val bluetooth = booleanBroadcastFlow(
        IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        },
    ) { bluetoothAdapter?.isEnabled == true }

    private val wifi = wifiFlow()
    private val volume = volumeFlow()
    private val location = flowOf(runCatching { locationManager?.isLocationEnabled == true }.getOrDefault(false))
    override val state: StateFlow<SystemBarState> = combine(
        clock,
        bluetooth,
        wifi,
        volume,
        location,
    ) { time, bluetoothEnabled, wifiConnected, volumeValue, locationEnabled ->
        SystemBarState(
            time = time,
            userName = "Driver",
            bluetoothEnabled = bluetoothEnabled,
            wifiConnected = wifiConnected,
            locationEnabled = locationEnabled,
            mediaVolume = volumeValue,
            mediaVolumeMax = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 100,
        )
    }.combine(notificationRepository.notifications) { base, notifications ->
        base.copy(
            userName = userNameValue(),
            notificationCount = notifications.size,
        )
    }.stateIn(scope, SharingStarted.Eagerly, SystemBarState(userName = userNameValue()))

    private fun userNameValue(): String = runCatching {
        userManager?.userName?.takeIf(String::isNotBlank)
    }.getOrNull() ?: "Driver"

    private fun formatTime(): String =
        DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(Date())

    private fun timeFlow(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, filter) }
            .onFailure { Timber.tag(TAG).w(it, "Unable to register clock receiver") }
        trySend(Unit)
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }

    private fun booleanBroadcastFlow(
        filter: IntentFilter,
        value: () -> Boolean,
    ): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(runCatching(value).getOrDefault(false))
            }
        }
        runCatching { context.registerReceiver(receiver, filter) }
            .onFailure { Timber.tag(TAG).w(it, "Unable to register status receiver") }
        trySend(runCatching(value).getOrDefault(false))
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    private fun wifiFlow(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        if (connectivityManager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        fun readConnected(network: Network? = null): Boolean {
            val capabilities = network?.let(connectivityManager::getNetworkCapabilities)
                ?: connectivityManager.activeNetwork?.let(connectivityManager::getNetworkCapabilities)
            return capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = trySend(readConnected(network)).isSuccess.let { }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))
            }

            override fun onLost(network: Network) {
                trySend(readConnected())
            }
        }
        trySend(readConnected())
        runCatching { connectivityManager.registerDefaultNetworkCallback(callback) }
            .onFailure { Timber.tag(TAG).w(it, "Unable to register network callback") }
        awaitClose { runCatching { connectivityManager.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    private fun volumeFlow(): Flow<Int> = callbackFlow {
        val manager = audioManager
        if (manager == null) {
            trySend(0)
            awaitClose { }
            return@callbackFlow
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(manager.getStreamVolume(AudioManager.STREAM_MUSIC))
            }
        }
        trySend(manager.getStreamVolume(AudioManager.STREAM_MUSIC))
        runCatching {
            context.registerReceiver(receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"))
        }.onFailure { Timber.tag(TAG).d(it, "Volume callback unavailable; using initial volume") }
        awaitClose { runCatching { context.unregisterReceiver(receiver) } }
    }.distinctUntilChanged()

    private companion object {
        const val TAG = "CarSystemUI.SystemBarData"
    }
}
