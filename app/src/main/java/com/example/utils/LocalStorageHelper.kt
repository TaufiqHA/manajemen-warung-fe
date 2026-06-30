package com.example.utils

import android.content.Context
import com.example.ui.screens.MenuItem
import com.example.ui.screens.TransaksiHarian
import com.example.ui.screens.BiayaOperasional
import com.example.data.Transaction
import com.example.data.Item
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

import kotlinx.coroutines.launch

class LocalStorageHelper(private val context: Context) {
    private val prefs = context.getSharedPreferences("warung_prototype_data_prefs", Context.MODE_PRIVATE)
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    // MenuItem Adapters
    private val menuItemListType = Types.newParameterizedType(List::class.java, MenuItem::class.java)
    private val menuItemListAdapter = moshi.adapter<List<MenuItem>>(menuItemListType)

    // TransaksiHarian Adapters
    private val transaksiListType = Types.newParameterizedType(List::class.java, TransaksiHarian::class.java)
    private val transaksiListAdapter = moshi.adapter<List<TransaksiHarian>>(transaksiListType)

    // BiayaOperasional Adapters
    private val biayaListType = Types.newParameterizedType(List::class.java, BiayaOperasional::class.java)
    private val biayaListAdapter = moshi.adapter<List<BiayaOperasional>>(biayaListType)

    // Transaction Adapters (nested history)
    private val nestedTrxListType = Types.newParameterizedType(List::class.java, Transaction::class.java)
    private val nestedTrxListAdapter = moshi.adapter<List<Transaction>>(nestedTrxListType)

    // Unsynced Transactions Adapter
    private val unsyncedTrxListType = Types.newParameterizedType(List::class.java, com.example.data.TransactionRequest::class.java)
    private val unsyncedTrxListAdapter = moshi.adapter<List<com.example.data.TransactionRequest>>(unsyncedTrxListType)

    private val unsyncedStatusListType = Types.newParameterizedType(List::class.java, com.example.data.UnsyncedStatusUpdate::class.java)
    private val unsyncedStatusListAdapter = moshi.adapter<List<com.example.data.UnsyncedStatusUpdate>>(unsyncedStatusListType)

    private val unsyncedItemUpdateListType = Types.newParameterizedType(List::class.java, com.example.data.UnsyncedItemUpdate::class.java)
    private val unsyncedItemUpdateListAdapter = moshi.adapter<List<com.example.data.UnsyncedItemUpdate>>(unsyncedItemUpdateListType)

    private val lastStatusUpdateMap = mutableMapOf<String, Long>()
    private val lastItemUpdateMap = mutableMapOf<String, Long>()

