package com.example.utils

import java.text.NumberFormat
import java.util.*

fun formatRupiah(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
    return format.format(amount).replace("Rp", "Rp ").replace(",00", "")
}

fun formatRupiah(amount: Long): String {
    return formatRupiah(amount.toDouble())
}
