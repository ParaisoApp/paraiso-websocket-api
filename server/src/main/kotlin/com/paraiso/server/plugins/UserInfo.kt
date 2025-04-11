package com.paraiso.server.plugins

data class UserInfo(
    val id: String,
    var connected: Boolean,
    var lastSeen: Long = System.currentTimeMillis()
)
