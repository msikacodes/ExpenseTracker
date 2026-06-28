package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.myapplication.data.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

fun exportExpensesToCsv(context: Context, expenses: List<Expense>, fileName: String = "expenses.csv"): File {
    val file = File(context.cacheDir, fileName)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    file.bufferedWriter().use { writer ->
        writer.appendLine("Date,Description,Category,Amount (TZS)")
        expenses.sortedByDescending { it.date }.forEach { expense ->
            writer.appendLine("${formatter.format(Date(expense.date))},${expense.description},${expense.category.displayName},${expense.amount.toLong()}")
        }
    }
    return file
}

fun shareCsvFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Expenses"))
}
