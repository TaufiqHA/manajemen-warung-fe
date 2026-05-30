package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfilViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences(application)

    // Form inputs state
    var namaInput by mutableStateOf("")
    var alamatInput by mutableStateOf("")
    var emailInput by mutableStateOf("")

    // Current stored states
    var namaStored by mutableStateOf("WARUNG KITA")
        private set
    var alamatStored by mutableStateOf("")
        private set
    var emailStored by mutableStateOf("owner@gmail.com")
        private set

    // Validation error states
    var namaError by mutableStateOf<String?>(null)
    var alamatError by mutableStateOf<String?>(null)
    var emailError by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            // Fetch initial values
            namaStored = userPrefs.namaWarung.first()
            alamatStored = userPrefs.alamatWarung.first()
            emailStored = userPrefs.email.first()
            resetForm()
            
            // Collect any external updates to keep it in sync
            launch {
                userPrefs.namaWarung.collect { namaStored = it }
            }
            launch {
                userPrefs.alamatWarung.collect { alamatStored = it }
            }
            launch {
                userPrefs.email.collect { emailStored = it }
            }
        }
    }

    fun resetForm() {
        namaInput = namaStored
        alamatInput = alamatStored
        emailInput = emailStored
        clearErrors()
    }

    private fun clearErrors() {
        namaError = null
        alamatError = null
        emailError = null
    }

    fun saveProfile(onSuccess: () -> Unit = {}) {
        clearErrors()
        var hasError = false

        if (namaInput.isBlank()) {
            namaError = "Nama warung tidak boleh kosong"
            hasError = true
        } else if (namaInput.length > 50) {
            namaError = "Nama warung maksimal 50 karakter"
            hasError = true
        }

        if (alamatInput.length > 100) {
            alamatError = "Alamat maksimal 100 karakter"
            hasError = true
        }

        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        if (emailInput.isBlank()) {
            emailError = "Email tidak boleh kosong"
            hasError = true
        } else if (!emailInput.matches(emailRegex)) {
            emailError = "Format email tidak valid"
            hasError = true
        }

        if (!hasError) {
            viewModelScope.launch {
                userPrefs.saveProfile(
                    nama = namaInput.trim(),
                    alamat = alamatInput.trim(),
                    email = emailInput.trim()
                )
                onSuccess()
            }
        }
    }
}
