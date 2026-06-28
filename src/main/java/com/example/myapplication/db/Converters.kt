package com.example.myapplication.db

import androidx.room.TypeConverter
import com.example.myapplication.data.Category

class Converters {
    @TypeConverter
    fun fromCategory(category: Category): String {
        return category.name
    }

    @TypeConverter
    fun toCategory(value: String): Category {
        return Category.valueOf(value)
    }
}
