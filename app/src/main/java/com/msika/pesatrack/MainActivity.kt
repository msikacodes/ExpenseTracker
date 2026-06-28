package com.msika.pesatrack

import android.app.KeyguardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.msika.pesatrack.data.Expense
import com.msika.pesatrack.navigation.Destination
import com.msika.pesatrack.ui.addexpense.AddExpenseBottomSheet
import com.msika.pesatrack.ui.dashboard.DashboardScreen
import com.msika.pesatrack.ui.settings.EnglishStrings
import com.msika.pesatrack.ui.settings.LocalAppStrings
import com.msika.pesatrack.ui.settings.SettingsDrawer
import com.msika.pesatrack.ui.settings.SwahiliStrings
import com.msika.pesatrack.ui.theme.ExpenseTrackerTheme
import com.msika.pesatrack.util.PreferencesManager
import com.msika.pesatrack.util.exportExpensesToCsv
import com.msika.pesatrack.util.shareCsvFile
import com.msika.pesatrack.viewmodel.ExpenseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: ExpenseViewModel = viewModel()
            val prefsManager = remember { PreferencesManager(applicationContext) }
            val scope = rememberCoroutineScope()

            val themeMode by prefsManager.themeMode.collectAsState(initial = "system")
            val language by prefsManager.language.collectAsState(initial = "en")
            val monthlyBudget by prefsManager.monthlyBudget.collectAsState(initial = 0.0)
            val biometricLock by prefsManager.biometricLock.collectAsState(initial = false)
            val strings = if (language == "sw") SwahiliStrings else EnglishStrings

            var isUnlocked by remember { mutableStateOf(!biometricLock) }
            LaunchedEffect(biometricLock) { if (!biometricLock) isUnlocked = true }

            var showBottomSheet by remember { mutableStateOf(false) }
            var editingExpense by remember { mutableStateOf<Expense?>(null) }
            val backStack = remember { mutableStateListOf<Any>(Destination.Dashboard) }
            val drawerState = rememberDrawerState(DrawerValue.Closed)

            val onExport: () -> Unit = {
                scope.launch {
                    val expenses = viewModel.allExpenses.value
                    if (expenses.isEmpty()) return@launch
                    withContext(Dispatchers.IO) {
                        val file = exportExpensesToCsv(this@MainActivity, expenses, strings)
                        shareCsvFile(this@MainActivity, file, strings)
                    }
                }
            }

            CompositionLocalProvider(LocalAppStrings provides strings) {
                ExpenseTrackerTheme(themeMode = themeMode) {
                    if (biometricLock && !isUnlocked) {
                        BiometricLockScreen(
                            strings = strings,
                            onAuthenticated = { isUnlocked = true }
                        )
                    } else {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            gesturesEnabled = drawerState.isOpen,
                            drawerContent = {
                                SettingsDrawer(
                                    themeMode = themeMode,
                                    language = language,
                                    monthlyBudget = monthlyBudget,
                                    biometricLock = biometricLock,
                                    onThemeModeChange = { scope.launch { prefsManager.setThemeMode(it) } },
                                    onLanguageChange = { scope.launch { prefsManager.setLanguage(it) } },
                                    onBudgetChange = { scope.launch { prefsManager.setMonthlyBudget(it) } },
                                    onBiometricLockChange = { enabled ->
                                        scope.launch { prefsManager.setBiometricLock(enabled) }
                                        if (!enabled) isUnlocked = true
                                    },
                                    onExport = onExport,
                                    strings = strings
                                )
                            }
                        ) {
                            AppContent(
                                viewModel = viewModel,
                                showBottomSheet = showBottomSheet,
                                editingExpense = editingExpense,
                                onShowBottomSheet = { showBottomSheet = it },
                                onSetEditingExpense = { editingExpense = it },
                                backStack = backStack,
                                drawerState = drawerState,
                                monthlyBudget = monthlyBudget,
                                onExportClick = onExport
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppContent(
    viewModel: ExpenseViewModel,
    showBottomSheet: Boolean,
    editingExpense: Expense?,
    onShowBottomSheet: (Boolean) -> Unit,
    onSetEditingExpense: (Expense?) -> Unit,
    backStack: androidx.compose.runtime.snapshots.SnapshotStateList<Any>,
    drawerState: DrawerState,
    monthlyBudget: Double,
    onExportClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is Destination.Dashboard -> NavEntry(key) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onAddExpenseClick = { onShowBottomSheet(true) },
                        onMenuClick = { scope.launch { drawerState.open() } },
                        onEditExpense = { expense ->
                            onSetEditingExpense(expense)
                            onShowBottomSheet(true)
                        },
                        monthlyBudget = monthlyBudget,
                        onExportClick = onExportClick
                    )
                }
                else -> NavEntry(Unit) { }
            }
        }
    )

    if (showBottomSheet) {
        AddExpenseBottomSheet(
            existingExpense = editingExpense,
            onDismiss = {
                onShowBottomSheet(false)
                onSetEditingExpense(null)
            },
            onConfirm = { expense ->
                if (editingExpense != null) {
                    viewModel.updateExpense(expense)
                } else {
                    viewModel.addExpense(expense)
                }
                onShowBottomSheet(false)
                onSetEditingExpense(null)
            }
        )
    }
}

@Composable
fun BiometricLockScreen(
    strings: com.msika.pesatrack.ui.settings.AppStrings,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val authLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            onAuthenticated()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(strings.unlockApp, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(strings.authenticateToContinue, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    val km = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    if (km?.isDeviceSecure == true) {
                        val intent = km.createConfirmDeviceCredentialIntent(
                            strings.unlockApp,
                            strings.authenticateToContinue
                        )
                        if (intent != null) {
                            authLauncher.launch(intent)
                            return@Button
                        }
                    }
                    // Fallback: device not secured
                    onAuthenticated()
                }
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.authenticateToContinue)
            }
        }
    }
}
