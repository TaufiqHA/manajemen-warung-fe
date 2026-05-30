package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
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
                tx.items.filter { it.itemId == itemId }.map {
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
