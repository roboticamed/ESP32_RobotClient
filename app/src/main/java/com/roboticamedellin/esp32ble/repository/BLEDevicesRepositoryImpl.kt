package com.roboticamedellin.esp32ble.repository

import android.annotation.SuppressLint
import com.roboticamedellin.esp32ble.domain.BLEItem
import com.roboticamedellin.esp32ble.framework.BleScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class BLEDevicesRepositoryImpl(
    private val bleScanner: BleScanner
) : BLEDevicesRepository {

    private val _valueStateFlow = MutableStateFlow("")

    init {
        bleScanner.rxCallback = { value ->
            _valueStateFlow.value = value
        }
    }

    override fun scanDevices() {
        bleScanner.startScan()
    }

    override fun disconnect() {
        bleScanner.disconnect()
    }

    override fun sendCommand(command: String) {
        bleScanner.sendValue(command)
    }

    override fun connectToDevice(address: String) {
        bleScanner.connectToDevice(address)
    }

    @SuppressLint("MissingPermission")
    override fun getDevicesFlow(): Flow<List<BLEItem>> = bleScanner.devicesStateFlow
        .map { devices ->
            devices.map { device ->
                BLEItem(
                    name = device.value.name ?: "Unknown",
                    address = device.key
                )
            }
        }

    override fun getDataFlow(): Flow<String> = _valueStateFlow

}
