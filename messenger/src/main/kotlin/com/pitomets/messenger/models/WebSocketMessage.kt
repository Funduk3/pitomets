package com.pitomets.messenger.models

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val chatId: Long? = null,
    val content: String? = null,
    val senderId: Long? = null,
    val lastMessageIds: Map<String, String>? = null // Map<chatId, lastMessageId> как строки для JSON
)