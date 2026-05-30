package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.TransactionModel
import com.example.ui.screens.BiayaOperasional
import com.example.ui.screens.TransaksiHarian
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.*
import com.itextpdf.layout.properties.BorderRadius
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.*

fun generateQuotationPdf(context: Context, data: TransactionModel, shopName: String = "TOKO KAMI") {
    // 1. Setup File Output
    val logoManager = LogoManager(context)
    val logoPath = logoManager.getLogoPath()

    val fileName = "Penawaran_${data.invoiceCode}.pdf"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    val writer = PdfWriter(FileOutputStream(file))
    val pdf = PdfDocument(writer)
    val document = Document(pdf, PageSize.A4)
    
    // Helper format Rupiah
    val formatRupiah = { amount: Double ->
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.format(amount).replace("Rp", "Rp. ").replace(",00", "")
    }

    // --- HEADER ---
    val headerTable = Table(UnitValue.createPercentArray(floatArrayOf(50f, 50f))).useAllAvailableWidth()
    
    // Kiri: Logo (Circular) & Kepada Yth
    val leftHeader = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
    if (logoPath != null) {
        val imageData = ImageDataFactory.create(logoPath)
        val logoImage = Image(imageData)
        logoImage.setWidth(60f)
        logoImage.setHeight(60f)
        logoImage.setBorderRadius(BorderRadius(100f))
        leftHeader.add(logoImage)
    } else {
        leftHeader.add(Paragraph(shopName).setBold().setFontSize(14f))
    }

    leftHeader.add(Paragraph("\nKepada Yth:"))
    leftHeader.add(Paragraph(data.customerName).setBold())
    leftHeader.add(Paragraph(data.customerAddress).setFontSize(10f))
    headerTable.addCell(leftHeader)

    // Kanan: Judul & Info Table
    val rightHeader = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
    rightHeader.add(Paragraph("SURAT PENAWARAN")
        .setBold()
        .setFontSize(18f)
        .setTextAlignment(TextAlignment.RIGHT))
    
    val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(33f, 33f, 34f))).useAllAvailableWidth()
    infoTable.addCell(Cell().add(Paragraph("Kode").setBold().setFontSize(10f)))
    infoTable.addCell(Cell().add(Paragraph("Tanggal").setBold().setFontSize(10f)))
    infoTable.addCell(Cell().add(Paragraph("Sales").setBold().setFontSize(10f)))
    
    infoTable.addCell(Cell().add(Paragraph(data.invoiceCode).setFontSize(10f)))
    infoTable.addCell(Cell().add(Paragraph(data.date).setFontSize(10f)))
    infoTable.addCell(Cell().add(Paragraph(data.salesName).setFontSize(10f)))
    
    rightHeader.add(infoTable)
    headerTable.addCell(rightHeader)
    
    document.add(headerTable)
    document.add(Paragraph("\n"))

    // --- TABEL BARANG ---
    val table = Table(UnitValue.createPercentArray(floatArrayOf(5f, 35f, 10f, 15f, 15f, 20f))).useAllAvailableWidth()
    
    // Header Tabel
    val headers = arrayOf("No", "Nama Barang", "Qty", "@Harga", "@Diskon", "Jumlah")
    headers.forEach { h ->
        table.addHeaderCell(Cell().add(Paragraph(h).setBold().setTextAlignment(TextAlignment.CENTER)))
    }

    // Isi Tabel
    data.items.forEachIndexed { index, item ->
        table.addCell(Cell().add(Paragraph((index + 1).toString()).setTextAlignment(TextAlignment.CENTER)))
        table.addCell(Cell().add(Paragraph(item.name)))
        table.addCell(Cell().add(Paragraph("${item.qty} ${item.unit}").setTextAlignment(TextAlignment.CENTER)))
        table.addCell(Cell().add(Paragraph(formatRupiah(item.price)).setTextAlignment(TextAlignment.RIGHT)))
        table.addCell(Cell().add(Paragraph("${item.discountPercent}%").setTextAlignment(TextAlignment.CENTER)))
        table.addCell(Cell().add(Paragraph(formatRupiah(item.subTotal)).setTextAlignment(TextAlignment.RIGHT)))
    }

    document.add(table)

    // --- FOOTER TABEL (Total & Grand Total) ---
    val footerTable = Table(UnitValue.createPercentArray(floatArrayOf(65f, 15f, 20f))).useAllAvailableWidth()
    
    // Row Total
    footerTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
    footerTable.addCell(Cell().add(Paragraph("Total").setBold()))
    footerTable.addCell(Cell().add(Paragraph(formatRupiah(data.total)).setTextAlignment(TextAlignment.RIGHT)))
    
    // Row PPN 11%
    footerTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
    footerTable.addCell(Cell().add(Paragraph("PPN 11%").setBold()))
    footerTable.addCell(Cell().add(Paragraph(formatRupiah(data.ppn)).setTextAlignment(TextAlignment.RIGHT)))

    // Row Grand Total
    footerTable.addCell(Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
    footerTable.addCell(Cell().add(Paragraph("Grand Total").setBold().setFontSize(14f)))
    footerTable.addCell(Cell().add(Paragraph(formatRupiah(data.grandTotal)).setBold().setFontSize(14f).setTextAlignment(TextAlignment.RIGHT)))

    document.add(footerTable)
    document.add(Paragraph("\n"))

    // --- KETERANGAN & TANDA TANGAN ---
    val signTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
    
    // Kiri: Catatan
    val noteCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER)
    noteCell.add(Paragraph("Keterangan:").setBold())
    noteCell.add(Paragraph(data.notes.ifEmpty { "-" }).setFontSize(10f))
    signTable.addCell(noteCell)
    
    // Kanan: Tanda Tangan
    val signCell = Cell().setBorder(com.itextpdf.layout.borders.Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER)
    signCell.add(Paragraph("Hormat Kami,"))
    signCell.add(Paragraph("\n\n\n"))
    signCell.add(Paragraph(data.salesName).setUnderline().setBold())
    signCell.add(Paragraph("Sales").setFontSize(10f))
    signTable.addCell(signCell)

    document.add(signTable)
    
    // Tutup Dokumen
    document.close()
    
    // --- OPEN PDF ---
    openPdfFile(context, file)
}

