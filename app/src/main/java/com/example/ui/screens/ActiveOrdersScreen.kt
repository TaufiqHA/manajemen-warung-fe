package com.example.ui.screens

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
    var nestedTransactions by remember { mutableStateOf(storageHelper.getNestedTransactions()) }
    
    // Only show PENDING and READY orders
    val activeOrders = nestedTransactions.filter { it.status == "PENDING" || it.status == "READY" }.sortedByDescending { it.tanggalTransaksi }

    val salesViewModel: SalesViewModel = viewModel()
    
    var showReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    var showKitchenReceiptDialog by remember { mutableStateOf<Transaction?>(null) }
    var showAddItemDialog by remember { mutableStateOf<Transaction?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Daftar Pesanan Aktif",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
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
                            nestedTransactions = storageHelper.getNestedTransactions()
                        },
                        onProcessPayment = {
                            // Untuk simplifikasi: saat dibayar, status jadi COMPLETED, cetak struk kasir
                            storageHelper.updateTransactionStatus(order.kodeTransaksi, "COMPLETED")
                            nestedTransactions = storageHelper.getNestedTransactions()
                            showReceiptDialog = order
                        },
                        onUpdateItemServedQty = { itemId, newQty ->
                            storageHelper.updateItemServedQty(order.kodeTransaksi, itemId, newQty)
                            nestedTransactions = storageHelper.getNestedTransactions()
                            
                            // Check if all items are fully served
                            val updatedTx = storageHelper.getNestedTransactions().find { it.kodeTransaksi == order.kodeTransaksi }
                            if (updatedTx != null) {
                                val allServed = updatedTx.items.all { it.servedQty >= it.qty }
                                if (allServed && updatedTx.status == "PENDING") {
                                    storageHelper.updateTransactionStatus(updatedTx.kodeTransaksi, "READY")
                                    nestedTransactions = storageHelper.getNestedTransactions()
                                }
                            }
                        },
                        onPrintDapur = {
                            showKitchenReceiptDialog = order
                        },
                        onAddItemsClick = {
                            showAddItemDialog = order
                        }
                    )
                }
            }
        }
    }

    if (showAddItemDialog != null) {
        val transaction = showAddItemDialog!!
        var searchQuery by remember { mutableStateOf("") }
        val filteredMenu = menuList.filter { 
            it.nama.contains(searchQuery, ignoreCase = true) 
        }
        var selectedItem by remember(searchQuery) { mutableStateOf<MenuItem?>(filteredMenu.firstOrNull()) }
        var quantity by remember { mutableStateOf(1) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = null },
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
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple dropdown simulation (or LazyColumn of items)
                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(filteredMenu) { menu ->
                            val isSelected = selectedItem?.id == menu.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedItem = menu }
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
                        IconButton(onClick = { if (quantity > 1) quantity-- }) {
                            Icon(AppIcons.Remove, contentDescription = "Kurangi")
                        }
                        Text(text = quantity.toString(), fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp))
                        IconButton(onClick = { quantity++ }) {
                            Icon(AppIcons.Add, contentDescription = "Tambah")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    selectedItem?.let { menu ->
                        val newItem = com.example.data.TransactionItem(
                            itemId = menu.id,
                            namaBarang = menu.nama,
                            qty = quantity,
                            harga = menu.harga.toLong(),
                            subTotal = quantity * menu.harga.toLong(),
                            servedQty = 0
                        )
                        storageHelper.addItemsToTransaction(transaction.kodeTransaksi, newItem)
                        storageHelper.updateTransactionStatus(transaction.kodeTransaksi, "PENDING")
                        nestedTransactions = storageHelper.getNestedTransactions()
                    }
                    showAddItemDialog = null
                }) {
                    Text("Simpan Tambahan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = null }) {
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
    onAddItemsClick: () -> Unit
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            order.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.qty}x ${item.namaBarang}")
                    }
                    if (!isReady) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (item.servedQty > 0) onUpdateItemServedQty(item.itemId, item.servedQty - 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = AppIcons.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = "${item.servedQty} / ${item.qty}",
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = { if (item.servedQty < item.qty) onUpdateItemServedQty(item.itemId, item.servedQty + 1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(imageVector = AppIcons.Add, contentDescription = "Tambah", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "${item.qty} / ${item.qty} Disajikan",
                            fontSize = 12.sp,
                            color = SuccessColor
                        )
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
