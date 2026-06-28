package com.msika.pesatrack.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination {
    @Serializable
    data object Dashboard : Destination
}
