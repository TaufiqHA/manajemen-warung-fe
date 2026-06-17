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
import com.example.utils.LocalStorageHelper
import com.example.utils.formatRupiah
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SalesViewModel

@Composable
fun ActiveOrdersTabContent(
    modifier: Modifier = Modifier,
    menuList: List<MenuItem> = emptyList()
) {
    val context = LocalContext.current
    val storageHelper = remember { LocalStorageHelper(context) }
    val salesViewModel: SalesViewModel = viewModel()
    
    val nestedTransactions by salesViewModel.activeOrders.collectAsState()

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
                        onMarkReady = {
                            storageHelper.updateTransactionStatus(order.kodeTransaksi, "READY")
                            salesViewModel.syncLocalActiveOrders()
                        },
                        onProcessPayment = {
                            // Untuk simplifikasi: saat dibayar, status jadi COMPLETED, cetak struk kasir
                            storageHelper.updateTransactionStatus(order.kodeTransaksi, "COMPLETED")
                            salesViewModel.syncLocalActiveOrders()
                            showReceiptDialog = order
                        },
                        onUpdateItemServedQty = { itemId, newQty ->
                            storageHelper.updateItemServedQty(order.kodeTransaksi, itemId, newQty)
                            salesViewModel.syncLocalActiveOrders()
                            
                            // Check if all items are fully served
                            val updatedTx = storageHelper.getNestedTransactions().find { it.kodeTransaksi == order.kodeTransaksi }
                            if (updatedTx != null) {
                                val allServed = updatedTx.items.all { it.servedQty >= it.qty }
                                if (allServed && updatedTx.status == "PENDING") {
                                    storageHelper.updateTransactionStatus(updatedTx.kodeTransaksi, "READY")
                                    salesViewModel.syncLocalActiveOrders()
                                } else if (!allServed && updatedTx.status == "READY") {
                                    storageHelper.updateTransactionStatus(updatedTx.kodeTransaksi, "PENDING")
                                    salesViewModel.syncLocalActiveOrders()
                                }
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
                    storageHelper.updateItemQuantity(transaction.kodeTransaksi, item.itemId, editQuantity)
                    salesViewModel.syncLocalActiveOrders()
                    itemToEdit = null
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
        val receiptText = salesViewModel.formatReceipt(transaction)
        
        AlertDialog(
            onDismissRequest = { showReceiptDialog = null },
            title = { Text("Struk Pembayaran (Kasir)") },
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
                    TextButton(onClick = { showReceiptDialog = null }) {
                        Text("Tutup")
                    }
                    Button(onClick = { 
                        // Simulate Print or trigger Print Intent
                        showReceiptDialog = null 
                    }) {
                        Text("Selesai")
                    }
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
                    modifier = Modifier.fillMaxWidth(),
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
}

@Composable
fun OrderCard(
    order: Transaction,
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
                    Text(
                        text = order.customerName.ifBlank { order.kodeTransaksi },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    val bottomText = if (order.customerName.isBlank()) "Waktu: $timeStr" else "Waktu: $timeStr\nNo: ${order.kodeTransaksi}"
                    Text(text = bottomText, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            Text(
                                text = "${item.servedQty} / ${item.qty}",
                                modifier = Modifier.padding(horizontal = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (item.servedQty == item.qty) SuccessColor else MaterialTheme.colorScheme.onSurface
                            )
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
