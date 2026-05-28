package com.example.data

// Data class untuk hasil rekap per tanggal
data class DailyItemReport(
    val date: Int,           // Tanggal transaksi (1, 2, 3... 31)
    val totalAmount: Double  // Total penjualan (Rp) pada tanggal tersebut
)
