package com.roboticamedellin.esp32ble.framework

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import java.util.UUID

class BluetoothGattCallbackImpl(
    private val context: Context,
    private val bluetoothGatt: BluetoothGatt
) : BluetoothGattCallback() {

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