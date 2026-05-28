package com.example.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Item
import com.example.data.Transaction
import com.example.data.TransactionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.*

class SalesViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<TransactionItem>()
    val cartItems: List<TransactionItem> = _cartItems

    // --- Autocomplete State ---
    private val _allItems = MutableStateFlow<List<Item>>(listOf(
        Item("1", "Indomie Goreng", 3000),
        Item("2", "Kopi Kapal Api", 1500),
        Item("3", "Beras 5kg", 65000),
        Item("4", "Gula Pasir 1kg", 16000),
        Item("5", "Telur Ayam 1kg", 28000),
        Item("6", "Minyak Goreng 2L", 35000),
        Item("7", "Teh Pucuk", 3500),
        Item("8", "Aqua 600ml", 3000),
        Item("9", "Roti Tawar", 15000),
        Item("10", "Susu Kental Manis", 12000)
    )) 
   
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isDropdownExpanded = MutableStateFlow(false)
    val isDropdownExpanded = _isDropdownExpanded.asStateFlow()

    val filteredItems = combine(_allItems, _searchQuery) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _isDropdownExpanded.value = true
    }

    fun onDropdownExpandedChanged(expanded: Boolean) {
        _isDropdownExpanded.value = expanded
    }

    fun onItemSelected(item: Item, onPriceUpdate: (Long) -> Unit) {
        _searchQuery.value = item.name 
        _isDropdownExpanded.value = false 
        onPriceUpdate(item.price)
    }
    // --------------------------

    val totalHarga: Long
        get() = _cartItems.sumOf { it.subTotal }

    fun addToCart(nama: String, qty: Int, harga: Long) {
        val item = _allItems.value.find { it.name == nama && it.price == harga }
        val itemId = item?.id ?: "TEMP-${nama.hashCode()}"

        val existingItemIndex = _cartItems.indexOfFirst { it.itemId == itemId && it.harga == harga }
        if (existingItemIndex != -1) {
            val existingItem = _cartItems[existingItemIndex]
            val updatedItem = existingItem.copy(
                qty = existingItem.qty + qty,
                subTotal = (existingItem.qty + qty) * existingItem.harga
            )
            _cartItems[existingItemIndex] = updatedItem
        } else {
            _cartItems.add(TransactionItem(itemId, nama, qty, harga))
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
