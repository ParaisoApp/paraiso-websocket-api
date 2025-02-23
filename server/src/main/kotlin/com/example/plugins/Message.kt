package com.example.plugins

 import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val content: String? = "",
    val type: String? = ""
)
