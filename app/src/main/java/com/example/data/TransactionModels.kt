package com.example.data

data class Item(
    val id: String,
    val name: String,
    val price: Long
)

data class TransactionItem(
    val itemId: String,
    val namaBarang: String,
    val qty: Int,
    val harga: Long,
    val subTotal: Long = qty * harga
)

data class Transaction(
    val kodeTransaksi: String,
    val tanggalTransaksi: Long, // Use timestamp for simplicity
    val items: List<TransactionItem>,
    val totalHarga: Long,
    val diskonPersen: Double = 0.0,
    val diskonNominal: Long = 0L,
    val totalSetelahDiskon: Long = totalHarga
)

data class TransactionRequest(
    val idTransaksi: String?,
    val waktu: String?,
    val dicatatOleh: String?,
    val payment_method: String? = null,
    val items: List<com.example.ui.screens.TransaksiHarian>
)

data class CancelTransactionRequest(
    val reason: String
)

data class UpdateWarungRequest(
    val name: String,
    val address: String,
    val email: String
)
