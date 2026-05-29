package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LoginResponse
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val response: LoginResponse) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class LogoutState {
    object Idle : LogoutState()
    object Loading : LogoutState()
    object Success : LogoutState()
    data class Error(val message: String) : LogoutState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val authRepository = AuthRepository(application.applicationContext)

    private val _loginState = MutableStateFlow<AuthState>(AuthState.Idle)
    val loginState: StateFlow<AuthState> = _loginState.asStateFlow()

    private val _logoutState = MutableStateFlow<LogoutState>(LogoutState.Idle)
    val logoutState: StateFlow<LogoutState> = _logoutState.asStateFlow()

    fun login(email: String, password: String) {
        _loginState.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.login(email, password)
                .onSuccess {
                    _loginState.value = AuthState.Success(it)
                }
                .onFailure {
                    _loginState.value = AuthState.Error(it.message ?: "Login failed")
                }
        }
    }

    fun logout() {
        _logoutState.value = LogoutState.Loading
        viewModelScope.launch {
            authRepository.logout()
                .onSuccess {
                    _logoutState.value = LogoutState.Success
                }
                .onFailure {
                    _logoutState.value = LogoutState.Error(it.message ?: "Logout failed")
                }
        }
    }

    fun isUserLoggedIn(): Boolean = authRepository.isUserLoggedIn()
}
