package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onLoginSuccess: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun handleLogin() {
        emailError = email.isBlank()
        passwordError = password.isBlank()

        if (!emailError && !passwordError) {
            isLoading = true
            coroutineScope.launch {
                // Simulate network call
                delay(1500)
                isLoading = false
                if (email == "owner@warung.com" || email == "admin@warung.com") { // Simple mock condition for now
                    val role = if (email == "owner@warung.com") "Owner" else "Admin"
                    onLoginSuccess(role)
                } else {
                    snackbarHostState.showSnackbar("Email atau password salah")
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Warung Manager",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    if (email.isNotBlank()) emailError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Masukkan email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                isError = emailError,
                singleLine = true,
                supportingText = {
                    if (emailError) {
                        Text("Email wajib diisi", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    if (password.isNotBlank()) passwordError = false
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Masukkan password") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(imageVector = image, contentDescription = if (showPassword) "Hide password" else "Show password")
                    }
                },
                isError = passwordError,
                singleLine = true,
                supportingText = {
                    if (passwordError) {
                        Text("Password wajib diisi", color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            PrimaryButton(
                text = "MASUK",
                onClick = { handleLogin() },
                isLoading = isLoading
            )
        }
    }
}
