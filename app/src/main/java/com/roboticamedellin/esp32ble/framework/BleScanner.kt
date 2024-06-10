package com.roboticamedellin.esp32ble.framework

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

const val SERVICE_UUID = "81fcf4c4-6939-42a9-9a32-33209d86738a"
const val CHARACTERISTIC_UUID_RX = "9a317ed6-3ff9-4d67-b2a7-3b25b4ef9818"
const val CHARACTERISTIC_UUID_TX = "d4a20c30-0612-4fe1-8258-c12822df3d6e"

class BleScanner(
    private val context: Context
) {

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }

    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    private val _devicesMapStateFlow = MutableStateFlow<Map<String, BluetoothDevice>>(mapOf())
    val devicesStateFlow = _devicesMapStateFlow.asStateFlow()

    private val _dataFlowState = MutableStateFlow("")
    val valueFlowState = _dataFlowState.asStateFlow()

    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceAddress = device.address

            if (deviceMap.containsKey(deviceAddress).not()) {
                deviceMap[device.address] = device
                _devicesMapStateFlow.value = deviceMap
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                val device = result.device
                val deviceAddress = device.address

                if (deviceMap.containsKey(deviceAddress).not()) {
                    deviceMap[device.address] = device
                    _devicesMapStateFlow.value = deviceMap
                }
            }
        }
    }

    fun startScan() {
        val filters: List<ScanFilter> = listOf()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        bluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, bleScanCallback)

        Handler(Looper.getMainLooper()).postDelayed({
            stopScan()
        }, 10000)
    }

    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        bluetoothAdapter.bluetoothLeScanner.stopScan(bleScanCallback)
    }

    private var bluetoothGatt: BluetoothGatt? = null
    private var gattCallbackImpl: BluetoothGattCallbackImpl? = null

    fun disconnect() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }

        // Disconnect doesn't work caused by: https://blog.classycode.com/a-short-story-about-android-ble-connection-timeouts-and-gatt-internal-errors-fa89e3f6a456
        // and https://issuetracker.google.com/issues/37121017#comment13
        bluetoothGatt?.disconnect()
        gattCallbackImpl?.disconnect()
        bluetoothGatt?.close()
        gattCallbackImpl = null
        bluetoothGatt = null

        deviceMap.clear()
    }

    @SuppressLint("MissingPermission")
    private fun createGattCallback(): BluetoothGattCallbackImpl {
        return BluetoothGattCallbackImpl(
            context = context,
            onCharacteristicChangedCallback = { value ->
                _dataFlowState.value = value
            }
        ) {
            bluetoothGatt?.discoverServices()
        }
    }

    fun connectToDevice(deviceAddress: String) {
        val device = deviceMap[deviceAddress] ?: return
        connectToDevice(device)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        gattCallbackImpl = createGattCallback()
        bluetoothGatt = device.connectGatt(context, false, gattCallbackImpl)

    }

    fun sendValue(value: String) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        val bytes = value.toByteArray(Charsets.UTF_8)
        val characteristic = bluetoothGatt?.getService(UUID.fromString(SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_RX))
        characteristic?.value = bytes
        bluetoothGatt?.writeCharacteristic(characteristic)
    }
}
