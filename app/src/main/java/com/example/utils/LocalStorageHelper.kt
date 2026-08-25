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

    fun addTransaction(transaction: Transaction, paymentMethod: String = "Cash") {
        // Daftarkan sebagai orderan yang sedang aktif/berjalan
        addActiveOrderId(transaction.kodeTransaksi)

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
                metodePembayaran = paymentMethod
            )
            currentFlat.add(flatItem)
            newFlatItems.add(flatItem)
        }
        saveTransaksiList(currentFlat)

        // 3. Post to API in background (dengan antrean offline)
        @kotlin.OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val apiItems = newFlatItems.map { 
                com.example.data.TransactionItemRequest(
                    namaItem = it.namaItem,
                    jumlah = it.jumlah,
                    harga = it.harga,
                    catatan = it.catatan
                )
            }
            val request = com.example.data.TransactionRequest(
                idTransaksi = transaction.kodeTransaksi,
                waktu = timeStr,
                dicatatOleh = "Admin Toko",
                payment_method = paymentMethod.uppercase(),
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

    fun getActiveOrderIds(): Set<String> {
        return prefs.getStringSet("active_order_ids", emptySet()) ?: emptySet()
    }

    fun addActiveOrderId(trxId: String) {
        val current = getActiveOrderIds().toMutableSet()
        current.add(trxId)
        prefs.edit().putStringSet("active_order_ids", current).apply()
    }

    fun removeActiveOrderId(trxId: String) {
        val current = getActiveOrderIds().toMutableSet()
        current.remove(trxId)
        prefs.edit().putStringSet("active_order_ids", current).apply()
    }

    fun getCompletedOrderIds(): Set<String> {
        return prefs.getStringSet("completed_order_ids", emptySet()) ?: emptySet()
    }

    fun markOrderCompleted(trxId: String) {
        val current = getCompletedOrderIds().toMutableSet()
        current.add(trxId)
        prefs.edit().putStringSet("completed_order_ids", current).apply()
        removeActiveOrderId(trxId)
    }

    fun getOrderItemProgress(key: String): Int {
        return prefs.getInt("item_progress_$key", 0)
    }

    fun setOrderItemProgress(key: String, qty: Int) {
        prefs.edit().putInt("item_progress_$key", qty).apply()
    }

    fun getCompletedOrderItemKeys(): Set<String> {
        return prefs.getStringSet("completed_order_item_keys", emptySet()) ?: emptySet()
    }

    fun setOrderItemCompleted(key: String, isCompleted: Boolean) {
        val current = getCompletedOrderItemKeys().toMutableSet()
        if (isCompleted) {
            current.add(key)
        } else {
            current.remove(key)
        }
        prefs.edit().putStringSet("completed_order_item_keys", current).apply()
    }

    fun setOrderAllItemsCompleted(keys: List<String>, isCompleted: Boolean) {
        val current = getCompletedOrderItemKeys().toMutableSet()
        if (isCompleted) {
            current.addAll(keys)
        } else {
            current.removeAll(keys)
        }
        prefs.edit().putStringSet("completed_order_item_keys", current).apply()
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
