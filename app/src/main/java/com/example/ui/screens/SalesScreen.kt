package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.InvoiceItem
import com.example.data.Transaction
import com.example.data.TransactionModel
import com.example.ui.components.AppIcons
import com.example.ui.theme.DangerColor
import com.example.ui.theme.SuccessColor
import com.example.ui.viewmodel.SalesViewModel
import com.example.utils.formatRupiah
import com.example.utils.generateQuotationPdf
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@android.annotation.SuppressLint("MissingPermission")
@Composable
fun SalesScreen(
    onBack: () -> Unit,
    viewModel: SalesViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isExpanded by viewModel.isDropdownExpanded.collectAsState()
    val filteredItems by viewModel.filteredItems.collectAsState()

    var qty by remember { mutableStateOf("1") }
    var harga by remember { mutableStateOf("") }
    var diskonItem by remember { mutableStateOf("") }
    
    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptText by remember { mutableStateOf("") }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }

    var isDiscountPercent by remember { mutableStateOf(true) } // true = %, false = Rp
    var discountInput by remember { mutableStateOf("") }
    var discountError by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("Cash") }

    val subtotal = viewModel.totalHarga
    val discountValue = discountInput.toDoubleOrNull() ?: 0.0
    val nominalDiskon = if (isDiscountPercent) {
        (subtotal * (discountValue / 100.0)).toLong()
    } else {
        discountValue.toLong()
    }
    val totalAkhir = (subtotal - nominalDiskon).coerceAtLeast(0L)

    LaunchedEffect(discountValue, isDiscountPercent, subtotal) {
        discountError = when {
            discountValue < 0.0 -> "Diskon tidak boleh negatif"
            isDiscountPercent && discountValue > 100.0 -> "Diskon persen maksimal 100%"
            !isDiscountPercent && nominalDiskon > subtotal -> "Diskon melebihi total harga"
            else -> null
        }
    }

    val sharedPrefs = remember { context.getSharedPreferences("printer_prefs", Context.MODE_PRIVATE) }
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    var showPrinterDialog by remember { mutableStateOf(false) }
    var pairedDevicesList by remember { mutableStateOf<List<BluetoothDevice>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val connectGranted = permissions[android.Manifest.permission.BLUETOOTH_CONNECT] ?: true
        if (!connectGranted) {
            Toast.makeText(context, "Izin Bluetooth dibutuhkan untuk mencetak struk", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkBluetoothAndPrint(onPermissionGranted: () -> Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                onPermissionGranted()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.BLUETOOTH_CONNECT,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            }
        } else {
            onPermissionGranted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Penjualan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // A. Bagian Atas: Form Input Barang
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Tambah Barang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    
                    com.example.ui.components.AutocompleteItemDropdown(
                        query = searchQuery,
                        onQueryChanged = { viewModel.onSearchQueryChanged(it) },
                        isExpanded = isExpanded,
                        onExpandedChanged = { viewModel.onDropdownExpandedChanged(it) },
                        filteredItems = filteredItems,
                        onItemSelected = { selectedItem -> 
                            viewModel.onItemSelected(selectedItem) { price ->
                                harga = price.toString()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Qty") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = harga,
                            onValueChange = { harga = it },
                            label = { Text("Harga") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = diskonItem,
                            onValueChange = { diskonItem = it },
                            label = { Text("Diskon") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Button(
                        onClick = {
                            val q = qty.toIntOrNull() ?: 0
                            val h = harga.toLongOrNull() ?: 0L
                            val d = diskonItem.toLongOrNull() ?: 0L
                            if (searchQuery.isNotBlank() && q > 0 && h > 0) {
                                viewModel.addToCart(searchQuery, q, h, d)
                                // Reset fields
                                viewModel.onSearchQueryChanged("")
                                qty = "1"
                                harga = ""
                                diskonItem = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(AppIcons.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tambah ke Keranjang")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // B. Bagian Tengah: Daftar Keranjang Belanja (Cart)
            Text("Keranjang Belanja", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.cartItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.namaBarang, fontWeight = FontWeight.Bold)
                                Text("${item.qty} x ${formatRupiah(item.harga)}", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(formatRupiah(item.subTotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { viewModel.removeFromCart(item) }) {
                                Icon(AppIcons.Delete, contentDescription = "Hapus", tint = DangerColor)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // --- Input Diskon ---
            if (viewModel.cartItems.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text(if (isDiscountPercent) "Diskon (%)" else "Diskon (Rp)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = discountError != null,
                        singleLine = true
                    )
                    
                    FilterChip(
                        selected = isDiscountPercent,
                        onClick = { isDiscountPercent = true },
                        label = { Text("%") }
                    )
                    FilterChip(
                        selected = !isDiscountPercent,
                        onClick = { isDiscountPercent = false },
                        label = { Text("Rp") }
                    )
                }
                
                if (discountError != null) {
                    Text(
                        text = discountError ?: "",
                        color = DangerColor,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // C. Bagian Bawah: Ringkasan & Aksi
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            if (nominalDiskon > 0 && discountError == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text(formatRupiah(subtotal), style = MaterialTheme.typography.bodyMedium)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val discLabel = if (isDiscountPercent) "Diskon (${discountValue.toLong()}%)" else "Diskon"
                    Text(discLabel, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Text("-${formatRupiah(nominalDiskon)}", style = MaterialTheme.typography.bodyMedium, color = DangerColor)
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (nominalDiskon > 0 && discountError == null) "Total Akhir" else "Total Harga", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatRupiah(totalAkhir),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SuccessColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Pembayaran:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                listOf("Cash", "QRIS", "Transfer").forEach { method ->
                    FilterChip(
                        selected = paymentMethod == method,
                        onClick = { paymentMethod = method },
                        label = { Text(method) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (viewModel.cartItems.isNotEmpty() && discountError == null) {
                        val transaction = viewModel.processTransaction(
                            diskonPersen = if (isDiscountPercent) discountValue else 0.0,
                            diskonNominal = nominalDiskon,
                            totalSetelahDiskon = totalAkhir,
                            paymentMethod = paymentMethod
                        )
                        currentTransaction = transaction
                        receiptText = viewModel.formatReceipt(transaction)
                        showReceiptDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.cartItems.isNotEmpty() && discountError == null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(AppIcons.Print, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Proses & Cetak Struk", fontSize = 18.sp)
            }
        }
    }

    if (showReceiptDialog) {
        AlertDialog(
            onDismissRequest = { 
                showReceiptDialog = false
                viewModel.clearCart()
                discountInput = ""
                paymentMethod = "Cash"
            },
            title = { Text("Struk Penjualan") },
            text = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = receiptText,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        currentTransaction?.let { trx ->
                            val quotationData = TransactionModel(
                                customerName = "Pelanggan Umum",
                                customerAddress = "Jl. Raya Warung No. 123",
                                items = trx.items.map { 
                                    InvoiceItem(
                                        name = it.namaBarang,
                                        qty = it.qty,
                                        price = it.harga.toDouble()
                                    )
                                },
                                salesName = "Admin Warung",
                                invoiceCode = trx.kodeTransaksi,
                                date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(trx.tanggalTransaksi)),
                                notes = "Terima kasih atas kunjungan Anda."
                            )
                            generateQuotationPdf(context, quotationData, viewModel.namaWarungState.value)
                        }
                    }) {
                        Icon(AppIcons.Pdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export PDF")
                    }

                    TextButton(onClick = {
                        checkBluetoothAndPrint {
                            if (bluetoothAdapter == null) {
                                Toast.makeText(context, "Bluetooth tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
                                return@checkBluetoothAndPrint
                            }
                            if (!bluetoothAdapter.isEnabled) {
                                Toast.makeText(context, "Nyalakan Bluetooth terlebih dahulu", Toast.LENGTH_SHORT).show()
                                return@checkBluetoothAndPrint
                            }

                            // Get last used printer
                            val lastPrinterMac = sharedPrefs.getString("last_printer_mac", null)
                            val bondedDevices = try { bluetoothAdapter.bondedDevices } catch (e: SecurityException) { emptySet() }
                            
                            val lastDevice = bondedDevices.find { it.address == lastPrinterMac }
                            if (lastDevice != null) {
                                Toast.makeText(context, "Mencetak ke ${lastDevice.name}...", Toast.LENGTH_SHORT).show()
                                viewModel.printToThermal(lastDevice, receiptText) { success ->
                                    val message = if (success) "Struk berhasil dicetak" else "Gagal mencetak struk"
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                // Show printer selection dialog
                                pairedDevicesList = bondedDevices.toList()
                                if (pairedDevicesList.isEmpty()) {
                                    Toast.makeText(context, "Tidak ada perangkat Bluetooth terpasang (paired). Pasangkan printer terlebih dahulu di pengaturan HP.", Toast.LENGTH_LONG).show()
                                } else {
                                    showPrinterDialog = true
                                }
                            }
                        }
                    }) {
                        Icon(AppIcons.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cetak Struk")
                    }
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showReceiptDialog = false 
                    viewModel.clearCart()
                    discountInput = ""
                }) {
                    Text("Selesai")
                }
            }
        )
    }

    if (showPrinterDialog) {
        AlertDialog(
            onDismissRequest = { showPrinterDialog = false },
            title = { Text("Pilih Printer Bluetooth") },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pairedDevicesList) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPrinterDialog = false
                                    sharedPrefs.edit().putString("last_printer_mac", device.address).apply()
                                    Toast.makeText(context, "Mencetak ke ${device.name ?: "Printer"}...", Toast.LENGTH_SHORT).show()
                                    viewModel.printToThermal(device, receiptText) { success ->
                                        val message = if (success) "Struk berhasil dicetak" else "Gagal mencetak struk"
                                        (context as? android.app.Activity)?.runOnUiThread {
                                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold)
                                Text(device.address, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrinterDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