    fun getUnsyncedTransactions(): List<com.example.data.TransactionRequest> {
        val json = prefs.getString("unsynced_transactions", null)
        return if (json != null) {
            try {
                unsyncedTrxListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun getLastStatusUpdateTime(kodeTransaksi: String): Long {
        return lastStatusUpdateMap[kodeTransaksi] ?: 0L
    }

    fun saveUnsyncedTransactions(list: List<com.example.data.TransactionRequest>) {
        val json = unsyncedTrxListAdapter.toJson(list)
        prefs.edit().putString("unsynced_transactions", json).apply()
    }

    fun addUnsyncedTransaction(request: com.example.data.TransactionRequest) {
        val current = getUnsyncedTransactions().toMutableList()
        // Hindari duplikasi berdasarkan idTransaksi
        if (current.none { it.idTransaksi == request.idTransaksi }) {
            current.add(request)
            saveUnsyncedTransactions(current)
        }
    }

    fun removeUnsyncedTransaction(transactionId: String) {
        val list = getUnsyncedTransactions().toMutableList()
        list.removeAll { it.idTransaksi == transactionId }
        saveUnsyncedTransactions(list)
    }

    fun getUnsyncedStatusUpdates(): List<com.example.data.UnsyncedStatusUpdate> {
        val json = prefs.getString("unsynced_status_updates", null)
        return if (json != null) {
            try {
                unsyncedStatusListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveUnsyncedStatusUpdates(list: List<com.example.data.UnsyncedStatusUpdate>) {
        val json = unsyncedStatusListAdapter.toJson(list)
        prefs.edit().putString("unsynced_status_updates", json).apply()
    }

    fun addUnsyncedStatusUpdate(update: com.example.data.UnsyncedStatusUpdate) {
        val list = getUnsyncedStatusUpdates().toMutableList()
        list.removeAll { it.transactionId == update.transactionId }
        list.add(update)
        saveUnsyncedStatusUpdates(list)
    }

    fun removeUnsyncedStatusUpdate(transactionId: String) {
        val list = getUnsyncedStatusUpdates().toMutableList()
        list.removeAll { it.transactionId == transactionId }
        saveUnsyncedStatusUpdates(list)
    }

    fun getUnsyncedItemUpdates(): List<com.example.data.UnsyncedItemUpdate> {
        val json = prefs.getString("unsynced_item_updates", null)
        return if (json != null) {
            try {
                unsyncedItemUpdateListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun saveUnsyncedItemUpdates(list: List<com.example.data.UnsyncedItemUpdate>) {
        val json = unsyncedItemUpdateListAdapter.toJson(list)
        prefs.edit().putString("unsynced_item_updates", json).apply()
    }

    fun addUnsyncedItemUpdate(update: com.example.data.UnsyncedItemUpdate) {
        val list = getUnsyncedItemUpdates().toMutableList()
        list.removeAll { it.transactionId == update.transactionId && it.itemId == update.itemId }
        list.add(update)
        saveUnsyncedItemUpdates(list)
    }

    fun removeUnsyncedItemUpdate(transactionId: String, itemId: String) {
        val list = getUnsyncedItemUpdates().toMutableList()
        list.removeAll { it.transactionId == transactionId && it.itemId == itemId }
        saveUnsyncedItemUpdates(list)
    }

    fun getLastItemUpdateTime(kodeTransaksi: String, itemId: String): Long {
        return lastItemUpdateMap["${kodeTransaksi}_${itemId}"] ?: 0L
    }

    fun getMenuList(): List<MenuItem> {
        val json = prefs.getString("menu_list", null)
        return if (json != null) {
            try {
                menuItemListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                getDefaultMenuList()
            }
        } else {
            val defaultList = getDefaultMenuList()
            saveMenuList(defaultList)
            defaultList
        }
    }

    fun saveMenuList(list: List<MenuItem>) {
        val json = menuItemListAdapter.toJson(list)
        prefs.edit().putString("menu_list", json).apply()
    }

    fun getTransaksiList(): List<TransaksiHarian> {
        val json = prefs.getString("transaksi_list", null)
        return if (json != null) {
            try {
                transaksiListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                getDefaultTransaksiList()
            }
        } else {
            val defaultList = getDefaultTransaksiList()
            saveTransaksiList(defaultList)
            defaultList
        }
    }

    fun saveTransaksiList(list: List<TransaksiHarian>) {
        val json = transaksiListAdapter.toJson(list)
        prefs.edit().putString("transaksi_list", json).apply()
    }

    fun getBiayaList(): List<BiayaOperasional> {
        val json = prefs.getString("biaya_list", null)
        return if (json != null) {
            try {
                biayaListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                getDefaultBiayaList()
            }
        } else {
            val defaultList = getDefaultBiayaList()
            saveBiayaList(defaultList)
            defaultList
        }
    }

    fun saveBiayaList(list: List<BiayaOperasional>) {
        val json = biayaListAdapter.toJson(list)
        prefs.edit().putString("biaya_list", json).apply()
    }

    fun getNestedTransactions(): List<Transaction> {
        val json = prefs.getString("nested_transaction_history", null)
        return if (json != null) {
            try {
                nestedTrxListAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                getDefaultNestedTransactions()
            }
        } else {
            val defaultList = getDefaultNestedTransactions()
            saveNestedTransactions(defaultList)
            defaultList
        }
    }

    fun saveNestedTransactions(list: List<Transaction>) {
        val json = nestedTrxListAdapter.toJson(list)
        prefs.edit().putString("nested_transaction_history", json).apply()
    }

    fun updateTransactionStatus(kodeTransaksi: String, newStatus: String) {
        lastStatusUpdateMap[kodeTransaksi] = System.currentTimeMillis()
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            list[index] = list[index].copy(status = newStatus)
            saveNestedTransactions(list)
        }
        
        // Update flat list for dashboard filtering
        val flatList = getTransaksiList().toMutableList()
        var flatUpdated = false
        var currentPaymentMethod: String? = null
        for (i in flatList.indices) {
            if (flatList[i].idTransaksi == kodeTransaksi) {
                flatList[i] = flatList[i].copy(orderStatus = newStatus)
                currentPaymentMethod = flatList[i].metodePembayaran
                flatUpdated = true
            }
        }
        if (flatUpdated) saveTransaksiList(flatList)

        // 3. Post status update to API in background
        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                com.example.data.api.RetrofitClient.getTransactionApiService(context).updateTransactionStatus(
                    transactionId = kodeTransaksi,
                    request = com.example.data.UpdateStatusRequest(
                        status = newStatus,
                        payment_method = currentPaymentMethod
                    )
                )
                removeUnsyncedStatusUpdate(kodeTransaksi)
            } catch (e: Exception) {
                addUnsyncedStatusUpdate(com.example.data.UnsyncedStatusUpdate(kodeTransaksi, newStatus))
                android.util.Log.e("LocalStorageHelper", "Failed to sync status update to server, queued for offline", e)
            }
        }
    }

    fun updateTransactionPaymentMethod(kodeTransaksi: String, newPaymentMethod: String) {
        val flatList = getTransaksiList().toMutableList()
        var flatUpdated = false
        for (i in flatList.indices) {
            if (flatList[i].idTransaksi == kodeTransaksi) {
                val newCatatan = if (newPaymentMethod == "BELUM LUNAS") "" else "Via: $newPaymentMethod"
                flatList[i] = flatList[i].copy(
                    payment_method = newPaymentMethod,
                    paymentMethod = newPaymentMethod,
                    metode_pembayaran = newPaymentMethod,
                    metodePembayaranRaw = newPaymentMethod,
                    catatan = newCatatan
                )
                flatUpdated = true
            }
        }
        if (flatUpdated) saveTransaksiList(flatList)
    }

    fun updateTransactionDiscount(kodeTransaksi: String, diskonPersen: Double, diskonNominal: Long, totalSetelahDiskon: Long) {
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            val transaction = list[index]
            list[index] = transaction.copy(
                diskonPersen = diskonPersen,
                diskonNominal = diskonNominal,
                totalSetelahDiskon = totalSetelahDiskon
            )
            saveNestedTransactions(list)
        }
    }

    fun updateItemServedQty(kodeTransaksi: String, itemId: String, newServedQty: Int) {
        val list = getNestedTransactions().toMutableList()
        val txIndex = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (txIndex != -1) {
            val transaction = list[txIndex]
            val newItems = transaction.items.toMutableList()
            val itemIndex = newItems.indexOfFirst { it.itemId == itemId }
            if (itemIndex != -1) {
                newItems[itemIndex] = newItems[itemIndex].copy(servedQty = newServedQty)
                list[txIndex] = transaction.copy(items = newItems)
                saveNestedTransactions(list)
            }
        }
    }

    fun addItemsToTransaction(kodeTransaksi: String, newItem: com.example.data.TransactionItem) {
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            val transaction = list[index]
            val currentItems = transaction.items.toMutableList()
            
            // Check if item already exists
            val existingItemIndex = currentItems.indexOfFirst { it.itemId == newItem.itemId }
            if (existingItemIndex != -1) {
                val existingItem = currentItems[existingItemIndex]
                currentItems[existingItemIndex] = existingItem.copy(
                    qty = existingItem.qty + newItem.qty,
                    subTotal = existingItem.subTotal + newItem.subTotal
                )
            } else {
                currentItems.add(newItem)
            }
            
            // Recalculate totals
            val newTotalHarga = currentItems.sumOf { it.subTotal }
            val newDiskonNominal = (newTotalHarga * transaction.diskonPersen / 100).toLong()
            val newTotalSetelahDiskon = newTotalHarga - newDiskonNominal
            
            list[index] = transaction.copy(
                items = currentItems,
                totalHarga = newTotalHarga,
                diskonNominal = newDiskonNominal,
                totalSetelahDiskon = newTotalSetelahDiskon
            )
            saveNestedTransactions(list)
            
            // Update flat list
            val flatList = getTransaksiList().toMutableList()
            val apiFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
            apiFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val timeStr = apiFormatter.format(java.util.Date(transaction.tanggalTransaksi))
            
            flatList.removeAll { it.idTransaksi == kodeTransaksi }
            currentItems.forEach { item ->
                val flatItem = TransaksiHarian(
                    idTransaksi = transaction.kodeTransaksi,
                    id = item.itemId,
                    namaItem = item.namaBarang,
                    jumlah = item.qty,
                    harga = item.harga.toDouble(),
                    waktu = timeStr,
                    dicatatOleh = "Admin Toko",
                    catatan = "Via: Tambahan",
                    payment_method = "CASH",
                    paymentMethod = "CASH",
                    metode_pembayaran = "CASH",
                    metodePembayaranRaw = "CASH",
                    orderStatus = transaction.status
                )
                flatList.add(flatItem)
            }
            saveTransaksiList(flatList)
            
            // API Sync
            @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    // Prevent sending invalid product_id by setting it to null for custom/unknown items
                    val isUnknownProduct = getMenuList().none { it.id == newItem.itemId }
                    com.example.data.api.RetrofitClient.getTransactionApiService(context).addTransactionItem(
                        transactionId = kodeTransaksi,
                        request = com.example.data.AddTransactionItemRequest(
                            product_id = if (isUnknownProduct) null else newItem.itemId,
                            quantity = newItem.qty,
                            unit_price = newItem.harga.toDouble(),
                            subtotal = newItem.subTotal.toDouble()
                        )
                    )
                } catch (e: Exception) {
                    android.util.Log.e("LocalStorageHelper", "Failed to sync add item", e)
                }
            }
        }
    }

    fun addTransaction(transaction: Transaction, paymentMethod: String = "CASH") {
        // 1. Add to nested transactions
        val currentNested = getNestedTransactions().toMutableList()
        currentNested.add(transaction)
        saveNestedTransactions(currentNested)

        // 2. Add to flat TransaksiHarian list for Dashboard/LabaRugi
        val currentFlat = getTransaksiList().toMutableList()
        val apiFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
        apiFormatter.timeZone = java.util.TimeZone.getTimeZone("UTC")
        val timeStr = apiFormatter.format(java.util.Date(transaction.tanggalTransaksi))
        
        val newFlatItems = mutableListOf<TransaksiHarian>()
        transaction.items.forEach { item ->
            val flatItem = TransaksiHarian(
                idTransaksi = transaction.kodeTransaksi,
                id = item.itemId,
                namaItem = item.namaBarang,
                jumlah = item.qty,
                harga = item.harga.toDouble(),
                waktu = timeStr,
                dicatatOleh = "Admin Toko",
                catatan = if (paymentMethod == "BELUM LUNAS") "" else "Via: $paymentMethod",
                payment_method = paymentMethod,
                paymentMethod = paymentMethod,
                metode_pembayaran = paymentMethod,
                metodePembayaranRaw = paymentMethod,
                orderStatus = transaction.status
            )
            currentFlat.add(flatItem)
            newFlatItems.add(flatItem)
        }
        saveTransaksiList(currentFlat)

        // 3. Post to API in background (dengan antrean offline)
        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apiItems = newFlatItems.map { flatIt ->
                val isUnknownProduct = getMenuList().none { it.id == flatIt.id }
                val rawProductId = if (isUnknownProduct) null else flatIt.id.removePrefix("PRD-").toIntOrNull()?.toString()
                com.example.data.TransactionItemRequest(
                    namaItem = flatIt.namaItem,
                    jumlah = flatIt.jumlah,
                    harga = flatIt.harga,
                    catatan = flatIt.catatan,
                    servedQty = transaction.items.find { it.itemId == flatIt.id }?.servedQty ?: 0,
                    product_id = rawProductId,
                    quantity = flatIt.jumlah,
                    unit_price = flatIt.harga,
                    subtotal = flatIt.harga * flatIt.jumlah
                )
            }
            val pm = paymentMethod.uppercase()
            val validPaymentMethod = if (pm == "BELUM LUNAS") null else pm
            val request = com.example.data.TransactionRequest(
                idTransaksi = transaction.kodeTransaksi,
                waktu = timeStr,
                dicatatOleh = "Admin Toko",
                payment_method = validPaymentMethod,
                customerName = transaction.customerName,
                orderStatus = transaction.status,
                status = transaction.status,
                items = apiItems,
                discountAmount = transaction.diskonNominal
            )
            
            try {
                val response = com.example.data.api.RetrofitClient.getTransactionApiService(context).createTransaction(request)
                if (!response.isSuccessful) {
                    addUnsyncedTransaction(request)
                    val err = response.errorBody()?.string() ?: "Unknown error"
                    android.util.Log.e("LocalStorageHelper", "API CreateTx Error: $err")
                    // Tampilkan error ke layar agar user bisa melihatnya
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        android.widget.Toast.makeText(context, "Gagal upload: $err", android.widget.Toast.LENGTH_LONG).show()
                    }
                    // Dump error to file for debugging
                    try {
                        val file = java.io.File(context.filesDir, "api_error.txt")
                        file.writeText("Payload: $request\nError: $err")
                    } catch (e: Exception) {}
                }
            } catch (e: java.lang.Exception) {
                // Jika gagal (misal tidak ada koneksi), simpan ke antrean lokal
                addUnsyncedTransaction(request)
            }
        }
    }

