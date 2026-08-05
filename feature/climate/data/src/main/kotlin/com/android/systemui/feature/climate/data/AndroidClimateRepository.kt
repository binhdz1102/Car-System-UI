package com.android.systemui.feature.climate.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.VehicleUnit
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import com.android.systemui.core.common.ActionResult
import com.android.systemui.core.common.IoDispatcher
import com.android.systemui.feature.climate.domain.ClimateRepository
import com.android.systemui.feature.climate.domain.ClimateState
import com.android.systemui.feature.climate.domain.ClimateUnit
import com.android.systemui.feature.climate.domain.ClimateZoneState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * VHAL adapter used by both the bottom-bar temperature controls and the XML climate panel.
 *
 * The adapter intentionally discovers the current vehicle configuration instead of assuming all
 * HVAC properties exist. That mirrors the discovery approach in My-System-App and keeps a missing
 * property from taking down the SystemUI process.
 */
@Singleton
class AndroidClimateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : ClimateRepository {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val mutableState = MutableStateFlow(ClimateState())
    private val registeredProperties = mutableSetOf<Int>()

    @Volatile
    private var propertyManager: CarPropertyManager? = null
    private var car: Car? = null

    override val state: StateFlow<ClimateState> = mutableState.asStateFlow()

    private val propertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(value: CarPropertyValue<*>) {
            scope.launch { refreshInternal() }
        }

