package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.RetrofitClient
import com.example.ui.screens.BiayaOperasional
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BiayaViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _biayaList = MutableStateFlow<List<BiayaOperasional>>(emptyList())
    val biayaList: StateFlow<List<BiayaOperasional>> = _biayaList.asStateFlow()

    // 1. Tambahkan state untuk menyimpan tanggal (null secara default agar fetch semua data)
    private val _startDate = MutableStateFlow<String?>(null)
    val startDate: StateFlow<String?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<String?>(null)
    val endDate: StateFlow<String?> = _endDate.asStateFlow()

    // 2. Buat fungsi untuk meng-update filter tanggal dari UI (Nantinya)
    fun updateDateFilter(start: String?, end: String?) {
        _startDate.value = start
        _endDate.value = end
        // Panggil ulang fungsi fetch data dengan parameter baru
        loadExpenses()
    }

    // 3. Pastikan fungsi pemanggilan API (loadExpenses) menggunakan state tanggal ini
    fun loadExpenses() {
        viewModelScope.launch {
            try {
                // Teruskan startDate dan endDate ke API
                val apiService = RetrofitClient.getExpenseApiService(getApplication())
                val response = apiService.getExpenses(
                    filter = null,
                    startDate = _startDate.value,
                    endDate = _endDate.value
                )
                if (response.isSuccessful && response.body()?.data != null) {
                    _biayaList.value = response.body()!!.data!!
                }
            } catch (e: Exception) {
                // handle error log jika diperlukan
            }
        }
    }
}
