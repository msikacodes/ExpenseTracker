package com.example.myapplication.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.settings.AppStrings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Exports the expenses list to a CSV file in a localized format, escaping CSV-specific characters.
 */
fun exportExpensesToCsv(
    context: Context,
    expenses: List<Expense>,
    strings: AppStrings,
    fileName: String = "expenses.csv"
): File {
    val file = File(context.cacheDir, fileName)
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    file.bufferedWriter().use { writer ->
        writer.appendLine("${strings.csvHeaderDate},${strings.csvHeaderDescription},${strings.csvHeaderCategory},${strings.csvHeaderAmount}")
        expenses.sortedByDescending { it.date }.forEach { expense ->
            val localizedCategory = when (expense.category) {
                Category.FOOD -> strings.categoryFood
                Category.TRANSPORT -> strings.categoryTransport
                Category.SHOPPING -> strings.categoryShopping
                Category.UTILITIES -> strings.categoryUtilities
                Category.OTHER -> strings.categoryOther
            }
            val formattedDate = formatter.format(Date(expense.date))
            val escapedDesc = escapeCsvField(expense.description)
            val escapedCat = escapeCsvField(localizedCategory)
            val amountStr = expense.amount.toLong().toString()
            writer.appendLine("$formattedDate,$escapedDesc,$escapedCat,$amountStr")
        }
    }
    return file
}

private fun escapeCsvField(field: String): String {
    val escaped = field.replace("\"", "\"\"")
    return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
        "\"$escaped\""
    } else {
        escaped
    }
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
