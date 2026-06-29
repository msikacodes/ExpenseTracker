package com.msika.pesatrack.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.msika.pesatrack.data.Category
import com.msika.pesatrack.data.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE date >= :startTimestamp AND date <= :endTimestamp AND (:category IS NULL OR category = :category) AND (:query = '' OR description LIKE '%' || :query || '%' OR CAST(amount AS TEXT) LIKE '%' || :query || '%') ORDER BY date DESC")
    fun getFilteredExpenses(
        startTimestamp: Long,
        endTimestamp: Long,
        category: Category?,
        query: String
    ): Flow<List<Expense>>

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
