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

class LocalStorageHelper(context: Context) {
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

    fun addTransaction(transaction: Transaction) {
        // 1. Add to nested transactions
        val currentNested = getNestedTransactions().toMutableList()
        currentNested.add(transaction)
        saveNestedTransactions(currentNested)

        // 2. Add to flat TransaksiHarian list for Dashboard/LabaRugi
        val currentFlat = getTransaksiList().toMutableList()
        val formatter = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val timeStr = formatter.format(java.util.Date(transaction.tanggalTransaksi))
        
        transaction.items.forEach { item ->
            currentFlat.add(
                TransaksiHarian(
                    idTransaksi = transaction.kodeTransaksi,
                    id = item.itemId,
                    namaItem = item.namaBarang,
                    jumlah = item.qty,
                    harga = item.harga.toDouble(),
                    waktu = timeStr,
                    dicatatOleh = "Admin Toko",
                    catatan = ""
                )
            )
        }
        saveTransaksiList(currentFlat)
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
