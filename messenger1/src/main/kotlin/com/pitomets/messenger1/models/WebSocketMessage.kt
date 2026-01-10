package com.pitomets.messenger1.models

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val chatId: Long? = null,
    val content: String? = null,
    val senderId: Long? = null
)