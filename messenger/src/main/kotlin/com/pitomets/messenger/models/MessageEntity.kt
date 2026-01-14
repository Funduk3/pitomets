package com.pitomets.messenger.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class MessageEntity(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val content: String,
    @Contextual val createdAt: Instant,
    val isRead: Boolean
)