        override fun onErrorEvent(propertyId: Int, areaId: Int) {
            Timber.tag(TAG).w(
                "VHAL rejected property=%s area=%s",
                VehiclePropertyIds.toString(propertyId),
                Integer.toHexString(areaId),
            )
            mutableState.update { it.copy(lastError = "Vehicle rejected the climate update") }
        }
    }

    init {
        runCatching {
            car = Car.createCar(
                context,
                /* handler = */ null,
                Car.CAR_WAIT_TIMEOUT_WAIT_FOREVER,
            ) { connectedCar, ready ->
                propertyManager = if (ready) {
                    connectedCar.getCarManager(Car.PROPERTY_SERVICE) as? CarPropertyManager
                } else {
                    null
                }
                Timber.tag(TAG).i("Car service ready=%s manager=%s", ready, propertyManager != null)
                scope.launch {
                    if (ready) registerCallbacks()
                    refreshInternal()
                }
            }
        }.onFailure { throwable ->
            Timber.tag(TAG).e(throwable, "Unable to create the car service connection")
            mutableState.value = ClimateState(lastError = "Vehicle climate service is unavailable")
        }
    }

    override suspend fun refresh() = withContext(dispatcher) { refreshInternal() }

    override suspend fun adjustTemperature(areaId: Int, displayStep: Int): ActionResult =
        withContext(dispatcher) {
            val manager = propertyManager
                ?: return@withContext ActionResult.Failure("Vehicle climate service is not connected")
            try {
                val config = manager.getCarPropertyConfig(VehiclePropertyIds.HVAC_TEMPERATURE_SET)
                // AOSP TemperatureControlView writes the Celsius increment even when the UI is
                // rendered in Fahrenheit. The simulator exposes this as configArray[2] * 0.1C.
                val step = config?.configArray?.getOrNull(2)?.toFloat()?.div(10f) ?: 0.5f
                val supportedAreas = config?.areaIds
                    ?.filter { supportedArea -> areaId == 0 || areaId and supportedArea != 0 }
                    .orEmpty()
                if (supportedAreas.isEmpty()) {
                    return@withContext ActionResult.Failure(
                        "Vehicle does not expose a temperature area for target=$areaId",
                    )
                }
                supportedAreas.forEach { supportedArea ->
                    val current = manager.getFloatProperty(
                        VehiclePropertyIds.HVAC_TEMPERATURE_SET,
                        supportedArea,
                    )
                    val minValue = (config?.getMinValue(supportedArea) as? Number)?.toFloat() ?: -40f
                    val maxValue = (config?.getMaxValue(supportedArea) as? Number)?.toFloat() ?: 100f
                    val next = (current + (displayStep * step)).coerceIn(minValue, maxValue)
                    manager.setFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, supportedArea, next)
                    Timber.tag(TAG).d(
                        "Temperature target=%s area=%s %.2f -> %.2f",
                        areaId,
                        supportedArea,
                        current,
                        next,
                    )
                }
                refreshInternal()
                ActionResult.Success
            } catch (throwable: Throwable) {
                Timber.tag(TAG).e(throwable, "Unable to adjust temperature area=%s", areaId)
                ActionResult.Failure(throwable.message ?: "Unable to adjust temperature", throwable)
            }
        }

    override suspend fun setAc(enabled: Boolean): ActionResult =
        setBooleanOnAllAreas(VehiclePropertyIds.HVAC_AC_ON, enabled)

    override suspend fun setAuto(enabled: Boolean): ActionResult =
        setBooleanOnAllAreas(VehiclePropertyIds.HVAC_AUTO_ON, enabled)

    override suspend fun setFanSpeed(speed: Int): ActionResult =
        withContext(dispatcher) {
            val manager = propertyManager
                ?: return@withContext ActionResult.Failure("Vehicle climate service is not connected")
            runCatching {
                val config = manager.getCarPropertyConfig(VehiclePropertyIds.HVAC_FAN_SPEED)
                    ?: error("Fan speed is not supported by this vehicle")
                config.areaIds.forEach { areaId ->
                    manager.setIntProperty(VehiclePropertyIds.HVAC_FAN_SPEED, areaId, speed)
                }
                refreshInternal()
                ActionResult.Success
            }.getOrElse { throwable ->
                Timber.tag(TAG).e(throwable, "Unable to set fan speed=%s", speed)
                ActionResult.Failure(throwable.message ?: "Unable to set fan speed", throwable)
            }
        }

    private suspend fun setBooleanOnAllAreas(propertyId: Int, enabled: Boolean): ActionResult =
        withContext(dispatcher) {
            val manager = propertyManager
                ?: return@withContext ActionResult.Failure("Vehicle climate service is not connected")
            runCatching {
                val config = manager.getCarPropertyConfig(propertyId)
                    ?: error("${VehiclePropertyIds.toString(propertyId)} is not supported")
                config.areaIds.forEach { areaId ->
                    manager.setBooleanProperty(propertyId, areaId, enabled)
                }
                refreshInternal()
                ActionResult.Success
            }.getOrElse { throwable ->
                Timber.tag(TAG).e(throwable, "Unable to set %s=%s", propertyId, enabled)
                ActionResult.Failure(throwable.message ?: "Unable to update climate", throwable)
            }
        }

    private fun registerCallbacks() {
        val manager = propertyManager ?: return
        CLIMATE_PROPERTIES.forEach { propertyId ->
            if (!registeredProperties.add(propertyId)) return@forEach
            runCatching {
                manager.registerCallback(
                    propertyCallback,
                    propertyId,
                    CarPropertyManager.SENSOR_RATE_ONCHANGE,
                )
            }.onFailure { throwable ->
                Timber.tag(TAG).d(throwable, "Property not available: %s", propertyId)
            }
        }
    }

    private fun refreshInternal() {
        val manager = propertyManager
        if (manager == null) {
            mutableState.value = ClimateState(
                connected = false,
                lastError = "Vehicle climate service is not connected",
            )
            return
        }

        try {
            val unit = readUnit(manager)
            mutableState.value = ClimateState(
                connected = true,
                unit = unit,
                driver = readZone(manager, ClimateState.DRIVER_AREA_ID),
                passenger = readZone(manager, ClimateState.PASSENGER_AREA_ID),
                fanSpeed = readInt(manager, VehiclePropertyIds.HVAC_FAN_SPEED),
                acEnabled = readBoolean(manager, VehiclePropertyIds.HVAC_AC_ON),
                autoEnabled = readBoolean(manager, VehiclePropertyIds.HVAC_AUTO_ON),
            )
        } catch (throwable: Throwable) {
            Timber.tag(TAG).e(throwable, "Unable to refresh climate state")
            mutableState.update {
                it.copy(connected = true, lastError = throwable.message ?: "Unable to read climate")
            }
        }
    }

    private fun readZone(manager: CarPropertyManager, areaId: Int): ClimateZoneState {
        val config = manager.getCarPropertyConfig(VehiclePropertyIds.HVAC_TEMPERATURE_SET)
            ?: return ClimateZoneState(areaId)
        val supportedAreas = config.areaIds.filter { supportedArea ->
            areaId == 0 || areaId and supportedArea != 0
        }
        val value = supportedAreas.asSequence()
            .mapNotNull { supportedArea ->
                runCatching {
                    manager.getFloatProperty(VehiclePropertyIds.HVAC_TEMPERATURE_SET, supportedArea)
                }.getOrNull()
            }
            .firstOrNull()
        Timber.tag(TAG).d("Read temperature target=%s supportedAreas=%s value=%s", areaId, supportedAreas, value)
        return ClimateZoneState(
            areaId = areaId,
            temperatureCelsius = value,
            available = value != null,
            writable = config.access and CarPropertyConfig.VEHICLE_PROPERTY_ACCESS_WRITE != 0,
        )
    }

    private fun readUnit(manager: CarPropertyManager): ClimateUnit {
        val raw = runCatching {
            manager.getIntProperty(VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS, 0)
        }.getOrNull()
        return if (raw == VehicleUnit.FAHRENHEIT) ClimateUnit.FAHRENHEIT else ClimateUnit.CELSIUS
    }

    private fun readInt(manager: CarPropertyManager, propertyId: Int): Int? =
        runCatching {
            val config = manager.getCarPropertyConfig(propertyId) ?: return@runCatching null
            config.areaIds.firstOrNull()?.let { manager.getIntProperty(propertyId, it) }
        }.getOrNull()

    private fun readBoolean(manager: CarPropertyManager, propertyId: Int): Boolean? =
        runCatching {
            val config = manager.getCarPropertyConfig(propertyId) ?: return@runCatching null
            config.areaIds.firstOrNull()?.let { manager.getBooleanProperty(propertyId, it) }
        }.getOrNull()

    private companion object {
        const val TAG = "CarSystemUI.Climate"
        val CLIMATE_PROPERTIES = intArrayOf(
            VehiclePropertyIds.HVAC_TEMPERATURE_SET,
            VehiclePropertyIds.HVAC_TEMPERATURE_DISPLAY_UNITS,
            VehiclePropertyIds.HVAC_FAN_SPEED,
            VehiclePropertyIds.HVAC_AC_ON,
            VehiclePropertyIds.HVAC_AUTO_ON,
        )
    }
}
