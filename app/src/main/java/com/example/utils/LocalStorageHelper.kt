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

    fun removeUnsyncedTransaction(idTransaksi: String) {
        val current = getUnsyncedTransactions().toMutableList()
        current.removeAll { it.idTransaksi == idTransaksi }
        saveUnsyncedTransactions(current)
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
        val list = getNestedTransactions().toMutableList()
        val index = list.indexOfFirst { it.kodeTransaksi == kodeTransaksi }
        if (index != -1) {
            list[index] = list[index].copy(status = newStatus)
            saveNestedTransactions(list)
        }
        
        // Update flat list for dashboard filtering
        val flatList = getTransaksiList().toMutableList()
        var flatUpdated = false
        for (i in flatList.indices) {
            if (flatList[i].idTransaksi == kodeTransaksi) {
                flatList[i] = flatList[i].copy(orderStatus = newStatus)
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
                    request = com.example.data.UpdateStatusRequest(status = newStatus)
                )
            } catch (e: Exception) {
                // Ignore for offline mode. Idealnya disimpan ke antrean offline 'unsynced_status_updates'
                android.util.Log.e("LocalStorageHelper", "Failed to sync status update to server", e)
            }
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
                    metodePembayaran = "CASH",
                    orderStatus = transaction.status
                )
                flatList.add(flatItem)
            }
            saveTransaksiList(flatList)
            
            // API Sync
            @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    com.example.data.api.RetrofitClient.getTransactionApiService(context).addTransactionItem(
                        transactionId = kodeTransaksi,
                        request = com.example.data.AddTransactionItemRequest(
                            product_id = newItem.itemId,
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

    fun addTransaction(transaction: Transaction, paymentMethod: String = "Cash") {
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
                catatan = "Via: $paymentMethod",
                metodePembayaran = paymentMethod,
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
                com.example.data.TransactionItemRequest(
                    namaItem = flatIt.namaItem,
                    jumlah = flatIt.jumlah,
                    harga = flatIt.harga,
                    catatan = flatIt.catatan,
                    servedQty = transaction.items.find { it.itemId == flatIt.id }?.servedQty ?: 0
                )
            }
            val request = com.example.data.TransactionRequest(
                idTransaksi = transaction.kodeTransaksi,
                waktu = timeStr,
                dicatatOleh = "Admin Toko",
                payment_method = paymentMethod.uppercase(),
                customerName = transaction.customerName,
                orderStatus = transaction.status,
                items = apiItems
            )
            
            try {
                com.example.data.api.RetrofitClient.getTransactionApiService(context).createTransaction(request)
            } catch (e: java.lang.Exception) {
                // Jika gagal (misal tidak ada koneksi), simpan ke antrean lokal
                addUnsyncedTransaction(request)
            }
        }
    }

    fun syncUnsyncedData() {
        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val unsyncedList = getUnsyncedTransactions()
            if (unsyncedList.isEmpty()) return@launch

            val apiService = com.example.data.api.RetrofitClient.getTransactionApiService(context)
            
            for (request in unsyncedList) {
                try {
                    // Coba kirim ulang ke server
                    apiService.createTransaction(request)
                    // Jika berhasil (tidak ada exception), hapus dari antrean lokal
                    request.idTransaksi?.let { removeUnsyncedTransaction(it) }
                } catch (e: Exception) {
                    // Jika masih gagal, biarkan saja di dalam antrean untuk percobaan berikutnya
                }
            }
        }
    }

    fun updateItemQuantity(kodeTransaksi: String, itemId: String, newQty: Int) {
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
