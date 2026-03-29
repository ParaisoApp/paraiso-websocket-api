package com.paraiso.domain.auth

import kotlinx.serialization.Serializable

@Serializable
data class Login(
    val userId: String,
    val email: String,
    val password: String
)
@Serializable
data class AuthIdResponse(
    val id: String,
    val connection: String,
    val provider: String,
    val email: String,
    val name: String?,
    val picture: String?,
    val userId: String
)
@Serializable
data class AuthId(
    val id: String,
    val connection: String,
    val provider: String,
    val email: String,
    val name: String?,
    val picture: String?
)
@Serializable
data class TicketResponse(
    val ticket: String,
    val userId: String
)

fun AuthIdResponse.toEntity() =
    AuthId(
        id = id,
        connection = connection,
        provider = provider,
        email = email,
        name = name,
        picture = picture,
    )
