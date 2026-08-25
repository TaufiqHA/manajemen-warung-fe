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
            
            // 1. Kirim command inisialisasi ESC/POS (ESC @)
            val initCommand = byteArrayOf(0x1B, 0x40)
            outputStream.write(initCommand)
            
            // 2. Set perataan teks ke kiri (ESC a 0)
            val alignLeftCommand = byteArrayOf(0x1B, 0x61, 0x00)
            outputStream.write(alignLeftCommand)

            // 3. Pastikan format baris baru dikenali printer (CRLF) lalu kirim per baris
            val formattedText = receiptText.replace("\r\n", "\n").replace("\n", "\r\n")
            outputStream.write(formattedText.toByteArray(Charsets.UTF_8))
            
            // 4. Tambahkan line feed (baris baru) yang cukup di akhir agar kertas bisa disobek
            val feedCommand = byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)
            outputStream.write(feedCommand)
            
            outputStream.flush()
            
            // Tambahkan jeda waktu dinamis berdasarkan jumlah baris agar buffer printer selesai mencetak sebelum socket ditutup
            val lineCount = formattedText.lines().size
            val sleepMs = maxOf(2500L, lineCount * 120L)
            try {
                Thread.sleep(sleepMs)
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
            
            // 1. Kirim command inisialisasi ESC/POS (ESC @)
            val initCommand = byteArrayOf(0x1B, 0x40)
            outputStream.write(initCommand)
            
            // 2. Set perataan teks ke kiri (ESC a 0)
            val alignLeftCommand = byteArrayOf(0x1B, 0x61, 0x00)
            outputStream.write(alignLeftCommand)

            // 3. Pastikan format baris baru dikenali printer (CRLF) lalu kirim
            val formattedText = receiptText.replace("\r\n", "\n").replace("\n", "\r\n")
            outputStream.write(formattedText.toByteArray(Charsets.UTF_8))
            
            // 4. Tambahkan line feed (baris baru) yang cukup di akhir agar kertas bisa disobek
            val feedCommand = byteArrayOf(0x0A, 0x0A, 0x0A, 0x0A)
            outputStream.write(feedCommand)
            
            outputStream.flush()
            socket.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
