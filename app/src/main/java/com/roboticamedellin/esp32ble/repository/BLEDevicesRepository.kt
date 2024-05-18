package com.roboticamedellin.esp32ble.repository

import com.roboticamedellin.esp32ble.domain.BLEItem
import kotlinx.coroutines.flow.Flow

interface BLEDevicesRepository {

    fun scanDevices()

    fun disconnect()

    fun sendCommand(command: String)

    fun connectToDevice(address: String)

    fun getDevicesFlow(): Flow<List<BLEItem>>

    fun getDataFlow(): Flow<String>

}
