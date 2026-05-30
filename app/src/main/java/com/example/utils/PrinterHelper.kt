package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import java.util.UUID

object PrinterHelper {
    // UUID standar untuk Serial Port Profile (SPP) pada printer thermal Bluetooth
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingPermission")
    fun printReceipt(device: BluetoothDevice, receiptText: String): Boolean {
        return try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            
            val outputStream = socket.outputStream
            // Kirim teks struk (format sudah diatur persis seperti dialog)
            outputStream.write(receiptText.toByteArray())
            
            // Tambahkan baris baru di akhir agar gulungan kertas pas untuk disobek
            outputStream.write("\n\n\n".toByteArray())
            
            outputStream.flush()
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
