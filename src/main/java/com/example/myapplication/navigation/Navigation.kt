package com.example.myapplication.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object Dashboard : Destination
}
