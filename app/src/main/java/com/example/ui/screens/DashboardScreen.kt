package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.DangerColor
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.InfoColor
import com.example.ui.theme.PrimaryColor
import java.text.NumberFormat
import java.util.Locale

// Enumeration for Bottom Navigation Tabs
enum class DashboardTab(val label: String) {
    Beranda("Beranda"),
    Penjualan("Penjualan"),
    LabaRugi("Laba Rugi"),
    Biaya("Biaya"),
    Profil("Profil")
}

// Structures for stateful interaction
data class MenuItem(
    val id: String,
    val nama: String,
    val harga: Double
)

data class TransaksiHarian(
    val id: String,
    val namaItem: String,
    val jumlah: Int,
    val harga: Double,
    val waktu: String,
    val dicatatOleh: String,
    val catatan: String = ""
)

data class BiayaOperasional(
    val id: String,
    val kategori: String,
    val keterangan: String,
    val jumlah: Double,
    val tanggal: String,
    val pembuat: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(role: String, onLogout: () -> Unit) {
    var activeTab by remember { mutableStateOf(DashboardTab.Beranda) }
    
    // Live State Lists for local mockup persistence
    val menuList = remember {
        mutableStateListOf(
            MenuItem("1", "Mie Ayam Extra Pedas", 15000.0),
            MenuItem("2", "Nasi Goreng Spesial", 18000.0),
            MenuItem("3", "Es Teh Manis Jumbo", 4000.0),
            MenuItem("4", "Ayam Bakar Madu", 22000.0)
        )
    }

    val transaksiList = remember {
        mutableStateListOf(
            TransaksiHarian("1", "Mie Ayam Extra Pedas", 3, 15000.0, "08:30", "Admin", "Pedas level 5"),
            TransaksiHarian("2", "Nasi Goreng Spesial", 2, 18000.0, "09:15", "Owner", ""),
            TransaksiHarian("3", "Es Teh Manis Jumbo", 4, 4000.0, "11:00", "Admin", "Es dikit"),
            TransaksiHarian("4", "Ayam Bakar Madu", 2, 22000.0, "12:15", "Owner", "")
        )
    }

    val biayaList = remember {
        mutableStateListOf(
            BiayaOperasional("1", "Bahan Baku", "Beli daging ayam & bumbu dapur", 350000.0, "21 Mei 2026", "Admin"),
            BiayaOperasional("2", "Listrik", "Tagihan listrik bulanan toko", 280000.0, "20 Mei 2026", "Owner"),
            BiayaOperasional("3", "Air", "Pembayaran bulanan PAM", 120000.0, "18 Mei 2026", "Admin")
        )
    }

    // Role parameters
    var userName by remember { mutableStateOf(if (role == "Owner") "Budi Santoso" else "Siti Aminah") }
    val userEmail = if (role == "Owner") "budi@warung.com" else "siti@warung.com"

    // UI Feedback Overlay (Toast-like snackbars)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Tab representation list filtered by authorization role
    val tabsToShow = remember(role) {
        if (role == "Owner") {
            listOf(DashboardTab.Beranda, DashboardTab.Penjualan, DashboardTab.LabaRugi, DashboardTab.Biaya, DashboardTab.Profil)
        } else {
            listOf(DashboardTab.Beranda, DashboardTab.Penjualan, DashboardTab.Biaya, DashboardTab.Profil)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                tabsToShow.forEach { tab ->
                    val isSelected = activeTab == tab
                    val icon = when (tab) {
                        DashboardTab.Beranda -> Icons.Filled.Home
                        DashboardTab.Penjualan -> Icons.Filled.ShoppingCart
                        DashboardTab.LabaRugi -> Icons.Filled.TrendingUp
                        DashboardTab.Biaya -> Icons.Filled.Payments
                        DashboardTab.Profil -> Icons.Filled.Person
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tab },
                        icon = { Icon(imageVector = icon, contentDescription = tab.label) },
                        label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                DashboardTab.Beranda -> {
                    BerandaTabContent(
                        role = role,
                        userName = userName,
                        transaksiList = transaksiList,
                        onNavigateTab = { activeTab = it },
                        snackbarHostState = snackbarHostState,
                        biayaList = biayaList
                    )
                }
                DashboardTab.Penjualan -> {
                    PenjualanTabContent(
                        role = role,
                        transaksiList = transaksiList,
                        menuList = menuList,
                        snackbarHostState = snackbarHostState
                    )
                }
                DashboardTab.LabaRugi -> {
                    LabaRugiTabContent(
                        transaksiList = transaksiList,
                        biayaList = biayaList
                    )
                }
                DashboardTab.Biaya -> {
                    BiayaTabContent(
                        role = role,
                        biayaList = biayaList,
                        snackbarHostState = snackbarHostState
                    )
                }
                DashboardTab.Profil -> {
                    ProfilTabContent(
                        userName = userName,
                        userRole = role,
                        userEmail = userEmail,
                        onLogoutClick = onLogout,
                        onNameChange = { userName = it }
                    )
                }
            }
        }
    }
}

// ------------------------- HELPERS -------------------------
fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

// ------------------------- 1. BERANDA TAB -------------------------
@Composable
fun BerandaTabContent(
    role: String,
    userName: String,
    transaksiList: List<TransaksiHarian>,
    biayaList: List<BiayaOperasional>,
    onNavigateTab: (DashboardTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val totalPenjualanHarian = transaksiList.sumOf { it.jumlah * it.harga }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        // Welcoming Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Halo, $userName 👋",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Role Badge
                Surface(
                    color = if (role == "Owner") InfoColor.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = role,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (role == "Owner") InfoColor else Color.DarkGray
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Icon(
                imageVector = Icons.Filled.Storefront,
                contentDescription = "Store icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Kamis, 21 Mei 2026",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = "Ringkasan hari ini",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Total Sales Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💰 Total Penjualan Hari Ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatRupiah(totalPenjualanHarian),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${transaksiList.size} Transaksi Berhasil",
                    style = MaterialTheme.typography.labelMedium,
                    color = SuccessColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "RINGKASAN TRANSAKSI TAHUNAN",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Simple Beautiful Bar Chart Simulation
        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                // Dummy Data for Chart
                val chartData = listOf(0.4f, 0.6f, 0.3f, 0.8f, 0.5f, 0.9f, 0.7f)
                val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                
                chartData.forEachIndexed { index, value ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.12f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(value)
                                    .background(
                                        color = if (index == chartData.size - 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = labels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = if (index == chartData.size - 1) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

// ------------------------- 2. PENJUALAN TAB -------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PenjualanTabContent(
    role: String,
    transaksiList: MutableList<TransaksiHarian>,
    menuList: MutableList<MenuItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showActionChooser by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var showAddMenuForm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<TransaksiHarian?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<TransaksiHarian?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val totalPenjualanHarian = transaksiList.sumOf { it.jumlah * it.harga }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Penjualan Harian",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Kamis, 21 Mei 2026",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ringkasan Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
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
                        Text("Total Hari Ini", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(
                            text = formatRupiah(totalPenjualanHarian),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${transaksiList.size} Transaksi",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "DAFTAR TRANSAKSI",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (transaksiList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Storefront, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada transaksi hari ini", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transaksiList, key = { it.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    itemToDelete = item
                                    false
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> DangerColor
                                        else -> Color.Transparent
                                    }
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Hapus", tint = Color.White)
                                }
                            },
                            content = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedItemForDetail = item },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🍜", style = MaterialTheme.typography.titleSmall)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(item.namaItem, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "${item.jumlah} porsi × ${formatRupiah(item.harga)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "${item.waktu} · oleh ${item.dicatatOleh}",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                                    color = Color.LightGray
                                                )
                                            }
                                        }
                                        Text(
                                            text = formatRupiah(item.jumlah * item.harga),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = SuccessColor
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showActionChooser = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = Color.White)
        }

        if (showActionChooser) {
            AlertDialog(
                onDismissRequest = { showActionChooser = false },
                title = { Text("Pilih Tindakan", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showActionChooser = false; showAddForm = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tambah Transaksi Penjualan")
                        }
                        Button(
                            onClick = { showActionChooser = false; showAddMenuForm = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Tambah Menu Baru")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showActionChooser = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // ---------- ADD MENU MODAL ----------
        if (showAddMenuForm) {
            var namaMenu by remember { mutableStateOf("") }
            var hargaMenu by remember { mutableStateOf("") }

            var namaError by remember { mutableStateOf(false) }
            var hargaError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddMenuForm = false },
                title = { Text("Tambah Menu Baru") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = namaMenu,
                            onValueChange = {
                                namaMenu = it
                                namaError = false
                            },
                            label = { Text("Nama Menu *") },
                            placeholder = { Text("cth: Nasi Uduk") },
                            isError = namaError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (namaError) {
                            Text("Wajib diisi", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedTextField(
                            value = hargaMenu,
                            onValueChange = {
                                hargaMenu = it
                                hargaError = false
                            },
                            label = { Text("Harga (Rp) *") },
                            placeholder = { Text("cth: 15000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = hargaError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (hargaError) {
                            Text("Harga tidak valid", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val hargaDouble = hargaMenu.toDoubleOrNull()
                            if (namaMenu.isBlank()) namaError = true
                            if (hargaDouble == null || hargaDouble <= 0) hargaError = true

                            if (!namaError && !hargaError) {
                                menuList.add(
                                    MenuItem(
                                        id = java.util.UUID.randomUUID().toString(),
                                        nama = namaMenu,
                                        harga = hargaDouble ?: 0.0
                                    )
                                )
                                showAddMenuForm = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Menu berhasil ditambahkan", withDismissAction = true)
                                }
                            }
                        }
                    ) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddMenuForm = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // ---------- ADD FORM MODAL ----------
        if (showAddForm) {
            var namaItem by remember { mutableStateOf("") }
            var qty by remember { mutableStateOf("1") }
            var hargaSatuan by remember { mutableStateOf("") }
            var catatan by remember { mutableStateOf("") }

            var namaError by remember { mutableStateOf(false) }
            var hargaError by remember { mutableStateOf(false) }

            var expanded by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddForm = false },
                title = { Text("Tambah Transaksi") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = namaItem,
                                onValueChange = {
                                    namaItem = it
                                    namaError = false
                                },
                                label = { Text("Pilih/Cari Menu *") },
                                isError = namaError,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            
                            val filterOptions = menuList.filter { it.nama.contains(namaItem, ignoreCase = true) }
                            if (filterOptions.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filterOptions.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption.nama) },
                                            onClick = {
                                                namaItem = selectionOption.nama
                                                hargaSatuan = selectionOption.harga.toInt().toString()
                                                expanded = false
                                                namaError = false
                                                hargaError = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        if (namaError) {
                            Text("Menu wajib dipilih/diisi", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }

                        // Stepper qty count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jumlah (Porsi/Pcs)", style = MaterialTheme.typography.bodyMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val current = qty.toIntOrNull() ?: 1
                                    if (current > 1) qty = (current - 1).toString()
                                }) {
                                    Icon(imageVector = Icons.Filled.Remove, contentDescription = "Kurang")
                                }
                                Text(
                                    text = qty,
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(onClick = {
                                    val current = qty.toIntOrNull() ?: 1
                                    qty = (current + 1).toString()
                                }) {
                                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Tambah")
                                }
                            }
                        }

                        OutlinedTextField(
                            value = hargaSatuan,
                            onValueChange = {
                                hargaSatuan = it
                                hargaError = false
                            },
                            label = { Text("Harga Satuan (Rp) *") },
                            placeholder = { Text("cth: 15000") },
                            isError = hargaError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (hargaError) {
                            Text("Harga wajib diisi dan valid", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }

                        // Auto-computed Total
                        val quantityNum = qty.toIntOrNull() ?: 1
                        val priceNum = hargaSatuan.toDoubleOrNull() ?: 0.0
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Total: ${formatRupiah(quantityNum * priceNum)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = catatan,
                            onValueChange = { catatan = it },
                            label = { Text("Catatan (Opsional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (namaItem.isBlank()) {
                                namaError = true
                                return@Button
                            }
                            val priceValue = hargaSatuan.toDoubleOrNull()
                            if (priceValue == null || priceValue <= 0) {
                                hargaError = true
                                return@Button
                            }

                            // Dynamic registration
                            val uniqueId = (transaksiList.size + 1).toString()
                            transaksiList.add(
                                TransaksiHarian(
                                    id = uniqueId,
                                    namaItem = namaItem,
                                    jumlah = qty.toIntOrNull() ?: 1,
                                    harga = priceValue,
                                    waktu = "10:30",
                                    dicatatOleh = role,
                                    catatan = catatan
                                )
                            )
                            showAddForm = false
                        }
                    ) {
                        Text("SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddForm = false }) {
                        Text("BATAL")
                    }
                }
            )
        }

        // ---------- CONFIRM DELETE DIALOG ----------
        itemToDelete?.let { item ->
            ConfirmDialog(
                title = "Hapus Transaksi?",
                text = "Transaksi ${item.namaItem} senilai ${formatRupiah(item.jumlah * item.harga)} akan dihapus.",
                onConfirm = {
                    transaksiList.remove(item)
                    itemToDelete = null
                },
                onDismiss = { itemToDelete = null }
            )
        }

        // ---------- DETAIL TRANSACTION COMPONENT ----------
        selectedItemForDetail?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForDetail = null },
                title = { Text("Detail Transaksi", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🍜", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.namaItem, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jumlah", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text("${item.jumlah} porsi", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Harga Satuan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(formatRupiah(item.harga), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(formatRupiah(item.jumlah * item.harga), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = SuccessColor)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(item.waktu, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dicatat oleh", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(item.dicatatOleh, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (item.catatan.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Catatan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Text(item.catatan, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedItemForDetail = null }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}

// ------------------------- 3. BIAYA TAB -------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiayaTabContent(
    role: String,
    biayaList: MutableList<BiayaOperasional>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showAddForm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<BiayaOperasional?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<BiayaOperasional?>(null) }

    val categories = listOf("Semua", "Bahan Baku", "Listrik", "Gaji", "Air", "Lainnya")

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Laporan Biaya",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            var selectedDateFilter by remember { mutableStateOf("Bulan Ini") }
            val dates = listOf("Hari Ini", "Bulan Ini", "Semua")

            // Date filtering list chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates) { dateLabel ->
                    val isSelected = selectedDateFilter == dateLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDateFilter = dateLabel },
                        label = { Text(dateLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logic to calculate totals
            val (bahanBakuList, opsList, dllList) = remember(biayaList, selectedDateFilter) {
                // In a real app we would filter exactly by dates, for UI we just group them.
                val a = biayaList.filter { it.kategori == "Bahan Baku" }
                val b = biayaList.filter { it.kategori in listOf("Listrik", "Gaji", "Air") || it.kategori == "Biaya Operasional" }
                val c = biayaList.filter { it.kategori == "Lainnya" || (it.kategori != "Bahan Baku" && it.kategori !in listOf("Listrik", "Gaji", "Air", "Biaya Operasional")) }
                Triple(a, b, c)
            }

            val totalA = bahanBakuList.sumOf { it.jumlah }
            val totalB = opsList.sumOf { it.jumlah }
            val totalC = dllList.sumOf { it.jumlah }
            val grandTotal = totalA + totalB + totalC

            // Rekap Master Section
            Text(
                text = "Rekap",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "A. Biaya Bahan Baku", style = MaterialTheme.typography.bodyMedium)
                        Text(text = formatRupiah(totalA), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "B. Biaya Operasional", style = MaterialTheme.typography.bodyMedium)
                        Text(text = formatRupiah(totalB), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "C. Biaya dll", style = MaterialTheme.typography.bodyMedium)
                        Text(text = formatRupiah(totalC), style = MaterialTheme.typography.bodyMedium)
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Jumlah", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(text = formatRupiah(grandTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = DangerColor)
                    }
                }
            }

            // A. Biaya Bahan Baku Details
            Text(
                text = "A. Biaya Bahan Baku",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (bahanBakuList.isEmpty()) {
                        Text("Tidak ada data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        bahanBakuList.forEachIndexed { index, b ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${index + 1}. ${b.keterangan}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = formatRupiah(b.jumlah), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Jumlah", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = formatRupiah(totalA), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // B. Biaya Operasional Details
            Text(
                text = "B. Biaya Operasional",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (opsList.isEmpty()) {
                        Text("Tidak ada data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        opsList.forEachIndexed { index, b ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${index + 1}. ${b.keterangan} (${b.kategori})", style = MaterialTheme.typography.bodyMedium)
                                Text(text = formatRupiah(b.jumlah), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Jumlah", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = formatRupiah(totalB), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // C. Biaya dll Details
            Text(
                text = "C. Biaya dll",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (dllList.isEmpty()) {
                        Text("Tidak ada data", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        dllList.forEachIndexed { index, b ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${index + 1}. ${b.keterangan}", style = MaterialTheme.typography.bodyMedium)
                                Text(text = formatRupiah(b.jumlah), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Jumlah", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text(text = formatRupiah(totalC), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(72.dp)) // space for FAB
        }

        // FAB to add new operational cost
        FloatingActionButton(
            onClick = { showAddForm = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Tambah Biaya", tint = Color.White)
        }

        // ---------- ADD BIAYA MODAL ----------
        if (showAddForm) {
            var selectedKategori by remember { mutableStateOf("Bahan Baku") }
            var keterangan by remember { mutableStateOf("") }
            var jumlahStr by remember { mutableStateOf("") }

            var keteranganError by remember { mutableStateOf(false) }
            var jumlahError by remember { mutableStateOf(false) }

            var expandedDropdown by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddForm = false },
                title = { Text("Tambah Biaya Operasional") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Custom dropdown choice represent for kategori
                        Text("Kategori *", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedTextField(
                                value = selectedKategori,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth().clickable { expandedDropdown = true },
                                trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }
                            )
                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                categories.filter { it != "Semua" }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedKategori = cat
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = keterangan,
                            onValueChange = {
                                keterangan = it
                                keteranganError = false
                            },
                            label = { Text("Keterangan *") },
                            placeholder = { Text("cth: Pembelian telur") },
                            isError = keteranganError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (keteranganError) {
                            Text("Keterangan wajib diisi", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedTextField(
                            value = jumlahStr,
                            onValueChange = {
                                jumlahStr = it
                                jumlahError = false
                            },
                            label = { Text("Jumlah Biaya (Rp) *") },
                            placeholder = { Text("cth: 50000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = jumlahError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (jumlahError) {
                            Text("Jumlah wajib berupa nominal angka valid", color = DangerColor, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (keterangan.isBlank()) {
                                keteranganError = true
                                return@Button
                            }
                            val nominalVal = jumlahStr.toDoubleOrNull()
                            if (nominalVal == null || nominalVal <= 0) {
                                jumlahError = true
                                return@Button
                            }

                            biayaList.add(
                                BiayaOperasional(
                                    id = (biayaList.size + 1).toString(),
                                    kategori = selectedKategori,
                                    keterangan = keterangan,
                                    jumlah = nominalVal,
                                    tanggal = "21 Mei 2026",
                                    pembuat = role
                                )
                            )
                            showAddForm = false
                        }
                    ) {
                        Text("SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddForm = false }) {
                        Text("BATAL")
                    }
                }
            )
        }

        // ---------- CONFIRM DELETE DIALOG ----------
        itemToDelete?.let { item ->
            ConfirmDialog(
                title = "Hapus Riwayat Biaya?",
                text = "Catatan pengeluaran ${item.keterangan} senilai ${formatRupiah(item.jumlah)} akan dihapus permanen.",
                onConfirm = {
                    biayaList.remove(item)
                    itemToDelete = null
                },
                onDismiss = { itemToDelete = null }
            )
        }

        // ---------- DETAIL TRANSACTION COMPONENT ----------
        selectedItemForDetail?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForDetail = null },
                title = { Text("Detail Biaya Operasional", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val iconStr = when (item.kategori) {
                            "Bahan Baku" -> "🛒"
                            "Listrik" -> "⚡"
                            "Gaji" -> "👷"
                            "Air" -> "💧"
                            else -> "📦"
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(iconStr, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(item.kategori, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Divider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Keterangan", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(item.keterangan, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Jumlah Biaya", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(formatRupiah(item.jumlah), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = DangerColor)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Waktu", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(item.tanggal, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Dicatat oleh", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            Text(item.pembuat, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedItemForDetail = null }) {
                        Text("Tutup")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            selectedItemForDetail = null
                            itemToDelete = item
                        }
                    ) {
                        Text("Hapus", color = DangerColor)
                    }
                }
            )
        }
    }
}

// ------------------------- 5. PROFIL TAB -------------------------
@Composable
fun ProfilTabContent(
    userName: String,
    userRole: String,
    userEmail: String,
    onLogoutClick: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var currentNameState by remember { mutableStateOf(userName) }
    var displayPasswordChange by remember { mutableStateOf(false) }
    var displayNamaChange by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        // App header title
        Text(
            text = "Profil Pengguna",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Initials avatar circle shape
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
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
                text = initials.uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 28.sp),
                fontWeight = FontWeight.ExtraBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(currentNameState, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(userEmail, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = userRole,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick settings items
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { displayNamaChange = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("👤 Ubah Nama", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { displayPasswordChange = true }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒 Ubah Password", style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
                Divider(modifier = Modifier.padding(horizontal = 12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ℹ️ Versi Aplikasi", style = MaterialTheme.typography.bodyMedium)
                    Text("1.0.0", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout Button
        Button(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Filled.ExitToApp, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("KELUAR", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }

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

// ------------------------- 5. LABA RUGI TAB -------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabaRugiTabContent(
    transaksiList: List<TransaksiHarian>,
    biayaList: List<BiayaOperasional>,
    modifier: Modifier = Modifier
) {
    var selectedLabaDateFilter by remember { mutableStateOf("Bulan Ini") }
    val labaDateFilters = listOf("Hari Ini", "7 Hari", "Bulan Ini", "Semua")

    // Dynamic scale modifier based on filter to show different metrics dynamically
    val multiplier = when (selectedLabaDateFilter) {
        "Hari Ini" -> 0.1
        "7 Hari" -> 0.4
        "Bulan Ini" -> 1.0
        else -> 2.5
    }

    val totalPemasukan = transaksiList.sumOf { it.jumlah * it.harga } * multiplier
    val totalBiaya = biayaList.sumOf { it.jumlah } * multiplier
    val labaBersih = totalPemasukan - totalBiaya

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Inventori Laba-Rugi",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Laporan Performa Keuangan",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Date selection chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(labaDateFilters) { dateFilter ->
                val isSelected = selectedLabaDateFilter == dateFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedLabaDateFilter = dateFilter },
                    label = { Text(dateFilter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Rekap",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // A. Laporan Global Penjualan
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "A. Laporan Global Penjualan ($selectedLabaDateFilter)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Total Penjualan:", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = formatRupiah(totalPemasukan),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColor
                    )
                }
            }
        }

        // B. Laporan per item penjualan
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "B. Laporan per item penjualan ($selectedLabaDateFilter)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (transaksiList.isEmpty()) {
                    Text(
                        text = "Tidak ada data penjualan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    // Group by item name
                    val groupedItems = transaksiList.groupBy { it.namaItem }
                    groupedItems.forEach { (itemName, items) ->
                        val itemQuantity = items.sumOf { it.jumlah } * multiplier
                        val itemTotal = items.sumOf { it.jumlah * it.harga } * multiplier
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "$itemName (${itemQuantity.toInt()}x)", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = formatRupiah(itemTotal),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // C. Laporan Laba-Rugi
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (labaBersih >= 0) SuccessColor.copy(alpha = 0.1f) else DangerColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "C. Laporan Laba-Rugi",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(Penjualan - Pengeluaran)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Penjualan", style = MaterialTheme.typography.bodyMedium)
                    Text(text = formatRupiah(totalPemasukan), style = MaterialTheme.typography.bodyMedium, color = SuccessColor)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Pengeluaran", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "- ${formatRupiah(totalBiaya)}", style = MaterialTheme.typography.bodyMedium, color = DangerColor)
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Laba Bersih:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRupiah(labaBersih),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (labaBersih >= 0) SuccessColor else DangerColor
                    )
                }
            }
        }
    }
}
