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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = com.example.data.api.RetrofitClient.getProductApiService(getApplication()).getProducts()
                if (response.isSuccessful && response.body()?.data != null) {
                    val apiItems = response.body()!!.data!!
                    storageHelper.saveMenuList(apiItems)
                    launch(Dispatchers.Main) {
                        _allItems.value = apiItems.map { Item(it.id, it.nama, it.harga.toLong()) }
                    }
                }
            } catch (e: Exception) {}
        }
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

    fun addToCart(nama: String, qty: Int, harga: Long, diskon: Long = 0L) {
        val item = _allItems.value.find { it.name == nama }
        val itemId = item?.id ?: "TEMP-${nama.hashCode()}"

        val displayNama = if (diskon > 0) "$nama (Diskon: ${com.example.utils.formatRupiah(diskon)})" else nama
        val finalHarga = (harga - diskon).coerceAtLeast(0L)

        val existingItemIndex = _cartItems.indexOfFirst { it.itemId == itemId && it.harga == finalHarga && it.namaBarang == displayNama }
        if (existingItemIndex != -1) {
            val existingItem = _cartItems[existingItemIndex]
            val updatedItem = existingItem.copy(
                qty = existingItem.qty + qty,
                subTotal = (existingItem.qty + qty) * existingItem.harga
            )
            _cartItems[existingItemIndex] = updatedItem
        } else {
            _cartItems.add(TransactionItem(itemId, displayNama, qty, finalHarga))
        }
    }

    fun removeFromCart(item: TransactionItem) {
        _cartItems.remove(item)
    }

    fun clearCart() {
        _cartItems.clear()
    }

    fun processTransaction(customerName: String, diskonPersen: Double = 0.0, diskonNominal: Long = 0L, totalSetelahDiskon: Long = totalHarga, paymentMethod: String = "Cash"): Transaction {
        val dateNow = Date()
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(dateNow)
        val timeStr = SimpleDateFormat("HHmmss", Locale.getDefault()).format(dateNow)
        val kode = "TRX-$dateStr$timeStr"
        val transaction = Transaction(
            kodeTransaksi = kode,
            tanggalTransaksi = System.currentTimeMillis(),
            customerName = customerName,
            status = "PENDING",
            items = _cartItems.toList(),
            totalHarga = totalHarga,
            diskonPersen = diskonPersen,
            diskonNominal = diskonNominal,
            totalSetelahDiskon = totalSetelahDiskon
        )
        // Simpan transaksi secara lokal ke SharedPreferences dengan metode pembayaran
        storageHelper.addTransaction(transaction, paymentMethod)
        return transaction
    }

    private fun formatReceiptNumber(number: Long): String {
        return java.text.NumberFormat.getIntegerInstance(Locale("in", "ID")).format(number)
    }

    fun formatReceipt(transaction: Transaction): String {
        val lineWidth = 32 // Standar printer thermal 58mm (Font A)
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.tanggalTransaksi))
        
        val sb = StringBuilder()
        
        // Header - Nama Warung (Centered)
        val namaWarungUpper = namaWarungState.value.uppercase()
        val namaPadding = (lineWidth - namaWarungUpper.length) / 2
        sb.append(" ".repeat(namaPadding.coerceAtLeast(0)) + namaWarungUpper + "\n")

        // Header - Alamat (Centered & Wrapped)
        val alamat = alamatWarungState.value
        if (alamat.isNotBlank()) {
            val alamatLines = alamat.chunked(lineWidth)
            alamatLines.forEach { line ->
                val pad = (lineWidth - line.length) / 2
                sb.append(" ".repeat(pad.coerceAtLeast(0)) + line + "\n")
            }
        }
        
        sb.append("-".repeat(lineWidth) + "\n")
        sb.append("No: ${transaction.kodeTransaksi}\n")
        sb.append("Tgl: $dateStr\n")
        sb.append("-".repeat(lineWidth) + "\n")
        
        transaction.items.forEach { item ->
            // Nama barang (Wrapped)
            val namaLines = item.namaBarang.chunked(lineWidth)
            namaLines.forEach { sb.append(it + "\n") }
            
            val qtyStr = "${item.qty} x ${formatReceiptNumber(item.harga)}"
            val subtotalStr = formatReceiptNumber(item.subTotal)
            val padding = lineWidth - qtyStr.length - subtotalStr.length
            
            if (padding >= 1) {
                sb.append(qtyStr + " ".repeat(padding) + subtotalStr + "\n")
            } else {
                sb.append(qtyStr + "\n")
                sb.append(" ".repeat((lineWidth - subtotalStr.length).coerceAtLeast(0)) + subtotalStr + "\n")
            }
        }
        
        sb.append("-".repeat(lineWidth) + "\n")
        
        if (transaction.diskonNominal > 0) {
            val subtotalLabel = "Subtotal"
            val subtotalVal = formatReceiptNumber(transaction.totalHarga)
            val subPadding = lineWidth - subtotalLabel.length - subtotalVal.length
            sb.append(subtotalLabel + " ".repeat(subPadding.coerceAtLeast(1)) + subtotalVal + "\n")
            
            val discLabel = if (transaction.diskonPersen > 0) {
                "Diskon (${transaction.diskonPersen.toLong()}%)"
            } else {
                "Diskon"
            }
            val discVal = "-${formatReceiptNumber(transaction.diskonNominal)}"
            val discPadding = lineWidth - discLabel.length - discVal.length
            sb.append(discLabel + " ".repeat(discPadding.coerceAtLeast(1)) + discVal + "\n")
            sb.append("-".repeat(lineWidth) + "\n")
        }
        
        val totalLabel = "TOTAL"
        val totalVal = formatReceiptNumber(transaction.totalSetelahDiskon)
        val totalPadding = lineWidth - totalLabel.length - totalVal.length
        sb.append(totalLabel + " ".repeat(totalPadding.coerceAtLeast(1)) + totalVal + "\n")
        sb.append("-".repeat(lineWidth) + "\n")
        
        // Footer (Centered)
        val footer1 = "Terima Kasih"
        val f1Padding = (lineWidth - footer1.length) / 2
        sb.append(" ".repeat(f1Padding.coerceAtLeast(0)) + footer1 + "\n")
        
        val footer2 = "Selamat Belanja Lagi"
        val f2Padding = (lineWidth - footer2.length) / 2
        sb.append(" ".repeat(f2Padding.coerceAtLeast(0)) + footer2 + "\n")

        val footer3 = namaWarungState.value
        val f3Padding = (lineWidth - footer3.length) / 2
        sb.append(" ".repeat(f3Padding.coerceAtLeast(0)) + footer3 + "\n")
        
        return sb.toString()
    }

    fun formatKitchenReceipt(transaction: Transaction): String {
        val lineWidth = 32
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val dateStr = sdf.format(Date(transaction.tanggalTransaksi))
        
        val sb = StringBuilder()
        
        // Header
        val title = "STRUK DAPUR"
        val titlePadding = (lineWidth - title.length) / 2
        sb.append(" ".repeat(titlePadding.coerceAtLeast(0)) + title + "\n")
        
        sb.append("-".repeat(lineWidth) + "\n")
        sb.append("No: ${transaction.kodeTransaksi}\n")
        sb.append("Tgl: $dateStr\n")
        if (transaction.customerName.isNotBlank()) {
            sb.append("Pelanggan: ${transaction.customerName}\n")
        }
        sb.append("-".repeat(lineWidth) + "\n")
        
        transaction.items.forEach { item ->
            val qtyStr = "${item.qty} x "
            val namaLines = item.namaBarang.chunked(lineWidth - qtyStr.length)
            
            sb.append(qtyStr + namaLines.firstOrNull().orEmpty() + "\n")
            for (i in 1 until namaLines.size) {
                sb.append(" ".repeat(qtyStr.length) + namaLines[i] + "\n")
            }
        }
        
        sb.append("-".repeat(lineWidth) + "\n")
        return sb.toString()
    }

    fun printToThermal(device: BluetoothDevice, receiptText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PrinterHelper.printReceipt(device, receiptText)
            onComplete(success)
        }
    }

    /**
     * Mencetak ke printer melalui jaringan (untuk Virtual Thermal Printer / Network Printer)
     */
    fun printToNetwork(ipAddress: String, port: Int, receiptText: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = PrinterHelper.printToNetwork(ipAddress, port, receiptText)
            onComplete(success)
        }
    }
}
