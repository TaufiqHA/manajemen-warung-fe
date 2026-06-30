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
    val status: String? = null, // Tambahan untuk kompatibilitas
    val items: List<TransactionItemRequest>,
    @com.squareup.moshi.Json(name = "discount_amount") val discountAmount: Long? = null
)

data class TransactionItemRequest(
    val namaItem: String? = null,
    val jumlah: Int? = null,
    val harga: Double? = null,
    val catatan: String = "",
    val servedQty: Int = 0,
    // Tambahan untuk kompatibilitas backend
    val product_id: String? = null,
    val quantity: Int? = null,
    val unit_price: Double? = null,
    val subtotal: Double? = null
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
    val status: String,
    val payment_method: String? = null,
    @com.squareup.moshi.Json(name = "discount_amount") val discountAmount: Long? = null
)

data class AddTransactionItemRequest(
    val product_id: String?,
    val quantity: Int,
    val unit_price: Double,
    val subtotal: Double
)

data class UnsyncedStatusUpdate(
    val transactionId: String,
    val status: String
)

data class UpdateTransactionItemRequest(
    val quantity: Int,
    val subtotal: Double
)

data class UnsyncedItemUpdate(
    val transactionId: String,
    val itemId: String,
    val quantity: Int,
    val subtotal: Double
)
