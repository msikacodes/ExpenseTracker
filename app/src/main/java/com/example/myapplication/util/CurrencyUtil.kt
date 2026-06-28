package com.example.myapplication.util

import java.text.NumberFormat
import java.util.Locale

fun formatTZS(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale.ENGLISH)
    formatter.minimumFractionDigits = 0
    formatter.maximumFractionDigits = 0
    return "TSh ${formatter.format(amount)}"
}
