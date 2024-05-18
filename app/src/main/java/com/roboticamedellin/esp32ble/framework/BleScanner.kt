package com.roboticamedellin.esp32ble.framework

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
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

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("GATT_ESP32", "Connected to GATT server.")
                if (ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    return
                }
                bluetoothGatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("GATT_ESP32", "Disconnected from GATT server.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                return
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                for (service in gatt.services) {
                    Log.d("GATT_ESP32", "Service discovered UUID: ${service.uuid}")
                    for (characteristic in service.characteristics) {
                        Log.d("GATT_ESP32", "Characteristic discovered: ${characteristic.uuid}")
                    }
                }

                val service = gatt.getService(UUID.fromString(SERVICE_UUID))
                val txCharacteristic =
                    service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_TX))
                val rxCharacteristic =
                    service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_RX))

                // Enable notifications for the TX characteristic
                gatt.setCharacteristicNotification(txCharacteristic, true)

            } else {
                Log.w("GATT_ESP32", "onServicesDiscovered received: $status")
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == UUID.fromString(CHARACTERISTIC_UUID_TX)) {
                val value = characteristic.value
                val stringValue = String(value, Charsets.UTF_8)
                Log.d("GATT_ESP32", "TX Characteristic changed: $stringValue")
                // Handle the changed value as needed
            }
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

        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }
}
