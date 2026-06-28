package com.msika.pesatrack.db

import androidx.room.TypeConverter
import com.msika.pesatrack.data.Category

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
