package com.example.myapplication.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.ui.settings.AppStrings
import com.example.myapplication.ui.settings.EnglishStrings
import com.example.myapplication.ui.settings.LocalAppStrings
import com.example.myapplication.ui.theme.ExpenseTrackerTheme
import com.example.myapplication.util.formatTZS
import com.example.myapplication.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private val monthNames = listOf(
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onAddExpenseClick: () -> Unit,
    onMenuClick: () -> Unit,
    onEditExpense: (Expense) -> Unit,
    monthlyBudget: Double,
    onExportClick: () -> Unit
) {
    val expenses by viewModel.filteredExpenses.collectAsState()
    val totalSpent by viewModel.totalSpent.collectAsState()
    val categoryTotals by viewModel.categoryTotals.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val totalCount by viewModel.totalExpensesCount.collectAsState()
    val avgPerDay by viewModel.averagePerDay.collectAsState()
    val highest by viewModel.highestExpense.collectAsState()
    val lowest by viewModel.lowestExpense.collectAsState()
    val strings = LocalAppStrings.current

    var showSearch by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.appName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Rounded.Menu, contentDescription = strings.menu)
                    }
                },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(if (showSearch) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = strings.search)
                    }
                    IconButton(onClick = { showStats = !showStats }) {
                        Icon(Icons.Rounded.BarChart, contentDescription = strings.statistics)
                    }
                    IconButton(onClick = onExportClick) {
                        Icon(Icons.Rounded.Share, contentDescription = strings.export)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            LargeFloatingActionButton(
                onClick = onAddExpenseClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add", modifier = Modifier.size(36.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets.systemBars
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search bar
            if (showSearch) {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(strings.searchExpenses) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = strings.close)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // Category filter chips
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { viewModel.setCategory(null) },
                        label = { Text(strings.allCategories) }
                    )
                    Category.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { viewModel.setCategory(cat) },
                            label = { Text(cat.displayName) },
                            leadingIcon = {
                                Icon(cat.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            // Month navigator
            item {
                MonthNavigator(
                    selectedMonth = selectedMonth,
                    selectedYear = selectedYear,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() },
                    strings = strings
                )
            }

            // Total spent card
            item {
                TotalSpentCard(totalSpent = totalSpent)
            }

            // Budget progress
            if (monthlyBudget > 0 && totalSpent > 0) {
                item {
                    BudgetProgressBar(
                        spent = totalSpent,
                        budget = monthlyBudget,
                        strings = strings
                    )
                }
            }

            // Statistics section
            if (showStats && expenses.isNotEmpty()) {
                item {
                    StatisticsSection(
                        totalCount = totalCount,
                        totalSpent = totalSpent,
                        avgPerDay = avgPerDay,
                        highest = highest,
                        lowest = lowest,
                        strings = strings
                    )
                }
            }

            // Pie chart + category breakdown
            if (categoryTotals.isNotEmpty()) {
                item {
                    CategoryBreakdown(
                        categoryTotals = categoryTotals,
                        totalSpent = totalSpent,
                        strings = strings
                    )
                }
            }

            // Transaction header
            item {
                Text(
                    text = strings.transactions,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Transaction list or empty
            if (expenses.isEmpty()) {
                item {
                    EmptyTransactions(strings = strings)
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    SwipeToDeleteItem(
                        expense = expense,
                        onDelete = {
                            viewModel.deleteExpense(expense)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = strings.expenseDeleted,
                                    actionLabel = strings.undo,
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.undoDelete()
                                }
                            }
                        }
                    ) {
                        ExpenseItem(
                            expense = expense,
                            onClick = { onEditExpense(expense) }
                        )
                    }
                }
            }

            // Bottom spacer for FAB
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun EmptyTransactions(strings: AppStrings) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.Receipt, contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(strings.noExpenses, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(strings.tapToAdd, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        }
    }
}

@Composable
fun MonthNavigator(
    selectedMonth: Int, selectedYear: Int,
    onPrevious: () -> Unit, onNext: () -> Unit, strings: AppStrings
) {
    val now = Calendar.getInstance()
    val isCurrent = selectedMonth == now.get(Calendar.MONTH) && selectedYear == now.get(Calendar.YEAR)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = strings.previousMonth,
                tint = MaterialTheme.colorScheme.primary)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${monthNames[selectedMonth]} $selectedYear",
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (isCurrent) Text(strings.thisMonth,
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        IconButton(onClick = onNext, enabled = !isCurrent) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = strings.nextMonth,
                tint = if (isCurrent) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun TotalSpentCard(totalSpent: Double) {
    val animated by animateFloatAsState(totalSpent.toFloat(), tween(800))
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text("Total Spent", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            Text(formatTZS(animated.toDouble()),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun BudgetProgressBar(spent: Double, budget: Double, strings: AppStrings) {
    val fraction = (spent / budget).toFloat().coerceIn(0f, 1.5f)
    val isExceeded = spent > budget
    val barColor = when {
        isExceeded -> MaterialTheme.colorScheme.error
        fraction > 0.75f -> Color(0xFFFFA000)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(strings.budget, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (isExceeded) {
                    Surface(shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)) {
                        Text(strings.budgetExceeded, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${formatTZS(spent)} / ${formatTZS(budget)}",
                    style = MaterialTheme.typography.bodySmall)
                if (!isExceeded) {
                    Text("${strings.budgetRemaining}: ${formatTZS(budget - spent)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun StatisticsSection(
    totalCount: Int, totalSpent: Double, avgPerDay: Double,
    highest: Double, lowest: Double, strings: AppStrings
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.statistics, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem(strings.totalExpenses, "$totalCount")
                StatItem(strings.averagePerDay, formatTZS(avgPerDay))
                StatItem(strings.highestExpense, formatTZS(highest))
                StatItem(strings.lowestExpense, formatTZS(lowest))
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center)
    }
}

@Composable
fun CategoryBreakdown(
    categoryTotals: Map<Category, Double>,
    totalSpent: Double,
    strings: AppStrings
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(strings.spendingByCategory,
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            val catEntries = categoryTotals.entries.toList()
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Pie chart
                Canvas(modifier = Modifier.size(120.dp)) {
                    val arcSize = Size(size.width, size.height)
                    var startAngle = -90f
                    catEntries.forEach { (cat, amount) ->
                        val sweep = (amount / totalSpent * 360).toFloat()
                        drawArc(color = cat.color, startAngle = startAngle,
                            sweepAngle = sweep, useCenter = true, size = arcSize)
                        startAngle += sweep
                    }
                    // Center hole for donut
                    drawCircle(color = Color.Transparent, radius = size.minDimension * 0.35f,
                        style = Stroke(width = 2.dp.toPx()))
                }
                Spacer(Modifier.width(16.dp))
                // Legend
                Column(modifier = Modifier.weight(1f)) {
                    catEntries.sortedByDescending { it.value }.forEach { (cat, amount) ->
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(cat.color))
                            Spacer(Modifier.width(8.dp))
                            Text(cat.displayName, style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f))
                            Text(formatTZS(amount), style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Horizontal bars
            val sortedCatEntries = catEntries.sortedByDescending { it.value }
            sortedCatEntries.forEach { (category, amount) ->
                val fraction = (amount / totalSpent).toFloat()
                CategoryBar(category, amount, fraction)
                if (category != sortedCatEntries.last().key) Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun CategoryBar(category: Category, amount: Double, fraction: Float) {
    val animated by animateFloatAsState(fraction, tween(600))
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(category.icon, contentDescription = null, tint = category.color,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(category.displayName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium)
            }
            Text(formatTZS(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(3.dp))
                .background(category.color))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteItem(expense: Expense, onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f)),
                contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp))
            }
        }
    ) { content() }
}

@Composable
fun ExpenseItem(expense: Expense, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = expense.category.color.copy(alpha = 0.15f)
            ) {
                Icon(expense.category.icon, contentDescription = null,
                    tint = expense.category.color, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(expense.date)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    if (expense.photoUri != null) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }
            Text(formatTZS(expense.amount), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun DashboardPreview() {
    val sample = listOf(
        Expense(amount = 45000.0, category = Category.FOOD, description = "Lunch at cafe"),
        Expense(amount = 15000.0, category = Category.TRANSPORT, description = "Taxi home"),
        Expense(amount = 80000.0, category = Category.SHOPPING, description = "Groceries")
    )
    ExpenseTrackerTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MonthNavigator(Calendar.getInstance().get(Calendar.MONTH),
                    Calendar.getInstance().get(Calendar.YEAR), {}, {}, EnglishStrings)
                TotalSpentCard(140000.0)
                BudgetProgressBar(140000.0, 200000.0, EnglishStrings)
                sample.forEach { ExpenseItem(it) }
            }
        }
    }
}
