package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Category
import com.example.myapplication.data.Expense
import com.example.myapplication.db.ExpenseDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

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

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses, _selectedMonth, _selectedYear, _searchQuery, _selectedCategory
    ) { expenses, month, year, query, category ->
        expenses.filter { expense ->
            val cal = Calendar.getInstance().apply { timeInMillis = expense.date }
            val matchesMonth = cal.get(Calendar.MONTH) == month && cal.get(Calendar.YEAR) == year
            val matchesSearch = query.isBlank() ||
                    expense.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || expense.category == category
            matchesMonth && matchesSearch && matchesCategory
        }
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
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (daysInMonth > 0) expenses.sumOf { it.amount } / daysInMonth else 0.0
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
