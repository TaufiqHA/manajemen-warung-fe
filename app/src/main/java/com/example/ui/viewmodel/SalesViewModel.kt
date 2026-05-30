package com.example.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import com.example.data.Item
import com.example.data.Transaction
import com.example.data.TransactionItem
import com.example.data.UserPreferences
import com.example.utils.PrinterHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences(application)
    private val storageHelper = com.example.utils.LocalStorageHelper(application)

    val namaWarungState = userPrefs.namaWarung.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "WARUNG KITA"
    )

    val alamatWarungState = userPrefs.alamatWarung.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ""
    )

    private val _cartItems = mutableStateListOf<TransactionItem>()
    val cartItems: List<TransactionItem> = _cartItems

    // --- Autocomplete State ---
    private val _allItems = MutableStateFlow<List<Item>>(emptyList()) 

    init {
        loadItems()
    }

    fun loadItems() {
        val menuItems = storageHelper.getMenuList()
        _allItems.value = menuItems.map { Item(it.id, it.nama, it.harga.toLong()) }
    }
   
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

    fun processTransaction(diskonPersen: Double = 0.0, diskonNominal: Long = 0L, totalSetelahDiskon: Long = totalHarga): Transaction {
        val kode = "TRX-${SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(Date())}"
        val transaction = Transaction(
            kodeTransaksi = kode,
            tanggalTransaksi = System.currentTimeMillis(),
            items = _cartItems.toList(),
            totalHarga = totalHarga,
            diskonPersen = diskonPersen,
            diskonNominal = diskonNominal,
            totalSetelahDiskon = totalSetelahDiskon
        )
        // Simpan transaksi secara lokal ke SharedPreferences
        storageHelper.addTransaction(transaction)
        return transaction
    }

    fun formatReceipt(transaction: Transaction): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.tanggalTransaksi))
        
        val sb = StringBuilder()
        
        val namaWarungUpper = namaWarungState.value.uppercase()
        val namaPadding = (30 - namaWarungUpper.length) / 2
        val namaLine = " ".repeat(namaPadding.coerceAtLeast(0)) + namaWarungUpper + "\n"
        sb.append(namaLine)

        val alamat = alamatWarungState.value
        if (alamat.isNotBlank()) {
            val alamatLine = if (alamat.length > 30) {
                alamat.take(30)
            } else {
                val pad = (30 - alamat.length) / 2
                " ".repeat(pad.coerceAtLeast(0)) + alamat
            }
            sb.append(alamatLine + "\n")
        }
        
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
        if (transaction.diskonNominal > 0) {
            val subtotalLabel = "Subtotal"
            val subtotalVal = transaction.totalHarga.toString()
            val subPadding = 30 - subtotalLabel.length - subtotalVal.length
            sb.append(subtotalLabel + " ".repeat(subPadding.coerceAtLeast(1)) + subtotalVal + "\n")
            
            val discLabel = if (transaction.diskonPersen > 0) {
                "Diskon (${transaction.diskonPersen.toLong()}%)"
            } else {
                "Diskon"
            }
            val discVal = "-${transaction.diskonNominal}"
            val discPadding = 30 - discLabel.length - discVal.length
            sb.append(discLabel + " ".repeat(discPadding.coerceAtLeast(1)) + discVal + "\n")
            sb.append("------------------------------\n")
        }
        val totalLabel = "TOTAL"
        val totalVal = transaction.totalSetelahDiskon.toString()
        val totalPadding = 30 - totalLabel.length - totalVal.length
        sb.append(totalLabel + " ".repeat(totalPadding.coerceAtLeast(1)) + totalVal + "\n")
        sb.append("------------------------------\n")
        sb.append("    Terima Kasih    \n")
        sb.append("  Selamat Belanja Lagi  \n")
        
        return sb.toString()
    }

    fun printToThermal(device: BluetoothDevice, receiptText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PrinterHelper.printReceipt(device, receiptText)
            onComplete(success)
        }
    }
}
