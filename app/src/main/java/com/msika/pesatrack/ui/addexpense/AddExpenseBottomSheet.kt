package com.msika.pesatrack.ui.addexpense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.msika.pesatrack.data.Category
import com.msika.pesatrack.data.Expense
import com.msika.pesatrack.ui.settings.AppStrings
import com.msika.pesatrack.ui.settings.EnglishStrings
import com.msika.pesatrack.ui.settings.LocalAppStrings
import com.msika.pesatrack.ui.theme.ExpenseTrackerTheme
import com.msika.pesatrack.util.PhotoAttachment
import com.msika.pesatrack.util.copyUriToInternalStorage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    existingExpense: Expense? = null,
    onDismiss: () -> Unit,
    onConfirm: (Expense) -> Unit
) {
    val isEditing = existingExpense != null
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    var amount by remember { mutableStateOf(if (isEditing) existingExpense!!.amount.toString() else "") }
    var description by remember { mutableStateOf(if (isEditing) existingExpense!!.description else "") }
    var selectedCategory by remember { mutableStateOf(if (isEditing) existingExpense!!.category else Category.FOOD) }
    var photoUri by remember { mutableStateOf(if (isEditing) existingExpense!!.photoUri else null) }
    var selectedDateMillis by remember { mutableStateOf(if (isEditing) existingExpense!!.date else System.currentTimeMillis()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AddExpenseContent(
            amount = amount,
            onAmountChange = { amount = it },
            description = description,
            onDescriptionChange = { description = it },
            selectedCategory = selectedCategory,
            onCategoryChange = { selectedCategory = it },
            photoUri = photoUri,
            onPhotoChange = { photoUri = it },
            selectedDateMillis = selectedDateMillis,
            onDateChange = { selectedDateMillis = it },
            onConfirm = {
                val amountDouble = amount.toDoubleOrNull() ?: 0.0
                if (amountDouble > 0 && description.isNotBlank()) {
                    val persistentPhotoUri = photoUri?.let { uriStr ->
                        copyUriToInternalStorage(context, uriStr) ?: uriStr
                    }
                    onConfirm(
                        if (isEditing) existingExpense!!.copy(
                            amount = amountDouble,
                            category = selectedCategory,
                            description = description,
                            photoUri = persistentPhotoUri,
                            date = selectedDateMillis
                        ) else Expense(
                            amount = amountDouble,
                            category = selectedCategory,
                            description = description,
                            photoUri = persistentPhotoUri,
                            date = selectedDateMillis
                        )
                    )
                }
            },
            isEditing = isEditing,
            strings = strings
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseContent(
    amount: String, onAmountChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    selectedCategory: Category, onCategoryChange: (Category) -> Unit,
    photoUri: String? = null, onPhotoChange: (String?) -> Unit = {},
    selectedDateMillis: Long = System.currentTimeMillis(),
    onDateChange: (Long) -> Unit = {},
    onConfirm: () -> Unit,
    isEditing: Boolean = false,
    strings: AppStrings
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange(it) }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(strings.close) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = if (isEditing) strings.editExpense else strings.addNewExpense,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = amount, onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onAmountChange(it) },
            label = { Text(strings.amount) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            prefix = { Text("TSh ") },
            shape = RoundedCornerShape(16.dp), singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = description, onValueChange = onDescriptionChange,
            label = { Text(strings.description) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp), singleLine = true
        )
        Spacer(Modifier.height(16.dp))

        // Date picker button
        OutlinedButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${strings.selectDate}: ${dateFormatter.format(Date(selectedDateMillis))}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(Modifier.height(16.dp))

        Text(strings.receipt, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(8.dp))
        PhotoAttachment(
            photoUri = photoUri?.let { android.net.Uri.parse(it) },
            onPhotoSelected = { uri -> onPhotoChange(uri?.toString()) }
        )
        Spacer(Modifier.height(20.dp))

        Text(strings.selectCategory,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(Category.entries) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat.getLocalizedName()) },
                    leadingIcon = { Icon(cat.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cat.color,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = amount.isNotBlank() && description.isNotBlank(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(if (isEditing) strings.updateExpense else strings.confirmExpense,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddExpensePreview() {
    ExpenseTrackerTheme {
        Surface {
            AddExpenseContent(
                amount = "45000", onAmountChange = {},
                description = "Lunch", onDescriptionChange = {},
                selectedCategory = Category.FOOD, onCategoryChange = {},
                onConfirm = {}, isEditing = false, strings = EnglishStrings
            )
        }
    }
}
