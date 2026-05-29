package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserRole
import com.example.ui.components.ProfileMenuItem
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.DangerColor

@Composable
fun ProfilTabContent(
    userName: String,
    userRole: UserRole,
    userEmail: String,
    onLogoutClick: () -> Unit,
    onNameChange: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var currentNameState by remember { mutableStateOf(userName) }
    var displayPasswordChange by remember { mutableStateOf(false) }
    var displayNamaChange by remember { mutableStateOf(false) }
    var displayHelpMessage by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        
        // App header title
        Text(
            text = "Profil Pengguna",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 1. Premium Header Profil Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Initials avatar circle shape with a premium design
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = currentNameState.split(" ")
                        .filter { it.isNotBlank() }
                        .map { it.firstOrNull() ?: "" }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                        .let { if (it.isEmpty()) "WM" else it }
                    Text(
                        text = initials,
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentNameState,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = userRole.displayName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 2. Card Group 1: Akun & Pengaturan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                ProfileMenuItem(
                    iconResId = R.drawable.ic_custom_profile,
                    title = "Ubah Nama",
                    onClick = { displayNamaChange = true }
                )
                
                if (userRole == UserRole.OWNER) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ProfileMenuItem(
                        iconResId = R.drawable.ic_custom_settings,
                        title = "Pengaturan Logo",
                        onClick = { onNavigateToSettings() }
                    )
                }
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    iconResId = R.drawable.ic_custom_lock,
                    title = "Ubah Password",
                    onClick = { displayPasswordChange = true }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Card Group 2: Informasi & Bantuan
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                ProfileMenuItem(
                    iconResId = R.drawable.ic_custom_help,
                    title = "Bantuan & Dukungan",
                    onClick = { displayHelpMessage = true }
                )
                Divider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ProfileMenuItem(
                    iconResId = R.drawable.ic_custom_info,
                    title = "Versi Aplikasi",
                    trailingText = "1.0.0",
                    showChevron = false
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Logout Warning Button
        Button(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_custom_logout),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onError,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "KELUAR",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onError
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ---------- DIALOGS ----------

        // CONFIRM LOGOUT DIALOG
        if (showLogoutConfirm) {
            ConfirmDialog(
                title = "Keluar?",
                text = "Yakin ingin logout dari aplikasi Warung Manager?",
                confirmText = "KELUAR",
                onConfirm = {
                    showLogoutConfirm = false
                    onLogoutClick()
                },
                onDismiss = { showLogoutConfirm = false }
            )
        }

        // BANTUAN & DUKUNGAN DIALOG
        if (displayHelpMessage) {
            AlertDialog(
                onDismissRequest = { displayHelpMessage = false },
                title = { Text("Bantuan & Dukungan") },
                text = {
                    Text(
                        text = "Untuk bantuan teknis atau pertanyaan seputar penggunaan aplikasi Warung Manager, silakan hubungi tim dukungan kami melalui email di support@warungmanager.com.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { displayHelpMessage = false }) {
                        Text("OK")
                    }
                }
            )
        }

        // ---------- UBAH NAMA DIALOG ----------
        if (displayNamaChange) {
            var newNamaInput by remember { mutableStateOf(currentNameState) }
            var isNamaValidError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { displayNamaChange = false },
                title = { Text("Ubah Nama Profil") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newNamaInput,
                            onValueChange = {
                                newNamaInput = it
                                isNamaValidError = false
                            },
                            label = { Text("Nama Baru") },
                            singleLine = true,
                            isError = isNamaValidError,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isNamaValidError) {
                            Text(
                                text = "Nama minimal harus 8 karakter!",
                                color = DangerColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newNamaInput.trim().length < 8) {
                                isNamaValidError = true
                            } else {
                                onNameChange(newNamaInput.trim())
                                currentNameState = newNamaInput.trim()
                                displayNamaChange = false
                            }
                        }
                    ) {
                        Text("SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { displayNamaChange = false }) {
                        Text("BATAL")
                    }
                }
            )
        }

        // ---------- PASSWORD CHANGE DIALOG ----------
        if (displayPasswordChange) {
            var oldPassword by remember { mutableStateOf("") }
            var newPassword by remember { mutableStateOf("") }
            var confPassword by remember { mutableStateOf("") }
            var inlineErrorMsg by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { displayPasswordChange = false },
                title = { Text("Ubah Password") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { 
                                oldPassword = it
                                inlineErrorMsg = ""
                            },
                            label = { Text("Password Lama") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { 
                                newPassword = it 
                                inlineErrorMsg = ""
                            },
                            label = { Text("Password Baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confPassword,
                            onValueChange = { 
                                confPassword = it 
                                inlineErrorMsg = ""
                            },
                            label = { Text("Konfirmasi Password Baru") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (inlineErrorMsg.isNotEmpty()) {
                            Text(
                                text = inlineErrorMsg,
                                color = DangerColor,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (oldPassword.isBlank()) {
                                inlineErrorMsg = "Password lama wajib diisi!"
                            } else if (newPassword.length < 8) {
                                inlineErrorMsg = "Password baru minimal harus 8 karakter!"
                            } else if (newPassword != confPassword) {
                                inlineErrorMsg = "Konfirmasi password baru tidak cocok!"
                            } else {
                                displayPasswordChange = false
                            }
                        }
                    ) {
                        Text("SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { displayPasswordChange = false }) {
                        Text("BATAL")
                    }
                }
            )
        }
    }
}
