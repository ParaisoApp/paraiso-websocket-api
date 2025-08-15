package com.paraiso.domain.users

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val city: String?,
    val state: String?,
    val country: Country?
) { companion object }

@Serializable
data class Country(
    val name: String?,
    val code: String?
) { companion object }
