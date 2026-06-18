package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.util.Calendar

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val storageHelper = com.example.utils.LocalStorageHelper(application)

    // Filter yang dipilih user
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) 
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId = _selectedItemId.asStateFlow()

    // Data semua barang untuk dropdown
    private val _allItems = MutableStateFlow<List<Item>>(emptyList())
    val allItems = _allItems.asStateFlow()

    // Data semua transaksi
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())

    init {
        loadData()
    }

    fun loadData() {
        val menuItems = storageHelper.getMenuList()
        _allItems.value = menuItems.map { Item(it.id, it.nama, it.harga.toLong()) }
        _allTransactions.value = storageHelper.getNestedTransactions()

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pResponse = com.example.data.api.RetrofitClient.getProductApiService(getApplication()).getProducts()
                if (pResponse.isSuccessful && pResponse.body()?.data != null) {
                    val apiItems = pResponse.body()!!.data!!
                    storageHelper.saveMenuList(apiItems)
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        _allItems.value = apiItems.map { Item(it.id, it.nama, it.harga.toLong()) }
                    }
                }
            } catch (e: Exception) {}

            try {
                val tResponse = com.example.data.api.RetrofitClient.getTransactionApiService(getApplication()).getTransactions()
                if (tResponse.isSuccessful && tResponse.body()?.data != null) {
                    val apiFlatTrx = tResponse.body()!!.data!!.toMutableList()
                    val localFlatList = storageHelper.getTransaksiList()
                    val unsyncedUpdates = storageHelper.getUnsyncedStatusUpdates().map { it.transactionId }
                    for (i in apiFlatTrx.indices) {
                        val flatIt = apiFlatTrx[i]
                        val lastUpdate = storageHelper.getLastStatusUpdateTime(flatIt.idTransaksi)
                        val hasUnsynced = unsyncedUpdates.contains(flatIt.idTransaksi)
                        val localMatch = localFlatList.find { it.idTransaksi == flatIt.idTransaksi }
                        if (localMatch != null) {
                            if (localMatch.orderStatus == "COMPLETED" || hasUnsynced || System.currentTimeMillis() - lastUpdate < 15000) {
                                apiFlatTrx[i] = apiFlatTrx[i].copy(
                                    orderStatus = localMatch.orderStatus,
                                    metodePembayaran = localMatch.metodePembayaran,
                                    catatan = localMatch.catatan
                                )
                            }
                        }
                    }

                    val serverTxIds = apiFlatTrx.map { it.idTransaksi }.toSet()
                    val unsyncedTxIdsSet = storageHelper.getUnsyncedTransactions().mapNotNull { it.idTransaksi }.toSet()
                    val sdfTime = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    sdfTime.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val currentTime = System.currentTimeMillis()

                    val missingLocalFlat = localFlatList.filter { 
                        if (it.idTransaksi in serverTxIds) return@filter false
                        if (it.idTransaksi in unsyncedTxIdsSet) return@filter true
                        try {
                            val timeMs = sdfTime.parse(it.waktu)?.time ?: 0L
                            currentTime - timeMs < 300000
                        } catch (e: Exception) { false }
                    }
                    if (missingLocalFlat.isNotEmpty()) {
                        apiFlatTrx.addAll(missingLocalFlat)
                    }

                    storageHelper.saveTransaksiList(apiFlatTrx)
                    
                    val grouped = apiFlatTrx.groupBy { it.idTransaksi }
                    val transactions = grouped.map { (trxId, items) ->
                        val firstItem = items.firstOrNull()
                        val formatter = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
                        val dateLong = try {
                            val cleanId = trxId.substringAfter("TRX-")
                            // Try parsing with new format, or fallback to old if needed
                            formatter.parse(cleanId)?.time ?: System.currentTimeMillis()
                        } catch (e: Exception) {
                            try {
                                val oldFormatter = java.text.SimpleDateFormat("yyyyMMdd-HHmmss", java.util.Locale.getDefault())
                                val cleanId = trxId.substringAfter("TRX-")
                                oldFormatter.parse(cleanId)?.time ?: System.currentTimeMillis()
                            } catch (e2: Exception) {
                                System.currentTimeMillis()
                            }
                        }
                        
                        val trxItems = items.map {
                            TransactionItem(
                                itemId = it.id,
                                namaBarang = it.namaItem,
                                qty = it.jumlah,
                                harga = it.harga.toLong(),
                                subTotal = (it.jumlah * it.harga).toLong()
                            )
                        }
                        
                        Transaction(
                            kodeTransaksi = trxId,
                            tanggalTransaksi = dateLong,
                            items = trxItems,
                            totalHarga = trxItems.sumOf { it.subTotal }
                        )
                    }
                    storageHelper.saveNestedTransactions(transactions)
                    launch(kotlinx.coroutines.Dispatchers.Main) {
                        _allTransactions.value = transactions
                    }
                }
            } catch (e: Exception) {}
        }
    }

    // Logika Inti: Filter berdasarkan bulan & item, lalu group berdasarkan tanggal
    val monthlyItemReport: StateFlow<List<DailyItemReport>> = combine(
        _allTransactions, _selectedMonth, _selectedItemId
    ) { transactions, month, itemId ->
        
        if (itemId == null) return@combine emptyList()

        val calendar = Calendar.getInstance()

        // 1. Ambil semua item dari transaksi yang sesuai bulan dan itemId
        val dayAndAmountList = transactions.flatMap { tx ->
            calendar.timeInMillis = tx.tanggalTransaksi
            val txMonth = calendar.get(Calendar.MONTH) + 1
            
            if (txMonth == month) {
                tx.items.filter { it.itemId == itemId && !it.namaBarang.contains("[BATAL]", ignoreCase = true) }.map {
                    calendar.get(Calendar.DAY_OF_MONTH) to it.subTotal.toDouble()
                }
            } else {
                emptyList()
            }
        }

        // 2. Grouping berdasarkan tanggal (Day of Month) dan jumlahkan
        val grouped = dayAndAmountList.groupBy({ it.first }, { it.second })

        // 3. Ubah map hasil grouping menjadi list DailyItemReport dan urutkan berdasarkan tanggal
        grouped.map { (day, amounts) ->
            DailyItemReport(
                date = day,
                totalAmount = amounts.sum()
            )
        }.sortedBy { it.date }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Fungsi untuk UI memperbarui filter
    fun setMonth(month: Int) { _selectedMonth.value = month }
    fun setItem(id: String) { _selectedItemId.value = id }
}