    fun syncUnsyncedData() {
        val unsynced = getUnsyncedTransactions()
        val unsyncedStatus = getUnsyncedStatusUpdates()
        val unsyncedItems = getUnsyncedItemUpdates()
        if (unsynced.isEmpty() && unsyncedStatus.isEmpty() && unsyncedItems.isEmpty()) return

        val apiService = com.example.data.api.RetrofitClient.getTransactionApiService(context)
        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            for (request in unsynced) {
                try {
                    // Coba kirim ulang ke server
                    apiService.createTransaction(request)
                    // Jika berhasil (tidak ada exception), hapus dari antrean lokal
                    request.idTransaksi?.let { removeUnsyncedTransaction(it) }
                } catch (e: Exception) {
                    // Jika masih gagal, biarkan saja di dalam antrean untuk percobaan berikutnya
                }
            }
            
            val flatList = getTransaksiList()
            for (statusUpdate in unsyncedStatus) {
                try {
                    val currentPaymentMethod = flatList.find { it.idTransaksi == statusUpdate.transactionId }?.metodePembayaran
                    apiService.updateTransactionStatus(
                        transactionId = statusUpdate.transactionId,
                        request = com.example.data.UpdateStatusRequest(
                            status = statusUpdate.status,
                            payment_method = currentPaymentMethod
                        )
                    )
                    removeUnsyncedStatusUpdate(statusUpdate.transactionId)
                } catch (e: Exception) {
                    // Jika gagal, biarkan di antrean
                }
            }
            
            for (itemUpdate in unsyncedItems) {
                try {
                    apiService.updateTransactionItem(
                        transactionId = itemUpdate.transactionId,
                        itemId = itemUpdate.itemId,
                        request = com.example.data.UpdateTransactionItemRequest(
                            quantity = itemUpdate.quantity,
                            subtotal = itemUpdate.subtotal
                        )
                    )
                    removeUnsyncedItemUpdate(itemUpdate.transactionId, itemUpdate.itemId)
                } catch (e: Exception) {
                    // Biarkan di antrean
                }
            }
        }
    }

    fun updateItemQuantity(kodeTransaksi: String, itemId: String, newQty: Int) {
        lastItemUpdateMap["${kodeTransaksi}_${itemId}"] = System.currentTimeMillis()
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            val transaction = list[index]
            val currentItems = transaction.items.toMutableList()
            
            val itemIndex = currentItems.indexOfFirst { it.itemId == itemId }
            if (itemIndex != -1) {
                val item = currentItems[itemIndex]
                val newSubTotal = newQty * item.harga
                currentItems[itemIndex] = item.copy(qty = newQty, subTotal = newSubTotal)
                
                // Recalculate totals
                val newTotalHarga = currentItems.sumOf { it.subTotal }
                val newDiskonNominal = (newTotalHarga * transaction.diskonPersen / 100).toLong()
                val newTotalSetelahDiskon = newTotalHarga - newDiskonNominal
                
                list[index] = transaction.copy(
                    items = currentItems,
                    totalHarga = newTotalHarga,
                    diskonNominal = newDiskonNominal,
                    totalSetelahDiskon = newTotalSetelahDiskon
                )
                saveNestedTransactions(list)
                
                // Update flat list
                val flatList = getTransaksiList().toMutableList()
                val flatIndex = flatList.indexOfFirst { it.idTransaksi == kodeTransaksi && it.id == itemId }
                if (flatIndex != -1) {
                    flatList[flatIndex] = flatList[flatIndex].copy(jumlah = newQty)
                    saveTransaksiList(flatList)
                }

                // Sync API in background
                @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
                kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        com.example.data.api.RetrofitClient.getTransactionApiService(context).updateTransactionItem(
                            transactionId = kodeTransaksi,
                            itemId = itemId,
                            request = com.example.data.UpdateTransactionItemRequest(
                                quantity = newQty,
                                subtotal = newSubTotal.toDouble()
                            )
                        )
                        removeUnsyncedItemUpdate(kodeTransaksi, itemId)
                    } catch (e: Exception) {
                        addUnsyncedItemUpdate(com.example.data.UnsyncedItemUpdate(kodeTransaksi, itemId, newQty, newSubTotal.toDouble()))
                        android.util.Log.e("LocalStorageHelper", "Failed to sync item update, queued for offline", e)
                    }
                }
            }
        }
    }

    fun removeItemFromTransaction(kodeTransaksi: String, itemId: String) {
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            val transaction = list[index]
            val currentItems = transaction.items.toMutableList()
            
            val itemIndex = currentItems.indexOfFirst { it.itemId == itemId }
            if (itemIndex != -1) {
                currentItems.removeAt(itemIndex)
                
                if (currentItems.isEmpty()) {
                    // Jika pesanan jadi kosong, bisa ubah status jadi CANCELLED atau hapus
                    list[index] = transaction.copy(status = "CANCELLED", items = emptyList(), totalHarga = 0, totalSetelahDiskon = 0)
                } else {
                    // Recalculate totals
                    val newTotalHarga = currentItems.sumOf { it.subTotal }
                    val newDiskonNominal = (newTotalHarga * transaction.diskonPersen / 100).toLong()
                    val newTotalSetelahDiskon = newTotalHarga - newDiskonNominal
                    
                    list[index] = transaction.copy(
                        items = currentItems,
                        totalHarga = newTotalHarga,
                        diskonNominal = newDiskonNominal,
                        totalSetelahDiskon = newTotalSetelahDiskon
                    )
                }
                saveNestedTransactions(list)
                
                // Update flat list
                val flatList = getTransaksiList().toMutableList()
                val flatIndex = flatList.indexOfFirst { it.idTransaksi == kodeTransaksi && it.id == itemId }
                if (flatIndex != -1) {
                    flatList.removeAt(flatIndex)
                    saveTransaksiList(flatList)
                }
            }
        }
    }

    private fun getDefaultMenuList(): List<MenuItem> {
        return emptyList()
    }

    private fun getDefaultTransaksiList(): List<TransaksiHarian> {
        return emptyList()
    }

    private fun getDefaultBiayaList(): List<BiayaOperasional> {
        return emptyList()
    }

    private fun getDefaultNestedTransactions(): List<Transaction> {
        return emptyList()
    }
}
