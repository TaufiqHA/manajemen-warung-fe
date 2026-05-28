package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val monthNames = DateFormatSymbols().months

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Bulanan per Item") },
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
                            leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
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
                    ExposedDropdownMenuBox(
                        expanded = itemExpanded,
                        onExpandedChange = { itemExpanded = it }
                    ) {
                        val selectedItemName = allItems.find { it.id == selectedItemId }?.name ?: "Pilih Item"
                        OutlinedTextField(
                            value = selectedItemName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Pilih Item") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemExpanded) },
                            leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = itemExpanded,
                            onDismissRequest = { itemExpanded = false }
                        ) {
                            allItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.name) },
                                    onClick = {
                                        viewModel.setItem(item.id)
                                        itemExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Rincian Harian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(reportData) { data ->
                        ReportRowItem(data)
                    }
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Tanggal ${data.date}", fontWeight = FontWeight.Bold)
            Text(text = formatRupiah(data.totalAmount.toLong()), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}