fun openPdfFile(context: Context, file: File) {
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.setDataAndType(uri, "application/pdf")
    intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_CLEAR_TOP
    context.startActivity(intent)
}

fun generateLaporanBiayaPdf(context: Context, biayaList: List<BiayaOperasional>) {
    val fileName = "Laporan_Biaya_${System.currentTimeMillis()}.pdf"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    val writer = PdfWriter(FileOutputStream(file))
    val pdf = PdfDocument(writer)
    val document = Document(pdf, PageSize.A4)
    
    val formatRupiah = { amount: Double ->
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.format(amount).replace("Rp", "Rp. ").replace(",00", "")
    }

    // Title
    document.add(Paragraph("LAPORAN BIAYA OPERASIONAL").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("Tanggal Cetak: " + java.text.SimpleDateFormat("d MMMM yyyy HH:mm", Locale("in", "ID")).format(Date())).setFontSize(10f))
    document.add(Paragraph("\n"))

    // Table
    val table = Table(UnitValue.createPercentArray(floatArrayOf(10f, 25f, 35f, 15f, 15f))).useAllAvailableWidth()
    val headers = arrayOf("No", "Tanggal", "Keterangan", "Kategori", "Jumlah")
    headers.forEach { h ->
        table.addHeaderCell(Cell().add(Paragraph(h).setBold().setTextAlignment(TextAlignment.CENTER)))
    }

    biayaList.forEachIndexed { index, item ->
        table.addCell(Cell().add(Paragraph((index + 1).toString()).setTextAlignment(TextAlignment.CENTER)))
        table.addCell(Cell().add(Paragraph(item.tanggal)))
        table.addCell(Cell().add(Paragraph(item.keterangan)))
        table.addCell(Cell().add(Paragraph(item.kategori)))
        table.addCell(Cell().add(Paragraph(formatRupiah(item.jumlah)).setTextAlignment(TextAlignment.RIGHT)))
    }
    
    document.add(table)
    document.add(Paragraph("\n"))

    // Total Summary
    val totalAmount = biayaList.sumOf { it.jumlah }
    val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f))).useAllAvailableWidth()
    summaryTable.addCell(Cell().add(Paragraph("Total Pengeluaran").setBold()).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
    summaryTable.addCell(Cell().add(Paragraph(formatRupiah(totalAmount)).setBold().setTextAlignment(TextAlignment.RIGHT)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER))
    
    document.add(summaryTable)

    document.close()
    openPdfFile(context, file)
}

