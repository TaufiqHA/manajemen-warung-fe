package com.example.data

data class InvoiceItem(
    val name: String,
    val qty: Int,
    val unit: String = "bh",
    val price: Double,
    val discountPercent: Double = 0.0
) {
    val subTotal: Double
        get() = qty * price * (1 - (discountPercent / 100))
}

data class TransactionModel(
    val customerName: String,
    val customerAddress: String,
    val items: List<InvoiceItem>,
    val salesName: String,
    val invoiceCode: String,
    val date: String,
    val notes: String = ""
) {
    val total: Double
        get() = items.sumOf { it.subTotal }
    
    val ppn: Double
        get() = total * 0.11
    
    val grandTotal: Double
        get() = total + ppn
}
