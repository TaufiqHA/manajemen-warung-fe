package com.example.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.ui.components.AppIcons
import com.example.ui.components.ConfirmDialog
import com.example.ui.theme.DangerColor
import com.example.ui.theme.SuccessColor
import com.example.ui.theme.InfoColor
import com.example.utils.formatRupiah
import com.example.data.UserRole
import com.example.data.RincianHarian
import com.example.data.MenuTerlaris
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodel.SalesViewModel
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.text.font.FontFamily
import com.example.utils.generateQuotationPdf
import com.example.utils.generateLaporanBiayaPdf
import com.example.utils.generateLabaRugiPdf
import com.example.data.InvoiceItem
import com.example.data.TransactionModel
import com.example.data.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Enumeration for Bottom Navigation Tabs
enum class DashboardTab(val label: String) {
    Beranda("Beranda"),
    Penjualan("Penjualan"),
    ManajemenBarang("Barang"),
    LabaRugi("Laba Rugi"),
    Biaya("Biaya"),
    Profil("Profile")
}

// Struktur data untuk setiap item menu
data class DashboardMenu(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tab: DashboardTab,
    val allowedRoles: List<UserRole>
)

// Konfigurasi hak akses menu
val allMenus = listOf(
    DashboardMenu(
        title = "Beranda",
        icon = AppIcons.Dashboard,
        tab = DashboardTab.Beranda,
        allowedRoles = listOf(UserRole.OWNER, UserRole.ADMIN_TOKO, UserRole.ADMIN_KANTOR)
    ),
    DashboardMenu(
        title = "Laba Rugi",
        icon = AppIcons.TrendingUp,
        tab = DashboardTab.LabaRugi,
        allowedRoles = listOf(UserRole.OWNER)
    ),
    DashboardMenu(
        title = "Penjualan",
        icon = AppIcons.Transaction,
        tab = DashboardTab.Penjualan,
        allowedRoles = listOf(UserRole.ADMIN_TOKO)
    ),
    DashboardMenu(
        title = "Barang",
        icon = AppIcons.Product,
        tab = DashboardTab.ManajemenBarang,
        allowedRoles = listOf(UserRole.ADMIN_TOKO)
    ),
    DashboardMenu(
        title = "Biaya Operasional",
        icon = AppIcons.Payments,
        tab = DashboardTab.Biaya,
        allowedRoles = listOf(UserRole.ADMIN_KANTOR)
    ),
    DashboardMenu(
        title = "Profile",
        icon = AppIcons.Profile,
        tab = DashboardTab.Profil,
        allowedRoles = listOf(UserRole.OWNER, UserRole.ADMIN_TOKO, UserRole.ADMIN_KANTOR)
    )
)

// Structures for stateful interaction
data class MenuItem(
    val id: String,
    val nama: String,
    val harga: Double
)