fun generateLabaRugiPdf(context: Context, transaksiList: List<TransaksiHarian>, biayaList: List<BiayaOperasional>, filterName: String) {
    val fileName = "Laporan_Laba_Rugi_${System.currentTimeMillis()}.pdf"
    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
    val writer = PdfWriter(FileOutputStream(file))
    val pdf = PdfDocument(writer)
    val document = Document(pdf, PageSize.A4)

    val formatRupiah = { amount: Double ->
        val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        format.format(amount).replace("Rp", "Rp. ").replace(",00", "")
    }

    // Title
    document.add(Paragraph("LAPORAN RINGKASAN LABA-RUGI").setBold().setFontSize(18f).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("Periode/Filter: $filterName").setFontSize(11f).setTextAlignment(TextAlignment.CENTER))
    document.add(Paragraph("Tanggal Cetak: " + java.text.SimpleDateFormat("d MMMM yyyy HH:mm", Locale("in", "ID")).format(Date())).setFontSize(10f))
    document.add(Paragraph("\n"))

    // Financial Agregates Table
    val totalPemasukan = transaksiList.sumOf { it.jumlah * it.harga }
    val totalPengeluaran = biayaList.sumOf { it.jumlah }
    val labaBersih = totalPemasukan - totalPengeluaran

    val summaryTable = Table(UnitValue.createPercentArray(floatArrayOf(60f, 40f))).useAllAvailableWidth()
    summaryTable.addCell(Cell().add(Paragraph("TOTAL PENDAPATAN (PENJUALAN)").setBold()))
    summaryTable.addCell(Cell().add(Paragraph(formatRupiah(totalPemasukan)).setBold().setTextAlignment(TextAlignment.RIGHT)))

    summaryTable.addCell(Cell().add(Paragraph("TOTAL PENGELUARAN (BIAYA OPERASIONAL)").setBold()))
    summaryTable.addCell(Cell().add(Paragraph(formatRupiah(totalPengeluaran)).setBold().setTextAlignment(TextAlignment.RIGHT)))

    val profitCell = Cell().add(Paragraph("LABA BERSIH").setBold().setFontSize(14f))
    val profitValCell = Cell().add(Paragraph(formatRupiah(labaBersih)).setBold().setFontSize(14f).setTextAlignment(TextAlignment.RIGHT))
    summaryTable.addCell(profitCell)
    summaryTable.addCell(profitValCell)

    document.add(summaryTable)
    document.add(Paragraph("\n"))

    // Breakdown Section - Pemasukan
    document.add(Paragraph("Rincian Transaksi Penjualan").setBold().setFontSize(12f))
    val trxTable = Table(UnitValue.createPercentArray(floatArrayOf(15f, 25f, 30f, 10f, 20f))).useAllAvailableWidth()
    val trxHeaders = arrayOf("No TRX", "Waktu", "Item", "Qty", "Jumlah")
    trxHeaders.forEach { h ->
        trxTable.addHeaderCell(Cell().add(Paragraph(h).setBold().setTextAlignment(TextAlignment.CENTER)))
    }
    transaksiList.forEach { item ->
        trxTable.addCell(Cell().add(Paragraph(item.idTransaksi).setFontSize(9f)))
        trxTable.addCell(Cell().add(Paragraph(item.waktu).setFontSize(9f)))
        trxTable.addCell(Cell().add(Paragraph(item.namaItem).setFontSize(9f)))
        trxTable.addCell(Cell().add(Paragraph(item.jumlah.toString()).setTextAlignment(TextAlignment.CENTER).setFontSize(9f)))
        trxTable.addCell(Cell().add(Paragraph(formatRupiah(item.jumlah * item.harga)).setTextAlignment(TextAlignment.RIGHT).setFontSize(9f)))
    }
    document.add(trxTable)
    
    document.add(Paragraph("\n"))

    // Breakdown Section - Pengeluaran
    document.add(Paragraph("Rincian Pengeluaran / Biaya").setBold().setFontSize(12f))
    val costTable = Table(UnitValue.createPercentArray(floatArrayOf(20f, 25f, 35f, 20f))).useAllAvailableWidth()
    val costHeaders = arrayOf("Tanggal", "Kategori", "Keterangan", "Nominal")
    costHeaders.forEach { h ->
        costTable.addHeaderCell(Cell().add(Paragraph(h).setBold().setTextAlignment(TextAlignment.CENTER)))
    }
    biayaList.forEach { item ->
        costTable.addCell(Cell().add(Paragraph(item.tanggal).setFontSize(9f)))
        costTable.addCell(Cell().add(Paragraph(item.kategori).setFontSize(9f)))
        costTable.addCell(Cell().add(Paragraph(item.keterangan).setFontSize(9f)))
        costTable.addCell(Cell().add(Paragraph(formatRupiah(item.jumlah)).setTextAlignment(TextAlignment.RIGHT).setFontSize(9f)))
    }
    document.add(costTable)

    document.close()
    openPdfFile(context, file)
}
