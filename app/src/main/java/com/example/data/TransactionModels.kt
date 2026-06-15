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
    val subTotal: Long = qty * harga,
    val servedQty: Int = 0
)

data class Transaction(
    val kodeTransaksi: String,
    val tanggalTransaksi: Long, // Use timestamp for simplicity
    val customerName: String = "",
    val status: String = "PENDING", // PENDING, READY, COMPLETED
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
    val customerName: String? = null,
    val orderStatus: String? = null, // PENDING, READY, COMPLETED
    val items: List<TransactionItemRequest>
)

data class TransactionItemRequest(
    val namaItem: String,
    val jumlah: Int,
    val harga: Double,
    val catatan: String = "",
    val servedQty: Int = 0
)

data class CancelTransactionRequest(
    val reason: String
)

data class UpdateWarungRequest(
    val name: String,
    val address: String,
    val email: String
)

data class UpdateStatusRequest(
    val status: String
)

data class AddTransactionItemRequest(
    val product_id: String,
    val quantity: Int,
    val unit_price: Double,
    val subtotal: Double
)
