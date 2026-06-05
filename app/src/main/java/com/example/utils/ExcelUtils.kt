package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.ui.screens.MenuItem
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * Fungsi untuk men-generate daftar menu ke dalam file Excel (.xlsx)
 * Parameter sama persis dengan `generateMenuCetakPdf`
 */
fun generateMenuCetakExcel(context: Context, menuList: List<MenuItem>, categoryOrder: List<String>) {
    // 1. Inisialisasi Workbook (File Excel) dan Sheet
    val workbook = HSSFWorkbook()
    val sheet = workbook.createSheet("Daftar Menu")

    // 2. Buat Baris Header (Baris pertama / Index 0)
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("No")
    headerRow.createCell(1).setCellValue("Kategori")
    headerRow.createCell(2).setCellValue("Nama Item")
    headerRow.createCell(3).setCellValue("Harga")

    // 3. Persiapkan dan urutkan data sama seperti logika PDF
    val groupedMenu = menuList.groupBy { it.kategori }
    val keys = (categoryOrder.filter { it in groupedMenu } + groupedMenu.keys.filter { it !in categoryOrder }).distinct()

    var rowIndex = 1
    var itemNumber = 1

    // 4. Looping data menu untuk diisi ke dalam baris-baris Excel
    for (kategori in keys) {
        val items = groupedMenu[kategori] ?: continue
        for (item in items) {
            val row = sheet.createRow(rowIndex++)
            
            // Kolom 0: Nomor Urut
            row.createCell(0).setCellValue(itemNumber.toDouble())
            // Kolom 1: Kategori
            row.createCell(1).setCellValue(kategori)
            // Kolom 2: Nama Menu
            row.createCell(2).setCellValue(item.nama)
            // Kolom 3: Harga
            row.createCell(3).setCellValue(item.harga)
            
            itemNumber++
        }
    }

    // 5. Auto-size kolom agar lebar kolom menyesuaikan teks
    for (i in 0..3) {
        sheet.autoSizeColumn(i)
    }

    // 6. Buat nama file dan simpan ke Storage (Folder Documents)
    val fileName = "Menu_Pemesanan_${System.currentTimeMillis()}.xls"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    
    val fileOut = FileOutputStream(file)
    workbook.write(fileOut)
    fileOut.close()
    workbook.close()

    // 7. Buka file secara otomatis setelah berhasil dibuat
    openExcelFile(context, file)
}

/**
 * Fungsi untuk membuka file .xlsx menggunakan aplikasi eksternal (Viewer Excel)
 */
fun openExcelFile(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW)
    
    // Gunakan MIME Type standar untuk format .xls
    intent.setDataAndType(uri, "application/vnd.ms-excel")
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
    
    context.startActivity(intent)
}
