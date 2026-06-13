package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.UserRole
import com.example.ui.theme.DangerColor

import androidx.compose.ui.platform.LocalContext
import com.example.data.UserData
import com.example.data.repository.UserRepository
import kotlinx.coroutines.launch

@Composable
fun UserManagementTabContent(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository(context) }
    val coroutineScope = rememberCoroutineScope()

    var users by remember { mutableStateOf<List<UserData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedUser by remember { mutableStateOf<UserData?>(null) }
    var displayEditDialog by remember { mutableStateOf(false) }

    fun loadUsers() {
        coroutineScope.launch {
            isLoading = true
            val result = userRepository.getAllUsers()
            if (result.isSuccess) {
                users = result.getOrNull() ?: emptyList()
            } else {
                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Gagal memuat user")
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadUsers()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        
        Text(
            text = "Manajemen User",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = user.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = user.role.displayName,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    selectedUser = user
                                    displayEditDialog = true
                                }
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_custom_profile),
                                    contentDescription = "Edit User",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        if (displayEditDialog && selectedUser != null) {
            var newName by remember { mutableStateOf(selectedUser!!.name) }
            var newEmail by remember { mutableStateOf(selectedUser!!.email) }
            var newPassword by remember { mutableStateOf("") }
            var isError by remember { mutableStateOf(false) }
            var isSaving by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { displayEditDialog = false },
                title = { Text("Edit User: ${selectedUser!!.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { 
                                newName = it
                                isError = false
                            },
                            label = { Text("Nama Baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Email Baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Password Baru (Opsional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isError) {
                            Text(
                                text = "Nama tidak boleh kosong!",
                                color = DangerColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isBlank()) {
                                isError = true
                            } else {
                                isSaving = true
                                coroutineScope.launch {
                                    val result = userRepository.updateUserById(
                                        userId = selectedUser!!.id,
                                        name = newName.trim(),
                                        email = newEmail.trim().takeIf { it.isNotBlank() },
                                        password = if (newPassword.isNotBlank()) newPassword else null
                                    )
                                    isSaving = false
                                    if (result.isSuccess) {
                                        displayEditDialog = false
                                        loadUsers()
                                        snackbarHostState.showSnackbar("User berhasil diperbarui")
                                    } else {
                                        snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Gagal memperbarui user")
                                    }
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text(if (isSaving) "MENYIMPAN..." else "SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { displayEditDialog = false }, enabled = !isSaving) {
                        Text("BATAL")
                    }
                }
            )
        }
    }
}
