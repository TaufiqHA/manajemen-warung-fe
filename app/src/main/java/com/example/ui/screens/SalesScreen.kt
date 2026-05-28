package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.theme.DangerColor
import com.example.ui.theme.SuccessColor
import com.example.ui.viewmodel.SalesViewModel
import com.example.utils.formatRupiah
import com.example.utils.generateQuotationPdf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    
    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptText by remember { mutableStateOf("") }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaksi Penjualan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
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
                            label = { Text("Harga Satuan") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(2f)
                        )
                    }
                    
                    Button(
                        onClick = {
                            val q = qty.toIntOrNull() ?: 0
                            val h = harga.toLongOrNull() ?: 0L
                            if (searchQuery.isNotBlank() && q > 0 && h > 0) {
                                viewModel.addToCart(searchQuery, q, h)
                                // Reset fields
                                viewModel.onSearchQueryChanged("")
                                qty = "1"
                                harga = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
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
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = DangerColor)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // C. Bagian Bawah: Ringkasan & Aksi
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total Harga", style = MaterialTheme.typography.titleMedium)
                Text(
                    formatRupiah(viewModel.totalHarga),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SuccessColor
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (viewModel.cartItems.isNotEmpty()) {
                        val transaction = viewModel.processTransaction()
                        currentTransaction = transaction
                        receiptText = viewModel.formatReceipt(transaction)
                        showReceiptDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = viewModel.cartItems.isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
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
                        generateQuotationPdf(context, quotationData)
                    }
                }) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
            },
            confirmButton = {
                Button(onClick = { 
                    showReceiptDialog = false 
                    viewModel.clearCart()
                }) {
                    Text("Selesai")
                }
            }
        )
    }
}
