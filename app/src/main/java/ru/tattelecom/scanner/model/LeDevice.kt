package ru.tattelecom.scanner.model

import android.bluetooth.BluetoothDevice

data class LeDevice(val device: BluetoothDevice, var rssi: Int)