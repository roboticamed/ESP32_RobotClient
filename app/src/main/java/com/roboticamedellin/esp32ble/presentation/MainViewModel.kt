package com.roboticamedellin.esp32ble.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roboticamedellin.esp32ble.repository.BLEDevicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private var bleDevicesRepository: BLEDevicesRepository? = null

    private val _dataFlowState = MutableStateFlow("")
    val dataFlowState = _dataFlowState.asStateFlow()

    private val _deviceListState = MutableStateFlow<List<BLEItemUI>>(emptyList())
    val deviceListState = _deviceListState.asStateFlow()

    fun initDependencies(
        bleDevicesRepository: BLEDevicesRepository
    ) {
        this.bleDevicesRepository = bleDevicesRepository
    }

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

        viewModelScope.launch {
            bleDevicesRepository?.getDataFlow()?.collect { data ->
                _dataFlowState.value = data
            }
        }
    }

    fun sendCommand(command: String) {
        bleDevicesRepository?.sendCommand(command)
    }

}