data class TransaksiHarian(
    val idTransaksi: String,
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

@Composable
fun MenuCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    role: UserRole, 
    onLogout: () -> Unit, 
    onNavigateToSales: () -> Unit,
    onNavigateToMonthlyReport: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    var activeTab by remember { mutableStateOf(DashboardTab.Beranda) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val storageHelper = remember { com.example.utils.LocalStorageHelper(context) }
    
    // Live State Lists for local mockup persistence
    val menuList = remember {
        mutableStateListOf<MenuItem>().apply {
            addAll(storageHelper.getMenuList())
        }
    }

    val transaksiList = remember {
        mutableStateListOf<TransaksiHarian>().apply {
            addAll(storageHelper.getTransaksiList())
        }
    }

    val biayaList = remember {
        mutableStateListOf<BiayaOperasional>().apply {
            addAll(storageHelper.getBiayaList())
        }
    }

    // Auto-save side effects when list content changes
    LaunchedEffect(menuList.toList()) {
        storageHelper.saveMenuList(menuList)
    }

    LaunchedEffect(transaksiList.toList()) {
        storageHelper.saveTransaksiList(transaksiList)
    }

    LaunchedEffect(biayaList.toList()) {
        storageHelper.saveBiayaList(biayaList)
    }

    // Role parameters
    var userName by remember { 
        mutableStateOf(
            when (role) {
                UserRole.OWNER -> "Budi Santoso"
                UserRole.ADMIN_KANTOR -> "Andi Kantor"
                else -> "Siti Aminah"
            }
        ) 
    }
    val userEmail = when (role) {
        UserRole.OWNER -> "owner@warung.com"
        UserRole.ADMIN_KANTOR -> "adminkantor@warung.com"
        else -> "admin@warung.com"
    }

    // UI Feedback Overlay (Toast-like snackbars)
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Filter menus based on role
    val visibleMenus = remember(role) {
        allMenus.filter { it.allowedRoles.contains(role) }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                visibleMenus.forEach { menu ->
                    val isSelected = activeTab == menu.tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = menu.tab },
                        icon = { Icon(imageVector = menu.icon, contentDescription = menu.title) },
                        label = { Text(menu.title, style = MaterialTheme.typography.labelMedium) }
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
                        role = role.displayName,
                        userName = userName,
                        transaksiList = transaksiList,
                        visibleMenus = visibleMenus,
                        onNavigateTab = { activeTab = it },
                        snackbarHostState = snackbarHostState,
                        biayaList = biayaList,
                        onLogoutClick = if (role == UserRole.OWNER) onLogout else null
                    )
                }
                DashboardTab.Penjualan -> {
                    PenjualanTabContent(
                        role = role.displayName,
                        transaksiList = transaksiList,
                        menuList = menuList,
                        snackbarHostState = snackbarHostState,
                        onNavigateToSales = onNavigateToSales
                    )
                }
                DashboardTab.ManajemenBarang -> {
                    BarangTabContent(
                        role = role.displayName,
                        menuList = menuList,
                        snackbarHostState = snackbarHostState
                    )
                }
                DashboardTab.LabaRugi -> {
                    LabaRugiTabContent(
                        transaksiList = transaksiList,
                        biayaList = biayaList,
                        onNavigateToMonthlyReport = onNavigateToMonthlyReport
                    )
                }
                DashboardTab.Biaya -> {
                    BiayaTabContent(
                        role = role.displayName,
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
                        onNameChange = { userName = it },
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
            }
        }
    }
}

