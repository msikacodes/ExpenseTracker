package com.example.myapplication.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.ui.theme.FoodColor
import com.example.myapplication.ui.theme.OtherColor
import com.example.myapplication.ui.theme.ShoppingColor
import com.example.myapplication.ui.theme.TransportColor
import com.example.myapplication.ui.theme.UtilitiesColor

enum class Category(val displayName: String, val color: Color, val icon: ImageVector) {
    FOOD("Food", FoodColor, Icons.Rounded.Restaurant),
    TRANSPORT("Transport", TransportColor, Icons.Rounded.DirectionsCar),
    SHOPPING("Shopping", ShoppingColor, Icons.Rounded.ShoppingBag),
    UTILITIES("Utilities", UtilitiesColor, Icons.Rounded.Tv),
    OTHER("Other", OtherColor, Icons.Rounded.Category)
}
