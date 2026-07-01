package com.example

import com.example.ui.screens.TransaksiHarian
import com.example.ui.screens.MenuItem
import com.example.data.MenuTerlaris
import org.junit.Assert.assertEquals
import org.junit.Test

class TopSellingLogicTest {

    @Test
    fun testGroupingLogicFixed() {
        val transactions = listOf(
            TransaksiHarian("TRX-1", "PRD-1", "Ciki", 10, 1000.0, "20260531", "User"),
            TransaksiHarian("TRX-2", "PRD-1", "[BATAL] Ciki", 5, 1000.0, "20260531", "User"),
            TransaksiHarian("TRX-3", "PRD-1", "Ciki", 5, 1000.0, "20260531", "User"),
            TransaksiHarian("TRX-4", "PRD-2", "Kopi", 2, 5000.0, "20260531", "User")
        )
        
        val menuList = listOf(
            MenuItem("PRD-1", "Ciki Original", 1000.0),
            MenuItem("PRD-2", "Kopi Original", 5000.0)
        )

        // Fixed Logic (reproduced from DashboardScreen.kt)
        val menuTerlarisList = transactions
            .filter { !it.namaItem.contains("[BATAL]", ignoreCase = true) }
            .groupBy { it.id } // Grouping by Product ID
            .map { (productId, items) ->
                val totalQty = items.sumOf { it.jumlah }
                val totalRevenue = items.sumOf { it.jumlah * it.harga }
                val originalName = menuList.find { it.id == productId }?.nama ?: items.first().namaItem
                originalName to Pair(totalQty, totalRevenue)
            }
            .filter { it.second.first >= 1 }
            .sortedByDescending { it.second.first }
            .mapIndexed { index, pair ->
                MenuTerlaris(
                    namaBarang = pair.first,
                    totalQty = pair.second.first,
                    totalPendapatan = pair.second.second.toLong(),
                    ranking = index + 1
                )
            }

        println("Result: $menuTerlarisList")
        
        // We expect only ONE "Ciki" entry (Ciki Original) with totalQty 15
        val cikiEntries = menuTerlarisList.filter { it.namaBarang.contains("Ciki") }
        assertEquals(1, cikiEntries.size)
        assertEquals(15, cikiEntries[0].totalQty)
        assertEquals("Ciki Original", cikiEntries[0].namaBarang)
    }
}
