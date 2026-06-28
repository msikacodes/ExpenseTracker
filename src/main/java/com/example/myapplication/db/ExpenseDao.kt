package com.example.myapplication.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.myapplication.data.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE description LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchExpenses(query: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalSpent(): Flow<Double?>
}
