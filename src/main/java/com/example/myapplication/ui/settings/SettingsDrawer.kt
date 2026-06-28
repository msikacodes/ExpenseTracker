package com.example.myapplication.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDrawer(
    themeMode: String,
    language: String,
    monthlyBudget: Double,
    biometricLock: Boolean,
    onThemeModeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBudgetChange: (Double) -> Unit,
    onBiometricLockChange: (Boolean) -> Unit,
    onExport: () -> Unit,
    strings: AppStrings
) {
    var showBudgetInput by remember { mutableStateOf(false) }
    var budgetText by remember { mutableStateOf(if (monthlyBudget > 0) monthlyBudget.toLong().toString() else "") }

    ModalDrawerSheet(
        modifier = Modifier.width(300.dp),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Rounded.AccountBalance, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(strings.appName, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Text("v1.0", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // -- Appearance --
            Text(strings.appearance, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))

            SectionIconText(Icons.Rounded.DarkMode, strings.theme)
            RadioGroup(
                options = listOf("system" to strings.systemDefault, "light" to strings.light, "dark" to strings.dark),
                selected = themeMode,
                onSelect = onThemeModeChange
            )

            SectionIconText(Icons.Rounded.Language, strings.language)
            RadioGroup(
                options = listOf("en" to strings.english, "sw" to strings.swahili),
                selected = language,
                onSelect = onLanguageChange
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // -- Preferences --
            Text(strings.preferences, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))

            // Currency
            SectionIconText(Icons.Rounded.MonetizationOn, strings.currency)
            Text("TZS - Tanzanian Shilling",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 52.dp, bottom = 12.dp))

            // Budget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Savings, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(strings.budget, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f))
                TextButton(onClick = { showBudgetInput = !showBudgetInput }) {
                    Text(if (showBudgetInput) strings.close else if (monthlyBudget > 0) formatBudget(monthlyBudget) else strings.setBudget)
                }
            }
            if (showBudgetInput) {
                Row(
                    modifier = Modifier.padding(start = 52.dp, end = 24.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        prefix = { Text("TSh ") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            val amt = budgetText.toDoubleOrNull() ?: 0.0
                            onBudgetChange(amt)
                            showBudgetInput = false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Set") }
                }
            }

            // Export
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.FileDownload, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onExport) {
                    Text(strings.exportCSV)
                }
            }

            // Biometric lock
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(start = 20.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(strings.biometricLock, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f))
                Switch(
                    checked = biometricLock,
                    onCheckedChange = onBiometricLockChange
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // About
            Text(strings.about, style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp))

            SectionIconText(Icons.Rounded.Info, strings.version)
            Text("1.0.0", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = 52.dp, bottom = 24.dp))

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun SectionIconText(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 24.dp, bottom = 4.dp)) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
    }
}

@Composable
fun RadioGroup(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.padding(start = 44.dp, top = 4.dp, bottom = 4.dp)) {
        options.forEach { (value, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                RadioButton(selected = selected == value, onClick = { onSelect(value) })
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

private fun formatBudget(amount: Double): String {
    if (amount >= 1_000_000) return "TSh ${(amount / 1_000_000).toLong()}M"
    if (amount >= 1_000) return "TSh ${(amount / 1_000).toLong()}K"
    return "TSh ${amount.toLong()}"
}
