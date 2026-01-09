package com.pitomets.messenger1.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.datetime.Instant

@Serializable
data class ChatEntity(
    val id: Long,
    val user1Id: Long,
    val user2Id: Long,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant
)

