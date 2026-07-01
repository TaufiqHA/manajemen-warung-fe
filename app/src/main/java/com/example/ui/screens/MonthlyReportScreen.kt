package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AppIcons
import com.example.data.DailyItemReport
import com.example.data.Item
import com.example.ui.viewmodel.ReportViewModel
import com.example.utils.formatRupiah
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = viewModel()
) {
    val reportData by viewModel.monthlyItemReport.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedItemId by viewModel.selectedItemId.collectAsState()
    val allItems by viewModel.allItems.collectAsState()

    var monthExpanded by remember { mutableStateOf(false) }
    var itemExpanded by remember { mutableStateOf(false) }
    var itemSearchQuery by remember { mutableStateOf("") }
    var showRincian by remember(selectedMonth, selectedItemId) { mutableStateOf(false) }

    LaunchedEffect(selectedItemId, allItems) {
        if (selectedItemId != null) {
            val name = allItems.find { it.id == selectedItemId }?.name
            if (name != null && itemSearchQuery != name) {
                itemSearchQuery = name
            }
        }
    }

    val monthNames = DateFormatSymbols().months

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Bulanan per Item") },
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
            // --- Bagian Atas: Filter ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Filter Laporan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown Bulan
                    ExposedDropdownMenuBox(
                        expanded = monthExpanded,
                        onExpandedChange = { monthExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = monthNames[selectedMonth - 1],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Bulan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
                            leadingIcon = { Icon(AppIcons.Calendar, contentDescription = null) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = monthExpanded,
                            onDismissRequest = { monthExpanded = false }
                        ) {
                            for (i in 1..12) {
                                DropdownMenuItem(
                                    text = { Text(monthNames[i - 1]) },
                                    onClick = {
                                        viewModel.setMonth(i)
                                        monthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dropdown Item
                    val filteredItems = remember(itemSearchQuery, allItems) {
                        if (itemSearchQuery.length < 3) {
                            emptyList()
                        } else {
                            allItems.filter { it.name.contains(itemSearchQuery, ignoreCase = true) }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = itemExpanded,
                        onExpandedChange = { 
                            if (it && itemSearchQuery.length >= 3 && filteredItems.isNotEmpty()) {
                                itemExpanded = true
                            } else if (!it) {
                                itemExpanded = false
                            }
                        }
                    ) {
                        OutlinedTextField(
                            value = itemSearchQuery,
                            onValueChange = { newValue ->
                                itemSearchQuery = newValue
                                itemExpanded = newValue.length >= 3 && allItems.any { it.name.contains(newValue, ignoreCase = true) }
                            },
                            label = { Text("Pilih Item") },
                            placeholder = { Text("Ketik min. 3 huruf (cth: kopi)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                            leadingIcon = { Icon(AppIcons.Product, contentDescription = null) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth(),
                            singleLine = true
                        )
                        if (filteredItems.isNotEmpty() && itemSearchQuery.length >= 3) {
                            ExposedDropdownMenu(
                                expanded = itemExpanded,
                                onDismissRequest = { itemExpanded = false }
                            ) {
                                filteredItems.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            itemSearchQuery = item.name
                                            viewModel.setItem(item.id)
                                            itemExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // --- Bagian Bawah: Hasil Laporan ---
            if (selectedItemId == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Silakan pilih item terlebih dahulu", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (reportData.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Tidak ada data penjualan untuk item ini di bulan ${monthNames[selectedMonth - 1]}.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                val totalQty = remember(reportData) { reportData.sumOf { it.qty } }
                val totalAmount = remember(reportData) { reportData.sumOf { it.totalAmount } }

                Text("Ringkasan Penjualan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Penjualan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = "$totalQty Item",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Pendapatan", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            Text(
                                text = formatRupiah(totalAmount.toLong()),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showRincian = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(AppIcons.List, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Lihat Rincian")
                        }
                    }
                }

                if (showRincian) {
                    val selectedItemName = allItems.find { it.id == selectedItemId }?.name ?: "Item"
                    AlertDialog(
                        onDismissRequest = { showRincian = false },
                        title = {
                            Text("Rincian Penjualan: $selectedItemName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        },
                        text = {
                            Box(modifier = Modifier.heightIn(max = 400.dp)) {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    items(reportData) { data ->
                                        ReportRowItem(data)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(onClick = { showRincian = false }) {
                                Text("Tutup")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportRowItem(data: DailyItemReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Tanggal ${data.date}", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "${data.qty} Item terjual", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text = formatRupiah(data.totalAmount.toLong()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