// ------------------------- 1. BERANDA TAB -------------------------
@Composable
fun BerandaTabContent(
    role: String,
    userName: String,
    transaksiList: List<TransaksiHarian>,
    biayaList: List<BiayaOperasional>,
    visibleMenus: List<DashboardMenu>,
    onNavigateTab: (DashboardTab) -> Unit,
    snackbarHostState: SnackbarHostState,
    onLogoutClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val totalPenjualanHarian = transaksiList.sumOf { it.jumlah * it.harga }

    // Kelompokkan berdasarkan idTransaksi untuk mendapatkan jumlah struk riil
    val groupedTransactions = transaksiList.groupBy { it.idTransaksi }

    // Hitung transaksi yang batal (jika ada item di struk tersebut yang berawalan ❌)
    val canceledTransactionsCount = groupedTransactions.count { (_, items) -> 
        items.any { it.namaItem.startsWith("❌") } 
    }

    // Hitung transaksi yang berhasil (total struk dikurangi yang batal)
    val successfulTransactionsCount = groupedTransactions.size - canceledTransactionsCount

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onLogoutClick != null) {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = AppIcons.Logout,
                            contentDescription = "Logout",
                            tint = DangerColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(
                    imageVector = AppIcons.Store,
                    contentDescription = "Store icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
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
                val summaryText = if (canceledTransactionsCount > 0) {
                    "$successfulTransactionsCount Transaksi Berhasil • $canceledTransactionsCount Dibatalkan"
                } else {
                    "$successfulTransactionsCount Transaksi Berhasil"
                }

                Text(
                    text = summaryText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (canceledTransactionsCount > 0) Color.Gray else SuccessColor
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
                // Dynamic Data for Chart from transaksiList
                val labels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                val counts = remember(transaksiList.toList()) {
                    val arr = IntArray(7)
                    val calendar = java.util.Calendar.getInstance()
                    transaksiList.forEach { trx ->
                        try {
                            val dateStr = if (trx.idTransaksi.startsWith("TRX-") && trx.idTransaksi.length >= 12) {
                                trx.idTransaksi.substring(4, 12)
                            } else {
                                ""
                            }
                            if (dateStr.isNotEmpty()) {
                                val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
                                val date = sdf.parse(dateStr)
                                if (date != null) {
                                    calendar.time = date
                                    val dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                                    val index = when (dayOfWeek) {
                                        java.util.Calendar.MONDAY -> 0
                                        java.util.Calendar.TUESDAY -> 1
                                        java.util.Calendar.WEDNESDAY -> 2
                                        java.util.Calendar.THURSDAY -> 3
                                        java.util.Calendar.FRIDAY -> 4
                                        java.util.Calendar.SATURDAY -> 5
                                        java.util.Calendar.SUNDAY -> 6
                                        else -> 0
                                    }
                                    arr[index] += trx.jumlah
                                }
                            }
                        } catch (e: Exception) {
                            // ignore
                        }
                    }
                    arr
                }

                val maxCount = remember(counts) { counts.maxOrNull() ?: 0 }
                val chartData = remember(counts, maxCount) {
                    counts.map { count ->
                        if (maxCount > 0) count.toFloat() / maxCount else 0f
                    }
                }
                
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
    onNavigateToSales: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showActionChooser by remember { mutableStateOf(false) }
    var showAddForm by remember { mutableStateOf(false) }
    var showAddMenuForm by remember { mutableStateOf(false) }
    var transactionIdToDelete by remember { mutableStateOf<String?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<TransaksiHarian?>(null) }
    var showCancelConfirmation by remember { mutableStateOf<TransaksiHarian?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val storageHelper = remember { com.example.utils.LocalStorageHelper(context) }
    val salesViewModel: SalesViewModel = viewModel()

    var showReceiptDialog by remember { mutableStateOf(false) }
    var receiptText by remember { mutableStateOf("") }
    var currentTransaction by remember { mutableStateOf<Transaction?>(null) }

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
            val groupedTransactions = remember(transaksiList.toList()) {
                transaksiList.groupBy { it.idTransaksi }
            }
            val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

            if (transaksiList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(AppIcons.Store, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Belum ada transaksi hari ini", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (trxId, itemsInTrx) ->
                        item(key = trxId) {
                            val isExpanded = expandedStates[trxId] ?: false
                            val totalTrxPrice = itemsInTrx.sumOf { it.jumlah * it.harga }
                            val totalItems = itemsInTrx.size
                            val time = itemsInTrx.firstOrNull()?.waktu ?: ""
                            val cashier = itemsInTrx.firstOrNull()?.dicatatOleh ?: ""
                            val isCanceled = itemsInTrx.any { it.namaItem.startsWith("❌") }

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        transactionIdToDelete = trxId
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
                                        Icon(AppIcons.Delete, contentDescription = "Hapus", tint = Color.White)
                                    }
                                },
                                content = {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedStates[trxId] = !isExpanded },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
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
                                                        Text("🧾", style = MaterialTheme.typography.titleSmall)
                                                    }
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Column {
                                                        Text(trxId, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (isCanceled) DangerColor else Color.Unspecified)
                                                        Text(
                                                            text = "$time · $totalItems item · oleh $cashier",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = formatRupiah(totalTrxPrice.toLong()),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isCanceled) DangerColor else SuccessColor
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Expand/Collapse",
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            AnimatedVisibility(visible = isExpanded) {
                                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                                    Divider(modifier = Modifier.padding(bottom = 8.dp))
                                                    itemsInTrx.forEach { detail ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                                                .clickable { selectedItemForDetail = detail },
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column {
                                                                Text(detail.namaItem, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = if (detail.namaItem.startsWith("❌")) DangerColor else Color.Unspecified)
                                                                Text(
                                                                    text = "${detail.jumlah} × ${formatRupiah(detail.harga.toLong())}",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    color = Color.Gray
                                                                )
                                                                if (detail.catatan.isNotBlank()) {
                                                                    Text(
                                                                        text = "Catatan: ${detail.catatan}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = Color.LightGray
                                                                    )
                                                                }
                                                            }
                                                            Text(
                                                                text = formatRupiah((detail.jumlah * detail.harga).toLong()),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            )
                        }
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
            Icon(AppIcons.Add, contentDescription = "Tambah", tint = Color.White)
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
                            onClick = { 
                                showActionChooser = false
                                onNavigateToSales()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Tambah Transaksi Penjualan")
                        }
                        if (role != UserRole.ADMIN_TOKO.displayName) {
                            Button(
                                onClick = { showActionChooser = false; showAddMenuForm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("Tambah Menu Baru")
                            }
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
                                    Icon(imageVector = AppIcons.Remove, contentDescription = "Kurang")
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
                                    Icon(imageVector = AppIcons.Add, contentDescription = "Tambah")
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
                            val trxId = "TRX-20260521-00" + (transaksiList.distinctBy { it.idTransaksi }.size + 1)
                            transaksiList.add(
                                TransaksiHarian(
                                    idTransaksi = trxId,
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
        transactionIdToDelete?.let { trxId ->
            val itemsToDelete = transaksiList.filter { it.idTransaksi == trxId }
            val totalValue = itemsToDelete.sumOf { it.jumlah * it.harga }
            ConfirmDialog(
                title = "Hapus Transaksi?",
                text = "Seluruh item pada Transaksi $trxId senilai ${formatRupiah(totalValue)} akan dihapus.",
                onConfirm = {
                    transaksiList.removeAll(itemsToDelete)
                    transactionIdToDelete = null
                },
                onDismiss = { transactionIdToDelete = null }
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                val transaction = storageHelper.getNestedTransactions().find { it.kodeTransaksi == item.idTransaksi }
                                if (transaction != null) {
                                    currentTransaction = transaction
                                    receiptText = salesViewModel.formatReceipt(transaction)
                                    showReceiptDialog = true
                                } else {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Data transaksi tidak ditemukan")
                                    }
                                }
                                selectedItemForDetail = null
                            }
                        ) {
                            Text("Cetak Ulang")
                        }
                        TextButton(onClick = { selectedItemForDetail = null }) {
                            Text("Tutup")
                        }
                    }
                },
                dismissButton = {
                    if (!item.namaItem.startsWith("❌")) {
                        TextButton(
                            onClick = {
                                showCancelConfirmation = item
                            }
                        ) {
                            Text("Batalkan", color = DangerColor)
                        }
                    }
                }
            )
        }

        if (showReceiptDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showReceiptDialog = false
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
                                generateQuotationPdf(context, quotationData, salesViewModel.namaWarungState.value)
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
                        showReceiptDialog = false 
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
                                        salesViewModel.printToThermal(device, receiptText) { success ->
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

        showCancelConfirmation?.let { cancelItem ->
            ConfirmDialog(
                title = "Konfirmasi Pembatalan",
                text = "Apakah anda yakin membatalkan penjualan ini?",
                onConfirm = {
                    val trxId = cancelItem.idTransaksi
                    val indices = transaksiList.withIndex().filter { it.value.idTransaksi == trxId }.map { it.index }
                    indices.forEach { idx ->
                        val currentItem = transaksiList[idx]
                        if (!currentItem.namaItem.startsWith("❌")) {
                            transaksiList[idx] = currentItem.copy(
                                namaItem = "❌ [BATAL] ${currentItem.namaItem}",
                                harga = 0.0
                            )
                        }
                    }
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Transaksi ${cancelItem.idTransaksi} berhasil dibatalkan")
                    }
                    
                    showCancelConfirmation = null
                    selectedItemForDetail = null 
                },
                onDismiss = {
                    showCancelConfirmation = null
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
    val context = androidx.compose.ui.platform.LocalContext.current
    var showAddForm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<BiayaOperasional?>(null) }
    var selectedItemForDetail by remember { mutableStateOf<BiayaOperasional?>(null) }
    var itemToEdit by remember { mutableStateOf<BiayaOperasional?>(null) }

    val categories = listOf("Semua", "Bahan Baku", "Biaya Operasional", "Biaya dll")

    var selectedDateFilter by remember { mutableStateOf("Bulan Ini") }
    val dates = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Bulan Lalu", "Semua")

    fun getCalendarForBiaya(tanggalStr: String): java.util.Calendar? {
        return try {
            val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("in", "ID"))
            val date = sdf.parse(tanggalStr) ?: return null
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal
        } catch (e: Exception) {
            null
        }
    }

    fun isBiayaMatchingFilter(tanggalStr: String, filter: String): Boolean {
        val cal = getCalendarForBiaya(tanggalStr) ?: return false
        val today = java.util.Calendar.getInstance()
        return when (filter) {
            "Hari Ini" -> cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
            "Minggu Ini" -> cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.WEEK_OF_YEAR) == today.get(java.util.Calendar.WEEK_OF_YEAR)
            "Bulan Ini" -> cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH)
            "Bulan Lalu" -> {
                val lastMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
                cal.get(java.util.Calendar.YEAR) == lastMonth.get(java.util.Calendar.YEAR) && cal.get(java.util.Calendar.MONTH) == lastMonth.get(java.util.Calendar.MONTH)
            }
            else -> true
        }
    }

    val filteredList = remember(biayaList.toList(), selectedDateFilter) {
        if (selectedDateFilter == "Semua") biayaList else biayaList.filter { isBiayaMatchingFilter(it.tanggal, selectedDateFilter) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Laporan Biaya",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { generateLaporanBiayaPdf(context, filteredList) }) {
                    Icon(imageVector = AppIcons.Pdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

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
            val (bahanBakuList, opsList, dllList) = remember(filteredList) {
                val a = filteredList.filter { it.kategori == "Bahan Baku" }
                val b = filteredList.filter { it.kategori in listOf("Listrik", "Gaji", "Air") || it.kategori == "Biaya Operasional" }
                val c = filteredList.filter { it.kategori == "Lainnya" || (it.kategori != "Bahan Baku" && it.kategori !in listOf("Listrik", "Gaji", "Air", "Biaya Operasional")) }
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
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedItemForDetail = b }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedItemForDetail = b }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "${index + 1}. ${b.keterangan}", style = MaterialTheme.typography.bodyMedium)
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
                            Row(modifier = Modifier.fillMaxWidth().clickable { selectedItemForDetail = b }.padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
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
            Icon(AppIcons.Add, contentDescription = "Tambah Biaya", tint = Color.White)
        }

        // ---------- ADD BIAYA MODAL ----------
        if (showAddForm || itemToEdit != null) {
            val isEdit = itemToEdit != null
            var selectedKategori by remember(itemToEdit) { mutableStateOf(itemToEdit?.kategori ?: "Bahan Baku") }
            var keterangan by remember(itemToEdit) { mutableStateOf(itemToEdit?.keterangan ?: "") }
            var jumlahStr by remember(itemToEdit) {
                mutableStateOf(itemToEdit?.jumlah?.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } ?: "")
            }
            var selectedDate by remember(itemToEdit) {
                mutableStateOf(itemToEdit?.tanggal ?: java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("in", "ID")).format(java.util.Date()))
            }

            var keteranganError by remember { mutableStateOf(false) }
            var jumlahError by remember { mutableStateOf(false) }

            var expandedDropdown by remember { mutableStateOf(false) }

            val calendar = java.util.Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("in", "ID")).format(calendar.time)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )

            AlertDialog(
                onDismissRequest = {
                    showAddForm = false
                    itemToEdit = null
                },
                title = { Text(if (isEdit) "Edit Biaya Operasional" else "Tambah Biaya Operasional") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tanggal Field
                        Text("Tanggal *", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(AppIcons.Calendar, contentDescription = null) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { datePickerDialog.show() }.background(Color.Transparent))
                        }

                        // Custom dropdown choice represent for kategori
                        Text("Kategori *", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedTextField(
                                value = selectedKategori,
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(AppIcons.ArrowDropDown, contentDescription = null) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { expandedDropdown = true }.background(Color.Transparent))
                            
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

                            if (isEdit) {
                                val index = biayaList.indexOfFirst { it.id == itemToEdit!!.id }
                                if (index != -1) {
                                    biayaList[index] = itemToEdit!!.copy(
                                        kategori = selectedKategori,
                                        keterangan = keterangan,
                                        jumlah = nominalVal,
                                        tanggal = selectedDate
                                    )
                                }
                                itemToEdit = null
                            } else {
                                biayaList.add(
                                    BiayaOperasional(
                                        id = (biayaList.size + 1).toString(),
                                        kategori = selectedKategori,
                                        keterangan = keterangan,
                                        jumlah = nominalVal,
                                        tanggal = selectedDate,
                                        pembuat = role
                                    )
                                )
                                showAddForm = false
                            }
                        }
                    ) {
                        Text("SIMPAN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddForm = false
                        itemToEdit = null
                    }) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                selectedItemForDetail = null
                                itemToEdit = item
                            }
                        ) {
                            Text("Edit")
                        }
                        TextButton(
                            onClick = {
                                selectedItemForDetail = null
                                itemToDelete = item
                            }
                        ) {
                            Text("Hapus", color = DangerColor)
                        }
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
    onNavigateToMonthlyReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedLabaDateFilter by remember { mutableStateOf("Bulan Ini") }
    val labaDateFilters = listOf("Hari Ini", "Minggu Ini", "Bulan Ini", "Bulan Lalu", "Semua")

    val todaySdf = remember { java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()) }
    val todayStr = remember { todaySdf.format(java.util.Date()) }

    fun getTrxDateStr(idTransaksi: String): String {
        if (!idTransaksi.startsWith("TRX-") || idTransaksi.length < 12) {
            return todayStr
        }
        return idTransaksi.substring(4, 12)
    }

    fun getCalendarForTrx(idTransaksi: String): java.util.Calendar? {
        val dateStr = getTrxDateStr(idTransaksi)
        return try {
            val sdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return null
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal
        } catch (e: Exception) {
            null
        }
    }

    fun getCalendarForBiaya(tanggalStr: String): java.util.Calendar? {
        return try {
            val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("in", "ID"))
            val date = sdf.parse(tanggalStr) ?: return null
            val cal = java.util.Calendar.getInstance()
            cal.time = date
            cal
        } catch (e: Exception) {
            null
        }
    }

    fun isTrxMatchingFilter(idTransaksi: String, filter: String): Boolean {
        val cal = getCalendarForTrx(idTransaksi) ?: return false
        val today = java.util.Calendar.getInstance()
        return when (filter) {
            "Hari Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH) &&
                cal.get(java.util.Calendar.DAY_OF_MONTH) == today.get(java.util.Calendar.DAY_OF_MONTH)
            }
            "Minggu Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.WEEK_OF_YEAR) == today.get(java.util.Calendar.WEEK_OF_YEAR)
            }
            "Bulan Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH)
            }
            "Bulan Lalu" -> {
                val lastMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
                cal.get(java.util.Calendar.YEAR) == lastMonth.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == lastMonth.get(java.util.Calendar.MONTH)
            }
            else -> true
        }
    }

    fun isBiayaMatchingFilter(tanggalStr: String, filter: String): Boolean {
        val cal = getCalendarForBiaya(tanggalStr) ?: return false
        val today = java.util.Calendar.getInstance()
        return when (filter) {
            "Hari Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH) &&
                cal.get(java.util.Calendar.DAY_OF_MONTH) == today.get(java.util.Calendar.DAY_OF_MONTH)
            }
            "Minggu Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.WEEK_OF_YEAR) == today.get(java.util.Calendar.WEEK_OF_YEAR)
            }
            "Bulan Ini" -> {
                cal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == today.get(java.util.Calendar.MONTH)
            }
            "Bulan Lalu" -> {
                val lastMonth = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
                cal.get(java.util.Calendar.YEAR) == lastMonth.get(java.util.Calendar.YEAR) &&
                cal.get(java.util.Calendar.MONTH) == lastMonth.get(java.util.Calendar.MONTH)
            }
            else -> true
        }
    }

    val filteredTransactions = remember(transaksiList.toList(), selectedLabaDateFilter) {
        if (selectedLabaDateFilter == "Semua") {
            transaksiList
        } else {
            transaksiList.filter { isTrxMatchingFilter(it.idTransaksi, selectedLabaDateFilter) }
        }
    }

    val filteredBiaya = remember(biayaList.toList(), selectedLabaDateFilter) {
        if (selectedLabaDateFilter == "Semua") {
            biayaList
        } else {
            biayaList.filter { isBiayaMatchingFilter(it.tanggal, selectedLabaDateFilter) }
        }
    }

    val totalPemasukan = filteredTransactions.sumOf { it.jumlah * it.harga }
    val totalBiaya = filteredBiaya.sumOf { it.jumlah }
    val labaBersih = totalPemasukan - totalBiaya

    fun formatReadableDate(dateStr: String): String {
        return try {
            val fromSdf = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault())
            val date = fromSdf.parse(dateStr) ?: return dateStr
            val toSdf = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("in", "ID"))
            toSdf.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    val rincianList = remember(filteredTransactions) {
        filteredTransactions.groupBy { getTrxDateStr(it.idTransaksi) }
            .map { (dateStr, items) ->
                val readableDate = formatReadableDate(dateStr)
                val uniqueTrxCount = items.distinctBy { it.idTransaksi }.size
                val totalRevenue = items.sumOf { it.jumlah * it.harga }.toLong()
                dateStr to RincianHarian(
                    tanggal = readableDate,
                    jumlahOrder = uniqueTrxCount,
                    totalPenjualan = totalRevenue
                )
            }
            .sortedByDescending { it.first }
            .map { it.second }
    }

    val menuTerlarisList = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.namaItem }
            .map { (namaItem, items) ->
                val totalQty = items.sumOf { it.jumlah }
                val totalRevenue = items.sumOf { it.jumlah * it.harga }
                namaItem to Pair(totalQty, totalRevenue)
            }
            .sortedByDescending { it.second.first }
            .take(5)
            .mapIndexed { index, pair ->
                MenuTerlaris(
                    namaBarang = pair.first,
                    totalQty = pair.second.first,
                    totalPendapatan = pair.second.second.toLong(),
                    ranking = index + 1
                )
            }
    }

    val groupedTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { it.idTransaksi }
    }

    val rincianPengeluaranList = remember(filteredBiaya) {
        filteredBiaya.groupBy { it.tanggal }
            .map { (dateStr, items) ->
                val jumlahTransaksi = items.size
                val totalPengeluaran = items.sumOf { it.jumlah }.toLong()
                Triple(dateStr, jumlahTransaksi, totalPengeluaran)
            }
            .sortedByDescending { getCalendarForBiaya(it.first)?.timeInMillis ?: 0L }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = "Laporan Keuangan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Analisis Performa Warung",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))

        // --- Fitur Baru: Laporan Bulanan per Item ---
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Laporan Bulanan per Item",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Lihat rincian penjualan harian untuk setiap barang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onNavigateToMonthlyReport,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(AppIcons.Calendar, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Buka Laporan Item")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ringkasan Laba-Rugi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { generateLabaRugiPdf(context, filteredTransactions, filteredBiaya, selectedLabaDateFilter) }) {
                Icon(imageVector = AppIcons.Pdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Date selection chips for Laba-Rugi
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
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

        // C. Laporan Laba-Rugi (Keeping this as summary)
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (labaBersih >= 0) SuccessColor.copy(alpha = 0.1f) else DangerColor.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Rekap Performa ($selectedLabaDateFilter)",
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
                    Text(text = formatRupiah(totalPemasukan.toLong()), style = MaterialTheme.typography.bodyMedium, color = SuccessColor)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Total Pengeluaran", style = MaterialTheme.typography.bodyMedium)
                    Text(text = "- ${formatRupiah(totalBiaya.toLong())}", style = MaterialTheme.typography.bodyMedium, color = DangerColor)
                }
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Laba Bersih:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = formatRupiah(labaBersih.toLong()),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (labaBersih >= 0) SuccessColor else DangerColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Rincian Harian",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mei 2026",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                if (rincianList.isEmpty()) {
                    Text(
                        text = "Belum ada rincian harian",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    rincianList.forEachIndexed { index, item ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.tanggal,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.jumlahOrder} kali order  •  ${formatRupiah(item.totalPenjualan)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- START: RINCIAN PENGELUARAN ---
        Text(
            text = "Rincian Pengeluaran",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (rincianPengeluaranList.isEmpty()) {
                    Text(
                        text = "Belum ada rincian pengeluaran",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    rincianPengeluaranList.forEachIndexed { index, item ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = item.first, // Menampilkan Tanggal
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.second} pengeluaran  •  - ${formatRupiah(item.third)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = DangerColor
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🧾 Daftar Transaksi Per Struk ($selectedLabaDateFilter)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (groupedTransactions.isEmpty()) {
                    Text(
                        text = "Belum ada transaksi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    val trxKeys = groupedTransactions.keys.toList()
                    trxKeys.forEachIndexed { index, trxId ->
                        val items = groupedTransactions[trxId] ?: emptyList()
                        val totalHarga = items.sumOf { it.jumlah * it.harga }
                        val isCanceled = items.any { it.namaItem.startsWith("❌") }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = trxId,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCanceled) DangerColor else Color.Unspecified
                                )
                                if (isCanceled) {
                                    Text(
                                        text = "Dibatalkan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerColor
                                    )
                                } else {
                                    Text(
                                        text = "Berhasil",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessColor
                                    )
                                }
                            }
                            Text(
                                text = formatRupiah(totalHarga.toLong()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isCanceled) DangerColor else SuccessColor
                            )
                        }

                        if (index < trxKeys.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🏆 Menu Terlaris ($selectedLabaDateFilter)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (menuTerlarisList.isEmpty()) {
                    Text(
                        text = "Belum ada data penjualan",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    menuTerlarisList.forEachIndexed { index, item ->
                        if (index > 0) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val rankColor = when (item.ranking) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "#${item.ranking} ",
                                        fontWeight = FontWeight.Bold,
                                        color = rankColor,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = item.namaBarang,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.totalQty} porsi terjual  •  ${formatRupiah(item.totalPendapatan)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------- 6. MANAJEMEN BARANG TAB -------------------------
@Composable
fun BarangTabContent(
    role: String,
    menuList: MutableList<MenuItem>,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showAddMenuForm by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<MenuItem?>(null) }
    var editNamaMenu by remember { mutableStateOf("") }
    var editHargaMenu by remember { mutableStateOf("") }
    var editError by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text(
                text = "Barang",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (menuList.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Belum ada data barang", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(menuList) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.nama, fontWeight = FontWeight.Bold)
                                    Text(formatRupiah(item.harga), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                if (role == UserRole.ADMIN_TOKO.displayName || role == UserRole.OWNER.displayName) {
                                    Row {
                                        IconButton(onClick = { 
                                            itemToEdit = item
                                            editNamaMenu = item.nama
                                            editHargaMenu = item.harga.toLong().toString()
                                            editError = false
                                        }) {
                                            Icon(AppIcons.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { 
                                            menuList.remove(item)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Barang berhasil dihapus")
                                            }
                                        }) {
                                            Icon(AppIcons.Delete, contentDescription = "Hapus", tint = DangerColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (role == UserRole.ADMIN_TOKO.displayName || role == UserRole.OWNER.displayName) {
            FloatingActionButton(
                onClick = { showAddMenuForm = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(AppIcons.Add, contentDescription = "Tambah Barang", tint = Color.White)
            }
        }

        if (showAddMenuForm) {
            var namaMenu by remember { mutableStateOf("") }
            var hargaMenu by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showAddMenuForm = false },
                title = { Text("Tambah Barang Baru") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = namaMenu,
                            onValueChange = { namaMenu = it },
                            label = { Text("Nama Barang") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = hargaMenu,
                            onValueChange = { hargaMenu = it },
                            label = { Text("Harga") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val h = hargaMenu.toDoubleOrNull() ?: 0.0
                        if (namaMenu.isNotBlank() && h > 0) {
                            menuList.add(MenuItem(java.util.UUID.randomUUID().toString(), namaMenu, h))
                            showAddMenuForm = false
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Barang berhasil ditambahkan")
                            }
                        }
                    }) {
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

        itemToEdit?.let { currentItem ->
            AlertDialog(
                onDismissRequest = { itemToEdit = null },
                title = { Text("Edit Barang") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = editNamaMenu,
                            onValueChange = { 
                                editNamaMenu = it
                                editError = false
                            },
                            label = { Text("Nama Barang") },
                            isError = editError && editNamaMenu.isBlank(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editHargaMenu,
                            onValueChange = { 
                                editHargaMenu = it
                                editError = false
                            },
                            label = { Text("Harga") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = editError && (editHargaMenu.toDoubleOrNull() ?: 0.0) <= 0.0,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (editError) {
                            Text(
                                text = "Nama tidak boleh kosong & harga harus valid", 
                                color = DangerColor, 
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val h = editHargaMenu.toDoubleOrNull() ?: 0.0
                        if (editNamaMenu.isNotBlank() && h > 0) {
                            val index = menuList.indexOfFirst { it.id == currentItem.id }
                            if (index != -1) {
                                menuList[index] = currentItem.copy(nama = editNamaMenu, harga = h)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Barang berhasil diupdate")
                                }
                            }
                            itemToEdit = null
                        } else {
                            editError = true
                        }
                    }) {
                        Text("Simpan")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToEdit = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}
