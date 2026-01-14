package com.pitomets.messenger.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.datetime.Instant

@Serializable
data class MessageEntity(
    val id: Long,
    val chatId: Long,
    val senderId: Long,
    val content: String,
    @Contextual val createdAt: Instant,
    val isRead: Boolean
)

