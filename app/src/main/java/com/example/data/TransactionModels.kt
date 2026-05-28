package com.example.data

data class Item(
    val id: String,
    val name: String,
    val price: Long
)

data class TransactionItem(
    val namaBarang: String,
    val qty: Int,
    val harga: Long,
    val subTotal: Long = qty * harga
)

data class Transaction(
    val kodeTransaksi: String,
    val tanggalTransaksi: Long, // Use timestamp for simplicity
    val items: List<TransactionItem>,
    val totalHarga: Long
)
