package com.msika.pesatrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.msika.pesatrack.data.Expense
import com.msika.pesatrack.navigation.Destination
import com.msika.pesatrack.ui.addexpense.AddExpenseBottomSheet
import com.msika.pesatrack.ui.dashboard.DashboardScreen
import com.msika.pesatrack.ui.settings.AppStrings
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

            // Lock automatically when app goes to background
            if (biometricLock) {
                LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
                    isUnlocked = false
                }
            }

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
    strings: AppStrings,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var authError by remember { mutableStateOf<String?>(null) }
    var deviceNotSecured by remember { mutableStateOf(false) }

    fun showPrompt() {
        activity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)

        if (canAuth == BiometricManager.BIOMETRIC_ERROR_NO_DEVICE_CREDENTIAL) {
            deviceNotSecured = true
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authError = null
                    onAuthenticated()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val dismissed = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                            errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                            errorCode == BiometricPrompt.ERROR_CANCELED
                    if (!dismissed) authError = errString.toString()
                }
                override fun onAuthenticationFailed() { /* prompt handles UI feedback */ }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(strings.unlockApp)
            .setSubtitle(strings.authenticateToContinue)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { showPrompt() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (deviceNotSecured) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = if (deviceNotSecured) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                strings.unlockApp,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (deviceNotSecured) {
                Text(
                    strings.deviceNotSecured,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(32.dp))
                Button(onClick = onAuthenticated) {
                    Text(strings.continueAnyway)
                }
            } else {
                Text(
                    strings.authenticateToContinue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                authError?.let { error ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(32.dp))
                Button(onClick = { showPrompt() }) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null,
                        modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.tryAgain)
                }
            }
        }
    }
}
