package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val category: Category,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val photoUri: String? = null
)
