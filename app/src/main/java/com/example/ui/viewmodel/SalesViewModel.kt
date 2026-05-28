package com.example.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.example.data.Transaction
import com.example.data.TransactionItem
import java.text.SimpleDateFormat
import java.util.*

class SalesViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<TransactionItem>()
    val cartItems: List<TransactionItem> = _cartItems

    val totalHarga: Long
        get() = _cartItems.sumOf { it.subTotal }

    fun addToCart(nama: String, qty: Int, harga: Long) {
        val existingItemIndex = _cartItems.indexOfFirst { it.namaBarang == nama && it.harga == harga }
        if (existingItemIndex != -1) {
            val existingItem = _cartItems[existingItemIndex]
            val updatedItem = existingItem.copy(
                qty = existingItem.qty + qty,
                subTotal = (existingItem.qty + qty) * existingItem.harga
            )
            _cartItems[existingItemIndex] = updatedItem
        } else {
            _cartItems.add(TransactionItem(nama, qty, harga))
        }
    }

    fun removeFromCart(item: TransactionItem) {
        _cartItems.remove(item)
    }

    fun clearCart() {
        _cartItems.clear()
    }

    fun processTransaction(): Transaction {
        val kode = "TRX-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}"
        val transaction = Transaction(
            kodeTransaksi = kode,
            tanggalTransaksi = System.currentTimeMillis(),
            items = _cartItems.toList(),
            totalHarga = totalHarga
        )
        // In a real app, we would save this to a database
        return transaction
    }

    fun formatReceipt(transaction: Transaction): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.tanggalTransaksi))
        
        val sb = StringBuilder()
        sb.append("      WARUNG KITA      \n")
        sb.append("------------------------------\n")
        sb.append("No: ${transaction.kodeTransaksi}\n")
        sb.append("Tgl: $dateStr\n")
        sb.append("------------------------------\n")
        
        transaction.items.forEach { item ->
            sb.append("${item.namaBarang}\n")
            val qtyStr = "${item.qty} x ${item.harga}"
            val subtotalStr = item.subTotal.toString()
            val padding = 30 - qtyStr.length - subtotalStr.length
            sb.append(qtyStr + " ".repeat(padding.coerceAtLeast(1)) + subtotalStr + "\n")
        }
        
        sb.append("------------------------------\n")
        val totalLabel = "TOTAL"
        val totalVal = transaction.totalHarga.toString()
        val totalPadding = 30 - totalLabel.length - totalVal.length
        sb.append(totalLabel + " ".repeat(totalPadding.coerceAtLeast(1)) + totalVal + "\n")
        sb.append("------------------------------\n")
        sb.append("    Terima Kasih    \n")
        sb.append("  Selamat Belanja Lagi  \n")
        
        return sb.toString()
    }
}
