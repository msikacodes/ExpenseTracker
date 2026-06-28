package com.example.myapplication.ui.addexpense

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.settings.AppStrings
import com.example.myapplication.ui.settings.EnglishStrings
import com.example.myapplication.ui.settings.LocalAppStrings
import com.example.myapplication.ui.theme.ExpenseTrackerTheme
import com.example.myapplication.util.PhotoAttachment
import com.example.myapplication.util.copyUriToInternalStorage
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
    val currentAmount = if (isEditing) existingExpense!!.amount.toString() else ""
    val currentDesc = if (isEditing) existingExpense!!.description else ""
    val currentCategory = if (isEditing) existingExpense!!.category else Category.FOOD
    val currentPhoto = if (isEditing) existingExpense!!.photoUri else null
    val strings = LocalAppStrings.current

    var amount by remember { mutableStateOf(currentAmount) }
    var description by remember { mutableStateOf(currentDesc) }
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var photoUri by remember { mutableStateOf(currentPhoto) }

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
                            photoUri = persistentPhotoUri
                        ) else Expense(
                            amount = amountDouble,
                            category = selectedCategory,
                            description = description,
                            photoUri = persistentPhotoUri
                        )
                    )
                }
            },
            isEditing = isEditing,
            strings = strings
        )
    }
}

@Composable
fun AddExpenseContent(
    amount: String, onAmountChange: (String) -> Unit,
    description: String, onDescriptionChange: (String) -> Unit,
    selectedCategory: Category, onCategoryChange: (Category) -> Unit,
    photoUri: String? = null, onPhotoChange: (String?) -> Unit = {},
    onConfirm: () -> Unit,
    isEditing: Boolean = false,
    strings: AppStrings
) {
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
