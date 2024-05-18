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
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

const val SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_UUID_RX = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"
const val CHARACTERISTIC_UUID_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E"

class BleScanner(
    private val context: Context
) {

    var rxCallback: (String) -> Unit = {}

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager
        bluetoothManager.adapter
    }

    private val _devicesMap = MutableStateFlow<Map<String, BluetoothDevice>>(mapOf())
    val devicesStateFlow: StateFlow<Map<String, BluetoothDevice>> get() = _devicesMap
    private val deviceMap = mutableMapOf<String, BluetoothDevice>()

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            val deviceAddress = device.address

            if (deviceMap.containsKey(deviceAddress).not()) {
                deviceMap[device.address] = device
                _devicesMap.value = deviceMap
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>) {
            super.onBatchScanResults(results)
            for (result in results) {
                val device = result.device
                val deviceAddress = device.address

                if (deviceMap.containsKey(deviceAddress).not()) {
                    deviceMap[device.address] = device
                    _devicesMap.value = deviceMap
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
        bluetoothAdapter.bluetoothLeScanner.startScan(filters, settings, leScanCallback)

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
        bluetoothAdapter.bluetoothLeScanner.stopScan(leScanCallback)
    }

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
        bluetoothGatt.disconnect()
    }

    private lateinit var bluetoothGatt: BluetoothGatt

    @SuppressLint("MissingPermission")
    private val gattCallbackImpl = BluetoothGattCallbackImpl(
        context = context,
        onCharacteristicChangedCallback = { value ->
            rxCallback(value)
        }
    ) {
        bluetoothGatt.discoverServices()
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
        val characteristic = bluetoothGatt.getService(UUID.fromString(SERVICE_UUID))
            .getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_RX))
        characteristic.value = bytes
        bluetoothGatt.writeCharacteristic(characteristic)
    }
}
