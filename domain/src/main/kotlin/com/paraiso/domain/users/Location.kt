package com.paraiso.domain.users

import com.paraiso.domain.util.Constants
import kotlinx.serialization.Serializable


@Serializable
data class Location(
    val city: String,
    val state: String,
    val country: Country,
) { companion object }

@Serializable
data class Country(
    val name: String,
    val code: String,
) { companion object }

fun Location.Companion.initLocation() =
    Location(
        city = Constants.EMPTY,
        state = Constants.EMPTY,
        country = Country.initCountry(),
    )
fun Country.Companion.initCountry() =
    Country(
        name = Constants.EMPTY,
        code = Constants.EMPTY,
    )