package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserResponse
import com.example.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class Success(val response: UserResponse) : UserState()
    data class Error(val message: String) : UserState()
}

data class LocalUser(
    val id: String?,
    val username: String?,
    val role: String?
)

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository = UserRepository(application.applicationContext)

    private val _userState = MutableStateFlow<UserState>(UserState.Idle)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    fun fetchCurrentUser() {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            userRepository.getCurrentUser()
                .onSuccess {
                    _userState.value = UserState.Success(it)
                }
                .onFailure {
                    _userState.value = UserState.Error(it.message ?: "Failed to get user profile")
                }
        }
    }

    fun getLocalUser(): LocalUser {
        return LocalUser(
            id = userRepository.getLocalUserId(),
            username = userRepository.getLocalUserName(),
            role = userRepository.getLocalUserRole()
        )
    }
}
