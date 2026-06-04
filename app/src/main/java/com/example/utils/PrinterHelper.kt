package com.example.utils

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import java.net.Socket
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
            
            // Tambahkan jeda waktu agar printer sempat memproses buffer sebelum socket ditutup
            try {
                Thread.sleep(1500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Mencetak struk ke printer thermal melalui jaringan (Network/LAN).
     * Digunakan juga untuk simulasi ke Virtual Thermal Printer.
     */
    fun printToNetwork(ipAddress: String, port: Int, receiptText: String): Boolean {
        return try {
            val socket = Socket(ipAddress, port)
            socket.soTimeout = 5000 // Timeout 5 detik
            val outputStream = socket.getOutputStream()
            
            // Kirim teks struk
            outputStream.write(receiptText.toByteArray())
            
            // Tambahkan baris baru (feed paper)
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
