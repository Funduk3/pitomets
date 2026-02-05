package com.pitomets.messenger.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ChatEntity(
    val id: Long,
    val user1Id: Long,
    val user2Id: Long,
    val listingId: Long? = null,
    val listingTitle: String? = null,
    @Contextual val createdAt: Instant,
    @Contextual val updatedAt: Instant,
    val unreadCountUser1: Int,
    val unreadCountUser2: Int
)
