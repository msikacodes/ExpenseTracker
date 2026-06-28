package com.msika.pesatrack.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.msika.pesatrack.ui.settings.LocalAppStrings
import com.msika.pesatrack.ui.theme.FoodColor
import com.msika.pesatrack.ui.theme.OtherColor
import com.msika.pesatrack.ui.theme.ShoppingColor
import com.msika.pesatrack.ui.theme.TransportColor
import com.msika.pesatrack.ui.theme.UtilitiesColor

enum class Category(val displayName: String, val color: Color, val icon: ImageVector) {
    FOOD("Food", FoodColor, Icons.Rounded.Restaurant),
    TRANSPORT("Transport", TransportColor, Icons.Rounded.DirectionsCar),
    SHOPPING("Shopping", ShoppingColor, Icons.Rounded.ShoppingBag),
    UTILITIES("Utilities", UtilitiesColor, Icons.Rounded.Tv),
    OTHER("Other", OtherColor, Icons.Rounded.Category);

    @Composable
    fun getLocalizedName(): String {
        val strings = LocalAppStrings.current
        return when (this) {
            FOOD -> strings.categoryFood
            TRANSPORT -> strings.categoryTransport
            SHOPPING -> strings.categoryShopping
            UTILITIES -> strings.categoryUtilities
            OTHER -> strings.categoryOther
        }
    }
}
