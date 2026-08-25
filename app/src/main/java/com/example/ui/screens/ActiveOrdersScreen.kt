package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Transaction
import com.example.ui.components.AppIcons
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.DangerColor
import com.example.utils.LocalStorageHelper
import com.example.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SalesViewModel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.data.InvoiceItem
import com.example.data.TransactionModel
import com.example.utils.generateQuotationPdf

@Composable
fun ActiveOrdersTabContent(
    modifier: Modifier = Modifier,
    menuList: List<MenuItem> = emptyList(),
    isReadOnly: Boolean = false
) {
    val context = LocalContext.current
    val storageHelper = remember { LocalStorageHelper(context) }
    val salesViewModel: SalesViewModel = viewModel()
    
    val nestedTransactions by salesViewModel.activeOrders.collectAsState()

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

    LaunchedEffect(Unit) {
        salesViewModel.loadActiveOrders()
        salesViewModel.startPollingActiveOrders()
    }
    
    // Only show PENDING and READY orders
    val activeOrders = nestedTransactions.filter { it.status == "PENDING" || it.status == "READY" }.sortedByDescending { it.tanggalTransaksi }
    
    var showReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    var showKitchenReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    var showAddItemDialog by remember { mutableStateOf<Transaction?>(null) }
    var itemToEdit by remember { mutableStateOf<Pair<Transaction, com.example.data.TransactionItem>?>(null) }
    var showPaymentDialog by remember { mutableStateOf<Transaction?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf("Cash") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Daftar Pesanan Aktif (${activeOrders.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { salesViewModel.fetchActiveOrders() }) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (activeOrders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada pesanan aktif", color = Color.Gray)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(activeOrders) { order ->
                    OrderCard(
                        order = order,
                        isReadOnly = isReadOnly,
                        onMarkReady = {
                            salesViewModel.updateActiveOrderStatus(order.kodeTransaksi, "READY")
                        },
                        onProcessPayment = {
                            showPaymentDialog = order
                        },
                        onUpdateItemServedQty = { itemId, newQty ->
                            salesViewModel.updateItemServedQty(order.kodeTransaksi, itemId, newQty)
                            
                            // Check if all items are fully served
                            val allServed = order.items.all {
                                if (it.itemId == itemId) newQty >= it.qty else it.servedQty >= it.qty
                            }
                            if (allServed && order.status == "PENDING") {
                                salesViewModel.updateActiveOrderStatus(order.kodeTransaksi, "READY")
                            } else if (!allServed && order.status == "READY") {
                                salesViewModel.updateActiveOrderStatus(order.kodeTransaksi, "PENDING")
                            }
                        },
                        onPrintDapur = {
                            showKitchenReceiptDialog = order
                        },
                        onAddItemsClick = {
                            showAddItemDialog = order
                        },
                        onEditItemClick = { item ->
                            itemToEdit = Pair(order, item)
                        }
                    )
                }
            }
        }
    }

    if (itemToEdit != null) {
        val transaction = itemToEdit!!.first
        val item = itemToEdit!!.second
        var editQuantity by remember { mutableStateOf(item.qty) }

        AlertDialog(
            onDismissRequest = { itemToEdit = null },
            title = { Text("Edit Item Pesanan") },
            text = {
                Column {
                    Text("Item: ${item.namaBarang}")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (editQuantity > 1) editQuantity-- }) {
                            Icon(AppIcons.Remove, contentDescription = "Kurangi")
                        }
                        Text(text = editQuantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                        IconButton(onClick = { editQuantity++ }) {
                            Icon(AppIcons.Add, contentDescription = "Tambah")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    salesViewModel.updateItemQuantityInActiveOrder(transaction.kodeTransaksi, item.itemId, editQuantity, item.harga.toDouble()) {
                        itemToEdit = null
                    }
                }) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                Row {
                    var isDeleting by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = {
                            if (isDeleting) return@TextButton
                            isDeleting = true
                            salesViewModel.removeItemFromActiveOrder(
                                kodeTransaksi = transaction.kodeTransaksi,
                                itemId = item.itemId,
                                onSuccess = {
                                    isDeleting = false
                                    itemToEdit = null
                                },
                                onError = { errorMsg ->
                                    isDeleting = false
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                        enabled = !isDeleting
                    ) {
                        Text(if (isDeleting) "Menghapus..." else "Hapus Item")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { itemToEdit = null },
                        enabled = !isDeleting
                    ) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    if (showAddItemDialog != null) {
        val transaction = showAddItemDialog!!
        var searchQuery by remember { mutableStateOf("") }
        val filteredMenu = menuList.filter { 
            it.nama.contains(searchQuery, ignoreCase = true) 
        }
        var selectedItem by remember(searchQuery) { mutableStateOf<MenuItem?>(filteredMenu.firstOrNull()) }
        var quantity by remember { mutableStateOf(1) }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isLoading) showAddItemDialog = null },
            title = { Text("Tambah Item Pesanan") },
            text = {
                Column {
                    Text("Pilih menu tambahan untuk: ${transaction.customerName.ifBlank { "Pelanggan" }}")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari menu...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple dropdown simulation (or LazyColumn of items)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredMenu) { menu ->
                            val isSelected = selectedItem?.id == menu.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isLoading) { selectedItem = menu }
                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(menu.nama)
                                Text(formatRupiah(menu.harga))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (quantity > 1) quantity-- }, enabled = !isLoading) {
                            Icon(AppIcons.Remove, contentDescription = "Kurangi")
                        }
                        Text(text = quantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                        IconButton(onClick = { quantity++ }, enabled = !isLoading) {
                            Icon(AppIcons.Add, contentDescription = "Tambah")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedItem?.let { menu ->
                            isLoading = true
                            val newItem = com.example.data.TransactionItem(
                                itemId = menu.id,
                                namaBarang = menu.nama,
                                qty = quantity,
                                harga = menu.harga.toLong(),
                                subTotal = quantity * menu.harga.toLong(),
                                servedQty = 0
                            )
                            
                            salesViewModel.addItemToActiveOrder(
                                kodeTransaksi = transaction.kodeTransaksi,
                                newItem = newItem,
                                onSuccess = {
                                    isLoading = false
                                    android.widget.Toast.makeText(context, "Item berhasil ditambahkan", android.widget.Toast.LENGTH_SHORT).show()
                                    showAddItemDialog = null
                                },
                                onError = { errorMsg ->
                                    isLoading = false
                                    android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_LONG).show()
                                }
                            )
                        } ?: run {
                            android.widget.Toast.makeText(context, "Pilih menu terlebih dahulu", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Simpan Tambahan")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = null }, enabled = !isLoading) {
                    Text("Batal")
                }
            }
        )
    }

    if (showReceiptDialog != null) {
        val transaction = showReceiptDialog!!
        val receiptText = salesViewModel.formatReceipt(transaction, selectedPaymentMethod)
        
        AlertDialog(
            onDismissRequest = { showReceiptDialog = null },
            title = { Text("Struk Pembayaran (Kasir)") },
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = receiptText,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        val quotationData = TransactionModel(
                            customerName = transaction.customerName.ifBlank { "Pelanggan Umum" },
                            customerAddress = "Jl. Raya Warung", 
                            items = transaction.items.map { 
                                InvoiceItem(
                                    name = it.namaBarang,
                                    qty = it.qty,
                                    price = it.harga.toDouble()
                                )
                            },
                            salesName = "Kasir",
                            invoiceCode = transaction.kodeTransaksi,
                            date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(transaction.tanggalTransaksi)),
                            notes = "Terima kasih atas kunjungan Anda."
                        )
                        generateQuotationPdf(context, quotationData, salesViewModel.namaWarungState.value)
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
                                salesViewModel.printToThermal(lastDevice, receiptText) { success ->
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
                    showReceiptDialog = null 
                }) {
                    Text("Selesai")
                }
            }
        )
    }

    if (showPrinterDialog) {
        val transactionForPrint = showReceiptDialog
        val receiptTextToPrint = if (transactionForPrint != null) salesViewModel.formatReceipt(transactionForPrint, selectedPaymentMethod) else ""
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
                                    salesViewModel.printToThermal(device, receiptTextToPrint) { success ->
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

    if (showKitchenReceiptDialog != null) {
        val transaction = showKitchenReceiptDialog!!
        // Format Struk Dapur manually here for simplicity, or add it to ViewModel
        val kitchenReceiptText = buildString {
            appendLine("===== STRUK DAPUR =====")
            appendLine("No : ${transaction.kodeTransaksi}")
            appendLine("Jam: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.tanggalTransaksi))}")
            appendLine("Plg: ${transaction.customerName.ifBlank { "Umum" }}")
            appendLine("-----------------------")
            transaction.items.forEach { item ->
                appendLine("${item.qty}x ${item.namaBarang}")
            }
            appendLine("-----------------------")
        }
        
        AlertDialog(
            onDismissRequest = { showKitchenReceiptDialog = null },
            title = { Text("Struk Dapur") },
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    color = Color.White,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text(
                        text = kitchenReceiptText,
                        modifier = Modifier.padding(16.dp),
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { showKitchenReceiptDialog = null }) {
                        Text("Tutup")
                    }
                    Button(onClick = { 
                        showKitchenReceiptDialog = null 
                    }) {
                        Text("Print")
                    }
                }
            }
        )
    }

    if (showPaymentDialog != null) {
        val transaction = showPaymentDialog!!
        var discountInput by remember { mutableStateOf("") }
        var isDiscountPercent by remember { mutableStateOf(false) }
        var discountError by remember { mutableStateOf<String?>(null) }

        val discountAmount = discountInput.toLongOrNull() ?: 0L
        val diskonNominal = if (isDiscountPercent) {
            (transaction.totalHarga * discountAmount / 100)
        } else {
            discountAmount
        }
        val totalAfterDiscount = (transaction.totalHarga - diskonNominal).coerceAtLeast(0L)

        LaunchedEffect(discountAmount, isDiscountPercent, transaction.totalHarga) {
            discountError = when {
                discountAmount < 0 -> "Diskon tidak boleh negatif"
                isDiscountPercent && discountAmount > 100 -> "Diskon persen maksimal 100%"
                !isDiscountPercent && diskonNominal > transaction.totalHarga -> "Diskon melebihi total harga"
                else -> null
            }
        }

        AlertDialog(
            onDismissRequest = { showPaymentDialog = null },
            title = { Text("Pilih Metode Pembayaran") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text("Total Pesanan: ${formatRupiah(transaction.totalHarga)}", color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Total Tagihan: ${formatRupiah(totalAfterDiscount)}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Metode Pembayaran:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "QRIS", "Transfer").forEach { method ->
                            FilterChip(
                                selected = selectedPaymentMethod == method,
                                onClick = { selectedPaymentMethod = method },
                                label = { Text(method) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                var isPaying by remember { mutableStateOf(false) }
                Button(
                    onClick = {
                        if (isPaying || discountError != null) return@Button
                        isPaying = true
                        
                        // Local discount update for receipt
                        storageHelper.updateTransactionDiscount(
                            transaction.kodeTransaksi,
                            if (isDiscountPercent) discountAmount.toDouble() else 0.0,
                            if (!isDiscountPercent) discountAmount else diskonNominal,
                            totalAfterDiscount
                        )
                        
                        salesViewModel.updateActiveOrderStatus(
                            kodeTransaksi = transaction.kodeTransaksi,
                            newStatus = "COMPLETED",
                            paymentMethod = selectedPaymentMethod,
                            discountAmount = diskonNominal,
                            onSuccess = {
                                isPaying = false
                                showPaymentDialog = null
                                
                                val latestTransaction = nestedTransactions.find { it.kodeTransaksi == transaction.kodeTransaksi } ?: transaction
                                showReceiptDialog = latestTransaction.copy(
                                    diskonPersen = if (isDiscountPercent) discountAmount.toDouble() else 0.0,
                                    diskonNominal = if (!isDiscountPercent) discountAmount else diskonNominal,
                                    totalSetelahDiskon = totalAfterDiscount,
                                    status = "COMPLETED"
                                )
                            },
                            onError = { errMsg ->
                                isPaying = false
                                android.widget.Toast.makeText(context, errMsg, android.widget.Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    enabled = !isPaying && discountError == null
                ) {
                    Text(if (isPaying) "Memproses..." else "Konfirmasi Bayar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun OrderCard(
    order: Transaction,
    isReadOnly: Boolean = false,
    onMarkReady: () -> Unit,
    onProcessPayment: () -> Unit,
    onUpdateItemServedQty: (String, Int) -> Unit,
    onPrintDapur: () -> Unit,
    onAddItemsClick: () -> Unit,
    onEditItemClick: (com.example.data.TransactionItem) -> Unit
) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val timeStr = sdf.format(Date(order.tanggalTransaksi))
    
    val isReady = order.status == "READY"
    val containerColor = if (isReady) SuccessColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        var isExpanded by remember { mutableStateOf(false) }
        
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    val displayName = order.customerName.ifBlank { "Pelanggan Umum" }
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "${order.kodeTransaksi}\nWaktu: $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isReady) SuccessColor else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isReady) "SIAP" else "DAPUR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = AppIcons.ArrowDropDown,
                        contentDescription = "Expand",
                        tint = Color.Gray
                    )
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top // Mengikuti flex items-start
                ) {
                    // Container Nama Menu + Tombol Edit
                    Row(
                        modifier = Modifier
                            .weight(1f) // flex-1 min-w-0
                            .padding(end = 12.dp), // mr-3
                        verticalAlignment = Alignment.Top // items-start
                    ) {
                        Text(
                            text = "${item.qty}x ${item.namaBarang}",
                            modifier = Modifier.weight(1f) // flex-1, break-words
                        )
                        if (!isReadOnly) {
                            IconButton(
                                onClick = { onEditItemClick(item) },
                                modifier = Modifier
                                    .padding(start = 8.dp) // ml-2
                                    .size(24.dp) // shrink-0
                            ) {
                                Icon(
                                    imageVector = AppIcons.Edit,
                                    contentDescription = "Edit Item",
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { if (item.servedQty > 0) onUpdateItemServedQty(item.itemId, item.servedQty - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Remove, 
                                        contentDescription = "Kurang", 
                                        modifier = Modifier.size(16.dp),
                                        tint = if (item.servedQty > 0) MaterialTheme.colorScheme.onSurface else Color.LightGray
                                    )
                                }
                            }
                            Text(
                                text = "${item.servedQty} / ${item.qty}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (item.servedQty == item.qty) SuccessColor else MaterialTheme.colorScheme.onSurface
                            )
                            if (!isReadOnly) {
                                IconButton(
                                    onClick = { if (item.servedQty < item.qty) onUpdateItemServedQty(item.itemId, item.servedQty + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = AppIcons.Add, 
                                        contentDescription = "Tambah", 
                                        modifier = Modifier.size(16.dp),
                                        tint = if (item.servedQty < item.qty) MaterialTheme.colorScheme.onSurface else Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Total: ${formatRupiah(order.totalSetelahDiskon)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                if (!isReadOnly) {
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onPrintDapur) {
                            Text("Print Dapur")
                        }
                        TextButton(onClick = onAddItemsClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                            Text("+ Item")
                        }
                        if (!isReady) {
                            OutlinedButton(onClick = onMarkReady) {
                                Text("Selesai")
                            }
                        } else {
                            Button(onClick = onProcessPayment) {
                                Text("Bayar & Cetak")
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
}
