package com.roboticamedellin.esp32ble.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roboticamedellin.esp32ble.repository.BLEDevicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var bleDevicesRepository: BLEDevicesRepository? = null
    fun initDependencies(
        bleDevicesRepository: BLEDevicesRepository
    ) {
        this.bleDevicesRepository = bleDevicesRepository
    }

    private val _deviceListState = MutableStateFlow<List<BLEItemUI>>(emptyList())
    val deviceListState = _deviceListState.asStateFlow()

    val dataFlow by lazy { bleDevicesRepository?.getDataFlow() }

    fun startScan() {
        bleDevicesRepository?.scanDevices()

        viewModelScope.launch {
            bleDevicesRepository?.getDevicesFlow()?.collect { items ->
                _deviceListState.value = items.map { BLEItemUI(it.name, it.address) }
            }
        }
    }

    fun disconnect() {
        bleDevicesRepository?.disconnect()
    }

    fun connectToDevice(address: String) {
        bleDevicesRepository?.connectToDevice(address)
    }

}

//#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
//#define CHARACTERISTIC_UUID_RX "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
//#define CHARACTERISTIC_UUID_TX "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"