package com.roboticamedellin.esp32ble.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlin.random.nextUInt

class MainViewModel : ViewModel() {

    private val _deviceListState = MutableStateFlow<List<String>>(emptyList())
    val deviceListState = _deviceListState.asStateFlow()

    fun startScan() {
        _deviceListState.value = listOf(
            "Device ${Random.nextUInt()}",
            "Device ${Random.nextUInt()}",
            "Device ${Random.nextUInt()}",
            "Device ${Random.nextUInt()}",
            "Device ${Random.nextUInt()}",
            "Device ${Random.nextUInt()}",
        )
    }

    fun stopScan() {

    }

    fun disconnect() {

    }

    fun connectToDevice() {

    }

}