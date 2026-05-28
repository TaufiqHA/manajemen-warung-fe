package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

class ReportViewModel : ViewModel() {

    // Filter yang dipilih user
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1) 
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _selectedItemId = MutableStateFlow<String?>(null)
    val selectedItemId = _selectedItemId.asStateFlow()

    // Data semua barang untuk dropdown
    val allItems = StateFlow<List<Item>>(
        MutableStateFlow(listOf(
            Item("1", "Indomie Goreng", 3000),
            Item("2", "Kopi Kapal Api", 1500),
            Item("3", "Beras 5kg", 65000),
            Item("4", "Gula Pasir 1kg", 16000),
            Item("5", "Telur Ayam 1kg", 28000)
        )).asStateFlow()
    )

    // Data semua transaksi (Mockup)
    private val _allTransactions = MutableStateFlow<List<Transaction>>(generateMockTransactions())

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

    private fun generateMockTransactions(): List<Transaction> {
        val transactions = mutableListOf<Transaction>()
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        
        // Generate transactions for the last 30 days
        for (i in 0 until 60) {
            calendar.set(Calendar.DAY_OF_MONTH, (i % 28) + 1)
            // Some in current month, some in previous
            if (i > 30) calendar.set(Calendar.MONTH, currentMonth - 1)
            
            val items = listOf(
                TransactionItem("1", "Indomie Goreng", (1..5).random(), 3000),
                TransactionItem("2", "Kopi Kapal Api", (1..3).random(), 1500)
            )
            val total = items.sumOf { it.subTotal }
            
            transactions.add(
                Transaction(
                    kodeTransaksi = "TRX-MOCK-$i",
                    tanggalTransaksi = calendar.timeInMillis,
                    items = items,
                    totalHarga = total
                )
            )
        }
        return transactions
    }
}
