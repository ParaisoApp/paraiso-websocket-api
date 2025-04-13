package com.paraiso.server.messageTypes

import com.paraiso.domain.auth.UserRole
import com.paraiso.domain.auth.UserStatus
import com.paraiso.domain.util.Constants.UNKNOWN
import kotlinx.serialization.Serializable
import com.paraiso.domain.auth.User as UserDomain

@Serializable
data class User(
    val id: String,
    val name: String,
    val roles: UserRole,
    val banned: Boolean,
    val status: UserStatus,
    val blockList: Set<String>,
    val lastSeen: Long
) { companion object }

fun User.Companion.createUnknown() =
    User(
        id = UNKNOWN,
        name = UNKNOWN,
        banned = false,
        roles = UserRole.GUEST,
        lastSeen = System.currentTimeMillis(),
        blockList = emptySet(),
        status = UserStatus.CONNECTED
    )
fun UserDomain.toResponse() = User(
    id = id,
    name = name,
    banned = banned,
    roles = roles,
    lastSeen = lastSeen,
    blockList = blockList,
    status = status
)
fun User.toDomain() = UserDomain(
    id = id,
    name = name,
    banned = banned,
    roles = roles,
    lastSeen = lastSeen,
    blockList = blockList,
    status = status
)
