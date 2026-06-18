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

    private val _activeOrders = MutableStateFlow<List<Transaction>>(emptyList())
    val activeOrders = _activeOrders.asStateFlow()

    fun loadActiveOrders() {
        _activeOrders.value = storageHelper.getNestedTransactions()
    }

    fun syncLocalActiveOrders() {
        _activeOrders.value = storageHelper.getNestedTransactions()
    }

    fun fetchActiveOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = com.example.data.api.RetrofitClient.getTransactionApiService(getApplication()).getTransactions()
                if (response.isSuccessful && response.body()?.data != null) {
                    // Berhasil konek, otomatis sinkronisasi data offline
                    storageHelper.syncUnsyncedData()
                    
                    val flatList = response.body()!!.data!!.toMutableList()
                    val localFlatList = storageHelper.getTransaksiList()
                    val unsyncedUpdates = storageHelper.getUnsyncedStatusUpdates().map { it.transactionId }
                    val unsyncedItemUpdates = storageHelper.getUnsyncedItemUpdates()

                    for (i in flatList.indices) {
                        val flatIt = flatList[i]
                        val lastUpdate = storageHelper.getLastStatusUpdateTime(flatIt.idTransaksi)
                        val hasUnsynced = unsyncedUpdates.contains(flatIt.idTransaksi)
                        
                        val localMatch = localFlatList.find { it.idTransaksi == flatIt.idTransaksi }
                        if (localMatch != null) {
                            if (localMatch.orderStatus == "COMPLETED" || hasUnsynced || System.currentTimeMillis() - lastUpdate < 15000) {
                                flatList[i] = flatList[i].copy(
                                    orderStatus = localMatch.orderStatus,
                                    metodePembayaran = localMatch.metodePembayaran,
                                    catatan = localMatch.catatan
                                )
                            }
                        }
                    }
                    val grouped = flatList.groupBy { it.idTransaksi }
                    val newTransactions = grouped.map { (idTx, items) ->
                        val firstItem = items.first()
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val timeMs = try { sdf.parse(firstItem.waktu)?.time ?: System.currentTimeMillis() } catch(e: Exception) { System.currentTimeMillis() }
                        
                        val transactionItems = items.map {
                            com.example.data.TransactionItem(
                                itemId = it.id,
                                namaBarang = it.namaItem,
                                qty = it.jumlah,
                                harga = it.harga.toLong(),
                                subTotal = (it.jumlah * it.harga).toLong(),
                                servedQty = 0
                            )
                        }
                        val total = transactionItems.sumOf { it.subTotal }
                        com.example.data.Transaction(
                            kodeTransaksi = idTx,
                            tanggalTransaksi = timeMs,
                            customerName = "", 
                            status = firstItem.orderStatus ?: "COMPLETED",
                            items = transactionItems,
                            totalHarga = total,
                            diskonPersen = 0.0,
                            diskonNominal = 0L,
                            totalSetelahDiskon = total
                        )
                    }

                    val localTransactions = storageHelper.getNestedTransactions().toMutableList()
                    var hasChanges = false

                    for (newTx in newTransactions) {
                        val index = localTransactions.indexOfFirst { it.kodeTransaksi == newTx.kodeTransaksi }
                        if (index != -1) {
                            val localTx = localTransactions[index]
                            var txChanged = false
                            
                            var resolvedStatus = newTx.status
                            if (localTx.status != newTx.status) {
                                val lastUpdate = storageHelper.getLastStatusUpdateTime(localTx.kodeTransaksi)
                                val hasUnsynced = unsyncedUpdates.contains(localTx.kodeTransaksi)
                                if (localTx.status == "COMPLETED" || hasUnsynced || System.currentTimeMillis() - lastUpdate < 15000) {
                                    // Ignore server status if we just updated it locally, it is unsynced, or already COMPLETED locally
                                    resolvedStatus = localTx.status
                                } else {
                                    txChanged = true
                                }
                            }
                            
                            val mergedItems = newTx.items.map { newIt ->
                                val localIt = localTx.items.find { it.itemId == newIt.itemId }
                                val lastItemUpdate = storageHelper.getLastItemUpdateTime(localTx.kodeTransaksi, newIt.itemId)
                                val hasUnsyncedItem = unsyncedItemUpdates.any { it.transactionId == localTx.kodeTransaksi && it.itemId == newIt.itemId }
                                
                                if (localIt != null && (hasUnsyncedItem || System.currentTimeMillis() - lastItemUpdate < 15000)) {
                                    localIt
                                } else {
                                    newIt.copy(servedQty = localIt?.servedQty ?: 0)
                                }
                            }
                            
                            val resolvedTotalHarga = if (localTx.items == mergedItems) localTx.totalHarga else mergedItems.sumOf { it.subTotal }
                            val resolvedTotalSetelahDiskon = if (localTx.items == mergedItems) localTx.totalSetelahDiskon else (resolvedTotalHarga - localTx.diskonNominal)
                            
                            if (localTx.items != mergedItems || localTx.totalHarga != resolvedTotalHarga) {
                                txChanged = true
                            }
                            if (txChanged) {
                                localTransactions[index] = localTx.copy(
                                    status = resolvedStatus,
                                    items = mergedItems,
                                    totalHarga = resolvedTotalHarga,
                                    totalSetelahDiskon = resolvedTotalSetelahDiskon
                                )
                                hasChanges = true
                            }
                        } else {
                            localTransactions.add(newTx)
                            hasChanges = true
                        }
                    }

                    // --- LOCAL DATA RESCUE ---
                    val serverTxIds = flatList.map { it.idTransaksi }.toSet()
                    val unsyncedTxIdsSet = storageHelper.getUnsyncedTransactions().mapNotNull { it.idTransaksi }.toSet()
                    val sdfTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    sdfTime.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val currentTime = System.currentTimeMillis()

                    val missingLocalFlat = localFlatList.filter { 
                        if (it.idTransaksi in serverTxIds) return@filter false
                        if (it.idTransaksi in unsyncedTxIdsSet) return@filter true
                        
                        try {
                            val timeMs = sdfTime.parse(it.waktu)?.time ?: 0L
                            currentTime - timeMs < 300000 // Keep if less than 5 minutes old
                        } catch (e: Exception) {
                            false
                        }
                    }
                    if (missingLocalFlat.isNotEmpty()) {
                        flatList.addAll(missingLocalFlat)
                    }
                    // -------------------------

                    if (hasChanges || flatList != localFlatList) {
                        storageHelper.saveTransaksiList(flatList)
                        storageHelper.saveNestedTransactions(localTransactions)
                        _activeOrders.value = localTransactions
                    } else if (_activeOrders.value != localTransactions) {
                        storageHelper.saveTransaksiList(flatList)
                        _activeOrders.value = localTransactions
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun startPollingActiveOrders() {
        viewModelScope.launch(Dispatchers.IO) {
            while(true) {
                fetchActiveOrders()
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    fun addItemToActiveOrder(
        kodeTransaksi: String, 
        newItem: TransactionItem, 
        onSuccess: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Prevent sending invalid product_id by setting it to null for custom/unknown items
                val isUnknownProduct = storageHelper.getMenuList().none { it.id == newItem.itemId }
                val request = com.example.data.AddTransactionItemRequest(
                    product_id = if (isUnknownProduct) null else newItem.itemId,
                    quantity = newItem.qty,
                    unit_price = newItem.harga.toDouble(),
                    subtotal = newItem.subTotal.toDouble()
                )
                val response = com.example.data.api.RetrofitClient.getTransactionApiService(getApplication())
                    .addTransactionItem(kodeTransaksi, request)
                    
                if (response.isSuccessful) {
                    // Update lokal jika perlu atau panggil fetchActiveOrders
                    fetchActiveOrders()
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    var errorMessage = "Gagal menambah item: ${response.message()}"
                    
                    try {
                        if (errorBodyString != null) {
                            val jsonObject = org.json.JSONObject(errorBodyString)
                            if (jsonObject.has("error")) {
                                val errors = jsonObject.getJSONArray("error")
                                if (errors.length() > 0) {
                                    errorMessage = errors.getString(0)
                                }
                            } else if (jsonObject.has("message")) {
                                errorMessage = jsonObject.getString("message")
                            }
                        }
                    } catch (e: Exception) {
                        // Abaikan jika bukan JSON
                    }
                    
                    launch(Dispatchers.Main) {
                        onError(errorMessage)
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    onError("Terjadi kesalahan koneksi: ${e.message}")
                }
            }
        }
    }

    fun removeItemFromActiveOrder(
        kodeTransaksi: String, 
        itemId: String, 
        onSuccess: () -> Unit, 
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = com.example.data.api.RetrofitClient.getTransactionApiService(getApplication())
                    .removeTransactionItem(kodeTransaksi, itemId)
                    
                if (response.isSuccessful) {
                    storageHelper.removeItemFromTransaction(kodeTransaksi, itemId)
                    fetchActiveOrders()
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                } else {
                    launch(Dispatchers.Main) {
                        onError("Gagal menghapus item: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) {
                    // Jika gagal koneksi (offline), terapkan secara lokal sementara?
                    // Karena ini penghapusan, jika gagal koneksi lebih baik tidak diizinkan untuk menghindari inkonsistensi
                    onError("Terjadi kesalahan koneksi: ${e.message}")
                }
            }
        }
    }

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

    fun formatReceipt(transaction: Transaction, paymentMethod: String? = null): String {
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
        if (paymentMethod != null) {
            sb.append("Pembayaran: $paymentMethod\n")
        }
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
