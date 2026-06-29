package com.msika.pesatrack.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.msika.pesatrack.data.Category
import com.msika.pesatrack.data.Expense
import com.msika.pesatrack.db.ExpenseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

private data class FilteredParams(
    val startTimestamp: Long,
    val endTimestamp: Long,
    val category: Category?,
    val query: String
)

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ExpenseDatabase.getDatabase(application)
    private val dao = db.expenseDao()

    val allExpenses: StateFlow<List<Expense>> = dao.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // -- Filters --
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH))
    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<Category?>(null)

    val selectedMonth: StateFlow<Int> = _selectedMonth
    val selectedYear: StateFlow<Int> = _selectedYear
    val searchQuery: StateFlow<String> = _searchQuery
    val selectedCategory: StateFlow<Category?> = _selectedCategory

    private val _debouncedQuery = _searchQuery.debounce(300L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        _selectedMonth, _selectedYear, _debouncedQuery, _selectedCategory
    ) { month, year, query, category ->
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        FilteredParams(startCal.timeInMillis, endCal.timeInMillis, category, query)
    }.flatMapLatest { params ->
        dao.getFilteredExpenses(params.startTimestamp, params.endTimestamp, params.category, params.query)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSpent: StateFlow<Double> = filteredExpenses.map { expenses ->
        expenses.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categoryTotals: StateFlow<Map<Category, Double>> = filteredExpenses.map { expenses ->
        expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // -- Statistics --
    val totalExpensesCount: StateFlow<Int> = filteredExpenses.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val highestExpense: StateFlow<Double> = filteredExpenses.map { expenses ->
        expenses.maxOfOrNull { it.amount } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val lowestExpense: StateFlow<Double> = filteredExpenses.map { expenses ->
        expenses.minOfOrNull { it.amount } ?: 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val averagePerDay: StateFlow<Double> = combine(
        filteredExpenses, _selectedMonth, _selectedYear
    ) { expenses, month, year ->
        val now = Calendar.getInstance()
        val isCurrentMonth = month == now.get(Calendar.MONTH) && year == now.get(Calendar.YEAR)
        val divisor = if (isCurrentMonth) {
            now.get(Calendar.DAY_OF_MONTH)
        } else {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
            }.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
        if (divisor > 0) expenses.sumOf { it.amount } / divisor else 0.0
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // -- Filters --
    private var lastDeletedExpense: Expense? = null

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setCategory(category: Category?) { _selectedCategory.value = category }

    fun previousMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, -1)
        }
        _selectedMonth.value = cal.get(Calendar.MONTH)
        _selectedYear.value = cal.get(Calendar.YEAR)
    }

    fun nextMonth() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            add(Calendar.MONTH, 1)
        }
        _selectedMonth.value = cal.get(Calendar.MONTH)
        _selectedYear.value = cal.get(Calendar.YEAR)
    }

    // -- CRUD --
    fun addExpense(expense: Expense) {
        viewModelScope.launch { dao.insertExpense(expense) }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch { dao.updateExpense(expense) }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            dao.deleteExpense(expense)
            lastDeletedExpense = expense
        }
    }

    fun undoDelete() {
        lastDeletedExpense?.let { expense ->
            viewModelScope.launch {
                dao.insertExpense(expense)
                lastDeletedExpense = null
            }
        }
    }
}
